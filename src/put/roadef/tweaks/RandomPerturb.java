package put.roadef.tweaks;

import java.util.Random;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Solution;
import put.roadef.TweakOperator;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

/**
 * Tries to heavily perturb the current solution by a random walk (feasible solutions only). 
 */
public class RandomPerturb implements TweakOperator, Setup {
	private int numPerturbAttempts;
	
	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		Problem problem = solution.getProblem();
		Random random = problem.getRandom();

		int ass[] = solution.getAssignment();
		for (int i=0; i<numPerturbAttempts; ++i) {
			int p = random.nextInt(problem.getNumProcesses());
			
			int oldm = solution.getMachine(p);
			int machine = random.nextInt(problem.getNumMachines());
			for (int j = 0; j < problem.getNumMachines(); ++j, machine = (machine + 1) % problem.getNumMachines()) {
				if (machine == ass[p] || machine == oldm)
					continue;
				
				if (deadline.hasExpired())
					return solution;
				
				solution.moveProcess(p, machine);
				if (solution.isFeasible())
					break;
			}
			if (!solution.isFeasible())
				solution.moveProcess(p, oldm);
		}
		return solution;
	}

	@Override
	public boolean isDeterministic() {
		return false;
	}
	
	@Override
	public void setup(RoadefConfiguration configuration, String base) {		
		numPerturbAttempts = configuration.getInt(base + ".num_perturbs");
	}

	@Override
	public boolean isGreedy() {
		return false;
	}
}
