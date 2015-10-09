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

import weka.gui.GenericPropertiesCreator;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests OptionHandlers. Run from the command line with:<p/>
 * java weka.core.OptionHandlerTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class OptionHandlersTest 
  extends TestCase {

  /**
   * tests a specific OptionHandler
   */
  public static class OptionHandlerTest
    extends TestCase {
  
    /** the class to test */
    protected String m_Classname;
    
    /** the OptionHandler tester */
    protected CheckOptionHandler m_OptionTester;
    
    /**
     * Constructs the <code>OptionHandlersTest</code>.
     *
     * @param name the name of the test class
     * @param classname the actual classname
     */
    public OptionHandlerTest(String name, String classname) { 
      super(name); 
      
      m_Classname = classname;
    }
    
    /**
     * returns the classname this test is for
     * 
     * @return		the classname
     */
    public String getClassname() {
      return m_Classname;
    }
    
    /**
     * configures the optionhandler
     * 
     * @return		the configured optionhandler, null in case of an error
     */
    protected OptionHandler getOptionHandler() {
      OptionHandler	result;
      
      try {
	result = (OptionHandler) Class.forName(m_Classname).newInstance();
      }
      catch (Exception e) {
	result = null;
      }
      
      return result;
    }
    
    /**
     * Called by JUnit before each test method.
     *
     * @throws Exception if an error occurs
     */
    protected void setUp() throws Exception {
      super.setUp();
      
      m_OptionTester = new CheckOptionHandler();
      m_OptionTester.setOptionHandler(getOptionHandler());
      m_OptionTester.setUserOptions(new String[0]);
      m_OptionTester.setSilent(true);
    }

    /** 
     * Called by JUnit after each test method
     *
     * @throws Exception if an error occurs
     */
    protected void tearDown() throws Exception {
      super.tearDown();
      
      m_OptionTester = null;
    }
    
    /**
     * tests the listing of the options
     * 
     * @throws Exception if test fails
     */
    public void testListOptions() throws Exception {
      if (m_OptionTester.getOptionHandler() != null) {
        if (!m_OptionTester.checkListOptions())
  	fail(getClassname() + ": " + "Options cannot be listed via listOptions.");
      }
    }
    
    /**
     * tests the setting of the options
     * 
     * @throws Exception if test fails
     */
    public void testSetOptions() throws Exception {
      if (m_OptionTester.getOptionHandler() != null) {
        if (!m_OptionTester.checkSetOptions())
  	fail(getClassname() + ": " + "setOptions method failed.");
      }
    }
    
    /**
     * tests whether there are any remaining options
     * 
     * @throws Exception if test fails
     */
    public void testRemainingOptions() throws Exception {
      if (m_OptionTester.getOptionHandler() != null) {
        if (!m_OptionTester.checkRemainingOptions())
  	fail(getClassname() + ": " + "There were 'left-over' options.");
      }
    }
    
    /**
     * tests the whether the user-supplied options stay the same after setting.
     * getting, and re-setting again.
     * 
     * @see 	#m_OptionTester
     * @throws Exception if test fails
     */
    public void testCanonicalUserOptions() throws Exception {
      if (m_OptionTester.getOptionHandler() != null) {
        if (!m_OptionTester.checkCanonicalUserOptions())
  	fail(getClassname() + ": " + "setOptions method failed");
      }
    }
    
    /**
     * tests the resetting of the options to the default ones
     * 
     * @throws Exception if test fails
     */
    public void testResettingOptions() throws Exception {
      if (m_OptionTester.getOptionHandler() != null) {
        if (!m_OptionTester.checkSetOptions())
  	fail(getClassname() + ": " + "Resetting of options failed");
      }
    }
  }
  
  /**
   * Constructs the <code>OptionHandlersTest</code>.
   *
   * @param name the name of the test class
   */
  public OptionHandlersTest(String name) { 
    super(name); 
  }

  /**
   * dummy for JUnit, does nothing, only to prevent JUnit from complaining 
   * about "no tests"
   * 
   * @throws Exception never happens
   */
  public void testDummy() throws Exception {
    // does nothing, only to prevent JUnit from complaining about "no tests"
  }
  
  /**
   * generate all tests
   * 
   * @return		all the tests
   */
  public static Test suite() {
    TestSuite suite = new TestSuite();
    
    try {
      // determine all test methods in the OptionHandlerTest class
      Vector<String> testMethods = new Vector<String>();
      Method[] methods = OptionHandlerTest.class.getDeclaredMethods();
      for (int i = 0; i < methods.length; i++) {
	if (methods[i].getName().startsWith("test"))
	  testMethods.add(methods[i].getName());
      }
      
      // get all classes that are accessible through the GUI
      GenericPropertiesCreator creator = new GenericPropertiesCreator();
      creator.execute(false);
      Properties props = creator.getOutputProperties();
      
      // traverse all super-classes
      Enumeration names = props.propertyNames();
      while (names.hasMoreElements()) {
	String name = names.nextElement().toString();

	// add tests for all listed classes
	StringTokenizer tok = new StringTokenizer(props.getProperty(name, ""), ",");
	while (tok.hasMoreTokens()) {
	  String classname = tok.nextToken();
	  
	  // does class implement OptionHandler?
	  try {
	    Class cls = Class.forName(classname);
	    if (!ClassDiscovery.hasInterface(OptionHandler.class, cls))
	      continue;
	  }
	  catch (Exception e) {
	    // some other problem, skip this class
	    continue;
	  }
	  
	  // add tests for this class
	  for (int i = 0; i < testMethods.size(); i++)
	    suite.addTest(new OptionHandlerTest(testMethods.get(i), classname));
	}
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return suite;
  }

  /**
   * for running the tests from commandline
   * 
   * @param args	the commandline arguments - ignored
   */
  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
