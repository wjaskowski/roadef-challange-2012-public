package put.roadef.tweaks;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.Problem;
import put.roadef.RuntimeStats;
import put.roadef.Safety;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;
import put.roadef.hh.Heuristic;
import put.roadef.neighborhoods.Neighborhood;
import put.roadef.neighborhoods.Neighborhood.Decision;
import put.roadef.neighborhoods.Neighborhood.NeighborProcessor;
import put.roadef.neighborhoods.RandomizedNeighborhood;

public class HillClimber extends Solver implements RandomizedTweakOperator, Heuristic, Setup {
	private boolean greedy;
	private boolean processEqualNeighbors;

	private Neighborhood<Solution> neighborhood;

	private StopWatch timer = new StopWatch();

	private class Stats {
		public long numNeighborhoodVisits;
		public long numFeasibleAccepted;
		public long numFeasibleNotAccepted;

		public void reset() {
			numNeighborhoodVisits = 0;
			numFeasibleAccepted = 0;
			numFeasibleNotAccepted = 0;
		}

		private final NumberFormat nf = NumberFormat.getInstance(Locale.US);

		@SuppressWarnings("unused")
		public void log(Logger logger, Level level) {
			logger.log(level, "numNeighborhoodVisits = " + nf.format(numNeighborhoodVisits));
			logger.log(level, "numFeasibleAccepted = " + nf.format(numFeasibleAccepted));
			logger.log(level, "numFeasibleNotAccepted = " + nf.format(numFeasibleNotAccepted));
		}
	}

	Stats stats = new Stats();
	private int maxNumIterations = -1;
	private long maxDurationMs = -1;

	public HillClimber() {
	}

	public HillClimber(Neighborhood<Solution> neighborhood, boolean greedy) {
		this.neighborhood = neighborhood;
		this.greedy = greedy;
	}

