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

	//@TODO make a larger log? I mean, rather than saving the best score, keeping an array of previous scores just for the record. That might be useful info.

	@XmlElement(name="argStrings")
	private String mArgStrings;
////
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

	private boolean averagedFlag;

	public Configuration(){} //Apparently the XML Parser requires this. Don't remove it if you don't want the compiler screaming at your face.

	public Configuration(List<String> args){

		this.mArgStrings = "";
		for (String s : args){
			this.mArgStrings+=(s+" ");
		}
		this.mFolds  = new ArrayList<String>(); //@TODO Create a confmerger class? Or make those lazy.
		this.mScores = new ArrayList<String>();
		this.mEvaluatedScore=0;
		this.mEvaluatedFold=0;
		this.mEvaluatedScore=0;
		this.mAmtFolds=0;
		this.mAmtScores=0;
		this.mFolds=null;
		this.mScores=null;
		this.averagedFlag=false;
	}


	public void mergeWith(Configuration c){
//		System.out.println("mergeWith");
		if(c.hashCode()!=this.hashCode()){
			throw new RuntimeException("Not equivalent configurations!");
		}

		if (this.mEvaluatedFold==c.mEvaluatedFold){
			throw new RuntimeException("Evaluated same fold twice!");
		}

		String wNewFold = Integer.toString(c.getEvaluatedFold()); // new Integer(c.getEvaluatedFold());
		String wMyFold = Integer.toString(mEvaluatedFold);
		mFolds.add(wNewFold);
		mAmtFolds++;

		String wNewScore = Double.toString(c.getEvaluatedScore()); //new Double(c.getEvaluatedScore());
		String wMyScore = Double.toString(mEvaluatedScore);
		averagedFlag=false;
		mScores.add(wNewScore);
		mAmtScores++;

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
	//	System.out.println("compareTo");
		if (!(aTarget instanceof Configuration)) throw new RuntimeException("Comparing Configuration to another type!");

		Configuration cTarget = (Configuration) aTarget; //c for casted

		this.lazyUpdateAverage();
		cTarget.lazyUpdateAverage();

			if (this.mFolds.size() > cTarget.mFolds.size()){
				return 1;
			}else if (this.mFolds.size() < cTarget.mFolds.size()){
				return -1;
			}else{
				if      (this.mAverageScore < cTarget.mAverageScore ) return 1;//Smaller score is better. @TODO if score metric changes, change that.
				else if (this.mAverageScore > cTarget.mAverageScore)  return -1;
				else return 0;
			}

		//}
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

	public double getEvaluatedScore()   { return mEvaluatedScore;}
	public int getEvaluatedFold()   		{ return mEvaluatedFold;}
	public String getArgStrings() 			{ return mArgStrings;} //In case someone wants to make a "clone", just call clone()

}
