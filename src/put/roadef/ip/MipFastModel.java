package put.roadef.ip;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import put.roadef.Deadline;
import put.roadef.ImmutableSolution;
import put.roadef.Problem;
import put.roadef.Problem.Machine;
import put.roadef.Problem.Service;
import put.roadef.Solution;
import put.roadef.SolutionIO;

/**
 * Mixed Integer Programming model of a problem. By default this class generate
 * a model of a subproblem. Subproblem is defined as a set of processes that we
 * want to move and a set of machines which should be considered as a
 * possibility of a new assignment.
 * 
 * This model can be solved by a MipSolver class (frontend to third part
 * software - it could be the opensource solver or a commercial one)
 * 
 * 
 * @author Gawi
 * 
 */

public class MipFastModel {
	public Logger logger = Logger.getLogger(MipFastModel.class);

	/**
	 * Status of the last run of solver
	 */
	public int solutionStatus = -1;

	/**
	 * name of class which will be used to solve MIP problem
	 */
	protected String mipStringSolver = "LpMipSolver";

	private boolean useSemiBooleans = true;

	public MipFastModel() {
		//set default solver
		mipStringSolver = "CplexMipSolver";
	}

	public MipFastModel(String mipSolverClassName) {
		this(mipSolverClassName, true);
	}

	public MipFastModel(String mipSolverClassName, boolean useSemiBooleans) {
		this.useSemiBooleans = useSemiBooleans;
		//create class from current package
		try {
			String name = "put.roadef.ip." + mipSolverClassName;
			Class<?> c;
			c = Class.forName(name);
			Class<?>[] solverClassParams = null;

			c.getConstructor(solverClassParams);

			mipStringSolver = mipSolverClassName;
		} catch (ClassNotFoundException e) {
			logger.error("Solver class not found: " + mipSolverClassName);
			logger.error(e.getMessage());
		} catch (SecurityException e) {
			logger.error("Problem with solver class: " + mipSolverClassName);
			logger.error(e.getMessage());
		} catch (NoSuchMethodException e) {
			logger.error("Problem with solver class: " + mipSolverClassName);
			logger.error(e.getMessage());
		}
	}

	/**
	 * @return A best solution found by a solver or null (if no solution found).
	 *         It may return worse solution than the baseSolution
	 */
	public Solution modifyAssignmentsForProcesses(Problem problem, Solution baseSolution, int[] processes, Deadline deadline,
			boolean acceptOnlyImprovedSolution) {
		return modifyAssignmentsForProcesses(problem, baseSolution, processes, deadline, acceptOnlyImprovedSolution, false);
	}

	public Solution modifyAssignmentsForProcesses(Problem problem, Solution baseSolution, int[] processes, Deadline deadline,
			boolean acceptOnlyImprovedSolution, boolean setInitialSolution) {

		int machinesUsage[] = new int[problem.getNumMachines()];
		int machinesNum = 0;
		int usedProcesses[] = new int[problem.getNumProcesses()];
		int procs = 0;
		for (int i = 0; i < processes.length; i++) {
			usedProcesses[processes[i]]++;
			if (usedProcesses[processes[i]] == 1) {
				procs++;
			}
			if (machinesUsage[baseSolution.getMachine(processes[i])] == 0) {
				machinesUsage[baseSolution.getMachine(processes[i])] = 1;
				machinesNum++;
			}
		}
		int[] newProcesses = new int[procs];
		int j = 0;
		for (int i = 0; i < processes.length; i++) {
			usedProcesses[processes[i]]--;
			if (usedProcesses[processes[i]] == 0) {
				newProcesses[j++] = processes[i];
			}
		}

		int machines[] = new int[machinesNum];
		int id = 0;
		for (int i = 0; i < problem.getNumMachines(); i++) {
			if (machinesUsage[i] == 1) {
				machines[id++] = i;
			}
		}
		return modifyAssignments(problem, baseSolution, newProcesses, machines, deadline, acceptOnlyImprovedSolution,
				setInitialSolution);
	}

	public Solution modifyAssignmentsForServices(Problem problem, Solution baseSolution, int[] services, Deadline deadline,
			boolean acceptOnlyImprovedSolution) {
		return modifyAssignmentsForServices(problem, baseSolution, services, deadline, acceptOnlyImprovedSolution, false);
	}

	public Solution modifyAssignmentsForServices(Problem problem, Solution baseSolution, int[] services, Deadline deadline,
			boolean acceptOnlyImprovedSolution, boolean setInitialSolution) {
		int[] machines = new int[problem.getNumMachines()];
		for (int i = 0; i < machines.length; i++)
			machines[i] = i;
		int x = 0;
		for (int i = 0; i < services.length; i++)
			x += problem.getService(services[i]).processes.length;
		int[] processes = new int[x];
		int id = 0;
		for (int i = 0; i < services.length; i++)
			for (int j = 0; j < problem.getService(services[i]).processes.length; j++)
				processes[id++] = problem.getService(services[i]).processes[j];

		return modifyAssignments(problem, baseSolution, processes, machines, deadline, acceptOnlyImprovedSolution,
				setInitialSolution);
	}

	private int x_variables = 0;
	private int y_variables = 0;
	private int t_variables = 0;
	private int z_variables = 0;
	private int smc_variables = 0;
	private int MAX_PROCESSES = 50000;
	private int MAX_LOCATIONS = 50000;
	private int MAX_RESOURCES = 5000;
	private int MAX_BALANCES = 5000;

	private int[] processPosition;
	private int[] revProcessPosition;

	private int[] machinePosition;
	private int[] revMachinePosition;

	private int getVariableIdX(int process, int machine) {
		if (processPosition[process] < 0) {
			logger.fatal("Invalid process id... Don't know what to do ;/ (return 0)");
			return 0;
		}
		if (machinePosition[machine] < 0) {
			logger.fatal("Invalid machine id... Don't know what to do ;/ (return 0)");
			return 0;
		}

		int offset = processPosition[process] + machinePosition[machine] * MAX_PROCESSES;
		if (offset >= x_variables) {
			logger.fatal("Something wrong happened: offset bigger than number of variables_x...");
		}
		return offset;
	}

