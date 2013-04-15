package put.roadef.solvers;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.Random;

import org.apache.log4j.Logger;


import put.roadef.Deadline;
import put.roadef.MachinePair;
import put.roadef.MachinePairQueue;
import put.roadef.PerformanceTimer;
import put.roadef.Problem;
import put.roadef.ProblemUtils;
import put.roadef.RuntimeStats;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.Timer;
import put.roadef.TweakOperator;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;
import put.roadef.hh.Heuristic;
import put.roadef.ip.MipFastModel;
import put.roadef.ip.MipSolver;
import put.roadef.neighborhoods.AllProcessesNeighborhood2;
import put.roadef.tweaks.HillClimber;

public class IPMachinesDynamicImprover extends Solver implements TweakOperator, Setup, Heuristic {
	private MipFastModel model = new MipFastModel("CplexFastMipSolver");
	//private MipFastModel model = new MipFastModel("GurobiFastMipSolver");

	private static Logger logger = Logger.getLogger(IPMachinesDynamicImprover.class);
	private Problem problem;

	private AllProcessesNeighborhood2 neighborhood = new AllProcessesNeighborhood2(AllProcessesNeighborhood2.Order.LoadCostOrder,
			true, true);

	private TweakOperator hillclimber = new HillClimber(neighborhood, true);

	// Configuration
	private int maxNumTriesWithoutImprovement;
	private long maxTimeMillis;
	private long optimisticCostCutoff;
	private double maxNumProcessesForSolver;
	private double maxNumMachinesForSolver;
	private double maxNumMachinesDeltaMinus;
	private double maxNumMachinesDeltaPlus;
	private double maxNumProcessesDeltaMinus;
	private double maxNumProcessesDeltaPlus;
	private int maxNumElementsInQueue;
	private boolean useHillClimber;

	private boolean randomMachines;
	int numTriesWithoutImprovement = 0;

	long hcTotalImp = 0;
	double hcTotalDurationMs = 0;
	long ipTotalImp = 0;
	double ipTotalDurationMs = 0;

	MachinePairQueue queue;
	private long maxHillClimberDurationMs;
	private Timer timer = new Timer();
	private PerformanceTimer perfTimer = new PerformanceTimer(IPMachinesDynamicImprover.class.getName());

	private double visitedCountWeight;

	public IPMachinesDynamicImprover() {

	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		optimisticCostCutoff = configuration.getInt(base + ".optimistic_cost_cutoff", 2000);
		maxNumElementsInQueue = configuration.getInt(base + ".max_num_elements_in_queue", 50000); // Max spotted 12 000 tuples for a1_4. Usually 1000-5000.
		visitedCountWeight = configuration.getDouble(base + ".visited_count_weight", 1.0);

		maxNumProcessesForSolver = configuration.getInt(base + ".max_processes_for_solver", 100);
		maxNumMachinesForSolver = configuration.getInt(base + ".max_machines_for_solver", 6);

		maxNumTriesWithoutImprovement = configuration.getInt(base + ".max_num_tries_without_improvement", 300);

		maxNumMachinesDeltaMinus = configuration.getDouble(base + ".max_num_machines_delta_minus", 1);
		maxNumProcessesDeltaMinus = configuration.getDouble(base + ".max_num_processes_delta_minus", 20);

		maxNumMachinesDeltaPlus = configuration.getDouble(base + ".max_num_machines_delta_plus", 0.025);
		maxNumProcessesDeltaPlus = configuration.getDouble(base + ".max_num_processes_delta_plus", 0.5);

		randomMachines = configuration.getBoolean(base + ".random_machines", false);

		maxHillClimberDurationMs = configuration.getInt(base + ".max_hill_climber_duration_ms", 10000000); // No deadline		

		useHillClimber = configuration.getBoolean(base + ".use_hc", true);
	}

	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		perfTimer.start("init");
		logger.info(this.getClass().getName() + ".tweak()");

		if (maxTimeMillis > 0)
			deadline = Deadline.min(new Deadline(maxTimeMillis), deadline);

		SmartSolution ss = SmartSolution.promoteToSmartSolution(solution);
		problem = ss.getProblem();

		if (!randomMachines && queue == null) // Check for null for to make Hyper Heuristic work
			queue = createQueue(deadline, ss);

