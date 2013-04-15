package put.roadef.selectors;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import put.roadef.AssignmentDetails;
import put.roadef.Problem;

public class LocationProcessSelector implements ProcessSelector {

	@Override
	public int[] selectProcessesToMove(Problem problem, int[] currentAssignment,
			AssignmentDetails serviceDetails) {
		int[] processes = new int[problem.getNumProcesses()];
		for (int p = 0; p < processes.length; p++) {
			processes[p] = p;
		}
		return processes;
	}

	@Override
	public int[] selectProcessesToMoveWith(Problem problem, int process, int[] currentAssignment,
			AssignmentDetails assignmentDetails) {
		int machine = currentAssignment[process];
		int location = problem.getMachine(machine).location;
		int[] machines = problem.getMachinesInLocation(location);
		
		IntSet candidates = new IntOpenHashSet();
		for (int m : machines) {
			candidates.addAll(assignmentDetails.getProcessesInMachine(m));
		}
		return candidates.toIntArray();
	}

}
