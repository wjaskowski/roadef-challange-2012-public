package put.roadef;



/**
 * Moves a candidates solution in a search space. Represents basic step of any
 * iterative algorithm - local search step, perturbation of stochastic
 * algorithms or mutation in evolutionary algorithms. Just a modification of
 * candidate solution.
 * 
 * @author marcin
 * 
 */
public interface TweakOperator {	
	/**
	 * Tweaks the solution. It could be an improvement, but it may be also a perturbation.
	 * It can (and should!) change the solution given as the argument. It may return the same solution to the one given (the same but tweaked).
	 */
	Solution tweak(Solution solution, Deadline deadline);
	
	/**
	 * True if the Tweak Operator is deterministic (assuming no deadline has arrived). 
	 */
	boolean isDeterministic();
	
	/**
	 * Returns true if it stops as soon as first tweaked solution is found.
	 */
	boolean isGreedy();
}
