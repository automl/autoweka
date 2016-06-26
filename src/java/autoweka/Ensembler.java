package autoweka;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;


import static weka.classifiers.meta.AutoWEKAClassifier.configurationRankingPath;
import static weka.classifiers.meta.AutoWEKAClassifier.configurationInfoDirPath;
import static weka.classifiers.meta.AutoWEKAClassifier.configurationHashSetPath;
import static weka.classifiers.meta.AutoWEKAClassifier.instancewiseInfoDirPath;

public class Ensembler{
  private List<Configuration> mCfgList;
  private Integer [] mCorrectClasses;
  private int [] mFoldSizes;
  private int    mAmtFolds;
  private int    mAmtInstances;
  private int    mAmtClasses;

  private String iwpPath; //Aliasing for readability
  private String rPath;

  public Ensembler(String temporaryDirPath) throws Exception{
    this.iwpPath = temporaryDirPath+instancewiseInfoDirPath;
    this.rPath   = temporaryDirPath+configurationRankingPath;
    mCfgList = ConfigurationCollection.fromXML(rPath,ConfigurationCollection.class).asArrayList();
    mAmtFolds = mCfgList.get(0).getAmtFolds();
    parseFoldData();
  }

  private void parseFoldData() throws Exception{ //TODO merge this with the similar method in EE somehow.

    List<Integer> correctClassesList = new ArrayList<Integer>();
    mFoldSizes = new int[mAmtFolds];
    String winnerHash = Integer.toString(mCfgList.get(0).hashCode());
    int i=0;
    try{
      for(i=0;i<mAmtFolds;i++){//iterating over folds
        FileReader     ciFR;
        BufferedReader ciBR;
          ciFR = new FileReader(iwpPath+"hash:"+winnerHash+"_fold:"+i+".txt");
          ciBR = new BufferedReader(ciFR);

        String currentLine = ciBR.readLine();//skip first line
        int foldSize = 0;
        while(currentLine!=null){//iterating over instances
          currentLine = ciBR.readLine();
          String [] gambiarra1 = currentLine.split(","); //TODO use regex
          String [] gambiarra2 = gambiarra1[1].split(":");
          correctClassesList.add(Integer.valueOf(gambiarra2[0])); //TODO FIXME PLEASE
          foldSize++;
        }
        mFoldSizes[i]=foldSize;
      }
    }catch(Exception e){
      System.out.println("Couldn't find instancewise predictions for final incument on "+i+"-th fold"); //TODO make this more fine grained
      throw e;
    }

    mCorrectClasses = new Integer[correctClassesList.size()];
    correctClassesList.toArray(mCorrectClasses); //TODO optimize this into int array
  }

  public List<Configuration> hillclimb(boolean onlyFullyPredicted) throws Exception{ //Doing it the straightforward way. Gonna try a faster way later, just wanna get this working.

    List<Configuration> configBatch = (ArrayList<Configuration>)((ArrayList<Configuration>) mCfgList).clone(); //shallow copying. Ugly trick but works.

    //Removing configurations not evaluated on all folds
    if(onlyFullyPredicted){
      for(int i = configBatch.size()-1; i>=0 ; i--) if(configBatch.get(i).getAmtFolds()!=mAmtFolds) configBatch.remove(i);
    }

    //Parsing the predictions made by each configuration
    List<EnsembleElement> eeBatch = new ArrayList<EnsembleElement>();
    for (Configuration c: configBatch){
      EnsembleElement ee = new EnsembleElement(c,mAmtInstances,mFoldSizes);
      ee.parseInstancewiseInfo(); //TODO  maybe put a call to this in the EE constructor
      eeBatch.add(ee);
    }

    //Building the ensemble
    List<EnsembleElement> currentPartialEnsemble = new ArrayList<EnsembleElement>();
    int [] partialEnsembleErrorCounts = new int[eeBatch.size()];

    for(int i = 0; !eeBatch.isEmpty() ;i++){//Iterating over available ensemble slots. TODO make the initialization batch flexible.

      int [] modelChoiceErrorCounts = new int[eeBatch.size()];
      for(int j = 0; j<eeBatch.size(); j++){//Iterating over remaining ensembles in the batch
        modelChoiceErrorCounts[j] = evaluateModelChoice(currentPartialEnsemble,eeBatch.get(j));
      }

      int indexWithSmallestError = Util.indexMin(partialEnsembleErrorCounts);
      partialEnsembleErrorCounts[i]= indexWithSmallestError;
      currentPartialEnsemble.add(eeBatch.get(indexWithSmallestError));
      eeBatch.remove(indexWithSmallestError);
    } //TODO have something that evaluates that its likely not climbing anymore to stop earlier

    //Slicing it at best place
    int bestIndex = Util.indexMin(partialEnsembleErrorCounts);
    currentPartialEnsemble = Util.getSlicedList(currentPartialEnsemble,0,bestIndex);

    List<Configuration> rv = new ArrayList<Configuration>();
    for(EnsembleElement ee : currentPartialEnsemble){
      rv.add(ee.getModel());
    }
    return rv;
  }

