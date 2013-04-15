package put.roadef.selectors;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import put.roadef.AssignmentDetails;
import put.roadef.Problem;

public class ServiceProcessSelector implements ProcessSelector {

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
		int service = problem.getProcess(process).service;
		IntArrayList candidates = new IntArrayList();
		for (int p : problem.getProcessesOfService(service)) {
			if (p > process)
				candidates.add(p);
		}
		return candidates.toIntArray();
	}
}
