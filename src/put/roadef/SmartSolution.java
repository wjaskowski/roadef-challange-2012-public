package put.roadef;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.text.NumberFormat;
import java.util.Locale;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import put.roadef.Problem.Balance;
import put.roadef.Problem.Machine;
import put.roadef.Problem.Process;
import put.roadef.Problem.Service;

//TODO: metody update* sa niezalezne od siebie i operuja na niezaleznych danych. Powinienem wydzielic je jako klasy. 
//TODO: Albo/oraz utworzyc klasy Machine/Process/Resource przechowujace rozne pola (np. spelnienie constrainta)

//TODO: Quick UndoMove method

//TODO: Performance:
// Najwiecej psuje konstruktor, ktory jest wolny (=> stworzyo lekki Solution (readonly, tylko z kosztem i assignmentem) + przejsc na szybsze struktury danych)
// roadef.SmartSolution.updateServiceConflictsConstraints(int, int, int)	19.494286	151 ms (19,5%)	1116609
// put.roadef.SmartSolution.updateNeihborhoodConstraints(int, int, int)	9.088542	70.5 ms (9,1%)	1116609
// put.roadef.SmartSolution.updateSpreadConstraints(int, int, int)	4.9231	38.1 ms (4,9%)	1116609
// put.roadef.Problem.checkTransientUsageConstraint(put.roadef.ImmutableSolution, int)	2.0211082	15.6 ms (2%)	2233592
public class SmartSolution extends AbstractSolution {
	private long machineMoveCostNotWeighted;
	private long processMoveCostNotWeighted;
	private final int numProcessesMovedInService[];
	private final int numServicesHavingCertainNumberOfMovedProcesses[]; // [maxNumProcessesInService]
	private int maxNumberOfMovedProcesses;
	private long loadCost;
	private final long[][] resourceUsage;
	private final long[][] transientUsage;
	private long balanceCost;
	private long[][] balanceCostsForMachines;
	private int numCapacityConstraintsSatisfied;
	private boolean isCapacityConstraintSatisfied[];
	private int numTransientUsageConstraintsSatisfied;
	private boolean isTransientUsageConstraintSatisfied[];
	public final Int2IntOpenHashMap machinesInService[];
	private int numProcessesForWhichServiceConflictsAreSatisfied;
	private final Int2IntOpenHashMap locationsInService[];
	private int[] numDistinctLocationsInService;
	private int numServicesForWithSpreadConstratinsSatisfied;
	public final Int2IntOpenHashMap neighborhoodsInService[];
	private final int numNeighborhoodsNotSatisifedForDependency[];
	private int numDependenciesSatisfied;
	public final IntOpenHashSet processesInMachine[];

	private boolean isFeasible;
	private long cost;

	private int hashCode;

	public static Logger logger = Logger.getLogger(SmartSolution.class);

	public class Stats {
		public long numIsFeasible;
		public long numCheckCapacityConstraintsFailed;
		public long numCheckTransientUsageConstraintsFailed;
		public long numCheckServiceConflictsConstraintsFailed;
		public long numCheckSpreadConstraintsFailed;
		public long numCheckServiceDependenciesConstraintsFailed;
		public int numIsNotFeasible;

		public Stats() {
		}

		public Stats(Stats stats) {
			numIsFeasible = stats.numIsFeasible;
			numCheckCapacityConstraintsFailed = stats.numCheckCapacityConstraintsFailed;
			numCheckTransientUsageConstraintsFailed = stats.numCheckTransientUsageConstraintsFailed;
			numCheckServiceConflictsConstraintsFailed = stats.numCheckServiceConflictsConstraintsFailed;
			numCheckSpreadConstraintsFailed = stats.numCheckSpreadConstraintsFailed;
			numCheckServiceDependenciesConstraintsFailed = stats.numCheckServiceDependenciesConstraintsFailed;
			numIsNotFeasible = stats.numIsNotFeasible;
		}

		public void reset() {
			numIsFeasible = 0;
			numIsNotFeasible = 0;
			numCheckCapacityConstraintsFailed = 0;
			numCheckTransientUsageConstraintsFailed = 0;
			numCheckServiceConflictsConstraintsFailed = 0;
			numCheckSpreadConstraintsFailed = 0;
			numCheckServiceDependenciesConstraintsFailed = 0;
		}

		private final NumberFormat nf = NumberFormat.getInstance(Locale.US);

		public void log(Logger logger, Level level) {
			logger.log(level, "numIsFeasible = " + nf.format(numIsFeasible));
			logger.log(level, "numIsNotFeasible = " + nf.format(numIsNotFeasible));
			logger.log(level, "numCheckCapacityConstraintsFailed = " + nf.format(numCheckCapacityConstraintsFailed));
			logger.log(level, "numCheckTransientUsageConstraintsFailed = " + nf.format(numCheckTransientUsageConstraintsFailed));
			logger.log(level,
					"numCheckServiceConflictsConstraintsFailed = " + nf.format(numCheckServiceConflictsConstraintsFailed));
			logger.log(level, "numCheckSpreadConstraintsFailed = " + nf.format(numCheckSpreadConstraintsFailed));
			logger.log(level,
					"numCheckServiceDependenciesConstraintsFailed = " + nf.format(numCheckServiceDependenciesConstraintsFailed));
		}
	}

