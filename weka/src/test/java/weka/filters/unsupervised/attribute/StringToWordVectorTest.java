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
 * Tests StringToWordVector. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.StringToWordVectorTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public class StringToWordVectorTest extends AbstractFilterTest {
  
  public StringToWordVectorTest(String name) { super(name);  }

  /** Creates an example StringToWordVector */
  public Filter getFilter() {
    StringToWordVector f = new StringToWordVector();
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numInstances(),  result.numInstances());
  }

  public void testWordsToKeep() {
    ((StringToWordVector)m_Filter).setWordsToKeep(3);
    Instances result = useFilter();
    // Number of instances shouldn't change
    assertEquals(m_Instances.numInstances(),  result.numInstances());

    // Number of attributes will be minus 2 string attributes plus
    // the word attributes (aiming for 3 -- could be higher in the case of ties)
    assertEquals(m_Instances.numAttributes() - 2 + 3, result.numAttributes());
  }


  public static Test suite() {
    return new TestSuite(StringToWordVectorTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
