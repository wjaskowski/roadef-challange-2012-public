package put.roadef.solvers;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.PerformanceStats;
import put.roadef.Problem;
import put.roadef.Safety;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

public class SequentialSolver extends Solver implements Setup {
	private ArrayList<Solver> solvers = new ArrayList<Solver>();
	private Logger logger = Logger.getLogger(SequentialSolver.class); 
	
	public SequentialSolver() {}
	
	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		Solution sol = SmartSolution.promoteToSmartSolution(initialSolution.clone());
		
		int id = 0;
		PerformanceStats stats = new PerformanceStats();
		for (Solver solver : solvers) {
			stats.start(sol, solver.getClass().getSimpleName() + " " + id);
			sol = solver.solve(problem, sol, deadline);
			stats.stop(sol);
			Safety.saveSolution(sol);
			
			if (deadline.hasExpired())
				break;
			id++;
		}
		logger.info("Final stats:");
		for (Map.Entry<String, PerformanceStats.Perf> p : stats.performances.entrySet())
			logger.info(p.getValue().toString());
		return sol;
	}
	
	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		int numSolvers = configuration.getInt(base + ".num_solvers");		
		for (int i = 0; i < numSolvers; ++i)
			solvers.add((Solver)configuration.getInstanceAndSetup(base + ".solver." + i));
	}
}
