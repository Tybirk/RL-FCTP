## Reinforcement learning for the fixed charge transportation problem

## Contents

* [Welcome!](https://github.com/Tybirk/RL-FCTP#Welcome!)
* [Getting Started](https://github.com/Tybirk/RL-FCTP#Getting-started)
* [References](https://github.com/Tybirk/RL-FCTP#References)


## Welcome!
This repository contains Java classes to represent instances of the fixed charge transportation problem (FCTP), heuristic algorithms to find solutions to the FCTP and a framework which enables the utilization of reinforcement learning methods to solve instances of the FCTP. We owe most of the code for manipulating solutions to the FCTP to Andreas Klose (http://home.math.au.dk/aklose/FCTP/), apart from the added classes PEheur and RL_composite. For the reinforcement learning part, we extended the framework build by Medipixel (https://github.com/medipixel/rl_algorithms/blob/master/README.md) to handle the fixed charge transportation problem by customizing a gym environment.


## Getting started

### Prerequisites
* This repository is tested on [Anaconda](https://www.anaconda.com/distribution/) virtual environment with Python 3.6.1+
    ```
    $ conda create -n rl_algorithms python=3.6.1
    $ conda activate rl_algorithms
    ```

### Installation
First, clone the repository.
```
git clone https://github.com/Tybirk/RL-FCTP.git
```
Secondly, install packages required to execute the code. Just type:
```
pip install -r requirements.txt
```

Thirdly, install the custom FCTP environment
```
cd Python/gym-FCTP
pip install -e .
```

### Usages

#### Java part
You can run configure algorithm choice and some hyperparameters in the file FCTPheur.ini. 
You run the chosen algorithm on an instance from a file, e.g. Glover/N3004.FCTP by calling
```
java FCTPmain Glover/N3004.FCTP
``` 
In the file FCTPheur.java, you can inspect more closely which methods are then called for what hyperparameter setting. 

#### Reinforcement learning part
In the folder gym-FCTP/gym_FCTP/envs, the implementation of the FCTP environment can be found. The actions are implemented in Java and called in Python with the help of Pyjnius. 


You can train or test `algorithm` on `env_name` if `Python/rl_algorithms/env_name/algorithm.py` exists. (`Python/rl_algorithms/env_name/algorithm.py` contains hyper-parameters and details of networks.) As of now, there is just one environment, namely fctp_v1.

```
cd Python/rl_algorithms
python run_fctp_v1.py --algo dqn <other-options>
python run_fctp_v1.py --algo n_step_a2c <other-options>
``` 

The hyperparameters for each run can be modified in the folder Python/rl_algorithms/fctp_v1

##### Arguments for run-files

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
