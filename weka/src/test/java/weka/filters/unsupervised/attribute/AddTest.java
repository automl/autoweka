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
import weka.core.SelectedTag;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests Add. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.AddTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public class AddTest extends AbstractFilterTest {
  
  public AddTest(String name) { super(name);  }

  /** Creates a default Add */
  public Filter getFilter() {
    return new Add();
  }

  /** Creates a specialized Add */
  public Filter getFilter(int pos) {
    Add af = new Add();
    af.setAttributeIndex("" + (pos + 1));
    return af;
  }

  public void testAddFirst() {
    m_Filter = getFilter(0);
    testBuffered();
  }

  public void testAddLast() {
    m_Filter = getFilter(m_Instances.numAttributes() - 1);
    testBuffered();
  }

  /**
   * Checks the generated attribute type.
   */
  protected void testType(int attType) {
    Instances icopy = new Instances(m_Instances);
    Instances result = null;
    try {
      m_Filter.setInputFormat(icopy);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    try {
      result = Filter.useFilter(icopy, m_Filter);
      assertNotNull(result);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on useFilter(): \n" + ex.getMessage());
    }
    assertEquals(attType, result.attribute(result.numAttributes() - 1).type());
  }
  
  public void testAddNominal() {
    m_Filter = getFilter();
    ((Add)m_Filter).setNominalLabels("hello,there,bob");
    testBuffered();
    testType(Attribute.NOMINAL);
  }

  public void testAddString() {
    m_Filter = getFilter();
    ((Add) m_Filter).setAttributeType(new SelectedTag(Attribute.STRING, Add.TAGS_TYPE));
    testBuffered();
    testType(Attribute.STRING);
  }

  public void testAddDate() {
    m_Filter = getFilter();
    ((Add) m_Filter).setAttributeType(new SelectedTag(Attribute.DATE, Add.TAGS_TYPE));
    testBuffered();
    testType(Attribute.DATE);
  }

  public static Test suite() {
    return new TestSuite(AddTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
