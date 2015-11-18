package weka.classifiers.meta;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;

import weka.core.Attribute;
import weka.core.AdditionalMeasureProducer;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.converters.ArffSaver;
import weka.core.Drawable;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Serializable;

import java.nio.file.Files;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import autoweka.Experiment;
import autoweka.ExperimentConstructor;
import autoweka.InstanceGenerator;
import autoweka.instancegenerators.CrossValidation;
import autoweka.Util;
import autoweka.Trajectory;
import autoweka.TrajectoryGroup;
import autoweka.TrajectoryMerger;

import autoweka.tools.GetBestFromTrajectoryGroup;

public class AutoWEKAClassifier extends AbstractClassifier implements AdditionalMeasureProducer {

    /** for serialization */
    static final long serialVersionUID = 2907034203562786373L;

    final Logger log = LoggerFactory.getLogger(AutoWEKAClassifier.class);

    static final int DEFAULT_TIME_LIMIT = 60;

    static enum Resampling {
        CrossValidation,
        MultiLevel,
        RandomSubSampling,
        TerminationHoldout
    }
    static final Resampling DEFAULT_RESAMPLING = Resampling.CrossValidation;

    static final Map<Resampling, String> resamplingArgsMap;
    static {
        resamplingArgsMap = new HashMap<Resampling, String>();
        resamplingArgsMap.put(Resampling.CrossValidation, "numFolds=10");
        resamplingArgsMap.put(Resampling.MultiLevel, "numLevels=2[$]autoweka.instancegenerators.CrossValidation[$]numFolds=10");
        resamplingArgsMap.put(Resampling.RandomSubSampling, "numSamples=10:percent=66");
        resamplingArgsMap.put(Resampling.TerminationHoldout, "terminationPercent=66[$]autoweka.instancegenerators.CrossValidation[$]numFolds=10");
    }
    static final String DEFAULT_RESAMPLING_ARGS = resamplingArgsMap.get(DEFAULT_RESAMPLING);

    static final String DEFAULT_EXTRA_ARGS = "initialIncumbent=RANDOM:acq-func=EI";

    /* The Chosen One. */
    protected Classifier classifier;
    protected AttributeSelection as;

    protected String classifierClass;
    protected String[] classifierArgs;
    protected String attributeSearchClass;
    protected String[] attributeSearchArgs;
    protected String attributeEvalClass;
    protected String[] attributeEvalArgs;

    protected static String msExperimentPath;
    protected static String expName = "Auto-WEKA";

    protected int seed = 123;
    protected int timeLimit = DEFAULT_TIME_LIMIT;
    protected Resampling resampling = DEFAULT_RESAMPLING;
    protected String resamplingArgs = DEFAULT_RESAMPLING_ARGS;
    protected String extraArgs = DEFAULT_EXTRA_ARGS;

    protected double estimatedError = -1;

    /**
     * Main method for testing this class.
     *
     * @param argv should contain command line options (see setOptions)
     */
    public static void main(String[] argv) {
        // this always succeeds...
        runClassifier(new AutoWEKAClassifier(), argv);
    }

    public AutoWEKAClassifier() {
        classifier = null;
        classifierClass = null;
        classifierArgs = null;
        attributeSearchClass = null;
        attributeSearchArgs = new String[0];
        attributeEvalClass = null;
        attributeEvalArgs = new String[0];
    }

