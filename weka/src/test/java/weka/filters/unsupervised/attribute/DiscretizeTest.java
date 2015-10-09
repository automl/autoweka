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
 * Tests Discretize. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.DiscretizeTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public class DiscretizeTest extends AbstractFilterTest {
  
  public DiscretizeTest(String name) { super(name);  }

  /** Creates a default Discretize */
  public Filter getFilter() {
    Discretize f= new Discretize();
    return f;
  }

  /** Creates a specialized Discretize */
  public Filter getFilter(String rangelist) {
    
    try {
      Discretize f = new Discretize();
      f.setAttributeIndices(rangelist);
      return f;
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception setting attribute range: " + rangelist 
           + "\n" + ex.getMessage()); 
    }
    return null;
  }

  public void testTypical() {
    m_Filter = getFilter("1,2");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    // None of the attributes should have changed, since 1,2 aren't numeric
    for (int i = 0; i < result.numAttributes(); i++) {
      assertEquals(m_Instances.attribute(i).type(), result.attribute(i).type());
      assertEquals(m_Instances.attribute(i).name(), result.attribute(i).name());
    }
  }

  public void testTypical2() {
    m_Filter = getFilter("3-4");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    for (int i = 0; i < result.numAttributes(); i++) {
      if (i != 2) {
        assertEquals(m_Instances.attribute(i).type(), result.attribute(i).type());
        assertEquals(m_Instances.attribute(i).name(), result.attribute(i).name());
      } else {
        assertEquals(Attribute.NOMINAL, result.attribute(i).type());
        assertEquals(10, result.attribute(i).numValues());
      }
    }
  }

  public void testInverted() {
    m_Filter = getFilter("1,2");
    ((Discretize)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    for (int i = 0; i < result.numAttributes(); i++) {
      if ((i < 2) || !m_Instances.attribute(i).isNumeric()) {
        assertEquals(m_Instances.attribute(i).type(), result.attribute(i).type());
        assertEquals(m_Instances.attribute(i).name(), result.attribute(i).name());
      } else {
        assertEquals(Attribute.NOMINAL, result.attribute(i).type());
        assertEquals(10, result.attribute(i).numValues());
      }
    }
  }

  public void testNonInverted2() {
    m_Filter = getFilter("first-3");
    ((Discretize)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    for (int i = 0; i < result.numAttributes(); i++) {
      if ((i < 3) || !m_Instances.attribute(i).isNumeric()) {
        assertEquals(m_Instances.attribute(i).type(), result.attribute(i).type());
        assertEquals(m_Instances.attribute(i).name(), result.attribute(i).name());
      } else {
        assertEquals(Attribute.NOMINAL, result.attribute(i).type());
        assertEquals(10, result.attribute(i).numValues());
      }
    }
  }

  public void testBins() {
    m_Filter = getFilter("3");
    ((Discretize)m_Filter).setBins(5);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(Attribute.NOMINAL, result.attribute(2).type());
    assertEquals(5, result.attribute(2).numValues());

    ((Discretize)m_Filter).setBins(20);
    result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(Attribute.NOMINAL, result.attribute(2).type());
    assertEquals(20, result.attribute(2).numValues());
  }

  public void testFindNumBins() {
    m_Filter = getFilter("3");
    ((Discretize)m_Filter).setFindNumBins(true);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(Attribute.NOMINAL, result.attribute(2).type());
    assertTrue(5 >= result.attribute(2).numValues());
  }

  public static Test suite() {
    return new TestSuite(DiscretizeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
