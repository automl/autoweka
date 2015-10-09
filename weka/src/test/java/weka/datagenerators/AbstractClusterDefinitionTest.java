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

import weka.core.CheckGOE;
import weka.core.SerializationHelper;

import junit.framework.TestCase;

/**
 * Abstract Test class for ClusterDefinitions.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public abstract class AbstractClusterDefinitionTest 
  extends TestCase {

  /** The cluster definition to be tested */
  protected ClusterDefinition m_Definition;
  
  /** for testing GOE stuff */
  protected CheckGOE m_GOETester;

  /**
   * Constructs the <code>AbstractClusterDefinitionTest</code>. 
   * Called by subclasses.
   *
   * @param name the name of the test class
   */
  public AbstractClusterDefinitionTest(String name) { 
    super(name); 
  }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default cluster definition to test.
   *
   * @throws Exception if an error occurs 
   */
  protected void setUp() throws Exception {
    m_Definition   = getDefinition();
    m_GOETester    = getGOETester();
  }

  /** Called by JUnit after each test method */
  protected void tearDown() {
    m_Definition   = null;
    m_GOETester    = null;
  }

  /**
   * Used to create an instance of a specific ClusterDefinition.
   *
   * @return a suitably configured <code>ClusterDefinition</code> value
   */
  public abstract ClusterDefinition getDefinition();
  
  /**
   * Configures the CheckGOE used for testing GOE stuff.
   * Sets the ClusterDefinition returned from the getDefinition() method.
   * 
   * @return	the fully configured CheckGOE
   * @see	#getDefinition()
   */
  protected CheckGOE getGOETester() {
    CheckGOE		result;
    
    result = new CheckGOE();
    result.setObject(getDefinition());
    result.setSilent(true);
    
    return result;
  }

  /**
   * tests whether setting the options returned by getOptions() works
   */
  public void testOptions() {
    try {
      m_Definition.setOptions(m_Definition.getOptions());
    }
    catch (Exception e) {
      fail("setOptions(getOptions()) does not work: " + e.getMessage());
    }
  }

  /**
   * tests whether the scheme declares a serialVersionUID.
   */
  public void testSerialVersionUID() {
    if (SerializationHelper.needsUID(m_Definition.getClass()))
      fail("Doesn't declare serialVersionUID!");
  }
  
  /**
   * tests for a globalInfo method
   */
  public void testGlobalInfo() {
    if (!m_GOETester.checkGlobalInfo())
      fail("No globalInfo method");
  }
  
  /**
   * tests the tool tips
   */
  public void testToolTips() {
    if (!m_GOETester.checkToolTips())
      fail("Tool tips inconsistent");
  }
}
