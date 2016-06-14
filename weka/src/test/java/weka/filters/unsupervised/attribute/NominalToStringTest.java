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
 * Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.rules.ZeroR;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.TestInstances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests NominalToString. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.NominalToStringTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 11506 $
 */
public class NominalToStringTest
  extends AbstractFilterTest {
  
  public NominalToStringTest(String name) {
    super(name);
  }

  /** Creates an example NominalToString */
  public Filter getFilter() {
    NominalToString f = new NominalToString();
    f.setAttributeIndexes("2");
    return f;
  }

  /**
   * returns the configured FilteredClassifier. Since the base classifier is
   * determined heuristically, derived tests might need to adjust it.
   * 
   * @return the configured FilteredClassifier
   */
  protected FilteredClassifier getFilteredClassifier() {
    FilteredClassifier 	result;
    
    result = super.getFilteredClassifier();
    ((NominalToString) result.getFilter()).setAttributeIndexes("1");
    result.setClassifier(new ZeroR());
    
    return result;
  }
  
  /**
   * returns data generated for the FilteredClassifier test
   * 
   * @return		the dataset for the FilteredClassifier
   * @throws Exception	if generation of data fails
   */
  protected Instances getFilteredClassifierData() throws Exception{
    TestInstances	test;
    Instances		result;

    test = TestInstances.forCapabilities(m_FilteredClassifier.getCapabilities());
    test.setNumRelational(0);
    test.setClassIndex(TestInstances.CLASS_IS_LAST);

    result = test.generate();
    
    return result;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    assertEquals("Attribute type should now be STRING",
                 Attribute.STRING, result.attribute(1).type());

    assertEquals(3, result.attribute(1).numValues());
  }

  public void testMissing() {
    ((NominalToString)m_Filter).setAttributeIndexes("5");
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    assertEquals("Attribute type should now be STRING",
                 Attribute.STRING, result.attribute(4).type());
    assertEquals(4, result.attribute(4).numValues());
    for (int i = 0; i < result.numInstances(); i++) {
      assertTrue("Missing values should be preserved",
             m_Instances.instance(i).isMissing(4) ==
             result.instance(i).isMissing(4));
    }
  }
  
  /**
   * tests the filter in conjunction with the FilteredClassifier
   */
  public void testFilteredClassifier() {
    try {
      Instances data = getFilteredClassifierData();

      for (int i = 0; i < data.numAttributes(); i++) {
	if (data.classIndex() == i)
	  continue;
	if (data.attribute(i).isNominal()) {
	  ((NominalToString) m_FilteredClassifier.getFilter()).setAttributeIndexes(
	      "" + (i + 1));
	  break;
	}
      }
    }
    catch (Exception e) {
      fail("Problem setting up test for FilteredClassifier: " + e.toString());
    }
    
    super.testFilteredClassifier();
  }

  public static Test suite() {
    return new TestSuite(NominalToStringTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
