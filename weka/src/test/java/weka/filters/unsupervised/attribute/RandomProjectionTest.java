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

import weka.core.Instances;
import weka.core.SelectedTag;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests RandomProjection. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.RandomProjectionTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class RandomProjectionTest 
  extends AbstractFilterTest {
  
  public RandomProjectionTest(String name) { 
    super(name);  
  }

  /** Need to remove non-nominal attributes, set class index */
  protected void setUp() throws Exception {
    super.setUp();

    // class index
    m_Instances.setClassIndex(1);
  }
  
  /** Creates a default RandomProjection */
  public Filter getFilter() {
    return getFilter(new RandomProjection().getNumberOfAttributes());
  }
  
  /** Creates a RandomProjection with the number of attributes */
  protected Filter getFilter(int numAtts) {
    RandomProjection f = new RandomProjection();
    f.setNumberOfAttributes(numAtts);
    return f;
  }

  /**
   * performs some checks on the given result
   *
   * @param result the instances to compare against original dataset
   */
  protected void checkResult(Instances result) {
    assertEquals(
        ((RandomProjection) m_Filter).getNumberOfAttributes() + 1, 
        result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }

  /**
   * tests the given distribution type
   *
   * @param type the distribution type to use
   * @see RandomProjection#TAGS_DSTRS_TYPE
   */
  protected void checkDistributionType(int type) {
    m_Filter = getFilter();
    ((RandomProjection) m_Filter).setDistribution(
        new SelectedTag(type, RandomProjection.TAGS_DSTRS_TYPE));
    Instances result = useFilter();
    checkResult(result);
  }

  public void testSparse1() {
    checkDistributionType(RandomProjection.SPARSE1);
  }

  public void testSparse2() {
    checkDistributionType(RandomProjection.SPARSE2);
  }

  public void testGaussian() {
    checkDistributionType(RandomProjection.GAUSSIAN);
  }

  public void testNumberOfAttributes() {
    m_Filter = getFilter(5);
    Instances result = useFilter();
    checkResult(result);
  }

  public static Test suite() {
    return new TestSuite(RandomProjectionTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
