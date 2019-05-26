# -*- coding: utf-8 -*-
"""Run module random agent

- Author: Peter Emil Tybirk
"""

import argparse

import gym
import torch
import torch.optim as optim

from algorithms.random_agent import Random_Agent

# hyper parameters
hyper_params = {}


def run(env: gym.Env, env_name: str, args: argparse.Namespace, state_dim=75, action_dim=17):
    """Run training or test.

    Args:
        env (gym.Env): openAI Gym environment with continuous action space
        args (argparse.Namespace): arguments including training settings
        state_dim (int): dimension of states - unused
        action_dim (int): dimension of actions - unused

    """

    # create an agent
    agent = Random_Agent(env, args)

    # run
    if args.test:
        names = ['N3000',
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
            agent.test(name='Glover/' + name + '.FCTP')

    else:
        agent.train()
