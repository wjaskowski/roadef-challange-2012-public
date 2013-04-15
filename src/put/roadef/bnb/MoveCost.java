package put.roadef.bnb;

import java.util.Collection;

import put.roadef.Problem;
import put.roadef.Problem.Machine;
import put.roadef.Problem.Process;

public class MoveCost implements IncrementalSolutionCost {

	private Problem problem;
	private int[] originalAssignment;

	private long processMoveCost;
	private long machineMoveCost;

	private int maxNumberOfMovedProcesses;
	private int[] numProcessesMovedInService;
	private int[] numServicesHavingCertainNumberOfMovedProcesses;

	public MoveCost(Problem problem) {
		this.problem = problem;
		this.originalAssignment = problem.getOriginalSolution().getAssignment();

		this.processMoveCost = 0;
		this.machineMoveCost = 0;

		this.maxNumberOfMovedProcesses = 0;
		this.numProcessesMovedInService = new int[problem.getNumServices()];
		this.numServicesHavingCertainNumberOfMovedProcesses = new int[problem
				.getMaxNumProcessesInService() + 1];
	}

	public MoveCost(Problem problem, int[] assignment) {
		this(problem);

		for (int p = 0; p < problem.getNumProcesses(); p++) {
			Process process = problem.getProcess(p);
			if (assignment[p] != originalAssignment[p]) {
				processMoveCost += process.moveCost;
				machineMoveCost += problem.getMachine(originalAssignment[p]).moveCosts[assignment[p]];
				numProcessesMovedInService[process.service]++;
			}
		}

		for (int s = 0; s < problem.getNumServices(); ++s) {
			numServicesHavingCertainNumberOfMovedProcesses[numProcessesMovedInService[s]]++;
			if (maxNumberOfMovedProcesses < numProcessesMovedInService[s])
				maxNumberOfMovedProcesses = numProcessesMovedInService[s];
		}
	}

	@Override
	public void addAssignment(Process process, Machine machine) {
		int originalMachine = originalAssignment[process.id];
		if (machine.id != originalAssignment[process.id]) {
			processMoveCost += process.moveCost;
			machineMoveCost += problem.getMachine(originalMachine).moveCosts[machine.id];

			numProcessesMovedInService[process.service] += 1;
			int v = numProcessesMovedInService[process.service];
			numServicesHavingCertainNumberOfMovedProcesses[v - 1] -= 1;
			numServicesHavingCertainNumberOfMovedProcesses[v] += 1;

			if (maxNumberOfMovedProcesses < v) {
				maxNumberOfMovedProcesses = v;
			}
		}
	}

	@Override
	public void removeAssignment(Process process, Machine machine) {
		int originalMachine = originalAssignment[process.id];
		if (machine.id != originalAssignment[process.id]) {
			processMoveCost -= process.moveCost;
			machineMoveCost -= problem.getMachine(originalMachine).moveCosts[machine.id];

			numProcessesMovedInService[process.service] -= 1;
			int v = numProcessesMovedInService[process.service];
			numServicesHavingCertainNumberOfMovedProcesses[v + 1] -= 1;
			numServicesHavingCertainNumberOfMovedProcesses[v] += 1;

			if (maxNumberOfMovedProcesses == v + 1
					&& numServicesHavingCertainNumberOfMovedProcesses[v + 1] == 0) {
				maxNumberOfMovedProcesses -= 1;
			}
		}
	}

	@Override
	public long getCost() {
		return processMoveCost * problem.getProcessMoveCostWeight() + machineMoveCost
				* problem.getMachineMoveCostWeight() + maxNumberOfMovedProcesses
				* problem.getServiceMoveCostWeight();
	}

	@Override
	public long getLowerBound(PartialSolution partialSolution) {
		long processMoveCostBound = 0;
		long machineMoveCostBound = 0;

		Collection<Process> processes = partialSolution.getUnassignedProcesses();
		int[] numProcessesMovedInService = this.numProcessesMovedInService.clone();
		int maxNumProcessesMovedInService = this.maxNumberOfMovedProcesses;

		for (Process p : processes) {
			Machine originalMachine = problem.getMachine(originalAssignment[p.id]);
			if (!partialSolution.canBeAssigned(p, originalMachine)) {
				numProcessesMovedInService[p.service]++;
				maxNumProcessesMovedInService = Math.max(maxNumProcessesMovedInService,
						numProcessesMovedInService[p.service]);

				processMoveCostBound += p.moveCost;
				//TODO: sort machines and try assigning to get more precise bound
				machineMoveCostBound += originalMachine.minMoveCost;
			}
		}

		return (processMoveCost + processMoveCostBound) * problem.getProcessMoveCostWeight() + 
				(machineMoveCost + machineMoveCostBound) * problem.getMachineMoveCostWeight() + 
				+ maxNumProcessesMovedInService * problem.getServiceMoveCostWeight();
	}
}
