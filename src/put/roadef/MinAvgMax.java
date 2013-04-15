package put.roadef;

import java.util.Locale;

public class MinAvgMax {
	public int min;
	public int max;
	public double avg;
	public String toString() {
		return String.format(Locale.US, "[%d;%.1f;%d]", min, avg, max);
	}
}
