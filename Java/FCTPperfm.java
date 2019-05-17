import java.io.*;
import java.util.*;
import java.lang.management.*;

/**
 * Class used for keeping track of some statistics for measuring performance of the applied heuristic methods
 * for solving the FCTP.
 *
 * @author  Andreas Klose
 * @version 15/03/2018
 */

public class FCTPperfm
{
  /** total number of "iterations" performed by a heuristic */
  private int iterTot;
    
  /** maximum number of iterations performed in one repition of a stochastic heuristic search */
  private int iterMax;

  /** smallest number of iterations performed in one repition of a stochastic heuristic search */
  private int iterMin;

  /** best objective value obtained in several repitions of a stochastic heuristic search */
  private double minObj;
    
  /** worst objective value obtained in several repitions of a stochastic heuristic search */
  private double maxObj;
    
  /** average objective value obtained in several repitions of a stochastic heuristic search */
  private double meanObj;
    
  /** Clock ticks at start of heuristic procedure (used for measuring computation time) */
  private long startTicks;
    
  /** CPU time in seconds used by heuristic procedure */
  private double CPUtime;
     
  /** Number of nano seconds per second */
  private static final long NANO_PR_SEC=1000000000;
  
  /** Used for CPU time measurement (if supported) */
  private static final ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
        
  /**
   * Constructor for objects of class FCTPperfm
   */
  public FCTPperfm()
  {
    resetStats( );    
  }

  /**
   *  Reset the statistics for performance measurement
   */
  public void resetStats()
  {
    iterTot = 0;
    iterMax = 0;
    iterMin = Integer.MAX_VALUE;
    minObj  = Double.MAX_VALUE;
    maxObj  = 0.0;
    meanObj = 0.0;
    CPUtime = 0.0;
    startTicks = bean.getCurrentThreadCpuTime( );      
  }    
    
  /**
   *   Update performance statistics (iterations, objective values)
   *   
   *   @param numIter is number of iterations performed in last run of the heuristic procedure
   *   @param curObj  objective value of the solution obtained in the procedure's last run
   */
  public void updateStats( int numIter, double curObj )
  {
    CPUtime = (double)( bean.getCurrentThreadCpuTime( ) - startTicks)/(double)NANO_PR_SEC;    
    meanObj += curObj;
    iterTot += numIter;
    if ( numIter > iterMax ) iterMax = numIter;
    if ( numIter < iterMin ) iterMin = numIter;
    if ( curObj > maxObj ) maxObj = curObj;
    if ( curObj < minObj ) minObj = curObj;      
  }    
  
  /**
   * Print solution performance summary to a file
   */
  private void writePerfToFile( ) 
  {
    try
    {
      File file = new File( FCTPparam.outFile );
      boolean newfile = !file.exists();
      if ( newfile ) file.createNewFile();
      FileWriter fileWri = new FileWriter( file.getName(), true );
      PrintWriter out = new PrintWriter( fileWri );
      if ( newfile ) 
        out.println("Problem ID;Method;Min_obj;Max_obj;Average_obj;Total CPU time; CPU time per run; #runs");
                
      String inp_split[] = FCTPparam.inFile.split("/");
      String filename = inp_split[inp_split.length-1];
      String filesplit[] = filename.split("\\.");
      String probname = filesplit[0];
      double  tim_per_run = CPUtime/FCTPparam.num_runs;
      
      if ( iterMax==0 )
        out.println(probname+";"+FCTPparam.getProcName()+";"+minObj+";"+maxObj+";"+meanObj+";"+CPUtime+";"
          +tim_per_run+";"+FCTPparam.num_runs);
      else
      {
        double iterMean = (double)iterTot/(double)FCTPparam.num_runs;
        out.println(probname+";"+FCTPparam.getProcName()+";"+minObj+";"+maxObj+";"+meanObj+";"+CPUtime+";"
          +tim_per_run+";"+FCTPparam.num_runs+";"+iterMin+";"+iterMax+";"+iterMean);
      }    
      out.close();
    } 
    catch (IOException e )
    {
      e.printStackTrace();
    }
  }

  /**
   *   Print information about the performance reached by a heuristic
   *   
   *   @param intialSol need to be true initial solution is to be reported
   */
  public void displayPerformance( boolean initialSol )
  {
    if ( initialSol )  
    {
      if ( FCTPparam.greedy_meas == 0 )   
        System.out.println("Solution obtained with LP heuristic" );
      else
        System.out.println("Solution obtained with Greedy using greedy measure"+FCTPparam.greedy_meas );
    }    
    else 
      System.out.println("Solution obtained with procedure "+FCTPparam.getProcName( ) );
      
    if ( (FCTPparam.num_runs > 1) && ( !initialSol ) )
    {
      System.out.println("Average objective value : "+meanObj );
      System.out.println("Best objective value    : "+minObj );
      System.out.println("Worst objective value   : "+maxObj );
      System.out.println("Total comp. time (secs) : "+CPUtime );
      System.out.println("Comp. time per run      : "+CPUtime/FCTPparam.num_runs);
      if ( iterMax > 0 )
      {
        double iterMean = (double)iterTot/(double)FCTPparam.num_runs;
        System.out.println("Max. no. of iterations: "+iterMax );
        System.out.println("Min. no. of iterations: "+iterMin );
        System.out.println("Aver.no. of iterations: "+iterMean );
      }
    } 
    else 
    { 
      System.out.println("Objective value   : "+minObj );
      System.out.println("Comp. time (secs) : "+CPUtime );
      if (!initialSol ) System.out.println("Number iterations : "+iterTot );
    }
    
    // if ( !initialSol ) if ( FCTPparam.outFile != null ) writePerfToFile ( );
  }
    
}
