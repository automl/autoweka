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
 * Copyright 2002 Eibe Frank
 */

package weka.filters.unsupervised.attribute;

import weka.core.Instances;
import weka.core.Utils;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests Normalize. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.NormalizeTest
 *
 * @author <a href="mailto:len@reeltwo.com">Eibe Frank</a>
 * @version $Revision: 8034 $
 */
public class StandardizeTest extends AbstractFilterTest {
  
  public StandardizeTest(String name) { super(name);  }

  /** Creates an example Standardize */
  public Filter getFilter() {
    Standardize f = new Standardize();
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());

    // Check conversion is OK
    for (int j = 0; j < result.numAttributes(); j++) {
      if (result.attribute(j).isNumeric()) {
	double mean = result.meanOrMode(j);
	assertTrue("Mean should be 0", Utils.eq(mean, 0));
	double stdDev = Math.sqrt(result.variance(j));
	assertTrue("StdDev should be 1 (or 0)", 
		   Utils.eq(stdDev, 0) || Utils.eq(stdDev, 1));
      }
    }
  }


  public static Test suite() {
    return new TestSuite(StandardizeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
