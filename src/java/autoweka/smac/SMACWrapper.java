package autoweka.smac;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.Properties;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import autoweka.Wrapper;
import autoweka.ClassifierResult;
import autoweka.Util;

import autoweka.Configuration;
import autoweka.ConfigurationCollection;
import autoweka.ConfigurationRanker;

public class SMACWrapper extends Wrapper
{
    private boolean mRawEval = false;

    public static void main(String[] args)
    {
        SMACWrapper wrapper = new SMACWrapper();
        try {
            wrapper.run(args);
        } catch(Exception e) {
            System.exit(1);
        }
    }

    @Override
    protected void _processWrapperParameterStart(Queue<String> args)
    {
        //First argument is the instance file - smac adds a path here
        mInstance = args.poll();
        //The instance info... ignore
        args.poll();
        //The timeout
        mTimeout = Float.parseFloat(args.poll());
        //The cutoff length... ignore
        args.poll();
        // some random seed -- ignore
        args.poll();
    }

    @Override
    protected void _processParameter(String arg, Queue<String> args)
    {
        if(arg.equals("-raw")){
            mRawEval = true;
        }
    }

    @Override
    protected void _processResults(ClassifierResult res)
    {
        //Get the score
        double score = res.getScore();
        if(mRawEval)
        {
            score = res.getRawScore();
        }

        //Did we complete?
        String resultStr = "SAT";
        if(!res.getCompleted())
        {
            resultStr = "TIMEOUT";
        }

        StringBuilder extraResultsSB = new StringBuilder();
        int i = 0;
        while(mProperties.containsKey("extraRun" + i))
        {
            //Run this instance
            ClassifierResult evalRes = mRunner.evaluateClassifierOnTesting(res.getClassifier(), mProperties.getProperty("extraRun" + i), mResultMetric, mTimeout);
            extraResultsSB.append("(");
            extraResultsSB.append(evalRes.getEvaluationTime());
            extraResultsSB.append(" ");
            extraResultsSB.append(evalRes.getRawScore());
            extraResultsSB.append(") ");
            i++;
        }
        //We need to add the norm penalty
        if(mRawEval)
        {
            extraResultsSB.append("[");
            extraResultsSB.append(res.getNormalizationPenalty());
            extraResultsSB.append("] ");
        }
        if(res.getMemOut()){
            extraResultsSB.append("MEMOUT ");
        }

        extraResultsSB.append(res.getPercentEvaluated());

        //Print the result string
        System.out.println("Result for ParamILS: " + resultStr + ", " + res.getTime() + ", 0, " + score + ", " + mExperimentSeed + ", EXTRA " + extraResultsSB.toString());
        System.exit(0);
    }

