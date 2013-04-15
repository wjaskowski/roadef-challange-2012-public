package put.roadef.neighborhoods;

import java.util.Random;

import put.roadef.Deadline;
import put.roadef.Solution;

public abstract class RandomizedNeighborhood implements Neighborhood<Solution> {
	
	public abstract void visit(Solution solution, Deadline deadline, NeighborProcessor processor, Random random);
	
	@Override
	public void visit(Solution solution, Deadline deadline, NeighborProcessor processor) {
		visit(solution, deadline, processor, solution.getProblem().getRandom());
	}
	
}
