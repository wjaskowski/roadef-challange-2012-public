package put.roadef.bnb;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import put.roadef.MyArrayUtils;
import put.roadef.Problem;

public class BranchAndBoundRandomLocationImprover extends BranchAndBoundRandomImprover {
	
	@Override
	protected int[] generateRandomArray(Problem problem, PartialSolution solution, int length) {
		int randomLocation = random.nextInt(problem.getNumLocations());
		int[] machines = problem.getMachinesInLocation(randomLocation);
		
		IntSet processes = new IntOpenHashSet();
		for (int m : machines) {
			processes.addAll(solution.getProcessesInMachine(m));
		}

		return MyArrayUtils.random(processes.toIntArray(), random, length);
	}
}
