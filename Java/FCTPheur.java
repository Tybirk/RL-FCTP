/**
 * Implements different (meta-) heuristics for the Fixed Charge Transportation Problem (FCTP)
 *
 * @author Andreas Klose
 * @version 22/05/2018
 */

public class FCTPheur extends FCTPpop {
    /**
     * class used to keep track of heuristic's performance
     */
    protected FCTPperfm perfMeter;

    /**
     * Constructor of class FCTPheur that reads data from file or passes the data to be used
     * to the class FCTPdata.
     *
     * @param fname name (that is full path) of the input data file
     */
    public FCTPheur(String fname) throws Exception {
        super(fname);
        // BEGIN(The following only for use with Pyjnius)
        FCTPparam.setParam(FCTPparam.INPUTFILE, fname);
        FCTPparam.setParam(FCTPparam.INITFILE, "FCTPheur.ini");
        FCTPparam.ReadIniFile();
        // END(Pynius)
        perfMeter = new FCTPperfm();
    }

    /**
     * Constructor that takes data as parameters. All data are copied.
     *
     * @param mm      number of suppliers
     * @param nn      number of customers
     * @param s       integer array of mm supply quantities
     * @param d       integer array of nn demand quantities
     * @param tc      tc[arc] is for arc=i*nn+j the unit transporation cost from supplier i to customer j
     * @param fc      fc[arc] is for arc=i*nn+j the fixed cost on arc from supplier i to customer
     * @param copyDat true if data should be copied to new arrays. Otherwise just a reference is set to the data.
     */
    public FCTPheur(int mm, int nn, int[] s, int[] d, double[] tc, double[] fc, boolean copyDat) {
        super(mm, nn, s, d, tc, fc, copyDat);
        perfMeter = new FCTPperfm();
    }

    /**
     * Method for constructing a first feasible solution.
     */
    public void initialSolution() throws Exception {
        //if (randgen.nextBoolean()) LPheu();
        //else RandGreedy(0.4);
        perfMeter.resetStats();
        if (FCTPparam.greedy_meas <= 0) {
            if (!LPheu()) throw new Exception("LP heuristic failed to give basic solution");
        } else {
            RandGreedy(0.4);
        }
        perfMeter.updateStats(0, solution.totalCost);
        if (FCTPparam.whatOut != FCTPparam.NONE) {
            perfMeter.displayPerformance(true);
            if (FCTPparam.whatOut == FCTPparam.DETAILED) solution.printFlows();
        }

    }

