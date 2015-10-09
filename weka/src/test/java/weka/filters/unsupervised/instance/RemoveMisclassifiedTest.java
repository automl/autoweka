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

package weka.filters.unsupervised.instance;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests RemoveMisclassified. Run from the command line with: <p/>
 * java weka.filters.unsupervised.instance.RemoveMisclassifiedTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class RemoveMisclassifiedTest 
  extends AbstractFilterTest {
  
  public RemoveMisclassifiedTest(String name) { 
    super(name);  
  }

  /** Need to remove non-nominal attributes, set class index */
  protected void setUp() throws Exception {
    super.setUp();

    // class index
    m_Instances.setClassIndex(1);
    
    // remove attributes that are not nominal/numeric
    int i = 0;
    while (i < m_Instances.numAttributes()) {
      if (    !m_Instances.attribute(i).isNominal()
           && !m_Instances.attribute(i).isNumeric() )
        m_Instances.deleteAttributeAt(i);
      else
        i++;
    }
  }
  
  /** Creates a default RemoveMisclassified, suited for nominal class */
  public Filter getFilter() {
    return getFilter(true);
  }

  /**
   * Creates a RemoveMisclassified, with either J48 (true) or M5P (false)
   * as classifier
   */
  protected Filter getFilter(boolean nominal) {
    RemoveMisclassified f = new RemoveMisclassified();
    
    // classifier
    if (nominal)
      f.setClassifier(new weka.classifiers.trees.J48());
    else
      f.setClassifier(new weka.classifiers.trees.M5P());
    
    // threshold
    if (!nominal)
      f.setThreshold(2.0);
    
    return f;
  }

  public void testNominal() {
    m_Filter = getFilter(true);
    m_Instances.setClassIndex(0);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
  }

  public void testNumeric() {
    m_Filter = getFilter(false);
    m_Instances.setClassIndex(1);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
  }

  public void testInverting() {
    // not inverted
    m_Filter = getFilter();
    m_Instances.setClassIndex(0);
    Instances result = useFilter();
    
    // inverted
    m_Filter = getFilter();
    ((RemoveMisclassified) m_Filter).setInvert(true);
    m_Instances.setClassIndex(0);
    Instances resultInv = useFilter();

    assertEquals(
        m_Instances.numInstances(), 
        result.numInstances() + resultInv.numInstances());
  }

  public static Test suite() {
    return new TestSuite(RemoveMisclassifiedTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
