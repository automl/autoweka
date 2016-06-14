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
 *    NormalEstimator.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.estimators;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Aggregateable;
import weka.core.RevisionUtils;
import weka.core.Statistics;
import weka.core.Utils;

/**
 * Simple probability estimator that places a single normal distribution over
 * the observed values.
 * 
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 9785 $
 */
public class NormalEstimator extends Estimator implements IncrementalEstimator,
    Aggregateable<NormalEstimator> {

  /** for serialization */
  private static final long serialVersionUID = 93584379632315841L;

  /** The sum of the weights */
  private double m_SumOfWeights;

  /** The sum of the values seen */
  private double m_SumOfValues;

  /** The sum of the values squared */
  private double m_SumOfValuesSq;

  /** The current mean */
  private double m_Mean;

  /** The current standard deviation */
  private double m_StandardDev;

  /** The precision of numeric values ( = minimum std dev permitted) */
  private double m_Precision;

  /**
   * Round a data value using the defined precision for this estimator
   * 
   * @param data the value to round
   * @return the rounded data value
   */
  private double round(double data) {

    return Math.rint(data / m_Precision) * m_Precision;
  }

  // ===============
  // Public methods.
  // ===============

  /**
   * Constructor that takes a precision argument.
   * 
   * @param precision the precision to which numeric values are given. For
   *          example, if the precision is stated to be 0.1, the values in the
   *          interval (0.25,0.35] are all treated as 0.3.
   */
  public NormalEstimator(double precision) {

    m_Precision = precision;

    // Allow at most 3 sd's within one interval
    m_StandardDev = m_Precision / (2 * 3);
  }

  /**
   * Add a new data value to the current estimator.
   * 
   * @param data the new data value
   * @param weight the weight assigned to the data value
   */
  @Override
  public void addValue(double data, double weight) {

    if (weight == 0) {
      return;
    }
    data = round(data);
    m_SumOfWeights += weight;
    m_SumOfValues += data * weight;
    m_SumOfValuesSq += data * data * weight;

    computeParameters();
  }

  /**
   * Compute the parameters of the distribution
   */
  protected void computeParameters() {
    if (m_SumOfWeights > 0) {
      m_Mean = m_SumOfValues / m_SumOfWeights;
      double stdDev = Math.sqrt(Math.abs(m_SumOfValuesSq - m_Mean
          * m_SumOfValues)
          / m_SumOfWeights);
      // If the stdDev ~= 0, we really have no idea of scale yet,
      // so stick with the default. Otherwise...
      if (stdDev > 1e-10) {
        m_StandardDev = Math.max(m_Precision / (2 * 3),
        // allow at most 3sd's within one interval
            stdDev);
      }
    }
  }

  /**
   * Get a probability estimate for a value
   * 
   * @param data the value to estimate the probability of
   * @return the estimated probability of the supplied value
   */
  @Override
  public double getProbability(double data) {

    data = round(data);
    double zLower = (data - m_Mean - (m_Precision / 2)) / m_StandardDev;
    double zUpper = (data - m_Mean + (m_Precision / 2)) / m_StandardDev;

    double pLower = Statistics.normalProbability(zLower);
    double pUpper = Statistics.normalProbability(zUpper);
    return pUpper - pLower;
  }

  /**
   * Display a representation of this estimator
   */
  @Override
  public String toString() {

    return "Normal Distribution. Mean = " + Utils.doubleToString(m_Mean, 4)
        + " StandardDev = " + Utils.doubleToString(m_StandardDev, 4)
        + " WeightSum = " + Utils.doubleToString(m_SumOfWeights, 4)
        + " Precision = " + m_Precision + "\n";
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
   * Return the value of the mean of this normal estimator.
   * 
   * @return the mean
   */
  public double getMean() {
    return m_Mean;
  }

  /**
   * Return the value of the standard deviation of this normal estimator.
   * 
   * @return the standard deviation
   */
  public double getStdDev() {
    return m_StandardDev;
  }

  /**
   * Return the value of the precision of this normal estimator.
   * 
   * @return the precision
   */
  public double getPrecision() {
    return m_Precision;
  }

  /**
   * Return the sum of the weights for this normal estimator.
   * 
   * @return the sum of the weights
   */
  public double getSumOfWeights() {
    return m_SumOfWeights;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 9785 $");
  }

  @Override
  public NormalEstimator aggregate(NormalEstimator toAggregate)
      throws Exception {

    m_SumOfWeights += toAggregate.m_SumOfWeights;
    m_SumOfValues += toAggregate.m_SumOfValues;
    m_SumOfValuesSq += toAggregate.m_SumOfValuesSq;

    if (toAggregate.m_Precision < m_Precision) {
      m_Precision = toAggregate.m_Precision;
    }

    computeParameters();

    return this;
  }

  @Override
  public void finalizeAggregation() throws Exception {
    // nothing to do
  }

  public static void testAggregation() {
    NormalEstimator ne = new NormalEstimator(0.01);
    NormalEstimator one = new NormalEstimator(0.01);
    NormalEstimator two = new NormalEstimator(0.01);

    java.util.Random r = new java.util.Random(1);

    for (int i = 0; i < 100; i++) {
      double z = r.nextDouble();

      ne.addValue(z, 1);
      if (i < 50) {
        one.addValue(z, 1);
      } else {
        two.addValue(z, 1);
      }
    }

    try {
      System.out.println("\n\nFull\n");
      System.out.println(ne.toString());
      System.out.println("Prob (0): " + ne.getProbability(0));

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
   * @param argv should contain a sequence of numeric values
   */
  public static void main(String[] argv) {

    try {

      if (argv.length == 0) {
        System.out.println("Please specify a set of instances.");
        return;
      }
      NormalEstimator newEst = new NormalEstimator(0.01);
      for (int i = 0; i < argv.length; i++) {
        double current = Double.valueOf(argv[i]).doubleValue();
        System.out.println(newEst);
        System.out.println("Prediction for " + current + " = "
            + newEst.getProbability(current));
        newEst.addValue(current, 1);
      }

      NormalEstimator.testAggregation();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