    public void buildClassifier(Instances is) throws Exception {
        msExperimentPath = Files.createTempDirectory("autoweka").toString() + File.separator;
        getCapabilities().testWithFail(is);

        //Populate the experiment fields
        Experiment exp = new Experiment();
        exp.name = expName;

        //Which result metric do we use?
        Attribute classAttr = is.classAttribute();
        if(classAttr.isNominal()){
            exp.resultMetric = "errorRate"; 
        } else if(classAttr.isNumeric()) {
            exp.resultMetric = "rmse"; 
        }

        Properties props = Util.parsePropertyString("type=trainTestArff:testArff=__dummy__");
        ArffSaver saver = new ArffSaver();
        saver.setInstances(is);
        File fp = new File(msExperimentPath + expName + File.separator + expName + ".arff");
        saver.setFile(fp);
        saver.writeBatch();
        props.setProperty("trainArff", fp.getAbsolutePath());
        exp.datasetString = Util.propertiesToString(props);
        exp.instanceGenerator = "autoweka.instancegenerators." + String.valueOf(resampling);
        exp.instanceGeneratorArgs = "seed=" + seed + ":" + resamplingArgs;
        exp.attributeSelection = true;

        exp.attributeSelectionTimeout = timeLimit * 1;
        exp.tunerTimeout = timeLimit * 60;
        exp.trainTimeout = timeLimit * 5;

        exp.memory = "500m";
        exp.extraPropsString = extraArgs;

        //Setup all the extra args
        List<String> args = new LinkedList<String>();
        args.add("-experimentpath");
        args.add(msExperimentPath);

        //Make the thing
        ExperimentConstructor.buildSingle("autoweka.smac.SMACExperimentConstructor", exp, args);

        // run experiment
        Thread worker = new Thread(new Runnable() {
            public void run() {
                Process mProc = null;
                try {
                    ProcessBuilder pb = new ProcessBuilder(autoweka.Util.getJavaExecutable(), "-Xmx128m", "-cp", autoweka.Util.getAbsoluteClasspath(), "autoweka.tools.ExperimentRunner", msExperimentPath + expName, "" + seed);
                    pb.redirectErrorStream(true);

                    mProc = pb.start();

                    Thread killerHook = new autoweka.Util.ProcessKillerShutdownHook(mProc);
                    Runtime.getRuntime().addShutdownHook(killerHook);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(mProc.getInputStream()));
                    String line;
                    while((line = reader.readLine()) != null) {
                        Matcher m = Pattern.compile(".*Estimated mean quality of final incumbent config .* on test set: ([0-9.]+).*").matcher(line);
                        if(m.matches()) {
                            estimatedError = Double.parseDouble(m.group(1));
                        }
                        // fix nested logging...
                        if(line.matches(".*DEBUG.*")) {
                            log.debug(line);
                        } else if(line.matches(".*WARN.*")) {
                            log.warn(line);
                        } else if(line.matches(".*ERROR.*")) {
                            log.error(line);
                        } else {
                            log.info(line);
                        }
                        if(Thread.currentThread().isInterrupted()) {
                            mProc.destroy();
                            break;
                        }
                    }
                    Runtime.getRuntime().removeShutdownHook(killerHook);
                } catch (Exception e) {
                    if(mProc != null) mProc.destroy();
                    log.error(e.getMessage(), e);
                }
            } });
        worker.start();
        try {
            worker.join();
        } catch(InterruptedException e) {
            worker.interrupt();
        }

        // get results
        TrajectoryGroup group = TrajectoryMerger.mergeExperimentFolder(msExperimentPath + expName);

        // print trajectory information
        log.debug("Optimization trajectory:");
        for(Trajectory t: group.getTrajectories()) {
            log.debug("{}", t);
        }

        GetBestFromTrajectoryGroup mBest = new GetBestFromTrajectoryGroup(group);
        classifierClass = mBest.classifierClass;
        classifierArgs = Util.splitQuotedString(mBest.classifierArgs).toArray(new String[0]);
        attributeSearchClass = mBest.attributeSearchClass;
        if(mBest.attributeSearchArgs != null) {
            attributeSearchArgs = Util.splitQuotedString(mBest.attributeSearchArgs).toArray(new String[0]);
        }
        attributeEvalClass = mBest.attributeEvalClass;
        if(mBest.attributeEvalArgs != null) {
            attributeEvalArgs = Util.splitQuotedString(mBest.attributeEvalArgs).toArray(new String[0]);
        }

        log.info("classifier: {}, arguments: {}, attribute search: {}, attribute search arguments: {}, attribute evaluation: {}, attribute evaluation arguments: {}",
            classifierClass, classifierArgs, attributeSearchClass, attributeSearchArgs, attributeEvalClass, attributeEvalArgs);

        // train model on entire dataset and save
        as = new AttributeSelection();

        if(attributeSearchClass != null) {
            ASSearch asSearch = ASSearch.forName(attributeSearchClass, attributeSearchArgs.clone());
            as.setSearch(asSearch);
        }
        if(attributeEvalClass != null) {
            ASEvaluation asEval = ASEvaluation.forName(attributeEvalClass, attributeEvalArgs.clone());
            as.setEvaluator(asEval);
        }

