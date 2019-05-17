#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sun May  5 10:15:22 2019

@author: tybirk
"""


def make_latex_table(headers, results, outfile, comparison=False, comma_separate=True):
    """Input list of tuple of headers and list of lists/tuples with results
    corresponding to each header"""
    
    if comma_separate:
        results_new = []
        for sub_results in results[1:]: # Set commas for long numbers
            sub_results_new = []
            for result in sub_results:
                result = int(result)
                result = "{:,}".format(result)
                sub_results_new.append(result)
            results_new.append(sub_results_new)
    
        results[1:] = results_new
    
    if comparison:  # Make lowest value bold
        indices = np.argmin(np.array(results[1:]), axis=0)
        for i, idx in enumerate(indices):
            results[idx+1][i] = r'\textbf{' + str(results[idx+1][i]) + r'}'
    
    
    n = len(results)
    assert len(headers) == n, "Need same number of headers as table entries"
    for result in results:
        assert len(result) == len(results[0]), "Table entries differ in length"
    table = ''
    table += r'\begin{table}[htbp] \n\centering \n\begin{tabular}'
    table += '{' + 'c'*n + r'} \n'
    table += r'\toprule \n'
    for i, header in enumerate(headers):
        table += str(header) + ' '
        if i < len(headers) - 1:
            table += '& '
    table += r'\\ ' + r'\n' + r'\midrule ' + r'\n'
    
    for line in zip(*results):
        for i, element in enumerate(line):
            table += str(element) + ' '
            if i < len(line) - 1:
                table += '& '
        table += r'\\ \n'
    
    table += r'\bottomrule \n'
    table += r'\end{tabular} \n'
    table += r'\caption{CAPTION} \n'
    table += r'\label{LABEL} \n'
    table += r'\end{table}'

    tabular_lines = len(results[0]) + 4
    f = open(outfile,"w+")
    lines = table.split(r'\n')
    
    for i, line in enumerate(lines):
        if i == 0 or i == len(lines) - 1:
            f.write(line + '\n')
        elif i >= 3 and i < 3 + tabular_lines:
            f.write('   '*2 + line + '\n')
        else:
            f.write('   ' + line + '\n')
    f.close()

import numpy as np
# Eksempel på brug
headers = ['klasse', 'højde', 'bredde']
results = [['klasse1', 'klasse2', 'klasse3'], np.array([1000000,2,3]), [4,45000000,19]]
outfile = '/home/tybirk/Desktop/hejmeddig.txt'
make_latex_table(headers, results, outfile)