	public Stats stats = new Stats();

	public SmartSolution(ImmutableSolution solution) {
		super(solution);
		logger.trace("SmartSolution(ImmutableSolution solution)");
		machineMoveCostNotWeighted = problem.computeMachineMoveCostNotWeighted(this);
		processMoveCostNotWeighted = problem.computeProcessMoveCost(this);

		numProcessesMovedInService = new int[problem.getNumServices()];
		for (int s = 0; s < problem.getNumServices(); ++s) {
			numProcessesMovedInService[s] = problem.computeNumMovedProcessesOfService(s, solution);
		}
		numServicesHavingCertainNumberOfMovedProcesses = new int[problem.getMaxNumProcessesInService() + 1];
		for (int s = 0; s < problem.getNumServices(); ++s) {
			numServicesHavingCertainNumberOfMovedProcesses[numProcessesMovedInService[s]] += 1;
			if (maxNumberOfMovedProcesses < numProcessesMovedInService[s])
				maxNumberOfMovedProcesses = numProcessesMovedInService[s];
		}

		resourceUsage = problem.computeResourceUsage(this);
		transientUsage = problem.computeTransientResourceUsage(this);
		loadCost = problem.computeLoadCost(this);

		//
		balanceCost = 0;
		balanceCostsForMachines = new long[problem.getNumMachines()][];
		for (int m = 0; m < problem.getNumMachines(); m++) {
			balanceCostsForMachines[m] = new long[problem.getNumBalances()];
			for (int b = 0; b < problem.getNumBalances(); ++b) {
				balanceCostsForMachines[m][b] = problem.computeBalanceCost(this, problem.getBalance(b), problem.getMachine(m));
				balanceCost += balanceCostsForMachines[m][b];
			}
		}

		//
		isCapacityConstraintSatisfied = new boolean[problem.getNumMachines()];
		for (int m = 0; m < problem.getNumMachines(); ++m) {
			isCapacityConstraintSatisfied[m] = problem.checkCapacityConstraint(this, m);
			if (isCapacityConstraintSatisfied[m])
				numCapacityConstraintsSatisfied += 1;
		}

		//
		isTransientUsageConstraintSatisfied = new boolean[problem.getNumMachines()];
		for (int m = 0; m < problem.getNumMachines(); ++m) {
			isTransientUsageConstraintSatisfied[m] = problem.checkTransientUsageConstraint(this, m);
			if (isTransientUsageConstraintSatisfied[m])
				numTransientUsageConstraintsSatisfied += 1;
		}

		//
		//TODO: To jest troche kosztowne (choc nie jakos strasznie). Gdyby nie mozna bylo wychodzic poza przestrzen feasible, moglbym zrobic to taniej i latwiej
		machinesInService = new Int2IntOpenHashMap[problem.getNumServices()];
		for (int s = 0; s < problem.getNumServices(); ++s)
			machinesInService[s] = new Int2IntOpenHashMap();
		for (int p = 0; p < problem.getNumProcesses(); ++p) {
			int m = assignment[p];
			int s = problem.getProcess(p).service;
			if (machinesInService[s].add(m, +1) == 0)
				;
			numProcessesForWhichServiceConflictsAreSatisfied += 1;
		}

		//
		numDistinctLocationsInService = new int[problem.getNumServices()];
		locationsInService = new Int2IntOpenHashMap[problem.getNumServices()];
		for (int s = 0; s < problem.getNumServices(); ++s)
			locationsInService[s] = new Int2IntOpenHashMap();
		for (int p = 0; p < problem.getNumProcesses(); ++p) {
			int m = assignment[p];
			int s = problem.getProcess(p).service;
			int l = problem.getMachine(m).location;
			if (locationsInService[s].add(l, +1) == 0)
				numDistinctLocationsInService[s] += 1;
		}
		for (int s = 0; s < problem.getNumServices(); ++s) {
			assert numDistinctLocationsInService[s] > 0;
			if (numDistinctLocationsInService[s] >= problem.getService(s).spread)
				numServicesForWithSpreadConstratinsSatisfied += 1;
		}

		//
		neighborhoodsInService = new Int2IntOpenHashMap[problem.getNumServices()];
		for (int s = 0; s < problem.getNumServices(); ++s)
			neighborhoodsInService[s] = new Int2IntOpenHashMap();
		for (int p = 0; p < problem.getNumProcesses(); ++p) {
			int m = assignment[p];
			int s = problem.getProcess(p).service;
			int n = problem.getMachine(m).neighborhood;
			neighborhoodsInService[s].add(n, +1);
		}

		numNeighborhoodsNotSatisifedForDependency = new int[problem.getNumDependencies()];
		for (int d = 0; d < problem.getNumDependencies(); ++d) {
			int sa = problem.getDependency(d).serviceA;
			int sb = problem.getDependency(d).serviceB;
			for (int n : neighborhoodsInService[sa].keySet()) {
				if (neighborhoodsInService[sb].get(n) == 0)
					numNeighborhoodsNotSatisifedForDependency[d] += 1;
			}
			if (numNeighborhoodsNotSatisifedForDependency[d] == 0)
				numDependenciesSatisfied += 1;
		}

		processesInMachine = new IntOpenHashSet[problem.getNumMachines()];
		for (int m = 0; m < problem.getNumMachines(); ++m)
			processesInMachine[m] = new IntOpenHashSet();
		for (int p = 0; p < problem.getNumProcesses(); ++p)
			processesInMachine[assignment[p]].add(p);

		cost = getCostInternal();
		isFeasible = isFeasibleInternal();

		hashCode = calculateHashCode();
	}

