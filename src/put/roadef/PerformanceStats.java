package put.roadef;

import java.util.LinkedHashMap;

public class PerformanceStats {
	private long lastTime;
	private String lastMethodName;
	private long lastCost;

	public class Perf {
		public long totalTimeMs;
		public long totalImpr;
		public String methodName;
		public int numCalls;

		public Perf(String methodName) {
			this.methodName = methodName;
		}

		double getEffectiveness() {
			return totalTimeMs == 0 ? 0 : totalImpr / (double) totalTimeMs;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append(methodName + " improved by " + totalImpr);
			s.append(String.format(" during + %.2fs", totalTimeMs / 1000.0));
			s.append(" and " + numCalls + " calls");
			s.append(String.format(" (%.4f/ms)", getEffectiveness()));
			return s.toString();
		}
	}

	public LinkedHashMap<String, Perf> performances = new LinkedHashMap<String, Perf>();

	public void start(ImmutableSolution initialSolution, String methodName) {
		lastTime = System.currentTimeMillis();
		lastMethodName = methodName;
		lastCost = initialSolution.getCost();
	}

	public void stop(ImmutableSolution newSolution) {		
		if (!performances.containsKey(lastMethodName))
			performances.put(lastMethodName, new Perf(lastMethodName));
		Perf p = performances.get(lastMethodName);
		
		p.totalImpr += (lastCost - newSolution.getCost());
		p.totalTimeMs += System.currentTimeMillis() - lastTime;
		p.numCalls += 1;		
	}
}
