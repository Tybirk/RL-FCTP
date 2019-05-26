import java.io.*;
import java.util.*;

/**
 * FCTPparam - specifies paramters to be applied in the methods for solving FCTP
 *
 * @author  Andreas Klose
 * @version 22/05/2018
 */

public class FCTPparam
{  
  /** Constant: Identifier for parameter "impMethod" */
  public static final int IMPROVEMETHOD = 0;

  /** Constant: Identifier for parameter "greedy_meas " */
  public static final int GREEDYMEAS = 1;

  /** Constant: Identifier for parameter "ls_type" */
  public static final int LSTYPE = 2;

  /** Constant: Identifier/key for parameter "max_iter" */
  public static final int MAXITER = 3;
  
  /** Constant: Identifier for parameter "max_no_imp" */
  public static final int MAXNOIMP = 4;

  /** Constant: Identifier for parameter "gls_alpf" */
  public static final int GLSALPHAFCOST = 5;
 
  /** Constant: Identifier for parameter "gls_alpc" */
  public static final int GLSALPHATCOST = 6;
  
  /** Constant: Identifier for parameter "sa_beta" */
  public static final int SACOOLBETA = 7;
  
  /** Constant: Identifier for parameter "min_acc_rate" */
  public static final int MINACCRATE = 8;
  
  /** Constant: Identifier for parameter "ini_acc_rate" */
  public static final int INIACCRATE = 9;

  /** Constant: Identifier for parameter "SA_sample_growth" */
  public static final int SAMPLEGROWTH = 10;

  /** Constant: Identifier for parameter "num_runs" */
  public static final int NUMRUNS = 11;
  
  /** Constant: Identifier for parameter "do_restart" */
  public static final int DORESTART = 12;
  
  /** Constant: Identifier for parameter "whatout" */
  public static final int WHATOUT = 13;

  /** Constant: Identifier for parameter "screen_on" */
  public static final int SCREEN = 14;

  /** Constant: Identifier for parameter "lambda" */
  public static final int POPSIZE = 15;
  
  /** Constant: Identifier for parameter "mu" */
  public static final int NUMCHILDS = 16;
  
  /** Constant: Identifier for parameter "RTR_ILS_REP" */
  public static final int ILSREP = 17;
  
  /** Constant: Identifier for parameter "RTR_PROCENT" */
  public static final int RTRPROCENT = 18;

  /** Constant: Identifier for parameter "CPXTIME" */
  public static final int CPXTIME = 19;
  
  /** Constant: Identifier for parameter "CPXnodeLim" */
  public static final int CPXNODELIM = 20;
  
  /** Constant: Identifier for parameter "callbck" */
  public static final int CALLBCK = 21;
   
  /** Constant: Identifier for parameter "tolval" */
  public static final int TOLERANCE = 100;
  
  /** Constant: Identifier for parameter "initFile" */
  public static final int INITFILE = 200;
  
  /** Constant: Identifier for parameter "inFile" */
  public static final int INPUTFILE = 201;
  
  /** Constant: Identifier for parameter "outFile" */
  public static final int OUTPUTFILE = 202;
  
  /** Constant: Possible value of parameter "ls_type" **/
  public static final int FIRST_ACCEPT = 0;
  
  /** List of identifiers/keys of integer-valued parameters specified in the configuration file */
  private static int[] intPara = { IMPROVEMETHOD, GREEDYMEAS, LSTYPE, MAXITER, MAXNOIMP, NUMRUNS, 
                         WHATOUT, POPSIZE, NUMCHILDS, ILSREP, CPXNODELIM, CALLBCK };
                           
  /** List of identifiers/keys of double-valued parameters specified in the configuration file */                           
  private static int[] dblePara = { GLSALPHAFCOST, GLSALPHATCOST, SACOOLBETA, MINACCRATE, INIACCRATE, 
                                    SAMPLEGROWTH, RTRPROCENT, CPXTIME };

  /** List of identifiers/keys of boolean parameters specified in the configuration file */                           
  private static int[] boolPara = { DORESTART, SCREEN };
                           
  /** Parameter names as used in the configuration file */
  private static String[] paraName = {"ImproveMethod", "GreedyMeasure", "LocalSearch", "MaxIter", 
                              "MaxIterWithoutImprove", "GLS_alpha_fixedcost", "GLS_alpha_transpcost",
                              "SA_beta", "min_acc_rate", "ini_acc_rate", "SA_sample_growth", "Runs", 
                              "Restart", "Output", "Intermediate_Output", "lambda", "mu", 
                              "RTR_ILS_REP", "RTR_procent", "CPXTIME", "CPXNODELIM", "CALLBCK"};
                             
