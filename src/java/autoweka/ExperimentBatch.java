package autoweka;

import javax.xml.bind.annotation.*;
import java.io.InputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import java.net.URLDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helpful XML class that easily allows for the creation of multiple experiments.
 *
 * It's also easier to use this to make a batch of 1 than it is to specify everything on the command line to the ExperimentConstructor
 */
@XmlRootElement(name="experimentBatch")
@XmlAccessorType(XmlAccessType.NONE)
public class ExperimentBatch extends XmlSerializable
{
    final static Logger log = LoggerFactory.getLogger(ExperimentBatch.class);

    /** 
     * Enscapsulates a dataset that you want to run experiments on
     */
    @XmlRootElement(name="dataset")
    @XmlAccessorType(XmlAccessType.NONE)
    static public class DatasetComponent
    {
        /**
         * The class index - if not provided, will use the last attribute as the class
         */
        @XmlElement(name="classIndex")
        private String classIndex;
        
        /**
         * The name of the zip file that you want to use for instances
         */
        @XmlElement(name="zipFile")
        private String zipFile;

        /**
         * The Training file arff
         */
        @XmlElement(name="trainArff")
        private String trainArff;
        
        /**
         * The Testing file arff
         */
        @XmlElement(name="testArff")
        private String testArff;

        /**
         * The 'name' of this dataset, helpful to make it short and easy to identify
         */
        @XmlElement(name="name")
        public String name;

        /**
         * Gets the dataset string out of this component
         */
        public String getDatasetString()
        {
            int count = 0;
            count += (zipFile != null) ? 1 : 0;
            count += (trainArff != null && testArff != null) ? 1 : 0;
            if(count != 1)
                throw new IllegalArgumentException("A datasetComponent was found where the type could not be determined");

            Properties props = new Properties();
            if(classIndex != null)
                props.setProperty("classIndex", classIndex);

            if(zipFile != null)
            {
                props.setProperty("type", "zipFile");
                props.setProperty("zipFile", URLDecoder.decode(new File(zipFile).getAbsolutePath()));
            }
            
            if(trainArff != null && testArff != null) 
            {
                props.setProperty("type", "trainTestArff");
                props.setProperty("trainArff", URLDecoder.decode(new File(trainArff).getAbsolutePath()));
                props.setProperty("testArff", URLDecoder.decode(new File(testArff).getAbsolutePath()));
            }

            if(props.getProperty("type") == null)
                throw new RuntimeException("No type was determined for this datasetComponent");

            //Time to spit out the string
            return Util.propertiesToString(props);
        }

        public void setTrainTestArff(String _trainArff, String _testArff)
        {
            trainArff = _trainArff;
            testArff = _testArff;
        }
    }

    /**
     * Captures all the settings of an experiment that are not related to a dataset 
     */
    @XmlRootElement(name="experiment")
    @XmlAccessorType(XmlAccessType.NONE)
    static public class ExperimentComponent
    {
        /**
         * The experiment name's prefix
         */
        @XmlElement(name="name")
        public String name;
        /**
         * The name of the result metric to use - see ClassiferResult.Metric
         */
        @XmlElement(name="resultMetric")
        public String resultMetric;
        /**
         * The class name of the ExperimentConstructor to use
         */
        @XmlElement(name="experimentConstructor")
        public String constructor;
        /**
         * Any additional arguments that the ExperimentConstructor may want
         */
        @XmlElement(name="experimentConstructorArgs")
        public List<String> constructorArgs = new ArrayList<String>();
        /**
         * The class name of the instance generator
         */
        @XmlElement(name="instanceGenerator")
        public String instanceGenerator;
        /**
         * The property string that the instance generator will use to make instances
         */
        @XmlElement(name="instanceGeneratorArgs")
        public String instanceGeneratorArgs;
        //@XmlElement(name="regularizer")
        //public String regularizer;
        //@XmlElement(name="regularizerArgs")
        //public String regularizerArgs;
        /**
         * How many seconds should be spent for this experiment overall? (IE the SMBO method's budget)
         */
        @XmlElement(name="tunerTimeout")
        public float tunerTimeout = -1;
        /**
         * How many seconds should be spent training a set of hyperparameters on a specific partition of the training and test data?.
         *
         * Note that this is more of a guideline, Auto-WEKA has quite a bit of slack built into to accomodate classifiers that can return partial results,
         * or ones that decide to offload training onto their evaluation phase.
         */
        @XmlElement(name="trainTimeout")
        public float trainTimeout = -1;
        /**
         * Boolean indicating if Attribute/Feature selection should happen
         */
        @XmlElement(name="attributeSelection")
        public boolean attributeSelection = false;
        /** 
         * How many seconds should be spent performing attribute selection?
         */
        @XmlElement(name="attributeSelectionTimeout")
        public float attributeSelectionTimeout = -1;
        /**
         * The string passed to the Xmx argument of a sub process limiting the RAM that WEKA will have
         */
        @XmlElement(name="memory")
        public String memory;
        /**
         * The property string with any extra properties that the experiment might need
         */
        @XmlElement(name="extraProps")
        public String extraProps;
        /**
         * For analysis experiements, these extra classifier evaluations will be done for every point along the trajectory.
         *
         * For example, if we want to look at the Testing performance all the way along, we would add a new TrajectoryPointExtra that
         * contains an instance string of 'default', since that causes Auto-WEKA to preserve the provided training/test split
         */
        @XmlElement(name="trajectoryPointExtras")
        public List<Experiment.TrajectoryPointExtra> trajectoryPointExtras = new ArrayList<Experiment.TrajectoryPointExtra>();
        /**
         * Forces Auto-WEKA to only use the list of classifiers here - if it is empty, then Auto-WEKA will try to use everything that it can
         */
        @XmlElement(name="allowedClassifiers")
        public List<String> allowedClassifiers = new ArrayList<String>();
    }

