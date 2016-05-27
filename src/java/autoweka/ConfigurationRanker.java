package autoweka;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/*import autoweka.WekaArgumentConverter;
import autoweka.WekaArgumentConverter.Arguments;
import autoweka.XmlSerializable;*/

import java.io.File;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


public class ConfigurationRanker{
	
	/*
		Static part for ranking configurations in the end of the run
	*/
	
	//Loads configurations from temporary log, merges identical while merging the folds in which they were analyzed, sorts them and spits n of them to a xml
	public static void rank(int n, String aTemporaryLogPath,String aSortedLogPath){
		
		initializeLog(aSortedLogPath);

		HashMap<Integer,Configuration> configurationsMap = mergeConfigurations(aTemporaryLogPath);
		
		List<Configuration> mergedConfigurations = new ArrayList<Configuration>(configurationsMap.values());
		Collections.sort(mergedConfigurations);
		
		ArrayList<Configuration> choppedList = new ArrayList<Configuration>();
		int limit = (n<mergedConfigurations.size())?(n):(mergedConfigurations.size());
		for(int i=0; i<limit ; i++){
			choppedList.add(mergedConfigurations.get(i));
		}
		
		ConfigurationCollection spit = new ConfigurationCollection(choppedList);

		spit.toXML(aSortedLogPath);// ba dum tss
	}
	
	//Merging equivalent configurations while keeping track of the folds in which each instance was analyzed
	private static HashMap<Integer,Configuration> mergeConfigurations(String aTemporaryLogPath){
		
		HashMap<Integer,Configuration> configurationsMap = new HashMap<Integer,Configuration>();
		ConfigurationCollection loadedCC = ConfigurationCollection.fromXML(aTemporaryLogPath,ConfigurationCollection.class);
		ArrayList<Configuration> configurationsArrayList= loadedCC.asArrayList();
		
		int ciFoldId;
		Integer ciKey;
		double ciScore;
			
		for(Configuration ciConfig : configurationsArrayList){
			ciFoldId = ciConfig.getEvaluatedFold();
			ciKey   = ciConfig.hashCode();
			ciScore = ciConfig.getScore();
			if(configurationsMap.containsKey(ciKey)){
				configurationsMap.get(ciKey).updateEvaluationCollection( ciScore,ciFoldId );
			}else{
				configurationsMap.put(new Integer(ciKey,ciConfig);
				configurationsMap.get(ciKey).updateEvaluationCollection( ciScore,ciFoldId );
			}
		}

		return configurationsMap;
	}

	//@TODO check if theres something equivalent in util and if there isnt just throw this there
	public static void initializeLog(String aLogPath){

		try{
			File logFile = new File(aLogPath);
			logFile.createNewFile();
		}catch(Exception e){
			System.out.println("ConfigurationCollection: Couldn't make log file out of xml path!");
		}

	}

}