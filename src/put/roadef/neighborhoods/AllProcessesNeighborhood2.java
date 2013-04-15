package put.roadef.neighborhoods;

import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.MyArrayUtils;
import put.roadef.Problem;
import put.roadef.RuntimeStats;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

/**
 * Neighbor = a feasible solution made by moving one process to an optimal
 * machine. Neighborhood.size < NumProcesses.
 */
public class AllProcessesNeighborhood2 implements Neighborhood<Solution>, Setup {
	public enum Order {
		DefaultOrder, LoadCostOrder
	};

	private Order order;
	private boolean considerOnlyProcessesWithPositiveLoadCost;
	private static Logger logger = Logger.getLogger(AllProcessesNeighborhood2.class);

	private Int2IntLinkedOpenHashMap machinesWorthTrying;
	private Problem problem;
	private IntArrayList processesWorthTrying;
	private boolean firstVisit;
	private int[] machinesToConsider;
	private boolean setMachinesToConsiderManually;
	private boolean shuffle;

	@Override
	public void init(Problem problem) {
		if (!setMachinesToConsiderManually) {
			machinesToConsider = new int[problem.getNumMachines()];
			for (int i = 0; i < problem.getNumMachines(); ++i)
				machinesToConsider[i] = i;
		}

		// All are worth trying, initially
		machinesWorthTrying = new Int2IntLinkedOpenHashMap(problem.getNumMachines());
		for (int m : machinesToConsider)
			machinesWorthTrying.add(m, -1); // -1 means that it was added by me (not any process)

		this.problem = problem;
		processesWorthTrying = new IntArrayList(problem.getNumProcesses());
		for (int i = 0; i < problem.getNumProcesses(); ++i)
			processesWorthTrying.add(i);

		firstVisit = true;
	}

	public AllProcessesNeighborhood2() {

	}

