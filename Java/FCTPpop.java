import java.util.*;

/**
 * Population based metaheuristic methods for the Fixed Charge Transportation Problem (FCTP)
 *
 * @author  Andreas Klose
 * @version 08/05/2018
 */
public class FCTPpop extends PEheur
{  
  /**
   * Constructor 
   *    
   * @param fname name (that is full path) of the input data file
   */
  public FCTPpop( String fname ) throws Exception
  {
    super( fname );
  }

  /**
   * Constructor that takes data as parameters. All data are copied if copyDat=true
   * 
   *  @param mm       number of suppliers
   *  @param nn       number of customers
   *  @param s        integer array of mm supply quantities
   *  @param d        integer array of nn demand quantities
   *  @param tc       tc[arc] is for arc=i*nn+j the unit transporation cost from supplier i to customer j
   *  @param fc       fc[arc] is for arc=i*nn+j the fixed cost on arc from supplier i to customer 
   *  @param copyDat  true if data should be copied to new arrays. Otherwise just a reference is set to the data.
   */
  public FCTPpop( int mm, int nn, int[] s, int[] d, double[] tc, double[] fc, boolean copyDat ) 
  {
    super( mm, nn, s, d, tc, fc, copyDat );  
  }    

  /**
   * Random greedy construction done by a single ant
   *
   * @param double[] pheromone array of doubles containing current pheromone value of each arc
   * @param double q0          pseudo-random selection parameter 
   *                           (q0 is the probability with which a pure greedy step is applied)
   */
  private void AntProcess( double[] pheromone, double q0 )
  {
    // Initialise remaining supply and demand
    int[] r_supply = new int[m];  
    int[] r_demand = new int[n];  
    int[] suppliers = new int[m]; 
    int[] customers = new int[n]; 
    System.arraycopy( supply, 0, r_supply, 0, m );
    System.arraycopy( demand, 0, r_demand, 0, n );
    for ( int i=0; i < m; i++ ) suppliers[i]= i;
    for ( int j=0; j < n; j++ ) customers[j]= j;
    
    // Set initial flows to zero
    Arrays.fill( solution.flow, 0 );
    
    // Iteratively add arcs to the solution until demand and supply is exhausted
    int mm = m;
    int nn = n;
    int[] arc_lst = new int[narcs];      
    double[] arc_val= new double[narcs]; 
    while ( ( mm > 0 ) && ( nn > 0 ) )
    { // Compute greedy evaluations of arcs multiplied by pheromone values
      int lst_len = 0;
      double max_val = 0.0;
      int bst_arc = -1;
      for ( int i=0; i < mm; i++ )
      {
        int ii = suppliers[i];
        for ( int j=0; j < nn; j++ )
        {
          int jj = customers[j];
          double gval = GreedyValue( ii, jj, r_supply[ii], r_demand[jj] );
          gval *= pheromone[ii*n+jj]; // product of greedy and pheromone value
          if ( gval > max_val )
          {
            max_val = gval;
            bst_arc = i*nn+j;
          }  
          arc_val[lst_len] = gval;
          arc_lst[lst_len++] = i*nn+j;
        }
      }
      // Apply pseudo-random selection
      double a_rnd = randgen.nextDouble();
      int arc = bst_arc; // This is the pure greedy choice
      if ( a_rnd > q0 )
      { // Randomly choose an arc with probability proportional to its "value"
        double sum_val = 0.0;
        for ( int idx=0; idx < lst_len; idx++ ) sum_val += arc_val[idx];
        a_rnd = randgen.nextDouble();
        double asum = 0.0;
        for ( arc=0; arc < lst_len; arc++ )
        {
          asum += arc_val[arc]/sum_val;
          if ( asum + 1.0E-6 > a_rnd ) break;
        }
        arc = arc_lst[arc];
      }
      /* Flow as much as possible on the selected arc */
      int i  = arc/nn;
      int j  = arc % nn;
      int ii = suppliers[i];
      int jj = customers[j];
      int ecap = Math.min( r_supply[ii], r_demand[jj] );
      solution.flow[ii*n+jj] = ecap;
      r_supply[ii] -= ecap;
      r_demand[jj] -= ecap;
      if ( r_supply[ii]==0 ) suppliers[i]  = suppliers[--mm];
      if ( r_demand[jj]==0 ) customers[j] = customers[--nn];
    }

    // Determine basis tree and cost of constructed solution
    setBasicSolution();
    
    // Improve solution using local search
    LocalSearch( );
    
  }

  
  /**
   * Ant colony system approach to the FCTP
   */
  public void AntColony()
  {
    double rho   = 0.9; // parameter used to mimic pheromone evaporation
    double alpha = 0.8; // weight of iteration best ant

    FCTPsol best_sol = new FCTPsol( m, n );   
    FCTPsol iter_sol = new FCTPsol( m, n);
    best_sol.totalCost = Double.MAX_VALUE;

    double[] pheromone = new double[narcs];
    Arrays.fill( pheromone, 1.0 );
    
    // Scaling parameter Q
    double Q = Double.MAX_VALUE;
    int totS = 0;
    for ( int i=0; i < m; i++ ) totS += supply[i];
    for ( int arc=0; arc < narcs; arc++ )
    {
      double tmp = tcost[arc] + fcost[arc]/getCap(arc);
      if ( tmp < Q ) Q = tmp;
    }
    Q *= totS;
    
    if ( FCTPparam.screen_on ) 
    {
      System.out.println("================= ANT procedure ===============");
      System.out.println("Scaling parameter is "+Q);
      System.out.println("Iter  Ant_nr.  Iter_best  Best_objval");
    }

    int iter = 0; // iteration counter
    int itr = 0;  // counter of subsequent unsuccessful iterations
    int num_ants = m; // number of ants to be used
    do
    {
      iter++;
      iter_sol.totalCost = Double.MAX_VALUE;
      int it_queen=0;
      for ( int ant=0; ant < num_ants; ant++ ) 
      {
        // ant nr. "ant" constructs a solution
        AntProcess( pheromone, 0.5 );
        // new iteration-best ant?
        if ( solution.totalCost < iter_sol.totalCost ) 
        {
          iter_sol.Overwrite(solution);
          it_queen = ant;
        }  
      }
      itr++;
      if ( iter_sol.totalCost < best_sol.totalCost ) 
      { // new global best ant
        itr = 0;
        best_sol.Overwrite( iter_sol );
      }
      if ( FCTPparam.screen_on )
        System.out.format("%4d  %7d  %9.2f  %11.2f%n",iter,it_queen,iter_sol.totalCost,best_sol.totalCost);
      // Update pheromone values
      for ( int arc = 0; arc < narcs; arc++ )
      { 
        pheromone[arc] *= rho; // evaporation
        if ( best_sol.flow[arc] > 0 ) pheromone[arc] += (1.0-alpha)*Q/best_sol.totalCost;
        if ( iter_sol.flow[arc] > 0 ) pheromone[arc] += alpha*Q/iter_sol.totalCost;
      }  
      
    } while ( itr < FCTPparam.max_no_imp );
    
    // Reset the library's internal solution to best solution found above
    solution.Overwrite( best_sol );
    iterCount = iter;
    
  }
  