	private int getVariableIdY(int location, int service) {
		int offset = location + service * MAX_LOCATIONS;
		if (offset >= y_variables)
			logger.fatal("Something wrong happened: offset bigger than number of variables_y...");
		return offset + x_variables;
	}

	private int getVariableIdT(int balance, int machine) {
		if (machinePosition[machine] < 0) {
			logger.fatal("Invalid machine id... Don't know what to do ;/ (return 0)");
			return 0;
		}
		int offset = balance + machinePosition[machine] * MAX_BALANCES;
		if (offset >= t_variables)
			logger.fatal("Something wrong happened: offset bigger than number of variables_t...");
		return offset + x_variables + y_variables;
	}

	private int getVariableIdZ(int resource, int machine) {
		if (machinePosition[machine] < 0) {
			logger.fatal("Invalid machine id... Don't know what to do ;/ (return 0)");
			return 0;
		}
		int offset = resource + machinePosition[machine] * MAX_RESOURCES;
		if (offset >= z_variables)
			logger.fatal("Something wrong happened: offset bigger than number of variables_z...");
		return offset + x_variables + y_variables + t_variables;
	}

	private int getVariableIdSMC(int service) {
		int offset = service;
		if (offset >= smc_variables)
			logger.fatal("Something wrong happened: offset bigger than number of variables_smc...");
		return offset + x_variables + y_variables + t_variables + z_variables;
	}

	/**
	 * Metoda modyfikujaca rozwiazanie za pomoca programowania
	 * calkowitoliczbowego. Po krotce jak ten model wyglada: dla kazdego serwisu
	 * wybieramy wszystkie procesy ktore w tym serwisie istnieja. W ten sposob
	 * tworzymy zbior procesow ktory bedziemy obrabiac P. Nastepnie dla kazdego
	 * procesu i ze zbioru P tworzymy numMachines zmiennych decyzyjnych
	 * (boolowski) x_i_j, ktore definiuja czy proces i bedzie wykonywany na
	 * maszynie j. Odnosnie ograniczen odsylam do opisu odpowiednich metod
	 * budujacych ograniczenia. Odnosnie funkcji celu odsylam do metod
	 * odpowiedzialnych za tworzenie ograniczen zwiazanych z funkcja celu.
	 * 
	 * Wiecej informacji ogolnych w opisie klasy
	 * 
	 * @param problem
	 *            - problem ktorym sie zajmujemy
	 * @param baseSolution
	 *            - rozwiazanie ktore ulepszamy
	 * @param services
	 *            - lista serwisow ktorym chcemy zmienic przyporzadkowanie
	 *            (bedziemy operowac na wszystkich procesach w serwisie)
	 * @param timeoutSeconds
	 *            - timeout po ktorym przerywamy obliczenia chocby nie wiem co,
	 *            aktualnie przypisany jest do obliczen na solverze, nie
	 *            uwazglednia przygotowania rownan itp
	 * 
	 * @return A solution found by a solver (or null). This maybe worse than the
	 *         baseSolution.
	 */
	public Solution modifyAssignments(Problem problem, Solution solution, int[] processes, int[] machines, Deadline deadline,
			boolean acceptOnlyImprovedSolution) {
		return modifyAssignments(problem, solution, processes, machines, deadline, acceptOnlyImprovedSolution, false);
	}

