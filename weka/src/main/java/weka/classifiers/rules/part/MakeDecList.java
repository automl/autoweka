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
 *    MakeDecList.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.rules.part;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import weka.classifiers.trees.j48.ModelSelection;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.CapabilitiesHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;

/**
 * Class for handling a decision list.
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 11006 $
 */
public class MakeDecList implements Serializable, CapabilitiesHandler,
  RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = -1427481323245079123L;

  /** Vector storing the rules. */
  private Vector<ClassifierDecList> theRules;

  /** The confidence for C45-type pruning. */
  private double CF = 0.25f;

  /** Minimum number of objects */
  private final int minNumObj;

  /** The model selection method. */
  private final ModelSelection toSelectModeL;

  /**
   * How many subsets of equal size? One used for pruning, the rest for
   * training.
   */
  private int numSetS = 3;

  /** Use reduced error pruning? */
  private boolean reducedErrorPruning = false;

  /** Generated unpruned list? */
  private boolean unpruned = false;

  /** The seed for random number generation. */
  private int m_seed = 1;

  /**
   * Constructor for unpruned dec list.
   */
  public MakeDecList(ModelSelection toSelectLocModel, int minNum) {

    toSelectModeL = toSelectLocModel;
    reducedErrorPruning = false;
    unpruned = true;
    minNumObj = minNum;
  }

  /**
   * Constructor for dec list pruned using C4.5 pruning.
   */
  public MakeDecList(ModelSelection toSelectLocModel, double cf, int minNum) {

    toSelectModeL = toSelectLocModel;
    CF = cf;
    reducedErrorPruning = false;
    unpruned = false;
    minNumObj = minNum;
  }

  /**
   * Constructor for dec list pruned using hold-out pruning.
   */
  public MakeDecList(ModelSelection toSelectLocModel, int num, int minNum,
    int seed) {

    toSelectModeL = toSelectLocModel;
    numSetS = num;
    reducedErrorPruning = true;
    unpruned = false;
    minNumObj = minNum;
    m_seed = seed;
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = new Capabilities(this);
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
   * Builds dec list.
   * 
   * @exception Exception if dec list can't be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();

    ClassifierDecList currentRule;
    double currentWeight;
    Instances oldGrowData, newGrowData, oldPruneData, newPruneData;
    theRules = new Vector<ClassifierDecList>();
    if ((reducedErrorPruning) && !(unpruned)) {
      Random random = new Random(m_seed);
      data.randomize(random);
      data.stratify(numSetS);
      oldGrowData = data.trainCV(numSetS, numSetS - 1, random);
      oldPruneData = data.testCV(numSetS, numSetS - 1);
    } else {
      oldGrowData = data;
      oldPruneData = null;
    }

    while (Utils.gr(oldGrowData.numInstances(), 0)) {

      // Create rule
      if (unpruned) {
        currentRule = new ClassifierDecList(toSelectModeL, minNumObj);
        currentRule.buildRule(oldGrowData);
      } else if (reducedErrorPruning) {
        currentRule = new PruneableDecList(toSelectModeL, minNumObj);
        ((PruneableDecList) currentRule).buildRule(oldGrowData, oldPruneData);
      } else {
        currentRule = new C45PruneableDecList(toSelectModeL, CF, minNumObj);
        ((C45PruneableDecList) currentRule).buildRule(oldGrowData);
      }
      // Remove instances from growing data
      newGrowData = new Instances(oldGrowData, oldGrowData.numInstances());
      Enumeration<Instance> enu = oldGrowData.enumerateInstances();
      while (enu.hasMoreElements()) {
        Instance instance = enu.nextElement();
        currentWeight = currentRule.weight(instance);
        if (Utils.sm(currentWeight, 1)) {
          instance.setWeight(instance.weight() * (1 - currentWeight));
          newGrowData.add(instance);
        }
      }
      newGrowData.compactify();
      oldGrowData = newGrowData;

      // Remove instances from pruning data
      if ((reducedErrorPruning) && !(unpruned)) {
        newPruneData = new Instances(oldPruneData, oldPruneData.numInstances());
        enu = oldPruneData.enumerateInstances();
        while (enu.hasMoreElements()) {
          Instance instance = enu.nextElement();
          currentWeight = currentRule.weight(instance);
          if (Utils.sm(currentWeight, 1)) {
            instance.setWeight(instance.weight() * (1 - currentWeight));
            newPruneData.add(instance);
          }
        }
        newPruneData.compactify();
        oldPruneData = newPruneData;
      }
      theRules.addElement(currentRule);
    }
  }

  /**
   * Outputs the classifier into a string.
   */
  @Override
  public String toString() {

    StringBuffer text = new StringBuffer();

    for (int i = 0; i < theRules.size(); i++) {
      text.append(theRules.elementAt(i) + "\n");
    }
    text.append("Number of Rules  : \t" + theRules.size() + "\n");

    return text.toString();
  }

  /**
   * Classifies an instance.
   * 
   * @exception Exception if instance can't be classified
   */
  public double classifyInstance(Instance instance) throws Exception {

    double maxProb = -1;
    double[] sumProbs;
    int maxIndex = 0;

    sumProbs = distributionForInstance(instance);
    for (int j = 0; j < sumProbs.length; j++) {
      if (Utils.gr(sumProbs[j], maxProb)) {
        maxIndex = j;
        maxProb = sumProbs[j];
      }
    }

    return maxIndex;
  }

  /**
   * Returns the class distribution for an instance.
   * 
   * @exception Exception if distribution can't be computed
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    double[] currentProbs = null;
    double[] sumProbs;
    double currentWeight, weight = 1;
    int i, j;

    // Get probabilities.
    sumProbs = new double[instance.numClasses()];
    i = 0;
    while (Utils.gr(weight, 0)) {
      currentWeight = theRules.elementAt(i).weight(instance);
      if (Utils.gr(currentWeight, 0)) {
        currentProbs = theRules.elementAt(i).distributionForInstance(instance);
        for (j = 0; j < sumProbs.length; j++) {
          sumProbs[j] += weight * currentProbs[j];
        }
        weight = weight * (1 - currentWeight);
      }
      i++;
    }

    return sumProbs;
  }

  /**
   * Outputs the number of rules in the classifier.
   */
  public int numRules() {

    return theRules.size();
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11006 $");
  }
}
