
package autoweka;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.xml.bind.annotation.*;

import static weka.classifiers.meta.AutoWEKAClassifier.configurationRankingPath;
import static weka.classifiers.meta.AutoWEKAClassifier.configurationMapPath;
import static weka.classifiers.meta.AutoWEKAClassifier.foldwiseLogPath;

/*
A wrapper for a list of configurations. It works nicely with the XML Parser that already exists in Auto-WEKA.
Also contains method for ranking the configurations.
*/
@XmlRootElement(name="configurationCollection")
public class ConfigurationCollection extends XmlSerializable{

	//Indexes the existing Configurations by their ArgStrings for access purposes.
	private Map<String, Configuration> mArgStringsMap;

	@XmlElement(name="configurations")
	private ArrayList<Configuration> mConfigurations;

	/**
	 * Constructors
	 */

	//Builds and empty ConfigurationCollection
	public ConfigurationCollection(){
		mConfigurations = new ArrayList<Configuration>();
		mArgStringsMap = new HashMap<String,Configuration>();
	}

	//Wraps a List<Configuration>, also parsing the ArgStrings for the map
	public ConfigurationCollection(List<Configuration> aConfigurations){
		mConfigurations = (ArrayList) aConfigurations;
		mArgStringsMap = new HashMap<String,Configuration>();
		for(Configuration c: mConfigurations){
			if(!this.contains(c.getArgStrings())){
				mArgStringsMap.put(c.getArgStrings(),c);
			}
		}
		forceAverages();
	}

	//Parses a Configuration csv with the format (ArgStrings, fold, score)
	public ConfigurationCollection(String csvLog){
		this();
		//Parsing the log
		List<String[]> lines = Util.getLineElements(csvLog,",");
		for(String[] sa:lines){
			updateConfiguration(sa[0],Integer.parseInt(sa[1]),Double.parseDouble(sa[2]));
		}
		forceAverages();
	}

	//A XML Serialization trick. Superclass method is protected so we're overriding to make it public.
	public static <T extends XmlSerializable> T fromXML(String filename, Class<T> c){
		return XmlSerializable.fromXML(filename,c);
	}

	//Updates the avg score for every config in the collection
	private void forceAverages(){
		for(Configuration c: mConfigurations) c.doAverage();
	}

	/**
	 * Getters, setters and the like
	 */

	//Updates the contents of a given configuration in the collection. If it is not there, it is added and then updated.
	public void updateConfiguration(String argStrings, int foldId, double score){
		if(!this.contains(argStrings)){
			this.add(new Configuration(argStrings));
		}
		Configuration temp = mArgStringsMap.get(argStrings);
		temp.update(foldId,score);
	}

	public boolean contains(String argStrings){
		return (mArgStringsMap.containsKey(argStrings));
	}

	public void add(Configuration c){
		mConfigurations.add(c);
		if(!this.contains(c.getArgStrings())){
			mArgStringsMap.put(c.getArgStrings(),c);
		}

	}

	public Configuration get(int i){
		return mConfigurations.get(i);
	}

	public int size (){
		return mConfigurations.size();
	}

	public ArrayList<Configuration> asArrayList(){
		return mConfigurations;
	}

	public int getFullyEvaluatedAmt(){
		int rv=0;
		for (Configuration c : mConfigurations){
			if (c.getAmtFolds()==mConfigurations.get(0).getAmtFolds()){
				rv++;
			}
		}
		return rv;
	}

	/**
	* Ranking and spitting to XML.
	*/

	//Ranks the configurations by score and spits the collection to the configuration ranking xml file
	//Note: You can spit to xml by using the toXML method from the superclass. For the current intents and purposes,
	//we only ever spit a ConfigurationCollection when we rank it.
	//Note2: Currently, the smaller the score is, the better (thats why we use reverse()). TODO make it polymorphic regarding the metrics
	public List<Configuration> rank(String temporaryDirPath){
		return rank (temporaryDirPath,"IGNORE");
	}

	public List<Configuration> rank(String temporaryDirPath,String smacFinalIncumbent){

		//If we have analyzed zero configurations in all folds throughout the run, just return it as it is ¯\_(ツ)_/¯
		if(mConfigurations.size()==0){
			return mConfigurations;
		}

		//Aliasing this:
		String rPath = temporaryDirPath+"/"+configurationRankingPath;


		//Ranking
		forceAverages();
		Collections.sort(mConfigurations);
		Collections.reverse(mConfigurations);

		//Forcing the last incumbent to be the best configuration, in case of a tie
		if (!smacFinalIncumbent.equals("IGNORE")){
			forceFirst(smacFinalIncumbent); //gotta do this before slicing, otherwise smacFinalIncumbent might be lost in the slice
		}

		//Assigning Identifiers
		assignIdentifiers(temporaryDirPath);

		//If the list is larger than N, slicing it to size N
		// List<Configuration> slicedList = Util.getSlicedList(mConfigurations,0,(n<mConfigurations.size()-1)?(n):(mConfigurations.size()-1));//(ArrayList<Configuration>) mConfigurations.subList(0,  (n<mConfigurations.size())?(n):(mConfigurations.size())  );
		// ConfigurationCollection spitMe = new ConfigurationCollection(slicedList);

		//Spitting to xml
		Util.initializeFile(rPath);
		this.toXML(rPath);

		//Returns the list because why not
		return mConfigurations;
	}

	private void assignIdentifiers(String temporaryDirPath){
		Map<String,String> cfgMap = Util.getConfigurationMap(temporaryDirPath+"/"+configurationMapPath);
		for (Configuration c: mConfigurations){
			c.setIdentifier(Integer.parseInt(cfgMap.get(c.getArgStrings())));
		}
	}

	//Finds the SMAC final incumbent amongst the configurations tied with the top score and forces it to be the first on the ranking
	private void forceFirst(String smacFinalIncumbent){

		boolean flag=false;
		for(Configuration c : mConfigurations){
			if(c.getArgStrings().equals(smacFinalIncumbent)){
				flag=true;
			}
		}

		//If we don't need to change anything, returns imediately
		Configuration bestConfig = mConfigurations.get(0);
		if (bestConfig.getArgStrings().equals(smacFinalIncumbent)) {
			return;
		}

		double bestScore = bestConfig.getAverageScore();

		//Finds the final incumbent and does the switching
		for(int i = 1; i<mConfigurations.size(); i++){
			Configuration c = mConfigurations.get(i);
			if(c.getArgStrings().equals(smacFinalIncumbent)){
				if(c.getAverageScore()!=bestScore){
					throw new RuntimeException("Final incumbent doesn't have the best score");
				}else{
					mConfigurations.set(0,c);
					mConfigurations.set(i,bestConfig);
					return;
				}
			}
		}
		throw new RuntimeException("Couldn't find final incumbent on the log");

	}

}