	public Solution modifyAssignments(Problem problem, Solution solution, int[] processes, int[] machines, Deadline deadline,
			boolean acceptOnlyImprovedSolution, boolean setInitialSolution) {
		ArrayList<Equation> equations = new ArrayList<Equation>();

		int usedMachines[] = new int[problem.getNumMachines()];
		for (int i = 0; i < machines.length; i++) {
			usedMachines[machines[i]] = 1;
		}
		int usedProcesses[] = new int[problem.getNumProcesses()];
		for (int i = 0; i < processes.length; i++) {
			usedProcesses[processes[i]] = 1;
		}

		int usedServices[] = new int[problem.getNumServices()];
		int serviceNum = 0;
		for (int i = 0; i < processes.length; i++) {
			if (usedServices[problem.getProcess(processes[i]).service] == 0) {
				usedServices[problem.getProcess(processes[i]).service] = 1;
				serviceNum++;
			}
		}
		int services[] = new int[serviceNum];
		int id = 0;
		for (int i = 0; i < problem.getNumServices(); i++) {
			if (usedServices[i] == 1) {
				services[id++] = i;
			}
		}
		setVariablesLimits(processes, machines, services, problem);

		//add appropriate constraints
		equations.addAll(getResourceConstraintsForProcesses(problem, solution, processes, machines));
		if (deadline.hasExpired())
			return solution;
		equations.addAll(getConflictConstraintsForServices(problem, solution, services, machines, usedProcesses));
		if (deadline.hasExpired())
			return solution;
		equations.addAll(getCorrectAssignmentConstraints(problem, processes, machines));
		if (deadline.hasExpired())
			return solution;
		equations.addAll(getSpreadConstraints(problem, solution, services, machines, processes, usedProcesses, usedMachines));
		if (deadline.hasExpired())
			return solution;
		equations.addAll(getDependencyConstraints(problem, solution, services, usedServices, machines, usedMachines, processes,
				usedProcesses));
		if (deadline.hasExpired())
			return solution;
		equations.addAll(getLoadCostConstraints(problem, solution, processes, machines));
		if (deadline.hasExpired())
			return solution;
		equations.addAll(getBalanceCostConstraints(problem, solution, processes, machines));
		if (deadline.hasExpired())
			return solution;
		equations.addAll(getSmcConstraints(problem, solution, services, usedProcesses, usedMachines));
		if (deadline.hasExpired())
			return solution;

		//create apropriate integer programming solver
		MipSolver solver;
		try {
			String name = "put.roadef.ip." + mipStringSolver;
			Class<?> c;
			c = Class.forName(name);
			Class<?>[] solverClassParams = null;
			Object[] solverObjectParams = null;

			Constructor<?> co = c.getConstructor(solverClassParams);
			solver = (MipSolver) co.newInstance(solverObjectParams);

		} catch (Exception e) {
			logger.error("Solver not recognized");
			return solution;
		}

		//dodajmy wszystkie znalezione ograniczenia
		for (Equation eq : equations) {
			solver.addConstraint(eq);
		}

		solver.variablesTypes = new int[x_variables + y_variables + t_variables + z_variables + smc_variables];
		for (int i = 0; i < x_variables; i++)
			solver.setVeriableType(i, MipSolver.BOOLEAN);
		for (int i = x_variables; i < x_variables + y_variables; i++)
			solver.setVeriableType(i, useSemiBooleans ? MipSolver.SEMI_BOOLEAN : MipSolver.BOOLEAN);

		//ustawmy funkcje celu zwiazana z Load Cost - wiecj o tym w opisie 
		//funkcji tworzacej ograniczenia Load Cost
		ObjectiveFunction objectiveFunction = new ObjectiveFunction();
		for (int mach = 0; mach < machines.length; mach++) {
			int i = machines[mach];
			for (int j = 0; j < problem.getNumResources(); j++) {
				//"z" + j + "_" + i,
				objectiveFunction.addCoefficient(getVariableIdZ(j, i), (int) problem.getResource(j).loadCostWeight);
			}
		}

		//ustawmy funkcje celu zwiazana z Balance Cost - wiecj o tym w opisie 
		//funkcji tworzacej ograniczenia Balance Cost
		for (int mach = 0; mach < machines.length; mach++) {
			int i = machines[mach];
			for (int j = 0; j < problem.getNumBalances(); j++) {
				//"t" + j + "_" + i
				objectiveFunction.addCoefficient(getVariableIdT(j, i), (int) problem.getBalance(j).weight);
			}
		}

		//objectiveCost of Process move cost
		for (int i = 0; i < processes.length; i++) {
			int processId = processes[i];
			//"x" + processId + "_"
			//+ problem.getOriginalSolution().getAssignment()[processId]
			if (usedMachines[problem.getOriginalSolution().getMachine(processId)] > 0)
				objectiveFunction.addCoefficient(getVariableIdX(processId, problem.getOriginalSolution().getMachine(processId)),
						(int) (-problem.getProcessMoveCostWeight() * problem.getProcess(processId).moveCost));
		}

		objectiveFunction.addCoefficient(getVariableIdSMC(smc_variables - 1), (int) problem.getServiceMoveCostWeight());

		//objectiveCost of Machine move cost
		for (int i = 0; i < processes.length; i++) {
			int processId = processes[i];
			int machineId = problem.getOriginalSolution().getAssignment()[processId];
			Machine machine = problem.getMachine(machineId);
			for (int j = 0; j < machine.moveCosts.length; j++) {
				if (problem.getMachineMoveCostWeight() * machine.moveCosts[j] != 0 && usedMachines[j] > 0)
					objectiveFunction.addCoefficient(getVariableIdX(processId, j),
							(int) (problem.getMachineMoveCostWeight() * machine.moveCosts[j]));
			}
		}

		solver.setObjectiveFunction(objectiveFunction);

		solver.setInputVariables(x_variables);

		if (setInitialSolution) {
			Int2DoubleMap initialsolution = new Int2DoubleOpenHashMap();
			for (int processId : processes) {
				initialsolution.put(getVariableIdX(processId, solution.getMachine(processId)), 1);
			}
			solver.setInitialsolution(initialsolution);
		}
		if (deadline.hasExpired())
			return solution;

		//logger.info("#variables=" + solver.variablesTypes.length + ", #equations=" + equations.size());
		solutionStatus = solver.solve(deadline);

		//check status of the solution
		if (solutionStatus != MipSolver.OPTIMAL_STATUS && solutionStatus != MipSolver.FEASIBLE_STATUS) {
			if (solutionStatus == MipSolver.NOT_SOLVED_DUE_TO_TIMEOUT_STATUS) {
				logger.info("Solver did not find any solution, because it was aborted due to timelimit");
				//System.out.println("Solver did not find any solution, because it was aborted due to timelimit");
			} else {
				logger.error("Solver returned an unexpected solutionStatus = " + solutionStatus);
				saveFilesForDebug(problem, solution, processes, machines, null);
			}
			return solution;
		}

		ImmutableSolution solutionCopy = solution.lightClone();
		double[] doubleSol = solver.getSol();
		for (int i = 0; i < x_variables; i++) {
			if (doubleSol[i] > 0.1) {
				int process = revProcessPosition[i % MAX_PROCESSES];
				int machine = revMachinePosition[i / MAX_PROCESSES];
				solution.moveProcess(process, machine);
			}
		}
		// Ponizsze byc moze da sie jakos inteligentniej zrobic (np. w ogole nie przestawiajac maszyn powyzej, znajac tylko uzyskany koszt, ale ja (WJ) nie wiem jak to wyciagnac z cplex'a)
		if ((acceptOnlyImprovedSolution && solutionCopy.getCost() <= solution.getCost()) || !solution.isFeasible()) {
			//"=" dlatego, ze z pewnych wzgledow nie chcemy rozwiazan o identycznym koszcie, ale innych. Lepiej Rollback.

			if (!solution.isFeasible()) { //To jest hack (Gawi powiedzial, ze mam tak zrobic bo cplex cos dziwnego robi, gdy mu sie poda bazowe rozwiazanie.
				logger.error("Solution is not feasible! But be brave! I'll do my best");
				saveFilesForDebug(problem, solutionCopy, processes, machines, solution);
			}

			//Rollback!
			logger.info("Rollback");
			for (int i = 0; i < x_variables; i++) {
				if (doubleSol[i] > 0.1) {
					int process = revProcessPosition[i % MAX_PROCESSES];
					solution.moveProcess(process, solutionCopy.getMachine(process));
				}
			}
		}
		return solution;
	}

