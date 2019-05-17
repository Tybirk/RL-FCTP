import java.util.*;

/**
 * Greedy solution construction methods for the FCTP
 *
 * @author  Andreas Klose
 * @version 17/04/2018
 */
public class FCTPgreedy extends FCTPbas
{
  /**
   * Constructor 
   *    
   * @param fname name (that is full path) of the input data file
   */
  public FCTPgreedy( String fname ) throws Exception
  {
    super( fname );
  }

  /**
   * Constructor that takes data as parameters. All data are copied if copyDat is true.
   * 
   *  @param mm       number of suppliers
   *  @param nn       number of customers
   *  @param s        integer array of mm supply quantities
   *  @param d        integer array of nn demand quantities
   *  @param tc       tc[arc] is for arc=i*nn+j the unit transporation cost from supplier i to customer j
   *  @param fc       fc[arc] is for arc=i*nn+j the fixed cost on arc from supplier i to customer 
   *  @param copyDat  true if data should be copied to new arrays. Otherwise just a reference is set to the data.
   */
  public FCTPgreedy( int mm, int nn, int[] s, int[] d, double[] tc, double[] fc, boolean copyDat ) 
  {
    super( mm, nn, s, d, tc, fc, copyDat );  
  }    

  public double GreedyValue( int i, int j, int rs, int rd )
  {
    double gval = Double.MAX_VALUE;  
    if ( ( rs == 0 ) || ( rd == 0 ) ) return( gval );
    int arc_cap = getCap( i, j );
    int ecap = Math.min( rd, rs );
    if ( arc_cap < ecap ) ecap = arc_cap;
    switch ( FCTPparam.greedy_meas )
    {
      case FCTPparam.GR_LIN_CAP: gval = gettcost(i,j) + getfcost(i,j)/(double)arc_cap; break;
      case FCTPparam.GR_LIN_REMCAP: gval = gettcost(i,j) + getfcost(i,j)/(double)ecap; break;
      case FCTPparam.GR_LIN_TOTC: gval = getfcost(i,j) + gettcost(i,j)*ecap; break;
      default: gval = getfcost(i,j) + gettcost(i,j)*arc_cap; break;
    }  
    return( gval );
  }    




  /** 
   *  Greedy method for constructing feasible solution. The greedy measure specified in 
   *  parameter FCTPparam.greedy_meas is applied.
   */
  
  public void Greedy( ) 
  {  
    // Array of remaining supplies and demands
    int[] r_supply = new int[m];
    int[] r_demand = new int[n];
    System.arraycopy( supply, 0, r_supply, 0, m );
    System.arraycopy( demand, 0, r_demand, 0, n );
   
    // List of suppliers and customers that still have supplies and demand, resp.
    int[] suppliers = new int[m];
    int[] customers = new int[n];
    for ( int i=0; i < m; i++ ) suppliers[i]=i;
    for ( int j=0; j < n; j++ ) customers[j]=j;
    
    // Set all flows on the arcs to zero
    Arrays.fill( solution.flow, 0 );

    // Iteratively put as much transport as possible on a selected arc 
    // until demand and supply is exhausted.

    int mm = m; // remaining number of suppliers showing positive supplies
    int nn = n; // remaining number of customers showing positive demands
    while ( ( mm > 0 ) && ( nn > 0 ) ) 
    {
      double minVal = Double.MAX_VALUE;
      int is = -1; 
      int js = -1;
      // Compute greedy evaluations of arcs 
      for ( int i=0; i < mm; i++ ) 
      {
        int ii = suppliers[i];
        for ( int j=0; j < nn; j++ ) 
        {
          int jj = customers[j]; 
          double greedyVal = GreedyValue( ii, jj, r_supply[ii], r_demand[jj] );
          if ( greedyVal < minVal ) 
          {
            minVal = greedyVal;
            is = i;
            js = j; // Remember "best" supplier/customer pair
          }
        }
      }
      // Flow as much as possible on the selected arc
      int ii = suppliers[is];
      int jj = customers[js];
      int arc = ii*n + jj;
      solution.flow[arc] = Math.min( r_supply[ii], r_demand[jj] );
      r_supply[ii] -= solution.flow[arc];
      r_demand[jj] -= solution.flow[arc];    
      if ( r_supply[ii]==0 ) suppliers[is]  = suppliers[--mm];
      if ( r_demand[jj]==0 ) customers[js] = customers[--nn];
    }

    // Compute cost of the solution and the corresponding basis tree 
    setBasicSolution();

  }


