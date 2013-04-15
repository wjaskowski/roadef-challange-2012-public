package put.roadef.selectors;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

import put.roadef.Solution;


public interface GroupProcessSelector {

	List<IntList> getProcessesGroups(Solution solution);
	
	boolean isDeterministic();
	
	void reset();
}
