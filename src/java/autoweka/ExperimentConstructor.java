package autoweka;

import weka.core.Instances;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.HashMap;

import java.util.ArrayList;

import java.net.URLDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class responsible for generating all the necessary stuff to run an Auto-WEKA Experiment.
 *
 * Although this class is static, you should use it's main method to actually do the experiment construction.
 * In particular, if you run it with cmd line arguments of a number of ExperimentBatch XML files, it will do
 * the appropriate generation for you by calling the right classes
 */
public abstract class ExperimentConstructor
{
    final static Logger log = LoggerFactory.getLogger(ExperimentConstructor.class);

    /**
     * The directory with all the param files in it.
     */
    protected String mParamBaseDir = autoweka.Util.getAutoWekaDistributionPath() + File.separator + "params";

    protected List<ClassParams> mBaseClassParams = new ArrayList<ClassParams>();
    protected List<ClassParams> mMetaClassParams = new ArrayList<ClassParams>();
    protected List<ClassParams> mEnsembleClassParams = new ArrayList<ClassParams>();
    protected List<ClassParams> mAttribSearchClassParams = new ArrayList<ClassParams>();
    protected List<ClassParams> mAttribEvalClassParams = new ArrayList<ClassParams>();
    /**
     * List containing what classifiers are allowed to be used by this experiment
     */
    protected List<String> mAllowedClassifiers = new ArrayList<String>();
    /**
     * How deep should the ensemble tree go?
     */
    protected int mEnsembleMaxNum = 5;
    //These flags sort of have meaning....
    protected boolean mIncludeBase = true;
    protected boolean mIncludeMeta = true;
    protected boolean mIncludeEnsemble = true;

    /**
     * The output path for the experiments
     */
    protected String mExperimentPath = "experiments";

    /**
     * The active Experiment that we're trying to build
     */
    protected Experiment mExperiment = null;

    /**
     * Properties associated with our constructor.
     *
     * These are defined in a file in the CWD that has the form CANONICAL_CLASS_NAME.properties
     */
    protected Properties mProperties = null;

    /**
     * The instance generator we're using, all loaded up and ready to go with the appropriate dataset
     */
    protected InstanceGenerator mInstanceGenerator = null;


