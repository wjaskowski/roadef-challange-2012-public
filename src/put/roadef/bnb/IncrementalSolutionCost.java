package put.roadef.bnb;


public interface IncrementalSolutionCost extends Assignable {	
	
	/**
	 * Returns cost related to this constraint.
	 */
	long getCost();
	
	/**
	 * Returns lower bound of this cost in case of potential final solution
	 */
	long getLowerBound(PartialSolution partialSolution);
}
