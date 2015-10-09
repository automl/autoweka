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
 * Tests NumericTransform. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.NumericTransformTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public class NumericTransformTest extends AbstractFilterTest {

  /** Tolerance allowed in double comparisons */
  private static final double TOLERANCE = 0.001;

  public NumericTransformTest(String name) { super(name);  }

  /** Creates a default NumericTransform */
  public Filter getFilter() {
    return getFilter("first-last");
  }

  /** Creates a specialized NumericTransform */
  public Filter getFilter(String rangelist) {
    
    try {
      NumericTransform af = new NumericTransform();
      af.setAttributeIndices(rangelist);
      return af;
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception setting attribute range: " + rangelist 
           + "\n" + ex.getMessage()); 
    }
    return null;
  }

  public void testDefault() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // Check conversion is OK
    for (int i = 0; i < result.numInstances(); i++) {
      for (int j = 0; j < result.numAttributes(); j++) {
        if (m_Instances.instance(i).isMissing(j)) {
          assertTrue(result.instance(i).isMissing(j));
        } else if (result.attribute(j).isNumeric()) {
          assertEquals("Value should be same as Math.abs()",
                       Math.abs(m_Instances.instance(i).value(j)),
                       result.instance(i).value(j), TOLERANCE);
        } else {
          assertEquals("Value shouldn't have changed",
                       m_Instances.instance(i).value(j),
                       result.instance(i).value(j), TOLERANCE);
        }
      }
    }    
  }

  public void testInverted() {
    m_Filter = getFilter("1-3");
    ((NumericTransform)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // Check conversion is OK
    for (int i = 0; i < result.numInstances(); i++) {
      for (int j = 0; j < result.numAttributes(); j++) {
        if (m_Instances.instance(i).isMissing(j)) {
          assertTrue(result.instance(i).isMissing(j));
        } else if (result.attribute(j).isNumeric() && (j >=3)) {
          assertEquals("Value should be same as Math.abs()",
                       Math.abs(m_Instances.instance(i).value(j)),
                       result.instance(i).value(j), TOLERANCE);
        } else {
          assertEquals("Value shouldn't have changed",
                       m_Instances.instance(i).value(j),
                       result.instance(i).value(j), TOLERANCE);
        }
      }
    }
  }

  public void testClassNameAndMethodName() throws Exception {
    ((NumericTransform)m_Filter).setClassName("java.lang.Math");
    ((NumericTransform)m_Filter).setMethodName("rint");
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // Check conversion is OK
    for (int i = 0; i < result.numInstances(); i++) {
      for (int j = 0; j < result.numAttributes(); j++) {
        if (m_Instances.instance(i).isMissing(j)) {
          assertTrue(result.instance(i).isMissing(j));
        } else if (result.attribute(j).isNumeric()) {
          assertEquals("Value should be same as Math.rint()",
                       Math.rint(m_Instances.instance(i).value(j)),
                       result.instance(i).value(j), TOLERANCE);
        } else {
          assertEquals("Value shouldn't have changed",
                       m_Instances.instance(i).value(j),
                       result.instance(i).value(j), TOLERANCE);
        }
      }
    }
  }

  public static Test suite() {
    return new TestSuite(NumericTransformTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
