package put.roadef.selectors;

import put.roadef.AssignmentDetails;
import put.roadef.Problem;

public interface ProcessSelector {

	/**
	 * Returns processes that are good candidates for moving.
	 * 
	 * @param currentAssignment
	 *            given as an array to allow easy access for both Solution and
	 *            PartialSolution
	 */
	int[] selectProcessesToMove(Problem problem, int[] currentAssignment,
			AssignmentDetails serviceDetails);

	/**
	 * Returns processes that should be moved along with a given one.
	 * 
	 * @param currentAssignment
	 *            given as an array to allow easy access for both Solution and
	 *            PartialSolution
	 */
	int[] selectProcessesToMoveWith(Problem problem, int process, int[] currentAssignment,
			AssignmentDetails serviceDetails);
}
