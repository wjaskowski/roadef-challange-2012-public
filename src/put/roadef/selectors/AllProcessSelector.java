package put.roadef.selectors;

import put.roadef.AssignmentDetails;
import put.roadef.Problem;

public class AllProcessSelector implements ProcessSelector {

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
			AssignmentDetails serviceDetails) {
		int[] processes = new int[problem.getNumProcesses() - 1];
		for (int p = 0; p < process; p++) {
			processes[p] = p;
		}
		for (int p = process + 1; p <= processes.length; p++) {
			processes[p - 1] = p;
		}
		return processes;
	}

}
