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
 *    CorrelationAttributeEval.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.attributeSelection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 <!-- globalinfo-start -->
 * CorrelationAttributeEval :<br/>
 * <br/>
 * Evaluates the worth of an attribute by measuring the correlation (Pearson's) between it and the class.<br/>
 * <br/>
 * Nominal attributes are considered on a value by value basis by treating each value as an indicator. An overall correlation for a nominal attribute is arrived at via a weighted average.<br/>
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  Output detailed info for nominal attributes</pre>
 * 
 <!-- options-end -->
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 9049 $
 */
public class CorrelationAttributeEval extends ASEvaluation implements
    AttributeEvaluator, OptionHandler {

  /** For serialization */
  private static final long serialVersionUID = -4931946995055872438L;

  /** The correlation for each attribute */
  protected double[] m_correlations;

  /** Whether to output detailed (per value) correlation for nominal attributes */
  protected boolean m_detailedOutput = false;

  /** Holds the detailed output info */
  protected StringBuffer m_detailedOutputBuff;

  /**
   * Returns a string describing this attribute evaluator
   * 
   * @return a description of the evaluator suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "CorrelationAttributeEval :\n\nEvaluates the worth of an attribute "
        + "by measuring the correlation (Pearson's) between it and the class.\n\n"
        + "Nominal attributes are considered on a value by "
        + "value basis by treating each value as an indicator. An overall "
        + "correlation for a nominal attribute is arrived at via a weighted average.\n";
  }

  /**
   * Returns the capabilities of this evaluator.
   * 
   * @return the capabilities of this evaluator
   * @see Capabilities
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
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   **/
  @Override
  public Enumeration listOptions() {
    // TODO Auto-generated method stub
    Vector<Option> newVector = new Vector<Option>();

    newVector.addElement(new Option(
        "\tOutput detailed info for nominal attributes", "D", 0, "-D"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  Output detailed info for nominal attributes</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    setOutputDetailedInfo(Utils.getFlag('D', options));
  }

  /**
   * Gets the current settings of WrapperSubsetEval.
   * 
   * @return an array of strings suitable for passing to setOptions()
   */
  @Override
  public String[] getOptions() {
    String[] options = new String[1];

    if (getOutputDetailedInfo()) {
      options[0] = "-D";
    } else {
      options[0] = "";
    }

    return options;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String outputDetailedInfoTipText() {
    return "Output per value correlation for nominal attributes";
  }

  /**
   * Set whether to output per-value correlation for nominal attributes
   * 
   * @param d true if detailed (per-value) correlation is to be output for
   *          nominal attributes
   */
  public void setOutputDetailedInfo(boolean d) {
    m_detailedOutput = d;
  }

  /**
   * Get whether to output per-value correlation for nominal attributes
   * 
   * @return true if detailed (per-value) correlation is to be output for
   *         nominal attributes
   */
  public boolean getOutputDetailedInfo() {
    return m_detailedOutput;
  }

  /**
   * Evaluates an individual attribute by measuring the correlation (Pearson's)
   * between it and the class. Nominal attributes are considered on a value by
   * value basis by treating each value as an indicator. An overall correlation
   * for a nominal attribute is arrived at via a weighted average.
   * 
   * @param attribute the index of the attribute to be evaluated
   * @return the correlation
   * @throws Exception if the attribute could not be evaluated
   */
  @Override
  public double evaluateAttribute(int attribute) throws Exception {
    return m_correlations[attribute];
  }

  /**
   * Describe the attribute evaluator
   * 
   * @return a description of the attribute evaluator as a String
   */
  @Override
  public String toString() {
    StringBuffer buff = new StringBuffer();

    if (m_correlations == null) {
      buff.append("Correlation attribute evaluator has not been built yet.");
    } else {
      buff.append("\tCorrelation Ranking Filter");

      if (m_detailedOutput && m_detailedOutputBuff.length() > 0) {
        buff.append("\n\tDetailed output for nominal attributes");
        buff.append(m_detailedOutputBuff);
      }
    }

    return buff.toString();
  }

  /**
   * Initializes an information gain attribute evaluator. Replaces missing
   * values with means/modes; Deletes instances with missing class values.
   * 
   * @param data set of instances serving as training data
   * @throws Exception if the evaluator has not been generated successfully
   */
  @Override
  public void buildEvaluator(Instances data) throws Exception {
    data = new Instances(data);
    data.deleteWithMissingClass();

    ReplaceMissingValues rmv = new ReplaceMissingValues();
    rmv.setInputFormat(data);
    data = Filter.useFilter(data, rmv);

    int numClasses = data.classAttribute().numValues();
    int classIndex = data.classIndex();
    int numInstances = data.numInstances();
    m_correlations = new double[data.numAttributes()];
    /*
     * boolean hasNominals = false; boolean hasNumerics = false;
     */
    List<Integer> numericIndexes = new ArrayList<Integer>();
    List<Integer> nominalIndexes = new ArrayList<Integer>();
    if (m_detailedOutput) {
      m_detailedOutputBuff = new StringBuffer();
    }

    // TODO for instance weights (folded into computing weighted correlations)
    // add another dimension just before the last [2] (0 for 0/1 binary vector
    // and
    // 1 for corresponding instance weights for the 1's)
    double[][][] nomAtts = new double[data.numAttributes()][][];
    for (int i = 0; i < data.numAttributes(); i++) {
      if (data.attribute(i).isNominal() && i != classIndex) {
        nomAtts[i] = new double[data.attribute(i).numValues()][data
            .numInstances()];
        Arrays.fill(nomAtts[i][0], 1.0); // set zero index for this att to all
                                         // 1's
        nominalIndexes.add(i);
      } else if (data.attribute(i).isNumeric() && i != classIndex) {
        numericIndexes.add(i);
      }
    }

    // do the nominal attributes
    if (nominalIndexes.size() > 0) {
      for (int i = 0; i < data.numInstances(); i++) {
        Instance current = data.instance(i);
        for (int j = 0; j < current.numValues(); j++) {
          if (current.attribute(current.index(j)).isNominal()
              && current.index(j) != classIndex) {
            // Will need to check for zero in case this isn't a sparse
            // instance (unless we add 1 and subtract 1)
            nomAtts[current.index(j)][(int) current.valueSparse(j)][i] += 1;
            nomAtts[current.index(j)][0][i] -= 1;
          }
        }
      }
    }

    if (data.classAttribute().isNumeric()) {
      double[] classVals = data.attributeToDoubleArray(classIndex);

      // do the numeric attributes
      for (Integer i : numericIndexes) {
        double[] numAttVals = data.attributeToDoubleArray(i);
        m_correlations[i] = Utils.correlation(numAttVals, classVals,
            numAttVals.length);

        if (m_correlations[i] == 1.0) {
          // check for zero variance (useless numeric attribute)
          if (Utils.variance(numAttVals) == 0) {
            m_correlations[i] = 0;
          }
        }
      }

      // do the nominal attributes
      if (nominalIndexes.size() > 0) {

        // now compute the correlations for the binarized nominal attributes
        for (Integer i : nominalIndexes) {
          double sum = 0;
          double corr = 0;
          double sumCorr = 0;
          double sumForValue = 0;

          if (m_detailedOutput) {
            m_detailedOutputBuff.append("\n\n")
                .append(data.attribute(i).name());
          }

          for (int j = 0; j < data.attribute(i).numValues(); j++) {
            sumForValue = Utils.sum(nomAtts[i][j]);
            corr = Utils
                .correlation(nomAtts[i][j], classVals, classVals.length);

            // useless attribute - all instances have the same value
            if (sumForValue == numInstances || sumForValue == 0) {
              corr = 0;
            }
            if (corr < 0.0) {
              corr = -corr;
            }
            sumCorr += sumForValue * corr;
            sum += sumForValue;

            if (m_detailedOutput) {
              m_detailedOutputBuff.append("\n\t")
                  .append(data.attribute(i).value(j)).append(": ");
              m_detailedOutputBuff.append(Utils.doubleToString(corr, 6));
            }
          }
          m_correlations[i] = (sum > 0) ? sumCorr / sum : 0;
        }
      }
    } else {
      // class is nominal
      // TODO extra dimension for storing instance weights too
      double[][] binarizedClasses = new double[data.classAttribute()
          .numValues()][data.numInstances()];

      // this is equal to the number of instances for all inst weights = 1
      double[] classValCounts = new double[data.classAttribute().numValues()];

      for (int i = 0; i < data.numInstances(); i++) {
        Instance current = data.instance(i);
        binarizedClasses[(int) current.classValue()][i] = 1;
      }
      for (int i = 0; i < data.classAttribute().numValues(); i++) {
        classValCounts[i] = Utils.sum(binarizedClasses[i]);
      }

      double sumClass = Utils.sum(classValCounts);

      // do numeric attributes first
      if (numericIndexes.size() > 0) {
        for (Integer i : numericIndexes) {
          double[] numAttVals = data.attributeToDoubleArray(i);
          double corr = 0;
          double sumCorr = 0;

          for (int j = 0; j < data.classAttribute().numValues(); j++) {
            corr = Utils.correlation(numAttVals, binarizedClasses[j],
                numAttVals.length);
            if (corr < 0.0) {
              corr = -corr;
            }

            if (corr == 1.0) {
              // check for zero variance (useless numeric attribute)
              if (Utils.variance(numAttVals) == 0) {
                corr = 0;
              }
            }

            sumCorr += classValCounts[j] * corr;
          }
          m_correlations[i] = sumCorr / sumClass;
        }
      }

      if (nominalIndexes.size() > 0) {
        for (Integer i : nominalIndexes) {
          if (m_detailedOutput) {
            m_detailedOutputBuff.append("\n\n")
                .append(data.attribute(i).name());
          }

          double sumForAtt = 0;
          double corrForAtt = 0;
          for (int j = 0; j < data.attribute(i).numValues(); j++) {
            double sumForValue = Utils.sum(nomAtts[i][j]);
            double corr = 0;
            double sumCorr = 0;
            double avgCorrForValue = 0;

            sumForAtt += sumForValue;
            for (int k = 0; k < numClasses; k++) {

              // corr between value j and class k
              corr = Utils.correlation(nomAtts[i][j], binarizedClasses[k],
                  binarizedClasses[k].length);

              // useless attribute - all instances have the same value
              if (sumForValue == numInstances || sumForValue == 0) {
                corr = 0;
              }
              if (corr < 0.0) {
                corr = -corr;
              }
              sumCorr += classValCounts[k] * corr;
            }
            avgCorrForValue = sumCorr / sumClass;
            corrForAtt += sumForValue * avgCorrForValue;

            if (m_detailedOutput) {
              m_detailedOutputBuff.append("\n\t")
                  .append(data.attribute(i).value(j)).append(": ");
              m_detailedOutputBuff.append(Utils.doubleToString(avgCorrForValue,
                  6));
            }
          }

          // the weighted average corr for att i as
          // a whole (wighted by value frequencies)
          m_correlations[i] = (sumForAtt > 0) ? corrForAtt / sumForAtt : 0;
        }
      }
    }

    if (m_detailedOutputBuff != null && m_detailedOutputBuff.length() > 0) {
      m_detailedOutputBuff.append("\n");
    }
  }
  
  /**
   * Returns the revision string.
   * 
   * @return            the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 9049 $");
  }

  /**
   * Main method for testing this class.
   * 
   * @param args the options
   */
  public static void main(String[] args) {
    runEvaluator(new CorrelationAttributeEval(), args);
  }
}

