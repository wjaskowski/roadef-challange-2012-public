package put.roadef.solvers;

import java.util.List;

import put.roadef.CandidateSolutionGenerator;
import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.bnb.BranchAndBoundFinisher;
import put.roadef.tweaks.HillClimber;

public class ParallelHillClimbingSolver extends Solver {

	private static final int NUM_SOLUTIONS = 10;

	private CandidateSolutionGenerator generator;

	public ParallelHillClimbingSolver() {
		generator = new BranchAndBoundFinisher();
	}

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		HillClimber hillSolver = new HillClimber();
		Solution bestSolution = hillSolver.solve(problem, initialSolution, deadline);
		long bestFitness = bestSolution.getCost();

		while (!deadline.hasExpired()) {
			List<Solution> candidateSolutions = generator.getCandidateSolutions(bestSolution,
					NUM_SOLUTIONS, deadline);

			for (Solution s : candidateSolutions) {
				Solution optimizedCandidate = hillSolver.solve(problem, new SmartSolution(s),
						deadline);
				
				long fitness = optimizedCandidate.getCost();
				if (fitness < bestFitness) {
					bestFitness = fitness;
					bestSolution = optimizedCandidate;
				}
			}
		}
		
		return bestSolution;
	}

}
