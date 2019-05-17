from gym.envs.registration import register

register(
    id='FCTP-v0',
    entry_point='gym_FCTP.envs:FCTPEnv',
)

