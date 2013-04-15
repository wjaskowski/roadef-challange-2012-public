package put.roadef.solvers;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.ArrayList;
import java.util.Random;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.MachinePair;
import put.roadef.MachinePairQueue;
import put.roadef.Problem;
import put.roadef.ProblemUtils;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.TweakOperator;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;
import put.roadef.hh.Heuristic;
import put.roadef.ip.MipFastModel;
import put.roadef.neighborhoods.AllProcessesNeighborhood2;
import put.roadef.tweaks.HillClimber;

public class IPMachinesQuadroImprover2 extends Solver implements TweakOperator, Setup, Heuristic {
	private MipFastModel model = new MipFastModel("CplexFastMipSolver");
	//private MipFastModel model = new MipFastModel("CBCFastMipSolver");
	private static Logger logger = Logger.getLogger(IPMachinesQuadroImprover2.class);
	private Problem problem;

	private AllProcessesNeighborhood2 neighborhood = new AllProcessesNeighborhood2(AllProcessesNeighborhood2.Order.LoadCostOrder,
			true, true);
	private TweakOperator hillclimber = new HillClimber(neighborhood, true);

	// Configuration
	private int maxNumTriesWithoutImprovement;
	private long maxTimeMillis;
	private int maxTimeMillisForIPSolver;
	private long optimisticCostCutoff;
	private int maxNumProcesses;
	private int maxNumElementsInQueue;

	public IPMachinesQuadroImprover2() {
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		maxTimeMillis = (long) configuration.getInt(base + ".max_time_millis", -1);
		maxNumTriesWithoutImprovement = configuration.getInt(base + ".max_num_tries_without_improvement", 300);
		maxTimeMillisForIPSolver = configuration.getInt(base + ".max_time_millis_for_ipsolve", 3000);
		optimisticCostCutoff = configuration.getInt(base + ".optimistic_cost_cutoff", 10000);
		maxNumProcesses = configuration.getInt(base + ".max_num_processes", 150);
		maxNumElementsInQueue = configuration.getInt(base + ".max_num_elements_in_queue", 100000);
	}

	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		if (maxTimeMillis > 0)
			deadline = Deadline.min(new Deadline(maxTimeMillis), deadline);
		SmartSolution ss = SmartSolution.promoteToSmartSolution(solution);
		problem = ss.getProblem();

		int numTriesWithoutImprovement = 0;
		int numTuplesConsidered = 0;

		MachinePairQueue queue = new MachinePairQueue(problem.getNumMachines(), maxNumElementsInQueue, 1.0);

		logger.info("Adding machine pairs to the queue");
		for (int m1 = 0; m1 < problem.getNumMachines(); ++m1)
			for (int m2 = 0; m2 < m1; ++m2) {
				long ocost = ProblemUtils.computeOptimisticImprovementForMachines(ss, m1, m2);
				queue.setOptimisticCost(m1, m2, ocost);
				if (optimisticCostCutoff < ocost) {
					queue.addPair(m1, m2);
				}
			}

