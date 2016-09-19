package autoweka;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static weka.classifiers.meta.AutoWEKAClassifier.configurationRankingPath;
import static weka.classifiers.meta.AutoWEKAClassifier.configurationMapPath;
import static weka.classifiers.meta.AutoWEKAClassifier.instancewiseInfoDirPath;

public class Ensembler{

	final static Logger log = LoggerFactory.getLogger(Ensembler.class);

	//Candidate Configurations
	private List<Configuration> mCfgList;  //List of configurations read from the configuration ranking file at rPath
	private Map<String,String> mCfgMap;

	//Data about the instances
	private Map<String,Integer> mLabelMap; //Maps the existing labels in the dataset to label indexes[0:n-1] where n is the number of labels
	private Map<Integer,Integer> mLabelFrequencies; //Maps label indexes to the amount of instances with that label
	private int [] mCorrectLabels; //Lists the correct label for each instance
	private int [] mFoldSizes;		 //Lists the size of every fold created by autoweka
	private int    mAmtFolds;
	private int    mAmtInstances;
	private int    mAmtLabels;

	//Global paths. Aliasing them for readability
	private String iwpPath; //Instancewise Predictions Directory Path (a directory containing txt files)
	private String cmPath;
	private String rPath;   //Configuration Ranking Path (a xml file)
	public Ensembler(String temporaryDirPath){ //TODO make some sort of factory for the many options
		iwpPath = temporaryDirPath+instancewiseInfoDirPath;
		rPath   = temporaryDirPath+configurationRankingPath;
		cmPath  = temporaryDirPath+configurationMapPath;

			// long startTime= System.nanoTime();
		mCfgList          = ConfigurationCollection.fromXML(rPath,ConfigurationCollection.class).asArrayList();
			// long endTime = System.nanoTime();
			// long totalTime = endTime-startTime;
			// long totalTimeSeconds = totalTime/1000000000;
			// System.out.println("@time for CC input: "+totalTime+" ms/"+totalTimeSeconds+" s");

		mCfgMap				= Util.getConfigurationMap(cmPath);
		mLabelMap         = new HashMap<String,Integer>();
		mLabelFrequencies = new HashMap<Integer,Integer>();
		mAmtFolds         = mCfgList.get(0).getAmtFolds();
		mFoldSizes        = new int[mAmtFolds];
		try{
			parseDatasetMetadata();
		}catch(FileNotFoundException e){
			log.debug("Couldn't find instancewise predictions for some fold");
		}catch(IOException e){
			log.debug("Couldn't read a line in one of the instancewise prediction logs");
		}
	}

	//Helper for the constructor.
	//Parses mAmtFolds, mFoldSizes, mCorrectLabels, mLabelsMap and mLabelFrequencies from the IWP logs of the best Configuration
	private void parseDatasetMetadata() throws FileNotFoundException,IOException{
		List<Integer> correctLabelsTemp = new ArrayList<Integer>();
		int winnerIdentifier = mCfgList.get(0).getIdentifier();
		//TODO fail autoweka silently if cant find all folds?
		//Iterating over folds
		for(int i=0;i<mAmtFolds;i++){
			String path = iwpPath+"hash:"+winnerIdentifier+"_fold:"+i+".txt";
			BufferedReader ciBR = Util.getBufferedReaderFromPath(path);

			int foldSize=0;
			String currentLine = ciBR.readLine();//skip first line
			for(currentLine = ciBR.readLine();currentLine!=null; currentLine = ciBR.readLine()){
				String correctLabel = Util.parseInstancewiseLine(currentLine,"ACTUAL_FULL");
				if(!mLabelMap.containsKey(correctLabel)){
					addLabel(correctLabel);
				}
				incrementLabelFrequency(correctLabel);
				correctLabelsTemp.add(mLabelMap.get(correctLabel));

				//Treating the unlikely edge case in which theres a label in the dataset
				//that's never assigned as the real one for any instance. Dont even know if thats possible. TODO check
				String predictedLabel = Util.parseInstancewiseLine(currentLine,"PREDICT_FULL");
				if(!mLabelMap.containsKey(predictedLabel)){
					addLabel(predictedLabel);
				}
				foldSize++;
			}
			mFoldSizes[i]=foldSize;
			ciBR.close();
		}

		mAmtLabels	  = mLabelMap.size();
		mAmtInstances = correctLabelsTemp.size();
		mCorrectLabels = new int[mAmtInstances];
		for(int i = 0; i < mAmtInstances;i++){
			mCorrectLabels[i]=correctLabelsTemp.get(i);
		}
	}

