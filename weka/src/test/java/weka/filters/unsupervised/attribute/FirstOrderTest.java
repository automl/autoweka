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
import weka.core.Instance;
import weka.core.Instances;
import weka.core.TestInstances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests FirstOrder. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.FirstOrderTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public class FirstOrderTest extends AbstractFilterTest {
  
  private static double EXPR_DELTA = 0.001;

  public FirstOrderTest(String name) { super(name);  }

  /** Creates a default FirstOrder */
  public Filter getFilter() {
    return getFilter("6,3");
  }

  /** Creates a specialized FirstOrder */
  public Filter getFilter(String rangelist) {
    
    try {
      FirstOrder af = new FirstOrder();
      af.setAttributeIndices(rangelist);
      return af;
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception setting attribute range: " + rangelist 
           + "\n" + ex.getMessage()); 
    }
    return null;
  }
  
  /**
   * returns data generated for the FilteredClassifier test
   * 
   * @return		the dataset for the FilteredClassifier
   * @throws Exception	if generation of data fails
   */
  protected Instances getFilteredClassifierData() throws Exception{
    TestInstances	test;
    Instances		result;

    test = new TestInstances();
    test.setNumNominal(0);
    test.setNumNumeric(6);
    test.setClassType(Attribute.NOMINAL);
    test.setClassIndex(TestInstances.CLASS_IS_LAST);

    result = test.generate();
    
    return result;
  }

  public void testTypical() {
    m_Filter = getFilter("6,3");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes() - 1, result.numAttributes());
    for (int i = 0; i < result.numInstances(); i++) {
      Instance orig = m_Instances.instance(i);
      if (orig.isMissing(5) || orig.isMissing(2)) {
        assertTrue("Instance " + (i + 1) + " should have been ?" , 
               result.instance(i).isMissing(4));
      } else {
        assertEquals(orig.value(5) - orig.value(2), 
                     result.instance(i).value(4), 
                     EXPR_DELTA);
      }
    }
  }

  public void testTypical2() {
    m_Filter = getFilter("3,6");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes() - 1, result.numAttributes());
    for (int i = 0; i < result.numInstances(); i++) {
      Instance orig = m_Instances.instance(i);
      if (orig.isMissing(5) || orig.isMissing(2)) {
        assertTrue("Instance " + (i + 1) + " should have been ?" , 
               result.instance(i).isMissing(4));
      } else {
        assertEquals(orig.value(5) - orig.value(2), 
                     result.instance(i).value(4), 
                     EXPR_DELTA);
      }
    }
  }

  public static Test suite() {
    return new TestSuite(FirstOrderTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