    /**
     * Improves solutions using a method specified by parameter FCTPparam.impMethod
     */
    public void improveSolution() {
        // Remember initial solution for possibly restarting stochastic search methods
        FCTPsol startSol = new FCTPsol(solution);

        // If several runs of a non-deterministic search is performed, keep track of
        // best solution obtained in all those runs
        FCTPsol theBest = null;
        if (FCTPparam.num_runs > 1) theBest = new FCTPsol(solution);

        // Reset performance measures
        perfMeter.resetStats();

        // If only CPLEX is tried, make sure that only one run is made
        //if ( FCTPparam.impMethod == FCTPparam.CPXOPT ) FCTPparam.num_runs=1;

        // Maybe first a heuristic and thereafter CPLEX should be called
        int impMeth = FCTPparam.impMethod;
        //FCTPparam.impMethod %= FCTPparam.CPXOPT*100;

        // Execute selected heuristic num_runs times
        for (int run = 0; run < FCTPparam.num_runs; run++) {
            switch (FCTPparam.impMethod) {
                case FCTPparam.LOCALSEARCH:
                    LocalSearch();
                    break;
                case FCTPparam.ILS:
                    ILS();
                    break;
                case FCTPparam.MSLS:
                    MSLS();
                    break;
                case FCTPparam.SA:
                    SA();
                    break;
                case FCTPparam.SA_OSMAN:
                    OsmanSA();
                    break;
                case FCTPparam.RTR:
                    RTR_proc();
                    break;
                case FCTPparam.RTR_ILS:
                    RTR_proc();
                    break;
                case FCTPparam.RTR_VNS:
                    RTR_proc();
                    break;
                case FCTPparam.ILS_RTR:
                    ILS();
                    break;
                case FCTPparam.RTRJ:
                    RTR_Jeanne();
                    break;
                case FCTPparam.GLS:
                    GLS();
                    break;
                case FCTPparam.VNS:
                    VNS();
                    break;
                case FCTPparam.VNS_RTR:
                    VNS();
                    break;
                case FCTPparam.GRASP:
                    Grasp();
                    break;
                case FCTPparam.ANTS:
                    AntColony();
                    break;
                case FCTPparam.EA:
                    EA();
                    break;
                case FCTPparam.EXTSS:
                    extSS_SCS();
                    break;
                case FCTPparam.ALTSS:
                    altSS_SCS();
                    break;
                case FCTPparam.TS:
                    TS();
                    break;
                case FCTPparam.IRNLS:
                    IRNLS(0);
                    break;
                case FCTPparam.IRNLSv2:
                    IRNLS_v2(0);
                    break;
                case FCTPparam.PIRNLS1:
                    int[] population_sizes = new int[2];
                    population_sizes[0] = 100;
                    population_sizes[1] = 20;
                    int[] max_runs = new int[2];
                    max_runs[0] = 600;
                    max_runs[1] = 800;
                    PIRNLS(population_sizes, max_runs, false);
                    break;
                case FCTPparam.PIRNLS2:
                    int[] population_sizes2 = new int[3];
                    population_sizes2[0] = 300;
                    population_sizes2[1] = 50;
                    population_sizes2[2] = 20;
                    int[] max_runs2 = new int[3];
                    max_runs2[0] = 200;
                    max_runs2[1] = 800;
                    max_runs2[2] = 1000;
                    PIRNLS(population_sizes2, max_runs2, false);
                    break;
                case FCTPparam.PIRNLSv2_1:
                    int[] population_sizes3 = new int[2];
                    population_sizes3[0] = 100;
                    population_sizes3[1] = 10;
                    int[] max_runs3 = new int[2];
                    max_runs3[0] = 1500;
                    max_runs3[1] = 2000;
                    PIRNLS(population_sizes3, max_runs3, true);
                    break;
                case FCTPparam.PIRNLSv2_2:
                    int[] population_sizes4 = new int[3];
                    population_sizes4[0] = 500;
                    population_sizes4[1] = 50;
                    population_sizes4[2] = 10;
                    int[] max_runs4 = new int[3];
                    max_runs4[0] = 300;
                    max_runs4[1] = 1500;
                    max_runs4[2] = 2500;
                    PIRNLS(population_sizes4, max_runs4, true);
                    break;
                //case FCTPparam.CPXOPT: FCTPoptim( ); break;
            }
            perfMeter.updateStats(getLSiter(), solution.totalCost);

            // Save the "greatest of all times"
            if (theBest != null)
                if (solution.totalCost < theBest.totalCost) theBest.Overwrite(solution);

            // Reset solution to the start solution if stochastic search methods is used
            boolean resetSol = ((FCTPparam.do_restart) && (run < FCTPparam.num_runs - 1));
            if (resetSol) solution.Overwrite(startSol);
            if (FCTPparam.impMethod == 52 || FCTPparam.impMethod == 53 || FCTPparam.impMethod == 54 || FCTPparam.impMethod == 55)
                break;
        }

        // Overwrite actual solution with best one found in all the runs
        if (theBest != null) solution.Overwrite(theBest);

        // Call CPLEX optimizer if optimal solution should be computed (after the heuristic)
        if (impMeth > FCTPparam.CPXOPT) {
            FCTPparam.impMethod = FCTPparam.CPXOPT;
            //FCTPoptim( );
            perfMeter.updateStats(getLSiter(), solution.totalCost);
        }

        // Give output on screen and possibly on file
        if (FCTPparam.whatOut != FCTPparam.NONE) {
            perfMeter.displayPerformance(false);
            if (FCTPparam.whatOut == FCTPparam.DETAILED) solution.printFlows();
        }
        try {
            fileWriter.close();
        } catch (Exception e) {
        }

    }

}
