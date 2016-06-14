/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    LogitBoost.java
 *    Copyright (C) 1999-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.IterativeClassifier;
import weka.classifiers.RandomizableIteratedSingleClassifierEnhancer;
import weka.classifiers.Sourcable;
import weka.core.Attribute;
import weka.core.BatchPredictor;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.UnassignedClassException;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

/**
 * <!-- globalinfo-start --> Class for performing additive logistic regression. <br/>
 * This class performs classification using a regression scheme as the base
 * learner, and can handle multi-class problems. For more information, see<br/>
 * <br/>
 * J. Friedman, T. Hastie, R. Tibshirani (1998). Additive Logistic Regression: a
 * Statistical View of Boosting. Stanford University.<br/>
 * <br/>
 * Can do efficient internal cross-validation to determine appropriate number of
 * iterations.
 * <p/>
 * <!-- globalinfo-end -->
 *
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;techreport{Friedman1998,
 *    address = {Stanford University},
 *    author = {J. Friedman and T. Hastie and R. Tibshirani},
 *    title = {Additive Logistic Regression: a Statistical View of Boosting},
 *    year = {1998},
 *    PS = {http://www-stat.stanford.edu/\~jhf/ftp/boost.ps}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 *
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -Q
 *  Use resampling instead of reweighting for boosting.
 * </pre>
 * 
 * <pre>
 * -P &lt;percent&gt;
 *  Percentage of weight mass to base training on.
 *  (default 100, reduce to around 90 speed up)
 * </pre>
 * 
 * <pre>
 * -L &lt;num&gt;
 *  Threshold on the improvement of the likelihood.
 *  (default -Double.MAX_VALUE)
 * </pre>
 * 
 * <pre>
 * -H &lt;num&gt;
 *  Shrinkage parameter.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -Z &lt;num&gt;
 *  Z max threshold for responses.
 *  (default 3)
 * </pre>
 * 
 * <pre>
 * -O &lt;int&gt;
 *  The size of the thread pool, for example, the number of cores in the CPU. (default 1)
 * </pre>
 * 
 * <pre>
 * -E &lt;int&gt;
 *  The number of threads to use for batch prediction, which should be &gt;= size of thread pool.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -I &lt;num&gt;
 *  Number of iterations.
 *  (default 10)
 * </pre>
 * 
 * <pre>
 * -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.DecisionStump)
 * </pre>
 * 
 * <pre>
 * -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <pre>
 * -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).
 * </pre>
 * 
 * <pre>
 * Options specific to classifier weka.classifiers.trees.DecisionStump:
 * </pre>
 * 
 * <pre>
 * -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <pre>
 * -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).
 * </pre>
 * 
 * <!-- options-end -->
 *
 * Options after -- are passed to the designated learner.
 * <p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 11958 $
 */
