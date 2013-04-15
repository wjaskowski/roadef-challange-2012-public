package put.roadef;

public class MachinePairPotential extends MachinePair implements Comparable<MachinePairPotential>{

	public long potential;
	
	public MachinePairPotential(int m1, int m2, long potential) {
		super(m1, m2);
		this.potential = potential;
	}

	@Override
	public int compareTo(MachinePairPotential mpp) {
		if (this.potential > mpp.potential) {
			return -1;
		} else if (this.potential < mpp.potential) {
			return 1;
		} else if (m1 != mpp.m1)  {
			return m1 - mpp.m1;
		} else if (m2 != mpp.m2) {
			return m2 - mpp.m2;
		} else {
			return 0;
		}
	}
}
