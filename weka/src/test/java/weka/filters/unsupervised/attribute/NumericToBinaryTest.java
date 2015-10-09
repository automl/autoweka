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
 * Tests NumericToBinary. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.NumericToBinaryTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public class NumericToBinaryTest extends AbstractFilterTest {
  
  public NumericToBinaryTest(String name) { super(name);  }

  /** Creates an example NumericToBinary */
  public Filter getFilter() {
    NumericToBinary f = new NumericToBinary();
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());

    // Check conversion is OK
    for (int j = 0; j < result.numAttributes(); j++) {
      if (m_Instances.attribute(j).isNumeric()) {
        assertTrue("Numeric attribute should now be nominal",
               result.attribute(j).isNominal());
        for (int i = 0; i < result.numInstances(); i++) {
          if (m_Instances.instance(i).isMissing(j)) {
            assertTrue(result.instance(i).isMissing(j));
          } else if (m_Instances.instance(i).value(j) == 0) {
            assertTrue("Output value should be 0", 
                   result.instance(i).value(j) == 0);
          } else {
            assertTrue("Output value should be 1", 
                   result.instance(i).value(j) == 1);
          }
        }
      }
    }
  }


  public static Test suite() {
    return new TestSuite(NumericToBinaryTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