	public HillClimber(Neighborhood<Solution> neighborhood, boolean greedy, int maxNumIterations) {
		this(neighborhood, greedy);
		this.maxNumIterations = maxNumIterations;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		greedy = configuration.getBoolean(base + ".greedy", true);
		neighborhood = (Neighborhood<Solution>) configuration.getInstanceAndSetup(base + ".neighborhood");
		processEqualNeighbors = configuration.getBoolean(base + ".process_equal_neighbors", false);
		maxNumIterations = configuration.getInt(base + ".max_num_iterations", maxNumIterations);
		maxDurationMs = configuration.getInt(base + ".max_duration_ms", (int) maxDurationMs);
	}

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		Solution s = initialSolution;
		if (!(initialSolution instanceof SmartSolution))
			s = new SmartSolution(s);
		return tweak(s, deadline, problem.getRandom());
	}

	boolean bestSolutionImproved;
	ImmutableSolution bestSolution;
	Solution greedyBestSolution;
	ImmutableSolution greedyBestSolutionLight;
	long bestSolutionCost;

	private Logger logger = Logger.getLogger(HillClimber.class);

	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		return tweak(solution, deadline, solution.getProblem().getRandom());
	}

	@Override
	public Solution tweak(Solution solution, Deadline deadline, Random random) {
		Deadline d = (maxDurationMs > 0 ? deadline.getTrimmedTo(maxDurationMs) : deadline);
		neighborhood.init(solution.getProblem());
		return greedy ? tweakGreedy(solution, d, random) : tweakSteepest(solution, d, random);
	}

	@Override
	public Solution move(Solution solution, Deadline deadline, Random random) {
		greedyBestSolutionLight = solution;
		bestSolutionCost = solution.getCost();

		neighborhood.init(solution.getProblem());
		neighborhood.visit(solution, deadline, new GreedyEqualNeighborProcessor(this, deadline, processEqualNeighbors));

		return new SmartSolution(greedyBestSolutionLight);
	}

	private Solution tweakSteepest(Solution solution, final Deadline deadline, Random random) {
		bestSolutionImproved = true;

		NeighborProcessor processor = new NeighborProcessor() {
			@Override
			public Decision processNeighbor(ImmutableSolution s) {
				if (bestSolution.getCost() > s.getCost()) {
					bestSolution = s.lightClone();
					bestSolutionImproved = true;
				}
				if (deadline.hasExpired())
					return Decision.Stop;
				// In steepest version we always reject in order to wait for the best
				return Decision.Reject;
			}
		};

		while (bestSolutionImproved && !deadline.hasExpired()) {
			bestSolutionImproved = false;
			bestSolution = solution.lightClone();
			if (neighborhood instanceof RandomizedNeighborhood) {
				((RandomizedNeighborhood) neighborhood).visit(solution, deadline, processor, random);
			} else {
				neighborhood.visit(solution, deadline, processor);
			}

			if (bestSolutionImproved)
				solution = new SmartSolution(bestSolution);
		}

		return solution;
	}

	public Solution tweakGreedy(Solution solution, final Deadline deadline, Random random) {
		timer.reset();
		timer.start();

		SmartSolution ss = (SmartSolution) solution;
		ss.stats.reset();

		stats.reset();

		bestSolutionImproved = true;
		greedyBestSolutionLight = greedyBestSolution = ss;
		bestSolutionCost = ss.getCost();

		NeighborProcessor processor = new NeighborProcessor() {
			@Override
			public Decision processNeighbor(ImmutableSolution neighbor) {
				boolean improved = false;
				if (bestSolutionCost > neighbor.getCost()) {
					bestSolutionCost = neighbor.getCost();
					bestSolutionImproved = improved = true;
					greedyBestSolutionLight = neighbor;
					stats.numFeasibleAccepted += 1;
				} else {
					stats.numFeasibleNotAccepted += 1;
				}

				if (deadline.hasExpired())
					return Decision.Stop;
				// In greedy version we accept to work on the new solution if it has improved

				return improved ? Decision.Accept : Decision.Reject;
			}
		};

		NeighborProcessor equalNeighborProcessor = new GreedyEqualNeighborProcessor(this, deadline, true);

		while (bestSolutionImproved && !deadline.hasExpired()
				&& (maxNumIterations < 0 || stats.numNeighborhoodVisits < maxNumIterations)) {
			Safety.saveSolution(ss);

			long oldCost = solution.getCost();
			double oldImp = solution.getImprovement();

			bestSolutionImproved = false;

			//TODO: PromoteToSmartSolution
			if (!(greedyBestSolutionLight instanceof SmartSolution))
				greedyBestSolution = new SmartSolution(greedyBestSolutionLight);
			else
				greedyBestSolution = (SmartSolution) greedyBestSolutionLight;

			if (neighborhood instanceof RandomizedNeighborhood) {
				((RandomizedNeighborhood) neighborhood).visit(greedyBestSolution, deadline, processor, random);
			} else {
				neighborhood.visit(greedyBestSolution, deadline, processor);
				stats.numNeighborhoodVisits += 1;
			}

			if (bestSolutionImproved)
				RuntimeStats.add(ss, 0, this.getClass().getSimpleName());

			if (!bestSolutionImproved && processEqualNeighbors) {
				logger.info("Looking for at least equal neighbor");
				if (neighborhood instanceof RandomizedNeighborhood) {
					((RandomizedNeighborhood) neighborhood).visit(greedyBestSolution, deadline, equalNeighborProcessor, random);
				} else {
					neighborhood.visit(greedyBestSolution, deadline, equalNeighborProcessor);
					stats.numNeighborhoodVisits += 1;
				}
			}

			//Safety.saveSolution(solution);

			// solution is always the best solution found so far
			if (oldCost > solution.getCost())
				logger.info(String.format("Was: %d(%.2f%%) -> by HC: %d(%.2f%%) IMP", oldCost, oldImp, solution.getCost(),
						solution.getImprovement()));
		}
		//stats.log(logger, Level.TRACE);
		//ss.stats.log(SmartSolution.logger, Level.TRACE);

		timer.stop();
		RuntimeStats.add(solution, timer.getTime(), this.getClass().getSimpleName());

		if (greedyBestSolutionLight instanceof SmartSolution)
			return (SmartSolution) greedyBestSolutionLight;
		return new SmartSolution(greedyBestSolutionLight);
	}

	@Override
	public boolean isDeterministic() {
		return neighborhood.isDeterministic();
	}

	@Override
	public boolean isGreedy() {
		return greedy;
	}

	public static class GreedyEqualNeighborProcessor implements NeighborProcessor {

		private HillClimber hc;
		private Deadline deadline;
		private Set<ImmutableSolution> equalSolutions;

		private boolean acceptEqual;

		public GreedyEqualNeighborProcessor(HillClimber hc, Deadline deadline) {
			this(hc, deadline, false);
		}

		public GreedyEqualNeighborProcessor(HillClimber hc, Deadline deadline, boolean acceptEqual) {
			this.hc = hc;
			this.deadline = deadline;
			this.acceptEqual = acceptEqual;
			equalSolutions = new HashSet<ImmutableSolution>();
		}

		@Override
		public Decision processNeighbor(ImmutableSolution neighbor) {
			boolean fresh = false;
			if (hc.bestSolutionCost >= neighbor.getCost()) {
				if (hc.bestSolutionCost > neighbor.getCost()) {
					hc.bestSolutionCost = neighbor.getCost();
					equalSolutions.clear();
				} else if (!acceptEqual || equalSolutions.contains(neighbor)) {
					return Decision.Reject;
				} else {
					equalSolutions.add(neighbor.lightClone());
				}

				hc.bestSolutionImproved = fresh = true;
				hc.greedyBestSolutionLight = neighbor;
			}

			if (deadline.hasExpired())
				return Decision.Stop;

			return fresh ? Decision.Accept : Decision.Reject;
		}
	}
}
