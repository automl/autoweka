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

import weka.core.Instance;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests AddValues. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.AddValuesTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 12378 $
 */
public class AddValuesTest 
  extends AbstractFilterTest {
  
  /**
   * initializes the test
   * 
   * @param name	the name of the test
   */
  public AddValuesTest(String name) { 
    super(name);  
  }

  /**
   * Creates a default AddValues
   * 
   * @return		the configured filter
   */
  public Filter getFilter() {
    AddValues filter = new AddValues();
    filter.setAttributeIndex("2");
    return filter;
  }

  /**
   * Creates a specialized AddValues
   * 
   * @param sorted	whether to sort
   * @param labels	the labels to add
   * @return		the configured filter
   */
  public Filter getFilter(boolean sorted, String labels) {
    AddValues filter = new AddValues();
    filter.setAttributeIndex("2");
    filter.setSort(sorted);
    filter.setLabels(labels);
    return filter;
  }
  
  /**
   * Compare two datasets to see if they differ.
   *
   * @param data1 one set of instances
   * @param data2 the other set of instances
   * @throws Exception if the datasets differ
   */
  protected void compDatasets(Instances data1, Instances data2)
    throws Exception {
    
    if (data1.numAttributes() != data2.numAttributes())
      throw new Exception("number of attributes has changed");
    
    if (!(data2.numInstances() == data1.numInstances()))
      throw new Exception("number of instances has changed");

    for (int i = 0; i < data2.numInstances(); i++) {
      Instance orig = data1.instance(i);
      Instance copy = data2.instance(i);
      for (int j = 0; j < orig.numAttributes(); j++) {
        if (orig.isMissing(j)) {
          if (!copy.isMissing(j))
            throw new Exception("instances have changed");
        }
        else if (!orig.toString(j).equals(copy.toString(j))) {
          throw new Exception("instances have changed");
        }
        
        if (orig.weight() != copy.weight())
          throw new Exception("instance weights have changed");
      }
    }
  }

  /**
   * performs the tests with the given filter
   * 
   * @param filter	the configured filter to use in the tests
   */
  protected void performTest(Filter filter) {
    m_Filter = filter;
    Instances icopy = new Instances(m_Instances);
    Instances result = null;
    
    try {
      m_Filter.setInputFormat(icopy);
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    
    try {
      result = Filter.useFilter(m_Instances, m_Filter);
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on useFilter(): \n" + ex.getMessage());
    }

    try {
      compDatasets(icopy, result);
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Datasets differ: \n" + ex.getMessage());
    }
  }
  
  /**
   * tests the default settings
   */
  public void testDefault() {
    performTest(getFilter());
  }
  
  /**
   * tests the sorting alone
   */
  public void testSort() {
    performTest(getFilter(true, ""));
  }

  /**
   * tests the adding of labels alone
   */
  public void testLabels() {
    performTest(getFilter(false, "__blah,__blubber"));
  }

  /**
   * tests the sorting and the adding of labels
   */
  public void testSortAndLabels() {
    performTest(getFilter(false, "__blah,__blubber"));
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
	  ((AddValues) m_FilteredClassifier.getFilter()).setAttributeIndex(
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
  
  /**
   * returns the test suite
   * 
   * @return		the test suite
   */
  public static Test suite() {
    return new TestSuite(AddValuesTest.class);
  }

  /**
   * for running the test from commandline
   * 
   * @param args	the command line arguments - ignored
   */
  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
