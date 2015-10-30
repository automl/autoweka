package autoweka;

import weka.core.Instances;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ListExperimentConstructor 
{
    final static Logger log = LoggerFactory.getLogger(ListExperimentConstructor.class);

    //TODO: This should really become an option
    protected String mParamBaseDir = "./params";

    protected ArrayList<ClassParams> mClassifierParams = new ArrayList<ClassParams>();
    protected ArrayList<Experiment.TrajectoryPointExtra> mTrajectoryPointExtras = new ArrayList<Experiment.TrajectoryPointExtra>();
    protected boolean mIncludeBase = true;
    protected boolean mIncludeMeta = true;
    protected boolean mIncludeEnsemble = true;

    protected String mExperimentPath = null;

    protected ListExperiment mExperiment = null;
    protected Properties mProperties = null;

    protected InstanceGenerator mInstanceGenerator = null;

    public static void main(String[] args)
    {
        //Is the first argument a -batch? If it is, then we need to load the given xml files and use those to generate things
        if(args[0].equals("-batch") || new File(args[0]).isFile())
        {
            for(int i = 0; i < args.length; i++)
            {
                if(!args[i].startsWith("-"))
                    generateBatches(args[i]);
            }
        }
        else
        {
            LinkedList<String> argList = new LinkedList<String>(Arrays.asList(args));
            String constructorName = argList.poll();
            ListExperiment exp = new ListExperiment();
            XmlSerializable.populateObjectFromCMDParams(exp, argList);
            buildSingle(constructorName, exp, argList);
        }
    }

    public static void generateBatches(String xmlFileName)
    {
        try
        {
            ListExperimentBatch batch = ListExperimentBatch.fromXML(xmlFileName);

            for(ListExperimentBatch.ListExperimentComponent expComp: batch.mExperiments)
            {
                //For each dataset component
                for(ExperimentBatch.DatasetComponent datasetComp: batch.mInstances)
                {
                    ListExperiment exp = ListExperimentBatch.createListExperiment(expComp, datasetComp);

                    //Use the extra args from the expComp to give the runner
                    buildSingle(expComp.constructor, exp, expComp.constructorArgs);
                }
            }
        }
        catch(Exception e)
        {
            log.error(e.getMessage(), e);
            throw new RuntimeException("Failed to create batch for " + xmlFileName);
        }

    }

    public static void buildSingle(String builderClassName, ListExperiment exp, List<String> args)
    {
        exp.validate();

        //The first parameter contains the full class of the experiment constructor
        log.info("Making Experiment {}", exp.name);
        Class<?> cls;
        ListExperimentConstructor builder;
        try
        {
            cls = Class.forName(builderClassName);
            builder = (ListExperimentConstructor)cls.newInstance();
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Could not find class '" + builderClassName + "': " + e.getMessage(), e);
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to instantiate '" + builderClassName + "': " + e.getMessage(), e);
        }

        builder.run(exp, new LinkedList<String>(args));
    }

    private void run(ListExperiment exp, List<String> args)
    {
        mExperiment = exp;
        Queue<String> argQueue = new LinkedList<String>(args);
        while(!args.isEmpty())
        {
            String arg = argQueue.poll();
            if(arg.equals("-nometa"))
                mIncludeMeta = false;
            else if (arg.equals("-noensemble"))
                mIncludeEnsemble = false;
            else if (arg.equals("-experimentpath"))
                mExperimentPath = argQueue.poll();
            else
                processArg(arg, argQueue);
        }

        if(mExperimentPath == null){
            throw new RuntimeException("No experiment path set");
        }
        if(exp.resultMetric == null){
            throw new RuntimeException("No Result Metric defined");
        }

        //Create an instance of the instance generator
        mInstanceGenerator = InstanceGenerator.create(mExperiment.instanceGenerator, mExperiment.datasetString);

        //Load up all the classifiers on our dataset that we can
        loadClassifiers();

        //Make sure that the folder for this experiment exists
        Util.makePath(mExperimentPath + File.separator + mExperiment.name);

        //Populate the experiment object
        mExperiment.argStrings = new ArrayList<String>();

        //Ask the sub class to go do stuff
        addArgStrings();

        /*
        for(String cls : mClassifierNames){
            mExperiment.argStrings.add("-targetclass " + cls);
        }*/

        mExperiment.trajectoryPointExtras = mTrajectoryPointExtras;
        mExperiment.toXML(mExperimentPath + File.separator + mExperiment.name + File.separator + mExperiment.name + ".listexperiment");
    }

    protected void processArg(String arg, Queue<String> args)
    {
        //Don't do anything with it
    }

    protected abstract void addArgStrings();

    protected void loadClassifiers()
    {
        Instances instances = mInstanceGenerator.getTraining();

        ApplicabilityTester.ApplicableClassifiers app = ApplicabilityTester.getApplicableClassifiers(instances, mParamBaseDir, null);

        //First, process the base classifiers
        if(mIncludeBase) {
            mClassifierParams.addAll(app.base);
        }

        //Next, process the meta classifiers
        if(mIncludeMeta) {
            mClassifierParams.addAll(app.meta);
        }

        //And do all the ensemble methods
        if(mIncludeEnsemble) {
            mClassifierParams.addAll(app.ensemble);
        }
    }
};

