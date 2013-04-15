package put.roadef;

public class SimpleSolution extends AbstractSolution {
	private long[][] resourceUsage; //[numMachines][numResources], unit32.	
	private long[][] transientUsage;

	private boolean isEvaluated;
	private long cost;
	
	public SimpleSolution(Problem problem, int assignment[]) {
		super(problem, assignment);

		resourceUsage = problem.computeResourceUsage(this);
		transientUsage = problem.computeTransientResourceUsage(this);

		this.isEvaluated = false;
	}

	public SimpleSolution(ImmutableSolution solution) {
		this(solution.getProblem(), solution.getAssignment());
	}

	public SimpleSolution(SimpleSolution solution) {
		super(solution);
		this.isEvaluated = solution.isEvaluated;
		this.cost = solution.cost;

		this.resourceUsage = new long[solution.resourceUsage.length][];
		for (int i = 0; i < resourceUsage.length; ++i)
			this.resourceUsage[i] = solution.resourceUsage[i].clone();

		this.transientUsage = new long[solution.transientUsage.length][];
		for (int i = 0; i < transientUsage.length; ++i)
			this.transientUsage[i] = solution.transientUsage[i].clone();
	}

	@Override
	public Solution clone() {
		return new SimpleSolution(this);
	}

	@Override
	public long getResourceUsage(int m, int r) {
		return resourceUsage[m][r];
	}

	@Override
	public long getTransientUsage(int m, int r) {
		return transientUsage[m][r];
	}

	@Override
	public boolean isFeasible() {
		//TODO: We can check, whether we have to recompute this (anything has changed?)
		return problem.isSolutionFeasible(this);
	}

	// O(r)
	@Override
	public void moveProcess(int processId, int destinationMachine) {
		int sourceMachine = assignment[processId];
		if (sourceMachine == destinationMachine) {
			return;
		}
		assignment[processId] = destinationMachine;

		updateResourceUsageAfterMove(processId, sourceMachine, destinationMachine);

		isEvaluated = false;
	}

	//TODO: turn on caching after Smart/Partial Solution are consistently implemented
	@Override
	public long getCost() {
		return problem.evaluateSolution(this);

		/*if (isEvaluated) {
			return cost;
		} else {
			cost = problem.evaluateSolution(this);
			isEvaluated = true;
			return cost;
		}*/
	}

	private void updateResourceUsageAfterMove(int processId, int sourceMachine,
			int destinationMachine) {
		int originalMachine = problem.getOriginalSolution().getMachine(processId);
		for (int r = 0; r < problem.getNumResources(); r++) {
			long requirement = problem.getProcess(processId).requirements[r];
			if (problem.getResource(r).isTransient) {
				if (sourceMachine != originalMachine) {
					transientUsage[sourceMachine][r] -= requirement;
				}
				if (destinationMachine != originalMachine) {
					transientUsage[destinationMachine][r] += requirement;
				}
			}

			resourceUsage[sourceMachine][r] -= requirement;
			resourceUsage[destinationMachine][r] += requirement;
		}
	}
	
	@Override
	public ImmutableSolution lightClone() {
		return new LightSolution(this);
	}
}
