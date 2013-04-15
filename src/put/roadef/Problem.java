package put.roadef;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

public class Problem {
	public Logger logger = Logger.getLogger(put.roadef.Problem.class);
	private static final int MAX_NUM_THREADS = 2;

	public static final int MAX_MACHINES = 5000;
	public static final int MAX_RESOURCES = 20;
	public static final int MAX_PROCESSES = 50000;
	public static final int MAX_SERVICES = 50000;
	public static final int MAX_NEIGHBORHOODS = 1000;
	public static final int MAX_DEPENDENCIES = 5000;
	public static final int MAX_LOCATIONS = 1000;
	public static final int MAX_BALANCE_COSTS = 10;

	public static final class Resource {
		public final boolean isTransient;
		public final long loadCostWeight; //uint32		
		public long maxProcessRequirement;
		public long minProcessRequirement;

		public Resource(boolean isTransient, long loadCostWeight) {
			this.isTransient = isTransient;
			this.loadCostWeight = loadCostWeight;
		}
	}

	public static final class Machine {
		public final int id;
		public final int location; // range: <0,1000) (locations limit)
		public final int neighborhood; //range: <0,1000) (neighborhoods limit)
		public final long[] capacities; // [resources.length] uint32
		public final long[] safetyCapacities; // [resources.length] uint32
		public final long[] moveCosts; // [machines.length] uint32
		public final long minMoveCost; //uint32

		public Machine(int id, int location, int neighborhood, long[] capacities, long[] safetyCapacities, long[] moveCosts,
				long minMoveCost) {
			this.id = id;
			this.location = location;
			this.neighborhood = neighborhood;
			this.capacities = capacities;
			this.safetyCapacities = safetyCapacities;
			this.moveCosts = moveCosts;
			this.minMoveCost = minMoveCost;
		}

		public Machine cloneToLocationAndNeighborhood(int location, int neighborhood) {
			return new Machine(this.id, location, neighborhood, this.capacities, this.safetyCapacities, this.moveCosts,
					this.minMoveCost);
		}
	}

	public static final class Service {
		public int spread; // range: <0,1000) (locations limit)
		public int numDependencies; // range: <0,5000) (services limit)

		public int[] dependencies; // range: <0,5000) (services limit). There are at most 5000 dependencies in total.
		public int[] dependencyIds;

		public int[] processes; // range: <0,50000) (processes limit). There are at most processes.length entries in total.
		private int[] transitiveDependencies;
		private int[] transitiveRevDependencies;

		/**
		 * Reverse dependencies describe services that are dependent from this
		 * service
		 */
		public int numRevDependencies; // range: <0,5000) (services limit)
		/**
		 * Reverse dependencies describe services that are dependent from this
		 * service
		 */
		public int[] revDependencies; // range: <0,5000) (services limit). There are at most 5000 dependencies in total.
		public int[] revDependencyIds;
	}

	/**
	 * Immutable class representing process. Only Problem class can instantiate
	 * Processes.
	 * 
	 * @author marcin
	 * 
	 */
	public static final class Process implements Comparable<Process> {
		public final int id;
		public final int service; // range: <0,5000) (services limit)
		public final long[] requirements; // uint32
		public final long moveCost; // uint32

		private Process(int id, int service, long[] requirements, long moveCost) {
			this.id = id;
			this.service = service;
			this.requirements = requirements;
			this.moveCost = moveCost;
		}

		@Override
		public int compareTo(Process process) {
			if (moveCost != process.moveCost) {
				return (int) (moveCost - process.moveCost);
			} else {
				return id - process.id;
			}
		}
	}

	static public class Balance {
		public int r1; // max: 20 (resources limit)
		public int r2; // max: 20 (resources limit)
		public long target; // uint32
		public long weight; // uint32
	}

	static public final class Dependency {
		public final int serviceA;
		public final int serviceB;
		public final int id;

		public Dependency(int id, int serviceA, int serviceB) {
			this.id = id;
			this.serviceA = serviceA;
			this.serviceB = serviceB;
		}
	}

	static public class Location {
		public int[] machines;
	}

	private static class Neighborhood {
		int[] machines;
	}

	Resource[] resources; // [resources.length] // max: 20

	Machine[] machines; // [machines.length] // max: 5000

	private Service[] services; // [services.length] // max: 5000
	private int maxNumProcessesInService;