  private int evaluateModelChoice( List<EnsembleElement> currentPartialEnsemble, EnsembleElement modelChoice){
    currentPartialEnsemble.add(modelChoice);
    int amtErrors = 0;
    for (int i = 0; i < mAmtInstances ; i++){
      if(_majorityVote(i, currentPartialEnsemble) != mCorrectClasses[i]){
        amtErrors++;
      }
    }
    return amtErrors;
  }

  private int _majorityVote(int instanceNum, List<EnsembleElement> currentPartialEnsemble){
    int [] votes = new int [mAmtClasses];
    for (EnsembleElement ee : currentPartialEnsemble){
      votes[ee.getPrediction(instanceNum)]++;
    }

    return Util.indexMax(votes);

    //TODO treat duplicate max indexes differently than returning first one?
  }

  private class EnsembleElement{ //TODO make this separate?

    private double mWeight;
    private Configuration mModel;
    private int [] mPredictions;
    private int [] mfoldSizes;
    private int mAmtInstances;

    public EnsembleElement(Configuration model, int amtInstances, int[] foldSizes, double weight){
      this(model,amtInstances,foldSizes);
      mWeight = weight;
    }

    public EnsembleElement(Configuration model, int amtInstances, int[] foldSizes){
      mModel = model;
      mFoldSizes = foldSizes;
      mAmtInstances = amtInstances;
      mPredictions = new int[amtInstances];
      mWeight=1;
    }

    public int getPrediction(int instanceNum){
      return mPredictions[instanceNum];
    }
    public Configuration getModel(){
      return mModel;
    }

    public void parseInstancewiseInfo() throws Exception{ //TODO get instancewiseLogPath from global, maybe have foldSizes globally too
      //TODO make a custom Exception
      String iwpPath = instancewiseInfoDirPath; // aliasing for readability

      int totalInstanceIndex=0;
      for(int i = 0; i<mFoldSizes.length;i++){ //iterating over instancewise logs for each fold
        File ciFile = new File(iwpPath+"hash:"+mModel.hashCode()+"_fold:"+i+".txt");

        if(ciFile.exists()){
          FileReader ciFR;
          BufferedReader ciBR;
          try{
            ciFR = new FileReader(ciFile);
            ciBR = new BufferedReader(ciFR); //hue
            ciBR.readLine(); //skipping first line of csv file
          }catch (Exception e){
            System.out.println("Couldn't initialize ciBR");
            throw e;
          }

          for(int j=0;j<mFoldSizes[i];j++,totalInstanceIndex++){ //iterating over lines
            String instanceLine;
            if(ciBR==null) throw new RuntimeException();//TODO fix this trick
            try{
              instanceLine = ciBR.readLine();
            }catch (Exception e){
              System.out.println("couldn't read the line"); //TODO use loggers etc
              throw e;
            }
            String [] gambiarraTemp = instanceLine.split(",");
            String [] gambiarra = gambiarraTemp[2].split(":");
            mPredictions[totalInstanceIndex]=Integer.parseInt(gambiarra[0]);
          }

        }else{
          for(int j=0;j<mFoldSizes[i];j++,totalInstanceIndex++){ //TODO define standard behaviour for this case
              mPredictions[totalInstanceIndex]=-1;
          }
        }
      }
    }//method

  }//EE class

}
