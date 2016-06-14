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
 *    SimpleLinearRegression.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.functions;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.evaluation.RegressionAnalysis;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

/**
 <!-- globalinfo-start --> 
 * Learns a simple linear regression model. Picks the
 * attribute that results in the lowest squared error. Can only deal with
 * numeric attributes.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start --> 
 * Valid options are:
 * <p/>
 * 
 * <pre>
 * -additional-stats
 *  Output additional statistics.
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
 <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 11130 $
 */
public class SimpleLinearRegression extends AbstractClassifier implements
  WeightedInstancesHandler {

  /** for serialization */
  static final long serialVersionUID = 1679336022895414137L;

  /** The chosen attribute */
  private Attribute m_attribute;

  /** The index of the chosen attribute */
  private int m_attributeIndex;

  /** The slope */
  private double m_slope;

  /** The intercept */
  private double m_intercept;

  /** The class mean for missing values */
  private double m_classMeanForMissing;

  /**
   * Whether to output additional statistics such as std. dev. of coefficients
   * and t-stats
   */
  protected boolean m_outputAdditionalStats;

  /** Degrees of freedom, used in statistical calculations */
  private int m_df;

  /** standard error of the slope */
  private double m_seSlope = Double.NaN;

  /** standard error of the intercept */
  private double m_seIntercept = Double.NaN;

  /** t-statistic of the slope */
  private double m_tstatSlope = Double.NaN;

  /** t-statistic of the intercept */
  private double m_tstatIntercept = Double.NaN;

  /** R^2 value for the regression */
  private double m_rsquared = Double.NaN;

  /** Adjusted R^2 value for the regression */
  private double m_rsquaredAdj = Double.NaN;

  /** F-statistic for the regression */
  private double m_fstat = Double.NaN;

  /** If true, suppress error message if no useful attribute was found */
  private boolean m_suppressErrorMessage = false;

  /**
   * Returns a string describing this classifier
   * 
   * @return a description of the classifier suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Learns a simple linear regression model. "
      + "Picks the attribute that results in the lowest squared error. "
      + "Can only deal with numeric attributes.";
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> newVector = new Vector<Option>();

    newVector.addElement(new Option("\tOutput additional statistics.",
      "additional-stats", 0, "-additional-stats"));

    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   <!-- options-start --> 
   * Valid options are:
   * <p/>
   * 
   * <pre>
   * -additional-stats
   *  Output additional statistics.
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
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    setOutputAdditionalStats(Utils.getFlag("additional-stats", options));

    super.setOptions(options);
    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    if (getOutputAdditionalStats()) {
      result.add("-additional-stats");
    }

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String outputAdditionalStatsTipText() {
    return "Output additional statistics (such as "
      + "std deviation of coefficients and t-statistics)";
  }

  /**
   * Set whether to output additional statistics (such as std. deviation of
   * coefficients and t-statistics
   * 
   * @param additional true if additional stats are to be output
   */
  public void setOutputAdditionalStats(boolean additional) {
    m_outputAdditionalStats = additional;
  }

  /**
   * Get whether to output additional statistics (such as std. deviation of
   * coefficients and t-statistics
   * 
   * @return true if additional stats are to be output
   */
  public boolean getOutputAdditionalStats() {
    return m_outputAdditionalStats;
  }

  /**
   * Generate a prediction for the supplied instance.
   * 
   * @param inst the instance to predict.
   * @return the prediction
   * @throws Exception if an error occurs
   */
  @Override
  public double classifyInstance(Instance inst) throws Exception {

    if (m_attribute == null) {
      return m_intercept;
    } else {
      if (inst.isMissing(m_attributeIndex)) {
        return m_classMeanForMissing;
      }
      return m_intercept + m_slope * inst.value(m_attributeIndex);
    }
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
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Builds a simple linear regression model given the supplied training data.
   * 
   * @param insts the training data.
   * @throws Exception if an error occurs
   */
  @Override
  public void buildClassifier(Instances insts) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(insts);

    if (m_outputAdditionalStats) {
      // check that the instances weights are all 1
      // because the RegressionAnalysis class does
      // not handle weights
      boolean ok = true;
      for (int i = 0; i < insts.numInstances(); i++) {
        if (insts.instance(i).weight() != 1) {
          ok = false;
          break;
        }
      }
      if (!ok) {
        throw new Exception(
          "Can only compute additional statistics on unweighted data");
      }
    }

    // Compute sums and counts
    double[] sum = new double[insts.numAttributes()];
    double[] count = new double[insts.numAttributes()];
    double[] classSumForMissing = new double[insts.numAttributes()];
    double[] classSumSquaredForMissing = new double[insts.numAttributes()];
    double classCount = 0;
    double classSum = 0;
    for (int j = 0; j < insts.numInstances(); j++) {
      Instance inst = insts.instance(j);
      if (!inst.classIsMissing()) {
        for (int i = 0; i < insts.numAttributes(); i++) {
          if (!inst.isMissing(i)) {
            sum[i] += inst.weight() * inst.value(i);
            count[i] += inst.weight();
          } else {
            classSumForMissing[i] += inst.classValue() * inst.weight();
            classSumSquaredForMissing[i] +=
              inst.classValue() * inst.classValue() * inst.weight();
          }
        }
        classCount += inst.weight();
        classSum += inst.weight() * inst.classValue();
      }
    }

    // Compute means
    double[] mean = new double[insts.numAttributes()];
    double[] classMeanForMissing = new double[insts.numAttributes()];
    double[] classMeanForKnown = new double[insts.numAttributes()];
    for (int i = 0; i < insts.numAttributes(); i++) {
      if (i != insts.classIndex()) {
        if (count[i] > 0) {
          mean[i] = sum[i] / count[i];
        }
        if (classCount - count[i] > 0) {
          classMeanForMissing[i] =
            classSumForMissing[i] / (classCount - count[i]);
        }
        if (count[i] > 0) {
          classMeanForKnown[i] = (classSum - classSumForMissing[i]) / count[i];
        }
      }
    }
    sum = null;
    count = null;

    double[] slopes = new double[insts.numAttributes()];
    double[] sumWeightedDiffsSquared = new double[insts.numAttributes()];
    double[] sumWeightedClassDiffsSquared = new double[insts.numAttributes()];

    // For all instances
    for (int j = 0; j < insts.numInstances(); j++) {
      Instance inst = insts.instance(j);

      // Only need to do something if the class isn't missing
      if (!inst.classIsMissing()) {

        // For all attributes
        for (int i = 0; i < insts.numAttributes(); i++) {
          if (!inst.isMissing(i) && (i != insts.classIndex())) {
            double yDiff = inst.classValue() - classMeanForKnown[i];
            double weightedYDiff = inst.weight() * yDiff;
            double diff = inst.value(i) - mean[i];
            double weightedDiff = inst.weight() * diff;
            slopes[i] += weightedYDiff * diff;
            sumWeightedDiffsSquared[i] += weightedDiff * diff;
            sumWeightedClassDiffsSquared[i] += weightedYDiff * yDiff;
          }
        }
      }
    }

    // Pick the best attribute
    double minSSE = Double.MAX_VALUE;
    m_attribute = null;
    int chosen = -1;
    double chosenSlope = Double.NaN;
    double chosenIntercept = Double.NaN;
    double chosenMeanForMissing = Double.NaN;
    for (int i = 0; i < insts.numAttributes(); i++) {

      // Do we have missing values for this attribute?
      double sseForMissing = classSumSquaredForMissing[i] -
        (classSumForMissing[i] * classMeanForMissing[i]);

      // Should we skip this attribute?
      if ((i == insts.classIndex()) || (sumWeightedDiffsSquared[i] == 0)) {
        continue;
      }

      // Compute final slope and intercept
      double numerator = slopes[i];
      slopes[i] /= sumWeightedDiffsSquared[i];
      double intercept = classMeanForKnown[i] - slopes[i] * mean[i];

      // Compute sum of squared errors
      double sse = sumWeightedClassDiffsSquared[i] - slopes[i] * numerator;

      // Add component due to missing value prediction
      sse += sseForMissing;

      // Check whether this is the best attribute
      if (sse < minSSE) {
        minSSE = sse;
        chosen = i;
        chosenSlope = slopes[i];
        chosenIntercept = intercept;
        chosenMeanForMissing = classMeanForMissing[i];
      }
    }

    // Set parameters
    if (chosen == -1) {
      if (!m_suppressErrorMessage) {
        System.err.println("----- no useful attribute found");
      }
      m_attribute = null;
      m_attributeIndex = 0;
      m_slope = 0;
      m_intercept = classSum / classCount;
      m_classMeanForMissing = 0;
    } else {
      m_attribute = insts.attribute(chosen);
      m_attributeIndex = chosen;
      m_slope = chosenSlope;
      m_intercept = chosenIntercept;
      m_classMeanForMissing = chosenMeanForMissing;

      if (m_outputAdditionalStats) {

        // Reduce data so that stats are correct
        Instances newInsts = new Instances(insts, insts.numInstances());
        for (int i = 0; i < insts.numInstances(); i++) {
          Instance inst = insts.instance(i);
          if (!inst.classIsMissing() && !inst.isMissing(m_attributeIndex)) {
            newInsts.add(inst);
          }
        }
        insts = newInsts;

        // do regression analysis
        m_df = insts.numInstances() - 2;
        double[] stdErrors = RegressionAnalysis.calculateStdErrorOfCoef(insts,
          m_attribute, m_slope, m_intercept, m_df);
        m_seSlope = stdErrors[0];
        m_seIntercept = stdErrors[1];
        double[] coef = new double[2];
        coef[0] = m_slope;
        coef[1] = m_intercept;
        double[] tStats = RegressionAnalysis
          .calculateTStats(coef, stdErrors, 2);
        m_tstatSlope = tStats[0];
        m_tstatIntercept = tStats[1];
        double ssr = RegressionAnalysis.calculateSSR(insts, m_attribute,
          m_slope, m_intercept);
        m_rsquared = RegressionAnalysis.calculateRSquared(insts, ssr);
        m_rsquaredAdj = RegressionAnalysis.calculateAdjRSquared(m_rsquared,
          insts.numInstances(), 2);
        m_fstat = RegressionAnalysis.calculateFStat(m_rsquared,
          insts.numInstances(), 2);
      }
    }
  }

  /**
   * Returns true if a usable attribute was found.
   * 
   * @return true if a usable attribute was found.
   */
  public boolean foundUsefulAttribute() {
    return (m_attribute != null);
  }

  /**
   * Returns the index of the attribute used in the regression.
   * 
   * @return the index of the attribute.
   */
  public int getAttributeIndex() {
    return m_attributeIndex;
  }

  /**
   * Returns the slope of the function.
   * 
   * @return the slope.
   */
  public double getSlope() {
    return m_slope;
  }

  /**
   * Returns the intercept of the function.
   * 
   * @return the intercept.
   */
  public double getIntercept() {
    return m_intercept;
  }

  /**
   * Turn off the error message that is reported when no useful attribute is
   * found.
   * 
   * @param s if set to true turns off the error message
   */
  public void setSuppressErrorMessage(boolean s) {
    m_suppressErrorMessage = s;
  }

  /**
   * Returns a description of this classifier as a string
   * 
   * @return a description of the classifier.
   */
  @Override
  public String toString() {

    StringBuffer text = new StringBuffer();
    if (m_attribute == null) {
      text.append("Predicting constant " + m_intercept);
    } else {
      text.append("Linear regression on " + m_attribute.name() + "\n\n");
      text
        .append(Utils.doubleToString(m_slope, 2) + " * " + m_attribute.name());
      if (m_intercept > 0) {
        text.append(" + " + Utils.doubleToString(m_intercept, 2));
      } else {
        text.append(" - " + Utils.doubleToString((-m_intercept), 2));
      }
      text.append("\n\nPredicting "
        + Utils.doubleToString(m_classMeanForMissing, 2) +
        " if attribute value is missing.");

      if (m_outputAdditionalStats) {
        // put regression analysis here
        int attNameLength = m_attribute.name().length() + 3;
        if (attNameLength < "Variable".length() + 3) {
          attNameLength = "Variable".length() + 3;
        }
        text.append("\n\nRegression Analysis:\n\n"
          + Utils.padRight("Variable", attNameLength)
          + "  Coefficient     SE of Coef        t-Stat");

        text.append("\n" + Utils.padRight(m_attribute.name(), attNameLength));
        text.append(Utils.doubleToString(m_slope, 12, 4));
        text.append("   " + Utils.doubleToString(m_seSlope, 12, 5));
        text.append("   " + Utils.doubleToString(m_tstatSlope, 12, 5));
        text.append(Utils.padRight("\nconst", attNameLength + 1)
          + Utils.doubleToString(m_intercept, 12, 4));
        text.append("   " + Utils.doubleToString(m_seIntercept, 12, 5));
        text.append("   " + Utils.doubleToString(m_tstatIntercept, 12, 5));
        text.append("\n\nDegrees of freedom = " + Integer.toString(m_df));
        text.append("\nR^2 value = " + Utils.doubleToString(m_rsquared, 5));
        text.append("\nAdjusted R^2 = "
          + Utils.doubleToString(m_rsquaredAdj, 5));
        text.append("\nF-statistic = " + Utils.doubleToString(m_fstat, 5));
      }
    }
    text.append("\n");
    return text.toString();
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11130 $");
  }

  /**
   * Main method for testing this class
   * 
   * @param argv options
   */
  public static void main(String[] argv) {
    runClassifier(new SimpleLinearRegression(), argv);
  }
}
