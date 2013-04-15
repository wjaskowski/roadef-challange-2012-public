package put.roadef;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.varia.LevelRangeFilter;

import put.roadef.conf.RoadefConfiguration;
import put.roadef.neighborhoods.AllProcessesNeighborhood;
import put.roadef.tweaks.HillClimber;

public class Main {

	static final int SAFETY_SECONDS = 1;
	static int timeLimitSeconds = 300;
	static String inputFileName = "";
	static String originalSolutionFilename = "";
	static String outputSolutionFilename = "";
	static String teamName = "J12";
	static String logFileName = "";
	static String confFileName = "machineReassignment.conf"; // Default configuration file name
	static long seed = 0;
	static Logger logger = Logger.getLogger(Main.class);
	static boolean logConsole = false;

	private final static NumberFormat nf = NumberFormat.getInstance(Locale.US);

	/**
	 * Solver that should be used for computations
	 */
	static Solver solver = new HillClimber();

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

		levelFilter.setLevelMin(logConsole ? Level.ALL : Level.WARN);
		consoleAppender.addFilter(levelFilter);
		rootLogger.addAppender(consoleAppender);

		final Deadline deadline = new Deadline(timeLimitSeconds * 1000 - SAFETY_SECONDS);

		if (logFileName != "")
			try {
				rootLogger.addAppender(new FileAppender(new PatternLayout(Common.LogPattern), logFileName, false));
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		RoadefConfiguration configuration = null;
		try {
			logger.info("Using " + confFileName);
			configuration = new RoadefConfiguration(confFileName); //TODO: new File(confFileName)
			solver = (Solver) configuration.getInstanceAndSetup("solver");
		} catch (ConfigurationException e) {
			e.printStackTrace(System.err);
			logger.fatal("Something extreamly bad has happend. That is your fault!", e);
		}
		String problemId = new File(inputFileName).getName().replaceFirst("model_", "").replaceFirst(".txt", "");
		RuntimeStats.init(new File(new File(outputSolutionFilename).getAbsoluteFile().getParentFile(), problemId + "_runtime_stats.csv"));
		
		logger.info("Reading the problem");
		Problem problem = new Problem(new File(inputFileName), new File(originalSolutionFilename));
		problem.setSeed(seed);
		
		RuntimeStats.add(problem.getOriginalSolution(), 0, problem.getClass().getSimpleName());
		
		logger.info("Starting solver");
		Solution outputSolution = solver.solve(problem, deadline);

		logger.info("Found solution of cost = " + nf.format(outputSolution.getCost()));
		logger.info("Improvement = "
				+ nf.format((problem.getOriginalFitness() - outputSolution.getCost()) / (double) problem.getOriginalFitness()
						* 100.0) + "%");
		
		logger.info("Writing the solution");
		SolutionIO.writeSolutionToFile(outputSolution, new File(outputSolutionFilename));
		logger.info("Finished");
	}

	static void parseCmd(String[] args) {
		//TODO: make it move beautiful
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-t")) {
				if (i == args.length - 1) {
					System.err.println("Invalid time limit");
					System.exit(0);
				} else {
					i++;
					timeLimitSeconds = Math.max(Integer.parseInt(args[i]) - SAFETY_SECONDS, 0);
				}
			} else if (args[i].equals("-p")) {
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
			} else if (args[i].equals("-o")) {
				if (i == args.length - 1) {
					System.err.println("Invalid output_solution_filename");
					System.exit(0);
				} else {
					i++;
					outputSolutionFilename = args[i];
				}
			} else if (args[i].equals("-s")) {
				if (i == args.length - 1) {
					System.err.println("Invalid seed");
					System.exit(0);
				} else {
					i++;
					seed = Integer.parseInt(args[i]);
				}
			} else if (args[i].equals("-name")) {
				System.out.println(teamName);
				if (args.length == 1)
					System.exit(0);
			} else if (args[i].equals("-conf")) { // This is an convenient extension over ROADEF program specification 
				if (i == args.length - 1) {
					System.err.println("Configuration file not given");
					System.exit(-1);
				} else {
					i++;
					confFileName = args[i];
				}
			} else if (args[i].equals("-log")) {
				if (i == args.length - 1) {
					System.err.println("No log filename provided");
					System.exit(-1);
				} else {
					i++;
					logFileName = args[i];
				}
			} else if (args[i].equals("-logConsole")) {
				logConsole = true;
			} else {
				System.err.println("Invalid argument " + args[i]);
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
		if (outputSolutionFilename == "") {
			System.err.println("Invalid output_solution_filename");
			System.exit(0);
		}
	}

	private static void usage() {
		System.out
				.println("executable -t time_limit -p instance_filename -i original_solution_filename -o new_solution_filename -name -s seed [-conf filename.conf] [-log filename.log] -[logConsole]");
	}
}
