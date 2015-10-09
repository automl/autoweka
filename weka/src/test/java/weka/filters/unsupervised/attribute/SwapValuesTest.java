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
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.attribute;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests SwapValues. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.SwapValuesTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public class SwapValuesTest extends AbstractFilterTest {
  
  public SwapValuesTest(String name) { super(name);  }

  /** Creates an example SwapValues */
  public Filter getFilter() {
    SwapValues f = new SwapValues();
    // Ensure the filter we return can run on the test dataset
    f.setAttributeIndex("2"); 
    return f;
  }

  public void testInvalidAttributeTypes() {
    Instances icopy = new Instances(m_Instances);
    try {
      ((SwapValues)m_Filter).setAttributeIndex("1");
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception selecting a STRING attribute!");
    } catch (Exception ex) {
      // OK
    }
    try {
      ((SwapValues)m_Filter).setAttributeIndex("3");
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception indicating a NUMERIC attribute!");
    } catch (Exception ex) {
      // OK
    }
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the swapping is correct
    int first = 0, second = 2;
    for (int i = 0; i < result.numInstances(); i++) {
      if (m_Instances.instance(i).value(1) == first) {
        assertTrue("Value should be swapped", result.instance(i).value(1) == second);
      } else if (m_Instances.instance(i).value(1) == second) {
        assertTrue("Value should be swapped", result.instance(i).value(1) == first);
      }
    }
  }

  public void testFirstValueIndex() {
    ((SwapValues)m_Filter).setFirstValueIndex("2");
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the swapping is correct
    int first = 1, second = 2;
    for (int i = 0; i < result.numInstances(); i++) {
      if (m_Instances.instance(i).value(1) == first) {
        assertTrue("Value should be swapped", result.instance(i).value(1) == second);
      } else if (m_Instances.instance(i).value(1) == second) {
        assertTrue("Value should be swapped", result.instance(i).value(1) == first);
      }
    }
  }

  public void testSecondValueIndex() {
    ((SwapValues)m_Filter).setSecondValueIndex("2");
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the swapping is correct
    int first = 0, second = 1;
    for (int i = 0; i < result.numInstances(); i++) {
      if (m_Instances.instance(i).value(1) == first) {
        assertTrue("Value should be swapped", result.instance(i).value(1) == second);
      } else if (m_Instances.instance(i).value(1) == second) {
        assertTrue("Value should be swapped", result.instance(i).value(1) == first);
      }
    }
  }

  public void testAttributeWithMissing() {
    ((SwapValues)m_Filter).setAttributeIndex("5");
    ((SwapValues)m_Filter).setFirstValueIndex("1");
    ((SwapValues)m_Filter).setSecondValueIndex("2");
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the swapping is correct
    int first = 0, second = 1;
    for (int i = 0; i < result.numInstances(); i++) {
      if (m_Instances.instance(i).isMissing(4)) {
        assertTrue("Missing in input should give missing in result:" 
               + m_Instances.instance(i) + " --> "
               + result.instance(i),
               result.instance(i).isMissing(4));
      } else if (m_Instances.instance(i).value(4) == first) {
        assertTrue("Value should be swapped", result.instance(i).value(4) == second);
      } else if (m_Instances.instance(i).value(4) == second) {
        assertTrue("Value should be swapped", result.instance(i).value(4) == first);
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
	  ((SwapValues) m_FilteredClassifier.getFilter()).setAttributeIndex(
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
    return new TestSuite(SwapValuesTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
