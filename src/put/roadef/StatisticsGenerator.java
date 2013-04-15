package put.roadef;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import put.roadef.ProblemStatistics.Stat;

public class StatisticsGenerator {
	
	private static final String outputFileName = "stats.html";
	
	public static void main(String args[]) throws IOException {
		if (args.length == 0)
			usage();
		
		File modelDirectory = new File(args[0]); 
		File solutionDirectory = (args.length == 1 ? null : new File(args[1])) ;
										
		generateStatistics(modelDirectory, solutionDirectory);
	}
	
	private static void usage() {
		System.out.println("usage: StatisticsGenerator <dataDirectory> [<solutionDirectory>]");
		System.out.println("examples:");
		System.out.println("  java put/roadef/StatisticsGenerator data/B/");
		System.out.println("  java StatisticsGenerator data/B/ results/B/2012-02-09-12.52.37#5e1e2e7");
		System.exit(0);
	}

	public static void generateStatistics(File dataDirectory, File solutionDirectory) throws IOException {
		ArrayList<ProblemStatistics> statisticsForAllProblems = new ArrayList<ProblemStatistics>();
		
		boolean makeOriginalSolutionStats = false; 
		if (solutionDirectory == null) {
			solutionDirectory = dataDirectory;
			makeOriginalSolutionStats = true;
		}
		
		for (File file : dataDirectory.listFiles()) {
			if (!Common.isModelFile(file))
				continue;
			System.out.println(file.getName());

			File modelFile = file;
			File originalSolutionFile = Common.getOriginalSolutionFile(modelFile); 
			
			File solutionFile = originalSolutionFile;
			if (!makeOriginalSolutionStats)			
				solutionFile = Common.getNewAssignmentFile(solutionDirectory, modelFile);

			Problem problem = new Problem(modelFile, originalSolutionFile);
			Solution solution = new SmartSolution(SolutionIO.readSolutionFromFile(problem, solutionFile));
			
			ImmutableSolution upperBoundSolution = null;
			File ubFile = Common.getUpperBoundFile(solutionDirectory, solutionFile);
			if (ubFile.exists())
				upperBoundSolution = SolutionIO.readSolutionFromFile(problem, ubFile);
			
			statisticsForAllProblems.add(new ProblemStatistics(problem, solution, upperBoundSolution));
		}
		System.out.println("Finished");
		
		ArrayList<String> keys = new ArrayList<String>();
		for (ProblemStatistics ps : statisticsForAllProblems) {
			if (keys.size() < ps.stats.size()) {
				keys.clear();
				for(Stat s : ps.stats)
					keys.add(s.key);
			}
		}			
		
		BufferedWriter html = new BufferedWriter(new FileWriter(new File(solutionDirectory, outputFileName)));
		html.write("<html><head></head><body>");
		
		html.write("<table border=\"1\" cellspacing=\"0\"><tr>");
			
		html.write("<th></th>");
		for (ProblemStatistics ps : statisticsForAllProblems)
			html.write("<th>" + ps.getProblem().getName() + "</th>");
		html.write("\n");
		
		html.write("</tr>\n");
		
		for (String key : keys) {
			html.write("<tr>");
			html.write("<td>" + key + "</td>");			
			for (ProblemStatistics ps : statisticsForAllProblems) {				
				HashMap<String, String> mymap = ps.getMap();
				if (!mymap.containsKey(key))
					html.write("<td>n/a;</td>");
				else
					html.write("<td>" + mymap.get(key) + "</td>");				
			}
			html.write("</tr>\n");
		}		
		html.close();
	}
}
