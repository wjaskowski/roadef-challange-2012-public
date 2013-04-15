package put.roadef.hh;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import java.util.Random;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

public class TunerHyperHeuristicSolver extends HyperHeuristicSolver implements Setup {

	int tunedHeuristic;
	private int maxTime;
	private int minTime;
	private int timeStep;
	private int numUses;

	private double[] initialImprovements;
	private double[] averagePerformance;
	private long[] totalImprovement;

	private DoubleArrayList[] performances;

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		Random random = problem.getRandom();

		int phases = ((maxTime - minTime) / timeStep) + 1;
		initialImprovements = new double[phases];
		averagePerformance = new double[phases];
		totalImprovement = new long[phases];
		performances = new DoubleArrayList[phases];

		Solution s = initialSolution;

		for (int time = minTime, currentPhase = 0; time <= maxTime && !deadline.hasExpired(); time += timeStep, currentPhase++) {
			logger.info("STARTING TUNING WITH TIME = " + time);

			heuristicStats.clear();

			heuristicMaxTimes[tunedHeuristic] = time;
			s = new SmartSolution(initialSolution);

			for (int i = 0; i < heuristics.size(); i++) {
				HeuristicStats stats = new HeuristicStats(heuristics.get(i), i, useLastPerformanceOnly, saveZeroImprovement);
				heuristicStats.add(stats);
			}

			HeuristicStats tunedStats = heuristicStats.get(tunedHeuristic);
			double costBefore = s.getCost();
			s = makeHeuristicMove(deadline, s, random, tunedStats);
			double costDiff = costBefore - s.getCost();

			initialImprovements[currentPhase] = ((double) costDiff * 100) / costBefore;
			performances[currentPhase] = new DoubleArrayList();

			int uses = 1;
			while (uses < numUses && !deadline.hasExpired()) {
				for (int i = 0; i < heuristics.size(); i++) {
					if (i == tunedHeuristic)
						continue;

					costBefore = s.getCost();
					s = makeHeuristicMove(deadline, s, random, heuristicStats.get(i));
					costDiff = costBefore - s.getCost();

					if (costDiff > 0) {
						costBefore = s.getCost();
						s = makeHeuristicMove(deadline, s, random, tunedStats);
						costDiff = costBefore - s.getCost();
						performances[currentPhase].add(((double) costDiff * 100) / costBefore);
						
						if (++uses == numUses)
							break;
					}
				}
			}

			totalImprovement[currentPhase] = tunedStats.getTotalImprovement();
			averagePerformance[currentPhase] = tunedStats.getAveragePerformance();
		}

		for (int time = minTime, currentPhase = 0; time <= maxTime; time += timeStep, currentPhase++) {
			logger.info("TIME :" + time);
			logger.info("INITIAL IMPROVEMENT : " + initialImprovements[currentPhase] + " %");
			logger.info("AVERAGE PERFORMANCE : " + averagePerformance[currentPhase]);
			logger.info("TOTAL IMPROVEMENT : " + totalImprovement[currentPhase]);
			logger.info(performances[currentPhase]);
		}

		return s;
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		super.setup(configuration, base);

		tunedHeuristic = configuration.getInt(base + ".tuned_heuristic");
		minTime = configuration.getInt(base + ".min_time");
		maxTime = configuration.getInt(base + ".max_time");
		timeStep = configuration.getInt(base + ".time_step");

		numUses = configuration.getInt(base + ".num_uses");
	}

}
