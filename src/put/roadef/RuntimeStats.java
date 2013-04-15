package put.roadef;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang.time.StopWatch;

public class RuntimeStats {	
	static private BufferedWriter out;
	static private StopWatch timer = new StopWatch();
	static private long lastCost = Long.MAX_VALUE;

	public static void init(File statsFile) throws IOException  {
		out = new BufferedWriter(new FileWriter(statsFile));
		out.write("#time[ms]\tcost\timprovement[%]\tduration[ms]\timprover_class_name\n");
		timer.start();
	}

	public static void add(ImmutableSolution ss, long timeMillis, String improverClassName) {
		if (out == null)
			return;
		// Improvements only
		if (ss.getCost() == lastCost)
			return;
		
		timer.split();
		try {
			out.write(timer.getSplitTime() + "\t" +  ss.getCost() + "\t" + ss.getImprovement() + "\t" + timeMillis + "\t" + improverClassName + "\n");
			out.flush();
		} catch (IOException e) {			
			e.printStackTrace();
		}
		timer.unsplit();
		lastCost = ss.getCost();
	}
	
	@Override
	protected void finalize() throws Throwable {
		out.close();
	}
}
