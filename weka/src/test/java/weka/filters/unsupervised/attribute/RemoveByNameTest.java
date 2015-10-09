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
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests RemoveByName. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.RemoveByNameTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class RemoveByNameTest
  extends AbstractFilterTest {
  
  /**
   * Initializes the test.
   * 
   * @param name	the name of the test
   */
  public RemoveByNameTest(String name) { 
    super(name);
  }

  /**
   * Creates a default RemoveByName.
   * 
   * @return		the filter
   */
  public Filter getFilter() {
    return getFilter(RemoveByName.DEFAULT_EXPRESSION, false);
  }
  
  /**
   * returns a custom filter.
   * 
   * @param expression	the expression to use
   * @param invert	whether to invert the matching sense
   * @return		the configured filter
   */
  protected Filter getFilter(String expression, boolean invert) {
    RemoveByName	filter;
    
    filter = new RemoveByName();
    filter.setExpression(expression);
    filter.setInvertSelection(invert);
    
    return filter;
  }

  /**
   * Tests removing all attributes starting with "String".
   */
  public void testTypical() {
    Instances 	result;

    m_Filter = getFilter("^String.*", false);

    // 1. with class attribute
    result = useFilter();
    // Number of attributes will be two less, number of instances won't change
    assertEquals(m_Instances.numAttributes() - 2, result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }

  /**
   * Tests removing all attributes starting with "Nominal", one of them being
   * the class attribute.
   */
  public void testTypicalWithClass() {
    Instances 	result;

    m_Instances.setClassIndex(1);  // "NominalAtt1"
    m_Filter = getFilter("^Nominal.*", false);

    // 1. with class attribute
    result = useFilter();
    // Number of attributes will be two less, number of instances won't change
    assertEquals(m_Instances.numAttributes() - 1, result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }

  /**
   * Tests removing all attributes but attributes ending with "Att2".
   */
  public void testTypicalInverted() {
    Instances 	result;

    m_Filter = getFilter(".*Att2$", true);

    // 1. with class attribute
    result = useFilter();
    // Number of attributes will be two less, number of instances won't change
    assertEquals(3, result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }

  /**
   * Returns a test suite.
   * 
   * @return		the test suite
   */
  public static Test suite() {
    return new TestSuite(RemoveByNameTest.class);
  }

  /**
   * Runs the test from commandline.
   * 
   * @param args	ignored
   */
  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
