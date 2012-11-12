//package SetExpansion;



import java.util.ArrayList;

//import DQE.Weight;


public class Ne_candidate implements Comparable<Ne_candidate> {
	public int iNE;
	public double sim;
	public ArrayList<Weight> top_features;

	public Ne_candidate() {
		top_features = new ArrayList();
	}
	/*
	public int compareTo(Ne_candidate ne2) {
		Double sim1 = new Double(sim);
		Double sim2 = new Double(ne2.sim);
		
		return sim1.compareTo(sim2);
	}
	*/
	
	public int compareTo(Ne_candidate ne2) {
		if(sim > ne2.sim) return 1;
		else if(sim < ne2.sim) return -1;
		else return 0;
	}
}
