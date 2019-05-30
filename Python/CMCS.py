import itertools
import numpy as np
import os
import networkx as nx

java_path = '/home/tybirk/Desktop/Speciale/FCTP/Java_code'
os.chdir(java_path)

def find_subsets(S,subset_size):
    """Find all subsets from S of size subset_size, returns a list of lists"""
    subsets_as_tuples = set(itertools.combinations(S, subset_size))
    return [list(subset) for subset in subsets_as_tuples]


def basis_vector(size, index):
    """Create standard basis-vector"""
    arr = [0]*size
    arr[index] = 1
    return arr

def get_possible_trans_matrices(size):
    """Create all possible transition matrices with dim = (size , size) """
    rows = [basis_vector(size, i) for i in range(size)]
    return list(itertools.product(rows, repeat=size)) # finds all row-combis

    
def get_meaningful_configurations(size):
    """Find all meaningful CMCS configs (not completely done, can still 
    prune out some) """
    m_sucs = np.array(get_possible_trans_matrices(size))
    m_fails = m_sucs[:]
    n = len(m_sucs)
    configurations = []
    graph_adj_mats = np.empty((n**2,size,size)) #  [None]*(n**2)
    for i in range(n):
        for j in range(n):
            if m_fails[j][1][1] == 1111: # set equal to 1 means if local search fails do not repeat
                graph_adj_mats[i*n+j] = 0
            else:
                graph_adj_mats[i*n + j] = m_sucs[i] + m_fails[j]
    graph_adj_mats = graph_adj_mats > 0 # check where sum is positive
    for i in range(n):
        for j in range(n): 
            G = nx.from_numpy_matrix(graph_adj_mats[i*n + j], create_using=nx.DiGraph())
            if nx.is_strongly_connected(G):
                configurations.append([m_sucs[i].tolist(), m_fails[j].tolist()])
        
    return configurations


def get_non_dominated(results):
    # finds non-dominated configurations by objective value

    n_instances, n_subsets, n_configs = results.shape
    dominated = np.zeros((n_subsets, n_configs))
    for subset in range(n_subsets):
        for config in range(n_configs):
            for subset2 in range(n_subsets):
                for config2 in range(n_configs):
                    if all(results[:, subset, config] > results[:, subset2, config2]):
                        dominated[subset, config] = 1
                        print(subset, config, "dominated by", subset2, config2)
                        break
                if dominated[subset, config]:
                    break
    """
    if round2:
        n_instances, n_configs = results.shape
        dominated = np.zeros((n_configs))
        for config in range(n_configs):
            for other_config in range(n_configs):
                if all(results[:, config] > results[:, other_config]):
                    dominated[config] = 1
                    break"""
                
    return ~np.array(dominated, dtype=bool) # tilde operator switches bools

import jnius_config
try:
    jnius_config.set_classpath('./', java_path)
except ValueError:
    print("Value error when setting classpath, proceed anyway")
try:
    from jnius import autoclass
except KeyError:
    # os.environ['JDK_HOME'] = "/usr/lib/jvm/java-1.8.0-openjdk-amd64"
    os.environ['JAVA_HOME'] = "/home/tybirk/anaconda3"
    from jnius import autoclass
CMCS = autoclass('CMCS')


def search(instances, timebudget, subsets, configurations, non_dominated = None):
    """Tries all meaningful configurations on all given instances"""
    results = np.zeros((len(instances), len(subsets), len(configurations)))
    length_subsets = len(subsets)
    length_configurations = len(configurations)
    
    # If special subset of non-dominated configurations is supplied
    if non_dominated is not None: 
        #for subset in subsets:
            #subset.insert(0,1)
        indices = np.argwhere(non_dominated)
        results += 9999999
        for j, idx in enumerate(indices):
            for k, instance in enumerate(instances):
                print("Subset {} ".format(idx[0]))
                print("Configuration {}".format(idx[1]))
                print("Test number {} out of {}".format(j+1,len(indices)))
                print("Instance: ", instance)
                heuris = CMCS(instance, subsets[idx[0]], 
                              configurations[idx[1]][0], configurations[idx[1]][1] )
                heuris.Solve(timebudget)
                results[k,idx[0], idx[1]] = heuris.solution.totalCost
                print("Cost: ", results[k,idx[0], idx[1]])     
    else:
        for i, subset in enumerate(subsets):
            #subset.insert(0, 1) # Always include LocalSearch (first accept)
            
            for j, configuration in enumerate(configurations):
                for k, instance in enumerate(instances):
                    print("Subset {} out of {}".format(i+1, length_subsets))
                    print("Configuration {} out of {}".format(j+1, length_configurations))
                    print("Instance: ", instance)
                    heuris = CMCS(instance, subset, 
                                  configuration[0], configuration[1] )
                    heuris.Solve(timebudget) #1000 time in milli seconds
                    results[k,i,j] = heuris.solution.totalCost
                    print("Cost: ", results[k,i,j])
            for l, instance in enumerate(instances):
                np.savetxt('CMCS_results/new_results_subset{0}_instance_{1}_200ms.txt'.format(i,l), results[l,i,:])
         
    #for k, instance in enumerate(instances):
    #np.savetxt('results_{0}.txt'.format(instance), results[k,:,:]) #delimiter=',') # Save intermediate results
        
    return results

