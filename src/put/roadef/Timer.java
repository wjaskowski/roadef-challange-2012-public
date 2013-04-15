package put.roadef;

import org.apache.commons.lang.time.StopWatch;

public class Timer {
	StopWatch watch = new StopWatch();
	boolean stopped = false;

	public Timer() {
		start();
	}

	public void start() {
		watch.reset();
		watch.start();
		stopped = false;
	}

	public long stop() {
		watch.stop();
		stopped = true;
		return watch.getTime();
	}

	public long getTime() {
		if (stopped)
			return watch.getTime();
		else {
			watch.split();
			long time = watch.getSplitTime();
			watch.unsplit();
			return time;
		}
	}
}