	Process[] processes; // [processes.length] // max: 50000

	public Balance[] balances; // [numBalances] // max: 10

	private ArrayList<Dependency> dependencies = new ArrayList<Dependency>(); // [numDependencies]

	private IntList servicesWithDependencies = new IntArrayList();
	private IntList servicesWithRevDependencies = new IntArrayList();

	private final long processMoveCostWeight; // uint32
	private final long serviceMoveCostWeight; // uint32
	private final long machineMoveCostWeight; // uint32

	private Location[] locations; // [numLocations], max: 5000 (machines limit). This array has at most machines.length elements in total. // max: 1000
	private Neighborhood[] neigborhoods; // [numNeighborhoods], max: 5000 (machines limit). This array has at most machines.length elements in total. // max: 1000

	private final ReadOnlySolution originalSolution;
	private final long originalFitness;
	private int firstTransient;
	private int standardCount;
	private int[] resourceMap;

	private String name;
	private Random[] random;
	private long seed;

	/**
	 * @return Number of the first transient resource in the resources array
	 *         [firstTransient...length)
	 */
	public int getFirstTransientResource() {
		return firstTransient;
	}

	/**
	 * @return Number of standard resources in the resources array
	 *         [0...standardCount-)
	 */
	public int getNumStandardResources() {
		return standardCount;
	}

	public String getName() {
		return name;
	}

	public int getNumDependencies() {
		return dependencies.size();
	}

	public Dependency getDependency(int d) {
		return dependencies.get(d);
	}

	public int getNumResources() {
		return resources.length;
	}

	public Location getLocation(int location) {
		return locations[location];
	}

	public Resource getResource(int resource) {
		return resources[resource];
	}

	public int getNumMachines() {
		return machines.length;
	}

	public Machine getMachine(int machine) {
		return machines[machine];
	}

	public int getNumServices() {
		return services.length;
	}

	public Service getService(int service) {
		return services[service];
	}

	public int[] getProcessesOfService(int service) {
		return services[service].processes;
	}

	public int getNumProcesses() {
		return processes.length;
	}

	public Process getProcess(int process) {
		return processes[process];
	}

	public int getNumBalances() {
		return balances.length;
	}

	public Balance getBalance(int balanceId) {
		return balances[balanceId];
	}

	public long getProcessMoveCostWeight() {
		return processMoveCostWeight;
	}

	public long getServiceMoveCostWeight() {
		return serviceMoveCostWeight;
	}

	public long getMachineMoveCostWeight() {
		return machineMoveCostWeight;
	}

	public int getNumLocations() {
		return locations.length;
	}

	public int getNumNeighborhoods() {
		return neigborhoods.length;
	}

	public int[] getMachinesInLocation(int location) {
		return locations[location].machines;
	}

	public int[] getMachinesInNeigborhood(int neigborhood) {
		return neigborhoods[neigborhood].machines;
	}

	public ImmutableSolution getOriginalSolution() {
		return originalSolution;
	}

	public Random getRandom() {
		return getRandom(0);
	}

	public Random getRandom(int thread) {
		return random[thread % random.length];
	}

	public long getOriginalFitness() {
		return originalFitness;
	}

	public int getMaxNumProcessesInService() {
		return maxNumProcessesInService;
	}

	public IntList getServicesWithRevDependencies() {
		return servicesWithRevDependencies;
	}

	public IntList getServicesWithDependencies() {
		return servicesWithDependencies;
	}

