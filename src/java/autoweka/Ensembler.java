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

	//Candidate Configurations
	private List<Configuration> mCfgList;  //List of configurations read from the configuration ranking file at rPath

	//Data about the instances
	private Map<String,Integer> mLabelMap; //Maps the existing labels in the dataset to ints [0:n-1] where n is the number of labels
	private int [] mCorrectLabels; //Lists the correct label for each instance
	private int [] mFoldSizes;		 //Lists the size of every fold created by autoweka
	private int    mAmtFolds;
	private int    mAmtInstances;
	private int    mAmtLabels;

	//Global paths. Aliasing them for readability
	private String iwpPath; //Instancewise Predictions Directory Path (a directory containing txt files)
	private String rPath;   //Configuration Ranking Path (a xml file)

	public Ensembler(String temporaryDirPath) throws FileNotFoundException,IOException{ //TODO make some sort of factory for the many options
		this.iwpPath     = temporaryDirPath+instancewiseInfoDirPath;
		this.rPath       = temporaryDirPath+configurationRankingPath;
		mCfgList         = ConfigurationCollection.fromXML(rPath,ConfigurationCollection.class).asArrayList();
		mLabelMap        = new HashMap<String,Integer>();
		//mInverseLabelMap = new HashMap<Integer,String>();
		mAmtFolds        = mCfgList.get(0).getAmtFolds(); //TODO compute it from some global
		mFoldSizes       = new int[mAmtFolds];
		parseFoldDataFromLogs();
	}

	//TODO make it a single loop, you´re doing something retarded 8D
	private void parseFoldDataFromLogs() throws FileNotFoundException,IOException{

		String winnerHash = Integer.toString(mCfgList.get(0).hashCode());
		int i = 0,instanceCounter=0;
		FileReader     ciFR = null;
		BufferedReader ciBR = null;

		//Counting the instances
		for(i=0;i<mAmtFolds;i++){
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

		//Parsing the real labels for every instance
		mCorrectLabels = new int[mAmtInstances];
		for(i=0;i<mAmtFolds;i++){

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
					//mInverseLabelMap.put( labelCounter , correctLabel );
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
		printArray(array,array.length);
	}

	public void printArray(int [] array, int limit){
		String s =("\n[");
		for(int i = 0; i < limit; i++){
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
		int noImprovementLimit = 5;
		int ensemble_size = 50;

		//Shallow copying mCfg list. Trick is ugly but works.
		List<Configuration> configBatch = (ArrayList<Configuration>)((ArrayList<Configuration>) mCfgList).clone();

		//Removing configurations not evaluated on all folds
		if(onlyFullyPredicted){
			for(int i = configBatch.size()-1; i>=0 ; i--) if(configBatch.get(i).getAmtFolds()!=mAmtFolds){
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
			int errAmt=0;
			for (int j = 0; j < mAmtInstances ; j++){
				int vote = _majorityVote(j, currentPartialEnsemble);
				if( vote != mCorrectLabels[j]){
					errAmt++;
				}
			}
			hillclimbingStepPerformances[i]=errAmt;
		}

		//Iterating over available ensemble slots. TODO make the initialization batch flexible.
		int noImporovementCounter = 0;
		for(int i = 3; i<ensemble_size ;i++){

			int [] performanceAndIndex = chooseModel(eeBatch,currentPartialEnsemble); //this is a "tuple"
			EnsembleElement ciChosenModel = eeBatch.get(performanceAndIndex[1]);
			currentPartialEnsemble.add(ciChosenModel);
			//eeBatch.remove(ciChosenModel);
			hillclimbingStepPerformances[i]=performanceAndIndex[0];
			// if(i!=0 && (hillclimbingStepPerformances[i]>hillclimbingStepPerformances[i-1])){
			// 	noImporovementCounter=0;
			// }else{
			// 	noImporovementCounter++;
			// }
			// if(noImporovementCounter==noImprovementLimit) {
			// 	break;
			// }
		} //TODO have something that evaluates that its likely not climbing anymore to stop earlier

		println("@Full hillclimbing trajectory models:");
		printList(currentPartialEnsemble);
		println("@Full hillclimbing trajectory scores:");
		printArray(hillclimbingStepPerformances);
		//Slicing it at highest hillclimbing performance
		int bestIndex = Util.indexMin(hillclimbingStepPerformances);
		currentPartialEnsemble = Util.getSlicedList(currentPartialEnsemble,0,bestIndex);

		//Setting up method output
		List<Configuration> rv = new ArrayList<Configuration>();
		for(EnsembleElement ee : currentPartialEnsemble){
			rv.add(ee.getModel());
		}

		println("@Sliced hillclimbing trajectory models:");
		printList(rv);

		println("@Sliced hillclimbing trajectory scores:");
		printArray(hillclimbingStepPerformances,rv.size());

		return rv;
	}


	private int[] chooseModel(List<EnsembleElement> eeBatch, List<EnsembleElement> currentPartialEnsemble){

		int [] possibleChoicePerformances = new int[eeBatch.size()];

		//Iterating over possible choices in the batch, and evaluating each one.
		for(int i = 0; i<eeBatch.size(); i++){
			currentPartialEnsemble.add(eeBatch.get(i));
			possibleChoicePerformances[i]=evaluateEnsemble(currentPartialEnsemble);
			//Iterating over {CPE + i-th choice} to compute the CPE performance with this choice.
			currentPartialEnsemble.remove(currentPartialEnsemble.size()-1);
		}

		int bestIndex = Util.randomizedIndexMin(possibleChoicePerformances);
		int [] output = {possibleChoicePerformances[bestIndex],bestIndex};
		return output; //Curse java and it's lack of native tuples
	}

	//Evaluates an Ensemble with regards to error amt. TODO make it general to other metrics
	private int evaluateEnsemble(List<EnsembleElement> currentPartialEnsemble){
		int errAmt=0;
		for (int j = 0; j < mAmtInstances ; j++){
			int vote = _majorityVote(j, currentPartialEnsemble);
			if( vote != mCorrectLabels[j]){
				errAmt++;
			}
		}
		return errAmt;
	}

	//Returns label index with the maximum votes. If there´s more than 1 maximum, it picks a random one. TODO bias towards more prevalent class
	private int _majorityVote(int instanceNum, List<EnsembleElement> currentPartialEnsemble){
		int [] votes = new int[mAmtLabels];
		for (EnsembleElement ee : currentPartialEnsemble){
			int vote_index = ee.getPrediction(instanceNum);
			votes[vote_index]++;
		}
		return Util.randomizedIndexMax(votes);
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
