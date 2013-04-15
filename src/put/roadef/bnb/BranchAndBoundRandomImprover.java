package put.roadef.bnb;

import java.util.Random;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.MyArrayUtils;
import put.roadef.Problem;
import put.roadef.SimpleSolution;
import put.roadef.Solution;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;
import put.roadef.tweaks.RandomizedTweakOperator;

public class BranchAndBoundRandomImprover implements RandomizedTweakOperator, Setup {

	private int maxNumRetries;
	private int numUnassignedProcesses;
	private boolean greedy;
	
	protected Random random;
	
	private Logger logger = Logger.getLogger(BranchAndBoundRandomImprover.class);
	
	private BranchAndBoundRecursiveSolver solver;

	public BranchAndBoundRandomImprover() {
		this.solver = new BranchAndBoundRecursiveSolver();
	}

	public BranchAndBoundRandomImprover(int numProcesses, int retries) {
		this.solver = new BranchAndBoundRecursiveSolver();
		this.numUnassignedProcesses = numProcesses;
		this.maxNumRetries = retries;
		this.greedy = false;
	}

	@Override
	public Solution tweak(Solution solution, Deadline deadline, Random random) {
		this.random = random;
		
		int retriesWithoutImprovement = 0;
		Problem problem = solution.getProblem();
		int[] assignment = solution.getAssignment();
		PartialSolution partialSolution = new PartialSolution(problem, assignment);
		long bestFitness = partialSolution.getCost();
		long visitedTerminals = 0;
		
		while (!deadline.hasExpired()) {
			boolean foundBetter = false;
			int[] unassigned = generateRandomArray(problem, partialSolution, numUnassignedProcesses);
			for (int process : unassigned) {
				partialSolution.unAssign(problem.getProcess(process),
						problem.getMachine(assignment[process]));
			}

			ImmutableSolution candidateSolution = solver.lightSolve(solution.getProblem(),
					partialSolution, bestFitness, deadline);
			visitedTerminals += solver.getNumVisitedTerminals();
			
			if (candidateSolution != null) {
				foundBetter = true;
				retriesWithoutImprovement = 0;
				bestFitness = candidateSolution.getCost();
				for (int process : unassigned) {
					assignment[process] = candidateSolution.getMachine(process);
				}
			}

			for (int process : unassigned) {
				partialSolution.assign(problem.getProcess(process),
						problem.getMachine(assignment[process]));
			}

			if ((greedy && foundBetter) || (++retriesWithoutImprovement == maxNumRetries)) {
				break;
			}
		}

		logger.info("Improver has visited " + visitedTerminals + " terminals");
		return new SimpleSolution(partialSolution.getSolutionCopy());
	}
	
	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		return tweak(solution, deadline, solution.getProblem().getRandom());
	}

	protected int[] generateRandomArray(Problem problem, PartialSolution partialSolution, int length) {
		int range = problem.getNumProcesses();
		int[] draw = new int[range];
		for (int i = 0; i < range; i++) {
			draw[i] = i;
		}

		return MyArrayUtils.random(draw, random, length);
	}

	public Solution oldTweak(Solution solution, Deadline deadline) {
		int retriesWithoutImprovement = 0;
		ImmutableSolution bestSolution = solution.clone();
		long bestFitness = bestSolution.getCost();

		while (!deadline.hasExpired()) {
			ImmutableSolution candidateSolution = solver.lightSolve(solution.getProblem(),
					BranchAndBoundFinisher.generateRandomPartialSolution(bestSolution,
							numUnassignedProcesses), bestFitness, deadline);

			long fitness = solver.getLowerBound();
			if (fitness < bestFitness) {
				retriesWithoutImprovement = 0;
				bestSolution = new SimpleSolution(candidateSolution);
				bestFitness = fitness;
			} else if (++retriesWithoutImprovement == maxNumRetries) {
				break;
			}
		}

		return new SimpleSolution(bestSolution);
	}

	@SuppressWarnings("unused")
	private int[] generateRandomUnassignment(Problem problem) {
		int numGeneratedProcesses = 0;
		Random random = problem.getRandom();
		int[] unassigned = new int[numUnassignedProcesses];

		while (numGeneratedProcesses != numUnassignedProcesses) {
			int randomProcess = random.nextInt(problem.getNumProcesses());
			boolean repeated = false;
			for (int i = 0; i < numGeneratedProcesses; i++) {
				if (unassigned[i] == randomProcess) {
					repeated = true;
					break;
				}
			}

			if (!repeated) {
				unassigned[numGeneratedProcesses] = randomProcess;
				numGeneratedProcesses++;
			}
		}

		return unassigned;
	}

	@Override
	public boolean isDeterministic() {
		return false;
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		greedy = configuration.getBoolean(base + ".greedy", true);
		maxNumRetries = configuration.getInt(base + ".max_num_retries");
		numUnassignedProcesses = configuration.getInt(base + ".num_unassigned_processes");
	}

	@Override
	public boolean isGreedy() {
		return greedy;
	}
}
