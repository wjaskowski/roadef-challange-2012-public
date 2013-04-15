package put.roadef.tweaks;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import put.roadef.Problem;
import put.roadef.Problem.Balance;
import put.roadef.Problem.Machine;
import put.roadef.Problem.Process;
import put.roadef.Solution;

public class ImbalancedMachineImprover extends SwitchProcessImprover {

	// Zasobu r2 musi byc co najmniej target * r1
	// Chcemy wyrzucic proces ktory wymaga r2 w ilosci wiekszej niz target * r1
	// Zamiast niego znajdujemy procesy ktore wymagaja r2 w ilosci mniejszej niz target * r1

	@Override
	public Comparator<Process> findCriticalProcesses(Solution solution, List<Process> costlyProcesses,
			List<Process> otherProcesses) {

		
		Problem problem = solution.getProblem();
		if (problem.getNumBalances() == 0)
			return null;
		
		
		int criticalBalance = -1;
		int imbalancedMachine = -1;
		long maxBalanceCost = Long.MIN_VALUE;

		for (int b = 0; b < problem.getNumBalances(); b++) {
			Balance balance = problem.getBalance(b);
			for (int m = 0; m < problem.getNumMachines(); m++) {
				Machine machine = problem.getMachine(m);
				long available1 = machine.capacities[balance.r1]
						- solution.getResourceUsage(m, balance.r1);
				long available2 = machine.capacities[balance.r2]
						- solution.getResourceUsage(m, balance.r2);
				long balanceCost = balance.weight
						* Math.max(0, balance.target * available1 - available2);

				if (balanceCost > maxBalanceCost) {
					criticalBalance = b;
					imbalancedMachine = m;
					maxBalanceCost = balanceCost;
				}
			}
		}

		int[] assignment = solution.getAssignment();
		for (int p = 0; p < problem.getNumProcesses(); p++) {
			if (assignment[p] == imbalancedMachine) {
				costlyProcesses.add(problem.getProcess(p));
			} else {
				otherProcesses.add(problem.getProcess(p));
			}
		}

		Comparator<Problem.Process> cmp = new ResourceBalanceComparator(
				problem.getBalance(criticalBalance));
		Collections.sort(costlyProcesses, Collections.reverseOrder(cmp));
		Collections.sort(otherProcesses, cmp);

		return cmp;
	}
}

class ResourceBalanceComparator implements Comparator<Problem.Process> {

	private Balance balance;

	public ResourceBalanceComparator(Balance balance) {
		this.balance = balance;
	}

	@Override
	public int compare(Process o1, Process o2) {
		double ratio1 = o1.requirements[balance.r2]
				/ (double) o1.requirements[balance.r1];
		double ratio2 = o2.requirements[balance.r2]
				/ (double) o2.requirements[balance.r1];

		if (ratio1 < ratio2) {
			return -1;
		} else if (ratio1 > ratio2) {
			return 1;
		} else {
			return 0;
		}
	}
}
