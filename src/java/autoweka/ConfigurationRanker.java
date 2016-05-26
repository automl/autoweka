package autoweka;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import autoweka.Util;
import autoweka.Configuration;
import autoweka.WekaArgumentConverter.Arguments;

import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/*
  Class for storing incumbents and their performances so that we can list the best ones after an Auto-WEKA run.
  The CL extensively used below stands for Configuration Log, which is a place to where autoweka will spit out
  every configuration that smac spits out.
*/

public class ConfigurationRanker extends XmlSerializable{
	
	//@TODO consider having a final constant here with the final output path and another on Configuration with the temporary output path.

	private HashMap<Integer,Configuration> mConfigurations;
	
	private static int mNumFolds;

	private static String   mCLPath;
	private static File     mCLFile;
	private static Document mCLDoc;
	
	private static String   mOutputPath;
	private static File     mOutputFile;
	private static Document mOutputDoc;

	
	/*
	A thread-safe singleton implementation (or, if you wanna get fancy, an implementation of Initialization on Demand Holder)
	*/
	private static class ConfigurationRankerHolder{
		private static final ConfigurationRanker singletonInstance = new ConfigurationRanker();
	}
	
	public static ConfigurationRanker getInstance(int aNumFolds, String aCLPath, String aOutputPath){
		
		ConfigurationRanker instance = ConfigurationRankerHolder.singletonInstance;
		
		instance.mNumFolds   = aNumFolds;
		instance.mCLPath     = aCLPath;
		instance.mOutputPath = aOutputPath;
		
		instance.mConfigurations = new HashMap<Integer,Configuration>();	
		
		instance.initializeCLs();
		return instance;
	}
	
	private ConfigurationRanker(){}
	
	private void initializeCLs(){

		try{
			this.mCLFile = new File(mCLPath);
			this.mOutputFile = new File(mOutputPath);
		}catch(Exception e){
			System.out.println("ConfigurationRanker: Couldn't make files out of xml paths");
		}

		/*System.out.println("ConfigurationRanker: About to make paths in case they dont exist (they probably dont)");
		if (!mCLFile.exists()){ //TODO check if these are right
			Util.makePath(mCLPath);
		}
		if (!mOutputFile.exists()){
			Util.makePath(mOutputPath);
		}

		System.out.println("ConfigurationRanker: Just made the paths, in case they didnt exist. Gonna set up the xmls.");
*/
		try{ //TODO filter by exception type. parserconfigurationexception and the others.

			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			
			this.mCLDoc = db.newDocument();
			this.mCLDoc = db.parse(mCLFile);

			this.mOutputDoc = db.newDocument();
			this.mOutputDoc = db.parse(mOutputFile);	
		}catch (Exception e){
			System.out.println("Could not initialize Configuration Ranker");
		}
	
	}

	
	/*
	Writing and reading to log
	*/

	//Write a single guy to the temporary log
	public static void writeToCL(List<String> aArgs, int aFoldId, double aScore){ //Use this from smacwrapper or whoever spits out smbo results ^_^
		
		Configuration spitMe = new Configuration(aArgs);
		spitMe.setEvaluationValues(aScore,aFoldId);
		spitMe.toXML(mCLPath); //ba dum tss
		
	}

	//Does all the ranking thing in the end of the autoweka run
	public void rank(){
		this.readFullCL();
		this.writeFullOutput();
	}

	private void readFullCL(){ // Use this internally when parsing a log built by calls to updateLog
		
		Configuration ciConfig; //ci for current iteration
		int ciKey, ciFoldId;
		double ciScore;

		while(true){
			
			try{
				ciConfig = fromXML(mCLPath, Configuration.class);
			}catch (RuntimeException e){
				break;
			}

			ciFoldId = ciConfig.getEvaluatedFold();
			if(ciFoldId<0 || ciFoldId>=mNumFolds) throw new RuntimeException("Fold out of bounds"); //Seems unnecessary but better safe than sorry
			ciKey   = ciConfig.hashCode();
			ciScore = ciConfig.getScore();
			
			if(mConfigurations.containsKey(ciKey)){
				mConfigurations.get(ciKey).updateEvaluationCollection( ciScore,ciFoldId );

			}else{
				mConfigurations.put(new Integer(ciConfig.hashCode()),ciConfig);
			}

		}

	}

	
	private void writeFullOutput(){
		List<Configuration> sConfigs = this.sortByScore(); //s for sorted
		for(Configuration ciConfig : sConfigs){
			ciConfig.toXML(mOutputPath);
		}
	}

	private List<Configuration> sortByScore(){
		//TODO check if theres any utility that does that in one line:
		//TODO maybe put this more generically in the Util class of autoweka
		List<Configuration> configs = new ArrayList<Configuration>(this.mConfigurations.values());
		Collections.sort(configs);
		return configs;
	}



	/*
	Getters and Setters
	*/
	public int getNumFolds() {return mNumFolds;}

}