package put.roadef;

import put.roadef.Problem.Balance;

public class ProblemUtils {
	static public long computeOptimisticImprovementForMachines(Solution solution, int m1, int m2) {
		Problem problem = solution.getProblem();

		long optimisticLoadDiff = 0;
		for (int r = 0; r < problem.getNumResources(); ++r) {
			optimisticLoadDiff += getOptimisticLoadCostDiff(solution, m1, m2, r);
		}

		long optimisticBalanceDiff = 0;
		for (int b = 0; b < problem.getNumBalances(); ++b) {
			//optimisticBalanceDiff += Math.min(getOptimisticBalanceDiff(solution, m1, m2, b),
			//		getOptimisticBalanceDiff2(solution, m1, m2, b));
			
			optimisticBalanceDiff += getOptimisticBalanceDiff(solution, m1, m2, b);
		}
		return optimisticLoadDiff + optimisticBalanceDiff;
	}

	private static long getOptimisticBalanceDiff(Solution solution, int m1, int m2, int bal) {
		Problem problem = solution.getProblem();
		Balance b = problem.getBalance(bal);

		long a1_used = solution.getResourceUsage(m1, b.r1);
		long a2_used = solution.getResourceUsage(m1, b.r2);
		long b1_used = solution.getResourceUsage(m2, b.r1);
		long b2_used = solution.getResourceUsage(m2, b.r2);

		long a1 = problem.machines[m1].capacities[b.r1] - a1_used;
		long a2 = problem.machines[m1].capacities[b.r2] - a2_used;
		long b1 = problem.machines[m2].capacities[b.r1] - b1_used;
		long b2 = problem.machines[m2].capacities[b.r2] - b2_used;

		long bal1 = a1 * b.target - b1;
		long bal2 = a2 * b.target - b2;
		if (bal1 < 0 && bal2 > 0) {
			// m2 ma koszt balansu, m1 nie ma
			return Math.min(Math.min(-bal1, bal2), a1_used * b.target + b2_used) * b.weight;
		} else if (bal1 > 0 && bal2 < 0) {
			// m1 ma koszt balansu, m2 nie ma		
			return Math.min(Math.min(-bal2, bal1), a2_used * b.target + b1_used) * b.weight;
		} else {
			// jesli obie maszyny maja koszt albo obie nie maja, to nic nie damy rady poprawic		
			return 0;
		}
	}

	static public long getOriginalBalanceCost(Solution ss, int m1, int m2, int b1) {
		Problem problem = ss.getProblem();
		return problem.computeBalanceCost(ss, problem.getBalance(b1), problem.getMachine(m1))
				+ problem.computeBalanceCost(ss, problem.getBalance(b1), problem.getMachine(m2));
	}

	static public long getOriginalLoadCost(Solution ss, int m1, int m2, int r) {
		Problem problem = ss.getProblem();
		return problem.computeLoadCost(ss, m1, r) + problem.computeLoadCost(ss, m2, r);
	}

	static public long getOptimisticBalanceCost(Solution ss, int m1, int m2, int b1) {
		Problem problem = ss.getProblem();
		Balance b = problem.getBalance(b1);
		long availr1 = problem.computeAvailableResources(ss, m1, b.r1) + problem.computeAvailableResources(ss, m2, b.r1);
		long availr2 = problem.computeAvailableResources(ss, m1, b.r2) + problem.computeAvailableResources(ss, m2, b.r2);
		return problem.computeBalanceValue(b.target, availr1, availr2);
	}

	static public long getOptimisticLoadCostDiff(Solution solution, int m1, int m2, int r) {
		SmartSolution ss = (SmartSolution) solution;
		Problem problem = ss.getProblem();
		long a = ss.getResourceUsage(m1, r);
		long b = ss.getResourceUsage(m2, r);
		long sa = problem.machines[m1].safetyCapacities[r];
		long sb = problem.machines[m2].safetyCapacities[r];

		long da = a - sa;
		long db = b - sb;
		if (db > 0 && da < 0) {
			return Math.min(-da, db) * problem.resources[r].loadCostWeight;
		} else if (db < 0 && da > 0) {
			return Math.min(da, -db) * problem.resources[r].loadCostWeight;
		}
		return 0;
	}

	static public long getOptimisticLoadCost(Solution ss, int m1, int m2, int r) {
		Problem problem = ss.getProblem();
		return problem.computeLoadCostNotWeighted(ss.getResourceUsage(m1, r) + ss.getResourceUsage(m2, r),
				problem.getMachine(m1).safetyCapacities[r] + problem.getMachine(m2).safetyCapacities[r])
				* problem.getResource(r).loadCostWeight;
	}

