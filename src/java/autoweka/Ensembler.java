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
	private Map<Integer,Integer> mLabelMap; //Maps class identifiers to ints 0-n where n is the number of classes
	private int [] mCorrectLabels;
	private int [] mFoldSizes;
	private int    mAmtFolds;
	private int    mAmtInstances;
	private int    mAmtLabels;

	private String iwpPath; //Aliasing for readability
	private String rPath;

	public Ensembler(String temporaryDirPath) throws FileNotFoundException,IOException{
		this.iwpPath = temporaryDirPath+instancewiseInfoDirPath;
		this.rPath   = temporaryDirPath+configurationRankingPath;
		mCfgList     = ConfigurationCollection.fromXML(rPath,ConfigurationCollection.class).asArrayList();
		mLabelMap    = new HashMap<Integer,Integer>();
		mAmtFolds    = mCfgList.get(0).getAmtFolds(); //TODO compute it from some global
		mFoldSizes   = new int[mAmtFolds];
		parseFoldDataFromLogs();
	}

	private void parseFoldDataFromLogs() throws FileNotFoundException,IOException{

		String winnerHash = Integer.toString(mCfgList.get(0).hashCode());
		int i = 0,instanceCounter=0;
		FileReader ciFR = null;
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
			ciBR.readLine();//skip first line
			for(String currentLine = ciBR.readLine() ; currentLine!=null ; currentLine = ciBR.readLine()){
				int ciActualLabelNumber = Integer.parseInt(Util.parseInstancewiseLine(currentLine,"ACTUAL_CODE"));
				mCorrectLabels[instanceCounter]=ciActualLabelNumber;
				if(!mLabelMap.containsKey(ciActualLabelNumber)){ //Labels can have crazy formats and values, lets map that to tame sequential ints.
					mLabelMap.put(ciActualLabelNumber,mLabelMap.size()); //using the size as a counter type of thing ^_^
				}
				instanceCounter++;
			}
			ciBR.close();
		}
		mAmtLabels=mLabelMap.size();
	}

	//Greedy ensemble selection hillclimbing process
	public List<Configuration> hillclimb(boolean onlyFullyPredicted) throws FileNotFoundException,IOException{ //Doing it the straightforward way. Gonna try a faster way later, just wanna get this working.

		//Shallow copying mCfg list. Trick is ugly but works.
		List<Configuration> configBatch = (ArrayList<Configuration>)((ArrayList<Configuration>) mCfgList).clone();

		//Removing configurations not evaluated on all folds
		if(onlyFullyPredicted){
			for(int i = configBatch.size()-1; i>=0 ; i--) if(configBatch.get(i).getAmtFolds()!=mAmtFolds){
				System.out.println("@removing: "+configBatch.get(i).getArgStrings());
				configBatch.remove(i);
			}
		}

		//Parsing the predictions made by each configuration
		List<EnsembleElement> eeBatch = new ArrayList<EnsembleElement>();
		for (Configuration c: configBatch){
			EnsembleElement ee = new EnsembleElement(c,mAmtInstances,mFoldSizes);
			ee.parseInstancewiseInfo(iwpPath); //TODO  maybe put a call to this in the EE constructor
			eeBatch.add(ee);
		}

		System.out.println("@eebatch:");
		for(EnsembleElement ee : eeBatch){
			System.out.println("@model_hash: "+ee.hashCode());
		}

		//Building the ensemble
		List<EnsembleElement> currentPartialEnsemble = new ArrayList<EnsembleElement>();
		int [] hillclimbingStepPerformances = new int[eeBatch.size()]; //So far, error count TODO make it general

		for(int i = 0; !eeBatch.isEmpty() ;i++){//Iterating over available ensemble slots. TODO make the initialization batch flexible.

			int[] performanceAndIndex = chooseModel(eeBatch,currentPartialEnsemble);
			EnsembleElement ciChosenModel = eeBatch.get(performanceAndIndex[1]);
			currentPartialEnsemble.add(ciChosenModel);
			eeBatch.remove(ciChosenModel);  //TODO just save bools right/wrong and then use logical ops ^o^
			hillclimbingStepPerformances[i]=performanceAndIndex[0];

			System.out.println("@currentPartialEnsemble on iteration i="+i);
			System.out.println("score: "+hillclimbingStepPerformances[i]);
			for(int k=0; k<currentPartialEnsemble.size();k++){
				System.out.println("hash: "+currentPartialEnsemble.get(k).getModel().hashCode());
			}
		} //TODO have something that evaluates that its likely not climbing anymore to stop earlier

		//Slicing it at best place
		int bestIndex = Util.indexMin(hillclimbingStepPerformances);
		currentPartialEnsemble = Util.getSlicedList(currentPartialEnsemble,0,bestIndex);

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
				if(_majorityVote(j, currentPartialEnsemble) != mCorrectLabels[j]){
					possibleChoicePerformances[i]++;
				}
			}
			currentPartialEnsemble.remove(eeBatch.get(i));
		}

		int bestIndex = Util.indexMin(possibleChoicePerformances);
		int [] output = {possibleChoicePerformances[bestIndex],bestIndex};
		return output; //Curse java and it's lack of native tuples
	}

	private int _majorityVote(int instanceNum, List<EnsembleElement> currentPartialEnsemble){
		int [] votes = new int [mAmtLabels]; //TODO compute amtclasses correctly
		//System.out.println("@amtClasses in _mv:"+mAmtLabels);
		for (EnsembleElement ee : currentPartialEnsemble){
			int vote = ee.getPrediction(instanceNum);
			Integer index = mLabelMap.get(vote);
		//	System.out.println("@wasmapped: instance:"+instanceNum+" vote:"+vote+ " to label index:"+ index);
			votes[index]++;
		}

		return Util.indexMax(votes);

		//TODO treat duplicate max indexes differently than returning first one?
	}

}
