package put.roadef.hh;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import put.roadef.Deadline;
import put.roadef.Problem;
import put.roadef.SmartSolution;
import put.roadef.Solution;
import put.roadef.ip.MipFastModel;

public class ParallelHyperHeuristicSolver extends HyperHeuristicSolver {

	private List<Solution> solutions = new ArrayList<Solution>();

	private MipFastModel model = new MipFastModel("CplexMipSolver");

	@Override
	public Solution solve(Problem problem, Solution initialSolution, Deadline deadline) {
		Solution s = initialSolution;
		if (!(initialSolution instanceof SmartSolution)) {
			s = new SmartSolution(s);
		}

		Random random = problem.getRandom();

		for (int i = 0; i < heuristics.size(); i++) {
			HeuristicStats stats = new HeuristicStats(heuristics.get(i), i, useLastPerformanceOnly, saveZeroImprovement);
			heuristicStats.add(stats);
		}

		while (!deadline.hasExpired()) {
			solutions.clear();
			for (int i = 0; i < heuristics.size(); i++) {
				SmartSolution candidate = new SmartSolution(s);
				solutions.add(makeHeuristicMove(deadline, candidate, random, heuristicStats.get(i)));
			}

			s = combineSolutions(problem, solutions);
		}

		for (HeuristicStats stats : heuristicStats) {
			stats.logPerformance();
		}

		return s;
	}

	private Solution combineSolutions(Problem problem, List<Solution> solutions) {
		IntSet modifiedProcesses = new IntOpenHashSet();
		IntSet modifiedMachines = new IntOpenHashSet();

		for (int p = 0; p < problem.getNumProcesses(); p++) {
			IntSet machines = new IntOpenHashSet();
			for (Solution s : solutions) {
				machines.add(s.getMachine(p));
			}

			if (machines.size() > 1) {
				modifiedProcesses.add(p);
				modifiedMachines.addAll(machines);
			}
		}

		long bestCost = Long.MAX_VALUE;
		Solution bestSolution = null;
		for (Solution s : solutions) {
			if (s.getCost() < bestCost) {
				bestCost = s.getCost();
				bestSolution = s;
			}
		}

		Solution candidateSolution = model.modifyAssignments(problem, bestSolution, modifiedProcesses.toIntArray(),
				modifiedMachines.toIntArray(), new Deadline(50000), true);
		if (candidateSolution != null && candidateSolution.getCost() < bestSolution.getCost()) {
			return candidateSolution;
		} else {
			return bestSolution;
		}
	}
}
