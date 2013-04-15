package put.roadef.bnb;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import put.roadef.CandidateSolutionGenerator;
import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.Problem;
import put.roadef.SimpleSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

public class BranchAndBoundFinisher extends Solver implements CandidateSolutionGenerator, Setup {

	private int numUnassignedProcesses;

	public BranchAndBoundFinisher() {

	}

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		BranchAndBoundRandomImprover improver = new BranchAndBoundRandomImprover(
				numUnassignedProcesses, -1);

		return improver.tweak(initialSolution, deadline);
	}

	public Solution oldSolve(Problem problem, Solution initialSolution, Deadline deadline) {
		BranchAndBoundRecursiveSolver rsolver = new BranchAndBoundRecursiveSolver();

		Solution bestSolution = initialSolution.clone();
		long bestFitness = bestSolution.getCost();

		while (!deadline.hasExpired()) {
			PartialSolution partialSolution = generateRandomPartialSolution(bestSolution,
					numUnassignedProcesses);
			Solution candidateSolution = rsolver.solve(problem, partialSolution, bestFitness,
					deadline);

			long fitness = rsolver.getLowerBound();
			if (fitness < bestFitness) {
				bestSolution = candidateSolution;
				bestFitness = fitness;
			}
		}

		return bestSolution;
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		numUnassignedProcesses = configuration.getInt(base + ".num_unassigned_processes");
	}

	public static PartialSolution generatePartialSolution(ImmutableSolution initialSolution,
			int[] unassignedProcesses) {
		Problem problem = initialSolution.getProblem();
		int[] assignment = initialSolution.getAssignment();
		PartialSolution partialSolution = new PartialSolution(problem, assignment);

		for (int unassignedProcess : unassignedProcesses) {
			partialSolution.unAssign(problem.getProcess(unassignedProcess),
					problem.getMachine(assignment[unassignedProcess]));
		}

		return partialSolution;
	}

	public static PartialSolution generateRandomPartialSolution(ImmutableSolution initialSolution,
			int numRandomUnassigns) {
		int[] assignment = initialSolution.getAssignment();

		int[] draw = new int[assignment.length];
		for (int p = 0; p < assignment.length; p++) {
			draw[p] = p;
		}

		Random r = initialSolution.getProblem().getRandom();
		numRandomUnassigns = Math.min(numRandomUnassigns, assignment.length);
		int[] unassignedProcesses = new int[numRandomUnassigns];
		for (int i = 0; i < numRandomUnassigns; i++) {
			int index = r.nextInt(assignment.length - i);
			int randomProcess = draw[index];
			draw[index] = draw[assignment.length - i - 1];
			unassignedProcesses[i] = randomProcess;
		}

		return generatePartialSolution(initialSolution, unassignedProcesses);
	}

	public static void main(String args[]) {
		BranchAndBoundFinisher solver = new BranchAndBoundFinisher();
		//Problem problem = new Problem("data/Tests/model_test.txt", "data/Tests/assignment_test.txt");
		Problem problem = new Problem(new File("data/A/model_a1_4.txt"), new File("data/A/assignment_a1_4.txt"));
		Solution s = solver.solve(problem, new SimpleSolution(problem.getOriginalSolution()),
				new Deadline(300000));
		System.out.println("Final cost = " + s.getCost());
	}

	@Override
	public List<Solution> getCandidateSolutions(Solution initialSolution, int count,
			Deadline deadline) {
		List<Solution> candidateSolutions = new ArrayList<Solution>();
		BranchAndBoundSolver solver = new BranchAndBoundSolver();

		Problem problem = initialSolution.getProblem();
		int[] assignment = initialSolution.getAssignment();
		PartialSolution partialSolution = new PartialSolution(problem, assignment);

		int[] draw = new int[assignment.length];
		for (int candidate = 0; candidate < count; candidate++) {
			if (deadline.hasExpired()) {
				return candidateSolutions;
			}

			for (int p = 0; p < assignment.length; p++) {
				draw[p] = p;
			}

			Random r = problem.getRandom();
			for (int i = 0; i < numUnassignedProcesses && i < assignment.length; i++) {
				int index = r.nextInt(assignment.length - i);
				int randomProcess = draw[index];
				draw[index] = draw[assignment.length - i - 1];
				draw[assignment.length - i - 1] = randomProcess;
				partialSolution.unAssign(problem.getProcess(randomProcess),
						problem.getMachine(assignment[randomProcess]));
			}

			candidateSolutions.add(solver.solve(problem, partialSolution, deadline));

			for (int i = 0; i < numUnassignedProcesses && i < assignment.length; i++) {
				int chosenProcess = draw[assignment.length - i - 1];
				partialSolution.assign(problem.getProcess(chosenProcess),
						problem.getMachine(assignment[chosenProcess]));
			}
		}

		return candidateSolutions;
	}
}
