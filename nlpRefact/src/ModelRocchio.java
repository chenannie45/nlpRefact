//package SetExpansion;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class ModelRocchio {

	class EmbededModelRocchio {
		public HashMap<Integer, Double> weight;
		public HashMap<Integer, Double> centroid;		
	}
	
	public RawData dataset;
	
	public ArrayList<Integer> seeds;
	public ArrayList<Integer> set_wrong;
	
	double thres_sim;
	double gama;
	double beta;
	double weight_gain;
	double weight_discount;
	String outFileName;
	int maxRound;
	
	public int max_seeds;
	
	HashSet<Integer> candidate_pool;
	HashSet<Integer> candidate_featurePool;
	ArrayList<Ne_candidate> shared_feature = new ArrayList<Ne_candidate>();
	HashMap<Integer,Integer> shared_featureSet = new HashMap<Integer,Integer>();
	HashMap<Integer, Integer> shared_featurePos = new HashMap<Integer,Integer>();
	
	
	ModelRocchio(RawData dataset) {
		this.dataset = dataset;
		seeds = new ArrayList<Integer>();
		set_wrong = new ArrayList<Integer>();
	}
	
	EmbededModelRocchio model1;
	EmbededModelRocchio model2;
	
	
	
	
	
	public void setNumSeeds(int nSeed){
		max_seeds = nSeed;
	}
	
	public void setSeeds(String[] neNames){
		for(String name:neNames){
			seeds.add(findNE(name));
		}
	}
	
	
	private Integer findNE(String inputName){
		String formatName = inputName.replace(' ', '_');
		formatName = formatName.toLowerCase();
		Integer theIndex = dataset.Name2Index.get(formatName);
		int distance = 100;
		if (theIndex == null){
			Iterator<String> nameIternator = dataset.Name2Index.keySet().iterator();
			while(nameIternator.hasNext()){
				String curCity = nameIternator.next();
				LevenshteinDistance lDistance = new LevenshteinDistance();
				int curDist = lDistance.LD(formatName,curCity);	
				if(curDist < distance){
					distance = curDist;
					theIndex = dataset.Name2Index.get(curCity);
				}
			}									
			
		}
		return theIndex;
		
	}
	
	public void runRichico() throws IOException{
		
		
		BufferedWriter out_rp = new BufferedWriter(new FileWriter(outFileName + ".rp"));
		int name_wrong = -1;
		int name_correct = -1;
		ArrayList<Ne_candidate> list1 = new ArrayList<Ne_candidate>();
		ArrayList<Ne_candidate> list2 = new ArrayList<Ne_candidate>();
		
		for(int i = 1;i <= maxRound;i++){
			for(Integer nameID : seeds){
				System.out.println(dataset.index_ne.get(nameID));
			}
			
			
			String str_f_out_prec = String.format("%s.%d.prec", outFileName, i);
			BufferedWriter out_prec = new BufferedWriter(new FileWriter(str_f_out_prec));
			
			train();
			list1 = apply_classifier(candidate_pool, 1);
			list2 = apply_classifier(candidate_pool, 2);
			
			Collections.sort(list1,Collections.reverseOrder());
			Collections.sort(list2,Collections.reverseOrder());
			
			HashMap<Integer, Double> map = get_precision_2view(list1, list2, outFileName, i);
			name_correct = get_max_pos(get_pos_candidate(list1, list2));
			name_wrong = get_max_neg(get_neg_candidate(list1, list2));
			if(name_correct!=-1){
				seeds.add(name_correct);
			}
			if(name_wrong!=-1){
				set_wrong.add(name_wrong);
			}
			
			
		
			for(int j = 1;j < map.keySet().size();j++) {
				int rank = j;
				String line = String.format("%d\t%f\n", 
						rank, map.get(rank));

				out_prec.write(line);

				if(rank==dataset.goldenSet.size()) {
					String line_rp = String.format("%d\t%f\n", 
							i, map.get(rank));
					out_rp.write(line_rp);
				}
			}
			out_prec.close();
		}
		
		out_rp.close();
	}
	
	public void init()  {
//		dataset = rawData;
		weight_gain = 1.1;
		weight_discount = 0.9;

		beta = 0.75;
		gama = -0.25;		

		thres_sim = 0.01;
		outFileName = "logr";
		maxRound = 10;
		
		candidate_pool = new HashSet<Integer>();
		candidate_featurePool = new HashSet<Integer>();
		
//		loadGoldSet();   /*move that to data class*/
		for(Integer name:seeds){
			expand_sharedfeature_pool(name);
		}
		constructCandidatePool();
	}
	
	private void expand_sharedfeature_pool(Integer name) {
		Iterator<Integer> fIterator	= dataset.ne_feature_logpmi.get(name).keySet().iterator();
		while(fIterator.hasNext()){
			int feature = fIterator.next();
			if(candidate_featurePool.contains(feature)){
				if(!shared_featureSet.keySet().contains(feature)){
					System.out.println("the shared feature is " + feature + ": " + dataset.index_feature.get(feature));
					Ne_candidate f_pmi = new Ne_candidate();
					f_pmi.iNE = feature;
					f_pmi.sim = dataset.ne_feature_logpmi.get(name).get(feature);
					
					shared_featurePos.put(feature, shared_feature.size());
					shared_feature.add(f_pmi);
					shared_featureSet.put(feature, 1);
					System.out.println("similarity is " + dataset.ne_feature_logpmi.get(name).get(feature));
			//		candidate_pool.addAll(fid_ne.get(feature));//only add the NEs that has shared features
				}
				else{
					int times = shared_featureSet.get(feature)+1;
					shared_featureSet.remove(feature);
					shared_featureSet.put(feature, times);
					int position = shared_featurePos.get(feature);
					double curAvg = shared_feature.get(position).sim;
					shared_feature.get(position).sim = (curAvg*(times-1)+dataset.ne_feature_logpmi.get(name).get(feature))/times;
				}
				
				continue;
			}
			candidate_featurePool.add(feature);
//			candidate_pool.addAll(fid_ne.get(feature));
			//System.out.print("feature_id:"+feature+" feature_size:"+fid_ne.get(feature).size());
			//System.out.println(" feature:"+index_feature.get(feature));
		}
	}
	
	private void constructCandidatePool() {
		// TODO Auto-generated method stub
		Collections.sort(shared_feature, Collections.reverseOrder());
		for(int i = 0; i < shared_feature.size(); i++){
			if(candidate_pool.size() < 15000){
				candidate_pool.addAll(dataset.fid_ne.get(shared_feature.get(i).iNE));
				System.out.println(dataset.index_feature.get(shared_feature.get(i).iNE) + " " + shared_feature.get(i).sim);
			}
			else{
				break;
			}
		}
	}


	/*
	 * 
	 */
	public void train() {
		model1 = train_classifier(1);
		model2 = train_classifier(2);
	}
	
	public EmbededModelRocchio train_classifier(int id_classifier) {
		EmbededModelRocchio singleModel = new EmbededModelRocchio();
		
		ArrayList<Ne_candidate> vec_words = new ArrayList<Ne_candidate>();
		HashMap<Integer, Double> weight = new HashMap();
		
		HashMap<Integer, Double> centroid =
			get_centroid(seeds, id_classifier, set_wrong);

		weight = increase_weight_type3(seeds, id_classifier, weight);
		weight = decrease_weight_type3(set_wrong, id_classifier, weight);
		
		singleModel.centroid = centroid;
		singleModel.weight = weight;
		
		return singleModel;
	}
	
	
	public void test(ArrayList<Ne_candidate> vect1, ArrayList<Ne_candidate> vect2,int round) {
		Collections.sort(vect1,Collections.reverseOrder());
		Collections.sort(vect2, Collections.reverseOrder());
		
		try {
			HashMap<Integer, Double> map = get_precision_2view(vect1, vect2, outFileName, round);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ArrayList<Ne_candidate> apply_classifier(HashSet<Integer> candidate_pool, int id_classifier) {
		ArrayList<Ne_candidate> vec_words = new ArrayList<Ne_candidate>();
		
		EmbededModelRocchio model = null;
		
		if(id_classifier==1) model = model1; else model = model2;
		
		double centroid_richicoo_sqrtX2 = get_centroid_richicoo_sqrtX2(model.centroid);
		HashMap<Integer, Double> centroid_richicoo = get_centroid_richicoo(seeds, id_classifier, set_wrong);

		
		System.out.println("calculating similarity for all NPs.. ");
		int count = 0;
		int old_perc = -1;

		Iterator<Integer> it4 = dataset.ne_feature_logpmi.keySet().iterator();
		while(it4.hasNext()) {
			int iNE = it4.next();

			if(!candidate_pool.contains(iNE))
				continue;

			// progress indicator
			count++;
			int finish_perc = count * 100 / dataset.ne_feature_logpmi.keySet().size();
			if(finish_perc % 10 == 0 && finish_perc != old_perc) {
				String str_line = String.format("%d/100 of %d", finish_perc, dataset.ne_feature_logpmi.size());
				System.out.println(str_line);
			}

			// skip known correct and incorrect ones
			if(seeds.contains(iNE))
				continue;
			if(set_wrong.contains(iNE))
				continue;

			if(centroid_richicoo_sqrtX2 != 0) {
				double cur_sqrtX2 = get_iNE_sqrtX2(iNE, id_classifier, model.weight);
				double sim = 0;

				//				if(type3_adjust_type == Type3_adjust_type.Expo)

				sim = cal_simularity(centroid_richicoo, dataset.ne_feature_logpmi.get(iNE), centroid_richicoo_sqrtX2, cur_sqrtX2, id_classifier, model.weight);

				if(sim >= thres_sim) {
					Ne_candidate f = new Ne_candidate();
					f.iNE = iNE;
					f.sim = sim;
					vec_words.add(f);
				}
			}
			old_perc = finish_perc;
		}

		System.out.println("finish calculating similarity.");

		//		System.out.print("vec_words.size(): ");
		//		System.out.println(vec_words.size());

		return vec_words;
	}
	

	public HashMap<Integer, Integer> get_pos_candidate(ArrayList<Ne_candidate> list1, ArrayList<Ne_candidate> list2) {
		HashMap<Integer, Integer> posCands = new HashMap<Integer, Integer>();
		
		ArrayList<Integer> judge_list = new ArrayList<Integer>();
		
		int searchScope = list1.size();
		if(searchScope >list2.size()){
			searchScope = list2.size();
		}
		
		for(int i = 0; i < searchScope; i++){
			Ne_candidate nec1 = list1.get(i);
			if(judge_list.contains(nec1.iNE)||seeds.contains(nec1.iNE)||set_wrong.contains(nec1.iNE)){
				continue;
			}
			judge_list.add(nec1.iNE);
			posCands.put(nec1.iNE,0);
			
			Ne_candidate nec2 = list2.get(i);
			if(judge_list.contains(nec2.iNE)||seeds.contains(nec2.iNE)||set_wrong.contains(nec2.iNE)){
				continue;
			}
			judge_list.add(nec2.iNE);
			posCands.put(nec2.iNE,0);		
			if(posCands.size()>= 10){
				break;
			}
		}
		return posCands;
	}
	
	private Integer get_max_pos(HashMap<Integer, Integer> poscands){
		int maxDim = 0;
		int maxCand = -1;
		for(int name : poscands.keySet()){
			if(is_in_golden_strict(name)){
				if(dataset.ne_feature_logpmi.get(name).size() > maxDim){
					maxDim = dataset.ne_feature_logpmi.get(name).size();
					maxCand = name;
				}
			}
		}
		return maxCand;
		
	}
	
	private Integer get_max_neg(HashMap<Integer, Integer> negCands){
		int maxDim = 0;
		int maxCand = -1;
		
		for(int name : negCands.keySet()){
			if(!is_in_golden_fuzzy(name)){
				if(dataset.ne_feature_logpmi.get(name).size() > maxDim){
					maxDim = dataset.ne_feature_logpmi.get(name).size();
					maxCand = name;
				}
			}
		}
		return maxCand;
		
	}

	public HashMap<Integer, Integer> get_neg_candidate(ArrayList<Ne_candidate> list1, ArrayList<Ne_candidate> list2) {
		HashMap<Integer, Integer> negCand = new HashMap<Integer, Integer>();
		
		ArrayList<Integer> top1 = new ArrayList<Integer>();
		ArrayList<Integer> top2 = new ArrayList<Integer>();
		
		int searchScope = list1.size();
		if(searchScope > list2.size()){
			searchScope = list2.size();
		}
		
		for(int i=0; i<searchScope-seeds.size(); i++) {
			top1.add(list1.get(i).iNE);
			top2.add(list2.get(i).iNE);
		}

		for(int i=0; i<searchScope-seeds.size(); i++) {
			int name1 = top1.get(i);
			int name2 = top2.get(i);

			if(!top2.contains(name1) && !set_wrong.contains(name1)&&!seeds.contains(name1)) {
				negCand.put(name1, 0);
			}

			if(!top1.contains(name2) && !set_wrong.contains(name2)&&!seeds.contains(name2)) {
				negCand.put(name2,0);
			}
			if(negCand.size() >= 10){
				break;
			}
		}
		
		return negCand;
	}
	
	
	private boolean is_in_golden_fuzzy(int iNE) {
		boolean is_name_golden = false;

		// fuzy match with golden set
		Iterator<Integer> it_golden = dataset.goldenSet.iterator();
		while(it_golden.hasNext()) {
			int iSeed = it_golden.next();
			String str_seed = dataset.index_ne.get(iSeed);
			String str_name = dataset.index_ne.get(iNE);

			LevenshteinDistance LD = new LevenshteinDistance();
			// contains the other
			if(str_name.contains(str_seed) || str_name.equals(str_seed)
					// edit distance <= 2
					|| LD.LD(str_name, str_seed)<=2) {
				is_name_golden = true;
				return true;
			}			
		}

		return is_name_golden;
	}

	private boolean is_in_golden_strict(int iNE) {
		if(dataset.goldenSet.contains(iNE))
			return true;
		else
			return false;
	}
	
	// id_classifier = 1, 1st half view
	// id_classifier = 2, 2nd half view
	// id_classifier = 3, entire feature space
	public double cal_simularity(HashMap<Integer, Double> v1, HashMap<Integer, Double> v2, 
			double sqrt_v1, double sqrt_v2,
			int id_classifier, HashMap<Integer, Double> weight) {

		if(sqrt_v1 == 0 || sqrt_v2 == 0 || v1.isEmpty() || v2.isEmpty())
			return 0;

		double v1v2 = 0;
		if(v1.size() < v2.size()) {
			Iterator<Integer> it = v1.keySet().iterator();
			while(it.hasNext()) {
				int iFeature = it.next();
				if(v2.containsKey(iFeature)) {
					if(id_classifier == 1 && !dataset.feature1.contains(iFeature))
						continue;
					if(id_classifier == 2 && !dataset.feature2.contains(iFeature))
						continue;

					// weight features;
					double w;
					if(weight.containsKey(iFeature))
						w = weight.get(iFeature);
					else
						w = 1;

					double d_a = w * v1.get(iFeature);
					double d_b = w * v2.get(iFeature);
					double d_c = d_a * d_b;
					if(!Double.isNaN(d_c)) {
						v1v2 += d_c;

						Weight we = new Weight();
						we.iFeature = iFeature;
						we.w = d_c;
					}
				}
			}
		}
		else {
			Iterator<Integer> it = v2.keySet().iterator();
			while(it.hasNext()) {
				int iFeature = it.next();
				if(v1.containsKey(iFeature)) {
					if(id_classifier == 1 && !dataset.feature1.contains(iFeature))
						continue;
					if(id_classifier == 2 && !dataset.feature2.contains(iFeature))
						continue;

					// weight feature
					double w;
					if(weight.containsKey(iFeature))
						w = weight.get(iFeature);
					else
						w = 1;

					double d_a = w * v1.get(iFeature);
					double d_b = w * v2.get(iFeature);
					double d_c = d_a * d_b;
					if(!Double.isNaN(d_c)) {
						v1v2 += d_c;

						Weight we = new Weight();
						we.iFeature = iFeature;
						we.w = d_c;
					}
				}
			}
		}

		return v1v2 / (sqrt_v1 * sqrt_v2);
	}
	
	// calculate centroid based on richicco formula
	public HashMap<Integer, Double> get_centroid_richicoo(ArrayList<Integer> seeds, int id_classifier, ArrayList<Integer> set_wrong) {
		// parameters in Richico formula
		Set<Integer> validfeatureIdx_all = new HashSet<Integer>();

		HashMap<Integer, Double> centroid = new HashMap();

		// centroid for the initial set of seeds
		HashMap<Integer, Double> centroid_raw = new HashMap();
		int nseeds = 0;
		Iterator<Integer> it_raw = seeds.iterator();
		while(it_raw.hasNext()) {
			//			nseeds++;
			//			if(nseeds > max_seeds)
			//				continue;

			int iSeed = it_raw.next();

			Iterator<Integer> it2 = dataset.ne_feature_logpmi.get(iSeed).keySet().iterator();
			while(it2.hasNext()) {
				int iFeature = it2.next();

				if(!validfeatureIdx_all.contains(iFeature))
					validfeatureIdx_all.add(iFeature);

				// normalize
				double logpmi = dataset.ne_feature_logpmi.get(iSeed).get(iFeature)/seeds.size();

				if(id_classifier==1 && !dataset.feature1.contains(iFeature))
					continue;
				if(id_classifier==2 && !dataset.feature2.contains(iFeature))
					continue;

				if(!centroid_raw.containsKey(iFeature))
					centroid_raw.put(iFeature, logpmi);
				else {
					double old_value = centroid_raw.get(iFeature);
					centroid_raw.put(iFeature, old_value + logpmi);
				}
			}
		}

		// centroid for pos feedbacks
		HashMap<Integer, Double> centroid_pos = new HashMap();

		// centroid for neg feedbacks
		HashMap<Integer, Double> centroid_neg = new HashMap();
		Iterator<Integer> it_neg = set_wrong.iterator();
		while(it_neg.hasNext()) {
			int iSeed = it_neg.next();

			Iterator<Integer> it2 = dataset.ne_feature_logpmi.get(iSeed).keySet().iterator();
			while(it2.hasNext()) {
				int iFeature = it2.next();

				if(!validfeatureIdx_all.contains(iFeature))
					validfeatureIdx_all.add(iFeature);

				// normalize
				double logpmi = dataset.ne_feature_logpmi.get(iSeed).get(iFeature)*gama/set_wrong.size();

				if(id_classifier==1 && !dataset.feature1.contains(iFeature))
					continue;
				if(id_classifier==2 && !dataset.feature2.contains(iFeature))
					continue;

				if(!centroid_neg.containsKey(iFeature))
					centroid_neg.put(iFeature, logpmi);
				else {
					double old_value = centroid_neg.get(iFeature);
					centroid_neg.put(iFeature, old_value + logpmi);
				}
			}
		}

		// combine all features
		for(Integer idx_feature : validfeatureIdx_all) {
			double logpmi = 0;

			if(centroid_raw.containsKey(idx_feature))
				logpmi += centroid_raw.get(idx_feature);

			//			if(centroid_pos.containsKey(idx_feature))
			//				logpmi += centroid_pos.get(idx_feature);

			if(centroid_neg.containsKey(idx_feature))
				logpmi += centroid_neg.get(idx_feature);

			// no Negative weighted items are added

			if(Double.isNaN(logpmi))
				continue;			
			if(logpmi <= 0)
				continue;

			centroid.put(idx_feature, logpmi);
		}

		return centroid;
	}

	
	public double get_iNE_sqrtX2(int iNE, int id_classifier, HashMap<Integer, Double> weight) {
		double cur_sqrtX2 = 0;
		Iterator<Integer> it5 = dataset.ne_feature_logpmi.get(iNE).keySet().iterator();
		while(it5.hasNext()) {
			int iFeature = it5.next();
			double logpmi = dataset.ne_feature_logpmi.get(iNE).get(iFeature);

			if(id_classifier==1 && !dataset.feature1.contains(iFeature))
				continue;
			if(id_classifier==2 && !dataset.feature2.contains(iFeature))
				continue;

			double w = 1;
			if(!weight.containsKey(iFeature))
				w = 1;
			else
				w = weight.get(iFeature);

			cur_sqrtX2 += w * w * logpmi * logpmi;
		}
		cur_sqrtX2 = Math.sqrt(cur_sqrtX2);

		return cur_sqrtX2;
	}
	
	public static double get_centroid_richicoo_sqrtX2(HashMap<Integer, Double> centroid) {
		double centroid_sqrtX2 = 0;
		double sum = 0;
		Iterator<Integer> it3 = centroid.keySet().iterator();
		while(it3.hasNext()) {
			int iFeature = it3.next();
			double logpmi = centroid.get(iFeature);

			if(Double.isNaN(logpmi))
				continue;

			sum += logpmi * logpmi;

			//String str_test = String.format("iFeature: %d, w: %f, logpmi: %f", iFeature, w, logpmi);
			//System.out.println(str_test);
		}
		centroid_sqrtX2 = Math.sqrt(sum);

/*		System.out.print("DBG[get_centroid_richicoo_sqrtX2]: centroid.size(): ");
		System.out.print(centroid.size());
		System.out.print(", centroid_sqrtX2: ");
		System.out.println(centroid_sqrtX2);*/////////////////////////

		return centroid_sqrtX2;
	}
	
	// return value:
	// HashMap<Integer, Double> centroid
	public HashMap<Integer, Double> get_centroid(ArrayList<Integer> seeds, int id_classifier) {
		HashMap<Integer, Double> centroid = new HashMap();
		Iterator<Integer> it = seeds.iterator();
		while(it.hasNext()) {
			int iSeed = it.next();

			Iterator<Integer> it2 = dataset.ne_feature_logpmi.get(iSeed).keySet().iterator();
			while(it2.hasNext()) {
				int iFeature = it2.next();
				// ? bug to fix / TODO: 
				double logpmi = dataset.ne_feature_logpmi.get(iSeed).get(iFeature)/seeds.size();

				if(id_classifier==1 && !dataset.feature1.contains(iFeature))
					continue;
				if(id_classifier==2 && !dataset.feature2.contains(iFeature))
					continue;

				if(!centroid.containsKey(iFeature))
					centroid.put(iFeature, logpmi);
				else {
					double old_value = centroid.get(iFeature);
					centroid.put(iFeature, old_value + logpmi);
				}
			}
		}

		return centroid;
	}
	
	// calculate centroid based on richicco formula
	public HashMap<Integer, Double> get_centroid(ArrayList<Integer> seeds, int id_classifier, ArrayList<Integer> set_wrong) {
		// parameters in Richico formula
		Set<Integer> validfeatureIdx_all = new HashSet<Integer>();

		HashMap<Integer, Double> centroid = new HashMap();

		// centroid for the initial set of seeds
		HashMap<Integer, Double> centroid_raw = new HashMap();
		int nseeds = 0;
		Iterator<Integer> it_raw = seeds.iterator();
		while(it_raw.hasNext()) {
			//			nseeds++;
			//			if(nseeds > max_seeds)
			//				continue;

			int iSeed = it_raw.next();

			Iterator<Integer> it2 = dataset.ne_feature_logpmi.get(iSeed).keySet().iterator();
			while(it2.hasNext()) {
				int iFeature = it2.next();

				if(!validfeatureIdx_all.contains(iFeature))
					validfeatureIdx_all.add(iFeature);

				// normalize
				double logpmi = dataset.ne_feature_logpmi.get(iSeed).get(iFeature)/seeds.size();

				if(id_classifier==1 && !dataset.feature1.contains(iFeature))
					continue;
				if(id_classifier==2 && !dataset.feature2.contains(iFeature))
					continue;

				if(!centroid_raw.containsKey(iFeature))
					centroid_raw.put(iFeature, logpmi);
				else {
					double old_value = centroid_raw.get(iFeature);
					centroid_raw.put(iFeature, old_value + logpmi);
				}
			}
		}

		// centroid for pos feedbacks
		HashMap<Integer, Double> centroid_pos = new HashMap();
		/*
		nseeds = 0;
		Iterator<Integer> it_pos = seeds.iterator();
		while(it_pos.hasNext()) {
			nseeds++;
			if(nseeds <= max_seeds)
				continue;

			int iSeed = it_pos.next();

			Iterator<Integer> it2 = ne_feature_logpmi.get(iSeed).keySet().iterator();
			while(it2.hasNext()) {
				int iFeature = it2.next();

				if(!validfeatureIdx_all.contains(iFeature))
					validfeatureIdx_all.add(iFeature);

				// normalize
				double logpmi = ne_feature_logpmi.get(iSeed).get(iFeature)*beta/(seeds.size() - max_seeds);

				if(id_classifier==1 && !feature1.contains(iFeature))
					continue;
				if(id_classifier==2 && !feature2.contains(iFeature))
					continue;

				if(!centroid_pos.containsKey(iFeature))
					centroid_pos.put(iFeature, logpmi);
				else {
					double old_value = centroid_pos.get(iFeature);
					centroid_pos.put(iFeature, old_value + logpmi);
				}
			}
		}
		 */
		// centroid for neg feedbacks
		HashMap<Integer, Double> centroid_neg = new HashMap();
		Iterator<Integer> it_neg = set_wrong.iterator();
		while(it_neg.hasNext()) {
			int iSeed = it_neg.next();

			Iterator<Integer> it2 = dataset.ne_feature_logpmi.get(iSeed).keySet().iterator();
			while(it2.hasNext()) {
				int iFeature = it2.next();

				if(!validfeatureIdx_all.contains(iFeature))
					validfeatureIdx_all.add(iFeature);

				// normalize
				double logpmi = dataset.ne_feature_logpmi.get(iSeed).get(iFeature)*gama/set_wrong.size();

				if(id_classifier==1 && !dataset.feature1.contains(iFeature))
					continue;
				if(id_classifier==2 && !dataset.feature2.contains(iFeature))
					continue;

				if(!centroid_neg.containsKey(iFeature))
					centroid_neg.put(iFeature, logpmi);
				else {
					double old_value = centroid_neg.get(iFeature);
					centroid_neg.put(iFeature, old_value + logpmi);
				}
			}
		}

		// combine all features
		for(Integer idx_feature : validfeatureIdx_all) {
			double logpmi = 0;

			if(centroid_raw.containsKey(idx_feature))
				logpmi += centroid_raw.get(idx_feature);

			//			if(centroid_pos.containsKey(idx_feature))
			//				logpmi += centroid_pos.get(idx_feature);

			if(centroid_neg.containsKey(idx_feature))
				logpmi += centroid_neg.get(idx_feature);

			// no Negative weighted items are added

			if(Double.isNaN(logpmi))
				continue;			
			if(logpmi <= 0)
				continue;

			centroid.put(idx_feature, logpmi);
		}

		return centroid;
	}
	
	// return value:
	// HashMap<Integer, Double> weight
	public HashMap<Integer, Double> increase_weight_type3(ArrayList<Integer> seeds, int id_classifier, HashMap<Integer, Double> weight) {
		Iterator<Integer> it = seeds.iterator();
		int nseeds = 0;
		while(it.hasNext()) {
			nseeds++;

			// for the initial seed set, don't adjust weight
			if(nseeds <= max_seeds)
				continue;

			int iSeed = it.next();

			Iterator<Integer> it2 = dataset.ne_feature_logpmi.get(iSeed).keySet().iterator();
			while(it2.hasNext()) {
				int iFeature = it2.next();

				if(id_classifier==1 && !dataset.feature1.contains(iFeature))
					continue;
				if(id_classifier==2 && !dataset.feature2.contains(iFeature))
					continue;

				if(!weight.containsKey(iFeature))
					weight.put(iFeature, new Double(1 * weight_gain));
				else {
					double old_value = weight.get(iFeature);
					old_value *= weight_gain;
					weight.put(iFeature, old_value);
				}
			}
		}

		return weight;
	}
	
	// return value:
	// HashMap<Integer, Double> weight
	public HashMap<Integer, Double> decrease_weight_type3(ArrayList<Integer> set_wrong, int id_classifier, HashMap<Integer, Double> weight) {
		Iterator<Integer> it22 = set_wrong.iterator();
		while(it22.hasNext()) {
			int name_wrong = it22.next();

			Iterator<Integer> it11 = dataset.ne_feature_logpmi.get(name_wrong).keySet().iterator();
			while(it11.hasNext()) {
				int iFeature = it11.next();
				if(id_classifier==1 && !dataset.feature1.contains(iFeature))
					continue;
				if(id_classifier==2 && !dataset.feature2.contains(iFeature))
					continue;

				if(!weight.containsKey(iFeature)) {
					weight.put(iFeature, new Double(1 * weight_discount));
				}
				else {
					double old_value = weight.get(iFeature);
					weight.put(iFeature, new Double(old_value * weight_discount));
				}
			}
		}

		return weight;
	}
	
	// get precision from two views
	public HashMap<Integer, Double> get_precision_2view(ArrayList<Ne_candidate> vec_name1,
			ArrayList<Ne_candidate> vec_name2, String out_file, int round) throws IOException {

		String f_out = String.format("%s.%d.rank", out_file, round);
		BufferedWriter out_list = new BufferedWriter(new FileWriter(f_out));

		HashMap<Integer, Double> map_p = new HashMap<Integer, Double>();

		ArrayList<Ne_candidate> words_sim = new ArrayList<Ne_candidate>();

		// to speed up, use 20000 instead of words_sim.size()
		int max_index = 20000;
		if(vec_name1.size() < max_index)
			max_index = vec_name1.size();
		if(vec_name2.size() < max_index)
			max_index = vec_name2.size();

		for(int cc = 0; cc < max_index; cc++) {
			Ne_candidate f = new Ne_candidate();

			double sim1 = 0, sim2 = 0;
			sim1 = vec_name1.get(cc).sim;

			for(int cc2 = 0; cc2 < max_index; cc2++) {
				if(vec_name2.get(cc2).iNE == f.iNE) {
					sim2 = vec_name2.get(cc2).sim;
					break;
				}
			}

			f.iNE = vec_name1.get(cc).iNE;
			f.sim = 1-(1-sim1)*(1-sim2);

			words_sim.add(f);
		}

		Collections.sort(words_sim, Collections.reverseOrder());
		
		
		System.out.println("20 top ranked NE are:");
		for(int i = 0; i < 20; i++){
			System.out.println(dataset.index_ne.get(words_sim.get(i).iNE));
		}

		for(int i=1; i <= seeds.size(); i++)
			map_p.put(i, new Double(1.0));

		int valid = seeds.size();
		int nonNegNE = seeds.size();
		// 5 times size of golden set, to find unincluded items in golden set
		for(int i=seeds.size()+1; i <= words_sim.size(); i++) {
//		for(int i=seeds[exper_no-1].size()+1; i <= goldenset.size() * 5; i++) {
			
			int is_valid = 0;
			int cur_iNE = words_sim.get(i-seeds.size()-1).iNE;
			
			if(set_wrong.contains(cur_iNE)){
				continue;
			}
			nonNegNE++;
			if(dataset.goldenSet.contains(cur_iNE)) {
				valid++;
				is_valid = 1;
			}
			String str_test = String.format("%d\t%d\t%f\t%s\t%d", i, is_valid, words_sim.get(i-seeds.size()-1).sim, dataset.index_ne.get(cur_iNE), cur_iNE);
			out_list.write(str_test);
			for(int j = 0; j < words_sim.get(i-seeds.size()-1).top_features.size(); j++) {
				out_list.write("\t");
				int cur_iFeature = words_sim.get(i-seeds.size()-1).top_features.get(j).iFeature;
				out_list.write(dataset.index_feature.get(cur_iFeature));
			}
			out_list.write("\n");

			map_p.put(nonNegNE, new Double(1.0*valid/nonNegNE));
		//	map_p.put(i, new Double(1.0*valid/i));
	
			
		}

		out_list.close();
		return map_p;
	}
	
	public static void main() {
		
	}
}
