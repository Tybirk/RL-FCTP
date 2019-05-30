import java.io.*;
import java.util.*;
import FCTPutil.*; // currently contains java implementation of network simplex method

/**
 * Class for manipulating basic solutions to the FCTP
 *
 * @author  Andreas Klose
 * @version 15/03/2018
 */

public class FCTPbas 
{

  /** Constant: An arc is basic if its status equals the constant BASIC (=1) */
  protected final int BASIC = 1;
  
  /** Constant: An arc is non-basic at lower bound if its equals the constant NONBASO (=0) */
  protected final int NONBAS0 = 0;
  
  /** Constant: An arc is non-basic at upper bound if its equals the constant NONBASU (=2) */
  protected final int NONBASU = 2;
  
  /** Constant: Alias name for a non-basic arc at lower bound */
  protected final int NONBAS = 0;
    
  /** Input data: number m of suppliers */
  public int m;

  /** Input data: number n of customers */     
  public int n;

  /** array of customer demands */
  public int[] demand;
  
  /** array of supply quantities */
  public int[] supply;
  
  /** tcost[arc] is unit transportation cost on arc i*n+j from supplier i to customer j */
  public double[] tcost;
  
  /** fcost[arc] is the fixed cost of arc i*n+j from supplier i to customer j */
  public double[] fcost;
  
  /** number of nodes in the bipartite graph */ 
  public int nnodes;
   
  /** number of arcs in the bipartite graph */ 
  public int narcs;
   
  /** arc capacities */
  private int[] cap=null;
   
  /** arc_t[arc] is the "tail", that means node i, of arc "arc=i*n+j" */
  private int[] arc_t; 
  
  /** arc_h[arc] is the "head", that means node m+j, of arc "arc=i*n+j" */
  private int[] arc_h;
  
  /** Object that keeps the current solutions */
  //protected FCTPsol solution;
  public FCTPsol solution;

  /** Object to store solutions in for RL agents */
  public FCTPsol saved_sol;

  /** Object to store best solution in for RL agents */
  public FCTPsol best_solution;
   
  /** random number generator. See Oracle's help center for a description of this class:
      https://docs.oracle.com/javase/7/docs/api/java/util/Random.html */
  protected Random randgen = new Random();

  /**
   * Get the percentage of fixed costs in current solution, used by RL agents
   * @return percentage of fixed costs in current solution
   */
  public double get_fcost_percent(){
    int count = 0;
    for(int i = 0; i < narcs; i++){
      if(solution.flow[i] > 0){
        count += fcost[i];
      }
    }
    return count/solution.totalCost;
  }


  /**
   * Initialize saved_sol and best_sol, used by RL agents
   */
  public void initialize_sols(){
    if(saved_sol == null){
      saved_sol = new FCTPsol(solution);
    }
    if(best_solution == null) {
      best_solution = new FCTPsol(solution);
    }
  }

  /**
   * Store a solution, used by RL agents
   */
  public void save_sol(){
    if(saved_sol == null){
      saved_sol = new FCTPsol(solution);
    }
    else {
      saved_sol.Overwrite(solution);
    }
  }

  /**
   * Check if an arc is basic, used by RL agents
   * @param arc the arc
   * @return Boolean indicating whether the arc is basic
   */
  public boolean isBasic(int arc){
    return solution.arc_stat[arc] == BASIC;
  }

  /**
   * Get all possible solutions reached by a single basic exchange, used by RL agents
   * @return Arraylist of FCTPsols with resulting solutions
   */
  public ArrayList<FCTPsol> get_possible_moves(){
    ArrayList<FCTPsol> sols = new ArrayList<FCTPsol>();
    FCTPsol cur_sol = new FCTPsol(solution);
    for(int arc=0; arc < narcs; arc++){
      if(solution.arc_stat[arc] != BASIC) {
        executeMove(arc);
        sols.add(solution);
        solution.Overwrite(cur_sol);
      }
    }
    return sols;
  }

