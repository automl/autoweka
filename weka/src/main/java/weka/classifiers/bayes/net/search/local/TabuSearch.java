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
 * TabuSearch.java
 * Copyright (C) 2004-2012 University of Waikato, Hamilton, New Zealand
 * 
 */

package weka.classifiers.bayes.net.search.local;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.bayes.BayesNet;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;

/**
 * <!-- globalinfo-start --> This Bayes Network learning algorithm uses tabu
 * search for finding a well scoring Bayes network structure. Tabu search is
 * hill climbing till an optimum is reached. The following step is the least
 * worst possible step. The last X steps are kept in a list and none of the
 * steps in this so called tabu list is considered in taking the next step. The
 * best network found in this traversal is returned.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * R.R. Bouckaert (1995). Bayesian Belief Networks: from Construction to
 * Inference. Utrecht, Netherlands.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;phdthesis{Bouckaert1995,
 *    address = {Utrecht, Netherlands},
 *    author = {R.R. Bouckaert},
 *    institution = {University of Utrecht},
 *    title = {Bayesian Belief Networks: from Construction to Inference},
 *    year = {1995}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -L &lt;integer&gt;
 *  Tabu list length
 * </pre>
 * 
 * <pre>
 * -U &lt;integer&gt;
 *  Number of runs
 * </pre>
 * 
 * <pre>
 * -P &lt;nr of parents&gt;
 *  Maximum number of parents
 * </pre>
 * 
 * <pre>
 * -R
 *  Use arc reversal operation.
 *  (default false)
 * </pre>
 * 
 * <pre>
 * -P &lt;nr of parents&gt;
 *  Maximum number of parents
 * </pre>
 * 
 * <pre>
 * -R
 *  Use arc reversal operation.
 *  (default false)
 * </pre>
 * 
 * <pre>
 * -N
 *  Initial structure is empty (instead of Naive Bayes)
 * </pre>
 * 
 * <pre>
 * -mbc
 *  Applies a Markov Blanket correction to the network structure, 
 *  after a network structure is learned. This ensures that all 
 *  nodes in the network are part of the Markov blanket of the 
 *  classifier node.
 * </pre>
 * 
 * <pre>
 * -S [BAYES|MDL|ENTROPY|AIC|CROSS_CLASSIC|CROSS_BAYES]
 *  Score type (BAYES, BDeu, MDL, ENTROPY and AIC)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * @version $Revision: 10154 $
 */
