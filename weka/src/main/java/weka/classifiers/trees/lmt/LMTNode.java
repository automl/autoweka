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
 *    LMTNode.java
 *    Copyright (C) 2003-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.lmt;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.j48.ClassifierSplitModel;
import weka.classifiers.trees.j48.ModelSelection;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.supervised.attribute.NominalToBinary;

/**
 * Auxiliary class for list of LMTNodes
 */
class CompareNode implements Comparator<LMTNode>, RevisionHandler {

  /**
   * Compares its two arguments for order.
   * 
   * @param o1 first object
   * @param o2 second object
   * @return a negative integer, zero, or a positive integer as the first
   *         argument is less than, equal to, or greater than the second.
   */
  @Override
  public int compare(LMTNode o1, LMTNode o2) {
    if (o1.m_alpha < o2.m_alpha) {
      return -1;
    }
    if (o1.m_alpha > o2.m_alpha) {
      return 1;
    }
    return 0;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11566 $");
  }
}

/**
 * Class for logistic model tree structure.
 * 
 * 
 * @author Niels Landwehr
 * @author Marc Sumner
 * @version $Revision: 11566 $
 */
public class LMTNode extends LogisticBase {

  /** for serialization */
  static final long serialVersionUID = 1862737145870398755L;

  /** Total number of training instances. */
  protected double m_totalInstanceWeight;

  /** Node id */
  protected int m_id;

  /** ID of logistic model at leaf */
  protected int m_leafModelNum;

  /** Alpha-value (for pruning) at the node */
  public double m_alpha;

  /**
   * Weighted number of training examples currently misclassified by the
   * logistic model at the node
   */
  public double m_numIncorrectModel;

  /**
   * Weighted number of training examples currently misclassified by the subtree
   * rooted at the node
   */
  public double m_numIncorrectTree;

  /** minimum number of instances at which a node is considered for splitting */
  protected int m_minNumInstances;

  /** ModelSelection object (for splitting) */
  protected ModelSelection m_modelSelection;

  /** Filter to convert nominal attributes to binary */
  protected NominalToBinary m_nominalToBinary;

  /** Number of folds for CART pruning */
  protected static int m_numFoldsPruning = 5;

  /**
   * Use heuristic that determines the number of LogitBoost iterations only once
   * in the beginning?
   */
  protected boolean m_fastRegression;

  /** Number of instances at the node */
  protected int m_numInstances;

  /** The ClassifierSplitModel (for splitting) */
  protected ClassifierSplitModel m_localModel;

  /** Array of children of the node */
  protected LMTNode[] m_sons;

  /** True if node is leaf */
  protected boolean m_isLeaf;

  /**
   * Constructor for logistic model tree node.
   * 
   * @param modelSelection selection method for local splitting model
   * @param numBoostingIterations sets the numBoostingIterations parameter
   * @param fastRegression sets the fastRegression parameter
   * @param errorOnProbabilities Use error on probabilities for stopping
   *          criterion of LogitBoost?
   * @param minNumInstances minimum number of instances at which a node is
   *          considered for splitting
   */
  public LMTNode(ModelSelection modelSelection, int numBoostingIterations,
    boolean fastRegression, boolean errorOnProbabilities, int minNumInstances,
    double weightTrimBeta, boolean useAIC, NominalToBinary ntb, int numDecimalPlaces) {
    m_modelSelection = modelSelection;
    m_fixedNumIterations = numBoostingIterations;
    m_fastRegression = fastRegression;
    m_errorOnProbabilities = errorOnProbabilities;
    m_minNumInstances = minNumInstances;
    m_maxIterations = 200;
    setWeightTrimBeta(weightTrimBeta);
    setUseAIC(useAIC);
    m_nominalToBinary = ntb;
    m_numDecimalPlaces = numDecimalPlaces;
  }

