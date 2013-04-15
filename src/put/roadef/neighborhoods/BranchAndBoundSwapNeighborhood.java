package put.roadef.neighborhoods;

import java.util.Random;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.MyArrayUtils;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.bnb.BranchAndBoundRecursiveSolver;
import put.roadef.bnb.PartialSolution;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;
import put.roadef.selectors.ProcessSelector;

public class BranchAndBoundSwapNeighborhood extends RandomizedNeighborhood implements Setup {

	private enum Order {
		DefaultOrder, RandomOrder, LoadCostOrder
	};
	
	private Order order;
	private int maxNumRetries;

	private ProcessSelector processSelector;

	private BranchAndBoundRecursiveSolver solver;

	private Logger logger = Logger.getLogger(BranchAndBoundSwapNeighborhood.class);

	public BranchAndBoundSwapNeighborhood() {
		solver = new BranchAndBoundRecursiveSolver();
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		if (configuration.getBoolean(base + ".random_order", false))
			order = Order.RandomOrder;
		String confOrder = configuration.getString(base + ".order", Order.DefaultOrder.toString());
		if (confOrder != Order.DefaultOrder.toString())
			order = Order.valueOf(confOrder);
		
		maxNumRetries = configuration.getInt(base + ".max_num_retries");
		processSelector = (ProcessSelector) (configuration.getInstanceAndSetup(base
				+ ".process_selector"));
	}

	@Override
	public boolean runsOnTheSpot() {
		return false;
	}

	@Override
	public boolean isDeterministic() {
		return order != Order.RandomOrder;
	}
	
	@Override
	public void init(Problem problem) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Solution solution, Deadline deadline, NeighborProcessor processor,
			Random random) {
		boolean improved = false;
		int retriesWithoutImprovement = 0;
		Problem problem = solution.getProblem();
		int[] assignment = solution.getAssignment();
		PartialSolution partialSol = new PartialSolution(problem, assignment);
		long bestFitness = partialSol.getCost();
		long visitedTerminals = 0;

		long startTime = System.currentTimeMillis();
		int[] processes = processSelector.selectProcessesToMove(problem, assignment, partialSol);
		if (order == Order.RandomOrder) {
			MyArrayUtils.shuffle(processes, random);
		} else if (order == Order.LoadCostOrder) {
			SmartSolution ss = (SmartSolution) solution;
			final long[] processPotencial = new long[problem.getNumProcesses()];
			for (int p = 0; p < problem.getNumProcesses(); ++p) {
				processPotencial[p] = ss.getLoadCost(p) + ss.getMoveCost(p);
			}

			MyArrayUtils.sort(processes, processPotencial);
			ArrayUtils.reverse(processes);
		}

		outerloop: for (int p1 : processes) {
			partialSol.unAssign(problem.getProcess(p1), problem.getMachine(assignment[p1]));
			int[] processesToMoveWith = processSelector.selectProcessesToMoveWith(problem, p1,
					assignment, partialSol);

			for (int p2 : processesToMoveWith) {
				partialSol.unAssign(problem.getProcess(p2), problem.getMachine(assignment[p2]));
				ImmutableSolution candidateSolution = solver.lightSolve(problem, partialSol,
						bestFitness, deadline);
				visitedTerminals += solver.getNumVisitedTerminals();

				if (candidateSolution != null) {
					improved = true;
					retriesWithoutImprovement = 0;
					bestFitness = candidateSolution.getCost();
				} else {
					candidateSolution = solution;
					++retriesWithoutImprovement;
				}

				Decision decision = processor.processNeighbor(candidateSolution);

				if (decision == Decision.Stop || (improved && retriesWithoutImprovement == maxNumRetries)) {
					break outerloop;
				} else if (decision == Decision.Accept) {
					assignment[p1] = candidateSolution.getMachine(p1);
					assignment[p2] = candidateSolution.getMachine(p2);
				}

				partialSol.assign(problem.getProcess(p2), problem.getMachine(assignment[p2]));
				
				if (deadline.hasExpired()) {
					break;
				}
			}
			partialSol.assign(problem.getProcess(p1), problem.getMachine(assignment[p1]));
			
			if (deadline.hasExpired()) {
				break;
			}
		}

		long endTime = System.currentTimeMillis();
		logger.info("Branch and Bound Neighborhood has visited " + visitedTerminals
				+ " terminals, in " + (endTime - startTime) + " ms");
	}
}