  /**
   *  Simple evolutionary algorithm for the FCTP. Mutation is applied by applying
   *  a random perturbation and improving the resulting solution by local search 
   *  (usually first-accept)
   */
  public void EA()
  {
    if ( FCTPparam.screen_on )
    {
      System.out.println("================= EVOLUTIONARY A. =============");
      System.out.print("Building initial population:");
    }

    // reserve mem for population of solutions
    FCTPsol[] Population = new FCTPsol[FCTPparam.lambda+FCTPparam.mu]; 
    
    // The first solution in the population is the one obtained by the construction procedure
    if ( FCTPparam.screen_on ) System.out.print(".");
    Population[0] = new FCTPsol( solution );

    // The Second solution is the initial one improved by a local search
    LocalSearch();
    Population[1] = new FCTPsol( solution );
    if ( FCTPparam.screen_on ) System.out.print(".");
    
    // average, best and worst fitness value
    double worst_fit = Population[0].totalCost;
    double best_fit  = Population[1].totalCost;
    double ave_fit   = worst_fit + best_fit;
    
    // Fill the population with solutions generated from a randomized greedy
    // where the last half of the population is purely randomly generated
    int pop_size = 2;
    for ( double greedy_rnd = 0.5; pop_size < FCTPparam.lambda; pop_size++ )
    {
      RandGreedy( greedy_rnd );
      if ( FCTPparam.screen_on ) System.out.print(".");
      Population[pop_size] = new FCTPsol( solution );
      ave_fit += solution.totalCost;
      if ( solution.totalCost > worst_fit ) worst_fit = solution.totalCost;
      if ( solution.totalCost < best_fit ) best_fit = solution.totalCost;        
      if ( pop_size >= FCTPparam.lambda/2 ) greedy_rnd = 1.0;
    }
    ave_fit /= FCTPparam.lambda;

    if ( FCTPparam.screen_on )
    {
      System.out.println();
      System.out.println("Generation  Mean_Fitness  Worst_Fitness  Best_Fitness");
      System.out.format("%10d  %12.2f  %13.2f  %12.2f%n",0,ave_fit,worst_fit,best_fit);
    }

    // Allocate memory for the child solutions
    for ( int nchild=0; nchild < FCTPparam.mu; nchild++ )
      Population[FCTPparam.lambda+nchild] = new FCTPsol( m, n );
         
    int iter  = 0;
    int nfail = 0;
    do
    {
      iter++;
      double oldbest = best_fit;
      for ( int nchilds=0; nchilds < FCTPparam.mu; nchilds++ )
      { // Generate "off-spring" of a randomly selected mother        
        int num = randgen.nextInt(FCTPparam.lambda);
        solution.Overwrite( Population[num] );
        // Reducing randomness of the kick for later generations can be worth 
        // to be investigated. Set, e.g., nkick = max( 10, (m+n-1)/2/log(iter+1) );
        Kicksolution( (m+n-1)/2 );
        LocalSearch( );
        Population[FCTPparam.lambda+nchilds].Overwrite( solution );
      }
      // Let new population consist of the best lambda solutions
      Arrays.sort( Population, 0, FCTPparam.lambda+FCTPparam.mu );
      // Compute average, worst and best fitness of new population
      best_fit  = Population[0].totalCost;
      worst_fit = Population[FCTPparam.lambda-1].totalCost;
      ave_fit   = 0.0;
      for ( int num=0; num < FCTPparam.lambda; num++ ) ave_fit += Population[num].totalCost;
      ave_fit /= FCTPparam.lambda;
      if ( FCTPparam.screen_on ) 
        System.out.format("%10d  %12.2f  %13.2f  %12.2f%n",iter,ave_fit,worst_fit,best_fit);
      nfail++;  
      if ( best_fit < oldbest ) nfail = 0;
      if ( nfail > FCTPparam.max_no_imp ) 
      {
        // Stop if no improvement in max_no_imp subsequent iterations
        // and difference between average and best solution in pool is small
        double dist = (ave_fit - best_fit)/best_fit * 100;
        if ( dist < 0.01 ) break;
      }  
    } while (true);

    // The first solution in "Population" is the best one obtained
    solution.Overwrite( Population[0] );
    iterCount = iter;

  }


