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
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 */

package weka.datagenerators;

import java.io.PrintWriter;
import java.io.StringWriter;

import junit.framework.TestCase;
import weka.core.CheckGOE;
import weka.core.CheckOptionHandler;
import weka.core.SerializationHelper;

/**
 * Abstract Test class for DataGenerators.
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10160 $
 */
public abstract class AbstractDataGeneratorTest extends TestCase {

  /** The datagenerator to be tested */
  protected DataGenerator m_Generator;

  /** for storing the result */
  protected StringWriter m_Output;

  /** the OptionHandler tester */
  protected CheckOptionHandler m_OptionTester;

  /** for testing GOE stuff */
  protected CheckGOE m_GOETester;

  /**
   * Constructs the <code>AbstractDataGeneratorTest</code>. Called by
   * subclasses.
   * 
   * @param name the name of the test class
   */
  public AbstractDataGeneratorTest(String name) {
    super(name);
  }

  /**
   * Called by JUnit before each test method. This implementation creates the
   * default datagenerator to test.
   * 
   * @throws Exception if an error occurs
   */
  @Override
  protected void setUp() throws Exception {
    m_Generator = getGenerator();
    m_Output = new StringWriter();
    m_Generator.setOutput(new PrintWriter(m_Output));
    m_OptionTester = getOptionTester();
    m_GOETester = getGOETester();
  }

  /** Called by JUnit after each test method */
  @Override
  protected void tearDown() {
    m_Generator = null;
    m_Output = null;
    m_GOETester = null;
    m_OptionTester = null;
  }

  /**
   * Used to create an instance of a specific DataGenerator.
   * 
   * @return a suitably configured <code>DataGenerator</code> value
   */
  public abstract DataGenerator getGenerator();

  /**
   * Configures the CheckOptionHandler uses for testing the optionhandling. Sets
   * the scheme to test.
   * 
   * @return the fully configured CheckOptionHandler
   */
  protected CheckOptionHandler getOptionTester() {
    CheckOptionHandler result;

    result = new CheckOptionHandler();
    result.setOptionHandler(getGenerator());
    result.setUserOptions(new String[0]);
    result.setSilent(true);

    return result;
  }

  /**
   * Configures the CheckGOE used for testing GOE stuff. Sets the Generator
   * returned from the getGenerator() method.
   * 
   * @return the fully configured CheckGOE
   * @see #getGenerator()
   */
  protected CheckGOE getGOETester() {
    CheckGOE result;

    result = new CheckGOE();
    result.setObject(getGenerator());
    result.setIgnoredProperties(result.getIgnoredProperties()
      + ",datasetFormat");
    result.setSilent(true);

    return result;
  }

  /**
   * tests whether setting the options returned by getOptions() works
   */
  public void testOptions() {
    try {
      m_Generator.setOptions(m_Generator.getOptions());
    } catch (Exception e) {
      fail("setOptions(getOptions()) does not work: " + e.getMessage());
    }
  }

  /**
   * tests whether data can be generated with the default options
   */
  public void testMakeData() {
    try {
      DataGenerator.makeData(m_Generator, new String[0]);
    } catch (Exception e) {
      fail("Generation of data failed: " + e.getMessage());
    }
  }

  /**
   * tests whether the scheme declares a serialVersionUID.
   */
  public void testSerialVersionUID() {
    if (SerializationHelper.needsUID(m_Generator.getClass())) {
      fail("Doesn't declare serialVersionUID!");
    }
  }

  /**
   * tests the listing of the options
   */
  public void testListOptions() {
    if (!m_OptionTester.checkListOptions()) {
      fail("Options cannot be listed via listOptions.");
    }
  }

  /**
   * tests the setting of the options
   */
  public void testSetOptions() {
    if (!m_OptionTester.checkSetOptions()) {
      fail("setOptions method failed.");
    }
  }

  /**
   * tests whether the default settings are processed correctly
   */
  public void testDefaultOptions() {
    if (!m_OptionTester.checkDefaultOptions()) {
      fail("Default options were not processed correctly.");
    }
  }

  /**
   * tests whether there are any remaining options
   */
  public void testRemainingOptions() {
    if (!m_OptionTester.checkRemainingOptions()) {
      fail("There were 'left-over' options.");
    }
  }

  /**
   * tests the whether the user-supplied options stay the same after setting.
   * getting, and re-setting again.
   */
  public void testCanonicalUserOptions() {
    if (!m_OptionTester.checkCanonicalUserOptions()) {
      fail("setOptions method failed");
    }
  }

  /**
   * tests the resetting of the options to the default ones
   */
  public void testResettingOptions() {
    if (!m_OptionTester.checkSetOptions()) {
      fail("Resetting of options failed");
    }
  }

  /**
   * tests for a globalInfo method
   */
  public void testGlobalInfo() {
    if (!m_GOETester.checkGlobalInfo()) {
      fail("No globalInfo method");
    }
  }

  /**
   * tests the tool tips
   */
  public void testToolTips() {
    if (!m_GOETester.checkToolTips()) {
      fail("Tool tips inconsistent");
    }
  }
}