	private SmartSolution(SmartSolution solution) {
		super(solution);
		logger.trace("SmartSolution(SmartSolution solution)");

		machineMoveCostNotWeighted = solution.machineMoveCostNotWeighted;
		processMoveCostNotWeighted = solution.processMoveCostNotWeighted;
		numProcessesMovedInService = solution.numProcessesMovedInService.clone();
		numServicesHavingCertainNumberOfMovedProcesses = solution.numServicesHavingCertainNumberOfMovedProcesses.clone();
		maxNumberOfMovedProcesses = solution.maxNumberOfMovedProcesses;

		resourceUsage = new long[solution.resourceUsage.length][];
		for (int i = 0; i < resourceUsage.length; ++i)
			resourceUsage[i] = solution.resourceUsage[i].clone();
		transientUsage = new long[solution.transientUsage.length][];
		for (int i = 0; i < transientUsage.length; ++i)
			transientUsage[i] = solution.transientUsage[i].clone();
		loadCost = solution.loadCost;
		balanceCost = solution.balanceCost;
		balanceCostsForMachines = new long[problem.getNumMachines()][];
		for (int m = 0; m < problem.getNumMachines(); ++m)
			balanceCostsForMachines[m] = solution.balanceCostsForMachines[m].clone();

		isCapacityConstraintSatisfied = solution.isCapacityConstraintSatisfied.clone();
		numCapacityConstraintsSatisfied = solution.numCapacityConstraintsSatisfied;
		isTransientUsageConstraintSatisfied = solution.isTransientUsageConstraintSatisfied.clone();
		numTransientUsageConstraintsSatisfied = solution.numTransientUsageConstraintsSatisfied;

		machinesInService = new Int2IntOpenHashMap[problem.getNumServices()];
		for (int s = 0; s < problem.getNumServices(); ++s)
			machinesInService[s] = solution.machinesInService[s].clone();
		numProcessesForWhichServiceConflictsAreSatisfied = solution.numProcessesForWhichServiceConflictsAreSatisfied;

		locationsInService = new Int2IntOpenHashMap[problem.getNumServices()];
		for (int s = 0; s < problem.getNumServices(); ++s)
			locationsInService[s] = solution.locationsInService[s].clone();
		numDistinctLocationsInService = solution.numDistinctLocationsInService.clone();
		numServicesForWithSpreadConstratinsSatisfied = solution.numServicesForWithSpreadConstratinsSatisfied;

		neighborhoodsInService = new Int2IntOpenHashMap[problem.getNumServices()];
		for (int s = 0; s < problem.getNumServices(); ++s)
			neighborhoodsInService[s] = solution.neighborhoodsInService[s].clone();

		numNeighborhoodsNotSatisifedForDependency = solution.numNeighborhoodsNotSatisifedForDependency.clone();
		numDependenciesSatisfied = solution.numDependenciesSatisfied;

		processesInMachine = solution.processesInMachine.clone();
		for (int m = 0; m < problem.getNumMachines(); ++m)
			processesInMachine[m] = solution.processesInMachine[m].clone();

		cost = solution.cost;
		isFeasible = solution.isFeasible;

		hashCode = solution.hashCode;

		stats = new Stats(solution.stats);
	}