	private void saveFilesForDebug(Problem problem, ImmutableSolution fromSolution, int[] processes, int[] machines,
			ImmutableSolution toSolution) {
		int num = 0;
		File fromSolutionFile;
		File errorDir = new File("model_errors");
		while (true) {
			fromSolutionFile = new File(errorDir,  "Err_FromSolution" + num + "_" + problem.getName() + ".txt");
			if (!fromSolutionFile.exists())
				break;
			num += 1;
		}
		SolutionIO.writeSolutionToFile(fromSolution, fromSolutionFile);
		if (toSolution != null)
			SolutionIO.writeSolutionToFile(toSolution, new File(errorDir, "Err_ToSolution" + num + "_" + problem.getName() + ".txt"));
		SolutionIO.writeArrayToFile(processes, new File(errorDir, "Err_Processes" + num + "_" + problem.getName() + ".txt"));
		SolutionIO.writeArrayToFile(machines, new File(errorDir, "Err_Machines" + num + "_" + problem.getName() + ".txt"));
		logger.info("Saving files for debug to " + fromSolutionFile);
	}

	private void setVariablesLimits(int[] processes, int[] machines, int[] services, Problem problem) {
		x_variables = processes.length * machines.length;
		//x_variables = processes.length * machines.length;
		y_variables = problem.getNumLocations() * services.length;
		//		y_variables = problem.getNumLocations() * services.length;
		t_variables = problem.getNumBalances() * machines.length;
		//t_variables = problem.getNumBalances() * machines.length;
		z_variables = problem.getNumResources() * machines.length;
		//				z_variables = problem.getNumResources() * machines.length;
		smc_variables = services.length + 1;

		MAX_PROCESSES = processes.length;
		MAX_LOCATIONS = problem.getNumLocations();
		MAX_RESOURCES = problem.getNumResources();
		MAX_BALANCES = problem.getNumBalances();

		processPosition = new int[problem.getNumProcesses()];
		for (int i = 0; i < processPosition.length; i++)
			processPosition[i] = -1;
		for (int i = 0; i < processes.length; i++) {
			if (processPosition[processes[i]] >= 0) {
				logger.error("Process " + processes[i] + " has been put more than once in the processes list!");
			}

			processPosition[processes[i]] = i;
		}

		revProcessPosition = new int[processes.length];
		for (int i = 0; i < processes.length; i++)
			revProcessPosition[i] = processes[i];

		machinePosition = new int[problem.getNumMachines()];
		for (int i = 0; i < machinePosition.length; i++)
			machinePosition[i] = -1;
		for (int i = 0; i < machines.length; i++) {
			if (machinePosition[machines[i]] >= 0) {
				logger.error("Machine " + processes[i] + " has been put more than once in the machines list!");
			}
			machinePosition[machines[i]] = i;
		}

		revMachinePosition = new int[machines.length];
		for (int i = 0; i < machines.length; i++)
			revMachinePosition[i] = machines[i];

	}

	/**
	 * Metoda ta jest odpowiedzialna za stworzenie listy ograniczen zwiazanych z
	 * rozmiarami zasobow na maszynach i zuzywanych przez procesy oraz przez
	 * ograniczenia zwiazane z tranzytywnoscia zasobow.
	 * 
	 * Ograniczenia maja postac: dla kazdej maszyny j oraz kazdego zasobu na
	 * niej tworzymy osobne ograniczenie. W tym ograniczeniu sumujemy dla
	 * kazdego z procesow a_i_j*x_i_j, gdzie a_i_j opisuje rozmiar zasobu
	 * wymagane przez proces i. Cala ta suma musi byc mniejsza rowna niz
	 * pojemnosc maszyny j pomniejszona o zasoby zuzywane przez pozostale proces
	 * (nie nalezace do zbioru przemieszczanych serwisow). Ladniej by to
	 * wygladalo w postaci wzorow :)
	 * 
	 * @param problem
	 *            - problem ktory rozpatrujemy
	 * @param solution
	 *            - rozwiazanie bazowe ktore ewoluujemy
	 * @param processes
	 *            - lista procesow ktore przemieszczamy
	 * @param resourceUsage
	 *            - zasoby zuzywane przez procesy, razem z tranzytywnoscia
	 * @return lista ograniczen
	 */
	private ArrayList<Equation> getResourceConstraintsForProcesses(Problem problem, Solution solution, int[] processes,
			int[] machines) {

		int ass[] = solution.getAssignment();
		int origass[] = problem.getOriginalSolution().getAssignment();
		ArrayList<Equation> equations = new ArrayList<Equation>();

		for (int machineIter = 0; machineIter < machines.length; machineIter++) {
			int i = machines[machineIter];
			for (int j = 0; j < problem.getNumResources(); j++) {

				//zasoby zuzywane przez wszystkie proces na maszynie i, zasobie j
				long resource;
				if (problem.getResource(j).isTransient) {
					resource = solution.getTransientUsage(i, j);
				} else
					resource = solution.getResourceUsage(i, j);

				//tablica zawierajaca wsztsrkie wspolczynniki rownania dla danej maszyny i danego zasobu
				long[] constraint = new long[processes.length];
				if (problem.getResource(j).isTransient) { //kiedy zasoby sa tranzytywne to obsluga jest troche bardziej skomplikowana
					for (int k = 0; k < processes.length; k++) {
						if (origass[processes[k]] == i) {
							//jezeli proces w rozwiazaniu poczatkowym byl przyporzadkowany do tej maszyny to
							//pomijamy bo jak ten process tu wyladuje to nic sie nie zmieni z tym zasobem 
						} else { //jezeli natomiast proces nie byl w rozwiazaniu poczatkowym w tym miejscu to 
							if (ass[processes[k]] == i) //jezeli znajduje sie w rozwiazniu ktore modyfikujemy 
							{
								resource -= problem.getProcess(processes[k]).requirements[j]; //to usuwamy z aktualnego rozwiazania zasoby
							}
							constraint[k] = problem.getProcess(processes[k]).requirements[j]; //dodajemy odpowiedniu wspolczynnik do rownania
						}
					}
				} else { //jezeli zasoby nie sa tranzytywne to
					for (int k = 0; k < processes.length; k++) {
						if (ass[processes[k]] == i) //usuwamy z aktualnego rozwiazania zasoby w sytuacji kiedy w rozwiazaniu modyfikownym proces byl tu przyporzadkowany 
						{
							resource -= problem.getProcess(processes[k]).requirements[j];
						}
						constraint[k] = problem.getProcess(processes[k]).requirements[j]; //dodajemy odpowiednie ograniczenie
					}
				}

				//no i przeksztalcamy to na rownianie
				Equation eq = new Equation();
				for (int k = 0; k < processes.length; k++)
					eq.addCoefficient(getVariableIdX(processes[k], i), (int) constraint[k]);
				eq.setType(Equation.LE);
				//po prawej stronie znajdue sie pojemnosc pomniejszona o zasoby wykorzystywane przez procesy ktorych nie ruszamy
				eq.setRightValue(problem.getMachine(i).capacities[j] - resource);
				equations.add(eq);
			}
		}

		return equations;
	}

