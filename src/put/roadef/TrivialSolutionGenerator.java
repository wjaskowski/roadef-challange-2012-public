package put.roadef;

public class TrivialSolutionGenerator implements RandomSolutionGenerator {

	@Override
	public SmartSolution getRandomSolution(Problem problem) {
		return new SmartSolution(problem.getOriginalSolution());
	}

}