		perfTimer.stop();
		int numTuplesConsidered = 0;
		while (!deadline.hasExpired()) {
			if (maxNumTriesWithoutImprovement > 0 && numTriesWithoutImprovement > maxNumTriesWithoutImprovement) {
				logger.info("Tried " + numTriesWithoutImprovement + " without improvement. Exiting.");
				queue = null; // a hack for Hyper Heuristic
				numTriesWithoutImprovement = 0; // a hack for Hyper Heuristic
				break;
			}
			numTriesWithoutImprovement += 1;

			perfTimer.start("getFirstMachinesPair");
			MachinePair pair = getFirstMachinesPair(ss);
			perfTimer.stop();
			if (pair == null) {
				logger.info("Exiting because pair is null.");
				break;
			}

			perfTimer.start("getMachinesSubset");
			IntArrayList machines = getMachinesSubset(ss, pair);
			IntOpenHashSet processes = new IntOpenHashSet();
			for (int m : machines)
				processes.addAll(ss.processesInMachine[m]);
			perfTimer.stop();

			//IntOpenHashSet newProcesses = new IntOpenHashSet(processes);
			//IntArrayList isolated = new IntArrayList();
			//for (int p : processes) 
			//	if (isIsolated(p, ss) && problem.getTransitiveRevDependencies(problem.getProcess(p).service).length > 50)
			//		newProcesses.remove(p);

			//logger.info("#p = " + processes.size() + "; #np = " + newProcesses.size());
			//processes = newProcesses;

			long oldCost = ss.getCost();
			double oldImp = ss.getImprovement();

			perfTimer.start("model.modifyAssignmentForProcesses");
			timer.start();
			ss = (SmartSolution) model.modifyAssignmentsForProcesses(problem, ss, processes.toIntArray(), deadline, true, true);
			timer.stop();
			perfTimer.stop();
			RuntimeStats.add(ss, timer.getTime(), !randomMachines ? "MipFastModel" : "MipFastModelRand");

			ipTotalDurationMs += timer.getTime();
			ipTotalImp += (oldCost - ss.getCost());

			if (model.solutionStatus != MipSolver.OPTIMAL_STATUS) {
				maxNumProcessesForSolver = Math.min(1, maxNumProcessesForSolver - maxNumProcessesDeltaMinus);
				maxNumMachinesForSolver = Math.max(2, maxNumMachinesForSolver - maxNumMachinesDeltaMinus);
				//logger.info("Smaller models, please...");
			} else {
				maxNumProcessesForSolver += maxNumProcessesDeltaPlus;
				maxNumMachinesForSolver += maxNumMachinesDeltaPlus;
				//logger.info("Bigger models, please...");
			}

			numTuplesConsidered += 1;

			perfTimer.start("queue.prepareChangePair");
			if (!randomMachines) {
				for (int m = 0; m < problem.getNumMachines(); ++m) {
					if (m != pair.m1 && m != pair.m2) {
						queue.prepareChangePair(m, pair.m1);
						queue.prepareChangePair(m, pair.m2);
					}
				}
			}
			perfTimer.stop();

			if (useHillClimber && ss.getCost() < oldCost) {
				perfTimer.start("hillClimber.tweak");
				long ipCost = ss.getCost();
				double ipImp = ss.getImprovement();

				timer.start();
				neighborhood.setMachines(machines.toIntArray());
				ss = (SmartSolution) hillclimber.tweak(ss, deadline.getTrimmedTo(maxHillClimberDurationMs));
				hcTotalDurationMs += timer.getTime();
				hcTotalImp += (ipCost - ss.getCost());

				logger.info(String.format("Was: %d(%.2f%%) -> by IP: %d(%.2f%%) -> by HC: %d(%.2f%%) IMP", oldCost, oldImp,
						ipCost, ipImp, ss.getCost(), ss.getImprovement()));

				numTriesWithoutImprovement = 0;

				perfTimer.stop();
				perfTimer.start("queue.setOptimisticCost()");
				if (!randomMachines) {
					for (int m = 0; m < problem.getNumMachines(); ++m) {
						if (m != pair.m1 && m != pair.m2) {
							if (optimisticCostCutoff < queue.getOptimisticCost(m, pair.m1))
								queue.setOptimisticCost(pair.m1, m,
										ProblemUtils.computeOptimisticImprovementForMachines(ss, m, pair.m1));
							if (optimisticCostCutoff < queue.getOptimisticCost(m, pair.m2))
								queue.setOptimisticCost(pair.m2, m,
										ProblemUtils.computeOptimisticImprovementForMachines(ss, m, pair.m2));
						}
					}
				}
				perfTimer.stop();
			}

			perfTimer.start("queue.finishChangePair()");
			if (!randomMachines) {
				queue.visitedCount[pair.m1] += 1;
				queue.visitedCount[pair.m2] += 1;

				// Recalculate optimistic value for pairs (m1,m) and (m2,m) for all m. Reconsider them in the priority queue
				for (int m = 0; m < problem.getNumMachines(); ++m) {
					if (m != pair.m1 && m != pair.m2) {
						if (optimisticCostCutoff < queue.getOptimisticCost(m, pair.m1))
							queue.finishChangePair(m, pair.m1);
						if (optimisticCostCutoff < queue.getOptimisticCost(m, pair.m2))
							queue.finishChangePair(m, pair.m2);
					}
				}
			}
			perfTimer.stop();
		}

		// Print statistics
		logger.info("numTuplesConsidered = " + numTuplesConsidered);
		double ipPerf = (ipTotalDurationMs == 0 ? 0 : ipTotalImp / ipTotalDurationMs);
		logger.info(String.format("ipTotalImp: %10d\t during %.1fs\t(%8.1f/ms)", ipTotalImp, ipTotalDurationMs / 1000.0, ipPerf));
		double hcPerf = (hcTotalDurationMs == 0 ? 0 : hcTotalImp / hcTotalDurationMs);
		logger.info(String.format("hcTotalImp: %10d\t during %.1fs\t(%8.1f/ms)", hcTotalImp, hcTotalDurationMs / 1000.0, hcPerf));

