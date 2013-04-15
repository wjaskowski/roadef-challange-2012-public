package put.roadef.bnb;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import put.roadef.AssignmentDetails;
import put.roadef.ImmutableSolution;
import put.roadef.LightSolution;
import put.roadef.Problem;
import put.roadef.Problem.Machine;
import put.roadef.Problem.Process;

public class PartialSolution implements AssignmentDetails {

	private final Problem problem;

	public int[] assignment;

	private IntSet[] processesInMachines;
	private TreeSet<Process> unassignedProcesses;

	private FastUtilServiceConstraints serviceConstraints;

	private List<Assignable> assignables;
	private List<IncrementalSolutionConstraints> constraints;
	private List<IncrementalSolutionCost> costs;

	/**
	 * Creates a new (empty) partial solution
	 * 
	 * @param problem
	 */
	public PartialSolution(Problem problem) {
		this.problem = problem;

		processesInMachines = new IntOpenHashSet[problem.getNumMachines()];
		for (int m = 0; m < problem.getNumMachines(); m++) {
			processesInMachines[m] = new IntOpenHashSet();
		}

		assignment = new int[problem.getNumProcesses()];
		Arrays.fill(assignment, -1);

		unassignedProcesses = new TreeSet<Process>();
		for (int p = 0; p < problem.getNumProcesses(); p++) {
			unassignedProcesses.add(problem.getProcess(p));
		}

		serviceConstraints = new FastUtilServiceConstraints(problem);
		initializeAssignables(new Assignable[] { new ResourceConstraints(problem),
				serviceConstraints, new MoveCost(problem) });
	}

	public PartialSolution(Problem problem, int[] assignment) {
		this.problem = problem;
		this.assignment = assignment.clone();

		processesInMachines = new IntOpenHashSet[problem.getNumMachines()];
		for (int m = 0; m < problem.getNumMachines(); m++) {
			processesInMachines[m] = new IntOpenHashSet();
		}

		unassignedProcesses = new TreeSet<Process>();
		for (int p = 0; p < assignment.length; p++) {
			processesInMachines[assignment[p]].add(p);
		}

		serviceConstraints = new FastUtilServiceConstraints(problem, assignment);
		initializeAssignables(new Assignable[] { new ResourceConstraints(problem, assignment),
				serviceConstraints, new MoveCost(problem, assignment) });
	}

	private void initializeAssignables(Assignable[] assignables) {
		this.assignables = new ArrayList<Assignable>();
		this.constraints = new ArrayList<IncrementalSolutionConstraints>();
		this.costs = new ArrayList<IncrementalSolutionCost>();

		for (Assignable a : assignables) {
			this.assignables.add(a);
			if (a instanceof IncrementalSolutionCost) {
				this.costs.add((IncrementalSolutionCost) a);
			}
			if (a instanceof IncrementalSolutionConstraints) {
				this.constraints.add((IncrementalSolutionConstraints) a);
			}
		}
	}

	public IntCollection getMostOverloadedMachines(int numMachines) {
		return ((ResourceConstraints) costs.get(0)).getOverloadedMachines(numMachines);
	}

	public Iterator<Machine> getFeasibleMachineIterator(Process process, Machine lastVisitedMachine) {
		return new FeasibleMachineIterator(process, lastVisitedMachine);
	}

	public ImmutableSolution getSolutionCopy() {
		return new LightSolution(problem, assignment.clone(), getCost());
	}

	public boolean isTerminal() {
		return unassignedProcesses.isEmpty();
	}

	public Set<Process> getSortedProcesses(Process lastVisitedProcess) {
		if (lastVisitedProcess != null) {
			return unassignedProcesses.tailSet(lastVisitedProcess);
		} else {
			return unassignedProcesses;
		}
	}

	public void unAssign(Process process, Machine machine) {
		assignment[process.id] = -1;
		unassignedProcesses.add(process);
		processesInMachines[machine.id].remove(process.id);

		for (Assignable a : assignables) {
			a.removeAssignment(process, machine);
		}
	}

	public void assign(Process process, Machine machine) {
		assignment[process.id] = machine.id;
		unassignedProcesses.remove(process);
		processesInMachines[machine.id].add(process.id);

		for (Assignable a : assignables) {
			a.addAssignment(process, machine);
		}
	}

	/**
	 * Checks feasibility constraints
	 */
	public boolean canBeAssigned(Process process, Machine machine) {
		for (IncrementalSolutionConstraints c : constraints) {
			if (!c.checkFutureConstraints(process, machine)) {
				return false;
			}
		}
		return true;
	}

	public long getLowerBound() {
		long bound = 0;
		for (IncrementalSolutionCost c : costs) {
			bound += c.getLowerBound(this);
		}
		return bound;
	}

	public SortedSet<Process> getUnassignedProcesses() {
		return unassignedProcesses;
	}

	public long getCost() {
		long cost = 0;
		for (IncrementalSolutionCost c : costs) {
			cost += c.getCost();
		}
		return cost;
	}

	public long getResourceCost() {
		return ((ResourceConstraints) costs.get(0)).getCost();
	}

	public long getResourceBound() {
		return ((ResourceConstraints) costs.get(0)).getLowerBound(this);
	}

	public long getMoveCost() {
		return ((MoveCost) costs.get(1)).getCost();
	}

	public long getMoveCostBound() {
		return ((MoveCost) costs.get(1)).getLowerBound(this);
	}

	private class FeasibleMachineIterator implements Iterator<Machine> {

		private Process process;
		private Machine machine;

		private Machine nextMachine;
		private boolean nextFound;

		public FeasibleMachineIterator(Process process, Machine lastVisitedMachine) {
			this.process = process;
			this.machine = lastVisitedMachine;
		}

		@Override
		public boolean hasNext() {
			if (!nextFound) {
				nextMachine = findNextFeasibleMachine();
				nextFound = true;
			}

			return nextMachine != null;
		}

		@Override
		public Machine next() {
			if (nextFound) {
				nextFound = false;
				machine = nextMachine;
			} else {
				machine = findNextFeasibleMachine();
			}
			return machine;
		}

		private Machine findNextFeasibleMachine() {
			for (int m = (machine == null) ? 0 : machine.id + 1; m < problem.getNumMachines(); m++) {
				//return problem.getMachine(m);
				if (canBeAssigned(process, problem.getMachine(m))) {
					return problem.getMachine(m);
				}
			}
			return null;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public Int2IntMap getNeighborhoodsForService(int service) {
		return serviceConstraints.getNeighborhoodsForService(service);
	}

	@Override
	public IntSet getMachinesForService(int service) {
		return serviceConstraints.getMachinesForService(service);
	}

	@Override
	public IntSet getProcessesInMachine(int machineId) {
		return processesInMachines[machineId];
	}
}
