package put.roadef;

public class LightSolution implements ImmutableSolution {
	private Problem problem;
	private int assignment[];
	private boolean isFeasible;
	private double improvement;
	private long cost;
	
	public LightSolution(ImmutableSolution solution) {
		problem = solution.getProblem();
		assignment = solution.getAssignment();
		isFeasible = solution.isFeasible();
		cost = solution.getCost();
		improvement = solution.getImprovement();
	}

	public LightSolution(Problem problem, int[] assignment, long cost) {
		this.problem = problem;
		this.assignment = assignment;
		this.cost = cost;
		this.isFeasible = true;
	}
	
	@Override
	public Problem getProblem() {
		return problem;
	}

	@Override
	public int[] getAssignment() {
		return assignment.clone();
	}

	@Override
	public int getMachine(int processId) {
		return assignment[processId];
	}
	
	@Override
	public Solution clone() {
		throw new IllegalStateException();
	}

	@Override
	public ImmutableSolution lightClone() {
		return new LightSolution(this);
	}

	@Override
	public boolean isFeasible() {
		return isFeasible;
	}

	@Override
	public long getCost() {
		return cost;
	}
	
	@Override
	public double getImprovement() {
		return improvement;
	}

	@Override
	public long getResourceUsage(int machineId, int resourceId) {
		throw new IllegalStateException();
	}

	@Override
	public long getTransientUsage(int machineId, int resourceId) {
		throw new IllegalStateException();
	}
}
