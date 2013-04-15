package put.roadef.neighborhoods;

import java.util.Random;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Solution;

/**
 * Neighbor = a feasible solution made by moving one process to an optimal
 * machine. Neighborhood.size < NumProcesses.
 */
public class AllProcessesNeighborhoodRandom implements Neighborhood<Solution> {
	public AllProcessesNeighborhoodRandom() {
	}
	
	@Override
	public void init(Problem problem) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Solution solution, Deadline deadline, NeighborProcessor processor) {
		Problem problem = solution.getProblem();
		Random random = problem.getRandom();
		int[] pid = new int[problem.getNumProcesses()];
		for (int p = 0; p < pid.length; ++p)
		{
			pid[p] = p;
		}
		
		//for (int p = 0; p < problem.getNumProcesses(); ++p) {
		for (int pp = pid.length; pp > 0; pp--) {
			final int idx = random.nextInt(pp);
			final int p = pid[idx];
			pid[idx] = pid[pp-1];
			
			int oldMachine = solution.getMachine(p);
			int bestMachine = -1;// = solution.getMachine(p);
			long bestCost = -1;// = solution.getCost();
			for (int m = 0; m < problem.getNumMachines(); ++m) {
				if (m == oldMachine)
					continue;
				solution.moveProcess(p, m);
				if (!solution.isFeasible())
					continue;

				long cost = solution.getCost();
				if (bestCost == -1 || bestCost > cost) {
					bestCost = cost;
					bestMachine = m;
				}
			}
			if (bestCost == -1)
				bestMachine = oldMachine;
			
			solution.moveProcess(p, bestMachine);
			
			// If we did not change anything, then this is not a proper neighbor.
			if (bestMachine != oldMachine) {
				Decision decision = processor.processNeighbor(solution);
				if (decision == Decision.Stop)
					return;
				// If rejected then move process to the old machine
				if (decision == Decision.Reject)
					solution.moveProcess(p, oldMachine);
			}
			
			// I do not want to check for the deadline to often 
			if (deadline.hasExpired())
				return;
		}
	}

	@Override
	public boolean runsOnTheSpot() {
		return true;
	}

	@Override
	public boolean isDeterministic() {
		return false;
	}
}