  /**
   * Generate arraylist of different random integers
   * @param size Maximum size of integer
   * @return Arraylist of random integers
   */
  public ArrayList<Integer> gen_random_ints(int size){
    if(size >= narcs){
      ArrayList<Integer> numbers = new ArrayList<Integer>(narcs);
      for (int i = 0; i < narcs; i++)
      {
        if(solution.arc_stat[i] != BASIC) {
          numbers.add(i);
        }
      }
      return numbers;
    }
    Random rng = new Random();
    Set<Integer> generated = new LinkedHashSet<Integer>();
    while (generated.size() < Math.min(size, narcs -(n+m-1)))
    {
      Integer next = rng.nextInt(narcs);
      // As we're adding to a set, this will automatically do a containment check
      if(solution.arc_stat[next] != BASIC) {
        generated.add(next);
      }
    }
    return new ArrayList<Integer>(generated);
  }

  /**
   * A forward look to what happens if the arcs from the list moves are introduces into the basis, used by RL agents
   * @param moves Arcs to try to introduce into the basis
   * @return Resulting solutions by applying basic exchanges corresponding to the arcs in moves
   */
  public ArrayList<FCTPsol> get_subset_of_moves(ArrayList<Integer> moves){
    ArrayList<FCTPsol> sols = new ArrayList<FCTPsol>();
    FCTPsol cur_sol = new FCTPsol(solution);
    for(int arc : moves){
      if(solution.arc_stat[arc] != BASIC) {
        executeMove(arc);
        FCTPsol the_sol = new FCTPsol(solution);
        sols.add(the_sol);
        solution.Overwrite(cur_sol);
      }
    }
    return sols;
  }


  /**
   * Restore saved solution, use by RL agents
   */
  public void restore_sol(){
    solution.Overwrite(saved_sol);
  }


  /**
   * Update best solution, used by RL agents
   */
  public void update_best_sol(){
    if(best_solution == null) {
       best_solution = new FCTPsol(solution);
    }
    if(solution.totalCost < best_solution.totalCost){
      best_solution.Overwrite(solution);
    }
  }

  /**
   * Allocates the memory required for the data, initializes arc data
   *
   * @param makeMem equals true if memory for keeping supply, demand and cost data has to be allocated
   */
  private void allocMem( boolean makeMem )
  {
    nnodes = m+n;
    narcs  = m*n;
    arc_h  = new int[narcs];
    arc_t  = new int[narcs];  
    for ( int i=0; i < m; i++ ) for ( int j=0; j < n; j++ )
    {        
      int arc = i*n+j;
      arc_t[arc] = i;   // supply node i = arc/n of the arc 
      arc_h[arc] = m+j; // demand node j = arc%n + m of the arc
    }    

    // Create the solution object
    solution = new FCTPsol( m, n );
    
    if ( makeMem )
    {
      supply = new int[m];
      demand = new int[n];
      tcost  = new double[narcs];
      fcost  = new double[narcs];
    }  
  }

  /**
   * Constructor that reads data from a given file
   * 
   *  @param fname full path to input file
   */
  public FCTPbas( String fname ) throws Exception 
  {
    try 
    {
       Scanner inFile = new Scanner ( new File ( fname ) );
       inFile.useLocale(Locale.US);
       m = inFile.nextInt(); 
       n = inFile.nextInt();
       allocMem( true );
       for ( int i=0; i < m; i++ ) supply[i] = inFile.nextInt();
       for ( int j=0; j < n; j++ ) demand[j] = inFile.nextInt();
       for ( int arc=0; arc < narcs; arc++ ) tcost[arc] = inFile.nextDouble();
       for ( int arc=0; arc < narcs; arc++ ) fcost[arc] = inFile.nextDouble();
    }
    catch ( Exception exc )
    {
      System.out.println("Error when reading data: "+exc.getMessage());
    }  
  }

