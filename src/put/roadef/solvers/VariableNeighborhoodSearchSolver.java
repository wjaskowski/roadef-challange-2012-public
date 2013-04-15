package put.roadef.solvers;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.RandomSolutionGenerator;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.TrivialSolutionGenerator;
import put.roadef.TweakOperator;
import put.roadef.conf.RoadefConfiguration;

public class VariableNeighborhoodSearchSolver extends Solver {

	private TweakOperator perturb;
	private TweakOperator optimize;

	private int neighborhoodVariability;
	private RandomSolutionGenerator randomGenerator;

	public VariableNeighborhoodSearchSolver() {
		this.randomGenerator = new TrivialSolutionGenerator();
	}

	public VariableNeighborhoodSearchSolver(TweakOperator perturbOperator,
			TweakOperator optimizeOperator) {
		this.perturb = perturbOperator;
		this.optimize = optimizeOperator;
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		perturb = (TweakOperator) configuration.getInstanceAndSetup(base + ".perturb");
		optimize = (TweakOperator) configuration.getInstanceAndSetup(base + ".optimize");
		neighborhoodVariability = configuration.getInt(base + ".variability");
	}

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		Solution bestSolution = randomGenerator.getRandomSolution(problem);
		bestSolution = optimize.tweak(bestSolution, deadline);
		long bestFitness = bestSolution.getCost();

		while (!deadline.hasExpired()) {
			boolean foundBetter = false;
			Solution candidateSolution = bestSolution.clone();

			for (int neighborhoodSize = 1; neighborhoodSize < neighborhoodVariability; neighborhoodSize++) {
				if (perturb.isDeterministic()) {
					candidateSolution = perturb.tweak(candidateSolution, deadline);
				} else {
					candidateSolution = bestSolution.clone();
					for (int step = 0; step < neighborhoodSize; step++) {
						candidateSolution = perturb.tweak(candidateSolution, deadline);
					}
				}

				Solution locallyOptimalSolution = optimize.tweak(candidateSolution, deadline);
				long newFitness = locallyOptimalSolution.getCost();
//				System.out.println("Locally optimal solution cost = " + newFitness);
				if (newFitness < bestFitness) {
					bestSolution = locallyOptimalSolution;
					bestFitness = newFitness;
					foundBetter = true;
					break;
				}
			}

			if (!foundBetter) {
				break;
			}
		}

		return bestSolution;
	}
}
