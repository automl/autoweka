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
 *    WrapperSubsetEval.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.attributeSelection;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.AbstractEvaluationMetric;
import weka.classifiers.evaluation.InformationRetrievalEvaluationMetric;
import weka.classifiers.rules.ZeroR;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.util.BitSet;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * WrapperSubsetEval:<br>
 * <br>
 * Evaluates attribute sets by using a learning scheme. Cross validation is used
 * to estimate the accuracy of the learning scheme for a set of attributes.<br>
 * <br>
 * For more information see:<br>
 * <br>
 * <p>Ron Kohavi, George H. John (1997). Wrappers for feature subset selection.
 * Artificial Intelligence. 97(1-2):273-324.
 * </p>
 <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;article{Kohavi1997,
 *    author = {Ron Kohavi and George H. John},
 *    journal = {Artificial Intelligence},
 *    note = {Special issue on relevance},
 *    number = {1-2},
 *    pages = {273-324},
 *    title = {Wrappers for feature subset selection},
 *    volume = {97},
 *    year = {1997},
 *    ISSN = {0004-3702}
 * }
 * </pre>
 * <p>
 <!-- technical-bibtex-end -->
 * 
 <!-- options-start -->
 * Valid options are:
 * </p>
 * 
 * <pre>
 * -B &lt;base learner&gt;
 *  class name of base learner to use for  accuracy estimation.
 *  Place any classifier options LAST on the command line
 *  following a "--". eg.:
 *   -B weka.classifiers.bayes.NaiveBayes ... -- -K
 *  (default: weka.classifiers.rules.ZeroR)
 * </pre>
 * 
 * <pre>
 * -F &lt;num&gt;
 *  number of cross validation folds to use for estimating accuracy.
 *  (default=5)
 * </pre>
 * 
 * <pre>
 * -R &lt;seed&gt;
 *  Seed for cross validation accuracy testimation.
 *  (default = 1)
 * </pre>
 * 
 * <pre>
 * -T &lt;num&gt;
 *  threshold by which to execute another cross validation
 *  (standard deviation---expressed as a percentage of the mean).
 *  (default: 0.01 (1%))
 * </pre>
 * 
 * <pre>
 * -E &lt;acc | rmse | mae | f-meas | auc | auprc&gt;
 *  Performance evaluation measure to use for selecting attributes.
 *  (Default = accuracy for discrete class and rmse for numeric class)
 * </pre>
 * 
 * <pre>
 * -IRclass &lt;label | index&gt;
 *  Optional class value (label or 1-based index) to use in conjunction with
 *  IR statistics (f-meas, auc or auprc). Omitting this option will use
 *  the class-weighted average.
 * </pre>
 * 
 * <pre>
 * Options specific to scheme weka.classifiers.rules.ZeroR:
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 <!-- options-end -->
 * 
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 12170 $
 */
