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
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.core.converters;

import weka.test.WekaTestSuite;

import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test class for converter classes. Run from the command line with: <p/>
 * java weka.core.converter.AllTests
 *
 * @author FracPete (frapcete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class AllTests 
  extends WekaTestSuite {

  /**
   * generates all the tests
   * 
   * @return		all the tests
   */
  public static Test suite() {
    TestSuite suite = new TestSuite();
    
    // all tests in converter package
    Vector packages = new Vector();
    packages.add("weka.core.converters");
    suite.addTest(suite(AbstractConverterTest.class.getName(), packages));
    
    return suite;
  }

  /**
   * for running the tests from commandline
   * 
   * @param args	the commandline arguments - ignored
   */
  public static void main(String []args) {
    junit.textui.TestRunner.run(suite());
  }
}

