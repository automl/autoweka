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
 * Copyright (C) 2008 University of Waikato 
 */

package weka.filters.unsupervised.instance;

import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests SubsetByExpression. Run from the command line with: <p/>
 * java weka.filters.unsupervised.instance.SubsetByExpressionTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class SubsetByExpressionTest
  extends AbstractFilterTest {
  
  /**
   * Setup the test.
   * 
   * @param name	the name of the test
   */
  public SubsetByExpressionTest(String name) {
    super(name);
  }

  /**
   * Called by JUnit before each test method. 
   * <p/>
   * Removes all the string attributes and sets the first attribute as class 
   * attribute.
   *
   * @throws Exception 	if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    m_Instances.deleteAttributeType(Attribute.STRING);
    m_Instances.setClassIndex(0);
  }

  /**
   * Creates a default SubsetByExpression filter.
   * 
   * @return		the filter
   */
  public Filter getFilter() {
    return new SubsetByExpression();
  }

  /**
   * Creates a SubsetByExpression filter with the given expression.
   * 
   * @param expr	the expression to use
   * @return		the filter
   */
  public Filter getFilter(String expr) {
    SubsetByExpression result = new SubsetByExpression();
    result.setExpression(expr);
    return result;
  }
  
  /**
   * Tests the "ismissing" functionality.
   */
  public void testIsmissing() {
    m_Filter = getFilter("ismissing(ATT3)");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(3, result.numInstances());
  }
  
  /**
   * Tests the "not ismissing" functionality.
   */
  public void testNotIsmissing() {
    m_Filter = getFilter("not ismissing(ATT3)");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances() - 3, result.numInstances());
  }
  
  /**
   * Tests the "CLASS" shortcut with 'is'.
   */
  public void testClassIs() {
    m_Filter = getFilter("CLASS is 'g'");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(3, result.numInstances());
  }
  
  /**
   * Tests the "CLASS" shortcut with 'is' over all class labels, using ' or '.
   */
  public void testClassIs2() {
    m_Filter = getFilter("(CLASS is 'r') or (CLASS is 'g') or (CLASS is 'b')");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }
  
  /**
   * Tests the "ATT1" shortcut with 'is'.
   */
  public void testAttIs() {
    m_Filter = getFilter("ATT1 is 'r'");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(12, result.numInstances());
  }
  
  /**
   * Tests the "&gt;" functionality.
   */
  public void testGreater() {
    m_Filter = getFilter("ATT2 > 4");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(13, result.numInstances());
  }
  
  /**
   * Tests the "&lt;" functionality.
   */
  public void testLess() {
    m_Filter = getFilter("ATT2 < 4");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(7, result.numInstances());
  }
  
  /**
   * Tests the "&gt;=" functionality.
   */
  public void testGreaterOrEqual() {
    m_Filter = getFilter("ATT2 >= 4");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(14, result.numInstances());
  }
  
  /**
   * Tests the "&lt;=" functionality.
   */
  public void testLessOrEqual() {
    m_Filter = getFilter("ATT2 <= 4");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(8, result.numInstances());
  }
  
  /**
   * Tests the "=" functionality.
   */
  public void testEqual() {
    m_Filter = getFilter("ATT2 = 4");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(1, result.numInstances());
  }
  
  /**
   * Tests the "ATT1" shortcut with 'is' and restricting it via ' and '.
   */
  public void testAnd() {
    m_Filter = getFilter("(ATT1 is 'r') and (ATT2 <= 5)");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(6, result.numInstances());
  }

  /**
   * Returns a test suite.
   * 
   * @return		test suite
   */
  public static Test suite() {
    return new TestSuite(SubsetByExpressionTest.class);
  }

  /**
   * Runs the test from command-line.
   * 
   * @param args	ignored
   */
  public static void main(String[] args){
    TestRunner.run(suite());
  }
}
