package put.roadef.ip;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.log4j.Logger;

import put.roadef.Deadline;

public class GurobiFastMipSolver extends MipSolver {

	public Logger logger = Logger.getLogger(put.roadef.ip.GurobiFastMipSolver.class);

	@Override
	public int solve(Deadline deadline) {

		int status = 0;
		try {
			GRBEnv env = new GRBEnv("mip1.log");
			//silent mode
			env.set(GRB.IntParam.OutputFlag, 0);

			env.set(GRB.DoubleParam.NodeLimit, Double.MAX_VALUE);

			//set timelimits
			env.set(GRB.DoubleParam.TimeLimit, deadline.getTimeToExpireMilliSeconds() / 1000.0);
			
			env.set(GRB.DoubleParam.NodeLimit, 400);
			env.set(GRB.DoubleParam.MIPGap, 0.0001);
			env.set(GRB.DoubleParam.MIPGapAbs, 100);
			env.set(GRB.IntParam.Threads, 1);

			GRBModel model = new GRBModel(env);

			//at the beginning prepare list of all variables
			int variablesNumber = variablesTypes.length;

			//			IloCplex cplex = new IloCplex();
			//create variables
			GRBVar[] variables = new GRBVar[variablesNumber];
			for (int i = 0; i < variablesNumber; i++) {
				int name = i;
				if (variablesTypes[name] == MipSolver.BOOLEAN) {
					variables[i] = model.addVar(0, 1, 0, GRB.BINARY, i + "");
				} else if (variablesTypes[name] == MipSolver.SEMI_BOOLEAN) {
					variables[i] = model.addVar(0, 1, 0, GRB.CONTINUOUS, i + "");
				} else {
					variables[i] = model.addVar(0, Double.MAX_VALUE, 0, GRB.CONTINUOUS, i + "");
				}
			}

			// Integrate new variables
			model.update();

			//add constraints
			int constr_id = 0;
			for (Equation eq : constraints) {
				//				System.out.println(eq.toString());
				GRBLinExpr expr = new GRBLinExpr();
				for (int var : eq.getCoefficients()) {
					expr.addTerm(eq.getCoefficient(var), variables[var]);
				}
				if (eq.type == Equation.EQ) {
					model.addConstr(expr, GRB.EQUAL, eq.rightValue, "c" + constr_id);
				} else if (eq.type == Equation.LE) {
					model.addConstr(expr, GRB.LESS_EQUAL, eq.rightValue, "c" + constr_id);
				} else if (eq.type == Equation.GE) {
					model.addConstr(expr, GRB.GREATER_EQUAL, eq.rightValue, "c" + constr_id);
				} else {
					logger.error("Unknown type of equation");
				}
				constr_id++;
			}

			//set objective function
			GRBLinExpr expr = new GRBLinExpr();
			for (int var : objectiveFunction.leftCoefficients.keySet()) {
				expr.addTerm(objectiveFunction.leftCoefficients.get(var), variables[var]);
			}
			if (objectiveFunction.type == ObjectiveFunction.MIN) {
				model.setObjective(expr, GRB.MINIMIZE);
			} else {
				model.setObjective(expr, GRB.MAXIMIZE);
			}

			//set initial solution
			for (int var : initialsolution.keySet()) {
				if (initialsolution.get(var) > 0) {
					variables[var].set(GRB.DoubleAttr.Start, initialsolution.get(var));
				}
			}
			model.update();

			status = 13;
			//if we found any feasible solution then set this solution,
			//however I found out that sometimes this solution isn't feasible...
			logger.info("Running gurobi...");

			// Optimize model

			model.optimize();

			int gurobiStatus = model.get(GRB.IntAttr.Status);
			String stringStatus = "";
			boolean res = false;

			if (gurobiStatus == GRB.OPTIMAL) {
				status = MipSolver.OPTIMAL_STATUS;
				stringStatus = "OPTIMAL";
				res = true;
			} else if (gurobiStatus == GRB.INFEASIBLE) {
				status = MipSolver.INFEASIBLE_STATUS;
				stringStatus = "INFEASIBLE";
			} else if (gurobiStatus == GRB.INF_OR_UNBD) {
				status = MipSolver.INFEASIBLE_STATUS;
				stringStatus = "INFEASIBLE OR UNBOUNDED";
			} else if (gurobiStatus == GRB.UNBOUNDED) {
				status = MipSolver.INFEASIBLE_STATUS;
				stringStatus = "UNBOUNDED";
//to trzeba poprawic
/*			} else if (gurobiStatus == GRB.TIME_LIMIT) {
				status = MipSolver.FEASIBLE_STATUS;
				stringStatus = "TIME_LIMIT";*/
			} else if (gurobiStatus == GRB.SUBOPTIMAL) {
				res = true;
				status = MipSolver.FEASIBLE_STATUS;
				stringStatus = "SUBOPTIMAL";
			} else {
				//this shoulnd't happen
				logger.error("Unknown gurobi status: " + gurobiStatus);
			}

			logger.debug("Gurobi status: " + stringStatus + " - " + model.get(GRB.IntAttr.Status));

			if (res) {
				sol = new double[variablesTypes.length];
				for (int j = 0; j < inputVariables; ++j) {
					int id = j;
					double v = variables[j].get(GRB.DoubleAttr.X);
					sol[id] = v;
				}
			}

			//finalize gurobi class
			model.dispose();
			env.dispose();
		} catch (GRBException e) {
			logger.error("Gurobi thrown an exception...");
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			e.printStackTrace(printWriter);
			logger.error(result.toString());
			return MipSolver.ERROR_STATUS;
		}
		return status;
	}
}
