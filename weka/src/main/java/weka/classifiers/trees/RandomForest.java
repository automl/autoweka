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
 *    RandomForest.java
 *    Copyright (C) 2001-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.meta.Bagging;
import weka.core.AdditionalMeasureProducer;
import weka.core.Aggregateable;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.PartitionGenerator;
import weka.core.Randomizable;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.gui.ProgrammaticProperty;

/**
 <!-- globalinfo-start -->
 * Class for constructing a forest of random trees.<br>
 * <br>
 * For more information see: <br>
 * <br>
 * Leo Breiman (2001). Random Forests. Machine Learning. 45(1):5-32.
 * <br><br>
 <!-- globalinfo-end -->
 * 
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Breiman2001,
 *    author = {Leo Breiman},
 *    journal = {Machine Learning},
 *    number = {1},
 *    pages = {5-32},
 *    title = {Random Forests},
 *    volume = {45},
 *    year = {2001}
 * }
 * </pre>
 * <br><br>
 <!-- technical-bibtex-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -P
 *  Size of each bag, as a percentage of the
 *  training set size. (default 100)</pre>
 * 
 * <pre> -O
 *  Calculate the out of bag error.</pre>
 * 
 * <pre> -store-out-of-bag-predictions
 *  Whether to store out of bag predictions in internal evaluation object.</pre>
 * 
 * <pre> -output-out-of-bag-complexity-statistics
 *  Whether to output complexity-based statistics when out-of-bag evaluation is performed.</pre>
 * 
 * <pre> -print
 *  Print the individual classifiers in the output</pre>
 * 
 * <pre> -I &lt;num&gt;
 *  Number of iterations.
 *  (current value 100)</pre>
 * 
 * <pre> -num-slots &lt;num&gt;
 *  Number of execution slots.
 *  (default 1 - i.e. no parallelism)
 *  (use 0 to auto-detect number of cores)</pre>
 * 
 * <pre> -K &lt;number of attributes&gt;
 *  Number of attributes to randomly investigate. (default 0)
 *  (&lt;1 = int(log_2(#predictors)+1)).</pre>
 * 
 * <pre> -M &lt;minimum number of instances&gt;
 *  Set minimum number of instances per leaf.
 *  (default 1)</pre>
 * 
 * <pre> -V &lt;minimum variance for split&gt;
 *  Set minimum numeric class variance proportion
 *  of train variance for split (default 1e-3).</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Seed for random number generator.
 *  (default 1)</pre>
 * 
 * <pre> -depth &lt;num&gt;
 *  The maximum depth of the tree, 0 for unlimited.
 *  (default 0)</pre>
 * 
 * <pre> -N &lt;num&gt;
 *  Number of folds for backfitting (default 0, no backfitting).</pre>
 * 
 * <pre> -U
 *  Allow unclassified instances.</pre>
 * 
 * <pre> -B
 *  Break ties randomly when several attributes look equally good.</pre>
 * 
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 * 
 * <pre> -num-decimal-places
 *  The number of decimal places for the output of numbers in the model (default 2).</pre>
 * 
 <!-- options-end -->
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 12645 $
 */
public class RandomForest extends Bagging {

  /** for serialization */
  static final long serialVersionUID = 1116839470751428698L;

  /**
   * The default number of iterations to perform.
   */
  protected int defaultNumberOfIterations() {
    return 100;
  }

  /**
   * Constructor that sets base classifier for bagging to RandomTre and default number of iterations to 100.
   */
  public RandomForest() {

    RandomTree rTree = new RandomTree();
    rTree.setDoNotCheckCapabilities(true);
    super.setClassifier(rTree);
    super.setRepresentCopiesUsingWeights(true);
    setNumIterations(defaultNumberOfIterations());
  }

  /**
   * Returns default capabilities of the base classifier.
   *
   * @return      the capabilities of the base classifier
   */
  public Capabilities getCapabilities() {

    // Cannot use the main RandomTree object because capabilities checking has been turned off
    // for that object.
    return (new RandomTree()).getCapabilities();
  }

  /**
   * String describing default classifier.
   *
   * @return the default classifier classname
   */
  @Override
  protected String defaultClassifierString() {

    return "weka.classifiers.trees.RandomTree";
  }

  /**
   * String describing default classifier options.
   *
   * @return the default classifier options
   */
  @Override
  protected String[] defaultClassifierOptions() {

    String[] args = {"-do-not-check-capabilities"};
    return args;
  }

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {

    return "Class for constructing a forest of random trees.\n\n"
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

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Leo Breiman");
    result.setValue(Field.YEAR, "2001");
    result.setValue(Field.TITLE, "Random Forests");
    result.setValue(Field.JOURNAL, "Machine Learning");
    result.setValue(Field.VOLUME, "45");
    result.setValue(Field.NUMBER, "1");
    result.setValue(Field.PAGES, "5-32");

    return result;
  }

