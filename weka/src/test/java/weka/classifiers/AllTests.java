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
 * Copyright (C) 2002 University of Waikato 
 */

package weka.classifiers;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.test.WekaTestSuite;

/**
 * Test class for all classifiers. Run from the command line with:
 * <p/>
 * java weka.classifiers.AllTests
 * 
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @author FracPete (frapcete at waikato dot ac dot nz)
 * @version $Revision: 11475 $
 */
public class AllTests
  extends WekaTestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new TestSuite(weka.classifiers.CostMatrixTest.class));
    suite.addTest(weka.classifiers.pmml.consumer.AllTests.suite());
    suite.addTest(suite("weka.classifiers.Classifier"));
    suite.addTest(suite("weka.classifiers.functions.supportVector.Kernel"));

    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
