# -*- coding: utf-8 -*-
"""Random agent for episodic tasks in OpenAI Gym.
Author: Peter Emil Tybirk
"""

import argparse
import os
from typing import Tuple

import gym
import numpy as np
import wandb
from collections import deque

from algorithms.common.abstract.agent import Agent


class Random_Agent(Agent):

    def __init__(
            self,
            env: gym.Env,
            args: argparse.Namespace,
    ):
        """Initialization.

        Args:
            env (gym.Env): openAI Gym environment
            args (argparse.Namespace): arguments including hyperparameters and training settings

        """
        Agent.__init__(self, env, args)
        self.transition = []

    def select_action(self, state: np.ndarray):
        """Select an action from the input space."""
        return np.random.randint(17)

    def step(self, action) -> Tuple[np.ndarray, np.float64, bool]:
        """Take an action and return the response of the env."""
        self.episode_step += 1
        next_state, reward, done, _ = self.env.step(action)

        if not self.args.test:
            done_bool = done
            if self.episode_step == self.args.max_episode_steps:
                done_bool = False
            self.transition.extend([next_state, reward, done_bool])

        return next_state, reward, done

    def load_params(self):
        pass

    def save_params(self):
        pass

    def update_model(self):
        pass

    def write_log(self, i: int, score):

        print(
            "[INFO] episode %d\tepisode step: %d\ttotal score: %.4f\n"
            % (i, self.episode_step, score)
        )

        if self.args.log:
            wandb.log(
                {
                    "score": score,
                }
            )

    def train(self):
        """Train the agent."""
        # logger
        if self.args.log:
            wandb.init(project=self.args.wandb_project)
            # wandb.watch([self.actor, self.critic], log="parameters")

        for self.i_episode in range(1, self.args.episode_num + 1):
            state = self.env.reset()
            done = False
            score = 0
            self.episode_step = 0

            while not done:
                self.episode_step += 1
                if self.args.render and self.i_episode >= self.args.render_after:
                    self.env.render()

                action = self.select_action(state)
                next_state, reward, done = self.step(action)

                state = next_state
                score += reward

            self.write_log(self.i_episode, score)
        # termination
        self.env.close()
