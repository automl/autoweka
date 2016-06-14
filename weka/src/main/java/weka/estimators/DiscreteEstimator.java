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
 *    DiscreteEstimator.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.estimators;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Aggregateable;
import weka.core.RevisionUtils;
import weka.core.Utils;

/**
 * Simple symbolic probability estimator based on symbol counts.
 * 
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 11247 $
 */
public class DiscreteEstimator extends Estimator implements
    IncrementalEstimator, Aggregateable<DiscreteEstimator> {

  /** for serialization */
  private static final long serialVersionUID = -5526486742612434779L;

  /** Hold the counts */
  private final double[] m_Counts;

  /** Hold the sum of counts */
  private double m_SumOfCounts;

  /** Initialization for counts */
  private double m_FPrior;

  /**
   * Constructor
   * 
   * @param numSymbols the number of possible symbols (remember to include 0)
   * @param laplace if true, counts will be initialised to 1
   */
  public DiscreteEstimator(int numSymbols, boolean laplace) {

    m_Counts = new double[numSymbols];
    m_SumOfCounts = 0;
    if (laplace) {
      m_FPrior = 1;
      for (int i = 0; i < numSymbols; i++) {
        m_Counts[i] = 1;
      }
      m_SumOfCounts = numSymbols;
    }
  }

  /**
   * Constructor
   * 
   * @param nSymbols the number of possible symbols (remember to include 0)
   * @param fPrior value with which counts will be initialised
   */
  public DiscreteEstimator(int nSymbols, double fPrior) {

    m_Counts = new double[nSymbols];
    m_FPrior = fPrior;
    for (int iSymbol = 0; iSymbol < nSymbols; iSymbol++) {
      m_Counts[iSymbol] = fPrior;
    }
    m_SumOfCounts = fPrior * nSymbols;
  }

  /**
   * Add a new data value to the current estimator.
   * 
   * @param data the new data value
   * @param weight the weight assigned to the data value
   */
  @Override
  public void addValue(double data, double weight) {

    m_Counts[(int) data] += weight;
    m_SumOfCounts += weight;
  }

  /**
   * Get a probability estimate for a value
   * 
   * @param data the value to estimate the probability of
   * @return the estimated probability of the supplied value
   */
  @Override
  public double getProbability(double data) {

    if (m_SumOfCounts == 0) {
      return 0;
    }
    return m_Counts[(int) data] / m_SumOfCounts;
  }

  /**
   * Gets the number of symbols this estimator operates with
   * 
   * @return the number of estimator symbols
   */
  public int getNumSymbols() {

    return (m_Counts == null) ? 0 : m_Counts.length;
  }

  /**
   * Get the count for a value
   * 
   * @param data the value to get the count of
   * @return the count of the supplied value
   */
  public double getCount(double data) {

    if (m_SumOfCounts == 0) {
      return 0;
    }
    return m_Counts[(int) data];
  }

  /**
   * Get the sum of all the counts
   * 
   * @return the total sum of counts
   */
  public double getSumOfCounts() {

    return m_SumOfCounts;
  }

  /**
   * Display a representation of this estimator
   */
  @Override
  public String toString() {

    StringBuffer result = new StringBuffer("Discrete Estimator. Counts = ");
    if (m_SumOfCounts > 1) {
      for (int i = 0; i < m_Counts.length; i++) {
        result.append(" ").append(Utils.doubleToString(m_Counts[i], 2));
      }
      result.append("  (Total = ").append(
          Utils.doubleToString(m_SumOfCounts, 2));
      result.append(")\n");
    } else {
      for (int i = 0; i < m_Counts.length; i++) {
        result.append(" ").append(m_Counts[i]);
      }
      result.append("  (Total = ").append(m_SumOfCounts).append(")\n");
    }
    return result.toString();
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

    // class
    if (!m_noClass) {
      result.enable(Capability.NOMINAL_CLASS);
      result.enable(Capability.MISSING_CLASS_VALUES);
    } else {
      result.enable(Capability.NO_CLASS);
    }

    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    return result;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11247 $");
  }

  @Override
  public DiscreteEstimator aggregate(DiscreteEstimator toAggregate)
      throws Exception {

    if (toAggregate.m_Counts.length != m_Counts.length) {
      throw new Exception("DiscreteEstimator to aggregate has a different "
          + "number of symbols");
    }

    m_SumOfCounts += toAggregate.m_SumOfCounts;
    for (int i = 0; i < m_Counts.length; i++) {
      m_Counts[i] += (toAggregate.m_Counts[i] - toAggregate.m_FPrior);
    }

    m_SumOfCounts -= (toAggregate.m_FPrior * m_Counts.length);

    return this;
  }

  @Override
  public void finalizeAggregation() throws Exception {
    // nothing to do
  }

  protected static void testAggregation() {
    DiscreteEstimator df = new DiscreteEstimator(5, true);
    DiscreteEstimator one = new DiscreteEstimator(5, true);
    DiscreteEstimator two = new DiscreteEstimator(5, true);

    java.util.Random r = new java.util.Random(1);

    for (int i = 0; i < 100; i++) {
      int z = r.nextInt(5);
      df.addValue(z, 1);

      if (i < 50) {
        one.addValue(z, 1);
      } else {
        two.addValue(z, 1);
      }
    }

    try {
      System.out.println("\n\nFull\n");
      System.out.println(df.toString());
      System.out.println("Prob (0): " + df.getProbability(0));

      System.out.println("\nOne\n" + one.toString());
      System.out.println("Prob (0): " + one.getProbability(0));

      System.out.println("\nTwo\n" + two.toString());
      System.out.println("Prob (0): " + two.getProbability(0));

      one = one.aggregate(two);

      System.out.println("\nAggregated\n");
      System.out.println(one.toString());
      System.out.println("Prob (0): " + one.getProbability(0));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv should contain a sequence of integers which will be treated as
   *          symbolic.
   */
  public static void main(String[] argv) {

    try {
      if (argv.length == 0) {
        System.out.println("Please specify a set of instances.");
        return;
      }
      int current = Integer.parseInt(argv[0]);
      int max = current;
      for (int i = 1; i < argv.length; i++) {
        current = Integer.parseInt(argv[i]);
        if (current > max) {
          max = current;
        }
      }
      DiscreteEstimator newEst = new DiscreteEstimator(max + 1, true);
      for (int i = 0; i < argv.length; i++) {
        current = Integer.parseInt(argv[i]);
        System.out.println(newEst);
        System.out.println("Prediction for " + current + " = "
            + newEst.getProbability(current));
        newEst.addValue(current, 1);
      }

      DiscreteEstimator.testAggregation();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