	/**
	 * Metoda ta tworzy ograniczenia powiazane z balance cost. Sa one potrzebne
	 * gdyz nie da sie w prosty sposob zamodelowac funkcji max(a,b) w IP.
	 * 
	 * Dla kazdej maszyny i kazdego balance costa mamy wartosc:
	 * 
	 * max( 0; target * A(m; r1) - A(m; r2)); Wartosc tej funkcji bedzie
	 * przechowywana w zmiennej t_j_i gdzie j bedzie okreslalo numer balance
	 * costa natomiast i bedzie okreslalo numer maszyny. Chcac te wartosc wpisac
	 * do t_j_i wystarczy latwo zauwazyc ze jezeli bedziemy minimalizowac sume
	 * elementow t_j_i to znajda sie tam jak najmniejsze wartosci nieujemne
	 * (nieujmenosc wynika z ograniczen programowania liniowego). W zwiazku z
	 * tym wystarczy stworzyc ograniczenie o postaci:
	 * 
	 * t_j_i >= target * A(m; r1) - A(m; r2)
	 * 
	 * 
	 * @param problem
	 *            - problem ktory rozwazamy
	 * @param solution
	 *            - rozwiazanie ktore ulepszamy
	 * @param processes
	 *            - zbior procesow ktore chcemy przeniesc w lepsze miejsce
	 * @param resourceUsage
	 *            - zuzyte zasoby przez wszystkie procesy
	 * @return ograniczenia okreslajace odpowiednie Balance costy dla calego
	 *         problemu
	 */
	private ArrayList<Equation> getBalanceCostConstraints(Problem problem, Solution solution, int[] processes, int[] machines) {
		ArrayList<Equation> equations = new ArrayList<Equation>();
		for (int mach = 0; mach < machines.length; mach++) {
			int i = machines[mach];
			for (int j = 0; j < problem.getNumBalances(); j++) {
				int res1 = problem.getBalance(j).r1;
				int res2 = problem.getBalance(j).r2;
				long target = problem.getBalance(j).target;

				long resource1 = solution.getResourceUsage(i, res1);

				long resource2 = solution.getResourceUsage(i, res2);

				Equation eq = new Equation();
				for (int proc = 0; proc < processes.length; proc++) {
					if (solution.getMachine(processes[proc]) == i) //usuwamy z aktualnego rozwiazania zasoby
					{
						resource1 -= problem.getProcess(processes[proc]).requirements[res1];
					}
					//dodajemy wspolczynnik modyfikujacy target*A(m;r1) w sytuacji kedy proces by zostal tu przypisany 
					eq.addCoefficient(getVariableIdX(processes[proc], i),
							(int) (target * problem.getProcess(processes[proc]).requirements[res1]));
					if (solution.getMachine(processes[proc]) == i) //usuwamy z aktualnego rozwiazania zasoby
					{
						resource2 -= problem.getProcess(processes[proc]).requirements[res2];
					}
					eq.addCoefficient(getVariableIdX(processes[proc], i),
							(int) -problem.getProcess(processes[proc]).requirements[res2]);
					//dodajemy wspolczynnik modyfikujacy -A(m;r2) w sytuacji kedy proces by zostal tu przypisany 

				}
				eq.addCoefficient(getVariableIdT(j, i), 1);
				eq.setRightValue(target * (problem.getMachine(i).capacities[res1] - resource1)
						- (problem.getMachine(i).capacities[res2] - resource2));
				eq.setType(Equation.GE);
				equations.add(eq);
			}
		}
		return equations;
	}

	/**
	 * Metoda ta tworzy ograniczenia powiazane z load cost. Sa one potrzebne
	 * gdyz nie da sie w prosty sposob zamodelowac funkcji max(a,b) w IP.
	 * 
	 * Dla kazdej maszyny i kazdego zasobu LC ma wartosc:
	 * 
	 * max( 0; U(m; r) - SC(m; r));
	 * 
	 * Wartosc tej funkcji bedzie przechowywana w zmiennej z_j_i gdzie j bedzie
	 * okreslalo numer zasobu natomiast i bedzie okreslalo numer maszyny. Chcac
	 * te wartosc wpisac do z_j_i wystarczy latwo zauwazyc ze jezeli bedziemy
	 * minimalizowac sume elementow z_j_i to znajda sie tam jak najmniejsze
	 * wartosci nieujemne (nieujmenosc wynika z ograniczen programowania
	 * liniowego). W zwiazku z tym wystarczy stworzyc ograniczenie o postaci:
	 * 
	 * z_j_i >= U(m; r) - SC(m; r)
	 * 
	 * @param problem
	 *            - problem ktory rozwazamy
	 * 
	 * @param solution
	 *            - rozwiazanie ktore ulepszamy
	 * @param processes
	 *            - zbior procesow ktore chcemy przeniesc w lepsze miejsce
	 * @param resourceUsage
	 *            - zuzyte zasoby przez wszystkie procesy
	 * @return ograniczenia okreslajace odpowiednie Balance costy dla calego
	 *         problemu
	 */
	private ArrayList<Equation> getLoadCostConstraints(Problem problem, Solution solution, int[] processes, int[] machines) {
		ArrayList<Equation> equations = new ArrayList<Equation>();
		for (int i = 0; i < machines.length; i++) {
			for (int j = 0; j < problem.getNumResources(); j++) {
				long resource;
				resource = solution.getResourceUsage(machines[i], j);
				long[] constraint = new long[processes.length];
				for (int k = 0; k < processes.length; k++) {
					if (solution.getMachine(processes[k]) == machines[i]) //usuwamy z aktualnego rozwiazania zasoby
						resource -= problem.getProcess(processes[k]).requirements[j];
					constraint[k] = problem.getProcess(processes[k]).requirements[j];
				}

				Equation eq = new Equation();
				eq.addCoefficient(getVariableIdZ(j, machines[i]), 1);
				for (int k = 0; k < processes.length; k++) {
					eq.addCoefficient(getVariableIdX(processes[k], machines[i]), (int) -constraint[k]);
				}
				eq.setRightValue((resource - problem.getMachine(machines[i]).safetyCapacities[j]));
				eq.setType(Equation.GE);
				equations.add(eq);
			}
		}
		return equations;
	}

