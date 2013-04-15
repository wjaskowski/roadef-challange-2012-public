package put.roadef.neighborhoods;

import org.apache.commons.lang.ArrayUtils;

import put.roadef.Deadline;
import put.roadef.MyArrayUtils;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

public class AllProcessesMachinePathNeighborhood implements Neighborhood<Solution>, Setup {
	private enum Order {
		DefaultOrder, RandomOrder, LoadCostOrder
	};
	
	private Order order;
	private boolean considerOnlyProcessesWithPositiveLoadCost;
	
	@Override
	public void init(Problem problem) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(Solution solution, Deadline deadline, NeighborProcessor processor) {
		Problem problem = solution.getProblem();
		SmartSolution ss = (SmartSolution) solution;

		int processes[] = new int[problem.getNumProcesses()];
		for (int i = 0; i < problem.getNumProcesses(); ++i)
			processes[i] = i;

		final long[] processPotencial = new long[problem.getNumProcesses()];
		if (order == Order.RandomOrder) {
			MyArrayUtils.shuffle(processes, problem.getRandom());
		} else if (order == Order.LoadCostOrder) {
			for (int p = 0; p < problem.getNumProcesses(); ++p) {
				processPotencial[p] = ss.getLoadCost(p) + ss.getMoveCost(p);
			}

			MyArrayUtils.sort(processes, processPotencial);
			ArrayUtils.reverse(processes);

			if (considerOnlyProcessesWithPositiveLoadCost) {
				int zero = -1;
				for (int i = 0; i < processes.length; ++i)
					if (processPotencial[processes[i]] <= 0) {
						zero = i;
						break;
					}
				if (zero != -1)
					processes = ArrayUtils.subarray(processes, 0, zero);
			}						
		}
		
		
		for (int p1 : processes) {
			int m1 = ss.getMachine(p1);
			int p1BestMachine = m1;
			for (int m2 = 0; m2 < problem.getNumMachines(); m2++) {
				if (m1 == m2)
					continue;

				ss.moveProcess(p1, m2);
	
				for (int p2 : ss.processesInMachine[m2]) {
					if (p2 == p1)
						continue;
					
					int p2BestMachine = m2;
					for (int m3 = 0; m3 < problem.getNumMachines(); m3++) {
						if (m3 == m2)
							continue;

						ss.moveProcess(p2, m3);
						if (!ss.isFeasible())
							continue;

						Decision decision = processor.processNeighbor(ss);
						if (decision == Decision.Accept) {
							p1BestMachine = m2;
							p2BestMachine = m3;
						} else if (decision == Decision.Stop) {
							ss.moveProcess(p1, p1BestMachine);
							ss.moveProcess(p2, p2BestMachine);
							return;
						}
					}
					
					ss.moveProcess(p2, p2BestMachine);
				}
				
				ss.moveProcess(p1, p1BestMachine);
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
	
	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		if (configuration.getBoolean(base + ".random_order", false))
			order = Order.RandomOrder;
		String confOrder = configuration.getString(base + ".order", Order.DefaultOrder.toString());
		if (confOrder != Order.DefaultOrder.toString())
			order = Order.valueOf(confOrder);

		considerOnlyProcessesWithPositiveLoadCost = configuration.getBoolean(base + ".positive_load_cost_processes_only", false);
	}
}