  /**
   * Method for building a logistic model tree (only called for the root node).
   * Grows an initial logistic model tree and prunes it back using the CART
   * pruning scheme.
   * 
   * @param data the data to train with
   * @throws Exception if something goes wrong
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {

    // heuristic to avoid cross-validating the number of LogitBoost iterations
    // at every node: build standalone logistic model and take its optimum
    // number
    // of iteration everywhere in the tree.
    if (m_fastRegression && (m_fixedNumIterations < 0)) {
      m_fixedNumIterations = tryLogistic(data);
    }

    // Need to cross-validate alpha-parameter for CART-pruning
    Instances cvData = new Instances(data);
    cvData.stratify(m_numFoldsPruning);

    double[][] alphas = new double[m_numFoldsPruning][];
    double[][] errors = new double[m_numFoldsPruning][];

    for (int i = 0; i < m_numFoldsPruning; i++) {
      // for every fold, grow tree on training set...
      Instances train = cvData.trainCV(m_numFoldsPruning, i);
      Instances test = cvData.testCV(m_numFoldsPruning, i);

      buildTree(train, null, train.numInstances(), 0, null);

      int numNodes = getNumInnerNodes();
      alphas[i] = new double[numNodes + 2];
      errors[i] = new double[numNodes + 2];

      // ... then prune back and log alpha-values and errors on test set
      prune(alphas[i], errors[i], test);
    }

    // don't need CV data anymore
    cvData = null;

    // build tree using all the data
    buildTree(data, null, data.numInstances(), 0, null);
    int numNodes = getNumInnerNodes();

    double[] treeAlphas = new double[numNodes + 2];

    // prune back and log alpha-values
    int iterations = prune(treeAlphas, null, null);

    double[] treeErrors = new double[numNodes + 2];

    for (int i = 0; i <= iterations; i++) {
      // compute midpoint alphas
      double alpha = Math.sqrt(treeAlphas[i] * treeAlphas[i + 1]);
      double error = 0;

      // compute error estimate for final trees from the midpoint-alphas and the
      // error estimates gotten in
      // the cross-validation
      for (int k = 0; k < m_numFoldsPruning; k++) {
        int l = 0;
        while (alphas[k][l] <= alpha) {
          l++;
        }
        error += errors[k][l - 1];
      }

      treeErrors[i] = error;
    }

    // find best alpha
    int best = -1;
    double bestError = Double.MAX_VALUE;
    for (int i = iterations; i >= 0; i--) {
      if (treeErrors[i] < bestError) {
        bestError = treeErrors[i];
        best = i;
      }
    }

    double bestAlpha = Math.sqrt(treeAlphas[best] * treeAlphas[best + 1]);

    // "unprune" final tree (faster than regrowing it)
    unprune();

    // CART-prune it with best alpha
    prune(bestAlpha);
  }

  /**
   * Method for building the tree structure. Builds a logistic model, splits the
   * node and recursively builds tree for child nodes.
   * 
   * @param data the training data passed on to this node
   * @param higherRegressions An array of regression functions produced by
   *          LogitBoost at higher levels in the tree. They represent a logistic
   *          regression model that is refined locally at this node.
   * @param totalInstanceWeight the total number of training examples
   * @param higherNumParameters effective number of parameters in the logistic
   *          regression model built in parent nodes
   * @throws Exception if something goes wrong
   */
  public void buildTree(Instances data,
    SimpleLinearRegression[][] higherRegressions, double totalInstanceWeight,
    double higherNumParameters, Instances numericDataHeader) throws Exception {

    // save some stuff
    m_totalInstanceWeight = totalInstanceWeight;
    m_train = data; // no need to copy the data here

    m_isLeaf = true;
    m_sons = null;

    m_numInstances = m_train.numInstances();
    m_numClasses = m_train.numClasses();

    // init
    m_numericDataHeader = numericDataHeader;
    m_numericData = getNumericData(m_train);

    if (higherRegressions == null) {
      m_regressions = initRegressions();
    } else {
      m_regressions = higherRegressions;
    }

    m_numParameters = higherNumParameters;
    m_numRegressions = 0;

    // build logistic model
    if (m_numInstances >= m_numFoldsBoosting) {
      if (m_fixedNumIterations > 0) {
        performBoosting(m_fixedNumIterations);
      } else if (getUseAIC()) {
        performBoostingInfCriterion();
      } else {
        performBoostingCV();
      }
    }

    m_numParameters += m_numRegressions;

    // store performance of model at this node
    Evaluation eval = new Evaluation(m_train);
    eval.evaluateModel(this, m_train);
    m_numIncorrectModel = eval.incorrect();

    boolean grow;
    // split node if more than minNumInstances...
    if (m_numInstances > m_minNumInstances) {
      // split node: either splitting on class value (a la C4.5) or splitting on
      // residuals
      if (m_modelSelection instanceof ResidualModelSelection) {
        // need ps/Ys/Zs/weights
        double[][] probs = getProbs(getFs(m_numericData));
        double[][] trainYs = getYs(m_train);
        double[][] dataZs = getZs(probs, trainYs);
        double[][] dataWs = getWs(probs, trainYs);
        m_localModel = ((ResidualModelSelection) m_modelSelection).selectModel(
          m_train, dataZs, dataWs);
      } else {
        m_localModel = m_modelSelection.selectModel(m_train);
      }
      // ... and valid split found
      grow = (m_localModel.numSubsets() > 1);
    } else {
      grow = false;
    }

    if (grow) {
      // create and build children of node
      m_isLeaf = false;
      Instances[] localInstances = m_localModel.split(m_train);

      // don't need data anymore, so clean up
      cleanup();

      m_sons = new LMTNode[m_localModel.numSubsets()];
      for (int i = 0; i < m_sons.length; i++) {
        m_sons[i] = new LMTNode(m_modelSelection, m_fixedNumIterations,
          m_fastRegression, m_errorOnProbabilities, m_minNumInstances,
          getWeightTrimBeta(), getUseAIC(), m_nominalToBinary, m_numDecimalPlaces);
        m_sons[i].buildTree(localInstances[i], copyRegressions(m_regressions),
          m_totalInstanceWeight, m_numParameters, m_numericDataHeader);
        localInstances[i] = null;
      }
    } else {
      cleanup();
    }
  }

