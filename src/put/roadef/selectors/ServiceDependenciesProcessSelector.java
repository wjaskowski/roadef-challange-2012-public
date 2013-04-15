package put.roadef.selectors;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import put.roadef.AssignmentDetails;
import put.roadef.Problem;

/**
 * Selects process that has some reverse dependencies (other services depends on
 * it) and it is the only one of its service in current neighborhood.
 * 
 * @author marcin
 * 
 */
public class ServiceDependenciesProcessSelector implements ProcessSelector {

	@Override
	public int[] selectProcessesToMove(Problem problem, int[] currentAssignment,
			AssignmentDetails serviceDetails) {
		IntList candidates = new IntArrayList();
		IntList services = problem.getServicesWithRevDependencies();
		for (int service : services) {
			Int2IntMap neighborhoods = serviceDetails.getNeighborhoodsForService(service);
			for (Entry e : neighborhoods.int2IntEntrySet()) {
				if (e.getIntValue() == 1) {
					int neighborhood = e.getIntKey();
					int[] processes = problem.getService(service).processes;
					for (int process : processes) {
						int machine = currentAssignment[process];
						if (problem.getMachine(machine).neighborhood == neighborhood) {
							candidates.add(process);
							break;
						}
					}
				}
			}
		}
		
		return candidates.toIntArray();
	}

	@Override
	public int[] selectProcessesToMoveWith(Problem problem, int process, int[] currentAssignment,
			AssignmentDetails serviceDetails) {
		int machine = currentAssignment[process];
		int neighborhood = problem.getMachine(machine).neighborhood;
		int service = problem.getProcess(process).service;
		int[] revDependencies = problem.getTransitiveRevDependencies(service);
		
		IntList processes = new IntArrayList();
		for (int dep : revDependencies) {
			int[] dependantProcesses = problem.getService(dep).processes;
			for (int p : dependantProcesses) {
				if (problem.getMachine(currentAssignment[p]).neighborhood == neighborhood) {
					processes.add(p);
				}
			}
		}
		
		return processes.toIntArray();
	}
}