	/**
	 * 
	 * @param problemFileName
	 */
	public Problem(File problemFile, File originalSolutionFile) {
		//TODO/FIXME: We should use a better random (Mersenite Tweester)
		//TODO: Random should go away from this class
		random = new Random[MAX_NUM_THREADS];
		random[0] = new Random(getSeed());
		random[1] = new Random(getSeed() + 1);

		name = problemFile.getName().replace("model_", "").replace(".txt", "");

		//		Scanner scanner;
		//		try {
		//			scanner = new Scanner(problemFile);
		//		} catch (FileNotFoundException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//			scanner = scanner2;
		//		}
		QuickScanner scanner = new QuickScanner(problemFile);

		Runtime ra = Runtime.getRuntime();
		ra.gc();

		int numResources = scanner.nextInt();
		resources = new Resource[numResources];
		for (int r = 0; r < resources.length; r++) {
			boolean isTransient = (scanner.nextInt() == 1);
			long loadCostWeight = scanner.nextLong();
			resources[r] = new Resource(isTransient, loadCostWeight);
		}

		orderResources();

		int numMachines = scanner.nextInt();
		machines = new Machine[numMachines];
		for (int m = 0; m < machines.length; m++) {
			int neighborhood = scanner.nextInt();
			int location = scanner.nextInt();
			long[] capacities = new long[resources.length];
			for (int r = 0; r < resources.length; r++)
				capacities[resourceMap[r]] = scanner.nextLong();
			long[] safetyCapacities = new long[resources.length];
			for (int r = 0; r < resources.length; r++)
				safetyCapacities[resourceMap[r]] = scanner.nextLong();
			long[] moveCosts = new long[machines.length];

			long minMoveCost = Long.MAX_VALUE;
			for (int m2 = 0; m2 < machines.length; m2++) {
				moveCosts[m2] = scanner.nextLong();
				if (m2 != m) {
					minMoveCost = Math.min(minMoveCost, moveCosts[m2]);
				}
			}

			machines[m] = new Machine(m, location, neighborhood, capacities, safetyCapacities, moveCosts, minMoveCost);
		}
		remapLocationsNeighborhoods();

		int numServices = scanner.nextInt();
		services = new Service[numServices];
		int did = 0;
		for (int s = 0; s < services.length; s++) {
			services[s] = new Service();
			services[s].spread = scanner.nextInt();
			services[s].numDependencies = scanner.nextInt();
			services[s].dependencies = new int[services[s].numDependencies];
			services[s].dependencyIds = new int[services[s].numDependencies];
			for (int d = 0; d < services[s].numDependencies; d++) {
				services[s].dependencies[d] = scanner.nextInt();
				services[s].dependencyIds[d] = did;
				dependencies.add(new Dependency(did, s, services[s].dependencies[d]));
				did++;
			}
			if (services[s].numDependencies > 0) {
				servicesWithDependencies.add(s);
			}

			services[s].numRevDependencies = 0;
		}
		Collections.sort(servicesWithDependencies, new Comparator<Integer>() {
			@Override
			public int compare(Integer s1, Integer s2) {
				return services[s1].numDependencies - services[s2].numDependencies;
			}
		});

		for (int s = 0; s < services.length; s++) {
			for (int d = 0; d < services[s].numDependencies; d++) {
				services[services[s].dependencies[d]].numRevDependencies++;
			}
		}
		for (int s = 0; s < services.length; s++) {
			services[s].revDependencies = new int[services[s].numRevDependencies];
			services[s].revDependencyIds = new int[services[s].numRevDependencies];
			if (services[s].numRevDependencies > 0) {
				servicesWithRevDependencies.add(s);
			}
		}
		int[] servicesRevDepsIndex = new int[services.length];
		for (int s = 0; s < services.length; s++) {
			for (int d = 0; d < services[s].numDependencies; d++) {
				int depService = services[s].dependencies[d];
				int index = servicesRevDepsIndex[depService];
				services[depService].revDependencies[index] = s;
				services[depService].revDependencyIds[index] = services[s].dependencyIds[d];
				servicesRevDepsIndex[depService]++;
			}
		}
		Collections.sort(servicesWithRevDependencies, new Comparator<Integer>() {
			@Override
			public int compare(Integer s1, Integer s2) {
				return services[s1].numRevDependencies - services[s2].numRevDependencies;
			}
		});

		int numProcesses = scanner.nextInt();
		processes = new Process[numProcesses];
		for (int p = 0; p < processes.length; p++) {
			int service = scanner.nextInt();
			long[] requirements = new long[resources.length];
			for (int r = 0; r < resources.length; r++)
				requirements[resourceMap[r]] = scanner.nextLong();
			long moveCost = scanner.nextLong();
			processes[p] = new Process(p, service, requirements, moveCost);
		}
		groupProcessesByService();

		int numBalances = scanner.nextInt();
		balances = new Balance[numBalances];
		for (int b = 0; b < balances.length; b++) {
			balances[b] = new Balance();
			balances[b].r1 = resourceMap[scanner.nextInt()];
			balances[b].r2 = resourceMap[scanner.nextInt()];
			balances[b].target = scanner.nextLong();
			balances[b].weight = scanner.nextLong();
		}

		processMoveCostWeight = scanner.nextInt();
		serviceMoveCostWeight = scanner.nextInt();
		machineMoveCostWeight = scanner.nextInt();

		scanner.close();

		computeResourcesStatistics();

		originalSolution = SolutionIO.readSolutionFromFile(this, originalSolutionFile);
		originalFitness = evaluateSolution(originalSolution);

	}

