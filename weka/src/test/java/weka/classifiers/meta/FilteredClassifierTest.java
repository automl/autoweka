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
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.meta;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.Classifier;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests FilteredClassifier. Run from the command line with:<p>
 * java weka.classifiers.meta.FilteredClassifierTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 8034 $
 */
public class FilteredClassifierTest extends AbstractClassifierTest {

  public FilteredClassifierTest(String name) { super(name);  }

  /** Creates a default FilteredClassifier */
  public Classifier getClassifier() {
    return new FilteredClassifier();
  }

  public static Test suite() {
    return new TestSuite(FilteredClassifierTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