  /**
   * Prunes a logistic model tree using the CART pruning scheme, given a
   * cost-complexity parameter alpha.
   * 
   * @param alpha the cost-complexity measure
   * @throws Exception if something goes wrong
   */
  public void prune(double alpha) throws Exception {

    Vector<LMTNode> nodeList;
    CompareNode comparator = new CompareNode();

    // determine training error of logistic models and subtrees, and calculate
    // alpha-values from them
    treeErrors();
    calculateAlphas();

    // get list of all inner nodes in the tree
    nodeList = getNodes();

    boolean prune = (nodeList.size() > 0);

    while (prune) {

      // select node with minimum alpha
      LMTNode nodeToPrune = Collections.min(nodeList, comparator);

      // want to prune if its alpha is smaller than alpha
      if (nodeToPrune.m_alpha > alpha) {
        break;
      }

      nodeToPrune.m_isLeaf = true;
      nodeToPrune.m_sons = null;

      // update tree errors and alphas
      treeErrors();
      calculateAlphas();

      nodeList = getNodes();
      prune = (nodeList.size() > 0);
    }

    // discard references to models at internal nodes because they are not
    // needed
    for (Object node : getNodes()) {
      LMTNode lnode = (LMTNode) node;
      if (!lnode.m_isLeaf) {
        m_regressions = null;
      }
    }
  }

  /**
   * Method for performing one fold in the cross-validation of the
   * cost-complexity parameter. Generates a sequence of alpha-values with error
   * estimates for the corresponding (partially pruned) trees, given the test
   * set of that fold.
   * 
   * @param alphas array to hold the generated alpha-values
   * @param errors array to hold the corresponding error estimates
   * @param test test set of that fold (to obtain error estimates)
   * @throws Exception if something goes wrong
   */
  public int prune(double[] alphas, double[] errors, Instances test)
    throws Exception {

    Vector<LMTNode> nodeList;

    CompareNode comparator = new CompareNode();

    // determine training error of logistic models and subtrees, and calculate
    // alpha-values from them
    treeErrors();
    calculateAlphas();

    // get list of all inner nodes in the tree
    nodeList = getNodes();

    boolean prune = (nodeList.size() > 0);

    // alpha_0 is always zero (unpruned tree)
    alphas[0] = 0;

    Evaluation eval;

    // error of unpruned tree
    if (errors != null) {
      eval = new Evaluation(test);
      eval.evaluateModel(this, test);
      errors[0] = eval.errorRate();
    }

    int iteration = 0;
    while (prune) {

      iteration++;

      // get node with minimum alpha
      LMTNode nodeToPrune = Collections.min(nodeList, comparator);

      nodeToPrune.m_isLeaf = true;
      // Do not set m_sons null, want to unprune

      // get alpha-value of node
      alphas[iteration] = nodeToPrune.m_alpha;

      // log error
      if (errors != null) {
        eval = new Evaluation(test);
        eval.evaluateModel(this, test);
        errors[iteration] = eval.errorRate();
      }

      // update errors/alphas
      treeErrors();
      calculateAlphas();

      nodeList = getNodes();
      prune = (nodeList.size() > 0);
    }

    // set last alpha 1 to indicate end
    alphas[iteration + 1] = 1.0;
    return iteration;
  }

