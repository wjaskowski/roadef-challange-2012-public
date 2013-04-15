package put.roadef.neighborhoods;

import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.Problem;

public interface Neighborhood<T extends ImmutableSolution> {
	public enum Decision {
		Accept, Reject, Stop
	};

	public interface NeighborProcessor {
		/**
		 * Do something with the neighbor generated.
		 * 
		 * @warning: implementation of the method is not allowed to modify the
		 *           (initial) solution given as an argument to visit()
		 * 
		 * @param neighbor
		 *            Can be a modified version of the solution or a new object
		 *            (depending on the runsOnTheSpot)
		 * @return decision whether to - Accept the neighbor (i.e. the
		 *         neighborhood can generated new neighbors from this neighbor)
		 *         - Reject the neighbor (i.e. the neighborhood have to
		 *         generated new neighbors from the original solution (or the
		 *         last accepted neighbor) - Stop to return immediately from the
		 *         visit method (e.g. on expired deadline).
		 */
		Decision processNeighbor(ImmutableSolution neighbor);
	}
	
	/**
	 * Initializes the neighborhood before multiple visits
	 * @param problem TODO
	 */
	public void init(Problem problem);

	/**
	 * Generate neighbors on by one. For each neighbor generated call
	 * processor(neighbor).
	 * 
	 * @param solution
	 *            Solution for which the neighborhood will be generated. The
	 */
	public void visit(T solution, Deadline deadline, NeighborProcessor processor);

	/**
	 * We accept two possibilities: 1) (true) Argument of processNeighbor is
	 * (always!) a modified original solution (the references must be equal!).
	 * 2) (false) Argument of processNeighbor is (always!) a new object that can
	 * be copied and saved and will not be further modified by the
	 * neighborhood.visit() method.
	 */
	public boolean runsOnTheSpot();

	/**
	 * Whether it is deterministic. If it is deterministic we assume that visit
	 * will finish in finite time. Otherwise we assume that it may work
	 * Infinitely (e.g. by trying to move random processes)
	 * 
	 * @return
	 */
	public boolean isDeterministic();
}
