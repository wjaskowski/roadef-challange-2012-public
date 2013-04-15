package put.roadef.neighborhoods;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Problem.Service;
import put.roadef.SmartSolution;
import put.roadef.Solution;

/**
 * Neighbor = a feasible solution made by swapping two processes from the same
 * service.
 */
public class SwapProcessesInServicesNeighborhood implements Neighborhood<Solution> {

	private Logger logger = Logger.getLogger(SwapProcessesInServicesNeighborhood.class);

	private int currentService = 0;

	@Override
	public void init(Problem problem) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(Solution solution, Deadline deadline, put.roadef.neighborhoods.Neighborhood.NeighborProcessor processor) {
		Problem problem = solution.getProblem();
		logger.info("Visiting... starting from service " + currentService);

		SmartSolution ss = SmartSolution.promoteToSmartSolution(solution);
		
		for (int s = 0; s < problem.getNumServices(); ++s, ++currentService) {
			if (currentService >= problem.getNumServices()) {
				currentService = 0;
			}
			
			Service service = problem.getService(currentService);
			if (service.processes.length < 2)
				continue;
			for (int p1 = 0; p1 < service.processes.length; ++p1) {
				int p1id = service.processes[p1];
				int m1 = ss.getMachine(p1id);

				for (int p2 = p1 + 1; p2 < service.processes.length; ++p2) {
					int p2id = service.processes[p2];
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