    /**
     * Main method that can either take stuff on the command line to build a single experiment, or points to an ExperimentBatch XML file.
     *
     * You can specify all the options that are in an XML file by using the same XML tag name preceeded with two dashes
     *
     * @param args Any arguments.
     */
    public static void main(String[] args)
    {
        if(args.length < 0)
        {
            log.error("Arguments missing. Please refer to manual for help.");
        }
        //Is the first argument a -batch? If it is, then we need to load the given xml files and use those to generate things
        else if(args[0].equals("-batch") || new File(args[0]).isFile())
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
            Experiment exp = new Experiment();
            XmlSerializable.populateObjectFromCMDParams(exp, argList);
            buildSingle(constructorName, exp, argList);
        }
    }

    public static void generateBatches(String xmlFile)
    {
        try
        {
            ExperimentBatch batch = ExperimentBatch.fromXML(xmlFile);


            //For each type of experiment component
            for(ExperimentBatch.ExperimentComponent expComp: batch.mExperiments)
            {
                //For each dataset component
                for(ExperimentBatch.DatasetComponent datasetComp: batch.mDatasets)
                {
                    Experiment exp = ExperimentBatch.createExperiment(expComp, datasetComp);

                    //Use the extra args from the expComp to give the runner
                    buildSingle(expComp.constructor, exp, new LinkedList<String>(expComp.constructorArgs));
                }
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to create batch for " + xmlFile, e);
        }
    }

    public static void buildSingle(String builderClassName, Experiment exp, List<String> args)
    {
        exp.validate();

        //The first parameter contains the full class of the experiment constructor
        log.debug("Making Experiment {}", exp.name);
        Class<?> cls;
        ExperimentConstructor builder;
        try
        {
            cls = Class.forName(builderClassName);
            builder = (ExperimentConstructor)cls.newInstance();
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

    private void run(Experiment exp, List<String> args)
    {
        //See if we can load up this constructor's canonical class name's props file
        mProperties = new Properties();
        String propsFilePath = Util.getAutoWekaDistributionPath() + File.separator + this.getClass().getCanonicalName() + ".properties";
        try
        {
            mProperties.load(new java.io.FileInputStream(new java.io.File(propsFilePath)));
        }
        catch(java.io.FileNotFoundException e)
        {
            log.warn("No property file {}.properties found", propsFilePath);
        }
        catch(java.io.IOException e)
        {
            log.error(e.getMessage(), e);
        }

        if(exp.resultMetric == null){
            throw new RuntimeException("No Result Metric defined");
        }

        mExperiment = exp;
        Queue<String> argQueue = new LinkedList<String>(args);
        while(!argQueue.isEmpty())
        {
            String arg = argQueue.poll();
            if(arg.equals("-nometa"))
                mIncludeMeta = false;
            else if (arg.equals("-noensemble"))
                mIncludeEnsemble = false;
            else if (arg.equals("-experimentpath"))
                mExperimentPath = argQueue.poll();
            else if (arg.equals("-propertyoverride"))
                Util.parsePropertyString(mProperties, argQueue.poll());
            else
                processArg(arg, argQueue);
        }

        //Create an instance of the instance generator
        mInstanceGenerator = InstanceGenerator.create(mExperiment.instanceGenerator, mExperiment.datasetString);

        //Load up all the attribute selectors that we can
        if(mExperiment.attributeSelection)
            loadAttributeSelectors();

		mAllowedClassifiers = exp.allowedClassifiers;	
		
        //Load up all the classifiers for the dataset we can
        loadClassifiers();

		//Load all classifiers in mAllowedClassifiers
		if(mAllowedClassifiers.isEmpty()){
            if(mIncludeBase){
                for(ClassParams clsParams: mBaseClassParams){
                    mAllowedClassifiers.add(clsParams.getTargetClass());
                }
            }
            if(mIncludeMeta){
                for(ClassParams clsParams: mMetaClassParams){
                    mAllowedClassifiers.add(clsParams.getTargetClass());
                }
            }
            if(mIncludeEnsemble){
                for(ClassParams clsParams: mEnsembleClassParams){
                    mAllowedClassifiers.add(clsParams.getTargetClass());
                }
            }
		}else
		{
			mAllowedClassifiers.retainAll(exp.allowedClassifiers);
        }

        //Make sure we're conflict free
        checkPrefixes();

        //Make sure that the folder for this experiment exists
        Util.makePath(mExperimentPath + File.separator + mExperiment.name);

        //Generate all the stuff that needs to be created alongside the experiment file
        String absExperimentDir = URLDecoder.decode(new File(mExperimentPath + File.separator + mExperiment.name + File.separator).getAbsolutePath()) + File.separator;
        prepareExperiment(absExperimentDir);

        //Populate the experiment object
        mExperiment.type = getType();
        mExperiment.trajectoryParserClassName = getTrajectoryParserClassName();
        mExperiment.callString = getCallString(absExperimentDir);
        mExperiment.envVariables = getEnvVariables();
        mExperiment.toXML(mExperimentPath + File.separator + mExperiment.name + File.separator + mExperiment.name + ".experiment");
    }

    /*
     * Useful for subclasses so that they can get some of the handy properties that they'll need to pass to their wrappers
     */
    protected String getWrapperPropString()
    {
        Properties props = new Properties();
        props.setProperty("datasetString", mExperiment.datasetString);
        props.setProperty("instanceGenerator", mExperiment.instanceGenerator);
        props.setProperty("resultMetric", mExperiment.resultMetric);

        /*
        if(mExperiment.regularizer != null)
        {
            sb.append(":regularizer=");
            sb.append(mExperiment.regularizer);

            sb.append(":regularizerParams=");
            if(mExperiment.regularizerArgs != null)
                sb.append(mExperiment.regularizerArgs);
        }*/

        return Util.propertiesToString(props);
    }

    /**
     * Subclasses must provide this method which is responsible for
     * @param path The path to the experiment.
     */
    public abstract void prepareExperiment(String path);

    /**
     * Process a constructor argument, and suck out stuff from the arg queue
     * @param arg The argument.
     * @param args The queue of arguments.
     */
    public void processArg(String arg, Queue<String> args){
        log.warn("Ignoring unknown argument {}", arg);
    }

    /**
     * Get a string indicating the type of this Experiment (namely the name of the SMBO method)
     * @return The type of experiment.
     */
    protected abstract String getType();

    /**
     * Gets the name of the class that is used to parse the results of the SMBO method into a Trajectory
     * @return The name of the trajectory parser class.
     */
    protected abstract String getTrajectoryParserClassName();

    /**
     * Gets the set of strings that are called on the command line to invoke the SMBO method
     * @param experimentPath The path to the experiment.
     * @return The list of call strings.
     */
    protected abstract List<String> getCallString(String experimentPath);

    /**
     * Gets a list of all the environment variables that need to be set for this experiment to run
     * @return The list of environment variables.
     */
    protected List<String> getEnvVariables()
    {
        return Collections.emptyList();
    }

    private void loadAttributeSelectors()
    {
        Instances instances = mInstanceGenerator.getTraining();

        //First, process the evaluation methods
        mAttribEvalClassParams = ApplicabilityTester.getApplicableAttributeEvaluators(instances, mParamBaseDir);

        //Next, grab all the search methods
        mAttribSearchClassParams = ApplicabilityTester.getApplicableAttributeSearchers(instances, mParamBaseDir);

        //if(mAttribEvalClassParams.isEmpty())
            //throw new RuntimeException("Couldn't find any attribute evaluators");
    }

    private void loadClassifiers()
    {
        Instances instances = mInstanceGenerator.getTraining();
        List<String> allowed = null;
        if(mAllowedClassifiers.size() > 0)
            allowed = mAllowedClassifiers;

        ApplicabilityTester.ApplicableClassifiers app = ApplicabilityTester.getApplicableClassifiers(instances, mParamBaseDir, allowed);

        mBaseClassParams = app.base;
        mMetaClassParams = app.meta;
        mEnsembleClassParams = app.ensemble;
    }

    private void checkPrefixes()
    {
        ArrayList<String> prefixes = new ArrayList<String>();

        ArrayList<List<ClassParams>> classParams = new ArrayList<List<ClassParams>>();
        classParams.add(mBaseClassParams);
        classParams.add(mMetaClassParams);
        classParams.add(mEnsembleClassParams);
        classParams.add(mAttribEvalClassParams);
        classParams.add(mAttribSearchClassParams);

        for(List<ClassParams> params: classParams){
            for(ClassParams param: params)
            {
                String prefix = getPrefix(param.getTargetClass());
                if(prefixes.contains(prefix))
                {
                    throw new RuntimeException("Prefix '" + prefix + "' (" + param.getTargetClass() + ") is already in use.");
                }
                prefixes.add(prefix);
            }
        }
    }

    /**
     * Gets a prefix out of the classifier name by stripping all the packages and capital letters - needed to ensure that parameters with the same WEKA name don't collide.
     * @param classifierName The name of the classifier.
     * @return The prefix.
     */
    public String getPrefix(String classifierName)
    {
        return classifierName.replaceAll("\\.", "").toLowerCase();
    }


    /**
     * Populates a ParameterConditionalGroup with all the params/conditionals that are needed for
     * optimization methods that support a DAG structure
     * @return The parameter-conditional group.
     */
    public ParameterConditionalGroup generateAlgorithmParameterConditionalGroupForDAG()
    {
        ParameterConditionalGroup paramGroup = new ParameterConditionalGroup();
        //Add in all our our attribute search methods
        if(mExperiment.attributeSelection)
        {
            if(mAttribEvalClassParams.isEmpty()){
                throw new RuntimeException("Couldn't find any attribute eval methods");
            }
            if(mAttribSearchClassParams.isEmpty()){
                throw new RuntimeException("Couldn't find any attribute search methods");
            }

            List<String> searchParamNames = new ArrayList<String>();
            //Insert NONE for now, we need to take it out in a second.... after we build the param
            searchParamNames.add("NONE");
            for(ClassParams clsParams: mAttribSearchClassParams){
                searchParamNames.add(clsParams.getTargetClass());
            }
            Parameter attributesearch = new Parameter("attributesearch", searchParamNames);
            paramGroup.add(attributesearch);
            //Take out the NONE
            searchParamNames.remove(0);
            for(ClassParams clsParams: mAttribSearchClassParams){
                addClassifierToParameterConditionalGroupForDAG(paramGroup, clsParams, "assearch_", attributesearch);
            }

            //Now we have to build the attribute eval param
            List<String> evalParamNames = new ArrayList<String>();
            for(ClassParams clsParams: mAttribEvalClassParams){
                evalParamNames.add(clsParams.getTargetClass());
            }
            Parameter attributeeval = new Parameter("attributeeval", evalParamNames);
            paramGroup.add(attributeeval);
            for(ClassParams clsParams: mAttribEvalClassParams){
                addClassifierToParameterConditionalGroupForDAG(paramGroup, clsParams, "aseval_", attributeeval);
            }

            paramGroup.add(new Parameter("attributetime", "" + mExperiment.attributeSelectionTimeout));
            paramGroup.add(new Conditional(attributeeval, attributesearch, searchParamNames));
        }

        //First, insert the top level choice about which classifer is going to be selected
        List<String> classifiers = new ArrayList<String>();
        List<String> baseClassifiers = new ArrayList<String>();
        List<String> metaClassifiers = new ArrayList<String>();
        List<String> ensembleClassifiers = new ArrayList<String>();
        //Go build up the names of base methods
        for(ClassParams clsParams: mBaseClassParams) {
            String className = clsParams.getTargetClass();
            classifiers.add(className);
            baseClassifiers.add(className);
        }
        //Go build up the names of meta methods
        if(mIncludeMeta){
            for(ClassParams clsParams: mMetaClassParams) {
                String className = clsParams.getTargetClass();
                classifiers.add(className);
                metaClassifiers.add(className);
            }
        }
        //Go build up the names of ensemble methods
        if(mIncludeEnsemble)
        {
            for(ClassParams clsParams: mEnsembleClassParams) {
                String className = clsParams.getTargetClass();
                classifiers.add(className);
                ensembleClassifiers.add(className);
            }
        }

        //Sanity check - we do have one normal classifier?
        if(baseClassifiers.isEmpty())
        {
            throw new RuntimeException("No Base classifiers could be applied to this data set");
        }

        //Build the entire list of all classifiers as a parameter, and insert it
        Parameter targetclass = new Parameter("targetclass", classifiers, baseClassifiers.get(0));
        paramGroup.add(targetclass);

        //Next, insert all the default parameters for each method (Just the flat level
        if(mIncludeBase){
            for(ClassParams clsParams: mBaseClassParams) {
                addClassifierToParameterConditionalGroupForDAG(paramGroup, clsParams, "_0_", targetclass);
            }
        }
        if(mIncludeMeta){
            for(ClassParams clsParams: mMetaClassParams) {
                addClassifierToParameterConditionalGroupForDAG(paramGroup, clsParams, "_0_", targetclass);
            }
        }
        if(mIncludeEnsemble){
            for(ClassParams clsParams: mEnsembleClassParams) {
                addClassifierToParameterConditionalGroupForDAG(paramGroup, clsParams, "_0_", targetclass);
            }
        }

        //Add in the meta conditionals
        if(!metaClassifiers.isEmpty())
        {
            Parameter _1_W = new Parameter("_1_W", baseClassifiers);
            paramGroup.add(_1_W);
            paramGroup.add(new Conditional(_1_W, targetclass, metaClassifiers));
            Parameter _1_W_0_DASHDASH = new Parameter("_1_W_0_DASHDASH", "REMOVED");
            paramGroup.add(_1_W_0_DASHDASH);
            paramGroup.add(new Conditional(_1_W_0_DASHDASH, targetclass, metaClassifiers));
            for(ClassParams clsParams: mBaseClassParams) {
                addClassifierToParameterConditionalGroupForDAG(paramGroup, clsParams, "_1_W_1_", _1_W);
            }
        }

        //Add in the ensemble conditionals
        if(!ensembleClassifiers.isEmpty())
        {
            //Make the ensemble depth param
            List<String> levels = new ArrayList<String>();
            for(int l = 0; l < mEnsembleMaxNum; l++) {
                levels.add(String.format("%d", l));
            }
            Parameter _HIDDEN_ensemble_depth = new Parameter("_HIDDEN_ensemble_depth", levels);
            paramGroup.add(_HIDDEN_ensemble_depth);
            paramGroup.add(new Conditional(_HIDDEN_ensemble_depth, targetclass, ensembleClassifiers));

            //Now actually insert all the levels
            for(int i = 0; i < mEnsembleMaxNum; i++)
            {
                String prefix = "_1_" + String.format("%02d", i);
                Parameter gateParam = new Parameter(prefix + "_0_QUOTE_START_B", baseClassifiers, baseClassifiers.get(0));
                paramGroup.add(gateParam);
                for(ClassParams clsParams: mBaseClassParams) {
                    addClassifierToParameterConditionalGroupForDAG(paramGroup, clsParams, prefix + "_1_", gateParam);
                }

                //Add this level for everything beneath us
                levels.clear();
                for(int l = i; l < mEnsembleMaxNum; l++) {
                    levels.add(String.format("%d", l));
                }
                paramGroup.add(new Conditional(gateParam, _HIDDEN_ensemble_depth, levels));
                //Make the dummy param to close the quote
                Parameter endQuote = new Parameter("_1_" + String.format("%02d", i) + "_2_QUOTE_END", "REMOVED");
                paramGroup.add(endQuote);
                paramGroup.add(new Conditional(endQuote, _HIDDEN_ensemble_depth, levels));
            }
        }

        return paramGroup;
    }

    /* Internal helper method that adds a child classifier's params assuming a DAG structure */
    private void addClassifierToParameterConditionalGroupForDAG(ParameterConditionalGroup paramGroup, ClassParams clsParams, String prefix, Parameter parent)
    {
        Map<Parameter, Parameter> paramMap = new HashMap<Parameter, Parameter>();

        prefix = prefix + "_" + getPrefix(clsParams.getTargetClass()) + "_";

        int i = 0;
        for(Parameter oldParam : clsParams.getParameters())
        {
            //Add in these parameters - but update a map so we can do the conditionals properly in a sec...
            String tempPrefix = prefix + String.format("%02d", i++) + "_";
            Parameter param = new Parameter(tempPrefix + oldParam.name, oldParam);
            paramMap.put(oldParam, param);
            paramGroup.add(param);

            paramGroup.add(new Conditional(param, parent, clsParams.getTargetClass()));
        }
        for(Conditional cond: clsParams.getConditionals())
        {
            paramGroup.add(new Conditional(paramMap.get(cond.parameter), paramMap.get(cond.parent), cond));
        }
    }
};
