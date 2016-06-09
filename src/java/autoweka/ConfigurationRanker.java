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
	public static void rank(int n, String aTemporaryLogPath,String aSortedLogPath){

		List<Configuration> mergedConfigurations = mergeConfigurations(aTemporaryLogPath);

		for(Configuration c: mergedConfigurations){
				c.forceUpdateAverage();
		}
		Collections.sort(mergedConfigurations);
		Collections.reverse(mergedConfigurations);

		ArrayList<Configuration> choppedList = chopList(mergedConfigurations,n);

		ConfigurationCollection spit = new ConfigurationCollection(choppedList);

		spit.toXML(aSortedLogPath);// ba dum tss
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

		return new ArrayList<Configuration>(configurationsMap.values());
	}


}
