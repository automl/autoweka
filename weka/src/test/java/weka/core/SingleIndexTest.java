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
 * Tests SingleIndex. Run from the command line with:<p/>
 * java weka.core.SingleIndexTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class SingleIndexTest 
  extends TestCase {
  
  /**
   * Constructs the <code>SingleIndexTest</code>.
   *
   * @param name the name of the test class
   */
  public SingleIndexTest(String name) { 
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
   * returns a configured SingleIndex
   *
   * @param initial     the initial string, if null the default constructor
   *                    is used (and "1" is set - otherwise setUpper doesn't
   *                    work!)
   * @param upper       the upper limit
   */
  protected SingleIndex getIndex(String initial, int upper) {
    SingleIndex   result; 

    if (initial == null) {
      result = new SingleIndex();
      result.setSingleIndex("1");
      result.setUpper(upper);
    }
    else {
      result = new SingleIndex(initial);
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
    SingleIndex index = new SingleIndex();
    index.setSingleIndex(indexStr);
    index.setUpper(upper);

    assertEquals(indexStr, index.getSingleIndex());
    assertEquals(indexInt, index.getIndex());
  }

  /**
   * tests the constructor with initial value
   */
  public void testInitialValueConstructor() throws Exception {
    int upper = 10;
    int indexInt = 0;
    String indexStr = "" + (indexInt + 1);
    SingleIndex index = getIndex("1", upper);

    assertEquals(indexStr, index.getSingleIndex());
    assertEquals(indexInt, index.getIndex());
  }

  /**
   * tests whether "first" is interpreted correctly
   */
  public void testFirst() throws Exception {
    int upper = 10;
    SingleIndex index = getIndex("first", upper);

    assertEquals(0, index.getIndex());
    assertEquals("first", index.getSingleIndex());
  }

  /**
   * tests whether "last" is interpreted correctly
   */
  public void testLast() throws Exception {
    int upper = 10;
    SingleIndex index = getIndex("last", upper);

    assertEquals(upper, index.getIndex());
    assertEquals("last", index.getSingleIndex());
  }

  public static Test suite() {
    return new TestSuite(SingleIndexTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
