package put.roadef.hh;

import org.apache.log4j.Logger;

public class HeuristicStats implements Comparable<HeuristicStats> {

	private static Logger logger = Logger.getLogger(HeuristicStats.class);

	private static final double WEIGHT_RATIO = 2;

	private Heuristic heuristic;

	private double avgPerformance;
	private double lastPerformance;

	private long totalImprovement;
	private long totalMilliseconds;
	private long lastUseTime;
	
	private int usages;
	private int id;

	private boolean useLastPerformance;
	private boolean saveZeroImprovement;

	public HeuristicStats(Heuristic heuristic, int id, boolean useLastPerformanceOnly, boolean saveZeroImprovement) {
		this.heuristic = heuristic;
		this.id = id;
		this.useLastPerformance = useLastPerformanceOnly;
		this.saveZeroImprovement = saveZeroImprovement;
		this.lastUseTime = -1;
	}

	public Heuristic getHeuristic() {
		return heuristic;
	}

	public double getLastPerformance() {
		return lastPerformance;
	}

	public double getAveragePerformance() {
		return avgPerformance;
	}

	public long getTotalImprovement() {
		return totalImprovement;
	}

	public long totalMilliseconds() {
		return totalMilliseconds;
	}

	public long getLastUseTime() {
		return lastUseTime;
	}
	
	public void saveUsage(long timeMilliseconds, long improvement) {
		usages++;
		totalMilliseconds += timeMilliseconds;
		totalImprovement += improvement;

		if (saveZeroImprovement || improvement != 0) {
			lastPerformance = (((double) improvement) / (timeMilliseconds + 1));
		}

		avgPerformance = (double) totalImprovement / totalMilliseconds;
		lastUseTime = System.currentTimeMillis();
	}

	public void logPerformance() {
		logger.info(String.format("Heuristic %s (id = %d) was used %d times (%d ms) giving total improvement of %d.",
				heuristic.getClass(), id, usages, totalMilliseconds, totalImprovement));
	}

	@Override
	public int compareTo(HeuristicStats o) {
		if (useLastPerformance) {
			if (o.lastPerformance < this.lastPerformance) {
				return -1;
			} else if (o.lastPerformance > this.lastPerformance) {
				return 1;
			} else {
				return this.id - o.id;
			}
		} else {
			double otherPerformance = o.lastPerformance * WEIGHT_RATIO + o.avgPerformance;
			double thisPerformance = this.lastPerformance * WEIGHT_RATIO + this.avgPerformance;
			
			if (otherPerformance < thisPerformance) {
				return -1;
			} else if (otherPerformance > thisPerformance) {
				return 1;
			} else {
				return this.id - o.id;
			}
		}
	}

	public int getId() {
		return id;
	}
}
