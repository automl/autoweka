/*
*    This program is free software; you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation; either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program; if not, write to the Free Software
*    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

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

import java.net.URLDecoder;

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
import autoweka.Ensembler;

import autoweka.tools.GetBestFromTrajectoryGroup;


import autoweka.Configuration;
import autoweka.ConfigurationCollection;
import autoweka.ConfigurationRanker;

/**
 * Auto-WEKA interface for WEKA.

* * @author Lars Kotthoff
 */
public class AutoWEKAClassifier extends AbstractClassifier implements AdditionalMeasureProducer {

    /** For serialization. */
    static final long serialVersionUID = 2907034203562786373L;

    /** For logging Auto-WEKA's output. */
    final Logger log = LoggerFactory.getLogger(AutoWEKAClassifier.class);

    /** Default time limit for Auto-WEKA. */
    static final int DEFAULT_TIME_LIMIT = 15;
    /** Default memory limit for classifiers. */
    static final int DEFAULT_MEM_LIMIT = 1024;
    /** Default */
    static final int DEFAULT_N_BEST = 1;
    /** Internal evaluation method. */
    static enum Resampling {
        CrossValidation,
        MultiLevel,
        RandomSubSampling,
        TerminationHoldout
    }
    /** Default evaluation method. */
    static final Resampling DEFAULT_RESAMPLING = Resampling.TerminationHoldout;

    /** Default arguments for the different evaluation methods. */
    static final Map<Resampling, String> resamplingArgsMap;
    static {
        resamplingArgsMap = new HashMap<Resampling, String>();
        resamplingArgsMap.put(Resampling.CrossValidation, "numFolds=10");
        resamplingArgsMap.put(Resampling.MultiLevel, "numLevels=2[$]autoweka.instancegenerators.CrossValidation[$]numFolds=10");
        resamplingArgsMap.put(Resampling.RandomSubSampling, "numSamples=10:percent=66");
        resamplingArgsMap.put(Resampling.TerminationHoldout, "terminationPercent=30[$]autoweka.instancegenerators.CrossValidation[$]numFolds=10");
    }
    /** Arguments for the default evaluation method. */
    static final String DEFAULT_RESAMPLING_ARGS = resamplingArgsMap.get(DEFAULT_RESAMPLING);

    /** Default additional arguments for Auto-WEKA. */
    static final String DEFAULT_EXTRA_ARGS = "initialIncumbent=RANDOM:acq-func=EI";

    /** The path for the sorted best configurations **/
    public static final String configurationRankingPath = "EnsemblerLogging/configuration_ranking.xml";
	 /** The path for a map which assigns a number to every config evaluated by smac.**/
	 public static final String configurationMapPath = "EnsemblerLogging/configuration_map.txt";
	 /** The path for the log with the score for each fold of each configuration evaluated by SMAC **/
    public static final String foldwiseLogPath = "EnsemblerLogging/foldwise_log.txt";
    /** The path for the directory with the instancewise predictions for each config **/
    public static final String instancewiseInfoDirPath  = "EnsemblerLogging/instancewisePredictions/";

    /** The chosen classifier. */
    protected Classifier classifier;
    /** The chosen attribute selection method. */
    protected AttributeSelection as;

    /** The class of the chosen classifier. */
    protected String classifierClass;
    /** The arguments of the chosen classifier. */
    protected String[] classifierArgs;
    /** The class of the chosen attribute search method. */
    protected String attributeSearchClass;
    /** The arguments of the chosen attribute search method. */
    protected String[] attributeSearchArgs;
    /** The class of the chosen attribute evaluation. */
    protected String attributeEvalClass;
    /** The arguments of the chosen attribute evaluation method. */
    protected String[] attributeEvalArgs;

    /** The path to the internal Auto-WEKA files.*/
    protected static String msExperimentPath;
    /** The internal name of the experiment. */
    protected static String expName = "Auto-WEKA";
    /** The random seed. */
    protected int seed = 123;
    /** The time limit for running Auto-WEKA. */
    protected int timeLimit = DEFAULT_TIME_LIMIT;
    /** The memory limit for running classifiers. */
    protected int memLimit = DEFAULT_MEM_LIMIT;
    /** The amout of best configurations to return as output*/
    protected int nBestConfigs = DEFAULT_N_BEST;
    /** The internal evaluation method. */
    protected Resampling resampling = DEFAULT_RESAMPLING;
    /** The arguments to the evaluation method. */
    protected String resamplingArgs = DEFAULT_RESAMPLING_ARGS;
    /** The extra arguments for Auto-WEKA. */
    protected String extraArgs = DEFAULT_EXTRA_ARGS;

    /** The estimated error of the chosen method. */
    protected double estimatedError = -1;

    /** The evaluation for the best classifier. */
    protected Evaluation eval;