  /**
   * Method to "unprune" a logistic model tree. Sets all leaf-fields to false.
   * Faster than re-growing the tree because the logistic models do not have to
   * be fit again.
   */
  protected void unprune() {
    if (m_sons != null) {
      m_isLeaf = false;
      for (LMTNode m_son : m_sons) {
        m_son.unprune();
      }
    }
  }

  /**
   * Determines the optimum number of LogitBoost iterations to perform by
   * building a standalone logistic regression function on the training data.
   * Used for the heuristic that avoids cross-validating this number again at
   * every node.
   * 
   * @param data training instances for the logistic model
   * @throws Exception if something goes wrong
   */
  protected int tryLogistic(Instances data) throws Exception {

    // convert nominal attributes
    Instances filteredData = Filter.useFilter(data, m_nominalToBinary);

    LogisticBase logistic = new LogisticBase(0, true, m_errorOnProbabilities);

    // limit LogitBoost to 200 iterations (speed)
    logistic.setMaxIterations(200);
    logistic.setWeightTrimBeta(getWeightTrimBeta()); // Not in Marc's code.
                                                     // Added by Eibe.
    logistic.setUseAIC(getUseAIC());
    logistic.buildClassifier(filteredData);

    // return best number of iterations
    return logistic.getNumRegressions();
  }

  /**
   * Method to count the number of inner nodes in the tree
   * 
   * @return the number of inner nodes
   */
  public int getNumInnerNodes() {
    if (m_isLeaf) {
      return 0;
    }
    int numNodes = 1;
    for (LMTNode m_son : m_sons) {
      numNodes += m_son.getNumInnerNodes();
    }
    return numNodes;
  }

  /**
   * Returns the number of leaves in the tree. Leaves are only counted if their
   * logistic model has changed compared to the one of the parent node.
   * 
   * @return the number of leaves
   */
  public int getNumLeaves() {
    int numLeaves;
    if (!m_isLeaf) {
      numLeaves = 0;
      int numEmptyLeaves = 0;
      for (int i = 0; i < m_sons.length; i++) {
        numLeaves += m_sons[i].getNumLeaves();
        if (m_sons[i].m_isLeaf && !m_sons[i].hasModels()) {
          numEmptyLeaves++;
        }
      }
      if (numEmptyLeaves > 1) {
        numLeaves -= (numEmptyLeaves - 1);
      }
    } else {
      numLeaves = 1;
    }
    return numLeaves;
  }

  /**
   * Updates the numIncorrectTree field for all nodes. This is needed for
   * calculating the alpha-values.
   */
  public void treeErrors() {
    if (m_isLeaf) {
      m_numIncorrectTree = m_numIncorrectModel;
    } else {
      m_numIncorrectTree = 0;
      for (LMTNode m_son : m_sons) {
        m_son.treeErrors();
        m_numIncorrectTree += m_son.m_numIncorrectTree;
      }
    }
  }

  /**
   * Updates the alpha field for all nodes.
   */
  public void calculateAlphas() throws Exception {

    if (!m_isLeaf) {
      double errorDiff = m_numIncorrectModel - m_numIncorrectTree;

      if (errorDiff <= 0) {
        // split increases training error (should not normally happen).
        // prune it instantly.
        m_isLeaf = true;
        m_sons = null;
        m_alpha = Double.MAX_VALUE;
      } else {
        // compute alpha
        errorDiff /= m_totalInstanceWeight;
        m_alpha = errorDiff / (getNumLeaves() - 1);

        for (LMTNode m_son : m_sons) {
          m_son.calculateAlphas();
        }
      }
    } else {
      // alpha = infinite for leaves (do not want to prune)
      m_alpha = Double.MAX_VALUE;
    }
  }