  /** Parameter: controls which improvement method is applied. Possible values are NONE, LOCALSEARCH, 
      ILS, MSLS, SA, SA_OSMAN, GLS, VNS, RTR, RTRJ, GRASP, ANTS, EA, TS, RTR_ILS, ILS_RTR, RTR-VNS, VNS-RTR, 
      EXTSS, ALTSS */
  protected static int impMethod;

  /** Parameter: specifies the greedy measure to be applied. Possible values are: 
      NONE, GR_LIN_CAP, GR_LIN_REMCAP, GR_LIN_TOTC */
  protected static int greedy_meas;

  /** Parameter: type of local search type to be applied, that is FIRST_ACCEPT or BEST_ACCEPT */
  protected static int ls_type;     

  /** Parameter: maximum number of iterations */
  protected static int max_iter;
  
  /** Parameter: maximum number of subsequent iterations without improving incumbent solution */
  protected static int max_no_imp;

  /** Parameter: controls fixed cost penalty in guided LS */ 
  protected static double  gls_alpf;
  
  /** Parameter: controls transporation cost penalty guided LS */
  protected static double gls_alpc;
  
  /** Parameter: SA-cooling schedule newTemp = sa_beta * oldTemp */
  protected static double sa_beta;     
  
  /** Parameter: Minimum acceptance rate for use in classical application of SA */
  protected static double min_acc_rate;
  
  /** Parameter: Initial acceptance rate for use in classical application of SA */
  protected static double ini_acc_rate;

  /** Parameter: Growth factor of sample size with decreasing temperature in classical SA */
  protected static double sample_growth;
  
  /** Parameter: number of times a procedure is repeated */
  protected static int num_runs;
  
  /** Parameter: specifies if initial solution is reset or not in multiple runs. Possible values: NO, YES */
  protected static boolean do_restart;  
  
  /** Parameter: Detail of output. Possible values: NODETAIL, DETAILED */
  protected static int whatOut;
  
  /** Parameter: specifies if information about single iterations is displayed. Possible values: ON, OFF */
  protected static boolean screen_on;

  /** Parameter: size of population in EA */
  protected static int lambda;
 
  /** Parameter: number of childs to be generated in one iteration of an EA */
  protected static int mu;
  
  /** Parameter: number of ILS iterations used within Jeanne's RTR_ILS procedure */
  protected static int RTR_ILS_REP;

  /** Parameter: controls maximum acceptable deviation from the record in a RTR travel */
  protected static double RTR_percent;
 
  /** Parameter: controls maximum time available for CPLEX to solve the MIP */
  protected static double CPXtime;

  /** Parameter: controls node limit available for CPLEX to solve the MIP */
  protected static int CPXnodeLim;
  
  /** Parameter: specifies if a heuristic callback should be used and if which 
                 heuristic method is used for improving solutionsr */
  protected static int callbck;

  /** Constant: default value of parameter tolval */
  private static final double tolDefault = 1.0E-4;
  
  /** Parameter: tolerance value */
  protected static double tolval = tolDefault;
   
  /** Parameter: name (full path) of initialization file */
  protected static String initFile=null;
  
  /** Parameter: name (full path) of input data file */
  protected static String inFile;
  
  /** Parameter: name (full path) of output file (where to write summarized results) */
  protected static String outFile;
                            
  /** Constant: Possible value of parameter "ls_type" **/
  public static final int BEST_ACCEPT = 1;
  
  /** Constant: Possible value of parameter "greedy_meas" (linearization by given capacity) */
  public static final int GR_LIN_CAP = 1;

  /** Constant: Possible value of parameter "greedy_meas" (llinearization by remaining capacity) */
  public static final int GR_LIN_REMCAP = 2;

  /** Constant: Possible value of parameter "greedy_meas" (total cost of sending max. flow on the arc) */
  public static final int GR_LIN_TOTC = 3;
  
  /** Constant: Possible value of parameter "whatout" */
  public static final int NODETAIL = 1;
  
  /** Constant: Possible value of parameter "whatout" */
  public static final int DETAILED = 2;

  /** Constant: Possible value of parameter "screen_on" */
  public static final boolean OFF = false;
  
  /** Constant: Possible value of parameter "screen_on" */
  public static final boolean ON  = true;
  
  /** Constant: Possible value of parameter "do_restart" */
  public static final boolean NO = false;
  
  /** Constant: Possible value of parameter "do_restart" */
  public static final boolean YES = true;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int NONE=0;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int LOCALSEARCH=1;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int ILS=2;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int MSLS=3;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int SA=4;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int SA_OSMAN=5;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int GLS=6;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int VNS=7;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int RTR=8;

  /** Constant: Possible value of parameter "impMethod" */
  public static final int RTRJ=9;

