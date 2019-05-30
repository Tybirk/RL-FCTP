import gym
from gym import error, spaces, utils
from gym.utils import seeding
import os
import numpy as np
import jnius_config
from collections import deque
import random

java_path = '/home/tybirk/Desktop/Thesis_Code/Java'
os.chdir(java_path)  # Unfortunately seems necessary for Pyjnius to work, means logging files are in Java folder
fctp_env_path = '/home/tybirk/Desktop/Thesis_Code/Python/gym-FCTP/gym_FCTP/envs/'

"""
Establish connection to java code via pyjnius - set path above!
"""
try:
    jnius_config.set_classpath('./', java_path)
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
        # Names - notice that we omit N3606 and N3706 due to bugs with LP heuristic
        names = np.genfromtxt(fctp_env_path + 'instance_names.txt', dtype='str')
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
        self.success_last_20 = deque(maxlen=20)  # Keep track of successes of last 20 actions
        self.action_history = deque(maxlen=20)  # Keep track of last 20 actions
        self.n_runs = 0
        self.best_sol_val = 0
        self.best_sol = None  # Java FCTPsol object
        self.new_best = False
        self.no_new_best_count = 0
        self.action_counts = [0] * self.n_actions

        self.outfile = open("action_log.txt", "w+")
        self.outfile.close()

        # LP Upper bounds corresponding to instance names
        self.upper_bounds = np.loadtxt(fctp_env_path + 'upper_bounds.txt')

        # Best objectives obtained by Buson et al. (not true lower bounds!), alternative my best results
        # self.lower_bounds = np.loadtxt(fctp_env_path + 'buson_results.txt')
        self.lower_bounds = np.loadtxt(fctp_env_path + 'my_results.txt')

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

    def get_solution_features(self, instance, old_features, upper_bound, lower_bound):
        """
        Updates solution features based on current state of instance, old features and lower and upper bounds.
        :param instance: Java class RL_composite type with solution information etc.
        :param old_features: Features from latest timestep
        :param upper_bound: Upper bound on solution value for this instance
        :param lower_bound: Lower bound on solution value for this instance (not necesarily true lower bound)
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

    def IRNLS(self, max_no_improve, name):
        """
        A generator function allowing the agent to start of training by following the IRNLS policy

        :param max_no_improve: Maximum number of iterations without improvement before termination
        :param name:  Instance name, e.g. 'N3004'
        :return:
        """

        self.name = name
        self.instance = autoclass(java_path + 'RL_composite')(self.name)
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
                    num_cur_fail > 5 and self.instance.solution.totalCost < cur_sol * 1.05):
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


def get_relative_performance(current_value, start_value, lower_bound):
    return 1 - (current_value - lower_bound) / (start_value - lower_bound)