  /**
   * Return a list of all inner nodes in the tree
   * 
   * @return the list of nodes
   */
  public Vector<LMTNode> getNodes() {
    Vector<LMTNode> nodeList = new Vector<LMTNode>();
    getNodes(nodeList);
    return nodeList;
  }

  /**
   * Fills a list with all inner nodes in the tree
   * 
   * @param nodeList the list to be filled
   */
  public void getNodes(Vector<LMTNode> nodeList) {
    if (!m_isLeaf) {
      nodeList.add(this);
      for (LMTNode m_son : m_sons) {
        m_son.getNodes(nodeList);
      }
    }
  }

  /**
   * Returns a numeric version of a set of instances. All nominal attributes are
   * replaced by binary ones, and the class variable is replaced by a
   * pseudo-class variable that is used by LogitBoost.
   */
  @Override
  protected Instances getNumericData(Instances train) throws Exception {

    Instances filteredData = Filter.useFilter(train, m_nominalToBinary);

    return super.getNumericData(filteredData);
  }

  /**
   * Returns true if the logistic regression model at this node has changed
   * compared to the one at the parent node.
   * 
   * @return whether it has changed
   */
  public boolean hasModels() {
    return (m_numRegressions > 0);
  }

  /**
   * Returns the class probabilities for an instance according to the logistic
   * model at the node.
   * 
   * @param instance the instance
   * @return the array of probabilities
   */
  public double[] modelDistributionForInstance(Instance instance)
    throws Exception {

    // make copy and convert nominal attributes
    m_nominalToBinary.input(instance);
    instance = m_nominalToBinary.output();

    // saet numeric pseudo-class
    instance.setDataset(m_numericDataHeader);

    return probs(getFs(instance));
  }

  /**
   * Returns the class probabilities for an instance given by the logistic model
   * tree.
   * 
   * @param instance the instance
   * @return the array of probabilities
   */
  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {

    double[] probs;

    if (m_isLeaf) {
      // leaf: use logistic model
      probs = modelDistributionForInstance(instance);
    } else {
      // sort into appropiate child node
      int branch = m_localModel.whichSubset(instance);
      probs = m_sons[branch].distributionForInstance(instance);
    }
    return probs;
  }

  /**
   * Returns the number of leaves (normal count).
   * 
   * @return the number of leaves
   */
  public int numLeaves() {
    if (m_isLeaf) {
      return 1;
    }
    int numLeaves = 0;
    for (LMTNode m_son : m_sons) {
      numLeaves += m_son.numLeaves();
    }
    return numLeaves;
  }

  /**
   * Returns the number of nodes.
   * 
   * @return the number of nodes
   */
  public int numNodes() {
    if (m_isLeaf) {
      return 1;
    }
    int numNodes = 1;
    for (LMTNode m_son : m_sons) {
      numNodes += m_son.numNodes();
    }
    return numNodes;
  }

  /**
   * Returns a description of the logistic model tree (tree structure and
   * logistic models)
   * 
   * @return describing string
   */
  @Override
  public String toString() {
    // assign numbers to logistic regression functions at leaves
    assignLeafModelNumbers(0);
    try {
      StringBuffer text = new StringBuffer();

      if (m_isLeaf) {
        text.append(": ");
        text.append("LM_" + m_leafModelNum + ":" + getModelParameters());
      } else {
        dumpTree(0, text);
      }
      text.append("\n\nNumber of Leaves  : \t" + numLeaves() + "\n");
      text.append("\nSize of the Tree : \t" + numNodes() + "\n");

      // This prints logistic models after the tree, comment out if only tree
      // should be printed
      text.append(modelsToString());
      return text.toString();
    } catch (Exception e) {
      return "Can't print logistic model tree";
    }

  }

