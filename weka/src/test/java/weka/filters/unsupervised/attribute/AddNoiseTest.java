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

import weka.core.InstanceComparator;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests AddNoise. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.AddNoiseTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class AddNoiseTest 
  extends AbstractFilterTest {

  /** for comparing the instances */
  protected InstanceComparator m_Comparator;
  
  public AddNoiseTest(String name) { 
    super(name);  
  }

  /** Need to remove non-nominal attributes, set class index */
  protected void setUp() throws Exception {
    super.setUp();

    // class index
    m_Instances.setClassIndex(1);

    // only nominal attributes
    int i = 0;
    while (i < m_Instances.numAttributes()) {
      if (!m_Instances.attribute(i).isNominal())
        m_Instances.deleteAttributeAt(i);
      else
        i++;
    }

    m_Comparator = new InstanceComparator(true);
  }
  
  /** Creates a default AddNoise */
  public Filter getFilter() {
    AddNoise f = new AddNoise();
    return f;
  }

  public void testTypical() {
    m_Filter = getFilter();
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // at least one instance must be different
    boolean equal = true;
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      if (m_Comparator.compare(
            m_Instances.instance(i), result.instance(i)) != 0) {
        equal = false;
        break;
      }
    }
    if (equal)
      fail("No noise added!");
  }

  public void testNoNoise() {
    m_Filter = getFilter();
    ((AddNoise) m_Filter).setPercent(0);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // all instance's must be the same
    boolean equal = true;
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      if (m_Comparator.compare(
            m_Instances.instance(i), result.instance(i)) != 0) {
        equal = false;
        break;
      }
    }
    if (!equal)
      fail("Instances modified!");
  }

  public static Test suite() {
    return new TestSuite(AddNoiseTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
