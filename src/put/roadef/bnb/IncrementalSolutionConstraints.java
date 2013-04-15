package put.roadef.bnb;

import put.roadef.Problem.Machine;
import put.roadef.Problem.Process;

/**
 * Defines interface for a set of constraints which can be checked and updated
 * during incremental construction of Solution.
 * 
 * @author marcin
 * 
 */
public interface IncrementalSolutionConstraints extends Assignable {

	/**
	 * Checks if any constraint will be violated after assigning given process
	 * to the machine
	 */
	boolean checkFutureConstraints(Process process, Machine machine);
}
