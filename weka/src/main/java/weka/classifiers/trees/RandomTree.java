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
 *    RandomTree.java
 *    Copyright (C) 2001-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.ContingencyTables;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.PartitionGenerator;
import weka.core.Randomizable;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

/**
 <!-- globalinfo-start -->
 * Class for constructing a tree that considers K randomly  chosen attributes at each node. Performs no pruning. Also has an option to allow estimation of class probabilities (or target mean in the regression case) based on a hold-out set (backfitting).
 * <br><br>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p>
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
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 12505 $
 */
public class RandomTree extends AbstractClassifier implements OptionHandler,
  WeightedInstancesHandler, Randomizable, Drawable, PartitionGenerator {

  /** for serialization */
  private static final long serialVersionUID = -9051119597407396024L;

  /** The Tree object */
  protected Tree m_Tree = null;

  /** The header information. */
  protected Instances m_Info = null;

  /** Minimum number of instances for leaf. */
  protected double m_MinNum = 1.0;

  /** The number of attributes considered for a split. */
  protected int m_KValue = 0;

  /** The random seed to use. */
  protected int m_randomSeed = 1;

  /** The maximum depth of the tree (0 = unlimited) */
  protected int m_MaxDepth = 0;

  /** Determines how much data is used for backfitting */
  protected int m_NumFolds = 0;

  /** Whether unclassified instances are allowed */
  protected boolean m_AllowUnclassifiedInstances = false;

  /** Whether to break ties randomly. */
  protected boolean m_BreakTiesRandomly = false;

  /** a ZeroR model in case no model can be built from the data */
  protected Classifier m_zeroR;

  /**
   * The minimum proportion of the total variance (over all the data) required
   * for split.
   */
  protected double m_MinVarianceProp = 1e-3;

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {

    return "Class for constructing a tree that considers K randomly "
      + " chosen attributes at each node. Performs no pruning. Also has"
      + " an option to allow estimation of class probabilities (or target mean "
      + "in the regression case) based on a hold-out set (backfitting).";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String minNumTipText() {
    return "The minimum total weight of the instances in a leaf.";
  }

  /**
   * Get the value of MinNum.
   * 
   * @return Value of MinNum.
   */
  public double getMinNum() {

    return m_MinNum;
  }

  /**
   * Set the value of MinNum.
   * 
   * @param newMinNum Value to assign to MinNum.
   */
  public void setMinNum(double newMinNum) {

    m_MinNum = newMinNum;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String minVariancePropTipText() {
    return "The minimum proportion of the variance on all the data "
      + "that needs to be present at a node in order for splitting to "
      + "be performed in regression trees.";
  }

  /**
   * Get the value of MinVarianceProp.
   * 
   * @return Value of MinVarianceProp.
   */
  public double getMinVarianceProp() {

    return m_MinVarianceProp;
  }

  /**
   * Set the value of MinVarianceProp.
   * 
   * @param newMinVarianceProp Value to assign to MinVarianceProp.
   */
  public void setMinVarianceProp(double newMinVarianceProp) {

    m_MinVarianceProp = newMinVarianceProp;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String KValueTipText() {
    return "Sets the number of randomly chosen attributes. If 0, int(log_2(#predictors) + 1) is used.";
  }

  /**
   * Get the value of K.
   * 
   * @return Value of K.
   */
  public int getKValue() {

    return m_KValue;
  }

  /**
   * Set the value of K.
   * 
   * @param k Value to assign to K.
   */
  public void setKValue(int k) {

    m_KValue = k;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String seedTipText() {
    return "The random number seed used for selecting attributes.";
  }

  /**
   * Set the seed for random number generation.
   * 
   * @param seed the seed
   */
  @Override
  public void setSeed(int seed) {

    m_randomSeed = seed;
  }

  /**
   * Gets the seed for the random number generations
   * 
   * @return the seed for the random number generation
   */
  @Override
  public int getSeed() {

    return m_randomSeed;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String maxDepthTipText() {
    return "The maximum depth of the tree, 0 for unlimited.";
  }

  /**
   * Get the maximum depth of trh tree, 0 for unlimited.
   * 
   * @return the maximum depth.
   */
  public int getMaxDepth() {
    return m_MaxDepth;
  }

  /**
   * Set the maximum depth of the tree, 0 for unlimited.
   *
   * @param value the maximum depth.
   */
  public void setMaxDepth(int value) {
    m_MaxDepth = value;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numFoldsTipText() {
    return "Determines the amount of data used for backfitting. One fold is used for "
      + "backfitting, the rest for growing the tree. (Default: 0, no backfitting)";
  }

  /**
   * Get the value of NumFolds.
   * 
   * @return Value of NumFolds.
   */
  public int getNumFolds() {

    return m_NumFolds;
  }

  /**
   * Set the value of NumFolds.
   * 
   * @param newNumFolds Value to assign to NumFolds.
   */
  public void setNumFolds(int newNumFolds) {

    m_NumFolds = newNumFolds;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String allowUnclassifiedInstancesTipText() {
    return "Whether to allow unclassified instances.";
  }

  /**
   * Gets whether tree is allowed to abstain from making a prediction.
   * 
   * @return true if tree is allowed to abstain from making a prediction.
   */
  public boolean getAllowUnclassifiedInstances() {

    return m_AllowUnclassifiedInstances;
  }

  /**
   * Set the value of AllowUnclassifiedInstances.
   * 
   * @param newAllowUnclassifiedInstances true if tree is allowed to abstain from making a prediction
   */
  public void setAllowUnclassifiedInstances(boolean newAllowUnclassifiedInstances) {

    m_AllowUnclassifiedInstances = newAllowUnclassifiedInstances;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String breakTiesRandomlyTipText() {
    return "Break ties randomly when several attributes look equally good.";
  }

  /**
   * Get whether to break ties randomly.
   *
   * @return true if ties are to be broken randomly.
   */
  public boolean getBreakTiesRandomly() {

    return m_BreakTiesRandomly;
  }

  /**
   * Set whether to break ties randomly.
   *
   * @param newBreakTiesRandomly true if ties are to be broken randomly
   */
  public void setBreakTiesRandomly(boolean newBreakTiesRandomly) {

    m_BreakTiesRandomly = newBreakTiesRandomly;
  }

  /**
   * Lists the command-line options for this classifier.
   * 
   * @return an enumeration over all possible options
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>();

    newVector.addElement(new Option(
      "\tNumber of attributes to randomly investigate.\t(default 0)\n"
        + "\t(<1 = int(log_2(#predictors)+1)).", "K", 1,
      "-K <number of attributes>"));

    newVector.addElement(new Option(
      "\tSet minimum number of instances per leaf.\n\t(default 1)", "M", 1,
      "-M <minimum number of instances>"));

    newVector.addElement(new Option(
      "\tSet minimum numeric class variance proportion\n"
        + "\tof train variance for split (default 1e-3).", "V", 1,
      "-V <minimum variance for split>"));

    newVector.addElement(new Option("\tSeed for random number generator.\n"
      + "\t(default 1)", "S", 1, "-S <num>"));

    newVector.addElement(new Option(
      "\tThe maximum depth of the tree, 0 for unlimited.\n" + "\t(default 0)",
      "depth", 1, "-depth <num>"));

    newVector.addElement(new Option("\tNumber of folds for backfitting "
      + "(default 0, no backfitting).", "N", 1, "-N <num>"));
    newVector.addElement(new Option("\tAllow unclassified instances.", "U", 0,
      "-U"));
    newVector.addElement(new Option("\t" + breakTiesRandomlyTipText(), "B", 0,
            "-B"));
    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  }

  /**
   * Gets options from this classifier.
   * 
   * @return the options for the current setup
   */
  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    result.add("-K");
    result.add("" + getKValue());

    result.add("-M");
    result.add("" + getMinNum());

    result.add("-V");
    result.add("" + getMinVarianceProp());

    result.add("-S");
    result.add("" + getSeed());

    if (getMaxDepth() > 0) {
      result.add("-depth");
      result.add("" + getMaxDepth());
    }

    if (getNumFolds() > 0) {
      result.add("-N");
      result.add("" + getNumFolds());
    }

    if (getAllowUnclassifiedInstances()) {
      result.add("-U");
    }

    if (getBreakTiesRandomly()) {
      result.add("-B");
    }

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p>
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
    String tmpStr;

    tmpStr = Utils.getOption('K', options);
    if (tmpStr.length() != 0) {
      m_KValue = Integer.parseInt(tmpStr);
    } else {
      m_KValue = 0;
    }

    tmpStr = Utils.getOption('M', options);
    if (tmpStr.length() != 0) {
      m_MinNum = Double.parseDouble(tmpStr);
    } else {
      m_MinNum = 1;
    }

    String minVarString = Utils.getOption('V', options);
    if (minVarString.length() != 0) {
      m_MinVarianceProp = Double.parseDouble(minVarString);
    } else {
      m_MinVarianceProp = 1e-3;
    }

    tmpStr = Utils.getOption('S', options);
    if (tmpStr.length() != 0) {
      setSeed(Integer.parseInt(tmpStr));
    } else {
      setSeed(1);
    }

    tmpStr = Utils.getOption("depth", options);
    if (tmpStr.length() != 0) {
      setMaxDepth(Integer.parseInt(tmpStr));
    } else {
      setMaxDepth(0);
    }
    String numFoldsString = Utils.getOption('N', options);
    if (numFoldsString.length() != 0) {
      m_NumFolds = Integer.parseInt(numFoldsString);
    } else {
      m_NumFolds = 0;
    }

    setAllowUnclassifiedInstances(Utils.getFlag('U', options));

    setBreakTiesRandomly(Utils.getFlag('B', options));

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
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
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Builds classifier.
   * 
   * @param data the data to train with
   * @throws Exception if something goes wrong or the data doesn't fit
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {

    // Make sure K value is in range
    if (m_KValue > data.numAttributes() - 1) {
      m_KValue = data.numAttributes() - 1;
    }
    if (m_KValue < 1) {
      m_KValue = (int) Utils.log2(data.numAttributes() - 1) + 1;
    }

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();

    // only class? -> build ZeroR model
    if (data.numAttributes() == 1) {
      System.err
        .println("Cannot build model (only class attribute present in data!), "
          + "using ZeroR model instead!");
      m_zeroR = new weka.classifiers.rules.ZeroR();
      m_zeroR.buildClassifier(data);
      return;
    } else {
      m_zeroR = null;
    }

    // Figure out appropriate datasets
    Instances train = null;
    Instances backfit = null;
    Random rand = data.getRandomNumberGenerator(m_randomSeed);
    if (m_NumFolds <= 0) {
      train = data;
    } else {
      data.randomize(rand);
      data.stratify(m_NumFolds);
      train = data.trainCV(m_NumFolds, 1, rand);
      backfit = data.testCV(m_NumFolds, 1);
    }

    // Create the attribute indices window
    int[] attIndicesWindow = new int[data.numAttributes() - 1];
    int j = 0;
    for (int i = 0; i < attIndicesWindow.length; i++) {
      if (j == data.classIndex()) {
        j++; // do not include the class
      }
      attIndicesWindow[i] = j++;
    }

    double totalWeight = 0;
    double totalSumSquared = 0;

    // Compute initial class counts
    double[] classProbs = new double[train.numClasses()];
    for (int i = 0; i < train.numInstances(); i++) {
      Instance inst = train.instance(i);
      if (data.classAttribute().isNominal()) {
        classProbs[(int) inst.classValue()] += inst.weight();
        totalWeight += inst.weight();
      } else {
        classProbs[0] += inst.classValue() * inst.weight();
        totalSumSquared += inst.classValue() * inst.classValue()
          * inst.weight();
        totalWeight += inst.weight();
      }
    }

    double trainVariance = 0;
    if (data.classAttribute().isNumeric()) {
      trainVariance = RandomTree.singleVariance(classProbs[0], totalSumSquared,
        totalWeight) / totalWeight;
      classProbs[0] /= totalWeight;
    }

    // Build tree
    m_Tree = new Tree();
    m_Info = new Instances(data, 0);
    m_Tree.buildTree(train, classProbs, attIndicesWindow, totalWeight, rand, 0,
      m_MinVarianceProp * trainVariance);

    // Backfit if required
    if (backfit != null) {
      m_Tree.backfitData(backfit);
    }
  }

  /**
   * Computes class distribution of an instance using the tree.
   * 
   * @param instance the instance to compute the distribution for
   * @return the computed class probabilities
   * @throws Exception if computation fails
   */
  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {

    if (m_zeroR != null) {
      return m_zeroR.distributionForInstance(instance);
    } else {
      return m_Tree.distributionForInstance(instance);
    }
  }

  /**
   * Outputs the decision tree.
   * 
   * @return a string representation of the classifier
   */
  @Override
  public String toString() {

    // only ZeroR model?
    if (m_zeroR != null) {
      StringBuffer buf = new StringBuffer();
      buf.append(this.getClass().getName().replaceAll(".*\\.", "") + "\n");
      buf.append(this.getClass().getName().replaceAll(".*\\.", "")
        .replaceAll(".", "=")
        + "\n\n");
      buf
        .append("Warning: No model could be built, hence ZeroR model is used:\n\n");
      buf.append(m_zeroR.toString());
      return buf.toString();
    }

    if (m_Tree == null) {
      return "RandomTree: no model has been built yet.";
    } else {
      return "\nRandomTree\n==========\n"
        + m_Tree.toString(0)
        + "\n"
        + "\nSize of the tree : "
        + m_Tree.numNodes()
        + (getMaxDepth() > 0 ? ("\nMax depth of tree: " + getMaxDepth()) : (""));
    }
  }

  /**
   * Returns graph describing the tree.
   * 
   * @return the graph describing the tree
   * @throws Exception if graph can't be computed
   */
  @Override
  public String graph() throws Exception {

    if (m_Tree == null) {
      throw new Exception("RandomTree: No model built yet.");
    }
    StringBuffer resultBuff = new StringBuffer();
    m_Tree.toGraph(resultBuff, 0, null);
    String result = "digraph RandomTree {\n" + "edge [style=bold]\n"
      + resultBuff.toString() + "\n}\n";
    return result;
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
   * Builds the classifier to generate a partition.
   */
  @Override
  public void generatePartition(Instances data) throws Exception {

    buildClassifier(data);
  }

  /**
   * Computes array that indicates node membership. Array locations are
   * allocated based on breadth-first exploration of the tree.
   */
  @Override
  public double[] getMembershipValues(Instance instance) throws Exception {

    if (m_zeroR != null) {
      double[] m = new double[1];
      m[0] = instance.weight();
      return m;
    } else {

      // Set up array for membership values
      double[] a = new double[numElements()];

      // Initialize queues
      Queue<Double> queueOfWeights = new LinkedList<Double>();
      Queue<Tree> queueOfNodes = new LinkedList<Tree>();
      queueOfWeights.add(instance.weight());
      queueOfNodes.add(m_Tree);
      int index = 0;

      // While the queue is not empty
      while (!queueOfNodes.isEmpty()) {

        a[index++] = queueOfWeights.poll();
        Tree node = queueOfNodes.poll();

        // Is node a leaf?
        if (node.m_Attribute <= -1) {
          continue;
        }

        // Compute weight distribution
        double[] weights = new double[node.m_Successors.length];
        if (instance.isMissing(node.m_Attribute)) {
          System.arraycopy(node.m_Prop, 0, weights, 0, node.m_Prop.length);
        } else if (m_Info.attribute(node.m_Attribute).isNominal()) {
          weights[(int) instance.value(node.m_Attribute)] = 1.0;
        } else {
          if (instance.value(node.m_Attribute) < node.m_SplitPoint) {
            weights[0] = 1.0;
          } else {
            weights[1] = 1.0;
          }
        }
        for (int i = 0; i < node.m_Successors.length; i++) {
          queueOfNodes.add(node.m_Successors[i]);
          queueOfWeights.add(a[index - 1] * weights[i]);
        }
      }
      return a;
    }
  }

  /**
   * Returns the number of elements in the partition.
   */
  @Override
  public int numElements() throws Exception {

    if (m_zeroR != null) {
      return 1;
    }
    return m_Tree.numNodes();
  }

  /**
   * The inner class for dealing with the tree.
   */
  protected class Tree implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = 3549573538656522569L;

    /** The subtrees appended to this tree. */
    protected Tree[] m_Successors;

    /** The attribute to split on. */
    protected int m_Attribute = -1;

    /** The split point. */
    protected double m_SplitPoint = Double.NaN;

    /** The proportions of training instances going down each branch. */
    protected double[] m_Prop = null;

    /**
     * Class probabilities from the training data in the nominal case. Holds the
     * mean in the numeric case.
     */
    protected double[] m_ClassDistribution = null;

    /**
     * Holds the sum of squared errors and the weight in the numeric case.
     */
    protected double[] m_Distribution = null;

    /**
     * Backfits the given data into the tree.
     */
    public void backfitData(Instances data) throws Exception {

      double totalWeight = 0;
      double totalSumSquared = 0;

      // Compute initial class counts
      double[] classProbs = new double[data.numClasses()];
      for (int i = 0; i < data.numInstances(); i++) {
        Instance inst = data.instance(i);
        if (data.classAttribute().isNominal()) {
          classProbs[(int) inst.classValue()] += inst.weight();
          totalWeight += inst.weight();
        } else {
          classProbs[0] += inst.classValue() * inst.weight();
          totalSumSquared += inst.classValue() * inst.classValue()
            * inst.weight();
          totalWeight += inst.weight();
        }
      }

      double trainVariance = 0;
      if (data.classAttribute().isNumeric()) {
        trainVariance = RandomTree.singleVariance(classProbs[0],
          totalSumSquared, totalWeight) / totalWeight;
        classProbs[0] /= totalWeight;
      }

      // Fit data into tree
      backfitData(data, classProbs, totalWeight);
    }

    /**
     * Computes class distribution of an instance using the decision tree.
     * 
     * @param instance the instance to compute the distribution for
     * @return the computed class distribution
     * @throws Exception if computation fails
     */
    public double[] distributionForInstance(Instance instance) throws Exception {

      double[] returnedDist = null;

      if (m_Attribute > -1) {

        // Node is not a leaf
        if (instance.isMissing(m_Attribute)) {

          // Value is missing
          returnedDist = new double[m_Info.numClasses()];

          // Split instance up
          for (int i = 0; i < m_Successors.length; i++) {
            double[] help = m_Successors[i].distributionForInstance(instance);
            if (help != null) {
              for (int j = 0; j < help.length; j++) {
                returnedDist[j] += m_Prop[i] * help[j];
              }
            }
          }
        } else if (m_Info.attribute(m_Attribute).isNominal()) {

          // For nominal attributes
          returnedDist = m_Successors[(int) instance.value(m_Attribute)]
            .distributionForInstance(instance);
        } else {

          // For numeric attributes
          if (instance.value(m_Attribute) < m_SplitPoint) {
            returnedDist = m_Successors[0].distributionForInstance(instance);
          } else {
            returnedDist = m_Successors[1].distributionForInstance(instance);
          }
        }
      }

      // Node is a leaf or successor is empty?
      if ((m_Attribute == -1) || (returnedDist == null)) {

        // Is node empty?
        if (m_ClassDistribution == null) {
          if (getAllowUnclassifiedInstances()) {
            double[] result = new double[m_Info.numClasses()];
            if (m_Info.classAttribute().isNumeric()) {
              result[0] = Utils.missingValue();
            }
            return result;
          } else {
            return null;
          }
        }

        // Else return normalized distribution
        double[] normalizedDistribution = m_ClassDistribution.clone();
        if (m_Info.classAttribute().isNominal()) {
          Utils.normalize(normalizedDistribution);
        }
        return normalizedDistribution;
      } else {
        return returnedDist;
      }
    }

    /**
     * Outputs one node for graph.
     * 
     * @param text the buffer to append the output to
     * @param num unique node id
     * @return the next node id
     * @throws Exception if generation fails
     */
    public int toGraph(StringBuffer text, int num) throws Exception {

      int maxIndex = Utils.maxIndex(m_ClassDistribution);
      String classValue = m_Info.classAttribute().isNominal() ? m_Info
        .classAttribute().value(maxIndex) : Utils.doubleToString(
        m_ClassDistribution[0], 2);

      num++;
      if (m_Attribute == -1) {
        text.append("N" + Integer.toHexString(hashCode()) + " [label=\"" + num
          + ": " + classValue + "\"" + "shape=box]\n");
      } else {
        text.append("N" + Integer.toHexString(hashCode()) + " [label=\"" + num
          + ": " + classValue + "\"]\n");
        for (int i = 0; i < m_Successors.length; i++) {
          text.append("N" + Integer.toHexString(hashCode()) + "->" + "N"
            + Integer.toHexString(m_Successors[i].hashCode()) + " [label=\""
            + m_Info.attribute(m_Attribute).name());
          if (m_Info.attribute(m_Attribute).isNumeric()) {
            if (i == 0) {
              text.append(" < " + Utils.doubleToString(m_SplitPoint, 2));
            } else {
              text.append(" >= " + Utils.doubleToString(m_SplitPoint, 2));
            }
          } else {
            text.append(" = " + m_Info.attribute(m_Attribute).value(i));
          }
          text.append("\"]\n");
          num = m_Successors[i].toGraph(text, num);
        }
      }

      return num;
    }

    /**
     * Outputs a leaf.
     * 
     * @return the leaf as string
     * @throws Exception if generation fails
     */
    protected String leafString() throws Exception {

      double sum = 0, maxCount = 0;
      int maxIndex = 0;
      double classMean = 0;
      double avgError = 0;
      if (m_ClassDistribution != null) {
        if (m_Info.classAttribute().isNominal()) {
          sum = Utils.sum(m_ClassDistribution);
          maxIndex = Utils.maxIndex(m_ClassDistribution);
          maxCount = m_ClassDistribution[maxIndex];
        } else {
          classMean = m_ClassDistribution[0];
          if (m_Distribution[1] > 0) {
            avgError = m_Distribution[0] / m_Distribution[1];
          }
        }
      }

      if (m_Info.classAttribute().isNumeric()) {
        return " : " + Utils.doubleToString(classMean, 2) + " ("
          + Utils.doubleToString(m_Distribution[1], 2) + "/"
          + Utils.doubleToString(avgError, 2) + ")";
      }

      return " : " + m_Info.classAttribute().value(maxIndex) + " ("
        + Utils.doubleToString(sum, 2) + "/"
        + Utils.doubleToString(sum - maxCount, 2) + ")";
    }

    /**
     * Recursively outputs the tree.
     * 
     * @param level the current level of the tree
     * @return the generated subtree
     */
    protected String toString(int level) {

      try {
        StringBuffer text = new StringBuffer();

        if (m_Attribute == -1) {

          // Output leaf info
          return leafString();
        } else if (m_Info.attribute(m_Attribute).isNominal()) {

          // For nominal attributes
          for (int i = 0; i < m_Successors.length; i++) {
            text.append("\n");
            for (int j = 0; j < level; j++) {
              text.append("|   ");
            }
            text.append(m_Info.attribute(m_Attribute).name() + " = "
              + m_Info.attribute(m_Attribute).value(i));
            text.append(m_Successors[i].toString(level + 1));
          }
        } else {

          // For numeric attributes
          text.append("\n");
          for (int j = 0; j < level; j++) {
            text.append("|   ");
          }
          text.append(m_Info.attribute(m_Attribute).name() + " < "
            + Utils.doubleToString(m_SplitPoint, 2));
          text.append(m_Successors[0].toString(level + 1));
          text.append("\n");
          for (int j = 0; j < level; j++) {
            text.append("|   ");
          }
          text.append(m_Info.attribute(m_Attribute).name() + " >= "
            + Utils.doubleToString(m_SplitPoint, 2));
          text.append(m_Successors[1].toString(level + 1));
        }

        return text.toString();
      } catch (Exception e) {
        e.printStackTrace();
        return "RandomTree: tree can't be printed";
      }
    }

    /**
     * Recursively backfits data into the tree.
     * 
     * @param data the data to work with
     * @param classProbs the class distribution
     * @throws Exception if generation fails
     */
    protected void backfitData(Instances data, double[] classProbs,
      double totalWeight) throws Exception {

      // Make leaf if there are no training instances
      if (data.numInstances() == 0) {
        m_Attribute = -1;
        m_ClassDistribution = null;
        if (data.classAttribute().isNumeric()) {
          m_Distribution = new double[2];
        }
        m_Prop = null;
        return;
      }

      double priorVar = 0;
      if (data.classAttribute().isNumeric()) {

        // Compute prior variance
        double totalSum = 0, totalSumSquared = 0, totalSumOfWeights = 0;
        for (int i = 0; i < data.numInstances(); i++) {
          Instance inst = data.instance(i);
          totalSum += inst.classValue() * inst.weight();
          totalSumSquared += inst.classValue() * inst.classValue()
            * inst.weight();
          totalSumOfWeights += inst.weight();
        }
        priorVar = RandomTree.singleVariance(totalSum, totalSumSquared,
          totalSumOfWeights);
      }

      // Check if node doesn't contain enough instances or is pure
      // or maximum depth reached
      m_ClassDistribution = classProbs.clone();

      /*
       * if (Utils.sum(m_ClassDistribution) < 2 * m_MinNum ||
       * Utils.eq(m_ClassDistribution[Utils.maxIndex(m_ClassDistribution)],
       * Utils .sum(m_ClassDistribution))) {
       * 
       * // Make leaf m_Attribute = -1; m_Prop = null; return; }
       */

      // Are we at an inner node
      if (m_Attribute > -1) {

        // Compute new weights for subsets based on backfit data
        m_Prop = new double[m_Successors.length];
        for (int i = 0; i < data.numInstances(); i++) {
          Instance inst = data.instance(i);
          if (!inst.isMissing(m_Attribute)) {
            if (data.attribute(m_Attribute).isNominal()) {
              m_Prop[(int) inst.value(m_Attribute)] += inst.weight();
            } else {
              m_Prop[(inst.value(m_Attribute) < m_SplitPoint) ? 0 : 1] += inst
                .weight();
            }
          }
        }

        // If we only have missing values we can make this node into a leaf
        if (Utils.sum(m_Prop) <= 0) {
          m_Attribute = -1;
          m_Prop = null;

          if (data.classAttribute().isNumeric()) {
            m_Distribution = new double[2];
            m_Distribution[0] = priorVar;
            m_Distribution[1] = totalWeight;
          }

          return;
        }

        // Otherwise normalize the proportions
        Utils.normalize(m_Prop);

        // Split data
        Instances[] subsets = splitData(data);

        // Go through subsets
        for (int i = 0; i < subsets.length; i++) {

          // Compute distribution for current subset
          double[] dist = new double[data.numClasses()];
          double sumOfWeights = 0;
          for (int j = 0; j < subsets[i].numInstances(); j++) {
            if (data.classAttribute().isNominal()) {
              dist[(int) subsets[i].instance(j).classValue()] += subsets[i]
                .instance(j).weight();
            } else {
              dist[0] += subsets[i].instance(j).classValue()
                * subsets[i].instance(j).weight();
              sumOfWeights += subsets[i].instance(j).weight();
            }
          }

          if (sumOfWeights > 0) {
            dist[0] /= sumOfWeights;
          }

          // Backfit subset
          m_Successors[i].backfitData(subsets[i], dist, totalWeight);
        }

        // If unclassified instances are allowed, we don't need to store the
        // class distribution
        if (getAllowUnclassifiedInstances()) {
          m_ClassDistribution = null;
          return;
        }

        for (int i = 0; i < subsets.length; i++) {
          if (m_Successors[i].m_ClassDistribution == null) {
            return;
          }
        }
        m_ClassDistribution = null;

        // If we have a least two non-empty successors, we should keep this tree
        /*
         * int nonEmptySuccessors = 0; for (int i = 0; i < subsets.length; i++)
         * { if (m_Successors[i].m_ClassDistribution != null) {
         * nonEmptySuccessors++; if (nonEmptySuccessors > 1) { return; } } }
         * 
         * // Otherwise, this node is a leaf or should become a leaf
         * m_Successors = null; m_Attribute = -1; m_Prop = null; return;
         */
      }
    }

    /**
     * Recursively generates a tree.
     * 
     * @param data the data to work with
     * @param classProbs the class distribution
     * @param attIndicesWindow the attribute window to choose attributes from
     * @param random random number generator for choosing random attributes
     * @param depth the current depth
     * @throws Exception if generation fails
     */
    protected void buildTree(Instances data, double[] classProbs,
      int[] attIndicesWindow, double totalWeight, Random random, int depth,
      double minVariance) throws Exception {

      // Make leaf if there are no training instances
      if (data.numInstances() == 0) {
        m_Attribute = -1;
        m_ClassDistribution = null;
        m_Prop = null;

        if (data.classAttribute().isNumeric()) {
          m_Distribution = new double[2];
        }
        return;
      }

      double priorVar = 0;
      if (data.classAttribute().isNumeric()) {

        // Compute prior variance
        double totalSum = 0, totalSumSquared = 0, totalSumOfWeights = 0;
        for (int i = 0; i < data.numInstances(); i++) {
          Instance inst = data.instance(i);
          totalSum += inst.classValue() * inst.weight();
          totalSumSquared += inst.classValue() * inst.classValue()
            * inst.weight();
          totalSumOfWeights += inst.weight();
        }
        priorVar = RandomTree.singleVariance(totalSum, totalSumSquared,
          totalSumOfWeights);
      }

      // Check if node doesn't contain enough instances or is pure
      // or maximum depth reached
      if (data.classAttribute().isNominal()) {
        totalWeight = Utils.sum(classProbs);
      }
      // System.err.println("Total weight " + totalWeight);
      // double sum = Utils.sum(classProbs);
      if (totalWeight < 2 * m_MinNum ||

      // Nominal case
        (data.classAttribute().isNominal() && Utils.eq(
          classProbs[Utils.maxIndex(classProbs)], Utils.sum(classProbs)))

        ||

        // Numeric case
        (data.classAttribute().isNumeric() && priorVar / totalWeight < minVariance)

        ||

        // check tree depth
        ((getMaxDepth() > 0) && (depth >= getMaxDepth()))) {

        // Make leaf
        m_Attribute = -1;
        m_ClassDistribution = classProbs.clone();
        if (data.classAttribute().isNumeric()) {
          m_Distribution = new double[2];
          m_Distribution[0] = priorVar;
          m_Distribution[1] = totalWeight;
        }

        m_Prop = null;
        return;
      }

      // Compute class distributions and value of splitting
      // criterion for each attribute
      double val = -Double.MAX_VALUE;
      double split = -Double.MAX_VALUE;
      double[][] bestDists = null;
      double[] bestProps = null;
      int bestIndex = 0;

      // Handles to get arrays out of distribution method
      double[][] props = new double[1][0];
      double[][][] dists = new double[1][0][0];
      double[][] totalSubsetWeights = new double[data.numAttributes()][0];

      // Investigate K random attributes
      int attIndex = 0;
      int windowSize = attIndicesWindow.length;
      int k = m_KValue;
      boolean gainFound = false;
      double[] tempNumericVals = new double[data.numAttributes()];
      while ((windowSize > 0) && (k-- > 0 || !gainFound)) {

        int chosenIndex = random.nextInt(windowSize);
        attIndex = attIndicesWindow[chosenIndex];

        // shift chosen attIndex out of window
        attIndicesWindow[chosenIndex] = attIndicesWindow[windowSize - 1];
        attIndicesWindow[windowSize - 1] = attIndex;
        windowSize--;

        double currSplit = data.classAttribute().isNominal() ? distribution(
          props, dists, attIndex, data) : numericDistribution(props, dists,
          attIndex, totalSubsetWeights, data, tempNumericVals);

        double currVal = data.classAttribute().isNominal() ? gain(dists[0],
          priorVal(dists[0])) : tempNumericVals[attIndex];

        if (Utils.gr(currVal, 0)) {
          gainFound = true;
        }

        if ((currVal > val) || ((!getBreakTiesRandomly()) && (currVal == val) && (attIndex < bestIndex))) {
          val = currVal;
          bestIndex = attIndex;
          split = currSplit;
          bestProps = props[0];
          bestDists = dists[0];
        }
      }

      // Find best attribute
      m_Attribute = bestIndex;

      // Any useful split found?
      if (Utils.gr(val, 0)) {

        // Build subtrees
        m_SplitPoint = split;
        m_Prop = bestProps;
        Instances[] subsets = splitData(data);
        m_Successors = new Tree[bestDists.length];
        double[] attTotalSubsetWeights = totalSubsetWeights[bestIndex];

        for (int i = 0; i < bestDists.length; i++) {
          m_Successors[i] = new Tree();
          m_Successors[i].buildTree(subsets[i], bestDists[i], attIndicesWindow,
            data.classAttribute().isNominal() ? 0 : attTotalSubsetWeights[i],
            random, depth + 1, minVariance);
        }

        // If all successors are non-empty, we don't need to store the class
        // distribution
        boolean emptySuccessor = false;
        for (int i = 0; i < subsets.length; i++) {
          if (m_Successors[i].m_ClassDistribution == null) {
            emptySuccessor = true;
            break;
          }
        }
        if (emptySuccessor) {
          m_ClassDistribution = classProbs.clone();
        }
      } else {

        // Make leaf
        m_Attribute = -1;
        m_ClassDistribution = classProbs.clone();
        if (data.classAttribute().isNumeric()) {
          m_Distribution = new double[2];
          m_Distribution[0] = priorVar;
          m_Distribution[1] = totalWeight;
        }
      }
    }

    /**
     * Computes size of the tree.
     * 
     * @return the number of nodes
     */
    public int numNodes() {

      if (m_Attribute == -1) {
        return 1;
      } else {
        int size = 1;
        for (Tree m_Successor : m_Successors) {
          size += m_Successor.numNodes();
        }
        return size;
      }
    }

    /**
     * Splits instances into subsets based on the given split.
     * 
     * @param data the data to work with
     * @return the subsets of instances
     * @throws Exception if something goes wrong
     */
    protected Instances[] splitData(Instances data) throws Exception {

      // Allocate array of Instances objects
      Instances[] subsets = new Instances[m_Prop.length];
      for (int i = 0; i < m_Prop.length; i++) {
        subsets[i] = new Instances(data, data.numInstances());
      }

      // Go through the data
      for (int i = 0; i < data.numInstances(); i++) {

        // Get instance
        Instance inst = data.instance(i);

        // Does the instance have a missing value?
        if (inst.isMissing(m_Attribute)) {

          // Split instance up
          for (int k = 0; k < m_Prop.length; k++) {
            if (m_Prop[k] > 0) {
              Instance copy = (Instance) inst.copy();
              copy.setWeight(m_Prop[k] * inst.weight());
              subsets[k].add(copy);
            }
          }

          // Proceed to next instance
          continue;
        }

        // Do we have a nominal attribute?
        if (data.attribute(m_Attribute).isNominal()) {
          subsets[(int) inst.value(m_Attribute)].add(inst);

          // Proceed to next instance
          continue;
        }

        // Do we have a numeric attribute?
        if (data.attribute(m_Attribute).isNumeric()) {
          subsets[(inst.value(m_Attribute) < m_SplitPoint) ? 0 : 1].add(inst);

          // Proceed to next instance
          continue;
        }

        // Else throw an exception
        throw new IllegalArgumentException("Unknown attribute type");
      }

      // Save memory
      for (int i = 0; i < m_Prop.length; i++) {
        subsets[i].compactify();
      }

      // Return the subsets
      return subsets;
    }

    /**
     * Computes numeric class distribution for an attribute
     * 
     * @param props
     * @param dists
     * @param att
     * @param subsetWeights
     * @param data
     * @param vals
     * @return
     * @throws Exception if a problem occurs
     */
    protected double numericDistribution(double[][] props, double[][][] dists,
      int att, double[][] subsetWeights, Instances data, double[] vals)
      throws Exception {

      double splitPoint = Double.NaN;
      Attribute attribute = data.attribute(att);
      double[][] dist = null;
      double[] sums = null;
      double[] sumSquared = null;
      double[] sumOfWeights = null;
      double totalSum = 0, totalSumSquared = 0, totalSumOfWeights = 0;
      int indexOfFirstMissingValue = data.numInstances();

      if (attribute.isNominal()) {
        sums = new double[attribute.numValues()];
        sumSquared = new double[attribute.numValues()];
        sumOfWeights = new double[attribute.numValues()];
        int attVal;

        for (int i = 0; i < data.numInstances(); i++) {
          Instance inst = data.instance(i);
          if (inst.isMissing(att)) {

            // Skip missing values at this stage
            if (indexOfFirstMissingValue == data.numInstances()) {
              indexOfFirstMissingValue = i;
            }
            continue;
          }

          attVal = (int) inst.value(att);
          sums[attVal] += inst.classValue() * inst.weight();
          sumSquared[attVal] += inst.classValue() * inst.classValue()
            * inst.weight();
          sumOfWeights[attVal] += inst.weight();
        }

        totalSum = Utils.sum(sums);
        totalSumSquared = Utils.sum(sumSquared);
        totalSumOfWeights = Utils.sum(sumOfWeights);
      } else {
        // For numeric attributes
        sums = new double[2];
        sumSquared = new double[2];
        sumOfWeights = new double[2];
        double[] currSums = new double[2];
        double[] currSumSquared = new double[2];
        double[] currSumOfWeights = new double[2];

        // Sort data
        data.sort(att);

        // Move all instances into second subset
        for (int j = 0; j < data.numInstances(); j++) {
          Instance inst = data.instance(j);
          if (inst.isMissing(att)) {

            // Can stop as soon as we hit a missing value
            indexOfFirstMissingValue = j;
            break;
          }

          currSums[1] += inst.classValue() * inst.weight();
          currSumSquared[1] += inst.classValue() * inst.classValue()
            * inst.weight();
          currSumOfWeights[1] += inst.weight();
        }

        totalSum = currSums[1];
        totalSumSquared = currSumSquared[1];
        totalSumOfWeights = currSumOfWeights[1];

        sums[1] = currSums[1];
        sumSquared[1] = currSumSquared[1];
        sumOfWeights[1] = currSumOfWeights[1];

        // Try all possible split points
        double currSplit = data.instance(0).value(att);
        double currVal, bestVal = Double.MAX_VALUE;

        for (int i = 0; i < indexOfFirstMissingValue; i++) {
          Instance inst = data.instance(i);

          if (inst.value(att) > currSplit) {
            currVal = RandomTree.variance(currSums, currSumSquared,
              currSumOfWeights);
            if (currVal < bestVal) {
              bestVal = currVal;
              splitPoint = (inst.value(att) + currSplit) / 2.0;

              // Check for numeric precision problems
              if (splitPoint <= currSplit) {
                splitPoint = inst.value(att);
              }

              for (int j = 0; j < 2; j++) {
                sums[j] = currSums[j];
                sumSquared[j] = currSumSquared[j];
                sumOfWeights[j] = currSumOfWeights[j];
              }
            }
          }

          currSplit = inst.value(att);

          double classVal = inst.classValue() * inst.weight();
          double classValSquared = inst.classValue() * classVal;

          currSums[0] += classVal;
          currSumSquared[0] += classValSquared;
          currSumOfWeights[0] += inst.weight();

          currSums[1] -= classVal;
          currSumSquared[1] -= classValSquared;
          currSumOfWeights[1] -= inst.weight();
        }
      }

      // Compute weights
      props[0] = new double[sums.length];
      for (int k = 0; k < props[0].length; k++) {
        props[0][k] = sumOfWeights[k];
      }
      if (!(Utils.sum(props[0]) > 0)) {
        for (int k = 0; k < props[0].length; k++) {
          props[0][k] = 1.0 / props[0].length;
        }
      } else {
        Utils.normalize(props[0]);
      }

      // Distribute weights for instances with missing values
      for (int i = indexOfFirstMissingValue; i < data.numInstances(); i++) {
        Instance inst = data.instance(i);

        for (int j = 0; j < sums.length; j++) {
          sums[j] += props[0][j] * inst.classValue() * inst.weight();
          sumSquared[j] += props[0][j] * inst.classValue() * inst.classValue()
            * inst.weight();
          sumOfWeights[j] += props[0][j] * inst.weight();
        }
        totalSum += inst.classValue() * inst.weight();
        totalSumSquared += inst.classValue() * inst.classValue()
          * inst.weight();
        totalSumOfWeights += inst.weight();
      }

      // Compute final distribution
      dist = new double[sums.length][data.numClasses()];
      for (int j = 0; j < sums.length; j++) {
        if (sumOfWeights[j] > 0) {
          dist[j][0] = sums[j] / sumOfWeights[j];
        } else {
          dist[j][0] = totalSum / totalSumOfWeights;
        }
      }

      // Compute variance gain
      double priorVar = singleVariance(totalSum, totalSumSquared,
        totalSumOfWeights);
      double var = variance(sums, sumSquared, sumOfWeights);
      double gain = priorVar - var;

      // Return distribution and split point
      subsetWeights[att] = sumOfWeights;
      dists[0] = dist;
      vals[att] = gain;

      return splitPoint;
    }

    /**
     * Computes class distribution for an attribute.
     * 
     * @param props
     * @param dists
     * @param att the attribute index
     * @param data the data to work with
     * @throws Exception if something goes wrong
     */
    protected double distribution(double[][] props, double[][][] dists,
      int att, Instances data) throws Exception {

      double splitPoint = Double.NaN;
      Attribute attribute = data.attribute(att);
      double[][] dist = null;
      int indexOfFirstMissingValue = data.numInstances();

      if (attribute.isNominal()) {

        // For nominal attributes
        dist = new double[attribute.numValues()][data.numClasses()];
        for (int i = 0; i < data.numInstances(); i++) {
          Instance inst = data.instance(i);
          if (inst.isMissing(att)) {

            // Skip missing values at this stage
            if (indexOfFirstMissingValue == data.numInstances()) {
              indexOfFirstMissingValue = i;
            }
            continue;
          }
          dist[(int) inst.value(att)][(int) inst.classValue()] += inst.weight();
        }
      } else {

        // For numeric attributes
        double[][] currDist = new double[2][data.numClasses()];
        dist = new double[2][data.numClasses()];

        // Sort data
        data.sort(att);

        // Move all instances into second subset
        for (int j = 0; j < data.numInstances(); j++) {
          Instance inst = data.instance(j);
          if (inst.isMissing(att)) {

            // Can stop as soon as we hit a missing value
            indexOfFirstMissingValue = j;
            break;
          }
          currDist[1][(int) inst.classValue()] += inst.weight();
        }

        // Value before splitting
        double priorVal = priorVal(currDist);

        // Save initial distribution
        for (int j = 0; j < currDist.length; j++) {
          System.arraycopy(currDist[j], 0, dist[j], 0, dist[j].length);
        }

        // Try all possible split points
        double currSplit = data.instance(0).value(att);
        double currVal, bestVal = -Double.MAX_VALUE;
        for (int i = 0; i < indexOfFirstMissingValue; i++) {
          Instance inst = data.instance(i);
          double attVal = inst.value(att);

          // Can we place a sensible split point here?
          if (attVal > currSplit) {

            // Compute gain for split point
            currVal = gain(currDist, priorVal);

            // Is the current split point the best point so far?
            if (currVal > bestVal) {

              // Store value of current point
              bestVal = currVal;

              // Save split point
              splitPoint = (attVal + currSplit) / 2.0;

              // Check for numeric precision problems
              if (splitPoint <= currSplit) {
                splitPoint = attVal;
              }

              // Save distribution
              for (int j = 0; j < currDist.length; j++) {
                System.arraycopy(currDist[j], 0, dist[j], 0, dist[j].length);
              }
            }

            // Update value
            currSplit = attVal;
          }

          // Shift over the weight
          int classVal = (int) inst.classValue();
          currDist[0][classVal] += inst.weight();
          currDist[1][classVal] -= inst.weight();
        }
      }

      // Compute weights for subsets
      props[0] = new double[dist.length];
      for (int k = 0; k < props[0].length; k++) {
        props[0][k] = Utils.sum(dist[k]);
      }
      if (Utils.eq(Utils.sum(props[0]), 0)) {
        for (int k = 0; k < props[0].length; k++) {
          props[0][k] = 1.0 / props[0].length;
        }
      } else {
        Utils.normalize(props[0]);
      }

      // Distribute weights for instances with missing values
      for (int i = indexOfFirstMissingValue; i < data.numInstances(); i++) {
        Instance inst = data.instance(i);
        if (attribute.isNominal()) {

          // Need to check if attribute value is missing
          if (inst.isMissing(att)) {
            for (int j = 0; j < dist.length; j++) {
              dist[j][(int) inst.classValue()] += props[0][j] * inst.weight();
            }
          }
        } else {

          // Can be sure that value is missing, so no test required
          for (int j = 0; j < dist.length; j++) {
            dist[j][(int) inst.classValue()] += props[0][j] * inst.weight();
          }
        }
      }

      // Return distribution and split point
      dists[0] = dist;
      return splitPoint;
    }

    /**
     * Computes value of splitting criterion before split.
     * 
     * @param dist the distributions
     * @return the splitting criterion
     */
    protected double priorVal(double[][] dist) {

      return ContingencyTables.entropyOverColumns(dist);
    }

    /**
     * Computes value of splitting criterion after split.
     * 
     * @param dist the distributions
     * @param priorVal the splitting criterion
     * @return the gain after the split
     */
    protected double gain(double[][] dist, double priorVal) {

      return priorVal - ContingencyTables.entropyConditionedOnRows(dist);
    }

    /**
     * Returns the revision string.
     * 
     * @return the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 12505 $");
    }

    /**
     * Outputs one node for graph.
     * 
     * @param text the buffer to append the output to
     * @param num the current node id
     * @param parent the parent of the nodes
     * @return the next node id
     * @throws Exception if something goes wrong
     */
    protected int toGraph(StringBuffer text, int num, Tree parent)
      throws Exception {

      num++;
      if (m_Attribute == -1) {
        text.append("N" + Integer.toHexString(Tree.this.hashCode())
          + " [label=\"" + num + Utils.backQuoteChars(leafString()) + "\""
          + " shape=box]\n");

      } else {
        text.append("N" + Integer.toHexString(Tree.this.hashCode())
          + " [label=\"" + num + ": "
          + Utils.backQuoteChars(m_Info.attribute(m_Attribute).name())
          + "\"]\n");
        for (int i = 0; i < m_Successors.length; i++) {
          text.append("N" + Integer.toHexString(Tree.this.hashCode()) + "->"
            + "N" + Integer.toHexString(m_Successors[i].hashCode())
            + " [label=\"");
          if (m_Info.attribute(m_Attribute).isNumeric()) {
            if (i == 0) {
              text.append(" < " + Utils.doubleToString(m_SplitPoint, 2));
            } else {
              text.append(" >= " + Utils.doubleToString(m_SplitPoint, 2));
            }
          } else {
            text.append(" = "
              + Utils.backQuoteChars(m_Info.attribute(m_Attribute).value(i)));
          }
          text.append("\"]\n");
          num = m_Successors[i].toGraph(text, num, this);
        }
      }

      return num;
    }
  }

  /**
   * Computes variance for subsets.
   * 
   * @param s
   * @param sS
   * @param sumOfWeights
   * @return the variance
   */
  protected static double variance(double[] s, double[] sS,
    double[] sumOfWeights) {

    double var = 0;

    for (int i = 0; i < s.length; i++) {
      if (sumOfWeights[i] > 0) {
        var += singleVariance(s[i], sS[i], sumOfWeights[i]);
      }
    }

    return var;
  }

  /**
   * Computes the variance for a single set
   * 
   * @param s
   * @param sS
   * @param weight the weight
   * @return the variance
   */
  protected static double singleVariance(double s, double sS, double weight) {

    return sS - ((s * s) / weight);
  }

  /**
   * Main method for this class.
   * 
   * @param argv the commandline parameters
   */
  public static void main(String[] argv) {
    runClassifier(new RandomTree(), argv);
  }
}