  /**
   * This method only accepts RandomTree arguments.
   *
   * @param newClassifier the RandomTree to use.
   * @exception if argument is not a RandomTree
   */
  @ProgrammaticProperty
  public void setClassifier(Classifier newClassifier) {
    if (!(newClassifier instanceof RandomTree)) {
      throw new IllegalArgumentException("RandomForest: Argument of setClassifier() must be a RandomTree.");
    }
    super.setClassifier(newClassifier);
  }

  /**
   * This method only accepts true as its argument
   *
   * @param representUsingWeights must be set to true.
   * @exception if argument is not true
   */
  @ProgrammaticProperty
  public void setRepresentCopiesUsingWeights(boolean representUsingWeights) {
    if (!representUsingWeights) {
      throw new IllegalArgumentException("RandomForest: Argument of setRepresentCopiesUsingWeights() must be true.");
    }
    super.setRepresentCopiesUsingWeights(representUsingWeights);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numFeaturesTipText() {
    return ((RandomTree)getClassifier()).KValueTipText();
  }

  /**
   * Get the number of features used in random selection.
   *
   * @return Value of numFeatures.
   */
  public int getNumFeatures() {

    return ((RandomTree)getClassifier()).getKValue();
  }

  /**
   * Set the number of features to use in random selection.
   *
   * @param newNumFeatures Value to assign to numFeatures.
   */
  public void setNumFeatures(int newNumFeatures) {

    ((RandomTree)getClassifier()).setKValue(newNumFeatures);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String maxDepthTipText() {
    return ((RandomTree)getClassifier()).maxDepthTipText();
  }

  /**
   * Get the maximum depth of trh tree, 0 for unlimited.
   *
   * @return the maximum depth.
   */
  public int getMaxDepth() {
    return ((RandomTree)getClassifier()).getMaxDepth();
  }

  /**
   * Set the maximum depth of the tree, 0 for unlimited.
   *
   * @param value the maximum depth.
   */
  public void setMaxDepth(int value) {
    ((RandomTree)getClassifier()).setMaxDepth(value);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String breakTiesRandomlyTipText() {
    return ((RandomTree)getClassifier()).breakTiesRandomlyTipText();
  }

  /**
   * Get whether to break ties randomly.
   *
   * @return true if ties are to be broken randomly.
   */
  public boolean getBreakTiesRandomly() {

    return ((RandomTree)getClassifier()).getBreakTiesRandomly();
  }

  /**
   * Set whether to break ties randomly.
   *
   * @param newBreakTiesRandomly true if ties are to be broken randomly
   */
  public void setBreakTiesRandomly(boolean newBreakTiesRandomly) {

    ((RandomTree)getClassifier()).setBreakTiesRandomly(newBreakTiesRandomly);
  }

  /**
   * Returns description of the bagged classifier.
   *
   * @return description of the bagged classifier as a string
   */
  @Override
  public String toString() {

    if (m_Classifiers == null) {
      return "RandomForest: No model built yet.";
    }
    StringBuffer buffer = new StringBuffer("RandomForest\n\n");
    buffer.append(super.toString());
    return buffer.toString();
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>();

    newVector.addElement(new Option(
            "\tSize of each bag, as a percentage of the\n"
                    + "\ttraining set size. (default 100)",
            "P", 1, "-P"));

    newVector.addElement(new Option(
            "\tCalculate the out of bag error.",
            "O", 0, "-O"));

    newVector.addElement(new Option(
            "\tWhether to store out of bag predictions in internal evaluation object.",
            "store-out-of-bag-predictions", 0, "-store-out-of-bag-predictions"));

    newVector.addElement(new Option(
            "\tWhether to output complexity-based statistics when out-of-bag evaluation is performed.",
            "output-out-of-bag-complexity-statistics", 0, "-output-out-of-bag-complexity-statistics"));

    newVector.addElement(new Option(
            "\tPrint the individual classifiers in the output", "print", 0, "-print"));

    newVector.addElement(new Option(
            "\tNumber of iterations.\n"
                    + "\t(current value " + getNumIterations() + ")",
            "I", 1, "-I <num>"));

    newVector.addElement(new Option("\tNumber of execution slots.\n"
            + "\t(default 1 - i.e. no parallelism)\n"
            + "\t(use 0 to auto-detect number of cores)", "num-slots", 1,
            "-num-slots <num>"));

    // Add base classifier options
    List<Option> list = Collections.list(((OptionHandler)getClassifier()).listOptions());
    newVector.addAll(list);

    return newVector.elements();
  }

  /**
   * Gets the current settings of the forest.
   * 
   * @return an array of strings suitable for passing to setOptions()
   */
  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    result.add("-P");
    result.add("" + getBagSizePercent());

    if (getCalcOutOfBag()) {
      result.add("-O");
    }

    if (getStoreOutOfBagPredictions()) {
      result.add("-store-out-of-bag-predictions");
    }

    if (getOutputOutOfBagComplexityStatistics()) {
      result.add("-output-out-of-bag-complexity-statistics");
    }

    if (getPrintClassifiers()) {
      result.add("-print");
    }

    result.add("-I");
    result.add("" + getNumIterations());

    result.add("-num-slots");
    result.add("" + getNumExecutionSlots());

    if (getDoNotCheckCapabilities()) {
      result.add("-do-not-check-capabilities");
    }

    // Add base classifier options
    Vector<String> classifierOptions = new Vector<String>();
    Collections.addAll(classifierOptions, ((OptionHandler)getClassifier()).getOptions());
    Option.deleteFlagString(classifierOptions, "-do-not-check-capabilities");
    result.addAll(classifierOptions);

    return result.toArray(new String[result.size()]);
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p>
   * 
   * <pre> -P
   *  Size of each bag, as a percentage of the
   *  training set size. (default 100)</pre>
   * 
   * <pre> -O
   *  Calculate the out of bag error.</pre>
   * 
   * <pre> -store-out-of-bag-predictions
   *  Whether to store out of bag predictions in internal evaluation object.</pre>
   * 
   * <pre> -output-out-of-bag-complexity-statistics
   *  Whether to output complexity-based statistics when out-of-bag evaluation is performed.</pre>
   * 
   * <pre> -print
   *  Print the individual classifiers in the output</pre>
   * 
   * <pre> -I &lt;num&gt;
   *  Number of iterations.
   *  (current value 100)</pre>
   * 
   * <pre> -num-slots &lt;num&gt;
   *  Number of execution slots.
   *  (default 1 - i.e. no parallelism)
   *  (use 0 to auto-detect number of cores)</pre>
   * 
   * <pre> -K &lt;number of attributes&gt;
   *  Number of attributes to randomly investigate. (default 0)
   *  (&lt;1 = int(log_2(#predictors)+1)).</pre>
   * 
   * <pre> -M &lt;minimum number of instances&gt;
   *  Set minimum number of instances per leaf.
   *  (default 1)</pre>
   * 
   * <pre> -V &lt;minimum variance for split&gt;
   *  Set minimum numeric class variance proportion
   *  of train variance for split (default 1e-3).</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Seed for random number generator.
   *  (default 1)</pre>
   * 
   * <pre> -depth &lt;num&gt;
   *  The maximum depth of the tree, 0 for unlimited.
   *  (default 0)</pre>
   * 
   * <pre> -N &lt;num&gt;
   *  Number of folds for backfitting (default 0, no backfitting).</pre>
   * 
   * <pre> -U
   *  Allow unclassified instances.</pre>
   * 
   * <pre> -B
   *  Break ties randomly when several attributes look equally good.</pre>
   * 
   * <pre> -output-debug-info
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -do-not-check-capabilities
   *  If set, classifier capabilities are not checked before classifier is built
   *  (use with caution).</pre>
   * 
   * <pre> -num-decimal-places
   *  The number of decimal places for the output of numbers in the model (default 2).</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String bagSize = Utils.getOption('P', options);
    if (bagSize.length() != 0) {
      setBagSizePercent(Integer.parseInt(bagSize));
    } else {
      setBagSizePercent(100);
    }

    setCalcOutOfBag(Utils.getFlag('O', options));

    setStoreOutOfBagPredictions(Utils.getFlag("store-out-of-bag-predictions", options));

    setOutputOutOfBagComplexityStatistics(Utils.getFlag("output-out-of-bag-complexity-statistics", options));

    setPrintClassifiers(Utils.getFlag("print", options));

    String iterations = Utils.getOption('I', options);
    if (iterations.length() != 0) {
      setNumIterations(Integer.parseInt(iterations));
    } else {
      setNumIterations(defaultNumberOfIterations());
    }

    String numSlots = Utils.getOption("num-slots", options);
    if (numSlots.length() != 0) {
      setNumExecutionSlots(Integer.parseInt(numSlots));
    } else {
      setNumExecutionSlots(1);
    }

    RandomTree classifier = ((RandomTree)AbstractClassifier.forName(defaultClassifierString(), options));
    setDoNotCheckCapabilities(classifier.getDoNotCheckCapabilities());
    setSeed(classifier.getSeed());
    setDebug(classifier.getDebug());
    setNumFeatures(classifier.getNumDecimalPlaces());
    classifier.setDoNotCheckCapabilities(true);

    // Set base classifier and options
    setClassifier(classifier);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12645 $");
  }

  /**
   * Main method for this class.
   * 
   * @param argv the options
   */
  public static void main(String[] argv) {
    runClassifier(new RandomForest(), argv);
  }
}

