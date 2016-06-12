package autoweka;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import weka.classifiers.meta.AutoWEKAClassifier;

import java.io.File;



public class ConfigurationRanker{
	//@TODO maybe integrate this class to ConfigurationCollection

	//Loads configurations from temporary log, merges identical while merging the folds in which they were analyzed, sorts them and spits n of them to a xml
	public static void rank(int n, String aTemporaryLogPath,String aSortedLogPath, String smacBest){

		List<Configuration> mergedConfigurations = mergeConfigurations(aTemporaryLogPath);


		ArrayList<Configuration> choppedList = chopList(mergedConfigurations,n);

		forceFirst(choppedList,smacBest);


		ConfigurationCollection spit = new ConfigurationCollection(choppedList);


		spit.toXML(aSortedLogPath);// ba dum tss
	}

	private static void forceFirst(ArrayList<Configuration> choppedList, String smacBest ){
		if (choppedList.get(0).getArgStrings().equals(smacBest)){
			return;
		}
		double bestScore = choppedList.get(0).getAverageScore();

		for(int i = 0; i<choppedList.size();i++){
			Configuration c = choppedList.get(i);
			if(c.getArgStrings().equals(smacBest)){

				if(c.getAverageScore()!=bestScore){
					throw new RuntimeException("SMAC's final incumbent was found in the configuration log but it wasn't the best (or tied with the best)");
				}
				Configuration temp = choppedList.get(0);
				choppedList.remove(0);
				choppedList.add(0,c);
				choppedList.remove(i);
				choppedList.add(i,temp);
				return;
			}
		}
		throw new RuntimeException("SMAC's final incumbent wasn't found in the configuration log");
	}

	private static ArrayList<Configuration> chopList(List<Configuration> mergedConfigurations, int n){

		ArrayList<Configuration> choppedList = new ArrayList<Configuration>();

		int limit = (n<mergedConfigurations.size())?(n):(mergedConfigurations.size());
		for(int i=0; i<limit ; i++){
			mergedConfigurations.get(i).lazyUpdateAverage();
			choppedList.add(mergedConfigurations.get(i));
		}

		return choppedList;
	}

	//Helper method for the above
	private static ArrayList<Configuration> mergeConfigurations(String aTemporaryLogPath){

		Integer ciKey;
		ConfigurationCollection loadedCC = ConfigurationCollection.fromXML(aTemporaryLogPath,ConfigurationCollection.class);

		HashMap<Integer,Configuration> configurationsMap = new HashMap<Integer,Configuration>();
		ArrayList<Configuration> configurationsArrayList= loadedCC.asArrayList(); //@TODO make configurationcollection iterable later

		for(Configuration ciConfig : configurationsArrayList){
			ciKey = ciConfig.hashCode();
			if(configurationsMap.containsKey(ciKey)){
				configurationsMap.get(ciKey).mergeWith(ciConfig);
			}else{
				configurationsMap.put(new Integer(ciKey),ciConfig);
			}
		}

		ArrayList<Configuration> mergedConfigurations = new ArrayList<Configuration>(configurationsMap.values());
		for(Configuration c: mergedConfigurations){
			c.forceUpdateAverage();
		}
		Collections.sort(mergedConfigurations);
		Collections.reverse(mergedConfigurations);
		return mergedConfigurations;
	}


}
