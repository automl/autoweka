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
 *    AdaBoostM1.java
 *    Copyright (C) 1999-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.RandomizableIteratedSingleClassifierEnhancer;
import weka.classifiers.Sourcable;
import weka.classifiers.IterativeClassifier;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Randomizable;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

/**
 * <!-- globalinfo-start --> Class for boosting a nominal class classifier using
 * the Adaboost M1 method. Only nominal class problems can be tackled. Often
 * dramatically improves performance, but sometimes overfits.<br/>
 * <br/>
 * For more information, see<br/>
 * <br/>
 * Yoav Freund, Robert E. Schapire: Experiments with a new boosting algorithm.
 * In: Thirteenth International Conference on Machine Learning, San Francisco,
 * 148-156, 1996.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;inproceedings{Freund1996,
 *    address = {San Francisco},
 *    author = {Yoav Freund and Robert E. Schapire},
 *    booktitle = {Thirteenth International Conference on Machine Learning},
 *    pages = {148-156},
 *    publisher = {Morgan Kaufmann},
 *    title = {Experiments with a new boosting algorithm},
 *    year = {1996}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -P &lt;num&gt;
 *  Percentage of weight mass to base training on.
 *  (default 100, reduce to around 90 speed up)
 * </pre>
 * 
 * <pre>
 * -Q
 *  Use resampling for boosting.
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
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <pre>
 * -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.DecisionStump)
 * </pre>
 * 
 * <pre>
 * Options specific to classifier weka.classifiers.trees.DecisionStump:
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * Options after -- are passed to the designated classifier.
 * <p>
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 10969 $
 */