    /**
     * The list of datasets in this batch
     */
    @XmlElement(name="datasetComponent")
    public List<DatasetComponent> mDatasets = new ArrayList<DatasetComponent>();

    /**
     * The list of experiment prototypes in this batch
     */
    @XmlElement(name="experimentComponent")
    public List<ExperimentComponent> mExperiments = new ArrayList<ExperimentComponent>();

    public static ExperimentBatch fromXML(String filename)
    {
        return XmlSerializable.fromXML(filename, ExperimentBatch.class);
    }
    public static ExperimentBatch fromXML(InputStream xml)
    {
        return XmlSerializable.fromXML(xml, ExperimentBatch.class);
    }

    /** 
     * Builds an Experiment from an ExperimentComponent and a DatasetComponent
     */
    public static Experiment createExperiment(ExperimentComponent expComp, DatasetComponent datasetComp)
    {
        Experiment exp = new Experiment();
        exp.name = expComp.name + "-" + datasetComp.name;
        exp.resultMetric = expComp.resultMetric;
        exp.instanceGenerator = expComp.instanceGenerator;
        exp.instanceGeneratorArgs = expComp.instanceGeneratorArgs;
        exp.datasetString = datasetComp.getDatasetString();
        exp.attributeSelection = expComp.attributeSelection;
        exp.attributeSelectionTimeout = expComp.attributeSelectionTimeout;
        exp.callString = new ArrayList<String>();
        exp.envVariables = new ArrayList<String>();
        exp.tunerTimeout = expComp.tunerTimeout; 
        exp.trainTimeout = expComp.trainTimeout;
        exp.memory = expComp.memory;
        exp.extraPropsString = expComp.extraProps;
        exp.trajectoryPointExtras = expComp.trajectoryPointExtras;
        exp.allowedClassifiers = new ArrayList<String>(expComp.allowedClassifiers);

        return exp;
    }

    /**
     * Makes an empty batch file that you can fill in with stuff
     */
    public static void main(String[] args)
    {
        if(args.length != 1)
        {
            log.error("Usage: {} <xmlfilename>", ExperimentBatch.class.getCanonicalName());
            log.error("Makes a template experiment batch file");
            System.exit(1);
        }
        ExperimentBatch batch = new ExperimentBatch();

        ExperimentBatch.DatasetComponent dataset = new ExperimentBatch.DatasetComponent();
        dataset.name = "";
        dataset.zipFile = "(Optional - One must be defined though)";

        batch.mDatasets = Collections.singletonList(dataset);

        ExperimentBatch.ExperimentComponent experiment = new ExperimentBatch.ExperimentComponent();
        experiment.name = "";
        experiment.constructor = "";
        experiment.instanceGenerator = "";
        experiment.instanceGeneratorArgs = "(Optional)";
        //experiment.regularizer = "(Optional)";
        //experiment.regularizerArgs = "(Optional)";
        experiment.tunerTimeout = -1;
        experiment.trainTimeout = -1;
        experiment.memory = "";
        experiment.extraProps = "(Optional)";
        experiment.constructorArgs = Collections.singletonList("(Optional List)");

        batch.mExperiments = Collections.singletonList(experiment);

        batch.toXML(args[0]);
    }
}

