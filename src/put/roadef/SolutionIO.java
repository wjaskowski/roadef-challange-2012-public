package put.roadef;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public final class SolutionIO {
	static public ReadOnlySolution readSolutionFromFile(Problem problem, File solutionFile) {
		QuickScanner scanner = new QuickScanner(solutionFile);		

		int[] assignment = new int[problem.getNumProcesses()];
		for (int p = 0; p < problem.getNumProcesses(); p++) {
			assignment[p] = scanner.nextInt();
		}

		ReadOnlySolution solution = new ReadOnlySolution(problem, assignment);
		return solution;
	}

	static public void writeSolutionToFile(ImmutableSolution solution, File file) {
		try {
			FileWriter fstream;
			fstream = new FileWriter(file);
			BufferedWriter out = new BufferedWriter(fstream);
			for (int p = 0; p < solution.getProblem().getNumProcesses(); p++)
				out.write(solution.getMachine(p) + " ");
			out.close();
			//FIXME: Ja bym to napisal inaczej, ale w tej chwili nie bede zmienial)
		} catch (IOException e) {
			throw new IllegalArgumentException("Problem with the output file!", e);
		}
	}
	
	static public void writeArrayToFile(int[] array, File file) {
		try {
			FileWriter fstream;
			fstream = new FileWriter(file);
			BufferedWriter out = new BufferedWriter(fstream);
			for (int p = 0; p < array.length; ++p)
				out.write(array[p] + " ");
			out.close();
		} catch (IOException e) {
			throw new IllegalArgumentException("Problem with the output file!", e);
		}
	}
}