public class AdaBoostM1 extends RandomizableIteratedSingleClassifierEnhancer
  implements WeightedInstancesHandler, Sourcable, TechnicalInformationHandler, IterativeClassifier {

  /** for serialization */
  static final long serialVersionUID = -1178107808933117974L;

  /** Max num iterations tried to find classifier with non-zero error. */
  private static int MAX_NUM_RESAMPLING_ITERATIONS = 10;

  /** Array for storing the weights for the votes. */
  protected double[] m_Betas;

  /** The number of successfully generated base classifiers. */
  protected int m_NumIterationsPerformed;

  /** Weight Threshold. The percentage of weight mass used in training */
  protected int m_WeightThreshold = 100;

  /** Use boosting with reweighting? */
  protected boolean m_UseResampling;

  /** The number of classes */
  protected int m_NumClasses;

  /** a ZeroR model in case no model can be built from the data */
  protected Classifier m_ZeroR;

  /** The (weighted) training data */
  protected Instances m_TrainingData;

  /** Random number generator to be used for resampling */
  protected Random m_RandomInstance;

  /**
   * Constructor.
   */
  public AdaBoostM1() {

    m_Classifier = new weka.classifiers.trees.DecisionStump();
  }

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {

    return "Class for boosting a nominal class classifier using the Adaboost "
      + "M1 method. Only nominal class problems can be tackled. Often "
      + "dramatically improves performance, but sometimes overfits.\n\n"
      + "For more information, see\n\n" + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Yoav Freund and Robert E. Schapire");
    result.setValue(Field.TITLE, "Experiments with a new boosting algorithm");
    result.setValue(Field.BOOKTITLE,
      "Thirteenth International Conference on Machine Learning");
    result.setValue(Field.YEAR, "1996");
    result.setValue(Field.PAGES, "148-156");
    result.setValue(Field.PUBLISHER, "Morgan Kaufmann");
    result.setValue(Field.ADDRESS, "San Francisco");

    return result;
  }

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  @Override
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
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>();

    newVector.addElement(new Option(
      "\tPercentage of weight mass to base training on.\n"
        + "\t(default 100, reduce to around 90 speed up)", "P", 1, "-P <num>"));

    newVector.addElement(new Option("\tUse resampling for boosting.", "Q", 0,
      "-Q"));

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
   * -P &lt;num&gt;
   *  Percentage of weight mass to base training on.
   *  (default 100, reduce to around 90 speed up)
   * </pre>
   * 
   * <pre>
   * -Q
   *  Use resampling for boosting.
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
   * -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console
   * </pre>
   * 
   * <pre>
   * -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.trees.DecisionStump)
   * </pre>
   * 
   * <pre>
   * Options specific to classifier weka.classifiers.trees.DecisionStump:
   * </pre>
   * 
   * <pre>
   * -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * Options after -- are passed to the designated classifier.
   * <p>
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String thresholdString = Utils.getOption('P', options);
    if (thresholdString.length() != 0) {
      setWeightThreshold(Integer.parseInt(thresholdString));
    } else {
      setWeightThreshold(100);
    }

    setUseResampling(Utils.getFlag('Q', options));

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    if (getUseResampling()) {
      result.add("-Q");
    }

    result.add("-P");
    result.add("" + getWeightThreshold());

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String weightThresholdTipText() {
    return "Weight threshold for weight pruning.";
  }

  /**
   * Set weight threshold
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
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    if (super.getCapabilities().handles(Capability.NOMINAL_CLASS)) {
      result.enable(Capability.NOMINAL_CLASS);
    }
    if (super.getCapabilities().handles(Capability.BINARY_CLASS)) {
      result.enable(Capability.BINARY_CLASS);
    }

    return result;
  }

  /**
   * Method used to build the classifier.
   */
  public void buildClassifier(Instances data) throws Exception {

    // Initialize classifier
    initializeClassifier(data);

    // Perform boosting iterations
    while (next()) {};

    // Clean up
    done();
  }

  /**
   * Initialize the classifier.
   * 
   * @param data the training data to be used for generating the boosted
   *          classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  public void initializeClassifier(Instances data) throws Exception {

    super.buildClassifier(data);

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();

    m_ZeroR = new weka.classifiers.rules.ZeroR();
    m_ZeroR.buildClassifier(data);

    m_NumClasses = data.numClasses();
    m_Betas = new double[m_Classifiers.length];
    m_NumIterationsPerformed = 0;
    m_TrainingData = new Instances(data);

    m_RandomInstance = new Random(m_Seed);

    if ((m_UseResampling)
        || (!(m_Classifier instanceof WeightedInstancesHandler))) {

      // Normalize weights so that they sum to one and can be used as sampling probabilities
      double sumProbs = m_TrainingData.sumOfWeights();
      for (int i = 0; i < m_TrainingData.numInstances(); i++) {
        m_TrainingData.instance(i).setWeight(m_TrainingData.instance(i).weight() / sumProbs);
      }
    }
  }

  /**
   * Perform the next boosting iteration.
   *
   * @throws Exception if an unforeseen problem occurs
   */
  public boolean next() throws Exception {

    // Have we reached the maximum?
    if (m_NumIterationsPerformed >= m_NumIterations) {
      return false;
    }

    // only class? -> just use ZeroR model
    if (m_TrainingData.numAttributes() == 1) {
      return false;
    }

    if (m_Debug) {
      System.err.println("Training classifier "
                         + (m_NumIterationsPerformed + 1));
    }

    // Select instances to train the classifier on
    Instances trainData = null;
    if (m_WeightThreshold < 100) {
      trainData = selectWeightQuantile(m_TrainingData,
                                       (double) m_WeightThreshold / 100);
    } else {
      trainData = new Instances(m_TrainingData);
    }

    double epsilon = 0;
    if ((m_UseResampling)
        || (!(m_Classifier instanceof WeightedInstancesHandler))) {

      // Resample
      int resamplingIterations = 0;
      double[] weights = new double[trainData.numInstances()];
      for (int i = 0; i < weights.length; i++) {
        weights[i] = trainData.instance(i).weight();
      }
      do {
        Instances sample = trainData.resampleWithWeights(m_RandomInstance, weights);
        
        // Build and evaluate classifier
        m_Classifiers[m_NumIterationsPerformed].buildClassifier(sample);
        Evaluation evaluation = new Evaluation(m_TrainingData); 
        evaluation.evaluateModel(m_Classifiers[m_NumIterationsPerformed],
                                 m_TrainingData);
        epsilon = evaluation.errorRate();
        resamplingIterations++;
      } while (Utils.eq(epsilon, 0)
               && (resamplingIterations < MAX_NUM_RESAMPLING_ITERATIONS));
    } else {

      // Build the classifier
      if (m_Classifiers[m_NumIterationsPerformed] instanceof Randomizable) {
        ((Randomizable) m_Classifiers[m_NumIterationsPerformed])
          .setSeed(m_RandomInstance.nextInt());
      }
      m_Classifiers[m_NumIterationsPerformed].buildClassifier(trainData);

      // Evaluate the classifier
      Evaluation evaluation = new Evaluation(m_TrainingData); // Does this need to be a copy
      evaluation.evaluateModel(m_Classifiers[m_NumIterationsPerformed],
                               m_TrainingData);
      epsilon = evaluation.errorRate();
    }

    // Stop if error too big or 0
    if (Utils.grOrEq(epsilon, 0.5) || Utils.eq(epsilon, 0)) {
      if (m_NumIterationsPerformed == 0) {
        m_NumIterationsPerformed = 1; // If we're the first we have to use it
      }
      return false;
    }

    // Determine the weight to assign to this model
    double reweight = (1 - epsilon) / epsilon;
    m_Betas[m_NumIterationsPerformed] = Math.log(reweight);
    if (m_Debug) {
      System.err.println("\terror rate = " + epsilon + "  beta = "
                         + m_Betas[m_NumIterationsPerformed]);
    }
    
    // Update instance weights
    setWeights(m_TrainingData, reweight);

    // Model has been built successfully
    m_NumIterationsPerformed++;
    return true;
  }

  /**
   * Clean up after boosting.
   */
  public void done() {
    
    m_TrainingData = null;
    
    // Can discard ZeroR model if we don't need it anymore
    if (m_NumIterationsPerformed > 0) {
      m_ZeroR = null;
    }
  }

  /**
   * Sets the weights for the next iteration.
   * 
   * @param training the training instances
   * @param reweight the reweighting factor
   * @throws Exception if something goes wrong
   */
  protected void setWeights(Instances training, double reweight)
    throws Exception {

    double oldSumOfWeights, newSumOfWeights;

    oldSumOfWeights = training.sumOfWeights();
    Enumeration<Instance> enu = training.enumerateInstances();
    while (enu.hasMoreElements()) {
      Instance instance = enu.nextElement();
      if (!Utils.eq(
        m_Classifiers[m_NumIterationsPerformed].classifyInstance(instance),
        instance.classValue())) {
        instance.setWeight(instance.weight() * reweight);
      }
    }

    // Renormalize weights
    newSumOfWeights = training.sumOfWeights();
    enu = training.enumerateInstances();
    while (enu.hasMoreElements()) {
      Instance instance = enu.nextElement();
      instance.setWeight(instance.weight() * oldSumOfWeights / newSumOfWeights);
    }
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   * 
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @throws Exception if instance could not be classified successfully
   */
  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {

    // default model?
    if (m_NumIterationsPerformed == 0) {
      return m_ZeroR.distributionForInstance(instance);
    }

    if (m_NumIterationsPerformed == 0) {
      throw new Exception("No model built");
    }
    double[] sums = new double[instance.numClasses()];

    if (m_NumIterationsPerformed == 1) {
      return m_Classifiers[0].distributionForInstance(instance);
    } else {
      for (int i = 0; i < m_NumIterationsPerformed; i++) {
        sums[(int) m_Classifiers[i].classifyInstance(instance)] += m_Betas[i];
      }
      return Utils.logs2probs(sums);
    }
  }

  /**
   * Returns the boosted model as Java source code.
   * 
   * @param className the classname of the generated class
   * @return the tree as Java source code
   * @throws Exception if something goes wrong
   */
  @Override
  public String toSource(String className) throws Exception {

    if (m_NumIterationsPerformed == 0) {
      throw new Exception("No model built yet");
    }
    if (!(m_Classifiers[0] instanceof Sourcable)) {
      throw new Exception("Base learner " + m_Classifier.getClass().getName()
        + " is not Sourcable");
    }

    StringBuffer text = new StringBuffer("class ");
    text.append(className).append(" {\n\n");

    text.append("  public static double classify(Object[] i) {\n");

    if (m_NumIterationsPerformed == 1) {
      text.append("    return " + className + "_0.classify(i);\n");
    } else {
      text.append("    double [] sums = new double [" + m_NumClasses + "];\n");
      for (int i = 0; i < m_NumIterationsPerformed; i++) {
        text.append("    sums[(int) " + className + '_' + i
          + ".classify(i)] += " + m_Betas[i] + ";\n");
      }
      text.append("    double maxV = sums[0];\n" + "    int maxI = 0;\n"
        + "    for (int j = 1; j < " + m_NumClasses + "; j++) {\n"
        + "      if (sums[j] > maxV) { maxV = sums[j]; maxI = j; }\n"
        + "    }\n    return (double) maxI;\n");
    }
    text.append("  }\n}\n");

    for (int i = 0; i < m_Classifiers.length; i++) {
      text.append(((Sourcable) m_Classifiers[i]).toSource(className + '_' + i));
    }
    return text.toString();
  }

  /**
   * Returns description of the boosted classifier.
   * 
   * @return description of the boosted classifier as a string
   */
  @Override
  public String toString() {

    // only ZeroR model?
    if (m_NumIterationsPerformed == 0) {
      StringBuffer buf = new StringBuffer();
      if (m_ZeroR == null) {
        buf.append("AdaBoostM1: No model built yet.\n");
      } else {
        buf.append(this.getClass().getName().replaceAll(".*\\.", "") + "\n");
        buf.append(this.getClass().getName().replaceAll(".*\\.", "")
                   .replaceAll(".", "=")
                   + "\n\n");
        buf
          .append("Warning: No model could be built, hence ZeroR model is used:\n\n");
        buf.append(m_ZeroR.toString());
      }
      return buf.toString();
    }

    StringBuffer text = new StringBuffer();
    if (m_NumIterationsPerformed == 1) {
      text.append("AdaBoostM1: No boosting possible, one classifier used!\n");
      text.append(m_Classifiers[0].toString() + "\n");
    } else {
      text.append("AdaBoostM1: Base classifiers and their weights: \n\n");
      for (int i = 0; i < m_NumIterationsPerformed; i++) {
        text.append(m_Classifiers[i].toString() + "\n\n");
        text.append("Weight: " + Utils.roundDouble(m_Betas[i], 2) + "\n\n");
      }
      text.append("Number of performed Iterations: " + m_NumIterationsPerformed
        + "\n");
    }

    return text.toString();
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10969 $");
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv the options
   */
  public static void main(String[] argv) {
    runClassifier(new AdaBoostM1(), argv);
  }
}