  /**
   *  Path relinking procedure that searches a path from an initial solution to a
   *  guiding solution by means of applying basic exchanges. Morevoer, after each
   *  block of "impFreq" such moves, the current solution on the path is improved
   *  by calling an improvement procedure as as local search.
   *
   *  @param FCTPsol iniSol initial solution (source of the path)
   *  @param Solution guideSol guiding solution (sink of the path)
   *  @param int impFreq every impFreq iteration, the current solution on the path 
   *                     is improved by means of a local search. Set impFreq to a 
   *                     very high value if the improvement method should never be used.
   *
   *  @return the best solution found by the path relinking
   */
  private FCTPsol PathRelink( FCTPsol iniSol, FCTPsol guideSol, int impFreq )
  {
    boolean useILS = ( impFreq < 0 );
    if ( impFreq < 0 ) impFreq *= -1;

    // Compute difference in set of basic arcs between initial and guiding solution
    int basDist = 0;
    for ( int arc = 0; arc < narcs; arc++ )
    {
      if ( guideSol.arc_stat[arc] == BASIC)
      {
        if ( iniSol.arc_stat[arc] != BASIC ) basDist++;
      }
      else if ( iniSol.arc_stat[arc] == BASIC ) basDist++;
    }

    // decide on impFreq:
    impFreq = Math.min(Math.max(basDist/10,5),10);
    
    // As long as possible introduce an arc that is non-basic in the current solution
    // but basic in the guiding solution into the basis provided that the arc leaving
    // the basis is also non-basic in the guiding solution
    int iter = 0;
    FCTPsol pathSol = null;
    FCTPsol curSol = null;
    solution.Overwrite ( iniSol ); // instead of iniSol.setBasis() ?
    do
    {
      iter++;
      // Find best admissible move.
      double bstSav = -Double.MAX_VALUE;
      boolean canMove = false;
      for ( int arc = 0; arc < narcs; arc++ )
      {
        if ( (guideSol.arc_stat[arc] == BASIC) && ( solution.arc_stat[arc] != BASIC ) )
        {
          double cstSav = getCostSav( arc );
          int arcOut = getLeavingArc();
          if ( ( guideSol.arc_stat[arcOut] != BASIC ) && ( cstSav > bstSav ) )
          {
            canMove = true;
            bstSav = cstSav;
            RememberMove();
          }
        }
      }
      // Terminate the path relinking if there is no more an admissible move
      // or if we will reach the guiding solution by applying the move
      basDist -= 2;
      if ( ( basDist <= 0 ) || ( !canMove ) ) break;

      DoMove();

      // Check if we get an improved solution on the path between
      // initial and guiding solution
      if ( iter % impFreq == 0 )
      { 
        // Save the solution just reached on the path
        if ( curSol==null ) curSol = new FCTPsol( solution ); else curSol.Overwrite( solution );
        // Improve the solution by either ILS or ordinary local search
        //if ( useILS ) ILS(); else LocalSearch( );
        LS_first_acc();
        // Store improved solution as new solution on/nearby the path if it improves the one found 
        // so far and is also different from the initial as well as the guiding solution.
        if (   ( (pathSol==null) || (pathSol.totalCost > solution.totalCost) )
            && (!iniSol.equalTo(solution)) && (!guideSol.equalTo(solution)) )
        {
          if (pathSol == null ) pathSol = new FCTPsol( solution ); else pathSol.Overwrite( solution );
        }
        // Return to the solution on the path
        solution.Overwrite( curSol );
      }
      else if ( pathSol == null )
        pathSol = new FCTPsol( solution );
      else if ( pathSol.totalCost > solution.totalCost ) 
        pathSol.Overwrite( solution );
        
    } while (true);

    return ( pathSol );
       
  }

