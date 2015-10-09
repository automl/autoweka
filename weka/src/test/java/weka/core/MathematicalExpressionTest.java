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
 * Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import java.util.HashMap;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests MathematicalExpression. Run from the command line with:<p/>
 * java weka.core.MathematicalTest
 *
 * @author mhall (mhall{[at]}pentaho{[dot]}org)
 * @version $Revision: 8034 $
 */
public class MathematicalExpressionTest 
  extends TestCase {

  /**
   * Constructs the <code>MathematicalExpresionTest</code>.
   *
   * @param name the name of the test class
   */
  public MathematicalExpressionTest(String name) { 
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

  public static Test suite() {
    return new TestSuite(MathematicalExpressionTest.class);
  }

  protected double getExpressionResult(String expression) throws Exception {
    HashMap symbols = new HashMap();
    symbols.put("A", new Double(4));
    symbols.put("B", new Double(2));
    symbols.put("C", new Double(2));
    return MathematicalExpression.evaluate(expression, symbols);
  }

  public void testAddSub() throws Exception {
    double result = getExpressionResult("A-B+C");
    assertEquals(4.0, result);
  }

  public void testOperatorOrder() throws Exception {
    double result = getExpressionResult("A-B*C");
    assertEquals(0.0, result);
  }

  public void testBrackets() throws Exception {
    double result = getExpressionResult("(A-B)*C");
    assertEquals(4.0, result);
  }

  public void testExpressionWithConstants() throws Exception {
    double result = getExpressionResult("A-B*(C+5)");
    assertEquals(-10.0, result);
  }

  public void testExpressionWithFunction() throws Exception {
    double result = getExpressionResult("pow(A,B*1)-C*2");
    assertEquals(12.0, result);
  } 

  public void testExpressionWithIFELSE() throws Exception {
    double result = getExpressionResult("ifelse((C<1000|C>5000),(A+B),C+C)");
    assertEquals(6.0, result);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}