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
 * Tests Remove. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.RemoveTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public class RemoveTest extends AbstractFilterTest {
  
  public RemoveTest(String name) { super(name);  }

  /** Creates a default Remove */
  public Filter getFilter() {
    return getFilter("1-3");
  }

  /** Creates a specialized Remove */
  public Filter getFilter(String rangelist) {
    
    Remove af = new Remove();
    af.setAttributeIndices(rangelist);
    return af;
  }

  public void testTypical() {
    m_Filter = getFilter("1,2");
    ((Remove)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    assertEquals(2, result.numAttributes());
    assertEquals(m_Instances.attribute(0).name(), result.attribute(0).name());
    assertEquals(m_Instances.attribute(1).name(), result.attribute(1).name());
  }

  public void testTypical2() {
    m_Filter = getFilter("3-4");
    ((Remove)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    assertEquals(2, result.numAttributes());
    assertEquals(m_Instances.attribute(2).name(), result.attribute(0).name());
    assertEquals(m_Instances.attribute(3).name(), result.attribute(1).name());
  }

  public void testNonInverted() {
    m_Filter = getFilter("1,2");
    ((Remove)m_Filter).setInvertSelection(false);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes() - 2, result.numAttributes());
    assertEquals(m_Instances.attribute(2).name(), result.attribute(0).name());
    assertEquals(m_Instances.attribute(3).name(), result.attribute(1).name());
  }

  public void testNonInverted2() {
    m_Filter = getFilter("first-3");
    ((Remove)m_Filter).setInvertSelection(false);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes() - 3, result.numAttributes());
    assertEquals(m_Instances.attribute(3).name(), result.attribute(0).name());
  }

  public static Test suite() {
    return new TestSuite(RemoveTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
