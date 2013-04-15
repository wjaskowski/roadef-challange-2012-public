package put.roadef.ip;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.ArrayList;
import java.util.HashMap;

import put.roadef.Deadline;

/**
 * General Mixed-Integer Programming Interface
 * 
 * @author Gawi
 * 
 */
public abstract class MipSolver {

	public static final int OPTIMAL_STATUS = 0;
	public static final int FEASIBLE_STATUS = 1;
	public static final int INFEASIBLE_STATUS = 7;
	public static final int NOT_SOLVED_STATUS = 6;
	public static final int ERROR_STATUS = 8;
	public static final int NUMERICAL_PROBLEM_STATUS = 9;
	public static final int NOT_SOLVED_DUE_TO_TIMEOUT_STATUS = 10;

	public static final int INTEGER = 1;
	public static final int BOOLEAN = 2;
	public static final int SEMI_BOOLEAN = 3;	

	public int[] variablesTypes;

	protected int inputVariables;

	protected Int2DoubleMap initialsolution = new Int2DoubleOpenHashMap();
	/**
	 * List of constarints
	 */
	protected ArrayList<Equation> constraints = new ArrayList<Equation>();
	/**
	 * An objective function
	 */
	protected ObjectiveFunction objectiveFunction;
	/**
	 * Solution which was found during computations
	 */
	protected double[] sol;

	protected HashMap<String, Double> solution;

	public void addConstraint(Equation constraint) {
		constraints.add(constraint);
	}

	public void setObjectiveFunction(ObjectiveFunction objFun) {
		objectiveFunction = objFun;
	}

	public abstract int solve(Deadline deadline);

	public HashMap<String, Double> getSolution() {
		return solution;
	}

	protected void setInitialsolution(Int2DoubleMap initialsolution) {
		this.initialsolution = initialsolution;
	}

	public void setVeriableType(int i, int type) {
		variablesTypes[i] = type;
	}

	public double[] getSol() {
		return sol;
	}

	public int getInputVariables() {
		return inputVariables;
	}

	public void setInputVariables(int inputVariables) {
		this.inputVariables = inputVariables;
	}
}