    private transient weka.gui.Logger wLog;

    /**
     * Main method for testing this class.
     *
     * @param argv should contain command line options (see setOptions)
     */
    public static void main(String[] argv) {
        // this always succeeds...
        runClassifier(new AutoWEKAClassifier(), argv);
    }

    /** Constructs a new AutoWEKAClassifier. */
    public AutoWEKAClassifier() {
        classifier = null;
        classifierClass = null;
        classifierArgs = null;
        attributeSearchClass = null;
        attributeSearchArgs = new String[0];
        attributeEvalClass = null;
        attributeEvalArgs = new String[0];
        wLog = null;

        // work around broken XML parsers
        Properties props = System.getProperties();
        props.setProperty("org.xml.sax.parser", "com.sun.org.apache.xerces.internal.parsers.SAXParser");
        props.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        props.setProperty("javax.xml.parsers.SAXParserFactory", "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
    }


    /**
    * Find the best classifier, arguments, and attribute selection for the data.
    *
    * @param data the training data to be used for selecting and tuning the
    * classifier.
    * @throws Exception if the classifier could not be built successfully.
    */
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
        props.setProperty("trainArff", URLDecoder.decode(fp.getAbsolutePath()));
        exp.datasetString = Util.propertiesToString(props);
        exp.instanceGenerator = "autoweka.instancegenerators." + String.valueOf(resampling);
        exp.instanceGeneratorArgs = "seed=" + seed + ":" + resamplingArgs + ":seed=" + seed;
        exp.attributeSelection = true;

        exp.attributeSelectionTimeout = timeLimit * 1;
        exp.tunerTimeout = timeLimit * 50;
        exp.trainTimeout = timeLimit * 5;

        exp.memory = memLimit + "m";
        exp.extraPropsString = extraArgs;

        //Setup all the extra args
        List<String> args = new LinkedList<String>();
        args.add("-experimentpath");
        args.add(msExperimentPath);
        //Make the thing

        ExperimentConstructor.buildSingle("autoweka.smac.SMACExperimentConstructor", exp, args);

        //Initializing logs for Ensemble Selection
         String temporaryDirPath = msExperimentPath+expName+"/";
         //Util.makePath(temporaryDirPath+configurationInfoDirPath);
         Util.makePath(temporaryDirPath+instancewiseInfoDirPath);
         Util.initializeFile(temporaryDirPath+configurationRankingPath);
         Util.initializeFile(temporaryDirPath+foldwiseLogPath);
			Util.initializeFile(temporaryDirPath+configurationMapPath);



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
                    Pattern p = Pattern.compile(".*Estimated mean quality of final incumbent config .* on test set: ([0-9.]+).*");
                    Pattern pint = Pattern.compile(".*mean quality of.*: ([0-9E.]+);.*");
                    int tried = 0;
                    double bestError = -1;
                    while((line = reader.readLine()) != null) {
                        Matcher m = p.matcher(line);
                        if(m.matches()) {
                            estimatedError = Double.parseDouble(m.group(1));
                        }
                        m = pint.matcher(line);
                        if(m.matches()) {
                            bestError = Double.parseDouble(m.group(1));
                        }
                        // fix nested logging...
                        if(line.matches(".*DEBUG.*") || line.matches(".*Variance is less than.*")) {
                            //log.debug(line);
                        } else if(line.matches(".*INFO.*")) {
                            if(line.matches(".*ClassifierRunner - weka.classifiers.*")) {
                                tried++;
                                if(wLog != null) {
                                    String msg = "Performed " + tried + " evaluations, estimated error " + bestError + "...";
                                    wLog.statusMessage(msg);
                                    if(tried % 10 == 0)
                                        wLog.logMessage(msg);
                                }
                            }
                            //log.info(line);
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

		  if((group.getTrajectories().isEmpty())){
			  System.out.println("\n\namagad im emptyyy\n\n");
		  }else{
			  System.out.println("\n\nnot emptyyy\n\n"+group.getTrajectories().toString());
		  }

        GetBestFromTrajectoryGroup mBest = new GetBestFromTrajectoryGroup(group);

        if(mBest.errorEstimate == autoweka.ClassifierResult.INFINITY) {
            throw new Exception("All runs timed out, unable to find good configuration. Please allow more time and rerun.");
        }

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


        //Print log of best configurations
        if (nBestConfigs>1){
         //  ConfigurationRanker cr = new ConfigurationRanker(msExperimentPath+expName+"/");
         //  cr.rank( nBestConfigs , mBest.rawArgs);
			ConfigurationCollection cc = new ConfigurationCollection(msExperimentPath+expName+"/"+foldwiseLogPath);
			cc.rank(nBestConfigs , mBest.rawArgs,msExperimentPath+expName);

			 long startTime= System.nanoTime();
          Ensembler e = new Ensembler(msExperimentPath+expName+"/");
	   	 e.hillclimb();
			 long endTime = System.nanoTime();
			 long totalTime = endTime-startTime;
			 long totalTimeSeconds = totalTime/1000000000;
			 System.out.println("@end time in nanosecs: "+totalTime+" end time in secs: "+totalTimeSeconds);
        }

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


        classifier = AbstractClassifier.forName(classifierClass, classifierArgs.clone());

        is = as.reduceDimensionality(is);
        classifier.buildClassifier(is);

        eval = new Evaluation(is);
        eval.evaluateModel(classifier, is);
    }

    /**
    * Calculates the class membership for the given test instance.
    *
    * @param instance the instance to be classified
    * @return predicted class
    * @throws Exception if instance could not be classified successfully
    */
    public double classifyInstance(Instance i) throws Exception {
        if(classifier == null) {
            throw new Exception("Auto-WEKA has not been run yet to get a model!");
        }
        i = as.reduceDimensionality(i);
        return classifier.classifyInstance(i);
    }

    /**
    * Calculates the class membership probabilities for the given test instance.
    *
    * @param instance the instance to be classified
    * @return predicted class probability distribution
    * @throws Exception if instance could not be classified successfully.
    */
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
            new Option("\tThe memory limit for runs in MiB.\n" + "\t(default: " + DEFAULT_MEM_LIMIT + ")",
                "memLimit", 1, "-memLimit <limit>"));
        result.addElement(
            new Option("\tThe amount of best configurations to return.\n" + "\t(default: " + DEFAULT_MEM_LIMIT + ")",
                "nBestConfigs", 1, "-nBestConfigs <limit>"));
        //result.addElement(
        //    new Option("\tThe type of resampling used.\n" + "\t(default: " + String.valueOf(DEFAULT_RESAMPLING) + ")",
        //        "resampling", 1, "-resampling <resampling>"));
        //result.addElement(
        //    new Option("\tResampling arguments.\n" + "\t(default: " + DEFAULT_RESAMPLING_ARGS + ")",
        //        "resamplingArgs", 1, "-resamplingArgs <args>"));
        //result.addElement(
        //    new Option("\tExtra arguments.\n" + "\t(default: " + DEFAULT_EXTRA_ARGS + ")",
        //        "extraArgs", 1, "-extraArgs <args>"));

        Enumeration<Option> enu = super.listOptions();
        while (enu.hasMoreElements()) {
            result.addElement(enu.nextElement());
        }

        return result.elements();
    }

