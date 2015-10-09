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
 * Copyright (C) 2006 University of Waikato 
 */

package weka.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests Range. Run from the command line with:<p/>
 * java weka.core.RangeTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class RangeTest 
  extends TestCase {
  
  /**
   * Constructs the <code>RangeTest</code>.
   *
   * @param name the name of the test class
   */
  public RangeTest(String name) { 
    super(name); 
  }
  
  /**
   * Called by JUnit before each test method.
   *
   * @throws Exception if an error occurs
   */
  protected void setUp() throws Exception {
    super.setUp();
  }

  /** Called by JUnit after each test method */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * returns a configured Range
   *
   * @param initial     the initial string, if null the default constructor
   *                    is used (and "1" is set - otherwise setUpper doesn't
   *                    work!)
   * @param upper       the upper limit
   */
  protected Range getRange(String initial, int upper) {
    Range   result; 

    if (initial == null) {
      result = new Range();
      result.setRanges("1");
      result.setUpper(upper);
    }
    else {
      result = new Range(initial);
      result.setUpper(upper);
    }

    return result;
  }

  /**
   * test the default constructor
   */
  public void testDefaultConstructor() throws Exception {
    int upper = 10;
    int indexInt = 0;
    String indexStr = "" + (indexInt + 1);
    Range index = new Range();
    index.setRanges(indexStr);
    index.setUpper(upper);

    assertEquals(indexStr, index.getRanges());
    assertEquals(1, index.getSelection().length);
    assertEquals(indexInt, index.getSelection()[0]);
  }

  /**
   * tests the constructor with initial value
   */
  public void testInitialValueConstructor() throws Exception {
    int upper = 10;
    int indexInt = 0;
    String indexStr = "" + (indexInt + 1);
    Range index = getRange("1", upper);

    assertEquals(indexStr, index.getRanges());
    assertEquals(1, index.getSelection().length);
    assertEquals(indexInt, index.getSelection()[0]);
  }

  /**
   * tests whether "first" is interpreted correctly
   */
  public void testFirst() throws Exception {
    int upper = 10;
    Range index = getRange("first", upper);

    assertEquals("first", index.getRanges());
    assertEquals(1, index.getSelection().length);
    assertEquals(0, index.getSelection()[0]);
  }

  /**
   * tests whether "last" is interpreted correctly
   */
  public void testLast() throws Exception {
    int upper = 10;
    Range index = getRange("last", upper);

    assertEquals("last", index.getRanges());
    assertEquals(1, index.getSelection().length);
    assertEquals(upper, index.getSelection()[0]);
  }

  /**
   * tests whether "first-last" is interpreted correctly
   */
  public void testFirstLast() throws Exception {
    int upper = 10;
    Range index = getRange("first-last", upper);

    assertEquals("first-last", index.getRanges());
    assertEquals(upper + 1, index.getSelection().length);
  }

  /**
   * tests whether a simple "range" is interpreted correctly
   */
  public void testSimpleRange() throws Exception {
    int upper = 10;
    String range = "1-3";
    Range index = getRange(range, upper);
    int[] expected = new int[]{0,1,2};

    assertEquals(range, index.getRanges());
    assertEquals(expected.length, index.getSelection().length);
    for (int i = 0; i < expected.length; i++)
      assertEquals(expected[i], index.getSelection()[i]);
  }

  /**
   * tests whether a mixed "range" is interpreted correctly
   */
  public void testMixedRange() throws Exception {
    int upper = 10;
    String range = "1-3,6,8-last";
    Range index = getRange(range, upper);
    int[] expected = new int[]{0,1,2,5,7,8,9,10};

    assertEquals(range, index.getRanges());
    assertEquals(expected.length, index.getSelection().length);
    for (int i = 0; i < expected.length; i++)
      assertEquals(expected[i], index.getSelection()[i]);
  }

  /**
   * tests whether an unordered "range" is interpreted correctly
   */
  public void testUnorderedRange() throws Exception {
    int upper = 10;
    String range = "8-last,1-3,6";
    Range index = getRange(range, upper);
    int[] expected = new int[]{7,8,9,10,0,1,2,5};

    assertEquals(range, index.getRanges());
    assertEquals(expected.length, index.getSelection().length);
    for (int i = 0; i < expected.length; i++)
      assertEquals(expected[i], index.getSelection()[i]);
  }

  public static Test suite() {
    return new TestSuite(RangeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
