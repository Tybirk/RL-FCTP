"""Run module for DQN on the FCTP

- Author: Peter Emil Tybirk
"""

import argparse

import gym
import torch
import torch.nn as nn
import torch.optim as optim

from algorithms.common.helper_functions import identity
from algorithms.common.networks.mlp import init_layer_uniform
from algorithms.dqn.agent import DQNAgent
from algorithms.dqn.linear import NoisyLinearConstructor
from algorithms.dqn.networks import IQNMLP

device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")

# hyper parameters
hyper_params = {
    "N_STEP": 20,
    "GAMMA": 0.98,  # 0.99
    "TAU": 5e-3,
    "BUFFER_SIZE": int(1e4),  # openai baselines: int(1e4)
    "BATCH_SIZE": 32,  # openai baselines: 32
    "LR_DQN": 1.5e-6,  # dueling: 6.25e-5, openai baselines: 1e-4
    "ADAM_EPS": 1e-8,  # rainbow: 1.5e-4, openai baselines: 1e-8
    "WEIGHT_DECAY": 0.0,  # this makes saturation in cnn weights
    "MAX_EPSILON": 1.0,
    "MIN_EPSILON": 0.01,  # openai baselines: 0.01
    "EPSILON_DECAY": 1e-6,  # openai baselines: 1e-7 / 1e-1
    "W_N_STEP": 1.0,
    "W_Q_REG": 0.0,
    "PER_ALPHA": 0.6,  # openai baselines: 0.6
    "PER_BETA": 0.4,
    "PER_EPS": 1e-6,
    "GRADIENT_CLIP": 10.0,  # dueling: 10.0
    "UPDATE_STARTS_FROM": int(1e4),  # openai baselines: int(1e4)
    "TRAIN_FREQ": 4,  # in openai baselines, train_freq = 4
    "MULTIPLE_LEARN": 1,
    # Distributional Q function
    "USE_DIST_Q": "IQN",
    "N_TAU_SAMPLES": 64,
    "N_TAU_PRIME_SAMPLES": 64,
    "N_QUANTILE_SAMPLES": 32,
    "QUANTILE_EMBEDDING_DIM": 64,
    "KAPPA": 1.0,
    # NoisyNet
    "USE_NOISY_NET": True,
    "STD_INIT": 0.5
}


def run(env: gym.Env, env_name: str, args: argparse.Namespace):
    """Run training or test.

    Args:
        env (gym.Env): openAI Gym environment with continuous action space
        env_name (str): Environment name of openAI Gym
        args (argparse.Namespace): arguments including training settings

    """

    def get_mlp_model():
        action_dim = 17  # env.action_space.n
        hidden_sizes = [256, 256, 128]

        # use noisy net
        if hyper_params["USE_NOISY_NET"]:
            linear_layer = NoisyLinearConstructor(hyper_params["STD_INIT"])
            init_fn = identity
            hyper_params["MAX_EPSILON"] = 0.0
            hyper_params["MIN_EPSILON"] = 0.0
        else:
            linear_layer = nn.Linear
            init_fn = init_layer_uniform

        fc_model = IQNMLP(
            input_size=75,
            output_size=action_dim,
            hidden_sizes=hidden_sizes,
            n_quantiles=hyper_params["N_QUANTILE_SAMPLES"],
            quantile_embedding_dim=hyper_params["QUANTILE_EMBEDDING_DIM"],
            linear_layer=linear_layer,
            init_fn=init_fn,
        ).to(device)

        # create a model
        return fc_model

    dqn = get_mlp_model()
    dqn_target = get_mlp_model()
    dqn_target.load_state_dict(dqn.state_dict())

    # create optimizer
    dqn_optim = optim.Adam(
        dqn.parameters(),
        lr=hyper_params["LR_DQN"],
        weight_decay=hyper_params["WEIGHT_DECAY"],
        eps=hyper_params["ADAM_EPS"],
    )

    # make tuples to create an agent
    models = (dqn, dqn_target)

    # create an agent
    agent = DQNAgent(env, args, hyper_params, models, dqn_optim)
    agent.env_name = env_name

    # run
    if args.test:
        agent.test()
    else:
        agent.train(pre_train_episodes=1)