	/**
	 * Metoda generuje ograniczenia wynikajace z zaleznosci. W celu lepszego
	 * zrozumienia constrainta rozwazmy nastepujacy przypadek: Zalozmy ze mamy
	 * dwa serwisy A i B. Serwis A jest zalezny od serwisu B. Dla kazdego
	 * sasiedztwa musimy zatem zdefiniowac zaleznosc miedzy procesami. Zaleznosc
	 * taka bedzie miala postac
	 * 
	 * Suma po wszystkich maszynach w sasiedztwie i wszystkich procesach w A
	 * (x_ia_jn) <= Suma po wszystkich maszynach w sasiedztwie i wszystkich
	 * procesach w B (x_ib_jn)* 50000
	 * 
	 * jn oznacza maszyne w sasiedztwie n
	 * 
	 * ia oznacza proces w serwisie A
	 * 
	 * ib oznaczaproces w serwisie B
	 * 
	 * lewa strona jest wieksza od 0 jezeli co najmniej jeden proces z A
	 * znajduje sie w sasiedztwie (ale nigdy nie jest wiecej niz 50000) prawa
	 * strona jest wieksza wieksza rowna 50000 gdy co najmniej jeden proces z B
	 * nalezy do sasiedztwa
	 * 
	 * Powyzsze rownania nalezy stosowac gdy oba serwisy sa w zbiorze
	 * rozwazanych serwisow do nowej alokacji. Jezeli tylko jeden z serwisow
	 * jest w zbiorze do nowej alokacji sprawa ma sie duzo prosciej: albo
	 * explicite zakazujemu procesom na alokacji(alokujemy A natomiast w
	 * sasiedztwie nie ma zadnego procesu z B), albo zmuszamy do alokacji co
	 * najmniej jednego procesu (alokujemy B, a w sasiedztwie jest jakis proces
	 * z A), albo nic nie robimy (alokujemy A, a w sasiedztwie jest cos z B lub
	 * alokujemy B a w sasiedztwie nie ma nikogo z A)
	 * 
	 * Wazne!!! jezeli bedziemy przenosic proces z maszyny ktora nie jest w
	 * machines to moze sie okazac ze zlamiemy te ograniczenie
	 * 
	 * 
	 * @param problem
	 * @param solution
	 * @param services
	 * @return
	 */
	private ArrayList<Equation> getDependencyConstraints(Problem problem, Solution solution, int[] services, int[] usedServices,
			int[] machines, int[] usedMachines, int[] processes, int[] usedProcesses) {
		ArrayList<Equation> equations = new ArrayList<Equation>();
		//dependency constraints

		//tablica okreslajaca sasiedztwa - 0, jesli nie przyporzadkowujemy do zadnej maszyny z sasiedztwa
		//1 w przeciwnym raie
		int usedNeigborhoods[] = new int[problem.getNumNeighborhoods()];

		//liczba sasiedztw do ktorych przyporzadkowujemy 
		int neigborhoodNum = 0;
		for (int i = 0; i < machines.length; i++) {
			int id = problem.getMachine(machines[i]).neighborhood;
			if (usedNeigborhoods[id] == 0) {
				usedNeigborhoods[id] = 1;
				neigborhoodNum++;
			}
		}

		//lista sasiedztw do ktorych przyporzadkowujemy cos
		int neigborhoods[] = new int[neigborhoodNum];
		int id = 0;
		for (int i = 0; i < problem.getNumNeighborhoods(); i++) {
			if (usedNeigborhoods[i] == 1) {
				neigborhoods[id++] = i;
			}
		}

		//dla kazdego serwisu dajemy zbior sasiedztw w ktorych zawsze cos mamy
		IntSet serviceNeighborhoods[] = (IntOpenHashSet[]) new IntOpenHashSet[problem.getNumServices()];
		for (int s = 0; s < problem.getNumServices(); ++s)
			serviceNeighborhoods[s] = new IntOpenHashSet();

		for (int p = 0; p < problem.getNumProcesses(); ++p) {
			if (usedProcesses[p] == 0) {
				int service = problem.getProcess(p).service;
				int neighborhood = problem.getMachine(solution.getMachine(p)).neighborhood;
				if (usedNeigborhoods[neighborhood] == 1)
					serviceNeighborhoods[service].add(neighborhood);
			}
		}

		for (int i = 0; i < services.length; i++) {
			Service service = problem.getService(services[i]);
			int[] neighboorhoodsStatus = new int[neigborhoods.length];
			for (int j = 0; j < neighboorhoodsStatus.length; j++)
				neighboorhoodsStatus[j] = 1;
			for (int j = 0; j < service.dependencies.length; j++) {
				//sytuacja gdy oba procesy sa w zestawie do alokacji 
				if (usedServices[service.dependencies[j]] > 0) {
					for (int neighIter = 0; neighIter < neigborhoods.length; neighIter++) {
						int neighborhoodId = neigborhoods[neighIter];
						if (serviceNeighborhoods[service.dependencies[j]].contains(neighborhoodId))
							continue;
						int machinesInNeigh[] = problem.getMachinesInNeigborhood(neighborhoodId);
						int dependencyProcesses[] = problem.getService(service.dependencies[j]).processes;
						Equation eq = new Equation();
						for (int l = 0; l < machinesInNeigh.length; l++) {
							if (usedMachines[machinesInNeigh[l]] == 1) {
								for (int m = 0; m < service.processes.length; m++) {
									if (usedProcesses[service.processes[m]] == 1)
										eq.addCoefficient(getVariableIdX(service.processes[m], machinesInNeigh[l]), -1);
								}
								for (int m = 0; m < dependencyProcesses.length; m++) {
									if (usedProcesses[dependencyProcesses[m]] == 1)
										eq.addCoefficient(getVariableIdX(dependencyProcesses[m], machinesInNeigh[l]), 50000);
								}
							}
						}

						eq.setRightValue((long) 0);
						eq.setType(Equation.GE);
						equations.add(eq);
					}
				} else { //zrodlo nie jest w zmiennych do optymalizacji ale serwis zalezny juz tak
					for (int neighIter = 0; neighIter < neigborhoods.length; neighIter++) {
						int neighborhoodId = neigborhoods[neighIter];
						if (!serviceNeighborhoods[service.dependencies[j]].contains(neighborhoodId)) { //zakazujemy w sytuacji kiedy zrodlo sie nie znajduje w sasiedztwie
							neighboorhoodsStatus[neighIter] = 0;
						}

					}
				}

			}
			//Tworzymy rownianie zakazujace alokacji w sasiedztwie
			for (int neighIter = 0; neighIter < neighboorhoodsStatus.length; neighIter++) {
				if (neighboorhoodsStatus[neighIter] == 0) {
					int machinesInNeigh[] = problem.getMachinesInNeigborhood(neigborhoods[neighIter]);
					Equation eq = new Equation();
					for (int l = 0; l < machinesInNeigh.length; l++) {
						if (usedMachines[machinesInNeigh[l]] == 1) {
							for (int m = 0; m < service.processes.length; m++) {
								if (usedProcesses[service.processes[m]] == 1)
									eq.addCoefficient(getVariableIdX(service.processes[m], machinesInNeigh[l]), 1);
							}
						}
					}

					eq.setRightValue((long) 0);
					eq.setType(Equation.EQ);
					equations.add(eq);
				}
			}

			int requireNeigbhborhoods[] = new int[problem.getNumNeighborhoods()];
			//a teraz rozwazamy sytuacji kiedy bedzie trzeba zaalokowac co najmniej jeden proces w sasiedztwie
			for (int j = 0; j < service.numRevDependencies; j++) {
				for (int k : serviceNeighborhoods[service.revDependencies[j]]) {
					//jesli nie mamy czegos w tym sasiedztwie na stale to bedziemy musieli tam przynajmniej jeden proces wrzucic
					if (!serviceNeighborhoods[services[i]].contains(k)) {
						requireNeigbhborhoods[k] = 1;
					}
				}
			}
			for (int j = 0; j < problem.getNumNeighborhoods(); j++) {
				if (requireNeigbhborhoods[j] == 1) {
					int machinesInNeigh[] = problem.getMachinesInNeigborhood(j);
					Equation eq = new Equation();
					for (int k = 0; k < machinesInNeigh.length; k++) {
						if (usedMachines[machinesInNeigh[k]] == 1) {
							for (int m = 0; m < service.processes.length; m++) {
								if (usedProcesses[service.processes[m]] == 1)
									eq.addCoefficient(getVariableIdX(service.processes[m], machinesInNeigh[k]), 1);
							}
						}
					}

					eq.setRightValue((long) 1);
					eq.setType(Equation.GE);
					equations.add(eq);
				}
			}
		}
		return equations;
	}

