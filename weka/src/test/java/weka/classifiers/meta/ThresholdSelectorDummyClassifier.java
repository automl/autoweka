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
 * Copyright (C) 2002 University of Waikato 
 */

package weka.classifiers.meta;

import weka.classifiers.AbstractClassifier;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionUtils;

/**
 * Dummy classifier - used in ThresholdSelectorTest.
 * 
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @author FracPete (fracpet at waikato dor ac dot nz)
 * @version $Revision: 11263 $
 * @see ThresholdSelectorTest
 */
public class ThresholdSelectorDummyClassifier
  extends AbstractClassifier {

  /** for serialization */
  private static final long serialVersionUID = -2040984810834943903L;

  private final double[] m_Preds;
  private int m_Pos;

  public ThresholdSelectorDummyClassifier(double[] preds) {

    m_Preds = new double[preds.length];

    for (int i = 0; i < preds.length; i++) {
      m_Preds[i] = preds[i];
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

    // attribute
    result.enableAllAttributes();
    result.disable(Capability.STRING_ATTRIBUTES);
    result.disable(Capability.RELATIONAL_ATTRIBUTES);

    // class
    result.enable(Capability.NOMINAL_CLASS);

    return result;
  }

  @Override
  public void buildClassifier(Instances train) {
    m_Pos = 0;
  }

  @Override
  public double[] distributionForInstance(Instance test) throws Exception {
    double[] result = new double[test.numClasses()];
    int pred = 0;
    result[pred] = m_Preds[m_Pos];
    double residual = (1.0 - result[pred]) / (result.length - 1);
    for (int i = 0; i < result.length; i++) {
      if (i != pred) {
        result[i] = residual;
      }
    }
    m_Pos = (m_Pos + 1) % m_Preds.length;
    return result;
  }

  @Override
  public String toString() {
    StringBuffer buff = new StringBuffer();
    for (double m_Pred : m_Preds) {
      buff.append(m_Pred + " ");
    }

    return buff.toString();
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11263 $");
  }
}
