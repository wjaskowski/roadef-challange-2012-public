package put.roadef;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class MyArrayUtils {

	public static void shuffle(int[] arr, Random random) {
		for (int i = 0; i < arr.length; ++i) {
			int nextPos = random.nextInt(arr.length - i) + i;
			int tmp = arr[nextPos];
			arr[nextPos] = arr[i];
			arr[i] = tmp;
		}
	}
	
	public static void shuffle(IntArrayList arr, Random random) {
		for (int i = 0; i < arr.size(); ++i) {
			int nextPos = random.nextInt(arr.size() - i) + i;
			int tmp = arr.get(nextPos);
			arr.set(nextPos, arr.get(i));
			arr.set(i, tmp);
		}
	}

	public static int[] random(int[] array, Random random, int numDraws) {
		int draws = Math.min(numDraws, array.length);
		int[] randomElements = new int[draws];

		for (int i = 0; i < draws; i++) {
			int index = random.nextInt(array.length - i);
			int randomElement = array[index];
			array[index] = array[array.length - i - 1];
			randomElements[i] = randomElement;
		}

		return randomElements;
	}

	public static void sort(int[] array, final long[] keys) {
		Integer[] a = org.apache.commons.lang.ArrayUtils.toObject(array);

		Arrays.sort(a, new Comparator<Integer>() {
			@Override
			public int compare(Integer m1, Integer m2) {
				long diff = keys[m1] - keys[m2];
				if (diff < 0)
					return -1;
				else if (diff > 0)
					return 1;
				else
					return 0;
			}
		});
		for (int i = 0; i < array.length; ++i)
			array[i] = a[i];
	}

	
}