		logger.info(perfTimer.toString());

		return ss;
	}

	private IntArrayList getCriticalProcesses(int p, SmartSolution ss) {
		if (!isIsolated(p, ss)) {
			return new IntArrayList();
		}
		IntArrayList arr = new IntArrayList();

		int s = problem.getProcess(p).service;
		int n = problem.getMachine(ss.getMachine(p)).neighborhood;

		int[] depServices = problem.getTransitiveRevDependencies(s);

		for (int ds : depServices) {
			for (int m : ss.machinesInService[ds].keySet()) {
				if (problem.getMachine(m).neighborhood == n) {
					for (int p2 : ss.processesInMachine[m]) {
						if (problem.getProcess(p2).service == ds)
							arr.add(p2);
					}
				}
			}
		}

		return arr;
	}

	private boolean isIsolated(int p, SmartSolution ss) {
		Int2IntOpenHashMap map = ss.neighborhoodsInService[problem.getProcess(p).service];
		return map.get(problem.getMachine(ss.getMachine(p)).neighborhood) == 1;
	}

	private IntArrayList getMachinesSubset(SmartSolution ss, MachinePair pair) {
		IntArrayList machines = new IntArrayList(new int[] { pair.m1, pair.m2 });

		int numProcesses = ss.processesInMachine[pair.m1].size() + ss.processesInMachine[pair.m2].size();
		while (numProcesses < maxNumProcessesForSolver && machines.size() < maxNumMachinesForSolver
				&& machines.size() < problem.getNumMachines()) {

			int m3 = 0;
			double bestOptimisticImp = 0;
			for (int m = 0; m < problem.getNumMachines(); ++m) {
				//TODO: Tutaj powinnismy miec jakies drugie kryterium, remisy zdazaja sie bardzo bardzo czesto!
				if (machines.contains(m))
					continue;
				double maxopt = 0.0;
				for (int mm : machines) {
					double opt = ProblemUtils.computeOptimisticImprovementForMachines(ss, mm, m);
					if (maxopt < opt)
						maxopt = opt;
				}
				if (bestOptimisticImp < maxopt) {
					bestOptimisticImp = maxopt;
					m3 = m;
				} else if (bestOptimisticImp == maxopt) {
					if (problem.getRandom().nextDouble() < 3.0 / problem.getNumMachines())
						m3 = m;
				}
			}
			if (bestOptimisticImp < optimisticCostCutoff)
				break;

			numProcesses += ss.processesInMachine[m3].size();
			machines.add(m3);
		}
		logger.trace("Model settings: processes=" + maxNumProcessesForSolver + ", machines=" + maxNumMachinesForSolver);
		logger.trace("Candidates: " + pair.m1 + ", " + pair.m2 + ", " + "(numMachines=" + machines.size() + ", numProcesses="
				+ ss.processesInMachine[pair.m1].size() + "+" + ss.processesInMachine[pair.m2].size() + "+..." + "="
				+ numProcesses + ", numTriesWithoutImprovement=" + numTriesWithoutImprovement);

		return machines;
	}

	private MachinePair getFirstMachinesPair(SmartSolution ss) {
		MachinePair pair;
		do {
			if (!randomMachines) {
				pair = queue.getNext();
				if (pair == null) {
					logger.info("Priority queue is empty. Getting random machines");
					int m1 = problem.getRandom().nextInt(problem.getNumMachines());
					int m2 = m1;
					while (m1 == m2)
						m2 = problem.getRandom().nextInt(problem.getNumMachines());
					pair = new MachinePair(m1, m2);
				}
			} else {
				int m1 = problem.getRandom().nextInt(problem.getNumMachines());
				int m2 = m1;
				while (m1 == m2)
					m2 = problem.getRandom().nextInt(problem.getNumMachines());
				pair = new MachinePair(m1, m2);
			}
		} while (ss.processesInMachine[pair.m1].size() == 0 && ss.processesInMachine[pair.m2].size() == 0); // It is stupid to consider machine pairs without processes (HACK?)			
		return pair;
	}

	private MachinePairQueue createQueue(Deadline deadline, SmartSolution ss) {
		MachinePairQueue queue = new MachinePairQueue(problem.getNumMachines(), maxNumElementsInQueue, visitedCountWeight);

		logger.info("Adding machine pairs to the queue");
		for (int m1 = 0; m1 < problem.getNumMachines(); ++m1) {
			if (deadline.hasExpired())
				break;

			for (int m2 = 0; m2 < m1; ++m2) {
				long ocost = ProblemUtils.computeOptimisticImprovementForMachines(ss, m1, m2);
				if (optimisticCostCutoff < ocost) {
					queue.setOptimisticCost(m1, m2, ocost);
					queue.addPair(m1, m2);
				}
			}
		}

		logger.info("Queue size = " + queue.size());
		return queue;
	}

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		return tweak(initialSolution, deadline);
	}

	@Override
	public boolean isDeterministic() {
		return (!randomMachines || numTriesWithoutImprovement > maxNumTriesWithoutImprovement);
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