  /** Constant: Possible value of parameter "impMethod" */
  public static final int GRASP=10;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int ANTS=11;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int EA=12;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int TS=13;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int RTR_ILS=14;

  /** Constant: Possible value of parameter "impMethod" */
  public static final int ILS_RTR=15;

  /** Constant: Possible value of parameter "impMethod" */
  public static final int RTR_VNS=16;

  /** Constant: Possible value of parameter "impMethod" */
  public static final int VNS_RTR=17;

  /** Constant: Possible value of parameter "impMethod" */
  public static final int EXTSS=18;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int ALTSS=19;
  
  /** Constant: Possible value of parameter "impMethod" */
  public static final int CPXOPT=500;

  public static final int IRNLS=50;

  public static final int IRNLSv2=51;

  public static final int PIRNLS1=52;

  public static final int PIRNLS2=53;

  public static final int PIRNLSv2_1=54;

  public static final int PIRNLSv2_2=55;

  /**
   * Constructor for objects of class FCTPparam: sets parameters to default values
   */
  public FCTPparam()
  {
    setDefaults(); 
  }

  /**
   *  Set all parameters to default values
   */
  private static void setDefaults( )
  {
    tolval = tolDefault;
    max_iter = 50;
    max_no_imp = 100;
    gls_alpf = 0.1;
    gls_alpc = 0.0;
    sa_beta = 0.95;
    min_acc_rate = 0.001;
    ini_acc_rate = 0.3;
    sample_growth = 0.02;
    num_runs = 1;
    do_restart = YES;
    whatOut = NODETAIL;
    greedy_meas = GR_LIN_REMCAP;
    lambda = 100;
    mu = 100;
    RTR_ILS_REP = 20;
    RTR_percent = 0.1;
    CPXtime = Double.MAX_VALUE;
    CPXnodeLim = Integer.MAX_VALUE;
    callbck = 0;
    screen_on = ON;
    ls_type = FIRST_ACCEPT;
    impMethod = NONE;    
    inFile = null;
    outFile = null;
    initFile = null;
  }    
  
  /**
   *  Set an integer parameter to a certain value
   *  
   *  @param id    identifier of the parameter (see list on top of file)
   *  @param value (new) value of the parameter
   */
  public static void setParam ( int id, int value )
  {
    if ( value >= 0 )
    {
      switch ( id )
      {
        case MAXITER: max_iter = value; break;
        case MAXNOIMP: max_no_imp = value; break;
        case NUMRUNS: num_runs = value; break;
        case WHATOUT: if ( (value >= NONE) && (value <= DETAILED ) ) 
                        whatOut = value; break;
        case GREEDYMEAS: greedy_meas = value; break;
        case POPSIZE: lambda = value; break;
        case NUMCHILDS: mu = value; break;
        case ILSREP: RTR_ILS_REP = value; break;
        case IMPROVEMETHOD: impMethod = value; break;
        case LSTYPE: ls_type = value; break;
        case CPXNODELIM: if ( value > 0 ) CPXnodeLim = value; else CPXnodeLim = Integer.MAX_VALUE;
        case CALLBCK: if ( value >= 0 ) callbck = value;
      }
    }
  }

  /**
   *  Set a boolean parameter to a certain value
   *  
   *  @param id    identifier of the parameter (see list on top of file)
   *  @param value (new) value of the parameter
   */
  public static void setParam ( int id, boolean value )
  {
    switch ( id )
    {
      case SCREEN: screen_on = value; break;
      case DORESTART: do_restart = value; break;
    }
      
  }    

  /**
   *  Set a double parameter to a certain value
   *  
   *  @param id    identifier of the parameter (see list on top of file)
   *  @param value (new) value of the parameter
   */
  public static void setParam ( int id, double value )
  {
    if ( value > 0.0 )
    {
      switch ( id )
      {
        case GLSALPHAFCOST: gls_alpf = value; break;
        case GLSALPHATCOST: gls_alpc = value; break;
        case SACOOLBETA: sa_beta = Math.min( value, 0.99999 ); break;
        case MINACCRATE: min_acc_rate = Math.min(0.1, Math.max( 1.0E-6, value ) ); break;
        case INIACCRATE: ini_acc_rate = Math.min(0.9, Math.max( 0.1, value ) ); break;
        case SAMPLEGROWTH: sample_growth = Math.min(1.0, Math.max( 0.0, value ) ); break;
        case RTRPROCENT: RTR_percent = Math.max( tolval, value ); break;
        case TOLERANCE: tolval = Math.min( value, 0.99 ); break;
        case CPXTIME: CPXtime = value; break;
      }    
    }    

  }    

