package put.roadef;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class QuickScanner {
	private LongArrayList elements;
	private int pointer = 0;
	char[] arr;
	
	public QuickScanner(File file) {
		try {
			arr = FileUtils.readFileToString(file).toCharArray();
			
			elements = split();
			cp = 0;
		} catch (IOException e) {
			throw new IllegalArgumentException("Wrong filename " + file.getPath(), e);
		}		
	}
	
	int cp;
	private LongArrayList split() {		
		LongArrayList elements = new LongArrayList();
		
		skipwhite();
		while (cp < arr.length) {
			elements.add(readlong());
			skipwhite();
		}			
		return elements;
	}

	private long readlong() {
		long val = 0;
		while (cp < arr.length && '0' <= arr[cp] && arr[cp] <= '9') {
			val = 10 * val + (arr[cp] - '0');
			cp++;
		}
		return val;
	}

	private void skipwhite() {
		while (cp < arr.length && (arr[cp] == ' ' || arr[cp] == '\n' || arr[cp] == '\r' || arr[cp] == '\t'))				
			cp++;
	}

	public long nextLong() {		
		return elements.getLong(pointer++);
	}
	
	public int nextInt() {
		return (int)(elements.getLong(pointer++));
	}
	
	public void close() {
		
	}
}
