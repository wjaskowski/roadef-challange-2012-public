package put.roadef.bnb;

import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.Problem;
import put.roadef.Problem.Machine;
import put.roadef.Problem.Process;
import put.roadef.SimpleSolution;
import put.roadef.Solution;
import put.roadef.Solver;

public class BranchAndBoundRecursiveSolver extends Solver {

	private ImmutableSolution bestSolution;
	private long bestFitness;
	private Deadline deadline;
	private Problem problem;
	private int numVisitedTerminals;
	
	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		return solve(problem, new PartialSolution(problem), deadline);
	}

	public void setLowerBound(long fitness) {
		this.bestFitness = fitness;
	}

	public long getLowerBound() {
		return bestFitness;
	}

	public int getNumVisitedTerminals() {
		return numVisitedTerminals;
	}

	public Solution solve(Problem problem, PartialSolution partialSolution, Deadline deadline) {
		return solve(problem, partialSolution, problem.getOriginalFitness(), deadline);
	}

	public Solution solve(Problem problem, PartialSolution partialSolution, long bestSoFar,
			Deadline deadline) {
		ImmutableSolution lightSolution = lightSolve(problem, partialSolution, bestSoFar, deadline);
		if  (lightSolution == null) {
			return new SimpleSolution(problem.getOriginalSolution());
		} else {
			return new SimpleSolution(lightSolution);
		}
	}

	public ImmutableSolution lightSolve(Problem problem, PartialSolution partialSolution,
			long bestSoFar, Deadline deadline) {
		this.deadline = deadline;
		this.bestSolution = null;
		this.problem = problem;
		this.numVisitedTerminals = 0;
		this.bestFitness = bestSoFar;
		
		rsolve(partialSolution);

		return bestSolution;
	}

	private void rsolve(PartialSolution partialSolution) {
		if (deadline.hasExpired()) {
			return;
		}

		Process process = partialSolution.getUnassignedProcesses().first();
		for (int m = 0; m < problem.getNumMachines(); m++) {
			Machine machine = problem.getMachine(m);
			if (partialSolution.canBeAssigned(process, machine)) {
				partialSolution.assign(process, machine);
				if (partialSolution.isTerminal()) {
					numVisitedTerminals++;
					long fitness = partialSolution.getCost();
					if (fitness < bestFitness) {
						bestFitness = fitness;
						bestSolution = partialSolution.getSolutionCopy();
					}
				} else if (partialSolution.getLowerBound() < bestFitness) {
					rsolve(partialSolution);
				}
				partialSolution.unAssign(process, machine);
			}
		}
	}
}
