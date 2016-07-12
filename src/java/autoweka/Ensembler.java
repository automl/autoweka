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
import static weka.classifiers.meta.AutoWEKAClassifier.configurationInfoDirPath;
import static weka.classifiers.meta.AutoWEKAClassifier.instancewiseInfoDirPath;

public class Ensembler{

	final static Logger log = LoggerFactory.getLogger(Ensembler.class);

	private List<Configuration> mCfgList;   //List of configurations given as input via the configuration ranking
	private Map<String,Integer> mLabelMap; //Maps labels to ints 0-n where n is the number of classes
	private Map<Integer,String> mInverseLabelMap; //Maps ints 0-n to the labels

	private int [] mCorrectLabels; //Correct labels as ints defined by mLabelMap
	private int [] mFoldSizes;
	private int    mAmtFolds;
	private int    mAmtInstances;
	private int    mAmtLabels;

	private String iwpPath; //Aliasing for readability
	private String rPath;

	public Ensembler(String temporaryDirPath) throws FileNotFoundException,IOException{ //TODO make some sort of factory for the many options
		this.iwpPath     = temporaryDirPath+instancewiseInfoDirPath;
		this.rPath       = temporaryDirPath+configurationRankingPath;
		mCfgList         = ConfigurationCollection.fromXML(rPath,ConfigurationCollection.class).asArrayList();
		mLabelMap        = new HashMap<String,Integer>();
		mInverseLabelMap = new HashMap<Integer,String>();
		mAmtFolds        = mCfgList.get(0).getAmtFolds(); //TODO compute it from some global
		mFoldSizes       = new int[mAmtFolds];
		parseFoldDataFromLogs();
	}

	private void parseFoldDataFromLogs() throws FileNotFoundException,IOException{

		String winnerHash = Integer.toString(mCfgList.get(0).hashCode());
		int i = 0,instanceCounter=0;
		FileReader     ciFR = null;
		BufferedReader ciBR = null;

		//Counting instances
		for(i=0;i<mAmtFolds;i++){
			//opening buffers
			String path = iwpPath+"hash:"+winnerHash+"_fold:"+i+".txt";

			try{
				ciFR = new FileReader(path);
				ciBR = new BufferedReader(ciFR);
			}catch (FileNotFoundException e){
				log.debug("Couldn't find file: "+ path);
				throw e;
			}

			//Counting
			int foldSize=0;
			try{
				String currentLine = ciBR.readLine();//skip first line
				for(currentLine = ciBR.readLine();currentLine!=null; currentLine = ciBR.readLine()){
					foldSize++;
				}
			}catch (IOException e){
				log.debug("Couldn't read a line on file: "+ path);
				throw e;
			}

			//Saving
			mFoldSizes[i]=foldSize;
			mAmtInstances+=foldSize;
			ciBR.close();
		}

		//Parsing the actual labels for every instance
		mCorrectLabels = new int[mAmtInstances];
		for(i=0;i<mAmtFolds;i++){//iterating over folds
			//Setting up the buffers
			try{
				ciFR = new FileReader(iwpPath+"hash:"+winnerHash+"_fold:"+i+".txt");
				ciBR = new BufferedReader(ciFR);
			}catch(FileNotFoundException e){
				log.debug("Couldn't find instancewise predictions for final incument on "+i+"-th fold");
				throw e;
			}

			//Looking at every instance
			int labelCounter=0;
			ciBR.readLine();//skip first line
			for(String currentLine = ciBR.readLine() ; currentLine!=null ; currentLine = ciBR.readLine()){
				String correctLabel = Util.parseInstancewiseLine(currentLine,"ACTUAL_FULL");
				if(!mLabelMap.containsKey(correctLabel)){ //Labels can have crazy formats and values, lets map that to tame sequential ints.
					mLabelMap.put( correctLabel, labelCounter );
					mInverseLabelMap.put( labelCounter , correctLabel );
					labelCounter++;
				}
				mCorrectLabels[instanceCounter]=mLabelMap.get(correctLabel);
				instanceCounter++;
			}
			ciBR.close();
		}
		mAmtLabels=mLabelMap.size();
	}

