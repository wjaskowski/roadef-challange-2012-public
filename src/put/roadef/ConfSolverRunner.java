package put.roadef;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.varia.LevelRangeFilter;

import put.roadef.conf.RoadefConfiguration;

@Deprecated
public class ConfSolverRunner extends SolverRunner {

	public static void main(String[] args) {		
		Logger rootLogger = Logger.getRootLogger();
	    rootLogger.setLevel(Level.ALL); 
	    Appender consoleAppender = new ConsoleAppender(new PatternLayout(Common.LogPattern));
	    LevelRangeFilter levelFilter = new LevelRangeFilter();
	    levelFilter.setLevelMin(Level.WARN);
	    consoleAppender.addFilter(levelFilter);
	    rootLogger.addAppender(consoleAppender);
	    
		try {
			rootLogger.addAppender(new FileAppender(new PatternLayout(Common.LogPattern), Common.AllLogFilename, false));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		if (args.length != 0 && args.length != 2) {
			printUsageAndExit(1);
		} else if (args.length == 0) {
			try {
				generateReport();
				return;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		

		try {
			if (!args[1].equals("A") && !args[1].equals("B") && !args[1].equals("C") && !args[1].equals("D") && !args[1].equals("E"))
				printUsageAndExit(1);
			DATA_DIR += args[1];
			RESULTS_DIR += args[1];
			
			String confFileName = args[0];
			//create class from current package
			RoadefConfiguration configuration = new RoadefConfiguration(confFileName);
			String configurationName = configuration.getString("conf_name", "?");
			Solver solver = (Solver) configuration.getInstanceAndSetup("solver");
			String solDir = solveTests(solver, configurationName);
			
			//Save configuration file 
			FileUtils.copyFile(new File(confFileName), new File(solDir + File.separator + confFileName));

			generateReport();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.println("Finished!");
	}

	private static void printUsageAndExit(int exitCode) {
		System.out.println("Usage: ");
		System.out.println(" run_command <ConfigurationFile> <instance_set_name>");
		System.out.println(" e.g. run_command hc_greedy.conf A");
		System.out.println(" if no argument is given, I will just (re)generate the result table");
		System.exit(exitCode);
	}
}
