package put.roadef.solvers;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.tweaks.HillClimber;

public class IpHillMixedSolver extends Solver {

	private static final int HILL_CLIMBING_TIME_MILLIS = 12000;

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {

		HillClimber hillSolver = new HillClimber();
		HeuristicIpSolver ipSolver = new HeuristicIpSolver();
		Solution solution = initialSolution.clone();

		solution = ipSolver.solve(problem, solution,
				deadline.getShortenedBy(HILL_CLIMBING_TIME_MILLIS));

		return hillSolver.solve(problem, solution, deadline);
	}
}