  /**
   *  Set a string parameter to a certain value
   *  
   *  @param id    identifier of the parameter (see list on top of file)
   *  @param value (new) value of the parameter
   */
  public static void setParam ( int id, String value )
  {
    switch ( id )
    {
      case INITFILE: initFile = value; break;  
      case INPUTFILE: inFile = value; break;
      case OUTPUTFILE: outFile = value; break;
    }    
  }    

  /**
   * Print parameter values to a terminal/screen
   */
  public static void printParams( )
  {
    System.out.println("----------------------------------------------------------");         
    System.out.println("FCTP parameter setting:");
    System.out.println("----------------------------------------------------------");   
    System.out.println("Input data file            : "+inFile );
    System.out.println("Output is send to file     : "+outFile );
    System.out.println("Method to be applied       : "+getProcName( ) );
    if ( greedy_meas > 0 )
    {
      System.out.println("Start solutions obtained by: Greedy" );
      System.out.println("Greedy measure used        : "+greedy_meas );
    }
    else 
      System.out.println("Start solutions obtained by: LP heuristic" );
    System.out.println("Type of local search to use: "+ls_type );
    System.out.println("Number of iterations       : "+max_iter );
    System.out.println("Iterations without improve : "+max_no_imp );
    System.out.println("GLS - penalty fixed cost   : "+gls_alpf );
    System.out.println("GLS - penalty transp. cost : "+gls_alpc );
    System.out.println("SA - parameter beta        : "+sa_beta );
    System.out.println("SA - initial accept. rate  : "+ini_acc_rate );
    System.out.println("SA - final accept. rate    : "+min_acc_rate );
    System.out.println("SA - sample size growth    : "+sample_growth );
    System.out.println("Output detail              : "+whatOut );
    System.out.println("Intermediate Output is on  : "+screen_on );
    System.out.println("Number of runs             : "+num_runs);
    System.out.println("Each run with restart?     : "+do_restart);
    System.out.println("Population size in EA      : "+lambda);
    System.out.println("Number of childs in EA     : "+mu);
    System.out.println("ILS iterations in RTR-ILS  : "+RTR_ILS_REP);
    System.out.println("RTR-threshold precentage   : "+RTR_percent);
    System.out.println("CPLEX time limit           : "+CPXtime );
    System.out.println("CPLEX node limit           : "+CPXnodeLim );
    System.out.println("Callback heuristic         : "+callbck );
    System.out.println("----------------------------------------------------------"); 

  } 
    
  /**
   *  Return the name of the improvement method corresponding to the value of parameter "impMethod"
   */
  public static String getProcName (  )
  {
    switch ( impMethod )
    {
      case NONE: return("UNKNOWN");
      case LOCALSEARCH: return("Local Search");
      case ILS: return("Iterated Local Search");
      case ILS_RTR: return("Hybrid ILS-RTR");
      case MSLS: return("Multi-Start Local Search");
      case SA: return("Simulated Annealing");
      case SA_OSMAN: return("SA a la Osman");
      case GLS: return("Guided Local Search");
      case VNS: return("Variable Neighbourhood Search");
      case VNS_RTR: return("Hybrid VNS-RTR");
      case RTR: return("Record-to-Record Travel");
      case RTRJ: return("Jeanne's Record-to-Record Travel");
      case RTR_ILS: return("Hybrid RTR_ILS");
      case RTR_VNS: return("Hybrid RTR_VNS");
      case GRASP: return("GRASP");
      case ANTS: return("Ant Colony");
      case EA: return("Evolutionary Algorithm");
      case TS: return("Tabu Search");
      case EXTSS: return("Extended Scatter Search");
      case ALTSS: return("Alternative Scatter Search");
      case CPXOPT: return("Optimal solution with CPLEX");
      case 50: return("IRNLS");
      case 51: return("IRNLS_v2");
      case 52: return("PIRNLS");
      case 53: return("PIRNLS");

    }    
    return("UNKOWN");
  }    
  
  /** 
   *  Method that reads the initialization file and sets the parameters as specified in this file
   */
  public static void ReadIniFile( ) throws Exception
  {
    if ( initFile != null )
    {
      try
      {
        Properties p = new Properties( );  
        p.load(new FileInputStream(initFile) );
        for ( int pid : intPara ) 
        {
          String param = p.getProperty( paraName[pid] );
          if ( param != null ) setParam( pid, Integer.parseInt( param ) );
        }  
        for ( int pid : dblePara ) 
        {
          String param = p.getProperty( paraName[pid] );
          if ( param != null ) setParam( pid, Double.parseDouble( param ) );
        }  
        for ( int pid : boolPara ) 
        {
          String param = p.getProperty( paraName[pid] );
          if ( param != null ) setParam( pid, ( Integer.parseInt(param) > NONE ) );
        }    
      } catch ( Exception e)
      {
        throw ( e );   
      }    
    }  
    //printParams();
    
  }     

}
