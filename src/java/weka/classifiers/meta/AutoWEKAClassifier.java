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
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.io.File;
import java.io.Serializable;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;

import autoweka.Experiment;
import autoweka.ExperimentConstructor;
import autoweka.InstanceGenerator;
import autoweka.instancegenerators.CrossValidation;
import autoweka.Util;
import autoweka.TrajectoryGroup;
import autoweka.TrajectoryMerger;

import autoweka.tools.GetBestFromTrajectoryGroup;

import autoweka.ui.ExperimentRunner;
import autoweka.ui.TrainedModelRunner;

public class AutoWEKAClassifier extends AbstractClassifier {

    /** for serialization */
    static final long serialVersionUID = 2907034203562786373L;

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

    protected TrainedModelRunner mTrainedModelRunner;
    protected ExperimentRunner mRunner;

    /**
     * Main method for testing this class.
     *
     * @param argv should contain command line options (see setOptions)
     */
    public static void main(String[] argv) {
        runClassifier(new AutoWEKAClassifier(), argv);
    }

    public AutoWEKAClassifier() {
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
        File fp = new File(msExperimentPath + expName + File.separator + "Auto-WEKA-train.arff");
        saver.setFile(fp);
        saver.writeBatch();
        props.setProperty("trainArff", fp.getAbsolutePath());
        exp.datasetString = Util.propertiesToString(props);
        exp.instanceGenerator = "autoweka.instancegenerators.CrossValidation";
        exp.instanceGeneratorArgs = "numFolds=10";
        exp.attributeSelection = true;

        // hardcode for now to 1 hour-ish
        exp.attributeSelectionTimeout = 10;
        exp.tunerTimeout = 60;
        exp.trainTimeout = 10;

        exp.memory = "500m";
        exp.extraPropsString = "initialIncumbent=RANDOM:acq-func=EI";

        //Setup all the extra args
        List<String> args = new LinkedList<String>();
        args.add("-experimentpath");
        args.add(msExperimentPath);

        //Make the thing
        ExperimentConstructor.buildSingle("autoweka.smac.SMACExperimentConstructor", exp, args);

        mRunner = new ExperimentRunner();
        mRunner.setFolderSeedAndObserver(msExperimentPath + expName, "0", new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                mRunner.dispose();
                mTrainedModelRunner.setVisible(true);
                mTrainedModelRunner.openExperiment(msExperimentPath + expName);
            }
        });
        mRunner.setVisible(true);
        mTrainedModelRunner = new TrainedModelRunner();
        mRunner.runClicked();

        // wait for worker to finish
        mRunner.join();

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
        if(classifierClass == null) {
            throw new Exception("Auto-WEKA has not been run yet to get a model!");
        }
        i = as.reduceDimensionality(i);
        return classifier.classifyInstance(i);
    }

    public double[] distributionForInstance(Instance i) throws Exception {
        if(classifierClass == null) {
            throw new Exception("Auto-WEKA has not been run yet to get a model!");
        }
        i = as.reduceDimensionality(i);
        return classifier.distributionForInstance(i);
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