	private void computeResourcesStatistics() {
		for (int r = 0; r < resources.length; ++r) {
			resources[r].minProcessRequirement = processes[0].requirements[r];
			resources[r].maxProcessRequirement = processes[0].requirements[r];
			for (Process p : processes) {
				resources[r].minProcessRequirement = Math.min(resources[r].minProcessRequirement, p.requirements[r]);
				resources[r].maxProcessRequirement = Math.max(resources[r].maxProcessRequirement, p.requirements[r]);
			}
		}
	}

	private long getSeed() {
		return seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	private void orderResources() {
		Resource oldResources[] = resources.clone();
		resourceMap = new int[resources.length];
		standardCount = 0;
		firstTransient = resources.length;
		for (int r = 0; r < resources.length; ++r) {
			if (oldResources[r].isTransient) {
				resources[--firstTransient] = oldResources[r];
				resourceMap[r] = firstTransient;
			} else {
				resources[standardCount] = oldResources[r];
				resourceMap[r] = standardCount;
				standardCount += 1;
			}
		}
	}

	private void groupProcessesByService() {
		int[] numProcessesOfService = new int[services.length];
		for (int p = 0; p < processes.length; p++)
			numProcessesOfService[processes[p].service]++;

		for (int s = 0; s < services.length; s++)
			if (maxNumProcessesInService < numProcessesOfService[s])
				maxNumProcessesInService = numProcessesOfService[s];

		for (int s = 0; s < services.length; s++)
			services[s].processes = new int[numProcessesOfService[s]];

		int[] servicesPositions = new int[services.length];
		for (int p = 0; p < processes.length; p++) {
			int service = processes[p].service;
			services[service].processes[servicesPositions[service]++] = p;
		}
	}

	private void remapLocationsNeighborhoods() {
		Map<Integer, Integer> locationMap = new HashMap<Integer, Integer>();
		Map<Integer, Integer> neighborhoodMap = new HashMap<Integer, Integer>();

		for (int m = 0; m < machines.length; m++) {
			if (!locationMap.containsKey(machines[m].location))
				locationMap.put(machines[m].location, locationMap.size());
			if (!neighborhoodMap.containsKey(machines[m].neighborhood))
				neighborhoodMap.put(machines[m].neighborhood, neighborhoodMap.size());
		}

		int numLocations = locationMap.size();
		locations = new Location[numLocations];
		int numNeighborhoods = neighborhoodMap.size();
		neigborhoods = new Neighborhood[numNeighborhoods];

		int[] machinesInLocations = new int[locations.length];
		int[] machinesInNeigborhoods = new int[neigborhoods.length];
		for (int m = 0; m < machines.length; m++) {
			machinesInLocations[locationMap.get(machines[m].location)]++;
			machinesInNeigborhoods[neighborhoodMap.get(machines[m].neighborhood)]++;

			machines[m] = machines[m].cloneToLocationAndNeighborhood(locationMap.get(machines[m].location),
					neighborhoodMap.get(machines[m].neighborhood));
		}

		for (int loc = 0; loc < locations.length; loc++) {
			locations[loc] = new Location();
			locations[loc].machines = new int[machinesInLocations[loc]];
		}

		int[] locationPositions = new int[numLocations];
		for (int m = 0; m < machines.length; m++) {
			int location = machines[m].location;
			locations[location].machines[locationPositions[location]++] = m;
		}

		for (int n = 0; n < neigborhoods.length; n++) {
			neigborhoods[n] = new Neighborhood();
			neigborhoods[n].machines = new int[machinesInNeigborhoods[n]];
		}

		int[] neigborhoodPositions = new int[numNeighborhoods];
		for (int m = 0; m < machines.length; m++) {
			int neigborhood = machines[m].neighborhood;
			neigborhoods[neigborhood].machines[neigborhoodPositions[neigborhood]++] = m;
		}
	}

	public long evaluateSolution(ImmutableSolution solution) {
		return computeLoadCost(solution) + computeBalanceCost(solution) + computeProcessMoveCost(solution)
				+ computeServiceMoveCost(solution) + computeMachineMoveCost(solution);
	}

	// O(p)
	public long computeMachineMoveCost(ImmutableSolution solution) {
		return machineMoveCostWeight * computeMachineMoveCostNotWeighted(solution);
	}

	public long computeMachineMoveCostNotWeighted(ImmutableSolution solution) {
		int[] originalAssignment = originalSolution.getAssignment();
		int[] assignment = solution.getAssignment();

		long machineMoveCost = 0;
		for (int p = 0; p < processes.length; p++) {
			machineMoveCost += machines[originalAssignment[p]].moveCosts[assignment[p]];
		}
		return machineMoveCost;
	}

	// O(p)
	public long computeServiceMoveCost(ImmutableSolution solution) {
		return serviceMoveCostWeight * computeServiceMoveCostNotWeighted(solution);
	}

	long computeServiceMoveCostNotWeighted(ImmutableSolution solution) {
		int serviceMoveCost = 0;
		for (int s = 0; s < services.length; s++) {
			int numMovedProcesses = computeNumMovedProcessesOfService(s, solution);
			serviceMoveCost = Math.max(serviceMoveCost, numMovedProcesses);
		}
		return serviceMoveCost;
	}

	public int computeNumMovedProcessesOfService(int serviceId, ImmutableSolution solution) {
		int numMovedProcesses = 0;
		for (int p : services[serviceId].processes)
			if (originalSolution.getMachine(p) != solution.getMachine(p))
				numMovedProcesses++;
		return numMovedProcesses;
	}

	// O(p)
	public long computeProcessMoveCost(ImmutableSolution solution) {
		return processMoveCostWeight * computeProcessMoveCostNotWeighted(solution);
	}

	long computeProcessMoveCostNotWeighted(ImmutableSolution solution) {
		int[] originalAssignment = originalSolution.getAssignment();
		int[] assignment = solution.getAssignment();

		long processMoveCost = 0;
		for (int p = 0; p < processes.length; p++) {
			if (originalAssignment[p] != assignment[p]) {
				processMoveCost += processes[p].moveCost;
			}
		}
		long x = processMoveCost;
		return x;
	}

	// O(r * m)
	public long computeLoadCost(ImmutableSolution solution) {
		long weightedLoadCost = 0;
		for (int r = 0; r < resources.length; r++) {
			long loadCost = computeLoadCostNotWeighted(solution, r);
			weightedLoadCost += resources[r].loadCostWeight * loadCost;
		}
		return weightedLoadCost;
	}

	private long computeLoadCostNotWeighted(ImmutableSolution solution, int resource) {
		long loadCost = 0;
		for (int m = 0; m < machines.length; m++) {
			loadCost += computeLoadCostNotWeighted(solution.getResourceUsage(m, resource), machines[m].safetyCapacities[resource]);
		}
		return loadCost;
	}

	public long computeLoadCost(ImmutableSolution solution, int machineId, int resourceId) {
		return computeLoadCostNotWeighted(solution.getResourceUsage(machineId, resourceId),
				machines[machineId].safetyCapacities[resourceId]) * resources[resourceId].loadCostWeight;
	}

	public long computeLoadCostNotWeighted(long resourceUsage, long safetyCapacity) {
		return Math.max(resourceUsage - safetyCapacity, 0);
	}

	// O(b * m)
	public long computeBalanceCost(ImmutableSolution solution) {
		long balanceCost = 0;
		for (Balance b : balances) {
			for (int m = 0; m < machines.length; m++)
				balanceCost += computeBalanceCost(solution, b, machines[m]);
		}
		return balanceCost;
	}

	public long computeBalanceCost(ImmutableSolution solution, Balance balance, Machine machine) {
		long available1 = computeAvailableResources(solution, machine.id, balance.r1);
		long available2 = computeAvailableResources(solution, machine.id, balance.r2);
		return computeBalanceCost(balance, available1, available2);
	}

	public long computeBalanceCost(Balance b, long available1, long available2) {
		return Math.max(computeBalanceValue(b.target, available1, available2), 0) * b.weight;
	}

	public long computeBalanceValue(long target, long available1, long available2) {
		return target * available1 - available2;
	}

	public long computeAvailableResources(ImmutableSolution solution, Machine machine, int resourceId) {
		return computeAvailableResources(solution, machine.id, resourceId);
	}

	public long computeAvailableResources(ImmutableSolution solution, int machineId, int resourceId) {
		return machines[machineId].capacities[resourceId] - solution.getResourceUsage(machineId, resourceId);
	}

	public boolean isSolutionFeasible(ImmutableSolution solution) {
		return checkCapacityConstraints(solution) && checkTransientUsageConstraints(solution) && checkServiceConflicts(solution)
				&& checkSpreadConstraints(solution) && checkDependencies(solution);
	}

	// O(p + max(d,s,n)) 
	// If service s_a depends on service s_b, then each process of s_a should run in the neighborhood of a s_b process
	public boolean checkDependencies(ImmutableSolution solution) {
		int[] ass = solution.getAssignment();

		@SuppressWarnings("unchecked")
		HashSet<Integer> serviceNeighborhoods[] = (HashSet<Integer>[]) new HashSet[services.length];
		for (int s = 0; s < services.length; ++s)
			serviceNeighborhoods[s] = new HashSet<Integer>();

		for (int p = 0; p < processes.length; ++p) {
			int service = processes[p].service;
			int neighborhood = machines[ass[p]].neighborhood;
			serviceNeighborhoods[service].add(neighborhood);
		}

		for (int sa = 0; sa < services.length; ++sa) {
			for (int sb : services[sa].dependencies) {
				for (int pa : services[sa].processes) {
					int na = machines[ass[pa]].neighborhood;
					if (!serviceNeighborhoods[sb].contains(na)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	// O (s + p)
	public boolean checkSpreadConstraints(ImmutableSolution solution) {
		int[] assignment = solution.getAssignment();
		Set<Integer> distinctLocations = new HashSet<Integer>();

		for (int s = 0; s < services.length; s++) {
			distinctLocations.clear();
			for (int p : services[s].processes) {
				distinctLocations.add(machines[assignment[p]].location);
			}

			if (distinctLocations.size() < services[s].spread) {
				return false;
			}
		}

		return true;
	}

	// O (s + p)
	// Each process in a given service must be on a different machine
	public boolean checkServiceConflicts(ImmutableSolution solution) {
		int[] assignment = solution.getAssignment();
		Set<Integer> distinctMachines = new HashSet<Integer>();

		for (int s = 0; s < services.length; s++) {
			distinctMachines.clear();
			for (int p : services[s].processes) {
				int m = assignment[p];
				if (!distinctMachines.add(m)) {
					return false;
				}
			}
		}

		return true;
	}

	// O(m*r)
	public boolean checkTransientUsageConstraints(ImmutableSolution solution) {
		for (int m = 0; m < machines.length; m++)
			if (!checkTransientUsageConstraint(solution, m)) {
				return false;
			}
		return true;
	}

	public boolean checkTransientUsageConstraint(ImmutableSolution solution, int machineId) {
		for (int r = firstTransient; r < resources.length; r++)
			if (solution.getTransientUsage(machineId, r) > machines[machineId].capacities[r]) {
				return false;
			}
		return true;
	}

	// O(m*r)
	public boolean checkCapacityConstraints(ImmutableSolution solution) {
		for (int m = 0; m < machines.length; m++)
			if (!checkCapacityConstraint(solution, m)) {
				return false;
			}
		return true;
	}

	public boolean checkCapacityConstraint(ImmutableSolution solution, int machineId) {
		for (int r = 0; r < resources.length; r++) { // TODO: BW: czy tu nie wystarczy sprawdzac do r < firstTransient ??
			if (solution.getResourceUsage(machineId, r) > machines[machineId].capacities[r]) {
				return false;
			}
		}
		return true;
	}

	// TODO: Refactoring needed...
	// O(p * r)
	// Compute resources usage
	public long[][] computeResourceUsage(ImmutableSolution solution) {
		long[][] usage = new long[machines.length][resources.length];
		computeResourceUsage(solution, usage);
		return usage;
	}

	public void computeResourceUsage(ImmutableSolution solution, long[][] usage) {
		int[] assignment = solution.getAssignment();
		for (int p = 0; p < processes.length; p++) {
			for (int r = 0; r < resources.length; r++) {
				usage[assignment[p]][r] += processes[p].requirements[r];
			}
		}
	}

	//TODO Remove circular dependency between Problem and Solution.. 
	public long[][] computeTransientResourceUsage(ImmutableSolution solution) {
		if (originalSolution != null) {
			return computeTransientResourceUsage(solution, originalSolution);
		} else {
			return computeTransientResourceUsage(solution, solution);
		}
	}

	public void computeTransientResourceUsage(ImmutableSolution solution, long[][] transientResourceUsage) {
		if (originalSolution != null) {
			computeTransientResourceUsage(solution, originalSolution, transientResourceUsage);
		} else {
			computeTransientResourceUsage(solution, solution, transientResourceUsage);
		}
	}

	// O(p * r)
	// Compute transient resources usage
	public long[][] computeTransientResourceUsage(ImmutableSolution solution, ImmutableSolution originalSolution) {
		long[][] transientResourceUsage = new long[machines.length][resources.length];
		computeTransientResourceUsage(solution, originalSolution, transientResourceUsage);

		return transientResourceUsage;
	}

	public void computeTransientResourceUsage(ImmutableSolution solution, ImmutableSolution originalSolution,
			long[][] transientResourceUsage) {
		int[] assignment = solution.getAssignment();
		int[] originalAssignment = originalSolution.getAssignment();
		for (int p = 0; p < processes.length; p++) {
			boolean isProcessMoved = assignment[p] != originalAssignment[p];
			for (int r = firstTransient; r < resources.length; r++) {
				transientResourceUsage[assignment[p]][r] += processes[p].requirements[r];
				if (isProcessMoved)
					transientResourceUsage[originalAssignment[p]][r] += processes[p].requirements[r];
			}
		}
	}

	public int[] getTransitiveDependencies(int service) {
		if (services[service].transitiveDependencies == null) {
			services[service].transitiveDependencies = findTransitiveDependencies(service, false);
		}

		return services[service].transitiveDependencies;
	}

	public int[] getTransitiveRevDependencies(int service) {
		if (services[service].transitiveRevDependencies == null) {
			services[service].transitiveRevDependencies = findTransitiveDependencies(service, true);
		}

		return services[service].transitiveRevDependencies;
	}

	public long getLowerBound() {
		return getLoadCostLowerBound() + getBalanceCostLowerBound() + 0;
	}

	private long getTotalUsage(int resourceId) {
		long sum = 0;
		for (int m = 0; m < machines.length; ++m)
			sum += originalSolution.getResourceUsage(m, resourceId);
		return sum;
	}

	private long getTotalCapacity(int resourceId) {
		long sum = 0;
		for (int m = 0; m < machines.length; ++m)
			sum += machines[m].capacities[resourceId];
		return sum;
	}

	private long getTotalSafetyCapacity(int resourceId) {
		long sum = 0;
		for (int m = 0; m < machines.length; ++m)
			sum += machines[m].safetyCapacities[resourceId];
		return sum;
	}

	private long getLoadCostLowerBound() {
		long sum = 0;
		for (int r = 0; r < resources.length; ++r)
			sum += resources[r].loadCostWeight * (Math.max(getTotalUsage(r) - getTotalSafetyCapacity(r), 0));
		return sum;
	}

	private long getBalanceCostLowerBound() {
		long sum = 0;
		for (Balance b : balances) {
			long totalAvailR1 = getTotalCapacity(b.r1) - getTotalUsage(b.r1);
			long totalAvailR2 = getTotalCapacity(b.r2) - getTotalUsage(b.r2);
			sum += b.weight * Math.max(b.target * totalAvailR1 - totalAvailR2, 0);
		}
		return sum;
	}	

	//Simple BFS through the dependencies graph
	private int[] findTransitiveDependencies(int service, boolean reverse) {
		boolean[] visited = new boolean[services.length];
		IntPriorityQueue queue = new IntArrayFIFOQueue();
		IntList list = new IntArrayList();
		queue.enqueue(service);

		while (!queue.isEmpty()) {
			Service s = services[queue.dequeueInt()];
			int[] dependencies = (reverse) ? s.revDependencies : s.dependencies;
			for (int dep : dependencies) {
				if (!visited[dep]) {
					visited[dep] = true;
					queue.enqueue(dep);
					list.add(dep);
				}
			}
		}

		return list.toIntArray();
	}
}