package put.roadef.bnb;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.Problem;
import put.roadef.Problem.Machine;
import put.roadef.Problem.Process;
import put.roadef.SimpleSolution;
import put.roadef.Solution;
import put.roadef.Solver;

public class BranchAndBoundSolver extends Solver {

	private static final int NUM_CANDIDATE_SOLUTIONS = 10;

	/**
	 * Stack to remember a path from the root of the search tree to the current
	 * partial solution..
	 */
	private LinkedList<Node> stack = new LinkedList<Node>();

	/**
	 * Candidate solutions that could be used to start a population algorithm..
	 */
	private TreeMap<Long, ImmutableSolution> candidateSolutions = new TreeMap<Long, ImmutableSolution>();

	private boolean orderMatters = false;

	private long numFoundFeasibleSolutions = 0;

	private long bestFitness;

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		return solve(problem, new PartialSolution(problem), deadline);
	}

	public Solution solve(Problem problem, PartialSolution partialSolution, Deadline deadline) {
		stack.clear();
		candidateSolutions.clear();
		numFoundFeasibleSolutions = 0;
		bestFitness = problem.getOriginalFitness();

		boolean recurse = true;
		stack.push(new Node(null, null));

		while (!deadline.hasExpired()) {
			//currently considered process and machine
			Process lastVisitedProcess = null;
			Machine lastVisitedMachine = null;

			//are we popping from the stack (backtracking step)?
			if (!recurse) {
				Node previousMove = stack.pop();
				if (stack.isEmpty()) {
					break;
				}

				partialSolution.unAssign(previousMove.process, previousMove.machine);
				lastVisitedProcess = previousMove.process;
				lastVisitedMachine = previousMove.machine;
			}

			//if we do not find any feasible assignment, we will have to backtrack
			recurse = false;

			loop: for (Process process : partialSolution.getSortedProcesses(lastVisitedProcess)) {
				Iterator<Machine> machineIterator = partialSolution.getFeasibleMachineIterator(
						process, lastVisitedMachine);
				while (machineIterator.hasNext()) {
					lastVisitedMachine = machineIterator.next();
					if (partialSolution.canBeAssigned(process, lastVisitedMachine)) {
						partialSolution.assign(process, lastVisitedMachine);
						if (partialSolution.isTerminal()) {
							addCandidateSolution(partialSolution);
							partialSolution.unAssign(process, lastVisitedMachine);
						} else {
							//long lowerBound = partialSolution.getLowerBound();
							//if (lowerBound < bestFitness) {
								stack.push(new Node(process, lastVisitedMachine));
								recurse = true;
								break loop;
							// else {
							//	partialSolution.unAssign(process, lastVisitedMachine);
							//}
						}
					}
				}

				if (orderMatters && lastVisitedMachine != null) {
					lastVisitedMachine = null;
				} else {
					break;
				}
			}
		}

		if (candidateSolutions.isEmpty()) {
			return null;//return new SimpleSolution(problem.getOriginalSolution());
		} else {
			return new SimpleSolution(candidateSolutions.firstEntry().getValue());
		}
	}

	private void addCandidateSolution(PartialSolution partialSolution) {
		numFoundFeasibleSolutions++;
		long cost = partialSolution.getCost();

		bestFitness = Math.min(bestFitness, cost);
		if (candidateSolutions.size() >= NUM_CANDIDATE_SOLUTIONS) {
			if (candidateSolutions.lastKey() > cost) {
				candidateSolutions.remove(candidateSolutions.lastKey());
				candidateSolutions.put(cost, partialSolution.getSolutionCopy());
			}
		} else {
			candidateSolutions.put(cost, partialSolution.getSolutionCopy());
		}
	}

	public long getNumFoundSolutions() {
		return numFoundFeasibleSolutions;
	}

	private static class Node {
		public Process process;
		public Machine machine;

		public Node(Process process, Machine machine) {
			this.process = process;
			this.machine = machine;
		}

		@Override
		public String toString() {
			return process + " -> " + machine;
		}
	}

	public static void main(String args[]) {
		BranchAndBoundSolver solver = new BranchAndBoundSolver();
		Problem problem = new Problem(new File("data/Tests/model_test.txt"), new File("data/Tests/assignment_test.txt"));
		solver.solve(problem, new SimpleSolution(problem.getOriginalSolution()), new Deadline(
				300000));

	}

	public ImmutableSolution getSolutionDifferentThan(Solution solution) {
		int bestDifference = 0;
		ImmutableSolution bestDifferent = solution.clone();

		for (Long fitness : candidateSolutions.keySet()) {
			ImmutableSolution different = candidateSolutions.get(fitness);
			int diff = countDifference(different, solution);
			if (diff > bestDifference
					|| (diff == bestDifference && fitness < bestDifferent.getCost())) {
				bestDifference = diff;
				bestDifferent = different;
			}
		}
		
		return bestDifferent;
	}

	public int countDifference(ImmutableSolution different, ImmutableSolution bestSolution) {
		int[] a1 = different.getAssignment();
		int[] a2 = bestSolution.getAssignment();

		int diff = 0;
		for (int i = 0; i < a1.length; i++) {
			if (a1[i] != a2[i]) {
				diff++;
			}
		}
		return diff;
	}
}