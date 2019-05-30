import itertools
import numpy as np
import os
import networkx as nx


def find_subsets(S, subset_size):
    """Find all subsets from S of size subset_size, returns a list of lists"""
    subsets_as_tuples = set(itertools.combinations(S, subset_size))
    return [list(subset) for subset in subsets_as_tuples]


def basis_vector(size, index):
    """Create standard basis-vector"""
    arr = [0] * size
    arr[index] = 1
    return arr


def get_possible_trans_matrices(size):
    """Create all possible transition matrices with dim = (size , size) """
    rows = [basis_vector(size, i) for i in range(size)]
    return list(itertools.product(rows, repeat=size))  # finds all row-combis


def get_meaningful_configurations(size):
    """Find all meaningful CMCS configs (not completely done, can still 
    prune out some) """
    m_sucs = np.array(get_possible_trans_matrices(size))
    m_fails = m_sucs[:]
    n = len(m_sucs)
    configurations = []
    graph_adj_mats = np.empty((n ** 2, size, size))  # [None]*(n**2)
    for i in range(n):
        for j in range(n):
            if m_fails[j][1][1] == 1:  # set equal to 1 means if local search fails do not repeat
                graph_adj_mats[i * n + j] = 0
            else:
                graph_adj_mats[i * n + j] = m_sucs[i] + m_fails[j]

    graph_adj_mats = graph_adj_mats > 0  # check where sum is positive

    for i in range(n):
        for j in range(n):
            G = nx.from_numpy_matrix(graph_adj_mats[i * n + j], create_using=nx.DiGraph())
            if nx.is_strongly_connected(G):
                configurations.append([m_sucs[i].tolist(), m_fails[j].tolist()])

    return configurations


def get_non_dominated(results):
    """finds non-dominated configurations by objective value"""
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

    return ~np.array(dominated, dtype=bool)  # tilde operator switches bools


def search(instances, time_budget, subsets, configurations, non_dominated=None):
    """Tries all meaningful configurations on all given instances"""
    results = np.zeros((len(instances), len(subsets), len(configurations)))
    n_subsets = len(subsets)
    n_configs = len(configurations)

    # If special subset of non-dominated configurations is supplied
    if non_dominated is not None:
        indices = np.argwhere(non_dominated)
        results += 9999999
        for j, idx in enumerate(indices):
            for k, instance in enumerate(instances):
                print("Subset {} ".format(idx[0]))
                print("Configuration {}".format(idx[1]))
                print("Test number {} out of {}".format(j + 1, len(indices)))
                print("Instance: ", instance)
                heuris = CMCS(instance, subsets[idx[0]],
                              configurations[idx[1]][0], configurations[idx[1]][1])
                heuris.Solve(time_budget)
                results[k, idx[0], idx[1]] = heuris.solution.totalCost
                print("Cost: ", results[k, idx[0], idx[1]])
    else:
        for i, subset in enumerate(subsets):
            for j, configuration in enumerate(configurations):
                for k, instance in enumerate(instances):
                    print("Subset {} out of {}".format(i + 1, n_subsets))
                    print("Configuration {} out of {}".format(j + 1, n_configs))
                    print("Instance: ", instance)
                    heuris = CMCS(instance, subset,
                                  configuration[0], configuration[1])
                    heuris.Solve(time_budget)  # 1000 time in milli seconds
                    results[k, i, j] = heuris.solution.totalCost
                    print("Cost: ", results[k, i, j])
    return results


if __name__ == 'main':  # Example usage
    import jnius_config

    java_path = '/home/tybirk/Desktop/Speciale/FCTP/Java_code'
    os.chdir(java_path)

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

    instances = ['Glover/N3004.FCTP', 'Glover/N3104.FCTP',
                 'Glover/N3204.FCTP', 'Glover/N3304.FCTP', 'Glover/N3404.FCTP', 'Glover/N3604.FCTP',
                 'Glover/N3704.FCTP']

    subsets = find_subsets({1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, 3)

    configs= get_meaningful_configurations(3)

    results = search(instances, 500, subsets, configs, non_dominated=None)

    non_dominated = get_non_dominated(results)

    results2 = search(instances, 2000, subsets, configs, non_dominated)
