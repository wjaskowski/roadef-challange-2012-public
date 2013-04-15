package put.roadef;

import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Random;

public class ProblemGenerator {
	public final int MAX_MACHINES = 5000;
	public final int MAX_RESOURCES = 20;
	public final int MAX_PROCESSES = 50000;
	public final int MAX_SERVICES = 50000;
	public final int MAX_NEIGHBORHOODS = 1000;
	public final int MAX_DEPENDENCIES = 5000;
	public final int MAX_LOCATIONS = 1000;
	public final int MAX_BALANCE_COSTS = 10;

	public static final class Resource {
		public boolean isTransient;
		public long loadCostWeight; //uint32		

		public Resource(boolean isTransient, long loadCostWeight) {
			this.isTransient = isTransient;
			this.loadCostWeight = loadCostWeight;
		}
	}

	public static final class Machine {
		public int id;
		public final int location; // range: <0,1000) (locations limit)
		public final int neighborhood; //range: <0,1000) (neighborhoods limit)
		public long[] capacities; // [resources.length] uint32
		public long[] safetyCapacities; // [resources.length] uint32
		public long[] moveCosts; // [machines.length] uint32

		public Machine(int id, int location, int neighborhood, long[] capacities, long[] safetyCapacities, long[] moveCosts) {
			this.id = id;
			this.location = location;
			this.neighborhood = neighborhood;
			this.capacities = capacities;
			this.safetyCapacities = safetyCapacities;
			this.moveCosts = moveCosts;
		}

		public Machine cloneToLocationAndNeighborhood(int location, int neighborhood) {
			return new Machine(this.id, location, neighborhood, this.capacities, this.safetyCapacities, this.moveCosts);
		}
	}

	public static final class Service {
		public int spread; // range: <0,1000) (locations limit)
		public int numDependencies; // range: <0,5000) (services limit)

		public int[] dependencies; // range: <0,5000) (services limit). There are at most 5000 dependencies in total.

		public int[] processes; // range: <0,50000) (processes limit). There are at most processes.length entries in total.
	}

	/**
	 * Immutable class representing process. Only Problem class can instantiate
	 * Processes.
	 * 
	 * @author marcin
	 * 
	 */
	public static final class Process implements Comparable<Process> {
		public int id;
		public final int service; // range: <0,5000) (services limit)
		public long[] requirements; // uint32
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

		public Balance() {
		}

		public Balance(int r1, int r2) {
			this.r1 = r1;
			this.r2 = r2;
		}
	}

	private Resource[] resources; // [resources.length] // max: 20

	private Machine[] machines; // [machines.length] // max: 5000

	private Service[] services; // [services.length] // max: 5000
	private int maxNumProcessesInService;

	private Process[] processes; // [processes.length] // max: 50000

	private Balance[] balances; // [numBalances] // max: 10

	private long processMoveCostWeight; // uint32
	private long serviceMoveCostWeight; // uint32
	private long machineMoveCostWeight; // uint32

	private int numNeighbourhoods;

	private int[] assignment;
	private long[][] resourceUsage; //[numMachines][numResources], unit32.	

