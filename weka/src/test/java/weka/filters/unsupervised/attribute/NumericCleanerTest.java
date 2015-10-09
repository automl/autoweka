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

package weka.filters.unsupervised.attribute;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests NumericCleaner. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.NumericCleanerTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class NumericCleanerTest 
  extends AbstractFilterTest {
  
  public NumericCleanerTest(String name) { 
    super(name);  
  }

  /** Creates a default NumericCleaner */
  public Filter getFilter() {
    return new NumericCleaner();
  }

  /**
   * runs a simple test
   */
  public void testTypical() {
    Instances icopy = new Instances(m_Instances);
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

    assertEquals(icopy.numAttributes(), result.numAttributes());
    assertEquals(icopy.numInstances(), result.numInstances());
  }

  public static Test suite() {
    return new TestSuite(NumericCleanerTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
