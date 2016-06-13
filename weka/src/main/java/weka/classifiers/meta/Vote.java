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
 *    Vote.java
 *    Copyright (C) 2000-2012 University of Waikato
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Classifier;
import weka.classifiers.RandomizableMultipleClassifiersCombiner;
import weka.core.Aggregateable;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 * <!-- globalinfo-start --> Class for combining classifiers. Different
 * combinations of probability estimates for classification are available.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * Ludmila I. Kuncheva (2004). Combining Pattern Classifiers: Methods and
 * Algorithms. John Wiley and Sons, Inc..<br/>
 * <br/>
 * J. Kittler, M. Hatef, Robert P.W. Duin, J. Matas (1998). On combining
 * classifiers. IEEE Transactions on Pattern Analysis and Machine Intelligence.
 * 20(3):226-239.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -P &lt;path to serialized classifier&gt;
 *  Full path to serialized classifier to include.
 *  May be specified multiple times to include
 *  multiple serialized classifiers. Note: it does
 *  not make sense to use pre-built classifiers in
 *  a cross-validation.
 * </pre>
 * 
 * <pre>
 * -R &lt;AVG|PROD|MAJ|MIN|MAX|MED&gt;
 *  The combination rule to use
 *  (default: AVG)
 * </pre>
 * 
 * <pre>
 * -print
 *  Print the individual models in the output
 * </pre>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -B &lt;classifier specification&gt;
 *  Full class name of classifier to include, followed
 *  by scheme options. May be specified multiple times.
 *  (default: "weka.classifiers.rules.ZeroR")
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
 * Options specific to classifier weka.classifiers.rules.ZeroR:
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
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;book{Kuncheva2004,
 *    author = {Ludmila I. Kuncheva},
 *    publisher = {John Wiley and Sons, Inc.},
 *    title = {Combining Pattern Classifiers: Methods and Algorithms},
 *    year = {2004}
 * }
 * 
 * &#64;article{Kittler1998,
 *    author = {J. Kittler and M. Hatef and Robert P.W. Duin and J. Matas},
 *    journal = {IEEE Transactions on Pattern Analysis and Machine Intelligence},
 *    number = {3},
 *    pages = {226-239},
 *    title = {On combining classifiers},
 *    volume = {20},
 *    year = {1998}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * @author Alexander K. Seewald (alex@seewald.at)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Roberto Perdisci (roberto.perdisci@gmail.com)
 * @version $Revision: 12424 $
 */
