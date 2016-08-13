package autoweka;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import weka.classifiers.meta.AutoWEKAClassifier;

import javax.xml.bind.annotation.*;

/*
	Wraps a configuration and the evaluation(s) SMAC performed on it.
*/


@XmlRootElement(name="configuration")
public class Configuration extends XmlSerializable implements Comparable{

	//@TODO Use Doubles and Integers for scores and folds
	//@TODO make this have a Metric attribute and use it to be more generic
	//@TODO perhaps define > and < in terms of the score

	@XmlElement(name="argStrings")
	private String mArgStrings;

	@XmlElement(name="folds")
	private ArrayList<String> mFolds;

	@XmlElement(name="scores")
	private ArrayList<String> mScores;

	@XmlElement(name="avgScore")
	private double mAverageScore;

	@XmlElement(name="amtScores")
	private int mAmtScores;

	@XmlElement(name="amtFolds")
	private int mAmtFolds;

	@XmlElement(name="avgFlag")
	private boolean averagedFlag;

	@XmlElement(name="identifier")
	private int mIdentifier; //Assign those after the ranking process

	public Configuration(){
		this.mFolds  = new ArrayList<String>();
		this.mScores = new ArrayList<String>();
		this.averagedFlag=false;
	}

	public Configuration(List<String> argsList){
		this();
		this.mArgStrings = "";
		for (String s : argsList){
			this.mArgStrings+=(s+" ");
		}
	}

	public Configuration(String args){
		this();
		this.mArgStrings=args;
	}


	/*
	Utilities
	*/

	public void lazyUpdateAverage(){ //If average score is not up to date, update it. Trying to make the update system as lazy as possible.
		if(!averagedFlag){
			doAverage();
		}
	}

	public void doAverage(){ //Apparently theres no standard java method for that. @TODO Check if thats true
		double sum =0;
		for (String d: mScores){
			sum+=Double.parseDouble(d);
		}
		mAverageScore=sum/mScores.size();
		averagedFlag=true;

	}

	public int compareTo(Object aTarget){ //Compares only the average score. If necessary, updates this metric before comparing

		if (!(aTarget instanceof Configuration)) throw new RuntimeException("Comparing Configuration to another type!");

		Configuration cTarget = (Configuration) aTarget; //c for casted

		this.lazyUpdateAverage();
		cTarget.lazyUpdateAverage();

		if (this.mAmtFolds > cTarget.mAmtFolds){
			return 1;
		}else if (this.mAmtFolds < cTarget.mAmtFolds){
			return -1;
		}else{
			if      (this.mAverageScore < cTarget.mAverageScore ) return 1; //Assumes smaller score is better. If that isn't the case, change that.
			else if (this.mAverageScore > cTarget.mAverageScore)  return -1; // @TODO make this class receive a Metric as input and do this change automatically
			else return 0;
		}

	}

	public String toString(){
		return Integer.toString(this.hashCode());
	}

	public int hashCode(){ //Useful for merging configuration objects that refer to the same set of parameters.
		return mArgStrings.hashCode();
	}

	public void update(int foldId,double score){//TODO is it in the scope of this method to check argstrings?
		mFolds.add(Integer.toString(foldId));
		mScores.add(Double.toString(score));
		mAmtFolds++;
		mAmtScores++;
		doAverage();
	}

	/*
	Getters and setters
	*/
	public double getAverageScore(){
		lazyUpdateAverage();
		return mAverageScore;
	}

	public int getAmtFolds()          { return mAmtFolds;		}
	public int getIdentifier()        { return mIdentifier;	}
	public String getArgStrings() 	 { return mArgStrings;	}
	public List<String> getFolds() 	 { return mFolds;			}
	public List<String> getScores()   { return mScores;		}
	public void setIdentifier(int id) { mIdentifier = id;		}


}