public class LogitBoost extends RandomizableIteratedSingleClassifierEnhancer
  implements Sourcable, WeightedInstancesHandler, TechnicalInformationHandler,
  IterativeClassifier, BatchPredictor {

  /** for serialization */
  static final long serialVersionUID = -1105660358715833753L;

  /**
   * ArrayList for storing the generated base classifiers. Note: we are hiding
   * the variable from IteratedSingleClassifierEnhancer
   */
  protected ArrayList<Classifier[]> m_Classifiers;

  /** The number of classes */
  protected int m_NumClasses;

  /** The number of successfully generated base classifiers. */
  protected int m_NumGenerated;

  /** Weight thresholding. The percentage of weight mass used in training */
  protected int m_WeightThreshold = 100;

  /** A threshold for responses (Friedman suggests between 2 and 4) */
  protected static final double DEFAULT_Z_MAX = 3;

  /** Dummy dataset with a numeric class */
  protected Instances m_NumericClassData;

  /** The actual class attribute (for getting class names) */
  protected Attribute m_ClassAttribute;

  /** Use boosting with reweighting? */
  protected boolean m_UseResampling;

  /** The threshold on the improvement of the likelihood */
  protected double m_Precision = -Double.MAX_VALUE;

  /** The value of the shrinkage parameter */
  protected double m_Shrinkage = 1;

  /** The random number generator used */
  protected Random m_RandomInstance = null;

  /**
   * The value by which the actual target value for the true class is offset.
   */
  protected double m_Offset = 0.0;

  /** a ZeroR model in case no model can be built from the data */
  protected Classifier m_ZeroR;

  /** The Z max value to use */
  protected double m_zMax = DEFAULT_Z_MAX;

  /** The y values used during the training process. */
  protected double[][] m_trainYs;

  /** The F scores used during the training process. */
  protected double[][] m_trainFs;

  /** The probabilities used during the training process. */
  protected double[][] m_probs;

  /** The current loglikelihood. */
  protected double m_logLikelihood;

  /** The total weight of the data. */
  protected double m_sumOfWeights;

  /** The training data. */
  protected Instances m_data;

  /** The number of threads to use at prediction time in batch prediction. */
  protected int m_numThreads = 1;

  /** The size of the thread pool. */
  protected int m_poolSize = 1;

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {

    return "Class for performing additive logistic regression. \n"
      + "This class performs classification using a regression scheme as the "
      + "base learner, and can handle multi-class problems.  For more "
      + "information, see\n\n" + getTechnicalInformation().toString() + "\n\n"
      + "Can do efficient internal cross-validation to determine "
      + "appropriate number of iterations.";
  }

  /**
   * Constructor.
   */
  public LogitBoost() {

    m_Classifier = new weka.classifiers.trees.DecisionStump();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.TECHREPORT);
    result
      .setValue(Field.AUTHOR, "J. Friedman and T. Hastie and R. Tibshirani");
    result.setValue(Field.YEAR, "1998");
    result.setValue(Field.TITLE,
      "Additive Logistic Regression: a Statistical View of Boosting");
    result.setValue(Field.ADDRESS, "Stanford University");
    result.setValue(Field.PS, "http://www-stat.stanford.edu/~jhf/ftp/boost.ps");

    return result;
  }

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  protected String defaultClassifierString() {

    return "weka.classifiers.trees.DecisionStump";
  }

  /**
   * Select only instances with weights that contribute to the specified
   * quantile of the weight distribution
   *
   * @param data the input instances
   * @param quantile the specified quantile eg 0.9 to select 90% of the weight
   *          mass
   * @return the selected instances
   */
  protected Instances selectWeightQuantile(Instances data, double quantile) {

    int numInstances = data.numInstances();
    Instances trainData = new Instances(data, numInstances);
    double[] weights = new double[numInstances];

    double sumOfWeights = 0;
    for (int i = 0; i < numInstances; i++) {
      weights[i] = data.instance(i).weight();
      sumOfWeights += weights[i];
    }
    double weightMassToSelect = sumOfWeights * quantile;
    int[] sortedIndices = Utils.sort(weights);

    // Select the instances
    sumOfWeights = 0;
    for (int i = numInstances - 1; i >= 0; i--) {
      Instance instance = (Instance) data.instance(sortedIndices[i]).copy();
      trainData.add(instance);
      sumOfWeights += weights[sortedIndices[i]];
      if ((sumOfWeights > weightMassToSelect) && (i > 0)
        && (weights[sortedIndices[i]] != weights[sortedIndices[i - 1]])) {
        break;
      }
    }
    if (m_Debug) {
      System.err.println("Selected " + trainData.numInstances() + " out of "
        + numInstances);
    }
    return trainData;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(5);

    newVector.addElement(new Option(
      "\tUse resampling instead of reweighting for boosting.", "Q", 0, "-Q"));
    newVector.addElement(new Option(
      "\tPercentage of weight mass to base training on.\n"
        + "\t(default 100, reduce to around 90 speed up)", "P", 1,
      "-P <percent>"));
    newVector.addElement(new Option(
      "\tThreshold on the improvement of the likelihood.\n"
        + "\t(default -Double.MAX_VALUE)", "L", 1, "-L <num>"));
    newVector.addElement(new Option("\tShrinkage parameter.\n"
      + "\t(default 1)", "H", 1, "-H <num>"));
    newVector.addElement(new Option("\tZ max threshold for responses."
      + "\n\t(default 3)", "Z", 1, "-Z <num>"));
    newVector.addElement(new Option("\t" + poolSizeTipText() + " (default 1)",
      "O", 1, "-O <int>"));
    newVector.addElement(new Option("\t" + numThreadsTipText() + "\n"
      + "\t(default 1)", "E", 1, "-E <int>"));

    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -Q
   *  Use resampling instead of reweighting for boosting.
   * </pre>
   * 
   * <pre>
   * -P &lt;percent&gt;
   *  Percentage of weight mass to base training on.
   *  (default 100, reduce to around 90 speed up)
   * </pre>
   * 
   * <pre>
   * -L &lt;num&gt;
   *  Threshold on the improvement of the likelihood.
   *  (default -Double.MAX_VALUE)
   * </pre>
   * 
   * <pre>
   * -H &lt;num&gt;
   *  Shrinkage parameter.
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -Z &lt;num&gt;
   *  Z max threshold for responses.
   *  (default 3)
   * </pre>
   * 
   * <pre>
   * -O &lt;int&gt;
   *  The size of the thread pool, for example, the number of cores in the CPU. (default 1)
   * </pre>
   * 
   * <pre>
   * -E &lt;int&gt;
   *  The number of threads to use for batch prediction, which should be &gt;= size of thread pool.
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -I &lt;num&gt;
   *  Number of iterations.
   *  (default 10)
   * </pre>
   * 
   * <pre>
   * -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.trees.DecisionStump)
   * </pre>
   * 
   * <pre>
   * -output-debug-info
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console
   * </pre>
   * 
   * <pre>
   * -do-not-check-capabilities
   *  If set, classifier capabilities are not checked before classifier is built
   *  (use with caution).
   * </pre>
   * 
   * <pre>
   * Options specific to classifier weka.classifiers.trees.DecisionStump:
   * </pre>
   * 
   * <pre>
   * -output-debug-info
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console
   * </pre>
   * 
   * <pre>
   * -do-not-check-capabilities
   *  If set, classifier capabilities are not checked before classifier is built
   *  (use with caution).
   * </pre>
   * 
   * <!-- options-end -->
   *
   * Options after -- are passed to the designated learner.
   * <p>
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String thresholdString = Utils.getOption('P', options);
    if (thresholdString.length() != 0) {
      setWeightThreshold(Integer.parseInt(thresholdString));
    } else {
      setWeightThreshold(100);
    }

    String precisionString = Utils.getOption('L', options);
    if (precisionString.length() != 0) {
      setLikelihoodThreshold(new Double(precisionString).doubleValue());
    } else {
      setLikelihoodThreshold(-Double.MAX_VALUE);
    }

    String shrinkageString = Utils.getOption('H', options);
    if (shrinkageString.length() != 0) {
      setShrinkage(new Double(shrinkageString).doubleValue());
    } else {
      setShrinkage(1.0);
    }

    String zString = Utils.getOption('Z', options);
    if (zString.length() > 0) {
      setZMax(Double.parseDouble(zString));
    }

    setUseResampling(Utils.getFlag('Q', options));
    if (m_UseResampling && (thresholdString.length() != 0)) {
      throw new Exception("Weight pruning with resampling" + "not allowed.");
    }
    String PoolSize = Utils.getOption('O', options);
    if (PoolSize.length() != 0) {
      setPoolSize(Integer.parseInt(PoolSize));
    } else {
      setPoolSize(1);
    }
    String NumThreads = Utils.getOption('E', options);
    if (NumThreads.length() != 0) {
      setNumThreads(Integer.parseInt(NumThreads));
    } else {
      setNumThreads(1);
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    if (getUseResampling()) {
      options.add("-Q");
    } else {
      options.add("-P");
      options.add("" + getWeightThreshold());
    }
    options.add("-L");
    options.add("" + getLikelihoodThreshold());
    options.add("-H");
    options.add("" + getShrinkage());
    options.add("-Z");
    options.add("" + getZMax());

    options.add("-O");
    options.add("" + getPoolSize());

    options.add("-E");
    options.add("" + getNumThreads());

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String ZMaxTipText() {
    return "Z max threshold for responses";
  }

  /**
   * Set the Z max threshold on the responses
   * 
   * @param zMax the threshold to use
   */
  public void setZMax(double zMax) {
    m_zMax = zMax;
  }

  /**
   * Get the Z max threshold on the responses
   * 
   * @return the threshold to use
   */
  public double getZMax() {
    return m_zMax;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String shrinkageTipText() {
    return "Shrinkage parameter (use small value like 0.1 to reduce "
      + "overfitting).";
  }

  /**
   * Get the value of Shrinkage.
   *
   * @return Value of Shrinkage.
   */
  public double getShrinkage() {

    return m_Shrinkage;
  }

  /**
   * Set the value of Shrinkage.
   *
   * @param newShrinkage Value to assign to Shrinkage.
   */
  public void setShrinkage(double newShrinkage) {

    m_Shrinkage = newShrinkage;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String likelihoodThresholdTipText() {
    return "Threshold on improvement in likelihood.";
  }

  /**
   * Get the value of Precision.
   *
   * @return Value of Precision.
   */
  public double getLikelihoodThreshold() {

    return m_Precision;
  }

  /**
   * Set the value of Precision.
   *
   * @param newPrecision Value to assign to Precision.
   */
  public void setLikelihoodThreshold(double newPrecision) {

    m_Precision = newPrecision;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String useResamplingTipText() {
    return "Whether resampling is used instead of reweighting.";
  }

  /**
   * Set resampling mode
   *
   * @param r true if resampling should be done
   */
  public void setUseResampling(boolean r) {

    m_UseResampling = r;
  }

  /**
   * Get whether resampling is turned on
   *
   * @return true if resampling output is on
   */
  public boolean getUseResampling() {

    return m_UseResampling;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String weightThresholdTipText() {
    return "Weight threshold for weight pruning (reduce to 90 "
      + "for speeding up learning process).";
  }

  /**
   * Set weight thresholding
   *
   * @param threshold the percentage of weight mass used for training
   */
  public void setWeightThreshold(int threshold) {

    m_WeightThreshold = threshold;
  }

  /**
   * Get the degree of weight thresholding
   *
   * @return the percentage of weight mass used for training
   */
  public int getWeightThreshold() {

    return m_WeightThreshold;
  }

  /**
   * @return a string to describe the option
   */
  public String numThreadsTipText() {

    return "The number of threads to use for batch prediction, which should be >= size of thread pool.";
  }

  /**
   * Gets the number of threads.
   */
  public int getNumThreads() {

    return m_numThreads;
  }

  /**
   * Sets the number of threads
   */
  public void setNumThreads(int nT) {

    m_numThreads = nT;
  }

  /**
   * @return a string to describe the option
   */
  public String poolSizeTipText() {

    return "The size of the thread pool, for example, the number of cores in the CPU.";
  }

  /**
   * Gets the number of threads.
   */
  public int getPoolSize() {

    return m_poolSize;
  }

  /**
   * Sets the number of threads
   */
  public void setPoolSize(int nT) {

    m_poolSize = nT;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    result.enable(Capability.NOMINAL_CLASS);

    return result;
  }

  /**
   * Method used to build the classifier.
   */
  public void buildClassifier(Instances data) throws Exception {

    // Initialize classifier
    initializeClassifier(data);

    // For the given number of iterations
    while (next()) {
    }
    ;

    // Clean up
    done();
  }

  /**
   * Builds the boosted classifier
   * 
   * @param data the data to train the classifier with
   * @throws Exception if building fails, e.g., can't handle data
   */
  public void initializeClassifier(Instances data) throws Exception {

    m_RandomInstance = new Random(m_Seed);
    int classIndex = data.classIndex();

    if (m_Classifier == null) {
      throw new Exception("A base classifier has not been specified!");
    }

    if (!(m_Classifier instanceof WeightedInstancesHandler) && !m_UseResampling) {
      m_UseResampling = true;
    }

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    if (m_Debug) {
      System.err.println("Creating copy of the training data");
    }

    // remove instances with missing class
    m_data = new Instances(data);
    m_data.deleteWithMissingClass();

    // only class? -> build ZeroR model
    if (m_data.numAttributes() == 1) {
      System.err
        .println("Cannot build model (only class attribute present in data!), "
          + "using ZeroR model instead!");
      m_ZeroR = new weka.classifiers.rules.ZeroR();
      m_ZeroR.buildClassifier(m_data);
      return;
    } else {
      m_ZeroR = null;
    }

    m_NumClasses = m_data.numClasses();
    m_ClassAttribute = m_data.classAttribute();

    // Create the base classifiers
    if (m_Debug) {
      System.err.println("Creating base classifiers");
    }
    m_Classifiers = new ArrayList<Classifier[]>();

    // Build classifier on all the data
    int numInstances = m_data.numInstances();
    m_trainFs = new double[numInstances][m_NumClasses];
    m_trainYs = new double[numInstances][m_NumClasses];
    for (int j = 0; j < m_NumClasses; j++) {
      for (int i = 0, k = 0; i < numInstances; i++, k++) {
        m_trainYs[i][j] =
          (m_data.instance(k).classValue() == j) ? 1.0 - m_Offset
            : 0.0 + (m_Offset / (double) m_NumClasses);
      }
    }

    // Make class numeric
    m_data.setClassIndex(-1);
    m_data.deleteAttributeAt(classIndex);
    m_data.insertAttributeAt(new Attribute("'pseudo class'"), classIndex);
    m_data.setClassIndex(classIndex);
    m_NumericClassData = new Instances(m_data, 0);

    // Perform iterations
    m_probs = initialProbs(numInstances);
    m_logLikelihood = logLikelihood(m_trainYs, m_probs);
    m_NumGenerated = 0;
    if (m_Debug) {
      System.err.println("Avg. log-likelihood: " + m_logLikelihood);
    }
    m_sumOfWeights = m_data.sumOfWeights();
  }

  /**
   * Perform another iteration of boosting.
   */
  public boolean next() throws Exception {

    if (m_NumGenerated >= m_NumIterations) {
      return false;
    }

    // Do we only have a ZeroR model
    if (m_ZeroR != null) {
      return false;
    }

    double previousLoglikelihood = m_logLikelihood;
    performIteration(m_trainYs, m_trainFs, m_probs, m_data, m_sumOfWeights);
    m_logLikelihood = logLikelihood(m_trainYs, m_probs);
    if (m_Debug) {
      System.err.println("Avg. log-likelihood: " + m_logLikelihood);
    }
    if (Math.abs(previousLoglikelihood - m_logLikelihood) < m_Precision) {
      return false;
    }
    return true;
  }

  /**
   * Clean up after boosting.
   */
  public void done() {

    m_trainYs = m_trainFs = m_probs = null;
    m_data = null;
  }

  /**
   * Gets the intial class probabilities.
   * 
   * @param numInstances the number of instances
   * @return the initial class probabilities
   */
  private double[][] initialProbs(int numInstances) {

    double[][] probs = new double[numInstances][m_NumClasses];
    for (int i = 0; i < numInstances; i++) {
      for (int j = 0; j < m_NumClasses; j++) {
        probs[i][j] = 1.0 / m_NumClasses;
      }
    }
    return probs;
  }

  /**
   * Computes loglikelihood given class values and estimated probablities.
   * 
   * @param trainYs class values
   * @param probs estimated probabilities
   * @return the computed loglikelihood
   */
  private double logLikelihood(double[][] trainYs, double[][] probs) {

    double logLikelihood = 0;
    for (int i = 0; i < trainYs.length; i++) {
      for (int j = 0; j < m_NumClasses; j++) {
        if (trainYs[i][j] == 1.0 - m_Offset) {
          logLikelihood -= Math.log(probs[i][j]);
        }
      }
    }
    return logLikelihood / (double) trainYs.length;
  }

  /**
   * Performs one boosting iteration.
   * 
   * @param trainYs class values
   * @param trainFs F scores
   * @param probs probabilities
   * @param data the data to run the iteration on
   * @param origSumOfWeights the original sum of weights
   * @throws Exception in case base classifiers run into problems
   */
  private void performIteration(double[][] trainYs, double[][] trainFs,
    double[][] probs, Instances data, double origSumOfWeights) throws Exception {

    if (m_Debug) {
      System.err.println("Training classifier " + (m_NumGenerated + 1));
    }

    // Make space for classifiers
    Classifier[] classifiers = new Classifier[m_NumClasses];

    // Build the new models
    for (int j = 0; j < m_NumClasses; j++) {
      if (m_Debug) {
        System.err.println("\t...for class " + (j + 1) + " ("
          + m_ClassAttribute.name() + "=" + m_ClassAttribute.value(j) + ")");
      }

      // Make copy because we want to save the weights
      Instances boostData = new Instances(data);

      // Set instance pseudoclass and weights
      for (int i = 0; i < probs.length; i++) {

        // Compute response and weight
        double p = probs[i][j];
        double z, actual = trainYs[i][j];
        if (actual == 1 - m_Offset) {
          z = 1.0 / p;
          if (z > m_zMax) { // threshold
            z = m_zMax;
          }
        } else {
          z = -1.0 / (1.0 - p);
          if (z < -m_zMax) { // threshold
            z = -m_zMax;
          }
        }
        double w = (actual - p) / z;

        // Set values for instance
        Instance current = boostData.instance(i);
        current.setValue(boostData.classIndex(), z);
        current.setWeight(current.weight() * w);
      }

      // Scale the weights (helps with some base learners)
      double sumOfWeights = boostData.sumOfWeights();
      double scalingFactor = (double) origSumOfWeights / sumOfWeights;
      for (int i = 0; i < probs.length; i++) {
        Instance current = boostData.instance(i);
        current.setWeight(current.weight() * scalingFactor);
      }

      // Select instances to train the classifier on
      Instances trainData = boostData;
      if (m_WeightThreshold < 100) {
        trainData =
          selectWeightQuantile(boostData, (double) m_WeightThreshold / 100);
      } else {
        if (m_UseResampling) {
          double[] weights = new double[boostData.numInstances()];
          for (int kk = 0; kk < weights.length; kk++) {
            weights[kk] = boostData.instance(kk).weight();
          }
          trainData = boostData.resampleWithWeights(m_RandomInstance, weights);
        }
      }

      // Build the classifier
      classifiers[j] = AbstractClassifier.makeCopy(m_Classifier);
      classifiers[j].buildClassifier(trainData);
      if (m_NumClasses == 2) {
        break; // Don't actually need to build the other model in the two-class
               // case
      }
    }
    m_Classifiers.add(classifiers);

    // Evaluate / increment trainFs from the classifier
    for (int i = 0; i < trainFs.length; i++) {
      double[] pred = new double[m_NumClasses];
      double predSum = 0;
      for (int j = 0; j < m_NumClasses; j++) {
        double tempPred =
          m_Shrinkage * classifiers[j].classifyInstance(data.instance(i));
        if (Utils.isMissingValue(tempPred)) {
          throw new UnassignedClassException(
            "LogitBoost: base learner predicted missing value.");
        }
        pred[j] = tempPred;
        if (m_NumClasses == 2) {
          pred[1] = -tempPred; // Can treat 2 classes as special case
          break;
        }
        predSum += pred[j];
      }
      predSum /= m_NumClasses;
      for (int j = 0; j < m_NumClasses; j++) {
        trainFs[i][j] +=
          (pred[j] - predSum) * (m_NumClasses - 1) / m_NumClasses;
      }
    }
    m_NumGenerated++;

    // Compute the current probability estimates
    for (int i = 0; i < trainYs.length; i++) {
      probs[i] = probs(trainFs[i]);
    }
  }

  /**
   * Returns the array of classifiers that have been built.
   * 
   * @return the built classifiers
   */
  public Classifier[][] classifiers() {

    return m_Classifiers.toArray(new Classifier[0][0]);
  }

  /**
   * Computes probabilities from F scores
   * 
   * @param Fs the F scores
   * @return the computed probabilities
   */
  private double[] probs(double[] Fs) {

    double maxF = -Double.MAX_VALUE;
    for (int i = 0; i < Fs.length; i++) {
      if (Fs[i] > maxF) {
        maxF = Fs[i];
      }
    }
    double sum = 0;
    double[] probs = new double[Fs.length];
    for (int i = 0; i < Fs.length; i++) {
      probs[i] = Math.exp(Fs[i] - maxF);
      sum += probs[i];
    }
    Utils.normalize(probs, sum);
    return probs;
  }

  /**
   * Tool tip text
   * 
   * @return tool tip text for this property
   */
  public String batchSizeTipText() {
    return "Batch size option is not used in LogitBoost";
  }

  /**
   * Dummy method to satisfy BatchPredictor interface.
   */
  public void setBatchSize(String i) {
  }

  /**
   * Dummy method to satisfy BatchPredictor interface.
   */
  public String getBatchSize() {
    return "";
  }

  /**
   * Performs efficient batch predcition
   *
   * @return true, as LogitBoost can perform efficient batch prediction
   */
  @Override
  public boolean implementsMoreEfficientBatchPrediction() {
    return true;
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @throws Exception if instance could not be classified successfully
   */
  public double[] distributionForInstance(Instance inst) throws Exception {

    // default model?
    if (m_ZeroR != null) {
      return m_ZeroR.distributionForInstance(inst);
    }

    double[] Fs = new double[m_NumClasses];
    double[] pred = new double[m_NumClasses];
    Instance instance = (Instance) inst.copy();
    instance.setDataset(m_NumericClassData);
    for (int i = 0; i < m_NumGenerated; i++) {
      double predSum = 0;
      for (int j = 0; j < m_NumClasses; j++) {
        double tempPred =
          m_Shrinkage * m_Classifiers.get(i)[j].classifyInstance(instance);
        if (Utils.isMissingValue(tempPred)) {
          throw new UnassignedClassException(
            "LogitBoost: base learner predicted missing value.");
        }
        pred[j] = tempPred;
        if (m_NumClasses == 2) {
          pred[1] = -tempPred; // Can treat 2 classes as special case
          break;
        }
        predSum += pred[j];
      }
      predSum /= m_NumClasses;
      for (int j = 0; j < m_NumClasses; j++) {
        Fs[j] += (pred[j] - predSum) * (m_NumClasses - 1) / m_NumClasses;
      }
    }

    return probs(Fs);
  }

  /**
   * Calculates the class membership probabilities for the given test instances.
   * Uses multi-threading if requested.
   *
   * @param insts the instances to be classified
   * @return predicted class probability distributions
   * @throws Exception if instances could not be classified successfully
   */
  public double[][] distributionsForInstances(Instances insts) throws Exception {

    // default model?
    if (m_ZeroR != null) {
      double[][] preds = new double[insts.numInstances()][];
      for (int i = 0; i < preds.length; i++) {
        preds[i] = m_ZeroR.distributionForInstance(insts.instance(i));
      }
      return preds;
    }

    final Instances numericClassInsts = new Instances(m_NumericClassData);
    for (int i = 0; i < insts.numInstances(); i++) {
      numericClassInsts.add(insts.instance(i));
    }

    // Start thread pool
    ExecutorService pool = Executors.newFixedThreadPool(m_poolSize);

    double[][] Fs = new double[insts.numInstances()][m_NumClasses];

    // Set up result set, and chunk size
    final int chunksize = m_NumGenerated / m_numThreads;
    Set<Future<double[][]>> results = new HashSet<Future<double[][]>>();

    // For each thread
    for (int j = 0; j < m_numThreads; j++) {

      // Determine batch to be processed
      final int lo = j * chunksize;
      final int hi = (j < m_numThreads - 1) ? (lo + chunksize) : m_NumGenerated;

      // Create and submit new job, where each instance in batch is processed
      Future<double[][]> futureT = pool.submit(new Callable<double[][]>() {
        @Override
        public double[][] call() throws Exception {
          double[][] localFs =
            new double[numericClassInsts.numInstances()][m_NumClasses];
          for (int k = 0; k < numericClassInsts.numInstances(); k++) {
            Instance instance = numericClassInsts.instance(k);
            for (int i = lo; i < hi; i++) {
              double predSum = 0;
              double[] pred = new double[m_NumClasses];
              for (int j = 0; j < m_NumClasses; j++) {
                double tempPred =
                  m_Shrinkage
                    * m_Classifiers.get(i)[j].classifyInstance(instance);
                if (Utils.isMissingValue(tempPred)) {
                  throw new UnassignedClassException(
                    "LogitBoost: base learner predicted missing value.");
                }
                pred[j] = tempPred;
                if (m_NumClasses == 2) {
                  pred[1] = -tempPred; // Can treat 2 classes as special case
                  break;
                }
                predSum += pred[j];
              }
              predSum /= m_NumClasses;
              for (int j = 0; j < m_NumClasses; j++) {
                localFs[k][j] +=
                  (pred[j] - predSum) * (m_NumClasses - 1) / m_NumClasses;
              }
            }
          }
          return localFs;
        }
      });
      results.add(futureT);
    }

    // Incorporate predictions
    try {
      for (Future<double[][]> futureT : results) {
        double[][] f = futureT.get();
        for (int j = 0; j < Fs.length; j++) {
          for (int i = 0; i < Fs[j].length; i++) {
            Fs[j][i] += f[j][i];
          }
        }
      }
    } catch (Exception e) {
      System.out.println("Predictions could not be generated.");
      e.printStackTrace();
    }

    pool.shutdown();

    double[][] preds = new double[insts.numInstances()][];
    for (int i = 0; i < preds.length; i++) {
      preds[i] = probs(Fs[i]);
    }
    return preds;
  }

  /**
   * Returns the boosted model as Java source code.
   *
   * @param className the classname in the generated code
   * @return the tree as Java source code
   * @throws Exception if something goes wrong
   */
  public String toSource(String className) throws Exception {

    if (m_NumGenerated == 0) {
      throw new Exception("No model built yet");
    }
    if (!(m_Classifiers.get(0)[0] instanceof Sourcable)) {
      throw new Exception("Base learner " + m_Classifier.getClass().getName()
        + " is not Sourcable");
    }

    StringBuffer text = new StringBuffer("class ");
    text.append(className).append(" {\n\n");
    text.append("  private static double RtoP(double []R, int j) {\n"
      + "    double Rcenter = 0;\n"
      + "    for (int i = 0; i < R.length; i++) {\n"
      + "      Rcenter += R[i];\n" + "    }\n" + "    Rcenter /= R.length;\n"
      + "    double Rsum = 0;\n" + "    for (int i = 0; i < R.length; i++) {\n"
      + "      Rsum += Math.exp(R[i] - Rcenter);\n" + "    }\n"
      + "    return Math.exp(R[j]) / Rsum;\n" + "  }\n\n");

    text.append("  public static double classify(Object[] i) {\n"
      + "    double [] d = distribution(i);\n" + "    double maxV = d[0];\n"
      + "    int maxI = 0;\n" + "    for (int j = 1; j < " + m_NumClasses
      + "; j++) {\n" + "      if (d[j] > maxV) { maxV = d[j]; maxI = j; }\n"
      + "    }\n    return (double) maxI;\n  }\n\n");

    text.append("  public static double [] distribution(Object [] i) {\n");
    text.append("    double [] Fs = new double [" + m_NumClasses + "];\n");
    text.append("    double [] Fi = new double [" + m_NumClasses + "];\n");
    text.append("    double Fsum;\n");
    for (int i = 0; i < m_NumGenerated; i++) {
      text.append("    Fsum = 0;\n");
      for (int j = 0; j < m_NumClasses; j++) {
        text.append("    Fi[" + j + "] = " + className + '_' + j + '_' + i
          + ".classify(i); Fsum += Fi[" + j + "];\n");
        if (m_NumClasses == 2) {
          text.append("    Fi[1] = -Fi[0];\n"); // 2-class case is special
          break;
        }
      }
      text.append("    Fsum /= " + m_NumClasses + ";\n");
      text.append("    for (int j = 0; j < " + m_NumClasses + "; j++) {");
      text.append(" Fs[j] += (Fi[j] - Fsum) * " + (m_NumClasses - 1) + " / "
        + m_NumClasses + "; }\n");
    }

    text.append("    double [] dist = new double [" + m_NumClasses + "];\n"
      + "    for (int j = 0; j < " + m_NumClasses + "; j++) {\n"
      + "      dist[j] = RtoP(Fs, j);\n" + "    }\n    return dist;\n");
    text.append("  }\n}\n");

    for (int i = 0; i < m_Classifiers.get(0).length; i++) {
      for (int j = 0; j < m_Classifiers.size(); j++) {
        text.append(((Sourcable) m_Classifiers.get(j)[i]).toSource(
          className + '_' + i + '_' + j));
      }
      if (m_NumClasses == 2) {
        break; // Only need one classifier per iteration in this case
      }
    }
    return text.toString();
  }

  /**
   * Returns description of the boosted classifier.
   *
   * @return description of the boosted classifier as a string
   */
  public String toString() {

    // only ZeroR model?
    if (m_ZeroR != null) {
      StringBuffer buf = new StringBuffer();
      buf.append(this.getClass().getName().replaceAll(".*\\.", "") + "\n");
      buf.append(this.getClass().getName().replaceAll(".*\\.", "")
        .replaceAll(".", "=")
        + "\n\n");
      buf
        .append(
          "Warning: No model could be built, hence ZeroR model is used:\n\n");
      buf.append(m_ZeroR.toString());
      return buf.toString();
    }

    StringBuffer text = new StringBuffer();

    if (m_NumGenerated == 0) {
      text.append("LogitBoost: No model built yet.");
      // text.append(m_Classifiers[0].toString()+"\n");
    } else {
      text.append("LogitBoost: Base classifiers and their weights: \n");
      for (int i = 0; i < m_NumGenerated; i++) {
        text.append("\nIteration " + (i + 1));
        for (int j = 0; j < m_NumClasses; j++) {
          text.append("\n\tClass " + (j + 1) + " (" + m_ClassAttribute.name()
            + "=" + m_ClassAttribute.value(j) + ")\n\n"
            + m_Classifiers.get(i)[j].toString() + "\n");
          if (m_NumClasses == 2) {
            text.append("Two-class case: second classifier predicts "
              + "additive inverse of first classifier and "
              + "is not explicitly computed.\n\n");
            break;
          }
        }
      }
      text.append("Number of performed iterations: " + m_NumGenerated + "\n");
    }

    return text.toString();
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11958 $");
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String[] argv) {
    runClassifier(new LogitBoost(), argv);
  }
}
