package put.roadef;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.varia.LevelRangeFilter;

public class LowerBound {

	static Logger logger = Logger.getLogger(LowerBound.class);
	static String inputFileName = "";
	static String originalSolutionFilename = "";

	public static void main(String[] args) throws IOException {
		if (args[0].equals("-h")) {
			usage();
			System.exit(0);
		}
		parseCmd(args);

		Logger rootLogger = Logger.getRootLogger();
		rootLogger.setLevel(Level.ALL);
		Appender consoleAppender = new ConsoleAppender(new PatternLayout(Common.LogPattern));
		LevelRangeFilter levelFilter = new LevelRangeFilter();

		levelFilter.setLevelMin(Level.ALL);
		consoleAppender.addFilter(levelFilter);
		rootLogger.addAppender(consoleAppender);

		String problemId = new File(inputFileName).getName().replaceFirst("model_", "").replaceFirst(".txt", "");

		Problem problem = new Problem(new File(inputFileName), new File(originalSolutionFilename));

		System.out.println(problem.getLowerBound());
	}

	static void parseCmd(String[] args) {
		//TODO: make it move beautiful
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-p")) {
				if (i == args.length - 1) {
					System.err.println("Invalid instance_filename");
					System.exit(0);
				} else {
					i++;
					inputFileName = args[i];
				}
			} else if (args[i].equals("-i")) {
				if (i == args.length - 1) {
					System.err.println("Invalid original_solution_filename");
					System.exit(0);
				} else {
					i++;
					originalSolutionFilename = args[i];
				}
			}
		}
		if (inputFileName == "") {
			System.err.println("Invalid instance_filename");
			System.exit(0);
		}
		if (originalSolutionFilename == "") {
			System.err.println("Invalid original_solution_filename");
			System.exit(0);
		}
	}

	private static void usage() {
		System.out
				.println("executable -t time_limit -p instance_filename -i original_solution_filename -o new_solution_filename -name -s seed [-conf filename.conf] [-log filename.log] -[logConsole]");
	}
}
