package put.roadef.tweaks;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import put.roadef.Problem;
import put.roadef.Problem.Process;
import put.roadef.Solution;

/**
 * Simple operator which tries to improve given solution on the basis of
 * resource usage (ignoring other cost factors). The main idea is to find
 * greedily the most overloaded machine and its most demanding process (with the
 * highest requirements) that could be switched with a process that have
 * possibly low requirements.
 * 
 * @author marcin
 * 
 */
public class OverloadedMachineImprover extends SwitchProcessImprover {

	@Override
	public Comparator<Process> findCriticalProcesses(Solution solution,
			List<Process> criticalProcesses, List<Process> otherProcesses) {
		Problem problem = solution.getProblem();

		//TODO: what if there is no overloaded machine
		int overloadedMachine = -1;
		int overloadedResource = -1;
		long maxOverload = Long.MIN_VALUE;

		// We are looking for the most overloaded machine and 
		// resource (taking into account also resource weights)
		for (int r = 0; r < problem.getNumResources(); r++) {
			long weight = problem.getResource(r).loadCostWeight;
			for (int m = 0; m < problem.getNumMachines(); m++) {
				long overload = weight
						* (solution.getResourceUsage(m, r) - problem.getMachine(m).safetyCapacities[r]);
				if (overload > maxOverload) {
					overloadedMachine = m;
					overloadedResource = r;
					maxOverload = overload;
				}
			}
		}

		// All the processes are divided into two groups:
		// - overloading processes (assigned to overloaded machine)
		// - other processes (assigned to any other machine)
		int[] assignment = solution.getAssignment();
		for (int p = 0; p < problem.getNumProcesses(); p++) {
			if (assignment[p] == overloadedMachine) {
				criticalProcesses.add(problem.getProcess(p));
			} else {
				otherProcesses.add(problem.getProcess(p));
			}
		}

		// Overloading processes are sorted by decreasing requirement
		// of a critical resource, other processes - by increasing requirements
		Comparator<Problem.Process> cmp = new ResourceUsageProcessComparator(overloadedResource);
		Collections.sort(criticalProcesses, Collections.reverseOrder(cmp));
		Collections.sort(otherProcesses, cmp);

		return cmp;
	}

	public static class ResourceUsageProcessComparator implements Comparator<Problem.Process> {

		int comparedResource;

		public ResourceUsageProcessComparator(int resource) {
			comparedResource = resource;
		}

		@Override
		public int compare(Process o1, Process o2) {
			if (o1.requirements[comparedResource] < o2.requirements[comparedResource]) {
				return -1;
			} else if (o1.requirements[comparedResource] > o2.requirements[comparedResource]) {
				return 1;
			} else {
				return 0;
			}
		}
	}

}