package autoweka;

import java.io.File;

import java.util.Arrays;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import weka.classifiers.meta.AutoWEKAClassifier.temporaryDirPath;
import weka.classifiers.meta.AutoWEKAClassifier.configurationRankingPath;
import weka.classifiers.meta.AutoWEKAClassifier.configurationInfoDirPath;
import weka.classifiers.meta.AutoWEKAClassifier.configurationHashSetPath;


public class ConfigurationRanker{

	//Aliasing for readability
	public String cdPath; //configuration directory path
	public String rPath;  //configuration ranking path (xml file)
	public String hsPath; //hash set path (txt file)

	//Loads configurations from temporary log, merges identical while merging the folds in which they were analyzed, sorts them and spits n of them to a xml
	public ConfigurationRanker(){
		this.cdPath = temporaryDirPath+configurationInfoDirPath;
		this.rPath  = rankingPath+configurationRankingPath;
		this.hsPath = temporaryDirPath+configurationHashSetPath;
	}

	public List<Configuration> rank(int n){
		return rank(n, "IGNORE");
	}

	public List<Configuration> rank(int n, String smacFinalIncumbent){

		//Declaring some basic stuff
		List<Configuration> configurationList = new ArrayList<Configuration>();
		File hsFile = new File(hsPath);
		String [] hashArray;
		Set<String> hashSet;

		//Reading the hashSet and removing duplicates
		try{
			hashArray = (new Scanner(hsFile)).nextLine().split(",");
			hashSet = new HashSet<String>(Arrays.asList(hashArray));
		}catch(Exception e){
			System.out.println("Couldn't find config hash list file");
			return;
		}
		for(String hash : hashSet){
			configurationList.add(Configuration.fromXML(cdPath+hash+".xml",Configuration.class)); //TODO unhardcode
		}

		//Sorting the configurations
		for(Configuration c: configurationList){
			c.forceUpdateAverage();
		}
		Collections.sort(configurationList);
		Collections.reverse(configurationList); //TODO invert definition in compareto rather than using this?

		//Forcing the last incumbent to be the best configuration, in case of a tie
		if (!smacFinalIncumbent.equals("IGNORE")){
			forceFirst(configurationList,smacFinalIncumbent); //gotta do this before slicing, otherwise smacFinalIncumbent might be lost in the slice
		}

		//Slicing the list to size n
		configurationList = configurationList.subList(0,  (n<configurationList.size())?(n):(configurationList.size())  );

		//Spit to xml
		Util.initializeFile(rPath);
		ConfigurationCollection spitMe = new ConfigurationCollection(configurationList);
		spitMe.toXML(rPath); //ba dum tss

		return configurationList;
	}

	private static void forceFirst(List<Configuration> configurationList, String smacFinalIncumbent){

		Configuration bestConfig = configurationList.get(0);
		if (bestConfig.getArgStrings().equals(smacFinalIncumbent)) {
			return;
		}

		double bestScore = bestConfig.getAverageScore();

		for(int i = 1; i<configurationList.size(); i++){
			Configuration temp, c = configurationList.get(i);
			if(c.getArgStrings().equals(smacFinalIncumbent)){
				if(c.getAverageScore()!=bestScore){
					throw new RuntimeException("Final incumbent doesn't have the best score");
				}else{
					configurationList.set(0,c);
					configurationList.set(i,bestConfig);
					return;
				}
			}
		}
		throw new RuntimeException("Couldn't find final incumbent on the log");
	}

}
