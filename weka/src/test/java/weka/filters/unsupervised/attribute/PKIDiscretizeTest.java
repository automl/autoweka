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
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests PKIDiscretize. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.PKIDiscretizeTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class PKIDiscretizeTest 
  extends AbstractFilterTest {

  /** the attribute to discretize */
  protected int m_AttIndex;
  
  public PKIDiscretizeTest(String name) { 
    super(name);  
  }

  /** Need to remove non-nominal attributes, set class index */
  protected void setUp() throws Exception {
    super.setUp();

    m_Instances.setClassIndex(1);
    m_AttIndex = 2;
  }
  
  /** Creates a default PKIDiscretize */
  public Filter getFilter() {
    PKIDiscretize f = new PKIDiscretize();
    f.setAttributeIndicesArray(new int[]{m_AttIndex});
    return f;
  }

  public void testTypical() {
    m_Filter = getFilter();
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // the discretized attribute must be nominal
    assertTrue(result.attribute(m_AttIndex).isNominal());
  }

  public static Test suite() {
    return new TestSuite(PKIDiscretizeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
