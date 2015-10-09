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

package weka.filters.unsupervised.attribute;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests NominalToBinary. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.NominalToBinaryTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public class NominalToBinaryTest extends AbstractFilterTest {
  
  public NominalToBinaryTest(String name) { super(name);  }

  /** Creates an example NominalToBinary */
  public Filter getFilter() {
    NominalToBinary f = new NominalToBinary();
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes() + 5, result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Eibe can enhance this to check the binarizing is correct.
  }


  public static Test suite() {
    return new TestSuite(NominalToBinaryTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