"""
Example usage:

import copy

instances = ['Glover/N3004.FCTP', 'Glover/N3104.FCTP',
             'Glover/N3204.FCTP','Glover/N3304.FCTP','Glover/N3404.FCTP','Glover/N3604.FCTP', 'Glover/N3704.FCTP']
subsets = find_subsets({1,2,3,4,5,6,7,8,9,10,11}, 3) 
configurations = get_meaningful_configurations(3)
results = search(instances, 500, subsets, configurations, non_dominated = None)



results2 = np.zeros((len(instances), len(subsets), len(configurations)))
for i in range(len(subsets)):
    for l, instance in enumerate(instances):
        results[l, :, :] = np.loadtxt('CMCS_results/new_results_subset{0}_instance_{1}_200ms.txt'.format(i,l))

non_dominated = get_non_dominated(results)
results2 = search(instances, 2000, subsets, configurations, non_dominated)
for i, instance in enumerate(instances):
    np.savetxt('CMCS_results/new_non_dominated_2_secs.txt'.format(instance), results2[i])


instances2 =  ['Glover/N3000.FCTP', 'Glover/N300E.FCTP','Glover/N3100.FCTP',
                          'Glover/N310E.FCTP', 'Glover/N3200.FCTP','Glover/N320E.FCTP',
                          'Glover/N3300.FCTP','Glover/N330E.FCTP','Glover/N3400.FCTP',
                          'Glover/N340E.FCTP','Glover/N3500.FCTP','Glover/N350E.FCTP',
                          'Glover/N3600.FCTP','Glover/N360E.FCTP','Glover/N3700.FCTP',
                          'Glover/N370E.FCTP']

non_dominated2 = get_non_dominated(results2)
np.savetxt('CMCS_results/non_dominated2.txt', non_dominated2)
non_dominated2 = np.loadtxt('CMCS_results/non_dominated2.txt')

results3 = search(instances2, 2000, subsets, configurations, non_dominated2)
for i, instance in enumerate(instances2):
    np.savetxt('CMCS_results/new_{}_non_dominated_2_secs2.txt'.format(instance[7:]), results3[i])
non_dominated3 = get_non_dominated(results3)
percentage_improvements = (results3[:, non_dominated3] - lower_bounds[:,np.newaxis])/lower_bounds[:,np.newaxis]
sums = percentage_improvements.sum(axis=0)
worst_indices = (sums).argsort()[10:]
non_dominated4 = copy.deepcopy(non_dominated3)
indices = np.argwhere(non_dominated4)[worst_indices]
for index in indices:
    non_dominated4[index[0], index[1]] = False
    
np.savetxt('CMCS_results/non_dominated4.txt', non_dominated4)
non_dominated4 = np.loadtxt('CMCS_results/non_dominated4.txt')
non_dominated4 = non_dominated4.astype(int)

all_instances = get_full_path(names)
results4 = search(all_instances, 10000, subsets, configurations, non_dominated4)
for i, instance in enumerate(all_instances):
    np.savetxt('CMCS_results/new_{}_non_dominated_4_secs10.txt'.format(instance[7:]), results4[i])
    
indices = np.argwhere(non_dominated4)
results5 = []
for idx in indices:
    results5.append(results4[:, idx[0], idx[1]])

results5 = np.array(results5)
percentage_improvements = (results5 - lower_bounds[:])/lower_bounds[:]


best_config = [103, 250]
second_best = [119, 159]

results_best_config = np.zeros((10, len(all_instances)))
results_second_best_config = np.zeros((10, len(all_instances)))

for i, instance in enumerate(all_instances):
    for j in range(10):
        heuris = CMCS(instance, subsets[103], configurations[250][0], configurations[250][1] )
        heuris.Solve(10000)
        results_best_config[j,i]  = heuris.solution.totalCost
        heuris = CMCS(instance, subsets[119], configurations[159][0], configurations[159][1] )
        heuris.Solve(10000)
        results_second_best_config[j,i]  = heuris.solution.totalCost
        
np.savetxt('CMCS_results/best_config.txt', results_best_config)
np.savetxt('CMCS_results/second_best_config.txt', results_second_best_config)

best_results1 = np.min(results_best_config, axis=0)
best_results2 = np.min(results_second_best_config, axis=0)"""
