package put.roadef.selectors;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

public class OverloadingProcessSelector implements SwapProcessSelector, Setup {

	private boolean recalculate = true;
	private boolean firstVisit = true;
	
	private int currentMachineResourcePair = 0;

	List<MachineResourcePair> pairs = new ArrayList<MachineResourcePair>();
	private static Logger logger = Logger.getLogger(OverloadingProcessSelector.class);
	private IntList[] otherSortedProcesses;
	private long overloadsAlreadyComputed[][];
	private int num_overloaded_machines;
	
	@Override
	public List<IntList> getProcessesToSwap(Solution solution) {
		Problem problem = solution.getProblem();
		SmartSolution ss = SmartSolution.promoteToSmartSolution(solution);
		
		if (firstVisit) {
			logger.info("STARTED Initializing OverloadingProcessSelector neighborhood..");
			
			firstVisit = false;
			otherSortedProcesses = new IntList[problem.getNumResources()];
			overloadsAlreadyComputed = new long[problem.getNumResources()][problem.getNumMachines()];
			
			for (int r = 0; r < problem.getNumResources(); r++) {
				otherSortedProcesses[r] = new IntArrayList(problem.getNumProcesses());
				for (int p = 0; p < problem.getNumProcesses(); p++) {
					otherSortedProcesses[r].add(p);
				}
				Collections.sort(otherSortedProcesses[r], new ResourceUsageProcessComparator(problem, r));
			}
			
			logger.info("FINISHED Initializing OverloadingProcessSelector neighborhood..");
		}
		
		if (currentMachineResourcePair == pairs.size() || currentMachineResourcePair == num_overloaded_machines) {
			recalculate = true;
		}
		
		if (recalculate) {
			logger.info("RECALCULATING the most overloaded machines");
			
			pairs.clear();
			recalculate = false;
			currentMachineResourcePair = 0;

			for (int r = 0; r < problem.getNumResources(); r++) {
				long weight = problem.getResource(r).loadCostWeight;
				for (int m = 0; m < problem.getNumMachines(); m++) {
					long overload = weight * (ss.getResourceUsage(m, r) - problem.getMachine(m).safetyCapacities[r]);
					if (overloadsAlreadyComputed[r][m] == overload)
						continue;
					
					if (overload > 0) {
						pairs.add(new MachineResourcePair(m, r, overload));
					}
				}
			}

			logger.info("NUMBER of new overloaded machines = " + pairs.size());
			if (pairs.size() == 0) {
				return new ArrayList<IntList>();
			}
			
			Collections.sort(pairs);
		}

		MachineResourcePair pair = pairs.get(currentMachineResourcePair++);
		overloadsAlreadyComputed[pair.r][pair.m] = pair.overload;
		logger.info("Considering machine " + pair.m + " and resource " + pair.r + " of overload = " + pair.overload);
		
		List<IntList> result = new ArrayList<IntList>();
		IntList criticalProcesses = new IntArrayList();
		criticalProcesses.addAll(ss.processesInMachine[pair.m]);
		Comparator<Integer> cmp = new ResourceUsageProcessComparator(problem, pair.r);
		Collections.sort(criticalProcesses, Collections.reverseOrder(cmp));

		result.add(criticalProcesses.subList(0, criticalProcesses.size() / 2));
		result.add(otherSortedProcesses[pair.r].subList(0, problem.getNumProcesses() / 2));
		return result;
	}

	@Override
	public void reset() {
		
	}

	public static class MachineResourcePair implements Comparable<MachineResourcePair> {
		int m;
		int r;
		long overload;

		public MachineResourcePair(int m, int r, long overload) {
			this.m = m;
			this.r = r;
			this.overload = overload;
		}

		@Override
		public int compareTo(MachineResourcePair o) {
			if (this.overload < o.overload) {
				return 1;
			} else if (this.overload > o.overload) {
				return -1;
			} else {
				return 0;
			}
		}
	}

	public static class ResourceUsageProcessComparator implements Comparator<Integer> {

		private int comparedResource;
		private Problem problem;

		public ResourceUsageProcessComparator(Problem problem, int resource) {
			this.comparedResource = resource;
			this.problem = problem;
		}

		@Override
		public int compare(Integer o1, Integer o2) {
			if (problem.getProcess(o1).requirements[comparedResource] < problem.getProcess(o2).requirements[comparedResource]) {
				return -1;
			} else if (problem.getProcess(o1).requirements[comparedResource] > problem.getProcess(o2).requirements[comparedResource]) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		num_overloaded_machines = configuration.getInt(base + ".num_overloaded_machines", 100);
	}
}