	// This is based on the general Lower Bound approach. Results identical as the one above
	static public long getOptimisticLoadCostDiff2(Solution solution, int m1, int m2, int r) {
		SmartSolution ss = (SmartSolution) solution;
		Problem problem = ss.getProblem();
		long a = ss.getResourceUsage(m1, r);
		long b = ss.getResourceUsage(m2, r);
		long sa = problem.machines[m1].safetyCapacities[r];
		long sb = problem.machines[m2].safetyCapacities[r];

		long lb = problem.resources[r].loadCostWeight * Math.max(a + b - sa - sb, 0);
		long curr = problem.resources[r].loadCostWeight * (Math.max(a - sa, 0) + Math.max(b - sb, 0));
		return curr - lb;
	}

	// This is based on the general Lower Bound approach. Results identical as the one above
	private static long getOptimisticBalanceDiff2(Solution solution, int m1, int m2, int bal) {
		Problem problem = solution.getProblem();
		Balance b = problem.getBalance(bal);
		long orig = getOriginalBalanceCost(solution, m1, m2, bal);

		long m1_r1 = problem.machines[m1].capacities[b.r1] - solution.getResourceUsage(m1, b.r1);
		long m2_r1 = problem.machines[m2].capacities[b.r1] - solution.getResourceUsage(m2, b.r1);
		long m1_r2 = problem.machines[m1].capacities[b.r2] - solution.getResourceUsage(m1, b.r2);
		long m2_r2 = problem.machines[m2].capacities[b.r2] - solution.getResourceUsage(m2, b.r2);

		long lb = Math.max(b.target * (m1_r1 + m2_r1) - (m1_r2 + m2_r2), 0) * b.weight;
		return orig - lb;
	}

	static public long getOptimisticImprovementForMachines(Solution ss, int[] machines) {
		Problem problem = ss.getProblem();
		
		if (machines.length == 2) {
			return computeOptimisticImprovementForMachines(ss, machines[0], machines[1]);
		}		

		long optimisticLoadImp = 0;
		for (int r = 0; r < problem.getNumResources(); ++r) {
			optimisticLoadImp += getLoadCost(ss, machines, r) - getOptimisticLoadCostLowerBound(ss, machines, r);
		}

		long optimisticBalanceImp = 0;
		for (int b = 0; b < problem.getNumBalances(); ++b) {
			optimisticBalanceImp += getBalanceCost(ss, machines, b) - getOptimisticBalanceCostLowerBound(ss, machines, b);
		}
		System.out.println("Optimistic cost = " + optimisticLoadImp + " " + optimisticBalanceImp);
				
		return optimisticLoadImp + optimisticBalanceImp;
	}

	private static long getOptimisticLoadCostLowerBound(Solution ss, int[] machines, int r) {
		Problem problem = ss.getProblem();

		long total = 0;
		for (int m = 0; m < machines.length; ++m)
			total += (ss.getResourceUsage(m, r) - problem.machines[m].safetyCapacities[r]);
		return problem.resources[r].loadCostWeight * (Math.max(total, 0));
	}

	private static long getLoadCost(Solution ss, int[] machines, int r) {
		Problem problem = ss.getProblem();

		long total = 0;
		for (int m = 0; m < machines.length; ++m)
			total += Math.max(ss.getResourceUsage(m, r) - problem.machines[m].safetyCapacities[r], 0);
		return problem.resources[r].loadCostWeight * total;
	}

	private static long getOptimisticBalanceCostLowerBound(Solution ss, int[] machines, int b) {
		Problem problem = ss.getProblem();
		Balance bal = problem.balances[b];

		long totalR1 = 0;
		long totalR2 = 0;
		for (int m = 0; m < machines.length; ++m) {
			totalR1 += (problem.machines[m].capacities[bal.r1] - ss.getResourceUsage(m, bal.r1));
			totalR2 += (problem.machines[m].capacities[bal.r2] - ss.getResourceUsage(m, bal.r2));
		}

		return bal.weight * Math.max(bal.target * totalR1 - totalR2, 0);
	}

	private static long getBalanceCost(Solution ss, int[] machines, int b) {
		Problem problem = ss.getProblem();
		Balance bal = problem.balances[b];
		
		long total = 0;
		for (int m = 0; m < machines.length; ++m) {
			long a1 = problem.machines[m].capacities[bal.r1] - ss.getResourceUsage(m, bal.r1);
			long a2 = problem.machines[m].capacities[bal.r2] - ss.getResourceUsage(m, bal.r2);
			total += Math.max(bal.target * a1 - a2, 0);
		}		
		return bal.weight * total;
	}
}