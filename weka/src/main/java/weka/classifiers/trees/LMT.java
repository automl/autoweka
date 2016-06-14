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
 *    LMT.java
 *    Copyright (C) 2003-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.trees.j48.C45ModelSelection;
import weka.classifiers.trees.j48.ModelSelection;
import weka.classifiers.trees.lmt.LMTNode;
import weka.classifiers.trees.lmt.ResidualModelSelection;
import weka.core.AdditionalMeasureProducer;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.supervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 * <!-- globalinfo-start --> Classifier for building 'logistic model trees',
 * which are classification trees with logistic regression functions at the
 * leaves. The algorithm can deal with binary and multi-class target variables,
 * numeric and nominal attributes and missing values.<br/>
 * <br/>
 * For more information see: <br/>
 * <br/>
 * Niels Landwehr, Mark Hall, Eibe Frank (2005). Logistic Model Trees. Machine
 * Learning. 95(1-2):161-205.<br/>
 * <br/>
 * Marc Sumner, Eibe Frank, Mark Hall: Speeding up Logistic Model Tree
 * Induction. In: 9th European Conference on Principles and Practice of
 * Knowledge Discovery in Databases, 675-683, 2005.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;article{Landwehr2005,
 *    author = {Niels Landwehr and Mark Hall and Eibe Frank},
 *    journal = {Machine Learning},
 *    number = {1-2},
 *    pages = {161-205},
 *    title = {Logistic Model Trees},
 *    volume = {95},
 *    year = {2005}
 * }
 * 
 * &#64;inproceedings{Sumner2005,
 *    author = {Marc Sumner and Eibe Frank and Mark Hall},
 *    booktitle = {9th European Conference on Principles and Practice of Knowledge Discovery in Databases},
 *    pages = {675-683},
 *    publisher = {Springer},
 *    title = {Speeding up Logistic Model Tree Induction},
 *    year = {2005}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -B
 *  Binary splits (convert nominal attributes to binary ones)
 * </pre>
 * 
 * <pre>
 * -R
 *  Split on residuals instead of class values
 * </pre>
 * 
 * <pre>
 * -C
 *  Use cross-validation for boosting at all nodes (i.e., disable heuristic)
 * </pre>
 * 
 * <pre>
 * -P
 *  Use error on probabilities instead of misclassification error for stopping criterion of LogitBoost.
 * </pre>
 * 
 * <pre>
 * -I &lt;numIterations&gt;
 *  Set fixed number of iterations for LogitBoost (instead of using cross-validation)
 * </pre>
 * 
 * <pre>
 * -M &lt;numInstances&gt;
 *  Set minimum number of instances at which a node can be split (default 15)
 * </pre>
 * 
 * <pre>
 * -W &lt;beta&gt;
 *  Set beta for weight trimming for LogitBoost. Set to 0 (default) for no weight trimming.
 * </pre>
 * 
 * <pre>
 * -A
 *  The AIC is used to choose the best iteration.
 * </pre>
 * 
 * <pre>
 * -doNotMakeSplitPointActualValue
 *  Do not make split point actual value.
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Niels Landwehr
 * @author Marc Sumner
 * @version $Revision: 11568 $
 */
