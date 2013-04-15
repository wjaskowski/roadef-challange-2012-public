package put.roadef.bnb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import put.roadef.Problem;
import put.roadef.Problem.Machine;
import put.roadef.Problem.Process;
import put.roadef.Problem.Service;

public class ServiceConstraints implements IncrementalSolutionConstraints {

	private static Problem problem;

	/**
	 * Stores sorted collection of unassigned processes grouped by service
	 */
	private List<Set<Integer>> unassignedProcessesOfService = new ArrayList<Set<Integer>>();

	/**
	 * Counts how many processes of a given service are in a specified location
	 */
	private List<Map<Integer, Integer>> locationsOfServices = new ArrayList<Map<Integer, Integer>>();

	/**
	 * Stores machines occupied by processes of a given service
	 */
	private List<Set<Integer>> machinesOfServices = new ArrayList<Set<Integer>>();

	/**
	 * Counts how many processes in a given neighborhood belong to specified
	 * service
	 */
	private List<Map<Integer, Integer>> neighborhoodsOfServices = new ArrayList<Map<Integer, Integer>>();

	/**
	 * Counts how many processes need the service in a given neighborhood
	 */
	private List<Map<Integer, Integer>> neighborhoodsWhereServiceIsNeeded = new ArrayList<Map<Integer, Integer>>();

	/**
	 * Stores all neighborhoods where given service is still needed (there is
	 * more than 1 request for it)
	 */
	private static List<Set<Integer>> neighborhoodsWhereServiceIsStillNeeded = new ArrayList<Set<Integer>>();

	public ServiceConstraints(Problem problem) {
		if (ServiceConstraints.problem == null || ServiceConstraints.problem != problem) {
			ServiceConstraints.problem = problem;
			initialize();
		}
		clear();

		initialize();
		for (int p = 0; p < problem.getNumProcesses(); p++) {
			Process process = problem.getProcess(p);
			unassignedProcessesOfService.get(process.service).add(p);
		}
	}

	public ServiceConstraints(Problem problem, int[] assignment) {
		if (ServiceConstraints.problem == null || ServiceConstraints.problem != problem) {
			ServiceConstraints.problem = problem;
			initialize();
		}
		clear();

		initialize();

		for (int p = 0; p < problem.getNumProcesses(); p++) {
			int m = assignment[p];
			int l = problem.getMachine(m).location;
			int n = problem.getMachine(m).neighborhood;
			int s = problem.getProcess(p).service;

			machinesOfServices.get(s).add(m);
			incrementMapValue(locationsOfServices.get(s), l);
			incrementMapValue(neighborhoodsOfServices.get(s), n);
		}

		for (int d = 0; d < problem.getNumDependencies(); d++) {
			int sa = problem.getDependency(d).serviceA;
			int sb = problem.getDependency(d).serviceB;

			for (int n : neighborhoodsOfServices.get(sa).keySet()) {
				int processes = neighborhoodsOfServices.get(sa).get(n);
				incrementMapValue(neighborhoodsWhereServiceIsNeeded.get(sb), n, processes);
			}
		}
	}

	private void initialize() {
		int oldNumServices = machinesOfServices.size();
		for (int s = oldNumServices; s < problem.getNumServices(); s++) {
			machinesOfServices.add(new HashSet<Integer>());
			locationsOfServices.add(new HashMap<Integer, Integer>());
			neighborhoodsOfServices.add(new HashMap<Integer, Integer>());
			unassignedProcessesOfService.add(new HashSet<Integer>());
			neighborhoodsWhereServiceIsNeeded.add(new HashMap<Integer, Integer>());
			neighborhoodsWhereServiceIsStillNeeded.add(new HashSet<Integer>());
		}
	}

	private void clear() {
		for (int s = 0; s < problem.getNumServices(); s++) {
			machinesOfServices.get(s).clear();
			locationsOfServices.get(s).clear();
			neighborhoodsOfServices.get(s).clear();
			unassignedProcessesOfService.get(s).clear();
			neighborhoodsWhereServiceIsNeeded.get(s).clear();
			neighborhoodsWhereServiceIsStillNeeded.get(s).clear();
		}
	}

	@Override
	public void addAssignment(Process process, Machine machine) {
		int service = process.service;
		int neighborhood = machine.neighborhood;

		unassignedProcessesOfService.get(service).remove(process.id);

		incrementMapValue(locationsOfServices.get(service), machine.location);

		machinesOfServices.get(service).add(machine.id);

		incrementMapValue(neighborhoodsOfServices.get(service), neighborhood);

		for (int depService : problem.getService(service).dependencies) {
			incrementMapValue(neighborhoodsWhereServiceIsNeeded.get(depService), neighborhood);
			if (!neighborhoodsOfServices.get(depService).containsKey(neighborhood))
				neighborhoodsWhereServiceIsStillNeeded.get(depService).add(neighborhood);
		}

		neighborhoodsWhereServiceIsStillNeeded.get(service).remove(neighborhood);
	}

