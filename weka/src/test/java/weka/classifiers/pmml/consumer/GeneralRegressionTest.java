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
 * Copyright 2008 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.pmml.consumer;

import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests the pmml GeneralRegression classifier.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision 1.0 $
 */
public class GeneralRegressionTest extends AbstractPMMLClassifierTest {

  public GeneralRegressionTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
    m_modelNames = new ArrayList<String>();
    m_dataSetNames = new ArrayList<String>();
    m_modelNames.add("polynomial_regression_model.xml");
    m_modelNames.add("HEART_NOMREG.xml");
    m_dataSetNames.add("Elnino_small.arff");
    m_dataSetNames.add("heart-c.arff");
  }

  public static Test suite() {
    return new TestSuite(
      weka.classifiers.pmml.consumer.GeneralRegressionTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
