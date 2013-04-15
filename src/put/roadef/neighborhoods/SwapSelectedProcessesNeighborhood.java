package put.roadef.neighborhoods;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;
import put.roadef.selectors.SwapProcessSelector;

public class SwapSelectedProcessesNeighborhood implements Neighborhood<Solution>, Setup {

	private SwapProcessSelector processSelector;
	private int numIterationsInVisit ;

	@Override
	public void init(Problem problem) {
		processSelector.reset();
	}

	@Override
	public void visit(Solution solution, Deadline deadline, NeighborProcessor processor) {
		SmartSolution ss = (SmartSolution) solution;
		int iteration = 0;
		
		while (!deadline.hasExpired() && (iteration++ < numIterationsInVisit)) {
			List<IntList> processes = processSelector.getProcessesToSwap(solution);

			if (processes.isEmpty())
				break;
			
			for (int p1 : processes.get(0)) {
				int m1 = ss.getMachine(p1);
				
				for (int p2 : processes.get(1)) {
					if (p2 == p1)
						continue;
						
					int m2 = ss.getMachine(p2);

					ss.moveProcess(p1, m2);
					ss.moveProcess(p2, m1);
					
					if (!ss.isFeasible()) {
						ss.moveProcess(p2, m2);
						continue;
					}

					Decision decision = processor.processNeighbor(ss);
					if (decision == Decision.Accept) {
						m1 = m2;
					} else {
						ss.moveProcess(p2, m2);
						if (decision == Decision.Stop) {
							ss.moveProcess(p1, m1);
							return;
						}
					}
				}
				
				ss.moveProcess(p1, m1);
			}
		}
	}

	@Override
	public boolean runsOnTheSpot() {
		return false;
	}

	@Override
	public boolean isDeterministic() {
		return true;
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		processSelector = (SwapProcessSelector) (configuration.getInstanceAndSetup(base + ".process_selector"));
		numIterationsInVisit = configuration.getInt(base + ".num_iterations_per_visit", 10);
	}
}
