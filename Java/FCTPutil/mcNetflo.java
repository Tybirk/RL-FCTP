package FCTPutil;

/**
 *  Hang T. Lau's Java implementation of Kennigton and Helsgaun's network
 *  simplex algorithm for solving minimum cost network flow problems.
 *
 *  @version 11/02/2016
 *  @author  Hang T. Lau
 *  @source  Code taken from the book Hang. T. Lau (2007). A Java Library
 *           of Graph Algorithms and Optimization. Chapter 8,
 *           Chapman & Hall/CRC, Boca Raton, London, New York.
 */
import java.util.*;
import java.io.*;

public class mcNetflo
{
  /** We first declare a class for the arcs of the network */
  public static class mcArcs
  {
    int tail, head; // tail and head of the arc: 0 <= tail,head < nodes
    double cost;    // unit cost of flow
    int lb, ub;     // upper and lower bound on the arc's flow
    int flow;       // (optimal) flow on the arc
    
    /** Constructor of class mcArcs - empty arc */
    public mcArcs( ) 
    {
      tail=head=-1;
      ub=lb=0;
      flow=0;
      cost = 0.0;
    }
    
    /** Constructor of class mcArcs: Creates uncapacitated arc from i to j
        of cost "cst" */
    public mcArcs( int i, int j, double cst ) 
    {
      tail=i;
      head=j;
      ub=-1;
      lb=0;
      flow=0;
      cost = cst;
    }

    /** Constructor of class mcArcs: Creates arc from i to j of upper 
        capacity "cap" and cost "cst" */
    public mcArcs( int i, int j, int cap, double cst ) 
    {
      tail=i;
      head=j;
      ub=cap;
      lb=0;
      flow=0;
      cost = cst;
    }
    
    /** Constructor of class mcArcs: Creates arc from i to j of upper 
        capacity "cap", min flow requirement "low" and cost "cst" */
    public mcArcs( int i, int j, int low, int cap, double cst ) 
    {
      tail=i;
      head=j;
      ub=cap;
      lb=low;
      flow=0;
      cost = cst;
    }
    
    /** Constructor of class mcArcs: Creates a copy of "anyArc" */
    public mcArcs( mcArcs anyArc ) 
    {
      tail=anyArc.tail;
      head=anyArc.head;
      ub=anyArc.ub;
      lb=anyArc.lb;
      flow=anyArc.flow;
      cost = anyArc.cost;
    }

  }
  
  /** number of nodes of the graph, labeled from 1 to nodes */
  private static int nodes;
  
  /** number of edges in the graph */
  private static int edges; 
  
  /** number nonzero flow requirement at the nodes */
  private static int numdemand;
  
  /** 
   * nodedemand[i][1] is the flow requirement at node nodedemand[i][0] for 
   * i = 1, 2, …, nodedemand. The supply at a node will be positive flow 
   * requirement, and demand at a node will be negative flow requirement.
   */
  private static int[][] nodedemand;
  
  /** (nodei[p],nodej[p]) is the p-th  edge in the graph, p=1,2,…,edges. */   
  private static int[] nodei;
  private static int[] nodej;
  
  /** 
   * arccost[i] is the cost on edge i, for i = 1, 2, …, edges; and 
   * arccost[0] is the total cost of the optimal solution.
   */
  private static int[] arccost;
  
  /** 
   * upbound[i] is the upper bound flow requirement of edge i for i = 1, 2, …, edges. 
   * A zero value of upper bound on an edge denotes that edge to be unbounded. 
   * A −1 value of upper bound on an edge denotes that edge to have an upper bound 
   * value of zero.
   */
  private static int[] upbound;
  
  /**
   * lowbound[i] is the lower bound flow requirement of edge i for i = 1, 2, …, edges.
   */
  private static int[] lowbound;
  
  /**
   * arcsol[0][0] is the number of edges that has nonzero flow value in optimal solution; 
   * edge i is from node arcsol[0][i] to node arcsol[1][i], for i = 1, 2, …, arcsol[0][0].
   */
  private static int[][] arcsol;

  /**
   * flowsol[i] is the value of the flow on edge i in the optimal solution, 
   * for i = 1, 2, …, edges
   */
  private static int[] flowsol;
  
  /**
   ** arcid[n1][n2] is the identifier of the arc from node n1 to node n2
   *  0 <= n1,n2 < nodes
   */
  private static int[][] arcid;

  /** Scale value applied to arc costs in order to make them integer valued */  
  private static double scale;
  private static final int iscale = 1000; 
  
  /** Numerical tolerance value */
  private static final double tolval = 1.0E-5;
  
  /** Optimal objective function value as floating point number */
  private static double objval;
  
