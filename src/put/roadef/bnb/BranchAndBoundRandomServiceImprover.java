package put.roadef.bnb;

import put.roadef.MyArrayUtils;
import put.roadef.Problem;
import put.roadef.Problem.Service;

public class BranchAndBoundRandomServiceImprover extends BranchAndBoundRandomImprover {
	
	@Override
	protected int[] generateRandomArray(Problem problem, PartialSolution solution, int length) {
		int randomService = random.nextInt(problem.getNumServices());
		Service service = problem.getService(randomService);

		return MyArrayUtils.random(service.processes.clone(), random, length);
	}
}
