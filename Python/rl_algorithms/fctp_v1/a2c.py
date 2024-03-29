# -*- coding: utf-8 -*-
"""Run module for A2C on FCTp

- Author: Peter Emil Tybirk
"""

import argparse

import gym
import torch
import torch.optim as optim

from algorithms.a2c.agent import A2CAgent
from algorithms.common.networks.mlp import MLP, CategoricalDist

device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")

# hyper parameters
hyper_params = {
    "GAMMA": 0.98,
    "LR_ACTOR": 5e-5,
    "LR_CRITIC": 5e-5,
    "GRADIENT_CLIP_AC": 10,
    "GRADIENT_CLIP_CR": 10,
    "W_ENTROPY": 1e-6,
    "WEIGHT_DECAY": 1e-6,
}


def run(env: gym.Env, env_name: str, args: argparse.Namespace, state_dim=75, action_dim=17):
    """Run training or test.

    Args:
        env (gym.Env): openAI Gym environment with continuous action space
        args (argparse.Namespace): arguments including training settings
        state_dim (int): dimension of states
        action_dim (int): dimension of actions

    """
    # create models
    actor = CategoricalDist(
        input_size=state_dim, output_size=action_dim, hidden_sizes=[256, 256, 128]
    ).to(device)

    critic = MLP(input_size=state_dim, output_size=1, hidden_sizes=[256, 256, 128]).to(
        device
    )

    # create optimizer
    actor_optim = optim.Adam(
        actor.parameters(),
        lr=hyper_params["LR_ACTOR"],
        weight_decay=hyper_params["WEIGHT_DECAY"],
    )

    critic_optim = optim.Adam(
        critic.parameters(),
        lr=hyper_params["LR_CRITIC"],
        weight_decay=hyper_params["WEIGHT_DECAY"],
    )

    # make tuples to create an agent
    models = (actor, critic)
    optims = (actor_optim, critic_optim)

    # create an agent
    agent = A2CAgent(env, args, hyper_params, models, optims)

    # run
    if args.test:
        agent.test(name='Glover/N3004.FCTP')
    else:
        agent.train()
