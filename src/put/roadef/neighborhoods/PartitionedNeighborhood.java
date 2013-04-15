package put.roadef.neighborhoods;

import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

public class PartitionedNeighborhood implements Neighborhood<SmartSolution>, Setup {

	private Neighborhood<SmartSolution> innerNeighborhood;
	private int partitionSize;

	public PartitionedNeighborhood(Neighborhood<SmartSolution> neighborhood) {
		innerNeighborhood = neighborhood;
	}

	public PartitionedNeighborhood() {
	}
	
	@Override
	public void init(Problem problem) {
		// TODO Auto-generated method stub
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		partitionSize = configuration.getInt(base + ".size");
		innerNeighborhood = (Neighborhood<SmartSolution>) configuration.getInstanceAndSetup(base
				+ ".inner");

		if (innerNeighborhood.runsOnTheSpot() == false) {
			// nie wiem co tu powinno byc - w kazdym razie ups
			throw new InternalError(
					"PartitionedNeighborhood accepts only a neigbourhood which runs on the spot");

			// If processNeighbor returns Decision.Accept we replace current solution in the inner neighborhood 
		}
	}

	private ImmutableSolution bestSolution;
	private long bestCost;
	private int count;
	private boolean stop;

	@Override
	public void visit(SmartSolution solution, Deadline deadline, final NeighborProcessor processor) {

		stop = false;

		count = 0;
		bestSolution = null;
		bestCost = Long.MAX_VALUE;

		innerNeighborhood.visit(solution, deadline, new NeighborProcessor() {
			@Override
			public Decision processNeighbor(ImmutableSolution s) {
				if (s.getCost() < bestCost) {
					bestCost = s.getCost();
					bestSolution = s.lightClone();
				}

				count++;
				if (count >= partitionSize) {
					SmartSolution sol = new SmartSolution(bestSolution);
					Decision decision = processor.processNeighbor(sol);
					if (decision == Decision.Accept) {
						((SmartSolution) s).assign(sol);
					} else if (decision == Decision.Stop) {
						stop = true;
					}

					count = 0;
					bestSolution = null;
					bestCost = Long.MAX_VALUE;

					return decision;
				}

				// In steepest version we always reject in order to wait for the best
				return Decision.Reject;
			}
		});

		if (!stop && bestSolution != null) {
			SmartSolution sol = new SmartSolution(bestSolution);
			// ignore decision - we stop anyway
			processor.processNeighbor(sol);
		}
	}

	@Override
	public boolean runsOnTheSpot() {
		return true;
	}

	@Override
	public boolean isDeterministic() {
		return innerNeighborhood.isDeterministic();
	}
}
