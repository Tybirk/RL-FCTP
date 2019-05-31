import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Heuristics for the Fixed Charge Transportation Problem (FCTP)
 *
 * @author  Peter Emil Tybirk
 * @version 29/05/2019
 */

public class PEheur extends FCTPls {
    public double[] gvals1;
    public double[] gvals2;
    public double[] gvals3;
    public double[] arc_evaluation_measure;
    public RandomCollection<Integer> rc1;
    public RandomCollection<Integer> rc2;
    public RandomCollection<Integer> rc3;


    public PEheur(String fname) throws Exception {
        super(fname);

        this.gvals1 = get_greedy_values();
        this.gvals2 = get_greedy_values_2(5);
        this.gvals3 = get_greedy_values_3(5);

        this.rc1 = get_random_collection(gvals1, true);
        this.rc2 = get_random_collection(gvals2, true);
        this.rc3 = get_random_collection(gvals3, true);
    }

    public PEheur(int mm, int nn, int[] s, int[] d, double[] tc, double[] fc, boolean copyDat) {
        super(mm, nn, s, d, tc, fc, copyDat);
    }


    /**
     * Greedy1 evaluation
     *
     * @return Array of cost per unit sent on each arc at full capacity
     */
    public double[] get_greedy_values() {
        double[] gvals = new double[narcs];
        for (int arc = 0; arc < narcs; arc++) {
            double gval = (gettcost(arc) + getfcost(arc) / (double) getCap(arc)); // small value is best
            gvals[arc] = gval;
        }
        return gvals;
    }

