package put.roadef.neighborhoods;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Problem.Location;
import put.roadef.SmartSolution;
import put.roadef.Solution;

/**
 * Neighbor = a feasible solution made by swapping two processes from the same
 * location.
 */
public class SwapProcessesInLocationNeighborhood implements Neighborhood<Solution> {
	
	@Override
	public void init(Problem problem) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void visit(Solution solution, Deadline deadline, NeighborProcessor processor) {
		Problem problem = solution.getProblem();
		for (int l = 0; l < problem.getNumLocations(); ++l) {
			Location location = problem.getLocation(l);
			IntArrayList processesInLocation = new IntArrayList();
			for (int m = 0; m < location.machines.length; ++m) {
				int mid = solution.getMachine(m);
				processesInLocation.addAll(((SmartSolution) solution).processesInMachine[mid]);
			}
			for (int p1 = 0; p1 < processesInLocation.size(); ++p1) {
				int p1id = processesInLocation.get(p1);
				for (int p2 = p1 + 1; p2 < processesInLocation.size(); ++p2) {
					//TODO: This may be to often (performance of this check)
					if (deadline.hasExpired())
						return;

					int p2id = processesInLocation.get(p2);
					int m1 = solution.getMachine(p1id);
					int m2 = solution.getMachine(p2id);
					assert solution.isFeasible();
					solution.moveProcess(p1id, m2);
					solution.moveProcess(p2id, m1);
					if (!solution.isFeasible()) {
						solution.moveProcess(p1id, m1);
						solution.moveProcess(p2id, m2);
						continue;
					}
					Decision decision = processor.processNeighbor(solution);
					if (decision == Decision.Stop)
						return;
					if (decision == Decision.Reject) {
						//TODO: This could be optimized (we do not have to move p1 back every time). But this is tricky
						solution.moveProcess(p1id, m1);
						solution.moveProcess(p2id, m2);
					}
				}
			}
		}
	}

	@Override
	public boolean runsOnTheSpot() {
		return true;
	}

	@Override
	public boolean isDeterministic() {
		return true;
	}
}
