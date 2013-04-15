package put.roadef.tweaks;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.TweakOperator;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

public class DoubleHillClimber extends Solver implements TweakOperator, Setup {
	private HillClimber hc = new HillClimber();
	private Logger logger = Logger.getLogger(DoubleHillClimber.class);
	
	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		int notimproved = 0;
		long cost = solution.getCost();
		while (notimproved < 2) {
			hc.tweak(solution, deadline);
			if (solution.getCost() < cost) {
				cost = solution.getCost();
				notimproved = 0;
			} else {
				notimproved += 1;
				logger.info("Did not improved (" + notimproved + ")");
			}
		}
		return solution;
	}

	@Override
	public boolean isDeterministic() {
		return hc.isDeterministic();
	}

	@Override
	public boolean isGreedy() {
		return hc.isGreedy();
	}

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		return tweak(new SmartSolution(initialSolution), deadline);
	}
	
	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		hc.setup(configuration, base + ".hc");
	}
}
