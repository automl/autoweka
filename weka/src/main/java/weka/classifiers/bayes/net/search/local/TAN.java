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
 * TAN.java
 * Copyright (C) 2004-2012 University of Waikato, Hamilton, New Zealand
 * 
 */

package weka.classifiers.bayes.net.search.local;

import java.util.Enumeration;

import weka.classifiers.bayes.BayesNet;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;

/**
 * <!-- globalinfo-start --> This Bayes Network learning algorithm determines
 * the maximum weight spanning tree and returns a Naive Bayes network augmented
 * with a tree.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * N. Friedman, D. Geiger, M. Goldszmidt (1997). Bayesian network classifiers.
 * Machine Learning. 29(2-3):131-163.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;article{Friedman1997,
 *    author = {N. Friedman and D. Geiger and M. Goldszmidt},
 *    journal = {Machine Learning},
 *    number = {2-3},
 *    pages = {131-163},
 *    title = {Bayesian network classifiers},
 *    volume = {29},
 *    year = {1997}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
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
 * @author Remco Bouckaert
 * @version $Revision: 10154 $
 */
public class TAN extends LocalScoreSearchAlgorithm implements
  TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 965182127977228690L;

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
    result
      .setValue(Field.AUTHOR, "N. Friedman and D. Geiger and M. Goldszmidt");
    result.setValue(Field.YEAR, "1997");
    result.setValue(Field.TITLE, "Bayesian network classifiers");
    result.setValue(Field.JOURNAL, "Machine Learning");
    result.setValue(Field.VOLUME, "29");
    result.setValue(Field.NUMBER, "2-3");
    result.setValue(Field.PAGES, "131-163");

    return result;
  }

  /**
   * buildStructure determines the network structure/graph of the network using
   * the maximimum weight spanning tree algorithm of Chow and Liu
   * 
   * @param bayesNet the network
   * @param instances the data to use
   * @throws Exception if something goes wrong
   */
  @Override
  public void buildStructure(BayesNet bayesNet, Instances instances)
    throws Exception {

    m_bInitAsNaiveBayes = true;
    m_nMaxNrOfParents = 2;
    super.buildStructure(bayesNet, instances);
    int nNrOfAtts = instances.numAttributes();

    if (nNrOfAtts <= 2) {
      return;
    }

    // determine base scores
    double[] fBaseScores = new double[instances.numAttributes()];

    for (int iAttribute = 0; iAttribute < nNrOfAtts; iAttribute++) {
      fBaseScores[iAttribute] = calcNodeScore(iAttribute);
    }

    // // cache scores & whether adding an arc makes sense
    double[][] fScore = new double[nNrOfAtts][nNrOfAtts];

    for (int iAttributeHead = 0; iAttributeHead < nNrOfAtts; iAttributeHead++) {
      for (int iAttributeTail = 0; iAttributeTail < nNrOfAtts; iAttributeTail++) {
        if (iAttributeHead != iAttributeTail) {
          fScore[iAttributeHead][iAttributeTail] = calcScoreWithExtraParent(
            iAttributeHead, iAttributeTail);
        }
      }
    }

    // TAN greedy search (not restricted by ordering like K2)
    // 1. find strongest link
    // 2. find remaining links by adding strongest link to already
    // connected nodes
    // 3. assign direction to links
    int nClassNode = instances.classIndex();
    int[] link1 = new int[nNrOfAtts - 1];
    int[] link2 = new int[nNrOfAtts - 1];
    boolean[] linked = new boolean[nNrOfAtts];

    // 1. find strongest link
    int nBestLinkNode1 = -1;
    int nBestLinkNode2 = -1;
    double fBestDeltaScore = 0.0;
    int iLinkNode1;
    for (iLinkNode1 = 0; iLinkNode1 < nNrOfAtts; iLinkNode1++) {
      if (iLinkNode1 != nClassNode) {
        for (int iLinkNode2 = 0; iLinkNode2 < nNrOfAtts; iLinkNode2++) {
          if ((iLinkNode1 != iLinkNode2)
            && (iLinkNode2 != nClassNode)
            && ((nBestLinkNode1 == -1) || (fScore[iLinkNode1][iLinkNode2]
              - fBaseScores[iLinkNode1] > fBestDeltaScore))) {
            fBestDeltaScore = fScore[iLinkNode1][iLinkNode2]
              - fBaseScores[iLinkNode1];
            nBestLinkNode1 = iLinkNode2;
            nBestLinkNode2 = iLinkNode1;
          }
        }
      }
    }
    link1[0] = nBestLinkNode1;
    link2[0] = nBestLinkNode2;
    linked[nBestLinkNode1] = true;
    linked[nBestLinkNode2] = true;

    // 2. find remaining links by adding strongest link to already
    // connected nodes
    for (int iLink = 1; iLink < nNrOfAtts - 2; iLink++) {
      nBestLinkNode1 = -1;
      for (iLinkNode1 = 0; iLinkNode1 < nNrOfAtts; iLinkNode1++) {
        if (iLinkNode1 != nClassNode) {
          for (int iLinkNode2 = 0; iLinkNode2 < nNrOfAtts; iLinkNode2++) {
            if ((iLinkNode1 != iLinkNode2)
              && (iLinkNode2 != nClassNode)
              && (linked[iLinkNode1] || linked[iLinkNode2])
              && (!linked[iLinkNode1] || !linked[iLinkNode2])
              && ((nBestLinkNode1 == -1) || (fScore[iLinkNode1][iLinkNode2]
                - fBaseScores[iLinkNode1] > fBestDeltaScore))) {
              fBestDeltaScore = fScore[iLinkNode1][iLinkNode2]
                - fBaseScores[iLinkNode1];
              nBestLinkNode1 = iLinkNode2;
              nBestLinkNode2 = iLinkNode1;
            }
          }
        }
      }

      link1[iLink] = nBestLinkNode1;
      link2[iLink] = nBestLinkNode2;
      linked[nBestLinkNode1] = true;
      linked[nBestLinkNode2] = true;
    }

    // 3. assign direction to links
    boolean[] hasParent = new boolean[nNrOfAtts];
    for (int iLink = 0; iLink < nNrOfAtts - 2; iLink++) {
      if (!hasParent[link1[iLink]]) {
        bayesNet.getParentSet(link1[iLink]).addParent(link2[iLink], instances);
        hasParent[link1[iLink]] = true;
      } else {
        if (hasParent[link2[iLink]]) {
          throw new Exception("Bug condition found: too many arrows");
        }
        bayesNet.getParentSet(link2[iLink]).addParent(link1[iLink], instances);
        hasParent[link2[iLink]] = true;
      }
    }

  } // buildStructure

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    return super.listOptions();
  } // listOption

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
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
    super.setOptions(options);
  } // setOptions

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    return super.getOptions();
  } // getOptions

  /**
   * This will return a string describing the classifier.
   * 
   * @return The string.
   */
  @Override
  public String globalInfo() {
    return "This Bayes Network learning algorithm determines the maximum weight spanning tree "
      + " and returns a Naive Bayes network augmented with a tree.\n\n"
      + "For more information see:\n\n" + getTechnicalInformation().toString();
  } // globalInfo

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10154 $");
  }

} // TAN

