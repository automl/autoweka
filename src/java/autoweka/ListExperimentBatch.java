package autoweka;

import javax.xml.bind.annotation.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Similar to an ExperimentBatch, a ListExperiment Batch provides a way of easily making ListExperiments
 */
@XmlRootElement(name="listExperimentBatch")
@XmlAccessorType(XmlAccessType.NONE)
class ListExperimentBatch extends XmlSerializable
{
    final static Logger log = LoggerFactory.getLogger(ListExperimentBatch.class);

    @XmlRootElement(name="experiment")
    @XmlAccessorType(XmlAccessType.NONE)
    static public class ListExperimentComponent
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
        /**
         * How many seconds should be spent training a set of hyperparameters on a specific partition of the training and test data?.
         *
         * Note that this is more of a guideline, Auto-WEKA has quite a bit of slack built into to accomodate classifiers that can return partial results,
         * or ones that decide to offload training onto their evaluation phase.
         */
        @XmlElement(name="trainTimeout")
        public float trainTimeout = -1;
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
         * For analysis experiements, these extra classifier evaluations will be done for every argstring in the experiment.
         */
        @XmlElement(name="trajectoryPointExtras")
        public List<Experiment.TrajectoryPointExtra> trajectoryPointExtras = new ArrayList<Experiment.TrajectoryPointExtra>();
        /**
         * The seed to use 
         */
        @XmlElement(name="seed")
        public String seed = "0";
    }

    @XmlElement(name="datasetComponent")
    List<ExperimentBatch.DatasetComponent> mInstances;

    @XmlElement(name="listExperimentComponent")
    List<ListExperimentComponent> mExperiments;

    public static ListExperimentBatch fromXML(String filename)
    {
        return XmlSerializable.fromXML(filename, ListExperimentBatch.class);
    }
    public static ListExperimentBatch fromXML(InputStream xml)
    {
        return XmlSerializable.fromXML(xml, ListExperimentBatch.class);
    }

    /**
     * Creates a ListExperiment out of the corresponding bits
     */
    public static ListExperiment createListExperiment(ListExperimentComponent expComp, ExperimentBatch.DatasetComponent datasetComp)
    {
        ListExperiment exp = new ListExperiment();
        exp.name = expComp.name + "-" + datasetComp.name;
        exp.resultMetric = expComp.resultMetric;
        exp.instanceGenerator = expComp.instanceGenerator;
        exp.instanceGeneratorArgs = expComp.instanceGeneratorArgs;
        exp.datasetString = datasetComp.getDatasetString();
        exp.trainTimeout = expComp.trainTimeout;
        exp.memory = expComp.memory;
        exp.extraPropsString = expComp.extraProps;
        exp.trajectoryPointExtras = expComp.trajectoryPointExtras;

        return exp;
    }

    /**
     * Makes a new template ListExperimentBatch template
     */
    public static void main(String[] args)
    {
        if(args.length != 1)
        {
            log.error("Useage: {} <xmlfilename>", ExperimentBatch.class.getCanonicalName());
            log.error("Makes a template experiment batch file");
            System.exit(1);
        }
        ListExperimentBatch batch = new ListExperimentBatch();

        ExperimentBatch.DatasetComponent instance = new ExperimentBatch.DatasetComponent();
        instance.name = "";

        batch.mInstances = Collections.singletonList(instance);

        ListExperimentComponent experiment = new ListExperimentComponent();
        experiment.name = "";
        experiment.instanceGenerator = "";
        experiment.instanceGeneratorArgs = "(Optional)";
        experiment.trainTimeout = -1;
        experiment.memory = "";
        experiment.extraProps = "(Optional)";
        experiment.constructorArgs = Collections.singletonList("(Optional List)");

        batch.mExperiments = Collections.singletonList(experiment);

        batch.toXML(args[0]);
    }
}

