package put.roadef;

/**
 * A read only solution.
 * 
 * @author Wojtek
 * 
 */
public final class ReadOnlySolution implements ImmutableSolution {
	private Solution solution;

	public ReadOnlySolution(Problem problem, int assignment[]) {
		this.solution = new SimpleSolution(problem, assignment);
	}

	@Override
	public Problem getProblem() {
		return solution.getProblem();
	}

	@Override
	public int[] getAssignment() {
		return solution.getAssignment();
	}

	@Override
	public int getMachine(int processId) {
		return solution.getMachine(processId);
	}

	@Override
	public boolean isFeasible() {
		return solution.isFeasible();
	}

	@Override
	public long getCost() {
		return solution.getCost();
	}
	
	@Override
	public double getImprovement() {
		return solution.getImprovement();
	}

	@Override
	public Solution clone() {
		return solution.clone();
	}

	@Override
	public long getResourceUsage(int machineId, int resourceId) {
		return solution.getResourceUsage(machineId, resourceId);
	}

	@Override
	public long getTransientUsage(int machineId, int resourceId) {
		return solution.getTransientUsage(machineId, resourceId);
	}

	@Override
	public ImmutableSolution lightClone() {
		return new LightSolution(this);
	}

}
