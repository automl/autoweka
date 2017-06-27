package autoweka;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.NoSuchElementException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;

import static weka.classifiers.meta.AutoWEKAClassifier.configurationRankingPath;
import static weka.classifiers.meta.AutoWEKAClassifier.configurationInfoDirPath;
import static weka.classifiers.meta.AutoWEKAClassifier.configurationHashSetPath;

public class ConfigurationRanker{

	//Loads configurations from temporary log, merges identical while merging the folds in which they were analyzed, sorts them and spits n of them to a xml

	public static void rank(int n, String temporaryDirPath, String smacBest) throws FileNotFoundException, NoSuchElementException{

		//Declaring some basic stuff
		String hsPath = temporaryDirPath+configurationHashSetPath;
		String cdPath = temporaryDirPath+configurationInfoDirPath;
		String rPath  = temporaryDirPath+configurationRankingPath;

		List<Configuration> configs = new ArrayList<Configuration>();
		File hashSetFile = new File(hsPath);
		String [] redundantConfigHashes;
		Set<String> configHashes;

		//Reading the hashes and removing duplicates

		redundantConfigHashes = (new Scanner(hashSetFile)).nextLine().split(",");
		configHashes = new HashSet<String>(Arrays.asList(redundantConfigHashes));

//

		for(String hash : configHashes){
			configs.add(Configuration.fromXML(cdPath+hash+".xml",Configuration.class));
		}

		//Sorting the configurations
		for(Configuration c: configs){
			c.forceUpdateAverage();
		}
		Collections.sort(configs);
		Collections.reverse(configs);

		//Forcing the last incumbent to be the best configuration, in case of a tie
		if (!smacBest.equals("IGNORE")){
			forceFirst(configs,smacBest); //gotta do this before slicing, otherwise smacBest might be lost in the slice
		}

		//Slicing the list to size n
		configs = configs.subList(0,  (n<configs.size())?(n):(configs.size())  );

		//Spit to xml
		Util.initializeFile(rPath);
		ConfigurationCollection spitMe = new ConfigurationCollection(configs);
		spitMe.toXML(rPath); //ba dum tss
	}

	private static void forceFirst(List<Configuration> configs, String smacBest){

		Configuration bestConfig = configs.get(0);
		if (bestConfig.getArgStrings().equals(smacBest)) {
			return;
		}

		double bestScore = bestConfig.getAverageScore();

		for(int i = 1; i<configs.size(); i++){
			Configuration temp, c = configs.get(i);
			if(c.getArgStrings().equals(smacBest)){
				if(c.getAverageScore()!=bestScore){
					throw new RuntimeException("Final incumbent doesn't have the best score");
				}else{
					configs.set(0,c);
					configs.set(i,bestConfig);
					return;
				}
			}
		}
		throw new RuntimeException("Couldn't find final incumbent on the log");
	}

}
