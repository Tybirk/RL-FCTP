# -*- coding: utf-8 -*-
"""
  Module for running n-step-a2c agent
- Author: Peter Emil Tybirk

"""

import argparse

import gym
import torch
import torch.optim as optim

from algorithms.a2c.n_step_agent import A2CAgent
from algorithms.common.networks.mlp import MLP, CategoricalDist

device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")

# hyper parameters
hyper_params = {
    "GAMMA": 0.98,
    "LR_ACTOR": 2e-6,  
    "LR_CRITIC": 2e-6,  
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
        all_names = ['N3000',
                 'N3001',
                 'N3002',
                 'N3003',
                 'N3005',
                 'N3006',
                 'N3007',
                 'N3008',
                 'N300A',
                 'N300B',
                 'N300C',
                 'N300D',
                 'N300E',
                 'N3100',
                 'N3101',
                 'N3102',
                 'N3103',
                 'N3105',
                 'N3106',
                 'N3107',
                 'N3108',
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
                 'N3405',
                 'N3406',
                 'N3407',
                 'N3408',
                 'N340A',
                 'N340B',
                 'N340C',
                 'N340D',
                 'N340E',
                 'N3500',
                 'N3501',
                 'N3502',
                 'N3503',
                 'N3505',
                 'N3506',
                 'N3507',
                 'N3508',
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
                 'N360A',
                 'N360B',
                 'N360C',
                 'N360D',
                 'N360E',
                 'N3700',
                 'N3701',
                 'N3702',
                 'N3703',
                 'N3705',
                 'N3706',
                 'N3707',
                 'N3708',
                 'N370A',
                 'N370B',
                 'N370C',
                 'N370D',
                 'N370E']
        
        for name in names:
            agent.test(name='Glover/'+name+'.FCTP')

    else:
        agent.train()
