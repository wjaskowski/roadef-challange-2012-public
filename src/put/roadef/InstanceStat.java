package put.roadef;

import java.util.ArrayList;

public class InstanceStat {
	public long originalCost;
	public String instanceName;
	public ArrayList<RunStat> runStats = new ArrayList<RunStat>();

	public InstanceStat(String instanceName, long originalFitness) {
		this.instanceName = instanceName;
		this.originalCost = originalFitness;
	}

	public double getAvgCost() {
		double sum = 0.0;
		for (RunStat rs : runStats)
			sum += rs.cost;
		return sum / runStats.size();
	}

	public double getStdDevFitness() {
		if (runStats.size() == 0)
			return 0;
		double sum = 0.0;
		double avg = getAvgCost();
		for (RunStat rs : runStats) {
			sum += Math.pow(rs.cost - avg, 2);
		}
		return Math.sqrt(sum);
	}

	public double getStdDevImprovement() {
		if (runStats.size() == 0)
			return 0;
		double sum = 0.0;
		double avg = getAvgImprovement();
		for (RunStat rs : runStats) {
			sum += Math.pow(Common.computeImprovement(rs.cost, originalCost) - avg, 2);
		}
		return Math.sqrt(sum);
	}

	public long getMinFitness() {
		long min = runStats.size() > 0 ? runStats.get(0).cost : -1;
		for (RunStat rs : runStats)
			min = Math.min(min, rs.cost);
		return min;
	}

	public long getMaxFitness() {
		long max = runStats.size() > 0 ? runStats.get(0).cost : -1;
		for (RunStat rs : runStats)
			max = Math.max(max, rs.cost);
		return max;
	}

	public double getAvgImprovement() {
		return Common.computeImprovement(getAvgCost(), originalCost);
	}

	public String getErrorLog() {
		String errLog = "";
		for (int i = 0; i < runStats.size(); ++i)
			if (!runStats.get(i).errLog.isEmpty()) {
				String err = runStats.get(i).errLog.replace("\n", "");
				err = err.substring(0, Math.min(err.length(), 50));
				errLog += String.format("Run %d: ", i) + err + "\n";
			}
		return errLog;
	}

	public static ArrayList<InstanceStat> newFromPrototype(ArrayList<InstanceStat> prototypeStats) {
		ArrayList<InstanceStat> arr = new ArrayList<InstanceStat>();
		for (InstanceStat s : prototypeStats)
			arr.add(new InstanceStat(s.instanceName, s.originalCost));
		return arr;
	}

	public double getAvgCost95ConfidenceDelta() {
		if (runStats.size() == 0)
			return 0.0;
		return 1.96 * getStdDevFitness() / runStats.size();
	}

	public double getAvgImprovement95ConfidenceDelta() {
		if (runStats.size() == 0)
			return 0.0;
		return 1.96 * getStdDevImprovement() / runStats.size();
	}
}