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

import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests MakeIndicator. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.MakeIndicatorTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public class MakeIndicatorTest extends AbstractFilterTest {
  
  public MakeIndicatorTest(String name) { super(name);  }

  /** Creates an example MakeIndicator */
  public Filter getFilter() {
    MakeIndicator f = new MakeIndicator();
    // Ensure the filter we return can run on the test dataset
    f.setAttributeIndex("2"); 
    return f;
  }


  public void testInvalidAttributeTypes() {
    Instances icopy = new Instances(m_Instances);
    try {
      ((MakeIndicator)m_Filter).setAttributeIndex("1");
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception selecting a STRING attribute!");
    } catch (Exception ex) {
      // OK
    }
    try {
      ((MakeIndicator)m_Filter).setAttributeIndex("3");
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception indicating a NUMERIC attribute!");
    } catch (Exception ex) {
      // OK
    }
  }

  public void testDefault() {
    ((MakeIndicator)m_Filter).setAttributeIndex("2");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that default attribute type is numeric
    assertEquals("Default attribute encoding should be NUMERIC",
                 Attribute.NUMERIC, result.attribute(1).type());
    // Check that default indication is correct
    for (int i = 0; i < result.numInstances(); i++) {
      assertTrue("Checking indicator for instance: " + (i + 1),
             (m_Instances.instance(i).value(1) == 2) ==
             (result.instance(i).value(1) == 1));
    }
  }

  public void testNominalEncoding() {
    ((MakeIndicator)m_Filter).setAttributeIndex("2");
    ((MakeIndicator)m_Filter).setNumeric(false);    
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that default attribute type is numeric
    assertEquals("New attribute encoding should be NOMINAL",
                 Attribute.NOMINAL, result.attribute(1).type());
    // Check that default indication is correct
    for (int i = 0; i < result.numInstances(); i++) {
      assertTrue("Checking indicator for instance: " + (i + 1),
             (m_Instances.instance(i).value(1) == 2) ==
             (result.instance(i).value(1) == 1));
    }
  }

  public void testMultiValueIndication() {
    ((MakeIndicator)m_Filter).setAttributeIndex("2");
    try {
      ((MakeIndicator)m_Filter).setValueIndices("1,3");
    } catch (Exception ex) {
      fail("Is Range broken?");
    }
    ((MakeIndicator)m_Filter).setNumeric(false);    
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that default attribute type is numeric
    assertEquals("New attribute encoding should be NOMINAL",
                 Attribute.NOMINAL, result.attribute(1).type());
    // Check that default indication is correct
    for (int i = 0; i < result.numInstances(); i++) {
      assertTrue("Checking indicator for instance: " + (i + 1),
             ((m_Instances.instance(i).value(1) == 0) ||
              (m_Instances.instance(i).value(1) == 2)) 
             ==
             (result.instance(i).value(1) == 1));
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
	  ((MakeIndicator) m_FilteredClassifier.getFilter()).setAttributeIndex(
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
    return new TestSuite(MakeIndicatorTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
