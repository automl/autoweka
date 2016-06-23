package autoweka;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.Map;
import java.util.HashMap;
import java.util.Stream;
import java.util.stream.Collectors;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

import weka.classifiers.meta.AutoWEKAClassifier.temporaryDirPath;
import weka.classifiers.meta.AutoWEKAClassifier.configurationRankingPath;
import weka.classifiers.meta.AutoWEKAClassifier.configurationInfoDirPath;
import weka.classifiers.meta.AutoWEKAClassifier.configurationHashSetPath;
import weka.classifiers.meta.AutoWEKAClassifier.instancewiseInfoDirPath;

public class Ensembler{
  private List<Configuration> cfgList;

  //private List<EnsembleElement> mCurrentEnsemble;
  private int [] mFoldSizes;
  private Integer [] mCorrectClasses;
  private int    mAmtFolds;

  public String iwpPath; //i

  public Ensembler(String iwpPath,String rPath){ //TODO grab fold amt form winner then treat for exceptions in case u cant parse them all
    this.cfgList = ConfigurationCollection.fromXML(rPath,ConfigurationCollection.class).asArrayList();
    mAmtFolds = cfgList.get(0).getAmtFolds();
    parseFoldData(iwpPath);
  }

  private void parseFoldData(String iwpPath){
    List<Integer> correctClassesList = new ArrayList<Integer>();
    mFoldSizes = new int[mAmtFolds];
    String winnerHash = cfgList.get(0).hashCode();
    for(i=0;i<mAmtFolds;i++){
      try{
        FileReader ciFileReader = new FileReader(iwpPath+"/hash:"+winnerHash+"_fold:"+i+".txt");
        BufferedReader ciBufferedReader = new BufferedReader(ciFileReader);
      }catch(Exception e){
        System.out.println("Couldn't find instancewise predictions for final incument on "+i+"-th fold");
      }
      String currentLine = ciBufferedReader.readLine();//skip first line
      int foldSize = 0;
      while(currentLine!=null){
        correctClassesList = ciBufferedReader.readLine()
        String [] gambiarra1 = currentLine.split(","); //TODO use regex
        String [] gambiarra2 = gambiarra1[1].split(":");
        predictions.add(Integer.valueOf(gambiarra2[0]));
        foldSize++;
      }
      mFoldSizes[i]=foldSize;
    }
    mCorrectClasses = new Integer[correctClassesList.size()];
    correctClassesList.toArray(mCorrectClasses); //TODO optimize this into int array
  }


  public void hillclimb(boolean onlyFullyPredicted){ //Doing it the straightforward way. Gonna try a faster way later, just wanna get this working.

    List<Configuration> configBatch = (List<Configuration>) cfgList.clone(); //shallow copying

    if(onlyFullyPredicted){
      for(int i = configBatch.size()-1; i>=0 ; i--) if(configBatch.get(i)!=mFoldAmt) configBatch.remove(i);
    }

    List<EnsembleElement> eeBatch = new List<EnsembleElement>();
    for (Configuration c: configBatch){
      EnsembleElement ciEE = new EnsembleElement(c);
      ciEE.parseInstancewiseInfo(iwpPath,mFoldSizes); //TODO review this
      eeBatch.add(ciEE);
    }

    int [] partialEnsembleScores = new int [eeBatch.size()];
    //int
    List<EnsembleElement> currentPartialEnsemble = new ArrayList<EnsembleElement>();

    bestPartialEnsembleScore = 0;
    bestPartialEnsembleIndex = 0;
    for(i = 0; !eeBatch.isEmpty(); i++){ //Iterating over available ensemble slots. Starts at 1 cause we have the best one in already. TODO make the initialization batch flexible.

      int bestChoiceScore = 0;
      EnsembleElement chosenModel = null;
      for( EnsembleElement ee : eeBatch){
        int scoreWithThisModel = _evaluateModelChoice(currentPartialEnsemble,ee);
        if (scoreWithThisModel>bestChoiceScore){
          bestChoiceScore = scoreWithThisModel;
          chosenModel=ee;
        }
      }
      currentPartialEnsemble.add(chosenModel);
      eeBatch.remove(chosenModel);
      if(bestChoiceScore>bestPartialEnsembleScore){
        bestPartialEnsembleScore = bestChoiceScore;
        bestPartialEnsembleIndex = i;
      }
    }
    //TODO:
    //Slice models that make it worse
    //return currentPE
  }

  private int _evaluateModelChoice(List<EnsembleElement> currentPartialEnsemble, EnsembleElement modelChoice){

  }

  private int _majorityVote(int instanceNum, List<EnsembleElement> currentPartialEnsemble){
    int [] votes = new int [amtClasses];
    for (EnsembleElement ee : currentPartialEnsemble){
      votes[ee.getPrediction(instanceNum)]++;
    }
    int max = 0;
    int maxindex=0;
    for(int i = 0; i<votes.length;i++){
      if(votes[i]>max){
        max = votes[i]; //TODO make an utility for this kinda stuff?
        maxindex = i;
      }
    }
    //TODO treat duplicate max indexes?
  }


  private class EnsembleElement{ //TODO make this separate?

    private double mWeight;
    private Configuration mModel;
    private int[] mPredictions; //TODO bring that here from configuration

    public EnsembleElement(Configuration model, double weight){
      this(model);
      mWeight = weight;
    }

    public EnsembleElement(Configuration model){
      mModel = model;
      mPredictions = new int[model.getInstanceAmt()];
      mWeight=1;
    }

    public int getPrediction(int instanceNum){
      return mPredictions[instanceNum];
    }

    public void parseInstancewiseInfo(String instancewiseLogPath, Map <Integer,Integer> foldMetadata){ //TODO get instancewiseLogPath from global, maybe have foldmetadata globally too
      int max = Collections.max(foldMetadata.keySet());
      int currentInstanceIndex=0;
      mPredictions = new int[max];

      for(int i = 0; i< max;i++){
        File ciFile = new File(instancewiseLogPath+"/"+"instancewise_predictions_hash:"+mModel.hashCode()+"_fold:"+i+".txt");
        FileReader ciFR = new FileReader(ciFile);
        BufferedReader ciBR = new BufferedReader(ciFR); //hue
        if(ciFile.exists()){
          ciBR.nextLine(); //skipping first line of csv file
          for(int j=0;j<foldMetadata.get(i);j++,currentInstanceIndex++){
            String instanceLine = ciBR.nextLine();
            String [] gambiarraTemp = instanceLine.split(",");
            String [] gambiarra = gambiarraTemp[2].split(":");
            mPredictions[currentInstanceIndex]=Integer.parseInt(gambiarra[0]);
          }
        }else{
          for(int j=0;j<foldMetaData.get(i);j++,currentInstanceIndex++){
              mPredictions[currentInstanceIndex]=-1;
          }
        }
      }
    }

  }

}
