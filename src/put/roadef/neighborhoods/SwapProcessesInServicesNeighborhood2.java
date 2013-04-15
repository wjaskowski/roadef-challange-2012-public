package put.roadef.neighborhoods;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;

/**
 * Neighbor = a feasible solution made by swapping two processes from the same
 * service.
 */
public class SwapProcessesInServicesNeighborhood2 implements Neighborhood<Solution> {

	private IntArrayList processes;

	@Override
	public void init(Problem problem) {
	}

	@Override
	public void visit(Solution solution, Deadline deadline, put.roadef.neighborhoods.Neighborhood.NeighborProcessor processor) {
		Problem problem = solution.getProblem();

		SmartSolution ss = SmartSolution.promoteToSmartSolution(solution);

		for (int p1id : processes) {
			int m1 = ss.getMachine(p1id);

			int s1 = problem.getProcess(p1id).service;
			for (int p2id : problem.getService(s1).processes) {
				int m2 = ss.getMachine(p2id);

				ss.moveProcess(p1id, m2);
				ss.moveProcess(p2id, m1);

				if (!ss.isFeasible()) {
					ss.moveProcess(p2id, m2);
					continue;
				}

				Decision decision = processor.processNeighbor(ss);
				if (decision == Decision.Accept) {
					m1 = m2;
				} else {
					ss.moveProcess(p2id, m2);
					if (decision == Decision.Stop) {
						ss.moveProcess(p1id, m1);
						return;
					}
				}
			}

			ss.moveProcess(p1id, m1);
		}
	}

	public void setProcesses(IntArrayList processes) {
		this.processes = new IntArrayList(processes);
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
