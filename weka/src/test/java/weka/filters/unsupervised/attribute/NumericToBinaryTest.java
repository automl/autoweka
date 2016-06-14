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
 * Tests NumericToBinary. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.NumericToBinaryTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @author Christopher Beckham (cjb60 at students dot waikato dot ac dot nz)
 * @version $Revision: 11520 $
 */
public class NumericToBinaryTest extends AbstractFilterTest {
  
  public NumericToBinaryTest(String name) { super(name);  }

  /** Creates an example NumericToBinary */
  public Filter getFilter() {
    NumericToBinary f = new NumericToBinary();
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());

    // Check conversion is OK
    for (int j = 0; j < result.numAttributes(); j++) {
      if (m_Instances.attribute(j).isNumeric()) {
        assertTrue("Numeric attribute should now be nominal",
               result.attribute(j).isNominal());
        for (int i = 0; i < result.numInstances(); i++) {
          if (m_Instances.instance(i).isMissing(j)) {
            assertTrue(result.instance(i).isMissing(j));
          } else if (m_Instances.instance(i).value(j) == 0) {
            assertTrue("Output value should be 0", 
                   result.instance(i).value(j) == 0);
          } else {
            assertTrue("Output value should be 1", 
                   result.instance(i).value(j) == 1);
          }
        }
      }
    }
  }
  
  /**
   * Make sure that the filter binarizes the index we specify.
   */
  public void testSpecificIndex() {
	  int att1 = m_Instances.attribute("NumericAtt1").index();
	  int att2 = m_Instances.attribute("NumericAtt2").index();  
	  // Set the attribute index to point to NumericAtt1, so we expect that only this
	  // attribute will be binarized.
	  ((NumericToBinary)m_Filter).setAttributeIndices( String.valueOf(att1+1) );
	  Instances result = useFilter();
	  assertTrue("NumericAtt1 should be nominal", result.attribute(att1).isNominal());
	  assertTrue("NumericAtt2 should be numeric", result.attribute(att2).isNumeric());  
	  
  }
  
  /**
   * Make sure the filter binarizes the index we specify + invert
   */
  public void testInvertSelection() {
	  int att1 = m_Instances.attribute("NumericAtt1").index();
	  int att2 = m_Instances.attribute("NumericAtt2").index(); 
	  // Set the attribute index to point to NumericAtt1, but invert the selection, so that
	  // it will try to apply the filter to *every other* attribute. Of course, because this
	  // only applies to numeric attributes, the filter should only change NumericAtt2
	  ((NumericToBinary)m_Filter).setAttributeIndices( String.valueOf(att2+1) );
	  ((NumericToBinary)m_Filter).setInvertSelection(true);
	  Instances result = useFilter();
	  assertTrue("NumericAtt1 should be nominal", result.attribute(att1).isNominal());
	  assertTrue("NumericAtt2 should be numeric", result.attribute(att2).isNumeric());
  }
  
  /**
   * Make sure the filter binarizes the appropriate attributes in the range
   * we specify
   */
  public void testRange() {
	  int att1 = m_Instances.attribute("NumericAtt1").index();
	  int att2 = m_Instances.attribute("NumericAtt2").index(); 
	  // Use the string expression "first-last" for the attribute index. This should
	  // change both numeric attributes.
	  ((NumericToBinary)m_Filter).setAttributeIndices("first-last");
	  ((NumericToBinary)m_Filter).setInvertSelection(false);
	  Instances result = useFilter();
	  assertTrue("NumericAtt1 should be nominal", result.attribute(att1).isNominal());
	  assertTrue("NumericAtt2 should be nominal", result.attribute(att2).isNominal());	
  }


  public static Test suite() {
    return new TestSuite(NumericToBinaryTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
