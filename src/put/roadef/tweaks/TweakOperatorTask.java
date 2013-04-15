package put.roadef.tweaks;

import java.util.Random;
import java.util.concurrent.Callable;

import put.roadef.Deadline;
import put.roadef.Solution;
import put.roadef.TweakOperator;

public class TweakOperatorTask implements Callable<Solution> {

	private TweakOperator operator;
	private Solution initialSolution;
	private Deadline deadline;
	private Random random;

	public TweakOperatorTask(TweakOperator operator, Solution initialSolution, Deadline deadline) {
		this.operator = operator;
		this.initialSolution = initialSolution;
		this.deadline = deadline;
	}

	public TweakOperatorTask(RandomizedTweakOperator operator, Solution initialSolution,
			Deadline deadline, Random random) {
		this(operator, initialSolution, deadline);
		this.random = random;
	}

	@Override
	public Solution call() throws Exception {
		if (random != null) {
			return ((RandomizedTweakOperator) operator).tweak(initialSolution, deadline, random);
		} else {
			return operator.tweak(initialSolution, deadline);
		}
	}

}
