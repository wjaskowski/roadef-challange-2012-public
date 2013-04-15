package put.roadef.selectors;

import put.roadef.MachinePair;
import put.roadef.MyArrayUtils;
import put.roadef.Problem;
import put.roadef.Solution;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

public class OverloadedMachinesProcessSelector extends OptimalMachinesProcessSelector implements Setup {

	private int numOverloadedMachines;
	private int numUnderloadedMachines;

	private int pairPositionToConsider;
	private boolean recalculate;

	private int machines[];
	private long overloads[];
	private MachinePair machinePairs[];

	@Override
	protected int[] findOptimalMachines(Solution solution) {
		Problem problem = solution.getProblem();

		if (recalculate) {
			recalculate = false;
			pairPositionToConsider = -1;

			int numOverloadedMachines = Math.min(this.numOverloadedMachines, problem.getNumMachines() / 2);
			int numUnderloadedMachines = Math.min(this.numUnderloadedMachines, problem.getNumMachines() / 2);

			machines = new int[problem.getNumMachines()];
			for (int m = 0; m < machines.length; m++) {
				machines[m] = m;
			}

			overloads = new long[problem.getNumMachines()];
			machinePairs = new MachinePair[numOverloadedMachines * numUnderloadedMachines];
			
			for (int m = 0; m < problem.getNumMachines(); m++) {
				overloads[m] = 0;
				for (int r = 0; r < problem.getNumResources(); r++) {
					overloads[m] += solution.getResourceUsage(m, r) - problem.getMachine(m).safetyCapacities[r];
				}
			}

			MyArrayUtils.sort(machines, overloads);

			int counter = 0;
			for (int overloaded = 0; overloaded < numOverloadedMachines; overloaded++) {
				for (int underloaded = 1; underloaded <= numUnderloadedMachines; underloaded++) {
					machinePairs[counter++] = new MachinePair(machines[overloaded], machines[machines.length - underloaded]);
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
	public void reset() {
		recalculate = true;
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		numOverloadedMachines = configuration.getInt(base + ".num_overloaded_machines", 1);
		numUnderloadedMachines = configuration.getInt(base + ".num_underloaded_machines", 1);
	}
}