	//Helper for parseDatasetMetadata. Adds a label to the records.
	private void addLabel(String labelName){ //TODO Errocheck?
		int labelCounter = mLabelMap.size();
		mLabelMap.put( labelName, labelCounter );
		mLabelFrequencies.put(labelCounter,0);
	}

	//Helper for parseDatasetMetadata. Increments a point in the label histogram.
	private void incrementLabelFrequency(String correctLabel){ //Oh Java thou art a heartless type freak
		Integer labelIndex = mLabelMap.get(correctLabel);
		Integer temp = mLabelFrequencies.get(labelIndex);
		temp+=1;
		mLabelFrequencies.put(labelIndex,temp);
	}



	//The hillclimb method contains an implemention of Rich Caruana's Ensemble Selection, a greedy hillclimbing algorithm.
	//I'm doing it the straightforward way. A faster way is feasible, but so far this one always takes less than a second anyway

	//You can call it with default values using this. Curse java for its lack of support for default parameter values.
	public List<Configuration> hillclimb() throws FileNotFoundException,IOException{
		return hillclimb(true,5,200,3);
	}

	//You can also call it with other parameters.
	public List<Configuration> hillclimb(boolean onlyFullyPredicted, int noImprovementLimit, int ensembleMaxSize, int initialEnsembleSize){

		//Shallow copying mCfg list. Trick is ugly, but works ¯\_(ツ)_/¯
		List<Configuration> configBatch = (ArrayList<Configuration>)((ArrayList<Configuration>) mCfgList).clone();

		//If that's the case, removing configurations not evaluated on all folds
		if(onlyFullyPredicted){
			for(int i = configBatch.size()-1; i>=0 ; i--) if(configBatch.get(i).getAmtFolds()!=mAmtFolds){
				configBatch.remove(i);
			}
		}

		//Parsing the predictions made by each configuration
		List<EnsembleElement> eeBatch = new ArrayList<EnsembleElement>();
		for (Configuration c : configBatch){
			EnsembleElement ee = new EnsembleElement(c);
			eeBatch.add(ee);
		}


		//Initializing the ensemble
		List<EnsembleElement> currentPartialEnsemble = new ArrayList<EnsembleElement>();
		List<Integer> hillclimbingStepPerformances = new ArrayList<Integer>(); //So far, error counts TODO make it general
		for( int i=0 ; i<initialEnsembleSize && i<eeBatch.size(); i++ ){
			currentPartialEnsemble.add(eeBatch.get(i)); //Assumes theyre sorted
			hillclimbingStepPerformances.add(evaluateEnsemble(currentPartialEnsemble));
		}

		//Iterating over available ensemble slots.
		int noImprovementCounter = 0;
		for(int i = currentPartialEnsemble.size(); i<ensembleMaxSize ;i++){
			hillclimbingStepPerformances.add(takeStep(eeBatch,currentPartialEnsemble));
			if(i>0){
				if(hillclimbingStepPerformances.get(i)<hillclimbingStepPerformances.get(i-1)){
				 noImprovementCounter=0;
			   }else{
				 noImprovementCounter++;
			   }
			}
			if(noImprovementCounter==noImprovementLimit){
				break;
			}
		}

		//Slicing from 0 to the first occurrence of the smallest error count
		int sliceIndex = Util.indexMin(hillclimbingStepPerformances);
		currentPartialEnsemble = Util.getSlicedList(currentPartialEnsemble,0,sliceIndex);

		//Unwrapping the Configurations from the EnsembleElements
		List<Configuration> rv = new ArrayList<Configuration>();
		for(EnsembleElement ee : currentPartialEnsemble){
			rv.add(ee.getModel());
		}

		return rv;
	}


