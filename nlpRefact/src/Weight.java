//package SetExpansion;


public class Weight implements Comparable<Weight> {
	public int iFeature;
	public double w;
	
	public int compareTo(Weight we2) {
		if(w > we2.w) return 1;
		else if(w < we2.w) return -1;
		else return 0;
	}
}