    /**
     * Returns the options of the current setup.
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
        result.add("-memLimit");
        result.add("" + memLimit);
        result.add("-nBestConfigs");
        result.add("" + nBestConfigs);
        //result.add("-resampling");
        //result.add("" + resampling);
        //result.add("-resamplingArgs");
        //result.add("" + resamplingArgs);
        //result.add("-extraArgs");
        //result.add("" + extraArgs);

        Collections.addAll(result, super.getOptions());
        return result.toArray(new String[result.size()]);
    }

    /**
     * Set the options for the current setup.
     *
     * @param options the new options
     */
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

        tmpStr = Utils.getOption("memLimit", options);
        if (tmpStr.length() != 0) {
            memLimit = Integer.parseInt(tmpStr);
        } else {
            memLimit = DEFAULT_MEM_LIMIT;
        }


        tmpStr = Utils.getOption("nBestConfigs", options);
        if (tmpStr.length() != 0) {
            nBestConfigs = Integer.parseInt(tmpStr);
        } else {
            nBestConfigs = DEFAULT_N_BEST;
        }


        //tmpStr = Utils.getOption("resampling", options);
        //if (tmpStr.length() != 0) {
        //    resampling = Resampling.valueOf(tmpStr);
        //} else {
        //    resampling = DEFAULT_RESAMPLING;
        //}
        //resamplingArgs = resamplingArgsMap.get(resampling);

        //tmpStr = Utils.getOption("resamplingArgs", options);
        //if (tmpStr.length() != 0) {
        //    resamplingArgs = tmpStr;
        //}

        //tmpStr = Utils.getOption("extraArgs", options);
        //if (tmpStr.length() != 0) {
        //    extraArgs = tmpStr;
        //} else {
		  //}
        //    extraArgs = DEFAULT_EXTRA_ARGS;

