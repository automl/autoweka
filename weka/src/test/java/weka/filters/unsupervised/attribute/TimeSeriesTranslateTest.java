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

import weka.core.Instance;
import weka.core.Instances;
import weka.filters.AbstractTimeSeriesFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests TimeSeriesTranslate. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.TimeSeriesTranslateTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public class TimeSeriesTranslateTest 
  extends AbstractTimeSeriesFilterTest {

  public TimeSeriesTranslateTest(String name) { super(name);  }

  /** Creates a default TimeSeriesTranslate */
  public Filter getFilter() {
    return getFilter("2-3");
  }

  /** Creates a specialized TimeSeriesTranslate */
  public Filter getFilter(String rangelist) {
    
    TimeSeriesTranslate af = new TimeSeriesTranslate();
    af.setAttributeIndices(rangelist);
    af.setFillWithMissing(false);
    return af;
  }

  public void testInverted() {
    m_Filter = getFilter("1,4,2-3");
    ((TimeSeriesTranslate)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    // Number of attributes shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances() - 1, result.numInstances());
    // Check conversion looks OK
    for (int i = 0; i < result.numInstances(); i++) {
      Instance in = m_Instances.instance(i + 1);
      Instance out = result.instance(i);
      for (int j = 0; j < result.numAttributes(); j++) {
        if ((j != 4) && (j != 5)&& (j != 6)) {
          if (in.isMissing(j)) {
            assertTrue("Nonselected missing values should pass through",
                   out.isMissing(j));
          } else if (result.attribute(j).isString()) {
            assertEquals("Nonselected attributes shouldn't change. "
                         + in + " --> " + out,
                         m_Instances.attribute(j).value((int)in.value(j)),
                         result.attribute(j).value((int)out.value(j)));
          } else {
            assertEquals("Nonselected attributes shouldn't change. "
                         + in + " --> " + out,
                         in.value(j),
                         out.value(j), TOLERANCE);
          }
        }
      }
    }    
  }


  public static Test suite() {
    return new TestSuite(TimeSeriesTranslateTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
