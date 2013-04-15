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
import put.roadef.tweaks.IPRandomImprover;

public class IPMachinesQuadroImprover extends Solver implements TweakOperator {
	private MipFastModel model = new MipFastModel("CplexFastMipSolver");

	private int maxTimeToSpendMilliseconds = 6000;

	private TweakOperator hillclimber = new HillClimber(new AllProcessesNeighborhood(false), true);
	private TweakOperator iprandom = new IPRandomImprover();

	private int maxNumTriesWithoutImprovement = 5;

	public IPMachinesQuadroImprover() {
	}

	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		Problem problem = solution.getProblem();

		solution = new SmartSolution(solution);
		//System.out.println();
		//System.out.println("bestSolution = " + solution.getCost());
		IntOpenHashSet tabu = new IntOpenHashSet();

		int numTriesWithoutImprovement = 0;

		while (!deadline.hasExpired()) {

			long loadCosts[] = new long[problem.getNumMachines()];
			for (int m = 0; m < problem.getNumMachines(); ++m) {
				for (int r = 0; r < problem.getNumResources(); ++r) {
					loadCosts[m] += problem.computeLoadCostNotWeighted(solution.getResourceUsage(m, r),
							problem.getMachine(m).safetyCapacities[r]);
				}
			}

			long maxDiff = 0;
			int maxM1 = 0;
			int maxM2 = 0;
			int maxM3 = 0;
			int maxM4 = 0;

			for (int m1 = 0; m1 < problem.getNumMachines(); ++m1) {
				for (int m2 = m1 + 1; m2 < problem.getNumMachines(); ++m2) {
					for (int m3 = m2 + 1; m3 < problem.getNumMachines(); ++m3) {
						for (int m4 = m3 + 1; m4 < problem.getNumMachines(); ++m4) {
							if (tabu.contains(m1 * problem.getNumMachines()
									* problem.getNumMachines() * problem.getNumMachines() + m2
									* problem.getNumMachines() * problem.getNumMachines() + m3
									* problem.getNumMachines() + m4))
								continue;
							long totalLoadCost = 0;
							for (int r = 0; r < problem.getNumResources(); ++r) {
								totalLoadCost += problem.computeLoadCostNotWeighted(
										solution.getResourceUsage(m1, r)
												+ solution.getResourceUsage(m2, r)
												+ solution.getResourceUsage(m3, r)
												+ solution.getResourceUsage(m4, r),
										problem.getMachine(m1).safetyCapacities[r]
												+ problem.getMachine(m2).safetyCapacities[r]
												+ problem.getMachine(m3).safetyCapacities[r]
												+ problem.getMachine(m4).safetyCapacities[r]);
							}
							long diff = loadCosts[m1] + loadCosts[m2] + loadCosts[m3] + loadCosts[m4]
									- totalLoadCost;
							if (maxDiff < diff) {
								maxDiff = diff;
								maxM1 = m1;
								maxM2 = m2;
								maxM3 = m3;
								maxM4 = m4;
							}
						}
					}
				}
			}
			if (maxDiff == 0)
				break;
			tabu.add(maxM1 * problem.getNumMachines() * problem.getNumMachines() * problem.getNumMachines() + maxM2
					* problem.getNumMachines() * problem.getNumMachines() + maxM3 * problem.getNumMachines() + maxM4);

			IntArrayList processes = new IntArrayList();
			processes.addAll(((SmartSolution) solution).processesInMachine[maxM1]);
			processes.addAll(((SmartSolution) solution).processesInMachine[maxM2]);
			processes.addAll(((SmartSolution) solution).processesInMachine[maxM3]);
			processes.addAll(((SmartSolution) solution).processesInMachine[maxM4]);
			//System.out.println("Candidates: " + maxM1 + ", " + maxM2 + ", " + maxM3 + ", " + maxM4 + " (diff= "
//					+ maxDiff + ", numProcesses = " + processes.size());
			if (deadline.getTimeToExpireMilliSeconds() <= 2000.0)
				break;
			Deadline tempDeadline = deadline.getShortenedBy(2000).getTrimmedTo(maxTimeToSpendMilliseconds);
			Solution s = model.modifyAssignmentsForProcesses(problem, solution,
					processes.toIntArray(), tempDeadline, true);
			if (s.getCost() < solution.getCost()) {
				solution = (SmartSolution) s;
				//long ipCost = solution.getCost();
				solution = (SmartSolution) hillclimber.tweak(solution, deadline);
				//System.out.println("Improved: (IP)" + ipCost + " -> (HC)" + solution.getCost());
				numTriesWithoutImprovement = 0;
			} else {
				numTriesWithoutImprovement += 1;
				if (maxNumTriesWithoutImprovement > 0
						&& numTriesWithoutImprovement > maxNumTriesWithoutImprovement) {
					//System.out.println("Fallback to LongTimeIPSolver");

					while (!deadline.hasExpired()) {
						s = iprandom.tweak(solution, deadline);
						if (s != null && s.getCost() < solution.getCost()) {
							//long ipCost = s.getCost();
							solution = hillclimber.tweak(s, deadline);
							//System.out.println("Improved: (IP)" + ipCost + " -> (HC)"
								//	+ solution.getCost());
							numTriesWithoutImprovement = 0;
							break;
						}
					}
				}
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