	/**
	 * This method generate constraints which model SMC cost. SMC_i define a
	 * number of processess in i-th service that move and SMC define a cost of
	 * the whole solution (but after some trasformation).
	 * 
	 * 
	 * @param problem
	 * @param solution
	 * @param services
	 * @return
	 */
	private ArrayList<Equation> getSmcConstraints(Problem problem, Solution solution, int[] services, int[] usedProcesses,
			int[] usedMachines) {
		ArrayList<Equation> equations = new ArrayList<Equation>();

		int usedServices[] = new int[problem.getNumServices()];

		int assignments[] = problem.getOriginalSolution().getAssignment();
		int newAssignments[] = solution.getAssignment();

		//compute constraints
		for (int i = 0; i < services.length; i++) {
			//mark that we will not use this service in computing lower bound of the SMC value 
			usedServices[services[i]] = 1;
			Equation eq = new Equation();
			Service service = problem.getService(services[i]);
			int correct = 0;
			for (int j = 0; j < service.processes.length; j++) {
				if (usedProcesses[service.processes[j]] == 1) {
					if (usedMachines[assignments[service.processes[j]]] == 1) {
						eq.addCoefficient(getVariableIdX(service.processes[j], assignments[service.processes[j]]), 1);
					}
				} else {
					if (solution.getMachine(service.processes[j]) == assignments[service.processes[j]])
						correct++;
				}
			}
			eq.addCoefficient(getVariableIdSMC(i), 1);
			eq.setType(Equation.EQ);
			eq.setRightValue((long) service.processes.length - correct);
			equations.add(eq);

			eq = new Equation();
			eq.addCoefficient(getVariableIdSMC(smc_variables - 1), 1);
			eq.addCoefficient(getVariableIdSMC(i), -1);
			eq.setRightValue((long) 0);
			eq.setType(Equation.GE);
			equations.add(eq);

		}

		long minSMC = 0;
		//compute minimal SMC cost of services not used in this assignment generator
		for (int i = 0; i < usedServices.length; i++) {
			if (usedServices[i] == 0) {
				int tmpSMC = 0;
				Service service = problem.getService(i);
				for (int j = 0; j < service.processes.length; j++) {
					int id = service.processes[j];
					if (assignments[id] != newAssignments[id])
						tmpSMC++;
				}
				minSMC = Math.max(minSMC, tmpSMC);
			}
		}
		Equation eq = new Equation();
		eq.addCoefficient(getVariableIdSMC(smc_variables - 1), 1);
		eq.setRightValue(minSMC);
		eq.setType(Equation.GE);
		equations.add(eq);

		return equations;
	}

