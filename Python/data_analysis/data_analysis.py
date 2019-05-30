#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Apr 29 09:35:59 2019

@author: tybirk
"""

import os
import numpy as np
import matplotlib.pyplot as plt
import ast
import seaborn as sns
import pandas as pd
import matplotlib.ticker as ticker



def get_results_from_path_old(path):
    directory = path
    directory = os.fsencode(directory)
    os.chdir(directory)
    
    results_100 = {}
    time_dict = {}
    results_final= {}
    results_before = {}
    
    for file in os.listdir(directory):
         filename = os.fsdecode(file)
         with open(filename) as f:
             lines = f.readlines()
             if len(lines) == 0:
                 print(filename, "is empty")
             else:
                 res = np.zeros((100, 3))
                 for i, line in enumerate(lines[1:]):
                     name = filename.split(".")[0]
                     if i < 100:
                         r = line.split()
                         r = [float(x) for x in r]
                         res[i] = r
                     elif i == 100:
                         results_before[name] = float(line)
                     elif i==101:
                         results_final[name] = int(float(line))
                     elif i == 102:
                         time_before = float(line)
                     elif i == 103:
                         time_after= float(line)
                         time_dict[name] = (time_before, time_after)
                 results_100[name] = res
    print("Number of instances : ", len(results_final))
    
    return results_final, results_100, results_before, time_dict

def get_results_from_path(path):
    
    directory = path
    directory = os.fsencode(directory)
    os.chdir(directory)
    
    results_100 = {}
    results_10 = {}
    time_dict = {}
    results_final= {}
    results_before = {}
    
    for file in os.listdir(directory):
         filename = os.fsdecode(file)
         with open(filename) as f:
             lines = f.readlines()
             if len(lines) == 0:
                 print(filename, "is empty")
             else:
                 res = np.zeros((100, 3))
                 res2 = np.zeros((10,3))
                 for i, line in enumerate(lines[1:]):
                     name = filename.split(".")[0]
                     if i < 100:
                         r = line.split()
                         r = [float(x) for x in r]
                         res[i] = r
                     elif i == 100:
                         results_before[name] = float(line)
                     elif i > 100 and i <=110:
                         r = [float(x) for x in r]
                         r = line.split()
                         res2[i%100-1] = r
                     elif i == 112:
                         results_final[name] = int(float(line))
                     elif i == 113:
                         time_before = float(line)
                     elif i == 114:
                         time_after= float(line)
                         time_dict[name] = (time_before, time_after)
                 results_100[name] = res
                 results_10[name] = res2
    print("Number of instances : ", len(results_final))
    
    return results_final, results_100, results_before, time_dict




def read_buson_results():
    symbols = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
               'B', 'C', 'D', 'E']
    buson_results = []
    names = []
    results_dictionary2 = {}
    
    with open('/home/tybirk/Desktop/Speciale/FCTP/Java_code/buson_results.txt') as f:
        lines = f.readlines()
        j = -1
        for i, line in enumerate(lines):
            if len(line) == 1:
                continue
            
            if i % 17 == 0:
                j += 1
                continue
            name = 'N3' + str(j) + '0' + symbols[i % 17 - 1]
            names.append(name)
            buson_results.append(int(line))
            results_dictionary2[name] = int(line)
    return names, results_dictionary2


def get_buson_results(instances, *args):
    if isinstance(instances, str):
        instances = [instances]
    
    if args:
        instances.extend(list(args))
    results = []
    for instance in instances:
        idx = instance.find('N')
        if idx == -1:
            idx = instance.find('n')
            instance = instance.upper()
            if idx == -1:
                print("Invalid name: ", instance )
        
        print(instance[idx:(idx+5)],':', results_dictionary_buson[instance[idx:(idx+5)]])
        results.append(results_dictionary_buson[instance[idx:(idx+5)]])
    return results
        

def compare_instances_from_path(path):
    results_final, results_100, results_before, time_dict = get_results_from_path(path)
    names, results_buson = read_buson_results()
    common_names = sorted([name for name in results_final.keys() if name in names])
    print('Instances not present:', sorted([name for name in names if name not in results_final.keys()]))
    differences = []
    for name in common_names:
        print(name, "Buson:", results_buson[name], "Mine:", results_final[name])
        differences.append(results_final[name] - results_buson[name])
    
    res1 = [results_final[name] for name in common_names]
    res2 = [results_buson[name] for name in common_names]
    
    print(differences)
    print("best on ", sum(np.array(differences)<0), "out of", len(differences) )
    return differences, res1, res2

    
def get_upper_bounds(instance_names, from_file=True):
    os.chdir('/home/tybirk/Desktop/Speciale/FCTP/Java_code')
    if from_file:
        with open('upper_bounds.txt', 'r') as f:
            lines = f.readlines()
            string = ''
            for line in lines:
                line = line[:-1]
                string += line
            ub1 = ast.literal_eval(string)
        ub = {}
        for name in instance_names:
            ub[name] = ub1[name]
        return ub
    
    import jnius_config
    # jnius_config.set_classpath('./', 'C:\\Users\\peter\\Desktop\\Speciale\\FCTP\\Java_code')
    try:
        jnius_config.set_classpath('./', '/home/tybirk/Desktop/Speciale/FCTP/Java_code')
    except ValueError:
        print("Value error when setting classpath, proceed anyway")
    try:
        from jnius import autoclass
    except KeyError:
        # os.environ['JDK_HOME'] = "/usr/lib/jvm/java-1.8.0-openjdk-amd64"
        os.environ['JAVA_HOME'] = "/home/tybirk/anaconda3"
        from jnius import autoclass
        
    upper_bounds = {}
    full_instance_names = ['Glover/'+x+'.FCTP' for x in instance_names]
    for name1, name2 in zip(full_instance_names,instance_names):
       inst = autoclass('RL_composite')(name1)
       if name2 == 'N3606' or name2 == 'N3706':
           inst.RandGreedy(0.3)
           inst.LS_first_acc()
       else:
           inst.LPheu()
    

       upper_bounds[name2] = inst.solution.totalCost
       
    return upper_bounds



def get_relative_performance(results1, results2, names, upper_bounds, lower_bounds):
    measure = []
    for name in names:
        res = results1[name] - lower_bounds[name]
        res /= (upper_bounds[name] - lower_bounds[name])
        measure.append(res)
    return measure

def get_relative_performance2(results1, results2, upper_bounds, names, compare_to_min=False):
    measure = []
    for name in names:
        if compare_to_min: 
            res = results1[name] - min(results1[name], results2[name])
        else:
            res = results1[name] - results2[name]
        res /= (upper_bounds[name]- min(results1[name], results2[name]))
        measure.append(res)
    return measure


def visualize_relative_performance(results1, results2, upper_bounds, names, compare_to_min=False, fname=None):
    rel_perf = get_relative_performance2(results1, results2, upper_bounds, names, compare_to_min)
    print(np.mean(rel_perf))
    plt.plot(np.linspace(0, 120, 120), [0]*120)
    plt.plot(rel_perf, '.')

    a = plt.gca()
    plt.xticks([15*i for i in range(9)], ['']*9)
    a.set_xticks([15*i + 7.5 for i in range(8)],  minor=True)
    a.set_xticklabels(['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'], minor=True)
    a.tick_params(which='minor', length=0)
    if fname:
        plt.savefig(fname, format='eps', dpi=1000)
    plt.show()
    


def get_full_path(instances):
    return ['Glover/'+x+'.FCTP' for x in instances]

def make_tables():
    from latex_tables import make_latex_table
    headers = ['Instance','IRNLS', 'Buson']
    for i in range(8):
        names_cur = names[i*15:(i+1)*15]
        results_mine = [results_final[name] for name in names_cur]
        results_buson = [results_dictionary_buson[name] for name in names_cur]
        results = [names_cur, results_mine, results_buson]
        outfile = 'tables/' + names_cur[0] + '-' + names_cur[-1] + '_table.txt'
        make_latex_table(headers, results, outfile, True)

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
            if line[:6] == '[INFO]':
                action_list = ast.literal_eval(lines[i-1])
                actions.append(action_list)
            if '[INFO] test 99' in lines[i-1]:
                res = ast.literal_eval(line)
                results_before[current_instance] = res
            if 'FINAL RESULT:' in line:
                res = line.split('FCTP ')[1]
                results_after[current_instance] = int(float(res))
                
    return results_before, results_after, actions

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



def action_analysis(actions, k):
    actions = np.array(actions)[:100]
    top_actions = []
    for a in actions:
        top_indices = (-a).argsort()[:k]
        top_actions.append(top_indices)
    y = np.array(top_actions).flatten()
    x = np.array([[i]*k for i in range(len(actions))]).flatten()
    labels = [i % k + 1 for i in range(k*len(actions))]
    df = pd.DataFrame(np.array([x,y,labels]).T, columns=('x', 'y', 'labels' ))
    sns.set_style('ticks')
    sns.set_palette((sns.cubehelix_palette(k, start=2, rot=0, dark=0.5, light=.95, reverse=True)) )# pastal,  muted, dark, deep, colorblind, bright, Reds
    the_plot = sns.pairplot(x_vars='x', y_vars='y', data=df, hue="labels", size=7, aspect=1.5)
    the_plot.set(ylim=(0.17), yticks=np.arange(17))
    the_plot.savefig('/home/tybirk/Desktop/Speciale/FCTP/Plots/output.png')
    plt.show()



if __name__ == "__main__":
    # path = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/PEheur_many_runs'
    # path = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/PEheur_new_many_runs'
    path = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/PEheur_new_many_runs3'
    #path = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/PEheur_uden_sidste_search'
    results_final, results_100, results_before, time_dict = get_results_from_path(path)
    names, results_dictionary_buson = read_buson_results()
    diff, res1, res2 = compare_instances_from_path(path)
    ub = get_upper_bounds(names, from_file=True)
    visualize_relative_performance(results_final, results_dictionary_buson, ub, names, fname='/home/tybirk/Desktop/Speciale/FCTP/Plots/rel_perf_mine.eps', compare_to_min=True)
    visualize_relative_performance(results_dictionary_buson, results_final, ub, names, fname='/home/tybirk/Desktop/Speciale/FCTP/Plots/rel_perf_buson.eps', compare_to_min=True)
    visualize_relative_performance(results_final, results_dictionary_buson, ub, names, fname='/home/tybirk/Desktop/Speciale/FCTP/Plots/rel_perf_diff.eps')
   
    test_instances = ['N3004', 'N3009', 'N3104', 'N3109', 'N3204', 'N3209', 'N3304', 'N3309',
          'N3404', 'N3409', 'N3504', 'N3509', 'N3604', 'N3609', 'N3704', 'N3709']
    
    
    rl_path1 = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/rl_run_test1'
    rl_path2 = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/rl_run_test2'
    rl_path3 = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/rl_run_test3'
    rl_path1 += '/output.log'
    rl_path2 += '/output.log'
    rl_path3 += '/output.log'



    rl_before1, rl_after1, rl_actions1 = get_rl_results(rl_path1)
    rl_before2, rl_after2, rl_actions2 = get_rl_results(rl_path2)
    rl_before3, rl_after3, rl_actions3 = get_rl_results(rl_path3)
    
    rl_final = {**rl_after1, **rl_after2, **rl_after3}

