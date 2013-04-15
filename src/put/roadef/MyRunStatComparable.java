package put.roadef;

import java.util.Comparator;

public class MyRunStatComparable implements Comparator<RunStat> {

	@Override
	public int compare(RunStat o1, RunStat o2) {
		if (o1.cost < o2.cost) return 1;
		if (o1.cost > o2.cost) return -1;
		return 0;
	}
}