	/**
	 * Metoda tworzu ograniczenia dla spread constraint. Jest to chyba
	 * najbardziej skomplikowany model ograniczen ze wszystkich. Zalozmy ze mamy
	 * serwis A, ktory ma spread ustawione na k. Liczbe procesow w A oznaczmy
	 * jako z.
	 * 
	 * Dla kazdej lokacji zdefiniujmy nowa zmienna y_l. Zmienna ta jest zmienna
	 * rzeczywista z przedzialu 0,1 i moze przyjac wartosc wieksza od 0 gdy
	 * dowolny proces z A bedzie wykonywany w l lub 0 w przeciwnym razie. W tym
	 * celu tworzymy ograniczenie:
	 * 
	 * Suma dla kazdej maszyny w lokalizacji dla kazdego procesu w serwisie po
	 * x_i_j >=y_l
	 * 
	 * nastepnie tworzymy nastepnego constrainta: suma y_l >= k
	 * 
	 * TODO upewnic sie ze to dziala dobrze - znalezc jakis przyklad gdzie bez
	 * tych contraintow sie wysypie
	 * 
	 * @param problem
	 * @param services
	 * @return
	 */
	private ArrayList<Equation> getSpreadConstraints(Problem problem, Solution solution, int[] services, int[] machines,
			int[] processes, int[] usedProcesses, int[] usedMachines) {
		ArrayList<Equation> equations = new ArrayList<Equation>();
		for (int i = 0; i < services.length; i++) {
			int[] usedLocations = new int[problem.getNumLocations()];
			int numLoc = 0;
			int[] p = problem.getService(services[i]).processes;
			for (int j = 0; j < p.length; j++)
				if (usedProcesses[p[j]] == 0) {
					int idLoc = problem.getMachine(solution.getMachine(p[j])).location;
					if (usedLocations[idLoc] == 0) {
						usedLocations[idLoc] = 1;
						numLoc++;
					}
				}
			if (problem.getService(services[i]).spread > numLoc) { //model this constraint only if static processes don't generate enough spread
				for (int j = 0; j < problem.getNumLocations(); j++) {
					if (usedLocations[j] == 0) {

						Equation eq = new Equation();
						int[] machinesInLoc = problem.getMachinesInLocation(j);
						for (int k = 0; k < machinesInLoc.length; k++) {
							int machineId = machinesInLoc[k];
							if (usedMachines[machineId] == 1) {
								for (int m = 0; m < p.length; m++) {
									if (usedProcesses[p[m]] == 1)
										eq.addCoefficient(getVariableIdX(p[m], machineId), 1);
								}
							}
						}
						eq.addCoefficient(getVariableIdY(j, i), -1);
						eq.setRightValue((long) 0);
						eq.setType(Equation.GE);
						equations.add(eq);
					}
				}
				Equation eq = new Equation();
				for (int j = 0; j < problem.getNumLocations(); j++) {
					if (usedLocations[j] == 0) {
						eq.addCoefficient(getVariableIdY(j, i), 1);
					}
				}
				eq.setRightValue((long) problem.getService(services[i]).spread - numLoc);
				eq.setType(Equation.GE);
				equations.add(eq);
			}
		}
		return equations;
	}

	/**
	 * Metoda tworzu ograniczenia dla samego modelu - czyli jeden proces musi
	 * byc przypisany jednej maszynie
	 * 
	 * @param problem
	 * @param services
	 * @return
	 */
	private ArrayList<Equation> getCorrectAssignmentConstraints(Problem problem, int[] processes, int[] machines) {
		ArrayList<Equation> equations = new ArrayList<Equation>();
		for (int j = 0; j < processes.length; j++) {
			Equation eq = new Equation();
			for (int machineIter = 0; machineIter < machines.length; machineIter++) {
				eq.addCoefficient(getVariableIdX(processes[j], machines[machineIter]), 1);
			}
			eq.setType(Equation.EQ);
			eq.setRightValue((long) 1);
			equations.add(eq);
		}
		return equations;
	}

	/**
	 * Metoda tworzu ograniczenia conflict constraints - dla jednej maszyny moze
	 * istniec co najwyzej jeden proces w serwisie
	 * 
	 * @param problem
	 * @param services
	 * @return
	 */
	private ArrayList<Equation> getConflictConstraintsForServices(Problem problem, Solution solution, int[] services,
			int[] machines, int[] usedProcesses) {
		ArrayList<Equation> equations = new ArrayList<Equation>();
		for (int machineIter = 0; machineIter < machines.length; machineIter++) {
			int machineId = machines[machineIter];
			for (int i = 0; i < services.length; i++) {
				//right part of the equation (by default it is equal to 1, but if we have already a process on current 
				//machine (the process which is not supposed to be moved) then this value should be decreased to 0
				long right = 1;
				int[] p = problem.getService(services[i]).processes;
				if (p.length > 1) { //jezeli serwis ma tylko jeden proces to ograniczenia conflict nie maja prawa bytu
					Equation eq = new Equation();
					for (int j = 0; j < p.length; j++) {
						if (usedProcesses[p[j]] == 1)
							eq.addCoefficient(getVariableIdX(p[j], machineId), 1);
						else if (solution.getMachine(p[j]) == machineId)
							right = 0;

					}
					eq.setRightValue(right);
					eq.setType(Equation.LE);
					equations.add(eq);
				}
			}
		}
		return equations;
	}

}
