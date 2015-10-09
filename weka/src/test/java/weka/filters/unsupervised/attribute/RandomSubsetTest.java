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
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests RandomSubset. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.RandomSubsetTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class RandomSubsetTest 
  extends AbstractFilterTest {
  
  /**
   * Initializes the test.
   * 
   * @param name	the name of the test
   */
  public RandomSubsetTest(String name) { 
    super(name);  
  }

  /**
   * Creates a default RandomSubset.
   * 
   * @return		the filter
   */
  public Filter getFilter() {
    return new RandomSubset();
  }

  /**
   * Creates a specialized RandomSubset.
   * 
   * @param num		the number of attributes
   * @return		the filter
   */
  public Filter getFilter(double num) {
    RandomSubset result = new RandomSubset();
    result.setNumAttributes(num);
    return result;
  }

  /**
   * performs the actual test.
   * 
   * @param numSel	the number of attributes to select
   * @param numOut	the number of attributes that are expected
   */
  protected void performTest(double numSel, int numOut) {
    m_Filter         = getFilter(numSel);
    Instances icopy  = new Instances(m_Instances);
    Instances result = null;
    try {
      m_Filter.setInputFormat(icopy);
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    try {
      result = Filter.useFilter(icopy, m_Filter);
      assertNotNull(result);
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on useFilter(): \n" + ex.getMessage());
    }

    assertEquals(numOut, result.numAttributes());
    assertEquals(icopy.numInstances(), m_Instances.numInstances());
  }

  /**
   * Tests a percentage.
   */
  public void testPercentage() {
    performTest(0.5, 4);
  }

  /**
   * Tests an absolute number.
   */
  public void testAbsolute() {
    performTest(5, 5);
  }

  /**
   * Returns a test suite.
   * 
   * @return		the test suite
   */
  public static Test suite() {
    return new TestSuite(RandomSubsetTest.class);
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
