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
 * Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.instance;

import weka.core.InstanceComparator;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests ReservoirSample. Run from the command line with: <p/>
 * java weka.filters.unsupervised.instance.ReservoirSampleTest
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}org)
 * @version $Revision: 8034 $
 */
public class ReservoirSampleTest
  extends AbstractFilterTest {

  /** for comparing the instances */
  protected InstanceComparator m_Comparator;
  
  public ReservoirSampleTest(String name) { 
    super(name);  
  }

  protected void setUp() throws Exception {
    super.setUp();
    
    m_Comparator = new InstanceComparator(true);
  }

  /** Creates a default ReservoirSample */
  public Filter getFilter() {
    ReservoirSample r = new ReservoirSample();
    return r;
  }

  public void testTypical() {
    m_Filter = getFilter();
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    
    // instances should be indentical as default settings ask for
    // a larger sample than there is number of instances in the test
    // dataset
    boolean equal = true;
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      if (m_Comparator.compare(
            m_Instances.instance(i), result.instance(i)) != 0) {
        equal = false;
        break;
      }
    }
    if (!equal) {
      fail("Result should be equal");
    }
  }

  public void testSubSample() {
    m_Filter = getFilter();
    ((ReservoirSample)m_Filter).setSampleSize(10);
    
    Instances result = useFilter();
    assertEquals(result.numInstances(), 10);

    // instances should be different from the first 10 instances in
    // the original data

    boolean equal = true;
    for (int i = 0; i < result.numInstances(); i++) {
      if (m_Comparator.compare(
            m_Instances.instance(i), result.instance(i)) != 0) {
        equal = false;
        break;
      }
    }

    if (equal) {
      fail("Result should be different than the first 10 instances");
    }
  }

  public void testHeaderOnlyInput() {
    m_Filter = getFilter();
    m_Instances = new Instances(m_Instances, 0);
    Instances result = useFilter();
    assertEquals(result.numInstances(), m_Instances.numInstances());
  }

  public static Test suite() {
    return new TestSuite(ReservoirSampleTest.class);
  }
  
  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}