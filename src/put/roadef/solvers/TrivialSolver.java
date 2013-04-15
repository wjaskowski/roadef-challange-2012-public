package put.roadef.solvers;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Solution;
import put.roadef.Solver;

public class TrivialSolver extends Solver {

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		return initialSolution.clone();
	}

}
