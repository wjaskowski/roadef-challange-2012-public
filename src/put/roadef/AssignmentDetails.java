package put.roadef;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntSet;

public interface AssignmentDetails {
	
	Int2IntMap getNeighborhoodsForService(int service);
	
	IntSet getMachinesForService(int service);
	
	IntSet getProcessesInMachine(int machine);
}
