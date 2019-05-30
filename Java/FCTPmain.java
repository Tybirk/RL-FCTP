import java.io.*;
import java.util.*;

/**
 *  Class that creates an instance of class "FCTPheur" and uses
 *  the method implemented in FCTPheur to solve instances of the FCTP
 *
 * @author  Andreas Klose
 * @version 15/03/2018
 */
public class FCTPmain
{       
    
  /**
   *   Main method: Reads command line parameters, then the initialization file 
   *   with parameter values, and then uses class FCTPheur to construct and improve 
   *   a solution, and finally prints the results
   */
  public static void main ( String[] args )
  {
    // Display greeting message and read arguments from command line
    //System.out.println("==========================================================");
    //System.out.println("  Heuristics for the Fixed-Charge Transportation Problem  ");
    //System.out.println("----------------------------------------------------------");
    if ( args.length == 0 ) {
      System.out.println("Usage: Input_file <Batch - Output file >");
      System.out.println("----------------------------------------------------------");
      return;
    } 
    
    // Tell class FCTPparam about initialization file, input and output filename
    FCTPparam.setParam( FCTPparam.INPUTFILE, args[0] );
    if ( args.length > 1 ) FCTPparam.setParam( FCTPparam.OUTPUTFILE, args[1] );
    else { FCTPparam.setParam(FCTPparam.OUTPUTFILE, "results_greedy3/" + args[0].split("/")[1] + ".txt"); }
    FCTPparam.setParam( FCTPparam.INITFILE, "FCTPheur.ini" );
    
    try 
    {    
      // Read parameters from initialization file
      FCTPparam.ReadIniFile(  ); 
      // Construct instance of class FHeuris
      FCTPheur FHeuris = new FCTPheur( args[0] ); 
      // Compute initial solution
      FHeuris.initialSolution( );


      // Improve initial solution
      if ( FCTPparam.impMethod != FCTPparam.NONE ) FHeuris.improveSolution( );                
    }
    catch(Exception exc) 
    {
      System.out.println("Error: " + exc.getMessage());
    }

  }
    
}  