        super.setOptions(options);
        Utils.checkForRemainingOptions(options);
    }

    /**
     * Set the random seed.
     * @param s The random seed.
     */
    public void setSeed(int s) {
        seed = s;
    }

    /**
     * Get the random seed.
     * @return The random seed.
     */
    public int getSeed() {
        return seed;
    }

    /**
     * Returns the tip text for this property.
     * @return tip text for this property
     */
    public String seedTipText() {
        return "the seed for the random number generator (you do not usually need to change this)";
    }

    /**
     * Set the time limit.
     * @param tl The time limit in minutes.
     */
    public void setTimeLimit(int tl) {
        timeLimit = tl;
    }

    /**
     * Get the time limit.
     * @return The time limit in minutes.
     */
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

    /**
     * Set the memory limit.
     * @param ml The memory limit in MiB.
     */
    public void setMemLimit(int ml) {
        memLimit = ml;
    }

    /**
     * Get the memory limit.
     * @return The memory limit in MiB.
     */
    public int getMemLimit() {
        return memLimit;
    }

    /**
     * Returns the tip text for this property.
     * @return tip text for this property
     */
    public String memLimitTipText() {
        return "the memory limit for runs (in MiB)";
    }


   /**
    * Set the amount of configurations that will be given as output
    * @param The amount of best configurations desired by the user
    */
   public void setNBestConfigs(int nbc) {
       nBestConfigs = nbc;
   }

   /**
    * Get the memory limit.
    * @return The amount of best configurations that will be given as output
    */
   public int getNBestConfigs() {
       return nBestConfigs;
   }

   /**
    * Returns the tip text for this property.
    * @return tip text for this property
    */
   public String nBestConfigsTipText() {
       return "How many of the best configurations should be returned as output";
   }


    //public void setResampling(Resampling r) {
    //    resampling = r;
    //    resamplingArgs = resamplingArgsMap.get(r);
    //}

    //public Resampling getResampling() {
    //    return resampling;
    //}

    ///**
    // * Returns the tip text for this property.
    // * @return tip text for this property
    // */
    //public String ResamplingTipText() {
    //    return "the type of resampling";
    //}

    //public void setResamplingArgs(String args) {
    //    resamplingArgs = args;
    //}

    //public String getResamplingArgs() {
    //    return resamplingArgs;
    //}

    ///**
    // * Returns the tip text for this property.
    // * @return tip text for this property
    // */
    //public String resamplingArgsTipText() {
    //    return "resampling arguments";
    //}

    //public void setExtraArgs(String args) {
    //    extraArgs = args;
    //}

    //public String getExtraArgs() {
    //    return extraArgs;
    //}

    ///**
    // * Returns the tip text for this property.
    // * @return tip text for this property
    // */
    //public String extraArgsTipText() {
    //    return "extra arguments";
    //}

    /** Set the WEKA logger.
     * Used for providing feedback during execution.
     */
    public void setLog(weka.gui.Logger log) {
        this.wLog = log;
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
        return "Automatically finds the best model with its best parameter settings for a given dataset.\n\n"
            + "For more information see:\n\n"
            + getTechnicalInformation().toString();
    }

    /**
     * This will return a string describing the classifier.
     * @return The string.
     */
    public String toString() {
        String res = "best classifier: " + classifierClass + "\n" +
            "arguments: " + (classifierArgs != null ? Arrays.toString(classifierArgs) : "[]") + "\n" +
            "attribute search: " + attributeSearchClass + "\n" +
            "attribute search arguments: " + (attributeSearchArgs != null ? Arrays.toString(attributeSearchArgs) : "[]") + "\n" +
            "attribute evaluation: " + attributeEvalClass + "\n" +
            "attribute evaluation arguments: " + (attributeEvalArgs != null ? Arrays.toString(attributeEvalArgs) : "[]") + "\n" +
            "estimated error: " + estimatedError + "\n\n"; //@TODO looks like this error is printing the wrong value
        try {
            res += eval.toSummaryString();
            res += "\n";
            res += eval.toMatrixString();
            res += "\n";
            res += eval.toClassDetailsString();
        } catch(Exception e) { }

        res += "\n\nFor better performance, try giving Auto-WEKA more time.";
        return res;
    }

    /**
     * Returns the error estimated during Auto-WEKA's internal evaluation.
     * @return The estimated error.
     */
    public double measureEstimatedError() {
        return estimatedError;
    }

    /**
    * Returns an enumeration of the additional measure names
    * @return an enumeration of the measure names
    */
    public Enumeration enumerateMeasures() {
        Vector newVector = new Vector(1);
        newVector.addElement("measureEstimatedError");
        return newVector.elements();
    }

    /**
    * Returns the value of the named measure
    * @param additionalMeasureName the name of the measure to query for its value
    * @return the value of the named measure
    * @throws IllegalArgumentException if the named measure is not supported
    */
    public double getMeasure(String additionalMeasureName) {
        if (additionalMeasureName.compareToIgnoreCase("measureEstimatedError") == 0) {
            return measureEstimatedError();
        } else {
            throw new IllegalArgumentException(additionalMeasureName
                    + " not supported (Auto-WEKA)");
        }
  }
}
