package put.roadef;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public final class OriginalSolutionChecker {
	
	private static final String SOLUTION_CHECKER_PATH = "checker/solution_checker";
	
	/**
	 * Runs original solution_checker.
	 * 
	 * @return
	 * <li>the computed cost of the new solution,</li>
	 * <li>-1 if the new solution is invalid,</li>
	 * <li><code>null</code> if something goes wrong (e.g. you do not have <i>solution_checker</i> program or given files does not exists)</li> 
	 */
	public static Long check(File instance_file, File initial_solution_file, File new_solution_file)
	//(String instance_filename, String initial_solution_filename, String new_solution_filename)
	{
		if (!instance_file.exists() || !initial_solution_file.exists() || !new_solution_file.exists())
			return null;
		
		try
		{
			List<String> command = new ArrayList<String>();
			command.add(SOLUTION_CHECKER_PATH);
			command.add(instance_file.toString());
			command.add(initial_solution_file.toString());
			command.add(new_solution_file.toString());

			ProcessBuilder builder = new ProcessBuilder(command);

			final Process process = builder.start();
			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = br.readLine();
			
			if (line == null)
			{
				br.close();
				return -1l;
			}
			
			final String OKSTRING = "Solution is valid. Total objective cost is "; 
			
			if (line.startsWith(OKSTRING))
			{
				br.close();
				long cost = Long.parseLong(line.substring(OKSTRING.length()));
				return cost;
			}
			else
			{
				System.err.println(line);
				while ((line = br.readLine()) != null)
					System.err.println(line);
				return -1l;
			}
		}
		catch (Exception e)
		{
//			e.printStackTrace();
		}
		return null;
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println(check(
				new File("data/A/model_a1_1.txt"),
				new File("data/A/assignment_a1_1.txt"),
				new File("data/A/assignment_a1_1.txt")
				));

	}

}
