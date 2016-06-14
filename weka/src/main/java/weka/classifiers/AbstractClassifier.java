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
 *    AbstractClassifier.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers;

import weka.core.Attribute;
import weka.core.BatchPredictor;
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.CapabilitiesIgnorer;
import weka.core.CommandlineRunnable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.SerializedObject;
import weka.core.Utils;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Abstract classifier. All schemes for numeric or nominal prediction in Weka
 * extend this class. Note that a classifier MUST either implement
 * distributionForInstance() or classifyInstance().
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 12701 $
 */
public abstract class AbstractClassifier implements Classifier, BatchPredictor,
  Cloneable, Serializable, OptionHandler, CapabilitiesHandler, RevisionHandler,
  CapabilitiesIgnorer, CommandlineRunnable {

  /** for serialization */
  private static final long serialVersionUID = 6502780192411755341L;

  /** Whether the classifier is run in debug mode. */
  protected boolean m_Debug = false;

  /** Whether capabilities should not be checked before classifier is built. */
  protected boolean m_DoNotCheckCapabilities = false;

  /**
   * The number of decimal places used when printing numbers in the model.
   */
  public static int NUM_DECIMAL_PLACES_DEFAULT = 2;
  protected int m_numDecimalPlaces = NUM_DECIMAL_PLACES_DEFAULT;

  /** Default preferred batch size for batch predictions */
  public static String BATCH_SIZE_DEFAULT = "100";
  protected String m_BatchSize = BATCH_SIZE_DEFAULT;

  /**
   * Creates a new instance of a classifier given it's class name and (optional)
   * arguments to pass to it's setOptions method. If the classifier implements
   * OptionHandler and the options parameter is non-null, the classifier will
   * have it's options set.
   *
   * @param classifierName the fully qualified class name of the classifier
   * @param options an array of options suitable for passing to setOptions. May
   *          be null.
   * @return the newly created classifier, ready for use.
   * @exception Exception if the classifier name is invalid, or the options
   *              supplied are not acceptable to the classifier
   */
  public static Classifier forName(String classifierName, String[] options)
    throws Exception {

    return ((AbstractClassifier) Utils.forName(Classifier.class, classifierName,
      options));
  }

  /**
   * Creates a deep copy of the given classifier using serialization.
   *
   * @param model the classifier to copy
   * @return a deep copy of the classifier
   * @exception Exception if an error occurs
   */
  public static Classifier makeCopy(Classifier model) throws Exception {

    return (Classifier) new SerializedObject(model).getObject();
  }

  /**
   * Creates a given number of deep copies of the given classifier using
   * serialization.
   *
   * @param model the classifier to copy
   * @param num the number of classifier copies to create.
   * @return an array of classifiers.
   * @exception Exception if an error occurs
   */
  public static Classifier[] makeCopies(Classifier model, int num)
    throws Exception {

    if (model == null) {
      throw new Exception("No model classifier set");
    }
    Classifier[] classifiers = new Classifier[num];
    SerializedObject so = new SerializedObject(model);
    for (int i = 0; i < classifiers.length; i++) {
      classifiers[i] = (Classifier) so.getObject();
    }
    return classifiers;
  }

  /**
   * runs the classifier instance with the given options.
   *
   * @param classifier the classifier to run
   * @param options the commandline options
   */
  public static void runClassifier(Classifier classifier, String[] options) {
    try {
      if (classifier instanceof CommandlineRunnable) {
        ((CommandlineRunnable)classifier).preExecution();
      }
      System.out.println(Evaluation.evaluateModel(classifier, options));
    } catch (Exception e) {
      if (((e.getMessage() != null)
        && (e.getMessage().indexOf("General options") == -1))
        || (e.getMessage() == null)) {
        e.printStackTrace();
      } else {
        System.err.println(e.getMessage());
      }
    }
    if (classifier instanceof CommandlineRunnable) {
      try {
        ((CommandlineRunnable) classifier).postExecution();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  /**
   * Classifies the given test instance. The instance has to belong to a dataset
   * when it's being classified. Note that a classifier MUST implement either
   * this or distributionForInstance().
   *
   * @param instance the instance to be classified
   * @return the predicted most likely class for the instance or
   *         Utils.missingValue() if no prediction is made
   * @exception Exception if an error occurred during the prediction
   */
  @Override
  public double classifyInstance(Instance instance) throws Exception {

    double[] dist = distributionForInstance(instance);
    if (dist == null) {
      throw new Exception("Null distribution predicted");
    }
    switch (instance.classAttribute().type()) {
    case Attribute.NOMINAL:
      double max = 0;
      int maxIndex = 0;

      for (int i = 0; i < dist.length; i++) {
        if (dist[i] > max) {
          maxIndex = i;
          max = dist[i];
        }
      }
      if (max > 0) {
        return maxIndex;
      } else {
        return Utils.missingValue();
      }
    case Attribute.NUMERIC:
    case Attribute.DATE:
      return dist[0];
    default:
      return Utils.missingValue();
    }
  }

  /**
   * Predicts the class memberships for a given instance. If an instance is
   * unclassified, the returned array elements must be all zero. If the class is
   * numeric, the array must consist of only one element, which contains the
   * predicted value. Note that a classifier MUST implement either this or
   * classifyInstance().
   *
   * @param instance the instance to be classified
   * @return an array containing the estimated membership probabilities of the
   *         test instance in each class or the numeric prediction
   * @exception Exception if distribution could not be computed successfully
   */
  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {

    double[] dist = new double[instance.numClasses()];
    switch (instance.classAttribute().type()) {
    case Attribute.NOMINAL:
      double classification = classifyInstance(instance);
      if (Utils.isMissingValue(classification)) {
        return dist;
      } else {
        dist[(int) classification] = 1.0;
      }
      return dist;
    case Attribute.NUMERIC:
    case Attribute.DATE:
      dist[0] = classifyInstance(instance);
      return dist;
    default:
      return dist;
    }
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = Option
      .listOptionsForClassHierarchy(this.getClass(), AbstractClassifier.class);

    newVector.addElement(new Option(
      "\tIf set, classifier is run in debug mode and\n"
        + "\tmay output additional info to the console",
      "output-debug-info", 0, "-output-debug-info"));
    newVector.addElement(new Option(
      "\tIf set, classifier capabilities are not checked before classifier is built\n"
        + "\t(use with caution).",
      "-do-not-check-capabilities", 0, "-do-not-check-capabilities"));
    newVector.addElement(new Option(
      "\tThe number of decimal places for the output of numbers in the model"
        + " (default " + m_numDecimalPlaces + ").",
      "num-decimal-places", 1, "-num-decimal-places"));
    newVector.addElement(new Option(
            "\tThe desired batch size for batch prediction " + " (default " + m_BatchSize + ").",
            "batch-size", 1, "-batch-size"));


    return newVector.elements();
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();
    for (String s : Option.getOptionsForHierarchy(this,
      AbstractClassifier.class)) {
      options.add(s);
    }

    if (getDebug()) {
      options.add("-output-debug-info");
    }
    if (getDoNotCheckCapabilities()) {
      options.add("-do-not-check-capabilities");
    }
    if (getNumDecimalPlaces() != NUM_DECIMAL_PLACES_DEFAULT) {
      options.add("-num-decimal-places");
      options.add("" + getNumDecimalPlaces());
    }
    if (!(getBatchSize().equals(BATCH_SIZE_DEFAULT))) {
      options.add("-batch-size");
      options.add("" + getBatchSize());
    }
    return options.toArray(new String[0]);
  }

  /**
   * Parses a given list of options. Valid options are:
   * <p>
   *
   * -D <br>
   * If set, classifier is run in debug mode and may output additional info to
   * the console.
   * <p>
   *
   * -do-not-check-capabilities <br>
   * If set, classifier capabilities are not checked before classifier is built
   * (use with caution).
   * <p>
   *
   * -num-decimal-laces <br>
   * The number of decimal places for the output of numbers in the model.
   * <p>
   *
   * -batch-size <br>
   * The desired batch size for batch prediction.
   * <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    Option.setOptionsForHierarchy(options, this, AbstractClassifier.class);
    setDebug(Utils.getFlag("output-debug-info", options));
    setDoNotCheckCapabilities(
      Utils.getFlag("do-not-check-capabilities", options));

    String optionString = Utils.getOption("num-decimal-places", options);
    if (optionString.length() != 0) {
      setNumDecimalPlaces((new Integer(optionString)).intValue());
    }
    optionString = Utils.getOption("batch-size", options);
    if (optionString.length() != 0) {
      setBatchSize(optionString);
    }
  }

  /**
   * Get whether debugging is turned on.
   * 
   * @return true if debugging output is on
   */
  public boolean getDebug() {

    return m_Debug;
  }

  /**
   * Set debugging mode.
   *
   * @param debug true if debug output should be printed
   */
  public void setDebug(boolean debug) {

    m_Debug = debug;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String debugTipText() {
    return "If set to true, classifier may output additional info to "
      + "the console.";
  }

  /**
   * Get whether capabilities checking is turned off.
   * 
   * @return true if capabilities checking is turned off.
   */
  @Override
  public boolean getDoNotCheckCapabilities() {

    return m_DoNotCheckCapabilities;
  }

  /**
   * Set whether not to check capabilities.
   *
   * @param doNotCheckCapabilities true if capabilities are not to be checked.
   */
  @Override
  public void setDoNotCheckCapabilities(boolean doNotCheckCapabilities) {

    m_DoNotCheckCapabilities = doNotCheckCapabilities;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String doNotCheckCapabilitiesTipText() {
    return "If set, classifier capabilities are not checked before classifier is built"
      + " (Use with caution to reduce runtime).";
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numDecimalPlacesTipText() {
    return "The number of decimal places to be used for the output of numbers in "
      + "the model.";
  }

  /**
   * Get the number of decimal places.
   */
  public int getNumDecimalPlaces() {
    return m_numDecimalPlaces;
  }

  /**
   * Set the number of decimal places.
   */
  public void setNumDecimalPlaces(int num) {
    m_numDecimalPlaces = num;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String batchSizeTipText() {
    return "The preferred number of instances to process if batch prediction is "
      + "being performed. More or fewer instances may be provided, but this gives "
      + "implementations a chance to specify a preferred batch size.";
  }

  /**
   * Set the preferred batch size for batch prediction.
   * 
   * @param size the batch size to use
   */
  @Override
  public void setBatchSize(String size) {
    m_BatchSize = size;
  }

  /**
   * Get the preferred batch size for batch prediction.
   *
   * @return the preferred batch size
   */
  @Override
  public String getBatchSize() {
    return m_BatchSize;
  }

  /**
   * Return true if this classifier can generate batch predictions in an
   * efficient manner. Default implementation here returns false. Subclasses to
   * override as appropriate.
   * 
   * @return true if this classifier can generate batch predictions in an
   *         efficient manner.
   */
  @Override
  public boolean implementsMoreEfficientBatchPrediction() {
    return false;
  }

  /**
   * Batch prediction method. This default implementation simply calls
   * distributionForInstance() for each instance in the batch. If subclasses can
   * produce batch predictions in a more efficient manner than this they should
   * override this method and also return true from
   * implementsMoreEfficientBatchPrediction()
   * 
   * @param batch the instances to get predictions for
   * @return an array of probability distributions, one for each instance in the
   *         batch
   * @throws Exception if a problem occurs.
   */
  @Override
  public double[][] distributionsForInstances(Instances batch)
    throws Exception {
    double[][] batchPreds = new double[batch.numInstances()][];
    for (int i = 0; i < batch.numInstances(); i++) {
      batchPreds[i] = distributionForInstance(batch.instance(i));
    }

    return batchPreds;
  }

  /**
   * Returns the Capabilities of this classifier. Maximally permissive
   * capabilities are allowed by default. Derived classifiers should override
   * this method and first disable all capabilities and then enable just those
   * capabilities that make sense for the scheme.
   *
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = new Capabilities(this);
    result.enableAll();

    return result;
  }

  /**
   * Returns the revision string.
   *
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12701 $");
  }

  /**
   * Perform any setup stuff that might need to happen before commandline
   * execution. Subclasses should override if they need to do something here
   *
   * @throws Exception if a problem occurs during setup
   */
  @Override
  public void preExecution() throws Exception {
  }

  /**
   * Execute the supplied object.
   *
   * @param toRun the object to execute
   * @param options any options to pass to the object
   * @throws Exception if the object is not of the expected type.
   */
  @Override
  public void run(Object toRun, String[] options) throws Exception {
    if (!(toRun instanceof Classifier)) {
      throw new IllegalArgumentException("Object to run is not a Classifier!");
    }
    runClassifier((Classifier) toRun, options);
  }

  /**
   * Perform any teardown stuff that might need to happen after execution.
   * Subclasses should override if they need to do something here
   *
   * @throws Exception if a problem occurs during teardown
   */
  @Override
  public void postExecution() throws Exception {
  }
}
