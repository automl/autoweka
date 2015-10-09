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
 * Tests RemoveUseless. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.RemoveUselessTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class RemoveUselessTest 
  extends AbstractFilterTest {

  public RemoveUselessTest(String name) { 
    super(name);  
  }

  /** Need to remove non-nominal attributes, set class index */
  protected void setUp() throws Exception {
    super.setUp();

    // class index
    m_Instances.setClassIndex(1);
  }
  
  /** Creates a default RemoveUseless */
  public Filter getFilter() {
    return getFilter(new RemoveUseless().getMaximumVariancePercentageAllowed());
  }

  /**
   * creates a RemoveUseless filter with the given percentage of allowed
   * variance
   */
  protected Filter getFilter(double percentage) {
    RemoveUseless f = new RemoveUseless();
    f.setMaximumVariancePercentageAllowed(percentage);
    return f;
  }

  public void testNoneRemoved() {
    m_Filter = getFilter(100);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }

  public void testSomeRemoved() {
    m_Filter = getFilter(5);
    Instances result = useFilter();
    assertTrue(m_Instances.numAttributes() > result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }

  public static Test suite() {
    return new TestSuite(RemoveUselessTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