  /**
   * Constructor that takes data as parameters. 
   * 
   *  @param mm       number of suppliers
   *  @param nn       number of customers
   *  @param s        integer array of mm supply quantities
   *  @param d        integer array of nn demand quantities
   *  @param tc       tc[arc] is for arc=i*nn+j the unit transporation cost from supplier i to customer j
   *  @param fc       fc[arc] is for arc=i*nn+j the fixed cost on arc from supplier i to customer 
   *  @param copyDat  if true the data arrays are copied; otherwise just a pointer reference is set
   */
  public FCTPbas( int mm, int nn, int[] s, int[] d, double[] tc, double[]fc, boolean copyDat ) 
  {
    m = mm; 
    n = nn;
    allocMem( copyDat );
    if ( copyDat )
    {
      for ( int i=0; i < m; i++ ) supply[i] = s[i];
      for ( int j=0; j < j; j++ ) demand[j] = d[j];
      for ( int arc=0; arc < narcs; arc++ ) tcost[arc] = tc[arc];
      for ( int arc=0; arc < narcs; arc++ ) fcost[arc] = fc[arc];
    }
    else
    {
      supply = s;
      demand = d;
      tcost  = tc;
      fcost  = fc;
    }  
  }

  /**
   *  Returns the number/identifier of the tree containing node "node" 
   *  
   *  @param node   node for which the (sub-)tree comprising the node is to be found
   *  @param father father[node] is the father node of node in the current forest of trees
   *  
   *  @return id/number of the root node of the tree that contains "node"
   */
  private int FindTree ( int node, int[] father ) 
  {
    int root=node, pred;
    
    // Tree is identified by id of its root node 
    while ( father[root] >= 0 ) root = father[root];

    // Identify nodes of the tree by assigning them to the same root 
    while ( father[node] >= 0 ) 
    {
      pred = father[node];
      father[node] = root;
      node = pred;
    }
    
    return( root );
  
  }

  /**
   *  Merges to trees identified by their root nodes t1 and t2
   *  
   *  @param t1      root node of first tree 
   *  @param t2      root node of second tree 
   *  @param size    integer array where size[t] is the size of tree t
   *  @param father  integer array where father[n] is node n's father node
   */
  private void MergeTrees( int t1, int t2, int[] size, int[] father ) 
  {
    if ( size[t1] < size[t2] ) 
    {
      int tmp = t1;
      t1 = t2;
      t2 = tmp;
    }  
    size[t1] += size[t2];
    father[t2]= t1;
  }

