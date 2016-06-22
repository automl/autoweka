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

public class Ensembler{
  private List<EnsembleElement> mCurrentEnsemble;
  private Map<Integer,Integer> mFoldMetadata; //TODO rename to something more clever. TODO discuss if this is needed (i.e. if we will use models with missing predictions)

  private String mExperimentDir;
  private String mConfigurationLoggingDir;
  private String mInstancewiseLoggingDir;
  private int    mfoldAmt;

  public Ensembler(String aExperimentDir){
    mExperimentDir = aExperimentDir;
    mConfigurationLoggingDir = mExperimentDir+"/EnsemblerLogging";
    mInstancewiseLoggingDir  = mConfigurationLoggingDir+"/instancewise";
    mFoldMetadata = new HashMap<Integer,Integer>();
    parseFoldMetadata();
  }

  private void parseFoldMetadata(){ //This might take a while, but is only done once. For reasonably sized runs, this seems to be pretty fast
    String [] files = new File(mInstancewiseLoggingDir).list();
    for (String ciFilename : files){
      String [] gambiarraTemp = ciFilename.split("fold:"); //TODO Use regex instead of this lol
      String [] gambiarra = gambiarraTemp[1].split(".txt");
      int foldNumber = Integer.parseInt(gambiarra[0]);

      if (!mFoldMetadata.containsKey(foldNumber)){
        try{
          int instanceAmt; //TODO optimize this to skipping to eof and parsing last line's 1st number
          FileReader ciFileReader = new FileReader(mInstancewiseLoggingDir+"/"+ciFilename);
          BufferedReader ciBufferedReader = new BufferedReader(ciFileReader);
          for(instanceAmt=0;ciBufferedReader.readLine()!=null;instanceAmt++);
          mFoldMetadata.put(foldNumber,instanceAmt);
        }catch (Exception e){
          log.debug("Couldn't compute instance amount for fold "+foldNumber);
        }
      }
    }
  }

  public void hillclimb(boolean onlyFullyPredicted){ //Doing it the straightforward way. Gonna try a faster way later, just wanna get this working.

    ConfigurationCollection sortedCC = ConfigurationCollection.fromXML(mExperimentDir+"/SortedConfigurationLog.xml",ConfigurationCollection.class);
    List<Configuration> configBatch = sortedCC.asArrayList();

    if(onlyFullyPredicted){
      for(int i = configBatch.size()-1; i>=0 ; i--) if(configBatch.get(i)!=mFoldAmt) configBatch.remove(i);
    }

    List<EnsembleElement> eeBatch = new List<EnsembleElement>();
    for (Configuration c: configBatch){
      EnsembleElement ciEE = new EnsembleElement(c);
      ciEE.parseInstancewiseInfo(mInstancewiseLoggingDir,mFoldMetadata);
      eeBatch.add(ciEE);
    }

    int [] partialEnsembleScores = new int [eeBatch.size()];
    int
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