  /** Status of the optimization result */
  private static int status;
  
  /** Returns the optimal objective function value */
  public double GetObjVal() { return ( objval ); }
  
  /** Returns the status of the optimization */
  public int GetStatus() { return ( status ); } 

//------------------------------------------------------------------------------
  /** 
   * Retrieves the flow solution for a min-cost network flow problem from the data 
   * structure (arcsol,flowsol) and stores it in integer array flow. Returns the 
   * objective function value.
   */
  private static double Getflows( mcArcs[] arcs )
  {
    double totc = 0.0;
    int nparcs = arcsol[0][0]; // arcs with positive flow
    for ( int a=0; a < edges; a++ ) arcs[a].flow = 0;
    for ( int a=1; a <= nparcs; a++ )
    {
      int i=arcsol[0][a]-1;
      int j=arcsol[1][a]-1;
      int arc = arcid[i][j];
      arcs[arc].flow = flowsol[a];
      totc += arcs[arc].cost*arcs[arc].flow;
    }
    return ( totc );
  }  
  
//------------------------------------------------------------------------------
  /** 
   * Retrieves the flow solution for a transportation problem from the data 
   * structure (arcsol,flowsol) and stores it in integer array flow. Returns the 
   * objective function value.
   */
  private static double Getflows( int m, int n, double[] c, int[] flow )
  {
    double totc = 0.0;
    int nparcs = arcsol[0][0]; // arcs with positive flow
    for ( int a=0; a < edges; a++ ) flow[a] = 0;
    for ( int a=1; a <= nparcs; a++ )
    {
      int i=arcsol[0][a]-1;
      int j=arcsol[1][a]-m-1;
      int arc = i*n+j;
      flow[arc] = flowsol[a];
      totc += c[arc]*flow[arc];
    }
    return ( totc );
  }  
  
//------------------------------------------------------------------------------
  /** 
   * Retrieves the flow solution for a fixed-charge transportation problem from 
   * the data structure (arcsol,flowsol) and stores it in integer array flow. 
   * Returns the objective function value (i.e. flow + fixed cost)
   */
  private static double Getflows( int m, int n, double[] c, double[] f, int[] flow )
  {
    double totc = 0.0;
    int nparcs = arcsol[0][0]; // arcs with positive flow
    for ( int a=0; a < edges; a++ ) flow[a] = 0;
    for ( int a=1; a <= nparcs; a++ )
    {
      int i = arcsol[0][a]-1;
      int j = arcsol[1][a]-m-1;
      int arc = i*n+j;
      flow[arc] = flowsol[a];
      totc += c[arc]*flow[arc] + f[arc];
    }
    return ( totc );
  }  

//------------------------------------------------------------------------------
  /**
   * Constructor of class mcNetflo. Solves linear min-cost network flow problem.
   *
   * @param n - number of nodes
   * @param m - number of arcs
   * @param supply - supply[i] is the supply of node i=0,...,n-1
   * @param arcs - array of network arcs i=0, ..., m-1
   */
  public mcNetflo ( int n, int m, int[] supply, mcArcs[] arcs )
  {
    scale = 1.0;
    // Check if there are non-integer flow cost values
    for ( int a=0; a < m; a++ )
    {
      double fc = arcs[a].cost - (int)(arcs[a].cost+tolval);
      if ( fc > tolval )
      {
        scale = iscale;
        break;
      }
    }    
    
    // number of nodes and edges in the graph
    nodes = n;
    edges = m;
    
              
    // Set node requirements
    numdemand = 0;
    nodedemand = new int[nodes+1][2];
    for ( int node=0; node < n; node++ ) if ( supply[node] !=0 )
    {
      nodedemand[++numdemand][0] = node+1;
      nodedemand[numdemand][1] = supply[node];
    }
    
    // Set arc cost, lower and upper bounds on flows
    nodei = new int[edges+1];
    nodej = new int[edges+1];
    arccost = new int[edges+1];
    upbound = new int[edges+1];
    lowbound = new int[edges+1];
    arcid = new int[nodes][nodes];
    for ( int arc=0; arc < m; arc++ )
    {
      int a = arc+1; 
      nodei[a] = arcs[arc].tail+1;
      nodej[a] = arcs[arc].head+1;
      arccost[a] = (int) ( arcs[arc].cost*scale + tolval );
      upbound[a] = arcs[arc].ub;
      lowbound[a] = arcs[arc].lb;
      arcid[arcs[arc].tail][arcs[arc].head] = arc;
    }  
    
    // now use the minCostNetworkFlow method for computing optimal flows
    flowsol= new int[edges+1];
    arcsol = new int[2][edges+1];
    status = minCostNetworkFlow ( );
    
    // Retrieve the optimal solution
    if ( status == 0 ) objval = Getflows( arcs ); 
       
  }

//------------------------------------------------------------------------------
  /**
   * Constructor of class mcNetflo. Solves linear transportation problem of 
   * Hitchcock type
   *
   * @param m - number of suppliers
   * @param n - number of customers 
   * @param s - s[i] is the supply of supplier i=0,..,m-1
   * @param d - d[j] is the demand of customer j=0,..,n-1
   * @param c - c[arc], where arc=i*n+j is the unit flow cost on the arc from i to j
   * @param flow - flow[arc], where arc=i*n+j is the optimal flow on the arc from i to j
   */
  public mcNetflo ( int n, int m, int[] s, int[] d, double[] c, int[] flow )
  {
    nodes = m+n;
    edges = m*n;
    scale = 1.0;
    // Check if there are non-integer flow cost values
    for ( int a=0; a < edges; a++ )
    {
      double fc = c[a] - (int)(c[a]+tolval);
      if ( fc > tolval )
      {
        scale = iscale;
        break;
      }
    }        
              
    // Set node requirements
    numdemand = nodes;
    nodedemand = new int[nodes+1][2];
    for ( int i=0; i < m; i++ )
    {
      int ii = i+1;
      nodedemand[ii][0] = ii;
      nodedemand[ii][1] = s[i];
    }
    for ( int j=0; j < n; j++ )
    {
      int jj=m+j+1;
      nodedemand[jj][0] = jj;
      nodedemand[jj][1] = -d[j];
    }
      
    // Set arc cost, lower and upper bounds on flows
    nodei = new int[edges+1];
    nodej = new int[edges+1];
    arccost = new int[edges+1];
    upbound = new int[edges+1];
    lowbound = new int[edges+1];
    for ( int i=0; i < m; i++ ) for ( int j=0; j < n; j++ )
    {
      int a = i*n+j+1; 
      nodei[a] = i+1;
      nodej[a] = m+j+1;
      arccost[a] = (int) ( c[a-1]*scale + tolval );
      upbound[a] = 0; // means uncapacitated
      lowbound[a] = 0;
    }  
    
    // now use the minCostNetworkFlow method for computing optimal flows
    flowsol= new int[edges+1];
    arcsol = new int[2][edges+1];
    status = minCostNetworkFlow ( );
    
    // Retrieve the objective function value
    if ( status == 0 ) objval = Getflows( m, n, c, flow ); 
       
  }

//------------------------------------------------------------------------------
  /**
   * Constructor of class mcNetflo. Solves linear relaxation of a fixed charge 
   * transportation problem
   *
   * @param m - number of suppliers
   * @param n - number of customers 
   * @param s - s[i] is the supply of supplier i=0,..,m-1
   * @param d - d[j] is the demand of customer j=0,..,n-1
   * @param c - c[arc], where arc=i*n+j is the unit flow cost on the arc from i to j
   * @param f - f[arc], where arc=i*n+j is fixed cost of the arc from i to j
   * @param flow - flow[arc], where arc=i*n+j is the optimal flow on the arc from i to j
   */
  public mcNetflo ( int n, int m, int[] s, int[] d, double[] c, double[] f, int[] flow )
  {
    nodes = m+n;
    edges = m*n;
    scale = iscale;
              
    // Set node requirements
    numdemand = nodes;
    nodedemand = new int[nodes+1][2];
    for ( int i=0; i < m; i++ )
    {
      int ii = i+1;
      nodedemand[ii][0] = ii;
      nodedemand[ii][1] = s[i];
    }
    for ( int j=0; j < n; j++ )
    {
      int jj=m+j+1;
      nodedemand[jj][0] = jj;
      nodedemand[jj][1] = -d[j];
    }
      
    // Set arc cost, lower and upper bounds on flows
    nodei = new int[edges+1];
    nodej = new int[edges+1];
    arccost = new int[edges+1];
    upbound = new int[edges+1];
    lowbound = new int[edges+1];
    for ( int i=0; i < m; i++ ) for ( int j=0; j < n; j++ )
    {
      int cap = Math.min( s[i], d[j] );
      int a = i*n+j; 
      double lcst = c[a] + f[a]/(double)cap;
      nodei[++a] = i+1;
      nodej[a] = m+j+1;
      arccost[a] = (int) ( lcst*scale + tolval );
      upbound[a] = 0; // means uncapacitated
      lowbound[a] = 0;
    }
    
    // Reduce arc costs by the row and column minima. Done for avoiding 
    // to large integers in the arccost (which obviously gives problems
    // for the minCostNetworkFlow() method)
    for ( int i=0; i < m; i++ )
    {
      int imin = Integer.MAX_VALUE;
      for ( int j=0; j < n; j++ ) 
      {
        int a = i*n+j;
        if ( imin > arccost[a] ) imin=arccost[a];
      }  
      if ( imin > 0 )
      {
        for ( int j=0; j < n; j++ )
        {
          int a = i*n+j; 
          arccost[a] -= imin;
        }  
      }    
    }  
    for ( int j=0; j < n; j++ )
    {
      int jmin = Integer.MAX_VALUE;
      for ( int i=0; i < m; i++ )
      {
        int a = i*n+j;
        if ( arccost[a] < jmin ) jmin = arccost[a];
      }
      if ( jmin > 0 )
      {
        for ( int i=0; i < m; i++ )
        {
          int a = i*n+j;
          arccost[a] -= jmin;
        }
      }  
    } 
    
    // now use the minCostNetworkFlow method for computing optimal flows
    flowsol= new int[edges+1];
    arcsol = new int[2][edges+1];
    status = minCostNetworkFlow ( );
    
    // Retrieve the objective function value
    if ( status == 0 ) objval = Getflows( m, n, c, f, flow );
       
  }
   
//------------------------------------------------------------------------------
  
