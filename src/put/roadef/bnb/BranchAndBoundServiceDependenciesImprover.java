package put.roadef.bnb;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.Random;

import put.roadef.Problem;

public class BranchAndBoundServiceDependenciesImprover extends BranchAndBoundRandomImprover {

	@Override
	protected int[] generateRandomArray(Problem problem, PartialSolution solution, int length) {
		Random random = problem.getRandom();
			
		int randomService = random.nextInt(problem.getNumServices());
		int[] deps = problem.getTransitiveDependencies(randomService);
		IntSet selected = new IntOpenHashSet();
		
		while (selected.size() < length) {
			int numSelected = selected.size();
			int[] processes = problem.getProcessesOfService(randomService);
			int randomProcess = processes[random.nextInt(processes.length)];
			selected.add(randomProcess);
			for (int d = 0; d < deps.length && selected.size() < length; d++) {
				processes = problem.getProcessesOfService(deps[d]);
				randomProcess = processes[random.nextInt(processes.length)];
				selected.add(randomProcess);
			}
			
			if (selected.size() == numSelected) {
				break;
			}
		}
		
		return selected.toIntArray();
	}
}
