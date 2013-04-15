package put.roadef;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class Common {
	public static final String LogPattern = "%r\t%-5p %c - %m%n";
	//public static final String LogPattern = "%m%n";
	public static final String AllLogFilename = "all.log"; 

	static boolean isModelFile(File file) {
		return file.getName().startsWith("model");
	}

	public static File getOriginalSolutionFile(File modelFile) {
		return new File(modelFile.getAbsolutePath().replaceFirst("model", "assignment"));
	}

	public static File getNewAssignmentFile(File solutionDirectory, File modelFile) {
		return new File(solutionDirectory, modelFile.getName().replaceFirst("model", "new_assignment"));
	}
	
	public static File getUpperBoundFile(File solutionDirectory, File newAssignmentFile) {
		String name = newAssignmentFile.getName();
		return new File(new File(newAssignmentFile.getParentFile().getParentFile(), "UpperBound"), name);
	}
		
	public static Appender addFileAppender(String logFileName) throws IOException {
		Appender myFileAppender = new FileAppender(new PatternLayout(LogPattern), logFileName, false);
		Logger.getRootLogger().addAppender(myFileAppender);
		return myFileAppender;
	}
	
	public static double computeImprovement(double cost, double originalCost) {
		if (originalCost == 0)
			return 0;
		return (100.0 * (originalCost - cost)) / originalCost;
	}
}
