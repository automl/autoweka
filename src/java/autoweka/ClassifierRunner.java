package autoweka;

import weka.classifiers.Evaluation;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.evaluation.output.prediction.CSV;
import weka.core.Instances;
import weka.core.Instance;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static weka.classifiers.meta.AutoWEKAClassifier.configurationRankingPath;
import static weka.classifiers.meta.AutoWEKAClassifier.configurationInfoDirPath;
import static weka.classifiers.meta.AutoWEKAClassifier.configurationHashSetPath;

/**
 * Class that is responsible for actually running a WEKA classifier from start to finish using the Auto-WEKA argument format.
 *
 * Note that this class can leak memory epically bad if it 'terminates' the classifier, so this should always be called in
 * a sub process from your main work to prevent memouts
 */
public class ClassifierRunner
{
    final Logger log = LoggerFactory.getLogger(ClassifierRunner.class);

    //private ParameterRegularizer mRegularizer = null;
    private InstanceGenerator mInstanceGenerator = null;
    private boolean mTestOnly = false;
    private boolean mDisableOutput = false;
    private java.io.PrintStream mSavedOutput = null;
    private String mPredictionsFileName = null;
	private String mIndividualResultsFileName;

    /**
     * Prepares a runner with the specified properties.
     *
     * Importantly, you must define 'instanceGenerator' and 'datasetString', while optional properties are 'verbose', 'onlyTest' and 'disableOutput'
     */
    public ClassifierRunner(Properties props)
    {
        //Get the instnace generator
        mInstanceGenerator = InstanceGenerator.create(props.getProperty("instanceGenerator"), props.getProperty("datasetString"));
        //Get the regularizer - experimental to the point of not working
        //mRegularizer = ParameterRegularizer.create(props.getProperty("regularizer"), props.getProperty("regularizerParameterFileName"), props.getProperty("regularizerParams"));

        mTestOnly = Boolean.valueOf(props.getProperty("onlyTest", "false"));
        mDisableOutput = Boolean.valueOf(props.getProperty("disableOutput", "false"));
        mPredictionsFileName = props.getProperty("predictionsFileName", null);
        mIndividualResultsFileName = props.getProperty("individualResultsFile", "individual-results.tsv");
    }

    /**
     * Kind of a hack, since this lets us look at what instances we should be running
     */
    public InstanceGenerator getInstanceGenerator(){
        return mInstanceGenerator;
    }

    /** Wrapper method on the runner thread so we can be doubly sure we terminate when we should */
    private class RunnerThread extends WorkerThread
    {
        private String instanceStr;
        private String resultMetric;
        private float timeout;
        private String mSeed;
        private List<String> args;
        public ClassifierResult result;

        public RunnerThread(String _instanceStr, String _resultMetric, float _timeout, String _mSeed, List<String> _args)
        {
            instanceStr = _instanceStr;
            resultMetric = _resultMetric;
            timeout = _timeout;
            mSeed = _mSeed;
            args = _args;
        }
        protected void doWork() throws Exception
        {
            result = _run(instanceStr, resultMetric, timeout, mSeed, args);
        }

        protected String getOpName()
        {
            return "Main Thread";
        }
    }

    /**
     * Public interface to running a classifier specified in the Auto-WEKA format of arguments to generate a classifier result
     */
    public ClassifierResult run(String instanceStr, String resultMetric, float timeout, String mSeed, List<String> args)
    {
        java.io.PrintStream stderr = System.err;
        System.setErr(System.out);

        RunnerThread runner = new RunnerThread(instanceStr, resultMetric, timeout, mSeed, args);
        float time = runner.runWorker(timeout * 2.05f);
        System.setErr(stderr);
        if(runner.getException() != null)
            throw (RuntimeException)runner.getException();
        if(runner.terminated())
        {
            ClassifierResult res = new ClassifierResult(resultMetric);
            res.setTrainingTime(time);
        }

        return runner.result;
    }

    /**
     * Have a pre-trained classifier and want to get another set of testing data out of it? Use this
     */
    public ClassifierResult evaluateClassifierOnTesting(AbstractClassifier classifier, String instanceStr, String resultMetric, float evaluateClassifierOnInstances)
    {
        ClassifierResult res = new ClassifierResult(resultMetric);
        res.setClassifier(classifier);
        Instances instances = mInstanceGenerator.getTestingFromParams(instanceStr);
        _evaluateClassifierOnInstances(classifier, res, instances, evaluateClassifierOnInstances,null,null);

        return res;
    }

