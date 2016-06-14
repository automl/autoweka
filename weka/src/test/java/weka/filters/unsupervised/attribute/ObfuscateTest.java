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

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests Obfuscate. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.ObfuscateTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 12656 $
 */
public class ObfuscateTest extends AbstractFilterTest {
  
  public ObfuscateTest(String name) { super(name);  }

  protected FilteredClassifier getFilteredClassifier() {
    FilteredClassifier result = super.getFilteredClassifier();
    result.setDoNotCheckForModifiedClassAttribute(true);

    return result;
  }

  /** Creates a default Obfuscate */
  public Filter getFilter() {
    return new Obfuscate();
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    
    assertTrue(!m_Instances.relationName().equals(result.relationName()));
    for (int i = 0; i < m_Instances.numAttributes(); i++) {
      Attribute inatt = m_Instances.attribute(i);
      Attribute outatt = result.attribute(i);
      if (!inatt.isString() && !inatt.isDate()) {
        assertTrue("Attribute names should be changed",
               !inatt.name().equals(outatt.name()));
        if (inatt.isNominal()) {
          assertEquals("Number of nominal values shouldn't change",
                       inatt.numValues(), outatt.numValues());
          for (int j = 0; j < inatt.numValues(); j++) {
            assertTrue("Nominal labels should be changed",
                   !inatt.value(j).equals(outatt.value(j)));
          }
        }
      }
    }
  }

  public static Test suite() {
    return new TestSuite(ObfuscateTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
