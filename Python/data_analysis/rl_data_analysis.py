#!/usr/bin/env python3
import os
import ast
import numpy as np
from scipy.signal import savgol_filter
import matplotlib.pyplot as plt
import seaborn as sns
from matplotlib import gridspec
from matplotlib import rc
import pandas as pd
from statsmodels.tsa.seasonal import seasonal_decompose


#path = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/new_runs_not_comp/'
#path = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/new_runs_not_comp/'
path = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/'
folders = ['20-step-a2c-1e-5-800']
#folders = ['10-step-a2c-1e-6', '20-step-a2c-5e-6', '10-step-a2c-5e-5', '20-step-dqn-5e-5', '1-step-a2c-1e-6', '10-step-a2c-5e-6', '20-step-a2c-1e-6', '20-step-dqn-1e-6', '1-step-a2c-5e-6', '10-step-dqn-5e-6', '20-step-dqn-5e-6', '1-step-a2c-5e-5', '20-step-a2c-5e-5', '10-step-dqn-1e-6', '10-step-dqn-5e-5']
import os
titles = ['20-step A2C, 5e-5']
#titles = ['10-step A2C, 1e-6', '20-step A2C, 5e-6', '10-step A2C, 5e-5',  '20-step DQN, 5e-5', '1-step A2C, 1e-6', '10-step A2C, 5e-6', '20-step A2C, 1e-6', '20-step DQN, 1e-6', '1-step A2C, 5e-6', '10-step DQN, 5e-6', '20-step DQN, 5e-6', '1-step A2C, 5e-5', '20-step A2C, 5e-5', '10-step DQN, 1e-6', '10-step DQN, 5e-5']

# traverse root directory, and list directories as dirs and files as files
"""
runs = []
for root, dirs, files in os.walk(path):
    for direc in dirs:
        for root, dirs, files in os.walk(path+'/'+direc):
            for file in files:
                if file == 'wandb-summary.json':
                    with open(path+'/'+direc + '/' + file) as f:
                        lines = f.readlines()
                        for line in lines:
                            if '_step' in line:
                                steps = line.split(':')[1]
                                steps = steps[1:]
                                if ',' in steps:
                                    steps = steps.split(',')[0]
                                if len(steps) > 0:
                                    steps = int(steps)
                                    if steps > 500:
                                        runs.append(direc)"""
                                        
result_dicts = []

for folder in folders:
    results_dict = {}
    folder_path = path + folder
    with open(folder_path + '/wandb-history.jsonl', 'r') as f:
        lines = f.readlines()
        results_dict = ast.literal_eval(lines[0])
        for key in results_dict:
            results_dict[key] = [results_dict[key]]
        for line in lines:
            line_dict = ast.literal_eval(line)
            for key in line_dict:
                if key in results_dict:
                    results_dict[key].append(line_dict[key])
    results_dict['folder_name'] =  folder
    result_dicts.append(results_dict.copy())


    

result_add = seasonal_decompose(result_dicts[0]['score'], model='additive', extrapolate_trend='freq', freq=50)
result_add.plot()


def plot_values(y, x=None, smoothing=None, ax = None):
    if not x:
        x = np.arange(len(y))
        
    def smooth(y, box_pts):
        box = np.ones(box_pts)/box_pts
        y_smooth = np.convolve(y, box, mode='same')
        return y_smooth

    if smoothing == 'savgol':
        y_smooth = savgol_filter(y, 7, 1)
        
    elif smoothing:
        y_smooth = smooth(y, 10)
    
    if ax:
        if smoothing is not None:
            ax.plot(x, y_smooth)
        else:
            ax.plot(x,y)
    else:
        if smoothing is not None:
            plt.plot(x, y_smooth)
        else:
            plt.plot(x,y)

fig, axs = plt.subplots(3,2, figsize=(5.5, 7.3), facecolor='w', edgecolor='k', sharey='col', sharex = True)
#fig.subplots_adjust(hspace = .5, wspace=.001)
line_x, line_y = [0,1000], [1.12, 1.12]
fs = 11
fname='/home/tybirk/Desktop/Speciale/FCTP/Plots/10-step-a2c.eps'

