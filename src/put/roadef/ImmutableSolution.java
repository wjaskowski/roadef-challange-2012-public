package put.roadef;

public interface ImmutableSolution {
	Problem getProblem();

	/**
	 * @return the copy of machine-process assignment table
	 */
	int[] getAssignment();

	/**
	 * @return the machine to which process is assigned
	 */
	int getMachine(int processId);

	/**
	 * Clones the solution (a deep copy except of problem field)
	 */
	Solution clone();
	
	/**
	 * Quickly creates a simple copy of a solution
	 */
	ImmutableSolution lightClone();

	/**
	 * Computes whether the solution is feasible. May be cached. May take a long
	 * time.
	 */
	boolean isFeasible();

	/**
	 * Computes the cost of the solution. May be cached. May take a long time.
	 */
	long getCost();
	
	/**
	 * Computes the improvement over the initial solution. May be cached. May take a long time.
	 */
	double getImprovement();

	//TODO: Te dwa powinny predzej czy pozniej wyleciec, bo one nie sa zbyt generyczne
	long getResourceUsage(int machineId, int resourceId);
	long getTransientUsage(int machineId, int resourceId);
}
