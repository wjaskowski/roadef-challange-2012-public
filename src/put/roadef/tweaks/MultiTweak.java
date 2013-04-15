package put.roadef.tweaks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.TweakOperator;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

public class MultiTweak extends Solver implements TweakOperator, Setup {

	private boolean greedy;
	private int numThreads;

	private ArrayList<TweakOperator> tweaks;
	private ExecutorService threadPool;

	private Logger logger = Logger.getLogger(MultiTweak.class);

	public MultiTweak(TweakOperator... tweaks) {
		this.tweaks = new ArrayList<TweakOperator>(Arrays.asList(tweaks));
	}

	public MultiTweak() {
		this.tweaks = new ArrayList<TweakOperator>();
	}

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		Solution bestSolution = initialSolution.clone();
		while (!deadline.hasExpired()) {
			bestSolution = tweak(bestSolution, deadline);
		}		
		return bestSolution;
	}
	
	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		Solution bestSolution = solution;
		long bestFitness = solution.getCost();
		logger.info("Cost before tweaking: " + bestFitness);
		threadPool = Executors.newFixedThreadPool(numThreads);
		
		for (int t = 0; t < tweaks.size(); t += numThreads) {
			List<Solution> candidateSolutions = new ArrayList<Solution>();
			List<TweakOperatorTask> tasks = new ArrayList<TweakOperatorTask>();

			for (int i = 0; i < numThreads; i++) {
				if (t + i >= tweaks.size()) {
					break;
				}

				if (tweaks.get(t + i) instanceof RandomizedTweakOperator) {
					tasks.add(new TweakOperatorTask((RandomizedTweakOperator) tweaks.get(t + i),
							bestSolution.clone(), deadline, solution.getProblem().getRandom(i)));
				} else {
					tasks.add(new TweakOperatorTask(tweaks.get(t + i), bestSolution.clone(),
							deadline));
				}
			}

			long start = System.currentTimeMillis();
			logger.info("Starting " + tasks.size() + " tweaking threads.");
			try {
				List<Future<Solution>> results = threadPool.invokeAll(tasks);
				for (Future<Solution> future : results) {
					candidateSolutions.add(future.get());
				}
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long end = System.currentTimeMillis();
			logger.info("All tasks completed in " + (end - start) + " ms");

			boolean foundBetter = false;
			for (Solution candidateSolution : candidateSolutions) {
				long fitness = candidateSolution.getCost();
				logger.info("Found solution with fitness = " + fitness);
				if (fitness < bestFitness) {
					bestFitness = fitness;
					bestSolution = candidateSolution;
					foundBetter = true;
				}
			}

			if (foundBetter && greedy) {
				break;
			}
		}

		threadPool.shutdown();
		return bestSolution;
	}

	@Override
	public boolean isDeterministic() {
		for (TweakOperator tweak : tweaks) {
			if (!tweak.isDeterministic()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		numThreads = configuration.getInt(base + ".num_threads", 2);
		greedy = configuration.getBoolean(base + ".greedy", true);
		int numTweaks = configuration.getInt(base + ".num_children");
		for (int i = 0; i < numTweaks; i++) {
			tweaks.add((TweakOperator) configuration.getInstanceAndSetup(base + ".child." + i));
		}
	}

	@Override
	public boolean isGreedy() {
		return greedy;
	}
}
