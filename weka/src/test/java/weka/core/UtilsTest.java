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
 * Copyright (C) 2007 University of Waikato 
 */

package weka.core;

import weka.filters.unsupervised.attribute.StringToWordVector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests Utils. Run from the command line with:<p/>
 * java weka.core.UtilsTest
 *
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class UtilsTest 
  extends TestCase {
  
  /**
   * Constructs the <code>UtilsTest</code>.
   *
   * @param name 	the name of the test class
   */
  public UtilsTest(String name) { 
    super(name); 
  }
  
  /**
   * Called by JUnit before each test method.
   *
   * @throws Exception 	if an error occurs
   */
  protected void setUp() throws Exception {
    super.setUp();
  }

  /**
   * Called by JUnit after each test method
   * 
   * @throws Exception 	if an error occurs
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * tests splitOptions and joinOptions
   * 
   * @see Utils#splitOptions(String)
   * @see Utils#joinOptions(String[])
   */
  public void testSplittingAndJoining() {
    String[] 	options;
    String[] 	newOptions;
    String 	joined;
    int		i;
    
    try {
      options    = new StringToWordVector().getOptions();
      joined     = Utils.joinOptions(options);
      newOptions = Utils.splitOptions(joined);
      assertEquals("Same number of options", options.length, newOptions.length);
      for (i = 0; i < options.length; i++) {
	if (!options[i].equals(newOptions[i]))
	  fail("Option " + (i+1) + " differs");
      }
    }
    catch (Exception e) {
      fail("Exception: " + e);
    }
  }
  
  /**
   * tests quote and unquote
   * 
   * @see Utils#quote(String)
   * @see Utils#unquote(String)
   */
  public void testQuoting() {
    String 	input;
    String 	output;
    
    input  = "blahblah";
    output = Utils.quote(input);
    assertTrue("No quoting necessary", !output.startsWith("'") && !output.endsWith("'"));
    
    input  = "";
    output = Utils.quote(input);
    assertTrue("Empty string quoted", output.startsWith("'") && output.endsWith("'"));
    assertTrue("Empty string restored", input.equals(Utils.unquote(output)));
    
    input  = " ";
    output = Utils.quote(input);
    assertTrue("Blank quoted", output.startsWith("'") && output.endsWith("'"));
    assertTrue("Blank restored", input.equals(Utils.unquote(output)));
    
    input  = "{";
    output = Utils.quote(input);
    assertTrue(">" + input + "< quoted", output.startsWith("'") && output.endsWith("'"));
    assertTrue(">" + input + "< restored", input.equals(Utils.unquote(output)));
    
    input  = "}";
    output = Utils.quote(input);
    assertTrue(">" + input + "< quoted", output.startsWith("'") && output.endsWith("'"));
    assertTrue(">" + input + "< restored", input.equals(Utils.unquote(output)));
    
    input  = ",";
    output = Utils.quote(input);
    assertTrue(">" + input + "< quoted", output.startsWith("'") && output.endsWith("'"));
    assertTrue(">" + input + "< restored", input.equals(Utils.unquote(output)));
    
    input  = "?";
    output = Utils.quote(input);
    assertTrue(">" + input + "< quoted", output.startsWith("'") && output.endsWith("'"));
    assertTrue(">" + input + "< restored", input.equals(Utils.unquote(output)));
    
    input  = "\r\n\t'\"%";
    output = Utils.quote(input);
    assertTrue(">" + input + "< quoted", output.startsWith("'") && output.endsWith("'"));
    assertTrue(">" + input + "< restored", input.equals(Utils.unquote(output)));
  }
  
  /**
   * tests backQuoteChars and unbackQuoteChars
   * 
   * @see Utils#backQuoteChars(String)
   * @see Utils#unbackQuoteChars(String)
   */
  public void testBackQuoting() {
    String 	input;
    String 	output;
    
    input  = "blahblah";
    output = Utils.backQuoteChars(input);
    assertTrue("No backquoting necessary", input.equals(output));
    
    input  = "\r\n\t'\"%";
    output = Utils.backQuoteChars(input);
    assertTrue(">" + input + "< restored", input.equals(Utils.unbackQuoteChars(output)));
    
    input  = "\\r\\n\\t\\'\\\"\\%";
    output = Utils.backQuoteChars(input);
    assertTrue(">" + input + "< restored", input.equals(Utils.unbackQuoteChars(output)));
    
    input  = Utils.joinOptions(new StringToWordVector().getOptions());
    output = Utils.backQuoteChars(input);
    assertTrue(">" + input + "< restored", input.equals(Utils.unbackQuoteChars(output)));
  }
  
  public static Test suite() {
    return new TestSuite(UtilsTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