  /**
   *  Uses the values currently stored in the array "flows" and tries to derive a 
   *  a basis corresponding to this solutiom. The procedure proceeds similar to 
   *  Kruskal's method for constructing a minimum spanning tree. Initially we have 
   *  a forest consisting of trees that just consists of single nodes. The true 
   *  basic edges (positive flow smaller than capacity) are added as long as this 
   *  does not result in a cycle (otherwise an error is returned). If this way, a 
   *  minimum spanning tree is obtained, the procedure terminates. Otherwise, 
   *  nonbasic arcs are added to the forest until a tree is obtained.
   *  
   *  @return true if successful
   */
  public boolean setBasis ( )
  { 
    int  ntrees=nnodes;    
    int[] size = new int[ntrees];
    int[] father = new int[ntrees];

    // Initialize the forest to consist of single unconnected nodes
    for ( int node=0; node < ntrees; node++ ) 
    { 
      size[node]=1;    // subtree has just one node
      father[node]=-1; // each node is root of its subtree 
    }
    
    // Add "true" basic arcs to the forest as long as this does not result in a cycle
    boolean cycle = false;
    int numbv = 0;
    for ( int arc=0; arc < narcs; arc++) 
    {
      if ( solution.flow[arc] == 0 )
        solution.arc_stat[arc] = NONBAS0;
      else if ( (cap != null) && (solution.flow[arc] == cap[arc]) )
        solution.arc_stat[arc] = NONBASU;
      else  
      {
        cycle = ( (numbv++) == nnodes );
        if ( ! (cycle) ) 
        {
          ntrees--;
          solution.arc_stat[arc] = BASIC;  
          int itree = FindTree( arc_t[arc], father );
          int jtree = FindTree( arc_h[arc], father );
          cycle = (itree == jtree );
          if ( !( cycle ) ) MergeTrees( itree, jtree, size, father );
        }
        if ( cycle ) return ( false ); // solution is not basic
      }  
    }
     
    if ( ntrees > 1 )
    {
      // Graph is still not connected, hence add non-basic arcs to the basis
      // (first those at upper, then those at lower bound)   
      int[] NBstat = new int[2];
      NBstat[0] = NONBASU;
      NBstat[1] = NONBAS0;
      for ( int i=0; i < 2; i++ ) for ( int arc=0; (arc < narcs) && (ntrees > 1); arc++ )
      {
        if ( solution.arc_stat[arc] == NBstat[i] ) 
        {
          int itree = FindTree( arc_t[arc], father );
          int jtree = FindTree( arc_h[arc], father );
          if ( itree != jtree ) // adding the non-basic arc does not create a cycle
          {
            ntrees--;
            numbv++;
            solution.arc_stat[arc] = BASIC;
            MergeTrees( itree, jtree, size, father );	
          }
        }
      }  
    }  
   
    if ( ntrees > 1 ) return ( false ); // hups: solution seems not to be basic

    // Set the predecessor of each node in the basis tree. 
    // Do this by exploring the set of arcs incident to each node 
    // and adding and removing nodes to and from a queue	 
    boolean[] explored = new boolean[nnodes];
    int[] queue=size;
    int[] depth=father;
    	 
    solution.tree_p[0] = -1;  
    depth[0] = 0;
    int nqueue = 0;
    for ( int node=0; node < nnodes; node++ ) explored[node]=false;
    queue[nqueue++] = 0;
    while ( nqueue > 0 ) 
    {
      int node = queue[--nqueue];
      if ( node < m ) // node is a supplier
      { 
        int i = m;  
        for ( int j=0; j < n; j++, i++ ) 
        {
          if ( ! ( explored[i] ) )
          {    
            if ( solution.arc_stat[node*n + j] == BASIC ) 
            {
              solution.tree_p[i] = node;
              depth[i] = depth[node] + 1;
              queue[nqueue++] = i;
            }
          }
        }  
      } 
      else // node is a customer
      { 
        int j = node - m;  
        for ( int i=0; i < m; i++ ) 
        {
          if ( ! ( explored[i] ) )
          {
            if ( solution.arc_stat[i*n + j] == BASIC ) 
            {  
              solution.tree_p[i] = node;
              depth[i] = depth[node] + 1;
              queue[nqueue++] = i;
             }
          }
        }
      }
      explored[node]=true;
    }   
	
    // For each node n in the tree, find the number of nodes in the subtree rooted at n. 
    // Do this level for level. Starting with the nodes at lowest level.
    int maxlevel = 0;
    for ( int node=0; node < nnodes; node++ ) 
    {
      if ( depth[node] > maxlevel ) maxlevel = depth[node];
      solution.tree_t[node] = 0;
    }  
    for ( int level=maxlevel; level > 0; level-- ) 
    {
      for ( int node=0; node < nnodes; node++ ) 
      {
        if ( depth[node]==level ) 
          solution.tree_t[solution.tree_p[node]] += (++solution.tree_t[node]);
      }  
    }
    solution.tree_t[0] = nnodes;
  
    return( true );
  
  }

  /**
   *  Returns the unit transport cost on arc "arc"
   *  
   *  @param arc number of the arc whose unit cost should be returned
   */
  public double gettcost( int arc ) { return( tcost[arc] );}

  /**
   *  Returns the unit transport cost on arc from supplier i to customer j
   *  
   *  @param i index of the supplier ( 0 <= i < m )
   *  @param j index of the customer ( 0 <= j < n )
   */
  public double gettcost( int i, int j ) { return( tcost[i*n+j] );}

  /**
   *  Returns the fixed cost on arc "arc"
   *  
   *  @param arc number of the arc whose unit cost should be returned
   */
  public double getfcost( int arc ) { return( fcost[arc] );}

  /**
   *  Returns the fixed cost on arc from supplier i to customer j
   *  
   *  @param i index of the supplier ( 0 <= i < m )
   *  @param j index of the customer ( 0 <= j < n )
   */
  public double getfcost( int i, int j ) { return( fcost[i*n+j] );}

  /**
   *  Returns the capacity on arc "arc"
   *  
   *  @param arc number of the arc whose capacity should be returned
   */
  public int getCap( int arc )
  {
    int carc = Math.min( supply[arc/n], demand[arc%n] );  
    if ( cap == null ) return( carc );
    if ( carc < cap[arc] ) return( carc );
    return( cap[arc] );    
  }