  /**
   *  A scatter search applied on a given pool "Pool" of solutions. The procedure was proposed by 
   *  my students Sune Lauth Gadegaard, Camilla Saaby Hoeholt, and Sandra Bastholm Fischer within 
   *  a "Projektarbejde i matematik-oekonomi", Fall 2011. In each main iteration, a reference set 
   *  of up to 4 solutions is built. These solutions are: (1) the best one found so far, (2) the 
   *  solution that was the best so far in the previous iteration, (3) the worst solution in the 
   *  pool and (4) the one most "distant" from the best. Forward and backward pathes are established 
   *  between all pairs of solutions from the reference set and a local search is applied on the 
   *  best of these "path solutions". After each iteration, the worst and farthest solution is 
   *  removed from the pool. The method thus stops if the pool is exhausted.
   *
   *  @param Pool Pool of solutions on which the scatter search is applied
   *  @param impFreq if different from zero, every abs(impFreq) step in the path relinking, an 
   *                 improvement procedure is applied. This is standard local search if impFreq > 0 
   *                 and ILS if impFreg < 0. 
   */
  private void SS_I( ArrayList Pool, int impFreq )
  {
     boolean doDisplay = FCTPparam.screen_on;
     FCTPparam.screen_on = false;
     if ( doDisplay )
     {
      System.out.println("-----------------------------------------------------------");
      System.out.println("Poolsize  Cur_objval  Best_objval");
     } 
     FCTPsol[] refSet = new FCTPsol[4]; 
     FCTPsol bestSol = (FCTPsol) Collections.min( Pool );
     int numRef = 1;
     refSet[0] = bestSol;
     Pool.remove( bestSol );

     ArrayList pathList = new ArrayList();
   
     do
     { 
       // Extract and remove the worst solution from current pool
       int oldnumRef = numRef;
       FCTPsol worst = (FCTPsol) Collections.max( Pool );
       refSet[numRef++] = worst;
       Pool.remove ( worst );

       // Extract and remove farthest solution from current pool
       int maxDist = 0;
       int fIndex  = -1;
       for ( int s = 0; s < Pool.size(); s++ )
       {
         int dist = bestSol.DistanceTo( (FCTPsol) Pool.get(s) );
         if ( dist > maxDist )
         {
           fIndex = s;
           maxDist = dist;
         }
       }
       if ( fIndex >= 0 )
       {
         FCTPsol farthest = (FCTPsol) Pool.get( fIndex );
         refSet[numRef++] = farthest;
         Pool.remove ( farthest );
       }  

       // Find forward and backward paths between the solutions from the refset
       pathList.clear();
       for ( int i = 0; i < numRef; i++ ) for ( int j = i+1; j < numRef; j++ )
       {
         FCTPsol solij = PathRelink( refSet[i], refSet[j], impFreq );
         if ( solij != null ) pathList.add( solij );
         FCTPsol solji = PathRelink( refSet[j], refSet[i], impFreq );
         if ( solji != null ) pathList.add( solji );
       }

       // Check if new best solution found and update the reference set
       if ( pathList.size() > 0 )
       {
         FCTPsol pathSol = (FCTPsol) Collections.min( pathList );
         solution.Overwrite( pathSol );
         if ( impFreq >= 0 ) 
         {
           ILS();
           pathSol.Overwrite( solution );
         }
         if ( pathSol.totalCost < bestSol.totalCost )
         {
           bestSol = pathSol;
           refSet[0] = pathSol;
           numRef = 1;
         }
         else
         {
           refSet[1] = pathSol;
           numRef = 2;
         }
         if ( doDisplay ) System.out.format("%8d%12.2f%13.2f%n",Pool.size(),pathSol.totalCost,bestSol.totalCost);
       }
       else
         numRef = oldnumRef;
     
     } while ( Pool.size() > 0 );

     // Copy best solution reached to the default solution object
     solution.Overwrite( bestSol );
     if ( impFreq < 0 ) ILS(); // Apply ILS to best solution found so far
     FCTPparam.screen_on = doDisplay;
     iterCount = 0; // no idea to what the iteration number should be set

  }  
   

