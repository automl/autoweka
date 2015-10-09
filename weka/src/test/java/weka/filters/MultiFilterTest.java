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

package weka.filters;

import weka.core.Instances;
import weka.filters.unsupervised.attribute.Add;
import weka.filters.unsupervised.attribute.AddExpression;
import weka.filters.unsupervised.attribute.Center;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Tests MultiFilter. Run from the command line with: <p/>
 * java weka.filters.MultiFilterTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class MultiFilterTest extends AbstractFilterTest {
  
  public MultiFilterTest(String name) { 
    super(name);  
  }

  /** Creates a default MultiFilter */
  public Filter getFilter() {
    return new MultiFilter();
  }

  /** Creates a configured MultiFilter */
  public Filter getConfiguredFilter() {
    MultiFilter result = new MultiFilter();
    
    Filter[] filters = new Filter[2];
    filters[0] = new Add();
    ((Add) filters[0]).setAttributeIndex("last");
    filters[1] = new AddExpression();
    ((AddExpression) filters[1]).setExpression("a3+a6");
    
    result.setFilters(filters);
    
    return result;
  }

  /** Creates a configured MultiFilter (variant) */
  public Filter getConfiguredFilterVariant() {
    MultiFilter result = new MultiFilter();
    
    Filter[] filters = new Filter[2];
    filters[0] = new ReplaceMissingValues();
    filters[1] = new Center();
    
    result.setFilters(filters);
    
    return result;
  }

  public void testDefault() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
  }

  /**  
   * tests Add + AddExpression filter
   */
  public void testConfigured() {
    m_Filter = getConfiguredFilter();
    Instances result = useFilter();
    // Number of attributes should be 2 more
    assertEquals(m_Instances.numAttributes() + 2, result.numAttributes());
    // Number of instances shouldn't change
    assertEquals(m_Instances.numInstances(),  result.numInstances());
  }

  /**  
   * tests ReplaceMissingValues + Center filter
   */
  public void testConfiguredVariant() {
    m_Filter = getConfiguredFilterVariant();
    Instances result = useFilter();
    // Number of atytributes + instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
  }

  public static Test suite() {
    return new TestSuite(MultiFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