	@Override
	public void visit(Solution solution, Deadline deadline, NeighborProcessor processor) {
		SmartSolution ss = (SmartSolution) solution;

		//logger.info("Visiting the neighborhood...");

		long[] processPotencial = computeProcessPotential(ss);
		if (firstVisit) {
			if (shuffle)
				MyArrayUtils.shuffle(processesWorthTrying, problem.getRandom());
			sortProcessesUsing(processPotencial);
		}
		boolean[] isProcessWorthTrying = computeIsProcessWorthTrying(processPotencial);
		
		double lastImpr = solution.getImprovement();

		//logger.info("Num machines worth trying = " + machinesWorthTrying.size());
		int numAcceptedProcessesToMove = 0;
		for (int p1 = 0; p1 < processesWorthTrying.size(); ++p1) {
			int p = processesWorthTrying.get(p1);

			if (!isProcessWorthTrying[p])
				continue;			
			
			// Remove machines not worth trying
			IntArrayList machinesToRemoveFromList = new IntArrayList();
			for (Int2IntMap.Entry e : machinesWorthTrying.int2IntEntrySet()) {
				if (e.getIntValue() == p)
					machinesToRemoveFromList.push(e.getIntKey());
			}
			for (int m : machinesToRemoveFromList)
				machinesWorthTrying.remove(m);

			// Find the best machine for process. Neighborhood *excludes* the current machine (oldMachine)
			int bestMachine = -1;
			long bestCost = -1;

			int sourceMachine = ss.getMachine(p);
			int[] machinesArr = machinesWorthTrying.keySet().toIntArray();
			if (shuffle)
				MyArrayUtils.shuffle(machinesArr, problem.getRandom());
			for (int m : machinesArr) {
				if (m == sourceMachine) // Do no try to move the process to the source machine
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
				solution.moveProcess(p, sourceMachine);
			else {
				solution.moveProcess(p, bestMachine);

				// If we did not change anything, then this is not a proper neighbor, so we will not provide to the algorithm (decision) 
				Decision decision = processor.processNeighbor(solution);
				if (decision == Decision.Accept) {
					numAcceptedProcessesToMove += 1;
					machinesWorthTrying.put(sourceMachine, p);
					machinesWorthTrying.put(bestMachine, p);
					
					
					double impr = solution.getImprovement();
					double imprDiff = impr - lastImpr;
					if (imprDiff >= 1.0) {
						RuntimeStats.add(solution, 0, "HillClimberVisit");
						lastImpr = impr;
					}
				}

				// If not accepted then move process to the source machine
				if (decision != Decision.Accept) // Reject or Stop. (On Stop we don't want to accept (possibly worst solution)
					solution.moveProcess(p, sourceMachine);
				if (decision == Decision.Stop)
					break;
			}

			// I do not want to check for the deadline too often 
			if (deadline.hasExpired()) {
				break;
			}
			
		}
		if (firstVisit) {
			IntArrayList machinesToRemoveFromList = new IntArrayList();
			for (Int2IntMap.Entry e : machinesWorthTrying.int2IntEntrySet()) {
				int m = e.getIntKey();
				if (e.getIntValue() == -1) {
					machinesToRemoveFromList.push(m);
				}
			}
			for (int m : machinesToRemoveFromList)
				machinesWorthTrying.remove(m);
		}
		firstVisit = false;
		//logger.info("numAcceptedProcessesToMove = " + numAcceptedProcessesToMove);
	}

	private long[] computeProcessPotential(SmartSolution ss) {
		final long[] processPotencial = new long[problem.getNumProcesses()];
		// We compute it only for processes in processesWorthTrying
		for (int p : processesWorthTrying)
			processPotencial[p] = ss.getLoadCost(p) + ss.getMoveCost(p);// + ss.getBalanceCost(p); I does not add anything (except additional time)		
		return processPotencial;
	}

	private void sortProcessesUsing(long[] processPotencial) {
		if (order == Order.LoadCostOrder) {
			// Just sorting according to processPotencial
			int[] tmp = processesWorthTrying.toIntArray();
			MyArrayUtils.sort(tmp, processPotencial);
			ArrayUtils.reverse(tmp);
			for (int i = 0; i < tmp.length; ++i)
				processesWorthTrying.set(i, tmp[i]);
		}
	}

	boolean[] computeIsProcessWorthTrying(long[] processPotencial) {
		int numWorthTrying = 0;
		boolean[] processIsWorthTrying = new boolean[problem.getNumProcesses()]; // To moglbym przyciac do liczby procesow (i indeksowac wtedy indeksem procesu a nie jego numerem), ale to nie ma to sensu, bo processPotencial i tak jest duzy

		if (considerOnlyProcessesWithPositiveLoadCost) {
			for (int i = 0; i < processesWorthTrying.size(); ++i)
				processIsWorthTrying[i] = false;

			for (int p : processesWorthTrying)
				if (processPotencial[p] > 0) {
					processIsWorthTrying[p] = true;
					numWorthTrying += 1;
				}
		} else {
			for (int i = 0; i < processesWorthTrying.size(); ++i)
				processIsWorthTrying[i] = true;
		}
		//logger.info("Num processes worth trying: " + numWorthTrying + " out of " + processesWorthTrying.size() + " considered");
		return processIsWorthTrying;
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
		String confOrder = configuration.getString(base + ".order", Order.DefaultOrder.toString());
		if (confOrder != Order.DefaultOrder.toString())
			order = Order.valueOf(confOrder);

		considerOnlyProcessesWithPositiveLoadCost = configuration.getBoolean(base + ".positive_load_cost_processes_only", false);
		shuffle = configuration.getBoolean(base + ".shuffle", false);
	}

	public AllProcessesNeighborhood2(Order order, boolean considerOnlyProcessesWithPositiveLoadCost,
			boolean setMachinesToConsiderManually) {
		this.order = order;
		this.considerOnlyProcessesWithPositiveLoadCost = considerOnlyProcessesWithPositiveLoadCost;
		this.setMachinesToConsiderManually = setMachinesToConsiderManually;
	}

	public void setMachines(int[] machinesToOnlyConsider) {
		this.machinesToConsider = machinesToOnlyConsider.clone();
	}
}

//Filterning machines
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