public class Vote extends RandomizableMultipleClassifiersCombiner implements
  TechnicalInformationHandler, EnvironmentHandler, Aggregateable<Classifier> {

  /** for serialization */
  static final long serialVersionUID = -637891196294399624L;

  /** combination rule: Average of Probabilities */
  public static final int AVERAGE_RULE = 1;
  /** combination rule: Product of Probabilities (only nominal classes) */
  public static final int PRODUCT_RULE = 2;
  /** combination rule: Majority Voting (only nominal classes) */
  public static final int MAJORITY_VOTING_RULE = 3;
  /** combination rule: Minimum Probability */
  public static final int MIN_RULE = 4;
  /** combination rule: Maximum Probability */
  public static final int MAX_RULE = 5;
  /** combination rule: Median Probability (only numeric class) */
  public static final int MEDIAN_RULE = 6;
  /** combination rules */
  public static final Tag[] TAGS_RULES = {
    new Tag(AVERAGE_RULE, "AVG", "Average of Probabilities"),
    new Tag(PRODUCT_RULE, "PROD", "Product of Probabilities"),
    new Tag(MAJORITY_VOTING_RULE, "MAJ", "Majority Voting"),
    new Tag(MIN_RULE, "MIN", "Minimum Probability"),
    new Tag(MAX_RULE, "MAX", "Maximum Probability"),
    new Tag(MEDIAN_RULE, "MED", "Median") };

  /** Combination Rule variable */
  protected int m_CombinationRule = AVERAGE_RULE;

  /** List of file paths to serialized models to load */
  protected List<String> m_classifiersToLoad = new ArrayList<String>();

  /** List of de-serialized pre-built classifiers to include in the ensemble */
  protected List<Classifier> m_preBuiltClassifiers =
    new ArrayList<Classifier>();

  /** Environment variables */
  protected transient Environment m_env = Environment.getSystemWide();

  /** Structure of the training data */
  protected Instances m_structure;

  /** Print the individual models in the output */
  protected boolean m_dontPrintModels;

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {
    return "Class for combining classifiers. Different combinations of "
      + "probability estimates for classification are available.\n\n"
      + "For more information see:\n\n" + getTechnicalInformation().toString();
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option(
      "\tFull path to serialized classifier to include.\n"
        + "\tMay be specified multiple times to include\n"
        + "\tmultiple serialized classifiers. Note: it does\n"
        + "\tnot make sense to use pre-built classifiers in\n"
        + "\ta cross-validation.", "P", 1, "-P <path to serialized "
        + "classifier>"));

    result.addElement(new Option("\tThe combination rule to use\n"
      + "\t(default: AVG)", "R", 1, "-R " + Tag.toOptionList(TAGS_RULES)));

    result.addElement(new Option(
      "\tSuppress the printing of the individual models in the output",
      "do-not-print", 0, "-do-not-print"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Gets the current settings of Vote.
   * 
   * @return an array of strings suitable for passing to setOptions()
   */
  @Override
  public String[] getOptions() {
    int i;
    Vector<String> result = new Vector<String>();
    String[] options;

    options = super.getOptions();
    for (i = 0; i < options.length; i++) {
      result.add(options[i]);
    }

    result.add("-R");
    result.add("" + getCombinationRule());

    for (i = 0; i < m_classifiersToLoad.size(); i++) {
      result.add("-P");
      result.add(m_classifiersToLoad.get(i));
    }

    if (m_dontPrintModels) {
      result.add("-do-not-print");
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -P &lt;path to serialized classifier&gt;
   *  Full path to serialized classifier to include.
   *  May be specified multiple times to include
   *  multiple serialized classifiers. Note: it does
   *  not make sense to use pre-built classifiers in
   *  a cross-validation.
   * </pre>
   * 
   * <pre>
   * -R &lt;AVG|PROD|MAJ|MIN|MAX|MED&gt;
   *  The combination rule to use
   *  (default: AVG)
   * </pre>
   * 
   * <pre>
   * -print
   *  Print the individual models in the output
   * </pre>
   * 
   * <pre>
   * -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -B &lt;classifier specification&gt;
   *  Full class name of classifier to include, followed
   *  by scheme options. May be specified multiple times.
   *  (default: "weka.classifiers.rules.ZeroR")
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
   * Options specific to classifier weka.classifiers.rules.ZeroR:
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
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    tmpStr = Utils.getOption('R', options);
    if (tmpStr.length() != 0) {
      setCombinationRule(new SelectedTag(tmpStr, TAGS_RULES));
    } else {
      setCombinationRule(new SelectedTag(AVERAGE_RULE, TAGS_RULES));
    }

    m_classifiersToLoad.clear();
    while (true) {
      String loadString = Utils.getOption('P', options);
      if (loadString.length() == 0) {
        break;
      }

      m_classifiersToLoad.add(loadString);
    }

    setDoNotPrintModels(Utils.getFlag("-do-not-print", options));

    super.setOptions(options);
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

    result = new TechnicalInformation(Type.BOOK);
    result.setValue(Field.AUTHOR, "Ludmila I. Kuncheva");
    result.setValue(Field.TITLE,
      "Combining Pattern Classifiers: Methods and Algorithms");
    result.setValue(Field.YEAR, "2004");
    result.setValue(Field.PUBLISHER, "John Wiley and Sons, Inc.");

    additional = result.add(Type.ARTICLE);
    additional.setValue(Field.AUTHOR,
      "J. Kittler and M. Hatef and Robert P.W. Duin and J. Matas");
    additional.setValue(Field.YEAR, "1998");
    additional.setValue(Field.TITLE, "On combining classifiers");
    additional.setValue(Field.JOURNAL,
      "IEEE Transactions on Pattern Analysis and Machine Intelligence");
    additional.setValue(Field.VOLUME, "20");
    additional.setValue(Field.NUMBER, "3");
    additional.setValue(Field.PAGES, "226-239");

    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    if (m_preBuiltClassifiers.size() == 0 && m_classifiersToLoad.size() > 0) {
      try {
        loadClassifiers(null);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (m_preBuiltClassifiers.size() > 0) {
      if (m_Classifiers.length == 0) {
        result =
          (Capabilities) m_preBuiltClassifiers.get(0).getCapabilities().clone();
      }
      for (int i = 1; i < m_preBuiltClassifiers.size(); i++) {
        result.and(m_preBuiltClassifiers.get(i).getCapabilities());
      }

      for (Capability cap : Capability.values()) {
        result.enableDependency(cap);
      }
    }

    // class
    if ((m_CombinationRule == PRODUCT_RULE)
      || (m_CombinationRule == MAJORITY_VOTING_RULE)) {
      result.disableAllClasses();
      result.disableAllClassDependencies();
      result.enable(Capability.NOMINAL_CLASS);
      result.enableDependency(Capability.NOMINAL_CLASS);
    } else if (m_CombinationRule == MEDIAN_RULE) {
      result.disableAllClasses();
      result.disableAllClassDependencies();
      result.enable(Capability.NUMERIC_CLASS);
      result.enableDependency(Capability.NUMERIC_CLASS);
    }

    return result;
  }

  /**
   * Buildclassifier selects a classifier from the set of classifiers by
   * minimising error on the training data.
   * 
   * @param data the training data to be used for generating the boosted
   *          classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {

    // remove instances with missing class
    Instances newData = new Instances(data);
    newData.deleteWithMissingClass();
    m_structure = new Instances(newData, 0);

    if (m_classifiersToLoad.size() > 0) {
      m_preBuiltClassifiers.clear();
      loadClassifiers(data);

      if (m_Classifiers.length == 1
        && m_Classifiers[0] instanceof weka.classifiers.rules.ZeroR) {
        // remove the single ZeroR
        m_Classifiers = new Classifier[0];
      }
    }

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    for (int i = 0; i < m_Classifiers.length; i++) {
      getClassifier(i).buildClassifier(newData);
    }
  }

  /**
   * Load serialized models to include in the ensemble
   * 
   * @param data training instances (used in a header compatibility check with
   *          each of the loaded models)
   * 
   * @throws Exception if there is a problem de-serializing a model
   */
  private void loadClassifiers(Instances data) throws Exception {
    for (String path : m_classifiersToLoad) {
      if (Environment.containsEnvVariables(path)) {
        try {
          path = m_env.substitute(path);
        } catch (Exception ex) {
        }
      }

      File toLoad = new File(path);
      if (!toLoad.isFile()) {
        throw new Exception("\"" + path
          + "\" does not seem to be a valid file!");
      }
      ObjectInputStream is =
        new ObjectInputStream(new BufferedInputStream(new FileInputStream(
          toLoad)));
      Object c = is.readObject();
      if (!(c instanceof Classifier)) {
        is.close();
        throw new Exception("\"" + path + "\" does not contain a classifier!");
      }
      Object header = null;
      header = is.readObject();
      if (header instanceof Instances) {
        if (data != null && !data.equalHeaders((Instances) header)) {
          is.close();
          throw new Exception("\"" + path + "\" was trained with data that is "
            + "of a differnet structure than the incoming training data");
        }
      }
      if (header == null) {
        System.out.println("[Vote] warning: no header instances for \"" + path
          + "\"");
      }
      is.close();
      addPreBuiltClassifier((Classifier) c);
    }
  }

  /**
   * Add a prebuilt classifier to the list for use in the ensemble
   * 
   * @param c a prebuilt Classifier to add.
   */
  public void addPreBuiltClassifier(Classifier c) {
    m_preBuiltClassifiers.add(c);
  }

  /**
   * Remove a prebuilt classifier from the list to use in the ensemble
   * 
   * @param c the classifier to remove
   */
  public void removePreBuiltClassifier(Classifier c) {
    m_preBuiltClassifiers.remove(c);
  }

  /**
   * Classifies the given test instance.
   * 
   * @param instance the instance to be classified
   * @return the predicted most likely class for the instance or
   *         Utils.missingValue() if no prediction is made
   * @throws Exception if an error occurred during the prediction
   */
  @Override
  public double classifyInstance(Instance instance) throws Exception {
    double result;
    double[] dist;
    int index;

    switch (m_CombinationRule) {
    case AVERAGE_RULE:
    case PRODUCT_RULE:
    case MAJORITY_VOTING_RULE:
    case MIN_RULE:
    case MAX_RULE:
      dist = distributionForInstance(instance);
      if (instance.classAttribute().isNominal()) {
        index = Utils.maxIndex(dist);
        if (dist[index] == 0) {
          result = Utils.missingValue();
        } else {
          result = index;
        }
      } else if (instance.classAttribute().isNumeric()) {
        result = dist[0];
      } else {
        result = Utils.missingValue();
      }
      break;
    case MEDIAN_RULE:
      result = classifyInstanceMedian(instance);
      break;
    default:
      throw new IllegalStateException("Unknown combination rule '"
        + m_CombinationRule + "'!");
    }

    return result;
  }

  /**
   * Classifies the given test instance, returning the median from all
   * classifiers. Can assume that class is numeric.
   * 
   * @param instance the instance to be classified
   * @return the predicted most likely class for the instance or
   *         Utils.missingValue() if no prediction is made
   * @throws Exception if an error occurred during the prediction
   */
  protected double classifyInstanceMedian(Instance instance) throws Exception {
    double[] results =
      new double[m_Classifiers.length + m_preBuiltClassifiers.size()];

    int numResults = 0;
    for (Classifier m_Classifier : m_Classifiers) {
      double pred = m_Classifier.classifyInstance(instance);
      if (!Utils.isMissingValue(pred)) {
        results[numResults++] = pred;
      }
    }

    for (int i = 0; i < m_preBuiltClassifiers.size(); i++) {
      double pred = m_preBuiltClassifiers.get(i).classifyInstance(instance);
      if (!Utils.isMissingValue(pred)) {
        results[numResults++] = pred;
      }
    }

    if (numResults == 0) {
      return Utils.missingValue();
    } else if (numResults == 1) {
      return results[0];
    } else {
      double[] actualResults = new double[numResults];
      System.arraycopy(results, 0, actualResults, 0, numResults);
      return Utils.kthSmallestValue(actualResults, actualResults.length / 2);
    }
  }

  /**
   * Classifies a given instance using the selected combination rule.
   * 
   * @param instance the instance to be classified
   * @return the distribution
   * @throws Exception if instance could not be classified successfully
   */
  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {
    double[] result = new double[instance.numClasses()];

    switch (m_CombinationRule) {
    case AVERAGE_RULE:
      result = distributionForInstanceAverage(instance);
      break;
    case PRODUCT_RULE:
      result = distributionForInstanceProduct(instance);
      break;
    case MAJORITY_VOTING_RULE:
      result = distributionForInstanceMajorityVoting(instance);
      break;
    case MIN_RULE:
      result = distributionForInstanceMin(instance);
      break;
    case MAX_RULE:
      result = distributionForInstanceMax(instance);
      break;
    case MEDIAN_RULE:
      result[0] = classifyInstance(instance);
      break;
    default:
      throw new IllegalStateException("Unknown combination rule '"
        + m_CombinationRule + "'!");
    }

    if (!instance.classAttribute().isNumeric() && (Utils.sum(result) > 0)) {
      Utils.normalize(result);
    }

    return result;
  }

  /**
   * Classifies a given instance using the Average of Probabilities combination
   * rule.
   * 
   * @param instance the instance to be classified
   * @return the distribution
   * @throws Exception if instance could not be classified successfully
   */
  protected double[] distributionForInstanceAverage(Instance instance)
    throws Exception {

    double[] probs = new double[instance.numClasses()];

    double numPredictions = 0;
    for (int i = 0; i < m_Classifiers.length; i++) {
      double[] dist = getClassifier(i).distributionForInstance(instance);
      if (!instance.classAttribute().isNumeric()
        || !Utils.isMissingValue(dist[0])) {
        for (int j = 0; j < dist.length; j++) {
          probs[j] += dist[j];
        }
        numPredictions++;
      }
    }

    for (int i = 0; i < m_preBuiltClassifiers.size(); i++) {
      double[] dist =
        m_preBuiltClassifiers.get(i).distributionForInstance(instance);
      if (!instance.classAttribute().isNumeric()
        || !Utils.isMissingValue(dist[0])) {
        for (int j = 0; j < dist.length; j++) {
          probs[j] += dist[j];
        }
        numPredictions++;
      }
    }

    if (instance.classAttribute().isNumeric()) {
      if (numPredictions == 0) {
        probs[0] = Utils.missingValue();
      } else {
        for (int j = 0; j < probs.length; j++) {
          probs[j] /= numPredictions;
        }
      }
    } else {

      // Should normalize "probability" distribution
      if (Utils.sum(probs) > 0) {
        Utils.normalize(probs);
      }
    }

    return probs;
  }

  /**
   * Classifies a given instance using the Product of Probabilities combination
   * rule. Can assume that class is nominal.
   * 
   * @param instance the instance to be classified
   * @return the distribution
   * @throws Exception if instance could not be classified successfully
   */
  protected double[] distributionForInstanceProduct(Instance instance)
    throws Exception {

    double[] probs = new double[instance.numClasses()];
    for (int i = 0; i < probs.length; i++) {
      probs[i] = 1.0;
    }

    int numPredictions = 0;
    for (int i = 0; i < m_Classifiers.length; i++) {
      double[] dist = getClassifier(i).distributionForInstance(instance);
      if (Utils.sum(dist) > 0) {
        for (int j = 0; j < dist.length; j++) {
          probs[j] *= dist[j];
        }
        numPredictions++;
      }
    }

    for (int i = 0; i < m_preBuiltClassifiers.size(); i++) {
      double[] dist =
        m_preBuiltClassifiers.get(i).distributionForInstance(instance);
      if (Utils.sum(dist) > 0) {
        for (int j = 0; j < dist.length; j++) {
          probs[j] *= dist[j];
        }
        numPredictions++;
      }
    }

    // No predictions?
    if (numPredictions == 0) {
      return new double[instance.numClasses()];
    }

    // Should normalize to get "probabilities"
    if (Utils.sum(probs) > 0) {
      Utils.normalize(probs);
    }

    return probs;
  }

  /**
   * Classifies a given instance using the Majority Voting combination rule. Can
   * assume that class is nominal.
   * 
   * @param instance the instance to be classified
   * @return the distribution
   * @throws Exception if instance could not be classified successfully
   */
  protected double[] distributionForInstanceMajorityVoting(Instance instance)
    throws Exception {

    double[] probs = new double[instance.classAttribute().numValues()];
    double[] votes = new double[probs.length];

    for (int i = 0; i < m_Classifiers.length; i++) {
      probs = getClassifier(i).distributionForInstance(instance);
      int maxIndex = 0;
      for (int j = 0; j < probs.length; j++) {
        if (probs[j] > probs[maxIndex]) {
          maxIndex = j;
        }
      }

      // Consider the cases when multiple classes happen to have the same
      // probability
      if (probs[maxIndex] > 0) {
        for (int j = 0; j < probs.length; j++) {
          if (probs[j] == probs[maxIndex]) {
            votes[j]++;
          }
        }
      }
    }

    for (int i = 0; i < m_preBuiltClassifiers.size(); i++) {
      probs = m_preBuiltClassifiers.get(i).distributionForInstance(instance);
      int maxIndex = 0;

      for (int j = 0; j < probs.length; j++) {
        if (probs[j] > probs[maxIndex]) {
          maxIndex = j;
        }
      }

      // Consider the cases when multiple classes happen to have the same
      // probability
      if (probs[maxIndex] > 0) {
        for (int j = 0; j < probs.length; j++) {
          if (probs[j] == probs[maxIndex]) {
            votes[j]++;
          }
        }
      }
    }

    int tmpMajorityIndex = 0;
    for (int k = 1; k < votes.length; k++) {
      if (votes[k] > votes[tmpMajorityIndex]) {
        tmpMajorityIndex = k;
      }
    }

    // No votes received
    if (votes[tmpMajorityIndex] == 0) {
      return new double[instance.numClasses()];
    }

    // Consider the cases when multiple classes receive the same amount of votes
    Vector<Integer> majorityIndexes = new Vector<Integer>();
    for (int k = 0; k < votes.length; k++) {
      if (votes[k] == votes[tmpMajorityIndex]) {
        majorityIndexes.add(k);
      }
    }
    int majorityIndex = tmpMajorityIndex;
    if (majorityIndexes.size() > 1) {
      // resolve ties by looking at the predicted distribution
      double[] distPreds = distributionForInstanceAverage(instance);
      majorityIndex = Utils.maxIndex(distPreds);
      // Resolve the ties according to a uniform random distribution
      // majorityIndex = majorityIndexes.get(m_Random.nextInt(majorityIndexes.size()));
    }

    // set probs to 0
    probs = new double[probs.length];

    probs[majorityIndex] = 1; // the class that have been voted the most
                              // receives 1

    return probs;
  }

  /**
   * Classifies a given instance using the Maximum Probability combination rule.
   * 
   * @param instance the instance to be classified
   * @return the distribution
   * @throws Exception if instance could not be classified successfully
   */
  protected double[] distributionForInstanceMax(Instance instance)
    throws Exception {

    double[] probs = new double[instance.numClasses()];

    double numPredictions = 0;
    for (int i = 0; i < m_Classifiers.length; i++) {
      double[] dist = getClassifier(i).distributionForInstance(instance);
      if (!instance.classAttribute().isNumeric()
        || !Utils.isMissingValue(dist[0])) {
        for (int j = 0; j < dist.length; j++) {
          if ((probs[j] < dist[j]) || (numPredictions == 0)) {
            probs[j] = dist[j];
          }
        }
        numPredictions++;
      }
    }

    for (int i = 0; i < m_preBuiltClassifiers.size(); i++) {
      double[] dist =
        m_preBuiltClassifiers.get(i).distributionForInstance(instance);
      if (!instance.classAttribute().isNumeric()
        || !Utils.isMissingValue(dist[0])) {
        for (int j = 0; j < dist.length; j++) {
          if ((probs[j] < dist[j]) || (numPredictions == 0)) {
            probs[j] = dist[j];
          }
        }
        numPredictions++;
      }
    }

    if (instance.classAttribute().isNumeric()) {
      if (numPredictions == 0) {
        probs[0] = Utils.missingValue();
      }
    } else {

      // Should normalize "probability" distribution
      if (Utils.sum(probs) > 0) {
        Utils.normalize(probs);
      }
    }

    return probs;
  }

  /**
   * Classifies a given instance using the Minimum Probability combination rule.
   * 
   * @param instance the instance to be classified
   * @return the distribution
   * @throws Exception if instance could not be classified successfully
   */
  protected double[] distributionForInstanceMin(Instance instance)
    throws Exception {

    double[] probs = new double[instance.numClasses()];

    double numPredictions = 0;
    for (int i = 0; i < m_Classifiers.length; i++) {
      double[] dist = getClassifier(i).distributionForInstance(instance);
      if (!instance.classAttribute().isNumeric()
        || !Utils.isMissingValue(dist[0])) {
        for (int j = 0; j < dist.length; j++) {
          if ((probs[j] > dist[j]) || (numPredictions == 0)) {
            probs[j] = dist[j];
          }
        }
        numPredictions++;
      }
    }

    for (int i = 0; i < m_preBuiltClassifiers.size(); i++) {
      double[] dist =
        m_preBuiltClassifiers.get(i).distributionForInstance(instance);
      if (!instance.classAttribute().isNumeric()
        || !Utils.isMissingValue(dist[0])) {
        for (int j = 0; j < dist.length; j++) {
          if ((probs[j] > dist[j]) || (numPredictions == 0)) {
            probs[j] = dist[j];
          }
        }
        numPredictions++;
      }
    }

    if (instance.classAttribute().isNumeric()) {
      if (numPredictions == 0) {
        probs[0] = Utils.missingValue();
      }
    } else {

      // Should normalize "probability" distribution
      if (Utils.sum(probs) > 0) {
        Utils.normalize(probs);
      }
    }

    return probs;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String combinationRuleTipText() {
    return "The combination rule used.";
  }

  /**
   * Gets the combination rule used
   * 
   * @return the combination rule used
   */
  public SelectedTag getCombinationRule() {
    return new SelectedTag(m_CombinationRule, TAGS_RULES);
  }

  /**
   * Sets the combination rule to use. Values other than
   * 
   * @param newRule the combination rule method to use
   */
  public void setCombinationRule(SelectedTag newRule) {
    if (newRule.getTags() == TAGS_RULES) {
      m_CombinationRule = newRule.getSelectedTag().getID();
    }
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String preBuiltClassifiersTipText() {
    return "The pre-built serialized classifiers to include. Multiple "
      + "serialized classifiers can be included alongside those "
      + "that are built from scratch when this classifier runs. "
      + "Note that it does not make sense to include pre-built "
      + "classifiers in a cross-validation since they are static "
      + "and their models do not change from fold to fold.";
  }

  /**
   * Set the paths to pre-built serialized classifiers to load and include in
   * the ensemble
   * 
   * @param preBuilt an array of File paths to serialized models
   */
  public void setPreBuiltClassifiers(File[] preBuilt) {
    m_classifiersToLoad.clear();
    if (preBuilt != null && preBuilt.length > 0) {
      for (File element : preBuilt) {
        String path = element.toString();
        m_classifiersToLoad.add(path);
      }
    }
  }

  /**
   * Get the paths to pre-built serialized classifiers to load and include in
   * the ensemble
   * 
   * @return an array of File paths to serialized models
   */
  public File[] getPreBuiltClassifiers() {
    File[] result = new File[m_classifiersToLoad.size()];

    for (int i = 0; i < m_classifiersToLoad.size(); i++) {
      result[i] = new File(m_classifiersToLoad.get(i));
    }

    return result;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String doNotPrintModelsTipText() {
    return "Do not print the individual trees in the output";
  }

  /**
   * Set whether to print the individual ensemble models in the output
   * 
   * @param print true if the individual models are to be printed
   */
  public void setDoNotPrintModels(boolean print) {
    m_dontPrintModels = print;
  }

  /**
   * Get whether to print the individual ensemble models in the output
   * 
   * @return true if the individual models are to be printed
   */
  public boolean getDoNotPrintModels() {
    return m_dontPrintModels;
  }

  /**
   * Output a representation of this classifier
   * 
   * @return a string representation of the classifier
   */
  @Override
  public String toString() {

    if (m_Classifiers == null) {
      return "Vote: No model built yet.";
    }

    String result = "Vote combines";
    result += " the probability distributions of these base learners:\n";
    for (int i = 0; i < m_Classifiers.length; i++) {
      result += '\t' + getClassifierSpec(i) + '\n';
    }

    for (Classifier c : m_preBuiltClassifiers) {
      result +=
        "\t" + c.getClass().getName()
          + Utils.joinOptions(((OptionHandler) c).getOptions()) + "\n";
    }

    result += "using the '";

    switch (m_CombinationRule) {
    case AVERAGE_RULE:
      result += "Average";
      break;

    case PRODUCT_RULE:
      result += "Product";
      break;

    case MAJORITY_VOTING_RULE:
      result += "Majority Voting";
      break;

    case MIN_RULE:
      result += "Minimum";
      break;

    case MAX_RULE:
      result += "Maximum";
      break;

    case MEDIAN_RULE:
      result += "Median";
      break;

    default:
      throw new IllegalStateException("Unknown combination rule '"
        + m_CombinationRule + "'!");
    }

    result += "' combination rule \n";

    StringBuilder resultBuilder = null;
    if (!m_dontPrintModels) {
      resultBuilder = new StringBuilder();
      resultBuilder.append(result).append("\nAll the models:\n\n");
      for (Classifier c : m_Classifiers) {
        resultBuilder.append(c).append("\n");
      }

      for (Classifier c : m_preBuiltClassifiers) {
        resultBuilder.append(c).append("\n");
      }
    }

    return resultBuilder == null ? result : resultBuilder.toString();
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12424 $");
  }

  /**
   * Set environment variable values to substitute in the paths of serialized
   * models to load
   * 
   * @param env the environment variables to use
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Aggregate an object with this one
   * 
   * @param toAggregate the object to aggregate
   * @return the result of aggregation
   * @throws Exception if the supplied object can't be aggregated for some
   *           reason
   */
  @Override
  public Classifier aggregate(Classifier toAggregate) throws Exception {

    if (m_structure == null && m_Classifiers.length == 1
      && (m_Classifiers[0] instanceof weka.classifiers.rules.ZeroR)) {
      // remove the single untrained ZeroR
      setClassifiers(new Classifier[0]);
    }

    // Can't do any training data compatibility checks unfortunately
    addPreBuiltClassifier(toAggregate);

    return this;
  }

  /**
   * Call to complete the aggregation process. Allows implementers to do any
   * final processing based on how many objects were aggregated.
   * 
   * @throws Exception if the aggregation can't be finalized for some reason
   */
  @Override
  public void finalizeAggregation() throws Exception {
    // nothing to do
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv should contain the following arguments: -t training file [-T
   *          test file] [-c class index]
   */
  public static void main(String[] argv) {
    runClassifier(new Vote(), argv);
  }

}
