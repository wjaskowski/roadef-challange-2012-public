package put.roadef.tweaks;

import java.util.Random;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.TweakOperator;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.hh.Heuristic;
import put.roadef.ip.MipFastModel;

/**
 * Tries to create a new solution basing on the current one (probably a better one, but not necessarily). 
 * Gets a number of random processes and runs an IP solver on them.   
 */
public class IPRandomImprover extends Solver implements TweakOperator, Heuristic {
        
        private MipFastModel model = new MipFastModel("CplexFastMipSolver");
		private Logger logger = Logger.getLogger(IPRandomImprover.class);
		private long maxTimeMillis;
		private int maxTimeMillisForIPSolver;
		private int numProcessesToModify;
        
        public IPRandomImprover() {     }

        @Override
        public Solution tweak(Solution solution, Deadline deadline) {
                Problem problem = solution.getProblem();
                Random random = problem.getRandom();
                
                if (maxTimeMillis > 0)
        			deadline = Deadline.min(new Deadline(maxTimeMillis), deadline);
                
                SmartSolution ss = SmartSolution.promoteToSmartSolution(solution);
                                                
                while (!deadline.hasExpired()) {
                        //System.out.println(numProcessesToModify);                        
                        int processes[] = new int[numProcessesToModify];
                        for (int i=0; i<Math.min(problem.getNumProcesses(), numProcessesToModify); ++i)
                                processes[i] = random.nextInt(problem.getNumProcesses());
                        
            			Deadline tempDeadline = deadline.getTrimmedTo(maxTimeMillisForIPSolver);
                        long oldCost = ss.getCost();
                        double oldImp = ss.getImprovement();
                        ss = (SmartSolution)model.modifyAssignmentsForProcesses(problem, ss, processes, tempDeadline, true, true);
                        
                        // Found a better solution?
                        if (ss.getCost() < oldCost) {
            				String msg = String.format("Was: %d(%.2f%%) -> by IP: %d(%.2f%%) IMP", oldCost, oldImp, ss.getCost(), ss.getImprovement());
            				logger.info(msg);
                        }
                        
                        //numProcessesToModify += 1;
                        //timeToSpend += 200;
                }
                return ss;
        }

        @Override
        public boolean isDeterministic() {
                return false;
        }

        @Override
        public boolean isGreedy() {
                return false;
        }

		@Override
		public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
			return tweak(new SmartSolution(initialSolution), deadline);
		}
		
		@Override
		public void setup(RoadefConfiguration configuration, String base) {
			maxTimeMillis = (long) configuration.getInt(base + ".max_time_millis", -1);
			//maxNumTriesWithoutImprovement = configuration.getInt(base + ".max_num_tries_without_improvement", 1000);
			maxTimeMillisForIPSolver = configuration.getInt(base + ".max_time_millis_for_ipsolve", 20000);
			numProcessesToModify = configuration.getInt(base + ".num_pprocesses_to_modify", 100);
		}

		@Override
		public Solution move(Solution solution, Deadline deadline, Random random) {
			return tweak(new SmartSolution(solution), deadline);
		}
}