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
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.meta;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;

import java.io.InputStreamReader;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests CostSensitiveClassifier. Run from the command line with:<p>
 * java weka.classifiers.meta.CostSensitiveClassifierTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 8034 $
 */
public class CostSensitiveClassifierTest extends AbstractClassifierTest {

  public CostSensitiveClassifierTest(String name) { 
    super(name);  
  }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default classifier to test and loads a test set of Instances.
   *
   * @exception Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    super.setUp();

    // can handle only as much classes as there are in the CostMatrix!
    m_NClasses = ((CostSensitiveClassifier) getClassifier()).getCostMatrix().numRows();
  }

  /** Creates a default CostSensitiveClassifier */
  public Classifier getClassifier() {

    CostSensitiveClassifier cl = new CostSensitiveClassifier();
    
    // load costmatrix
    try {
      cl.setCostMatrix(
          new CostMatrix(
            new InputStreamReader(ClassLoader.getSystemResourceAsStream(
                  "weka/classifiers/data/ClassifierTest.cost"))));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    
    return cl;
  }

  public static Test suite() {
    return new TestSuite(CostSensitiveClassifierTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
