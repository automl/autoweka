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

package weka.filters;

import weka.core.Instance;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.TimeSeriesTranslate;
import weka.filters.unsupervised.attribute.TimeSeriesTranslateTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests TimeSeriesTranslateFilter. Run from the command line with:<p>
 * java weka.filters.TimeSeriesTranslateFilterTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public abstract class AbstractTimeSeriesFilterTest extends AbstractFilterTest {

  /** Tolerance allowed in double comparisons */
  protected static final double TOLERANCE = 0.001;

  public AbstractTimeSeriesFilterTest(String name) { super(name);  }

  /** Creates a default TimeSeriesTranslateFilter */
  public abstract Filter getFilter();

  public void testDefault() {
    testInstanceRange_X(((TimeSeriesTranslate)m_Filter).getInstanceRange());
  }

  public void testInstanceRange() {

    testInstanceRange_X(-5);
    testInstanceRange_X(-2);
    testInstanceRange_X(2);
    testInstanceRange_X(5);
  }

  public void testFillWithMissing() {

    ((TimeSeriesTranslate)m_Filter).setFillWithMissing(true);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // Check conversion looks OK
    for (int i = 0; i < result.numInstances(); i++) {
      Instance in = m_Instances.instance(i);
      Instance out = result.instance(i);
      for (int j = 0; j < result.numAttributes(); j++) {
        if ((j != 1) && (j != 2)) {
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

  private void testInstanceRange_X(int range) {
    ((TimeSeriesTranslate)m_Filter).setInstanceRange(range);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances() - Math.abs(range), result.numInstances());
    // Check conversion looks OK
    for (int i = 0; i < result.numInstances(); i++) {
      Instance in = m_Instances.instance(i - ((range > 0) ? 0 : range));
      Instance out = result.instance(i);
      for (int j = 0; j < result.numAttributes(); j++) {
        if ((j != 1) && (j != 2)) {
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
  
  /**
   * tests the filter in conjunction with the FilteredClassifier
   */
  public void testFilteredClassifier() {
    try {
      Instances data = getFilteredClassifierData();

      for (int i = 0; i < data.numAttributes(); i++) {
	if (data.classIndex() == i)
	  continue;
	if (data.attribute(i).isNumeric()) {
	  ((TimeSeriesTranslate) m_FilteredClassifier.getFilter()).setAttributeIndices("" + (i + 1));
	  ((TimeSeriesTranslate) m_FilteredClassifier.getFilter()).setFillWithMissing(true);
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
    return new TestSuite(TimeSeriesTranslateTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
