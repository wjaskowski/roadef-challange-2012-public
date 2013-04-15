package put.roadef;

import java.io.File;

import org.apache.log4j.Logger;

public class Safety {
	public static final long PERIOD_MILIS = 15 * 1000;

	private static long bestCost = Long.MAX_VALUE;
	private static ImmutableSolution bestSolution;
	private static long next = Long.MIN_VALUE; // save immediately

	private static Logger logger = Logger.getLogger(Safety.class);

	/**
	 * Should be called if anything other than Main.main method is run (e.g.
	 * SolverRunner, unit tests, etc.)
	 * 
	 * @param outputSolutionFilename
	 *            file to save solution
	 */
	public static void init(String outputSolutionFilename) {
		bestCost = Long.MAX_VALUE;
		bestSolution = null;
		Main.outputSolutionFilename = outputSolutionFilename;
	}

	/**
	 * Saves solution not more often than PERIOD_MILIS. It ensures that the
	 * solution is better than the last saved but not check its feasibility.
	 * 
	 * @param solution
	 *            Have to be feasible.
	 */
	public static void saveSolution(ImmutableSolution solution) {
		if (Main.outputSolutionFilename == null)
			return;
		long time = System.currentTimeMillis();
		if (time >= next) {
			if (solution.getCost() < bestCost) {
				if (!solution.isFeasible())
				{
					logger.error("Solution is not feasible! I am not saving!");
					return;
				}

				bestSolution = solution.lightClone();
				bestCost = bestSolution.getCost();

				logger.info(String.format("Currently best solution saved (cost = %d, imp = %.3f)", solution.getCost(),
						solution.getImprovement()));
				SolutionIO.writeSolutionToFile(bestSolution, new File(Main.outputSolutionFilename));
			}

			next = time + PERIOD_MILIS;
		}
	}

	/**
	 * Returns last saved solution.
	 * 
	 * @return
	 */
	public static ImmutableSolution getLastSolution() {
		return bestSolution;
	}
}
