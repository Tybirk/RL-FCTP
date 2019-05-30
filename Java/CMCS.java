import java.io.FileWriter;
import java.util.HashMap;

/**
 * Conditional Markov chain search for the Fixed Charge Transportation Problem (FCTP)
 *
 * @author  Peter Emil Tybirk
 * @version 21/05/2019
 */

public class CMCS extends FCTPheur {
    private int[] components;  //Integers specifying which subset of components to use
    private boolean[][] m_suc;  //Matrix defining configuration transitions in case of success
    private boolean[][] m_fail; //Matrix defining configuration transitions in case of fail
    private int size;  // Number of components in subset
    private int current_component;
    private int current_component_idx;
    private String name_of_file;
    private FCTPsol best_sol;
    private HashMap<Integer, Integer> component_to_idx;

    public CMCS( String fname, int[] components, boolean[][] m_suc, boolean[][] m_fail) throws Exception
    {
        super( fname );
        this.name_of_file = fname;
        this.components = components;
        this.m_suc = m_suc;
        this.m_fail = m_fail;
        this.size = components.length;
        this.component_to_idx= new HashMap<Integer, Integer>();

        for(int i = 0; i<size;i++){
            component_to_idx.put(components[i], i);
        }

        String instance_name = name_of_file.substring(name_of_file.length() - 10); //Read instance name
        FCTPparam.setParam(FCTPparam.OUTPUTFILE, "CMCS_results/" + "test_" + instance_name + ".txt");

        if(FCTPparam.outFile != null && !FCTPparam.outFile.isEmpty()) {
            fileWriter = new FileWriter(FCTPparam.outFile);
            fileWriter.write(FCTPparam.inFile + "\n");
        }

    }

    /**
     * Method for running the current configuration,
     *
     * @param time_budget time_budget in milliseconds. If it is 0 we instead run for a maximum of FCTPparam.max_no_imp
     *                    iterations without improvement
     */
    public void Solve(int time_budget) {
        // Initialize solution
        if(randgen.nextBoolean() == true) RandGreedy(0.3);
        else LPheu();
        Kicksolution((m+n-1)/2);

        double old_cost = solution.totalCost;
        best_sol = new FCTPsol( solution );

        boolean improved;
        long start_time = System.currentTimeMillis();
        long current_time;
        int n_fail = 0;
        current_component = components[0]; // Set first component
        long elapsed_time = 0;

        if(time_budget==0){  //Run until max_fails
            while(n_fail < FCTPparam.max_no_imp) {

                use_current_component();
                n_fail++;

                if (solution.totalCost < old_cost) {
                    improved = true;
                    if (solution.totalCost < best_sol.totalCost) {
                        best_sol.Overwrite(solution);
                        n_fail = 0;
                    }
                } else {
                    improved = false;
                }

                set_next_component(improved);
            }
        }
        else {    //run for time budget
            while (elapsed_time < time_budget) {
                use_current_component();
                n_fail++;
                if (solution.totalCost < old_cost) {
                    improved = true;
                    if (solution.totalCost < best_sol.totalCost) {
                        best_sol.Overwrite(solution);
                        n_fail = 0;
                    }
                } else {
                    improved = false;
                }

                set_next_component(improved);

                current_time = System.currentTimeMillis();
                elapsed_time = (current_time - start_time);
            }
        }

        solution.Overwrite(best_sol);
        System.out.println("Best solution value: " + best_sol.totalCost);

        //Log result
        try {
            fileWriter.write(best_sol.totalCost + "\n");
        }
        catch (Exception exc){
            System.out.println("Error: " + exc.getMessage());
        }
    }

    /**
     * Method for setting next component based on configuration matrices
     * @param improved Boolean indicating whether the last selected component improved the solution
     */
    public void set_next_component(boolean improved){
        current_component_idx = component_to_idx.get(current_component);

        if(improved){
            for(int i = 0; i < size; i++){
                if(m_suc[current_component_idx][i]){
                    current_component = components[i];
                    break;
                }
            }
        }
        else{
            for(int i = 0; i < size; i++){
                if(m_fail[current_component_idx][i]){
                    current_component = components[i];
                    break;
                }
            }
        }
    }

    /**
     * Applies current component
     */
    public void use_current_component(){
        switch ( current_component ) {
            case 1: modified_cost_local_search(4,0); LS_best_acc(); break;
            case 2: RNLS(50, 20); LS_first_acc(); break;
            case 3: modified_cost_local_search(5,0); LS_first_acc(); break;
            case 4: Kicksolution_greedy(0, rc1); LS_first_acc(); break;
            case 5: Kicksolution_greedy(0, rc2); LS_first_acc(); break;
            case 6: Kicksolution_greedy(0, rc3); LS_first_acc(); break;
            case 7: solution.Overwrite(best_sol); RNLS(50, 20); LS_first_acc(); break;
            case 8: solution.Overwrite(best_sol); modified_cost_local_search(4,0); LS_first_acc(); break;
            case 9: modified_cost_local_search(1,0); LS_first_acc(); break;
            case 10: modified_cost_local_search(2,0); LS_first_acc(); break;
            case 11: modified_cost_local_search(4,0); LS_first_acc(); break;
        }
    }

    /**
     * Run CMCS and log results
     */
    public void run_cmcs(){
        for (int i = 0; i < FCTPparam.num_runs; i++) {
            Solve(0);
        }
        try {
            fileWriter.close();
        }
        catch (Exception exc) {
            System.out.println("Error: " + exc.getMessage());
        }
    }


    /**
     * Try a configuration
     * @param args Where to find the instance
     */
    public static void main ( String[] args ) {

        // Tell class FCTPparam about initialization file, input and output filename
        FCTPparam.setParam(FCTPparam.INPUTFILE, args[0]);
        if (args.length > 1) FCTPparam.setParam(FCTPparam.OUTPUTFILE, args[1]);
        FCTPparam.setParam(FCTPparam.INITFILE, "FCTPheur.ini");

        // configuration: [[[False, False, True], [False, True, False], [False, True, False]], [[False, True, False], [False, False, True], [True, False, False]]]
        try {
            // Read parameters from initialization file
            FCTPparam.ReadIniFile();

            int[] components = {2, 5, 6};
            boolean[][] m_suc = new boolean[3][3];
            m_suc[0][0] = false;
            m_suc[0][1] = false;
            m_suc[0][2] = true;
            m_suc[1][0] = false;
            m_suc[1][1] = true;
            m_suc[1][2] = false;
            m_suc[2][0] = false;
            m_suc[2][1] = true;
            m_suc[2][2] = false;

            boolean[][] m_fail = new boolean[3][3];
            m_fail[0][0] = false;
            m_fail[0][1] = true;
            m_fail[0][2] = false;
            m_fail[1][0] = false;
            m_fail[1][1] = false;
            m_fail[1][2] = true;
            m_fail[2][0] = true;
            m_fail[2][1] = false;
            m_fail[2][2] = false;

            CMCS cmcs = new CMCS(args[0], components, m_suc, m_fail);
            cmcs.run_cmcs();
            // Compute initial solution

        } catch (Exception exc) {
            System.out.println("Error: " + exc.getMessage());
        }
    }

}