        as.SelectAttributes(is);
        is = as.reduceDimensionality(is);

        classifier = AbstractClassifier.forName(classifierClass, classifierArgs.clone());
        classifier.buildClassifier(is);
    }

    public double classifyInstance(Instance i) throws Exception {
        if(classifier == null) {
            throw new Exception("Auto-WEKA has not been run yet to get a model!");
        }
        i = as.reduceDimensionality(i);
        return classifier.classifyInstance(i);
    }

    public double[] distributionForInstance(Instance i) throws Exception {
        if(classifier == null) {
            throw new Exception("Auto-WEKA has not been run yet to get a model!");
        }
        i = as.reduceDimensionality(i);
        return classifier.distributionForInstance(i);
    }

    /**
     * Gets an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    @Override
    public Enumeration<Option> listOptions() {
        Vector<Option> result = new Vector<Option>();
        result.addElement(
            new Option("\tThe seed for the random number generator.\n" + "\t(default: " + seed + ")",
                "seed", 1, "-seed <seed>"));
        result.addElement(
            new Option("\tThe time limit for tuning in minutes (approximately).\n" + "\t(default: " + DEFAULT_TIME_LIMIT + ")",
                "timeLimit", 1, "-timeLimit <limit>"));
        result.addElement(
            new Option("\tThe type of resampling used.\n" + "\t(default: " + String.valueOf(DEFAULT_RESAMPLING) + ")",
                "resampling", 1, "-resampling <resampling>"));
        result.addElement(
            new Option("\tResampling arguments.\n" + "\t(default: " + DEFAULT_RESAMPLING_ARGS + ")",
                "resamplingArgs", 1, "-resamplingArgs <args>"));
        result.addElement(
            new Option("\tExtra arguments.\n" + "\t(default: " + DEFAULT_EXTRA_ARGS + ")",
                "extraArgs", 1, "-extraArgs <args>"));

        Enumeration<Option> enu = super.listOptions();
        while (enu.hasMoreElements()) {
            result.addElement(enu.nextElement());
        }

        return result.elements();
    }

    /**
     * returns the options of the current setup.
     *
     * @return the current options
     */
    @Override
    public String[] getOptions() {
        Vector<String> result = new Vector<String>();

        result.add("-seed");
        result.add("" + seed);
        result.add("-timeLimit");
        result.add("" + timeLimit);
        result.add("-resampling");
        result.add("" + resampling);
        result.add("-resamplingArgs");
        result.add("" + resamplingArgs);
        result.add("-extraArgs");
        result.add("" + extraArgs);

        Collections.addAll(result, super.getOptions());
        return result.toArray(new String[result.size()]);
    }

    @Override
    public void setOptions(String[] options) throws Exception {
        String tmpStr;
        String[] tmpOptions;

        tmpStr = Utils.getOption("seed", options);
        if (tmpStr.length() != 0) {
            seed = Integer.parseInt(tmpStr);
        }

        tmpStr = Utils.getOption("timeLimit", options);
        if (tmpStr.length() != 0) {
            timeLimit = Integer.parseInt(tmpStr);
        } else {
            timeLimit = DEFAULT_TIME_LIMIT;
        }

        tmpStr = Utils.getOption("resampling", options);
        if (tmpStr.length() != 0) {
            resampling = Resampling.valueOf(tmpStr);
        } else {
            resampling = DEFAULT_RESAMPLING;
        }
        resamplingArgs = resamplingArgsMap.get(resampling);

        tmpStr = Utils.getOption("resamplingArgs", options);
        if (tmpStr.length() != 0) {
            resamplingArgs = tmpStr;
        }

        tmpStr = Utils.getOption("extraArgs", options);
        if (tmpStr.length() != 0) {
            extraArgs = tmpStr;
        } else {
            extraArgs = DEFAULT_EXTRA_ARGS;
        }

        super.setOptions(options);
        Utils.checkForRemainingOptions(options);
    }

    public void setSeed(int s) {
        seed = s;
    }

    public int getSeed() {
        return seed;
    }

    /**
     * Returns the tip text for this property.
     * @return tip text for this property
     */
    public String seedTipText() {
        return "the seed for the random number generator";
    }

    public void setTimeLimit(int tl) {
        timeLimit = tl;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    /**
     * Returns the tip text for this property.
     * @return tip text for this property
     */
    public String timeLimitTipText() {
        return "the time limit for tuning (in minutes)";
    }

    public void setResampling(Resampling r) {
        resampling = r;
        resamplingArgs = resamplingArgsMap.get(r);
    }

    public Resampling getResampling() {
        return resampling;
    }

    /**
     * Returns the tip text for this property.
     * @return tip text for this property
     */
    public String ResamplingTipText() {
        return "the type of resampling";
    }

    public void setResamplingArgs(String args) {
        resamplingArgs = args;
    }

    public String getResamplingArgs() {
        return resamplingArgs;
    }

    /**
     * Returns the tip text for this property.
     * @return tip text for this property
     */
    public String resamplingArgsTipText() {
        return "resampling arguments";
    }

    public void setExtraArgs(String args) {
        extraArgs = args;
    }

    public String getExtraArgs() {
        return extraArgs;
    }

    /**
     * Returns the tip text for this property.
     * @return tip text for this property
     */
    public String extraArgsTipText() {
        return "extra arguments";
    }

    /**
     * Returns default capabilities of the classifier.
     *
     * @return      the capabilities of this classifier
     */
    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();
        result.disableAll();

        // attributes
        result.enable(Capability.NOMINAL_ATTRIBUTES);
        result.enable(Capability.NUMERIC_ATTRIBUTES);
        result.enable(Capability.DATE_ATTRIBUTES);
        result.enable(Capability.STRING_ATTRIBUTES);
        result.enable(Capability.RELATIONAL_ATTRIBUTES);
        result.enable(Capability.MISSING_VALUES);

        // class
        result.enable(Capability.NOMINAL_CLASS);
        result.enable(Capability.NUMERIC_CLASS);
        result.enable(Capability.DATE_CLASS);
        result.enable(Capability.MISSING_CLASS_VALUES);

        // instances
        result.setMinimumNumberInstances(1);

        return result;
    }

    /**
     * Returns an instance of a TechnicalInformation object, containing 
     * detailed information about the technical background of this class,
     * e.g., paper reference or book this class is based on.
     * 
     * @return the technical information about this class
     */
    public TechnicalInformation getTechnicalInformation() {
        TechnicalInformation result = new TechnicalInformation(Type.INPROCEEDINGS);
        result.setValue(Field.AUTHOR, "Chris Thornton, Frank Hutter, Holger Hoos, and Kevin Leyton-Brown");
        result.setValue(Field.YEAR, "2013");
        result.setValue(Field.TITLE, "Auto-WEKA: Combined Selection and Hyperparameter Optimization of Classifiaction Algorithms");
        result.setValue(Field.BOOKTITLE, "Proc. of KDD 2013");

        return result;
    }

    /**
     * This will return a string describing the classifier.
     * @return The string.
     */
    public String globalInfo() {
        return "Automatically finds the best model with its best parameter settings for a given classification task.\n\n"
            + "For more information see:\n\n"
            + getTechnicalInformation().toString();
    }

    public String toString() {
        return "classifier: " + classifierClass + "\n" +
            "arguments: " + (classifierArgs != null ? Arrays.toString(classifierArgs) : "[]") + "\n" +
            "attribute search: " + attributeSearchClass + "\n" +
            "attribute search arguments: " + (attributeSearchArgs != null ? Arrays.toString(attributeSearchArgs) : "[]") + "\n" +
            "attribute evaluation: " + attributeEvalClass + "\n" +
            "attribute evaluation arguments: " + (attributeEvalArgs != null ? Arrays.toString(attributeEvalArgs) : "[]") + "\n" +
            "estimated error: " + estimatedError + "\n";
    }

    public double measureEstimatedError() {
        return estimatedError;
    }


    public Enumeration enumerateMeasures() {
        Vector newVector = new Vector(1);
        newVector.addElement("measureEstimatedError");
        return newVector.elements();
    }

    public double getMeasure(String additionalMeasureName) {
        if (additionalMeasureName.compareToIgnoreCase("measureEstimatedError") == 0) {
            return measureEstimatedError();
        } else {
            throw new IllegalArgumentException(additionalMeasureName 
                    + " not supported (Auto-WEKA)");
        }
    }
}
