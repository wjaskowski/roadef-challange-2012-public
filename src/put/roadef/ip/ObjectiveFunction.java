package put.roadef.ip;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import org.apache.log4j.Logger;

/**
 * This class represents objective function.
 * 
 * @author Gawi
 * 
 */
public class ObjectiveFunction {

	public Logger logger = Logger.getLogger(put.roadef.ip.ObjectiveFunction.class);

	static int MIN = 0;
	static int MAX = 1;

	/**
	 * Coefficiences - the key is the name and value is a value :)
	 */
	public Int2IntMap leftCoefficients;

	/**
	 * Type of the objective function (min/max)
	 */
	public Integer type;

	public ObjectiveFunction() {
		leftCoefficients = new Int2IntOpenHashMap();
		type = MIN;
	}

	public String toString() {
		String result = "";
		if (type == MIN)
			result += "min_f(x) = ";
		else if (type == MAX)
			result += "max_f(x) = ";
		else
			logger.error("Invalid type of objective function!");

		for (int key : leftCoefficients.keySet()) {
			result += "x" + key + "*" + leftCoefficients.get(key) + " + ";
		}
		return result;
	}

	public void addCoefficient(int key, int l) {
		if (leftCoefficients.containsKey(key)) {
			leftCoefficients.put(key, (int) (l + leftCoefficients.get(key)));
		} else
			leftCoefficients.put(key, l);
	}

	public void setType(int pType) {
		type = pType;
	}

}
