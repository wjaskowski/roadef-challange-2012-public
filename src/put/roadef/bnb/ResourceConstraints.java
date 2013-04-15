package put.roadef.bnb;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.longs.Long2IntAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2IntSortedMap;

import java.util.Collection;
import java.util.Collections;

import put.roadef.Problem;
import put.roadef.Problem.Balance;
import put.roadef.Problem.Machine;
import put.roadef.Problem.Process;

public class ResourceConstraints implements IncrementalSolutionConstraints, IncrementalSolutionCost {

	private final Problem problem;

	private long[][] resourceUsage;
	private long[][] transientUsage;

	private long loadCost;
	private long[] loadCosts;
	private long balanceCost;
	private long[][] balanceCosts;

	private int[] originalAssignment;

	@Override
	public long getCost() {
		return loadCost + balanceCost;
	}

	@Override
	public long getLowerBound(PartialSolution partialSolution) {
		return getPreciseLowerBound(partialSolution);
	}

	public long getPreciseLowerBound(PartialSolution partialSolution) {
		long costBound = 0;

		Collection<Process> processes = partialSolution.getUnassignedProcesses();
		for (Process process : processes) {
			long minAdditionalCost = Long.MAX_VALUE;
			for (int m = 0; m < problem.getNumMachines(); m++) {
				Machine machine = problem.getMachine(m);
				if (checkFutureConstraints(process, machine)) {
					long additionalCost = 0;

					for (int r = 0; r < problem.getNumResources(); r++) {
						long oldResourceUsage = resourceUsage[m][r];
						resourceUsage[m][r] += process.requirements[r];
						long safety = machine.safetyCapacities[r];
						long diff = problem.computeLoadCostNotWeighted(resourceUsage[m][r], safety)
								- problem.computeLoadCostNotWeighted(oldResourceUsage, safety);
						additionalCost += diff * problem.getResource(r).loadCostWeight;
					}

					for (int b = 0; b < problem.getNumBalances(); b++) {
						long diff = computeBalanceCost(problem.getBalance(b), machine)
								- balanceCosts[m][b];
						additionalCost += diff * problem.getBalance(b).weight;
					}

					for (int r = 0; r < problem.getNumResources(); r++) {
						resourceUsage[m][r] -= process.requirements[r];
					}

					minAdditionalCost = Math.min(minAdditionalCost, additionalCost);
				}
			}

			costBound += minAdditionalCost;
		}

		return loadCost + balanceCost + costBound;
	}

	public ResourceConstraints(Problem problem) {
		this.problem = problem;
		this.loadCost = 0;
		this.loadCosts = new long[problem.getNumMachines()];
		this.balanceCost = 0;
		this.balanceCosts = new long[problem.getNumMachines()][problem.getNumBalances()];
		this.resourceUsage = new long[problem.getNumMachines()][problem.getNumResources()];
		this.transientUsage = problem.computeTransientResourceUsage(problem.getOriginalSolution());
		this.originalAssignment = problem.getOriginalSolution().getAssignment();

		for (int m = 0; m < problem.getNumMachines(); m++) {
			Machine machine = problem.getMachine(m);
			for (int b = 0; b < problem.getNumBalances(); b++) {
				Balance balance = problem.getBalance(b);
				balanceCosts[m][b] = computeBalanceCost(balance, machine);
				balanceCost += balanceCosts[m][b] * balance.weight;
			}
		}
	}

	public ResourceConstraints(Problem problem, int[] assignment) {
		this.problem = problem;
		this.loadCost = 0;
		this.loadCosts = new long[problem.getNumMachines()];
		this.balanceCost = 0;
		this.balanceCosts = new long[problem.getNumMachines()][problem.getNumBalances()];
		this.resourceUsage = new long[problem.getNumMachines()][problem.getNumResources()];
		this.transientUsage = problem.computeTransientResourceUsage(problem.getOriginalSolution());
		this.originalAssignment = problem.getOriginalSolution().getAssignment();

		for (int p = 0; p < assignment.length; p++) {
			Process process = problem.getProcess(p);
			int originalMachineId = problem.getOriginalSolution().getMachine(p);
			for (int r = 0; r < problem.getNumResources(); r++) {
				if (problem.getResource(r).isTransient && (assignment[p] != originalMachineId)) {
					transientUsage[assignment[p]][r] += process.requirements[r];
				}
				resourceUsage[assignment[p]][r] += process.requirements[r];
			}
		}

		for (int m = 0; m < problem.getNumMachines(); m++) {
			Machine machine = problem.getMachine(m);
			for (int b = 0; b < problem.getNumBalances(); b++) {
				Balance balance = problem.getBalance(b);
				balanceCosts[m][b] = computeBalanceCost(balance, machine);
				balanceCost += balanceCosts[m][b] * balance.weight;
			}

			for (int r = 0; r < problem.getNumResources(); r++) {
				long overload = Math.max(0, resourceUsage[m][r] - machine.safetyCapacities[r])
						* problem.getResource(r).loadCostWeight;
				loadCost += overload;
				loadCosts[m] += overload;
			}
		}
	}

