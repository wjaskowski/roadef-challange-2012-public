package put.roadef.bnb;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.Problem;
import put.roadef.SimpleSolution;
import put.roadef.Solution;
import put.roadef.TweakOperator;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;
import put.roadef.tweaks.OverloadedMachineImprover;

public class BranchAndBoundImprover implements TweakOperator, Setup {

	private int maxNumRetries;
	private int numUnassignedProcesses;

	private BranchAndBoundSolver solver;

	private OverloadedMachineImprover overloadedNeighborhood = new OverloadedMachineImprover();

	public BranchAndBoundImprover() {
		this.solver = new BranchAndBoundSolver();
	}

	public BranchAndBoundImprover(int numProcesses, int retries) {
		numUnassignedProcesses = numProcesses;
		maxNumRetries = retries;
	}

	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		//System.out.println("Perturbing...");
		Solution bestSolution = solution.clone();
		int retriesWithoutImprovement = 0;
		long bestFitness = bestSolution.getCost();

		ImmutableSolution bestDifferentSolution = solution.clone();
		long bestDifference = 0;

		while (!deadline.hasExpired()) {
			Solution candidateSolution = solver.solve(solution.getProblem(), BranchAndBoundFinisher
					.generateRandomPartialSolution(bestSolution, numUnassignedProcesses), deadline);
			long fitness = candidateSolution.getCost();
			//			System.out.println("Fitness after BnB(" + numUnassignedProcesses + ") = " + fitness);
			if (fitness < bestFitness) {
				retriesWithoutImprovement = 0;
				bestSolution = candidateSolution;
				bestFitness = fitness;
				return bestSolution;
			} else if (++retriesWithoutImprovement == maxNumRetries) {
				break;
			} else {
				ImmutableSolution differentSolution = solver.getSolutionDifferentThan(bestSolution);
				int difference = solver.countDifference(differentSolution, bestSolution);
				if (difference > bestDifference) {
					bestDifference = difference;
					bestDifferentSolution = differentSolution;
				} else if (difference == bestDifference
						&& differentSolution.getCost() < bestDifferentSolution.getCost()) {
					bestDifferentSolution = differentSolution;
				}
			}
		}

		//System.out.println("Best Difference = " + bestDifference);
		//System.out.println(bestDifferentSolution.getCost());
		return new SimpleSolution(bestDifferentSolution);
	}

	@Override
	public boolean isDeterministic() {
		return false;
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		maxNumRetries = configuration.getInt(base + ".max_num_retries");
		numUnassignedProcesses = configuration.getInt(base + ".num_unassigned_processes");
	}

	@SuppressWarnings("unused")
	private int checkBruteForceSolutions(Solution solution, int[] unassignedProcesses) {
		int numBruteForceSolutions = 0;
		int m = solution.getProblem().getNumMachines();
		int[] assignment = solution.getAssignment().clone();

		for (int i = 0; i < m; i++) {
			for (int j = 0; j < m; j++) {
				for (int k = 0; k < m; k++) {
					assignment[unassignedProcesses[0]] = i;
					assignment[unassignedProcesses[1]] = j;
					assignment[unassignedProcesses[2]] = k;
					Solution s = new SimpleSolution(solution.getProblem(), assignment);
					if (s.isFeasible()) {
						numBruteForceSolutions++;
					}
				}
			}
		}
		return numBruteForceSolutions;
	}

	public int[] getRandomizedOverloadingProcesses(Solution solution) {
		Problem problem = solution.getProblem();
		int[] assignment = solution.getAssignment();

		long[][] overload = new long[problem.getNumMachines()][problem.getNumResources()];
		for (int r = 0; r < problem.getNumResources(); r++) {
			long weight = problem.getResource(r).loadCostWeight;
			for (int m = 0; m < problem.getNumMachines(); m++) {
				overload[m][r] = weight
						* (solution.getResourceUsage(m, r) - problem.getMachine(m).safetyCapacities[r]);
			}
		}

		long[] maxSumRequirements = new long[problem.getNumMachines()];
		int[] maxSumRequirementsProcess = new int[problem.getNumMachines()];

		for (int p = 0; p < assignment.length; p++) {
			int machine = assignment[p];
			long sumRequirements = 0;
			for (int r = 0; r < problem.getNumResources(); r++) {
				if (overload[machine][r] > 0) {
					sumRequirements += problem.getProcess(p).requirements[r];
				}
			}

			if (sumRequirements > maxSumRequirements[machine]) {
				maxSumRequirements[machine] = sumRequirements;
				maxSumRequirementsProcess[machine] = p;
			}
		}

		int[] draw = new int[problem.getNumMachines()];
		for (int m = 0; m < draw.length; m++) {
			draw[m] = m;
		}

		Random r = solution.getProblem().getRandom();
		int numRandomUnassigns = Math.min(numUnassignedProcesses, draw.length);
		int[] unassignedProcesses = new int[numRandomUnassigns];
		for (int i = 0; i < numRandomUnassigns; i++) {
			int index = r.nextInt(draw.length - i);
			int randomMachine = draw[index];
			draw[index] = draw[draw.length - i - 1];
			unassignedProcesses[i] = maxSumRequirementsProcess[randomMachine];
		}

		return unassignedProcesses;
	}

	public int[] getOverloadingProcesses(Solution solution) {
		List<Problem.Process> criticalProcesses = new ArrayList<Problem.Process>();
		List<Problem.Process> otherProcesses = new ArrayList<Problem.Process>();

		overloadedNeighborhood.findCriticalProcesses(solution, criticalProcesses, otherProcesses);
		int numUnassignedOverloadingProcesses = Math.min(criticalProcesses.size(),
				numUnassignedProcesses / 2);
		int numUnassignedOtherProcesses = Math.min(otherProcesses.size(), numUnassignedProcesses
				- numUnassignedOverloadingProcesses);
		int[] unassignedProcesses = new int[numUnassignedOverloadingProcesses
				+ numUnassignedOtherProcesses];

		for (int i = 0; i < unassignedProcesses.length; i++) {
			if (i < numUnassignedOverloadingProcesses) {
				unassignedProcesses[i] = criticalProcesses.get(i).id;
			} else {
				unassignedProcesses[i] = otherProcesses.get(i - numUnassignedOverloadingProcesses).id;
			}
		}

		return unassignedProcesses;
	}

	@Override
	public boolean isGreedy() {
		return true;
	}
}
