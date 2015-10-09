package autoweka.randomsearch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import autoweka.ClassParams;
import autoweka.Experiment;
import autoweka.InstanceGenerator;
import autoweka.Parameter;
import autoweka.SubProcessWrapper;
import autoweka.Util;

class RandomSearchWorker
{
    String mSeed;
    Random mRand;
    float mTimeRemaining;
    Experiment mExperiment;
    ClassParams mParams;
    File mExperimentDir;
    List<String> mInstances;

    public static void main(String[] args){
        RandomSearchWorker worker = new RandomSearchWorker(new File(args[0]).getAbsoluteFile().getParentFile(), Experiment.fromXML(args[0]), args[1]);
        worker.run();
    }

    public RandomSearchWorker(File experimentDir, Experiment experiment, String seed)
    {
        mSeed = seed;
        mRand = new Random(Integer.parseInt(seed));

        mInstances = new ArrayList<String>();
        for(String s: InstanceGenerator.create(experiment.instanceGenerator, "__dummy__").getAllInstanceStrings(experiment.instanceGeneratorArgs)){
            mInstances.add(s);
        }
        mInstances.add("default");

        mExperiment = experiment;
        mExperimentDir = experimentDir;
        mTimeRemaining = experiment.tunerTimeout;

        mParams = new ClassParams(experimentDir.getAbsolutePath() + File.separator + "autoweka.params");
    }

    public void run()
    {
        while(mTimeRemaining > 0)
        {
            evaluatePoint();
        }
    }

    private void evaluatePoint(){
        boolean resultExists = true;
        String argString = null;
        RandomSearchResult res = null;
        while(resultExists){
            Map<String, String> argMap = new HashMap<String, String>();        
            
            for(Parameter param : mParams.getParameters()){
                argMap.put(param.name, param.getRandomValue(mRand));
            }
            argString = Util.argMapToString(mParams.filterParams(argMap));

            //Make the output result
            res = new RandomSearchResult(argString);
            resultExists = res.resultExists(mExperimentDir);
        }

        res.touchResultFile(mExperimentDir);
        System.out.println("Evaluating point with hash '" + res.argHash + "'");

        for(String instance : mInstances){
            SubProcessWrapper.ErrorAndTime errAndTime = SubProcessWrapper.getErrorAndTime(mExperimentDir, mExperiment, instance, argString, mSeed);
            res.addInstanceResult(instance, errAndTime);
            mTimeRemaining -= errAndTime.time;
            System.out.println("Spent " + errAndTime.time + " getting a response of " + errAndTime.error);
        }

        res.saveResultFile(mExperimentDir);
    }
}
