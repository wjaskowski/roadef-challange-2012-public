package put.roadef.solvers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

public class ParallelSolver extends Solver implements Setup {

	private static final int NUM_THREADS = 2;
	private int numThreads;

	private Solver[] solvers = new Solver[NUM_THREADS];
	private ExecutorService threadPool;

	private Logger logger = Logger.getLogger(ParallelSolver.class);
	private long synchronizationTimeMillis;
	
	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		threadPool = Executors.newFixedThreadPool(numThreads);
		
		SmartSolution ss1 = SmartSolution.promoteToSmartSolution(initialSolution);
		SmartSolution ss2 = new SmartSolution(ss1);
		List<SolverTask> tasks = new ArrayList<SolverTask>();
		
		while (!deadline.hasExpired()) {
			tasks.clear();
			tasks.add(new SolverTask(solvers[0], ss1, Deadline.min(new Deadline(synchronizationTimeMillis), deadline)));
			tasks.add(new SolverTask(solvers[1], ss2, Deadline.min(new Deadline(synchronizationTimeMillis), deadline)));
			
			logger.info("STARTING PARALLEL SOLVERS");
			try {
				List<Future<Solution>> solutions = threadPool.invokeAll(tasks);
				ss1 = SmartSolution.promoteToSmartSolution(solutions.get(0).get());
				ss2 = SmartSolution.promoteToSmartSolution(solutions.get(1).get());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			logger.info("PARALLEL SOLVERS COMPLETED");
			logger.info("Solution 1 cost = " + ss1.getCost());
			logger.info("Solution 2 cost = " + ss2.getCost());
			
			if (ss1.getCost() < ss2.getCost()) {
				ss2 = new SmartSolution(ss1);
			} else if (ss2.getCost() < ss1.getCost()) {
				ss1 = new SmartSolution(ss2);
			}
		}		
		
		threadPool.shutdown();
		
		return ss1;
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		synchronizationTimeMillis = configuration.getInt(base + ".sync_time", 50000);
		numThreads = configuration.getInt(base + ".num_solvers", 2);
		for (int s = 0; s < NUM_THREADS; s++)
			solvers[s] = (Solver) configuration.getInstanceAndSetup(base + ".solver." + s);
	}
	
	
	public static class SolverTask implements Callable<Solution> {

		private SmartSolution solution;
		private Solver solver;
		private Deadline deadline;

		public SolverTask(Solver solver, SmartSolution solution, Deadline deadline) {
			this.solver = solver;
			this.solution = solution;
			this.deadline = deadline;
		}

		@Override
		public Solution call() throws Exception {
			return solver.solve(solution.getProblem(), solution, deadline);
		}
		
	}
}
