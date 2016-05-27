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


@XmlRootElement(name="configuration")
public class Configuration extends XmlSerializable implements Comparable{ 

	//@TODO make a larger log? I mean, rather than saving a score keeping an array of previous scores.

	@XmlElement(name="argStrings")
	private ArrayList<String>  mArgStrings;

	@XmlElement(name="score")
	private double 	      mScore;
	
	@XmlElement(name="evaluatedFold")
	private int 		  mEvaluatedFold;

	@XmlElement(name="folds")
	private ArrayList<String> mFolds;

	public Configuration(){}

	public Configuration(List<String> args){
		this.mScore=0;
		this.mArgStrings = new ArrayList<String>(args);
		//this.mArguments = WekaArgumentConverter.convert(args);
		this.mFolds=null;
	}
	
	public void updateEvaluationCollection(double aNewScore, int aNewFold){
		
		if (mFolds == null) mFolds = new ArrayList<String>();
		mScore = (aNewScore > mScore) ? (aNewScore):(mScore); //TODO check if java has a standard max function
		
		//Integer wNewFold = new Integer(aNewFold); //w for wrapped
		String wNewFold = Integer.toString(aNewFold);
		if(!mFolds.contains(wNewFold)){
			mFolds.add(wNewFold);
		}
	}

	/*
	Utilities
	*/
	public Configuration clone(){
		return new Configuration(mArgStrings);
	}

	public int compareTo(Object aTarget){
		
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
		
		return (","+Integer.toString(this.hashCode())+Double.toString(mScore)+strFolds);
	}
	
	public int hashCode(){
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
