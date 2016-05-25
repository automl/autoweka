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
 * Copyright (C) 2002-2016 University of Waikato
 */

package weka.filters;

import junit.framework.TestCase;
import weka.classifiers.Classifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.Capabilities.Capability;
import weka.core.CheckGOE;
import weka.core.CheckOptionHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.SerializationHelper;
import weka.core.TestInstances;
import weka.test.Regression;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;

/**
 * Abstract Test class for Filters.
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @authro FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 12375 $
 */
public abstract class AbstractFilterTest
  extends TestCase {

  // TODO: 
  // * Check that results between incremental and batch use are
  //   the same
  // * Check batch operation is OK
  // * Check memory use between subsequent runs
  // * Check memory use when multiplying data?

  /** Set to true to print out extra info during testing */
  protected static boolean VERBOSE = false;

  /** The filter to be tested */
  protected Filter m_Filter;

  /** A set of instances to test with */
  protected Instances m_Instances;
  
  /** the OptionHandler tester */
  protected CheckOptionHandler m_OptionTester;
  
  /** the FilteredClassifier instance used for tests */
  protected FilteredClassifier m_FilteredClassifier;
  
  /** for testing GOE stuff */
  protected CheckGOE m_GOETester;

  /**
   * Constructs the <code>AbstractFilterTest</code>. Called by subclasses.
   *
   * @param name the name of the test class
   */
  public AbstractFilterTest(String name) {
    super(name);
  }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default filter to test and loads a test set of Instances.
   *
   * @throws Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    m_Filter             = getFilter();
    m_Instances          = new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/data/FilterTest.arff"))));
    m_OptionTester       = getOptionTester();
    m_GOETester          = getGOETester();
    m_FilteredClassifier = getFilteredClassifier();
  }

  /** Called by JUnit after each test method */
  protected void tearDown() {
    m_Filter             = null;
    m_Instances          = null;
    m_OptionTester       = null;
    m_GOETester          = null;
    m_FilteredClassifier = null;
  }
  
  /**
   * Configures the CheckOptionHandler uses for testing the optionhandling.
   * Sets the scheme to test.
   * 
   * @return	the fully configured CheckOptionHandler
   */
  protected CheckOptionHandler getOptionTester() {
    CheckOptionHandler		result;
    
    result = new CheckOptionHandler();
    if (getFilter() instanceof OptionHandler)
      result.setOptionHandler((OptionHandler) getFilter());
    else
      result.setOptionHandler(null);
    result.setUserOptions(new String[0]);
    result.setSilent(true);
    
    return result;
  }
  
  /**
   * Configures the CheckGOE used for testing GOE stuff.
   * Sets the Filter returned from the getFilter() method.
   * 
   * @return	the fully configured CheckGOE
   * @see	#getFilter()
   */
  protected CheckGOE getGOETester() {
    CheckGOE		result;
    
    result = new CheckGOE();
    result.setObject(getFilter());
    result.setSilent(true);
    
    return result;
  }

  /**
   * returns the configured FilteredClassifier. Since the base classifier is
   * determined heuristically, derived tests might need to adjust it.
   * 
   * @return the configured FilteredClassifier
   */
  protected FilteredClassifier getFilteredClassifier() {
    FilteredClassifier	result;
    Filter		filter;
    Classifier		cls;
    
    result = new FilteredClassifier();
    
    // set filter
    filter = getFilter();
    result.setFilter(filter);
    
    // set classifier
    if (filter.getCapabilities().handles(Capability.NOMINAL_CLASS))
      cls = new weka.classifiers.trees.J48();
    else if (filter.getCapabilities().handles(Capability.BINARY_CLASS))
      cls = new weka.classifiers.trees.J48();
    else if (filter.getCapabilities().handles(Capability.UNARY_CLASS))
      cls = new weka.classifiers.trees.J48();
    else if (filter.getCapabilities().handles(Capability.NUMERIC_CLASS))
      cls = new weka.classifiers.trees.M5P();
    else if (filter.getCapabilities().handles(Capability.DATE_CLASS))
      cls = new weka.classifiers.trees.M5P();
    else
      throw new IllegalStateException("Cannot determine base classifier for FilteredClassifier!");
    result.setClassifier(cls);
    
    return result;
  }
  
  /**
   * returns data generated for the FilteredClassifier test
   * 
   * @return		the dataset for the FilteredClassifier
   * @throws Exception	if generation of data fails
   */
  protected Instances getFilteredClassifierData() throws Exception {
    TestInstances	test;
    Instances		result;

    // NB: in order to make sure that the classifier can handle the data,
    //     we're using the classifier's capabilities to generate the data.
    test = TestInstances.forCapabilities(
  	m_FilteredClassifier.getClassifier().getCapabilities());
    test.setClassIndex(TestInstances.CLASS_IS_LAST);

    result = test.generate();
    
    return result;
  }
  
  /**
   * Used to create an instance of a specific filter. The filter
   * should be configured to operate on a dataset that contains
   * attributes in this order:<p>
   *
   * String, Nominal, Numeric, String, Nominal, Numeric<p>
   *
   * Where the first three attributes do not contain any missing values,
   * but the last three attributes do. If the filter is for some reason
   * incapable of accepting a dataset of this type, override setUp() to 
   * either manipulate the default dataset to be compatible, or load another
   * test dataset. <p>
   *
   * The configured filter should preferrably do something
   * meaningful, since the results of filtering are used as the default
   * regression output (and it would hardly be interesting if the filtered 
   * data was the same as the input data).
   *
   * @return a suitably configured <code>Filter</code> value
   */
  public abstract Filter getFilter();

  /**
   * Simple method to return the filtered set of test instances after
   * passing through the test filter. m_Filter contains the filter and
   * m_Instances contains the test instances.
   *
   * @return the Instances after filtering through the filter we have set
   * up to test.  
   */
  protected Instances useFilter() {

    Instances result = null;
    Instances icopy = new Instances(m_Instances);
    try {
      m_Filter.setInputFormat(icopy);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    try {
      result = Filter.useFilter(icopy, m_Filter);
      assertNotNull(result);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on useFilter(): \n" + ex.getMessage());
    }
    return result;
  }

  /**
   * tests whether the scheme declares a serialVersionUID.
   */
  public void testSerialVersionUID() {
    if (SerializationHelper.needsUID(m_Filter.getClass()))
      fail("Doesn't declare serialVersionUID!");
  }
  
  /**
   * Test buffered operation. Output instances are only collected after
   * all instances are passed through
   */
  public void testBuffered() {

    Instances icopy = new Instances(m_Instances);
    Instances result = null;
    try {
      m_Filter.setInputFormat(icopy);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    try {
      result = Filter.useFilter(icopy, m_Filter);
      assertNotNull(result);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on useFilter(): \n" + ex.getMessage());
    }

    // Check the output is valid for printing by trying to write out to 
    // a stringbuffer
    StringWriter sw = new StringWriter(2000);
    sw.write(result.toString());

    // Check the input hasn't been modified
    // We just check the headers are the same and that the instance
    // count is the same.
    assertTrue(icopy.equalHeaders(m_Instances));
    assertEquals(icopy.numInstances(), m_Instances.numInstances());

    // Try repeating the filtering and check we get the same results
    Instances result2 = null;
    try {
      m_Filter.setInputFormat(icopy);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    try {
      result2 = Filter.useFilter(icopy, m_Filter);
      assertNotNull(result2);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on useFilter(): \n" + ex.getMessage());
    }

    // Again check the input hasn't been modified
    // We just check the headers are the same and that the instance
    // count is the same.
    assertTrue(icopy.equalHeaders(m_Instances));
    assertEquals(icopy.numInstances(), m_Instances.numInstances());

    // Check the same results for both runs
    assertTrue(result.equalHeaders(result2));
    assertEquals(result.numInstances(), result2.numInstances());
    
  }

  /**
   * Test incremental operation. Each instance is removed as soon as it
   * is made available
   */
  public void testIncremental() {

    Instances icopy = new Instances(m_Instances);
    Instances result = null;
    boolean headerImmediate = false;
    try {
      headerImmediate = m_Filter.setInputFormat(icopy);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    if (headerImmediate) {
      if (VERBOSE) System.err.println("Filter makes header immediately available.");
      result = m_Filter.getOutputFormat();
    }
    // Pass all the instances to the filter
    for (int i = 0; i < icopy.numInstances(); i++) {
      if (VERBOSE) System.err.println("Input instance to filter");
      boolean collectNow = false;
      try {
        collectNow = m_Filter.input(icopy.instance(i));
      } catch (Exception ex) {
        ex.printStackTrace();
        fail("Exception thrown on input(): \n" + ex.getMessage());
      }
      if (collectNow) {
        if (VERBOSE) System.err.println("Filter said collect immediately");
	if (!headerImmediate) {
	  fail("Filter didn't return true from setInputFormat() earlier!");
	}
        if (VERBOSE) System.err.println("Getting output instance");
	result.add(m_Filter.output());
      }
    }
    // Say that input has finished, and print any pending output instances
    if (VERBOSE) System.err.println("Setting end of batch");
    boolean toCollect = false;
    try {
      toCollect = m_Filter.batchFinished();
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on batchFinished(): \n" + ex.getMessage());
    }
    if (toCollect) {
      if (VERBOSE) System.err.println("Filter said collect output");
      if (!headerImmediate) {
        if (VERBOSE) System.err.println("Getting output format");
	result = m_Filter.getOutputFormat();
      }
      if (VERBOSE) System.err.println("Getting output instance");
      while (m_Filter.numPendingOutput() > 0) {
	result.add(m_Filter.output());
        if (VERBOSE) System.err.println("Getting output instance");
      }
    }
    
    assertNotNull(result);

    // Check the output iss valid for printing by trying to write out to 
    // a stringbuffer
    StringWriter sw = new StringWriter(2000);
    sw.write(result.toString());
  }

  /**
   * Describe <code>testRegression</code> method here.
   *
   */
  public void testRegression() {

    Regression reg = new Regression(this.getClass());
    Instances result = useFilter();
    reg.println(result.toString());
    try {
      String diff = reg.diff();
      if (diff == null) {
        System.err.println("Warning: No reference available, creating."); 
      } else if (!diff.equals("")) {
        fail("Regression test failed. Difference:\n" + diff);
      }
    } catch (java.io.IOException ex) {
      fail("Problem during regression testing.\n" + ex);
    }

    reg = new Regression(this.getClass());

    // Run the filter using deprecated calls to check it still works the same
    Instances icopy = new Instances(m_Instances);
    try {
      m_Filter.setInputFormat(icopy);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    try {
      for (int i = 0; i < icopy.numInstances(); i++) {
        m_Filter.input(icopy.instance(i));
      }
      m_Filter.batchFinished();
      result = m_Filter.getOutputFormat();
      weka.core.Instance processed;
      while ((processed = m_Filter.output()) != null) {
        result.add(processed);
      }
      assertNotNull(result);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on useFilter(): \n" + ex.getMessage());
    }
    reg.println(result.toString());
    try {
      String diff = reg.diff();
      if (diff == null) {
        System.err.println("Warning: No reference available, creating."); 
      } else if (!diff.equals("")) {
        fail("Regression test failed when using deprecated methods. Difference:\n" + diff);
      }
    } catch (java.io.IOException ex) {
      fail("Problem during regression testing.\n" + ex);
    }
  }

  public void testThroughput() {

    if (VERBOSE) {
      Instances icopy = new Instances(m_Instances);
      // Make a bigger dataset
      Instances result = null;
      for (int i = 0; i < 20000; i++) {
        icopy.add(m_Instances.instance(i%m_Instances.numInstances()));
      }
      long starttime, endtime;
      double secs, rate;


      // Time incremental usage
      starttime = System.currentTimeMillis();
      boolean headerImmediate = false;
      try {
        headerImmediate = m_Filter.setInputFormat(icopy);
        if (headerImmediate) {
          result = m_Filter.getOutputFormat();
        }
        for (int i = 0; i < icopy.numInstances(); i++) {
          boolean collectNow = false;
          collectNow = m_Filter.input(icopy.instance(i));
          if (collectNow) {
            if (!headerImmediate) {
              fail("Filter didn't return true from setInputFormat() earlier!");
            }
            result.add(m_Filter.output());
          }
        }
        // Say that input has finished, and print any pending output instances
        boolean toCollect = false;
        toCollect = m_Filter.batchFinished();
        if (toCollect) {
          if (!headerImmediate) {
            result = m_Filter.getOutputFormat();
          }
          while (m_Filter.numPendingOutput() > 0) {
            result.add(m_Filter.output());
          }
        }
      } catch (Exception ex) {
        ex.printStackTrace();
        fail("Exception thrown during incremental filtering: \n" + ex.getMessage());
      }
      endtime = System.currentTimeMillis();
      secs = (double)(endtime - starttime) / 1000;
      rate = (double)icopy.numInstances() / secs;
      System.err.println("\n" + m_Filter.getClass().getName() 
                         + " incrementally processed " 
                         + rate + " instances per sec"); 
      
      // Time batch usage
      starttime = System.currentTimeMillis();
      try {
        m_Filter.setInputFormat(icopy);
        result = Filter.useFilter(icopy, m_Filter);
        assertNotNull(result);
      } catch (Exception ex) {
        ex.printStackTrace();
        fail("Exception thrown during batch filtering: \n" + ex.getMessage());
      }
      endtime = System.currentTimeMillis();
      secs = (double)(endtime - starttime) / 1000;
      rate = (double)icopy.numInstances() / secs;
      System.err.println("\n" + m_Filter.getClass().getName() 
                         + " batch processed " 
                         + rate + " instances per sec"); 


    }
  }
  
  /**
   * tests the listing of the options
   */
  public void testListOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkListOptions())
	fail("Options cannot be listed via listOptions.");
    }
  }
  
  /**
   * tests the setting of the options
   */
  public void testSetOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkSetOptions())
	fail("setOptions method failed.");
    }
  }
  
  /**
   * tests whether the default settings are processed correctly
   */
  public void testDefaultOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkDefaultOptions())
	fail("Default options were not processed correctly.");
    }
  }
  
  /**
   * tests whether there are any remaining options
   */
  public void testRemainingOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkRemainingOptions())
	fail("There were 'left-over' options.");
    }
  }
  
  /**
   * tests the whether the user-supplied options stay the same after setting.
   * getting, and re-setting again.
   * 
   * @see 	#getOptionTester()
   */
  public void testCanonicalUserOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkCanonicalUserOptions())
	fail("setOptions method failed");
    }
  }
  
  /**
   * tests the resetting of the options to the default ones
   */
  public void testResettingOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkSetOptions())
	fail("Resetting of options failed");
    }
  }
  
  /**
   * tests the filter in conjunction with the FilteredClassifier
   */
  public void testFilteredClassifier() {
    Instances		data;
    int			i;
    
    // skip this test if a subclass has set the
    // filtered classifier to null
    if (m_FilteredClassifier == null) {
      return;
    }
    
    try {
      // generate data
      data = getFilteredClassifierData();
      
      // build classifier
      m_FilteredClassifier.buildClassifier(data);

      // test classifier
      for (i = 0; i < data.numInstances(); i++) {
	m_FilteredClassifier.classifyInstance(data.instance(i));
      }
    }
    catch (Exception e) {
      fail("Problem with FilteredClassifier: " + e.toString());
    }
  }
  
  /**
   * simulates batch filtering
   */
  public void testBatchFiltering() {
    Instances result = null;
    Instances icopy = new Instances(m_Instances);
    
    // setup filter
    try {
      if (m_Filter.setInputFormat(icopy)) {
	result = m_Filter.getOutputFormat();
	assertNotNull("Output format defined (setup)", result);
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex);
    }

    // first batch
    try {
      for (int i = 0; i < icopy.numInstances(); i++) {
	if (m_Filter.input(icopy.instance(i))) {
	  Instance out = m_Filter.output();
	  assertNotNull("Instance not made available immediately (1. batch)", out);
	  result.add(out);
	}
      }
      m_Filter.batchFinished();

      if (result == null) {
	result = m_Filter.getOutputFormat();
	assertNotNull("Output format defined (1. batch)", result);
	assertTrue("Pending output instances (1. batch)", m_Filter.numPendingOutput() > 0);
      }

      while (m_Filter.numPendingOutput() > 0)
	result.add(m_Filter.output());
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown during 1. batch: \n" + ex);
    }

    // second batch
    try {
      result = null;
      if (m_Filter.isOutputFormatDefined())
	result = m_Filter.getOutputFormat();
      
      for (int i = 0; i < icopy.numInstances(); i++) {
	if (m_Filter.input(icopy.instance(i))) {
	  if (result == null) {
	    fail("Filter didn't return true from isOutputFormatDefined() (2. batch)");
	  }
	  else {
	    Instance out = m_Filter.output();
	    assertNotNull("Instance not made available immediately (2. batch)", out);
	    result.add(out);
	  }
	}
      }
      m_Filter.batchFinished();
      
      if (result == null) {
	result = m_Filter.getOutputFormat();
	assertNotNull("Output format defined (2. batch)", result);
	assertTrue("Pending output instances (2. batch)", m_Filter.numPendingOutput() > 0);
      }

      while (m_Filter.numPendingOutput() > 0)
	result.add(m_Filter.output());
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown during 2. batch: \n" + ex);
    }
  }
  
  /**
   * simulates batch filtering (with the second dataset being smaller)
   */
  public void testBatchFilteringSmaller() {
    Instances result = null;
    Instances icopy = new Instances(m_Instances);
    
    // setup filter
    try {
      if (m_Filter.setInputFormat(icopy)) {
	result = m_Filter.getOutputFormat();
	assertNotNull("Output format defined (setup)", result);
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex);
    }

    // first batch
    try {
      for (int i = 0; i < icopy.numInstances(); i++) {
	if (m_Filter.input(icopy.instance(i))) {
	  Instance out = m_Filter.output();
	  assertNotNull("Instance not made available immediately (1. batch)", out);
	  result.add(out);
	}
      }
      m_Filter.batchFinished();

      if (result == null) {
	result = m_Filter.getOutputFormat();
	assertNotNull("Output format defined (1. batch)", result);
	assertTrue("Pending output instances (1. batch)", m_Filter.numPendingOutput() > 0);
      }

      while (m_Filter.numPendingOutput() > 0)
	result.add(m_Filter.output());
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown during 1. batch: \n" + ex);
    }

    // second batch
    try {
      result = null;
      if (m_Filter.isOutputFormatDefined())
	result = m_Filter.getOutputFormat();
      
      // delete some instances
      int num = (int) ((double) icopy.numInstances() * 0.3);
      for (int i = 0; i < num; i++)
	icopy.delete(0);
      
      for (int i = 0; i < icopy.numInstances(); i++) {
	if (m_Filter.input(icopy.instance(i))) {
	  if (result == null) {
	    fail("Filter didn't return true from isOutputFormatDefined() (2. batch)");
	  }
	  else {
	    Instance out = m_Filter.output();
	    assertNotNull("Instance not made available immediately (2. batch)", out);
	    result.add(out);
	  }
	}
      }
      m_Filter.batchFinished();
      
      if (result == null) {
	result = m_Filter.getOutputFormat();
	assertNotNull("Output format defined (2. batch)", result);
	assertTrue("Pending output instances (2. batch)", m_Filter.numPendingOutput() > 0);
      }

      while (m_Filter.numPendingOutput() > 0)
	result.add(m_Filter.output());
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown during 2. batch: \n" + ex);
    }
  }
  
  /**
   * simulates batch filtering (with the second dataset being bigger)
   */
  public void testBatchFilteringLarger() {
    Instances result = null;
    Instances icopy = new Instances(m_Instances);
    
    // setup filter
    try {
      if (m_Filter.setInputFormat(icopy)) {
	result = m_Filter.getOutputFormat();
	assertNotNull("Output format defined (setup)", result);
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex);
    }

    // first batch
    try {
      for (int i = 0; i < icopy.numInstances(); i++) {
	if (m_Filter.input(icopy.instance(i))) {
	  Instance out = m_Filter.output();
	  assertNotNull("Instance not made available immediately (1. batch)", out);
	  result.add(out);
	}
      }
      m_Filter.batchFinished();

      if (result == null) {
	result = m_Filter.getOutputFormat();
	assertNotNull("Output format defined (1. batch)", result);
	assertTrue("Pending output instances (1. batch)", m_Filter.numPendingOutput() > 0);
      }

      while (m_Filter.numPendingOutput() > 0)
	result.add(m_Filter.output());
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown during 1. batch: \n" + ex);
    }

    // second batch
    try {
      result = null;
      if (m_Filter.isOutputFormatDefined())
	result = m_Filter.getOutputFormat();
      
      // add some instances
      int num = (int) ((double) icopy.numInstances() * 0.3);
      for (int i = 0; i < num; i++)
	icopy.add(icopy.instance(i));
      
      for (int i = 0; i < icopy.numInstances(); i++) {
	if (m_Filter.input(icopy.instance(i))) {
	  if (result == null) {
	    fail("Filter didn't return true from isOutputFormatDefined() (2. batch)");
	  }
	  else {
	    Instance out = m_Filter.output();
	    assertNotNull("Instance not made available immediately (2. batch)", out);
	    result.add(out);
	  }
	}
      }
      m_Filter.batchFinished();
      
      if (result == null) {
	result = m_Filter.getOutputFormat();
	assertNotNull("Output format defined (2. batch)", result);
	assertTrue("Pending output instances (2. batch)", m_Filter.numPendingOutput() > 0);
      }

      while (m_Filter.numPendingOutput() > 0)
	result.add(m_Filter.output());
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown during 2. batch: \n" + ex);
    }
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
   * Compares the two datasets.
   *
   * @param data1	first dataset
   * @param data2	second dataset
   * @return		null if the same, otherwise first difference
   */
  protected String compareDatasets(Instances data1, Instances data2) {
    int		x;
    int		y;
    Comparable	c1;
    Comparable	c2;

    if (data1.numInstances() != data2.numInstances())
      System.err.println(
	getName() + " [compareDatasets] datasets differ in number of instances: "
	  + data1.numInstances() + " != " + data2.numInstances());
    if (data1.numAttributes() != data2.numAttributes())
      System.err.println(
	getName() + " [compareDatasets] datasets differ in number of attributes: "
	  + data1.numAttributes() + " != " + data2.numAttributes());
    for (x = 0; x < data1.numAttributes() && x < data2.numAttributes(); x++) {
      if (data1.attribute(x).type() != data2.attribute(x).type()) {
	System.err.println(
	  getName() + " [compareDatasets] datasets differ in attribute type at " + (x+1) + ": "
	    + Attribute.typeToString(data1.attribute(x).type()) + " != " + Attribute.typeToString(data2.attribute(x).type()));
      }
    }

    for (y = 0; y < data1.numInstances() && y < data2.numInstances(); y++) {
      for (x = 0; x < data1.numAttributes() && x < data2.numAttributes(); x++) {
	if (data1.attribute(x).type() == data2.attribute(x).type()) {
	  switch (data1.attribute(x).type()) {
	    case Attribute.STRING:
	      c1 = data1.instance(y).stringValue(x);
	      c2 = data2.instance(y).stringValue(x);
	      break;
	    case Attribute.RELATIONAL:
	      c1 = data1.instance(y).relationalValue(x).toString();
	      c2 = data2.instance(y).relationalValue(x).toString();
	      break;
	    default:
	      c1 = data1.instance(y).value(x);
	      c2 = data2.instance(y).value(x);
	      break;
	  }
	  if (c1.compareTo(c2) != 0)
	    return "Values differ in instance " + (y + 1) + " at attribute " + (x + 1) + ":\n"
	      + c1 + "\n"
	      + "!=\n"
	      + c2;
	}
      }
    }

    return null;
  }

  /**
   * Tests whether applying filter data changes input data.
   */
  public void testChangesInputData() {
    Instances 	result;
    String 	msg;
    Instances 	icopy;

    icopy = new Instances(m_Instances);

    // initializing filter
    try {
      m_Filter.setInputFormat(m_Instances);
    }
    catch (Exception e) {
      fail("Failed to use setInputFormat: " + e);
    }

    // 1st filtering
    try {
      result = Filter.useFilter(m_Instances, m_Filter);
      msg = compareDatasets(m_Instances, icopy);
      assertNotNull("Filtered data is null", result);
      assertNull("1st filtering changed input data: " + msg, msg);
    }
    catch (Exception e) {
      fail("Failed to apply filter for 1st time: " + e);
    }

    // 1st filtering
    try {
      result = Filter.useFilter(m_Instances, m_Filter);
      msg = compareDatasets(m_Instances, icopy);
      assertNotNull("Filtered data is null", result);
      assertNull("2nd filtering changed input data: " + msg, msg);
    }
    catch (Exception e) {
      fail("Failed to apply filter for 2nd time: " + e);
    }
  }
}