	public void println(String s){ //Because i'm tired of typing it all
		System.out.println(s); //TODO erase that later
	}

	public void print(String s){
		System.out.print(s);
	}

	public void printArray(int [] array){
		String s =("\n[");
		for(int i = 0; i < array.length; i++){
			s+=(array[i]+",");
		}
		s+=("]\n");
		System.out.println(s);
	}

	public void printList(List list){
		String s =("\n[");
		for(int i = 0; i < list.size(); i++){
			s+=(list.get(i).toString()+",");
		}
		s+=("]\n");
		System.out.println(s);
	}

	//Greedy ensemble selection hillclimbing process
	public List<Configuration> hillclimb(boolean onlyFullyPredicted) throws FileNotFoundException,IOException{ //Doing it the straightforward way. Gonna try a faster way later, just wanna get this working.
		System.out.println("@mCorrectLabels");
		printArray(mCorrectLabels);

		int ensemble_size=50;

		//Shallow copying mCfg list. Trick is ugly but works.
		List<Configuration> configBatch = (ArrayList<Configuration>)((ArrayList<Configuration>) mCfgList).clone();

		//Removing configurations not evaluated on all folds
		//System.out.print("\n");
		if(onlyFullyPredicted){
			for(int i = configBatch.size()-1; i>=0 ; i--) if(configBatch.get(i).getAmtFolds()!=mAmtFolds){
			//	System.out.print(", @removing: "+configBatch.get(i).hashCode());
				configBatch.remove(i);
			}
		}

		//Parsing the predictions made by each configuration
		List<EnsembleElement> eeBatch = new ArrayList<EnsembleElement>();
		for (Configuration c : configBatch){
			EnsembleElement ee = new EnsembleElement(c);
			ee.parseInstancewiseInfo(iwpPath); //TODO  maybe put a call to this in the EE constructor
			eeBatch.add(ee);
		}

		//Initializing the ensemble
		List<EnsembleElement> currentPartialEnsemble = new ArrayList<EnsembleElement>();
		int [] hillclimbingStepPerformances = new int[ensemble_size]; //So far, error count TODO make it general

		println("@eebatch");
		printList(eeBatch);
		for( int i=0 ; i<3 && i<eeBatch.size() ; i++ ){
			currentPartialEnsemble.add(eeBatch.get(i)); //They should be sorted, right?
		}

		//Iterating over available ensemble slots. TODO make the initialization batch flexible.
		for(int i = 0; i<ensemble_size ;i++){

			int [] performanceAndIndex = chooseModel(eeBatch,currentPartialEnsemble); //this is a "tuple"
			EnsembleElement ciChosenModel = eeBatch.get(performanceAndIndex[1]);
			currentPartialEnsemble.add(ciChosenModel);
			//eeBatch.remove(ciChosenModel);
			hillclimbingStepPerformances[i]=performanceAndIndex[0];

			//System.out.println("@currentPartialEnsemble on iteration i="+i);
			//System.out.println("score: "+hillclimbingStepPerformances[i]);
			//for(int k=0; k<currentPartialEnsemble.size();k++) System.out.print(","+currentPartialEnsemble.get(k).getModel().hashCode());
			//System.out.println("\n");

		} //TODO have something that evaluates that its likely not climbing anymore to stop earlier

		printList(currentPartialEnsemble);
		printArray(hillclimbingStepPerformances);
		//Slicing it at highest hillclimbing performance
		int bestIndex = Util.indexMin(hillclimbingStepPerformances);
		currentPartialEnsemble = Util.getSlicedList(currentPartialEnsemble,0,bestIndex);

		//Setting up method output
		List<Configuration> rv = new ArrayList<Configuration>();
		for(EnsembleElement ee : currentPartialEnsemble){
			rv.add(ee.getModel());
		}
		return rv;
	}


