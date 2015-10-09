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
 * Tests ReplaceMissingWithUserConstant. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.ReplaceMissingWithUserConstantTest
 *
 * @author Mark Hall
 * @version $Revision: 9169 $
 */
public class ReplaceMissingWithUserConstantTest extends AbstractFilterTest {
  
  public ReplaceMissingWithUserConstantTest(String name) { super(name);  }

  /** Creates a default ReplaceMissingWithUserConstant */
  public Filter getFilter() {
    ReplaceMissingWithUserConstant filter = new ReplaceMissingWithUserConstant();
    filter.setDateReplacementValue("1969-08-28");
    filter.setDateFormat("yyyy-MM-dd");
    return filter;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    for (int j = 0; j < m_Instances.numAttributes(); j++) {
      for (int i = 0; i < m_Instances.numInstances(); i++) {
        assertTrue("All missing values should have been replaced " + result.instance(i),
                   !result.instance(i).isMissing(j));
      }
    }
  }

  public static Test suite() {
    return new TestSuite(ReplaceMissingWithUserConstantTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
