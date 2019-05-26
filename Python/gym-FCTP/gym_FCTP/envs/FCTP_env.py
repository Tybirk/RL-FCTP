import gym
from gym import error, spaces, utils
from gym.utils import seeding
import os
import numpy as np
import jnius_config
from collections import deque
import random

path = '/home/tybirk/Desktop/Thesis_Code/Java'  # Change to directory containing the java code
os.chdir(path)


"""
Establish connection to java code via pyjnius
"""
try:
    jnius_config.set_classpath('./', path)
except ValueError:
    print("Value error when setting classpath, proceed anyway")  # Error when path already set

try:
    from jnius import autoclass
except KeyError:
    os.environ['JAVA_HOME'] = "/home/tybirk/anaconda3"  # Set your java home
    from jnius import autoclass





class FCTPEnv(gym.Env):
    metadata = {'render.modes': ['human']}
    reward_range = (-50, 500)  # Set range which includes all possible rewards

    def __init__(self):
        names = ['N3000',
                 'N3001',
                 'N3002',
                 'N3003',
                 'N3004',
                 'N3005',
                 'N3006',
                 'N3007',
                 'N3008',
                 'N3009',
                 'N300A',
                 'N300B',
                 'N300C',
                 'N300D',
                 'N300E',
                 'N3100',
                 'N3101',
                 'N3102',
                 'N3103',
                 'N3104',
                 'N3105',
                 'N3106',
                 'N3107',
                 'N3108',
                 'N3109',
                 'N310A',
                 'N310B',
                 'N310C',
                 'N310D',
                 'N310E',
                 'N3200',
                 'N3201',
                 'N3202',
                 'N3203',
                 'N3204',
                 'N3205',
                 'N3206',
                 'N3207',
                 'N3208',
                 'N3209',
                 'N320A',
                 'N320B',
                 'N320C',
                 'N320D',
                 'N320E',
                 'N3300',
                 'N3301',
                 'N3302',
                 'N3303',
                 'N3304',
                 'N3305',
                 'N3306',
                 'N3307',
                 'N3308',
                 'N3309',
                 'N330A',
                 'N330B',
                 'N330C',
                 'N330D',
                 'N330E',
                 'N3400',
                 'N3401',
                 'N3402',
                 'N3403',
                 'N3404',
                 'N3405',
                 'N3406',
                 'N3407',
                 'N3408',
                 'N3409',
                 'N340A',
                 'N340B',
                 'N340C',
                 'N340D',
                 'N340E',
                 'N3500',
                 'N3501',
                 'N3502',
                 'N3503',
                 'N3504',
                 'N3505',
                 'N3506',
                 'N3507',
                 'N3508',
                 'N3509',
                 'N350A',
                 'N350B',
                 'N350C',
                 'N350D',
                 'N350E',
                 'N3600',
                 'N3601',
                 'N3602',
                 'N3603',
                 'N3604',
                 'N3605',
                 'N3607',
                 'N3608',
                 'N3609',
                 'N360A',
                 'N360B',
                 'N360C',
                 'N360D',
                 'N360E',
                 'N3700',
                 'N3701',
                 'N3702',
                 'N3703',
                 'N3704',
                 'N3705',
                 'N3707',
                 'N3708',
                 'N3709',
                 'N370A',
                 'N370B',
                 'N370C',
                 'N370D',
                 'N370E']
        self.instance_names = ['Glover/' + name + '.FCTP' for name in names]  # Set to correct folder

        self.idx_dict = {x: i for i, x in enumerate(self.instance_names)}  # Associate each instance with an idx
        self.feature_names = ('Current_performance', 'Best_performance', 'Relative number of arcs with positive flow',
                              'Succes latest action', 'fixed cost percent', 'relative number of fails',
                              'number of successes last 20 actions')

        self.n_actions = 17
        self.history_depth = 3  # How many actions to remember in state space
        self.comp = False  # Boolean indicating whether to use two component action type
        self.n_features = len(self.feature_names) + self.n_actions * self.history_depth
        self.n_instances = len(self.instance_names)
        self.name = ""  # Name of current instance
        self.max_no_improve = 800
        self.instance_idx = None
        self.instance = None  # Java class for instance/solution

        # Arrays to remember actions, needs configuration for history depth beyond 3
        self.action_vec = [0] * self.n_actions
        self.old_action_vec = [0] * self.n_actions
        self.older_action_vec = [0] * self.n_actions

        self.actions_taken = []
        self.step_number = 0
        self.success_last_20 = deque(maxlen=20)  # Keep track of succeses of last 20 actions
        self.action_history = deque(maxlen=20)  # Keep track of last 20 actions
        self.n_runs = 0
        self.best_sol_val = 0
        self.best_sol = None  # Java FCTPsol object
        self.new_best = False
        self.no_new_best_count = 0
        self.action_counts = [0] * self.n_actions

        self.outfile = open("action_log.txt", "w+")
        self.outfile.close()

        self.upper_bounds = [168777.0, 168146.0, 168926.0, 169410.0, 168257.0, 168998.0, 168212.0, 168680.0, 166768.0,
                             168035.0, 168147.0, 169697.0, 166707.0, 167358.0, 170632.0, 180541.0, 180510.0, 181183.0,
                             180981.0, 181029.0, 181098.0, 180193.0, 180976.0, 180021.0, 179277.0, 180696.0, 182173.0,
                             178894.0, 179621.0, 182210.0, 203848.0, 205221.0, 205921.0, 204263.0, 205614.0, 205554.0,
                             203242.0, 203930.0, 204119.0, 201875.0, 206008.0, 206877.0, 202551.0, 203539.0, 205445.0,
                             250121.0, 249342.0, 252250.0, 245753.0, 251187.0, 251632.0, 247395.0, 249280.0, 242916.0,
                             248415.0, 252601.0, 253498.0, 247658.0, 247888.0, 249883.0, 329655.0, 328335.0, 334581.0,
                             324310.0, 338256.0, 336314.0, 324346.0, 324088.0, 325279.0, 330083.0, 335221.0, 337374.0,
                             333088.0, 332262.0, 332202.0, 484798.0, 478337.0, 481167.0, 470527.0, 487687.0, 492921.0,
                             475828.0, 469898.0, 477325.0, 485149.0, 489043.0, 496293.0, 498369.0, 498369.0, 484315.0,
                             767436.0, 764958.0, 764492.0, 760602.0, 777690.0, 775318.0, 754795.0, 764236.0, 771304.0,
                             791767.0, 785277.0, 777567.0, 777567.0, 779153.0, 1349313.0, 1315384.0, 1325421.0,
                             1337367.0, 1331474.0, 1350880.0, 1302123.0, 1329854.0, 1323512.0, 1368217.0, 1384662.0,
                             1355945.0, 1355945.0, 1317375.0]  # Upper bounds corresponding to instance names

        self.lower_bounds = [167737,
                             166473,
                             167541,
                             168198,
                             167074,
                             167387,
                             165562,
                             166980,
                             165384,
                             166882,
                             167192,
                             168327,
                             165123,
                             166062,
                             169177,
                             178299,
                             177152,
                             178292,
                             178489,
                             178753,
                             177726,
                             176161,
                             177299,
                             175810,
                             177089,
                             178558,
                             179173,
                             175298,
                             176941,
                             179351,
                             198517,
                             197898,
                             199208,
                             198252,
                             200059,
                             197769,
                             196071,
                             196937,
                             195639,
                             197086,
                             199377,
                             200057,
                             195019,
                             197085,
                             199328,
                             237220,
                             236557,
                             237746,
                             235481,
                             239606,
                             236106,
                             234021,
                             233777,
                             233312,
                             236227,
                             239517,
                             238615,
                             232750,
                             234418,
                             236167,
                             307279,
                             306404,
                             310610,
                             302880,
                             311985,
                             307561,
                             304424,
                             302817,
                             305135,
                             308254,
                             312607,
                             309900,
                             304055,
                             304869,
                             307315,
                             440880,
                             438005,
                             444003,
                             431410,
                             449277,
                             439854,
                             437289,
                             432158,
                             438444,
                             443750,
                             449041,
                             444519,
                             440203,
                             440734,
                             443741,
                             698688,
                             694071,
                             691340,
                             684562,
                             706189,
                             692928,
                             681441,
                             693838,
                             691868,
                             707272,
                             703234,
                             700759,
                             700759,
                             693381,
                             1197613,
                             1186310,
                             1172668,
                             1170230,
                             1203084,
                             1186342,
                             1169810,
                             1192859,
                             1186592,
                             1216295,
                             1199189,
                             1198455,
                             1198455,
                             1176172]  # Buson lower bounds corresponding to instance names

        self.state = None

        high = np.array([200, 200, 1, 1, 1, 1] + [1] * self.n_actions * self.history_depth)  # obs space upper bounds
        low = np.array([-10000, -10000, 0, 0, 0, 0] +
                       [0] * self.n_actions * self.history_depth)  # obs space lower bounds

        self.action_space = spaces.Discrete(self.n_actions)
        self.observation_space = spaces.Box(low, high, dtype=np.float32)

    def set_comp(self, is_comp):
        """
        Set action type
        :param is_comp: True means two component actions, False is single
        """
        self.comp = is_comp

    def step(self, action, return_action=False):
        """
        Step in the environment, calculate next state and reward based on action
        :param action: Integer indicating which action to take
        :param return_action: Whether or not to include action in return
        :return: array state, float reward, boolean done, dictionary with information, (optional) int action
        """
        # Store information
        self.actions_taken.append(action)
        self.step_number += 1
        self.older_action_vec = self.old_action_vec[:]
        self.old_action_vec = self.action_vec[:]
        self.action_vec = [0] * self.n_actions
        self.action_vec[action] = 1
        self.action_history.append(self.action_vec[:])
        self.action_counts[action] += 1
        self.new_best = False
        self.no_new_best_count += 1
        done = False

        self.instance.do_action(action, self.comp)  # Call to java class, alters solution

        updated_features = self.get_solution_features(self.instance, old_features=self.state,
                                                      upper_bound=self.upper_bounds[self.instance_idx],
                                                      lower_bound=self.lower_bounds[self.instance_idx])

        next_state = np.concatenate(
            (
                np.array(updated_features, dtype=np.float64), self.no_new_best_count / self.max_no_improve,
                self.action_vec,
                self.old_action_vec, self.older_action_vec,
                np.sum(self.action_history, axis=0)),
            axis=None)

        cost = self.instance.solution.totalCost

        if self.state[1] != 1:
            reward = (next_state[1] - self.state[1]) / abs((1 - self.state[1]))  # new_best - old_best / (1-old_best)
        else:
            reward = (next_state[1] - self.state[1]) / abs((1 - 0.9999))  # Avoid division by zero

        if reward > 0:
            self.new_best = True
            self.best_sol_val = self.instance.solution.totalCost
            self.best_sol = self.instance.best_sol
            self.no_new_best_count = 0

        self.state = next_state

        if self.no_new_best_count >= self.max_no_improve:
            done = True

        if return_action:
            return action, self.state, reward, done, {'cost': cost}

        return self.state, reward, done, {'cost': cost}

    def reset(self, name=None):
        """
        Resets the environment to a new random instance, instantiates new RL_composite java object
        :param name: Name of instance to reset to. If None, a random instance is chosen
        :return: State of reset problem
        """
        self.n_runs += 1

        # Write actions to file for analysis
        self.outfile = open("action_log.txt", "a+")  # append text
        action_list = [int(a) for a in self.actions_taken]
        self.outfile.write(f"{self.name} \n {self.best_sol_val} \n {action_list}")
        self.outfile.close()

        # Reset variables
        self.actions_taken = []
        self.succes_last_20 = deque(maxlen=20)
        self.action_history = deque(maxlen=20)
        self.step_number = 0
        self.no_new_best_count = 0
        self.action_vec = [0] * self.n_actions
        self.old_action_vec = [0] * self.n_actions
        self.older_action_vec = [0] * self.n_actions
        self.action_counts = [0] * self.n_actions
        self.action_vec[random.randint(0, self.n_actions - 1)] = 1  # Random initial action

        if not name:
            self.instance_idx = random.randint(0, self.n_instances - 1)
            self.name = self.instance_names[self.instance_idx]
        else:
            self.name = name
            self.instance_idx = self.idx_dict[name]
        print(self.name)
        self.instance = autoclass('RL_composite')(self.name)

        self.best_sol = self.instance.solution
        self.best_sol_val = self.best_sol.totalCost

        self.state = np.concatenate((np.array([0, 0, self.instance.solution.get_rel_pos_flow(), 0,
                                               self.instance.get_fcost_percent(), 0, 0]), self.action_vec,
                                     self.old_action_vec, self.older_action_vec, [0] * self.n_actions))

        return self.state

    def render(self, mode='human', close=False):
        """Print status messages to monitor training, overwrites method from gym.Env"""
        if self.new_best:
            print(self.instance.solution.totalCost)

        if self.step_number % 500 == 0 and self.step_number > 400:
            print(self.action_counts)

    def seed(self, x):
        random.seed(x)

    def IRNLS(self, max_no_improve, name):
        """
        A generator function allowing the agent to start of training by following the IRNLS policy

        :param max_no_improve: Maximum number of iterations without improvement before termination
        :param name:  Instance name, e.g. 'N3004'
        :return:
        """

        self.name = name
        self.instance = autoclass('RL_composite')(self.name)
        num_fail = 0
        num_cur_fail = 0
        num_cur_fail_2 = 0

        self.best_sol_val = self.instance.solution.totalCost
        cur_sol = self.instance.solution.totalCost

        while num_fail < max_no_improve:
            if random.random() > 0.5:
                yield self.step(0, return_action=True)  # first accept LS
            else:
                yield self.step(1, return_action=True)  # best accept LS

            num_fail += 1
            num_cur_fail += 1
            num_cur_fail_2 += 1

            if self.instance.solution.totalCost < cur_sol or (
                    num_cur_fail > 10 and self.instance.solution.totalCost < cur_sol * 1.05):
                yield self.step(9, return_action=True)
                cur_sol = self.instance.solution.totalCost
                num_cur_fail = 0
            else:
                yield self.step(10, return_action=True)

            if self.instance.solution.totalCost < self.best_sol_val:
                num_fail = 0
                num_cur_fail_2 = 0
                self.best_sol_val = self.instance.solution.totalCost

            if num_cur_fail_2 >= 30:
                yield self.step(8, return_action=True)
                num_cur_fail = 0
                num_cur_fail_2 = 0

            if random.random() > 0.5:
                yield self.step(2, return_action=True)  # RNLS
            else:
                action = random.choice([3, 4, 5, 6, 7, 11, 12, 13, 14, 15, 16])
                yield self.step(action, return_action=True)

    def intensify_diversify(self, results):
        """
        Call java method for population based intensification and diversification
        :param results: List of solutions, initial population
        :return:
        """
        self.instance.intensify_diversify(len(results), results)
        self.instance.IRNLS(2000)
        self.best_sol = self.instance.solution
        self.best_sol_val = self.instance.solution.totalCost

    def get_solution_features(self, instance, old_features, upper_bound, lower_bound):
        """
        Updates solution features based on current state of instance, old features and lower and upper bounds.
        :param instance: Java class RL_composite type with solution information etc.
        :param old_features: Features from latest timestep
        :param upper_bound: Upper bound on solution value for this instance
        :param lower_bound: Lower bound on solution value for this instance
        :return: Current performance, best performance, relative positive flow, success of latest action,
        percentage of costs which are fixed costs and number of last 20 actions which were successes.
        """
        perf = get_relative_performance(instance.solution.totalCost, upper_bound, lower_bound)
        best_perf = perf if perf > old_features[1] else old_features[1]
        rel_pos_flow = instance.solution.get_rel_pos_flow()  # Call to java method
        success_latest = perf > old_features[0]
        self.success_last_20.append(success_latest)
        fcost_perc = instance.get_fcost_percent()  # Call to java method

        return perf, best_perf, rel_pos_flow, success_latest, fcost_perc, sum(self.success_last_20)


def get_relative_performance(current_value, start_value, lower_bound):
    return 1 - (current_value - lower_bound) / (start_value - lower_bound)