	//TODO: Bartek, napisz chociao co to robi? To jakas czesciowa kopia jest?
	public void assign(SmartSolution solution) {
		logger.error("To na pewno nie dziala, bu juz od tego czasu to klaso zmieniaoem. Ale nie to funkcje"); //WARNING!
		problem = solution.problem;
		assignment = solution.assignment;

		machineMoveCostNotWeighted = solution.machineMoveCostNotWeighted;
		processMoveCostNotWeighted = solution.processMoveCostNotWeighted;
		//numProcessesMovedInService = ss.numProcessesMovedInService;
		for (int i = 0; i < numProcessesMovedInService.length; i++)
			numProcessesMovedInService[i] = solution.numProcessesMovedInService[i];
		//numServicesHavingCertainNumberOfMovedProcesses = ss.numServicesHavingCertainNumberOfMovedProcesses; // [maxNumProcessesInService]
		for (int i = 0; i < numServicesHavingCertainNumberOfMovedProcesses.length; i++)
			numServicesHavingCertainNumberOfMovedProcesses[i] = solution.numServicesHavingCertainNumberOfMovedProcesses[i];
		maxNumberOfMovedProcesses = solution.maxNumberOfMovedProcesses;
		loadCost = solution.loadCost;
		//resourceUsage = ss.resourceUsage;
		for (int i = 0; i < resourceUsage.length; i++) {
			for (int j = 0; j < resourceUsage[i].length; j++)
				resourceUsage[i][j] = solution.resourceUsage[i][j];
		}

		//transientUsage = ss.transientUsage;
		for (int i = 0; i < transientUsage.length; i++) {
			for (int j = 0; j < transientUsage[i].length; j++)
				transientUsage[i][j] = solution.transientUsage[i][j];
		}

		balanceCost = solution.balanceCost;
		numCapacityConstraintsSatisfied = solution.numCapacityConstraintsSatisfied;
		isCapacityConstraintSatisfied = solution.isCapacityConstraintSatisfied;
		numTransientUsageConstraintsSatisfied = solution.numTransientUsageConstraintsSatisfied;
		isTransientUsageConstraintSatisfied = solution.isTransientUsageConstraintSatisfied;
		//machinesInService = ss.machinesInService;
		for (int i = 0; i < machinesInService.length; i++)
			machinesInService[i] = solution.machinesInService[i];
		numProcessesForWhichServiceConflictsAreSatisfied = solution.numProcessesForWhichServiceConflictsAreSatisfied;
		//locationsInService = ss.locationsInService;
		for (int i = 0; i < locationsInService.length; i++)
			locationsInService[i] = solution.locationsInService[i];

		numServicesForWithSpreadConstratinsSatisfied = solution.numServicesForWithSpreadConstratinsSatisfied;
		//neighborhoodsInService = ss.neighborhoodsInService;
		for (int i = 0; i < neighborhoodsInService.length; i++)
			neighborhoodsInService[i] = solution.neighborhoodsInService[i];
		//numNeighborhoodsNotSatisifedForDependency = ss.numNeighborhoodsNotSatisifedForDependency;
		for (int i = 0; i < numNeighborhoodsNotSatisifedForDependency.length; i++)
			numNeighborhoodsNotSatisifedForDependency[i] = solution.numNeighborhoodsNotSatisifedForDependency[i];
		numDependenciesSatisfied = solution.numDependenciesSatisfied;

		isFeasible = solution.isFeasible;
		cost = solution.cost;

		hashCode = solution.hashCode;
	}

	@Override
	public Solution clone() {
		return new SmartSolution(this);
	}

