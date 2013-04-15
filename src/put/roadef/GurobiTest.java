package put.roadef;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.varia.LevelRangeFilter;

import put.roadef.ip.CplexFastMipSolver;
import put.roadef.ip.MipFastModel;

public class GurobiTest {
	private void assertNotNull(String str, Object test){
		if (test==null) {
			(new Exception()).printStackTrace();
			System.out.println(str);
			System.exit(0);
		}
	}
	private void assertTrue(String str, boolean test){
		if (!test) {
			(new Exception()).printStackTrace();
			System.out.println(str);
			System.exit(0);
		}
	}
	private void assertTrue(boolean test){
		if (!test) {
			(new Exception()).printStackTrace();
			System.exit(0);
		}
	}

	public static void main(String[] args) {
		System.out.println("Test GurobiTest...\n");
		Logger rootLogger = Logger.getRootLogger();
		GurobiTest obj = new GurobiTest();
		obj.testUnpredictableNulls();
		obj.testUnpredictableUnboundModel();
		obj.testNoSolutionFoundFromMIPstart();
		obj.testInfeasibleSolution();
		obj.testSomeRecentProblems();
		obj.testInfeasibleSolution2();
	}

	public void tearDown() throws Exception {
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
			if (in != null)
				in.close();
		}
		return res.toIntArray();
	}

	public void testUnpredictableNulls() {
		File pFile = new File("data/A/model_a1_5.txt");
		File sFile = new File("data/A/assignment_a1_5.txt");
		Problem problem = new Problem(pFile, sFile);
		int[] processes = getIntVectorFromFile("testFiles/a_5_test_processes.txt");
		Solution sol = new SimpleSolution(SolutionIO.readSolutionFromFile(problem, new File("testFiles/a_5_test_assignment.txt")));

		MipFastModel model = new MipFastModel("GurobiFastMipSolver");

		assertTrue("Input solution not feasible for test " + "testFiles/gawi0_processes.txt", sol.isFeasible());

		Solution best = model.modifyAssignmentsForProcesses(problem, sol, processes, new Deadline(100000), true);
		assertNotNull("Empty output solution for test " + "testFiles/gawi0_processes.txt", best);
		assertTrue("Output solution not feasible for test " + "testFiles/gawi0_processes.txt", best.isFeasible());
		assertTrue("Invalid status returned by solver: " + model.solutionStatus, model.solutionStatus == 0
				|| model.solutionStatus == 1);
	}


	public void testUnpredictableUnboundModel() {
		File pFile = new File("data/B/model_b_04.txt");
		File sFile = new File("data/B/assignment_b_04.txt");
		Problem problem = new Problem(pFile, sFile);
		int id = 10;
		int[] processes = getIntVectorFromFile("testFiles/unbound/Processes" + id + "_b_04.txt");
		int[] machines = getIntVectorFromFile("testFiles/unbound/Machines" + id + "_b_04.txt");
		Solution sol = new SimpleSolution(SolutionIO.readSolutionFromFile(problem, new File("testFiles/unbound/FromSolution" + id
				+ "_b_04.txt")));

		MipFastModel model = new MipFastModel("GurobiFastMipSolver");

		assertTrue("Input solution not feasible ", sol.isFeasible());

		Solution best = model.modifyAssignments(problem, sol, processes, machines, new Deadline(100000), true, true);
		assertNotNull("Empty output solution ", best);
		assertTrue("Output solution not feasible ", best.isFeasible());
		assertTrue("Invalid status returned by solver: " + model.solutionStatus, model.solutionStatus == 0
				|| model.solutionStatus == 1);

	}


	public void testInfeasibleSolution() {
		File pFile = new File("data/B/model_b_03.txt");
		File sFile = new File("data/B/assignment_b_03.txt");
		Problem problem = new Problem(pFile, sFile);
		int id = 0;
		int[] processes = getIntVectorFromFile("testFiles/infeasible/Processes" + id + "_b_03.txt");
		int[] machines = getIntVectorFromFile("testFiles/infeasible/Machines" + id + "_b_03.txt");
		Solution sol = new SimpleSolution(SolutionIO.readSolutionFromFile(problem, new File("testFiles/infeasible/FromSolution"
				+ id + "_b_03.txt")));

		MipFastModel model = new MipFastModel("GurobiFastMipSolver");

		assertTrue("Input solution not feasible ", problem.isSolutionFeasible(sol));

		//change scalability

		CplexFastMipSolver.scaleParameter = 1;

		Solution best = model.modifyAssignments(problem, sol, processes, machines, new Deadline(100000), true, true);
		assertNotNull("Empty output solution ", best);
		assertTrue("Output solution not feasible ", best.isFeasible());
		assertTrue("Invalid status returned by solver: " + model.solutionStatus, model.solutionStatus == 0
				|| model.solutionStatus == 1);

	}

	public void testNoSolutionFoundFromMIPstart() {
		File dir = new File("testFiles/initialSolutionProblem");
				
		for (File f : dir.listFiles()) {
			if (!f.getName().startsWith("FromSolution"))
				continue;
			String id = f.getName().replaceFirst("FromSolution", "").replaceFirst(".txt", "");
			String fields[] = id.split("_");
			String instance_id = fields[1] + "_" + fields[2];
			String instance_dirid = fields[1].replaceAll("\\d*", "").toUpperCase();

					
			File pFile = new File(new File("data", instance_dirid), "model_" + instance_id + ".txt");
			File sFile = new File(new File("data", instance_dirid), "assignment_" + instance_id + ".txt");
			Problem problem = new Problem(pFile, sFile);

			int[] processes = getIntVectorFromFile(new File(dir, "Processes" + id + ".txt").getAbsolutePath());
			int[] machines = getIntVectorFromFile(new File(dir, "Machines" + id + ".txt").getAbsolutePath());
			Solution sol = new SimpleSolution(SolutionIO.readSolutionFromFile(problem, f));

			MipFastModel model = new MipFastModel("GurobiFastMipSolver");

			assertTrue("Input solution not feasible ", sol.isFeasible());

			Solution best = model.modifyAssignments(problem, sol, processes, machines, new Deadline(100000), true, true);
			assertNotNull("Empty output solution ", best);
			assertTrue("Output solution not feasible ", best.isFeasible());
			assertTrue("Invalid status returned by solver: " + model.solutionStatus, model.solutionStatus == 0
					|| model.solutionStatus == 1);
		}
	}

	public void testSomeRecentProblems() {
		File dir = new File("testFiles/problems");
				
		for (File f : dir.listFiles()) {
			if (!f.getName().startsWith("Err_FromSolution"))
				continue;
			String id = f.getName().replaceFirst("Err_FromSolution", "").replaceFirst(".txt", "");
			String fields[] = id.split("_");
			String instance_id = fields[1] + "_" + fields[2];
			String instance_dirid = fields[1].replaceAll("\\d*", "").toUpperCase();

					
			File pFile = new File(new File("data", instance_dirid), "model_" + instance_id + ".txt");
			File sFile = new File(new File("data", instance_dirid), "assignment_" + instance_id + ".txt");
			Problem problem = new Problem(pFile, sFile);

			int[] processes = getIntVectorFromFile(new File(dir, "Err_Processes" + id + ".txt").getAbsolutePath());
			int[] machines = getIntVectorFromFile(new File(dir, "Err_Machines" + id + ".txt").getAbsolutePath());
			Solution sol = new SimpleSolution(SolutionIO.readSolutionFromFile(problem, f));
			System.out.println(sol.isFeasible());

			MipFastModel model = new MipFastModel("GurobiFastMipSolver");

			assertTrue("Input solution not feasible ", sol.isFeasible());

			Solution best = model.modifyAssignments(problem, sol, processes, machines, new Deadline(100000), true, true);
			assertNotNull("Empty output solution ", best);
			assertTrue("Output solution not feasible ", best.isFeasible());
			assertTrue("Invalid status returned by solver: " + model.solutionStatus, model.solutionStatus == 0
					|| model.solutionStatus == 1);
		}
	}


	
	public void testInfeasibleSolution2() {

		File pFile = new File("data/B/model_b_01.txt");
		File sFile = new File("data/B/assignment_b_01.txt");
		Problem problem = new Problem(pFile, sFile);
		int id = 0;
		int[] processes = getIntVectorFromFile("testFiles/infeasible/Processes" + id + "_b_01.txt");
		int[] machines = getIntVectorFromFile("testFiles/infeasible/Machines" + id + "_b_01.txt");
		Solution sol = new SimpleSolution(SolutionIO.readSolutionFromFile(problem, new File("testFiles/infeasible/FromSolution"
				+ id + "_b_01.txt")));
		System.out.println(sol.isFeasible());

		MipFastModel model = new MipFastModel("GurobiFastMipSolver");

		assertTrue("Input solution not feasible ", problem.isSolutionFeasible(sol));

		//change scalability

		CplexFastMipSolver.scaleParameter = 1;

		Solution best = model.modifyAssignments(problem, sol, processes, machines, new Deadline(100000), true, true);
		assertNotNull("Empty output solution ", best);
		assertTrue("Output solution not feasible ", best.isFeasible());
		assertTrue("Invalid status returned by solver: " + model.solutionStatus, model.solutionStatus == 0
				|| model.solutionStatus == 1 || model.solutionStatus == 9); //9 = numerical problem

	}
}
