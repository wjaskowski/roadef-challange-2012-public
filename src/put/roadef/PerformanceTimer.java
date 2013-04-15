package put.roadef;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PerformanceTimer {
	String name;
	Map<String, Long> map = new HashMap<String, Long>();
	long startTime;
	String currentTask;

	public PerformanceTimer(String name) {
		this.name = name;
	}
	
	public void start(String taskName) {
		startTime = System.nanoTime();
		currentTask = taskName;
	}
	
	public void stop() {
		long stopTime = System.nanoTime();
		long elapsedTime = stopTime - startTime;
		Long val = map.get(currentTask);
		if (val == null)
			val = 0L;
		map.put(currentTask, val + elapsedTime);
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("Stats for " + name + "\n");
		for (Entry<String, Long> e : map.entrySet()) {
			s.append(String.format("%-40s:\t%5.1fs\n", e.getKey(), e.getValue() / 1000000000.0));
		}
		return s.toString();
	}
}
