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

package weka.filters.unsupervised.instance;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests RemoveWithValues. Run from the command line with:<p>
 * java weka.filters.unsupervised.instance.RemoveWithValuesTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public class RemoveWithValuesTest extends AbstractFilterTest {
  
  public RemoveWithValuesTest(String name) { super(name);  }

  /** Creates a default RemoveWithValues */
  public Filter getFilter() {
    RemoveWithValues f = new RemoveWithValues();
    f.setAttributeIndex("3");
    f.setInvertSelection(true);
    return f;
  }

  public void testString() {
    Instances icopy = new Instances(m_Instances);
    try {
      ((RemoveWithValues)m_Filter).setAttributeIndex("1");
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception selecting on a STRING attribute!");
    } catch (Exception ex) {
      // OK
    }
  }

  public void testNominal() {
    ((RemoveWithValues)m_Filter).setAttributeIndex("2");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals("Default nominal selection matches all values",
                 m_Instances.numInstances(),  result.numInstances());

    try {
      ((RemoveWithValues)m_Filter).setNominalIndices("1-2");
    } catch (Exception ex) {
      fail("Shouldn't ever get here unless Range chamges incompatibly");
    }
    result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertTrue(m_Instances.numInstances() > result.numInstances());

    try {
      ((RemoveWithValues)m_Filter).setNominalIndices("3-last");
    } catch (Exception ex) {
      fail("Shouldn't ever get here unless Range chamges incompatibly");
    }
    Instances result2 = useFilter();
    assertEquals(m_Instances.numAttributes(), result2.numAttributes());
    assertTrue(m_Instances.numInstances() > result2.numInstances());
    assertEquals(m_Instances.numInstances(), result.numInstances() + result2.numInstances());

    ((RemoveWithValues)m_Filter).setInvertSelection(false);
    result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances() + result2.numInstances());
  }

  public void testNumeric() {
    ((RemoveWithValues)m_Filter).setAttributeIndex("3");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals("Default split point matches values less than 0",
                 0,  result.numInstances());

    ((RemoveWithValues)m_Filter).setSplitPoint(3);
    result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertTrue(m_Instances.numInstances() > result.numInstances());

    // Test inversion is working.
    ((RemoveWithValues)m_Filter).setInvertSelection(false);
    Instances result2 = useFilter();
    assertEquals(m_Instances.numAttributes(), result2.numAttributes());
    assertTrue(m_Instances.numInstances() > result2.numInstances());
    assertEquals(m_Instances.numInstances(), result.numInstances() + result2.numInstances());
  }

  public void testMatchMissingValues() {
    ((RemoveWithValues)m_Filter).setAttributeIndex("5");
    ((RemoveWithValues)m_Filter).setInvertSelection(false);
    ((RemoveWithValues)m_Filter).setMatchMissingValues(false);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertTrue(result.numInstances() > 0);
    for (int i = 0; i < result.numInstances(); i++) {
      assertTrue("Should select only instances with missing values",
             result.instance(i).isMissing(4));
    }
  }
  
  /**
   * filter cannot be used in conjunction with the FilteredClassifier, since
   * an instance used in distributionForInstance/classifyInstance might get
   * deleted.
   */
  public void testFilteredClassifier() {
    // nothing
  }

  public static Test suite() {
    return new TestSuite(RemoveWithValuesTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
