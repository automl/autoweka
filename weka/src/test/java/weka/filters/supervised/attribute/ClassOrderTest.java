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

package weka.filters.supervised.attribute;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests ClassOrder. Run from the command line with: <p/>
 * java weka.filters.supervised.attribute.ClassOrderTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class ClassOrderTest 
  extends AbstractFilterTest {
  
  /** the class index to use for the tests */
  protected int m_ClassIndex = 4;
  
  public ClassOrderTest(String name) { 
    super(name);  
  }

  /** Need to set the class index */
  protected void setUp() throws Exception {
    super.setUp();
    m_Instances.setClassIndex(m_ClassIndex);
  }

  /** Creates a default ClassOrder */
  public Filter getFilter() {
    return new ClassOrder();
  }

  /**
   * compares the generated dataset with the original one
   */
  protected void performTests(Instances result) {
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // None of the attributes should have changed
    for (int i = 0; i < result.numAttributes(); i++) {
      assertEquals(m_Instances.attribute(i).type(), result.attribute(i).type());
      assertEquals(m_Instances.attribute(i).name(), result.attribute(i).name());
    }
    // did the order change?
    boolean orderEqual = true;
    for (int i = 0; i < result.numClasses(); i++) {
      if (!m_Instances.classAttribute().value(i).equals(
            result.classAttribute().value(i))) {
        orderEqual = false;
        break;
      }
    }
    if (orderEqual)
      fail("Order wasn't changed!");
  }

  /**
   * tests the RANDOM order
   */
  public void testRandom() {
    m_Filter = getFilter();
    ((ClassOrder) m_Filter).setClassOrder(ClassOrder.RANDOM);
    Instances result = useFilter();
    performTests(result);
  }

  /**
   * tests the FREQ_ASCEND order
   */
  public void testFreqAscend() {
    m_Filter = getFilter();
    ((ClassOrder) m_Filter).setClassOrder(ClassOrder.FREQ_ASCEND);
    Instances result = useFilter();
    performTests(result);
  }

  /**
   * tests the FREQ_DESCEND order
   */
  public void testFreqDescend() {
    m_Filter = getFilter();
    ((ClassOrder) m_Filter).setClassOrder(ClassOrder.FREQ_DESCEND);
    Instances result = useFilter();
    performTests(result);
  }

  public static Test suite() {
    return new TestSuite(ClassOrderTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
