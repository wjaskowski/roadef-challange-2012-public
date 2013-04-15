package put.roadef.solvers;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.TweakOperator;
import put.roadef.ip.MipFastModel;
import put.roadef.neighborhoods.AllProcessesNeighborhood;
import put.roadef.tweaks.HillClimber;

public class IPOverloadedMachineImprover extends Solver implements TweakOperator {
	private MipFastModel model = new MipFastModel("CplexFastMipSolver");

	private int maxTimeToSpendMilliseconds = 10000;

	private TweakOperator hillclimber = new HillClimber(new AllProcessesNeighborhood(false), true);

	public IPOverloadedMachineImprover() {
	}

	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		Problem problem = solution.getProblem();

		solution = new SmartSolution(solution);
		//System.out.println();
		//System.out.println("bestSolution = " + solution.getCost());
		IntOpenHashSet tabu = new IntOpenHashSet();
		boolean improved = false;
		while (!deadline.hasExpired()) {

			long maxLoadCost = -1;
			int maxM = 0;
			long loadCosts[] = new long[problem.getNumMachines()];
			for (int m = 0; m < problem.getNumMachines(); ++m) {
				if (tabu.contains(m))
					continue;
				for (int r = 0; r < problem.getNumResources(); ++r) {
					loadCosts[m] += problem.computeLoadCostNotWeighted(solution.getResourceUsage(m, r),
							problem.getMachine(m).safetyCapacities[r]);
				}
				if (maxLoadCost < loadCosts[m]) {
					maxLoadCost = loadCosts[m];
					maxM = m;
				}
			}

			tabu.add(maxM);

			IntArrayList processes = new IntArrayList();
			processes.addAll(((SmartSolution) solution).processesInMachine[maxM]);
			//System.out.println("Candidates: " + maxM + " (diff= " + maxLoadCost
			//	+ ", numProcesses = " + processes.size());
			if (deadline.getTimeToExpireMilliSeconds() <= 2000.0)
				break;
			Deadline tempDeadline = deadline.getShortenedBy(2000).getTrimmedTo(maxTimeToSpendMilliseconds);
			Solution s = model.modifyAssignmentsForProcesses(problem, solution, processes.toIntArray(), tempDeadline, true);
			if (s.getCost() < solution.getCost()) {
				solution = (SmartSolution) s;				
				solution = (SmartSolution) hillclimber.tweak(solution, deadline);
				//System.out.println("Improved: (IP)" + ipCost + " -> (HC)" + solution.getCost());
				improved = true;
			}

			if (tabu.size() == problem.getNumMachines()) {
				if (!improved)
					break;
				tabu.clear();
				improved = false;
			}

			//System.out.println("Next iteration");
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
