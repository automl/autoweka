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
import weka.core.SparseInstance;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests ChangeDateFormat. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.ChangeDateFormatTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class ChangeDateFormatTest 
  extends AbstractFilterTest {

  /** for comparing the instances */
  protected InstanceComparator m_Comparator;
  
  public ChangeDateFormatTest(String name) { 
    super(name);  
  }

  /** Need to set class index */
  protected void setUp() throws Exception {
    super.setUp();

    m_Instances.setClassIndex(1);
    m_Comparator = new InstanceComparator(true);
  }
  
  /** Creates a default ChangeDateFormat */
  public Filter getFilter() {
    ChangeDateFormat f = new ChangeDateFormat();
    return f;
  }

  /**
   * format must be different in precision (e.g., yyyy-MM instead of
   * yyyy-MM-dd) from the one in "weka.filters.data.FilterTest.arff", otherwise
   * this test will fail! 
   * Note: Sparse instances are skipped.
   */
  public void testTypical() {
    m_Filter = getFilter();
    ((ChangeDateFormat) m_Filter).setDateFormat("yyyy-MM");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // all instance's must be different
    boolean equal = false;
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      if (m_Instances.instance(i) instanceof SparseInstance)
        continue;
      if (m_Comparator.compare(
            m_Instances.instance(i), result.instance(i)) == 0) {
        equal = true;
        break;
      }
    }
    if (equal)
      fail("Instances not changed!");
  }

  /**
   * format must be the same as in "weka.filters.data.FilterTest.arff",
   * otherwise this test will fail!
   * Note: Sparse instances are skipped.
   */
  public void testSameFormat() {
    m_Filter = getFilter();
    ((ChangeDateFormat) m_Filter).setDateFormat("yyyy-MM-dd");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // all instance's must be the same
    boolean equal = true;
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      if (m_Instances.instance(i) instanceof SparseInstance)
        continue;
      if (m_Comparator.compare(
            m_Instances.instance(i), result.instance(i)) != 0) {
        equal = false;
        break;
      }
    }
    if (!equal)
      fail("Instances modified!");
  }
  
  /**
   * tests the filter in conjunction with the FilteredClassifier
   */
  public void testFilteredClassifier() {
    try {
      Instances data = getFilteredClassifierData();

      for (int i = 0; i < data.numAttributes(); i++) {
	if (data.classIndex() == i)
	  continue;
	if (data.attribute(i).isDate()) {
	  ((ChangeDateFormat) m_FilteredClassifier.getFilter()).setAttributeIndex(
	      "" + (i + 1));
	  break;
	}
      }
    }
    catch (Exception e) {
      fail("Problem setting up test for FilteredClassifier: " + e.toString());
    }
    
    super.testFilteredClassifier();
  }

  public static Test suite() {
    return new TestSuite(ChangeDateFormatTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
