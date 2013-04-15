package put.roadef.selectors;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;

import put.roadef.SmartSolution;
import put.roadef.Solution;

public abstract class OptimalMachinesProcessSelector implements SwapProcessSelector, GroupProcessSelector {

	@Override
	public boolean isDeterministic() {
		return true;
	}

	@Override
	public List<IntList> getProcessesToSwap(Solution solution) {
		List<IntList> result = new ArrayList<IntList>();
		int[] machines = findOptimalMachines(solution);
		for (int m : machines) {
			result.add(new IntArrayList(((SmartSolution) solution).processesInMachine[m]));
		}
		return result;
	}

	@Override
	public List<IntList> getProcessesGroups(Solution solution) {
		List<IntList> result = new ArrayList<IntList>();
		int[] machines = findOptimalMachines(solution);
		
		IntArrayList group = new IntArrayList();
		for (int m : machines) {
			group.addAll(((SmartSolution) solution).processesInMachine[m]);
		}
		result.add(group);
		return result;
	}

	protected abstract int[] findOptimalMachines(Solution solution);
}
