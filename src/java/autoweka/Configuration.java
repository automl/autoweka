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

/*
	Wraps a configuration and the evaluation(s) SMAC performed on it.
*/
/*
	The idea here is that, when a configuration is evaluated by smac, we record the corresponding score and the fold id for the evaluation. Therefore,
	an instance of the Configuration class represents one evaluation, and contains a mScore and a mEvalautedFold.  When we are going over all of the 
	smac evaluations to sort the best configurations, we merge equivalent instances of Configuration into a single one, keeping track of the best score
	and filling the mFolds list with each fold upon which it was evaluated.
*/
@XmlRootElement(name="configuration")
public class Configuration extends XmlSerializable implements Comparable{ 

	//@TODO make a larger log? I mean, rather than saving the best score, keeping an array of previous scores just for the record. That might be useful info.

	@XmlElement(name="argStrings")
	private ArrayList<String>  mArgStrings;

	@XmlElement(name="score")
	private double mScore;
	
	@XmlElement(name="evaluatedFold")
	private int mEvaluatedFold; 

	@XmlElement(name="folds")
	private ArrayList<String> mFolds;

	public Configuration(){} //Apparently the XML Parser requires this. Don't remove it if you don't want the compiler screaming at your face.

	public Configuration(List<String> args){
		this.mScore=0;
		this.mArgStrings = new ArrayList<String>(args);
		this.mFolds=null;
	}
	
	public void updateEvaluationCollection(double aNewScore, int aNewFold){
		
		if (mFolds == null) mFolds = new ArrayList<String>();
		mScore = (aNewScore > mScore) ? (aNewScore):(mScore); //TODO Is it worth it to import Math and use .max()?
		String wNewFold = Integer.toString(aNewFold);
		if(!mFolds.contains(wNewFold)){
			mFolds.add(wNewFold);
		}
	}

	/*
	Utilities
	*/
	public int compareTo(Object aTarget){ //Compares the score.
		
		if (!(aTarget instanceof Configuration)) throw new RuntimeException("Comparing Configuration to another type!");
		Configuration target = (Configuration) aTarget;

		if(this.mScore==target.mScore) return 0;
		else if (this.mScore>target.mScore) return 1;
		else return -1;
	}

	public String toString(){
		String strFolds = "[";
		for(String fold : mFolds){
			strFolds+=(fold.toString()+"/");
		}
		strFolds+="]";
		return (Integer.toString(this.hashCode())+","+Double.toString(mScore)+","+strFolds);
	}
	
	public int hashCode(){ //Useful for merging configuration objects that refer to the same set of parameters.
		return mArgStrings.hashCode();
	}

	
	/*
	Getters and setters
	*/
	public void setEvaluationValues(double aScore, int aFoldId){ //These always go together
		mScore=aScore;
		mEvaluatedFold=aFoldId;
	}
	
	public double getScore()	  		{return mScore;}
	public int getEvaluatedFold() 		{return mEvaluatedFold;}
	public List<String> getArgStrings() {return mArgStrings;} //In case someone wants to make a "clone", just call clone()

}