public class TabuSearch extends HillClimber implements
  TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 1457344073228786447L;

  /** number of runs **/
  int m_nRuns = 10;

  /** size of tabu list **/
  int m_nTabuList = 5;

  /** the actual tabu list **/
  Operation[] m_oTabuList = null;

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

    result = new TechnicalInformation(Type.PHDTHESIS);
    result.setValue(Field.AUTHOR, "R.R. Bouckaert");
    result.setValue(Field.YEAR, "1995");
    result.setValue(Field.TITLE,
      "Bayesian Belief Networks: from Construction to Inference");
    result.setValue(Field.INSTITUTION, "University of Utrecht");
    result.setValue(Field.ADDRESS, "Utrecht, Netherlands");

    return result;
  }

  /**
   * search determines the network structure/graph of the network with the Tabu
   * search algorithm.
   * 
   * @param bayesNet the network
   * @param instances the data to use
   * @throws Exception if something goes wrong
   */
  @Override
  protected void search(BayesNet bayesNet, Instances instances)
    throws Exception {
    m_oTabuList = new Operation[m_nTabuList];
    int iCurrentTabuList = 0;
    initCache(bayesNet, instances);

    // keeps track of score pf best structure found so far
    double fBestScore;
    double fCurrentScore = 0.0;
    for (int iAttribute = 0; iAttribute < instances.numAttributes(); iAttribute++) {
      fCurrentScore += calcNodeScore(iAttribute);
    }

    // keeps track of best structure found so far
    BayesNet bestBayesNet;

    // initialize bestBayesNet
    fBestScore = fCurrentScore;
    bestBayesNet = new BayesNet();
    bestBayesNet.m_Instances = instances;
    bestBayesNet.initStructure();
    copyParentSets(bestBayesNet, bayesNet);

    // go do the search
    for (int iRun = 0; iRun < m_nRuns; iRun++) {
      Operation oOperation = getOptimalOperation(bayesNet, instances);
      performOperation(bayesNet, instances, oOperation);
      // sanity check
      if (oOperation == null) {
        throw new Exception(
          "Panic: could not find any step to make. Tabu list too long?");
      }
      // update tabu list
      m_oTabuList[iCurrentTabuList] = oOperation;
      iCurrentTabuList = (iCurrentTabuList + 1) % m_nTabuList;

      fCurrentScore += oOperation.m_fDeltaScore;
      // keep track of best network seen so far
      if (fCurrentScore > fBestScore) {
        fBestScore = fCurrentScore;
        copyParentSets(bestBayesNet, bayesNet);
      }

      if (bayesNet.getDebug()) {
        printTabuList();
      }
    }

    // restore current network to best network
    copyParentSets(bayesNet, bestBayesNet);

    // free up memory
    bestBayesNet = null;
    m_Cache = null;
  } // search

  /**
   * copyParentSets copies parent sets of source to dest BayesNet
   * 
   * @param dest destination network
   * @param source source network
   */
  void copyParentSets(BayesNet dest, BayesNet source) {
    int nNodes = source.getNrOfNodes();
    // clear parent set first
    for (int iNode = 0; iNode < nNodes; iNode++) {
      dest.getParentSet(iNode).copy(source.getParentSet(iNode));
    }
  } // CopyParentSets

  /**
   * check whether the operation is not in the tabu list
   * 
   * @param oOperation operation to be checked
   * @return true if operation is not in the tabu list
   */
  @Override
  boolean isNotTabu(Operation oOperation) {
    for (int iTabu = 0; iTabu < m_nTabuList; iTabu++) {
      if (oOperation.equals(m_oTabuList[iTabu])) {
        return false;
      }
    }
    return true;
  } // isNotTabu

  /**
   * print tabu list for debugging purposes.
   */
  void printTabuList() {
    for (int i = 0; i < m_nTabuList; i++) {
      Operation o = m_oTabuList[i];
      if (o != null) {
        if (o.m_nOperation == 0) {
          System.out.print(" +(");
        } else {
          System.out.print(" -(");
        }
        System.out.print(o.m_nTail + "->" + o.m_nHead + ")");
      }
    }
    System.out.println();
  } // printTabuList

  /**
   * @return number of runs
   */
  public int getRuns() {
    return m_nRuns;
  } // getRuns

  /**
   * Sets the number of runs
   * 
   * @param nRuns The number of runs to set
   */
  public void setRuns(int nRuns) {
    m_nRuns = nRuns;
  } // setRuns

  /**
   * @return the Tabu List length
   */
  public int getTabuList() {
    return m_nTabuList;
  } // getTabuList

  /**
   * Sets the Tabu List length.
   * 
   * @param nTabuList The nTabuList to set
   */
  public void setTabuList(int nTabuList) {
    m_nTabuList = nTabuList;
  } // setTabuList

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> newVector = new Vector<Option>(4);

    newVector.addElement(new Option("\tTabu list length", "L", 1,
      "-L <integer>"));
    newVector
      .addElement(new Option("\tNumber of runs", "U", 1, "-U <integer>"));
    newVector.addElement(new Option("\tMaximum number of parents", "P", 1,
      "-P <nr of parents>"));
    newVector.addElement(new Option(
      "\tUse arc reversal operation.\n\t(default false)", "R", 0, "-R"));

    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  } // listOptions

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -L &lt;integer&gt;
   *  Tabu list length
   * </pre>
   * 
   * <pre>
   * -U &lt;integer&gt;
   *  Number of runs
   * </pre>
   * 
   * <pre>
   * -P &lt;nr of parents&gt;
   *  Maximum number of parents
   * </pre>
   * 
   * <pre>
   * -R
   *  Use arc reversal operation.
   *  (default false)
   * </pre>
   * 
   * <pre>
   * -P &lt;nr of parents&gt;
   *  Maximum number of parents
   * </pre>
   * 
   * <pre>
   * -R
   *  Use arc reversal operation.
   *  (default false)
   * </pre>
   * 
   * <pre>
   * -N
   *  Initial structure is empty (instead of Naive Bayes)
   * </pre>
   * 
   * <pre>
   * -mbc
   *  Applies a Markov Blanket correction to the network structure, 
   *  after a network structure is learned. This ensures that all 
   *  nodes in the network are part of the Markov blanket of the 
   *  classifier node.
   * </pre>
   * 
   * <pre>
   * -S [BAYES|MDL|ENTROPY|AIC|CROSS_CLASSIC|CROSS_BAYES]
   *  Score type (BAYES, BDeu, MDL, ENTROPY and AIC)
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String sTabuList = Utils.getOption('L', options);
    if (sTabuList.length() != 0) {
      setTabuList(Integer.parseInt(sTabuList));
    }
    String sRuns = Utils.getOption('U', options);
    if (sRuns.length() != 0) {
      setRuns(Integer.parseInt(sRuns));
    }

    super.setOptions(options);
  } // setOptions

  /**
   * Gets the current settings of the search algorithm.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    options.add("-L");
    options.add("" + getTabuList());

    options.add("-U");
    options.add("" + getRuns());

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  } // getOptions

  /**
   * This will return a string describing the classifier.
   * 
   * @return The string.
   */
  @Override
  public String globalInfo() {
    return "This Bayes Network learning algorithm uses tabu search for finding a well scoring "
      + "Bayes network structure. Tabu search is hill climbing till an optimum is reached. The "
      + "following step is the least worst possible step. The last X steps are kept in a list and "
      + "none of the steps in this so called tabu list is considered in taking the next step. "
      + "The best network found in this traversal is returned.\n\n"
      + "For more information see:\n\n" + getTechnicalInformation().toString();
  } // globalInfo

  /**
   * @return a string to describe the Runs option.
   */
  public String runsTipText() {
    return "Sets the number of steps to be performed.";
  } // runsTipText

  /**
   * @return a string to describe the TabuList option.
   */
  public String tabuListTipText() {
    return "Sets the length of the tabu list.";
  } // tabuListTipText

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10154 $");
  }

} // TabuSearch
