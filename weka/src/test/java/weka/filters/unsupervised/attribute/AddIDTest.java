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
 * Tests AddID. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.AddIDTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class AddIDTest 
  extends AbstractFilterTest {
  
  public AddIDTest(String name) { 
    super(name);  
  }

  /** Creates a default AddID */
  public Filter getFilter() {
    return new AddID();
  }

  /** Creates a specialized AddID */
  public Filter getFilter(int pos) {
    AddID af = new AddID();
    af.setIDIndex("" + (pos + 1));
    return af;
  }

  /**
   * performs the actual test
   */
  protected void performTest() {
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

    assertEquals((icopy.numAttributes() + 1), result.numAttributes());
    assertEquals(icopy.numInstances(), m_Instances.numInstances());
  }

  public void testAddFirst() {
    m_Filter = getFilter(0);
    testBuffered();
    performTest();
  }

  public void testAddLast() {
    m_Filter = getFilter(m_Instances.numAttributes() - 1);
    testBuffered();
    performTest();
  }

  public static Test suite() {
    return new TestSuite(AddIDTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
