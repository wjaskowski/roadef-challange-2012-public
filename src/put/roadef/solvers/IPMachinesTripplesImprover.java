package put.roadef.solvers;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.TweakOperator;
import put.roadef.ip.MipFastModel;
import put.roadef.neighborhoods.AllProcessesNeighborhood;
import put.roadef.tweaks.HillClimber;

public class IPMachinesTripplesImprover extends Solver implements TweakOperator {
	private MipFastModel model = new MipFastModel("CplexMipSolver");

        private int maxTimeToSpendMilliseconds = 6000;

        private TweakOperator hillclimber = new HillClimber(new AllProcessesNeighborhood(false), true);
    	@SuppressWarnings("unused")
        private TweakOperator quadroimprover = new IPMachinesQuadroImprover();

        private int maxNumTriesWithoutImprovement = 10000;

		private Logger logger = Logger.getLogger(IPMachinesTripplesImprover.class);

        public IPMachinesTripplesImprover() {
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

                        long loadCosts[][] = new long[problem.getNumMachines()][];
                        for (int m = 0; m < problem.getNumMachines(); ++m) {
                                loadCosts[m] = new long[problem.getNumResources()];
                                for (int r = 0; r < problem.getNumResources(); ++r) {
                                        loadCosts[m][r] += problem.computeLoadCostNotWeighted(solution.getResourceUsage(m, r),
                                                        problem.getMachine(m).safetyCapacities[r]);
                                }
                        }

                        long maxDiff = 0;
                        int maxM1 = 0;
                        int maxM2 = 0;
                        int maxM3 = 0;

                        for (int m1 = 0; m1 < problem.getNumMachines(); ++m1) {
                                for (int m2 = m1 + 1; m2 < problem.getNumMachines(); ++m2) {
                                        for (int m3 = m2 + 1; m3 < problem.getNumMachines(); ++m3) {
                                                if (tabu.contains(m1 * problem.getNumMachines() * problem.getNumMachines()
                                                                + m2 * problem.getNumMachines() + m3))
                                                        continue;
                                                for (int r = 0; r < problem.getNumResources(); ++r) {
                                                        long totalLoadCost = problem.computeLoadCostNotWeighted(
                                                                        solution.getResourceUsage(m1, r)
                                                                                        + solution.getResourceUsage(m2, r)
                                                                                        + solution.getResourceUsage(m3, r),
                                                                        problem.getMachine(m1).safetyCapacities[r]
                                                                                        + problem.getMachine(m2).safetyCapacities[r]
                                                                                        + problem.getMachine(m3).safetyCapacities[r]);

                                                        long diff = loadCosts[m1][r] + loadCosts[m2][r] + loadCosts[m3][r]
                                                                        - totalLoadCost;
                                                        if (maxDiff < diff) {
                                                                maxDiff = diff;
                                                                maxM1 = m1;
                                                                maxM2 = m2;
                                                                maxM3 = m3;
                                                        }
                                                }
                                        }
                                }
                        }
                        if (maxDiff == 0) {
                                if (tabu.size() == 0) {
                                	logger.info("maxDiff == 0 && tabu.size() == 0");
                                        break;
                                }
                                tabu.clear();
                                continue;
                        }
                        tabu.add(maxM1 * problem.getNumMachines() * problem.getNumMachines() + maxM2
                                        * problem.getNumMachines() + maxM3);

                        IntArrayList processes = new IntArrayList();
                        processes.addAll(((SmartSolution) solution).processesInMachine[maxM1]);
                        processes.addAll(((SmartSolution) solution).processesInMachine[maxM2]);
                        processes.addAll(((SmartSolution) solution).processesInMachine[maxM3]);
                        System.out.println("Candidates: " + maxM1 + ", " + maxM2 + ", " + maxM3 + " (diff= "
                                      + maxDiff + ", numProcesses = " + processes.size()
                                        + ", numTriesWithoutImprovement = " + numTriesWithoutImprovement);
                        if (deadline.getTimeToExpireMilliSeconds() <= 2000.0)
                                break;
            			Deadline tempDeadline = deadline.getShortenedBy(2000).getTrimmedTo(maxTimeToSpendMilliseconds);
                        long oldCost = solution.getCost();
                        double oldImp = solution.getImprovement();
                        solution = model.modifyAssignmentsForProcesses(problem, solution, processes.toIntArray(), tempDeadline, true);
                        if (solution != null && solution.getCost() < oldCost) {
            				long ipCost = solution.getCost();
            				double ipImp = solution.getImprovement();
            				solution = (SmartSolution) hillclimber.tweak(solution, deadline);                                //if (100 * (oldCost - solution.getCost()) / (double)solution.getCost() > 0.05)
                                numTriesWithoutImprovement = 0;
                                
                                String msg = String.format("Was: %d(%.2f%%) -> by IP: %d(%.2f%%) -> by HC: %d(%.2f%%) IMP", oldCost, oldImp, ipCost, ipImp, solution.getCost(), solution.getImprovement());
                				logger.info(msg);
                        } else {
                                numTriesWithoutImprovement += 1;
                                if (maxNumTriesWithoutImprovement > 0
                                                && numTriesWithoutImprovement > maxNumTriesWithoutImprovement) {
                                        //System.out.println("Fallback to Quadro");
                                        //solution = quadroimprover.tweak(solution, deadline);
                                	return solution;
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