  /**
   *  Returns the capacity on arc from supplier i to customer j
   *  
   *  @param i index of the supplier 0 <= i < m
   *  @param j index of the customer 0 <= j < n
   */
  public int getCap( int i, int j )
  {
    return( getCap( i*n + j ) );  
  }

  /**
   *  Returns the current flow on the arc "arc"
   *  
   *  @param arc index of the arc  (0 <= arc < narcs = m*n)
   *  
   *  @return flow in current solution on the arc
   */
  public int getFlow( int arc)
  {
    if ( ( arc >= 0 ) && (arc < narcs ) ) return ( solution.flow[arc] );
    return( 0 );
  }    

  /**
   *  Returns the current flow on the arc from supplier i to customer i
   *  
   *  @param i index of the supplier (i=0, ..., m-1)
   *  @param j index of the customer (j=0; ..., n-1);
   *  
   *  @return flow in current solution on the arc from supplier i to customer j
   */
  public int getFlow( int i, int j )
  {
    return( getFlow( i*n+j ) );
  }    

  /**
   *  Sets up the basis corresponding to the solution stored in the array called "flow"
   *  where flow[arc] is for arc = i*n+j the flow from supplier i to customer j.
   *  
   *  @return true if successful and false if not (solution is not basic!)
   */
  public boolean setBasicSolution( )
  {
    solution.ComputeCost( fcost, tcost );
    return ( setBasis( ) );
  }    

  /**
   *  Sets a solution as basic solution
   *  
   *  @param  arcFlow flows on the arcs in the solution
   *  @return true if successful and false if not (solution is not basic!)
   */
  public boolean setBasicSolution( int[] arcFlow )
  {
    for ( int arc=0; arc < narcs; arc++ ) solution.flow[arc] = arcFlow[arc];
    return( setBasicSolution() );
  }    

  /**
   *  Solves the LP relaxation and uses the resulting solution as
   *  heuristic solution to the FCTP (known as Balinki's method)
   *  
   *  @return true if successful and false if error occurred
   */
  public boolean LPheu ( )
  {         
    // Create mcNetflo object;
    mcNetflo LPrelax = new mcNetflo( n, m, supply, demand, tcost, fcost, solution.flow );

    //System.out.println(LPrelax.GetObjVal());
    if ( LPrelax.GetStatus( ) == 0 )
      return( setBasicSolution( ) );
    else
      return ( false );
  }    

//-------------------------------------------------------------------------------------

  /**
   *  class used for storing data belonging to a move=basic exchange
   */
  private class moveData
  {
    /** index of the non-basic arc possibly to be introduced in the basis */
    int in_arc; 
     
    /** status of the ingoing arc */
    int inarc_stat;
    
    /** index of arc that leaves the basis (if negative, the in_arc just switches its status */
    int out_arc; 
    
    /** boolean that is true if outgoing arc will be non-basic at upper bound */
    boolean to_upper;
    
    /** apex node of the cycle that results if in_arc is added to the the basis tree */
    int apex; 
    
    /** change of flow on the arcs of the cycle */
    int flow_chg;
    
    /** Let (i,j) be the entering arc. If the leaving arc lies on the path in 
        the basis tree from i back to the root, then i_path is true */
    boolean i_path;  
    
    /** Fixed cost of last arc selected to leave the basis */
    double FCout;
    
    /** sign=1 if in_arc is a non_basic arc at lower bound. Otherwise sign=-1 */
    int sign;
    
    /** Savings in costs that can be realised by this basis exchange */ 
    double saving; 
    
    /** 
     *  Constructor of object 
     */
    moveData ( int in_arc )
    {
      this.in_arc = in_arc;
      this.inarc_stat = solution.arc_stat[in_arc];
      this.out_arc = -1;
      this.to_upper = false;
      this.apex = -1;
      this.flow_chg = (cap==null) ? Integer.MAX_VALUE : cap[in_arc];
      this.sign = 1;
      if ( solution.arc_stat[in_arc] == NONBASU ) 
      { 
        sign = -1;
        flow_chg = solution.flow[in_arc];
      }
      this.i_path = false;
      this.FCout = -1.0;
      this.saving = 0.0;
    }    
    
