import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Local search methods for the Fixed Charge Transportation Problem (FCTP)
 *
 * @author  Andreas Klose
 * @version 11/05/2018
 */
public class FCTPls extends FCTPgreedy
{
  /** number of iterations performed by last execution of a local search method listed here */
  protected int iterCount;
  protected int[] edge_counts = new int[narcs];
  protected int[] best_edge_counts = new int[narcs];
  protected double[] counts = new double[narcs];
  protected double[] sol_avgs = new double[narcs];
  protected double[] greedy_values = null;
  public FileWriter fileWriter;
 
  /**
   * Constructor 
   *    
   * @param fname name (that is full path) of the input data file
   */
  public FCTPls( String fname ) throws Exception
  {
    super( fname );
    if(FCTPparam.outFile != null && !FCTPparam.outFile.isEmpty()) {
      fileWriter = new FileWriter(FCTPparam.outFile);
      fileWriter.write(FCTPparam.inFile + "\n");
    }
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
  public FCTPls( int mm, int nn, int[] s, int[] d, double[] tc, double[] fc, boolean copyDat ) 
  {
    super( mm, nn, s, d, tc, fc, copyDat );  
  }    

  /**
   *  Return number of iterations performed by one the local search method implemented here
   */
  public int getLSiter( )
  {
    return ( iterCount );
  }
    
  /**
   *  Best accept local search (using basic exchanges to create neighbouring solutons)
   */
  public void LS_best_acc()
  {  
    boolean improve=false;  
    int iter = 0; // internal iteration counter
    do
    {
      double bestSav = FCTPparam.tolval;  
      improve = false;
      for ( int arc=0; arc < narcs; arc++ ) 
      {
        if ( solution.arc_stat[arc] != BASIC ) 
        {
          double saving = getCostSav( arc );
          if ( saving > bestSav )
          {
            bestSav = saving;
            improve = true;
            RememberMove( );
          }    
        }
      }    
      if ( improve )
      {
        iter++;
        DoMove( );
      }    
    } while ( improve );    
    iterCount = iter;
  }



  /**
   *  First accept local search (using basic exchanges to create neighbouring solutons)
   */
  public void LS_first_acc()
  {  
    int cnt = 0;
    int arc = ThreadLocalRandom.current().nextInt(0, narcs-1);;
    int iter = 1;
    do
    {
      arc %= narcs;  
      cnt++;
      if ( solution.arc_stat[arc] != BASIC )
      {
        double saving = getCostSav( arc );
        if ( saving > FCTPparam.tolval )
        {
          RememberMove( );
          DoMove( );
          cnt = 0;
          iter++;
        }
      }
      arc++;
    } while ( cnt < narcs );
    iterCount = iter; 
  }

  /**
   *  Performs a local search on the current solution using basic exchanges to create neighbouring solutions.
   *  If FCTPparam.ls_type = BEST_ACCEPT, a best accept strategy is followed.
   *  If FCTPparam.ls_type = FIRST_ACCEPT, a first accept strategy is followed.
   */
  public void LocalSearch( )
  {
    switch ( FCTPparam.ls_type )
    {
      case FCTPparam.FIRST_ACCEPT: LS_first_acc(); break;
      case FCTPparam.BEST_ACCEPT: LS_best_acc( ); break;
    }
  }


  
  /**
   * Applies a random kick to the solution stored in the object "solution" 
   *
   * @param num_exchanges - number of non-basic arcs to be made basic. If equal to zero, this number
   *                        is randomly decided (between 5 arcs and 20% of the basic arcs)
   */
  public void Kicksolution( int num_exchanges )
  {
    // Record indices of non-basic arcs.
    int[] nb_arcs = new int[narcs];
    int num_nb=0;
    for ( int arc=0; arc < narcs; arc++ )
      if ( solution.arc_stat[arc] != BASIC ) nb_arcs[num_nb++] = arc;

    // If number of exchanges unspecified, then randomly decide on it
    if ( num_exchanges == 0 ) 
    {
      int num_basic = m+n-1;
      num_exchanges = 3;
      // Let the number of exchanges be random between 5 and 20% of number
      // of basic variables
      if ( num_basic/5 > 5 ) num_exchanges = 5 + randgen.nextInt( num_basic/5-4);
    }

    // Apply "numexchanges" random basic exchanges.
    for ( int itr=0; itr < num_exchanges; itr++ ) 
    {
      // Pick a non-basic arc at random 
      int in_arc = randgen.nextInt(num_nb);
      int arc = nb_arcs[in_arc];
      // Introduce the selected non-basic arc into the basis 
      getCostSav( arc );
      RememberMove();
      DoMove();
      nb_arcs[in_arc] = nb_arcs[--num_nb];
    }
    
  }



  
  /**
   *  Iterated local search that uses KickSolution() to generate new (partially) random
   *  start solution from a "current" solution. A single iteration either applies
   *  a first accept or a best accept local search (actually any other local search method
   *  could be used to this end provided it is fast enough.
   */
  public void ILS() 
  {
    // Initialise parameter controlling when to reset the current solution
    int beta = ( m+n-1 )/10;
    if ( beta < 5 ) beta = 5;
    
    // Initialise iteration counters
    int num_fail = 0;
    int iter = 0;
    int miter = FCTPparam.max_iter; // save parameter value (will be altered if ILS_RTR)
    FCTPparam.max_iter = 10; // used in RTR_travel()

    // Display something on the screen, so that we can see that something happens
    boolean display = FCTPparam.screen_on;
    FCTPparam.screen_on = false;
    if ( display ) 
    {
      System.out.println("=============== DOING ILS ================");
      System.out.println("ITER  OBJ (before LS)  OBJ (after LS)  BEST_OBJ");
    }
    
    // Save the initial solution as both the "current" and incumbent solution
    FCTPsol best_sol = new FCTPsol( solution );
    FCTPsol cur_sol  = new FCTPsol( solution ); 

    // Do the actual ILS:      
    do 
    {
      iter++;
      // Display objective value before the local search
      if ( display ) System.out.format("%4d%17.2f",iter,solution.totalCost );

      // Improve solution using local search 
      if (FCTPparam.impMethod==FCTPparam.ILS_RTR ) RTR_proc( ); else LocalSearch();
        
      // The solution obtained after local search is "accepted", that is will
      // take the role of the "current" solution", if it improves this solution. 
      boolean accept = ( solution.totalCost < cur_sol.totalCost );
      
      // Check if new overall best solution has been detected
      if ( solution.totalCost < best_sol.totalCost )
      {
        best_sol.Overwrite( solution );
        num_fail = 0;
      }
      else 
        num_fail++;
     
      // Display objective value after local search and overall best value
      if ( display ) System.out.format("%16.2f%10.2f%n",solution.totalCost,best_sol.totalCost);        
      
      // Every beta iterations, we reset the "current" solution to the best one. 
      if ( iter % beta == 0 )
      {
        accept = false;  
        cur_sol.Overwrite( best_sol );
      }
      
      // If solution is accepted, then overwrite "current solution". Otherwise,
      // overwrite the actual solution with the "current solution". 
      if ( accept ) 
        cur_sol.Overwrite( solution); 
      else
        solution.Overwrite( cur_sol );
      
      // Apply a random kick to the solution stored in object "solution", which at this
      // point should equal the actual solution stored in object "solution".
      Kicksolution( (m+n-1)/17); //

    } while ( num_fail < FCTPparam.max_no_imp  );

    // We're ready with the ILS. Now set the library's internal solution to 
    // the best one found above. 
    solution.Overwrite(best_sol);
    iterCount = iter;
    FCTPparam.screen_on = display;
    FCTPparam.max_iter = miter;
  }

  public void ILS(int max_reps)
  {

    // Initialise parameter controlling when to reset the current solution
    int beta = ( m+n-1 )/10;
    if ( beta < 5 ) beta = 5;

    // Initialise iteration counters
    int num_fail = 0;
    int iter = 0;

    // Display something on the screen, so that we can see that something happens
    if ( FCTPparam.screen_on )
    {
      System.out.println("=============== DOING ILS ================");
      System.out.println("ITER  OBJ (before LS)  OBJ (after LS)  BEST_OBJ");
    }

    // Save the initial solution as both the "current" and incumbent solution
    FCTPsol best_sol = new FCTPsol( solution );
    FCTPsol cur_sol  = new FCTPsol( solution );

    // Do the actual ILS:
    do
    {
      iter++;
      // Display objective value before the local search
      if ( FCTPparam.screen_on ) System.out.format("%4d%17.2f",iter,solution.totalCost );

      // Improve solution using local search
      LocalSearch( );

      for (int arc = 0; arc < narcs; arc++) {
        if (solution.flow[arc] > 0) {
          edge_counts[arc]++;
        }
      }


      // The solution obtained after local search is "accepted", that is will
      // take the role of the "current" solution", if it improves this solution.
      boolean accept = ( solution.totalCost < cur_sol.totalCost );

      // Check if new overall best solution has been detected
      if ( solution.totalCost < best_sol.totalCost )
      {
        best_sol.Overwrite( solution );
        num_fail = 0;
      }
      else
        num_fail++;

      // Display objective value after local search and overall best value
      if ( FCTPparam.screen_on ) System.out.format("%16.2f%10.2f%n",solution.totalCost,best_sol.totalCost);

      // Every beta iterations, we reset the "current" solution to the best one.
      if ( iter % beta == 0 )
      {
        accept = false;
        cur_sol.Overwrite( best_sol );
      }

      // If solution is accepted, then overwrite "current solution". Otherwise,
      // overwrite the actual solution with the "current solution".
      if ( accept )
        cur_sol.Overwrite( solution);
      else
        solution.Overwrite( cur_sol );

      // Apply a random kick to the solution stored in object "solution", which at this
      // point should equal the actual solution stored in object "solution".
      Kicksolution( 0 );

    } while ( num_fail < max_reps);

    // We're ready with the ILS. Now set the library's internal solution to
    // the best one found above.
    solution.Overwrite(best_sol);
    iterCount = iter;
  }


  /**
   *  A (random) multi-start local search method that repeatedly constructs an initial basic feasible solution
   *  at random and thereafter applies local search to this initial solution. In the first iteration, however,
   *  local search to the solution generated before by (deterministic) greedy or the LP heuristic is applied.
   */
  public void MSLS() 
  {
    // Store current solution as best solution found so far  
    FCTPsol best_sol = new FCTPsol( solution ); 

    // Say hello
    if ( FCTPparam.screen_on ) 
    {
      System.out.println("========== Multi-Random-Start ============");  
      System.out.println("ITER  OBJ (before LS)  OBJ (after LS)  BEST_OBJ");
    }

    // Iteration counters: number of calls to local search and number of subsequent iterations
    // that passed without finding a new best solution
    int iter = 0;
    int itr = 0;
    do
    {
      iter++;
      // Display objective value before the local search
      if ( FCTPparam.screen_on ) System.out.format("%4d%17.2f",iter,solution.totalCost );
     
      // Apply local search to initial solution
      LocalSearch( );
      itr++;
      
      // Save new best solution if an improved one is detected
      if ( solution.totalCost < best_sol.totalCost ) 
      {
        itr = 0;
        best_sol.Overwrite( solution );
      }
      
      // Display objective value after local search and overall best value
      if ( FCTPparam.screen_on ) System.out.format("%16.2f%10.2f%n",solution.totalCost,best_sol.totalCost);        
   
      // Create the initial solution completely at random
      RandSol( );
    } while ( itr < FCTPparam.max_no_imp );

    // Set solution to best one found above
    solution.Overwrite( best_sol );
  
  }


  /**
   *  Determines initial temperature such that acceptance rate of about "acc_rate"*100% is reached.
   *  
   *  @param num_nb number of non-basic arcs
   *  @param nb_arcs array of indices of the non-basic arcs
   *  @param acc_rate intial acceptance rate (larger 0, smaller 1)
   */
  private double SA_heatUp( int num_nb, int[] nb_arcs, double acc_rate )
  {
    double mean = 0.0; 
    for (int arc=0; arc < num_nb; arc++) mean += Math.max(0.0, -getCostSav(nb_arcs[arc]));
    mean /= num_nb;
    return( -mean/Math.log(acc_rate) );
  }    
    
  /** 
   *  Applies a standard simulated annealing procedure to the FCTP. 
   */
  public void SA() 
  {     
    // "Iterations" will equal total number of transitions 
    int iter=0; 
    
    // Store initial solution as best solution found so far 
    FCTPsol best_sol = new FCTPsol( solution );
    
    // Store array of non-basic arcs
    int[] nb_arcs = new int[narcs];
    int num_nb = 0;
    for (int arc=0; arc < narcs; arc++) if (solution.arc_stat[arc] != BASIC) nb_arcs[num_nb++] = arc;
    
    // Fix initial temperature so that initial acceptance rate is about FCTPparam.ini_acc_rate*100 %
    double temp = SA_heatUp( num_nb, nb_arcs, FCTPparam.ini_acc_rate );
   
    // Number of transitions at initial temperature
    int sample_size = num_nb; 

    // Say hello
    if ( FCTPparam.screen_on ) 
    {
      System.out.println("================ Simulated annealing ===============");
      System.out.println("Temperature  Sample_Size  Acc_rate  Current Obj  Incumbent");
     }
     
    // Main loop  
    boolean goon=false;
    int num_fail = 0;
    do 
    { 
      // Sample at current temperature 
      boolean improve = false;
      int non_degen = num_nb;
      int num_accepted = 0;
      for ( int h=0; (h < sample_size) && (non_degen > 0); h++ ) 
      { 
        // Make a random basic exchange but avoid degenerate ones 
        boolean is_degen;
        double saving;
        int indx;
        do 
        {
          indx  = randgen.nextInt(non_degen);
          saving = getCostSav( nb_arcs[indx] );
          is_degen = isDegenerated( );
          if ( is_degen )
          {
            int arc = nb_arcs[indx];  
	    nb_arcs[indx] = nb_arcs[--non_degen];
            nb_arcs[non_degen] = arc;
          }  
        } while ( ( is_degen ) && ( non_degen > 0 ) );
        boolean accept = ( (saving > 0.0) || ((! is_degen) && ( Math.log(randgen.nextDouble()) < saving/temp )) );
        // Apply the move if accept and record new set of non-basic arcs
        if ( accept ) 
        { 
          iter++;
          num_accepted++;
          RememberMove();
          DoMove();
          nb_arcs[indx] = getLeavingArc();
          non_degen = num_nb;
          if ( solution.totalCost < best_sol.totalCost ) 
          { 
            improve = true;
            best_sol.Overwrite( solution );
          }
        } 
      }
      double acc_rate = (double)num_accepted/(double)sample_size;
      if ( FCTPparam.screen_on ) 
        System.out.format("%11.2f%13d%10.2f%13.2f%11.2f%n",temp,sample_size,acc_rate,solution.totalCost,best_sol.totalCost);        
      if ( improve ) num_fail = 0; else num_fail++;
      // sample_size at next temperature level
      sample_size += Math.max( sample_size*FCTPparam.sample_growth, 1 ); 
      // Adjust the temperature 
      temp *= FCTPparam.sa_beta;
      // Stop if acceptance rate below minimum and no improved solution in recent iterations
      goon = ( ( acc_rate > FCTPparam.min_acc_rate) || (num_fail < FCTPparam.max_no_imp ) );
    } while ( goon );

    // Reset solution to best one found by procedure above and apply deterministic local search
    solution.Overwrite( best_sol );
    LocalSearch( );
    iterCount=iter;
  } 

  /**
   * Applies simulated annealing to the FCTP in a similar way as Osman (1995) did
   * for the generalised assignment problem, that is, by moving through the neighbourhood
   * in an implicitly given order and moving the first accepted neighbouring solution
   */
  public void OsmanSA() 
  {
    // We assume that an initial feasible solution is stored in object "solution" and first apply ordinary local search
    LocalSearch( );
    
    // Store current solution as best one found so far
    FCTPsol best_sol = new FCTPsol( solution );
    
    // Initialise start and end temperature as largest and smallest deterioation 
    // in objective value observed when moving from the current point to a neighbouring solution
    double Tstart  = 0.0;
    double Tfinal  = Double.MAX_VALUE;
    int    num_nb  = 0;
    int[]  nb_arcs = new int[narcs];
    for ( int arc=0; arc < narcs; arc++ ) if (solution.arc_stat[arc] != BASIC )
    {
      nb_arcs[num_nb++] = arc;
      double deterio = -getCostSav( arc );
      if ( deterio > 0.0 ) 
      {
        if (deterio > Tstart ) Tstart = deterio;
        if (deterio < Tfinal ) Tfinal = deterio;
      }	
    }
    
    // Initialise parameter beta of cooling schedule T'=T/(1+beta*T)
    double Tcurr = Tstart;
    double Tbest = Tstart;
    double Treset= Tstart;
    double beta0 = (Tstart-Tfinal)/(Tstart*Tfinal)/(m+n+1);
     
    if ( FCTPparam.screen_on ) 
    {
       System.out.println("=============== Osman style simulated annealing ==================");
       System.out.println("#Accepted  Temperature  Current Obj  Incumbent");
       System.out.format("%9d%13.2f%13.2f%11.2f%n",0,Tcurr,solution.totalCost,best_sol.totalCost);
    }
    
    int num_reset = 0;    
    int iter = 0; // number of temperature adjustments
    do 
    { 
      // Sweep through the neighbourhood. Each time a move is accepted, reduce temperature. 
      // Reset temperature, if no move was accepted.
      int ncheck = num_nb;
      int num_acc = 0;
      while ( ncheck > 0 )
      {
        int arc = randgen.nextInt(ncheck);
        int arcIn = nb_arcs[arc];
        double saving = getCostSav( arcIn );
        boolean accept = ( saving > FCTPparam.tolval );
        if ( ! accept ) accept = ( (saving < -FCTPparam.tolval) && (Math.log(randgen.nextDouble()) < saving/Tcurr ) );
        if ( accept )
        {
          if ( Math.abs(saving) < FCTPparam.tolval ) System.out.println("Hups move of zero saving");   
          RememberMove();
          DoMove();
          nb_arcs[arc] = getLeavingArc();
          if ( solution.totalCost < best_sol.totalCost ) 
	  { 
            best_sol.Overwrite( solution );
	    Tbest = Tcurr;
            num_reset = 0;
	  }
          double beta = beta0/(num_nb+Math.sqrt(++iter)); 
          Tcurr = Tcurr/(1.0+beta*Tcurr);
          ncheck = num_nb;
          num_acc++;
        }
        else
        {
          nb_arcs[arc] = nb_arcs[--ncheck];
          nb_arcs[ncheck] = arcIn;
        }
      }
      // no move has been accepted -> Reanneal and stop if number of reannealings reaches max.
      Treset /= 2.0;
      if ( Treset < Tbest ) Treset = Tbest;
      Tcurr = Treset;
      num_reset++;
      if ( FCTPparam.screen_on )
        System.out.format("%9d%13.2f%13.2f%11.2f%n",num_acc,Tcurr,solution.totalCost,best_sol.totalCost);
    } while ( num_reset < FCTPparam.max_iter );
    
    // Reset libary's solution to best one found above
    solution.Overwrite( best_sol );
    LocalSearch( );
    iterCount=iter;
    
  }

  /**
   *  Creates a new integer array comprising random integers from 0 to dim-1
   * 
   * @param    dim - dimension of the array
   * @return  random permutation of integers 0 ... dim-1
   */
  protected int[] RandOrder( int dim )
  {
    int rnd_array[] = new int[dim];
    
    for ( int i=0; i < dim; i++ ) rnd_array[i]=i;
    while ( dim > 0 ) {
      int i = randgen.nextInt( dim );
      int ii= rnd_array[i];
      rnd_array[i] = rnd_array[--dim];
      rnd_array[dim] = ii;
    }
    return rnd_array;
  }

  /**
   * Application of a a single record to record travel that makes basic exchanges and either
   * applies the first improving one or the best accepted one. Non-basic arcs are scanned by 
   * looping over suppliers and customers; best-moves for non-basic arcs adjacent to the 
   * current supplier node are searched.
   * 
   * @param  record - best objective function value found so far
   *	    
   * @return true if a move has been applied; otherwise false
   */
  protected boolean RTR_travel( double record )
  { 
    int supplier[] = RandOrder(m);
    int customer[] = RandOrder(n);
    boolean move_made = false;
    double deviat = FCTPparam.RTR_percent*record;
        
    for ( int i=0; i < m; i++ )
    {
      double bestsav = -Double.MAX_VALUE;
      for ( int j = 0; j < n; j++ ) 
      {
        int arc = supplier[i]*n + customer[j];
        if ( solution.arc_stat[arc] != BASIC ) // arc is non-basic
        {
          double saving = getCostSav( arc ); 
          if ( saving > bestsav ) 
          {
            RememberMove();
            bestsav = saving;
            // If improving move, leave j-loop, do the move and continue i-loop
            if ( bestsav > FCTPparam.tolval ) break;
          }	 
        }
      }
      // Apply any improving move found. Otherwise the best one if accpetable.
      if ( (bestsav > FCTPparam.tolval) || (solution.totalCost-bestsav < record+deviat) )
      {
        DoMove();
        move_made = true;
      }
    }
    return( move_made );
  }

  protected void RTR_move( double record )
  {
    int supplier[] = RandOrder(m);
    int customer[] = RandOrder(n);
    double deviat = FCTPparam.RTR_percent*record;

    for ( int i=0; i < m; i++ )
    {
      double bestsav = -Double.MAX_VALUE;
      for ( int j = 0; j < n; j++ )
      {
        int arc = supplier[i]*n + customer[j];
        if ( solution.arc_stat[arc] != BASIC ) // arc is non-basic
        {
          double saving = getCostSav( arc );
          if ( saving > bestsav )
          {
            RememberMove();
            bestsav = saving;
            // If improving move, leave j-loop, do the move and continue i-loop
            if ( bestsav > FCTPparam.tolval ) break;
          }
        }
      }
      // Apply any improving move found. Otherwise the best one if accpetable.
      if ( (bestsav > FCTPparam.tolval) || (solution.totalCost-bestsav < record+deviat) )
      {
        DoMove();
      }
    }
  }

  /**
   * Applies a Record-to-Record travel to the FCTP.
   */
  public void RTR_proc( ) 
  {
    boolean screen_flag = FCTPparam.screen_on;
    FCTPparam.screen_on = false;
    
    // Save current solution as incumbent solution
    FCTPsol best_sol = new FCTPsol( solution );
        
    if ( screen_flag ) {
       System.out.println("==================== RTR procedure ===================");
       System.out.println("Iter  Current Objval  Best Objval");
     }

    // Main loop: Repeatedly do local search on an initial solution constructed by RTR
    int num_fail = 0;
    int iter = 0;
    do 
    {
      // Apply up to max_iter single record-to-record travels
      for ( int cnt=0; cnt < FCTPparam.max_iter; cnt++ )
        if ( ! RTR_travel( best_sol.totalCost ) ) break;
      
      // Improve RTR solution using some local search method
      switch ( FCTPparam.impMethod )
      {
        case FCTPparam.RTR_ILS: ILS( ); break;
        case FCTPparam.RTR_VNS: VNS( ); break;
        default: LocalSearch(); break;
      }
      iter++;
      
      // Check if new record is obtained
      num_fail++;
      if ( solution.totalCost < best_sol.totalCost ) 
      {
        num_fail = 0;
        best_sol.Overwrite( solution );
      }
      // Give some output if screen is on
      if ( screen_flag ) 
        System.out.format("%4d%16.2f%13.2f%n",iter,solution.totalCost,best_sol.totalCost );
    } while (num_fail < FCTPparam.max_no_imp );
    
    // Reset solution to best one computed above
    solution.Overwrite( best_sol );
    FCTPparam.screen_on = screen_flag;
    iterCount=iter;
    
  } 
  
  /**
   *  RTR travel for the FCTP as suggested in Jeanne Aslak Andersen's take-home
   *  exercise in the course on "Metaheuristics", Spring 2010
   */
  public void RTR_Jeanne( ) 
  {
    // Get first local optimum
    LocalSearch( );
    
    // Save current solution as incumbent solution
    FCTPsol best_sol = new FCTPsol( solution );
       
    // Obtain acceptable deviation as average cost increase 
    // of a basic exchange at the first local optimal solution
    double deviat = 0.0;
    int num = 0;
    for ( int arc=0; arc < narcs; arc++ ) if ( solution.arc_stat[arc] != BASIC )
    {
      deviat -= getCostSav( arc );
      num++;
    }
    deviat /= num;
 
    if ( FCTPparam.screen_on )
    {
      System.out.println("===========  Jeanne Aslak Andersens RTR ===============");
      System.out.println("ITER  NEW_OBJVAL  CURRENT_OBJ  BEST_OBJVAL");
    }
    
    // Allocate memory for the new solution reached at the current iteration
    FCTPsol new_sol = new FCTPsol( m, n );
    int num_fail = 0;
    int iter = 0;

    // Main loop
    do
    {
      iter++;    
      // Apply a couple of times a random perturbation followed by local search.
      new_sol.totalCost = Double.MAX_VALUE;
      for ( int cnt = 0; cnt < FCTPparam.RTR_ILS_REP; cnt++ )
      {
        Kicksolution( 0 );
        LocalSearch( ); 
        if ( solution.totalCost < new_sol.totalCost ) new_sol.Overwrite( solution );
      }      
      num_fail++;
      // If new solution can be accepted, then let it be the current one
      if ( new_sol.totalCost - best_sol.totalCost < deviat )
      {
        solution.Overwrite( new_sol );
        deviat *= 0.8; 
        // Check if the solution even improves the best one found so far
        if ( new_sol.totalCost < best_sol.totalCost )
        {
          best_sol.Overwrite( new_sol );
          num_fail = 0;
        }
      }
      if ( FCTPparam.screen_on )
        System.out.format("%4d%12.2f%13.2f%13.2f%n",iter,new_sol.totalCost,solution.totalCost,best_sol.totalCost);
    } while ( num_fail < FCTPparam.max_no_imp );

    // Reset the solution to best solution found above
    solution.Overwrite( best_sol );
    iterCount = iter;
    
  }
   
  
  /**
   *  Adjusts the penalties and modifies the fixed and transportation costs to be used in the next 
   *  call to the local search used within the guided local search.
   * 
   *  @param lamb_f penalty weight to be applied for the fixed cost
   *  @param  lamb_c penalty weight to be applied for the transportation cost
   *  @param  rho integer array where rho[arc] is penalty counter of arc "arc"
   *  @param  fc array of original fixed cost
   *  @param  tc array of original transportation cost
   */
  private void GLS_Penalize_Arcs( double lamb_f, double lamb_c, int[] rho, double[] fc, double[] tc ) 
  {   
    // Find set of arcs showing largest "utility"
    double max_util = 0.0;
    List<Integer> arc_lst = new ArrayList();
    for ( int arc=0; arc < narcs; arc++ ) if ( solution.flow[arc] > 0 )
    {
      double util = (fc[arc] + (getCap(arc)-solution.flow[arc])*tc[arc])/(1+rho[arc]);
      if ( util > max_util + FCTPparam.tolval ) 
      { 
        max_util = util;
	arc_lst.clear();
        arc_lst.add(new Integer(arc));
      }
      else if (util > max_util - FCTPparam.tolval ) 
        arc_lst.add(new Integer(arc));
    } 
      
    // Increase costs of arcs showing largest "utility"
    for (Iterator it = arc_lst.iterator(); it.hasNext (); ) 
    {
      int arc = ( (Integer) it.next() ).intValue();
      rho[arc]++;
      fcost[arc] += lamb_f;
      tcost[arc] += lamb_c;
    }
    
  }

  /**
   *  Guided local search for the FCTP
   */
  public void GLS(  )
  {
    // Obtain a first local optimum by calling the local search procedure
    LocalSearch( ); 
    
    // Store solution as the current best one
    FCTPsol best_sol = new FCTPsol( solution );
    
    // Store original fixed cost and unit transportation cost
    double[] fc = new double[narcs];
    double[] tc = new double[narcs];
    System.arraycopy( fcost, 0, fc, 0, narcs );
    System.arraycopy( tcost, 0, tc, 0, narcs );
    
    // Compute the weight lambda of the penalty function 
    int[] rho = new int[narcs]; // penalty counter for each arc 
    Arrays.fill( rho, 0 );
    double lamb_f = 0.0; // penalty weight associated with fixed cost 
    double lamb_c = 0.0; // penalty weight associated with transp. cost
    int cnt = 0;         // #arcs with positive flow
    for ( int arc=0; arc < narcs; arc++ ) if ( solution.flow[arc] > 0 )
    {
      lamb_f += fc[arc];
      lamb_c += tc[arc];
      cnt++;
    }  
    lamb_f *= FCTPparam.gls_alpf/cnt;
    lamb_c *= FCTPparam.gls_alpc/cnt;
    
    if ( FCTPparam.screen_on ) 
    {    
      System.out.println("============== Doing GLS =============");
      System.out.println("Iter  modified obj  current obj  best objval ");
    }
    
    // Main loop: Penalize arcs selected in local optimum and re-apply local search
    int num_fail = 0;
    int iter = 0;
    do
    {
      iter++;        
      // Adjust penalty of attributes (arcs) showing largest "utility"
      GLS_Penalize_Arcs( lamb_f, lamb_c, rho, fc, tc );
      
      // Compute solution's modified cost, apply local search and obtain original cost value
      solution.ComputeCost( fcost, tcost );
      LocalSearch( ); 
      double mod_obj = solution.totalCost;
      double cur_obj = solution.returnCost( fc, tc );
      
      // Check if new improved solution has been found
      num_fail++;
      if ( cur_obj < best_sol.totalCost ) 
      {
        num_fail = 0;
        best_sol.Overwrite( solution ); 
        best_sol.totalCost = cur_obj;
      }        
      if ( FCTPparam.screen_on ) 
        System.out.format("%4d%14.2f%13.2f%13.2f%n",iter,mod_obj,cur_obj,best_sol.totalCost);
      if ( iter % FCTPparam.max_iter == 0 ) 
      {
        // Reset the penalties if no improve found after max_iter subsequent iterations
        System.arraycopy( fc, 0, fcost, 0, narcs );
        System.arraycopy( tc, 0, tcost, 0, narcs );
        Arrays.fill( rho, 0 );
        solution.ComputeCost( fcost, tcost );
        LocalSearch();
      } 
           
    } while ( num_fail < FCTPparam.max_no_imp );
    
    // Set solution to best one found above, reset costs to original values
    solution.Overwrite( best_sol ); 
    System.arraycopy( fc, 0, fcost, 0, narcs );
    System.arraycopy( tc, 0, tcost, 0, narcs );
    iterCount = iter;
    
  }  

  /**
   * Variable neighbourhood search for the FCTP
   */
  public void VNS()
  {    
    boolean display = FCTPparam.screen_on;
    FCTPparam.screen_on = false;
    int miter = FCTPparam.max_iter;
    FCTPparam.max_iter = 10; // used by RTR_travel if VNS_RTR is applied
    
    // Ensure to start with a local optimal solution
    if ( FCTPparam.impMethod==FCTPparam.VNS_RTR ) RTR_proc(); else LocalSearch( );
    
    // Save current solution and create a copy keeping the best solution
    FCTPsol cur_sol = new FCTPsol( solution );
    FCTPsol bst_sol = new FCTPsol( solution );
    
    if ( display ) 
    {
      System.out.println("=========================== VNS ==============================");
      System.out.println("Iter  Obj (before LS)  Obj (after LS)  Cur_ObjVal  Best_ObjVal");
    }

    // Initialise iteration counter etc.
    int kmin = 2;
    int kmax = (m+n-1);
    int k = kmin;
    int no_imp = 0;
    int iter = 0;

    do 
    {
      iter++;
      // Apply up to k random basic exchanges to solution "solution"
      Kicksolution( k );
      if ( display ) System.out.format("%4d%16.2f",iter,solution.totalCost);
      // Improve resulting solution using local search or RTR
      if ( FCTPparam.impMethod==FCTPparam.VNS_RTR ) RTR_proc(); else LocalSearch( );
      double ls_obj = solution.totalCost;
         
      // Check if current or even best solution is improved
      no_imp++;
      k++;
      if ( solution.totalCost < cur_sol.totalCost ) 
      { 
        // new solution "solution" is accepted  
        k = kmin;
        cur_sol.Overwrite( solution );
        if ( solution.totalCost < bst_sol.totalCost ) 
        { // new incumbent solution 
          bst_sol.Overwrite( solution );
          no_imp = 0;
        } 
      }
      else // solution is rejected. Set it back to current solution
        solution.Overwrite( cur_sol );
      if ( k > kmax ) k = kmin;
      if ( display ) 
        System.out.format("%16.2f%12.2f%13.2f%n",ls_obj,cur_sol.totalCost,bst_sol.totalCost);
    } while (no_imp < FCTPparam.max_no_imp);

    /* Set solution to the best one found above */
    solution.Overwrite( bst_sol );
    iterCount = iter;
    FCTPparam.screen_on = display;
    FCTPparam.max_iter = miter;
    
  }

  /**
   * GRASP for the FCTP
   */
  public void Grasp()
  {
    FCTPsol best_sol = new FCTPsol( m, n );
    best_sol.totalCost = Double.MAX_VALUE;
    double alp_min  = 0.05;
    double alp_max  = 0.5;

    if ( FCTPparam.screen_on ) {
      System.out.println("======================== DOING GRASP ==========================");
      System.out.println("ITER  ALPHA  OBJVAL (before LS)  OBJVAL (after LS)  BEST_OBJVAL");
    }

    for ( int iter=1; iter <= FCTPparam.max_iter; iter++ ) {
      // Generate random parameter alpha used in RCL
      double alpha = alp_min + randgen.nextDouble()*(alp_max-alp_min);

      // Apply randomized greedy procedure
      RandGreedy( alpha );
      double greedy_obj = solution.totalCost;

      // Improve solution using local search
      LocalSearch( );
      double cur_obj = solution.totalCost;

      // Check if incumbent solution is improved
      if ( cur_obj < best_sol.totalCost ) best_sol.Overwrite( solution );
      if ( FCTPparam.screen_on )
        System.out.format("%4d%7.2f%20.2f%19.2f%13.2f%n",iter,alpha,greedy_obj,cur_obj,best_sol.totalCost);
    }
    solution.Overwrite( best_sol );
    iterCount = FCTPparam.max_iter;

  }
  
    
  /**
   * Basic tabu search procedure for solving the FCTP
   */
  public void TS()
  {
    // Guarantee that the initial solution is a local optimum
    LocalSearch();

    // Save current solution as the best one found so far
    FCTPsol bestSol = new FCTPsol( solution );
    double cur_obj = solution.totalCost;

    // Initialize the tabu list as well as the objective value matrix z(i,j)
    // which is used for regional aspiration
    int[] tlist = new int[narcs];
    double[] zval = new double[narcs];
    Arrays.fill( zval, cur_obj );
    Arrays.fill( tlist, 0 );
    
    if ( FCTPparam.screen_on ) {
      System.out.println("================= TABU SEARCH ==================");
      System.out.println("Iter  #admissible  Cur_ObjVal  Best_ObjVal");
      System.out.format("%4d  %11d  %10.2f  %11.2f%n",0,0,cur_obj,cur_obj);
    }

    int itr = 0;
    int iter= 0;    
    do {
      iter++;
      double bstsav = -Double.MAX_VALUE;
      int num_moves = 0;
      int inArc = -1;
      int outArc = -1; 
      for ( int arcIn = 0; arcIn < narcs; arcIn++ ) if ( solution.arc_stat[arcIn] != BASIC )
      {
        double cstsav = getCostSav( arcIn );
        int arcOut = getLeavingArc();
        boolean tabu = ( ( tlist[arcIn] > iter ) || ( tlist[arcOut] > iter ) );
        boolean admissible = ( !tabu || ( cur_obj - cstsav + FCTPparam.tolval < bestSol.totalCost ) );
        if ( !admissible ) // Apply regional aspiration
          admissible = ( cur_obj - cstsav + FCTPparam.tolval < Math.min( zval[arcIn], zval[arcOut] ) );
        if ( ( admissible) && ( cstsav > bstsav ) )
        {
          bstsav = cstsav;
          inArc = arcIn;
          outArc = arcOut;
          num_moves++;
          RememberMove();
        }
      }
      if ( num_moves == 0 ) break; // no move is admissable
      zval[inArc] = cur_obj;
      zval[outArc] = cur_obj;
      DoMove();
      cur_obj -= bstsav;
      int tenure_out = randgen.nextInt(3) + 5;
      int tenure_in = randgen.nextInt(4) + 7;
      tlist[inArc] = iter + tenure_out + 1; // time when arc is non-tabu again
      tlist[outArc]= iter + tenure_in  + 1; // time when arc is non-tabu again
      // Check if new incumbent solution has been found
      itr++;
      if ( bestSol.totalCost > cur_obj )
      {
        bestSol.Overwrite( solution );
        itr = 0;
      }
      if ( FCTPparam.screen_on )  
        System.out.format("%4d  %11d  %10.2f  %11.2f%n",iter,num_moves,cur_obj,bestSol.totalCost);
    } while ( itr < FCTPparam.max_no_imp );
    
    // Reset the library's solution
    solution.Overwrite( bestSol );
    iterCount = iter;
  
  }



}
