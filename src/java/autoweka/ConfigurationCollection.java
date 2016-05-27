
package autoweka;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.*;

import autoweka.WekaArgumentConverter;
import autoweka.WekaArgumentConverter.Arguments;
import autoweka.XmlSerializable;

import java.io.File;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/*
Simply a wrapper for a list of configurations. Might seem silly and redundant, but the purpose of this is to work nicely with the XML Parser that already exists in AutoWeka.
Also contains the static methods for organizing,sorting and spitting them out to an output file after autoweka is finished running
*/
@XmlRootElement(name="configurationCollection")
public class ConfigurationCollection extends XmlSerializable{
	
	@XmlElement(name="configurations")
	private ArrayList<Configuration> mConfigurations;

	public ConfigurationCollection(){
		mConfigurations = new ArrayList<Configuration>();
	}
	public ConfigurationCollection(List<Configuration> aConfigurations){
		mConfigurations = new ArrayList<Configuration> (aConfigurations);
	}
	public ConfigurationCollection(ArrayList<Configuration> aConfigurations){
		mConfigurations = aConfigurations;
	}
	public void add(Configuration c){ 
		mConfigurations.add(c);
	}

	
	/*
		Static part for ranking configurations in the end of the run
	*/
	//Loads configurations from temporary log, merges identical while merging the folds in which they were analyzed, sorts them and spits to a xml
	public static void rank(String aTemporaryLogPath,String aSortedLogPath){
		
		initializeSortedLog(aSortedLogPath);

		HashMap<Integer,Configuration> configurationsMap = mergeConfigurations(aTemporaryLogPath);
		
		List<Configuration> mergedConfigurations = new ArrayList<Configuration>(configurationsMap.values());
		Collections.sort(mergedConfigurations);
		ConfigurationCollection spit = new ConfigurationCollection(mergedConfigurations);

		spit.toXML(aSortedLogPath);// ba dum tss
	}
	
	//Merging identical configurations while keeping track of the folds in which they were analyzed
	private static HashMap<Integer,Configuration> mergeConfigurations(String aTemporaryLogPath){
		
		HashMap<Integer,Configuration> configurationsMap = new HashMap<Integer,Configuration>();
		ConfigurationCollection loadedCC = fromXML(aTemporaryLogPath,ConfigurationCollection.class);
		ArrayList<Configuration> configurationsArrayList= loadedCC.mConfigurations;
		
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
				configurationsMap.put(new Integer(ciConfig.hashCode()),ciConfig);
			}
		}

		return configurationsMap;
	}

	private static void initializeSortedLog(String aOutputPath){

		try{
			File outputFile = new File(aOutputPath);
			outputFile.createNewFile();
		}catch(Exception e){
			System.out.println("ConfigurationCollection: Couldn't make output files out of xml paths");
		}

		try{ 
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			System.out.println("ConfigurationRanker: HUE 2");
			Document outputDoc = db.newDocument();	
			System.out.println("ConfigurationRanker: HUE 4");
		}catch (Exception e){
			System.out.println("Could not initialize output logs");
		}
	
	}

}