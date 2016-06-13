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
 *    AprioriItemSet.java
 *    Copyright (C) 2004-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.associations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import weka.core.ContingencyTables;
import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.WekaEnumeration;

/**
 * Class for storing a set of items. Item sets are stored in a lexicographic
 * order, which is determined by the header information of the set of instances
 * used for generating the set of items. All methods in this class assume that
 * item sets are stored in lexicographic order. The class provides methods that
 * are used in the Apriori algorithm to construct association rules.
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 12014 $
 */
public class AprioriItemSet extends ItemSet implements Serializable,
  RevisionHandler {

  /** for serialization */
  static final long serialVersionUID = 7684467755712672058L;

  /**
   * Constructor
   * 
   * @param totalTrans the total number of transactions in the data
   */
  public AprioriItemSet(int totalTrans) {
    super(totalTrans);
  }

  /**
   * Outputs the confidence for a rule.
   * 
   * @param premise the premise of the rule
   * @param consequence the consequence of the rule
   * @return the confidence on the training data
   */
  public static double confidenceForRule(AprioriItemSet premise,
    AprioriItemSet consequence) {

    return (double) consequence.m_counter / (double) premise.m_counter;
  }

  /**
   * Outputs the lift for a rule. Lift is defined as:<br>
   * confidence / prob(consequence)
   * 
   * @param premise the premise of the rule
   * @param consequence the consequence of the rule
   * @param consequenceCount how many times the consequence occurs independent
   *          of the premise
   * @return the lift on the training data
   */
  public double liftForRule(AprioriItemSet premise, AprioriItemSet consequence,
    int consequenceCount) {
    double confidence = confidenceForRule(premise, consequence);

    return confidence
      / ((double) consequenceCount / (double) m_totalTransactions);
  }

  /**
   * Outputs the leverage for a rule. Leverage is defined as: <br>
   * prob(premise & consequence) - (prob(premise) * prob(consequence))
   * 
   * @param premise the premise of the rule
   * @param consequence the consequence of the rule
   * @param premiseCount how many times the premise occurs independent of the
   *          consequent
   * @param consequenceCount how many times the consequence occurs independent
   *          of the premise
   * @return the leverage on the training data
   */
  public double leverageForRule(AprioriItemSet premise,
    AprioriItemSet consequence, int premiseCount, int consequenceCount) {
    double coverageForItemSet = (double) consequence.m_counter
      / (double) m_totalTransactions;
    double expectedCoverageIfIndependent = ((double) premiseCount / (double) m_totalTransactions)
      * ((double) consequenceCount / (double) m_totalTransactions);
    double lev = coverageForItemSet - expectedCoverageIfIndependent;
    return lev;
  }

  /**
   * Outputs the conviction for a rule. Conviction is defined as: <br>
   * prob(premise) * prob(!consequence) / prob(premise & !consequence)
   * 
   * @param premise the premise of the rule
   * @param consequence the consequence of the rule
   * @param premiseCount how many times the premise occurs independent of the
   *          consequent
   * @param consequenceCount how many times the consequence occurs independent
   *          of the premise
   * @return the conviction on the training data
   */
  public double convictionForRule(AprioriItemSet premise,
    AprioriItemSet consequence, int premiseCount, int consequenceCount) {
    double num = (double) premiseCount
      * (double) (m_totalTransactions - consequenceCount) / m_totalTransactions;
    double denom = ((premiseCount - consequence.m_counter) + 1);

    if (num < 0 || denom < 0) {
      System.err.println("*** " + num + " " + denom);
      System.err.println("premis count: " + premiseCount
        + " consequence count " + consequenceCount + " total trans "
        + m_totalTransactions);
    }
    return num / denom;
  }

  /**
   * Generates all rules for an item set.
   * 
   * @param minConfidence the minimum confidence the rules have to have
   * @param hashtables containing all(!) previously generated item sets
   * @param numItemsInSet the size of the item set for which the rules are to be
   *          generated
   * @return all the rules with minimum confidence for the given item set
   */
  public ArrayList<Object>[] generateRules(double minConfidence,
    ArrayList<Hashtable<ItemSet, Integer>> hashtables, int numItemsInSet) {

    ArrayList<Object> premises = new ArrayList<Object>(), consequences = new ArrayList<Object>(), conf = new ArrayList<Object>();
    // TODO
    ArrayList<Object> lift = new ArrayList<Object>(), lev = new ArrayList<Object>(), conv = new ArrayList<Object>();
    // TODO
    @SuppressWarnings("unchecked")
    ArrayList<Object>[] rules = new ArrayList[6], moreResults;
    AprioriItemSet premise, consequence;
    Hashtable<ItemSet, Integer> hashtable = hashtables.get(numItemsInSet - 2);

    // Generate all rules with one item in the consequence.
    for (int i = 0; i < m_items.length; i++) {
      if (m_items[i] != -1) {
        premise = new AprioriItemSet(m_totalTransactions);
        consequence = new AprioriItemSet(m_totalTransactions);
        premise.m_items = new int[m_items.length];
        consequence.m_items = new int[m_items.length];
        consequence.m_counter = m_counter;

        for (int j = 0; j < m_items.length; j++) {
          consequence.m_items[j] = -1;
        }
        System.arraycopy(m_items, 0, premise.m_items, 0, m_items.length);
        premise.m_items[i] = -1;

        consequence.m_items[i] = m_items[i];
        premise.m_counter = hashtable.get(premise).intValue();

        Hashtable<ItemSet, Integer> hashtableForConsequence = hashtables.get(0);
        int consequenceUnconditionedCounter = hashtableForConsequence.get(
          consequence).intValue();
        consequence.m_secondaryCounter = consequenceUnconditionedCounter;

        premises.add(premise);
        consequences.add(consequence);
        conf.add(new Double(confidenceForRule(premise, consequence)));

        double tempLift = liftForRule(premise, consequence,
          consequenceUnconditionedCounter);
        double tempLev = leverageForRule(premise, consequence,
          premise.m_counter, consequenceUnconditionedCounter);
        double tempConv = convictionForRule(premise, consequence,
          premise.m_counter, consequenceUnconditionedCounter);
        lift.add(new Double(tempLift));
        lev.add(new Double(tempLev));
        conv.add(new Double(tempConv));
      }
    }
    rules[0] = premises;
    rules[1] = consequences;
    rules[2] = conf;

    rules[3] = lift;
    rules[4] = lev;
    rules[5] = conv;

    pruneRules(rules, minConfidence);

    // Generate all the other rules
    moreResults = moreComplexRules(rules, numItemsInSet, 1, minConfidence,
      hashtables);
    if (moreResults != null) {
      for (int i = 0; i < moreResults[0].size(); i++) {
        rules[0].add(moreResults[0].get(i));
        rules[1].add(moreResults[1].get(i));
        rules[2].add(moreResults[2].get(i));

        // TODO
        rules[3].add(moreResults[3].get(i));
        rules[4].add(moreResults[4].get(i));
        rules[5].add(moreResults[5].get(i));
      }
    }
    return rules;
  }

  /**
   * Generates all significant rules for an item set.
   * 
   * @param minMetric the minimum metric (confidence, lift, leverage,
   *          improvement) the rules have to have
   * @param metricType (confidence=0, lift, leverage, improvement)
   * @param hashtables containing all(!) previously generated item sets
   * @param numItemsInSet the size of the item set for which the rules are to be
   *          generated
   * @param numTransactions
   * @param significanceLevel the significance level for testing the rules
   * @return all the rules with minimum metric for the given item set
   * @exception Exception if something goes wrong
   */
  public final ArrayList<Object>[] generateRulesBruteForce(double minMetric,
    int metricType, ArrayList<Hashtable<ItemSet, Integer>> hashtables,
    int numItemsInSet, int numTransactions, double significanceLevel)
    throws Exception {

    ArrayList<Object> premises = new ArrayList<Object>(), consequences = new ArrayList<Object>(), conf = new ArrayList<Object>(), lift = new ArrayList<Object>(), lev = new ArrayList<Object>(), conv = new ArrayList<Object>();
    @SuppressWarnings("unchecked")
    ArrayList<Object>[] rules = new ArrayList[6];
    AprioriItemSet premise, consequence;
    Hashtable<ItemSet, Integer> hashtableForPremise, hashtableForConsequence;
    int numItemsInPremise, help, max, consequenceUnconditionedCounter;
    double[][] contingencyTable = new double[2][2];
    double metric, chiSquared = 0;

    // Generate all possible rules for this item set and test their
    // significance.
    max = (int) Math.pow(2, numItemsInSet);
    for (int j = 1; j < max; j++) {
      numItemsInPremise = 0;
      help = j;
      while (help > 0) {
        if (help % 2 == 1) {
          numItemsInPremise++;
        }
        help /= 2;
      }
      if (numItemsInPremise < numItemsInSet) {
        hashtableForPremise = hashtables.get(numItemsInPremise - 1);
        hashtableForConsequence = hashtables.get(numItemsInSet
          - numItemsInPremise - 1);
        premise = new AprioriItemSet(m_totalTransactions);
        consequence = new AprioriItemSet(m_totalTransactions);
        premise.m_items = new int[m_items.length];

        consequence.m_items = new int[m_items.length];
        consequence.m_counter = m_counter;
        help = j;
        for (int i = 0; i < m_items.length; i++) {
          if (m_items[i] != -1) {
            if (help % 2 == 1) {
              premise.m_items[i] = m_items[i];
              consequence.m_items[i] = -1;
            } else {
              premise.m_items[i] = -1;
              consequence.m_items[i] = m_items[i];
            }
            help /= 2;
          } else {
            premise.m_items[i] = -1;
            consequence.m_items[i] = -1;
          }
        }
        premise.m_counter = hashtableForPremise.get(premise).intValue();
        consequenceUnconditionedCounter = hashtableForConsequence.get(
          consequence).intValue();
        consequence.m_secondaryCounter = consequenceUnconditionedCounter;

        if (significanceLevel != -1) {
          contingencyTable[0][0] = (consequence.m_counter);
          contingencyTable[0][1] = (premise.m_counter - consequence.m_counter);
          contingencyTable[1][0] = (consequenceUnconditionedCounter - consequence.m_counter);
          contingencyTable[1][1] = (numTransactions - premise.m_counter
            - consequenceUnconditionedCounter + consequence.m_counter);
          chiSquared = ContingencyTables.chiSquared(contingencyTable, false);
        }

        if (metricType == 0) {

          metric = confidenceForRule(premise, consequence);

          if ((!(metric < minMetric))
            && (significanceLevel == -1 || !(chiSquared > significanceLevel))) {
            premises.add(premise);
            consequences.add(consequence);
            conf.add(new Double(metric));
            lift.add(new Double(liftForRule(premise, consequence,
              consequenceUnconditionedCounter)));
            lev.add(new Double(leverageForRule(premise, consequence,
              premise.m_counter, consequenceUnconditionedCounter)));
            conv.add(new Double(convictionForRule(premise, consequence,
              premise.m_counter, consequenceUnconditionedCounter)));
          }
        } else {
          double tempConf = confidenceForRule(premise, consequence);
          double tempLift = liftForRule(premise, consequence,
            consequenceUnconditionedCounter);
          double tempLev = leverageForRule(premise, consequence,
            premise.m_counter, consequenceUnconditionedCounter);
          double tempConv = convictionForRule(premise, consequence,
            premise.m_counter, consequenceUnconditionedCounter);
          switch (metricType) {
          case 1:
            metric = tempLift;
            break;
          case 2:
            metric = tempLev;
            break;
          case 3:
            metric = tempConv;
            break;
          default:
            throw new Exception("ItemSet: Unknown metric type!");
          }
          if (!(metric < minMetric)
            && (significanceLevel == -1 || !(chiSquared > significanceLevel))) {
            premises.add(premise);
            consequences.add(consequence);
            conf.add(new Double(tempConf));
            lift.add(new Double(tempLift));
            lev.add(new Double(tempLev));
            conv.add(new Double(tempConv));
          }
        }
      }
    }
    rules[0] = premises;
    rules[1] = consequences;
    rules[2] = conf;
    rules[3] = lift;
    rules[4] = lev;
    rules[5] = conv;
    return rules;
  }

  /**
   * Subtracts an item set from another one.
   * 
   * @param toSubtract the item set to be subtracted from this one.
   * @return an item set that only contains items form this item sets that are
   *         not contained by toSubtract
   */
  public final AprioriItemSet subtract(AprioriItemSet toSubtract) {

    AprioriItemSet result = new AprioriItemSet(m_totalTransactions);

    result.m_items = new int[m_items.length];

    for (int i = 0; i < m_items.length; i++) {
      if (toSubtract.m_items[i] == -1) {
        result.m_items[i] = m_items[i];
      } else {
        result.m_items[i] = -1;
      }
    }
    result.m_counter = 0;
    return result;
  }

  /**
   * Generates rules with more than one item in the consequence.
   * 
   * @param rules all the rules having (k-1)-item sets as consequences
   * @param numItemsInSet the size of the item set for which the rules are to be
   *          generated
   * @param numItemsInConsequence the value of (k-1)
   * @param minConfidence the minimum confidence a rule has to have
   * @param hashtables the hashtables containing all(!) previously generated
   *          item sets
   * @return all the rules having (k)-item sets as consequences
   */
  @SuppressWarnings("unchecked")
  private final ArrayList<Object>[] moreComplexRules(ArrayList<Object>[] rules,
    int numItemsInSet, int numItemsInConsequence, double minConfidence,
    ArrayList<Hashtable<ItemSet, Integer>> hashtables) {

    AprioriItemSet newPremise;
    ArrayList<Object>[] result, moreResults;
    ArrayList<Object> newConsequences, newPremises = new ArrayList<Object>(), newConf = new ArrayList<Object>();
    Hashtable<ItemSet, Integer> hashtable;

    ArrayList<Object> newLift = null, newLev = null, newConv = null;
    // if (rules.length > 3) {
    newLift = new ArrayList<Object>();
    newLev = new ArrayList<Object>();
    newConv = new ArrayList<Object>();
    // }

    if (numItemsInSet > numItemsInConsequence + 1) {
      hashtable = hashtables.get(numItemsInSet - numItemsInConsequence - 2);
      newConsequences = mergeAllItemSets(rules[1], numItemsInConsequence - 1,
        m_totalTransactions);
      int newNumInConsequence = numItemsInConsequence + 1;

      Hashtable<ItemSet, Integer> hashtableForConsequence = hashtables
        .get(newNumInConsequence - 1);

      Enumeration<Object> enu = new WekaEnumeration<Object>(newConsequences);
      while (enu.hasMoreElements()) {
        AprioriItemSet current = (AprioriItemSet) enu.nextElement();
        for (int m_item : current.m_items) {
          if (m_item != -1) {
          }
        }

        current.m_counter = m_counter;
        newPremise = subtract(current);
        newPremise.m_counter = hashtable.get(newPremise).intValue();
        newPremises.add(newPremise);
        newConf.add(new Double(confidenceForRule(newPremise, current)));

        // if (rules.length > 3) {
        int consequenceUnconditionedCounter = hashtableForConsequence.get(
          current).intValue();
        current.m_secondaryCounter = consequenceUnconditionedCounter;

        double tempLift = liftForRule(newPremise, current,
          consequenceUnconditionedCounter);
        double tempLev = leverageForRule(newPremise, current,
          newPremise.m_counter, consequenceUnconditionedCounter);
        double tempConv = convictionForRule(newPremise, current,
          newPremise.m_counter, consequenceUnconditionedCounter);

        newLift.add(new Double(tempLift));
        newLev.add(new Double(tempLev));
        newConv.add(new Double(tempConv));
        // }
      }
      result = new ArrayList[rules.length];
      result[0] = newPremises;
      result[1] = newConsequences;
      result[2] = newConf;

      // if (rules.length > 3) {
      result[3] = newLift;
      result[4] = newLev;
      result[5] = newConv;
      // }
      pruneRules(result, minConfidence);
      moreResults = moreComplexRules(result, numItemsInSet,
        numItemsInConsequence + 1, minConfidence, hashtables);
      if (moreResults != null) {
        for (int i = 0; i < moreResults[0].size(); i++) {
          result[0].add(moreResults[0].get(i));
          result[1].add(moreResults[1].get(i));
          result[2].add(moreResults[2].get(i));
          //
          result[3].add(moreResults[3].get(i));
          result[4].add(moreResults[4].get(i));
          result[5].add(moreResults[5].get(i));
        }
      }
      return result;
    } else {
      return null;
    }
  }

  /**
   * Returns the contents of an item set as a string.
   * 
   * @param instances contains the relevant header information
   * @return string describing the item set
   */
  @Override
  public final String toString(Instances instances) {

    return super.toString(instances);
  }

  /**
   * Converts the header info of the given set of instances into a set of item
   * sets (singletons). The ordering of values in the header file determines the
   * lexicographic order.
   * 
   * @param instances the set of instances whose header info is to be used
   * @return a set of item sets, each containing a single item
   * @exception Exception if singletons can't be generated successfully
   */
  public static ArrayList<Object> singletons(Instances instances,
    boolean treatZeroAsMissing) throws Exception {

    ArrayList<Object> setOfItemSets = new ArrayList<Object>();
    AprioriItemSet current;

    for (int i = 0; i < instances.numAttributes(); i++) {
      if (instances.attribute(i).isNumeric()) {
        throw new Exception("Can't handle numeric attributes!");
      }
      int j = (treatZeroAsMissing) ? 1 : 0;
      for (; j < instances.attribute(i).numValues(); j++) {
        current = new AprioriItemSet(instances.numInstances());
        current.m_items = new int[instances.numAttributes()];
        for (int k = 0; k < instances.numAttributes(); k++) {
          current.m_items[k] = -1;
        }
        current.m_items[i] = j;
        setOfItemSets.add(current);
      }
    }
    return setOfItemSets;
  }

  /**
   * Merges all item sets in the set of (k-1)-item sets to create the (k)-item
   * sets and updates the counters.
   * 
   * @param itemSets the set of (k-1)-item sets
   * @param size the value of (k-1)
   * @param totalTrans the total number of transactions in the data
   * @return the generated (k)-item sets
   */
  public static ArrayList<Object> mergeAllItemSets(ArrayList<Object> itemSets,
    int size, int totalTrans) {

    ArrayList<Object> newVector = new ArrayList<Object>();
    AprioriItemSet result;
    int numFound, k;

    for (int i = 0; i < itemSets.size(); i++) {
      ItemSet first = (ItemSet) itemSets.get(i);
      out: for (int j = i + 1; j < itemSets.size(); j++) {
        ItemSet second = (ItemSet) itemSets.get(j);
        result = new AprioriItemSet(totalTrans);
        result.m_items = new int[first.m_items.length];

        // Find and copy common prefix of size 'size'
        numFound = 0;
        k = 0;
        while (numFound < size) {
          if (first.m_items[k] == second.m_items[k]) {
            if (first.m_items[k] != -1) {
              numFound++;
            }
            result.m_items[k] = first.m_items[k];
          } else {
            break out;
          }
          k++;
        }

        // Check difference
        while (k < first.m_items.length) {
          if ((first.m_items[k] != -1) && (second.m_items[k] != -1)) {
            break;
          } else {
            if (first.m_items[k] != -1) {
              result.m_items[k] = first.m_items[k];
            } else {
              result.m_items[k] = second.m_items[k];
            }
          }
          k++;
        }
        if (k == first.m_items.length) {
          result.m_counter = 0;
          newVector.add(result);
        }
      }
    }
    return newVector;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12014 $");
  }
}
