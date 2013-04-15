package put.roadef;

import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

public abstract class Solver implements Setup {
	//TODO: Byc moze przydalby sie jakis callback z informacja
	// o aktualnie najlepszym rozwiazaniu.

	/**
	 * Specifies how many milliseconds before the timout we should finish
	 * computation
	 */
	public static final int TIME_EPSILON_MILLIS = 20;

	/**
	 * @param timeoutSeconds
	 *            time limit in seconds
	 */
	public Solution solve(Problem problem, int timeoutSeconds) {
		return solve(problem, new Deadline(1000 * (long) timeoutSeconds - TIME_EPSILON_MILLIS));
	}

	/**
	 * @param Deadline
	 *            deadline time
	 */
	public Solution solve(Problem problem, Deadline deadline) {
		return solve(problem, problem.getOriginalSolution().clone(), deadline);
	}

	/**
	 * @param deadline
	 *            deadline time value in milliseconds from the epoch
	 * @param initialSolution
	 *            the initial solution that can be modified
	 */
	public abstract Solution solve(Problem problem, Solution initialSolution, Deadline deadline);
	
	
	@Override
	public void setup(RoadefConfiguration configuration, String base) {	}
}
