package put.roadef.neighborhoods;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;

public class SwapProcessesForMachineNeighborhood implements Neighborhood<Solution> {

	private int machine;
	private Problem problem;
	@SuppressWarnings("unused")
	private Logger logger = Logger.getLogger(SwapProcessesForMachineNeighborhood.class);

	public void setMachine(int machine) {
		this.machine = machine;
	}

	@Override
	public void init(Problem problem) {
		this.problem = problem;
	}

	@Override
	public void visit(Solution solution, Deadline deadline, NeighborProcessor processor) {
		SmartSolution ss = (SmartSolution) solution;

		for (int m = 0; m < problem.getNumMachines(); ++m) {
			if (machine == m)
				continue;
			int[] processesInMachine1 = ss.processesInMachine[machine].toIntArray();
			int[] processesInMachine2 = ss.processesInMachine[m].toIntArray();

			for (int p1 : processesInMachine1) {
				if (deadline.hasExpired())
					return;
				for (int p2 : processesInMachine2) {
					int m1 = solution.getMachine(p1);
					int m2 = solution.getMachine(p2);

					ss.moveProcess(p1, m2);
					ss.moveProcess(p2, m1);
					if (!ss.isFeasible()) {
						ss.moveProcess(p1, m1);
						ss.moveProcess(p2, m2);
						continue;
					}

					Decision decision = processor.processNeighbor(solution);
					if (decision != Decision.Accept) {
						solution.moveProcess(p1, m1);
						solution.moveProcess(p2, m2);
						
					}
					if (decision == Decision.Stop) {
						return;
					}
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
