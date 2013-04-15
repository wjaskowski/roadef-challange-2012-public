package put.roadef.tweaks;

import java.util.Random;

import put.roadef.Deadline;
import put.roadef.Solution;
import put.roadef.TweakOperator;

public interface RandomizedTweakOperator extends TweakOperator {
	/**
	 * Tweaks the solution. It could be an improvement, but it may be also a perturbation.
	 * It can (and should!) change the solution given as the argument. It may return the same solution to the one given (the same but tweaked).
	 */
	Solution tweak(Solution solution, Deadline deadline, Random random);
}
