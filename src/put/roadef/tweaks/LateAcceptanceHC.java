package put.roadef.tweaks;

import java.util.Arrays;
import java.util.Random;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;
import put.roadef.neighborhoods.Neighborhood;
import put.roadef.neighborhoods.Neighborhood.Decision;
import put.roadef.neighborhoods.Neighborhood.NeighborProcessor;
import put.roadef.neighborhoods.RandomizedNeighborhood;

// LAHC+ version of LAHC
public class LateAcceptanceHC extends Solver implements RandomizedTweakOperator, Setup {
//	private boolean greedy;

	private Neighborhood<Solution> neighborhood;
	
	private int length;
	private long[] costs;

	public LateAcceptanceHC() {
	}

	public LateAcceptanceHC(Neighborhood<Solution> neighborhood, boolean greedy) {
		this.neighborhood = neighborhood;
//		this.greedy = greedy;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setup(RoadefConfiguration configuration, String base) {
//		greedy = configuration.getBoolean(base + ".greedy");
		neighborhood = (Neighborhood<Solution>) configuration.getInstanceAndSetup(base
				+ ".neighborhood");
		
		length = configuration.getInt(base + ".length");
		costs = new long[length];
	}

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		Solution s = initialSolution;
		if (!(initialSolution instanceof SmartSolution))
			s = new SmartSolution(s);
		return tweak(s, deadline, problem.getRandom());
	}

	//boolean bestSolutionImproved;
	
	//Solution greedyBestSolution;
	//ImmutableSolution greedyBestSolutionLight;
	//long bestSolutionCost;
	
	private Logger logger = Logger.getLogger(LateAcceptanceHC.class);

	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		return tweak(solution, deadline, solution.getProblem().getRandom());
	}
	
	@Override
	public Solution tweak(Solution solution, Deadline deadline, Random random) {
		neighborhood.init(solution.getProblem());
		return /*greedy ?*/ tweakGreedy(solution, deadline, random)/* : tweakSteepest(solution, deadline, random)*/;
	}

	/*
	private Solution tweakSteepest(Solution solution, final Deadline deadline, Random random) {
		lastSolution = bestSolution = steepestSolution = solution;
		costV = lastSolutionCost = bestSolutionCost = steepestSolutionCost = solution.getCost();
		
		Arrays.fill(costs, lastSolutionCost);
		
		int vIdx = 0; 
		

		NeighborProcessor processor = new NeighborProcessor() {
			@Override
			public Decision processNeighbor(ImmutableSolution s) {		
				long newCost = s.getCost();
				if (newCost < steepestSolutionCost)
				{
					steepestSolution = s.lightClone();
					steepestSolutionCost = newCost;
				}
				if (deadline.hasExpired())
					return Decision.Stop;
				// In steepest version we always reject in order to wait for the best
				return Decision.Reject;
			}
		};
		
		while (!deadline.hasExpired()) {		
			if (neighborhood instanceof RandomizedNeighborhood) {
				((RandomizedNeighborhood)neighborhood).visit(solution, deadline, processor, random);
			} else {
				neighborhood.visit(solution, deadline, processor);
			}

			if (steepestSolutionCost < costV || steepestSolutionCost < lastSolutionCost)
			{
				lastSolutionCost = steepestSolutionCost;
				lastSolution = steepestSolution;
				
				if (steepestSolutionCost < bestSolutionCost)
				{
					bestSolution = lastSolution;
					bestSolutionCost = steepestSolutionCost;
				}
			}
			else
				break;

			if (costs[vIdx] > lastSolutionCost)
				costs[vIdx] = lastSolutionCost;

			vIdx++;
			if (vIdx == costs.length)
				vIdx = 0;
			
			costV = costs[vIdx];								
			
			solution = new SmartSolution(lastSolution);
		}

		return new SmartSolution(bestSolution);
	}
	*/
	
	private boolean moved;
	private long costV;
	
	private Solution lastSolutionSmart;
	private ImmutableSolution lastSolution;
	private long lastSolutionCost;

//	private ImmutableSolution steepestSolution;
//	private long steepestSolutionCost;
	
	private ImmutableSolution bestSolution;
	private long bestSolutionCost;

	//private int iteration = 0;
	private int vIdx; 

	
	public Solution tweakGreedy(Solution solution, final Deadline deadline, Random random) {
		int numNeighborhoodVisits = 0;
		
		lastSolution = bestSolution = lastSolutionSmart = solution;
		costV = lastSolutionCost = bestSolutionCost = solution.getCost();
		vIdx = 0;
		
		Arrays.fill(costs, lastSolutionCost);
		
//		iteration = 0;
		
		NeighborProcessor processor = new NeighborProcessor() {
			@Override
			public Decision processNeighbor(ImmutableSolution neighbor) {
				boolean accept = false;
				long newCost = neighbor.getCost();
				
				if (newCost < costV || newCost < lastSolutionCost)
				{
					lastSolutionCost = newCost;
					lastSolution = neighbor;
					
					if (newCost < bestSolutionCost)
					{
						bestSolution = neighbor.lightClone();
						bestSolutionCost = newCost;
					}
					
					moved = accept = true;
					
					if (costs[vIdx] > lastSolutionCost)
						costs[vIdx] = lastSolutionCost;
					//iteration++;
					
					//vIdx = (int)(iteration % costs.length);
					vIdx++;
					if (vIdx == costs.length)
						vIdx = 0;
					
					costV = costs[vIdx];					
				}
				if (deadline.hasExpired())
					return Decision.Stop;
				// In greedy version we accept to work on the new solution if it has improved
				return accept ? Decision.Accept : Decision.Reject;
			}
		};
		
		while (!deadline.hasExpired()) {
			moved = false;
			
			//vIdx = iteration % costs.length;
			//costV = costs[vIdx];
			
			if (!(lastSolutionSmart instanceof SmartSolution))
				lastSolutionSmart = new SmartSolution(lastSolution);
			else
				lastSolutionSmart = (SmartSolution) lastSolution;			
			
			if (neighborhood instanceof RandomizedNeighborhood) {
				((RandomizedNeighborhood)neighborhood).visit(lastSolutionSmart, deadline, processor, random);
			} else {
				neighborhood.visit(lastSolutionSmart, deadline, processor);
				numNeighborhoodVisits += 1;
			}				
			// solution is always the best solution found so far
			
			if (!moved)
				break;
		}
		logger.info("Neighborhood visited " + numNeighborhoodVisits + " times");
		return new SmartSolution(bestSolution);
	}

	@Override
	public boolean isDeterministic() {
		return true;
	}

	@Override
	public boolean isGreedy() {
		return true;//greedy;
	}
}
