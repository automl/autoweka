
package autoweka;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import javax.xml.bind.annotation.*;

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

	public void add(Configuration c){
		mConfigurations.add(c);
	}

	public Configuration get(int i){
		return mConfigurations.get(i);
	}

	public int size (){
		return mConfigurations.size();
	}

	//Gets the highest amount of fold evaluations from all the configurations within the collection
	public int getHighestEvaluationAmt(){
		int highest = 0;
		for (Configuration c : mConfigurations){
			if(c.getAmtFolds()>highest){
				highest = c.getAmtFolds();
			}
		}
		return highest;
	}

	//Returns the amount of configurations evaluated on all folds. The input integer is the maximum amount of folds possible according to user's CV options.
	public int getFullyEvaluatedAmt(int maxAmt){
		int counter = 0;
		for (Configuration c : mConfigurations){
			if(c.getAmtFolds()==maxAmt) counter++;
		}
		return counter;
	}

	//Optional version of the method that assumes the configuration with the most evaluations has the highest possible amount
	public int getFullyEvaluatedAmt() {
		int maxAmt = this.getHighestEvaluationAmt();
		return this.getFullyEvaluatedAmt(maxAmt);
	}

	//Returns a new ConfigurationCollection containing only the fully evaluated configurations
	public ConfigurationCollection getFullyEvaluatedCollection(int maxAmt){
		List<Configuration> rvConfigurations = new ArrayList<Configuration>();
		for(Configuration c : mConfigurations){
			if (c.getAmtFolds() == maxAmt){
				rvConfigurations.add(c);
			}
		}
		ConfigurationCollection rv = new ConfigurationCollection(rvConfigurations);
		return rv;
	}

	//Optional version of the method that assumes the configuration with the most evaluations has the highest possible amount
	public ConfigurationCollection getFullyEvaluatedCollection(){
		int maxAmt = this.getHighestEvaluationAmt();
		return this.getFullyEvaluatedCollection(maxAmt);
	}



	public ArrayList<Configuration> asArrayList(){
		return mConfigurations;
	}

	public static <T extends XmlSerializable> T fromXML(String filename, Class<T> c){ //Original is protected so we're overriding to make it public.
		return XmlSerializable.fromXML(filename,c);
	}

}
