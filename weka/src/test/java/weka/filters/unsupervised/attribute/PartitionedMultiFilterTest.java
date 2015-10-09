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
import weka.core.Range;
import weka.filters.AbstractFilterTest;
import weka.filters.AllFilter;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests PartitionedMultiFilter. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.PartitionedMultiFilterTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class PartitionedMultiFilterTest 
  extends AbstractFilterTest {
  
  public PartitionedMultiFilterTest(String name) { 
    super(name);  
  }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default filter to test and loads a test set of Instances.
   *
   * @throws Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    m_Instances.deleteAttributeType(Attribute.STRING);
    m_Instances.deleteAttributeType(Attribute.RELATIONAL);
  }

  /** Creates a default PartitionedMultiFilter */
  public Filter getFilter() {
    return new PartitionedMultiFilter();
  }
  
  /**
   * performs the actual test
   * 
   * @param filters	the filters to use
   * @param ranges	the ranges to use
   * @param remove	whether to remove unused attributes or not
   * @return		the processed dataset
   * @throws Exception	if apllying of filter fails
   */
  protected Instances applyFilter(Filter[] filters, Range[] ranges, boolean remove)
    throws Exception {
    
    PartitionedMultiFilter	filter;
    Instances			result;
    
    filter = (PartitionedMultiFilter) getFilter();
    filter.setFilters(filters);
    filter.setRanges(ranges);
    filter.setRemoveUnused(remove);
    filter.setInputFormat(m_Instances);
    
    result = Filter.useFilter(m_Instances, filter);
    
    return result;
  }
  
  /**
   * tests two filters with disjoint ranges
   */
  public void testDisjoint() {
    Instances result = null;
    m_Instances.setClassIndex(2);
    
    try {
      result = applyFilter(
	  new Filter[]{new AllFilter(), new AllFilter()},
	  new Range[]{new Range("1-2"),new Range("4-5")},
	  false);
    }
    catch (Exception e) {
      fail("Problem applying the filter: " + e);
    }
    
    assertEquals(m_Instances.numInstances(), result.numInstances());
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
  }
  
  /**
   * tests two filters with disjoint ranges and removing the unused attributes
   */
  public void testDisjointRemoveUnused() {
    Instances result = null;
    m_Instances.setClassIndex(2);
    
    try {
      result = applyFilter(
	  new Filter[]{new AllFilter(), new AllFilter()},
	  new Range[]{new Range("1-2"),new Range("5")},
	  true);
    }
    catch (Exception e) {
      fail("Problem applying the filter: " + e);
    }
    
    assertEquals(m_Instances.numInstances(), result.numInstances());
    assertEquals(m_Instances.numAttributes() - 1, result.numAttributes());
  }
  
  /**
   * tests two filters with overlapping ranges
   */
  public void testOverlapping() {
    Instances result = null;
    m_Instances.setClassIndex(2);
    
    try {
      result = applyFilter(
	  new Filter[]{new AllFilter(), new AllFilter()},
	  new Range[]{new Range("1,2,4"),new Range("2,4")},
	  false);
    }
    catch (Exception e) {
      fail("Problem applying the filter: " + e);
    }
    
    assertEquals(m_Instances.numInstances(), result.numInstances());
    assertEquals(m_Instances.numAttributes() + 2, result.numAttributes());
  }
  
  /**
   * tests two filters with overlapping ranges and removing the unused attributes
   */
  public void testOverlappingRemoveUnused() {
    Instances result = null;
    m_Instances.setClassIndex(2);
    
    try {
      result = applyFilter(
	  new Filter[]{new AllFilter(), new AllFilter()},
	  new Range[]{new Range("1,2,4"),new Range("2,4")},
	  true);
    }
    catch (Exception e) {
      fail("Problem applying the filter: " + e);
    }
    
    assertEquals(m_Instances.numInstances(), result.numInstances());
    assertEquals(m_Instances.numAttributes() + 1, result.numAttributes());
  }

  public static Test suite() {
    return new TestSuite(PartitionedMultiFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