  /**
   * "Extended" scatter search as proposed by my students Sune Lauth Gadegaard, Camilla Saaby Hoeholt 
   * and Sandra Bastholm Fischer within a "Projektarbejde i matematik-oekonomi", Fall 2011. First a 
   * pool of solution is constructed. Half of the pool is solutions obtained by applying ILS on the 
   * solution obtained from the LP heuristic. The other half is solutions generated by applying 
   * randomised greedy. With this pool, the scatter search method SS_I is executed. The best solution 
   * obtained this way is then again used as a starting point for the ILS.
   */
  public void extSS_SCS ()
  {
    boolean do_display = FCTPparam.screen_on;
    FCTPparam.screen_on = false;
    
    if ( do_display )
      System.out.println("=== Sune, Camilla and Sandra's extended scatter search ====");
     
    // Make a copy of the current initial solution (usuually generated by the LP heuristic)
    FCTPsol startSol = new FCTPsol( solution );

    // Try to fill half the pool with solutions from ILS
    ArrayList Pool = new ArrayList();
    int poolSize = n+m+1;
    int halfPool = poolSize/2;
    int maxTrial = 2*poolSize;
    int numTrial = 0;
    double greediness = 0.0;
    double greedyInc  = 1.0/(double)(halfPool-1);
    if ( do_display ) System.out.print("Filling pool with initial solutions: ");
    do
    {
      boolean useILS = Pool.size() < halfPool;
      if ( useILS )
        ILS();
      else
      {
        RandGreedy( greediness );
        greediness += greedyInc;
      }
      FCTPsol curSol = new FCTPsol( solution );
      if ( ! curSol.containedIn( Pool ) ) Pool.add( curSol );
      if ( useILS ) solution.Overwrite( startSol );
      if ( do_display ) System.out.print(".");
      numTrial++;
    } while ( ( Pool.size() < poolSize ) && ( numTrial < maxTrial ) );
    if ( do_display ) System.out.println();
        
    // Apply scatter search procedure SS_I on the pool of solutions
    FCTPparam.screen_on = do_display;
    SS_I( Pool, 10 );
    
    // Additionally apply ILS() with best solution so far as initial one
    FCTPparam.screen_on = false;
    ILS(); // should not be required as path solutions are now passed to ILS
    FCTPparam.screen_on = do_display;
    iterCount = 0;
    
  }

