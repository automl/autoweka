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
 * Copyright (C) 2007 University of Waikato 
 */

package weka.core.neighboursearch;

import weka.core.CheckGOE;
import weka.core.CheckOptionHandler;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.test.Regression;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;

import junit.framework.TestCase;

/**
 * Abstract Test class for neighboursearch algorithms.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public abstract class AbstractNearestNeighbourSearchTest
  extends TestCase {
  
  /** The NearestNeighbourSearch to be tested */
  protected NearestNeighbourSearch m_NearestNeighbourSearch;
  
  /** the OptionHandler tester */
  protected CheckOptionHandler m_OptionTester;
  
  /** for testing GOE stuff */
  protected CheckGOE m_GOETester;

  /** the dataset used for testing */
  protected Instances m_Instances;

  /** the number of neighbors to test */
  protected int m_NumNeighbors;

  /** for selecting random instances */
  protected Random m_Random;
  
  /**
   * Constructs the <code>AbstractNearestNeighbourSearchTest</code>. 
   * Called by subclasses.
   *
   * @param name the name of the test class
   */
  public AbstractNearestNeighbourSearchTest(String name) { 
    super(name); 
  }

  /**
   * Returns the Instances to be used in testing.
   * 
   * @return		the test instances
   * @throws Exception	if loading fails
   */
  protected Instances getInstances() throws Exception {
    Instances	result;
    
    result = new Instances(
		new BufferedReader(
		    new InputStreamReader(
			ClassLoader.getSystemResourceAsStream(
			    "weka/core/neighboursearch/anneal.arff"))));
    result.setClassIndex(result.numAttributes() - 1);
    
    return result;
  }
  
  /**
   * Configures the CheckOptionHandler uses for testing the option handling.
   * Sets the NearestNeighbourSearch returned from the 
   * getNearestNeighbourSearch() method
   * 
   * @return	the fully configured CheckOptionHandler
   * @see	#getNearestNeighbourSearch()
   */
  protected CheckOptionHandler getOptionTester() {
    CheckOptionHandler		result;
    
    result = new CheckOptionHandler();
    result.setOptionHandler(getNearestNeighbourSearch());
    result.setUserOptions(new String[0]);
    result.setSilent(true);
    
    return result;
  }
  
  /**
   * Configures the CheckGOE used for testing GOE stuff.
   * Sets the NearestNeighbourSearch returned from the 
   * getNearestNeighbourSearch() method.
   * 
   * @return	the fully configured CheckGOE
   * @see	#getNearestNeighbourSearch()
   */
  protected CheckGOE getGOETester() {
    CheckGOE		result;
    
    result = new CheckGOE();
    result.setObject(getNearestNeighbourSearch());
    result.setIgnoredProperties(result.getIgnoredProperties() + ",instances");
    result.setSilent(true);
    
    return result;
  }

  /**
   * Used to create an instance of a specific NearestNeighbourSearch.
   *
   * @return a suitably configured <code>NearestNeighbourSearch</code> value
   */
  public abstract NearestNeighbourSearch getNearestNeighbourSearch();
  
  /**
   * Called by JUnit before each test method. This implementation creates
   * the default NearestNeighbourSearch to test and loads a test set of Instances.
   *
   * @exception Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    m_NearestNeighbourSearch = getNearestNeighbourSearch();
    m_OptionTester           = getOptionTester();
    m_GOETester              = getGOETester();
    m_Instances	             = getInstances();
    m_NumNeighbors           = 3;
    m_Random                 = new Random(1);
  }

  /** Called by JUnit after each test method */
  protected void tearDown() {
    m_NearestNeighbourSearch = null;
    m_OptionTester           = null;
    m_GOETester              = null;
    m_Instances	             = null;
    m_NumNeighbors           = 0;
    m_Random                 = null;
  }

  /**
   * tests whether the scheme declares a serialVersionUID.
   */
  public void testSerialVersionUID() {
    boolean     result;

    result = !SerializationHelper.needsUID(m_NearestNeighbourSearch.getClass());

    if (!result)
      fail("Doesn't declare serialVersionUID!");
  }
  
  /**
   * tests the listing of the options
   */
  public void testListOptions() {
    if (!m_OptionTester.checkListOptions())
      fail("Options cannot be listed via listOptions.");
  }
  
  /**
   * tests the setting of the options
   */
  public void testSetOptions() {
    if (!m_OptionTester.checkSetOptions())
      fail("setOptions method failed.");
  }
  
  /**
   * tests whether the default settings are processed correctly
   */
  public void testDefaultOptions() {
    if (!m_OptionTester.checkDefaultOptions())
      fail("Default options were not processed correctly.");
  }
  
  /**
   * tests whether there are any remaining options
   */
  public void testRemainingOptions() {
    if (!m_OptionTester.checkRemainingOptions())
      fail("There were 'left-over' options.");
  }
  
  /**
   * tests the whether the user-supplied options stay the same after setting.
   * getting, and re-setting again.
   * 
   * @see 	#getOptionTester()
   */
  public void testCanonicalUserOptions() {
    if (!m_OptionTester.checkCanonicalUserOptions())
      fail("setOptions method failed");
  }
  
  /**
   * tests the resetting of the options to the default ones
   */
  public void testResettingOptions() {
    if (!m_OptionTester.checkSetOptions())
      fail("Resetting of options failed");
  }
  
  /**
   * tests for a globalInfo method
   */
  public void testGlobalInfo() {
    if (!m_GOETester.checkGlobalInfo())
      fail("No globalInfo method");
  }
  
  /**
   * tests the tool tips
   */
  public void testToolTips() {
    if (!m_GOETester.checkToolTips())
      fail("Tool tips inconsistent");
  }

  /**
   * tests whether the number of instances returned by the algorithms is the
   * same as was requested
   */
  public void testNumberOfNeighbors() {
    int		i;
    int 	instIndex;
    Instances	neighbors;
    
    try {
      m_NearestNeighbourSearch.setInstances(m_Instances);
    }
    catch (Exception e) {
      fail("Failed setting the instances: " + e);
    }
    
    for (i = 1; i <= m_NumNeighbors; i++) {
      instIndex = m_Random.nextInt(m_Instances.numInstances());
      try {
	neighbors = m_NearestNeighbourSearch.kNearestNeighbours(
	    		m_Instances.instance(instIndex), i);
	assertEquals(
	    "Returned different number of neighbors than requested", 
	    i, neighbors.numInstances());
      }
      catch (Exception e) {
	fail(
	    "Failed for " + i + " neighbors on instance " + (instIndex+1) 
	    + ": " + e);
      }
    }
  }
  
  /**
   * tests whether the tokenizer correctly initializes in the
   * buildTokenizer method
   */
  public void testBuildInitialization() {
    String[][][]	results;
    Instances		inst;
    int			i;
    int			n;
    int			m;
    
    results = new String[2][m_Instances.numInstances()][m_NumNeighbors];
    
    // two runs of determining neighbors
    for (i = 0; i < 2; i++) {
      try {
	m_NearestNeighbourSearch.setInstances(m_Instances);
	
	for (n = 0; n < m_Instances.numInstances(); n++) {
	  for (m = 1; m <= m_NumNeighbors; m++) {
	    inst = m_NearestNeighbourSearch.kNearestNeighbours(
			m_Instances.instance(n), m);
	    results[i][n][m - 1] = inst.toString();
	  }
	}
      }
      catch (Exception e) {
	fail("Build " + (i + 1) + " failed: " + e);
      }
    }
    
    // compare the results
    for (n = 0; n < m_Instances.numInstances(); n++) {
      for (m = 1; m <= m_NumNeighbors; m++) {
	if (!results[0][n][m - 1].equals(results[1][n][m - 1]))
	  fail("Results differ: instance #" + (n+1) + " with " + m + " neighbors");
      }
    }
  }

  /**
   * Runs the NearestNeighbourSearch with the given data and returns the 
   * generated results.
   *
   * @param data	the data to use
   * @return 		a <code>FastVector</code> containing the results.
   * @throws Exception	if search fails
   */
  protected FastVector useNearestNeighbourSearch(Instances data) throws Exception {
    FastVector		result;
    int			i;
    int			n;
    int			m;
    Instances		inst;
    StringBuffer	item;
    
    m_NearestNeighbourSearch.setInstances(m_Instances);
    
    result = new FastVector();
    for (i = 0; i < m_Instances.numInstances(); i++) {
      item = new StringBuffer((i+1) + ". " + m_Instances.instance(i).toString() + ": ");
      for (n = 1; n <= m_NumNeighbors; n++) {
	inst = m_NearestNeighbourSearch.kNearestNeighbours(
	    		m_Instances.instance(i), n);
	item.append(" neighbors=" + n + ": ");
	for (m = 0; m < inst.numInstances(); m++) {
	  if (m > 0)
	    item.append("; ");
	  item.append("neighbor_" + (m+1) + "=" + inst.instance(m));
	}
      }
      result.addElement(item.toString());
    }
    
    return result;
      
  }

  /**
   * Returns a string containing all the results.
   *
   * @param tokens 	a <code>FastVector</code> containing the results
   * @return 		a <code>String</code> representing the vector of results.
   */
  protected String resultsToString(FastVector results) {
    StringBuffer sb = new StringBuffer();
    
    sb.append(results.size()).append(" results for " + m_Instances.relationName() + ":\n");
    for (int i = 0; i < results.size(); i++)
      sb.append(results.elementAt(i)).append('\n');
    
    return sb.toString();
  }

  /**
   * Runs a regression test -- this checks that the output of the tested
   * object matches that in a reference version. When this test is
   * run without any pre-existing reference output, the reference version
   * is created.
   */
  public void testRegression() {
    Regression	reg;
    FastVector	regressionResult;
    
    reg = new Regression(this.getClass());
    
    try {
      regressionResult = useNearestNeighbourSearch(m_Instances);
      reg.println(resultsToString(regressionResult));
    }
    catch (Exception e) {
      fail("Regression test failed: " + e);
    }
    
    try {
      String diff = reg.diff();
      if (diff == null) {
        System.err.println("Warning: No reference available, creating."); 
      } else if (!diff.equals("")) {
        fail("Regression test failed. Difference:\n" + diff);
      }
    } 
    catch (java.io.IOException ex) {
      fail("Problem during regression testing.\n" + ex);
    }
  }
}
