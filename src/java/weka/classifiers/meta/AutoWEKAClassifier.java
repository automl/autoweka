package weka.classifiers.meta;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;

import weka.core.Attribute;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Vector;

import autoweka.Experiment;
import autoweka.ExperimentConstructor;
import autoweka.InstanceGenerator;
import autoweka.instancegenerators.CrossValidation;
import autoweka.Util;
import autoweka.TrajectoryGroup;
import autoweka.TrajectoryMerger;

import autoweka.tools.GetBestFromTrajectoryGroup;

public class AutoWEKAClassifier extends AbstractClassifier {

    /** for serialization */
    static final long serialVersionUID = 2907034203562786373L;

    static final int DEFAULT_TIME_LIMIT = 60;

    /* The Chosen One. */
    protected Classifier classifier;
    protected AttributeSelection as;

    protected String classifierClass;
    protected String[] classifierArgs;
    protected String attributeSearchClass;
    protected String[] attributeSearchArgs;
    protected String attributeEvalClass;
    protected String[] attributeEvalArgs;

    protected static String msExperimentPath = "wizardexperiments" + File.separator;
    protected static String expName = "Auto-WEKA";

    protected int timeLimit = DEFAULT_TIME_LIMIT;

    private Process mProc;

    /**
     * Main method for testing this class.
     *
     * @param argv should contain command line options (see setOptions)
     */
    public static void main(String[] argv) {
        runClassifier(new AutoWEKAClassifier(), argv);
    }

    public AutoWEKAClassifier() {
        classifier = null;
        classifierClass = null;
        classifierArgs = null;
        attributeSearchClass = null;
        attributeSearchArgs = null;
        attributeEvalClass = null;
        attributeEvalArgs = null;
    }

    public void buildClassifier(Instances is) throws Exception {
        getCapabilities().testWithFail(is);

        //Populate the experiment fields
        Experiment exp = new Experiment();
        exp.name = expName;

        //Which result metric do we use?
        Attribute classAttr = is.classAttribute();
        if(classAttr.isNominal()){
            exp.resultMetric = "errorRate"; 
        }else if(classAttr.isNumeric()) {
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
        exp.instanceGenerator = "autoweka.instancegenerators.CrossValidation";
        exp.instanceGeneratorArgs = "numFolds=10";
        exp.attributeSelection = true;

        exp.attributeSelectionTimeout = timeLimit * 1;
        exp.tunerTimeout = timeLimit * 60;
        exp.trainTimeout = timeLimit * 5;

        exp.memory = "500m";
        exp.extraPropsString = "initialIncumbent=RANDOM:acq-func=EI";

        //Setup all the extra args
        List<String> args = new LinkedList<String>();
        args.add("-experimentpath");
        args.add(msExperimentPath);

        //Make the thing
        ExperimentConstructor.buildSingle("autoweka.smac.SMACExperimentConstructor", exp, args);

        // run experiment
        Thread worker = new Thread(new Runnable() {
            public void run() {
                try {
                    ProcessBuilder pb = new ProcessBuilder(autoweka.Util.getJavaExecutable(), "-Xmx128m", "-cp", autoweka.Util.getAbsoluteClasspath(), "autoweka.tools.ExperimentRunner", msExperimentPath + expName, "0");
                    pb.redirectErrorStream(true);

                    mProc = pb.start();

                    Thread killerHook = new autoweka.Util.ProcessKillerShutdownHook(mProc);
                    Runtime.getRuntime().addShutdownHook(killerHook);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(mProc.getInputStream()));
                    String line;
                    while ((line = reader.readLine ()) != null) {
                        System.err.println(line);
                    }
                    Runtime.getRuntime().removeShutdownHook(killerHook);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } });
        worker.start();
        worker.join();

        // get results
        TrajectoryGroup group = TrajectoryMerger.mergeExperimentFolder(msExperimentPath + expName);
        GetBestFromTrajectoryGroup mBest = new GetBestFromTrajectoryGroup(group);
        classifierClass = mBest.classifierClass;
        classifierArgs = mBest.classifierArgs.split(" ");
        attributeSearchClass = mBest.attributeSearchClass;
        attributeSearchArgs = mBest.attributeSearchArgs.split(" ");
        attributeEvalClass = mBest.attributeEvalClass;
        attributeEvalArgs = mBest.attributeEvalArgs.split(" ");

        // train model on entire dataset and save
        ASSearch asSearch = ASSearch.forName(attributeSearchClass, attributeSearchArgs.clone());
        ASEvaluation asEval = ASEvaluation.forName(attributeEvalClass, attributeEvalArgs.clone());

        as = new AttributeSelection();
        as.setSearch(asSearch);
        as.setEvaluator(asEval);
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
            new Option("\tThe time limit for tuning in minutes (approximately).\n"
                        + "\t(default: 60)", "timeLimit", 60,
                        "-timeLimit <limit>"));
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

        result.add("-timeLimit");
        result.add("" + timeLimit);

        Collections.addAll(result, super.getOptions());
        return result.toArray(new String[result.size()]);
    }

    @Override
    public void setOptions(String[] options) throws Exception {
        String tmpStr;
        String[] tmpOptions;

        tmpStr = Utils.getOption("timeLimit", options);
        if (tmpStr.length() != 0) {
            timeLimit = Integer.parseInt(tmpStr);
        } else {
            timeLimit = DEFAULT_TIME_LIMIT;
        }

        super.setOptions(options);
        Utils.checkForRemainingOptions(options);
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
            "attribute evaluation arguments: " + (attributeEvalArgs != null ? Arrays.toString(attributeEvalArgs) : "[]") + "\n";
    }
}
