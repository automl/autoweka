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

package weka.filters.unsupervised.attribute;

import weka.classifiers.meta.FilteredClassifier;
import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.TestInstances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests AddCluster. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.AddClusterTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class AddClusterTest 
  extends AbstractFilterTest {
  
  public AddClusterTest(String name) { 
    super(name);  
  }

  /** Need to remove attributes that are not nominal/numeric */
  protected void setUp() throws Exception {
    super.setUp();
    
    // remove attributes that are not nominal/numeric
    int i = 0;
    while (i < m_Instances.numAttributes()) {
      if (   (    !m_Instances.attribute(i).isNominal()
               && !m_Instances.attribute(i).isNumeric() )
           || m_Instances.attribute(i).isDate() )
        m_Instances.deleteAttributeAt(i);
      else
        i++;
    }
  }

  /**
   * returns a configured cluster algorithm
   */
  protected Clusterer getClusterer() {
    EM c = new EM();
    try {
      c.setOptions(new String[0]);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return c;
  }
  
  /** Creates a default AddCluster, with SimpleKMeans as cluster
   * @see #getClusterer */
  public Filter getFilter() {
    AddCluster f = new AddCluster();
    f.setClusterer(getClusterer());
    return f;
  }

  /**
   * returns the configured FilteredClassifier. Since the base classifier is
   * determined heuristically, derived tests might need to adjust it.
   * 
   * @return the configured FilteredClassifier
   */
  protected FilteredClassifier getFilteredClassifier() {
    FilteredClassifier	result;
    
    result = new FilteredClassifier();
    
    result.setFilter(getFilter());
    result.setClassifier(new weka.classifiers.trees.J48());
    
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
    test.setClassType(Attribute.NOMINAL);
    test.setClassIndex(TestInstances.CLASS_IS_LAST);

    result = test.generate();
    
    return result;
  }

  public void testTypical() {
    m_Filter = getFilter();
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes() + 1, result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }

  public static Test suite() {
    return new TestSuite(AddClusterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
