package put.roadef.neighborhoods;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Solution;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;
import put.roadef.ip.MipFastModel;
import put.roadef.selectors.GroupProcessSelector;

public class IPNeighborhood implements Neighborhood<Solution>, Setup {

	private MipFastModel model = new MipFastModel("CplexFastMipSolver");
	private GroupProcessSelector groupProcessSelector;
	@SuppressWarnings("unused")
	private Logger logger = Logger.getLogger(IPNeighborhood.class);
	
	private static final long CPLEX_OVERHEAD = 2000;

	@Override
	public boolean isDeterministic() {
		return groupProcessSelector.isDeterministic();
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		groupProcessSelector = (GroupProcessSelector) (configuration.getInstanceAndSetup(base + ".process_selector"));
	}

	@Override
	public void init(Problem problem) {
		groupProcessSelector.reset();
	}

	@Override
	public void visit(Solution solution, Deadline deadline, NeighborProcessor processor) {
		Problem problem = solution.getProblem();

		while (!deadline.hasExpired()) {
			List<IntList> groups = groupProcessSelector.getProcessesGroups(solution);

			if (groups.isEmpty())
				break;

			for (IntList processes : groups) {
				if (deadline.getTimeToExpireMilliSeconds() <= CPLEX_OVERHEAD)
					break;

				Solution candidateSolution = model.modifyAssignmentsForProcesses(problem, solution, processes.toIntArray(),
						deadline.getShortenedBy(CPLEX_OVERHEAD), true, true);

				if (candidateSolution.getCost() < solution.getCost()) {
					solution = candidateSolution;
				}
			}
		}
	}

	@Override
	public boolean runsOnTheSpot() {
		return false;
	}
}
