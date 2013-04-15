package put.roadef.bnb;

import put.roadef.Problem.Machine;
import put.roadef.Problem.Process;

public interface Assignable {
	/**
	 * Updates all the constraints after assigning a process to the given
	 * machine
	 */
	void addAssignment(Process process, Machine machine);

	/**
	 * Updates all the constraints after removing a process from the given
	 * machine
	 */
	void removeAssignment(Process process, Machine machine);

}
