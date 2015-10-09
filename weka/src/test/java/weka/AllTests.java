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

package weka;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;         

/**
 * Test class for all tests in this directory. Run from the command line 
 * with:<p>
 * java weka.AllTests
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class AllTests extends TestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite();

    // Core components
    suite.addTest(weka.core.AllTests.suite());

    // associators
    suite.addTest(weka.associations.AllTests.suite());

    // attribute selection
    suite.addTest(weka.attributeSelection.AllTests.suite());

    // classifiers
    suite.addTest(weka.classifiers.AllTests.suite());

    // clusterers
    suite.addTest(weka.clusterers.AllTests.suite());

    // data generators
    suite.addTest(weka.datagenerators.AllTests.suite());

    // estimators
    //suite.addTest(weka.estimators.AllTests.suite());

    // filters
    suite.addTest(weka.filters.AllTests.suite());

    // High level applications
    //suite.addTest(weka.experiment.AllTests.suite());
    //suite.addTest(weka.gui.AllTests.suite());

    return suite;
  }

  public static void main(String []args) {
    junit.textui.TestRunner.run(suite());
  }
}