  /**
   * Returns a string describing the number of LogitBoost iterations performed
   * at this node, the total number of LogitBoost iterations performed
   * (including iterations at higher levels in the tree), and the number of
   * training examples at this node.
   * 
   * @return the describing string
   */
  public String getModelParameters() {

    StringBuffer text = new StringBuffer();
    int numModels = (int) m_numParameters;
    text.append(m_numRegressions + "/" + numModels + " (" + m_numInstances
      + ")");
    return text.toString();
  }

  /**
   * Help method for printing tree structure.
   * 
   * @throws Exception if something goes wrong
   */
  protected void dumpTree(int depth, StringBuffer text) throws Exception {

    for (int i = 0; i < m_sons.length; i++) {
      text.append("\n");
      for (int j = 0; j < depth; j++) {
        text.append("|   ");
      }
      text.append(m_localModel.leftSide(m_train));
      text.append(m_localModel.rightSide(i, m_train));
      if (m_sons[i].m_isLeaf) {
        text.append(": ");
        text.append("LM_" + m_sons[i].m_leafModelNum + ":"
          + m_sons[i].getModelParameters());
      } else {
        m_sons[i].dumpTree(depth + 1, text);
      }
    }
  }

  /**
   * Assigns unique IDs to all nodes in the tree
   */
  public int assignIDs(int lastID) {

    int currLastID = lastID + 1;

    m_id = currLastID;
    if (m_sons != null) {
      for (LMTNode m_son : m_sons) {
        currLastID = m_son.assignIDs(currLastID);
      }
    }
    return currLastID;
  }

  /**
   * Assigns numbers to the logistic regression models at the leaves of the tree
   */
  public int assignLeafModelNumbers(int leafCounter) {
    if (!m_isLeaf) {
      m_leafModelNum = 0;
      for (LMTNode m_son : m_sons) {
        leafCounter = m_son.assignLeafModelNumbers(leafCounter);
      }
    } else {
      leafCounter++;
      m_leafModelNum = leafCounter;
    }
    return leafCounter;
  }

  /**
   * Returns a string describing the logistic regression function at the node.
   */
  public String modelsToString() {

    StringBuffer text = new StringBuffer();
    if (m_isLeaf) {
      text.append("LM_" + m_leafModelNum + ":" + super.toString());
    } else {
      for (LMTNode m_son : m_sons) {
        text.append("\n" + m_son.modelsToString());
      }
    }
    return text.toString();
  }

  /**
   * Returns graph describing the tree.
   * 
   * @throws Exception if something goes wrong
   */
  public String graph() throws Exception {

    StringBuffer text = new StringBuffer();

    assignIDs(-1);
    assignLeafModelNumbers(0);
    text.append("digraph LMTree {\n");
    if (m_isLeaf) {
      text.append("N" + m_id + " [label=\"LM_" + m_leafModelNum + ":"
        + getModelParameters() + "\" " + "shape=box style=filled");
      text.append("]\n");
    } else {
      text.append("N" + m_id + " [label=\""
        + Utils.backQuoteChars(m_localModel.leftSide(m_train)) + "\" ");
      text.append("]\n");
      graphTree(text);
    }

    return text.toString() + "}\n";
  }

  /**
   * Helper function for graph description of tree
   * 
   * @throws Exception if something goes wrong
   */
  private void graphTree(StringBuffer text) throws Exception {

    for (int i = 0; i < m_sons.length; i++) {
      text.append("N" + m_id + "->" + "N" + m_sons[i].m_id + " [label=\""
        + Utils.backQuoteChars(m_localModel.rightSide(i, m_train).trim())
        + "\"]\n");
      if (m_sons[i].m_isLeaf) {
        text.append("N" + m_sons[i].m_id + " [label=\"LM_"
          + m_sons[i].m_leafModelNum + ":" + m_sons[i].getModelParameters()
          + "\" " + "shape=box style=filled");
        text.append("]\n");
      } else {
        text.append("N" + m_sons[i].m_id + " [label=\""
          + Utils.backQuoteChars(m_sons[i].m_localModel.leftSide(m_train))
          + "\" ");
        text.append("]\n");
        m_sons[i].graphTree(text);
      }
    }
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11566 $");
  }
}
