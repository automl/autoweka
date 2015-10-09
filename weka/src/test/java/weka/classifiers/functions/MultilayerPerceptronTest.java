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
 * Copyright 2001 Malcolm Ware. 
 */

package weka.classifiers.functions;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.Classifier;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests NeuralNetwork. Run from the command line with:<p>
 * java weka.classifiers.functions.MultilayerPerceptronTest
 *
 * @author <a href="mailto:mfw4@cs.waikato.ac.nz">Malcolm Ware</a>
 * @version $Revision: 8034 $
 */
public class MultilayerPerceptronTest extends AbstractClassifierTest {


  public MultilayerPerceptronTest(String name) { super(name);  }

  /** Creates a default ThresholdSelector */
  public Classifier getClassifier() {
    return new MultilayerPerceptron();
  }

  public static Test suite() {
    return new TestSuite(MultilayerPerceptronTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
