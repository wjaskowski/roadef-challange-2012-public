package put.roadef.neighborhoods;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;

/**
 * Neighbor = a feasible solution made by swapping two processes from the same
 * service.
 */
public class SwapProcessesInGivenMachinesNeighborhood implements Neighborhood<Solution> {

	private IntArrayList machines;
	@SuppressWarnings("unused")
	private Logger logger = Logger.getLogger(SwapProcessesInGivenMachinesNeighborhood.class);
	
	public void setMachines(IntArrayList machines) {
		this.machines = machines.clone();
	}

	@Override
	public void init(Problem problem) {
	}
	
	@Override
	public void visit(Solution solution, Deadline deadline, NeighborProcessor processor) {
		SmartSolution ss = (SmartSolution) solution;
		
		IntArrayList processes = new IntArrayList();
		for (int m : machines)
			processes.addAll(ss.processesInMachine[m]);
		
		for (int p1 : processes) {
			if (deadline.hasExpired())
				return;		
			for (int p2 : processes) {
				int m1 = solution.getMachine(p1);
				int m2 = solution.getMachine(p2);
				if (m1 == m2)
					continue;
			
				ss.moveProcess(p1, m2);
				ss.moveProcess(p2, m1);
				if (!ss.isFeasible()) {
					ss.moveProcess(p1, m1);
					ss.moveProcess(p2, m2);
					continue;
				}
				
				Decision decision = processor.processNeighbor(solution);
				if (decision == Decision.Stop)
					return;
				if (decision == Decision.Reject) {					
					solution.moveProcess(p1, m1);
					solution.moveProcess(p2, m2);
				}
			}
		}
	}

	@Override
	public boolean runsOnTheSpot() {
		return true;
	}

	@Override
	public boolean isDeterministic() {
		return true;
	}
}
