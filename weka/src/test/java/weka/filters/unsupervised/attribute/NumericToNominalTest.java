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

package weka.filters.unsupervised.attribute;

import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests NumericToNominal. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.NumericToNominalTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class NumericToNominalTest extends AbstractFilterTest {
  
  public NumericToNominalTest(String name) { 
    super(name);
  }

  /** Creates a default NumericToNominal */
  public Filter getFilter() {
    return new NumericToNominal();
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // no date and numeric attributes should remain
    if (result.checkForAttributeType(Attribute.DATE))
      fail("Date attribute(s) left over!");
    if (result.checkForAttributeType(Attribute.NUMERIC))
      fail("Numeric attribute(s) left over!");
  }

  public static Test suite() {
    return new TestSuite(NumericToNominalTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
