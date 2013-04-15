package put.roadef;

import java.util.Comparator;
import java.util.HashSet;
import java.util.TreeSet;

public class MachinePairQueue2 {
	public final int[] visitedCount;

	private TreeSet<MachinePairPotential> queue;
	private HashSet<MachinePairPotential> visited = new HashSet<MachinePairPotential>();

	private double VISITED_COUNT_WEIGHT = 1.0;

	private int maxSize;
	
	private HashSet<MachinePair> tmp;
	private HashSet<MachinePair> inQueue = new HashSet<MachinePair>();
	
	public MachinePairQueue2(int numMachines, int maxSize) {
		this.maxSize = maxSize;
		
		tmp = new HashSet<MachinePair>(numMachines * 3);
		visitedCount = new int[numMachines];

		queue = new TreeSet<MachinePairPotential>(new Comparator<MachinePairPotential>() {
			@Override
			public int compare(MachinePairPotential o1, MachinePairPotential o2) {
				
				int val = -sign(getPriority(o1.m1, o1.m2, o1.potential) - getPriority(o2.m1, o2.m2, o2.potential));
				if (val != 0)
					return val;
				return (o1.m1 * 5000 + o1.m2) - (o2.m1 * 5000 + o2.m2);
			}
		});
	}

	public long getPriority(int m1, int m2, long potential) {
		return potential / (int)((1 + VISITED_COUNT_WEIGHT * (visitedCount[m1] + visitedCount[m2])));
	}

	public void addPair(int m1, int m2, long p) {
		if (size() < maxSize) {
			MachinePairPotential mpp = new MachinePairPotential(m1, m2, p);
			queue.add(mpp);
			inQueue.add(mpp);
		}
		else {
			MachinePairPotential last = queue.last();			
			if (getPriority(m1, m2, p) < getPriority(last.m1, last.m2, last.potential))
				return;
			
			MachinePairPotential mpp = new MachinePairPotential(m1, m2, p);
			queue.add(mpp);
			queue.pollLast();
			
			inQueue.add(mpp);
			inQueue.remove(last);
		}
	}
	
	public MachinePair getNext() {
		MachinePairPotential pair = queue.pollFirst();
		if (pair != null) {
			inQueue.remove(pair);
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
		if (inQueue.remove(p))		
			tmp.add(p);
	}

	public void finishChangePair(int m1, int m2, long potential) {			
		MachinePair p = new MachinePair(m1, m2);
		if (tmp.remove(p)) {
			addPair(m1, m2, potential);
		}
	}	
}
