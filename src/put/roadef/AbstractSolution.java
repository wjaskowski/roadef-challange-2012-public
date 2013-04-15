package put.roadef;

import java.util.Arrays;

public abstract class AbstractSolution implements Solution {
	protected int assignment[];
	protected Problem problem;

	public AbstractSolution(Problem problem, int assignment[]) {
		this.problem = problem;
		this.assignment = assignment.clone();
	}

	protected AbstractSolution(ImmutableSolution solution) {
		this.problem = solution.getProblem();
		this.assignment = solution.getAssignment();
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
	public boolean equals(Object o) {
		if (o instanceof Solution) {
			return Arrays.equals(assignment, ((AbstractSolution) o).assignment);
		}
		return false;
	}
	
	@Override
	public abstract Solution clone();

	@Override
	public abstract boolean isFeasible();

	@Override
	public abstract long getCost();

	@Override
	public double getImprovement() {
		return Common.computeImprovement(getCost(), getProblem().getOriginalFitness());		
	}

	@Override
	public abstract void moveProcess(int processId, int destinationMachine);

	//TODO: Te dwa powinny predzej czy pozniej wyleciec, bo one nie sa zbyt generyczne
	@Override
	public abstract long getResourceUsage(int machineId, int resourceId);

	@Override
	public abstract long getTransientUsage(int machineId, int resourceId);
}