public class LMT extends AbstractClassifier implements OptionHandler,
  AdditionalMeasureProducer, Drawable, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -1113212459618104943L;

  /** Filter to replace missing values */
  protected ReplaceMissingValues m_replaceMissing;

  /** Filter to replace nominal attributes */
  protected NominalToBinary m_nominalToBinary;

  /** root of the logistic model tree */
  protected LMTNode m_tree;

  /**
   * use heuristic that determines the number of LogitBoost iterations only once
   * in the beginning?
   */
  protected boolean m_fastRegression;

  /** convert nominal attributes to binary ? */
  protected boolean m_convertNominal;

  /** split on residuals? */
  protected boolean m_splitOnResiduals;

  /**
   * use error on probabilties instead of misclassification for stopping
   * criterion of LogitBoost?
   */
  protected boolean m_errorOnProbabilities;

  /** minimum number of instances at which a node is considered for splitting */
  protected int m_minNumInstances;

  /** if non-zero, use fixed number of iterations for LogitBoost */
  protected int m_numBoostingIterations;

  /**
   * Threshold for trimming weights. Instances with a weight lower than this (as
   * a percentage of total weights) are not included in the regression fit.
   **/
  protected double m_weightTrimBeta;

  /** If true, the AIC is used to choose the best LogitBoost iteration */
  private boolean m_useAIC = false;

  /** Do not relocate split point to actual data value */
  private boolean m_doNotMakeSplitPointActualValue;

  /**
   * Creates an instance of LMT with standard options
   */
  public LMT() {
    m_fastRegression = true;
    m_numBoostingIterations = -1;
    m_minNumInstances = 15;
    m_weightTrimBeta = 0;
    m_useAIC = false;
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Builds the classifier.
   * 
   * @param data the data to train with
   * @throws Exception if classifier can't be built successfully
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    Instances filteredData = new Instances(data);
    filteredData.deleteWithMissingClass();

    // replace missing values
    m_replaceMissing = new ReplaceMissingValues();
    m_replaceMissing.setInputFormat(filteredData);
    filteredData = Filter.useFilter(filteredData, m_replaceMissing);

    // possibly convert nominal attributes globally
    m_nominalToBinary = new NominalToBinary();
    m_nominalToBinary.setInputFormat(filteredData);
    if (m_convertNominal) {
      filteredData = Filter.useFilter(filteredData, m_nominalToBinary);
    }

    int minNumInstances = 2;

    // create ModelSelection object, either for splits on the residuals or for
    // splits on the class value
    ModelSelection modSelection;
    if (m_splitOnResiduals) {
      modSelection = new ResidualModelSelection(minNumInstances);
    } else {
      modSelection = new C45ModelSelection(minNumInstances, filteredData, true,
        m_doNotMakeSplitPointActualValue);
    }

    // create tree root
    m_tree = new LMTNode(modSelection, m_numBoostingIterations,
      m_fastRegression, m_errorOnProbabilities, m_minNumInstances,
      m_weightTrimBeta, m_useAIC, m_nominalToBinary, m_numDecimalPlaces);
    // build tree
    m_tree.buildClassifier(filteredData);

    if (modSelection instanceof C45ModelSelection) {
      ((C45ModelSelection) modSelection).cleanup();
    }
  }

  /**
   * Returns class probabilities for an instance.
   * 
   * @param instance the instance to compute the distribution for
   * @return the class probabilities
   * @throws Exception if distribution can't be computed successfully
   */
  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {

    // replace missing values
    m_replaceMissing.input(instance);
    instance = m_replaceMissing.output();

    // possibly convert nominal attributes
    if (m_convertNominal) {
      m_nominalToBinary.input(instance);
      instance = m_nominalToBinary.output();
    }

    return m_tree.distributionForInstance(instance);
  }

  /**
   * Classifies an instance.
   * 
   * @param instance the instance to classify
   * @return the classification
   * @throws Exception if instance can't be classified successfully
   */
  @Override
  public double classifyInstance(Instance instance) throws Exception {

    double maxProb = -1;
    int maxIndex = 0;

    // classify by maximum probability
    double[] probs = distributionForInstance(instance);
    for (int j = 0; j < instance.numClasses(); j++) {
      if (Utils.gr(probs[j], maxProb)) {
        maxIndex = j;
        maxProb = probs[j];
      }
    }
    return maxIndex;
  }

  /**
   * Returns a description of the classifier.
   * 
   * @return a string representation of the classifier
   */
  @Override
  public String toString() {
    if (m_tree != null) {
      return "Logistic model tree \n------------------\n" + m_tree.toString();
    } else {
      return "No tree build";
    }
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> newVector = new Vector<Option>(9);

    newVector.addElement(new Option(
      "\tBinary splits (convert nominal attributes to binary ones)", "B", 0,
      "-B"));

    newVector.addElement(new Option(
      "\tSplit on residuals instead of class values", "R", 0, "-R"));

    newVector
      .addElement(new Option(
        "\tUse cross-validation for boosting at all nodes (i.e., disable heuristic)",
        "C", 0, "-C"));

    newVector.addElement(new Option(
      "\tUse error on probabilities instead of misclassification error "
        + "for stopping criterion of LogitBoost.", "P", 0, "-P"));

    newVector.addElement(new Option(
      "\tSet fixed number of iterations for LogitBoost (instead of using "
        + "cross-validation)", "I", 1, "-I <numIterations>"));

    newVector
      .addElement(new Option(
        "\tSet minimum number of instances at which a node can be split (default 15)",
        "M", 1, "-M <numInstances>"));

    newVector
      .addElement(new Option(
        "\tSet beta for weight trimming for LogitBoost. Set to 0 (default) for no weight trimming.",
        "W", 1, "-W <beta>"));

    newVector.addElement(new Option(
      "\tThe AIC is used to choose the best iteration.", "A", 0, "-A"));
    newVector.addElement(new Option("\tDo not make split point actual value.",
      "-doNotMakeSplitPointActualValue", 0, "-doNotMakeSplitPointActualValue"));

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
   * -B
   *  Binary splits (convert nominal attributes to binary ones)
   * </pre>
   * 
   * <pre>
   * -R
   *  Split on residuals instead of class values
   * </pre>
   * 
   * <pre>
   * -C
   *  Use cross-validation for boosting at all nodes (i.e., disable heuristic)
   * </pre>
   * 
   * <pre>
   * -P
   *  Use error on probabilities instead of misclassification error for stopping criterion of LogitBoost.
   * </pre>
   * 
   * <pre>
   * -I &lt;numIterations&gt;
   *  Set fixed number of iterations for LogitBoost (instead of using cross-validation)
   * </pre>
   * 
   * <pre>
   * -M &lt;numInstances&gt;
   *  Set minimum number of instances at which a node can be split (default 15)
   * </pre>
   * 
   * <pre>
   * -W &lt;beta&gt;
   *  Set beta for weight trimming for LogitBoost. Set to 0 (default) for no weight trimming.
   * </pre>
   * 
   * <pre>
   * -A
   *  The AIC is used to choose the best iteration.
   * </pre>
   * 
   * <pre>
   * -doNotMakeSplitPointActualValue
   *  Do not make split point actual value.
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    setConvertNominal(Utils.getFlag('B', options));
    setSplitOnResiduals(Utils.getFlag('R', options));
    setFastRegression(!Utils.getFlag('C', options));
    setErrorOnProbabilities(Utils.getFlag('P', options));

    String optionString = Utils.getOption('I', options);
    if (optionString.length() != 0) {
      setNumBoostingIterations((new Integer(optionString)).intValue());
    }

    optionString = Utils.getOption('M', options);
    if (optionString.length() != 0) {
      setMinNumInstances((new Integer(optionString)).intValue());
    }

    optionString = Utils.getOption('W', options);
    if (optionString.length() != 0) {
      setWeightTrimBeta((new Double(optionString)).doubleValue());
    }

    setUseAIC(Utils.getFlag('A', options));
    m_doNotMakeSplitPointActualValue = Utils.getFlag(
      "doNotMakeSplitPointActualValue", options);

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

    Vector<String> options = new Vector<String>();

    if (getConvertNominal()) {
        options.add("-B");
    }

    if (getSplitOnResiduals()) {
        options.add("-R");
    }

    if (!getFastRegression()) {
        options.add("-C");
    }

    if (getErrorOnProbabilities()) {
        options.add("-P");
    }

    options.add("-I");
    options.add("" + getNumBoostingIterations());

    options.add("-M");
    options.add("" + getMinNumInstances());

    options.add("-W");
    options.add("" + getWeightTrimBeta());

    if (getUseAIC()) {
        options.add("-A");
    }

    if (m_doNotMakeSplitPointActualValue) {
        options.add("-doNotMakeSplitPointActualValue");
    }

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }

  /**
   * Get the value of weightTrimBeta.
   */
  public double getWeightTrimBeta() {
    return m_weightTrimBeta;
  }

  /**
   * Get the value of useAIC.
   * 
   * @return Value of useAIC.
   */
  public boolean getUseAIC() {
    return m_useAIC;
  }

  /**
   * Set the value of weightTrimBeta.
   */
  public void setWeightTrimBeta(double n) {
    m_weightTrimBeta = n;
  }

  /**
   * Set the value of useAIC.
   * 
   * @param c Value to assign to useAIC.
   */
  public void setUseAIC(boolean c) {
    m_useAIC = c;
  }

  /**
   * Get the value of convertNominal.
   * 
   * @return Value of convertNominal.
   */
  public boolean getConvertNominal() {
    return m_convertNominal;
  }

  /**
   * Get the value of splitOnResiduals.
   * 
   * @return Value of splitOnResiduals.
   */
  public boolean getSplitOnResiduals() {
    return m_splitOnResiduals;
  }

  /**
   * Get the value of fastRegression.
   * 
   * @return Value of fastRegression.
   */
  public boolean getFastRegression() {
    return m_fastRegression;
  }

  /**
   * Get the value of errorOnProbabilities.
   * 
   * @return Value of errorOnProbabilities.
   */
  public boolean getErrorOnProbabilities() {
    return m_errorOnProbabilities;
  }

  /**
   * Get the value of numBoostingIterations.
   * 
   * @return Value of numBoostingIterations.
   */
  public int getNumBoostingIterations() {
    return m_numBoostingIterations;
  }

  /**
   * Get the value of minNumInstances.
   * 
   * @return Value of minNumInstances.
   */
  public int getMinNumInstances() {
    return m_minNumInstances;
  }

  /**
   * Set the value of convertNominal.
   * 
   * @param c Value to assign to convertNominal.
   */
  public void setConvertNominal(boolean c) {
    m_convertNominal = c;
  }

  /**
   * Set the value of splitOnResiduals.
   * 
   * @param c Value to assign to splitOnResiduals.
   */
  public void setSplitOnResiduals(boolean c) {
    m_splitOnResiduals = c;
  }

  /**
   * Set the value of fastRegression.
   * 
   * @param c Value to assign to fastRegression.
   */
  public void setFastRegression(boolean c) {
    m_fastRegression = c;
  }

  /**
   * Set the value of errorOnProbabilities.
   * 
   * @param c Value to assign to errorOnProbabilities.
   */
  public void setErrorOnProbabilities(boolean c) {
    m_errorOnProbabilities = c;
  }

  /**
   * Set the value of numBoostingIterations.
   * 
   * @param c Value to assign to numBoostingIterations.
   */
  public void setNumBoostingIterations(int c) {
    m_numBoostingIterations = c;
  }

  /**
   * Set the value of minNumInstances.
   * 
   * @param c Value to assign to minNumInstances.
   */
  public void setMinNumInstances(int c) {
    m_minNumInstances = c;
  }

  /**
   * Returns the type of graph this classifier represents.
   * 
   * @return Drawable.TREE
   */
  @Override
  public int graphType() {
    return Drawable.TREE;
  }

  /**
   * Returns graph describing the tree.
   * 
   * @return the graph describing the tree
   * @throws Exception if graph can't be computed
   */
  @Override
  public String graph() throws Exception {

    return m_tree.graph();
  }

  /**
   * Returns the size of the tree
   * 
   * @return the size of the tree
   */
  public int measureTreeSize() {
    return m_tree.numNodes();
  }

  /**
   * Returns the number of leaves in the tree
   * 
   * @return the number of leaves in the tree
   */
  public int measureNumLeaves() {
    return m_tree.numLeaves();
  }

  /**
   * Returns an enumeration of the additional measure names
   * 
   * @return an enumeration of the measure names
   */
  @Override
  public Enumeration<String> enumerateMeasures() {
    Vector<String> newVector = new Vector<String>(2);
    newVector.addElement("measureTreeSize");
    newVector.addElement("measureNumLeaves");

    return newVector.elements();
  }

  /**
   * Returns the value of the named measure
   * 
   * @param additionalMeasureName the name of the measure to query for its value
   * @return the value of the named measure
   * @throws IllegalArgumentException if the named measure is not supported
   */
  @Override
  public double getMeasure(String additionalMeasureName) {
    if (additionalMeasureName.compareToIgnoreCase("measureTreeSize") == 0) {
      return measureTreeSize();
    } else if (additionalMeasureName.compareToIgnoreCase("measureNumLeaves") == 0) {
      return measureNumLeaves();
    } else {
      throw new IllegalArgumentException(additionalMeasureName
        + " not supported (LMT)");
    }
  }

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {
    return "Classifier for building 'logistic model trees', which are classification trees with "
      + "logistic regression functions at the leaves. The algorithm can deal with binary and multi-class "
      + "target variables, numeric and nominal attributes and missing values.\n\n"
      + "For more information see: \n\n" + getTechnicalInformation().toString();
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
    TechnicalInformation additional;

    result = new TechnicalInformation(Type.ARTICLE);
    result
      .setValue(Field.AUTHOR, "Niels Landwehr and Mark Hall and Eibe Frank");
    result.setValue(Field.TITLE, "Logistic Model Trees");
    result.setValue(Field.JOURNAL, "Machine Learning");
    result.setValue(Field.YEAR, "2005");
    result.setValue(Field.VOLUME, "95");
    result.setValue(Field.PAGES, "161-205");
    result.setValue(Field.NUMBER, "1-2");

    additional = result.add(Type.INPROCEEDINGS);
    additional.setValue(Field.AUTHOR,
      "Marc Sumner and Eibe Frank and Mark Hall");
    additional.setValue(Field.TITLE,
      "Speeding up Logistic Model Tree Induction");
    additional
      .setValue(
        Field.BOOKTITLE,
        "9th European Conference on Principles and Practice of Knowledge Discovery in Databases");
    additional.setValue(Field.YEAR, "2005");
    additional.setValue(Field.PAGES, "675-683");
    additional.setValue(Field.PUBLISHER, "Springer");

    return result;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String convertNominalTipText() {
    return "Convert all nominal attributes to binary ones before building the tree. "
      + "This means that all splits in the final tree will be binary.";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String splitOnResidualsTipText() {
    return "Set splitting criterion based on the residuals of LogitBoost. "
      + "There are two possible splitting criteria for LMT: the default is to use the C4.5 "
      + "splitting criterion that uses information gain on the class variable. The other splitting "
      + "criterion tries to improve the purity in the residuals produces when fitting the logistic "
      + "regression functions. The choice of the splitting criterion does not usually affect classification "
      + "accuracy much, but can produce different trees.";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String fastRegressionTipText() {
    return "Use heuristic that avoids cross-validating the number of Logit-Boost iterations at every node. "
      + "When fitting the logistic regression functions at a node, LMT has to determine the number of LogitBoost "
      + "iterations to run. Originally, this number was cross-validated at every node in the tree. "
      + "To save time, this heuristic cross-validates the number only once and then uses that number at every "
      + "node in the tree. Usually this does not decrease accuracy but improves runtime considerably.";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String errorOnProbabilitiesTipText() {
    return "Minimize error on probabilities instead of misclassification error when cross-validating the number "
      + "of LogitBoost iterations. When set, the number of LogitBoost iterations is chosen that minimizes "
      + "the root mean squared error instead of the misclassification error.";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numBoostingIterationsTipText() {
    return "Set a fixed number of iterations for LogitBoost. If >= 0, this sets a fixed number of LogitBoost "
      + "iterations that is used everywhere in the tree. If < 0, the number is cross-validated.";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String minNumInstancesTipText() {
    return "Set the minimum number of instances at which a node is considered for splitting. "
      + "The default value is 15.";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String weightTrimBetaTipText() {
    return "Set the beta value used for weight trimming in LogitBoost. "
      + "Only instances carrying (1 - beta)% of the weight from previous iteration "
      + "are used in the next iteration. Set to 0 for no weight trimming. "
      + "The default value is 0.";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String useAICTipText() {
    return "The AIC is used to determine when to stop LogitBoost iterations. "
      + "The default is not to use AIC.";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String doNotMakeSplitPointActualValueTipText() {
    return "If true, the split point is not relocated to an actual data value."
      + " This can yield substantial speed-ups for large datasets with numeric attributes.";
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numDecimalPlacesTipText() {
    return "The number of decimal places to be used for the output of coefficients.";
  }

  /**
   * Gets the value of doNotMakeSplitPointActualValue.
   * 
   * @return the value
   */
  public boolean getDoNotMakeSplitPointActualValue() {
    return m_doNotMakeSplitPointActualValue;
  }

  /**
   * Sets the value of doNotMakeSplitPointActualValue.
   * 
   * @param m_doNotMakeSplitPointActualValue the value to set
   */
  public void setDoNotMakeSplitPointActualValue(
    boolean m_doNotMakeSplitPointActualValue) {
    this.m_doNotMakeSplitPointActualValue = m_doNotMakeSplitPointActualValue;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11568 $");
  }

  /**
   * Main method for testing this class
   * 
   * @param argv the commandline options
   */
  public static void main(String[] argv) {
    runClassifier(new LMT(), argv);
  }
}
