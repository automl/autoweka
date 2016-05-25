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

package weka.core;

import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.test.WekaTestSuite;

/**
 * Test class for all core classes. Run from the command line with:
 * <p/>
 * java weka.core.AllTests
 * 
 * @author FracPete (frapcete at waikato dot ac dot nz)
 * @version $Revision: 12074 $
 */
public class AllTests extends WekaTestSuite {

  /**
   * generates all the tests
   * 
   * @return all the tests
   */
  public static Test suite() {
    TestSuite suite = new TestSuite();

    // all test in core package
    Vector<String> packages = new Vector<String>();
    packages.add("weka.core");
    suite.addTest(suite(TestCase.class.getName(), packages));

    // all OptionHandler's
    // TODO: fix all errors
    // suite.addTest(OptionHandlersTest.suite());

    // converters
    suite.addTest(weka.core.converters.AllTests.suite());

    // neighboursearch
    suite.addTest(weka.core.neighboursearch.AllTests.suite());

    // tokenizers
    suite.addTest(weka.core.tokenizers.AllTests.suite());

    suite
      .addTestSuite(weka.core.expressionlanguage.ExpressionLanguageTest.class);

    suite.addTestSuite(weka.core.DictionaryBuilderTest.class);

    return suite;
  }

  /**
   * for running the tests from commandline
   * 
   * @param args the commandline arguments - ignored
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