    @Override //I've made this specific version of the method for SMACWrapper in order to not break anything.
    //@TODO check if integrating the new part in a conditional on the original method would break stuff.
    //The main issue is that the original method's signature would have to be changed to the one below.
    protected void _processResults(ClassifierResult res, ArrayList<String> wrapperArgs,String instanceString)
    {
        //Get the score
        double score = res.getScore();
        if(mRawEval)
        {
            score = res.getRawScore();
        }

        //Did we complete?
        String resultStr = "SAT";
        if(!res.getCompleted())
        {
            resultStr = "TIMEOUT";
        }

        StringBuilder extraResultsSB = new StringBuilder();
        int i = 0;
        while(mProperties.containsKey("extraRun" + i))
        {
            //Run this instance
            ClassifierResult evalRes = mRunner.evaluateClassifierOnTesting(res.getClassifier(), mProperties.getProperty("extraRun" + i), mResultMetric, mTimeout);
            extraResultsSB.append("(");
            extraResultsSB.append(evalRes.getEvaluationTime());
            extraResultsSB.append(" ");
            extraResultsSB.append(evalRes.getRawScore());
            extraResultsSB.append(") ");
            i++;
        }
        //We need to add the norm penalty
        if(mRawEval)
        {
            extraResultsSB.append("[");
            extraResultsSB.append(res.getNormalizationPenalty());
            extraResultsSB.append("] ");
        }
        if(res.getMemOut()){
            extraResultsSB.append("MEMOUT ");
        }
        extraResultsSB.append(res.getPercentEvaluated());

        //System.out.println("SMACWrapper 1");
        //Maybe put this in a separate support method
        String tempConfigLog = "TemporaryConfigurationLog.xml"; //TODO unhardcode this. Maybe have this as an input thing.
        // File tclFile = new File(tempConfigLog);
        // if(!tclFile.exists()){
        //     tclFile.createNewFile();
        // }
        String sortedConfigLog = "SortedConfigurationLog.xml";
        // File sclFile = new File(sortedConfigLog);
        // if(!sclFile.exists()){
        //   sclFile.createNewFile();
        // }

        ConfigurationCollection configurations;

        //System.out.println("SMACWrapper 2");
        //Grab the fold id from mInstance (the Instance String)
        Properties pInstanceString = Util.parsePropertyString(instanceString);
        int currentFold = Integer.parseInt(pInstanceString.getProperty("fold", "-1"));

        //System.out.println("SMACWrapper 3");
        //Instantiate a new Configuration
        Configuration currentConfig = new Configuration(wrapperArgs);
        currentConfig.setEvaluationValues(score,currentFold);

        //System.out.println("SMACWrapper 4");
        //Get the temporary log
        try{
          //  System.out.println("SMACWrapper 4 in try");
            configurations = ConfigurationCollection.fromXML(tempConfigLog,ConfigurationCollection.class);
        }catch(Exception e){
          //  System.out.println("SMACWrapper 4 in catch");
            //This will be the first configuration to be logged.
            ConfigurationRanker.initializeLog(sortedConfigLog);
            ConfigurationRanker.initializeLog(tempConfigLog);
            configurations = new ConfigurationCollection();
        }
        //System.out.println("SMACWrapper 5");
        //Adding the new guy and spiting the updated log out
        configurations.add(currentConfig);
        configurations.toXML(tempConfigLog);


        // File f = new File("setErr.txt");
        // PrintStream testlog=null;
        //
        // if(!f.exists()){
        //   try{
        //     f.createNewFile();
        //   }catch (Exception e){
        //     System.out.println("couldnt create new file");
        //   }
        // }
        //
        // try{
        //   testlog =  new PrintStream( new FileOutputStream(f,true) );
        // }catch(Exception e){
        //   System.out.println("couldnt instantiate printstream");
        // }
        //
        //
        //
        //
        // if (testlog!=null){
        //   testlog.println(".\n.\n.\n.\n.\n.\n.\n.");
        //   testlog.println("--------------This smac run:--------------");
        //   testlog.println("Args:\n"+wrapperArgs.toString());
        //   testlog.println("Fold id:\n"+currentFold);
        //   testlog.println("Score:\n"+score);
        //   testlog.println("--------------End of run--------------");
        // }
        //@TODO Later, check if this became pointless. If yes, just remove it
        System.out.println("Result for ParamILS: " + resultStr + ", " + res.getTime() + ", 0, " + score + ", " + mExperimentSeed + ", EXTRA " + extraResultsSB.toString());
        System.exit(0);
    }

    // protected void _preRun(){
    //   System.out.println("_preRun");
    //   File f = new File("setErr.txt");
    //   try{
    //     f.createNewFile();
    //   }catch(Exception e){
    //     System.out.println("couldnt create file on preRun :(");
    //   }
    //   try{
    //   System.setErr( new PrintStream( new FileOutputStream(f) ) );
    // } catch(Exception e){
    //   System.out.println("couldnt find the file on preRun :(");
    // }
    //   //System.setErr(new File("setErr.txt"));
    // }
}
