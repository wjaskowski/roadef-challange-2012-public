package put.roadef;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class FileUtils {
	public static String readFileContent(File fileName) throws FileNotFoundException {
		Scanner scanner;
		scanner = new Scanner(fileName);  
		scanner.useDelimiter("\\Z");
		String result = "";
		if (scanner.hasNext())
			result = scanner.next(); 
		scanner.close();
		return result;
	}
}