	private int[] chooseModel(List<EnsembleElement> eeBatch, List<EnsembleElement> currentPartialEnsemble){
		int [] possibleChoicePerformances = new int[eeBatch.size()];

		//Iterating over possible choices in the batch
		for(int i = 0; i<eeBatch.size(); i++){
			currentPartialEnsemble.add(eeBatch.get(i));

			//Iterating over {CPE + i-th choice} to compute the CPE performance with this choice.
			for (int j = 0; j < mAmtInstances ; j++){
				int vote = _majorityVote(j, currentPartialEnsemble);
				// System.out.print("@ vote,real: "+vote+","+mCorrectLabels[j]);
				// System.out.println( (vote != mCorrectLabels[j])? "WRONG": "");
				if( vote != mCorrectLabels[j]){
					possibleChoicePerformances[i]++;
				}
			}
			currentPartialEnsemble.remove(currentPartialEnsemble.size()-1);
		}
		System.out.println("@[choice performances], [best,index]\n");
		//printArray(possibleChoicePerformances);
		int bestIndex = Util.randomizedIndexMin(possibleChoicePerformances);
		int [] output = {possibleChoicePerformances[bestIndex],bestIndex};
		printArray(output);
		return output; //Curse java and it's lack of native tuples
	}



	private int _majorityVote(int instanceNum, List<EnsembleElement> currentPartialEnsemble){
		int [] votes = new int [mAmtLabels];
		for (EnsembleElement ee : currentPartialEnsemble){
			int vote_index = ee.getPrediction(instanceNum);
			//System.out.println(vote);
		   //System.out.println("@wasmapped: instance:"+instanceNum+" vote:"+vote+ " to label index:"+ index);
			votes[vote_index]++;
		}
		//printArray(votes);
		//return mInverseLabelMap.get(Util.indexMax(votes)); //TODO not randomized
		return Util.randomizedIndexMax(votes);
		//TODO treat duplicate max indexes differently than returning first one?
	}

	private class EnsembleElement{

		private Configuration mModel;
		private double mWeight;
		private int [] mPredictions;

		public EnsembleElement(Configuration model, double weight){
			this(model);
			mWeight = weight;
		}

		public EnsembleElement(Configuration model){
			mModel = model;
			mPredictions = new int[mAmtInstances];
			mWeight=1;
		}

		public void parseInstancewiseInfo(String iwpPath) throws FileNotFoundException,IOException{ //TODO get instancewiseLogPath from global, maybe have foldSizes globally too
			//TODO change the way iwpPath is provided

			//Iterating over folds
			int totalInstanceIndex=0;

			for(int i = 0; i<mFoldSizes.length;i++){ //iterating over instancewise logs for each fold

				String path = iwpPath+"hash:"+mModel.hashCode()+"_fold:"+i+".txt";
				File ciFile = new File(path);

				FileReader 	   ciFR = null;
				BufferedReader ciBR = null ;

				if(ciFile.exists()){
					try{
						ciFR = new FileReader(path);
						ciBR = new BufferedReader(ciFR); //hue
					}catch (FileNotFoundException e){
						System.out.println("Couldn't initialize ciBR");
						throw e;
					}

					//Iterating over lines
					try{
							ciBR.readLine(); //skipping first line of csv file
						for( String currentLine = ciBR.readLine() ; currentLine!=null ; currentLine = ciBR.readLine()){ //iterating over lines
							mPredictions[totalInstanceIndex]= mLabelMap.get(Util.parseInstancewiseLine(currentLine,"PREDICT_FULL"));
							//			System.out.println("@prediction for hash "+mModel.hashCode()+" on instance "+totalInstanceIndex+" is "+mPredictions[totalInstanceIndex]);
							totalInstanceIndex++;
						}
					}catch(IOException e){
						log.debug("Couldn't read a line in file "+path);
						throw e;
					}

				}else{ //TODO define standard behaviour for this case
					for(int j=0;j<mFoldSizes[i];j++,totalInstanceIndex++)	mPredictions[totalInstanceIndex]=-1;
				}
			}
		}

		public String toString()                  {  return Integer.toString(this.hashCode());			 	}
		public int hashCode()                     {	return mModel.hashCode();	    	}
		public String getArgStrings()             {  return mModel.getArgStrings(); 	}

		public int getPrediction(int instanceNum) {  return mPredictions[instanceNum];}
		public Configuration getModel()           {	return mModel;							}

	}

}
