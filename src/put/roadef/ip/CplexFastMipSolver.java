package put.roadef.ip;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CplexStatus;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.log4j.Logger;

import put.roadef.Deadline;

public class CplexFastMipSolver extends MipSolver {

	public Logger logger = Logger.getLogger(put.roadef.ip.CplexFastMipSolver.class);

	private double MIN_TIME_IN_SOLVER_SECONDS = 0.1;

	/**
	 * Sets an absolute tolerance on the gap between the best integer objective
	 * and the objective of the best node remaining. When this difference falls
	 * below the value of the EpAGap parameter, the mixed integer optimization
	 * is stopped.
	 */

	public static Double epaGap = null; //deafult 1e-6;
	/**
	 * Sets a relative tolerance on the gap between the best integer objective
	 * and the objective of the best node remaining. When the value
	 * |bestnode-bestinteger|/(1e-10+|bestinteger|) falls below the value of the
	 * EpGap parameter, the mixed integer optimization is stopped. For example,
	 * to instruct CPLEX to stop as soon as it has found
	 */

	public static Double epGap = null; //deafult 1e-4;

	/**
	 * With the default setting of BALANCED, CPLEX works toward a rapid proof of
	 * an optimal solution, but balances that with effort toward finding high
	 * quality feasible solutions early in the optimization. When this parameter
	 * is set to FEASIBILITY, CPLEX frequently will generate more feasible
	 * solutions as it optimizes the problem, at some sacrifice in the speed to
	 * the proof of optimality. When set to OPTIMALITY, less effort may be
	 * applied to finding feasible solutions early. With the setting BESTBOUND,
	 * even greater emphasis is placed on proving optimality through moving the
	 * best bound value, so that the detection of feasible solutions along the
	 * way becomes almost incidental. When the parameter is set to HIDDENFEAS,
	 * the MIP optimizer works hard to find high quality feasible solutions that
	 * are otherwise very difficult to find, so consider this setting when the
	 * FEASIBILITY setting has difficulty finding solutions of acceptable
	 * quality.
	 */
	public static Integer mipEmphasis = null; //change to 2

	/**
	 * Sets the maximum absolute value of the objective function. The barrier
	 * algorithm looks at this limit to detect unbounded problems.
	 */
	public static Double barObjRng = null;

	/**
	 * Cuts off any nodes that have an objective value at or above the upper
	 * cutoff value, when the problem is a minimization problem. When a mixed
	 * integer optimization problem is continued, the smaller of these values
	 * and the updated cutoff found during optimization are used during the next
	 * mixed integer optimization. A too-restrictive value for the upper cutoff
	 * parameter may result in no integer solutions being found.
	 */
	public static Double cutUp = null;

	/**
	 * What kind of scalability should be used: -1 No scaling 0 Equilibration
	 * scaling; default 1 More aggressive scaling *
	 */
	public static int scaleParameter = 0;

	/**
	 * Used to detect unbounded optimal faces. At higher values, the barrier
	 * algorithm is less likely to conclude that the problem has an unbounded
	 * optimal face, but more likely to have numerical difficulties if the
	 * problem has an unbounded face.
	 */
	public static Double barGrowth = null;

