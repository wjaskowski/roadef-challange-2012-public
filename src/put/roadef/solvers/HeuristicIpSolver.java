package put.roadef.solvers;

import java.util.ArrayList;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.tweaks.MoveServicesNeighborhood;

public class HeuristicIpSolver extends Solver {

        private MoveServicesNeighborhood neighborhood = new MoveServicesNeighborhood();
        
        @Override
        public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
                neighborhood.SINGLE_STEP_TIME=10;
                int MAX_PROCESSES = 100;

                //temporary result
                Solution sol = initialSolution.clone();

                //information about breaking the computation because of time
                boolean timeout = false;

                long score = 0;
                do {
                        score = problem.evaluateSolution(sol);

                        // create ip model for sets consisting from 1 to MAX_PROCESSES processes,
                        // if number of processes is set to 0 then create model for any number of processes but from only one service
                        for (int processes = 0; processes <= MAX_PROCESSES && !timeout; processes++) {
                                //create model starting from all services
                                for (int i = 0; i < problem.getNumServices() && !timeout; i++) {
                                        ArrayList<Integer> params = new ArrayList<Integer>();
                                        params.add(i);
                                        int z = problem.getService(i).processes.length;
                                        //add dependencies up to 'processes' number 
                                        for (Integer integer : problem.getService(i).dependencies) {
                                                if (z + problem.getService(integer).processes.length > processes)
                                                        continue;
                                                z += problem.getService(integer).processes.length;
                                                params.add(integer);
                                        }

                                        //if we didn't add anything then omit this iteration
                                        if (processes > 0 && params.size() == 1) {
                                                continue;
                                        }
                                        if (z<processes)
                                                continue;

                                        //create set of services as an array
                                        int[] s = new int[params.size()];
                                        for (int k = 0; k < s.length; k++)
                                                s[k] = params.get(k);

                                        neighborhood.services=s;
                                        //compute new solution
                                        sol = neighborhood.tweak(sol, deadline);
                                        //                              System.out.println(lpSolveStatus);


                                        //and now compute the genereal time of computation and check timeconstraints
                                        if (deadline.hasExpired()) {
                                                timeout = true;
                                        }
                                }
                        }
                } while (problem.evaluateSolution(sol) < score && !timeout);
                return sol;
        }

}