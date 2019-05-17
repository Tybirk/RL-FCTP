import java.io.*;
import java.util.*;

/**
 * Class to build and manage (basic) solutions to the FCTP
 *
 * @author  Andreas Klose
 * @version 15/03/2018
 */

public class FCTPsol implements Comparable
{  
  /** Integer array of size narcs=n*m. arc_stat[arc] of the arc i*n+j can be
      BASIC or NONBAS (or NONBASU if there are explicit capacities on arcs) */
  public int[] arc_stat;
   
  /** tree_p[node] = predecessor of node p in the basis tree */
  public int tree_p[];

  /** tree_t[node] = number of nodes in subtree of the basis tree rooted at node "node" */
  public int tree_t[];

  /** total cost of a basic solution */
  //protected double totalCost;
  public double totalCost;
   
  /** flow quantities in a basic solution: i.e. flow[arc] is the flow from supplier
      i to customer j on the arc i*n+j */
  public int[] flow;
 
  /** number of supply nodes */
  private int m;
  
  /** number of customer nodes */
  private int n;

  public int[] get_arc_stat(){
    return arc_stat;
  }

  public double get_rel_pos_flow(){
    int count = 0;
    for(int i: flow){
      if(i > 0){
        count++;
      }
    }
    return (n+m-1 - count)/(n+m-1);
  }

  public int num_basic(){
    int count = 0;
    for(int arc : arc_stat){
      if(arc ==1){
        count++;
      }
    }
    return count;
  }


  
  /**
   *  Allocate memory for (additional) fields required to store a solution
   *  
   *  @param num_nodes number of nodes in the transportation graph
   *  @param num_arcs  number of arcs in the transportation graph
   */
  private void allocMem( )
  {
    arc_stat = new int[m*n];
    tree_p = new int[m+n];
    tree_t = new int[m+n];
    flow = new int[m*n];
  }    
  
  /**
    * Constructor that allocates the memory to store a solution for a problem with
    * m suppliers and n customers.
    *  
    *  @param m number of supply nodes
    *  @param n number of demand nodes
    */
  public FCTPsol( int m, int n ) 
  {
    this.m = m;
    this.n = n;
    allocMem(  );  
  }

  /**
   *   Constructor that takes a source "solution object" and copies it to the newly created one
   *   
   *   @param source the solution with which the created solution should be initialized
   */
  public FCTPsol ( FCTPsol source )
  {
    this.m = source.m;
    this.n = source.n;
    allocMem( );
    Overwrite( source );
  }    

  /**
   *  Overwrite "this" solution with another solution
   *  
   *  @param source the solution with which this solution object should be overwritten.
   */
  public void Overwrite ( FCTPsol source )
  {
    if ( ( this.m == source.m ) && (this.n == source.n ) )   
    {
      System.arraycopy( source.arc_stat, 0, arc_stat, 0, arc_stat.length );
      System.arraycopy( source.tree_p, 0, tree_p, 0, tree_p.length );
      System.arraycopy( source.tree_t, 0, tree_t, 0, tree_t.length );
      System.arraycopy( source.flow, 0, flow, 0, flow.length );
      totalCost = source.totalCost;      
    }  
  }
  
  /**
   *  Computes the total cost of the current flow
   *  
   *  @param  fcost   array of fixed cost on arcs 0,..., narcs-1
   *  @param  tcost   array of unit cost on arcs 0,..., narcs-1
   *  @return total cost of the solution stored in array "flow"
   */
  public void ComputeCost( double[] fcost, double[] tcost )
  {
    totalCost = 0.0;
    for ( int arc=0; arc < flow.length; arc++ ) if ( flow[arc] > 0 )
      totalCost += fcost[arc] + tcost[arc]*flow[arc];
  }    

  public double returnCost( double[] fcost, double[] tcost )
  {
    double flowCost = 0.0;
    for ( int arc=0; arc < flow.length; arc++ ) if ( flow[arc] > 0 )
      flowCost += fcost[arc] + tcost[arc]*flow[arc];
    return( flowCost );  
  }    

  /**
   *  Computes the total cost of a transportation solution 
   *  
   *  @param  arcFlow array of flows on arcs arc=0,...,narcs-1
   *  @param  fcost   array of fixed cost on arcs 0,..., narcs-1
   *  @param  tcost   array of unit cost on arcs 0,..., narcs-1
   *  @return total cost of the solution    
   */
  public double ComputeCost( int[] arcFlow, double[] fcost, double tcost[] )
  {
    double flowCost = 0.0;
    for ( int arc=0; arc < arcFlow.length; arc++ ) if ( arcFlow[arc] > 0 )
      flowCost += fcost[arc] + tcost[arc]*arcFlow[arc];
    return( flowCost );
  }    

  /**
   *  Method that prints the flow quantities in a solution to the screen/terminal
   */
  public void printFlows( )
  {
    System.out.println("----------------------------------------------------------");            
    System.out.println("Transportation quantities of total cost = "+totalCost );
    System.out.println("----------------------------------------------------------");            
    System.out.println("i -> j : Flow");
    System.out.println("-------------");
    for ( int arc=0; arc < flow.length; arc++ ) if ( flow[arc] > 0 )
      System.out.println(arc/n+" -> "+arc%n+" : "+flow[arc]);  
    System.out.println("----------------------------------------------------------");      

  }    

  /**
   *  Compares this solution to another solution. This allows sorting of a list of solutions 
   *  according to non-decreasing objective values.
   *
   *  @param o - solution to which this solution is compared
   */
  public int compareTo(Object o) throws ClassCastException 
  {
    FCTPsol benchmark = (FCTPsol)o; 
    if ( this.totalCost > benchmark.totalCost ) return( 1 );
    if ( this.totalCost < benchmark.totalCost ) return(-1 );
    return( 0 );
  }
    
  /**
   * Returns true if this solution equals solution s.
   *
   * @param s - solution to which this solution is compared
   */
  public boolean equalTo( FCTPsol s ) 
  {
    if ( s.flow.length != this.flow.length ) return( false );    
    if ( Math.abs(this.totalCost - s.totalCost ) > FCTPparam.tolval ) return( false );
    return ( Arrays.equals( this.flow, s.flow ) );
  }
 
  /**
   * Returns true if this solution is contained in the array list "Pool" of solutions.
   *
   * @param Pool - list of solutions
   */
  public boolean containedIn( ArrayList Pool ) 
  {
    Iterator itr = Pool.iterator();
    while ( itr.hasNext() )
    {
      if ( equalTo( (FCTPsol) itr.next() ) ) return( true );
    }  
    return( false );  
  }

  /**
   * Returns the distance (L1-norm) between the flow vector of this solution and solution's s flow vector.
   *
   * @param s - solution to which the distance is computed
   */
  public int DistanceTo( FCTPsol s ) 
  {
    if ( s.flow.length != this.flow.length ) return( Integer.MAX_VALUE );  
    int dist = 0;
    for ( int arc=0; arc < s.flow.length; arc++ ) dist += Math.abs(this.flow[arc]- s.flow[arc] );
    return( dist );  
  }

  /**
   * Returns the number of arcs whose status (basic/non-basic) is different in this solution 
   * compared to solution s.
   *
   * @param s - solution to which the distance is computed
   */
  public int basDistTo( FCTPsol s )
  {
    if ( s.arc_stat.length != this.arc_stat.length ) return( Integer.MAX_VALUE );    
    int dist = 0;
    for (int arc=0; arc < s.arc_stat.length; arc++) if (arc_stat[arc] != s.arc_stat[arc]) dist++;
    return( dist );  
  }  

}