  /**
   * Construct a random initital basic feasbile soluton by selecting in each iteration one arc completely at
   * random and setting as much flow as possible on this arc until all supplies are exhausted and demands met. 
   */ 
  public void RandSol( ) 
  {  
    // Array of remaining supplies and demands
    int[] r_supply = new int[m];
    int[] r_demand = new int[n];
    System.arraycopy( supply, 0, r_supply, 0, m );
    System.arraycopy( demand, 0, r_demand, 0, n );
   
    // List of suppliers and customers that still have supplies and demand, resp.
    int[] supplier = new int[m];
    int[] customer = new int[n];
    for ( int i=0; i < m; i++ ) supplier[i]=i;
    for ( int j=0; j < n; j++ ) customer[j]=j;
    
    // Set all flows on the arcs to zero
    Arrays.fill( solution.flow, 0 );
  
    // Iteratively add arcs until demand and supply is exhausted.
    int mm = m;
    int nn = n;    
    while ( ( mm > 0 ) && ( nn > 0 ) ) 
    {
      // Pick supplier and customer index at random
      int i = randgen.nextInt( mm );
      int ii = supplier[i];
      int j = randgen.nextInt( nn );
      int jj = customer[j];
      int arc = ii*n + jj;
      // Put maximal flow on arc "arc" from ii to jj
      solution.flow[arc] = Math.min( r_supply[ii], r_demand[jj] );
      r_supply[ii] -= solution.flow[arc];
      r_demand[jj] -= solution.flow[arc];    
      if ( r_supply[ii]==0 ) supplier[i]  = supplier[--mm];
      if ( r_demand[jj]==0 ) customer[j] = customer[--nn];
    }
      
    // Compute cost of the solution and set up the corresponding basis tree
    setBasicSolution();

  }

  /**
   * Randomised greedy method to the FCTP 
   *
   * @param double alpha parameter in [0,1] controlling the restricted candidate list:
   *                     alpha=0 => a deterministic greedy is applied;
   *                     alpha=1 => a purely random basic solution is computed
   */
  public void RandGreedy( double alpha ) 
  {  
    // Initialise remaining supply and demand
    int[] r_supply  = new int[m];
    int[] r_demand  = new int[n];
    System.arraycopy( supply, 0, r_supply, 0, m );
    System.arraycopy( demand, 0, r_demand, 0, n );

    // Initialize list of suppliers and customers with positive remaining supply and demand
    int[] suppliers = new int[m];
    int[] customers = new int[n];
    for ( int i=0; i < m; i++ ) suppliers[i]=i;
    for ( int j=0; j < n; j++ ) customers[j]=j;
    
    // Set all flows on the arcs to zero
    Arrays.fill( solution.flow, 0 );

    // Iteratively add arcs until demand and supply is exhausted.
    int mm = m;
    int nn = n;
    ArrayList arc_lst = new ArrayList( );
    while ( ( mm > 0 ) && ( nn > 0 ) ) 
    {
      double min_val = Double.MAX_VALUE;
      double max_val = 0.0;
      // Compute greedy evaluations of arcs 
      for ( int i=0; i < mm; i++ ) 
      {
        int ii = suppliers[i];
        for ( int j=0; j < nn; j++ ) 
        {
          int jj  = customers[j];  
          int arc = i*nn + j;
          double gval =  GreedyValue( ii, jj, r_supply[ii], r_demand[jj] );
          if ( gval < min_val ) min_val = gval;
          if ( gval > max_val ) max_val = gval;
        }
      }
      // Build restricted candidate list 
      arc_lst.clear();
      for ( int i=0; i < mm; i++ ) 
      {
        int ii = suppliers[i];
        for ( int j=0; j < nn; j++ ) 
        {
          int jj = customers[j];
          double gval = GreedyValue( ii, jj, r_supply[ii], r_demand[jj] );
          if ( gval <= min_val + alpha*(max_val - min_val) + FCTPparam.tolval ) 
            arc_lst.add( i*nn+j );
        }
      }
      // Pick an arc randomly from the candidate list
      int indx = randgen.nextInt(arc_lst.size());
      int arc = (Integer) ( arc_lst.get(indx) );
      // Flow as much as possible on the selected arc 
      int i = arc/nn;
      int j = arc % nn;
      int ii = suppliers[i];
      int jj = customers[j];
      int ecap = Math.min( r_supply[ii], r_demand[jj] );
      solution.flow[ii*n+jj] = ecap;
      r_supply[ii] -= ecap;
      r_demand[jj] -= ecap;    
      if ( r_supply[ii]==0 ) suppliers[i] = suppliers[--mm];
      if ( r_demand[jj]==0 ) customers[j] = customers[--nn];
    }

    // Compute cost of the solution and set up the corresponding basis tree
    setBasicSolution();

  }

}
  