	@Override
	public int solve(Deadline deadline) {

		int status = 0;
		try {
			//at the beginning prepare list of all variables
			int variablesNumber = variablesTypes.length;

			IloCplex cplex = new IloCplex();
			//create variables
			IloNumVar[] variables = new IloNumVar[variablesNumber];
			for (int i = 0; i < variablesNumber; i++) {
				int name = i;
				if (variablesTypes[name] == MipSolver.BOOLEAN) {
					variables[i] = cplex.intVar(0, 1);
				} else if (variablesTypes[name] == MipSolver.SEMI_BOOLEAN) {
					variables[i] = cplex.numVar(0, 1);
				} else {
					variables[i] = cplex.numVar(0.0, Double.MAX_VALUE);
				}
			}
			//add constraints
			for (Equation eq : constraints) {
				//				System.out.println(eq.toString());
				IloNumExpr expr = null;
				for (int var : eq.getCoefficients()) {
					if (expr == null) {
						expr = cplex.prod(eq.getCoefficient(var), variables[var]);
					} else {
						expr = cplex.sum(expr, cplex.prod(eq.getCoefficient(var), variables[var]));
					}
				}
				if (eq.type == Equation.EQ) {
					cplex.addEq(expr, eq.rightValue);
				} else if (eq.type == Equation.LE) {
					cplex.addLe(expr, eq.rightValue);
				} else if (eq.type == Equation.GE) {
					cplex.addGe(expr, eq.rightValue);
				} else {
					logger.error("Unknown type of equation");
				}
			}

			//set objective function
			IloNumExpr expr = null;
			for (int var : objectiveFunction.leftCoefficients.keySet()) {
				if (expr == null) {
					expr = cplex.prod(objectiveFunction.leftCoefficients.get(var), variables[var]);
				} else {
					expr = cplex.sum(expr, cplex.prod(objectiveFunction.leftCoefficients.get(var), variables[var]));
				}
			}
			if (objectiveFunction.type == ObjectiveFunction.MIN) {
				cplex.addMinimize(expr);
			} else {
				cplex.addMaximize(expr);
			}

			//silent mode
			cplex.setOut(null);

			int x = 0;
			for (int var : initialsolution.keySet()) {
				if (initialsolution.get(var) > 0)
					x++;
			}
			double[] vals = new double[x];
			IloNumVar[] vars = new IloNumVar[x];
			int y = 0;
			for (int var : initialsolution.keySet()) {
				if (initialsolution.get(var) > 0) {
					vals[y] = initialsolution.get(var);
					vars[y] = variables[var];
					y++;
				}
			}

			cplex.setVectors(vals, null, vars, null, null, null);
			cplex.setParam(IloCplex.IntParam.Threads, 1);
			cplex.setParam(IloCplex.IntParam.ScaInd, scaleParameter);

			if (barGrowth != null)
				cplex.setParam(IloCplex.DoubleParam.BarGrowth, barGrowth);

			if (barObjRng != null)
				cplex.setParam(IloCplex.DoubleParam.BarObjRng, barObjRng);
			if (cutUp != null)
				cplex.setParam(IloCplex.DoubleParam.CutUp, cutUp);
			if (epaGap != null)
				cplex.setParam(IloCplex.DoubleParam.EpAGap, epaGap);
			if (epGap != null)
				cplex.setParam(IloCplex.DoubleParam.EpGap, epGap);
			if (mipEmphasis != null)
				cplex.setParam(IloCplex.IntParam.MIPEmphasis, mipEmphasis);

			status = 13;
			//if we found any feasible solution then set this solution,
			//however I found out that sometimes this solution isn't feasible...
			
			//set timelimits
			cplex.setParam(IloCplex.DoubleParam.TiLim, deadline.getTimeToExpireMilliSeconds());
			cplex.setParam(IloCplex.IntParam.NodeLim, 400);		// Number of nodes
			//cplex.setParam(IloCplex.DoubleParam.EpGap, 0.03);
			cplex.setParam(IloCplex.DoubleParam.EpAGap, 100);
			//cplex.setParam(IloCplex.IntParam.IntSolLim, 100);	// Number of Integer solutions
			

			//logger.info("Running cplex.solve()...");

			double start = System.currentTimeMillis();
			boolean res = cplex.solve();
			double elapsed = (System.currentTimeMillis() - start) / 1000.0;
			
			//logger.info("Finished. MIP Nodes visited and time[s]: " + cplex.getNnodes64() + " " + elapsed);			

			logger.debug(String.format("Result = %s; cplex.Status = %s; CplexStatus = %s", String.valueOf(res), cplex
					.getStatus().toString(), cplex.getCplexStatus()));

			if (res) {
				sol = new double[variablesTypes.length];
				for (int j = 0; j < inputVariables; ++j) {
					int id = j;
					double v = cplex.getValue(variables[j]);
					sol[id] = v;
				}
			}

			if (cplex.getStatus().equals(IloCplex.Status.Optimal)) {
				status = MipSolver.OPTIMAL_STATUS;
			} else if (cplex.getStatus().equals(IloCplex.Status.Infeasible)) {
				status = MipSolver.INFEASIBLE_STATUS;
			} else if (cplex.getStatus().equals(IloCplex.Status.Feasible)) {
				status = MipSolver.FEASIBLE_STATUS;
			} else if (cplex.getStatus().equals(IloCplex.Status.Unknown)) {
				status = MipSolver.NOT_SOLVED_STATUS;
				if (cplex.getCplexStatus().equals(CplexStatus.AbortTimeLim))
					status = MipSolver.NOT_SOLVED_DUE_TO_TIMEOUT_STATUS;
			} else {
				//this shoulnd't happen
				logger.error("Unknown cplex status: " + cplex.getStatus());
			}
			//finalize cplex class
			cplex.end();
		} catch (IloException e) {
			logger.error("Cplex thrown an exception...");
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			e.printStackTrace(printWriter);
			logger.error(result.toString());
			return MipSolver.ERROR_STATUS;
		}
		return status;
	}
}
