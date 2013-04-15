package put.roadef.tweaks;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Solution;
import put.roadef.TweakOperator;
import put.roadef.ip.MipFastModel;

public class MoveServiceNeighborhood implements TweakOperator {
	int nextService = 0;
	int lastImprovement = -1;
	long lastScore = -1;
	private boolean canImprove = true;

	MipFastModel model;

	public MoveServiceNeighborhood() {
		model = new MipFastModel("CplexFastMipSolver");
	}

	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		Problem problem = solution.getProblem();
		if (lastImprovement < 0)
			lastImprovement = problem.getNumServices() - 1;
		if (lastScore < 0)
			lastScore = problem.evaluateSolution(solution);

		int SINGLE_STEP_TIME = 10;

		if (nextService >= problem.getNumServices())
			nextService = 0;

		Solution bestSolution = solution.clone();
		while (deadline.getShortenedBy(1000 + SINGLE_STEP_TIME * 1000).hasExpired() && SINGLE_STEP_TIME > 0) {
			SINGLE_STEP_TIME--;
		}
		if (SINGLE_STEP_TIME <= 0)
			return bestSolution;
		bestSolution = model.modifyAssignmentsForServices(problem, bestSolution, new int[] { nextService }, new Deadline(
				SINGLE_STEP_TIME * 1000), true);
		//TODO mozliwe ze powinnismy takze balansowac limitami czasowymi, w przypadku cplexa nie jest to az tak wazne, ale 
		//w sytuacji lpsolva jest to BARDZO istotne
		//model.solutionStatus zawiera informacje o tym jak zakonczylo sie rozwiazywanie przez solver:
		//0 - optimum
		//1 - rozwiazanie nieoptymalne, ale pasujace(brak czasu)
		//2 - rozwiazanie nie znalezione (brak czasu)
		long newScore = problem.evaluateSolution(bestSolution);
		if (newScore < lastScore) {
			lastImprovement = nextService;
		} else if (lastImprovement == nextService) {
			canImprove = false;
		}
		lastScore = newScore;
		nextService = (nextService + 1) % problem.getNumServices();
		return bestSolution;
	}

	@Override
	public boolean isDeterministic() {
		return true;
	}

	public boolean canImprove() {
		return canImprove;
	}

	@Override
	public boolean isGreedy() {
		return false;
	}
}
