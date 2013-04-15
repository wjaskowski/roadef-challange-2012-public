package put.roadef;

import java.util.ArrayList;

public class ExperimentStat {
	public ArrayList<InstanceStat> instanceStats = new ArrayList<InstanceStat>();
	public String experimentName;
	public String confPath;
	public ExperimentStat(String name, String confPath) {
		experimentName = name;
		this.confPath = confPath;
	}
	public double getAvgImprovement() {
		double avg = 0.0;
		for (InstanceStat is : instanceStats)
			avg += is.getAvgImprovement();
		return avg / instanceStats.size(); 
	}
	public Object getAvg95ConfidenceDelta() {
		double avg = 0.0;
		for (InstanceStat is : instanceStats)
			avg += is.getAvgImprovement95ConfidenceDelta();
		return avg / instanceStats.size();
	}
}