    /**
     * Do the actual run of a classifier for AS, Training and Test
     */
    private ClassifierResult _run(String instanceStr, String resultMetric, float timeout, String mSeed, List<String> args)
    {

        //The first arg contains stuff we need to pass to the instance generator
        Instances training = mInstanceGenerator.getTrainingFromParams(instanceStr);
        Instances testing  = mInstanceGenerator.getTestingFromParams(instanceStr);


        //Next, start into the arguments that are for the actual classifier
        WekaArgumentConverter.Arguments wekaArgs = WekaArgumentConverter.convert(args);
        Map<String, String> propertyMap = wekaArgs.propertyMap;
        Map<String, List<String>> argMap = wekaArgs.argMap;

        //Build a result with the appropriate fields
        ClassifierResult res = new ClassifierResult(resultMetric);

        //See if we should do some attribute searching
        String attribSearchClassName = propertyMap.get("attributesearch");
        String attribEvalClassName = propertyMap.get("attributeeval");
        String attribTime = propertyMap.get("attributetime");
        if(!mTestOnly && ((attribSearchClassName != null && !attribSearchClassName.equals("NONE")) || (attribEvalClassName != null /*&& !attribEvalClassName.equals("NONE")*/) )){
            //Make sure that we have everything we need
            if(attribSearchClassName == null)
                throw new RuntimeException("Missing attribute search class name");
            if(attribEvalClassName == null)
                throw new RuntimeException("Missing attribute eval class name");
            if(attribTime == null)
                throw new RuntimeException("Missing the attribute evaluation time param");

            float attribTimeout = Float.parseFloat(attribTime);

            ASEvaluation asEval = null;
            ASSearch     asSearch = null;

            try{
                asEval   = ASEvaluation.forName(attribEvalClassName, argMap.get("attributeeval").toArray(new String[0]));
            }catch(Exception e){
                throw new RuntimeException("Failed to create ASEvaluation " + attribEvalClassName + ": " + e.getMessage(), e);
            }
            try{
                asSearch = ASSearch.forName(attribSearchClassName, argMap.get("attributesearch").toArray(new String[0]));
            }catch(Exception e){
                throw new RuntimeException("Failed to create ASSearch " + attribSearchClassName + ": " + e.getMessage(), e);
            }

            //Build ourselves a selector
            AttributeSelection attribSelect = new AttributeSelection();
            attribSelect.setEvaluator(asEval);
            attribSelect.setSearch(asSearch);

            AttributeSelectorThread asThread = new AttributeSelectorThread(attribSelect, training);

            disableOutput();
            float asTime = asThread.runWorker(attribTimeout);
            enableOutput();
            res.setAttributeSelectionTime(asTime);

            //If we had to stop/got an exception, we need to report a false run
            if(asThread.getException() != null || asThread.terminated())
            {
                if(asThread.getException() != null)
                {
                    res.setMemOut(asThread.getException().getCause() instanceof OutOfMemoryError);
                    log.warn("Attribute selection (search {} {}, eval {} {}) failed: {}", attribSearchClassName, argMap.get("attributeeval"), attribEvalClassName, argMap.get("attributesearch"), asThread.getException().getMessage(), asThread.getException());
                }

                asThread = null;
                res.setCompleted(false);
                return res;
            }
            else
            {
                res.setAttributeSelection(attribSelect);
                try
                {
                    //Filter the instances
                    int[] attrs = attribSelect.selectedAttributes();
                    log.debug("Using {}% attributes:", (100.0*(attrs.length) / training.numAttributes()));
                    for(int i = 0; i < attrs.length; i++){
                        log.debug("{}", i);
                    }
                    training = attribSelect.reduceDimensionality(training);
                    testing = attribSelect.reduceDimensionality(testing);
                    log.debug("Target class: {}", training.classIndex());
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
            }
        }

        //Now work on the actual classifier
        String targetClassifierName = propertyMap.get("targetclass");

        if(targetClassifierName == null || targetClassifierName.isEmpty())
        {
            throw new RuntimeException("No target classifier name specified!");
        }

        //Compute the regularization penalty
        float regPenalty = 0;
        res.setRegularizationPenalty(regPenalty);
        /*if(mRegularizer != null) {
            regPenalty = mRegularizer.getRegularizationPenalty(args);
        }*/

        //Get one of these classifiers
        String[] argsArray = argMap.get("classifier").toArray(new String[0]);
        String[] argsArraySaved = argMap.get("classifier").toArray(new String[0]);
        AbstractClassifier classifier;
        try
        {
            classifier = (AbstractClassifier) AbstractClassifier.forName(targetClassifierName, argsArray);
            res.setClassifier(classifier);
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Could not find class '" + targetClassifierName + "': " + e.getMessage(), e);
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to instantiate '" + targetClassifierName + "' with options " + Arrays.toString(argsArraySaved) + ": " + e.getMessage(), e);
        }
        /*
        try
        {
            classifier = (AbstractClassifier)weka.core.Utils.forName(AbstractClassifier.class, targetClassifierName, argsArray);
            res.setClassifier(classifier);
        }catch(Exception e){
            throw new RuntimeException(e);
        }*/

        //Are we just doing a test run?
        if(mTestOnly)
        {
            res.setCompleted(true);
            return res;
        }

        //Prepare to train the critter
        BuilderThread builderThread = new BuilderThread(classifier, training);

        disableOutput();
        float trainingTime = builderThread.runWorker(timeout);
        enableOutput();

        res.setTrainingTime(trainingTime);

        if(builderThread.getException() != null)
        {
            log.warn("Training classifier ({} {}) failed: {}", targetClassifierName, argsArraySaved, builderThread.getException().getMessage(), builderThread.getException());
            res.setMemOut(builderThread.getException().getCause() instanceof OutOfMemoryError);
        }

        //If we had to stop/got an exception, we need to report a false run
        if(builderThread.getException() != null || builderThread.terminated())
        {
            builderThread = null;
            res.setCompleted(false);
            return res;
        }
        else
        {
            //We have a good result so far
            res.setCompleted(true);
        }

        log.debug("Performing evaluation on {} instances.", testing.numInstances());

        //Get the evaluation
        if(!_evaluateClassifierOnInstances(classifier, res, testing, timeout,args,instanceStr)){
          return res;
        }

        // write out configuration info
        log.info("{};{};{};{};{};{};{};{}",
        targetClassifierName, argsArraySaved,
        attribEvalClassName, argMap.get("attributeeval"),
        attribSearchClassName, argMap.get("attributesearch"),
        instanceStr, res.getRawScore());
        
        try {
        	FileWriter writer = new FileWriter(mIndividualResultsFileName, true);
        	StringBuilder builder = new StringBuilder();
        	String delim = "\t";

        	List<String> attributeEvalArgs = argMap.get("attributeeval");
			String strAttributeEvalArgs = attributeEvalArgs != null ? Util.joinStrings(" ",  attributeEvalArgs) : "";
			
			List<String> attributeSearchArgs = argMap.get("attributesearch");
			String strAttributeSearchArgs = attributeSearchArgs != null ? Util.joinStrings(" ",  attributeSearchArgs) : "";
			
			builder
	        	.append(targetClassifierName).append(delim)
	        	.append(Util.joinStrings(" ",  argsArraySaved)).append(delim)
	        	.append(attribEvalClassName).append(delim)
	        	.append(strAttributeEvalArgs).append(delim)
	        	.append(attribSearchClassName).append(delim)
	        	.append(strAttributeSearchArgs).append(delim)
	        	.append(instanceStr).append(delim)
	        	.append(res.getRawScore()).append(delim)
	        	.append("\n");
        	writer.write(builder.toString());
        	writer.flush();
        	writer.close();
        }
        catch(IOException e) {
        	log.error(e.toString());
        }

        log.debug("Num Training: {}, num testing: {}", training.numInstances(), testing.numInstances());
        return res;
    }

    /**
     * Internal method that performs the evaluation of a classifier on a bunch of instances
     *
     * If true, then the training was good, otherwise it failed
     */
    private boolean _evaluateClassifierOnInstances(AbstractClassifier classifier, ClassifierResult res, Instances instances, float timeout,List<String> args,String instanceStr)
    {

        Evaluation eval = null;
        try
        {
            eval = new Evaluation(instances);
            EvaluatorThread evalThread = new EvaluatorThread(eval, classifier, instances, mPredictionsFileName);

            disableOutput();
            float evalTime = evalThread.runWorker(timeout);
            enableOutput();
            res.setEvaluationTime(evalTime);

            if(evalThread.getException() != null) {
                throw evalThread.getException();
            }

            log.debug("Completed evaluation on {}/{} instances.", (instances.numInstances() - eval.unclassified()), instances.numInstances());

            //Make sure that if we terminated the eval, we crap out accordingly
            res.setCompleted(!evalThread.terminated());

            res.setPercentEvaluated(100.0f*(float)(1.0f - eval.unclassified() / instances.numInstances()));
            log.debug("Percent evaluated: {}", res.getPercentEvaluated());
            //Check to make sure we evaluated enough data (and if we should log it)
            if(res.getPercentEvaluated() < 100)
            {
                res.setCompleted(false);
                log.debug("Evaluated less than 100% of the data.");
            }
            else if(!evalThread.terminated())
            {
                //We're good, we can safely report this value
                res.setScoreFromEval(eval, instances);
                saveConfiguration(res,args,instanceStr);
            }
        } catch(Exception e) {
            log.debug("Evaluating classifier failed: {}", e.getMessage(), e);
            res.setCompleted(false);
            res.setMemOut(e.getCause() instanceof OutOfMemoryError);
            return false;
        }
        log.trace(eval.toSummaryString("\nResults\n======\n", false));
        try
        {
            log.trace(eval.toMatrixString());
        }catch(Exception e)
        {
            //throw new RuntimeException("Failed to get confusion matrix", e);
        }
        log.debug(res.getDescription());

        return true;
    }

    protected void saveConfiguration(ClassifierResult res,List<String> args, String instanceStr){
      //Checking if we're doing this logging for this run of autoweka
      File sortedLog = new File(configurationRankingPath);
      if (!sortedLog.exists()){
        return;
      }

      //Setting up some basic stuff
      Configuration ciConfig = new Configuration(args);
      int ciHash             = ciConfig.hashCode();
      String ciFilename      = configurationInfoDirPath+ciHash+".xml";
      File ciFile            = new File(ciFilename);
      String configIndex     = configurationHashSetPath;

      //Computing Score and fold ID
      Properties pInstanceString = Util.parsePropertyString(instanceStr);
      int ciFold     = Integer.parseInt(pInstanceString.getProperty("fold", "-1"));
      double ciScore = res.getScore();

      //Updating the configuration data
      ciConfig.setEvaluationValues(ciScore,ciFold);

      if (ciFile.exists()){
        Configuration ciConfigFull = Configuration.fromXML(ciFilename,Configuration.class); //Find a faster way w/o IOs?
        ciConfigFull.mergeWith(ciConfig);
        ciConfigFull.toXML(ciFilename);
      }else{
        Util.initializeFile(ciFilename);
        ciConfig.toXML(ciFilename);
      }

      //Updating the configuration list
      try{
          BufferedWriter fp = new BufferedWriter(new FileWriter(configurationHashSetPath,true));//true for appending
          fp.write(ciHash+",");
          fp.flush();
          fp.close();
      }catch(IOException e){
          throw new RuntimeException("Couldn't write to configIndex");
      }

    }


    protected void disableOutput()
    {
        if(!mDisableOutput) return;
        mSavedOutput = System.out;
        System.setOut(new Util.NullPrintStream());
    }

    protected void enableOutput()
    {
        if(!mDisableOutput) return;
        System.setOut(mSavedOutput);
    }

    class BuilderThread extends WorkerThread
    {
        private AbstractClassifier mClassifier;
        private Instances mTrainInstances;

        public BuilderThread(AbstractClassifier cls, Instances inst)
        {
            mClassifier = cls;
            mTrainInstances = inst;
        }

        protected void doWork() throws Exception
        {
            mClassifier.buildClassifier(mTrainInstances);
        }

        protected String getOpName()
        {
            return "Training of classifier";
        }
    }

    public static class EvaluatorThread extends WorkerThread
    {
        private AbstractClassifier mClassifier;
        private Instances mInstances;
        private Evaluation mEval;
        private String mPredictionsFile;

        public EvaluatorThread(Evaluation ev, AbstractClassifier cls, Instances inst)
        {
            this(ev, cls, inst, null);
        }

        public EvaluatorThread(Evaluation ev, AbstractClassifier cls, Instances inst, String predictionsFile)
        {
            mEval = ev;
            mClassifier = cls;
            mInstances = inst;
            mPredictionsFile = predictionsFile;
        }
        protected void doWork() throws Exception
        {
            CSV out = null;
            StringBuffer buffer = null;
            if(mPredictionsFile != null){
                out = new CSV();
                buffer = new StringBuffer();
                out.setBuffer(buffer);
                out.setHeader(mInstances);
                out.setOutputDistribution(true);
                out.printHeader();
                mEval.evaluateModel(mClassifier, mInstances, out);
                out.printFooter();
                try{
                    BufferedWriter fp = new BufferedWriter(new FileWriter(mPredictionsFile));
                    fp.write(buffer.toString());
                    fp.flush();
                    fp.close();
                }catch(IOException e){
                    throw new RuntimeException(e);
                }
            } else {
                for (Instance instance : mInstances) {
                    mEval.evaluateModelOnceAndRecordPrediction(mClassifier, instance);
                }
            }
        }

        protected String getOpName()
        {
            return "Evaluation of classifier";
        }
    }

    class AttributeSelectorThread extends WorkerThread
    {
        private AttributeSelection mSelection;
        private Instances mInstances;

        public AttributeSelectorThread(AttributeSelection selection, Instances inst)
        {
            mInstances = inst;
            mSelection = selection;
        }

        protected void doWork() throws Exception
        {
            //Go do some training
            mSelection.SelectAttributes(mInstances);
        }

        protected String getOpName()
        {
            return "Attribute selection";
        }
    }
}
