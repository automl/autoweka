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
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.supervised.attribute;

import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests AddClassification. Run from the command line with: <p/>
 * java weka.filters.supervised.attribute.AddClassificationTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class AddClassificationTest 
  extends AbstractFilterTest {
  
  public AddClassificationTest(String name) { 
    super(name);  
  }

  /** Creates a default AddClassification */
  public Filter getFilter() {
    return new AddClassification();
  }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default filter to test and loads a test set of Instances.
   *
   * @throws Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    super.setUp();

    m_Instances.deleteAttributeType(Attribute.STRING);
    m_Instances.setClassIndex(0);
  }

  /**
   * sets up the filter and performs the test
   * 
   * @param num		whether the class is numeric or nominal
   * @param cl		whether the classification is to be output
   * @param dist	whether the distribution is to be output
   * @param error	whether the error flag is to be output
   * @param remove	whether to remove the old class attribute
   */
  protected void performTest(boolean num, boolean cl, boolean dist, boolean error, boolean remove) {
    Instances	icopy;
    int		numAtts;
    
    // setup dataset
    if (num)
      m_Instances.setClassIndex(1);
    else
      m_Instances.setClassIndex(0);
    icopy = new Instances(m_Instances);

    // setup filter
    m_Filter = getFilter();
    if (num)
      ((AddClassification) m_Filter).setClassifier(new weka.classifiers.trees.M5P());
    else
      ((AddClassification) m_Filter).setClassifier(new weka.classifiers.trees.J48());
    
    ((AddClassification) m_Filter).setOutputClassification(cl);
    ((AddClassification) m_Filter).setOutputDistribution(dist);
    ((AddClassification) m_Filter).setOutputErrorFlag(error);
    ((AddClassification) m_Filter).setRemoveOldClass(remove);
    
    numAtts = icopy.numAttributes();
    if (cl)
      numAtts++;
    if (dist)
      numAtts += icopy.numClasses();
    if (error)
      numAtts++;
    if (remove)
      numAtts--;
    
    Instances result = useFilter();
    assertEquals(result.numAttributes(), numAtts);
  }
  
  /**
   * performs the application with no options set
   */
  public void testDefault() {
    Instances icopy = new Instances(m_Instances);
    
    m_Filter = getFilter();
    Instances result = useFilter();
    assertEquals(result.numAttributes(), icopy.numAttributes());
  }
  
  /**
   * performs the application with no options set (Nominal class)
   */
  public void testNoneNominal() {
    performTest(false, false, false, false, false);
  }
  
  /**
   * performs the application with only error flag set (Nominal class)
   */
  public void testErrorFlagNominal() {
    performTest(false, false, false, true, false);
  }
  
  /**
   * performs the application with only classification set (Nominal class)
   */
  public void testClassificationNominal() {
    performTest(false, true, false, false, false);
  }
  
  /**
   * performs the application with only distribution set (Nominal class)
   */
  public void testDistributionNominal() {
    performTest(false, false, true, false, false);
  }
  
  /**
   * performs the application with no options set (Nominal class)
   */
  public void testNoneNumeric() {
    performTest(true, false, false, false, false);
  }
  
  /**
   * performs the application with only error flag set (Numeric class)
   */
  public void testErrorFlagNumeric() {
    performTest(true, false, false, true, false);
  }
  
  /**
   * performs the application with only classification set (Numeric class)
   */
  public void testClassificationNumeric() {
    performTest(true, true, false, false, false);
  }
  
  /**
   * performs the application with only distribution set (Numeric class)
   */
  public void testDistributionNumeric() {
    performTest(true, false, true, false, false);
  }

  public static Test suite() {
    return new TestSuite(AddClassificationTest.class);
  }
  
  /**
   * performs the application with only classification set (Nominal class)
   * and removal of the old class attribute
   */
  public void testClassificationRemoveNominal() {
    performTest(false, true, false, false, true);
  }
  
  /**
   * performs the application with only classification set (numeric class)
   * and removal of the old class attribute
   */
  public void testClassificationRemoveNumeric() {
    performTest(true, true, false, false, true);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
  
  /**
   * performs the application with only removal of the old class attribute
   * (nominal)
   */
  public void testClassificationOnlyRemoveNominal() {
    performTest(false, false, false, false, true);
  }
  
  /**
   * performs the application with only removal of the old class attribute
   * (numeric)
   */
  public void testClassificationOnlyRemoveNumeric() {
    performTest(true, false, false, false, true);
  }
}
