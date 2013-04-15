package put.roadef;

import java.util.Comparator;
import java.util.HashSet;
import java.util.TreeSet;

public class MachinePairQueue {
	private final long[][] optimisticCosts;
	public final int[] visitedCount;

	//TODO: To mona zamieni na fastutil IntPriorityQueue i kodowa jako m1*M+m2
	private TreeSet<MachinePair> queue;
	private HashSet<MachinePair> visited = new HashSet<MachinePair>();

	private int maxSize;
	private HashSet<MachinePair> tmp;
	private double visitedCountWeight;

	public MachinePairQueue(int numMachines, int maxSize, double visitedCountWeight) {
		this.visitedCountWeight = visitedCountWeight;
		this.maxSize = maxSize;
		optimisticCosts = new long[numMachines][];
		for (int i = 0; i < numMachines; ++i) {
			optimisticCosts[i] = new long[i];
		}
		tmp = new HashSet<MachinePair>(numMachines * 3);

		visitedCount = new int[numMachines];
		for (int i = 0; i < numMachines; ++i)
			visitedCount[i] = 0;		

		queue = new TreeSet<MachinePair>(new Comparator<MachinePair>() {
			@Override
			public int compare(MachinePair o1, MachinePair o2) {
				int val = -sign(getPriority(o1.m1, o1.m2) - getPriority(o2.m1, o2.m2));
				if (val != 0)
					return val;
				return (o1.m1 * 5000 + o1.m2) - (o2.m1 * 5000 + o2.m2);
			}
		});
	}

	public long getPriority(int m1, int m2) {
		if (m1 < m2) {
			int tmp = m1;
			m1 = m2;
			m2 = tmp;
		}
		return optimisticCosts[m1][m2] / (int)((1 + visitedCountWeight * (visitedCount[m1] + visitedCount[m2])));
	}

	public void addPair(int m1, int m2) {
		if (m1 < m2) {
			int tmp = m1;
			m1 = m2;
			m2 = tmp;
		}							
		
		if (size() < maxSize)
			queue.add(new MachinePair(m1, m2));
		else {
			//quick return if possible
			if (queue.size() > 0) {			
				MachinePair last = queue.last();			
				if (getPriority(m1, m2) < getPriority(last.m1, last.m2))
					return;
			}
			queue.add(new MachinePair(m1, m2));
			queue.pollLast();
		}
	}
	
	public void reconsiderPair(int m1, int m2) {	
		if (m1 < m2) {
			int tmp = m1;
			m1 = m2;
			m2 = tmp;
		}

		MachinePair pair = new MachinePair(m1, m2);		
		if (visited.contains(pair)) {
			visited.remove(pair);
			queue.add(pair);			
		}
	}

	public long getOptimisticCost(int m1, int m2) {
		if (m1 < m2) {
			int tmp = m1;
			m1 = m2;
			m2 = tmp;
		}
		return optimisticCosts[m1][m2];
	}
	
	public void setOptimisticCost(int m1, int m2, long cost) {
		if (m1 < m2) {
			int tmp = m1;
			m1 = m2;
			m2 = tmp;
		}
		
		optimisticCosts[m1][m2] = cost;
	}

	public MachinePair getNext() {
		MachinePair pair = queue.pollFirst();
		if (pair != null) {
			visited.add(pair);
		}
		return pair;
	}

	public int size() {
		return queue.size() + visited.size();
	}

	private int sign(long v) {
		if (v > 0)
			return 1;
		else if (v < 0)
			return -1;
		return 0;
	}

	public void prepareChangePair(int m1, int m2) {
		MachinePair p = new MachinePair(m1, m2);
		if (queue.remove(p))		
			tmp.add(p);
	}

	public void finishChangePair(int m1, int m2) {			
		MachinePair p = new MachinePair(m1, m2);
		if (tmp.remove(p)) {
			addPair(m1, m2);
		}
	}	
}
