package put.roadef;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;

/**
 * Solver runner run solver (specified in command line) on all tests available
 * in repository (DATA_DIR directory). After performing the tests report with
 * all results is generated (historical results and the new ones are included).
 * Report and all results are stored in RESULT_DIR directory. Solver is run with
 * the time limit TIME_LIMIT.
 * 
 * @author Gawi
 * 
 */
public class SolverRunner {
	//TODO: SolverRunner should use the code provided by organizers (in C++) to 
	//compute the objective cost and the feasibility

	static public String DATA_DIR = "data/";
	static public String RESULTS_DIR = "results/";
	static int TIME_LIMIT = 60 * 5;

	static Logger logger = Logger.getLogger(SolverRunner.class);
	private static Appender myFileAppender;

	static String COLOR_LOWER_BOUND = "#009FFF";
	static String COLOR_BEST = "#00FF00";
	static String COLOR_OUR_BEST = "FFFF00";	

	/**
	 * Generate a html report with the results
	 * 
	 * @throws IOException
	 */
	protected static void generateReport() throws IOException {
		logger.info("Generating report table... ");
		String reportFileName = RESULTS_DIR + "/report.html";
		FileWriter fstream = new FileWriter(reportFileName);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write("<html><head></head><body>");

		//report is a simple table
		//in columns are test files
		//in rows are time results for single algorithm run
		File instanceDir = new File(DATA_DIR);
		String[] instanceFileNames = instanceDir.list();
		Arrays.sort(instanceFileNames);

		out.write("<table border=\"1\" cellspacing=\"0\"><tr>");
		out.write("<th>Solver</th><th>Date</th><th>User</th><th>TotalTime</ht><th>AvgImpr</th>");

		//data of the table will be stored in the arraylist of the maps, one map for one solver, keys will indicate type of data
		ArrayList<HashMap<String, String>> reportData = new ArrayList<HashMap<String, String>>();

		int numInstances = 0;
		if (instanceFileNames == null) {
			System.err.println("Either dir does not exist or is not a directory");
		} else {
			for (int i = 0; i < instanceFileNames.length; i++) {
				String fileName = instanceFileNames[i];
				if (fileName.startsWith("model_") && !fileName.contains("test")) {
					String name = fileName.replaceFirst("model_", "").replaceFirst(".txt", "");
					out.write("<th colspan=\"1\">" + name + "</th>");
					numInstances += 1;
				}
			}
		}
		out.write("</tr>\n");

		//for every input test we have four columns - original score, new score, timelimit used to compute new solution and a computational time
		//out.write("<tr>");
		//out.write("<td></td><td></td><td></td>");
		//for (int i = 0; i < counter; i++) {
		//	out.write("<td>orig score</td><td>new score</td><td>time limit</td><td>comp time</td>");
		//}
		//out.write("</tr>");

		//now we read all results
		File resultDir = new File(RESULTS_DIR);
		String[] results = resultDir.list();
		Arrays.sort(results);
		//list directory with results
		for (int i = 0; i < results.length; i++) {
			String dirName = results[i];
			//if fileName is different then the report name then we have a next algorithm run
			if (!dirName.startsWith("report") && !dirName.startsWith(".")) {

				HashMap<String, String> row = new HashMap<String, String>();

				//at the beginning read the summary file
				FileInputStream inputStream = new FileInputStream(RESULTS_DIR + "/" + dirName + "/Solution.txt");
				DataInputStream in = new DataInputStream(inputStream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine;
				strLine = br.readLine();
				br.close();
				String[] params = strLine.split(";");
				row.put("className", params[0]);
				row.put("date", params[1]);
				row.put("userName", params[2]);

				double totalTime = 0;
				//now try to process results for all input files
				for (int j = 0; j < instanceFileNames.length; j++) {
					if (instanceFileNames[j].startsWith("model") && !instanceFileNames[j].contains("test")) {
						File checkExistence = new File(RESULTS_DIR + "/" + dirName + "/solve_" + instanceFileNames[j]);
						//check if the file exists (maybe evaluation of the algorithm had been prepared before the test was added)
						if (checkExistence.exists()) {
							//parse file with the results and put them into report
							inputStream = new FileInputStream(RESULTS_DIR + "/" + dirName + "/solve_" + instanceFileNames[j]);
							in = new DataInputStream(inputStream);
							br = new BufferedReader(new InputStreamReader(in));
							strLine = br.readLine();

							if (strLine != null) {
								if (strLine.contains(";"))
									params = strLine.split(";");
								else {
									params = new String[] { strLine, br.readLine(), br.readLine(), br.readLine() };
								}
								if (params.length >= 4 && params[3] != null && !params[3].equals("")) {
									row.put(instanceFileNames[j] + "_origSol", params[0]);
									row.put(instanceFileNames[j] + "_mySol", params[1]);
									row.put(instanceFileNames[j] + "_timeLimit", params[2]);
									row.put(instanceFileNames[j] + "_timeUsed", params[3]);
									totalTime += Double.parseDouble(params[3]);
								}
							}
							br.close();
						}
					}
				}
				row.put("totalTime", String.format(new Locale("en"), "%5.1fs", totalTime));
				reportData.add(row);
			}
		}

		// Compute best results for every instance
		long bestFitness[] = new long[instanceFileNames.length];
		for (int i = 0; i < instanceFileNames.length; ++i)
			bestFitness[i] = Long.MAX_VALUE;
		double bestAvgImpr = Long.MIN_VALUE;

		long bestFitnessOur[] = new long[instanceFileNames.length];
		for (int i = 0; i < instanceFileNames.length; ++i)
			bestFitnessOur[i] = Long.MAX_VALUE;
		double bestAvgImprOur = Long.MIN_VALUE;

		for (HashMap<String, String> solverResult : reportData) {
			boolean hasAverage = true;
			double avgImprovement = 0;
			String userName = solverResult.get("userName");
			String className = solverResult.get("className");
			for (int i = 0; i < instanceFileNames.length; i++) {
				if (instanceFileNames[i].startsWith("model") && !instanceFileNames[i].contains("test")) {
					if (!solverResult.containsKey(instanceFileNames[i] + "_origSol")) {
						hasAverage = false;
						continue;
					}
					try {
						long mySol = Long.parseLong(solverResult.get(instanceFileNames[i] + "_mySol").trim());
						long origSol = Long.parseLong(solverResult.get(instanceFileNames[i] + "_origSol").trim());
						if (!className.contains("LowerBound")) {
							if (bestFitness[i] > mySol)
								bestFitness[i] = mySol;
							if (!userName.equals("alien"))
								if (bestFitnessOur[i] > mySol)
									bestFitnessOur[i] = mySol;
						}
						avgImprovement += (origSol > 0 ? 100.0 * (origSol - mySol) / (double) origSol : 0);
					} catch (NumberFormatException e) {
						// Ignore exception
						hasAverage = false;
					}
				}
			}
			avgImprovement /= numInstances;
			if (!className.contains("LowerBound")) {
				if (bestAvgImpr < avgImprovement)
					bestAvgImpr = avgImprovement;
				if (!userName.equals("alien"))
					if (bestAvgImprOur < avgImprovement)
						bestAvgImprOur = avgImprovement;
			}
			solverResult.put("avgImprovement", hasAverage ? String.format("%7.4f%%", avgImprovement) : "n.a.");
		}

		//and now we can process all the data
		for (HashMap<String, String> row : reportData) {
			out.write("<tr>");
			String className = row.get("className");
			String shortClassName = className.replaceAll("put.roadef.", "");
			String date = row.get("date");
			//String shortDate = date.split(" ")[0];
			String userName = row.get("userName");
			out.write("<td><abbr title='" + className + "'>" + shortClassName + "</abbr></td>");
			out.write("<td><abbr title='" + date + "'>" + date + "</td>");
			out.write("<td>" + userName + "</td>");
			String avgImprovement = row.get("avgImprovement");
			String totalTime = row.get("totalTime");
			if (avgImprovement.equals("n.a."))
				totalTime = "n.a.";
			out.write("<td>" + totalTime + "</td>");

			String bgcolor = "#FFFFFF";
			if (className.contains("LowerBound"))
				bgcolor = COLOR_LOWER_BOUND;
			else {
				if (!userName.equals("alien"))
					if (avgImprovement.equals(String.format("%7.4f%%", bestAvgImprOur)))
						bgcolor = COLOR_OUR_BEST;
				if (avgImprovement.equals(String.format("%7.4f%%", bestAvgImpr)))
					bgcolor = COLOR_BEST;
			}

			out.write("<td bgcolor=" + bgcolor + ">" + avgImprovement + "</td>");
			//now try to process results for all input files
			for (int j = 0; j < instanceFileNames.length; j++) {
				if (instanceFileNames[j].startsWith("model") && !instanceFileNames[j].contains("test")) {
					//if there is no results for this input file then print "n.a"
					if (row.containsKey(instanceFileNames[j] + "_origSol")) {
						String origSolString = row.get(instanceFileNames[j] + "_origSol");
						String mySolString = row.get(instanceFileNames[j] + "_mySol");
						String timeLimitString = row.get(instanceFileNames[j] + "_timeLimit");
						String timeUsedString = row.get(instanceFileNames[j] + "_timeUsed");
						try {
							long origSol = Long.parseLong(origSolString.trim());
							long mySol = Long.parseLong(mySolString.trim());
							int timelimit = Integer.parseInt(timeLimitString.trim());
							double timeUsed = Double.parseDouble(timeUsedString.trim());
							double improvement = (origSol > 0 ? 100.0 * (origSol - mySol) / (double) origSol : 0);
							String alt = "Score: " + mySol + " / " + origSol + " | Time: " + String.format("%.1f", timeUsed)
									+ " / " + timelimit;

							bgcolor = "#FFFFFF";
							if (className.contains("LowerBound"))
								bgcolor = COLOR_LOWER_BOUND;
							else {
								if (!userName.equals("alien"))
									if (mySol == bestFitnessOur[j])
										bgcolor = COLOR_OUR_BEST;
								if (mySol == bestFitness[j])
									bgcolor = COLOR_BEST;
							}

							//solutions scores
							out.write("<td bgcolor=" + bgcolor + "><abbr title='" + alt + "'>"
									+ String.format(new Locale("en"), "%5.2f", improvement) + "%</abbr></td>");
						} catch (NumberFormatException ex) { //if there were an exception during test then mark it in red
							out.write("<td bgcolor=#FF0000>" + mySolString + "</td>");
						}
					} else {
						out.write("<td bgcolor=#AAAAAA>n.a.</td>");
					}
				}

			}
			out.write("</tr>\n");
		}
		out.write("</table>");
		out.write("</body>");
		out.write("</html>");
		out.close();
	}

	protected static String solveTests(Solver solver, String solverName) throws Exception {
		//create directory for this running's results
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
		String dateNow = formatter.format(Calendar.getInstance().getTime());
		String revHash = getRevisionHash();
		String solDirName = RESULTS_DIR + "/" + dateNow;
		if (revHash != null)
			solDirName += "#" + revHash;

		boolean success = (new File(solDirName)).mkdirs();

		if (!success) { //some problem with creating results
			System.err.println("Cannot create directory: " + solDirName);
			System.err.println("Terminated");
			System.exit(0);
		}

		//create summary of this test 
		String solFileName = solDirName + "/Solution.txt";
		FileWriter fstream = new FileWriter(solFileName);
		BufferedWriter out = new BufferedWriter(fstream);
		//put here a name of the class under test
		out.write(solverName);
		SimpleDateFormat fSDateFormat = new SimpleDateFormat(";yyyy-MM-d_HH.mm.ss");
		//timestamp
		out.write(fSDateFormat.format(Calendar.getInstance().getTime()) + ";");
		//and username
		String userName = System.getProperty("user.name");
		out.write(userName);

		out.close();

		//now try to run tests on all datasets that we can find (by finding all files in the test folder) 

		File dir = new File(DATA_DIR);
		String[] children = dir.list();
		java.util.Arrays.sort(children);
		//ArrayUtils.reverse(children);
		if (children == null) {
			// Either dir does not exist or is not a directory
		} else {
			for (int i = 0; i < children.length; i++) {
				String fileName = children[i];
				//in this directory we have two types of files, so process the test only when we
				//read a model file
				if (fileName.startsWith("model") && !fileName.contains("test")) {
					String assignmentFile = DATA_DIR + "/" + fileName.replaceFirst("model", "assignment");
					String modelFile = DATA_DIR + "/" + fileName;
					//create a file with results for this test 
					String resFileName = solDirName + "/" + fileName.replaceFirst("model", "solve_model");
					String newAssignmentFileName = solDirName + "/" + fileName.replaceFirst("model", "new_assignment");
					System.out.print("Testing " + solverName + " on " + modelFile + ", " + assignmentFile + "... ");

					if (myFileAppender != null)
						Logger.getRootLogger().removeAppender(myFileAppender);
					myFileAppender = Common.addFileAppender(newAssignmentFileName.replace(".txt", ".log"));

					logger.info("Reading problem " + modelFile);
					//read a problem
					Problem problem = new Problem(new File(modelFile), new File(assignmentFile));

					fstream = new FileWriter(resFileName);
					out = new BufferedWriter(fstream);
					//print original score
					out.write(problem.getOriginalFitness() + "; ");
					long timeBeg = Calendar.getInstance().getTimeInMillis();
					Solution sol = null;
					//print improved score
					try {
						logger.info("Running " + solverName + " on " + modelFile);
						sol = solver.solve(problem, TIME_LIMIT);
					} catch (Exception e) {
						out.write("Exception; " + e);
						System.out.println("Ups. Exception");
						e.printStackTrace();
					}
					//print timelimits and computation time
					long timeEnd = Calendar.getInstance().getTimeInMillis();

					long cost = 0;
					if (sol != null) {
						//write found solution to the file						

						SolutionIO.writeSolutionToFile(sol, new File(newAssignmentFileName));

						if (problem.isSolutionFeasible(sol)) {
							cost = problem.evaluateSolution(sol);

							Long checker_cost = OriginalSolutionChecker.check(new File(modelFile), new File(assignmentFile),
									new File(newAssignmentFileName));
							if (checker_cost == null || checker_cost == cost)
								out.write(cost + "; ");
							else {
								if (checker_cost == -1l) {
									out.write("invalid! BUG!; ");
									System.out.print("Invalid solution! We have a BUG ;(");
								} else {
									out.write("wrong cost " + cost + " - should be " + checker_cost + "! BUG!; ");
									System.out.print("BUG in cost computation (computed=" + cost + "; correct=" + checker_cost
											+ ")");
								}
							}
						} else {
							out.write("Not feasible!; ");
							System.out.print("Not feasible!");
						}
					}

					double timeElapsed = (timeEnd - timeBeg) / 1000.0;
					out.write(TIME_LIMIT + "; " + (timeElapsed + "; "));
					out.close();

					System.out.println("Done ("
							+ String.format("%5.2f", problem.getOriginalFitness() > 0 ? 100.0
									* (problem.getOriginalFitness() - cost) / (double) problem.getOriginalFitness() : 0) + ")"
							+ " [" + String.format("%7.3f", timeElapsed) + "s]");

					generateReport();
				}
			}
		}
		System.out.print("Generating stats... ");
		//StatisticsGenerator.generateStatistics(new File(solDirName));
		return solDirName;
	}

	private static String getRevisionHash() {
		try {
			String cmd = "";
			if (System.getProperty("os.name").startsWith("Windows"))
				cmd = "cmd /c ";
			cmd += "git log --pretty=format:'%h' -n 1";
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = bri.readLine();
			if (line == null) {
				Logger.getLogger("put.roadef").warn("Cannot get git's current revision");
				return null;
			}
			String hash = bri.readLine().trim().replaceAll("'", "");
			bri.close();
			p.waitFor();
			return hash;
		} catch (Exception err) {
			Logger.getLogger("put.roadef").warn("Cannot get git's current revision");
			return null;
		}
	}

	private static void printUsageAndExit(int exitCode) {
		System.out.println("Usage: ");
		System.out.println(" run_command <instance_set_name>");
		System.out.println(" e.g. run_command B");
		System.exit(exitCode);
	}

	public static void main(String[] args) {
		if (args.length == 1 && args[0] == "-h") {
			printUsageAndExit(0);
		} else if (args.length == 0) {
			printUsageAndExit(1);
		} else {
			DATA_DIR += args[0];
			RESULTS_DIR += args[0];
		}

		org.apache.log4j.BasicConfigurator.configure();
		try {
			generateReport();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("Finised");

		//		if (args.length == 1 && args[0] == "-h") {
		//			printUsageAndExit(0);
		//		} else if (args.length > 2) {
		//			printUsageAndExit(1);
		//		} else if (args.length == 0) {
		//			try {
		//				generateReport();
		//				return;
		//			} catch (IOException e) {
		//				e.printStackTrace();
		//			}
		//		}
		//		
		//
		//		try {
		//			//create class from current package
		//			String solverName = args[0];
		//			String operatorName = "";
		//			Class<?> c = RoadefConfiguration.getRoadefClass(solverName);
		//			Class<?>[] solverClassParams;
		//			Object[] solverObjectParams;
		//
		//			if (args.length == 2) {
		//				operatorName = args[1];
		//				Class<?> argClass = RoadefConfiguration.getRoadefClass(operatorName);
		//				Class<?>[] nullClassParams = null;
		//				Constructor<?> argConstructor = argClass.getConstructor(nullClassParams);
		//				Object[] nullParams = null;
		//				solverObjectParams = new Object[] { argConstructor.newInstance(nullParams) };
		//				solverClassParams = new Class[] { TweakOperator.class };
		//			} else {
		//				solverObjectParams = null;
		//				solverClassParams = null;
		//			}
		//
		//			Constructor<?> co = c.getConstructor(solverClassParams);
		//			Solver solver = (Solver) co.newInstance(solverObjectParams);
		//
		//			solveTests(solver, solver.getClass().getSimpleName().toString() + "("
		//					+ operatorName.getClass().getSimpleName().toString() + ")");
		//
		//			//generate a report			
		//			generateReport();
		//
		//		} catch (Exception e) {
		//			e.printStackTrace();
		//			System.exit(-1);
		//		}
		//		System.out.println("Finished!");
	}

}

//	private static void printUsageAndExit(int exitCode) {
//		System.out.println("Usage: ");
//		System.out.println(" run_command <SolverClassToTest> [<MoveOperator>]");
//		System.out.println(" if no argument is given, I will just (re)generate the result table");
//		System.exit(exitCode);
//	}
