package put.roadef.solvers;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.ip.MipFastModel;

public class SimplyCplexSolver extends Solver {
	private MipFastModel model = new MipFastModel("CplexFastMipSolver");

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		int services[] = new int[problem.getNumServices()];
		for (int i = 0; i < problem.getNumServices(); ++i)
			services[i] = i;
		return model.modifyAssignmentsForServices(problem, initialSolution, services,
				deadline.getShortenedBy(3000),true);
	}
}
