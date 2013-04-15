package put.roadef.neighborhoods;

import java.util.ArrayList;
import java.util.Arrays;

import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.Problem;
import put.roadef.Solution;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

public class MultiNeighborhood implements Neighborhood<Solution>, Setup {

	private ArrayList<Neighborhood<Solution>> neighborhoods;

	public MultiNeighborhood(Neighborhood<Solution>... neighborhoods) {
		this.neighborhoods = new ArrayList<Neighborhood<Solution>>(Arrays.asList(neighborhoods));
	}
	
	public MultiNeighborhood() {
		neighborhoods = new ArrayList<Neighborhood<Solution>>();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		int numNeighborhoods = configuration.getInt(base + ".num_children");		
		for (int i = 0; i < numNeighborhoods; ++i)
			neighborhoods.add((Neighborhood<Solution>)configuration.getInstanceAndSetup(base + ".child." + i));
	}
	
	@Override
	public void init(Problem problem) {
		for (Neighborhood<Solution> n : neighborhoods) {
			n.init(problem);
		}
	}

	private boolean stop;
	@Override
	public void visit(Solution solution, Deadline deadline,
			final NeighborProcessor processor) {
		stop = false;
		for (Neighborhood<Solution> n : neighborhoods) {
			n.visit(solution, deadline, new NeighborProcessor() {
				@Override
				public Decision processNeighbor(ImmutableSolution s) {
					Decision decision = processor.processNeighbor(s);
					if (decision == Decision.Stop) {
						stop = true;
					}
					return decision;
				}
			});
			if (stop)
				return;
		}
	}

	@Override
	public boolean runsOnTheSpot() {
		//TODO: This is bad and might be a problem... how to resolve it? 
		for (Neighborhood<Solution> n : neighborhoods)
			if (n.runsOnTheSpot())
				return true;
		return false;
	}

	@Override
	public boolean isDeterministic() {
		for (Neighborhood<Solution> n : neighborhoods)
			if (!n.isDeterministic())
				return false;
		return true;
	}
}
