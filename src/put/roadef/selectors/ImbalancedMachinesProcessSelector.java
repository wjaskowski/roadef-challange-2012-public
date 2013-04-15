package put.roadef.selectors;

import put.roadef.MachinePair;
import put.roadef.MyArrayUtils;
import put.roadef.Problem;
import put.roadef.Problem.Balance;
import put.roadef.Problem.Machine;
import put.roadef.Solution;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

public class ImbalancedMachinesProcessSelector extends OptimalMachinesProcessSelector implements Setup {

	private int numImbalancedMachines;

	private int pairPositionToConsider;
	private boolean recalculate;

	private int machines[];
	private long balanceCosts[];
	private MachinePair machinePairs[];

	@Override
	public void reset() {
		recalculate = true;
	}

	@Override
	protected int[] findOptimalMachines(Solution solution) {
		Problem problem = solution.getProblem();

		if (recalculate) {
			recalculate = false;
			pairPositionToConsider = -1;

			int numImbalancedMachines = Math.min(this.numImbalancedMachines, problem.getNumMachines() / 2);

			machines = new int[problem.getNumMachines()];
			for (int m = 0; m < machines.length; m++) {
				machines[m] = m;
			}

			balanceCosts = new long[problem.getNumMachines()];
			machinePairs = new MachinePair[numImbalancedMachines * numImbalancedMachines];

			for (int m = 0; m < problem.getNumMachines(); m++) {
				balanceCosts[m] = 0;
				Machine machine = problem.getMachine(m);
				for (int b = 0; b < problem.getNumBalances(); b++) {
					Balance balance = problem.getBalance(b);

					long available1 = problem.computeAvailableResources(solution, machine, balance.r1);
					long available2 = problem.computeAvailableResources(solution, machine, balance.r2);
					balanceCosts[m] += problem.computeBalanceValue(balance.target, available1, available2) * balance.weight;
				}
			}

			MyArrayUtils.sort(machines, balanceCosts);

			int counter = 0;
			for (int m1 = 0; m1 < numImbalancedMachines; m1++) {
				for (int m2 = 1; m2 <= numImbalancedMachines; m2++) {
					machinePairs[counter++] = new MachinePair(machines[m1], machines[machines.length - m2]);
				}
			}
		}

		if (++pairPositionToConsider == machinePairs.length) {
			recalculate = true;
			return new int[] {};
		}

		MachinePair pair = machinePairs[pairPositionToConsider];
		return new int[] { pair.m1, pair.m2 };
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		numImbalancedMachines = configuration.getInt(base + ".num_imbalanced_machines", 1);
	}

}
