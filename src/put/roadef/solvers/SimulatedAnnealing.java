package put.roadef.solvers;

import java.util.Random;

import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;
import put.roadef.neighborhoods.Neighborhood;
import put.roadef.neighborhoods.Neighborhood.Decision;
import put.roadef.neighborhoods.Neighborhood.NeighborProcessor;
import put.roadef.tweaks.RandomizedTweakOperator;

public class SimulatedAnnealing extends Solver implements RandomizedTweakOperator, Setup {
	private boolean greedy;
	private Neighborhood<Solution> neighborhood;
	private int numNeighborsToComputeInitialTime;
	private double initialProbabilityToAcceptWorse;
	private boolean stopOnBetterSolutionFound;
	private int timeLimitMilliseconds;

	public SimulatedAnnealing() {
	}

	public SimulatedAnnealing(Neighborhood<Solution> neighborhood, boolean greedy,
			int numNeighborsToComputeInitialTime, int initialProbabilityToAcceptWorse,
			int timeLimitMilliseconds) {
		this.neighborhood = neighborhood;
		this.greedy = greedy;
		this.numNeighborsToComputeInitialTime = numNeighborsToComputeInitialTime;
		this.initialProbabilityToAcceptWorse = initialProbabilityToAcceptWorse;
		this.timeLimitMilliseconds = timeLimitMilliseconds;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		greedy = configuration.getBoolean(base + ".greedy", true);
		neighborhood = (Neighborhood<Solution>) configuration.getInstanceAndSetup(base
				+ ".neighborhood");
		numNeighborsToComputeInitialTime = configuration.getInt(base
				+ ".num_neighbors_to_compute_initial_time");
		initialProbabilityToAcceptWorse = configuration.getDouble(base
				+ ".initial_probability_to_accept_worse");
		stopOnBetterSolutionFound = configuration.getBoolean(base
				+ ".stop_on_better_solution_found", true);
		returnBestSolution = configuration.getBoolean(base + ".return_best_solution", true);
		timeLimitMilliseconds = configuration.getInt(base + ".time_limit_milliseconds");
	}

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		Solution s = initialSolution;
		if (!(initialSolution instanceof SmartSolution))
			s = new SmartSolution(s);
		return tweak(s, deadline);
	}

	private ImmutableSolution bestSolution;
	private Solution currentSolution;
	private long currentSolutionCost;
	private long time;
	private long maxCostInNeighborhood;
	private int neighborCounter;
	private boolean returnBestSolution;
	private ImmutableSolution currentSolutionLight;

	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		return tweak(solution, deadline, solution.getProblem().getRandom());
	}
	
	@Override
	public Solution tweak(Solution solution, final Deadline deadline, Random random) {
		Deadline tweakDeadline = (timeLimitMilliseconds <= 0 ? deadline : Deadline.min(
				new Deadline(timeLimitMilliseconds), deadline));

		return greedy ? tweakGreedy(solution, tweakDeadline, random) : tweakSteepest(solution,
				tweakDeadline, random);
	}

	private Solution tweakSteepest(Solution solution, final Deadline deadline, Random random) {
		return null;
		//TODO
	}

	public Solution tweakGreedy(Solution solution, final Deadline deadline, final Random random) {
		time = deadline.getTimeToExpireMilliSeconds();
		if (time <= 0)
			return solution;

		currentSolution = solution;
		currentSolutionLight = solution;
		currentSolutionCost = solution.getCost();

		bestSolution = currentSolution.lightClone();

		//Set timeMultiplier so that all moves from the neighborhood are acceptable under timeMultiplier * t;
		maxCostInNeighborhood = 0;
		neighborCounter = 0;
		neighborhood.visit(currentSolution, deadline, new NeighborProcessor() {
			@Override
			public Decision processNeighbor(ImmutableSolution neighbor) {
				if (maxCostInNeighborhood < neighbor.getCost())
					maxCostInNeighborhood = neighbor.getCost();

				// If 0 we check the whole neighborhood
				if (numNeighborsToComputeInitialTime > 0) {
					neighborCounter += 1;
					if (neighborCounter >= numNeighborsToComputeInitialTime)
						return Decision.Stop;
				}
				return Decision.Reject;
			}
		});
		if (maxCostInNeighborhood < currentSolutionCost)
			maxCostInNeighborhood = (long) (currentSolutionCost * 1.1 + 1);

		final double timeMultiplier = (currentSolutionCost - maxCostInNeighborhood)
				/ (Math.log(initialProbabilityToAcceptWorse) * time);

		while (!deadline.hasExpired()) {
			if (!(currentSolutionLight instanceof SmartSolution))
				currentSolution = new SmartSolution(currentSolutionLight);
			else
				currentSolution = (SmartSolution) currentSolutionLight;
			neighborhood.visit(currentSolution, deadline, new NeighborProcessor() {
				@Override
				public Decision processNeighbor(ImmutableSolution neighbor) {
					boolean accept = false;
					if (neighbor.getCost() < currentSolutionCost)
						accept = true;
					else {
						time = deadline.getTimeToExpireMilliSeconds();
						if (time > 0) {
							long diff = currentSolutionCost - neighbor.getCost();
							if (random.nextDouble() < Math.exp(diff / (time * timeMultiplier)))
								accept = true;
						}
					}
					if (accept == true) {
						currentSolutionCost = neighbor.getCost();
						currentSolutionLight = neighbor;

						if (currentSolutionCost < bestSolution.getCost()) {
							bestSolution = currentSolution.lightClone();
							if (stopOnBetterSolutionFound)
								return Decision.Stop;
						}
					}
					if (deadline.hasExpired())
						return Decision.Stop;
					// In greedy version we accept to work on the new solution if it has been accepted in the SA scheme										
					return accept ? Decision.Accept : Decision.Reject;
				}
			});
			// solution is always the best solution found so far
		}
		// This matters just if no better solution found or deadline.hasExpired
		if (returnBestSolution)
			return new SmartSolution(bestSolution);
		else
			return new SmartSolution(currentSolutionLight);
	}

	@Override
	public boolean isDeterministic() {
		return false;
	}

	@Override
	public boolean isGreedy() {
		return greedy;
	}
}