  /**
   * Applies another type of a scatter search to the given pool Pool of solutions.
   * The method was proposed by my students Sune Lauth Gadegaard, Camilla Saaby Hoeholt
   * and Sandra Bastholm Fischer within a "Projektarbejde i matematik-Oekonomi", Fall 2011.
   * A forward and backward path-relinking is applied between the best and worst solution 
   * in the pool as well as between the best and the solution most distant to the best. Each 
   * solution generated on the path is used as a starting point for an iterated local search. 
   * The best "path solution" generated this way then replaces the worst solution in the pool.
   *
   * @param  Pool    Pool of solutions on which the scatter search is applied
   *         impFreq if different from zero, every abs(impFreq) step in the the path relinking, 
   *                  an improvement procedure is applied. This is standard local search if 
   *                  impFreq > 0 and ILS if impFreg < 0. 
   */
  private void SS_II ( ArrayList Pool, int impFreq )
  {
    FCTPsol[] refSet = new FCTPsol[3];
    refSet[0] = (FCTPsol) Collections.min( Pool ); // best solution in pool
    refSet[1] = (FCTPsol) Collections.max( Pool ); // worst solution in pool
    refSet[2] = null; // will later become solution most distant to best in pool

    boolean do_display = FCTPparam.screen_on;
    FCTPparam.screen_on = false;

    FCTPsol prevFarthest = null; // Solution that was the most distant in previous iteration

    if ( do_display ) System.out.println("Iter  Best in pool  Worst in pool");
    int iter = 0;
    int maxDist=0;
    boolean compFarthest = true;
    do 
    {
      if ( do_display ) System.out.format("%4d  %12.2f  %13.2f%n",iter,refSet[0].totalCost,refSet[1].totalCost);
      iter++;	
      if ( compFarthest )
      { // Find solution in Pool most distant from best solution
        maxDist = -1;
	int fIndex = 0;
        for ( int s = 0; s < Pool.size(); s++ )
        {
          int dist = refSet[0].basDistTo( (FCTPsol) Pool.get(s) );
          if ( dist > maxDist ) { fIndex = s; maxDist = dist; }
        }
	refSet[2] = (FCTPsol) Pool.get( fIndex );
      }
      FCTPsol pathBest = null;
      int num=3;
      // Check if worst and most distant are the same solutions or the most distant 
      // is the same as the one from the previous iteration
      if ( refSet[1].equalTo( refSet[2] ) 
        || ( ( prevFarthest != null ) && prevFarthest.equalTo( refSet[2] ) ) ) num--;
      for ( int i = 1; i < num; i++ )
      {
        FCTPsol pathSol = PathRelink( refSet[0], refSet[i], impFreq );
        if ( pathSol != null )
        { // try to improve the solution using ILS
          solution.Overwrite( pathSol );
          if ( impFreq > 0 ) ILS(); 
          if ( pathBest == null)
            pathBest = new FCTPsol( solution );
          else
            pathBest.Overwrite( solution );  
        }
        pathSol = PathRelink( refSet[i], refSet[0], impFreq );
        if ( pathSol != null )
        { // try to improve the solution using ILS
          solution.Overwrite( pathSol );
          if ( impFreq > 0 ) ILS();
          if ( pathBest == null )
            pathBest = new FCTPsol( solution );
          else
            pathBest.Overwrite( solution );  
        }
      }
      prevFarthest = refSet[2];
      // no solution on any path found -> terminate
      if ( pathBest ==  null ) break;
      // Check if worst solution in pool is improved. If not -> terminate.
      // Also check if recomputation of solution most distant from best is required.
      if ( pathBest.totalCost < refSet[1].totalCost )
      {
        boolean newBest = ( pathBest.totalCost < refSet[0].totalCost );
        compFarthest = newBest;
        if ( ! compFarthest ) compFarthest = refSet[1].equalTo( refSet[2] );
        Pool.remove( refSet[1] );
        Pool.add( pathBest );
        refSet[1] = (FCTPsol) Collections.max( Pool );
        if ( newBest ) refSet[0] = pathBest;  
        if ( ! compFarthest )
        {
          int dist = refSet[0].basDistTo( pathBest );
          if ( dist > maxDist )
          {
            maxDist = dist;
            refSet[2] = refSet[1];
          }
        }
      }
      else
        break;
    } while ( true );

    solution.Overwrite( refSet[0] );
    FCTPparam.screen_on = do_display;
    
  }