		logger.info("Queue size = " + queue.size());

outer:		while (!deadline.hasExpired()) {
			if (maxNumTriesWithoutImprovement > 0 && numTriesWithoutImprovement > maxNumTriesWithoutImprovement) {
				logger.info("Tried " + numTriesWithoutImprovement + " without improvement. Exiting.");
				break;
			}
			numTriesWithoutImprovement += 1;

			IntOpenHashSet processes = new IntOpenHashSet(200);
			ArrayList<MachinePair> pairs = new ArrayList<MachinePair>();
			IntOpenHashSet machines = new IntOpenHashSet();
			for (int i = 0; i < 2; ++i) {
				MachinePair pair = queue.getNext();
				if (pair == null) {
					logger.info("Priority queue is empty. Nothing to do: exit");
					break outer;
				}
				pairs.add(pair);
				machines.add(pair.m1);
				machines.add(pair.m2);

				if (ss.processesInMachine[pair.m1].size() == 0 && ss.processesInMachine[pair.m2].size() == 0) //HACK
					continue outer;

				long optimisticImp2 = queue.getOptimisticCost(pair.m1, pair.m2);

				processes.addAll(ss.processesInMachine[pair.m1]);
				processes.addAll(ss.processesInMachine[pair.m2]);									

				long priority = queue.getPriority(pair.m1, pair.m2);
				logger.trace("Candidates: " + pair.m1 + ", " + pair.m2 + ", " + " (optimistic=" + optimisticImp2 + ", priority="
						+ priority + ", numProcesses=" + ss.processesInMachine[pair.m1].size() + "+"
						+ ss.processesInMachine[pair.m2].size() + "=" + processes.size() + ", numTriesWithoutImprovement="
						+ numTriesWithoutImprovement);
			}

			long oldCost = ss.getCost();
			double oldImp = ss.getImprovement();
									

			int[] arr = org.apache.commons.lang.ArrayUtils.subarray(processes.toIntArray(), 0, maxNumProcesses);
			logger.info("Starting cplex");
			ss = (SmartSolution) model.modifyAssignmentsForProcesses(problem, ss, arr,
					deadline.getTrimmedTo(maxTimeMillisForIPSolver), true, true);
			numTuplesConsidered += 1;
			logger.info("Finished cplex");

			for (int i = 0; i < 2; ++i) {
				for (int m = 0; m < problem.getNumMachines(); ++m) {
					if (m != pairs.get(i).m1 && m != pairs.get(i).m2) {
						queue.prepareChangePair(m, pairs.get(i).m1);
						queue.prepareChangePair(m, pairs.get(i).m2);
					}
				}
			}

			if (ss.getCost() < oldCost) {
				long ipCost = ss.getCost();
				double ipImp = ss.getImprovement();

				//TODO: Tutaj jest chyba blad. Nie probuje przerzucac procesow z maszyn m1 i m2 na wszystkie maszyny.
				neighborhood.setMachines(machines.toIntArray());
				ss = (SmartSolution) hillclimber.tweak(ss, deadline);

				logger.info(String.format("Was: %d(%.2f%%) -> by IP: %d(%.2f%%) -> by HC: %d(%.2f%%) IMP", oldCost, oldImp,
						ipCost, ipImp, ss.getCost(), ss.getImprovement()));

				numTriesWithoutImprovement = 0;

				for (int i = 0; i < 2; ++i) {
					for (int m = 0; m < problem.getNumMachines(); ++m) {
						if (m != pairs.get(i).m1 && m != pairs.get(i).m2) {
							if (optimisticCostCutoff < queue.getOptimisticCost(m, pairs.get(i).m1))
								queue.setOptimisticCost(pairs.get(i).m1, m,
										ProblemUtils.computeOptimisticImprovementForMachines(ss, m, pairs.get(i).m1));
							if (optimisticCostCutoff < queue.getOptimisticCost(m, pairs.get(i).m2))
								queue.setOptimisticCost(pairs.get(i).m2, m,
										ProblemUtils.computeOptimisticImprovementForMachines(ss, m, pairs.get(i).m2));
						}
					}
				}
			}

			for (int m : machines) {
				queue.visitedCount[m] += 1;
			}

			// Recalculate optimistic value for pairs (m1,m) and (m2,m) for all m. Reconsider them in the priority queue
			for (int i = 0; i < 2; ++i) {
				for (int m = 0; m < problem.getNumMachines(); ++m) {
					if (m != pairs.get(i).m1 && m != pairs.get(i).m2) {
						if (optimisticCostCutoff < queue.getOptimisticCost(m, pairs.get(i).m1))
							queue.finishChangePair(m, pairs.get(i).m1);
						if (optimisticCostCutoff < queue.getOptimisticCost(m, pairs.get(i).m2))
							queue.finishChangePair(m, pairs.get(i).m2);
					}
				}
			}
		}
		logger.info("numPairsConsidered = " + numTuplesConsidered);
		return ss;
	}

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		return tweak(initialSolution, deadline);
	}

	@Override
	public boolean isDeterministic() {
		return true; //???
	}

	@Override
	public boolean isGreedy() {
		return true;
	}

	@Override
	public Solution move(Solution solution, Deadline deadline, Random random) {
		return tweak(solution, deadline);
	}
}