	/**
	 * 
	 * @param problemFileName
	 */
	public ProblemGenerator(File problemFile, File originalSolutionFile) {
		String name = problemFile.getName().replace("model_", "").replace(".txt", "");
		System.out.println(name);

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

		//		orderResources();

		int numMachines = scanner.nextInt();
		machines = new Machine[numMachines];
		for (int m = 0; m < machines.length; m++) {
			int neighborhood = scanner.nextInt();
			int location = scanner.nextInt();

			numNeighbourhoods = Math.max(numNeighbourhoods, 1 + neighborhood);

			long[] capacities = new long[resources.length];
			for (int r = 0; r < resources.length; r++)
				capacities[r] = scanner.nextLong();
			long[] safetyCapacities = new long[resources.length];
			for (int r = 0; r < resources.length; r++)
				safetyCapacities[r] = scanner.nextLong();
			long[] moveCosts = new long[machines.length];

			for (int m2 = 0; m2 < machines.length; m2++) {
				moveCosts[m2] = scanner.nextLong();
			}

			machines[m] = new Machine(m, location, neighborhood, capacities, safetyCapacities, moveCosts);
		}
		//		remapLocationsNeighborhoods();

		int numServices = scanner.nextInt();
		services = new Service[numServices];
		for (int s = 0; s < services.length; s++) {
			services[s] = new Service();
			services[s].spread = scanner.nextInt();
			services[s].numDependencies = scanner.nextInt();
			services[s].dependencies = new int[services[s].numDependencies];
			for (int d = 0; d < services[s].numDependencies; d++) {
				services[s].dependencies[d] = scanner.nextInt();
			}
		}

		int numProcesses = scanner.nextInt();
		processes = new Process[numProcesses];
		for (int p = 0; p < processes.length; p++) {
			int service = scanner.nextInt();
			long[] requirements = new long[resources.length];
			for (int r = 0; r < resources.length; r++)
				requirements[r] = scanner.nextLong();
			long moveCost = scanner.nextLong();
			processes[p] = new Process(p, service, requirements, moveCost);
		}
		groupProcessesByService();

		int numBalances = scanner.nextInt();
		balances = new Balance[numBalances];
		for (int b = 0; b < balances.length; b++) {
			balances[b] = new Balance();
			balances[b].r1 = scanner.nextInt();
			balances[b].r2 = scanner.nextInt();
			balances[b].target = scanner.nextLong();
			balances[b].weight = scanner.nextLong();
		}

		processMoveCostWeight = scanner.nextInt();
		serviceMoveCostWeight = scanner.nextInt();
		machineMoveCostWeight = scanner.nextInt();

		scanner.close();

		//originalSolution = SolutionIO.readSolutionFromFile(this, originalSolutionFile);
		//originalFitness = evaluateSolution(originalSolution);

		scanner = new QuickScanner(originalSolutionFile);

		assignment = new int[processes.length];
		for (int p = 0; p < assignment.length; p++) {
			assignment[p] = scanner.nextInt();
		}

		resourceUsage = computeResourceUsage();
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

	public long[][] computeResourceUsage() {
		long[][] usage = new long[machines.length][resources.length];
		for (int p = 0; p < processes.length; p++) {
			for (int r = 0; r < resources.length; r++) {
				usage[assignment[p]][r] += processes[p].requirements[r];
			}
		}
		return usage;
	}

	public void saveProblem(File problemFile, File originalSolutionFile) {
		try {
			PrintWriter pw = new PrintWriter(problemFile);

			pw.println(resources.length);
			for (int r = 0; r < resources.length; r++) {
				pw.println((resources[r].isTransient ? "1" : "0") + " " + resources[r].loadCostWeight);
			}

			pw.println(machines.length);
			for (Machine m : machines) {
				pw.print(m.neighborhood + " " + m.location);
				for (long c : m.capacities)
					pw.print(" " + c);
				for (long sc : m.safetyCapacities)
					pw.print(" " + sc);
				for (long mc : m.moveCosts)
					pw.print(" " + mc);
				pw.println(" ");
			}

			pw.println(services.length);
			for (Service s : services) {
				pw.print(s.spread + " " + s.dependencies.length);
				for (int d : s.dependencies)
					pw.print(" " + d);
				pw.println(" ");
			}

			pw.println(processes.length);
			for (Process p : processes) {
				pw.print(p.service);
				for (long r : p.requirements)
					pw.print(" " + r);
				pw.println(" " + p.moveCost + " ");
			}

			pw.println(balances.length);
			for (Balance b : balances) {
				pw.println(b.r1 + " " + b.r2 + " " + b.target);
				pw.println(b.weight);
			}

			pw.println(processMoveCostWeight + " " + serviceMoveCostWeight + " " + machineMoveCostWeight);

			pw.close();

			FileWriter fstream;
			fstream = new FileWriter(originalSolutionFile);
			BufferedWriter out = new BufferedWriter(fstream);
			for (int p = 0; p < assignment.length; p++)
				out.write(assignment[p] + " ");
			out.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static Random random;

	double nextDouble(double min, double max) {
		return min + random.nextDouble() * (max - min);
	}

	int nextInt(int min, int max) {
		return min + random.nextInt(max - min + 1);
	}

	private void changeResourceLoadCost(final double minFactor, final double maxFactor) {
		for (Resource r : resources) {
			r.loadCostWeight *= nextDouble(minFactor, maxFactor);
		}
	}

	/** Flips isTransient for factor*numResources different resources */
	private void changeTransient(final double factor) {
		int count = (int) Math.max(1.0, resources.length * factor);

		int[] ids = new int[resources.length];
		for (int i = 0; i < ids.length; i++)
			ids[i] = i;
		MyArrayUtils.shuffle(ids, random);

		for (int i = 0; i < count; i++) {
			Resource r = resources[ids[i]];
			r.isTransient = !r.isTransient;
		}
	}

	private void changeMachineMoveCosts(final int minDelta, final int maxDelta) {
		for (Machine m : machines) {
			for (int c = 0; c < m.moveCosts.length; c++) {
				if (c == m.id)
					continue;

				m.moveCosts[c] += nextInt(minDelta, maxDelta);
				m.moveCosts[c] = Math.max(m.moveCosts[c], 0);
			}
		}
	}

	private void changeMachineSafetyCapacities(final double minFactor, final double maxFactor) {
		for (Machine m : machines) {
			for (int r = 0; r < m.safetyCapacities.length; r++) {
				m.safetyCapacities[r] *= nextDouble(minFactor, maxFactor);
				m.safetyCapacities[r] = Math.min(m.capacities[r], Math.max(0, m.safetyCapacities[r]));
			}
		}
	}

	private void changeMachineCapacities(final double minFactor, final double maxFactor) {
		//final double FACTOR = 0.1;	// +/- 10%
		for (Machine m : machines) {
			for (int r = 0; r < m.capacities.length; r++) {
				m.capacities[r] *= nextDouble(minFactor, maxFactor);
				m.capacities[r] = Math.max(resourceUsage[m.id][r], m.capacities[r]);
			}
		}
	}

	private void changeProcessRequirement(final double minFactor, final double maxFactor) {
		for (Process p : processes) {
			int machine = assignment[p.id];

			for (int r = 0; r < p.requirements.length; r++) {
				long old = p.requirements[r];

				p.requirements[r] *= nextDouble(minFactor, maxFactor);
				p.requirements[r] = Math.max(0, p.requirements[r]);
				p.requirements[r] = Math
						.min(p.requirements[r], machines[machine].capacities[r] - resourceUsage[machine][r] + old);

				resourceUsage[assignment[p.id]][r] -= old - p.requirements[r];
			}
		}
	}

	private void changeSpread(final double minFactor, final double maxFactor) {
		IntAVLTreeSet distinctLocations = new IntAVLTreeSet();

		for (int s = 0; s < services.length; s++) {
			distinctLocations.clear();
			for (int p : services[s].processes) {
				distinctLocations.add(machines[assignment[p]].location);
			}

			int maxspread = distinctLocations.size();

			int spread = (int) (services[s].spread * nextDouble(minFactor, maxFactor));
			spread += nextInt(-1, 1);
			services[s].spread = Math.max(0, Math.min(maxspread, spread));
		}
	}

	private void changeServiceDependencies(final double addFactor, final double removeFactor) {
		BitSet servicesInNeighbourhood[] = new BitSet[numNeighbourhoods];
		for (int s = 0; s < servicesInNeighbourhood.length; ++s) {
			servicesInNeighbourhood[s] = new BitSet();
		}

		IntAVLTreeSet serviceToNeighbourhoods[] = new IntAVLTreeSet[services.length];
		for (int s = 0; s < services.length; ++s)
			serviceToNeighbourhoods[s] = new IntAVLTreeSet();

		for (Process p : processes) {
			int n = machines[assignment[p.id]].neighborhood;
			int s = p.service;

			servicesInNeighbourhood[n].set(s);

			serviceToNeighbourhoods[s].add(n);
		}

		for (int sid = 0; sid < services.length; sid++) {
			IntAVLTreeSet neighbourhoods = serviceToNeighbourhoods[sid];

			BitSet intersection = null;
			for (int n : neighbourhoods) {
				if (intersection == null) {
					intersection = new BitSet();
					intersection.or(servicesInNeighbourhood[n]);
				} else
					intersection.and(servicesInNeighbourhood[n]);
			}

			// intersection - all possible services I can depend on

			for (int d : services[sid].dependencies)
				intersection.clear(d);

			intersection.clear(sid); // exclude itself
			// services we can add as new dependencies

			int[] list = new int[intersection.cardinality()];
			int i = 0;
			for (int q = intersection.nextSetBit(0); q >= 0; q = intersection.nextSetBit(q + 1)) {
				list[i++] = q;
			}
			MyArrayUtils.shuffle(list, random);

			int addCount = nextInt(0, 1 + (int) (addFactor * services[sid].dependencies.length));
			//addCount = 0;
			int removeCount = nextInt(0, 1 + (int) (addFactor * services[sid].dependencies.length));

			int[] newdependencies = org.apache.commons.lang.ArrayUtils.addAll(services[sid].dependencies,
					Arrays.copyOfRange(list, 0, Math.min(list.length, addCount)));
			MyArrayUtils.shuffle(newdependencies, random);

			services[sid].dependencies = Arrays
					.copyOfRange(newdependencies, 0, Math.max(0, newdependencies.length - removeCount));
		}
	}

	private void addBalances(int min, int max) {
		if (resources.length == 1)
			return;

		int count = nextInt(min, max);
		count = Math.min(count, MAX_BALANCE_COSTS);

		ArrayList<Balance> all = new ArrayList<ProblemGenerator.Balance>();
		for (int r1 = 0; r1 < resources.length - 1; r1++)
			for (int r2 = r1 + 1; r2 < resources.length; r2++)
				all.add(new Balance(r1, r2));

		ArrayList<Balance> newBalances = new ArrayList<ProblemGenerator.Balance>(Arrays.asList(balances));

		for (int i = 0; i < count && !all.isEmpty(); i++) {
			Balance b = null;
			while (true) {
				int idx = random.nextInt(all.size());

				b = all.get(idx);
				all.remove(idx);

				for (Balance nb : newBalances) {
					if ((nb.r1 == b.r1 && nb.r2 == b.r2) || (nb.r1 == b.r2 && nb.r2 == b.r1)) {
						b = null;
						break;
					}
				}

				if (b != null || all.isEmpty())
					break;
			}

			if (b != null) {
				if (random.nextBoolean()) {
					int tmp = b.r1;
					b.r1 = b.r2;
					b.r2 = tmp;
				}

				// arbitralne zakresy wartosci
				b.target = nextInt(1, 3);
				b.weight = nextInt(8, 12);

				newBalances.add(b);
			}
		}

		Collections.shuffle(newBalances, random);

		balances = newBalances.toArray(new Balance[newBalances.size()]);
	}

	private void reorderMachines() {
		int[] newIdx = new int[machines.length];
		for (int i = 0; i < newIdx.length; i++)
			newIdx[i] = i;
		MyArrayUtils.shuffle(newIdx, random);

		// change: 1. machines (+moveCosts), 2. assignement, 3. resourcesUsage

		Machine[] newMachines = new Machine[machines.length];
		for (int i = 0; i < newIdx.length; i++) {
			Machine m = machines[i];
			m.id = newIdx[i];

			long[] newMoveCosts = new long[m.moveCosts.length];
			for (int j = 0; j < newMoveCosts.length; j++) {
				newMoveCosts[newIdx[j]] = m.moveCosts[j];
			}
			m.moveCosts = newMoveCosts;

			newMachines[newIdx[i]] = m;
		}
		machines = newMachines;

		for (int i = 0; i < assignment.length; i++) {
			assignment[i] = newIdx[assignment[i]];
		}

		long[][] newResourceUsage = new long[resourceUsage.length][];
		for (int i = 0; i < newIdx.length; i++) {
			newResourceUsage[newIdx[i]] = resourceUsage[i];
		}
		resourceUsage = newResourceUsage;
	}

	private void reorderProcesses() {
		int[] newIdx = new int[processes.length];
		for (int i = 0; i < newIdx.length; i++)
			newIdx[i] = i;
		MyArrayUtils.shuffle(newIdx, random);

		// change: 1. processes 2. assignement, 3. service.processes

		Process[] newProcesses = new Process[processes.length];
		for (int i = 0; i < newIdx.length; i++) {
			Process p = processes[i];
			p.id = newIdx[i];

			newProcesses[newIdx[i]] = processes[i];
		}
		processes = newProcesses;

		int[] newAssignement = new int[assignment.length];
		for (int i = 0; i < assignment.length; i++) {
			newAssignement[newIdx[i]] = assignment[i];
		}
		assignment = newAssignement;

		for (Service s : services) {
			for (int i = 0; i < s.processes.length; i++)
				s.processes[i] = newIdx[s.processes[i]];
		}
	}

	/*
	public void moveProcesses(double factor)
	{
		int count = (int)(processes.length * nextDouble(0, factor));
		
		int[] idx = new int[processes.length];
		for (int i = 0; i < idx.length; i++)
			idx[i] = i;
		MyArrayUtils.shuffle(idx, random);
		
		for (int p : idx)
		{
			if (count == 0)
				break;
			
			for (int tries = 0; tries < 20; tries++)
			{
				int newMachine = random.nextInt(machines.length);
				
				check
				
				resourceUsage[assignment[p.id]][r] -= old - p.requirements[r];
			}
		}
	}
	*/

	private void addResources(int min, int max) {
		int count = nextInt(min, max);
		count = Math.min(count, MAX_RESOURCES);
		count = Math.max(count, resources.length);

		int[] newIdx = new int[count];
		Resource[] newResources = new Resource[count];
		for (int i = 0; i < resources.length; i++) {
			newResources[i] = resources[i];
			newIdx[i] = i;
		}

		for (int i = resources.length; i < count; i++) {
			int oldIdx = random.nextInt(resources.length);
			Resource old = resources[oldIdx];
			newResources[i] = new Resource(old.isTransient, old.loadCostWeight);

			newIdx[i] = oldIdx;
		}

		resources = newResources;

		for (Process p : processes) {
			long[] newRequirements = new long[count];
			for (int i = 0; i < count; i++) {
				newRequirements[i] = p.requirements[newIdx[i]];
			}
			p.requirements = newRequirements;
		}

		for (Machine m : machines) {
			long[] newCapacity = new long[count];
			long[] newSafetyCapacities = new long[count];
			for (int i = 0; i < count; i++) {
				newCapacity[i] = m.capacities[newIdx[i]];
				newSafetyCapacities[i] = m.safetyCapacities[newIdx[i]];
			}
			m.capacities = newCapacity;
			m.safetyCapacities = newSafetyCapacities;
		}

		resourceUsage = computeResourceUsage();
	}

	private void changeWeights(final double minFactor, final double maxFactor) {
		processMoveCostWeight *= nextDouble(minFactor, maxFactor);
		processMoveCostWeight = Math.max(1, processMoveCostWeight);
		serviceMoveCostWeight *= nextDouble(minFactor, maxFactor);
		serviceMoveCostWeight = Math.max(1, serviceMoveCostWeight);

		machineMoveCostWeight *= nextDouble(minFactor, maxFactor);
		machineMoveCostWeight = Math.max(1, machineMoveCostWeight);
	}

	public static void main(String[] args) {
		random = new Random(1234321);

		final String DATA_DIR = "data/B";
		final String OUTPUT_DIR = "data/E";

		File dir = new File(DATA_DIR);
		String[] children = dir.list();
		for (int i = 0; i < children.length; i++) {
			String fileName = children[i];
			//in this directory we have two types of files, so process the test only when we
			//read a model file
			if (fileName.startsWith("model") && !fileName.contains("test")) {
				String assignmentFile = fileName.replaceFirst("model", "assignment");
				String modelFile = fileName;

				ProblemGenerator pg = new ProblemGenerator(new File(DATA_DIR, modelFile), new File(DATA_DIR, assignmentFile));

				//pg.makeMoreResources(random.nextBoolean() ? 12 : 1);
				//pg.resourceUsage = pg.computeResourceUsage();

				pg.reorderMachines();
				pg.reorderProcesses();

				pg.addResources(18, 25); // 20 will be more likely

				pg.changeProcessRequirement(0.8, 1.2); // +/- 10%
				pg.changeMachineCapacities(0.8, 1.3); // +/- 10%

				pg.changeMachineSafetyCapacities(0.8, 1.2); // +/- 10%

				pg.changeMachineMoveCosts(-1, +1); // +/- 1
				pg.changeTransient(0.2); // 20%
				pg.changeResourceLoadCost(0.9, 1.1); // +/- 10%

				pg.changeSpread(0.6, 1.4); // +/- 40$

				pg.addBalances(1, 4); // add from 1 to 2 new balances

				pg.changeServiceDependencies(0.2, 0.2); // add up to 20%, remove max 20%

				pg.changeWeights(0.5, 5.0);

				File newModel = new File(OUTPUT_DIR, modelFile);
				File newAssignement = new File(OUTPUT_DIR, assignmentFile);
				pg.saveProblem(newModel, newAssignement);

				Problem problem = new Problem(newModel, newAssignement);
				if (!problem.isSolutionFeasible(problem.getOriginalSolution())) {
					System.err.println("Ojej :(");
					System.exit(1);
				}

				//break;
			}
		}
	}

	@SuppressWarnings("unused")
	private void makeMoreResources(int newNumResources) {
		int oldNumResources = resources.length;
		
		for (Balance b : balances) {
			newNumResources = Math.max(newNumResources, b.r1 + 1);
			newNumResources = Math.max(newNumResources, b.r2 + 1);
		}

		Resource lastResource = resources[resources.length - 1];
		resources = enlargeResourceArray(resources, newNumResources);

		for (int i = oldNumResources; i < newNumResources; ++i) {
			resources[i] = new Resource(lastResource.isTransient, (int) Math.ceil(lastResource.loadCostWeight
					* random.nextDouble()));
		}
		for (Machine m : machines) {
			long lastCapacity = m.capacities[m.capacities.length - 1]; 
			m.capacities = enlargeLongArray(m.capacities, newNumResources);
			
			long lastCapacityS = m.safetyCapacities[m.safetyCapacities.length - 1]; 
			m.safetyCapacities = enlargeLongArray(m.safetyCapacities, newNumResources);
			for (int i = oldNumResources; i < newNumResources; ++i) {
				m.capacities[i] = lastCapacity + random.nextInt(1000);
				m.safetyCapacities[i] = lastCapacityS;
			}
		}
		
		for (Process p : processes) {
			long lastReq = p.requirements[p.requirements.length - 1];
			p.requirements = enlargeLongArray(p.requirements, newNumResources);
			for (int i = oldNumResources; i < newNumResources; ++i) {
				p.requirements[i] = lastReq;
			}
		}
	}

	private Resource[] enlargeResourceArray(Resource[] arr, int newSize) {
		Resource[] tmp = (Resource[]) (arr.clone());
		arr = new Resource[newSize];
		for (int i = 0; i < Math.min(arr.length, tmp.length); ++i)
			arr[i] = tmp[i];
		return arr;
	}

	private long[] enlargeLongArray(long[] arr, int newSize) {
		long[] tmp = (long[]) (arr.clone());
		arr = new long[newSize];
		for (int i = 0; i < Math.min(tmp.length, arr.length); ++i)
			arr[i] = tmp[i];
		return arr;
	}
}