from matplotlib import rcParams
rcParams.update({'figure.autolayout': True})
current_dicts = [res for res in result_dicts if '10-step-a2c' in res['folder_name']]
#current_dicts[0], current_dicts[1] = current_dicts[1], current_dicts[0] 
#current_dicts[1], current_dicts[2] = current_dicts[2], current_dicts[1] 
for i, ax in enumerate(axs.ravel()):
    if i % 2 == 1:
        result_add = seasonal_decompose(current_dicts[i//2]['score'], model='additive', extrapolate_trend='freq', freq=50)
        ax.plot(line_x, line_y, '--')
        ax.plot(result_add.trend[:1000])
    else:
        #ax.set_yticks([0.5,1,1.5])        
        ax.plot(line_x, line_y, '--')
        plot_values(current_dicts[i//2]['score'][:1000], ax=ax)
    
    ax.set_title(current_dicts[i//2]['folder_name'][-4:], fontsize=fs)
    

fig.text(0.5, 0.00, 'Episode', ha='center', fontsize=fs)
fig.text(0.00, 0.5, 'Total reward', va='center', rotation='vertical', fontsize=fs)
plt.tight_layout()
plt.savefig(fname, format='eps', dpi=5000, bbox_inches='tight')
plt.show()
    

fs = 11
fig = plt.figure()
fig.set_size_inches(18, 18)
line_x, line_y = [0,1000], [1.48, 1.48]
gs = gridspec.GridSpec(15, 1) #, height_ratios=[1, 1, 2]) 


ax1 = plt.subplot(gs[0])
plt.plot(line_x, line_y)
ax1.set_yticks([1, 2])
ax1.set_title(result_dicts[0]['folder_name'], fontsize=fs)
plot_values(result_dicts[0]['score'][:1000], smoothing='savgol')
plt.setp(ax1.get_xticklabels(), visible=False)

for i, result_dict in enumerate(result_dicts[1:]):
    ax = plt.subplot(gs[i], sharex=ax1, sharey=ax1)
    ax.set_title(result_dict['folder_name'], fontsize=fs)
    #plt.grid()
    plt.plot(line_x, line_y)
    ax.set_yticks([1, 2])
    plot_values(result_dict['score'][:1000], ax=ax, smoothing='savgol')
    if i != len(result_dicts) - 2:
        plt.setp(ax.get_xticklabels(), visible=False)
        
plt.tight_layout()
fig.text(0.52, 0.07, 'Episode', ha='center', fontsize=fs)
fig.text(0.02, 0.5, 'Total reward', va='center', rotation='vertical', fontsize=fs)
plt.savefig(fname, format='eps', dpi=5000)







#for i, folder in enumerate(folders):
    #result_dicts[i]['folder_name'] = folder


def get_rl_results(path):
    import ast
    with open(path) as f:
        actions = []
        results_before = {}
        results_after = {}
        lines = f.readlines()
        for i, line in enumerate(lines):
            if 'Glover' in line and len(line) < 20:
                current_instance = line[7:12]
            if line[:9] == '[INFO] ep':
                if lines[i-1][:3] == 'ter':
                    if lines[i-2][0] != '[':
                        continue
                    action_list = ast.literal_eval(lines[i-2])
                    actions.append(action_list)
                else:
                    if lines[i-1][0] != '[':
                        continue
                    action_list = ast.literal_eval(lines[i-1])
                    actions.append(action_list)
            if '[INFO] test 99' in lines[i-1]:
                res = ast.literal_eval(line)
                results_before[current_instance] = res
            if 'FINAL RESULT:' in line:
                res = line.split('FCTP ')[1]
                results_after[current_instance] = int(float(res))
                
    return results_before, results_after, actions

results_before, results_after, actions = get_rl_results('/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/new_runs_not_comp/10-step-a2c-5e-5/output.log')

def compare_rl_instances_from_path(path, results=None):
    if not results:
        results_before, results_final, _ = get_rl_results(path)
    else:
        results_final = results
    names, results_buson = read_buson_results()
    common_names = sorted([name for name in results_final.keys() if name in names])
    differences = []
    for name in common_names:
        print(name, "Buson:", results_buson[name], "Mine:", results_final[name])
        differences.append(results_final[name] - results_buson[name])
    
    print(differences)
    print("best on ", sum(np.array(differences)<0), "out of", len(differences) )


def action_analysis(actions, k, fname):
    actions = np.array(actions)[0:1000:10]
    top_actions = []
    for a in actions:
        top_indices = (-a).argsort()[:k]
        top_actions.append(top_indices)
    y = np.array(top_actions).flatten()
    x = np.array([[i*10]*k for i in range(len(actions))]).flatten()
    labels = [i % k + 1 for i in range(k*len(actions))]
    df = pd.DataFrame(np.array([x,y,labels]).T, columns=('Episode', 'Action index', 'Top 5 actions' ))
    sns.set_style('ticks')
    sns.set_palette((sns.cubehelix_palette(k, start=2, rot=0, dark=0.1, light=0.9, reverse=True)) )# pastal,  muted, dark, deep, colorblind, bright, Reds
    the_plot = sns.pairplot(x_vars='Episode', y_vars='Action index', data=df, hue="Top 5 actions", size=4, aspect=1.2)
    the_plot.set(ylim=(0.17), yticks=np.arange(17))
    the_plot.savefig(fname, format='eps', dpi=5000)
    plt.show()
    
def action_analysis2(actions, k, fname, set_title=None):
    actions = np.array(actions)[0:1000:10]
    top_actions = []
    action_counts = []
    for i, a in enumerate(actions):
        top_indices = (-a).argsort()[:k]
        n_actions = actions[i][top_indices]
        action_counts.append(n_actions)
        top_actions.append(top_indices)
    y = np.array(top_actions).flatten()
    y += 1
    x = np.array([[i*10]*k for i in range(len(actions))]).flatten()
    action_totals = [np.sum(a) for a in actions]
    labels = [action_counts[i]/action_totals[i] for i in range(len(actions))]
    labels = [x for i in range(len(actions)) for x in labels[i] ]
    plt.scatter(x, y, alpha = 1, c = labels, cmap='Greens')
    #plt.clim(0,1)
    plt.yticks(range(1,18))
    plt.xlabel('Episode')
    plt.ylabel('Action index')
    plt.colorbar()
    if set_title:
        plt.title(set_title)
    plt.tight_layout()
    plt.savefig(fname, format='eps', dpi=1000)
    plt.show()
    """
    print(action_counts[-10:])
    print(top_indices[-10:])
    labels = [action_counts[i]/1000 for i in range(100)]
    labels = [x for i in range(100) for x in labels[i] ]
    print(labels[-10:])
    df = pd.DataFrame(np.array([x,y,labels]).T, columns=('Episode', 'Action index', 'Top 5 actions' ))
    sns.set_style('ticks')
    sns.set_palette((sns.cubehelix_palette(k, start=2, rot=0, dark=0.1, light=0.9, reverse=True)) )# pastal,  muted, dark, deep, colorblind, bright, Reds
    the_plot = sns.pairplot(x_vars='Episode', y_vars='Action index', data=df, hue="Top 5 actions", size=4, aspect=1.2)
    the_plot.set(ylim=(0.17), yticks=np.arange(17))
    the_plot.savefig(fname, format='eps', dpi=5000)
    plt.show()"""
    

results_before, results_after, actions = get_rl_results(path + folders[0] +'/output.log')
results_before, results_after, actions = get_rl_results('/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/new_runs_not_comp/' + '10-step-dqn-5e-5' + '/output.log')
action_analysis2(actions, 17, "/home/tybirk/Desktop/Speciale/FCTP/Plots/10-step-a2c-2e-6-actions.eps", "10-step A2C" )

aggregate_actions = np.zeros((1000, 17))
for title, folder in zip(titles, folders):
    results_before, results_after, actions = get_rl_results('/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/new_runs_not_comp/' + folder + '/output.log')
    if len(actions) < 1000:
        actions = np.vstack([np.array(actions), np.ones((733, 17))/17])
    action_averages = np.array([a/np.sum(a) for a in actions[:1000]])
    aggregate_actions += action_averages
aggregate_actions /= len(folders)
    

action_analysis2(aggregate_actions, 17, '/home/tybirk/Desktop/Speciale/FCTP/Plots/average_actions.eps', set_title='Average across runs')

"""
for title, folder in zip(titles, folders):
    results_before, results_after, actions = get_rl_results('/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/new_runs_comp/' + folder + '/output.log')
    action_analysis2(actions, 17, '/home/tybirk/Desktop/Speciale/FCTP/Plots/' + folder + '-comp-actions.eps', set_title=title)"""





def get_rl_results2(path):
    import ast
    with open(path) as f:
        actions = {}
        results_before = {}
        results_after = {}
        scores = []
        lines = f.readlines()
        for i, line in enumerate(lines):
            if 'Glover' in line and len(line) < 20:
                current_instance = line[7:12]
                if not current_instance in actions:
                    actions[current_instance] = []
                    
            if line[:9] == '[INFO] ep':
                scores.append(line[-7:])
                if lines[i-1][:3] == 'ter':
                    if lines[i-2][0] != '[':
                        continue
                    action_list = ast.literal_eval(lines[i-2])
                    actions[current_instance].append(action_list)
                else:
                    if lines[i-1][0] != '[':
                        continue
                    action_list = ast.literal_eval(lines[i-1])
                    actions[current_instance].append(action_list)
            if '[INFO] test 99' in lines[i-1]:
                res = ast.literal_eval(line)
                results_before[current_instance] = res
            if 'FINAL RESULT:' in line:
                res = line.split('FCTP ')[1]
                results_after[current_instance] = int(float(res))
                
    return scores, results_after, actions

all_actions = np.zeros((len(names), 17))
for title, folder in zip(titles, folders):
    results_before, results_after, actions = get_rl_results2('/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/new_runs_comp/' + folder + '/output.log')
    for key in actions:
        idx = np.argwhere(np.array(names)==key)[0]
        print(key, idx)
        all_actions[idx, :] += np.sum(np.array(actions[key]), axis=0)

actions_by_instance = [np.mean(all_actions[i*15:(i+1)*15, :], axis=0) for i in range(8)]
mean_actions_by_instance = np.array([actions_by_instance[i]/sum(actions_by_instance[i]) for i in range(8)])

action_averages = np.array([a/np.sum(a) for a in actions[:1000]])
aggregate_actions += action_averages
aggregate_actions /= len(folders)




scores, results_after, actions = get_rl_results2('/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/random_actions_not_comp/output.log')
scores = [float(score) for score in scores]
np.mean(scores)