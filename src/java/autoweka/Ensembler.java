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
  private List<Configuration> mCfgList;

  //private List<EnsembleElement> mCurrentEnsemble;
  private int [] mFoldSizes;
  private Integer [] mCorrectClasses;
  private int    mAmtFolds;
  private int    mAmtInstances;

  private String iwpPath; //Aliasing for readability
  private String rPath;
  public Ensembler(){ //TODO grab fold amt form winner then treat for exceptions in case u cant parse them all
    this.iwpPath = instancewiseInfoDirPath;
    this.rPath   = configurationRankingPath;
    mCfgList = ConfigurationCollection.fromXML(rPath,ConfigurationCollection.class).asArrayList();
    mAmtFolds = mCfgList.get(0).getAmtFolds();
    parseFoldData();
  }

  private void parseFoldData(){

    List<Integer> correctClassesList = new ArrayList<Integer>();
    mFoldSizes = new int[mAmtFolds];
    String winnerHash = mCfgList.get(0).hashCode();

    for(i=0;i<mAmtFolds;i++){//iterating over folds

      try{
        FileReader     ciFileReader = new FileReader(iwpPath+"hash:"+winnerHash+"_fold:"+i+".txt");
        BufferedReader ciBufferedReader = new BufferedReader(ciFileReader);
      }catch(Exception e){
        System.out.println("Couldn't find instancewise predictions for final incument on "+i+"-th fold");
      }

      String currentLine = ciBufferedReader.readLine();//skip first line
      int foldSize = 0;
      while(currentLine!=null){//iterating over instances
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

    List<Configuration> configBatch = (List<Configuration>) mCfgList.clone(); //shallow copying

    //Removing configurations not evaluated on all folds
    if(onlyFullyPredicted){
      for(int i = configBatch.size()-1; i>=0 ; i--) if(configBatch.get(i)!=mFoldAmt) configBatch.remove(i);
    }

    //Parsing the predictions made by each configuration
    List<EnsembleElement> eeBatch = new List<EnsembleElement>();
    for (Configuration c: configBatch){
      EnsembleElement ee = new EnsembleElement(c);
      ee.parseInstancewiseInfo(mFoldSizes); //TODO review this
      eeBatch.add(ee);
    }

    //Building the ensemble
    List<EnsembleElement> currentPartialEnsemble = new ArrayList<EnsembleElement>();
    int [] partialEnsembleScores = new int[eeBatch.size()];
    int index = 0;
    while(!eeBatch.isEmpty()){//Iterating over available ensemble slots. TODO make the initialization batch flexible.

      int leastErrorsThisSlot = mInstanceAmt;
      EnsembleElement chosenModel = null;
      for(EnsembleElement ee : eeBatch){//Iterating over remaining ensembles in the batch
        int scoreWithThisModel= _evaluateModelChoice(currentPartialEnsemble,ee);
        if (scoreWithThisModel<leastErrorsThisSlot){
          leastErrorsThisSlot = scoreWithThisModel;
          partialEnsembleScores[index] = scoreWithThisModel;
          chosenModel = ee;
        }
      }
      if (chosenModel==null) throw new RuntimeException(); //TODO write message
      currentPartialEnsemble.add(chosenModel);
      eeBatch.remove(chosenModel);
      index++;
    } //TODO have something that evaluates that its likely not climbing anymore

    //Slicing it at best place
    int errorAmtPE=mAmtInstances;
    int bestIndex=0;
    for(int i=0;i<partialEnsembleScores.length;i++){
      if(partialEnsembleScores i < errorAmtPE){
        bestIndex = i;
      }
    }
    for(int i = currentPartialEnsemble.size()-1;i>bestIndex;i--){
      currentPartialEnsemble.remove(i);
    }

    List<Configuration> rv = new List<Configuration>();
    for(EnsembleElement ee : currentPartialEnsemble){
      rv.add(ee.getModel());
    }
    return rv;
  }

  private int _evaluateModelChoice(List<EnsembleElement> currentPartialEnsemble, EnsembleElement modelChoice){
    currentPartialEnsemble.add(modelChoice);
    int amtErrors = 0;
    for (int i = 0; i < mInstanceAmt ; i++){
      if(_majorityVote(i,currentPartialEnsemble) != mCorrectClasses[i]){
        amtErrors++
      }
    }
    return amtErrors;
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
    private int [] mPredictions;
    private int [] mfoldSizes;
    private int mInstanceAmt;

    public EnsembleElement(Configuration model, int instanceAmt, int[] foldSizes, double weight){
      this(model,instanceAmt,foldSizes);
      mWeight = weight;
    }

    public EnsembleElement(Configuration model, int instanceAmt, int[] foldSizes){
      mModel = model;
      mFoldSizes = foldSizes;
      mInstanceAmt = instanceAmt;
      mPredictions = new int[instanceAmt];
      mWeight=1;
    }

    public int getPrediction(int instanceNum){
      return mPredictions[instanceNum];
    }
    public Configuration getModel(){
      return mModel;
    }

    public void parseInstancewiseInfo(int [] foldMetadata){ //TODO get instancewiseLogPath from global, maybe have foldmetadata globally too
      String iwpPath = instancewiseInfoDirPath; // aliasing for readability

      mPredictions = new int[max];

      int totalInstanceIndex=0;
      for(int i = 0; i< max;i++){ //iterating over instancewise logs for each fold
        File ciFile = new File(iwpPath+"hash:"+mModel.hashCode()+"_fold:"+i+".txt");

        if(ciFile.exists()){

          FileReader ciFR = new FileReader(ciFile);
          BufferedReader ciBR = new BufferedReader(ciFR); //hue
          ciBR.nextLine(); //skipping first line of csv file

          for(int j=0;j<foldSizes.length;j++,totalInstanceIndex++){ //iterating over lines
            String instanceLine = ciBR.nextLine(); //TODO use regex
            String [] gambiarraTemp = instanceLine.split(",");
            String [] gambiarra = gambiarraTemp[2].split(":");
            mPredictions[totalInstanceIndex]=Integer.parseInt(gambiarra[0]);
          }

        }else{
          for(int j=0;j<foldMetaData.get(i);j++,totalInstanceIndex++){ //TODO define standard behaviour for this case
              mPredictions[totalInstanceIndex]=-1;
          }
        }
      }
    }//method

  }//EE class

}
