package put.roadef;

public class MachinePair {
	public MachinePair(int m1, int m2) {
		if (m1 > m2) {
			this.m1 = m1;
			this.m2 = m2;
		} else {
			this.m1 = m2;
			this.m2 = m1;
		}
	}

	public int m1;
	public int m2;

	@Override
	public int hashCode() {
		return m1 * Problem.MAX_MACHINES + m2;	
	}
	
	@Override
	public boolean equals(Object pair) {
		return m1 == ((MachinePair)pair).m1 && m2 == ((MachinePair)pair).m2; 
	}	
}