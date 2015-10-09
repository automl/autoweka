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
 * Copyright (C) 2008 University of Waikato 
 */

package weka.filters.unsupervised.attribute;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests MergeManyValues. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.MergeManyValuesTest
 *
 * @author Len Trigg (original MergeTwoValues code)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class MergeManyValuesTest
  extends AbstractFilterTest {
  
  /**
   * Initializes the test.
   * 
   * @param name	the name of the test
   */
  public MergeManyValuesTest(String name) {
    super(name);
  }

  /**
   * Creates an default MergeManyValues.
   * 
   * @return		the default filter.
   */
  public Filter getFilter() {
    MergeManyValues f = new MergeManyValues();
    // Ensure the filter we return can run on the test dataset
    f.setAttributeIndex("2"); 
    return f;
  }

  /**
   * Tests invalid attribute types.
   */
  public void testInvalidAttributeTypes() {
    Instances icopy = new Instances(m_Instances);
    try {
      ((MergeManyValues)m_Filter).setAttributeIndex("1");
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception selecting a STRING attribute!");
    } catch (Exception ex) {
      // OK
    }
    try {
      ((MergeManyValues)m_Filter).setAttributeIndex("3");
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception indicating a NUMERIC attribute!");
    } catch (Exception ex) {
      // OK
    }
  }

  /**
   * Tests default setup.
   */
  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the merging is correct
    int mergedIndex = -1;
    for (int i = 0; i < result.numInstances(); i++) {
      if ((m_Instances.instance(i).value(1) == 0) || 
          (m_Instances.instance(i).value(1) == 1)) {
        if (mergedIndex == -1) {
          mergedIndex = (int)result.instance(i).value(1);
        } else {
          assertEquals("Checking merged value for instance: " + (i + 1),
                       mergedIndex, (int)result.instance(i).value(1));
        }
      }
    }
  }

  /**
   * Tests a different range.
   */
  public void testDifferentRange() {
    ((MergeManyValues)m_Filter).setMergeValueRange("2,3");
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the merging is correct
    int mergedIndex = -1;
    for (int i = 0; i < result.numInstances(); i++) {
      if ((m_Instances.instance(i).value(1) == 1) || 
          (m_Instances.instance(i).value(1) == 2)) {
        if (mergedIndex == -1) {
          mergedIndex = (int)result.instance(i).value(1);
        } else {
          assertEquals("Checking merged value for instance: " + (i + 1),
                       mergedIndex, (int)result.instance(i).value(1));
        }
      }
    }
  }

  /**
   * Test merging all labels.
   */
  public void testMergeAll() {
    ((MergeManyValues)m_Filter).setMergeValueRange("first-last");
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    assertEquals(1, result.attribute(1).numValues());
  }

  /**
   * Tests attribute with missing values.
   */
  public void testAttributeWithMissing() {
    ((MergeManyValues)m_Filter).setAttributeIndex("5");
    ((MergeManyValues)m_Filter).setMergeValueRange("1,2");
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the merging is correct
    int mergedIndex = -1;
    for (int i = 0; i < result.numInstances(); i++) {
      if (m_Instances.instance(i).isMissing(4)) {
        assertTrue("Missing in input should give missing in result",
               result.instance(i).isMissing(4));
      } else if ((m_Instances.instance(i).value(4) == 0) || 
                 (m_Instances.instance(i).value(4) == 1)) {
        if (mergedIndex == -1) {
          mergedIndex = (int)result.instance(i).value(4);
        } else {
          assertEquals("Checking merged value for instance: " + (i + 1),
                       mergedIndex, (int)result.instance(i).value(4));
        }
      }
    }
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
	if (data.attribute(i).isNominal()) {
	  ((MergeManyValues) m_FilteredClassifier.getFilter()).setAttributeIndex(
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

  /**
   * Returns a test suite.
   * 
   * @return		the suite
   */
  public static Test suite() {
    return new TestSuite(MergeManyValuesTest.class);
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
