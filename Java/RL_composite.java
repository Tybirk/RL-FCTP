import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;


/**
 * A bridge for RL agents to invoke actions on instances of the FCTP
 *
 * @author  Peter Emil Tybirk
 * @version 29/05/2019
 */

public class RL_composite extends FCTPheur {
    public FCTPsol best_sol;
    public FCTPsol cur_sol;

    public RL_composite(String fname) throws Exception {
        super( fname );
        if (randgen.nextBoolean()) LPheu();
        else RandGreedy(0.3);
        Kicksolution((m+n-1)/2);

        this.best_sol = new FCTPsol(solution);
        this.cur_sol = new FCTPsol(solution);
    }

    /**
     * Method for invoking an action to mutate a feasible solution
     * @param action Integer indicating which action to take
     * @param two_component Whether or not to use two component actions
     */
    public void do_action(int action, boolean two_component){
        if (two_component){
            switch ( action )
            {
                case 0: RNLS(50, 20); LS_first_acc(); break;
                case 1: RNLS(50, 20); LS_best_acc(); break;
                case 2: RNLS(50, 10); LS_first_acc(); break;
                case 3: Kicksolution(0); LS_first_acc(); break;
                case 4: Kicksolution(0); LS_best_acc(); break;
                case 5: Kicksolution_greedy(0, rc1); LS_first_acc(); break;
                case 6: Kicksolution_greedy(0, rc2); LS_first_acc(); break;
                case 7: Kicksolution_greedy(0, rc3); LS_first_acc(); break;
                case 8: solution.Overwrite(best_sol);  RNLS(50, 20); LS_first_acc(); break;
                case 9: solution.Overwrite(best_sol); Kicksolution_greedy(0, rc2); LS_first_acc();  break;
                case 10: solution.Overwrite(best_sol); Kicksolution_greedy(0, rc3); LS_first_acc(); break;
                case 11: modified_cost_local_search(5,0); LS_first_acc(); break;
                case 12: modified_cost_local_search(1,0); LS_first_acc(); break;
                case 13: modified_cost_local_search(2,0); LS_first_acc(); break;
                case 14: modified_cost_local_search(3,0); LS_first_acc(); break;
                case 15: modified_cost_local_search(4,0); LS_first_acc(); break;
                case 16: modified_cost_local_search(4,0); LS_best_acc(); break;
            }
        }
        else{
            switch ( action )
            {
                case 0: LS_first_acc();  break;
                case 1: LS_best_acc(); break;
                case 2: RNLS(50, 20);  break; 
                case 3: RNLS((m+n-1)/5, 20); break;
                case 4: Kicksolution(0);  break;
                case 5: Kicksolution_greedy(0, rc1); break;  
                case 6: Kicksolution_greedy(0, rc2); break;
                case 7: Kicksolution_greedy(0, rc3); break;
                case 8: solution.Overwrite(best_sol); break; 
                case 9: cur_sol.Overwrite(solution); break; //Store solution
                case 10: solution.Overwrite(cur_sol); break; //Go back to stored solution
                case 11: RTR_move(solution.totalCost); break;
                case 12: modified_cost_local_search(1,0); break;
                case 13: modified_cost_local_search(2,0); break; 
                case 14: modified_cost_local_search(3,0); break; 
                case 15: modified_cost_local_search(4,0); break;
                case 16: modified_cost_local_search(5,0); break;
            }
            }

        if(solution.totalCost < best_sol.totalCost){
            best_sol.Overwrite(solution);
        }
    }

}