    /**
     * Greedy2 evaluation, calculates cost per unit at full capacity with average
     * value of k best arcs from each supplier/customer subtracted.
     *
     * @param k number of best customers/suppliers to calculate average over
     * @return Array of greedy evaluations
     */
    public double[] get_greedy_values_2(int k) {
        double[] gvals = new double[narcs];
        double[][] best_k_suppliers = new double[m][k]; //Best k values per unit at full capacity for each supplier
        double[][] best_k_customers = new double[n][k]; ////Best k values per unit at full capacity for each customer

        for (int i = 0; i < m; i++) Arrays.fill(best_k_suppliers[i], 99999.0);
        for (int i = 0; i < n; i++) Arrays.fill(best_k_customers[i], 99999.0);

        //calculate best k arcs for suppliers
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                Arrays.sort(best_k_suppliers[i]);
                double gval = (gettcost(i * n + j) + getfcost(i * n + j) / (double) getCap(i * n + j));

                if (gval < best_k_suppliers[i][k - 1]) {
                    best_k_suppliers[i][k - 1] = gval;
                }
            }
        }

        //calculate best k arcs for customers
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < m; i++) {
                Arrays.sort(best_k_customers[j]);
                double gval = (gettcost(i * n + j) + getfcost(i * n + j) / (double) getCap(i * n + j));
                if (gval < best_k_customers[j][k - 1]) {
                    best_k_customers[j][k - 1] = gval;
                }
            }
        }

        // Calculate averages over k
        double[] avg_j_supplier = new double[m];
        double[] avg_j_customer = new double[n];
        Arrays.fill(avg_j_supplier, 0);
        Arrays.fill(avg_j_customer, 0);

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < k; j++) {
                avg_j_supplier[i] += best_k_suppliers[i][j];
            }
            avg_j_supplier[i] /= k;
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < k; j++) {
                avg_j_customer[i] += best_k_customers[i][j];
            }
            avg_j_customer[i] /= k;
        }

        // Subtract averages from naive greedy evaluations
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                double gval = (gettcost(i * n + j) + getfcost(i * n + j) / (double) getCap(i * n + j));
                double gval2 = Math.min(gval - avg_j_supplier[i], gval - avg_j_customer[j]);
                gvals[i * n + j] = gval2;
            }
        }

        return gvals;
    }

    /**
     * Greedy3 evaluation. Equal to greedy2 valuation, but additional minimal cost per unit
     * if full supply or demand is not med added
     *
     * @param k number of best customers/suppliers to calculate average over
     * @return Array of greedy evaluations
     */
    public double[] get_greedy_values_3(int k) {
        double[] gvals3 = new double[narcs];
        double[] gvals2 = get_greedy_values_2(k);

        // Calculate greedy evaluation by finding greedy2 value and then
        // adding additional cost per unit needed
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                int sup = supply[i];
                int dem = demand[j];

                double gval3 = gvals2[i * n + j];

                double min_extra_cost = 999999.0;

                if (sup > dem) {
                    for (int l = 0; l < n; l++) { //Run through customers
                        if (l == j) continue;
                        double cost_per_unit = gettcost(i * n + l) + getfcost(i * n + l) / ((double) sup - dem);
                        if (cost_per_unit < min_extra_cost) min_extra_cost = cost_per_unit;
                    }
                    gval3 += min_extra_cost;
                } else if (dem > sup) {
                    for (int l = 0; l < m; l++) { //Run through suppliers
                        if (l == i) continue;
                        double cost_per_unit = gettcost(l * n + j) + getfcost(l * n + j) / ((double) dem - sup);
                        if (cost_per_unit < min_extra_cost) min_extra_cost = cost_per_unit;
                    }
                    gval3 += min_extra_cost;
                }
                gvals3[i * n + j] = gval3;
            }

        }

        return gvals3;
    }

    /**
     * Class for creating a set which you can draw from with probabilities proportional to some evaluation measure (weight)
     * See https://stackoverflow.com/questions/6409652/random-weighted-selection-in-java
     */
    public class RandomCollection<E> {
        private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
        private final Random random;
        private double total = 0;
        private double max_val = 0;

        public RandomCollection() {
            this(new Random());
        }

        public RandomCollection(Random random) {
            this.random = random;
        }

        // Find largest eval measure value for the purpose of inverting probabilities
        public void set_max_val(double[] gvals) {
            this.max_val = Arrays.stream(gvals).max().getAsDouble();
        }

        // Add elements with weight to collection. the boolean indicates if smaller weight should give higher probability
        public RandomCollection<E> add(double weight, E result, boolean small_is_better) {
            if (small_is_better) weight = this.max_val - weight;
            if (weight <= 0) return this;
            total += weight;
            map.put(total, result);
            return this;
        }

        // Get element from set with probability according to its weight
        public E next() {
            double value = random.nextDouble() * total;
            return map.higherEntry(value).getValue();
        }
    }

    /**
     * Get a random collection with weights according to evaluation measure.
     *
     * @param greedy_vals       Array where the i'th entry has an evaluation of the i'th arc
     * @param smaller_is_better Whether or not a small value is better in the evaluation measure
     * @return A random collection instance
     */
    public RandomCollection<Integer> get_random_collection(double[] greedy_vals, boolean smaller_is_better) {
        double[] gvals = greedy_vals;

        RandomCollection<Integer> rc = new RandomCollection<>();
        if (smaller_is_better) rc.set_max_val(gvals);

        //Now add all arcs
        int j = 0;
        for (double gval : gvals) {
            if (solution.arc_stat[j] != BASIC) {
                rc.add(gval, j, smaller_is_better);
            }
            j++;
        }

        return rc;
    }

    /**
     * Random neighbourhood local search
     *
     * @param max_iter maximum number of iterations
     * @param splits   the number of partitions of the total neighbourhood, governs size of random neighbourhood
     */
    public void RNLS(int max_iter, int splits) {
        int iter = 0;
        FCTPsol best_sol = new FCTPsol(solution);
        boolean improved = false;

        do {
            // Generate random neighbourhood
            Set<Integer> random_neighbourhood = new LinkedHashSet<Integer>();
            while (random_neighbourhood.size() < narcs / splits) {
                Integer next = randgen.nextInt(narcs);
                // As we're adding to a set, this will automatically do a containment check
                random_neighbourhood.add(next);
            }

            // Run through the random neighbourhood and find best move
            double bestSav = -9999999;
            for (int arc : random_neighbourhood) {
                if (solution.arc_stat[arc] != BASIC) {
                    double saving = getCostSav(arc);
                    if (saving != 0 && saving > bestSav) {
                        bestSav = saving;
                        RememberMove();
                    }
                }
            }

            DoMove();

            // Check for new best solution
            if (solution.totalCost < best_sol.totalCost) {
                best_sol.Overwrite(solution);
                improved = true;
            }

            iter++;

        } while (iter < max_iter);

        // If a new best solution has been found, we move back to that one
        if (improved) {
            solution.Overwrite(best_sol);
        }
    }

    /**
     * Random neighbourhood local search, faster implementation, but the neighbourhood may include duplicates
     * we did not use this in our experiments, but it would likely speed up the procedure slightly.
     *
     * @param max_iter maximum number of iterations
     * @param splits   the number of partitions of the total neighbourhood, governs size of random neighbourhood
     */

    public void RNLS_fast(int max_iter, int splits) {
        int iter = 0;
        FCTPsol best_sol = new FCTPsol(solution);
        boolean improved = false;
        int arc = 0;

        do {
            // Run through the random neighbourhood and find best move
            double bestSav = -9999999;
            for (int i = 0; i < narcs / splits; i++) {
                arc = randgen.nextInt(narcs);
                if (solution.arc_stat[arc] != BASIC) {
                    double saving = getCostSav(arc);
                    if (saving != 0 && saving > bestSav) {
                        bestSav = saving;
                        RememberMove();
                    }
                }
            }

            DoMove();

            // Check for new best solution
            if (solution.totalCost < best_sol.totalCost) {
                best_sol.Overwrite(solution);
                improved = true;
            }

            iter++;

        } while (iter < max_iter);

        // If a new best solution has been found, we move back to that one
        if (improved) {
            solution.Overwrite(best_sol);
        }
    }

    /**
     * Introduces num_exchanges new arcs into the basis with probabilities according to an evaluation measure
     *
     * @param num_exchanges - number of non-basic arcs to be made basic. If equal to zero, this number
     *                      is randomly decided (between 5 arcs and 20% of the basic arcs)
     * @param rc            - a RandomCollection object with evaluation of each arc
     */
    public void Kicksolution_greedy(int num_exchanges, RandomCollection<Integer> rc) {
        // If number of exchanges unspecified, then randomly decide on it
        FCTPsol best_sol = new FCTPsol(solution);
        boolean improve = false;

        if (num_exchanges == 0) {
            int num_basic = m + n - 1;
            num_exchanges = 3;
            // Let the number of exchanges be random between 5 and 20% of number
            // of basic variables
            if (num_basic / 5 > 5) num_exchanges = 5 + randgen.nextInt(num_basic / 5 - 4);
        }

        // Apply num_exchanges random basic exchanges.
        for (int itr = 0; itr < num_exchanges; itr++) {
            int in_arc;
            // Pick a non-basic arc with probability according to rc
            while (true) {
                in_arc = rc.next();
                if (solution.arc_stat[in_arc] != BASIC) break;
            }
            getCostSav(in_arc);
            RememberMove();
            DoMove();

            if(solution.totalCost < best_sol.totalCost){ //If we by chance improve the solution, we keep track of it!
                best_sol.Overwrite(solution);
                improve = true;
            }
        }

        if(improve){
            solution.Overwrite(best_sol);
        }

    }

    /**
     * Find indices of K biggest numbers in an array
     *
     * @param array the array to find indices from
     * @param k     the number indices to find
     * @return array of length k containing the k indices
     */
    public static int[] get_max_k_idx(double[] array, int k) {
        double[] max = new double[k];
        int[] maxIndex = new int[k];
        Arrays.fill(max, Double.NEGATIVE_INFINITY);
        Arrays.fill(maxIndex, -1);

        top:
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < k; j++) {
                if (array[i] > max[j]) {
                    for (int x = k - 1; x > j; x--) {
                        maxIndex[x] = maxIndex[x - 1];
                        max[x] = max[x - 1];
                    }
                    maxIndex[j] = i;
                    max[j] = array[i];
                    continue top;
                }
            }
        }
        return maxIndex;
    }

    /**
     * Modified cost local search.
     * 'Kick out arcs' from current solution by increasing fixed costs of n_kicked worst arcs according to
     * cost type
     *
     * @param cost_type Value between 1 and 6, indicating gvals1, gvals2, gvals3, per unit, random
     *                  and arc evaluation measure
     * @param n_kicked  Number of arcs kicked out. If equal to zero, this number
     *                  is randomly decided (between 5 arcs and 20% of the basic arcs).
     */
    public void modified_cost_local_search(int cost_type, int n_kicked) {
        double[] arc_costs = new double[narcs];

        switch (cost_type) {
            case 1:
                arc_costs = get_arc_costs_from_array(gvals1);
                break;
            case 2:
                arc_costs = get_arc_costs_from_array(gvals2);
                break;
            case 3:
                arc_costs = get_arc_costs_from_array(gvals3);
                break;
            case 4:
                arc_costs = get_arc_costs_per_unit();
                break;
            case 5:
                arc_costs = get_random_arc_costs();
                break;
            case 6:
                arc_costs = get_arc_costs_from_array(arc_evaluation_measure);
                break;
        }

        //Get number kicked
        if (n_kicked == 0) {
            int num_basic = m + n - 1;
            n_kicked = 3;
            // Let the number of exchanges be random between 5 and 20% of number
            // of basic variables
            if (num_basic / 5 > 5) n_kicked = 5 + randgen.nextInt(num_basic / 5 - 4);
        }

        int[] k_biggest = get_max_k_idx(arc_costs, n_kicked);
        double[] old_fc = new double[narcs];
        System.arraycopy(fcost, 0, old_fc, 0, narcs);   //Store old fcosts

        for (int i = 0; i < n_kicked; i++) {
            fcost[k_biggest[i]] = fcost[k_biggest[i]] * 100;
        }

        solution.ComputeCost(fcost, tcost);
        LS_first_acc();  // Search with modified cost structure

        System.arraycopy(old_fc, 0, fcost, 0, narcs);  //Restore old fcosts
        solution.ComputeCost(fcost, tcost);
    }

    /**
     * Assign cost to each arc in current solution at full capacity according to array of costs
     *
     * @param eval_array Array with arc evaluations
     * @return Array of evaluations of length narcs, which is 0 for all arcs outside current solution
     */
    public double[] get_arc_costs_from_array(double[] eval_array) {
        double[] arc_costs = new double[narcs];
        Arrays.fill(arc_costs, 0.0);
        for (int arc = 0; arc < narcs; arc++) {
            if (solution.flow[arc] > 0) {
                arc_costs[arc] = eval_array[arc];
            }
        }
        return arc_costs;
    }

    /**
     * Calculate cost of each arc in current solution per unit
     *
     * @return Array of costs of length narcs, which is 0 for all arcs outside current solution
     */
    public double[] get_arc_costs_per_unit() {
        double[] arc_costs = new double[narcs];
        Arrays.fill(arc_costs, 0.0);
        for (int arc = 0; arc < narcs; arc++) {
            if (solution.flow[arc] > 0) {
                arc_costs[arc] = (fcost[arc] + tcost[arc] * solution.flow[arc]) / solution.flow[arc];
            }
        }
        return arc_costs;
    }

    /**
     * Assign random cost to each arc for the purpose of random evaluations
     *
     * @return Array of random costs of length narcs
     */
    public double[] get_random_arc_costs() {
        double[] arc_costs = new double[narcs];
        Arrays.fill(arc_costs, 0.0);
        for (int arc = 0; arc < narcs; arc++) {
            if (solution.flow[arc] > 0) {
                arc_costs[arc] = ThreadLocalRandom.current().nextInt(1, m + n + 1);
            }
        }
        return arc_costs;
    }

    /**
     * Evaluation based ILS
     *
     * @param rc       Random collection to draw arcs from
     * @param max_runs Maximum number of iterations without improvement to the objective value
     */
    public void Evaluation_based_IRNLS(RandomCollection<Integer> rc, int max_runs) {

        if (max_runs == 0) {
            max_runs = FCTPparam.max_no_imp;
        }

        // Initialise iteration counters
        int num_fail = 0;
        int iter = 0;

        // Display something on the screen, so that we can see that something happens
        if (FCTPparam.screen_on) {
            System.out.println("=============== DOING Evaluation based IRNLS ================");
            System.out.println("ITER  OBJ (before LS)  OBJ (after LS)  BEST_OBJ");
        }

        // Save the initial solution as both the "current" and incumbent solution
        FCTPsol best_sol = new FCTPsol(solution);
        FCTPsol cur_sol = new FCTPsol(solution);
        do {
            iter++;
            num_fail++;

            if (FCTPparam.screen_on) System.out.format("%4d%17.2f", iter, solution.totalCost);

            // Improve solution using RNLS
            LS_first_acc();
            if (solution.totalCost < best_sol.totalCost) {
                best_sol.Overwrite(solution);
                num_fail = 0;
            }
            RNLS(50, 20);
            LS_first_acc();

            // The solution obtained after local search is "accepted", if within threshold 1.05 of currently stored sol
            boolean accept = (solution.totalCost < cur_sol.totalCost * 1.05);

            // Check if new overall best solution has been detected
            if (solution.totalCost < best_sol.totalCost) {
                best_sol.Overwrite(solution);
                num_fail = 0;
            }

            // Display objective value after local search and overall best value
            if (FCTPparam.screen_on) System.out.format("%16.2f%10.2f%n", solution.totalCost, best_sol.totalCost);

            // Reset to best solution every 10 iterations
            if (num_fail % 10 == 5) {
                solution.Overwrite(best_sol);
            } else if (accept)
                cur_sol.Overwrite(solution);
            else {
                solution.Overwrite(cur_sol);
            }
            // Introduce arcs into the basis according to evaluation measure
            Kicksolution_greedy(0, rc);

        } while (num_fail < max_runs);

        solution.Overwrite(best_sol);
        iterCount = iter;
    }

    /**
     * Takes array of solutions and calculates arc evaluation measures based on the average solution value
     * in solutions where the arc is present. Then, based on these evaluations an Evaluation based IRNLS i done
     * Afterwards, we reverse the evaluations and do the Evaluation based IRNLS again.
     *
     * @param init_sols Array of high quality solutions
     * @param max_runs  Maximum number of iterations without improvement to the objective value
     * @param v2        whether or not to use version 2 of IRNLS
     * @return Array of updated solution after intensification and diversification procedure.
     */
    public FCTPsol[] intensify_diversify(FCTPsol[] init_sols, int max_runs, boolean v2) {
        if (max_runs == 0) {
            max_runs = FCTPparam.max_no_imp;
        }
        int n_runs = init_sols.length;
        double[] counts = new double[narcs];
        double[] sol_avgs = new double[narcs];

        FCTPsol final_sol = new FCTPsol(solution);
        double worst = 0;

        // Get arc evaluations
        for (FCTPsol sol : init_sols) {
            // Store best solution
            if (sol.totalCost < final_sol.totalCost) {
                final_sol.Overwrite(sol);
            }

            if (sol.totalCost > worst) worst = sol.totalCost;

            for (int i = 0; i < narcs; i++) {
                if (sol.flow[i] > 0) {
                    counts[i]++;
                    sol_avgs[i] += sol.totalCost;
                }
            }

        }

        double[] sol_avgs_reversed = new double[narcs];

        // Calculate final arc evaluations
        for (int i = 0; i < narcs; i++) {
            if (counts[i] > 0) {
                sol_avgs_reversed[i] = worst - sol_avgs[i] / counts[i] + 1.0; //
                sol_avgs[i] = (sol_avgs[i] / counts[i]); //reverse greedy
            } else {
                sol_avgs_reversed[i] = 1.0;
                sol_avgs[i] = worst * 5;
            }
        }

        // Get collection which arcs can be drawn from according to arc evaluations
        RandomCollection<Integer> rc_intensify = get_random_collection(sol_avgs, true);
        RandomCollection<Integer> rc_diversify = get_random_collection(sol_avgs_reversed, true);

        FCTPsol[] new_sols = new FCTPsol[n_runs]; //To store solutions for statistics
        FCTPsol[] new_sols2 = new FCTPsol[n_runs]; //To store solutions for statistics

        // Now do actual intensification and diversification
        for (int i = 0; i < n_runs; i++) {
            solution.Overwrite(init_sols[i]);
            if (v2) Evaluation_based_IRNLS_v2(rc_intensify, max_runs);
            else {
                Evaluation_based_IRNLS(rc_intensify, max_runs);
            }
            new_sols[i] = new FCTPsol(solution);
            if (v2) Evaluation_based_IRNLS_v2(rc_diversify, max_runs / 4);
            else {
                Evaluation_based_IRNLS(rc_diversify, max_runs);
            }
            new_sols2[i] = new FCTPsol(solution);
            if (solution.totalCost < final_sol.totalCost) {
                final_sol.Overwrite(solution);
            }
            System.out.println("SOLUTION" + i + " cost " + init_sols[i].totalCost + "New cost before div " + new_sols[i].totalCost + " after div " + new_sols2[i].totalCost);
        }
        // Set solution to best one seen
        solution.Overwrite(final_sol);

        //Print and write statistics
        double total = 0;
        for (int i = 0; i < n_runs; i++) {
            try {
                fileWriter.write(init_sols[i].totalCost + " " + new_sols[i].totalCost + " " + new_sols2[i].totalCost + "\n");
                if (i == n_runs - 1) fileWriter.write(final_sol.totalCost + "\n");
            } catch (Exception exc) {
                System.out.println("Error: " + exc.getMessage());
            }
            total += new_sols2[i].totalCost;
        }

        System.out.println("AVG VALUE: " + total / n_runs);
        System.out.println("FINAL SOLUTION VALUE: " + solution.totalCost);

        return new_sols2;
    }

    /**
     * Iterated random neighbourhood local search with pool of mutations size 7
     *
     * @param max_runs Maximum number of iterations without new best objective value. If 0, it is set to
     *                 FCTPparam.max_no_imp
     */
    public void IRNLS(int max_runs) {
        int component;

        if (max_runs == 0) {
            max_runs = FCTPparam.max_no_imp;
        }

        int num_fail = 0; //Time since best solution found
        int num_cur_fail = 0; //Time since since last reset to cur sol
        int num_best_fail = 0; //Time since last reset to best sol
        int iter = 0;

        // Display something on the screen, so that we can see that something happens
        if (FCTPparam.screen_on) {
            System.out.println("=============== DOING IRNLS ================");
            System.out.println("ITER  OBJ (before LS)  OBJ (after LS)  BEST_OBJ");
        }

        // Save the initial solution as both the "current" and incumbent solution
        FCTPsol best_sol = new FCTPsol(solution);
        FCTPsol cur_sol = new FCTPsol(solution);

        do {
            iter++;
            if (FCTPparam.screen_on) System.out.format("%4d%17.2f", iter, solution.totalCost);

            // Improve solution using RNLS
            LS_first_acc();

            boolean accept = (solution.totalCost < cur_sol.totalCost || (num_cur_fail > 10 && solution.totalCost < cur_sol.totalCost * 1.05));

            if (solution.totalCost < best_sol.totalCost) {
                best_sol.Overwrite(solution);
                num_fail = 0;
                num_best_fail = 0;
            } else {
                num_fail++;
                num_best_fail++;
            }

            if (accept) {
                cur_sol.Overwrite(solution);
                num_cur_fail = 0;
            } else {
                solution.Overwrite(cur_sol);
                num_cur_fail++;
            }

            if (num_best_fail >= 30) {
                solution.Overwrite(best_sol);
                num_cur_fail = 0;
                num_best_fail = 0;
            }

            if (FCTPparam.screen_on) System.out.format("%16.2f%10.2f%n", solution.totalCost, best_sol.totalCost);
            if (iter % 2 == 0) RNLS(50, 20);
            else {
                component = randgen.nextInt(7); //Draw random element in [0,6] (pool of mutations)
                switch (component) {
                    case 0:
                        modified_cost_local_search(2, 0);
                        break;
                    case 1:
                        modified_cost_local_search(3, 0);
                        break;
                    case 2:
                        modified_cost_local_search(4, 0);
                        break;
                    case 3:
                        Kicksolution(0);
                        break;
                    case 4:
                        Kicksolution_greedy(0, rc1);
                        break;
                    case 5:
                        Kicksolution_greedy(0, rc2);
                        break;
                    case 6:
                        Kicksolution_greedy(0, rc3);
                        break;
                }
            }

        } while (num_fail < max_runs);
        solution.Overwrite(best_sol);
        System.out.format("%4d%17.2f%n", iter, solution.totalCost);
        iterCount = iter;
        try {
            fileWriter.write(solution.totalCost + "\n");
        } catch (Exception exc) {
            System.out.println("Error: " + exc.getMessage());
        }
    }

    /**
     * Another version of IRNLS
     *
     * @param max_runs Maximum number of iterations without new best objective value. If 0, it is set to
     *                 FCTPparam.max_no_imp
     */
    public void IRNLS_v2(int max_runs) {
        // Initialise parameter controlling when to reset the current solution
        if (max_runs == 0) {
            max_runs = FCTPparam.max_no_imp;
        }
        int num_fail = 0; //Time since best solution found
        int num_cur_fail = 0; //Time since since last reset to cur sol
        int num_best_fail = 0;  //Time since last reset to best sol
        int iter = 0;

        // Display something on the screen, so that we can see that something happens
        if (FCTPparam.screen_on) {
            System.out.println("=============== DOING IRNLS_v2 ================");
            System.out.println("ITER  OBJ (before LS)  OBJ (after LS)  BEST_OBJ");
        }

        // Save the initial solution as both the "current" and incumbent solution
        FCTPsol best_sol = new FCTPsol(solution);
        FCTPsol cur_sol = new FCTPsol(solution);

        do {
            iter++;
            if (FCTPparam.screen_on) System.out.format("%4d%17.2f", iter, solution.totalCost);

            LS_first_acc();

            if (FCTPparam.screen_on) System.out.format("%16.2f%10.2f%n", solution.totalCost, best_sol.totalCost);

            boolean accept = (solution.totalCost < cur_sol.totalCost || (num_cur_fail > 5 && solution.totalCost < cur_sol.totalCost * 1.03));

            if (solution.totalCost < best_sol.totalCost) {
                best_sol.Overwrite(solution);
                num_fail = 0;
                num_best_fail = 0;
            } else {
                num_fail++;
                num_best_fail++;
            }
            if (accept) {
                cur_sol.Overwrite(solution);
                num_cur_fail = 0;
            } else {
                solution.Overwrite(cur_sol);
                num_cur_fail++;
            }
            if (num_best_fail >= 30) {
                solution.Overwrite(best_sol);
                num_cur_fail = 0;
                num_best_fail = 0;
            }
            if (iter % 2 == 0) RNLS(50, 20);
            else {
                // Smaller pool of mutations, fixed order
                if (iter % 6 == 1) {
                    modified_cost_local_search(4, 0);
                } else if (iter % 6 == 3) {
                    modified_cost_local_search(5, 0);
                } else if (iter % 6 == 5) {
                    Kicksolution_greedy(0, rc3);
                }
            }
        } while (num_fail < max_runs);
        //Now set the library's internal solution to the bst one found above
        solution.Overwrite(best_sol);
        System.out.format("%4d%17.2f%n", iter, solution.totalCost);
        iterCount = iter;
    }

    /**
     * Evaluation based ILS, second version.
     *
     * @param rc       Random collection to draw arcs from
     * @param max_runs Maximal number of runs without improvement
     */
    public void Evaluation_based_IRNLS_v2(RandomCollection<Integer> rc, int max_runs) {
        // Initialise parameter controlling when to reset the current solution
        if (max_runs == 0) {
            max_runs = FCTPparam.max_no_imp;
        }
        int num_fail = 0; //Time since best solution found
        int num_cur_fail = 0; //Time since since last reset to cur sol
        int num_best_fail = 0;  //Time since last reset to best sol
        int iter = 0;

        // Display something on the screen, so that we can see that something happens
        if (FCTPparam.screen_on) {
            System.out.println("=============== DOING IRNLS_v2 ================");
            System.out.println("ITER  OBJ (before LS)  OBJ (after LS)  BEST_OBJ");
        }
        // Save the initial solution as both the "current" and incumbent solution
        FCTPsol best_sol = new FCTPsol(solution);
        FCTPsol cur_sol = new FCTPsol(solution);

        do {
            iter++;
            if (FCTPparam.screen_on) System.out.format("%4d%17.2f", iter, solution.totalCost);

            LS_first_acc();

            if (FCTPparam.screen_on) System.out.format("%16.2f%10.2f%n", solution.totalCost, best_sol.totalCost);

            boolean accept = (solution.totalCost < cur_sol.totalCost || (num_cur_fail > 5 && solution.totalCost < cur_sol.totalCost * 1.03));

            if (solution.totalCost < best_sol.totalCost) {
                best_sol.Overwrite(solution);
                num_fail = 0;
                num_best_fail = 0;
            } else {
                num_fail++;
                num_best_fail++;
            }
            if (accept) {
                cur_sol.Overwrite(solution);
                num_cur_fail = 0;
            } else {
                solution.Overwrite(cur_sol);
                num_cur_fail++;
            }
            if (num_best_fail >= 30) {
                solution.Overwrite(best_sol);
                num_cur_fail = 0;
                num_best_fail = 0;
            }
            if (iter % 2 == 0) RNLS(50, 20);
            else {
                // Smaller pool of mutations, fixed order
                if (iter % 6 == 1) {
                    modified_cost_local_search(4, 0);
                } else {
                    Kicksolution_greedy(0, rc);
                }
            }
        } while (num_fail < max_runs);
        solution.Overwrite(best_sol);
        System.out.format("%4d%17.2f%n", iter, solution.totalCost);
        iterCount = iter;
    }

    /**
     * A population based iterated random neighbourhood local search
     *
     * @param population_sizes A list of decreasing population sizes, e.g. [100, 50, 10] will create 100
     *                         initial solutions with IRNLS, and then apply the population based procedure with these
     *                         100 as start, then take the best resulting 50 solutions, and finally the resulting best
     *                         10
     * @param max_runs         List of max runs for IRNLS in each step, e.g. [800, 1500, 2000]
     * @param v2               Whether or not to use v2 of IRNLS and Evaluation based IRNLS
     */
    public void PIRNLS(int[] population_sizes, int[] max_runs, boolean v2) {
        int num_runs = population_sizes.length;

        FCTPsol[][] populations = new FCTPsol[num_runs][]; //Allocate space for populations

        //Set individual population sizes from array
        for (int i = 0; i < num_runs; i++) {
            populations[i] = new FCTPsol[population_sizes[i]];
        }

        // Create initial population of good solutions with IRNLS
        for (int i = 0; i < population_sizes[0]; i++) {

            // Here you can adjust proportion of RandGreedy to LPheu
            if (i > 2 * population_sizes[0] / 3) RandGreedy(0.4);
            else {
                LPheu();
                if (i > population_sizes[0] / 5) {
                    Kicksolution((m + n - 1) / 4);
                    if (i > population_sizes[0] / 3) Kicksolution((m + n - 1) / 4);
                }
            }

            if (v2) IRNLS_v2(max_runs[0]);
            else IRNLS(max_runs[0]);

            populations[0][i] = new FCTPsol(solution);
        }

        // Improve population by intesification and then diversification
        FCTPsol[] current_population = intensify_diversify(populations[0], max_runs[0], v2);

        //Iteratively run population based IRNLS with decreasing population size
        for (int j = 1; j < num_runs; j++) {

            //Get population_sizes[j] best solutions from current population
            double[] costs = new double[population_sizes[j - 1]];
            for (int k = 0; k < population_sizes[j - 1]; k++) {
                costs[k] = -current_population[k].totalCost;
            }

            int[] indices = get_max_k_idx(costs, population_sizes[j]);

            // Try to improve these solutions before next population based search
            int i = 0;
            for (int idx : indices) {
                solution.Overwrite(current_population[idx]);
                if (v2) IRNLS_v2(max_runs[j]);
                else {
                    IRNLS(max_runs[j]);
                }
                populations[j][i] = new FCTPsol(solution);
                i++;
            }
            current_population = intensify_diversify(populations[j], max_runs[j], v2);
        }

        //Finally, try one last search for improvement
        System.out.println("Final search initialized");
        IRNLS(5000);

        System.out.println("Final sol after last search" + solution.totalCost);
    }

}
