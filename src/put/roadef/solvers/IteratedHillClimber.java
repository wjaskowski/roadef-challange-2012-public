package put.roadef.solvers;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.Solver;
import put.roadef.TweakOperator;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;

public class IteratedHillClimber extends Solver implements TweakOperator, Setup {

	private TweakOperator perturb;
	private TweakOperator optimize;
	private int maxPerturbTimeMilliseconds;
	private Logger logger = Logger.getLogger(IteratedHillClimber.class);

	public IteratedHillClimber(TweakOperator perturbOperator, TweakOperator optimizeOperator,
			int maxPerturbTimeMilliseconds) {
		this.perturb = perturbOperator;
		this.optimize = optimizeOperator;
		this.maxPerturbTimeMilliseconds = maxPerturbTimeMilliseconds;
	}

	public IteratedHillClimber() {
	}

	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		perturb = (TweakOperator) configuration.getInstanceAndSetup(base + ".perturb");
		optimize = (TweakOperator) configuration.getInstanceAndSetup(base + ".optimize");
		maxPerturbTimeMilliseconds = configuration.getInt(base + ".max_perturb_time_milliseconds", -1);
	}

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		if (!(initialSolution instanceof SmartSolution))
			initialSolution = new SmartSolution(initialSolution);
		return tweak(initialSolution, deadline);
	}

	@Override
	public Solution tweak(Solution solution, Deadline deadline) {		
		logger.info("Starting IHC");
		solution = optimize.tweak((SmartSolution)solution, deadline);
		logger.info("Cost after optimize: " + solution.getCost());
		
		while (!deadline.hasExpired()) {
			//solution is the current best. Save it in case it gest worse
			ImmutableSolution saved = solution.lightClone();
			
			Deadline perturbDeadline = (maxPerturbTimeMilliseconds <= 0 ? deadline : 
				Deadline.min(new Deadline(maxPerturbTimeMilliseconds), deadline)); 
						
			logger.info("Cost before perturb: " + solution.getCost());
			solution = (SmartSolution)perturb.tweak(solution, perturbDeadline);
			logger.info("Cost before optimize: " + solution.getCost());
			solution = (SmartSolution)optimize.tweak(solution, deadline);
			logger.info("Cost after optimize: " + solution.getCost());

			// If solution has not improved (>=) in perturb+optimize
			if (!(solution.getCost() < saved.getCost())) {
				// If solution is worse than it was before optimize+perturb
				if (solution.getCost() > saved.getCost())
					solution = new SmartSolution(saved);					
				if (isDeterministic())
					break;
			} else {
				logger.info("Found better: " + solution.getCost() + "IMP");
			}
		}

		return solution;
	}

	@Override
	public boolean isDeterministic() {
		return (optimize.isDeterministic() && perturb.isDeterministic());
	}

	@Override
	public boolean isGreedy() {
		return false;
	}
}
