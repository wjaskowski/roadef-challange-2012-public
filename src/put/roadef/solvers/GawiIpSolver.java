package put.roadef.solvers;

import java.util.LinkedList;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.TweakOperator;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;
import put.roadef.tweaks.MoveServicesNeighborhood;

public class GawiIpSolver extends Solver implements Setup {

	private MoveServicesNeighborhood neighborhood;
	private TweakOperator optimizer;

	public GawiIpSolver() {
		this.neighborhood = new MoveServicesNeighborhood();
		this.optimizer = null;
	}
	
	@Override
	public void setup(RoadefConfiguration configuration, String base) {		
		Object o = configuration.getInstanceAndSetup(base + ".optimize");
		optimizer = (o != null ? (TweakOperator)o : null);
	}			

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		neighborhood.SINGLE_STEP_TIME=3;
		double numProc = 100;
		double sumProc = 0;
		int numTimeouts = 0;
		LinkedList<int[]> availableTasks = new LinkedList<int[]>();
		LinkedList<Integer> freeTasks = new LinkedList<Integer>();

		//temporary result
		Solution sol = initialSolution.clone();

		//information about breaking the computation because of time
		boolean timeout = false;
		for (int i = 0; i < problem.getNumServices(); i++) {
			availableTasks.add(new int[] { i });
		}

		@SuppressWarnings("unused")
		long score = 0;
		do {

			int[] task = availableTasks.pop();

			neighborhood.services = task;

			sol = neighborhood.tweak(sol, deadline);
			if (optimizer != null)
				sol = optimizer.tweak(new SmartSolution(sol), deadline);

			int numP = 0;
			for (int i = 0; i < task.length; i++) {
				numP += problem.getService(task[i]).processes.length;
			}
			if (neighborhood.solverStatus == 0 && numP <= numProc) { //extend current task
				int newTask[] = new int[task.length + 1];
				for (int i = 0; i < task.length; i++)
					newTask[i] = task[i];
				if (freeTasks.size() > 0) { //we have free services to extend group
					int newId = freeTasks.pop();
					newTask[task.length] = newId;
					availableTasks.add(newTask);
				} else { //we have to split good group to find a service to extend
					if (availableTasks.size() > 0) {
						int[] nextTask = availableTasks.pop(); //get next task
						newTask[task.length] = nextTask[0]; //extend current task
						availableTasks.add(newTask); //add to the queue

						if (nextTask.length > 1) { //if the splited task size was greater than 1 we 
													//add new small task 
							newTask = new int[1];
							newTask[0] = nextTask[1];
							availableTasks.add(newTask);
						}
						for (int i = 2; i < nextTask.length; i++)
							//the rest is to be used by other tasks
							freeTasks.add(nextTask[i]);

					} else
						//we solved the whole problem at once :)
						return sol;

				}
			} else {
				if (neighborhood.solverStatus > 0) {
					numP = 0;
					for (int i = 0; i < task.length - 1; i++) {
						numP += problem.getService(task[i]).processes.length;
					}
					sumProc += numP;
					numTimeouts++;
					numProc = sumProc / numTimeouts;
				}
				int[] newTask = new int[1];
				newTask[0] = task[0];
				availableTasks.add(newTask);
				if (task.length > 1) { //if not we have a problem - we cannot compute single task in feasible time...
					newTask = new int[1];
					newTask[0] = task[1];
					availableTasks.add(newTask);
				}
				for (int i = 2; i < task.length; i++) {
					freeTasks.add(task[i]);
				}
			}
			score = sol.getCost();
//			System.out.println(score + ", " + numProc + " (" + numTimeouts + ")");

			//and now compute the genereal time of computation and check timeconstraints
			if (deadline.hasExpired()) {
				timeout = true;
			}
		} while (!timeout);
		return sol;
	}

}
