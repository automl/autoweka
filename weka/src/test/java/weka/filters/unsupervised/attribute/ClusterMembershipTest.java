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
import weka.core.Instances;
import weka.core.TestInstances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests ClusterMembership. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.ClusterMembershipTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class ClusterMembershipTest 
  extends AbstractFilterTest {
  
  public ClusterMembershipTest(String name) { 
    super(name);  
  }

  /** Need to remove non-nominal/numeric attributes, set class index */
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

    // class index
    m_Instances.setClassIndex(1);
  }
  
  /** Creates a default ClusterMembership */
  public Filter getFilter() {
    ClusterMembership f = new ClusterMembership();
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
    test.setClassIndex(TestInstances.CLASS_IS_LAST);

    result = test.generate();
    
    return result;
  }

  public void testNominal() {
    m_Filter = getFilter();
    m_Instances.setClassIndex(1);
    Instances result = useFilter();
    // classes must be still the same
    assertEquals(m_Instances.numClasses(), result.numClasses());
    // at least one cluster per label besides class
    assertTrue(result.numAttributes() >= m_Instances.numClasses() + 1);
  }

  public void testNumeric() {
    m_Filter = getFilter();
    m_Instances.setClassIndex(2);
    Instances result = useFilter();
    // at least one cluster (only one clusterer is generateed) besides class
    assertTrue(result.numAttributes() >= 1 + 1);
  }

  public static Test suite() {
    return new TestSuite(ClusterMembershipTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