	/**
	 * Increments value associated with the given key in the map
	 * 
	 */
	private void incrementMapValue(Map<Integer, Integer> map, int key) {
		incrementMapValue(map, key, 1);
	}

	private void incrementMapValue(Map<Integer, Integer> map, int key, int value) {
		int count = map.containsKey(key) ? map.get(key) : 0;
		map.put(key, count + value);
	}

	@Override
	public void removeAssignment(Process process, Machine machine) {
		int service = process.service;
		int neighborhood = machine.neighborhood;

		unassignedProcessesOfService.get(service).add(process.id);

		decrementMapValue(locationsOfServices.get(service), machine.location);

		machinesOfServices.get(service).remove(machine.id);

		decrementMapValue(neighborhoodsOfServices.get(service), neighborhood);

		for (int depService : problem.getService(service).dependencies) {
			decrementMapValue(neighborhoodsWhereServiceIsNeeded.get(depService), neighborhood);
			if (!neighborhoodsWhereServiceIsNeeded.get(depService).containsKey(neighborhood)) {
				neighborhoodsWhereServiceIsStillNeeded.get(depService).remove(neighborhood);
			}
		}

		if (!neighborhoodsOfServices.get(service).containsKey(neighborhood)
				&& neighborhoodsWhereServiceIsNeeded.get(service).containsKey(neighborhood)) {
			neighborhoodsWhereServiceIsStillNeeded.get(service).add(neighborhood);
		}
	}

	/**
	 * Decrements value associated with the given key in the map
	 */
	private void decrementMapValue(Map<Integer, Integer> map, int key) {
		int count = map.get(key);
		if (count == 1) {
			map.remove(key);
		} else {
			map.put(key, count - 1);
		}
	}

	@Override
	public boolean checkFutureConstraints(Process process, Machine machine) {
		return checkFutureConflicts(process, machine) && checkFutureServiceSpread(process, machine)
				&& checkFutureDependencies(process, machine);
	}

	public boolean checkFutureConflicts(Process process, Machine machine) {
		return !machinesOfServices.get(process.service).contains(machine.id);
	}

	public boolean checkFutureServiceSpread(Process process, Machine machine) {
		Map<Integer, Integer> serviceLocations = locationsOfServices.get(process.service);
		Service service = problem.getService(process.service);
		int spreadNeeded = service.spread - serviceLocations.size();

		// spread will not increase after this assignment
		if (spreadNeeded > 0 && serviceLocations.containsKey(machine.location)) {
			//unassigned processes should be distributed among different locations
			if (unassignedProcessesOfService.get(process.service).size() == spreadNeeded) {
				return false;
			}
		}

		return true;
	}

	public boolean checkFutureDependencies(Process process, Machine machine) {
		Service service = problem.getService(process.service);

		// if there is already a process of the same service in this neighborhood
		// no new dependencies are implied by putting this process here 
		if (!neighborhoodsOfServices.get(process.service).containsKey(machine.neighborhood)) {
			//otherwise we check each dependency that should be also placed in this neighborhood
			for (int depService : service.dependencies) {

				// this service already assigned in this neighborhood - dependency satisfied
				// or even without this process, serviceNeeded has to be placed here
				if (neighborhoodsOfServices.get(depService).containsKey(machine.neighborhood)
						|| serviceNeeded(depService, machine.neighborhood)) {
					continue;
				}

				// there are more needs than available processes of serviceNeeded
				if (!enoughProcessesLeft(depService)) {
					return false;
				}
			}
		}

		// this process is not necessarily needed here and there is not enough service processes left 
		if (!serviceNeeded(process.service, machine.neighborhood)
				&& !enoughProcessesLeft(process.service)) {
			return false;
		}

		return true;
	}

	private boolean serviceNeeded(int serviceId, int neighborhood) {
		return neighborhoodsWhereServiceIsStillNeeded.get(serviceId).contains(neighborhood);
	}

	private boolean enoughProcessesLeft(int serviceId) {
		return unassignedProcessesOfService.get(serviceId).size() > neighborhoodsWhereServiceIsStillNeeded
				.get(serviceId).size();
	}

	public long getCost() {
		return 0;
	}
}