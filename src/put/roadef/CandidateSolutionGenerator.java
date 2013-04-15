package put.roadef;

import java.util.List;

/**
 * 
 * Generates candidate solutions to power population-based algorithms
 * 
 * @author marcin
 * 
 */
public interface CandidateSolutionGenerator {
	List<Solution> getCandidateSolutions(Solution initialSolution, int count, Deadline deadline);
}