  /**
   *  Alternative scatter search procedure suggested by Sune, Camilla and Sandra.
   *  The above scatter search SS_II is applied to a pool of solutions. The first half 
   *  of this pool is solutions obtained by applying randomized greedy followed by local 
   *  search. The other half is also obtained by randomized greedy but with a high degree 
   *  of randomness.
   */
  public void altSS_SCS ()
  {
    if ( FCTPparam.screen_on )
      System.out.println("=== Sune, Camilla and Sandra's alternative scatter search ====");
     
    // Fill the pool with solutions from randomized greedy followed by local search
    if ( FCTPparam.screen_on ) System.out.print("Filling pool with initial solutions: ");
    ArrayList Pool = new ArrayList();
    int poolSize = n+m+1;
    poolSize/=4;
    int halfPool = poolSize/2;
    int maxTrial = 2*poolSize;
    int numTrial = 0;
    double alpha_p = 1.0;
    double alpha = 0.9;
    //LocalSearch(false);
    //ILS();
    // Just add LP solution/initial solution to the pool as it is
    Pool.add( new FCTPsol( solution ) );
    do
    {
      if(Math.random() < 0.5){
        if(randgen.nextBoolean()) LPheu();
        else {RandGreedy(0.5);}
        ILS();
      }
      else {
        if(randgen.nextBoolean()) LPheu();
        else {RandGreedy(0.5);}
        ILS();
      }
      FCTPsol curSol = new FCTPsol( solution );
      if ( ! curSol.containedIn( Pool ) ) Pool.add( curSol );

      if ( FCTPparam.screen_on ) System.out.print(".");
      numTrial++;
    } while ( ( Pool.size() < poolSize ) && ( numTrial < maxTrial ) );
    if ( FCTPparam.screen_on ) System.out.println();

    // SS_II( Pool, -10 );
    SS_I( Pool, -10 );
       
  }  
  
}
