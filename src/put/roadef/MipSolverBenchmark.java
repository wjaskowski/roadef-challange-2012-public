package put.roadef;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.varia.LevelRangeFilter;

import put.roadef.ip.CplexFastMipSolver;
import put.roadef.ip.MipFastModel;
import put.roadef.ip.MipSolver;

public class MipSolverBenchmark {
	public Logger logger = Logger.getLogger(MipSolverBenchmark.class);

	private String benchmarkDir = "solverBenchmark/";

	private static final int MAX_MACHINES = 8;

	String[] problems = new String[] { "data\\B\\model_b_01.txt", "data\\B\\model_b_02.txt", "data\\B\\model_b_03.txt",
			"data\\B\\model_b_04.txt", "data\\B\\model_b_05.txt", "data\\B\\model_b_06.txt", "data\\B\\model_b_07.txt",
			"data\\B\\model_b_08.txt", "data\\B\\model_b_09.txt", "data\\B\\model_b_10.txt" };

	int maxDeadline = 20000;
	int numTestsPerProblem = 10;
	Random random = new Random();

	Vector<String> names = new Vector<String>();

	Vector<Vector<Integer>> timePerRun = new Vector<Vector<Integer>>();
	Vector<Vector<Integer>> statusPerRun = new Vector<Vector<Integer>>();
	Vector<String> params = new Vector<String>();
	Vector<String> problemsForRun = new Vector<String>();

	public MipSolverBenchmark() {
		random.setSeed(12345);
	}

	void generateTests() throws IOException {

		int id = 0;
		for (String problemName : problems) {
			String assignmentName = problemName.replaceFirst("model", "assignment");
			File pFile = new File(problemName);
			File sFile = new File(assignmentName);
			Problem problem = new Problem(pFile, sFile);
			for (int j = 0; j < numTestsPerProblem; j++) {

				File file = new File(new File("solverBenchmark", "tests"), "file-" + id + ".txt");
				FileWriter fstream = new FileWriter(file);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(problemName);
				out.close();
				file = new File(new File("solverBenchmark", "tests"), "machines-" + id + ".txt");
				int[] machines = generateMachines(problem);
				SolutionIO.writeArrayToFile(machines, file);

				id++;
			}
		}

	}

