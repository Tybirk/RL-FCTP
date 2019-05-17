#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed May  8 10:48:12 2019

@author: tybirk
"""
import numpy as np

path = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/CMCS_results/test'
import os

means = {}
best = {}
names = []

for root, dirs, files in os.walk(path):
    for file in files:
        with open(path + '/' + file, 'r') as f:
            results = []
            lines = f.readlines()
            name = lines[0][7:12]
            print(name)
            names.append(name)
            for line in lines[1:]:
                results.append(float(line))
                
            means[name] = np.mean(results)
            best[name] = np.min(results)
            
            
means
best

irnls_means = {}
irnls_best = {}

for name in names:
    result = results_100[name][:, 0][:50]
    irnls_means[name] = np.mean(result)
    irnls_best[name] = np.min(result)

results_100['N3603'][:, 0]

irnls_best['N3603'] = 693158.0
irnls_means['N3603'] = 698872.0

irnls_best
irnls_means2 = {}
irnls_best2 = {}
means2 = {}
best2 = {}
res1 = []
res2 = []
res3 = []
res4 = []

for name in sorted(names):
    res1.append(irnls_means[name])
    res2.append(irnls_best[name])
    res3.append(means[name])
    res4.append(best[name])
    
outfile = path + '/cmcs_avg_values_comparison.txt'
headers = ['Instance', 'IRNLS', 'CMCS']
results9 = [sorted(names), res1, res3]
make_latex_table(headers, results9, outfile, True)
    