    moveData ( moveData clone )
    {
      this.in_arc = clone.in_arc;
      this.inarc_stat = clone.inarc_stat;
      this.out_arc = clone.out_arc;
      this.to_upper = clone.to_upper;
      this.apex = clone.apex;
      this.flow_chg = clone.flow_chg;
      this.sign = clone.sign;
      this.FCout = clone.FCout;
      this.i_path = clone.i_path;
      this.saving = clone.saving;        
    }    
    
  }

//-------------------------------------------------------------------------------------

  /** Simple object used to store data of a basic exchange/move */
  private moveData trialMove; 
  private moveData storedMove;

  /**
   *  Computes cost saving that results if a non-basic arc "arc" is made a basic arc
   *  
   *  @return cost saving that results if the arc "arc=i*n+j" from supplier i to 
   *          customer j is introduced into the basis
   */
  public double getCostSav( int arc )
  {
    if ( solution.arc_stat[arc] == BASIC ) return ( 0.0 );    
    compCostSav( arc );
    return( trialMove.saving );
  }    
      
  /**
   *  Compute cost saving that results if a non-basic arc from supplier i to customer j is made basic
   *  
   *  @return cost saving that results if arc from supplier i to customer j is introduced into the basis
   */
  public double getCostSav( int i, int j )
  {
    int arc = i*n+j;
    if ( ( arc >= 0 ) && (arc < narcs ) ) return ( getCostSav(arc) );
    return ( 0.0 );
  }

  public void executeMove(int arc){
    double cost = getCostSav(arc);
    RememberMove();
    DoMove();
  }

  /**
   *  Stores the data of the basic exchange most recently investigated by the method getCostSav".
   */
  public void RememberMove( )
  {
    storedMove = new moveData( trialMove );  
  }    
  
  /**
   *  Returns true if the basic exchange just investigate by the most recent application of method getCostSav is degenerate
   */
  public boolean isDegenerated( )
  {
    return ( trialMove.flow_chg == 0 );    
  }    

  /**
   *  Returns the index of the basic arc that is going to leave the basis if the basic exchange just investigated by the
   *  most recent application of method getCostSav is performed.
   */
  public int getLeavingArc( )
  {
    return ( trialMove.out_arc );    
  }    

  /**
   *  Checks if the arc (pred_i, i ) becomes the new candidate of the bottleneck arc on the cycle 
   *  
   *  @param sign    positive if the flow on the non-basic arc is at zero and negative if at upper bound
   *  @param pred_i  first end node of the tentative "bottleneck" arc
   *  @param i       second end node of the tentative "bottleneck" arc
   *  
   *  @return        true if the arc becomes the new current bottleneck arc
   */
  private boolean Chkarc( int sign, int pred_i, int i )
  {
    int sn, cn;
    boolean to_inc;
                   
    if ( pred_i < m ) 
    {
      sn = pred_i;
      cn = i-m;
      to_inc = ( sign > 0 );
    }
    else
    {
      sn = i;
      cn = pred_i-m;
      to_inc = ( sign < 0 );
    }

    int arc = sn*n + cn;
    int delta=0;
    boolean bneck=false;

    if ( to_inc ) 
    {
      delta = (cap==null) ? Integer.MAX_VALUE : getCap(arc) - solution.flow[arc];
      bneck = ( delta < trialMove.flow_chg );
    }  
    else 
    {
      delta = solution.flow[arc];
      bneck = ( (delta < trialMove.flow_chg) || 
              ( (delta == trialMove.flow_chg) && (fcost[arc] > trialMove.FCout) ) );
    }            
    if ( bneck ) 
    {                        
      trialMove.flow_chg = delta;
      trialMove.out_arc  = arc;
      trialMove.to_upper = to_inc;
      if ( ! (to_inc) ) trialMove.FCout = fcost[arc];
    }

    return ( bneck );
    
  }	  

