package put.roadef.selectors;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

import put.roadef.Solution;

public interface SwapProcessSelector {
	
	List<IntList> getProcessesToSwap(Solution solution);

	void reset();
}
