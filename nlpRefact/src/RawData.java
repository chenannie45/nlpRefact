//package SetExpansion;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;


public class RawData {
		
	public HashMap<Integer, HashMap<Integer, Double> > ne_feature_logpmi;
	public HashMap<Integer, String> index_ne;
	public HashMap<String, Integer> Name2Index;
	public HashMap<Integer, String> index_feature;
	public HashSet<String> stop_words;
	public HashMap<Integer, HashSet<Integer>>fid_ne;
	public HashSet<Integer> goldenSet;

	//	public static HashSet<Integer> feature_all;
	public HashSet<Integer> feature1;
	public HashSet<Integer> feature2;
	
	public void loadFiles(){
		loadStopword();
		loadIndex_NE();
		loadIndex_Feature();
		loadNE_Feat_Pmi();
	}
	
	public void setFileIndex_ne(String name){
		fileIndex_ne = name;
	}
	public void setFileIndex_Feature(String name){
		fileIndex_Feature = name;
	}
	public void SetFileStopword(String name){
		fileStopword = name;
	}
	public void setFileNe_Feature_Pmi(String name){
		fileNe_Feature_Pmi = name;
	}
	
	private	String fileIndex_ne;
	private String fileIndex_Feature;
	private String fileStopword;
	private String fileNe_Feature_Pmi;
	private Random generator =  new Random();
	
	
	private boolean containsOnlyLetters(String str) {
		//It can't contain only numbers if it's null or empty...
		if (str == null || str.length() == 0)
			return false;

		for (int i = 0; i < str.length(); i++) {

			//If we find a non-digit character we return false.
			if (!Character.isLetter(str.charAt(i)))
				return false;
		}

		return true;
	}
	
	private void loadIndex_NE(){
		index_ne = new HashMap<Integer,String>();
		Name2Index = new HashMap<String,Integer>();

		try {
			FileInputStream fstream = new FileInputStream(fileIndex_ne);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String str_line;
			while((str_line = br.readLine())!=null) {
				String [] items = str_line.split("\t");
				String ne = items[0];
				int index = Integer.parseInt(items[1]);
				index_ne.put(index, ne);
								
				Name2Index.put(ne, index);
							
			}

			System.out.println("finished reading entities' name");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void loadIndex_Feature(){
		index_feature = new HashMap<Integer,String>();

		try {
			FileInputStream fstream = new FileInputStream(fileIndex_Feature);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String str_line;
			while((str_line = br.readLine())!=null) {
				String [] items = str_line.split("\t");
				String feature = items[0];
				int index = Integer.parseInt(items[1]);

				index_feature.put(index, feature);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void loadNE_Feat_Pmi(){
		ne_feature_logpmi = new HashMap<Integer,HashMap<Integer, Double>>();
		fid_ne = new HashMap<Integer,HashSet<Integer>>();

		//		feature_all = new HashSet();
		feature1 = new HashSet<Integer>();
		feature2 = new HashSet<Integer>();

		try {
			FileInputStream fstream = new FileInputStream(fileNe_Feature_Pmi);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String str_line;
			while((str_line = br.readLine())!=null) {
		//		System.out.println(str_line);
				String [] items = str_line.split("\t");
				int iNE = Integer.parseInt(items[0]);
				int iFeature = Integer.parseInt(items[1]);

				if(!index_feature.containsKey(iFeature))
					continue;

				String str_feature = index_feature.get(iFeature);
	
				String [] str_feature_items = str_feature.split("_");
				if(str_feature_items.length<3)
					continue;

				String str_token = str_feature_items[str_feature_items.length-1];

				if( !containsOnlyLetters(str_token) ||
						stop_words.contains(str_token) ||
						str_feature.contains("det_") ||
						str_feature.contains("num_"))
					continue;
					

				if(items[2].equals("NaN"))
					continue;

				double logpmi = Double.parseDouble(items[2]);
	
				if(!ne_feature_logpmi.containsKey(iNE)) {
					HashMap<Integer, Double> hm = new HashMap<Integer,Double>();
					hm.put(iFeature, logpmi);
					ne_feature_logpmi.put(iNE, hm);
				}
				else {
					if(!ne_feature_logpmi.get(iNE).containsKey(iFeature)) {
						ne_feature_logpmi.get(iNE).put(iFeature, logpmi);
					}
				}
				
				///initial the feature index and NE hashmap
				if(!fid_ne.containsKey(iFeature)){
					HashSet<Integer> hs = new HashSet<Integer>();
					hs.add(iNE);
					fid_ne.put(iFeature, hs);
				}
				else{
					fid_ne.get(iFeature).add(iNE);
				}
					
	
				if(!feature1.contains(iFeature) && !feature2.contains(iFeature)) {
	
					int randomIndex = generator.nextInt(1000) % 2;
					if(randomIndex == 0)
						feature1.add(iFeature);
					else
						feature2.add(iFeature);
				}
				//				feature_all.add(iFeature);
		}
			
			System.out.print("Feature1.size(): ");
			System.out.println(feature1.size());
			System.out.print("Feature2.size(): ");
			System.out.println(feature2.size());
		} catch (IOException e) {
			// TODO must finish on spt 03. inment
			e.printStackTrace();
		}
	}
	
	private void loadStopword(){
		stop_words = new HashSet<String>();

		try {
			FileInputStream fstream = new FileInputStream(fileStopword);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String str_line;
			while((str_line = br.readLine())!=null) {
				if(str_line.charAt(0) == '#')
					continue;
				stop_words.add(str_line.toLowerCase());
			}

			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void load_goldenset(String filename) {
		goldenSet = new HashSet<Integer>();
		
		try {
			FileInputStream fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String str_line;
			while((str_line = br.readLine())!=null) {
				String [] items = str_line.split("\t");
				int index = Integer.parseInt(items[0]);
			
				goldenSet.add(index);
							
			}

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
}
