package autoweka;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import weka.classifiers.meta.AutoWEKAClassifier;

import java.io.File;



public class ConfigurationRanker{

	/*
		Static part for ranking configurations in the end of the run
	*/

	//Loads configurations from temporary log, merges identical while merging the folds in which they were analyzed, sorts them and spits n of them to a xml
	public static void rank(int n, String aTemporaryLogPath,String aSortedLogPath){

		List<Configuration> mergedConfigurations;
		//initializeLog(aSortedLogPath);
		// if(AutoWEKAClassifier.getResampling()==AutoWEKAClassifier.Resampling.CrossValidation){
		HashMap<Integer,Configuration> configurationsMap = mergeConfigurations(aTemporaryLogPath); //throw this statement in mergeconfigs latter
		mergedConfigurations = new ArrayList<Configuration>(configurationsMap.values());
		// }else{
		// 	ConfigurationCollection loadedCC = ConfigurationCollection.fromXML(aTemporaryLogPath,ConfigurationCollection.class);
		// 	mergedConfigurations = loadedCC.asArrayList();
		// }
		for(Configuration c: mergedConfigurations){
				c.forceUpdateAverage();
		}

		Collections.sort(mergedConfigurations);
		Collections.reverse(mergedConfigurations);

		ArrayList<Configuration> choppedList = new ArrayList<Configuration>();
		int limit = (n<mergedConfigurations.size())?(n):(mergedConfigurations.size());
		for(int i=0; i<limit ; i++){ //Starting from the end because sort() sorts from worst to best. @TODO check if theres a standard method for chopping
			mergedConfigurations.get(i).lazyUpdateAverage();
			choppedList.add(mergedConfigurations.get(i));
		}

		ConfigurationCollection spit = new ConfigurationCollection(choppedList);

		spit.toXML(aSortedLogPath);// ba dum tss
	}

	// public static void rank(int n, String aTemporaryLogPath,String aSortedLogPath){
	//
	// 	List<Configuration> mergedConfigurations;
	// 	//initializeLog(aSortedLogPath);
	// 	// if(AutoWEKAClassifier.getResampling()==AutoWEKAClassifier.Resampling.CrossValidation){
	// 	HashMap<Integer,Configuration> configurationsMap = mergeConfigurations(aTemporaryLogPath); //throw this statement in mergeconfigs latter
	// 	mergedConfigurations = new ArrayList<Configuration>(configurationsMap.values());
	// 	// }else{
	// 	// 	ConfigurationCollection loadedCC = ConfigurationCollection.fromXML(aTemporaryLogPath,ConfigurationCollection.class);
	// 	// 	mergedConfigurations = loadedCC.asArrayList();
	// 	// }
	//
	// 	Collections.sort(mergedConfigurations);
	// 	Collections.reverse(mergedConfigurations);
	//
	// 	ArrayList<Configuration> choppedList = new ArrayList<Configuration>();
	// 	int limit = (n<mergedConfigurations.size())?(n):(mergedConfigurations.size());
	// 	for(int i=0; i<limit ; i++){ //Starting from the end because sort() sorts from worst to best. @TODO check if theres a standard method for chopping
	// 		mergedConfigurations.get(i).lazyUpdateAverage();
	// 		choppedList.add(mergedConfigurations.get(i));
	// 	}
	//
	// 	ConfigurationCollection spit = new ConfigurationCollection(choppedList);
	//
	// 	spit.toXML(aSortedLogPath);// ba dum tss
	// }


	//Merging equivalent configurations while keeping track of the folds in which each instance was analyzed
	private static HashMap<Integer,Configuration> mergeConfigurations(String aTemporaryLogPath){

		HashMap<Integer,Configuration> configurationsMap = new HashMap<Integer,Configuration>();
		ConfigurationCollection loadedCC = ConfigurationCollection.fromXML(aTemporaryLogPath,ConfigurationCollection.class);
		ArrayList<Configuration> configurationsArrayList= loadedCC.asArrayList();

		Integer ciKey;

		for(Configuration ciConfig : configurationsArrayList){

			ciKey = ciConfig.hashCode();

			if(configurationsMap.containsKey(ciKey)){
				//System.out.println("merging! "+ciConfig.hashCode());
				configurationsMap.get(ciKey).mergeWith(ciConfig);
			}else{
				//System.out.println("putting! "+ciConfig.hashCode());
				configurationsMap.put(new Integer(ciKey),ciConfig);
			}

		}

		return configurationsMap;
	}




	//@TODO check if theres something equivalent in util and if there isnt just throw this there
	public static void initializeLog(String aLogPath){

		try{
			File logFile = new File(aLogPath);
			if(!logFile.exists()){
				logFile.createNewFile();
			}
		}catch(Exception e){
			System.out.println("ConfigurationCollection: Couldn't make log file out of xml path!");
		}

	}

}
