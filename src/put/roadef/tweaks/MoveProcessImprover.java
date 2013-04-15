package put.roadef.tweaks;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Solution;
import put.roadef.TweakOperator;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

/**
 * Tries to improve the current solution. Moves every process to a best (in
 * terms of global cost function) machine for this process. Iterates over
 * processes only once.
 */
public class MoveProcessImprover implements TweakOperator, Setup {
	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		Problem problem = solution.getProblem();
		long fitness = solution.getCost();
		Solution bestSolution = solution.clone();

		for (int p = 0; p < problem.getNumProcesses(); ++p) {
			for (int m = 0; m < problem.getNumMachines(); ++m) {
				int oldm = bestSolution.getMachine(p);
				if (oldm == m)
					continue;

				if (deadline.hasExpired()) {
					return bestSolution;
				}

				bestSolution.moveProcess(p, m);
				boolean foundBetter = false;
				if (bestSolution.isFeasible()) {
					long newFitness = bestSolution.getCost();
					if (newFitness < fitness) {
						fitness = newFitness;
						foundBetter = true;
					}
				}

				if (!foundBetter) {
					bestSolution.moveProcess(p, oldm);
				}
			}
		}

		return bestSolution;
	}

	@Override
	public boolean isDeterministic() {
		return true;
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		//No parameters
	}

	@Override
	public boolean isGreedy() {
		return false;
	}
}
