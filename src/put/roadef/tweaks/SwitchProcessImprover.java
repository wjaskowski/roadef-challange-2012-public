package put.roadef.tweaks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.Problem.Process;
import put.roadef.Solution;
import put.roadef.TweakOperator;
import put.roadef.conf.RoadefConfiguration;
import put.roadef.conf.Setup;
import put.roadef.hh.Heuristic;

public abstract class SwitchProcessImprover implements TweakOperator, Setup, Heuristic {

	private boolean greedy;

	public SwitchProcessImprover() { }
	
	public SwitchProcessImprover(boolean greedy) {
		this.greedy = greedy;
	}
	
	@Override
	public void setup(RoadefConfiguration configuration, String base) {
		greedy = configuration.getBoolean(base + ".greedy", true);		
	}
	
	@Override
	public Solution move(Solution solution, Deadline deadline, Random random) {
		return tweak(solution, deadline);
	}
	
	@Override
	public Solution tweak(Solution solution, Deadline deadline) {
		List<Problem.Process> criticalProcesses = new ArrayList<Problem.Process>();
		List<Problem.Process> otherProcesses = new ArrayList<Problem.Process>();

		Comparator<Process> comparator = findCriticalProcesses(solution, criticalProcesses,
				otherProcesses);

		Solution bestSolution = solution.clone();
		if (comparator == null)
			return bestSolution;
		
		int[] assignment = solution.getAssignment();
		long bestFitness = solution.getCost();

		// Trying to switch processes between machines to balance
		// requirements of the overloaded resource
		for (Process process : criticalProcesses) {
			for (Process otherProcess : otherProcesses) {
				if (deadline.hasExpired()) {
					return bestSolution;
				}

				if (comparator.compare(process, otherProcess) <= 0) {
					break;
				}

				int processMachine = assignment[process.id];
				int otherProcessMachine = assignment[otherProcess.id];

				solution.moveProcess(process.id, otherProcessMachine);
				solution.moveProcess(otherProcess.id, processMachine);
				if (solution.isFeasible()) {
					long newFitness = solution.getCost();
					if (newFitness < bestFitness) {
						bestFitness = newFitness;
						bestSolution = solution.clone();
						if (greedy) {
							return bestSolution;
						}
					}
				}
				solution.moveProcess(process.id, processMachine);
				solution.moveProcess(otherProcess.id, otherProcessMachine);
			}
		}

		return bestSolution;
	}

	public abstract Comparator<Process> findCriticalProcesses(Solution solution,
			List<Process> criticalProcesses, List<Process> otherProcesses);

	@Override
	public boolean isDeterministic() {
		return true;
	}
	
	@Override
	public boolean isGreedy() {
		return greedy;
	}
}
