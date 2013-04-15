package put.roadef.ip;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import lpsolve.LpSolve;

import org.apache.log4j.Logger;

/**
 * This class represents single constraint in ip model.
 * 
 * @author Gawi
 * 
 */
public class Equation {

	public Logger logger = Logger.getLogger(put.roadef.ip.Equation.class);

	public static int GE = LpSolve.GE;
	public static int LE = LpSolve.LE;
	public static int EQ = LpSolve.EQ;

	/**
	 * Coefficiences - the key is the name and value is a value :)
	 */
	private Int2IntMap leftCoefficients;

	/**
	 * Constant on the right side of the constraint
	 */
	public Long rightValue;

	/**
	 * Type of the constraint (less equal, greater equal, equal)
	 */
	public Integer type;

	public Equation() {
		leftCoefficients = new Int2IntOpenHashMap();
		rightValue = (long) 0;
		type = LE;
	}

	/**
	 * Function used for debugging. Transform equation to a printable form.
	 */
	public String toString() {
		String res = "";
		for (int key : leftCoefficients.keySet()) {
			if (!res.equals(""))
				res += " + ";
			res += "x" + key + "*" + leftCoefficients.get(key);
		}
		if (type == LE)
			res += "<=";
		else if (type == GE)
			res += ">=";
		else if (type == EQ)
			res += "=";
		else
			logger.error("Invalid type of equation!");
		res += rightValue;
		return res;
	}

	/**
	 * Add a variable to the equation.
	 * 
	 * @param key
	 *            determine the number of a viariable
	 * @param value
	 *            determine the coefficient with the variable
	 */
	public void addCoefficient(int key, int value) {
		if (leftCoefficients.containsKey(key)) {
			leftCoefficients.put(key, (int) (value + leftCoefficients.get(key)));
		} else
			leftCoefficients.put(key, value);
	}

	/**
	 * Set type of the equation
	 * @param pType
	 */
	public void setType(int pType) {
		type = pType;
	}

	/**
	 * Set limit the constant value in the equation
	 * @param l
	 */
	public void setRightValue(Long l) {
		rightValue = l;
	}

	/**
	 * Get all variables
	 * @return
	 */
	public IntSet getCoefficients() {
		return leftCoefficients.keySet();
	}

	/**
	 * Get coefficient connected with a variable
	 * @param var
	 * @return
	 */
	public double getCoefficient(int var) {
		return leftCoefficients.get(var);
	}

}
