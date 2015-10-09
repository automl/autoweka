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
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package weka.core.converters;

import weka.core.Instance;
import weka.core.Instances;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests SVMLightLoader/SVMLightSaver. Run from the command line with:<p/>
 * java weka.core.converters.SVMLightTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class SVMLightTest 
  extends AbstractFileConverterTest {

  /**
   * Constructs the <code>SVMLightTest</code>.
   *
   * @param name the name of the test class
   */
  public SVMLightTest(String name) { 
    super(name);  
  }

  /**
   * returns the loader used in the tests.
   * 
   * @return the configured loader
   */
  public AbstractLoader getLoader() {
    return new SVMLightLoader();
  }

  /**
   * returns the saver used in the tests.
   * 
   * @return the configured saver
   */
  public AbstractSaver getSaver() {
    return new SVMLightSaver();
  }
  
  /**
   * Compare two datasets to see if they differ. Skips the equalHeaders
   * method, since the SVMLight format doesn't have any notion of attribute
   * names.
   *
   * @param data1 one set of instances
   * @param data2 the other set of instances
   * @throws Exception if the datasets differ
   */
  protected void compareDatasets(Instances data1, Instances data2)
    throws Exception {
    
    if (!(data2.numInstances() == data1.numInstances())) {
      throw new Exception("number of instances has changed");
    }
    for (int i = 0; i < data2.numInstances(); i++) {
      Instance orig = data1.instance(i);
      Instance copy = data2.instance(i);
      for (int j = 0; j < orig.numAttributes(); j++) {
        if (orig.isMissing(j)) {
          if (!copy.isMissing(j)) {
            throw new Exception("instances have changed");
          }
        } else if (orig.value(j) != copy.value(j)) {
          throw new Exception("instances have changed");
        }
        if (orig.weight() != copy.weight()) {
          throw new Exception("instance weights have changed");
        }	  
      }
    }
  }

  /**
   * returns a test suite.
   * 
   * @return the test suite
   */
  public static Test suite() {
    return new TestSuite(SVMLightTest.class);
  }

  /**
   * for running the test from commandline.
   * 
   * @param args the commandline arguments - ignored
   */
  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}