public class WrapperSubsetEval extends ASEvaluation
  implements SubsetEvaluator, OptionHandler, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -4573057658746728675L;

  /** training instances */
  private Instances m_trainInstances;
  /** class index */
  private int m_classIndex;
  /** number of attributes in the training data */
  private int m_numAttribs;
  /** holds an evaluation object */
  private Evaluation m_Evaluation;
  /** holds the base classifier object */
  private Classifier m_BaseClassifier;
  /** number of folds to use for cross validation */
  private int m_folds;
  /** random number seed */
  private int m_seed;
  /**
   * the threshold by which to do further cross validations when estimating the
   * accuracy of a subset
   */
  private double m_threshold;

  public static final int EVAL_DEFAULT = 1;
  public static final int EVAL_ACCURACY = 2;
  public static final int EVAL_RMSE = 3;
  public static final int EVAL_MAE = 4;
  public static final int EVAL_FMEASURE = 5;
  public static final int EVAL_AUC = 6;
  public static final int EVAL_AUPRC = 7;
  public static final int EVAL_CORRELATION = 8;
  public static final int EVAL_PLUGIN = 9;

  /**
   * Small subclass of Tag to store info about a plugin metric
   */
  protected static class PluginTag extends Tag {
    private static final long serialVersionUID = -6978438858413428382L;

    /** The metric object itself */
    protected AbstractEvaluationMetric m_metric;

    /** The particular statistic from the metric that this tag pertains to */
    protected String m_statisticName;

    /**
     * Constructor
     *
     * @param metric the metric object
     * @param statisticName the particular statistic that this tag pertains to
     */
    public PluginTag(int ID, AbstractEvaluationMetric metric,
      String statisticName) {
      super(ID, statisticName, statisticName);
      m_metric = metric;
      m_statisticName = statisticName;
    }

    /**
     * Get the name of the metric represented by this tag
     *
     * @return the name of the metric
     */
    public String getMetricName() {
      return m_metric.getMetricName();
    }

    /**
     * Get the name of the statistic that this tag pertains to
     *
     * @return the name of the statistic
     */
    public String getStatisticName() {
      return m_statisticName;
    }

    /**
     * Get the actual metric object
     *
     * @return the metric object
     */
    public AbstractEvaluationMetric getMetric() {
      return m_metric;
    }
  }

  /** Holds all tags for metrics */
  public static final Tag[] TAGS_EVALUATION;

  /**
   * If >= 0, and an IR metric is being used, then evaluate with respect to this
   * class value (0-based index)
   */
  protected int m_IRClassVal = -1;

  /** User supplied option for IR class value (either name or 1-based index) */
  protected String m_IRClassValS = "";

  protected static List<AbstractEvaluationMetric> PLUGIN_METRICS =
    AbstractEvaluationMetric.getPluginMetrics();

  static {
    int totalPluginCount = 0;
    if (PLUGIN_METRICS != null) {
      for (AbstractEvaluationMetric m : PLUGIN_METRICS) {
        totalPluginCount += m.getStatisticNames().size();
      }
    }

    TAGS_EVALUATION = new Tag[8 + totalPluginCount];
    TAGS_EVALUATION[0] = new Tag(EVAL_DEFAULT, "default",
      "Default: accuracy (discrete class); RMSE (numeric class)");
    TAGS_EVALUATION[1] =
      new Tag(EVAL_ACCURACY, "acc", "Accuracy (discrete class only)");
    TAGS_EVALUATION[2] = new Tag(EVAL_RMSE, "rmse",
      "RMSE (of the class probabilities for discrete class)");
    TAGS_EVALUATION[3] = new Tag(EVAL_MAE, "mae",
      "MAE (of the class probabilities for discrete class)");
    TAGS_EVALUATION[4] =
      new Tag(EVAL_FMEASURE, "f-meas", "F-measure (discrete class only)");
    TAGS_EVALUATION[5] = new Tag(EVAL_AUC, "auc",
      "AUC (area under the ROC curve - discrete class only)");
    TAGS_EVALUATION[6] = new Tag(EVAL_AUPRC, "auprc",
      "AUPRC (area under the precision-recall curve - discrete class only)");
    TAGS_EVALUATION[7] = new Tag(EVAL_CORRELATION, "corr-coeff",
      "Correlation coefficient - numeric class only");

    if (PLUGIN_METRICS != null) {
      int index = 8;
      for (AbstractEvaluationMetric m : PLUGIN_METRICS) {
        for (String stat : m.getStatisticNames()) {
          TAGS_EVALUATION[index++] = new PluginTag(index + 1, m, stat);
        }
      }
    }
  }

  /** The evaluation measure to use */
  protected Tag m_evaluationMeasure = TAGS_EVALUATION[0];

  /**
   * Returns a string describing this attribute evaluator
   * 
   * @return a description of the evaluator suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "WrapperSubsetEval:\n\n"
      + "Evaluates attribute sets by using a learning scheme. Cross "
      + "validation is used to estimate the accuracy of the learning "
      + "scheme for a set of attributes.\n\n" + "For more information see:\n\n"
      + getTechnicalInformation().toString();
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

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Ron Kohavi and George H. John");
    result.setValue(Field.YEAR, "1997");
    result.setValue(Field.TITLE, "Wrappers for feature subset selection");
    result.setValue(Field.JOURNAL, "Artificial Intelligence");
    result.setValue(Field.VOLUME, "97");
    result.setValue(Field.NUMBER, "1-2");
    result.setValue(Field.PAGES, "273-324");
    result.setValue(Field.NOTE, "Special issue on relevance");
    result.setValue(Field.ISSN, "0004-3702");

    return result;
  }

  /**
   * Constructor. Calls restOptions to set default options
   **/
  public WrapperSubsetEval() {
    resetOptions();
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   **/
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> newVector = new Vector<Option>(4);
    newVector.addElement(new Option(
      "\tclass name of base learner to use for \taccuracy estimation.\n"
        + "\tPlace any classifier options LAST on the command line\n"
        + "\tfollowing a \"--\". eg.:\n"
        + "\t\t-B weka.classifiers.bayes.NaiveBayes ... -- -K\n"
        + "\t(default: weka.classifiers.rules.ZeroR)",
      "B", 1, "-B <base learner>"));

    newVector.addElement(new Option(
      "\tnumber of cross validation folds to use for estimating accuracy.\n"
        + "\t(default=5)",
      "F", 1, "-F <num>"));

    newVector.addElement(new Option(
      "\tSeed for cross validation accuracy testimation.\n" + "\t(default = 1)",
      "R", 1, "-R <seed>"));

    newVector.addElement(
      new Option("\tthreshold by which to execute another cross validation\n"
        + "\t(standard deviation---expressed as a percentage of the mean).\n"
        + "\t(default: 0.01 (1%))", "T", 1, "-T <num>"));

    newVector
      .addElement(new Option(
        "\tPerformance evaluation measure to use for selecting attributes.\n"
          + "\t(Default = default: accuracy for discrete class and rmse for "
          + "numeric class)",
        "E", 1, "-E " + Tag.toOptionList(TAGS_EVALUATION)));

    newVector.addElement(new Option(
      "\tOptional class value (label or 1-based index) to use in conjunction with\n"
        + "\tIR statistics (f-meas, auc or auprc). Omitting this option will use\n"
        + "\tthe class-weighted average.",
      "IRclass", 1, "-IRclass <label | index>"));

    if ((m_BaseClassifier != null)
      && (m_BaseClassifier instanceof OptionHandler)) {
      newVector.addElement(new Option("", "", 0, "\nOptions specific to scheme "
        + m_BaseClassifier.getClass().getName() + ":"));
      newVector.addAll(
        Collections.list(((OptionHandler) m_BaseClassifier).listOptions()));
    }

    return newVector.elements();
  }

  /**
   * <p>Parses a given list of options.
   * </p>
   * 
   <!-- options-start -->
   *   Valid options are:
   * <br>
   * 
   * <pre>
   * -B &lt;base learner&gt;
   *  class name of base learner to use for  accuracy estimation.
   *  Place any classifier options LAST on the command line
   *  following a "--". eg.:
   *   -B weka.classifiers.bayes.NaiveBayes ... -- -K
   *  (default: weka.classifiers.rules.ZeroR)
   * </pre>
   * 
   * <pre>
   * -F &lt;num&gt;
   *  number of cross validation folds to use for estimating accuracy.
   *  (default=5)
   * </pre>
   * 
   * <pre>
   * -R &lt;seed&gt;
   *  Seed for cross validation accuracy testimation.
   *  (default = 1)
   * </pre>
   * 
   * <pre>
   * -T &lt;num&gt;
   *  threshold by which to execute another cross validation
   *  (standard deviation---expressed as a percentage of the mean).
   *  (default: 0.01 (1%))
   * </pre>
   * 
   * <pre>
   * -E &lt;acc | rmse | mae | f-meas | auc | auprc&gt;
   *  Performance evaluation measure to use for selecting attributes.
   *  (Default = accuracy for discrete class and rmse for numeric class)
   * </pre>
   * 
   * <pre>
   * -IRclass &lt;label | index&gt;
   *  Optional class value (label or 1-based index) to use in conjunction with
   *  IR statistics (f-meas, auc or auprc). Omitting this option will use
   *  the class-weighted average.
   * </pre>
   * 
   * <pre>
   * Options specific to scheme weka.classifiers.rules.ZeroR:
   * </pre>
   * 
   * <pre>
   * -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console
   * </pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String optionString;
    resetOptions();
    optionString = Utils.getOption('B', options);

    if (optionString.length() == 0) {
      optionString = ZeroR.class.getName();
    }
    setClassifier(AbstractClassifier.forName(optionString,
      Utils.partitionOptions(options)));
    optionString = Utils.getOption('F', options);

    if (optionString.length() != 0) {
      setFolds(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('R', options);
    if (optionString.length() != 0) {
      setSeed(Integer.parseInt(optionString));
    }

    // optionString = Utils.getOption('S',options);
    // if (optionString.length() != 0)
    // {
    // seed = Integer.parseInt(optionString);
    // }
    optionString = Utils.getOption('T', options);

    if (optionString.length() != 0) {
      Double temp;
      temp = Double.valueOf(optionString);
      setThreshold(temp.doubleValue());
    }

    optionString = Utils.getOption('E', options);
    if (optionString.length() != 0) {
      for (Tag t : TAGS_EVALUATION) {
        if (t.getIDStr().equalsIgnoreCase(optionString)) {
          setEvaluationMeasure(new SelectedTag(t.getIDStr(), TAGS_EVALUATION));
          break;
        }
      }
    }

    optionString = Utils.getOption("IRClass", options);
    if (optionString.length() > 0) {
      setIRClassValue(optionString);
    }
  }

  /**
   * Set the class value (label or index) to use with IR metric evaluation of
   * subsets. Leaving this unset will result in the class weighted average for
   * the IR metric being used.
   * 
   * @param val the class label or 1-based index of the class label to use when
   *          evaluating subsets with an IR metric
   */
  public void setIRClassValue(String val) {
    m_IRClassValS = val;
  }

  /**
   * Get the class value (label or index) to use with IR metric evaluation of
   * subsets. Leaving this unset will result in the class weighted average for
   * the IR metric being used.
   * 
   * @return the class label or 1-based index of the class label to use when
   *         evaluating subsets with an IR metric
   */
  public String getIRClassValue() {
    return m_IRClassValS;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String IRClassValueTipText() {
    return "The class label, or 1-based index of the class label, to use "
      + "when evaluating subsets with an IR metric (such as f-measure "
      + "or AUC. Leaving this unset will result in the class frequency "
      + "weighted average of the metric being used.";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String evaluationMeasureTipText() {
    return "The measure used to evaluate the performance of attribute combinations.";
  }

  /**
   * Gets the currently set performance evaluation measure used for selecting
   * attributes for the decision table
   * 
   * @return the performance evaluation measure
   */
  public SelectedTag getEvaluationMeasure() {
    return new SelectedTag(m_evaluationMeasure.getIDStr(), TAGS_EVALUATION);
  }

  /**
   * Sets the performance evaluation measure to use for selecting attributes for
   * the decision table
   * 
   * @param newMethod the new performance evaluation metric to use
   */
  public void setEvaluationMeasure(SelectedTag newMethod) {
    if (newMethod.getTags() == TAGS_EVALUATION) {
      m_evaluationMeasure = newMethod.getSelectedTag();
    }
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String thresholdTipText() {
    return "Repeat xval if stdev of mean exceeds this value.";
  }

  /**
   * Set the value of the threshold for repeating cross validation
   * 
   * @param t the value of the threshold
   */
  public void setThreshold(double t) {
    m_threshold = t;
  }

  /**
   * Get the value of the threshold
   * 
   * @return the threshold as a double
   */
  public double getThreshold() {
    return m_threshold;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String foldsTipText() {
    return "Number of xval folds to use when estimating subset accuracy.";
  }

  /**
   * Set the number of folds to use for accuracy estimation
   * 
   * @param f the number of folds
   */
  public void setFolds(int f) {
    m_folds = f;
  }

  /**
   * Get the number of folds used for accuracy estimation
   * 
   * @return the number of folds
   */
  public int getFolds() {
    return m_folds;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String seedTipText() {
    return "Seed to use for randomly generating xval splits.";
  }

  /**
   * Set the seed to use for cross validation
   * 
   * @param s the seed
   */
  public void setSeed(int s) {
    m_seed = s;
  }

  /**
   * Get the random number seed used for cross validation
   * 
   * @return the seed
   */
  public int getSeed() {
    return m_seed;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String classifierTipText() {
    return "Classifier to use for estimating the accuracy of subsets";
  }

  /**
   * Set the classifier to use for accuracy estimation
   * 
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier(Classifier newClassifier) {
    m_BaseClassifier = newClassifier;
  }

  /**
   * Get the classifier used as the base learner.
   * 
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier() {
    return m_BaseClassifier;
  }

  /**
   * Gets the current settings of WrapperSubsetEval.
   * 
   * @return an array of strings suitable for passing to setOptions()
   */
  @Override
  public String[] getOptions() {
    String[] classifierOptions = new String[0];

    if ((m_BaseClassifier != null)
      && (m_BaseClassifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler) m_BaseClassifier).getOptions();
    }

    String[] options = new String[13 + classifierOptions.length];
    int current = 0;

    if (getClassifier() != null) {
      options[current++] = "-B";
      options[current++] = getClassifier().getClass().getName();
    }

    options[current++] = "-F";
    options[current++] = "" + getFolds();
    options[current++] = "-T";
    options[current++] = "" + getThreshold();
    options[current++] = "-R";
    options[current++] = "" + getSeed();

    options[current++] = "-E";
    options[current++] = m_evaluationMeasure.getIDStr();

    if (m_IRClassValS != null && m_IRClassValS.length() > 0) {
      options[current++] = "-IRClass";
      options[current++] = m_IRClassValS;
    }

    options[current++] = "--";
    System.arraycopy(classifierOptions, 0, options, current,
      classifierOptions.length);
    current += classifierOptions.length;

    while (current < options.length) {
      options[current++] = "";
    }

    return options;
  }

  protected void resetOptions() {
    m_trainInstances = null;
    m_Evaluation = null;
    m_BaseClassifier = new ZeroR();
    m_folds = 5;
    m_seed = 1;
    m_threshold = 0.01;
  }

  /**
   * Returns the capabilities of this evaluator.
   * 
   * @return the capabilities of this evaluator
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result;

    if (getClassifier() == null) {
      result = super.getCapabilities();
      result.disableAll();
    } else {
      result = getClassifier().getCapabilities();
    }

    // set dependencies
    for (Capability cap : Capability.values()) {
      result.enableDependency(cap);
    }

    // adjustment for class based on selected evaluation metric
    result.disable(Capability.NUMERIC_CLASS);
    result.disable(Capability.DATE_CLASS);
    boolean pluginMetricNominalClass = false;
    if (m_evaluationMeasure.getID() >= EVAL_PLUGIN) {
      String metricName = ((PluginTag) m_evaluationMeasure).getMetricName();
      for (AbstractEvaluationMetric m : PLUGIN_METRICS) {
        if (m.getMetricName().equals(metricName)) {
          pluginMetricNominalClass = m.appliesToNominalClass();
          break;
        }
      }
    }
    if (m_evaluationMeasure.getID() != EVAL_ACCURACY
      && m_evaluationMeasure.getID() != EVAL_FMEASURE
      && m_evaluationMeasure.getID() != EVAL_AUC
      && m_evaluationMeasure.getID() != EVAL_AUPRC
      && !pluginMetricNominalClass) {
      result.enable(Capability.NUMERIC_CLASS);
      result.enable(Capability.DATE_CLASS);
    }

    result.setMinimumNumberInstances(getFolds());

    return result;
  }

  /**
   * Generates a attribute evaluator. Has to initialize all fields of the
   * evaluator that are not being set via options.
   * 
   * @param data set of instances serving as training data
   * @throws Exception if the evaluator has not been generated successfully
   */
  @Override
  public void buildEvaluator(Instances data) throws Exception {

    // can evaluator handle data?
    getCapabilities().testWithFail(data);

    m_trainInstances = data;
    m_classIndex = m_trainInstances.classIndex();
    m_numAttribs = m_trainInstances.numAttributes();

    if (m_IRClassValS != null && m_IRClassValS.length() > 0) {
      // try to parse as a number first
      try {
        m_IRClassVal = Integer.parseInt(m_IRClassValS);
        // make zero-based
        m_IRClassVal--;
      } catch (NumberFormatException e) {
        // now try as a named class label
        m_IRClassVal =
          m_trainInstances.classAttribute().indexOfValue(m_IRClassValS);
      }
    }
  }

  /**
   * Evaluates a subset of attributes
   * 
   * @param subset a bitset representing the attribute subset to be evaluated
   * @return the error rate
   * @throws Exception if the subset could not be evaluated
   */
  @Override
  public double evaluateSubset(BitSet subset) throws Exception {
    double evalMetric = 0;
    double[] repError = new double[5];
    int numAttributes = 0;
    int i, j;
    Random Rnd = new Random(m_seed);
    Remove delTransform = new Remove();
    delTransform.setInvertSelection(true);
    // copy the instances
    Instances trainCopy = new Instances(m_trainInstances);

    // count attributes set in the BitSet
    for (i = 0; i < m_numAttribs; i++) {
      if (subset.get(i)) {
        numAttributes++;
      }
    }

    // set up an array of attribute indexes for the filter (+1 for the class)
    int[] featArray = new int[numAttributes + 1];

    for (i = 0, j = 0; i < m_numAttribs; i++) {
      if (subset.get(i)) {
        featArray[j++] = i;
      }
    }

    featArray[j] = m_classIndex;
    delTransform.setAttributeIndicesArray(featArray);
    delTransform.setInputFormat(trainCopy);
    trainCopy = Filter.useFilter(trainCopy, delTransform);

    AbstractEvaluationMetric pluginMetric = null;
    String statName = null;
    String metricName = null;

    // max of 5 repetitions of cross validation
    for (i = 0; i < 5; i++) {
      m_Evaluation = new Evaluation(trainCopy);
      m_Evaluation.crossValidateModel(m_BaseClassifier, trainCopy, m_folds,
        Rnd);

      switch (m_evaluationMeasure.getID()) {
      case EVAL_DEFAULT:
        repError[i] = m_Evaluation.errorRate();
        break;
      case EVAL_ACCURACY:
        repError[i] = m_Evaluation.errorRate();
        break;
      case EVAL_RMSE:
        repError[i] = m_Evaluation.rootMeanSquaredError();
        break;
      case EVAL_MAE:
        repError[i] = m_Evaluation.meanAbsoluteError();
        break;
      case EVAL_FMEASURE:
        if (m_IRClassVal < 0) {
          repError[i] = m_Evaluation.weightedFMeasure();
        } else {
          repError[i] = m_Evaluation.fMeasure(m_IRClassVal);
        }
        break;
      case EVAL_AUC:
        if (m_IRClassVal < 0) {
          repError[i] = m_Evaluation.weightedAreaUnderROC();
        } else {
          repError[i] = m_Evaluation.areaUnderROC(m_IRClassVal);
        }
        break;
      case EVAL_AUPRC:
        if (m_IRClassVal < 0) {
          repError[i] = m_Evaluation.weightedAreaUnderPRC();
        } else {
          repError[i] = m_Evaluation.areaUnderPRC(m_IRClassVal);
        }
        break;
      case EVAL_CORRELATION:
        repError[i] = m_Evaluation.correlationCoefficient();
        break;
      default:
        if (m_evaluationMeasure.getID() >= EVAL_PLUGIN) {
          metricName = ((PluginTag) m_evaluationMeasure).getMetricName();
          statName = ((PluginTag) m_evaluationMeasure).getStatisticName();
          statName = ((PluginTag) m_evaluationMeasure).getStatisticName();
          pluginMetric = m_Evaluation.getPluginMetric(metricName);
          if (pluginMetric == null) {
            throw new Exception(
              "Metric  " + metricName + " does not seem to be " + "available");
          }
        }

        if (pluginMetric instanceof InformationRetrievalEvaluationMetric) {
          if (m_IRClassVal < 0) {
            repError[i] = ((InformationRetrievalEvaluationMetric) pluginMetric)
              .getClassWeightedAverageStatistic(statName);
          } else {
            repError[i] = ((InformationRetrievalEvaluationMetric) pluginMetric)
              .getStatistic(statName, m_IRClassVal);
          }
        } else {
          repError[i] = pluginMetric.getStatistic(statName);
        }
        break;
      }

      // check on the standard deviation
      if (!repeat(repError, i + 1)) {
        i++;
        break;
      }
    }

    for (j = 0; j < i; j++) {
      evalMetric += repError[j];
    }

    evalMetric /= i;
    m_Evaluation = null;

    switch (m_evaluationMeasure.getID()) {
    case EVAL_DEFAULT:
    case EVAL_ACCURACY:
    case EVAL_RMSE:
    case EVAL_MAE:
      if (m_trainInstances.classAttribute().isNominal()
        && (m_evaluationMeasure.getID() == EVAL_DEFAULT
          || m_evaluationMeasure.getID() == EVAL_ACCURACY)) {
        evalMetric = 1 - evalMetric;
      } else {
        evalMetric = -evalMetric; // maximize
      }
      break;
    default:
      if (pluginMetric != null
        && !pluginMetric.statisticIsMaximisable(statName)) {
        evalMetric = -evalMetric; // maximize
      }
    }

    return evalMetric;
  }

  /**
   * Returns a string describing the wrapper
   * 
   * @return the description as a string
   */
  @Override
  public String toString() {
    StringBuffer text = new StringBuffer();

    if (m_trainInstances == null) {
      text.append("\tWrapper subset evaluator has not been built yet\n");
    } else {
      text.append("\tWrapper Subset Evaluator\n");
      text.append(
        "\tLearning scheme: " + getClassifier().getClass().getName() + "\n");
      text.append("\tScheme options: ");
      String[] classifierOptions = new String[0];

      if (m_BaseClassifier instanceof OptionHandler) {
        classifierOptions = ((OptionHandler) m_BaseClassifier).getOptions();

        for (String classifierOption : classifierOptions) {
          text.append(classifierOption + " ");
        }
      }

      text.append("\n");
      String IRClassL = "";
      if (m_IRClassVal >= 0) {
        IRClassL = "(class value: "
          + m_trainInstances.classAttribute().value(m_IRClassVal) + ")";
      }
      switch (m_evaluationMeasure.getID()) {
      case EVAL_DEFAULT:
      case EVAL_ACCURACY:
        if (m_trainInstances.attribute(m_classIndex).isNumeric()) {
          text.append("\tSubset evaluation: RMSE\n");
        } else {
          text.append("\tSubset evaluation: classification accuracy\n");
        }
        break;
      case EVAL_RMSE:
        if (m_trainInstances.attribute(m_classIndex).isNumeric()) {
          text.append("\tSubset evaluation: RMSE\n");
        } else {
          text.append("\tSubset evaluation: RMSE (probability estimates)\n");
        }
        break;
      case EVAL_MAE:
        if (m_trainInstances.attribute(m_classIndex).isNumeric()) {
          text.append("\tSubset evaluation: MAE\n");
        } else {
          text.append("\tSubset evaluation: MAE (probability estimates)\n");
        }
        break;
      case EVAL_FMEASURE:
        text.append("\tSubset evaluation: F-measure "
          + (m_IRClassVal >= 0 ? IRClassL : "") + "\n");
        break;
      case EVAL_AUC:
        text.append("\tSubset evaluation: area under the ROC curve "
          + (m_IRClassVal >= 0 ? IRClassL : "") + "\n");
        break;
      case EVAL_AUPRC:
        text
          .append("\tSubset evaluation: area under the precision-recall curve "
            + (m_IRClassVal >= 0 ? IRClassL : "") + "\n");
        break;
      case EVAL_CORRELATION:
        text.append("\tSubset evaluation: correlation coefficient\n");
        break;
      default:
        text
          .append("\tSubset evaluation: " + m_evaluationMeasure.getReadable());
        if (((PluginTag) m_evaluationMeasure)
          .getMetric() instanceof InformationRetrievalEvaluationMetric) {
          text.append(" " + (m_IRClassVal > 0 ? IRClassL : ""));
        }
        text.append("\n");
        break;
      }

      text
        .append("\tNumber of folds for accuracy estimation: " + m_folds + "\n");
    }

    return text.toString();
  }

  /**
   * decides whether to do another repeat of cross validation. If the standard
   * deviation of the cross validations is greater than threshold% of the mean
   * (default 1%) then another repeat is done.
   * 
   * @param repError an array of cross validation results
   * @param entries the number of cross validations done so far
   * @return true if another cv is to be done
   */
  private boolean repeat(double[] repError, int entries) {
    int i;
    double mean = 0;
    double variance = 0;

    // setting a threshold less than zero allows for "manual" exploration
    // and prevents multiple xval for each subset
    if (m_threshold < 0) {
      return false;
    }

    if (entries == 1) {
      return true;
    }

    for (i = 0; i < entries; i++) {
      mean += repError[i];
    }

    mean /= entries;

    for (i = 0; i < entries; i++) {
      variance += ((repError[i] - mean) * (repError[i] - mean));
    }

    variance /= entries;

    if (variance > 0) {
      variance = Math.sqrt(variance);
    }

    if ((variance / mean) > m_threshold) {
      return true;
    }

    return false;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12170 $");
  }

  @Override
  public void clean() {
    m_trainInstances = new Instances(m_trainInstances, 0);
  }

  /**
   * Main method for testing this class.
   * 
   * @param args the options
   */
  public static void main(String[] args) {
    runEvaluator(new WrapperSubsetEval(), args);
  }
}
