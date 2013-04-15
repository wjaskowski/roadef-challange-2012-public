package put.roadef.neighborhoods;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.MyArrayUtils;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

/**
 * Neighbor = a feasible solution made by moving one process to an optimal
 * machine. Neighborhood.size < NumProcesses.
 */
public class AllProcessesNeighborhood implements Neighborhood<Solution>, Setup {
	private enum Order {
		DefaultOrder, RandomOrder, LoadCostOrder
	};

	private Order order;
	private boolean considerOnlyProcessesWithPositiveLoadCost;
	private static Logger logger = Logger.getLogger(AllProcessesNeighborhood.class);	

	public AllProcessesNeighborhood() {
	}

	public AllProcessesNeighborhood(boolean randomOrder) {
		if (randomOrder)
			this.order = Order.RandomOrder;
	}

	@Override
	public void visit(Solution solution, Deadline deadline, NeighborProcessor processor) {
		Problem problem = solution.getProblem();
		SmartSolution ss = (SmartSolution) solution;

		int processes[] = new int[problem.getNumProcesses()];
		for (int i = 0; i < problem.getNumProcesses(); ++i)
			processes[i] = i;

		int machines[] = new int[problem.getNumMachines()];
		for (int i = 0; i < problem.getNumMachines(); ++i)
			machines[i] = i;

		final long[] processPotencial = new long[problem.getNumProcesses()];
		if (order == Order.RandomOrder) {
			MyArrayUtils.shuffle(processes, problem.getRandom());
		} else if (order == Order.LoadCostOrder) {
			// Sorting
			//TODO: 
			// 2) zobacz ile kosztuje obliczanie tego (aktualizacja w SmartSolution / kolejka priorytetowa)
			// 3) oprocz tego co mozna zarobic uwzglednij rowniez co *minimalnie* mozna stracic
			for (int p = 0; p < problem.getNumProcesses(); ++p) {
				processPotencial[p] = ss.getLoadCost(p) + ss.getMoveCost(p);// + ss.getBalanceCost(p); I does not add anything (except additional time)
			}

			MyArrayUtils.sort(processes, processPotencial);
			ArrayUtils.reverse(processes);

			// Filtering processes
			if (considerOnlyProcessesWithPositiveLoadCost) {
				int zero = -1;
				for (int i = 0; i < processes.length; ++i)
					// Nie uwzglêdniamy balanceCost, wiêc to jest heurystyk¹ (przez to nieca³e s¹siedztwo jest przeszukane, poniewa¿ mo¿e siê zda¿yæ, ¿e processPotencial=0 
					// tymczasem wcale nie jest 0 
					if (processPotencial[processes[i]] <= 0) {
						zero = i;
						break;
					}
				if (zero != -1)
					processes = ArrayUtils.subarray(processes, 0, zero);
			}						
		}

		// Filterning machines
		// With this we could improve time of b3 and b5 by 50% (but only with transient risky code)
		//		int numMachinesFull = 0;
		//		for (int m1 = 0; m1 < machines.length; ++m1) {
		//			int m = machines[m1];
		//			for (int r = 0; r < problem.getNumResources(); ++r) {
		//				long possibleLoad = ss.getResourceUsage(m, r) + problem.getResource(r).minProcessRequirement;
		//				long possibleTransientLoad = ss.getTransientUsage(m, r) + problem.getResource(r).minProcessRequirement; // This is risky (since moving to the original machine should be always possible)
		//				if  (possibleLoad > problem.getMachine(m).capacities[r] || possibleTransientLoad > problem.getMachine(m).capacities[r])
		//				{
		//					numMachinesFull += 1;
		//					break;
		//				}
		//			}
		//		}
		//		logger.trace("NumMachines = " + problem.getNumMachines() + "; NumMachiniesFull = " + numMachinesFull);

		logger.info("Visiting the neighborhood...");
		for (int p1 = 0; p1 < processes.length; ++p1) {
			int p = processes[p1];

			int sourceMachine = ss.getMachine(p);

			// Neighborhood EXCLUDES the current machine (oldMachine)
			int bestMachine = -1;
			long bestCost = -1;

			for (int m1 = 0; m1 < problem.getNumMachines(); ++m1) {
				int m = machines[m1];
				if (m == sourceMachine) // Do no try to move the process to the source (original) machine
					continue;

				if (!ss.couldBeFeasibleQuick(p, m))
					continue;

				ss.moveProcess(p, m);
				if (!ss.isFeasible())
					continue;

				long cost = solution.getCost();
				if (bestCost == -1 || bestCost > cost) {
					bestCost = cost;
					bestMachine = m;
				}
			}

			if (bestCost == -1) // This may happen if the neighborhood has no feasible solutions
				bestMachine = sourceMachine;

			solution.moveProcess(p, bestMachine);

			// If we did not change anything, then this is not a proper neighbor, so we will not provide to the algorithm (decision) 
			if (bestMachine != sourceMachine) {
				Decision decision = processor.processNeighbor(solution);
				// If not accepted then move process to the source machine
				if (decision != Decision.Accept) // Reject or Stop. (On Stop we don't want to accept (possibly worst solution)
					solution.moveProcess(p, sourceMachine);
				if (decision == Decision.Stop)
					return;
			}

			// I do not want to check for the deadline to often 
			if (deadline.hasExpired()) {
				return;
			}
		}
	}

	@Override
	public boolean runsOnTheSpot() {
		return true;
	}

	@Override
	public boolean isDeterministic() {
		return order != Order.RandomOrder;
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

	@Override
	public void init(Problem problem) {
		// TODO Auto-generated method stub
		
	}
}