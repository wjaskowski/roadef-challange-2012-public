package put.roadef.hh;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Safety;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.conf.RoadefConfiguration;

public class HyperHeuristicSolver extends Solver {

	protected long maxTimePerMove;
	protected long heuristicMaxTimes[];
	protected long recomputeTime;

	protected boolean saveZeroImprovement;
	protected boolean useLastPerformanceOnly;

	protected List<Heuristic> heuristics = new ArrayList<Heuristic>();
	protected List<HeuristicStats> toRecompute = new ArrayList<HeuristicStats>();
	protected List<HeuristicStats> heuristicStats = new ArrayList<HeuristicStats>();
	protected static Logger logger = Logger.getLogger(HyperHeuristicSolver.class);

	TreeSet<HeuristicStats> queue = new TreeSet<HeuristicStats>();
	Set<HeuristicStats> exploitedHeuristics = new HashSet<HeuristicStats>();

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		exploitedHeuristics.clear();
		heuristicStats.clear();
		queue.clear();

		Solution s = initialSolution;
		if (!(initialSolution instanceof SmartSolution)) {
			s = new SmartSolution(s);
		}

		Random random = problem.getRandom();

		for (int i = 0; i < heuristics.size(); i++) {
			HeuristicStats stats = new HeuristicStats(heuristics.get(i), i, useLastPerformanceOnly, saveZeroImprovement);
			heuristicStats.add(stats);
			s = processHeuristicMove(deadline, s, random, stats);
		}

		while (!deadline.hasExpired() && queue.size() > 0) {
			logger.info("QUEUE State:");
			int counter = 0;
			toRecompute.clear();
			long now = System.currentTimeMillis();
			for (HeuristicStats stats : queue) {
				if (now - stats.getLastUseTime() > recomputeTime) {
					logger.info("RECOMPUTING: Heuristic # " + stats.getId() + " was not applied for "
							+ (now - stats.getLastUseTime()) + " ms");
					toRecompute.add(stats);
				}

				logger.info(counter + " : heuristic # " + stats.getId() + " -> " + stats.getLastPerformance());
				counter++;
			}

			for (HeuristicStats stats : toRecompute) {
				s = processHeuristicMove(deadline, s, random, stats);
				Safety.saveSolution(s);
			}

			HeuristicStats bestHeuristic = queue.pollFirst();
			s = processHeuristicMove(deadline, s, random, bestHeuristic);
			Safety.saveSolution(s);
		}

		for (HeuristicStats stats : heuristicStats) {
			stats.logPerformance();
		}

		return s;
	}

	protected Solution makeHeuristicMove(Deadline deadline, Solution s, Random random, HeuristicStats heuristicStats) {
		Heuristic heuristic = heuristicStats.getHeuristic();
		long costBefore = s.getCost();
		long startTime = System.currentTimeMillis();
		s = heuristic.move(s, Deadline.min(new Deadline(heuristicMaxTimes[heuristicStats.getId()]), deadline), random);
		long timeDiff = System.currentTimeMillis() - startTime;
		long costDiff = costBefore - s.getCost();

		heuristicStats.saveUsage(timeDiff, costDiff);
		logger.info(String.format("Heuristic # %d has improved the solution by %d (%f %%) in %d ms; its last performance = %f",
				heuristicStats.getId(), costDiff, (double) costDiff * 100 / costBefore, timeDiff,
				heuristicStats.getLastPerformance()));

		return s;
	}

	private Solution processHeuristicMove(Deadline deadline, Solution s, Random random, HeuristicStats heuristicStats) {
		Heuristic heuristic = heuristicStats.getHeuristic();
		double costBefore = s.getCost();
		s = makeHeuristicMove(deadline, s, random, heuristicStats);
		double costDiff = costBefore - s.getCost();

		if (costDiff > 0) {
			queue.add(heuristicStats);
			for (HeuristicStats stats : exploitedHeuristics) {
				queue.add(stats);
			}
			exploitedHeuristics.clear();
		} else if (heuristic.isDeterministic()) {
			exploitedHeuristics.add(heuristicStats);
		} else {
			queue.add(heuristicStats);
		}

		return s;
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		saveZeroImprovement = configuration.getBoolean(base + ".save_zero_improvement", false);
		useLastPerformanceOnly = configuration.getBoolean(base + ".use_last_performance", true);
		maxTimePerMove = configuration.getInt(base + ".max_time_per_move", 300000);
		recomputeTime = configuration.getInt(base + ".recompute_time", 50000);

		int numHeuristics = configuration.getInt(base + ".num_heuristics");
		heuristicMaxTimes = new long[numHeuristics];
		for (int i = 0; i < numHeuristics; ++i) {
			heuristics.add((Heuristic) configuration.getInstanceAndSetup(base + ".heuristic." + i));
			heuristicMaxTimes[i] = configuration.getInt(base + ".heuristic." + i + ".max_time", (int) maxTimePerMove);
		}
	}
}