	private int calculateHashCode() {
		int hashCode = 0;
		for (int i = 0; i < assignment.length; i++)
			hashCode += 524287 * (i + 1) * assignment[i];
		return hashCode;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	//TODO: destinationMachine => destinationMachineId
	public void moveProcess(int processId, int destinationMachine) {
		int sourceMachine = getMachine(processId);
		if (sourceMachine == destinationMachine)
			return;

		assignment[processId] = destinationMachine;

		int originalMachine = problem.getOriginalSolution().getMachine(processId);

		// O(1)
		updateProcessesInMachine(processId, sourceMachine, destinationMachine);

		// O(1)
		updateMachineMoveCost(sourceMachine, destinationMachine, originalMachine);
		updateProcessMoveCost(processId, sourceMachine, destinationMachine, originalMachine);
		updateServiceMoveCost(processId, sourceMachine, destinationMachine, originalMachine);

		// O(r)
		updateResourceUsageAndLoadCost(processId, sourceMachine, destinationMachine, originalMachine);
		
		// O(b)
		updateBalanceCost(processId, sourceMachine, destinationMachine, originalMachine);

		// O(r)
		updateCapacityConstraints(sourceMachine);
		updateCapacityConstraints(destinationMachine);

		// O(r)
		updateTransientUsageConstraints(sourceMachine);
		updateTransientUsageConstraints(destinationMachine);

		// O(1)
		updateServiceConflictsConstraints(processId, sourceMachine, destinationMachine);
		// O(1)
		updateSpreadConstraints(processId, sourceMachine, destinationMachine);
		
		// O(revd(service(p) + d(service(p)))
		updateNeihborhoodConstraints(processId, sourceMachine, destinationMachine);

		updateHashCode(processId, sourceMachine, destinationMachine);

		cost = getCostInternal();
		isFeasible = isFeasibleInternal();
	}

	public boolean couldBeFeasibleQuick(int processId, int destinationMachine) {
		if (!couldBeFeasibleLoadCost(processId, destinationMachine))
			return false;
		// This works, but it does not help. Service contraints are too rare.
		//		if (!couldBeFeasibeServiceConflicts(processId, destinationMachine))
		//			return false;
		return true;
	}

	@SuppressWarnings("unused")
	private boolean couldBeFeasibeServiceConflicts(int processId, int destinationMachine) {
		int s = problem.getProcess(processId).service;
		if (machinesInService[s].get(destinationMachine) == 0)
			return true;
		else
			return false;
	}

	private boolean couldBeFeasibleLoadCost(int processId, int destinationMachine) {
		int originalMachine = problem.getOriginalSolution().getMachine(processId);
		for (int r = 0; r < problem.getNumResources(); ++r) {
			if (resourceUsage[destinationMachine][r] + problem.getProcess(processId).requirements[r] > problem
					.getMachine(destinationMachine).capacities[r])
				return false;
			if (destinationMachine != originalMachine)
				if (transientUsage[destinationMachine][r] + problem.getProcess(processId).requirements[r] > problem
						.getMachine(destinationMachine).capacities[r])
					return false;
		}
		return true;
	}

	private void updateProcessesInMachine(int processId, int sourceMachineId, int destinationMachineId) {
		processesInMachine[sourceMachineId].remove(processId);
		processesInMachine[destinationMachineId].add(processId);
	}

	void updateHashCode(int processId, int sourceMachineId, int destinationMachineId) {
		hashCode += 524287 * (processId + 1) * (destinationMachineId - sourceMachineId);
	}

	private void updateNeihborhoodConstraints(int processId, int sourceMachineId, int destinationMachineId) {
		//This could be also achieved using a simple [services][neighborhoods] array, which would be quicker, but would require
		//more memory (5000*1000) (TODO?)
		int sourceNeighborhoodId = problem.getMachine(sourceMachineId).neighborhood;
		int destinationNeighborhoodId = problem.getMachine(destinationMachineId).neighborhood;

		if (sourceNeighborhoodId == destinationNeighborhoodId)
			return;

		int s = problem.getProcess(processId).service;
		Service service = problem.getService(s);

		//Remove sourceNeighborhoodId from service
		int sourceCount = neighborhoodsInService[s].add(sourceNeighborhoodId, -1);

		//Add destinationNeighborhoodId from service
		int destinationCount = neighborhoodsInService[s].add(destinationNeighborhoodId, +1);

		if (sourceCount == 1) {
			//TODO: te petle mozna zrobic szybciej trzymajac wiecej tablic i nie przechodzac po wszystkich revDependencies, ale tylko po tych po ktorych trzeba.
			//Trzeba trzymac dla kazdego service'u tablice HashSet<Integer>[] revDependenciesWithServicesInNeighborhood[neighborhood].
			for (int d = 0; d < service.revDependencies.length; ++d) {
				int sa = service.revDependencies[d];
				if (neighborhoodsInService[sa].get(sourceNeighborhoodId) > 0) {
					int did = service.revDependencyIds[d];
					//Neighborhood which was required for dependency sa<-s has been removed
					numNeighborhoodsNotSatisifedForDependency[did] += 1;
					assert 0 <= numNeighborhoodsNotSatisifedForDependency[did];
					if (numNeighborhoodsNotSatisifedForDependency[did] == 1)
						numDependenciesSatisfied -= 1;
				}
			}

			for (int d = 0; d < service.dependencies.length; ++d) {
				int sb = service.dependencies[d];
				if (neighborhoodsInService[sb].get(sourceNeighborhoodId) == 0) {
					int did = service.dependencyIds[d];
					//Dependency s<-sb on neighborhood sourceNeighborhoodId has been removed, so it get satisfied
					numNeighborhoodsNotSatisifedForDependency[did] -= 1;
					assert 0 <= numNeighborhoodsNotSatisifedForDependency[did];
					if (numNeighborhoodsNotSatisifedForDependency[did] == 0)
						numDependenciesSatisfied += 1;
				}
			}
		}

		if (destinationCount == 0) {
			for (int d = 0; d < service.revDependencies.length; ++d) {
				int sa = service.revDependencies[d];
				if (neighborhoodsInService[sa].get(destinationNeighborhoodId) > 0) {
					//Neighborhood which was required for sa<-a has been added
					int did = service.revDependencyIds[d];
					numNeighborhoodsNotSatisifedForDependency[did] -= 1;
					assert 0 <= numNeighborhoodsNotSatisifedForDependency[did];
					if (numNeighborhoodsNotSatisifedForDependency[did] == 0)
						numDependenciesSatisfied += 1;
				}
			}

			for (int d = 0; d < service.dependencies.length; ++d) {
				int sb = service.dependencies[d];
				int did = service.dependencyIds[d];
				if (neighborhoodsInService[sb].get(destinationNeighborhoodId) == 0) {
					numNeighborhoodsNotSatisifedForDependency[did] += 1;
					assert numNeighborhoodsNotSatisifedForDependency[did] <= neighborhoodsInService[s].size();
					if (numNeighborhoodsNotSatisifedForDependency[did] == 1)
						numDependenciesSatisfied -= 1;
				}
			}
		}
	}

	private void updateSpreadConstraints(int processId, int sourceMachineId, int destinationMachineId) {
		//This could be also achieved using a simple [services][locations] array, which would be quicker, but would require
		//more memory (5000*1000) (TODO?)
		int sourceLocationId = problem.getMachine(sourceMachineId).location;
		int destinationLocationId = problem.getMachine(destinationMachineId).location;

		if (sourceLocationId == destinationLocationId)
			return;

		int s = problem.getProcess(processId).service;
		int oldNumDistinctLocationsInService = numDistinctLocationsInService[s];

		if (locationsInService[s].add(sourceLocationId, -1) == 1)
			numDistinctLocationsInService[s] -= 1;

		if (locationsInService[s].add(destinationLocationId, +1) == 0)
			numDistinctLocationsInService[s] += 1;

		int newNumDistinctLocationsInService = numDistinctLocationsInService[s];
		int spread = problem.getService(s).spread;
		if (oldNumDistinctLocationsInService < spread && newNumDistinctLocationsInService >= spread)
			numServicesForWithSpreadConstratinsSatisfied += 1;
		else if (oldNumDistinctLocationsInService >= spread && newNumDistinctLocationsInService < spread)
			numServicesForWithSpreadConstratinsSatisfied -= 1;
	}

	private void updateServiceConflictsConstraints(int processId, int sourceMachineId, int destinationMachineId) {
		int s = problem.getProcess(processId).service;
		assert machinesInService[s].get(sourceMachineId) > 0;

		if (machinesInService[s].add(sourceMachineId, -1) == 2) // It was 2 (bad), it is 1 (good)				
			numProcessesForWhichServiceConflictsAreSatisfied += 1;

		if (machinesInService[s].add(destinationMachineId, +1) == 1) // It was 1 (good), it two (bad)			
			numProcessesForWhichServiceConflictsAreSatisfied -= 1;
	}

	private void updateTransientUsageConstraints(int machineId) {
		boolean isSatisfiedNew = problem.checkTransientUsageConstraint(this, machineId);
		if (isSatisfiedNew) {
			if (!isTransientUsageConstraintSatisfied[machineId]) {
				isTransientUsageConstraintSatisfied[machineId] = true;
				numTransientUsageConstraintsSatisfied += 1;
			}
		} else {
			if (isTransientUsageConstraintSatisfied[machineId]) {
				isTransientUsageConstraintSatisfied[machineId] = false;
				numTransientUsageConstraintsSatisfied -= 1;
			}
		}
	}

	private void updateCapacityConstraints(int machineId) {
		boolean isSatisfiedNew = problem.checkCapacityConstraint(this, machineId);
		if (isSatisfiedNew) {
			if (!isCapacityConstraintSatisfied[machineId]) {
				isCapacityConstraintSatisfied[machineId] = true;
				numCapacityConstraintsSatisfied += 1;
			}
		} else {
			if (isCapacityConstraintSatisfied[machineId]) {
				isCapacityConstraintSatisfied[machineId] = false;
				numCapacityConstraintsSatisfied -= 1;
			}
		}
	}

	private void updateBalanceCost(int processId, int sourceMachine, int destinationMachine, int originalMachine) {
		for (int b = 0; b < problem.getNumBalances(); ++b) {
			long diffSource = updateBalanceCost(sourceMachine, b, resourceUsage[sourceMachine],
					balanceCostsForMachines[sourceMachine]);
			long diffDestination = updateBalanceCost(destinationMachine, b, resourceUsage[destinationMachine],
					balanceCostsForMachines[destinationMachine]);
			balanceCost += (diffSource + diffDestination);
		}
	}

	private long updateBalanceCost(int machine, int balanceId, long[] resourceUsage, long[] savedBalanceCost) {
		long newBalanceCost = problem.computeBalanceCost(this, problem.getBalance(balanceId), problem.getMachine(machine));
		long diff = -savedBalanceCost[balanceId] + newBalanceCost;
		savedBalanceCost[balanceId] = newBalanceCost;
		return diff;
	}

	private void updateResourceUsageAndLoadCost(int processId, int sourceMachine, int destinationMachine, int originalMachine) {
		for (int r = 0; r < problem.getNumResources(); r++) {
			long requirement = problem.getProcess(processId).requirements[r];
			if (problem.getResource(r).isTransient) {
				if (sourceMachine != originalMachine) {
					transientUsage[sourceMachine][r] -= requirement;
				}
				if (destinationMachine != originalMachine) {
					transientUsage[destinationMachine][r] += requirement;
				}
			}

			addToResourceUsage(sourceMachine, r, -requirement);
			addToResourceUsage(destinationMachine, r, requirement);
		}
	}

	private void addToResourceUsage(int machine, int resource, long additionalUsage) {
		long oldResourceUsage = resourceUsage[machine][resource];
		long newResourceUsage = oldResourceUsage + additionalUsage;
		resourceUsage[machine][resource] = newResourceUsage;

		long safety = problem.getMachine(machine).safetyCapacities[resource];

		long diff = -problem.computeLoadCostNotWeighted(oldResourceUsage, safety) + problem.computeLoadCostNotWeighted(newResourceUsage, safety);
		loadCost += diff * problem.getResource(resource).loadCostWeight;
	}

	private void updateProcessMoveCost(int processId, int sourceMachine, int destinationMachine, int originalMachine) {
		assert sourceMachine != destinationMachine;

		if (sourceMachine == originalMachine)
			processMoveCostNotWeighted += problem.getProcess(processId).moveCost;
		else if (destinationMachine == originalMachine)
			processMoveCostNotWeighted -= problem.getProcess(processId).moveCost;
	}

	private void updateMachineMoveCost(int sourceMachine, int destinationMachine, int originalMachine) {
		assert sourceMachine != destinationMachine;

		long costDiff = problem.getMachine(originalMachine).moveCosts[destinationMachine]
				- problem.getMachine(originalMachine).moveCosts[sourceMachine];

		machineMoveCostNotWeighted += costDiff;
	}

	private void updateServiceMoveCost(int processId, int sourceMachine, int destinationMachine, int originalMachine) {
		assert sourceMachine != destinationMachine;

		int serviceId = problem.getProcess(processId).service;

		if (sourceMachine == originalMachine) {
			numProcessesMovedInService[serviceId] += 1;
			int v = numProcessesMovedInService[serviceId];
			numServicesHavingCertainNumberOfMovedProcesses[v - 1] -= 1;
			numServicesHavingCertainNumberOfMovedProcesses[v] += 1;

			if (maxNumberOfMovedProcesses < v)
				maxNumberOfMovedProcesses = v;
		} else if (destinationMachine == originalMachine) {
			numProcessesMovedInService[serviceId] -= 1;
			int v = numProcessesMovedInService[serviceId];
			numServicesHavingCertainNumberOfMovedProcesses[v + 1] -= 1;
			numServicesHavingCertainNumberOfMovedProcesses[v] += 1;

			if (maxNumberOfMovedProcesses == v + 1 && numServicesHavingCertainNumberOfMovedProcesses[v + 1] == 0)
				maxNumberOfMovedProcesses -= 1;
		}
	}

	public long getMachineMoveCost() {
		return machineMoveCostNotWeighted * problem.getMachineMoveCostWeight();
	}

	public long getProcessMoveCost() {
		return processMoveCostNotWeighted * problem.getProcessMoveCostWeight();
	}

	public long getServiceMoveCost() {
		return maxNumberOfMovedProcesses * problem.getServiceMoveCostWeight();
	}

	public long getLoadCost() {
		return loadCost;
	}

	public long getBalanceCost() {
		return balanceCost;
	}

	public boolean checkCapacityConstraintsSatisfied() {
		return getNumCapacityConstraintsUnsatisfied() == 0;
	}

	public boolean checkTransientUsageConstraintsSatisfied() {
		return getNumTransientConstraintsUnsatisfied() == 0;
	}

	public boolean checkServiceConflictsConstraintsSatisifed() {
		return getNumServiceConflictsConstraintsUnsatisifed() == 0;
	}

	public boolean checkSpreadConstraintsSatisfied() {
		return (getNumSpreadConstraintsUnsatisfied() == 0);
	}

	public boolean checkServiceDependenciesConstraintsSatisfied() {
		return (getNumServiceDependenciesConstraintsUnsatisfied() == 0);
	}

	private int getNumCapacityConstraintsUnsatisfied() {
		return problem.getNumMachines() - numCapacityConstraintsSatisfied;
	}

	private int getNumTransientConstraintsUnsatisfied() {
		return problem.getNumMachines() - numTransientUsageConstraintsSatisfied;
	}

	private int getNumServiceConflictsConstraintsUnsatisifed() {
		return problem.getNumProcesses() - numProcessesForWhichServiceConflictsAreSatisfied;
	}

	private int getNumSpreadConstraintsUnsatisfied() {
		return problem.getNumServices() - numServicesForWithSpreadConstratinsSatisfied;
	}

	private int getNumServiceDependenciesConstraintsUnsatisfied() {
		return problem.getNumDependencies() - numDependenciesSatisfied;
	}

	public int getNumConstraintsUnsatisfied() {
		return getNumCapacityConstraintsUnsatisfied() + getNumTransientConstraintsUnsatisfied()
				+ getNumServiceConflictsConstraintsUnsatisifed() + getNumSpreadConstraintsUnsatisfied()
				+ getNumServiceDependenciesConstraintsUnsatisfied();
	}

	private boolean isFeasibleInternal() {
		boolean isFeasible = checkCapacityConstraintsSatisfied() && checkTransientUsageConstraintsSatisfied()
				&& checkServiceConflictsConstraintsSatisifed() && checkSpreadConstraintsSatisfied()
				&& checkServiceDependenciesConstraintsSatisfied();

		// This is just for statistics. Does not cost a lot of time
		if (!checkCapacityConstraintsSatisfied())
			stats.numCheckCapacityConstraintsFailed += 1;
		if (!checkTransientUsageConstraintsSatisfied())
			stats.numCheckTransientUsageConstraintsFailed += 1;
		if (!checkServiceConflictsConstraintsSatisifed())
			stats.numCheckServiceConflictsConstraintsFailed += 1;
		if (!checkSpreadConstraintsSatisfied())
			stats.numCheckSpreadConstraintsFailed += 1;
		if (!checkServiceDependenciesConstraintsSatisfied())
			stats.numCheckServiceDependenciesConstraintsFailed += 1;
		if (isFeasible)
			stats.numIsFeasible += 1;
		else
			stats.numIsNotFeasible += 1;
		return isFeasible;
	}

	private long getCostInternal() {
		return getLoadCost() + getBalanceCost() + getProcessMoveCost() + getServiceMoveCost() + getMachineMoveCost();
	}

	@Override
	public long getCost() {
		return cost;
	}

	@Override
	public boolean isFeasible() {
		return isFeasible;
	}

	@Override
	public long getResourceUsage(int machineId, int resourceId) {
		return resourceUsage[machineId][resourceId];
	}

	@Override
	public long getTransientUsage(int machineId, int resourceId) {
		return transientUsage[machineId][resourceId];
	}

	//	public long getLoadCostForMachine(int machineId) {
	//		//TODO (Very important!): Te operacje moga byc cashowane i dostepne za darmo (no bo przeciez jeden move zmienia tylko dla jednej maszyny
	//		long cost = 0;
	//		for (int r = 0; r < problem.getNumResources(); ++r) {
	//			long loadCost = problem.computeLoadCost(resourceUsage[machineId][r], problem.getMachine(machineId).safetyCapacities[r]);
	//			cost += problem.getResource(r).loadCostWeight * loadCost;					
	//		}
	//		return cost;
	//	}

	/*
	 * @brief What is the load cost of a single process. 
	 * Potentially what can be gained when moving this process to another machine. UpperBound
	 */
	public long getLoadCost(int processId) {
		//TODO: This could be probably computed during MoveTo
		long cost = 0;
		Process p = problem.getProcess(processId);
		Machine m = problem.getMachine(assignment[p.id]);

		for (int r = 0; r < problem.getNumResources(); ++r) {
			long machineResourceUsage = resourceUsage[m.id][r];
			long loadCostCurrent = problem.computeLoadCostNotWeighted(machineResourceUsage, m.safetyCapacities[r]);
			long loadCostWithoutProcess = problem
					.computeLoadCostNotWeighted(machineResourceUsage - p.requirements[r], m.safetyCapacities[r]);
			cost += problem.getResource(r).loadCostWeight * (loadCostCurrent - loadCostWithoutProcess);
		}
		return cost;
	}

	/*
	 * @brief What is the balance cost of a single process. Potentially what can be gained when moving this process to another machine. UpperBound (optimistic profit)
	 */
	public long getBalanceCost(int processId) {
		Process p = problem.getProcess(processId);
		Machine m = problem.getMachine(assignment[p.id]);

		long optimisticProfit = 0;
		for (int i = 0; i < problem.getNumBalances(); ++i) {
			Balance b = problem.getBalance(i);

			long a1 = problem.computeAvailableResources(this, m, b.r1);
			long a2 = problem.computeAvailableResources(this, m, b.r2);
			long r1 = p.requirements[b.r1];
			long r2 = p.requirements[b.r2];
			long bm = problem.computeBalanceValue(b.target, a1, a2);
			long bp = problem.computeBalanceValue(b.target, r1, r2);

			optimisticProfit += Math.max(bm, 0) - Math.max(bm - bp, 0) + Math.max(-bp, 0) * b.weight; // may be negative
			// Dwa pierwsze skoadniki dotyczo aktualnej maszyny (m) i so dokoadne. Ostatni skoadnik dotyczy maszyny docelowej i jest optymistyczny. 
			// Gdyby trzymao wszystkie aktualne balancy maszyn i kolejko priorytetowo, moona by zacieonio ten optymistyczny balance (TODO)
		}
		return optimisticProfit; // May be negative
	}

	/*
	 * @brief Potencially what can be gained by moving (moveCosts) the process to another machine. UpperBound.
	 */
	public long getMoveCost(int processId) {
		put.roadef.Problem.Process p = problem.getProcess(processId);
		Machine m = problem.getMachine(assignment[p.id]);
		int originalMachine = problem.getOriginalSolution().getMachine(p.id);

		if (originalMachine == m.id)
			return 0;

		long cost = 0;
		// Only if moving to the originalMachine (UpperBound)
		cost += problem.getProcessMoveCostWeight() * p.moveCost;
		cost += problem.getMachineMoveCostWeight() * problem.getMachine(m.id).moveCosts[m.id];

		// Only if service the process is in has the most moved processes 
		if (maxNumberOfMovedProcesses == numProcessesMovedInService[p.service]
				&& numServicesHavingCertainNumberOfMovedProcesses[maxNumberOfMovedProcesses] == 1)
			cost += problem.getServiceMoveCostWeight() * 1;
		return cost;
	}

	@Override
	public ImmutableSolution lightClone() {
		return new LightSolution(this);
	}

	public static SmartSolution promoteToSmartSolution(Solution solution) {
		if (solution instanceof SmartSolution)
			return (SmartSolution)solution;
		else
			return new SmartSolution(solution);		
	}
}