  /**
   *  Computes the savings of cost on arc [pred_i,i] if the flow on that arc is
   *  increased by an amount of delta (delta may be negative )
   *  
   *  @param pred_i first node of the arc
   *  @param i      second node of the arc
   *  @delta        increase of the flow on the arc
   *  
   *  @return       cost saving obtained by inreasing the flow by delta
   */
  private double SaveOnArc( int pred_i, int i, int delta ) 
  {
    int nflow, arc;
  
    if ( pred_i < m )
    {
      int sn = pred_i;
      int cn = i-m; 
      arc= sn*n + cn;
      nflow = solution.flow[arc] + delta;
    }  
    else  
    {
      int sn  = i;
      int cn  = pred_i-m;
      arc = sn*n + cn;
      nflow = solution.flow[arc] - delta;
    }
    double sav = ( solution.flow[arc] - nflow )*tcost[arc];
    if ( solution.flow[arc] > 0 ) 
    {
      if ( nflow == 0 ) sav += fcost[arc];  
    } 
    else if ( nflow > 0 ) 
      sav -= fcost[arc];
    
    return( sav );

  }

  /**
   *  Computes the cycle and possible cost saving that results if arc "in_arc" is
   *  introduced into the basis
   *  
   *  @params in_arc number of the non-basic arc to be investigated
   */
  private void compCostSav( int in_arc )
  {
    int i = arc_t[in_arc]; // the arc's supply node
    int j = arc_h[in_arc]; // the arc's demand node (index in m, m+1,..,m+n)
  
    // From the end nodes of the non-basic arc "in_arc" move towards the root
    // until the common node (the apex) of these two pathes is found 

    trialMove = new moveData( in_arc );
    while ( i != j ) 
    {
      int sign = trialMove.sign;
      if ( solution.tree_t[i] <= solution.tree_t[j] ) 
      {
        do 
        { 
          int pred_i = solution.tree_p[i];
          if ( Chkarc( sign, pred_i, i ) ) trialMove.i_path = true;
          i = pred_i;	
        } while ( ( solution.tree_t[i] ) < solution.tree_t[j] );
      }
      else if ( solution.tree_t[i] > solution.tree_t[j] ) 
      {   
        do 
        {
          int pred_j = solution.tree_p[j];
          if ( Chkarc( -sign, pred_j, j ) ) trialMove.i_path = false;
          j = pred_j;	
        } while ( solution.tree_t[i] > solution.tree_t[j] );
      }
    } 
    trialMove.apex = i;
    
    // Compute the cost change by going from the apex of the cycle down to
    // the end nodes of the non-basic arc "in_arc" and adjusting flows 
    if ( trialMove.flow_chg > 0 )
    {
      if ( trialMove.sign < 0 ) 
      {
        trialMove.saving = tcost[in_arc]*(trialMove.flow_chg);
        if ( trialMove.flow_chg == solution.flow[in_arc] ) trialMove.saving += fcost[in_arc];
      }
      else {
        trialMove.saving = -fcost[in_arc] - (trialMove.flow_chg)*tcost[in_arc];
      }
      int delta = trialMove.sign * trialMove.flow_chg;
      i = arc_t[in_arc];
      while ( i != trialMove.apex ) 
      {
        int pred_i = solution.tree_p[i];
        trialMove.saving += SaveOnArc( pred_i, i, delta );
        i = pred_i;
      }
      j = arc_h[in_arc];
      while ( j != trialMove.apex ) 
      {
        int pred_j = solution.tree_p[j];
        trialMove.saving += SaveOnArc( pred_j, j, -delta );
        j = pred_j;
      }
    }
    
  }    

