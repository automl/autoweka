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

package weka.clusterers;

import weka.core.CheckGOE;
import weka.core.CheckOptionHandler;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.test.Regression;

import junit.framework.TestCase;

/**
 * Abstract Test class for Clusterers. Internally it uses the class
 * <code>CheckClusterer</code> to determine success or failure of the
 * tests. It follows basically the <code>runTests</code> method.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 *
 * @see CheckClusterer
 * @see CheckClusterer#runTests(boolean, boolean, boolean)
 */
public abstract class AbstractClustererTest 
  extends TestCase {

  /** The clusterer to be tested */
  protected Clusterer m_Clusterer;

  /** For testing the clusterer */
  protected CheckClusterer m_Tester;
  
  /** whether classifier is updateable */
  protected boolean m_updateableClusterer;

  /** whether clusterer handles weighted instances */
  protected boolean m_weightedInstancesHandler;

  /** whether clusterer handles multi-instance data */
  protected boolean m_multiInstanceHandler;

  /** whether to run CheckClusterer in DEBUG mode */
  protected boolean DEBUG = false;
  
  /** wether clusterer can predict nominal attributes (array index is attribute type of class) */
  protected boolean m_NominalPredictors;
  
  /** wether clusterer can predict numeric attributes (array index is attribute type of class) */
  protected boolean m_NumericPredictors;
  
  /** wether clusterer can predict string attributes (array index is attribute type of class) */
  protected boolean m_StringPredictors;
  
  /** wether clusterer can predict date attributes (array index is attribute type of class) */
  protected boolean m_DatePredictors;
  
  /** wether clusterer can predict relational attributes (array index is attribute type of class) */
  protected boolean m_RelationalPredictors;
  
  /** whether clusterer handles missing values */
  protected boolean m_handleMissingPredictors;
  
  /** the result of the regression test */
  protected String m_RegressionResults;
  
  /** the OptionHandler tester */
  protected CheckOptionHandler m_OptionTester;
  
  /** for testing GOE stuff */
  protected CheckGOE m_GOETester;
  
  /**
   * Constructs the <code>AbstractClustererTest</code>. Called by subclasses.
   *
   * @param name the name of the test class
   */
  public AbstractClustererTest(String name) { 
    super(name); 
  }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default clusterer to test and loads a test set of Instances.
   *
   * @exception Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    m_Clusterer = getClusterer();
    m_Tester    = new CheckClusterer();
    m_Tester.setSilent(true);
    m_Tester.setClusterer(m_Clusterer);
    m_Tester.setNumInstances(20);
    m_Tester.setDebug(DEBUG);
    m_OptionTester = getOptionTester();
    m_GOETester    = getGOETester();

    m_updateableClusterer         = m_Tester.updateableClusterer()[0];
    m_weightedInstancesHandler     = m_Tester.weightedInstancesHandler()[0];
    m_multiInstanceHandler         = m_Tester.multiInstanceHandler()[0];
    m_NominalPredictors            = false;
    m_NumericPredictors            = false;
    m_StringPredictors             = false;
    m_DatePredictors               = false;
    m_RelationalPredictors         = false;
    m_handleMissingPredictors      = false;
    m_RegressionResults            = "";

    // initialize attributes
    checkAttributes(true,  false, false, false, false, false);
    checkAttributes(false, true,  false, false, false, false);
    checkAttributes(false, false, true,  false, false, false);
    checkAttributes(false, false, false, true,  false, false);
    checkAttributes(false, false, false, false, true,  false);

    // 20% missing values
    m_handleMissingPredictors = checkMissingPredictors(20, false);
  }

  /** Called by JUnit after each test method */
  protected void tearDown() {
    m_Clusterer    = null;
    m_Tester       = null;
    m_OptionTester = null;
    m_GOETester    = null;

    m_updateableClusterer          = false;
    m_weightedInstancesHandler     = false;
    m_NominalPredictors            = false;
    m_NumericPredictors            = false;
    m_StringPredictors             = false;
    m_DatePredictors               = false;
    m_RelationalPredictors         = false;
    m_handleMissingPredictors      = false;
    m_RegressionResults            = "";
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
    if (getClusterer() instanceof OptionHandler)
      result.setOptionHandler((OptionHandler) getClusterer());
    else
      result.setOptionHandler(null);
    result.setUserOptions(new String[0]);
    result.setSilent(true);
    
    return result;
  }
  
  /**
   * Configures the CheckGOE used for testing GOE stuff.
   * Sets the Clusterer returned from the getClusterer() method.
   * 
   * @return	the fully configured CheckGOE
   * @see	#getClusterer()
   */
  protected CheckGOE getGOETester() {
    CheckGOE		result;
    
    result = new CheckGOE();
    result.setObject(getClusterer());
    result.setSilent(true);
    
    return result;
  }

  /**
   * Used to create an instance of a specific clusterer.
   *
   * @return a suitably configured <code>Clusterer</code> value
   */
  public abstract Clusterer getClusterer();

  /**
   * checks whether at least one attribute type can be handled
   * 
   * @return            true if at least one attribute type can be handled
   */
  protected boolean canPredict() {
    return    m_NominalPredictors
           || m_NumericPredictors
           || m_StringPredictors
           || m_DatePredictors
           || m_RelationalPredictors;
  }

  /**
   * tests whether the clusterer can handle certain attributes and if not,
   * if the exception is OK
   *
   * @param nom         to check for nominal attributes
   * @param num         to check for numeric attributes
   * @param str         to check for string attributes
   * @param dat         to check for date attributes
   * @param rel         to check for relational attributes
   * @param allowFail   whether a junit fail can be executed
   * @see CheckClusterer#canPredict(boolean, boolean, boolean, boolean, boolean, boolean)
   * @see CheckClusterer#runTests(boolean, boolean, boolean)
   */
  protected void checkAttributes(boolean nom, boolean num, boolean str, 
                                 boolean dat, boolean rel,
                                 boolean allowFail) {
    boolean[]     result;
    String        att;

    // determine text for type of attributes
    att = "";
    if (nom)
      att = "nominal";
    else if (num)
      att = "numeric";
    else if (str)
      att = "string";
    else if (dat)
      att = "date";
    else if (rel)
      att = "relational";
    
    result = m_Tester.canPredict(nom, num, str, dat, rel, m_multiInstanceHandler);
    if (nom)
      m_NominalPredictors = result[0];
    else if (num)
      m_NumericPredictors = result[0];
    else if (str)
      m_StringPredictors = result[0];
    else if (dat)
      m_DatePredictors = result[0];
    else if (rel)
      m_RelationalPredictors = result[0];

    if (!result[0] && !result[1] && allowFail)
      fail("Error handling " + att + " attributes!");
  }

  /**
   * tests whether the clusterer can handle different types of attributes and
   * if not, if the exception is OK
   *
   * @see #checkAttributes(boolean, boolean, boolean, boolean, boolean, boolean)
   */
  public void testAttributes() {
    // nominal
    checkAttributes(true,  false, false, false, false, true);
    // numeric
    checkAttributes(false, true,  false, false, false, true);
    // string
    checkAttributes(false, false, true,  false, false, true);
    // date
    checkAttributes(false, false, false, true,  false, true);
    // relational
    if (!m_multiInstanceHandler)
      checkAttributes(false, false, false, false, true,  true);
  }

  /**
   * tests whether the scheme declares a serialVersionUID.
   */
  public void testSerialVersionUID() {
    boolean[]     result;

    result = m_Tester.declaresSerialVersionUID();

    if (!result[0])
      fail("Doesn't declare serialVersionUID!");
  }

  /**
   * tests whether the clusterer handles instance weights correctly
   *
   * @see CheckClusterer#instanceWeights(boolean, boolean, boolean, boolean, boolean, boolean)
   * @see CheckClusterer#runTests(boolean, boolean, boolean)
   */
  public void testInstanceWeights() {
    boolean[]     result;
    
    if (m_weightedInstancesHandler) {
      if (!canPredict())
	return;
      
      result = m_Tester.instanceWeights(
          m_NominalPredictors,
          m_NumericPredictors,
          m_StringPredictors,
          m_DatePredictors, 
          m_RelationalPredictors, 
          m_multiInstanceHandler);

      if (!result[0])
        System.err.println("Error handling instance weights!");
    }
  }

  /**
   * tests whether the clusterer can handle zero training instances
   *
   * @see CheckClusterer#canHandleZeroTraining(boolean, boolean, boolean, boolean, boolean, boolean)
   * @see CheckClusterer#runTests(boolean, boolean, boolean)
   */
  public void testZeroTraining() {
    boolean[]     result;
    
    if (!canPredict())
      return;
    
    result = m_Tester.canHandleZeroTraining(
        m_NominalPredictors, 
        m_NumericPredictors, 
        m_StringPredictors, 
        m_DatePredictors, 
        m_RelationalPredictors, 
        m_multiInstanceHandler);

    if (!result[0] && !result[1])
      fail("Error handling zero training instances!");
  }

  /**
   * checks whether the clusterer can handle the given percentage of
   * missing predictors
   *
   * @param percent     the percentage of missing predictors
   * @param allowFail	if true a fail statement may be executed
   * @return            true if the clusterer can handle it
   */
  protected boolean checkMissingPredictors(int percent, boolean allowFail) {
    boolean[]     result;
    
    result = m_Tester.canHandleMissing(
        m_NominalPredictors, 
        m_NumericPredictors, 
        m_StringPredictors, 
        m_DatePredictors, 
        m_RelationalPredictors, 
        m_multiInstanceHandler, 
        true,
        percent);

    if (allowFail) {
      if (!result[0] && !result[1])
	fail("Error handling " + percent + "% missing predictors!");
    }
    
    return result[0];
  }

  /**
   * tests whether the clusterer can handle missing predictors (20% and 100%)
   *
   * @see CheckClusterer#canHandleMissing(boolean, boolean, boolean, boolean, boolean, boolean, boolean, int)
   * @see CheckClusterer#runTests(boolean, boolean, boolean)
   */
  public void testMissingPredictors() {
    if (!canPredict())
      return;
    
    // 20% missing
    checkMissingPredictors(20, true);

    // 100% missing
    if (m_handleMissingPredictors)
      checkMissingPredictors(100, true);
  }

  /**
   * tests whether the clusterer correctly initializes in the
   * buildClusterer method
   *
   * @see CheckClusterer#correctBuildInitialisation(boolean, boolean, boolean, boolean, boolean, boolean)
   * @see CheckClusterer#runTests(boolean, boolean, boolean)
   */
  public void testBuildInitialization() {
    boolean[]     result;
    
    if (!canPredict())
      return;
    
    result = m_Tester.correctBuildInitialisation(
        m_NominalPredictors, 
        m_NumericPredictors, 
        m_StringPredictors, 
        m_DatePredictors, 
        m_RelationalPredictors, 
        m_multiInstanceHandler);

    if (!result[0] && !result[1])
      fail("Incorrect build initialization!");
  }

  /**
   * tests whether the clusterer alters the training set during training.
   *
   * @see CheckClusterer#datasetIntegrity(boolean, boolean, boolean, boolean, boolean, boolean, boolean)
   * @see CheckClusterer#runTests(boolean, boolean, boolean)
   */
  public void testDatasetIntegrity() {
    boolean[]     result;
  
    if (!canPredict())
      return;
    
    result = m_Tester.datasetIntegrity(
        m_NominalPredictors, 
        m_NumericPredictors, 
        m_StringPredictors, 
        m_DatePredictors, 
        m_RelationalPredictors, 
        m_multiInstanceHandler, 
        m_handleMissingPredictors);

    if (!result[0] && !result[1])
      fail("Training set is altered during training!");
  }

  /**
   * tests whether the classifier produces the same model when trained
   * incrementally as when batch trained.
   *
   * @see CheckClusterer#updatingEquality(boolean, boolean, boolean, boolean, boolean, boolean)
   * @see CheckClusterer#runTests(boolean, boolean, boolean)
   */
  public void testUpdatingEquality() {
    boolean[]     result;
    
    if (m_updateableClusterer) {
        result = m_Tester.updatingEquality(
            m_NominalPredictors, 
            m_NumericPredictors, 
            m_StringPredictors, 
            m_DatePredictors, 
            m_RelationalPredictors, 
            m_multiInstanceHandler);

        if (!result[0])
          System.err.println(
              "Incremental training does not produce same result as batch training!");
    }
  }

  /**
   * Builds a model using the current Clusterer using the given data and 
   * returns the produced cluster assignments.
   * 
   * @param data 	the instances to test the Clusterer on
   * @return 		a String containing the cluster assignments.
   */
  protected String useClusterer(Instances data) throws Exception {
    String	result;
    Clusterer 	clusterer;
    int		i;
    double	cluster;
    
    try {
      clusterer = AbstractClusterer.makeCopy(m_Clusterer);
    } 
    catch (Exception e) {
      clusterer = null;
      e.printStackTrace();
      fail("Problem setting up to use Clusterer: " + e);
    }

    clusterer.buildClusterer(data);
    
    // generate result
    result = "";
    for (i = 0; i < data.numInstances(); i++) {
      if (i > 0)
	result += "\n";
      try {
	cluster = clusterer.clusterInstance(data.instance(i));
	result += "" + (i+1) + ": " + cluster;
      }
      catch (Exception e) {
	result += "" + (i+1) + ": " + e.toString();
      }
    }
    
    return result;
  }

  /**
   * Runs a regression test -- this checks that the output of the tested
   * object matches that in a reference version. When this test is
   * run without any pre-existing reference output, the reference version
   * is created.
   */
  public void testRegression() throws Exception {
    boolean	succeeded;
    Regression 	reg;
    Instances   train;
    
    // don't bother if not working correctly
    if (m_Tester.hasClasspathProblems())
      return;
    
    reg       = new Regression(this.getClass());
    train     = null;
    succeeded = false;
    
    train = m_Tester.makeTestDataset(
	42, m_Tester.getNumInstances(), 
	m_NominalPredictors ? 2 : 0,
	m_NumericPredictors ? 1 : 0, 
	m_StringPredictors ? 1 : 0,
	m_DatePredictors ? 1 : 0,
	m_RelationalPredictors ? 1 : 0,
	m_multiInstanceHandler);
    
    try {
      m_RegressionResults = useClusterer(train);
      succeeded = true;
      reg.println(m_RegressionResults);
    }
    catch (Exception e) {
      String msg = e.getMessage().toLowerCase();
      if (msg.indexOf("not in classpath") > -1)
	return;
      
      m_RegressionResults = null;
    }
    
    if (!succeeded) {
      fail("Problem during regression testing: no successful output generated");
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
}
