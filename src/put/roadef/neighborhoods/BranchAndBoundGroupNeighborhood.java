package put.roadef.neighborhoods;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.Problem;
import put.roadef.Solution;
import put.roadef.bnb.BranchAndBoundRecursiveSolver;
import put.roadef.bnb.PartialSolution;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;
import put.roadef.selectors.GroupProcessSelector;

public class BranchAndBoundGroupNeighborhood implements Neighborhood<Solution>, Setup {

	private BranchAndBoundRecursiveSolver solver;
	private GroupProcessSelector groupProcessSelector;

	public BranchAndBoundGroupNeighborhood() {
		solver = new BranchAndBoundRecursiveSolver();
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		groupProcessSelector = (GroupProcessSelector) (configuration.getInstanceAndSetup(base + ".process_selector"));
	}

	@Override
	public void init(Problem problem) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(Solution solution, Deadline deadline, NeighborProcessor processor) {

		Problem problem = solution.getProblem();
		int[] assignment = solution.getAssignment();
		PartialSolution partialSol = new PartialSolution(problem, assignment);
		long bestFitness = partialSol.getCost();

		List<IntList> groups = groupProcessSelector.getProcessesGroups(solution);
		for (IntList processes : groups) {
			for (int p : processes) {
				partialSol.unAssign(problem.getProcess(p), problem.getMachine(assignment[p]));
			}
			
			ImmutableSolution candidateSolution = solver.lightSolve(problem, partialSol, bestFitness, deadline);
			if (candidateSolution != null) {
				bestFitness = candidateSolution.getCost();
			} else {
				candidateSolution = solution;
			}
			
			Decision decision = processor.processNeighbor(candidateSolution);
			if (decision == Decision.Stop) {
				break;
			} else if (decision == Decision.Accept) {
				for (int p : processes) {
					assignment[p] = candidateSolution.getMachine(p);
				}
			}

			for (int p : processes) {
				partialSol.assign(problem.getProcess(p), problem.getMachine(assignment[p]));
			}
		}
	}

	@Override
	public boolean runsOnTheSpot() {
		return false;
	}

	@Override
	public boolean isDeterministic() {
		return groupProcessSelector.isDeterministic();
	}

}