	/**
	 * Updates load cost
	 */
	@Override
	public void addAssignment(Process process, Machine machine) {
		for (int r = 0; r < problem.getNumResources(); r++) {
			if (problem.getResource(r).isTransient
					&& (machine.id != originalAssignment[process.id])) {
				transientUsage[machine.id][r] += process.requirements[r];
			}

			addToResourceUsage(machine.id, r, process.requirements[r]);
		}

		for (int b = 0; b < problem.getNumBalances(); b++) {
			long diff = computeBalanceCost(problem.getBalance(b), machine)
					- balanceCosts[machine.id][b];
			balanceCosts[machine.id][b] += diff;
			balanceCost += diff * problem.getBalance(b).weight;
		}

	}

	@SuppressWarnings("unused")
	private void old_addToResourceUsage(int machine, int resource, long value) {
		long safety = problem.getMachine(machine).safetyCapacities[resource];
		long oldResourceUsage = resourceUsage[machine][resource];
		resourceUsage[machine][resource] += value;
		long newResourceUsage = resourceUsage[machine][resource];

		long diff = -problem.computeLoadCostNotWeighted(oldResourceUsage, safety)
				+ problem.computeLoadCostNotWeighted(newResourceUsage, safety);
		loadCost += diff * problem.getResource(resource).loadCostWeight;
		loadCosts[machine] += diff * problem.getResource(resource).loadCostWeight;
	}

	private void addToResourceUsage(int machine, int resource, long value) {
		long safety = problem.getMachine(machine).safetyCapacities[resource];
		long oldOverload = resourceUsage[machine][resource] - safety;
		resourceUsage[machine][resource] += value;
		long newOverload = resourceUsage[machine][resource] - safety;

		long diffOverload = 0;
		if (oldOverload > 0) {
			if (newOverload > 0) {
				diffOverload = newOverload - oldOverload;
			} else {
				diffOverload = -oldOverload;
			}
		} else if (newOverload > 0) {
			diffOverload = newOverload;
		}

		diffOverload *= problem.getResource(resource).loadCostWeight;
		loadCost += diffOverload;
		loadCosts[machine] += diffOverload;
	}

	private long computeBalanceCost(Balance balance, Machine machine) {
		long available1 = machine.capacities[balance.r1]
				- resourceUsage[machine.id][balance.r1];
		long available2 = machine.capacities[balance.r2]
				- resourceUsage[machine.id][balance.r2];

		return Math.max(0, balance.target * available1 - available2);
	}

	/**
	 * Updates load cost
	 */
	@Override
	public void removeAssignment(Process process, Machine machine) {
		for (int r = 0; r < problem.getNumResources(); r++) {
			if (problem.getResource(r).isTransient
					&& (machine.id != originalAssignment[process.id])) {
				transientUsage[machine.id][r] -= process.requirements[r];
			}

			addToResourceUsage(machine.id, r, -process.requirements[r]);
		}

		for (int b = 0; b < problem.getNumBalances(); b++) {
			long diff = computeBalanceCost(problem.getBalance(b), machine)
					- balanceCosts[machine.id][b];
			balanceCosts[machine.id][b] += diff;
			balanceCost += diff * problem.getBalance(b).weight;
		}
	}

	@Override
	public boolean checkFutureConstraints(Process process, Machine machine) {
		return checkFutureResourceUsage(process, machine);
	}

	public boolean checkFutureResourceUsage(Process process, Machine machine) {
		for (int r = 0; r < problem.getNumResources(); r++) {
			if (problem.getResource(r).isTransient
					&& (machine.id != originalAssignment[process.id])) {
				if (transientUsage[machine.id][r] + process.requirements[r] > machine.capacities[r]) {
					return false;
				}
			}

			if (resourceUsage[machine.id][r] + process.requirements[r] > machine.capacities[r]) {
				return false;
			}
		}

		return true;
	}

	public IntCollection getOverloadedMachines(int numMachines) {
		numMachines = Math.min(numMachines, problem.getNumMachines());
		Long2IntSortedMap mostOverloadedMachines = new Long2IntAVLTreeMap(
				Collections.reverseOrder());
		for (int m = 0; m < problem.getNumMachines(); m++) {
			if (mostOverloadedMachines.size() < numMachines) {
				if (mostOverloadedMachines.containsKey(loadCosts[m])) {
					mostOverloadedMachines.put(loadCosts[m] + 1, m);
				} else {
					mostOverloadedMachines.put(loadCosts[m], m);
				}
			} else {
				long smallestOverload = mostOverloadedMachines.lastLongKey();
				if (smallestOverload < loadCosts[m]) {
					mostOverloadedMachines.remove(smallestOverload);
					if (mostOverloadedMachines.containsKey(loadCosts[m])) {
						mostOverloadedMachines.put(loadCosts[m] + 1, m);
					} else {
						mostOverloadedMachines.put(loadCosts[m], m);
					}
				}
			}
		}

		return mostOverloadedMachines.values();
	}
}
