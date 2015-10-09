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
 * Copyright (C) 2006 University of Waikato 
 */

package weka.associations;

import weka.core.Attribute;
import weka.core.CheckGOE;
import weka.core.CheckOptionHandler;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.CheckScheme.PostProcessor;
import weka.test.Regression;

import junit.framework.TestCase;

/**
 * Abstract Test class for Associators. Internally it uses the class
 * <code>CheckAssociator</code> to determine success or failure of the
 * tests. It follows basically the <code>testsPerClassType</code> method.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 *
 * @see CheckAssociator
 * @see CheckAssociator#testsPerClassType(int, boolean, boolean)
 * @see PostProcessor
 */
public abstract class AbstractAssociatorTest 
  extends TestCase {
  
  /** The Associator to be tested */
  protected Associator m_Associator;

  /** For testing the Associator */
  protected CheckAssociator m_Tester;

  /** whether Associator handles weighted instances */
  protected boolean m_weightedInstancesHandler;

  /** whether Associator handles multi-instance data */
  protected boolean m_multiInstanceHandler;

  /** the number of classes to test with testNClasses() 
   * @see #testNClasses() */
  protected int m_NClasses;

  /** whether to run CheckAssociator in DEBUG mode */
  protected boolean DEBUG = false;

  /** the attribute type with the lowest value */
  protected final static int FIRST_CLASSTYPE = Attribute.NUMERIC;

  /** the attribute type with the highest value */
  protected final static int LAST_CLASSTYPE = Attribute.RELATIONAL;
  
  /** wether Associator can predict nominal attributes (array index is attribute type of class) */
  protected boolean[] m_NominalPredictors;
  
  /** wether Associator can predict numeric attributes (array index is attribute type of class) */
  protected boolean[] m_NumericPredictors;
  
  /** wether Associator can predict string attributes (array index is attribute type of class) */
  protected boolean[] m_StringPredictors;
  
  /** wether Associator can predict date attributes (array index is attribute type of class) */
  protected boolean[] m_DatePredictors;
  
  /** wether Associator can predict relational attributes (array index is attribute type of class) */
  protected boolean[] m_RelationalPredictors;
  
  /** whether Associator handles missing values */
  protected boolean[] m_handleMissingPredictors;

  /** whether Associator handles class with only missing values */
  protected boolean[] m_handleMissingClass;
  
  /** the results of the regression tests */
  protected String[] m_RegressionResults;
  
  /** the OptionHandler tester */
  protected CheckOptionHandler m_OptionTester;
  
  /** for testing GOE stuff */
  protected CheckGOE m_GOETester;
  
  /**
   * Constructs the <code>AbstractAssociatorTest</code>. Called by subclasses.
   *
   * @param name the name of the test class
   */
  public AbstractAssociatorTest(String name) { 
    super(name); 
  }

  /**
   * returns a custom PostProcessor for the CheckClassifier datasets, currently
   * only null.
   * 
   * @return		a custom PostProcessor, if necessary
   * @see PostProcessor
   */
  protected PostProcessor getPostProcessor() {
    return null;
  }
  
  /**
   * configures the CheckAssociator instance used throughout the tests
   * 
   * @return	the fully configured CheckAssociator instance used for testing
   */
  protected CheckAssociator getTester() {
    CheckAssociator	result;
    
    result = new CheckAssociator();
    result.setSilent(true);
    result.setAssociator(m_Associator);
    result.setNumInstances(20);
    result.setDebug(DEBUG);
    result.setPostProcessor(getPostProcessor());
    
    return result;
  }
  
  /**
   * Configures the CheckOptionHandler used for testing the optionhandling.
   * Sets the Associator returned from the getAssociator() method.
   * 
   * @return	the fully configured CheckOptionHandler
   * @see	#getAssociator()
   */
  protected CheckOptionHandler getOptionTester() {
    CheckOptionHandler		result;
    
    result = new CheckOptionHandler();
    if (getAssociator() instanceof OptionHandler)
      result.setOptionHandler((OptionHandler) getAssociator());
    else
      result.setOptionHandler(null);
    result.setUserOptions(new String[0]);
    result.setSilent(true);
    
    return result;
  }
  
  /**
   * Configures the CheckGOE used for testing GOE stuff.
   * Sets the Associator returned from the getAssociator() method.
   * 
   * @return	the fully configured CheckGOE
   * @see	#getAssociator()
   */
  protected CheckGOE getGOETester() {
    CheckGOE		result;
    
    result = new CheckGOE();
    result.setObject(getAssociator());
    result.setSilent(true);
    
    return result;
  }
  
  /**
   * Called by JUnit before each test method. This implementation creates
   * the default Associator to test and loads a test set of Instances.
   *
   * @exception Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    m_Associator   = getAssociator();
    m_Tester       = getTester();
    m_OptionTester = getOptionTester();
    m_GOETester    = getGOETester();

    m_weightedInstancesHandler     = m_Tester.weightedInstancesHandler()[0];
    m_multiInstanceHandler         = m_Tester.multiInstanceHandler()[0];
    // LAST_CLASSTYPE+1 = no class attribute
    m_NominalPredictors            = new boolean[LAST_CLASSTYPE + 2];
    m_NumericPredictors            = new boolean[LAST_CLASSTYPE + 2];
    m_StringPredictors             = new boolean[LAST_CLASSTYPE + 2];
    m_DatePredictors               = new boolean[LAST_CLASSTYPE + 2];
    m_RelationalPredictors         = new boolean[LAST_CLASSTYPE + 2];
    m_handleMissingPredictors      = new boolean[LAST_CLASSTYPE + 2];
    m_handleMissingClass           = new boolean[LAST_CLASSTYPE + 2];
    m_RegressionResults            = new String[LAST_CLASSTYPE + 2];
    m_NClasses                     = 4;

    // initialize attributes
    checkAttributes(true,  false, false, false, false, false);
    checkAttributes(false, true,  false, false, false, false);
    checkAttributes(false, false, true,  false, false, false);
    checkAttributes(false, false, false, true,  false, false);
    checkAttributes(false, false, false, false, true,  false);
    
    // initialize missing values handling
    for (int i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE + 1; i++) {
      // does the scheme support this type of class at all?
      if (!canPredict(i))
        continue;
      
      // 20% missing
      m_handleMissingPredictors[i] = checkMissingPredictors(i, 20, false);
      if (i <= LAST_CLASSTYPE)
	m_handleMissingClass[i] = checkMissingClass(i, 20, false);
    }
  }

  /** Called by JUnit after each test method */
  protected void tearDown() {
    m_Associator   = null;
    m_Tester       = null;
    m_OptionTester = null;
    m_GOETester    = null;

    m_weightedInstancesHandler     = false;
    m_NominalPredictors            = null;
    m_NumericPredictors            = null;
    m_StringPredictors             = null;
    m_DatePredictors               = null;
    m_RelationalPredictors         = null;
    m_handleMissingPredictors      = null;
    m_handleMissingClass           = null;
    m_RegressionResults            = null;
    m_NClasses                     = 4;
  }

  /**
   * Used to create an instance of a specific Associator.
   *
   * @return a suitably configured <code>Associator</code> value
   */
  public abstract Associator getAssociator();

  /**
   * checks whether at least one attribute type can be handled with the
   * given class type
   *
   * @param type      the class type to check for
   * @return          true if at least one attribute type can be predicted with
   *                  the given class
   */
  protected boolean canPredict(int type) {
    return    m_NominalPredictors[type]
           || m_NumericPredictors[type]
           || m_StringPredictors[type]
           || m_DatePredictors[type]
           || m_RelationalPredictors[type];
  }

  /** 
   * returns a string for the class type
   * 
   * @param type        the class type
   * @return            the class type as string
   */
  protected String getClassTypeString(int type) {
    if (type == LAST_CLASSTYPE + 1)
      return "no";
    else
      return CheckAssociator.attributeTypeToString(type);
  }

  /**
   * tests whether the Associator can handle certain attributes and if not,
   * if the exception is OK
   *
   * @param nom         to check for nominal attributes
   * @param num         to check for numeric attributes
   * @param str         to check for string attributes
   * @param dat         to check for date attributes
   * @param rel         to check for relational attributes
   * @param allowFail   whether a junit fail can be executed
   * @see CheckAssociator#canPredict(boolean, boolean, boolean, boolean, boolean, boolean, int)
   * @see CheckAssociator#testsPerClassType(int, boolean, boolean)
   */
  protected void checkAttributes(boolean nom, boolean num, boolean str, 
                                 boolean dat, boolean rel,
                                 boolean allowFail) {
    boolean[]     result;
    String        att;
    int           i;
    int           type;

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
    
    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE + 1; i++) {
      if (i == LAST_CLASSTYPE + 1)
	type = CheckAssociator.NO_CLASS;
      else
	type = i;
      result = m_Tester.canPredict(nom, num, str, dat, rel, m_multiInstanceHandler, type);

      if (nom)
        m_NominalPredictors[i] = result[0];
      else if (num)
        m_NumericPredictors[i] = result[0];
      else if (str)
        m_StringPredictors[i] = result[0];
      else if (dat)
        m_DatePredictors[i] = result[0];
      else if (rel)
        m_RelationalPredictors[i] = result[0];

      if (!result[0] && !result[1] && allowFail)
        fail("Error handling " + att + " attributes (" + getClassTypeString(i) 
            + " class)!");
    }
  }

  /**
   * tests whether the Associator can handle different types of attributes and
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
   * tests whether the Associator handles instance weights correctly
   *
   * @see CheckAssociator#instanceWeights(boolean, boolean, boolean, boolean, boolean, boolean, int)
   * @see CheckAssociator#testsPerClassType(int, boolean, boolean)
   */
  public void testInstanceWeights() {
    boolean[]     result;
    int           i;
    int           type;
    
    if (m_weightedInstancesHandler) {
      for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE + 1; i++) {
        // does the Associator support this type of class at all?
        if (!canPredict(i))
          continue;

        if (i == LAST_CLASSTYPE + 1)
          type = CheckAssociator.NO_CLASS;
        else
          type = i;
        
        result = m_Tester.instanceWeights(
            m_NominalPredictors[i], 
            m_NumericPredictors[i], 
            m_StringPredictors[i], 
            m_DatePredictors[i], 
            m_RelationalPredictors[i], 
            m_multiInstanceHandler, 
            type);

        if (!result[0])
          System.err.println("Error handling instance weights (" + getClassTypeString(i) 
              + " class)!");
      }
    }
  }

  /**
   * tests whether Associator handles N classes
   *
   * @see CheckAssociator#canHandleNClasses(boolean, boolean, boolean, boolean, boolean, boolean, int)
   * @see CheckAssociator#testsPerClassType(int, boolean, boolean)
   * @see #m_NClasses
   */
  public void testNClasses() {
    boolean[]     result;

    if (!canPredict(Attribute.NOMINAL))
      return;

    result = m_Tester.canHandleNClasses(
        m_NominalPredictors[Attribute.NOMINAL],
        m_NumericPredictors[Attribute.NOMINAL],
        m_StringPredictors[Attribute.NOMINAL],
        m_DatePredictors[Attribute.NOMINAL],
        m_RelationalPredictors[Attribute.NOMINAL],
        m_multiInstanceHandler,
        m_NClasses);

    if (!result[0] && !result[1])
      fail("Error handling " + m_NClasses + " classes!");
  }

  /**
   * checks whether the Associator can handle the class attribute at a given
   * position (0-based index, -1 means last).
   *
   * @param type        the class type
   * @param position	the position of the class attribute (0-based, -1 means last)
   * @return            true if the Associator can handle it
   */
  protected boolean checkClassAsNthAttribute(int type, int position) {
    boolean[]     result;
    String	  indexStr;
    
    result = m_Tester.canHandleClassAsNthAttribute(
        m_NominalPredictors[type], 
        m_NumericPredictors[type], 
        m_StringPredictors[type], 
        m_DatePredictors[type], 
        m_RelationalPredictors[type], 
        m_multiInstanceHandler, 
        type,
        position);

    if (position == -1)
      indexStr = "last";
    else
      indexStr = (position + 1) + ".";
    
    if (!result[0] && !result[1])
      fail("Error handling class as " + indexStr + " attribute (" 
          + getClassTypeString(type) + " class)!");
    
    return result[0];
  }

  /**
   * Tests whether the Associator can handle class attributes as Nth
   * attribute. In case of multi-instance Associators it performs no tests,
   * since the multi-instance data has a fixed format (bagID,bag,class).
   *
   * @see CheckAssociator#canHandleClassAsNthAttribute(boolean, boolean, boolean, boolean, boolean, boolean, int, int)
   * @see CheckAssociator#testsPerClassType(int, boolean, boolean)
   */
  public void testClassAsNthAttribute() {
    int           i;
    
    // multi-Instance data has fixed format!
    if (m_multiInstanceHandler)
      return;
    
    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
      // does the Associator support this type of class at all?
      if (!canPredict(i))
        continue;
      
      // first attribute
      checkClassAsNthAttribute(i, 0);

      // second attribute
      checkClassAsNthAttribute(i, 1);
    }
  }

  /**
   * tests whether the Associator can handle zero training instances
   *
   * @see CheckAssociator#canHandleZeroTraining(boolean, boolean, boolean, boolean, boolean, boolean, int)
   * @see CheckAssociator#testsPerClassType(int, boolean, boolean)
   */
  public void testZeroTraining() {
    boolean[]     result;
    int           i;
    int           type;
    
    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE + 1; i++) {
      // does the Associator support this type of class at all?
      if (!canPredict(i))
        continue;
      
      if (i == LAST_CLASSTYPE + 1)
	type = CheckAssociator.NO_CLASS;
      else
	type = i;

      result = m_Tester.canHandleZeroTraining(
          m_NominalPredictors[i], 
          m_NumericPredictors[i], 
          m_StringPredictors[i], 
          m_DatePredictors[i], 
          m_RelationalPredictors[i], 
          m_multiInstanceHandler, 
          type);

      if (!result[0] && !result[1])
        fail("Error handling zero training instances (" + getClassTypeString(i) 
            + " class)!");
    }
  }

  /**
   * checks whether the Associator can handle the given percentage of
   * missing predictors
   *
   * @param type        the class type
   * @param percent     the percentage of missing predictors
   * @param allowFail	if true a fail statement may be executed
   * @return            true if the Associator can handle it
   */
  protected boolean checkMissingPredictors(int type, int percent, boolean allowFail) {
    boolean[]     result;
    int           classType;
    
    if (type == LAST_CLASSTYPE + 1)
      classType = CheckAssociator.NO_CLASS;
    else
      classType = type;

    result = m_Tester.canHandleMissing(
        m_NominalPredictors[type], 
        m_NumericPredictors[type], 
        m_StringPredictors[type], 
        m_DatePredictors[type], 
        m_RelationalPredictors[type], 
        m_multiInstanceHandler, 
        classType,
        true,
        false,
        percent);

    if (allowFail) {
      if (!result[0] && !result[1])
	fail("Error handling " + percent + "% missing predictors (" 
	    + getClassTypeString(type) + " class)!");
    }
    
    return result[0];
  }

  /**
   * tests whether the Associator can handle missing predictors (20% and 100%)
   *
   * @see CheckAssociator#canHandleMissing(boolean, boolean, boolean, boolean, boolean, boolean, int, boolean, boolean, int)
   * @see CheckAssociator#testsPerClassType(int, boolean, boolean)
   */
  public void testMissingPredictors() {
    int           i;
    
    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE + 1; i++) {
      // does the Associator support this type of class at all?
      if (!canPredict(i))
        continue;
      
      // 20% missing
      checkMissingPredictors(i, 20, true);

      // 100% missing
      if (m_handleMissingPredictors[i])
        checkMissingPredictors(i, 100, true);
    }
  }

  /**
   * checks whether the Associator can handle the given percentage of
   * missing class labels
   *
   * @param type        the class type
   * @param percent     the percentage of missing class labels
   * @param allowFail	if true a fail statement may be executed
   * @return            true if the Associator can handle it
   */
  protected boolean checkMissingClass(int type, int percent, boolean allowFail) {
    boolean[]     result;
    
    result = m_Tester.canHandleMissing(
        m_NominalPredictors[type], 
        m_NumericPredictors[type], 
        m_StringPredictors[type], 
        m_DatePredictors[type], 
        m_RelationalPredictors[type], 
        m_multiInstanceHandler, 
        type,
        false,
        true,
        percent);

    if (allowFail) {
      if (!result[0] && !result[1])
	fail("Error handling " + percent + "% missing class labels (" 
	    + getClassTypeString(type) + " class)!");
    }
    
    return result[0];
  }

  /**
   * tests whether the Associator can handle missing class values (20% and
   * 100%)
   *
   * @see CheckAssociator#canHandleMissing(boolean, boolean, boolean, boolean, boolean, boolean, int, boolean, boolean, int)
   * @see CheckAssociator#testsPerClassType(int, boolean, boolean)
   */
  public void testMissingClass() {
    int           i;
    
    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
      // does the Associator support this type of class at all?
      if (!canPredict(i))
        continue;
      
      // 20% missing
      checkMissingClass(i, 20, true);

      // 100% missing
      if (m_handleMissingClass[i])
        checkMissingClass(i, 100, true);
    }
  }

  /**
   * tests whether the Associator correctly initializes in the
   * buildAssociator method
   *
   * @see CheckAssociator#correctBuildInitialisation(boolean, boolean, boolean, boolean, boolean, boolean, int)
   * @see CheckAssociator#testsPerClassType(int, boolean, boolean)
   */
  public void testBuildInitialization() {
    boolean[]     result;
    int           i;
    int           type;
    
    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE + 1; i++) {
      // does the Associator support this type of class at all?
      if (!canPredict(i))
        continue;
      
      if (i == LAST_CLASSTYPE + 1)
	type = CheckAssociator.NO_CLASS;
      else
	type = i;

      result = m_Tester.correctBuildInitialisation(
          m_NominalPredictors[i], 
          m_NumericPredictors[i], 
          m_StringPredictors[i], 
          m_DatePredictors[i], 
          m_RelationalPredictors[i], 
          m_multiInstanceHandler, 
          type);

      if (!result[0] && !result[1])
        fail("Incorrect build initialization (" + getClassTypeString(i) 
            + " class)!");
    }
  }

  /**
   * tests whether the Associator alters the training set during training.
   *
   * @see CheckAssociator#datasetIntegrity(boolean, boolean, boolean, boolean, boolean, boolean, int, boolean, boolean)
   * @see CheckAssociator#testsPerClassType(int, boolean, boolean)
   */
  public void testDatasetIntegrity() {
    boolean[]     result;
    int           i;
    int           type;
    
    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE + 1; i++) {
      // does the Associator support this type of class at all?
      if (!canPredict(i))
        continue;
      
      if (i == LAST_CLASSTYPE + 1)
	type = CheckAssociator.NO_CLASS;
      else
	type = i;

      result = m_Tester.datasetIntegrity(
          m_NominalPredictors[i], 
          m_NumericPredictors[i], 
          m_StringPredictors[i], 
          m_DatePredictors[i], 
          m_RelationalPredictors[i], 
          m_multiInstanceHandler, 
          type,
          m_handleMissingPredictors[i],
          m_handleMissingClass[i]);

      if (!result[0] && !result[1])
        fail("Training set is altered during training (" 
            + getClassTypeString(i) + " class)!");
    }
  }

  /**
   * Builds a model using the current Associator using the given data and 
   * returns the produced output.
   * TODO: unified rules as output instead of toString() result???
   *
   * @param data 	the instances to test the Associator on
   * @return 		a String containing the output of the Associator.
   * @throws Exception	if something goes wrong
   */
  protected String useAssociator(Instances data) throws Exception {
    Associator associator = null;
    
    try {
      associator = AbstractAssociator.makeCopy(m_Associator);
    } 
    catch (Exception e) {
      e.printStackTrace();
      fail("Problem setting up to use Associator: " + e);
    }

    associator.buildAssociations(data);
    
    return associator.toString();
  }
  
  /**
   * Provides a hook for derived classes to further modify the data. Currently,
   * the data is just passed through.
   * 
   * @param data	the data to process
   * @return		the processed data
   */
  protected Instances process(Instances data) {
    return data;
  }

  /**
   * Runs a regression test -- this checks that the output of the tested
   * object matches that in a reference version. When this test is
   * run without any pre-existing reference output, the reference version
   * is created.
   * 
   * @throws Exception 	if something goes wrong
   */
  public void testRegression() throws Exception {
    int		i;
    boolean	succeeded;
    Regression 	reg;
    Instances   train;
    int		type;
    
    // don't bother if not working correctly
    if (m_Tester.hasClasspathProblems())
      return;
    
    reg = new Regression(this.getClass());
    succeeded = false;
    train = null;
    
    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE + 1; i++) {
      // does the Associator support this type of class at all?
      if (!canPredict(i))
        continue;
        
      if (i == LAST_CLASSTYPE + 1)
	type = CheckAssociator.NO_CLASS;
      else
	type = i;

      train = m_Tester.makeTestDataset(
          42, m_Tester.getNumInstances(), 
  	  m_NominalPredictors[i] ? 2 : 0,
  	  m_NumericPredictors[i] ? 1 : 0, 
          m_StringPredictors[i] ? 1 : 0,
          m_DatePredictors[i] ? 1 : 0,
          m_RelationalPredictors[i] ? 1 : 0,
          2, 
          type,
          m_multiInstanceHandler);
  
      try {
        m_RegressionResults[i] = useAssociator(train);
        succeeded = true;
        reg.println(m_RegressionResults[i]);
      }
      catch (Exception e) {
	String msg = e.getMessage().toLowerCase();
	if (msg.indexOf("not in classpath") > -1)
	  return;

	m_RegressionResults[i] = null;
      }
    }
    
    if (!succeeded) {
      fail("Problem during regression testing: no successful predictions for any class type");
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
