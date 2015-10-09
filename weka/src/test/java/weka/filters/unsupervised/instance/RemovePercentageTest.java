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

package weka.filters.unsupervised.instance;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests RemovePercentage. Run from the command line with: <p/>
 * java weka.filters.unsupervised.instance.RemovePercentageTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class RemovePercentageTest 
  extends AbstractFilterTest {
  
  public RemovePercentageTest(String name) { 
    super(name);  
  }

  /** Need to remove non-nominal attributes, set class index */
  protected void setUp() throws Exception {
    super.setUp();

    m_Instances.setClassIndex(1);
  }
  
  /** Creates a default RemovePercentage */
  public Filter getFilter() {
    RemovePercentage f = new RemovePercentage();
    return f;
  }

  public void testTypical() {
    m_Filter = getFilter();
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
  }

  public void testInverting() {
    // non-inverted
    m_Filter = getFilter();
    ((RemovePercentage) m_Filter).setPercentage(20.0);
    Instances result = useFilter();

    // inverted
    m_Filter = getFilter();
    ((RemovePercentage) m_Filter).setPercentage(20.0);
    ((RemovePercentage) m_Filter).setInvertSelection(true);
    Instances resultInv = useFilter();

    assertEquals(
        m_Instances.numInstances(), 
        result.numInstances() + resultInv.numInstances());
  }

  public static Test suite() {
    return new TestSuite(RemovePercentageTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
