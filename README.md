## Reinforcement learning for the fixed charge transportation problem

## Contents

* [Welcome!](https://github.com/Tybirk/RL-FCTP#welcome)
* [Algorithms](https://github.com/medipixel/rl_algorithms#algorithms)
* [Performance](https://github.com/medipixel/rl_algorithms#performance)
* [Getting Started](https://github.com/medipixel/rl_algorithms#getting-started)
* [References](https://github.com/medipixel/rl_algorithms#references)


## Welcome!
This repository contains Java classes to represent instances of the fixed charge transportation problem (FCTP), heuristic algorithms to find solutions to the FCTP and code which enables the utilization of reinforcement learning methods to solve the FCTP. We owe most of the code for manipulating solutions to the FCTP to Andreas Klose http://home.math.au.dk/aklose/FCTP/, apart from the added classes PEheur and RL_composite. For the reinforcement learning part, we extended the framework build by Medipixel (https://github.com/medipixel/rl_algorithms/blob/master/README.md) to handle the fixed charge transportation problem, as well as building an $n$-step A2C agent. 

## Algorithms

## Performance

## Getting started

### Prerequisites
* This repository is tested on [Anaconda](https://www.anaconda.com/distribution/) virtual environment with python 3.6.1+
    ```
    $ conda create -n rl_algorithms python=3.6.1
    $ conda activate rl_algorithms
    ```
* In order to run Mujoco environments (e.g. `Reacher-v2`), you need to acquire [Mujoco license](https://www.roboti.us/license.html).

#### Installation
First, clone the repository.
```
git clone https://github.com/medipixel/rl_algorithms.git
cd rl_algorithms
```
Secondly, install packages required to execute the code. Just type:
```
make dep
```

###### For developers
You need to type the additional command which configures formatting and linting settings. It automatically runs formatting and linting when you commit the code.

```
make dev
```

After having done `make dev`, you can validate the code by the following commands.
```
make format  # for formatting
make test  # for linting
```

#### Usages
You can train or test `algorithm` on `env_name` if `examples/env_name/algorithm.py` exists. (`examples/env_name/algorithm.py` contains hyper-parameters and details of networks.)
```
python run_env_name.py --algo algorithm
``` 

e.g. running soft actor-critic on LunarLanderContinuous-v2.
```
python run_lunarlander_continuous_v2.py --algo sac <other-options>
```

e.g. running a custom agent, **if you have written your own example**: `examples/env_name/ddpg-custom.py`.
```
python run_env_name.py --algo ddpg-custom
```
You will see the agent run with hyper parameter and model settings you configured.

#### Arguments for run-files

In addition, there are various argument settings for running algorithms. If you check the options to run file you should command 
```
python <run-file> -h
```
- `--test`
    - Start test mode (no training).
- `--off-render`
    - Turn off rendering.
- `--log`
    - Turn on logging using [W&B](https://www.wandb.com/).
- `--seed <int>`
    - Set random seed.
- `--save-period <int>`
    - Set saving period of model and optimizer parameters.
- `--max-episode-steps <int>`
    - Set maximum episode step number of the environment. If the number is less than or equal to 0, it uses the default maximum step number of the environment.
- `--episode-num <int>`
    - Set the number of episodes for training.
- `--render-after <int>`
    - Start rendering after the number of episodes.
- `--load-from <save-file-path>`
    - Load the saved models and optimizers at the beginning.

#### W&B for logging
We use [W&B](https://www.wandb.com/) for logging of network parameters and others. For logging, please follow the steps below after requirement installation:

>0. Create a [wandb](https://www.wandb.com/) account
>1. Check your **API key** in settings, and login wandb on your terminal: `$ wandb login API_KEY`
>2. Initialize wandb: `$ wandb init`

For more details, read [W&B tutorial](https://docs.wandb.com/docs/started.html).

## References
http://home.math.au.dk/aklose/FCTP/javadoc/
https://github.com/medipixel/rl_algorithms/blob/master/README.md



