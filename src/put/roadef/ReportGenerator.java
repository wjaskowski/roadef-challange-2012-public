package put.roadef;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

import org.apache.log4j.Logger;

public class ReportGenerator {
	static public String DATA_DIR = "data/";
	static public String RESULTS_DIR = "results/";
	static int TIME_LIMIT = 60 * 5;

	static Logger logger = Logger.getLogger(ReportGenerator.class);

	private static void printUsageAndExit(int exitCode) {
		System.out.println("Usage: ");
		System.out.println(" run_command <instance_set_name>");
		System.out.println(" e.g. run_command B");
		System.exit(exitCode);
	}

	public static void main(String[] args) throws IOException {
		org.apache.log4j.BasicConfigurator.configure();
		if (args.length == 1 && args[0] == "-h") {
			printUsageAndExit(0);
		} else if (args.length == 0) {
			printUsageAndExit(1);
		} else {
			logger.info("Generating report table... ");
			generateReport(new File(DATA_DIR, args[0]), new File(RESULTS_DIR, args[0]));
			logger.info("Finished");
		}
	}

	public static boolean isNumeric(String str) {
		return str.matches("-?\\d+(\\.\\d+)?"); //match a number with optional '-' and decimal.
	}

	public static void generateReport(File dataDir, File resultsDir) throws IOException {
		

		ArrayList<InstanceStat> prototypeStats = new ArrayList<InstanceStat>();

		File[] instanceFiles = dataDir.listFiles();
		Arrays.sort(instanceFiles);

		if (instanceFiles == null) {
			logger.error("Either dir does not exist or is not a directory");
			return;
		}

		for (File instanceFile : instanceFiles) {
			if (instanceFile.getName().startsWith("model_") && !instanceFile.getName().contains("test")) {
				String name = instanceFile.getName().replaceFirst("model_", "").replaceFirst(".txt", "");
				long originalCost = Long
						.parseLong(FileUtils.readFileContent(new File(dataDir, "original_cost_" + name + ".txt")));
				prototypeStats.add(new InstanceStat(name, originalCost));
			}
		}

		ArrayList<ExperimentStat> stats = new ArrayList<ExperimentStat>();

		File[] resultDirFiles = resultsDir.listFiles();
		Arrays.sort(resultDirFiles);
		//list directory with results
		for (File resultDir : resultDirFiles) {
			//if fileName is different then the report name then we have a next algorithm run
			if (resultDir.isFile() || resultDir.getName().startsWith("."))
				continue;
			
			// Get name of the configuration file
			File[] confs = resultDir.listFiles(new FilenameFilter() {				
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".conf");
				}
			});
			String confPath = null;
			if (confs.length > 0)
				confPath = confs[0].getParentFile().getName() + "/" + confs[0].getName();			

			ExperimentStat experimentStat = new ExperimentStat(resultDir.getName(), confPath);
			stats.add(experimentStat);			
			experimentStat.instanceStats = InstanceStat.newFromPrototype(prototypeStats);

			File[] seedDirs = resultDir.listFiles();
			for (File seedDir : seedDirs) {
				if (!isNumeric(seedDir.getName()) || seedDir.isFile())
					continue;

				for (InstanceStat instanceStat : experimentStat.instanceStats) {
					File solvedAssignment = new File(seedDir, "solve_model_" + instanceStat.instanceName + ".txt");
					if (!solvedAssignment.exists())
						continue;
					RunStat runStat = new RunStat();

					// Read cost
					Scanner scanner = new Scanner(solvedAssignment);
					scanner.nextLong(); // originalSolution
					runStat.cost = scanner.nextLong();
					scanner.close();

					// Read times
					File timeLog = new File(seedDir, instanceStat.instanceName + "_time.log");
					scanner = new Scanner(timeLog);
					scanner.next(); // "real"
					runStat.realTimeSeconds = scanner.nextDouble();
					scanner.next(); // "user"
					runStat.userTimeSeconds = scanner.nextDouble();
					scanner.close();

					// Read error log
					File errLog = new File(seedDir, instanceStat.instanceName + "_err.log");
					runStat.errLog = FileUtils.readFileContent(errLog);
					
					File rs = new File(seedDir, instanceStat.instanceName + "_runtime_stats.svg");
					if (rs.exists())
						runStat.graph = rs.getParentFile().getParentFile().getName() + "/" + rs.getParentFile().getName() + "/" + rs.getName();

					instanceStat.runStats.add(runStat);
				}
			}
		}

		String reportFileName = resultsDir + "/report.html";
		BufferedWriter out = new BufferedWriter(new FileWriter(reportFileName));
		out.write("<table border=\"1\" cellspacing=\"0\">");
		out.write("<thead>");
		out.write(String.format("<tr><th rowspan=\"2\">Experiment</th><th colspan=\"%d\">Improvement [%%] &plusmn; 95%%-confidence-delta</th></tr>",
				prototypeStats.size() + 1));
		out.write("<tr><th>Avg</th>");
		for (InstanceStat is : prototypeStats) {
			out.write("<th colspan=\"1\">" + is.instanceName + "</th>");
		}
		out.write("</tr>\n");
		out.write("</thead>");

		out.write("<tbody>");
		for (ExperimentStat es : stats) {
			out.write("<tr>");
			out.write(String.format("<td><a href=\"%s\">%s</a></td>", es.confPath, es.experimentName));
			out.write(String.format("<td><b>%5.2f</b>&plusmn;%05.2f</b></td>", es.getAvgImprovement(), es.getAvg95ConfidenceDelta()));
			for (InstanceStat is : es.instanceStats) {
				StringBuilder alt = new StringBuilder();
				alt.append(String.format("AvgCost = %,.1f &plusmn; %,.1f / %,d%n", is.getAvgCost(), is.getAvgCost95ConfidenceDelta(),
						is.originalCost));
				alt.append("\n");

				ArrayList<RunStat> sorted = new ArrayList<RunStat>(is.runStats);
				Collections.sort(sorted, new MyRunStatComparable());

				for (RunStat rs : sorted) {
					alt.append(String.format("Imp = %5.2f%%, User = %,.1fs, Real = %,.1fs%n",
							Common.computeImprovement(rs.cost, is.originalCost), rs.userTimeSeconds, rs.realTimeSeconds));
				}
				alt.append("\n");

				String err = is.getErrorLog();
				alt.append(err.substring(0, Math.min(err.length(), 300)));

				out.write("<td>");				
				String graphPath = null;
				if (sorted.size() > 0)
					graphPath = sorted.get(sorted.size() - 1).graph;
				if (graphPath != null) {
					out.write("<a href='./" + graphPath + "'>");
					System.out.println(graphPath);
				}
				out.write(String.format("<abbr title='%s'><b>%5.2f</b>&plusmn;%4.2f</abbr>", alt.toString(),
						is.getAvgImprovement(), is.getAvgImprovement95ConfidenceDelta()));
				if (graphPath != null)
					out.write("</a>");
				out.write("</td>");
			}

			out.write("</tr>");
		}

		out.write("</tbody>");

		out.write("<tfoot>");
		out.write("<tr><th>Experiment</th><th>Avg</th>");
		for (InstanceStat is : prototypeStats) {
			out.write("<th colspan=\"1\">" + is.instanceName + "</th>");
		}
		out.write("</tfoot>");

		out.close();
	}
}
