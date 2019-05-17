#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue May  7 17:56:41 2019

@author: tybirk
"""
import numpy as np
from data_analysis import get_buson_results, read_buson_results, get_results_from_path, get_results_from_path_old, get_rl_results

path1 = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/PEheur_many_runs'
path2 = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/PEheur_new_many_runs'
path3 = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/PEheur_uden_sidste_search'
path4 = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/PEheur_new_many_runs3'
rl_path1 = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/rl_run_test1'
rl_path2 = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/rl_run_test2'
rl_path3 = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/wandb/rl_run_test3'
rl_path1 += '/output.log'
rl_path2 += '/output.log'
rl_path3 += '/output.log'

res1 = get_results_from_path_old(path1)[0]
res2 = get_results_from_path_old(path2)[0]
res3 = get_results_from_path_old(path3)[0]
res4 = get_results_from_path(path4)[0]
rl_final = {**get_rl_results(rl_path1)[1], **get_rl_results(rl_path2)[1], **get_rl_results(rl_path3)[1]}

from_excel = {'N3004' : 166900, 'N3009' : 166804, 'N300E' : 169138, 
              'N3104' : 178489, 'N3109' : 176881, 'N3404' : 311533,
              'N3409' : 307589, 'N340E' : 307277, 'N3504' : 447616,
              'N3509' : 442423, 'N3604' : 704078, 'N3609' : 696528,
              'N360E' : 694178, 'N3704' : 1205112, 'N709' : 1182417,
              'N370E' : 1178972}


all_results = [res1, res2, res3, res4, rl_final, from_excel]

names, buson_results = read_buson_results()

my_best_results = {}
my_best_heur = {}
diff = []
buson_was_best = []
for name in names:
    my_best_results[name] = min(x.get(name, 9999999999) for x in all_results)
    my_best_heur[name] = np.argmin([x.get(name, 9999999999) for x in all_results])
    diff.append(my_best_results[name] - buson_results[name])
    if my_best_results[name] - buson_results[name] > 0:
        buson_was_best.append(name)

diff = np.array(diff)
print(diff)
print(np.sum(diff < 0))

"""
bbb = get_full_path(buson_was_best)

f = open('buson_was_best.txt',"w+")
for inst in bbb:
    f.write('java FCTPmain ' + inst + ' && ')
f.close()"""

