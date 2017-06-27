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
/*
	The idea here is that, when a configuration is evaluated by smac, we record the corresponding score and the fold id for the evaluation. Therefore,
	an instance of the Configuration class represents one evaluation, and contains a mEvaluatedScore and a mEvalautedFold.  When we are going over all of the
	smac evaluations to sort the best configurations, we merge equivalent instances of Configuration into a single one, keeping track of the best score
	and filling the mFolds list with each fold upon which it was evaluated.
*/

@XmlRootElement(name="configuration")
public class Configuration extends XmlSerializable implements Comparable{


	@XmlElement(name="argStrings")
	private String mArgStrings;

	@XmlElement(name="evaluatedScore")
	private double mEvaluatedScore;

	@XmlElement(name="evaluatedFold")
	private int mEvaluatedFold;

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

	//Merges two instances of the same configuration (i.e. same argument string), while keeping track of scores and folds id's. Merging is done on caller configuration only.
	public void mergeWith(Configuration c){

		if(c.hashCode()!=this.hashCode()){
			throw new RuntimeException("Not equivalent configurations!");
		}
		//@TODO optimize those to Integer and Double later
		if(c.mFolds!=null){
			for(String fold : c.mFolds){
				this.mFolds.add(fold);
				this.mAmtFolds++;
			}
		}
		if(c.mFolds!=null){
			for(String score : c.mScores){
				this.mScores.add(score);
				averagedFlag=false;
				this.mAmtScores++;
			}
		}

	}

	/*
	Utilities
	*/

	public void lazyUpdateAverage(){ //If average score is not up to date, update it. Trying to make the update system as lazy as possible.
		if(!averagedFlag){
			mAverageScore=average(mScores);
			averagedFlag=true;
		}
	}

	public void forceUpdateAverage(){
		mAverageScore=average(mScores);
		averagedFlag=true;
	}

	private double average(List<String> l){ //Apparently theres no standard java method for that. @TODO Check if thats true
		double sum = 0;
		for (String d: l){
			sum+=Double.parseDouble(d);
		}
		return (sum/l.size());
	}

	public int compareTo(Object aTarget){ //Compares only the average score. If necessary, updates this metric before comparing

		if (!(aTarget instanceof Configuration)) throw new RuntimeException("Comparing Configuration to another type!");

		Configuration cTarget = (Configuration) aTarget; //c for casted

		this.lazyUpdateAverage();
		cTarget.lazyUpdateAverage();

			if (this.mFolds.size() > cTarget.mFolds.size()){
				return 1;
			}else if (this.mFolds.size() < cTarget.mFolds.size()){
				return -1;
			}else{
				if      (this.mAverageScore < cTarget.mAverageScore ) return 1; //Assumes smaller score is better. If that isn't the case, change that.
				else if (this.mAverageScore > cTarget.mAverageScore)  return -1; // @TODO make this class receive a Metric as input and do this change automatically
				else return 0;
			}

	}

	public String toString(){
		String strFolds = "[";
		for(String fold : mFolds){
			strFolds+=(fold.toString()+"/");
		}
		strFolds+="]";
		return (Integer.toString(this.hashCode())+","+Double.toString(mEvaluatedScore)+","+strFolds);
	}

	public int hashCode(){ //Useful for merging configuration objects that refer to the same set of parameters.
		return mArgStrings.hashCode();
	}

	/*
	Getters and setters
	*/
	public void setEvaluationValues(double aScore, int aFoldId){ //These always go together anyway.
		mEvaluatedScore = aScore;
		mEvaluatedFold  = aFoldId;
		if (mFolds==null){
			mFolds = new ArrayList<String>();
		}
		if (mScores == null){
			mScores = new ArrayList<String>();
		}
		mFolds.add(Integer.toString(aFoldId));
		mScores.add(Double.toString(aScore));
		mAmtFolds++;
		mAmtScores++;
		mAverageScore = average(mScores);
	}

	public void setScore(double aScore){
		mEvaluatedScore = aScore;
		if (mScores == null){
			mScores = new ArrayList<String>();
		}
		mScores.add(Double.toString(aScore));
		mAmtScores++;
	}

	public void setFoldId(int aFoldId){
		mEvaluatedFold  = aFoldId;
		if (mFolds==null){
			mFolds = new ArrayList<String>();
		}
		mFolds.add(Integer.toString(aFoldId));
		mAmtFolds++;
	}

	public double getAverageScore(){
		lazyUpdateAverage();
		return mAverageScore;
	}
	

	public int getEvaluationAmount(){
		return this.mScores.size();
	}

	public double getEvaluatedScore() { return mEvaluatedScore;}
	public int getEvaluatedFold()     { return mEvaluatedFold;}
	public int getAmtFolds()          { return mAmtFolds;}
	public String getArgStrings()     { return mArgStrings;}
	public List<String> getFolds()    { return mFolds;}
	public List<String> getScores()   { return mScores;}

}