	private int takeStep(List<EnsembleElement> eeBatch, List<EnsembleElement> currentPartialEnsemble){
		int [] possibleChoicePerformances = new int[eeBatch.size()];

		//Iterating over possible choices in the batch, and evaluating each one.
		for(int i = 0; i<eeBatch.size(); i++){
			currentPartialEnsemble.add(eeBatch.get(i));
			possibleChoicePerformances[i]=evaluateEnsemble(currentPartialEnsemble);
			eeBatch.get(i).setPerformance(possibleChoicePerformances[i]);
			currentPartialEnsemble.remove(currentPartialEnsemble.size()-1);
		}

		//TODO count errors on EE construction and pick the least error prone?
		int bestIndex = Util.indexOptimum(possibleChoicePerformances,"RANDOM_MIN");
		currentPartialEnsemble.add(eeBatch.get(bestIndex));
		return possibleChoicePerformances[bestIndex]; //Returns performance of the ensemble with the new model
	}


	//Helper for hillclimb. Evaluates an Ensemble with regards to error amt.
	private int evaluateEnsemble(List<EnsembleElement> currentPartialEnsemble){ //TODO make it support other metrics
		int errAmt=0;
		for (int j = 0; j < mAmtInstances ; j++){
			int vote = majorityVote(j, currentPartialEnsemble);
			if( vote != mCorrectLabels[j]){
				errAmt++;
			}
		}
		return errAmt;
	}

	//Helper for evaluateEnsemble. Returns label index with the maximum votes. If there´s more than 1 maximum, it picks a random one.
	private int majorityVote(int instanceNum, List<EnsembleElement> currentPartialEnsemble){
		int [] labels = new int[mAmtLabels];
		for (EnsembleElement ee : currentPartialEnsemble){
			int vote = ee.getPrediction(instanceNum);
			labels[vote]++;
		}

		List<Integer> maxValueIndexes = Util.getMaxValueIndexes(labels);
		//return Util.indexOptimum(labels,"RANDOM_MAX");
		if(maxValueIndexes.size()==1){
			return maxValueIndexes.get(0);
		}else{
			return mostPrevalentLabel(maxValueIndexes);
		}
	}

	//Helper for majorityVote. Returns, from a selection of labels, one which is most prevalent in the dataset we're working with.
	private int mostPrevalentLabel(List<Integer> labels){
		int max = 0;
		int vote = 0;
		for(Integer i : labels){
			if (mLabelFrequencies.get(i)>max){
				max=mLabelFrequencies.get(i);
				vote=i;
			}
		}
		return vote;
	}

	private class EnsembleElement{

		private Configuration mModel;
		private double mWeight;
		private int mPerformance;
		private int [] mPredictions;

		public EnsembleElement(Configuration model, double weight){
			this(model);
			mWeight = weight;
		}

		public EnsembleElement(Configuration model){
			mModel = model;
			mPredictions = new int[mAmtInstances];
			mWeight=1;
			mPerformance = 0;
			try{
				this.parseInstancewiseInfo();
			}catch(FileNotFoundException e){
				log.debug("Couldn't find an instancewise predictions log");
			}catch(IOException e){
				log.debug("Couldn't read a line form an instancewise prediction log");
			}
		}

		public void parseInstancewiseInfo() throws FileNotFoundException, IOException{
			int totalInstanceIndex=0;
			//Iterating over folds
			for(int i = 0; i<mFoldSizes.length;i++){
				String path = iwpPath+"hash:"+mModel.getIdentifier()+"_fold:"+i+".txt";
				File ciFile = new File(path);
				if(ciFile.exists()){
					BufferedReader ciBR = Util.getBufferedReaderFromPath(path);
					ciBR.readLine(); //skipping first line of csv file
					for( String currentLine = ciBR.readLine() ; currentLine!=null ; currentLine = ciBR.readLine()){
						mPredictions[totalInstanceIndex]= mLabelMap.get(Util.parseInstancewiseLine(currentLine,"PREDICT_FULL"));
						totalInstanceIndex++;
					}
				}else{ //TODO define standard behaviour for this case
					for(int j=0;j<mFoldSizes[i];j++,totalInstanceIndex++)	mPredictions[totalInstanceIndex]=-1;
				}
			}
		}

		public String toString()                  {  return Integer.toString(this.hashCode());	}
		public int hashCode()                     {	return mModel.hashCode();	    	         }
		public String getArgStrings()             {  return mModel.getArgStrings(); 	         }
		public int getPrediction(int instanceNum) {  return mPredictions[instanceNum];         }
		public Configuration getModel()           {	return mModel;							         }
		public int getPerformance()               {	return mPerformance;					         }
		public void setPerformance(int p)         {	mPerformance = p;   					         }

	}

}
