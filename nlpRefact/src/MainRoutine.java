//package SetExpansion;

import java.io.IOException;
import java.util.ArrayList;

public class MainRoutine {
	
	public static void main(String[] argv) throws IOException {
		RawData dataset = new RawData();
		String datFilesRoot = "/proteus106/cc3263/datSetExpansion_forChenChen/";

		dataset.SetFileStopword(datFilesRoot + "stopwords.dat");
		dataset.setFileIndex_ne(datFilesRoot + "jetne_index.dat");
		dataset.setFileIndex_Feature(datFilesRoot + "feature_index.dat");
		dataset.setFileNe_Feature_Pmi(datFilesRoot + "jetne_feature_logpmi.dat");
		dataset.loadFiles();
	
		String file_goldenset = "/proteus106/cc3263/datSetExpansion_forChenChen/goldsets/capitals";
		dataset.load_goldenset(file_goldenset);
		
		ModelRocchio model = new ModelRocchio(dataset);
		
		String[] names = {"Beijing","Paris","London"};
		System.out.println("Begin feature Pruned:");
		model.setNumSeeds(3);
		model.setSeeds(names);
		model.init();
		model.runRichico();
		
		System.out.println("Begin Original:");
		ModelRocchioOrigin modelO = new ModelRocchioOrigin(dataset);
		modelO.setNumSeeds(3);
		modelO.setSeeds(names);
		modelO.init(file_goldenset);
		modelO.runRichico();
	}
}