	public static void main(String[] args) {
		Logger rootLogger = Logger.getRootLogger();
		rootLogger.setLevel(Level.ALL);
		Appender consoleAppender = new ConsoleAppender(new PatternLayout(Common.LogPattern));
		LevelRangeFilter levelFilter = new LevelRangeFilter();

		levelFilter.setLevelMin(Level.ALL);
		consoleAppender.addFilter(levelFilter);
		rootLogger.addAppender(consoleAppender);
		MipSolverBenchmark benchmark = new MipSolverBenchmark();
		try {
			//benchmark.generateTests();
			benchmark.computeCplexDefault();
			benchmark.computeCplexDefaultOld();
			//			benchmark.computeCBCDefault();
			//			benchmark.computeGLPKDefault();
			//			benchmark.computeCplexNoScaling();
			//			benchmark.computeCplexAgressiveScaling();
//			Double[] a = new Double[2];
//			a[0] = null;
//			a[1] = 1e14;
//
//			Double[] d = new Double[2];
//			d[0] = null;
//			d[1] = 1e-3;
//
//			Integer[] e = new Integer[2];
//			e[0] = null;
//			e[1] = 2;
//			int id = 0;
////			for (Double param_a : a)
////			for (Double param_b : a)
////				for (Double param_c : a)
////					for (Double param_d : d)
////						for (Integer param_e : e)
////			benchmark.computeCplexBarrierModify(param_a, param_b, param_c, param_d, param_e, id++);
//			benchmark.computeCplexBarrierModify(1e14, 1e14, 1e14, null, null, 100);
			benchmark.generateReport(benchmark.names);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@SuppressWarnings("unused")
	private void computeGLPKDefault() throws IOException {
		String name = "GLPKFastMipSolver";
		MipFastModel solver = new MipFastModel("GLPKFastMipSolver");
		names.add(name);
		testSolver(solver, name);
	}

	@SuppressWarnings("unused")
	private void computeCBCDefault() throws IOException {
		String name = "CBCFastMipSolver";
		MipFastModel solver = new MipFastModel("CBCFastMipSolver");
		names.add(name);
		testSolver(solver, name);
	}

	private void computeCplexDefault() throws IOException {
		String name = "CplexFastMipSolverDef";
		MipFastModel solver = new MipFastModel("CplexFastMipSolver");
		names.add(name);
		testSolver(solver, name);
	}

	private void computeCplexDefaultOld() throws IOException {
		String name = "CplexFastMipSolverDefOld";
		MipFastModel solver = new MipFastModel("CplexFastMipSolver", false);
		names.add(name);
		testSolver(solver, name);
	}

	private void computeCplexNoScaling() throws IOException {
		int defaultScale = CplexFastMipSolver.scaleParameter;
		CplexFastMipSolver.scaleParameter = -1;
		String name = "CplexFastMipSolverNoScaling";
		MipFastModel solver = new MipFastModel("CplexFastMipSolver");
		names.add(name);
		testSolver(solver, name);
		CplexFastMipSolver.scaleParameter = defaultScale;
	}

	@SuppressWarnings("unused")
	private void computeCplexAgressiveScaling() throws IOException {
		int defaultScale = CplexFastMipSolver.scaleParameter;
		CplexFastMipSolver.scaleParameter = 1;
		String name = "CplexFastMipSolverAgressiveScaling";
		MipFastModel solver = new MipFastModel("CplexFastMipSolver");
		names.add(name);
		testSolver(solver, name);
		CplexFastMipSolver.scaleParameter = defaultScale;
	}

	private void computeCplexBarrierModify(Double a, Double b, Double c, Double d, Integer e, int id) throws IOException {
		Double defaultCutUp = CplexFastMipSolver.cutUp;
		CplexFastMipSolver.cutUp = a;
		Double defaultBarGrowth = CplexFastMipSolver.barGrowth;
		CplexFastMipSolver.barGrowth=b;
		Double defaultBarObjRng = CplexFastMipSolver.barObjRng;
		CplexFastMipSolver.barObjRng=c;

		Double defaultEpaGap = CplexFastMipSolver.epaGap;
		CplexFastMipSolver.epaGap = d;

		Integer defaultMipEmphasisp = CplexFastMipSolver.mipEmphasis;
		CplexFastMipSolver.mipEmphasis=e;

		String name = "CplexFastMipSolverDiffParam" + id;
		MipFastModel solver = new MipFastModel("CplexFastMipSolver");
		names.add(name);
		testSolver(solver, name);
		CplexFastMipSolver.barGrowth = defaultBarGrowth;
		CplexFastMipSolver.barObjRng = defaultBarObjRng;
		CplexFastMipSolver.cutUp = defaultCutUp;
		CplexFastMipSolver.epaGap = defaultEpaGap;
		CplexFastMipSolver.mipEmphasis = defaultMipEmphasisp;
	}

	void testSolver(MipFastModel model, String name) throws IOException {

		File resultDir = new File(benchmarkDir + "/tests");
		String[] results = resultDir.list();
		for (int i = 0; i < results.length; i++) {
			String fileName = benchmarkDir + "/tests/" + results[i];
			if (fileName.contains("file")) {
				String machinesFile = fileName.replaceAll("file", "machines");
				String id = results[i].replaceAll("file-", "").replaceAll(".txt", "");
				String solverDir = benchmarkDir + "/results/" + name + "/" + id;
				File dir = new File(solverDir);
				if (!dir.exists()) {
					dir.mkdirs();
				}
				String solutionFileName = solverDir + "/" + "sol.txt";
				File solutionFile = new File(solutionFileName);
				if (!solutionFile.exists()) {
					String modelFile = "";
					String assignmentFile = "";
					File file = new File(fileName);

					Scanner in;
					try {
						in = new Scanner(file);
						modelFile = in.nextLine();
						assignmentFile = modelFile.replaceAll("model", "assignment");
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					File pFile = new File(modelFile);
					File sFile = new File(assignmentFile);
					Problem problem = new Problem(pFile, sFile);
					SmartSolution solution = new SmartSolution(problem.getOriginalSolution());
					int[] machines = getIntVectorFromFile(machinesFile);
					int[] processes = getProcesses(machines, solution);

					System.out.println("Compute " + name + " for test " + id);
					long start = System.currentTimeMillis();
					Solution s = model.modifyAssignments(problem, solution.clone(), processes, machines,
							new Deadline(maxDeadline), true, true);
					long end = System.currentTimeMillis();
					int time = (int) (end - start);
					int deadline = maxDeadline;
					int status = model.solutionStatus;
					long cost = s.getCost();

					String dataFileName = solverDir + "/res.txt";
					FileWriter fstream = new FileWriter(dataFileName);
					BufferedWriter out = new BufferedWriter(fstream);
					out.write(time + " " + deadline + " " + status + " " + cost);
					out.close();

					SolutionIO.writeSolutionToFile(s, new File(solutionFileName));

				} else
					System.out.println("Solver " + name + " for test " + id + " has been already tested");

			}
		}

	}

	private int[] getIntVectorFromFile(String fileName) {
		IntArrayList res = new IntArrayList();
		File file = new File(fileName);

		Scanner in = null;
		try {
			in = new Scanner(file);
			Integer integer;
			while (in.hasNext()) {
				integer = in.nextInt();
				res.add(integer);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (in != null) in.close();
		}
		return res.toIntArray();
	}

	private long[] getLongVectorFromFile(String fileName) {
		LongArrayList res = new LongArrayList();
		File file = new File(fileName);

		Scanner in = null;
		try {
			in = new Scanner(file);
			Long integer;
			while (in.hasNext()) {
				integer = in.nextLong();
				res.add(integer);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (in != null) in.close();
		}
		return res.toLongArray();
	}

	private void generateReport(Vector<String> names) throws IOException {

		logger.info("Generating report table... ");
		String reportFileName = "solverBenchmark/report.html";
		FileWriter fstream = new FileWriter(reportFileName);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write("<html><head></head><body>");

		out.write("<table border=\"1\" cellspacing=\"0\"><tr>");
		out.write("<th>Solver</th><th>Test</th>");
		for (int i = 0; i < names.size(); i++)
			out.write("<th>" + names.get(i) + "</th>");
		out.write("</tr>");
		File resultDir = new File(benchmarkDir + "/tests");
		String[] results = resultDir.list();
		int optimal[] = new int[names.size()];
		int feasible[] = new int[names.size()];
		int other[] = new int[names.size()];
		int computed[] = new int[names.size()];
		double speedup[] = new double[names.size()];
		for (int i = 0; i < results.length; i++) {
			String fileName = benchmarkDir + "/tests/" + results[i];
			if (fileName.contains("file")) {
				String id = results[i].replaceAll("file-", "").replaceAll(".txt", "");
				String modelFile = "";
				File file = new File(fileName);

				Scanner in;
				try {
					in = new Scanner(file);
					modelFile = in.nextLine();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				out.write("<tr>");
				out.write("<td>" + modelFile + "</td>");
				out.write("<td>" + id + "</td>");

				long deadline = 0;
				for (int j = 0; j < names.size(); j++) {
					String solverDir = benchmarkDir + "/results/" + names.get(j) + "/" + id;
					String dataFileName = solverDir + "/res.txt";
					File f = new File(dataFileName);
					if (f.exists()) {
						long[] tmp = getLongVectorFromFile(dataFileName);
						deadline = Math.max(deadline, tmp[0]);
					}
				}
				for (int j = 0; j < names.size(); j++) {
					String solverDir = benchmarkDir + "/results/" + names.get(j) + "/" + id;
					String dataFileName = solverDir + "/res.txt";
					File f = new File(dataFileName);
					String bgcolor = "";
					bgcolor = "#AAAAAA";
					if (f.exists()) {
						long[] tmp = getLongVectorFromFile(dataFileName);
						long time = tmp[0];
						long status = tmp[2];
						if (status == MipSolver.OPTIMAL_STATUS) {
							bgcolor = "#00DD00";
							optimal[j]++;
						} else if (status == MipSolver.FEASIBLE_STATUS) {
							feasible[j]++;
							bgcolor = "#DDDD00";
						} else {
							other[j]++;
							bgcolor = "#AAAAAA";
						}

						out.write("<td align=\"right\" bgcolor="
								+ bgcolor
								+ ">"
								+ String.format(new Locale("en"), "%5.2f", ((double) 100 * (deadline - time))
										/ ((double) deadline)) + "%</td>");

						speedup[j] += ((double) 100 * (deadline - time)) / ((double) deadline);
						computed[j]++;

					} else
						out.write("<td align=\"right\" bgcolor=" + bgcolor + ">n.a.</td>");
				}
				//now try to process results for all input files
				out.write("</tr>\n");
			}

		}
		out.write("<tr>");
		out.write("<td colspan=\"2\">Optimal sol</td>");
		for (int j = 0; j < names.size(); j++)
			out.write("<td align=\"right\" >" + optimal[j] + "</td>");
		out.write("</tr>");

		out.write("<tr>");
		out.write("<td colspan=\"2\">Feasible sol</td>");
		for (int j = 0; j < names.size(); j++)
			out.write("<td  align=\"right\" >" + feasible[j] + "</td>");
		out.write("</tr>");

		out.write("<tr>");
		out.write("<td colspan=\"2\">Other sol</td>");
		for (int j = 0; j < names.size(); j++) {
			String bgcolor = "#FFFFFF";
			if (other[j] > 0)
				bgcolor = "#FF0000";
			out.write("<td  align=\"right\" bgcolor=\"" + bgcolor + "\">" + other[j] + "</td>");
		}
		out.write("</tr>");

		out.write("<tr>");
		out.write("<td colspan=\"2\">Average speedup</td>");
		for (int j = 0; j < names.size(); j++)
			out.write("<td align=\"right\" >" + String.format(new Locale("en"), "%5.2f", speedup[j] / computed[j]) + "</td>");
		out.write("</tr>");

		out.write("</table>");
		out.write("</body>");
		out.write("</html>");
		out.close();

	}

	private int[] getProcesses(int[] machines, SmartSolution solution) {
		int x = 0;
		for (int i = 0; i < machines.length; i++)
			x += solution.processesInMachine[machines[i]].size();
		int y = 0;
		int[] res = new int[x];
		for (int i = 0; i < machines.length; i++)
			for (int a : solution.processesInMachine[machines[i]]) {
				res[y++] = a;
			}
		return res;
	}

	private int[] generateMachines(Problem problem) {
		int r = (int) ((random.nextLong() % (MAX_MACHINES - 5)));
		if (r < 0)
			r *= -1;
		r += 7;

		int[] res = new int[r];
		int[] used = new int[problem.getNumMachines()];
		for (int i = 0; i < r; i++) {
			int x;
			do {
				x = (int) (random.nextLong() % problem.getNumMachines());
				if (x < 0)
					x *= -1;
			} while (used[x] != 0);
			used[x] = 1;
			res[i] = x;
		}
		return res;
	}
}