package put.roadef;

public interface Solution extends ImmutableSolution {	
	/**
	 * Moves a process to a destination machine
	 */
	void moveProcess(int processId, int destinationMachine);
}
