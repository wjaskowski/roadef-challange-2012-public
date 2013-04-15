package put.roadef.selectors;

import java.util.SortedSet;
import java.util.TreeSet;

import put.roadef.MachinePairPotential;
import put.roadef.Problem;
import put.roadef.ProblemUtils;
import put.roadef.Solution;

public class MachinePairProcessSelector extends OptimalMachinesProcessSelector {

	private int pairPositionToConsider;
	private boolean recalculate;
	
	private SortedSet<MachinePairPotential> machinePairSet = new TreeSet<MachinePairPotential>();
	private MachinePairPotential[] machinePairs;
	
	private static final int NUM_PAIRS = 10000;
	
	@Override
	protected int[] findOptimalMachines(Solution solution) {
		Problem problem = solution.getProblem();
		
		if (recalculate) {
			recalculate = false;
			machinePairSet.clear();
			pairPositionToConsider = -1;
			
			for (int m1 = 0; m1 < problem.getNumMachines(); ++m1) {
				for (int m2 = 0; m2 < m1; ++m2) {
					long optimisticImp = ProblemUtils.computeOptimisticImprovementForMachines(solution, m1, m2);
					if (optimisticImp <= 0) {
						continue;
					}
					
					if (machinePairSet.size() < NUM_PAIRS) {
						machinePairSet.add(new MachinePairPotential(m1, m2, optimisticImp));
					} else if (optimisticImp > machinePairSet.last().potential) {
						machinePairSet.remove(machinePairSet.last());
						machinePairSet.add(new MachinePairPotential(m1, m2, optimisticImp));
					}
				}
			}
			
			machinePairs = machinePairSet.toArray(new MachinePairPotential[0]);
		}
		
		if (++pairPositionToConsider == machinePairs.length) {
			recalculate = true;
			return new int[] { };
		} 
		
		MachinePairPotential pair = machinePairs[pairPositionToConsider];
		return new int[] {pair.m1, pair.m2};
	}

	@Override
	public void reset() {
		recalculate = true;
	}
	
}