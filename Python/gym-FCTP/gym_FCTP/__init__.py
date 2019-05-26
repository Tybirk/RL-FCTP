from gym.envs.registration import register

register(
    id='FCTP-v1',
    entry_point='gym_FCTP.envs:FCTPEnv',
)

