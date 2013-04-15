package put.roadef.solvers;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Random;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.TweakOperator;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;
import put.roadef.ip.MipFastModel;
import put.roadef.neighborhoods.AllProcessesNeighborhood;
import put.roadef.tweaks.HillClimber;

public class IPOneProcessPerMachineImprover extends Solver implements TweakOperator, Setup {
	private MipFastModel model = new MipFastModel("CplexFastMipSolver");

	private int maxTimeToSpendMilliseconds = 15000;

	private TweakOperator hillclimber = new HillClimber(new AllProcessesNeighborhood(false), true);

	private long maxTimeMillis = -1;

	public IPOneProcessPerMachineImprover() {
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		//maxTimeMillis = (long) configuration.getInt(base + ".max_time_millis");
	}

	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		if (maxTimeMillis > 0)
			deadline = new Deadline(maxTimeMillis);

		Problem problem = solution.getProblem();
		Random random = problem.getRandom();

		while (!deadline.hasExpired()) {
			IntArrayList processes = new IntArrayList();
			for (int m = 0; m < problem.getNumMachines(); ++m) {
				int[] pinm = ((SmartSolution) solution).processesInMachine[m].toIntArray();
				if (pinm.length > 0)
					processes.add(pinm[random.nextInt(pinm.length)]);
			}

			if (deadline.getTimeToExpireMilliSeconds() <= 2000.0)
				break;
			Deadline tempDeadline = deadline.getShortenedBy(2000).getTrimmedTo(maxTimeToSpendMilliseconds);
			//System.out.println("IPSolver with numProcesses = " + processes.size());
			Solution s = model.modifyAssignmentsForProcesses(problem, solution, processes.toIntArray(), tempDeadline, true);			
			if (s.getCost() < solution.getCost()) {
				solution = (SmartSolution) s;				
				solution = (SmartSolution) hillclimber.tweak(solution, deadline);
				//System.out.println("Improved: " + oldCost + " -> " +  ipCost + " (IP) -> " + solution.getCost() + " (HC)");
			}
		}
		return solution;
	}

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		return tweak(initialSolution, deadline);
	}

	@Override
	public boolean isDeterministic() {
		return true;
	}

	@Override
	public boolean isGreedy() {
		return true;
	}

}
