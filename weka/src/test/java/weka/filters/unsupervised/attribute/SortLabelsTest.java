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
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import java.util.HashSet;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests SortLabels. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.SortLabelsTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class SortLabelsTest 
  extends AbstractFilterTest {
  
  /**
   * Initializes the test with the given name.
   * 
   * @param name	the name of the test
   */
  public SortLabelsTest(String name) { 
    super(name);  
  }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default filter to test and loads a test set of Instances.
   *
   * @throws Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    m_Instances.deleteAttributeType(Attribute.STRING);
    m_Instances.deleteAttributeType(Attribute.RELATIONAL);
  }

  /**
   * Creates a default SortLabels filter.
   * 
   * @return		the filter
   */
  public Filter getFilter() {
    return new SortLabels();
  }

  /**
   * Creates a specialized SortLabels.
   * 
   * @param sort	the sort type
   * @param range	the sort range
   * @return		the configured filter
   */
  public Filter getFilter(int sort, String range) {
    SortLabels result = new SortLabels();
    result.setSortType(new SelectedTag(SortLabels.SORT_CASESENSITIVE, SortLabels.TAGS_SORTTYPE));
    result.setAttributeIndices(range);
    return result;
  }

  /**
   * performs the actual test.
   * 
   * @return 		the generated instances
   */
  protected Instances performTest() {
    Instances icopy = new Instances(m_Instances);
    Instances result = null;
    try {
      m_Filter.setInputFormat(icopy);
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    try {
      result = Filter.useFilter(icopy, m_Filter);
      assertNotNull(result);
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on useFilter(): \n" + ex.getMessage());
    }

    assertEquals("Number of attributes", icopy.numAttributes(), result.numAttributes());
    assertEquals("Number of instances", icopy.numInstances(), m_Instances.numInstances());
    for (int i = 0; i < result.numAttributes(); i++) {
      // test number of values
      assertEquals("Number of values differ for attribute #" + (i+1), icopy.attribute(i).numValues(), result.attribute(i).numValues());
      // test values
      HashSet<String> valuesOriginal = new HashSet<String>();
      HashSet<String> valuesResult = new HashSet<String>();
      for (int n = 0; n < icopy.attribute(i).numValues(); n++) {
	valuesOriginal.add(icopy.attribute(i).value(n));
	valuesResult.add(result.attribute(i).value(n));
      }
      assertEquals("Values differ for attribute #" + (i+1), valuesOriginal, valuesResult);
    }
    
    return result;
  }

  /**
   * Tests the case-sensitive sorting on all attributes.
   */
  public void testCaseSensitive() {
    m_Filter = getFilter(SortLabels.SORT_CASESENSITIVE, "first-last");
    testBuffered();
    Instances result = performTest();
    String[] sorted = new String[]{"b", "g", "r"};
    for (int i = 0; i < sorted.length; i++)
      assertEquals("Values differ for index #" + (i+1), result.attribute(0).value(i), sorted[i]);
  }

  /**
   * Tests the case-insensitive sorting on all attributes.
   */
  public void testCaseInsensitive() {
    m_Filter = getFilter(SortLabels.SORT_CASEINSENSITIVE, "first-last");
    testBuffered();
    Instances result = performTest();
    String[] sorted = new String[]{"b", "g", "r"};
    for (int i = 0; i < sorted.length; i++)
      assertEquals("Values differ for index #" + (i+1), sorted[i], result.attribute(0).value(i));
  }

  /**
   * Tests that ordering didn't change.
   */
  public void testUnchangedOrder() {
    m_Filter = getFilter(SortLabels.SORT_CASESENSITIVE, "first-last");
    testBuffered();
    Instances result = performTest();
    for (int i = 0; i < m_Instances.attribute(2).numValues(); i++)
      assertEquals("Values differ for index #" + (i+1), m_Instances.attribute(2).value(i), result.attribute(2).value(i));
  }

  /**
   * Returns a test.
   * 
   * @return		the test
   */
  public static Test suite() {
    return new TestSuite(SortLabelsTest.class);
  }

  /**
   * Runs the test from commandline.
   * 
   * @param args	ignored
   */
  public static void main(String[] args){
    TestRunner.run(suite());
  }
}
