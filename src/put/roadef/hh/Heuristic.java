package put.roadef.hh;

import java.util.Random;

import put.roadef.Deadline;
import put.roadef.Solution;

public interface Heuristic {
	public Solution move(Solution solution, Deadline deadline, Random random);

	public boolean isDeterministic();
}
