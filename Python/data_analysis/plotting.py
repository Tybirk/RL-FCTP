#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu May  9 14:47:18 2019

@author: tybirk
"""

from data_analysis import get_results_from_path, read_buson_results, compare_instances_from_path, get_upper_bounds
import numpy as np
import os
import numpy as np
import matplotlib.pyplot as plt
import ast
import seaborn as sns
import pandas as pd
import matplotlib.ticker as ticker
from matplotlib import gridspec
from matplotlib import rc


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

def relative_performance_plot():
    path = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/PEheur_new_many_runs3'
    #path = '/home/tybirk/Desktop/Speciale/FCTP/Java_code/PEheur_uden_sidste_search'
    results_final, results_100, results_before, time_dict = get_results_from_path(path)
    names, results_dictionary_buson = read_buson_results()
    diff, res1, res2 = compare_instances_from_path(path)
    ub = get_upper_bounds(names, from_file=True)
    fname='/home/tybirk/Desktop/Speciale/FCTP/Plots/relative_performances.eps'
    
    perf1 = get_relative_performance2(results_final, results_dictionary_buson, ub, names, True)
    perf2 = get_relative_performance2(results_dictionary_buson, results_final, ub, names, True)
    perf3 = get_relative_performance2(results_final, results_dictionary_buson, ub, names)
    line_x, line_y = np.linspace(0, 120, 120), [0]*120
    rc('font', **{'family': 'serif', 'serif': ['Computer Modern']})
    rc('text', usetex=False)
    fs = 11
    
    fig = plt.figure()
    fig.set_size_inches(6, 8)
    gs = gridspec.GridSpec(3, 1, height_ratios=[1, 1, 2]) 
    
    ax1 = plt.subplot(gs[0])
    ax1.set_title('PIRNLS', fontsize=fs)
    ax1.set_yticks([0.00, 0.02, 0.04])
    plt.plot(line_x, line_y)
    plt.plot(perf1, '.')
    plt.xticks([15*i for i in range(9)], ['']*9)
    
    plt.setp(ax1.get_xticklabels(), visible=False)
    ax2 = plt.subplot(gs[1], sharex=ax1, sharey=ax1)
    ax2.set_title('Buson', fontsize=fs)
    ax2.set_yticks([0.00, 0.02, 0.04])
    # ax2.set_ylabel('relative performance', fontsize=12)
    plt.plot(line_x, line_y)
    plt.plot(perf2, '.')
    plt.setp(ax2.get_xticklabels(), visible=False)
    
    ax3 = plt.subplot(gs[2], sharex=ax1)
    ax3.set_title('Difference', fontsize=fs)
    plt.plot(line_x, line_y)
    plt.plot(perf3, '.')
    ax3.set_xticks([15*i + 7.5 for i in range(8)],  minor=True)
    ax3.set_xticklabels(['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'], minor=True)
    
    ax1.tick_params(which='minor', length=0)
    ax2.tick_params(which='minor', length=0)
    ax3.tick_params(which='minor', length=0)
    ax3.set_yticks([-0.04, -0.02, 0.00, 0.02, 0.04])
    #ax3.set_xlabel('Instance', fontsize=12)
    plt.setp(ax1.get_xticklabels(minor=True), visible=False)
    plt.setp(ax2.get_xticklabels(minor=True), visible=False)
    #plt.tight_layout()
    
    fig.text(0.52, 0.07, 'Instance', ha='center', fontsize=fs)
    fig.text(0.02, 0.5, 'Relative performance', va='center', rotation='vertical', fontsize=fs)
    plt.savefig(fname, format='eps', dpi=2000)
    
    plt.show()

