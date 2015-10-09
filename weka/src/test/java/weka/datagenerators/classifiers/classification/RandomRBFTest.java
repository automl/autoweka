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

package weka.datagenerators.classifiers.classification;

import weka.datagenerators.AbstractDataGeneratorTest;
import weka.datagenerators.DataGenerator;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests RandomRBF. Run from the command line with:<p/>
 * java weka.datagenerators.classifiers.classification.RandomRBFTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class RandomRBFTest 
  extends AbstractDataGeneratorTest {

  public RandomRBFTest(String name) { 
    super(name);  
  }

  /** Creates a default RandomRBF */
  public DataGenerator getGenerator() {
    return new RandomRBF();
  }

  public static Test suite() {
    return new TestSuite(RandomRBFTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
