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

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests RenameAttribute. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.RenameAttributeTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class RenameAttributeTest 
  extends AbstractFilterTest {

  /**
   * Initializes the test.
   * 
   * @param name	the name of the test
   */
  public RenameAttributeTest(String name) { 
    super(name);  
  }

  /** 
   * Creates a default RenameAttribute.
   * 
   * @return		the default filter
   */
  public Filter getFilter() {
    return new RenameAttribute();
  }

  /** 
   * Creates a specialized RenameAttribute.
   * 
   * @param find	the find expression
   * @param replace	the replace expression
   * @param all		whether to replace all occurrences
   * @param range	the range of attributes
   * @param invert	whether to invert the selection
   * @return		the filter
   */
  public Filter getFilter(String find, String replace, boolean all, String range, boolean invert) {
    RenameAttribute 	result;
    
    result = new RenameAttribute();
    result.setFind(find);
    result.setReplace(replace);
    result.setReplaceAll(all);
    result.setAttributeIndices(range);
    result.setInvertSelection(invert);
    
    return result;
  }

  /**
   * performs the actual test.
   * 
   * @return		the generated data
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

    assertEquals(icopy.numAttributes(), result.numAttributes());
    assertEquals(icopy.numInstances(), m_Instances.numInstances());
    
    return result;
  }

  /**
   * Returns a test suite.
   * 
   * @return		the suite
   */
  public static Test suite() {
    return new TestSuite(RenameAttributeTest.class);
  }

  /**
   * Tests to replace only the first occurrence.
   */
  public void testReplaceFirst() {
    Instances	result;

    m_Filter = getFilter("t", "_", false, "first-last", false);
    result   = performTest();
    
    assertEquals("S_ringAtt1", result.attribute(0).name());
  }

  /**
   * Tests to replace all the occurrences.
   */
  public void testReplaceAll() {
    Instances	result;

    m_Filter = getFilter("t", "_", true, "first-last", false);
    result   = performTest();
    
    assertEquals("S_ringA__1", result.attribute(0).name());
  }

  /**
   * Tests the inverting of the attribute range.
   */
  public void testInvertRange() {
    Instances	result;

    m_Filter = getFilter("t", "_", true, "first", true);
    result   = performTest();
    
    assertTrue("The first attribute contains '_'!", (result.attribute(0).name().indexOf("_") == -1));
  }

  /**
   * Tests the use of capturing groups.
   */
  public void testGroup() {
    Instances	result;
    int		i;

    m_Filter = getFilter("(.+)(Att)(.+)", "$1$3", true, "first-last", false);
    result   = performTest();
    
    for (i = 0; i < result.numAttributes(); i++)
      assertTrue(result.attribute(i).name() + " still contains 'Att'", (result.attribute(i).name().indexOf("Att") == -1));
  }
  
  /**
   * Runs the test from command-line.
   * 
   * @param args	ignored
   */
  public static void main(String[] args){
    TestRunner.run(suite());
  }
}
