package put.roadef.solvers;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Solution;
import put.roadef.tweaks.MoveServiceNeighborhood;

/**
 * Iterate over services. For a service, improve it using serviceImprover. Do it while no further improvement is obtained.
 */
public class CustomIpSolver extends HeuristicIpSolver {

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		MoveServiceNeighborhood serviceImprover = new MoveServiceNeighborhood();
		Solution sol = initialSolution.clone();
		boolean timeout = false;
		while (!timeout && ((MoveServiceNeighborhood) serviceImprover).canImprove()) {
			sol = serviceImprover.tweak(sol, deadline);
			if (deadline.getShortenedBy(1000).hasExpired())
				timeout = true;
		}

		return sol;
	}
}