  /**
   *  Function minCostNetworkFlow - Solves min-cost network flow problem
   *
   * @return - the method returns an integer with the following values
   *           0: optimal solution found
   *           1: Infeasible, net required flow is negative
   *           2: Need to increase the size of internal edge-length arrays
   *           3: Error in the input of the arc list, arc cost, and arc flow
   *           4: Infeasible, net required flow imposed by arc flow lower
   *              bounds is negative
   */
  private static int minCostNetworkFlow ( ) 
  {
   int i, j, k, l, m, n, lastslackedge, solarc, temp, tmp, u, v, remain, rate;
   int arcnam, tedges, tedges1, nodes1, nodes2, nzdemand, value, valuez;
   int tail, ratez, tailz, trial, distdiff, olddist, treenodes, iterations;
   int right, point, part, jpart, kpart, spare, sparez, lead, otherend, sedge;
   int orig, load, curedge, p, q, r, vertex1, vertex2, track, spointer, focal;
   int newpr, newlead, artedge, maxint, artedge1, ipart, distlen;
   int after = 0, other = 0, left = 0, newarc = 0, newtail = 0;
   int pred[] = new int[nodes + 2];
   int succ[] = new int[nodes + 2];
   int dist[] = new int[nodes + 2];
   int sptpt[] = new int[nodes + 2];
   int flow[] = new int[nodes + 2];
   int dual[] = new int[nodes + 2];
   int arcnum[] = new int[nodes + 1];
   int head[] = new int[edges * 2];
   int cost[] = new int[edges * 2];
   int room[] = new int[edges * 2];
   int least[] = new int[edges * 2];
   int rim[] = new int[3];
   int ptr[] = new int[3];
   boolean infeasible;
   boolean flowz = false, newprz = false, artarc = false, removelist = false;
   boolean partz = false, ipartout = false, newprnb = false;
   for (p = 0; p <= nodes; p++)
    arcnum[p] = 0;
   maxint = 0;
   for (p = 1; p <= edges; p++) {
    arcnum[nodej[p]]++;
    if (arccost[p] > 0) maxint += arccost[p];
    if (upbound[p] > 0) maxint += upbound[p];
   }
   artedge = 1;
   artedge1 = artedge + 1;
   tedges = (edges * 2) - 2;
   tedges1 = tedges + 1;
   nodes1 = nodes + 1;
   nodes2 = nodes + 2;
   dual[nodes1] = 0;
   for (p = 1; p <= nodes1; p++) {
    pred[p] = 0;
    succ[p] = 0;
    dist[p] = 0;
    sptpt[p] = 0;
    flow[p] = 0;
   }
   head[artedge] = nodes1;
   cost[artedge] = maxint;
   room[artedge] = 0;
   least[artedge] = 0;
   remain = 0;
   nzdemand = 0;
   sedge = 0;
   // initialize supply and demand lists
   succ[nodes1] = nodes1;
   pred[nodes1] = nodes1;
   for (p = 1; p <= numdemand; p++) {
    flow[nodedemand[p][0]] = nodedemand[p][1];
    remain += nodedemand[p][1];
    if (nodedemand[p][1] <= 0) continue;
    nzdemand++;
    dist[nodedemand[p][0]] = nodedemand[p][1];
    succ[nodedemand[p][0]] = succ[nodes1];
    succ[nodes1] = nodedemand[p][0];
   }
   if (remain < 0) return 1;
   for (p = 1; p <= nodes; p++)
    dual[p] = arcnum[p];
   i = 1;
   j = artedge;
   for (p = 1; p <= nodes; p++) {
    i = -i;
    tmp = Math.max(1, dual[p]);
    if (j + tmp > tedges) return 2;
    dual[p] = (i >= 0 ? j + 1 : -(j + 1));
    for (q = 1; q <= tmp; q++) {
     j++;
     head[j] = (i >= 0 ? p : -p);
     cost[j] = 0;
     room[j] = -maxint;
     least[j] = 0;
    }
   }
   // check for valid input data
   sedge = j + 1;
   if (sedge > tedges) return 2;
   head[sedge] = (-i >= 0 ? nodes1 : -nodes1);
   valuez = 0;
   for (p = 1; p <= edges; p++) {
    if ((nodei[p] > nodes) || (nodej[p] > nodes) || (upbound[p] >= maxint))
     return 3;
    if (upbound[p] == 0) upbound[p] = maxint;
    if (upbound[p] < 0) upbound[p] = 0;
    if ((lowbound[p] >= maxint) || (lowbound[p] < 0) ||
     (lowbound[p] > upbound[p]))
     return 3;
    u = dual[nodej[p]];
    v = Math.abs(u);
    temp = (u >= 0 ? nodes1 : -nodes1);
    if ((temp ^ head[v]) <= 0) {
     sedge++;
     tmp = sedge - v;
     r = sedge;
     for (q = 1; q <= tmp; q++) {
      temp = r - 1;
      head[r] = head[temp];
      cost[r] = cost[temp];
      room[r] = room[temp];
      least[r] = least[temp];
      r = temp;
     }
     for (q = nodej[p]; q <= nodes; q++)
      dual[q] += (dual[q] >= 0 ? 1 : -1);
    }
    // insert new edge
    head[v] = (u >= 0 ? nodei[p] : -nodei[p]);
    cost[v] = arccost[p];
    valuez += arccost[p] * lowbound[p];
    room[v] = upbound[p] - lowbound[p];
    least[v] = lowbound[p];
    flow[nodei[p]] -= lowbound[p];
    flow[nodej[p]] += lowbound[p];
    dual[nodej[p]] = (u >= 0 ? v + 1 : -(v + 1));
    sptpt[nodei[p]] = -1;
   }
   i = nodes1;
   k = artedge;
   l = 0;
   sedge--;
   for (p = artedge1; p <= sedge; p++) {
    j = head[p];
    if ((i ^ j) <= 0) {
     i = -i;
     l++;
     dual[l] = k + 1;
    } else
    if (Math.abs(j) == l) continue;
    k++;
    if (k != p) {
     head[k] = head[p];
     cost[k] = cost[p];
     room[k] = room[p];
     least[k] = least[p];
    }
   }
   sedge = k;
   if (sedge + Math.max(1, nzdemand) + 1 > tedges) return 2;
   // add regular slacks
   i = -head[sedge];
   focal = succ[nodes1];
   succ[nodes1] = nodes1;
   if (focal == nodes1) {
    sedge++;
    head[sedge] = (i >= 0 ? nodes1 : -nodes1);
    cost[sedge] = 0;
    room[sedge] = -maxint;
    least[sedge] = 0;
   } else
    do {
     sedge++;
     head[sedge] = (i >= 0 ? focal : -focal);
     cost[sedge] = 0;
     room[sedge] = dist[focal];
     dist[focal] = 0;
     least[sedge] = 0;
     after = succ[focal];
     succ[focal] = 0;
     focal = after;
    } while (focal != nodes1);
   lastslackedge = sedge;
   sedge++;
   head[sedge] = (-i >= 0 ? nodes2 : -nodes2);
   cost[sedge] = maxint;
   room[sedge] = 0;
   least[sedge] = 0;
   // locate sources and sinks
   remain = 0;
   treenodes = 0;
   focal = nodes1;
   for (p = 1; p <= nodes; p++) {
    j = flow[p];
    remain += j;
    if (j == 0) continue;
    if (j < 0) {
     flow[p] = -j;
     right = nodes1;
     do {
      after = pred[right];
      if (flow[after] + j <= 0) break;
      right = after;
     } while (true);
     pred[right] = p;
     pred[p] = after;
     dist[p] = -1;
    } else {
     treenodes++;
     sptpt[p] = -sedge;
     flow[p] = j;
     succ[focal] = p;
     pred[p] = nodes1;
     succ[p] = nodes1;
     dist[p] = 1;
     dual[p] = maxint;
     focal = p;
    }
   }
   if (remain < 0) return 4;
   do {
    // select highest rank demand
    tail = pred[nodes1];
    if (tail == nodes1) break;
    do {
     // set link to artificial
     newarc = artedge;
     newpr = maxint;
     newprz = false;
     flowz = false;
     if (flow[tail] == 0) {
      flowz = true;
      break;
     }
     // look for sources
     trial = dual[tail];
     lead = head[trial];
     other = (lead >= 0 ? nodes1 : -nodes1);
     do {
      if (room[trial] > 0) {
       orig = Math.abs(lead);
       if (dist[orig] == 1) {
        if (sptpt[orig] != artedge) {
         rate = cost[trial];
         if (rate < newpr) {
          if (room[trial] <= flow[tail]) {
           if (flow[orig] >= room[trial]) {
            newarc = -trial;
            newpr = rate;
            if (newpr == 0) {
             newprz = true;
             break;
            }
           }
          } else {
           if (flow[orig] >= flow[tail]) {
            newarc = trial;
            newpr = rate;
            if (newpr == 0) {
             newprz = true;
             break;
            }
           }
          }
         }
        }
       }
      }
      trial++;
      lead = head[trial];
     } while ((lead ^ other) > 0);
     if (!newprz) {
      artarc = false;
      if (newarc == artedge) {
       artarc = true;
       break;
      }
     } else
      newprz = false;
     if (newarc > 0) break;
     newarc = -newarc;
     orig = Math.abs(head[newarc]);
     load = room[newarc];
     // mark unavailable
     room[newarc] = -load;
     // adjust flows
     flow[orig] -= load;
     flow[tail] -= load;
    } while (true);
    if (!flowz) {
     removelist = false;
     if (!artarc) {
      room[newarc] = -room[newarc];
      orig = Math.abs(head[newarc]);
      flow[orig] -= flow[tail];
      k = maxint;
      removelist = true;
     } else {
      // search for transshipment nodes
      artarc = false;
      trial = dual[tail];
      lead = head[trial];
      newprz = false;
      do {
       if (room[trial] > 0) {
        orig = Math.abs(lead);
        // is it linked
        if (dist[orig] == 0) {
         rate = cost[trial];
         if (rate < newpr) {
          newarc = trial;
          newpr = rate;
          if (newpr == 0) {
           newprz = true;
           break;
          }
         }
        }
       }
       trial++;
       lead = head[trial];
      } while ((lead ^ other) > 0);
      artarc = false;
      if (!newprz) {
       if (newarc == artedge)
        artarc = true;
      } else
       newprz = false;
      if (!artarc) {
       orig = Math.abs(head[newarc]);
       if (room[newarc] <= flow[tail]) {
        // get capacity
        load = room[newarc];
        // mark unavailable
        room[newarc] = -load;
        // adjust flows
        flow[orig] = load;
        flow[tail] -= load;
        pred[orig] = tail;
        pred[nodes1] = orig;
        dist[orig] = -1;
        continue;
       }
       // mark unavailable
       room[newarc] = -room[newarc];
       flow[orig] = flow[tail];
       pred[orig] = pred[tail];
       pred[tail] = orig;
       pred[nodes1] = orig;
       succ[orig] = tail;
       sptpt[tail] = newarc;
       dist[orig] = dist[tail] - 1;
       dual[tail] = newpr;
       treenodes++;
       continue;
      } else
       artarc = false;
     }
    }
    flowz = false;
    if (!removelist)
     k = 0;
    else
     removelist = false;
    pred[nodes1] = pred[tail];
    orig = Math.abs(head[newarc]);
    sptpt[tail] = newarc;
    dual[tail] = newpr;
    pred[tail] = orig;
    i = succ[orig];
    succ[orig] = tail;
    j = dist[orig] - dist[tail] + 1;
    focal = orig;
    do {
     // adjust dual variables
     focal = succ[focal];
     l = dist[focal];
     dist[focal] = l + j;
     k -= dual[focal];
     dual[focal] = k;
    } while (l != -1);
    succ[focal] = i;
    treenodes++;
   } while (true);
   // set up the expand tree
   tail = 1;
   trial = artedge1;
   lead = head[trial];
   do {
    if (treenodes == nodes) break;
    tailz = tail;
    newpr = maxint;
    do {
     // search for least cost connectable edge
     otherend = dist[tail];
     other = (lead >= 0 ? nodes1 : -nodes1);
     do {
      if (room[trial] > 0) {
       m = cost[trial];
       if (newpr >= m) {
        orig = Math.abs(lead);
        if (dist[orig] != 0) {
         if (otherend == 0) {
          i = orig;
          j = tail;
          k = m;
          l = trial;
          newpr = m;
         }
        } else {
         if (otherend != 0) {
          i = tail;
          j = orig;
          k = -m;
          l = -trial;
          newpr = m;
         }
        }
       }
      }
      trial++;
      lead = head[trial];
     } while ((lead ^ other) > 0);
     // prepare the next 'tail' group
     tail++;
     if (tail == nodes1) {
      tail = 1;
      trial = artedge1;
      lead = head[trial];
     }
     newprnb = false;
     if (newpr != maxint) {
      newprnb = true;
      break;
     }
    } while (tail != tailz);
    if (!newprnb) {
     for (p = 1; p <= nodes; p++) {
      if (dist[p] != 0) continue;
      // add artificial
      sptpt[p] = artedge;
      flow[p] = 0;
      succ[p] = succ[nodes1];
      succ[nodes1] = p;
      pred[p] = nodes1;
      dist[p] = 1;
      dual[p] = -maxint;
     }
     break;
    }
    newprnb = false;
    sptpt[j] = l;
    pred[j] = i;
    succ[j] = succ[i];
    succ[i] = j;
    dist[j] = dist[i] + 1;
    dual[j] = dual[i] - k;
    newarc = Math.abs(l);
    room[newarc] = -room[newarc];
    treenodes++;
   } while (true);
   for (p = 1; p <= nodes; p++) {
    q = Math.abs(sptpt[p]);
    room[q] = -room[q];
   }
   for (p = 1; p <= sedge; p++)
    if (room[p] + maxint == 0) room[p] = 0;
   room[artedge] = maxint;
   room[sedge] = maxint;
   // initialize price
   tail = 1;
   trial = artedge1;
   lead = head[trial];
   iterations = 0;
   // new iteration
   do {
    iterations++;
    // pricing basic edges
    tailz = tail;
    newpr = 0;
    do {
     ratez = -dual[tail];
     other = (lead >= 0 ? nodes1 : -nodes1);
     do {
      orig = Math.abs(lead);
      rate = dual[orig] + ratez - cost[trial];
      if (room[trial] < 0) rate = -rate;
      if (room[trial] != 0) {
       if (rate > newpr) {
        newarc = trial;
        newpr = rate;
        newtail = tail;
       }
      }
      trial++;
      lead = head[trial];
     } while ((lead ^ other) > 0);
     tail++;
     if (tail == nodes2) {
      tail = 1;
      trial = artedge1;
      lead = head[trial];
     }
     newprz = true;
     if (newpr != 0) {
      newprz = false;
      break;
     }
    } while (tail != tailz);
    if (newprz) {
     for (p = 1; p <= edges; p++)
      flowsol[p] = 0;
     // prepare summary
     infeasible = false;
     value = valuez;
     for (p = 1; p <= nodes; p++) {
      i = Math.abs(sptpt[p]);
      if ((flow[p] != 0) && (cost[i] == maxint)) infeasible = true;
      value += cost[i] * flow[p];
     }
     for (p = 1; p <= lastslackedge; p++)
      if (room[p] < 0) {
       q = -room[p];
       value += cost[p] * q;
      }
     if (infeasible) return 4;
     arccost[0] = value;
     for (p = 1; p <= nodes; p++) {
      q = Math.abs(sptpt[p]);
      room[q] = -flow[p];
     }
     solarc = 0;
     tail = 1;
     trial = artedge1;
     lead = head[trial];
     do {
      other = (lead >= 0 ? nodes1 : -nodes1);
      do {
       load = Math.max(0, -room[trial]) + least[trial];
       if (load != 0) {
        orig = Math.abs(lead);
        solarc++;
        arcsol[0][solarc] = orig;
        arcsol[1][solarc] = tail;
        flowsol[solarc] = load;
       }
       trial++;
       lead = head[trial];
      } while ((lead ^ other) > 0);
      tail++;
     } while (tail != nodes1);
     arcsol[0][0] = solarc;
     return 0;
    }
    // ration run_cmcs
    newlead = Math.abs(head[newarc]);
    part = Math.abs(room[newarc]);
    jpart = 0;
    // cycle search
    ptr[2] = (room[newarc] >= 0 ? tedges1 : -tedges1);
    ptr[1] = -ptr[2];
    rim[1] = newlead;
    rim[2] = newtail;
    distdiff = dist[newlead] - dist[newtail];
    kpart = 1;
    if (distdiff < 0) kpart = 2;
    if (distdiff != 0) {
     right = rim[kpart];
     point = ptr[kpart];
     q = Math.abs(distdiff);
     for (p = 1; p <= q; p++) {
      if ((point ^ sptpt[right]) <= 0) {
       // increase flow
       i = Math.abs(sptpt[right]);
       spare = room[i] - flow[right];
       sparez = -right;
      } else {
       // decrease flow
       spare = flow[right];
       sparez = right;
      }
      if (part > spare) {
       part = spare;
       jpart = sparez;
       partz = false;
       if (part == 0) {
        partz = true;
        break;
       }
      }
      right = pred[right];
     }
     if (!partz) rim[kpart] = right;
    }
    if (!partz) {
     do {
      if (rim[1] == rim[2]) break;
      for (p = 1; p <= 2; p++) {
       right = rim[p];
       if ((ptr[p] ^ sptpt[right]) <= 0) {
        // increase flow
        i = Math.abs(sptpt[right]);
        spare = room[i] - flow[right];
        sparez = -right;
       } else {
        // decrease flow
        spare = flow[right];
        sparez = right;
       }
       if (part > spare) {
        part = spare;
        jpart = sparez;
        kpart = p;
        partz = false;
        if (part == 0) {
         partz = true;
         break;
        }
       }
       rim[p] = pred[right];
      }
     } while (true);
     if (!partz) left = rim[1];
    }
    partz = false;
    if (part != 0) {
     // update flows
     rim[1] = newlead;
     rim[2] = newtail;
     if (jpart != 0) rim[kpart] = Math.abs(jpart);
     for (p = 1; p <= 2; p++) {
      right = rim[p];
      point = (ptr[p] >= 0 ? part : -part);
      do {
       if (right == left) break;
       flow[right] -= point * (sptpt[right] >= 0 ? 1 : -1);
       right = pred[right];
      } while (true);
     }
    }
    if (jpart == 0) {
     room[newarc] = -room[newarc];
     continue;
    }
    ipart = Math.abs(jpart);
    if (jpart <= 0) {
     j = Math.abs(sptpt[ipart]);
     // set old edge to upper bound
     room[j] = -room[j];
    }
    load = part;
    if (room[newarc] <= 0) {
     room[newarc] = -room[newarc];
     load = room[newarc] - load;
     newpr = -newpr;
    }
    if (kpart != 2) {
     vertex1 = newlead;
     vertex2 = newtail;
     curedge = -newarc;
     newpr = -newpr;
    } else {
     vertex1 = newtail;
     vertex2 = newlead;
     curedge = newarc;
    }
    // update tree
    i = vertex1;
    j = pred[i];
    distlen = dist[vertex2] + 1;
    if (part != 0) {
     point = (ptr[kpart] >= 0 ? part : -part);
     do {
      // update dual variable
      dual[i] += newpr;
      n = flow[i];
      flow[i] = load;
      track = (sptpt[i] >= 0 ? 1 : -1);
      spointer = Math.abs(sptpt[i]);
      sptpt[i] = curedge;
      olddist = dist[i];
      distdiff = distlen - olddist;
      dist[i] = distlen;
      focal = i;
      do {
       after = succ[focal];
       if (dist[after] <= olddist) break;
       dist[after] += distdiff;
       dual[after] += newpr;
       focal = after;
      } while (true);
      k = j;
      do {
       l = succ[k];
       if (l == i) break;
       k = l;
      } while (true);
      ipartout = false;
      if (i == ipart) {
       ipartout = true;
       break;
      }
      load = n - point * track;
      curedge = -(track >= 0 ? spointer : -spointer);
      succ[k] = after;
      succ[focal] = j;
      k = i;
      i = j;
      j = pred[j];
      pred[i] = k;
      distlen++;
     } while (true);
    }
    if (!ipartout) {
     do {
      dual[i] += newpr;
      n = flow[i];
      flow[i] = load;
      track = (sptpt[i] >= 0 ? 1 : -1);
      spointer = Math.abs(sptpt[i]);
      sptpt[i] = curedge;
      olddist = dist[i];
      distdiff = distlen - olddist;
      dist[i] = distlen;
      focal = i;
      do {
       after = succ[focal];
       if (dist[after] <= olddist) break;
       dist[after] += distdiff;
       // udpate dual variable
       dual[after] += newpr;
       focal = after;
      } while (true);
      k = j;
      do {
       l = succ[k];
       if (l == i) break;
       k = l;
      } while (true);
      // run_cmcs for leaving edge
      if (i == ipart) break;
      load = n;
      curedge = -(track >= 0 ? spointer : -spointer);
      succ[k] = after;
      succ[focal] = j;
      k = i;
      i = j;
      j = pred[j];
      pred[i] = k;
      distlen++;
     } while (true);
    }
    ipartout = false;
    succ[k] = after;
    succ[focal] = succ[vertex2];
    succ[vertex2] = vertex1;
    pred[vertex1] = vertex2;
   } while (true);
   
  } // end of minCostNetworkFlow
  
//------------------------------------------------------------------------------  

} // end of class mcNetflo
