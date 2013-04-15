package put.roadef.ip;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.log4j.Logger;

import put.roadef.Deadline;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

public abstract class ORToolsAbstractFastMipSolver extends MipSolver {
	private String solverType = "CBC_MIXED_INTEGER_PROGRAMMING";

	static {
		System.loadLibrary("jnilinearsolver");
	}

	protected ORToolsAbstractFastMipSolver(String type) {
		solverType = type;
	}

	public Logger logger = Logger.getLogger(ORToolsAbstractFastMipSolver.class);

	@Override
	public int solve(Deadline deadline) {

		int status = 0;
		try {
			MPSolver solver = new MPSolver("IntegerProgrammingModel", MPSolver.getSolverEnum(solverType));
			double infinity = MPSolver.infinity();

			//at the beginning prepare list of all variables
			int variablesNumber = variablesTypes.length;

			//create variables
			MPVariable[] variables = new MPVariable[variablesNumber];
			for (int i = 0; i < variablesNumber; i++) {
				int name = i;
				if (variablesTypes[name] == MipSolver.BOOLEAN) {
					variables[i] = solver.makeIntVar(0, 1, "x" + name);
				} else if (variablesTypes[name] == MipSolver.SEMI_BOOLEAN) {
					variables[i] = solver.makeNumVar(0, 1, "x" + name);
				} else {
					variables[i] = solver.makeNumVar(0, infinity, "x" + name);
				}
			}
			//add constraints
			for (Equation eq : constraints) {
				MPConstraint ct;
				if (eq.type == Equation.EQ) {
					ct = solver.makeConstraint(eq.rightValue, eq.rightValue);
				} else if (eq.type == Equation.LE) {
					ct = solver.makeConstraint(-infinity, eq.rightValue);
				} else if (eq.type == Equation.GE) {
					ct = solver.makeConstraint(eq.rightValue, infinity);
				} else {
					logger.error("Unknown equation type. Equation ommited");
					continue;
				}

				for (int var : eq.getCoefficients()) {
					ct.setCoefficient(variables[var], eq.getCoefficient(var));
				}
			}

			//set objective function
			for (int var : objectiveFunction.leftCoefficients.keySet()) {
				solver.setObjectiveCoefficient(variables[var], objectiveFunction.leftCoefficients.get(var));
			}

			//			int x = 0;
			//			for (int var : initialsolution.keySet()) {
			//				if (initialsolution.get(var) > 0)
			//					x++;
			//			}
			//			double[] vals = new double[x];
			//			IloNumVar[] vars = new IloNumVar[x];
			//			int y = 0;
			//			for (int var : initialsolution.keySet()) {
			//				if (initialsolution.get(var) > 0) {
			//					vals[y] = initialsolution.get(var);
			//					vars[y] = variables[var];
			//					y++;
			//				}
			//			}
			//			cplex.setVectors(vals, null, vars, null, null, null);

			//set timelimits
			//timelimit must be positive (0 represent no time limit)
			long timeLimit = (long) Math.max(1, deadline.getTimeToExpireMilliSeconds());

			solver.setTimeLimit(timeLimit);

			logger.info("Running " + solverType + "...");
			int resultStatus = solver.solve();
			logger.info("Status = " + resultStatus);

			status = 13;
			//if we found any feasible solution then set this solution,
			//however I found out that sometimes this solution isn't feasible...

			if (resultStatus == MPSolver.OPTIMAL || resultStatus == MPSolver.FEASIBLE) {
				sol = new double[variablesTypes.length];
				for (int j = 0; j < inputVariables; ++j) {
					int id = j;
					double v = variables[j].solutionValue();
					sol[id] = v;
				}
			}

			if (resultStatus == MPSolver.OPTIMAL) {
				status = 0;
			} else if (resultStatus == MPSolver.INFEASIBLE) {
				status = 7;
			} else if (resultStatus == MPSolver.FEASIBLE) {
				status = 1;
			} else if (resultStatus == MPSolver.NOT_SOLVED) {
				status = 6;
			} else {
				//this shoulnd't happen
				logger.error("Unknown solver status: " + resultStatus);
			}
			//finalize cplex class
		} catch (java.lang.ClassNotFoundException e) {
			logger.error(solverType + " thrown an exception");
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			e.printStackTrace(printWriter);
			logger.error(result.toString());
			return MipSolver.ERROR_STATUS;
		} catch (java.lang.NoSuchFieldException e) {
			logger.error(solverType + " thrown an exception");
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			e.printStackTrace(printWriter);
			logger.error(result.toString());
			return MipSolver.ERROR_STATUS;
		} catch (java.lang.IllegalAccessException e) {
			logger.error(solverType + " thrown an exception");
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			e.printStackTrace(printWriter);
			logger.error(result.toString());
			return MipSolver.ERROR_STATUS;
		}
		return status;
	}

}
