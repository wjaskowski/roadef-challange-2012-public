package put.roadef;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class ProblemStatistics {
	private final NumberFormat nf = NumberFormat.getInstance(Locale.US);
	
	class Stat {
		public String key;
		public String value;

		public Stat(String key, Object value) {
			this.key = key;
			this.value = value.toString();
		}
		
		public Stat(String key, Integer value) {
			this.key = key;
			this.value = nf.format(value);
		}
		
		public Stat(String key, Long value) {
			this.key = key;
			this.value = nf.format(value);
		}
	}

	private Problem problem;

	ArrayList<Stat> stats = new ArrayList<Stat>();
	
	public HashMap<String, String> getMap() {
		HashMap<String, String> a = new HashMap<String, String>();
		for (Stat s : stats)
			a.put(s.key, s.value);
		return a;
	}

	public ProblemStatistics(Problem problem, Solution solution, ImmutableSolution upperBoundSolution) {
		Locale.setDefault(Locale.US);
		this.problem = problem;	

		stats.add(new Stat("#Processes", problem.getNumProcesses()));
		stats.add(new Stat("#Machines", problem.getNumMachines()));
		stats.add(new Stat("#Resources", problem.getNumResources()));
		stats.add(new Stat("#Services", problem.getNumServices()));
		stats.add(new Stat("#Neighborhoods", problem.getNumNeighborhoods()));
		stats.add(new Stat("#Deps", problem.getNumDependencies()));
		stats.add(new Stat("#Locations", problem.getNumLocations()));
		stats.add(new Stat("#Balances", problem.getNumBalances()));
		
		stats.add(new Stat("#ProcessesPerService[min,avg,max]", getNumProcessesPerService(problem).toString()));
		
		stats.add(new Stat("#MachinesPerNeigh[min,avg,max]", getNumMachinesPerNeigh(problem).toString()));
		
		stats.add(new Stat("#ServicesWith1Process", getNumServicesWith1Process(problem)));
		stats.add(new Stat("#ServicesWith>1Process", problem.getNumServices() - getNumServicesWith1Process(problem)));
		
		stats.add(new Stat("#ServicesWithDeps", getNumServicesWithDependencies(problem)));
		stats.add(new Stat("#ServicesWithRevDeps",
				getNumServicesWithRevDependencies(problem)));
		stats.add(new Stat("#ProcessesInServicesWithDeps",
				getNumProcessesInServicesWithDependencies(problem)));
		stats.add(new Stat("#ProcessesInServiceWithRevDeps",
				getNumProcessesInServicesWithRevDependencies(problem)));

		SmartSolution ssolution = new SmartSolution(solution);

		stats.add(new Stat("SolutionCost", ssolution.getCost()));
		stats.add(new Stat("BalanceCost", String.format("%.1f%% (%s)",
				100 * ssolution.getBalanceCost() / (double) ssolution.getCost(),
				nf.format(ssolution.getBalanceCost()))));
		stats.add(new Stat("LoadCost", String.format("%.1f%% (%s)", 100 * ssolution.getLoadCost()
				/ (double) ssolution.getCost(), nf.format(ssolution.getLoadCost()))));
		stats.add(new Stat("MachineMoveCost", String.format("%.1f%% (%s)",
				100 * ssolution.getMachineMoveCost() / (double) ssolution.getCost(),
				nf.format(ssolution.getMachineMoveCost()))));
		stats.add(new Stat("ProcessMoveCost", String.format("%.1f%% (%s)",
				100 * ssolution.getProcessMoveCost() / (double) ssolution.getCost(),
				nf.format(ssolution.getProcessMoveCost()))));
		stats.add(new Stat("ServiceMoveCost", String.format("%.1f%% (%s)",
				100 * ssolution.getServiceMoveCost() / (double) ssolution.getCost(),
				nf.format(ssolution.getServiceMoveCost()))));
		
		int numMoved = getNumMovedProcesses(problem, ssolution, problem.getOriginalSolution());
		stats.add(new Stat("NumMovedProcesses", String.format("%.1f%% (%s/%s)",
				100 * numMoved / (double) problem.getNumProcesses(),
				nf.format(numMoved), nf.format(problem.getNumProcesses()))));
		
		if (upperBoundSolution != null)		
			stats.add(new Stat("DinstanceToUpperBound", nf.format(getNumMovedProcesses(problem, ssolution, upperBoundSolution))));
	}

	private MinAvgMax getNumMachinesPerNeigh(Problem problem2) {
		MinAvgMax res = new MinAvgMax();
		int sum = 0;
		int[] neighs = new int[problem.getNumNeighborhoods()];
		for (int s = 0; s < problem.getNumMachines(); s++) {
			int n = problem.getMachine(s).neighborhood;
			neighs[n]++;
		}
		
		res.min = problem.getNumMachines();
		res.max = 0;
		for (int n = 0; n < problem.getNumNeighborhoods(); ++n) {
			res.min = Math.min(res.min, neighs[n]);
			res.max = Math.max(res.max, neighs[n]);
			sum += neighs[n];
		}
		res.avg = sum / (double)problem.getNumNeighborhoods();
		return res;
	}

	private int getNumServicesWith1Process(Problem problem2) {
		int cnt = 0;
		for (int s = 0; s < problem.getNumServices(); s++)
			if (problem.getService(s).processes.length == 1)
				cnt += 1;
		return cnt;
	}

	private MinAvgMax getNumProcessesPerService(Problem problem) {
		MinAvgMax res = new MinAvgMax();
		res.min = problem.getNumServices();
		res.max = 0;
		int sum = 0;
		for (int s = 0; s < problem.getNumServices(); s++) {
			int ps = problem.getService(s).processes.length;
			res.min = Math.min(res.min, ps);
			res.max = Math.max(res.max, ps);
			sum += ps;
		}
		res.avg = sum / (double)problem.getNumServices();
		return res;
	}

	private int getNumMovedProcesses(Problem problem, SmartSolution solution, ImmutableSolution original) {
		int cnt = 0;
		for (int p = 0; p < problem.getNumProcesses(); p++)
			if (solution.getMachine(p) != original.getMachine(p))
				cnt += 1;
		return cnt;
	}

	private int getNumServicesWithDependencies(Problem problem) {
		int cnt = 0;
		for (int s = 0; s < problem.getNumServices(); ++s)
			if (problem.getService(s).numDependencies > 0)
				cnt += 1;
		return cnt;
	}

	private int getNumProcessesInServicesWithDependencies(Problem problem) {
		int cnt = 0;
		for (int s = 0; s < problem.getNumServices(); ++s)
			if (problem.getService(s).numDependencies > 0)
				cnt += problem.getService(s).processes.length;
		return cnt;
	}

	private int getNumServicesWithRevDependencies(Problem problem) {
		int cnt = 0;
		for (int s = 0; s < problem.getNumServices(); ++s)
			if (problem.getService(s).numRevDependencies > 0)
				cnt += 1;
		return cnt;
	}

	private int getNumProcessesInServicesWithRevDependencies(Problem problem) {
		int cnt = 0;
		for (int s = 0; s < problem.getNumServices(); ++s)
			if (problem.getService(s).numRevDependencies > 0)
				cnt += problem.getService(s).processes.length;
		return cnt;
	}

	public void print(PrintStream out) {
		for (Stat stat : stats)
			out.printf("%50s = %s\n", stat.key, stat.value);
	}

	public Problem getProblem() {
		return problem;
	}
}
