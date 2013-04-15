package put.roadef.tweaks;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Solution;
import put.roadef.TweakOperator;
import put.roadef.ip.MipFastModel;

public class MoveServicesNeighborhood implements TweakOperator {

        MipFastModel model;
        
        public int[] services;
        
        public int solverStatus=-1; 

        public int SINGLE_STEP_TIME = 10;
        
        public MoveServicesNeighborhood() {
                model = new MipFastModel("CplexFastMipSolver");
        }

        @Override
        public Solution tweak(Solution solution, Deadline deadline) {
                if (services==null)
                        return solution;
                Problem problem = solution.getProblem();

                Solution bestSolution = solution.clone();
                while (deadline.getShortenedBy(2000 + SINGLE_STEP_TIME * 1000).hasExpired() && SINGLE_STEP_TIME > 0) {
                        SINGLE_STEP_TIME--;
                }
                if (SINGLE_STEP_TIME <= 0) {
                        SINGLE_STEP_TIME = 10;
                        return bestSolution;
                }
                bestSolution = model.modifyAssignmentsForServices(problem, bestSolution,
                                services, new Deadline(SINGLE_STEP_TIME*1000),true);
                //TODO mozliwe ze powinnismy takze balansowac limitami czasowymi, w przypadku cplexa nie jest to az tak wazne, ale 
                //w sytuacji lpsolva jest to BARDZO istotne
                //model.solutionStatus zawiera informacje o tym jak zakonczylo sie rozwiazywanie przez solver:
                //0 - optimum
                //1 - rozwiazanie nieoptymalne, ale pasujace(brak czasu)
                //2 - rozwiazanie nie znalezione (brak czasu)
                
                solverStatus=model.solutionStatus;
                
                return bestSolution;
        }

        @Override
        public boolean isDeterministic() {
                return true;
        }

        @Override
        public boolean isGreedy() {
                return false;
        }
}