  /**
   * Implements the move (that is basic exchange) stored before by a call to method RememberMove().
   */
  public void DoMove( ) 
  {   
    if ( trialMove.out_arc < 0 ) 
    {
      solution.arc_stat[storedMove.in_arc] = ( storedMove.sign > 0 ) ? NONBASU : NONBAS0;
    }  
    else 
    {
      solution.arc_stat[storedMove.in_arc]  = BASIC;
      solution.arc_stat[storedMove.out_arc] = NONBAS0;
      if ( storedMove.to_upper ) 
        solution.arc_stat[storedMove.out_arc] = NONBASU;
    }
  
    solution.totalCost -= storedMove.saving;
    int i = arc_t[storedMove.in_arc];
    double delta = storedMove.flow_chg * storedMove.sign;
    solution.flow[storedMove.in_arc] += delta;
    while ( i != storedMove.apex ) 
    {
      int pred_i = solution.tree_p[i];
      if ( pred_i < m ) 
      {
        int sn = pred_i;
        int cn = i-m;
        int arc = sn*n + cn;
        solution.flow[arc] += delta;
      }  
      else
      {
        int sn = i;
        int cn = pred_i-m;
        int arc = sn*n + cn;
        solution.flow[arc] -= delta;
      }  
      i = pred_i;  
    }

    int j = arc_h[storedMove.in_arc];
    while ( j != storedMove.apex ) 
    {
      int pred_j = solution.tree_p[j];
      if ( pred_j < m ) 
      {
        int sn = pred_j;
        int cn = j-m;
        int arc = sn*n + cn;
        solution.flow[arc] -= delta;
      }  
      else
      {
        int sn = j;
        int cn = pred_j-m;
        int arc = sn*n + cn;
        solution.flow[arc] += delta;
      }  
      j = pred_j;  
    }
     
    // Update the predecessor labels by reversing the predecessor/successor 
    // relationship for nodes on the pivot stem. If [u,v] is the entering arc
    // and [p,q] is the leaving arc and if [p,q] lies on the path from u back
    // to the root (i.e. i_path=1), the pivot stem is the sequence of nodes
    // from u back to p (or q). (The pivot stem might be empty, which is e.g. the 
    // case if [p,q]=[p,v] and [p,q] is on the path from v into direction 
    // of the root.)
    int u = arc_t[storedMove.in_arc];
    int v = arc_h[storedMove.in_arc];
    if ( ! ( storedMove.i_path ) ) 
    {
      u = arc_h[storedMove.in_arc];
      v = arc_t[storedMove.in_arc];
    }    
    
    i = u;
    j = solution.tree_p[i];
    while ( ( i != arc_t[storedMove.out_arc] ) && ( i != arc_h[storedMove.out_arc] ) ) 
    {
      int pred_i = j;
      j = solution.tree_p[pred_i];
      solution.tree_p[pred_i] = i;
      i = pred_i;
    }
    solution.tree_p[u] = v;
    
    // After execution of the loop above, i is the end node of the leaving arc
    // that lies on the pivot stem. The t-label of a node n is the number of
    // nodes in the subtree rooted at n. For the nodes on the path from i back
    // to the apex of the cycle (exluding the apex), the t-label must be reduced
    // by t[i]
    j = (i==arc_t[storedMove.out_arc]) ? arc_h[storedMove.out_arc] : arc_t[storedMove.out_arc];
    while ( j != storedMove.apex )
    {
      solution.tree_t[j] -= solution.tree_t[i];
      j = solution.tree_p[j];
    }    
  
    // Next update the t-labels for nodes on the pivot stem. After performing the above 
    // two loops, i equals the leaving arc's end node that lies on the pivot stem and p is
    // the updated predecessor list. The pivot stem is thus given by:
    //   [i, p[i], p[p[i]], ...,  u]  (Note that the stem is empty if u=i).
    // Let i be the first node on the stem and u be the last. The t-values are updated as follows:
    // 1. For the first node i on the stem: t'[i] = t[i] - t[p[i]]
    // 2. For intermediate nodes j=p[i], ...., l, where u=p[l]: 
    //        t'[j] = t[j] - t[p[j]] + t'[s] with j=p[s]
    // 3. For the last node u on the stepm: t'[u] = t[u] + t'[l], if u=p[l].  
    if ( i != u ) // non-empty stem 
    {
      j = solution.tree_p[i]; 
      solution.tree_t[i] -= solution.tree_t[j];
      while ( j != u ) 
      {
        int pred_j = solution.tree_p[j];  
        solution.tree_t[j] += solution.tree_t[i] - solution.tree_t[pred_j];
        i = j;
        j = pred_j;
      }
      solution.tree_t[j] += solution.tree_t[i];
    }  
  
    // Finally, update the t-labels on the path (in the new tree) from node u back to the
    // apex of the circle. The t-labels have to be increased by t'[u].
    j = solution.tree_p[u];
    while ( j != storedMove.apex ) 
    {
      solution.tree_t[j] += solution.tree_t[u];
      j = solution.tree_p[j];
    }
                   
  }

}
