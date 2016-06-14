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
 * Copyright (C) 2002-2006 University of Waikato 
 */

package weka.classifiers;

import java.util.ArrayList;

import junit.framework.TestCase;
import weka.classifiers.evaluation.EvaluationUtils;
import weka.classifiers.evaluation.Prediction;
import weka.core.Attribute;
import weka.core.CheckGOE;
import weka.core.CheckOptionHandler;
import weka.core.CheckScheme.PostProcessor;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.test.Regression;

/**
 * Abstract Test class for Classifiers. Internally it uses the class
 * <code>CheckClassifier</code> to determine success or failure of the tests. It
 * follows basically the <code>testsPerClassType</code> method.
 * 
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10222 $
 * 
 * @see CheckClassifier
 * @see CheckClassifier#testsPerClassType(int, boolean, boolean, boolean)
 * @see PostProcessor
 */
public abstract class AbstractClassifierTest extends TestCase {

  /**
   * a class for postprocessing the test-data: all values of numeric attributs
   * are replaced with their absolute value
   */
  public static class AbsPostProcessor extends PostProcessor {

    /**
     * initializes the PostProcessor
     */
    public AbsPostProcessor() {
      super();
    }

    /**
     * Provides a hook for derived classes to further modify the data.
     * Currently, the data is just passed through.
     * 
     * @param data the data to process
     * @return the processed data
     */
    @Override
    public Instances process(Instances data) {
      Instances result;
      int i;
      int n;

      result = super.process(data);

      for (i = 0; i < result.numAttributes(); i++) {
        if (i == result.classIndex()) {
          continue;
        }
        if (!result.attribute(i).isNumeric()) {
          continue;
        }

        for (n = 0; n < result.numInstances(); n++) {
          result.instance(n).setValue(i, Math.abs(result.instance(n).value(i)));
        }
      }

      return result;
    }
  }

  /** The classifier to be tested */
  protected Classifier m_Classifier;

  /** For testing the classifier */
  protected CheckClassifier m_Tester;

  /** whether classifier is updateable */
  protected boolean m_updateableClassifier;

  /** whether classifier handles weighted instances */
  protected boolean m_weightedInstancesHandler;

  /** whether classifier handles multi-instance data */
  protected boolean m_multiInstanceHandler;

  /**
   * the number of classes to test with testNClasses()
   * 
   * @see #testNClasses()
   */
  protected int m_NClasses;

  /** whether to run CheckClassifier in DEBUG mode */
  protected boolean DEBUG = false;

  /** the attribute type with the lowest value */
  protected final static int FIRST_CLASSTYPE = Attribute.NUMERIC;

  /** the attribute type with the highest value */
  protected final static int LAST_CLASSTYPE = Attribute.RELATIONAL;

  /**
   * wether classifier can predict nominal attributes (array index is attribute
   * type of class)
   */
  protected boolean[] m_NominalPredictors;

  /**
   * wether classifier can predict numeric attributes (array index is attribute
   * type of class)
   */
  protected boolean[] m_NumericPredictors;

  /**
   * wether classifier can predict string attributes (array index is attribute
   * type of class)
   */
  protected boolean[] m_StringPredictors;

  /**
   * wether classifier can predict date attributes (array index is attribute
   * type of class)
   */
  protected boolean[] m_DatePredictors;

  /**
   * wether classifier can predict relational attributes (array index is
   * attribute type of class)
   */
  protected boolean[] m_RelationalPredictors;

  /** whether classifier handles missing values */
  protected boolean[] m_handleMissingPredictors;

  /** whether classifier handles class with only missing values */
  protected boolean[] m_handleMissingClass;

  /** whether classifier handles class as first attribute */
  protected boolean[] m_handleClassAsFirstAttribute;

  /** whether classifier handles class as second attribute */
  protected boolean[] m_handleClassAsSecondAttribute;

  /** the results of the regression tests */
  protected ArrayList<Prediction>[] m_RegressionResults;

  /** the OptionHandler tester */
  protected CheckOptionHandler m_OptionTester;

  /** for testing GOE stuff */
  protected CheckGOE m_GOETester;

  /**
   * Constructs the <code>AbstractClassifierTest</code>. Called by subclasses.
   * 
   * @param name the name of the test class
   */
  public AbstractClassifierTest(String name) {
    super(name);
  }

  /**
   * returns a custom PostProcessor for the CheckClassifier datasets, currently
   * only null.
   * 
   * @return a custom PostProcessor, if necessary
   * @see PostProcessor
   */
  protected PostProcessor getPostProcessor() {
    return null;
  }

  /**
   * configures the CheckClassifier instance used throughout the tests
   * 
   * @return the fully configured CheckClassifier instance used for testing
   */
  protected CheckClassifier getTester() {
    CheckClassifier result;

    result = new CheckClassifier();
    result.setSilent(true);
    result.setClassifier(m_Classifier);
    result.setNumInstances(20);
    result.setDebug(DEBUG);
    result.setPostProcessor(getPostProcessor());

    return result;
  }

  /**
   * Configures the CheckOptionHandler uses for testing the optionhandling. Sets
   * the classifier return from the getClassifier() method.
   * 
   * @return the fully configured CheckOptionHandler
   * @see #getClassifier()
   */
  protected CheckOptionHandler getOptionTester() {
    CheckOptionHandler result;

    result = new CheckOptionHandler();
    result.setOptionHandler((OptionHandler) getClassifier());
    result.setUserOptions(new String[0]);
    result.setSilent(true);

    return result;
  }

  /**
   * Configures the CheckGOE used for testing GOE stuff. Sets the Classifier
   * returned from the getClassifier() method.
   * 
   * @return the fully configured CheckGOE
   * @see #getClassifier()
   */
  protected CheckGOE getGOETester() {
    CheckGOE result;

    result = new CheckGOE();
    result.setObject(getClassifier());
    result.setSilent(true);

    return result;
  }

  /**
   * Called by JUnit before each test method. This implementation creates the
   * default classifier to test and loads a test set of Instances.
   * 
   * @exception Exception if an error occurs reading the example instances.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void setUp() throws Exception {
    m_Classifier = getClassifier();
    m_Tester = getTester();
    m_OptionTester = getOptionTester();
    m_GOETester = getGOETester();

    m_updateableClassifier = m_Tester.updateableClassifier()[0];
    m_weightedInstancesHandler = m_Tester.weightedInstancesHandler()[0];
    m_multiInstanceHandler = m_Tester.multiInstanceHandler()[0];
    m_NominalPredictors = new boolean[LAST_CLASSTYPE + 1];
    m_NumericPredictors = new boolean[LAST_CLASSTYPE + 1];
    m_StringPredictors = new boolean[LAST_CLASSTYPE + 1];
    m_DatePredictors = new boolean[LAST_CLASSTYPE + 1];
    m_RelationalPredictors = new boolean[LAST_CLASSTYPE + 1];
    m_handleMissingPredictors = new boolean[LAST_CLASSTYPE + 1];
    m_handleMissingClass = new boolean[LAST_CLASSTYPE + 1];
    m_handleClassAsFirstAttribute = new boolean[LAST_CLASSTYPE + 1];
    m_handleClassAsSecondAttribute = new boolean[LAST_CLASSTYPE + 1];
    m_RegressionResults = new ArrayList[LAST_CLASSTYPE + 1];
    m_NClasses = 4;

    // initialize attributes
    checkAttributes(true, false, false, false, false, false);
    checkAttributes(false, true, false, false, false, false);
    checkAttributes(false, false, true, false, false, false);
    checkAttributes(false, false, false, true, false, false);
    checkAttributes(false, false, false, false, true, false);

    // initialize missing values handling
    for (int i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
      // does the scheme support this type of class at all?
      if (!canPredict(i)) {
        continue;
      }

      // 20% missing
      m_handleMissingPredictors[i] = checkMissingPredictors(i, 20, false);
      m_handleMissingClass[i] = checkMissingClass(i, 20, false);
    }
  }

  /** Called by JUnit after each test method */
  @SuppressWarnings("unchecked")
  @Override
  protected void tearDown() {
    m_Classifier = null;
    m_Tester = null;
    m_OptionTester = null;
    m_GOETester = null;

    m_updateableClassifier = false;
    m_weightedInstancesHandler = false;
    m_NominalPredictors = new boolean[LAST_CLASSTYPE + 1];
    m_NumericPredictors = new boolean[LAST_CLASSTYPE + 1];
    m_StringPredictors = new boolean[LAST_CLASSTYPE + 1];
    m_DatePredictors = new boolean[LAST_CLASSTYPE + 1];
    m_RelationalPredictors = new boolean[LAST_CLASSTYPE + 1];
    m_handleMissingPredictors = new boolean[LAST_CLASSTYPE + 1];
    m_handleMissingClass = new boolean[LAST_CLASSTYPE + 1];
    m_handleClassAsFirstAttribute = new boolean[LAST_CLASSTYPE + 1];
    m_handleClassAsSecondAttribute = new boolean[LAST_CLASSTYPE + 1];
    m_RegressionResults = new ArrayList[LAST_CLASSTYPE + 1];
    m_NClasses = 4;
  }

  /**
   * Used to create an instance of a specific classifier.
   * 
   * @return a suitably configured <code>Classifier</code> value
   */
  public abstract Classifier getClassifier();

  /**
   * checks whether at least one attribute type can be handled with the given
   * class type
   * 
   * @param type the class type to check for
   * @return true if at least one attribute type can be predicted with the given
   *         class
   */
  protected boolean canPredict(int type) {
    return m_NominalPredictors[type] || m_NumericPredictors[type]
      || m_StringPredictors[type] || m_DatePredictors[type]
      || m_RelationalPredictors[type];
  }

  /**
   * returns a string for the class type
   * 
   * @param type the class type
   * @return the class type as string
   */
  protected String getClassTypeString(int type) {
    return CheckClassifier.attributeTypeToString(type);
  }

  /**
   * tests whether the classifier can handle certain attributes and if not, if
   * the exception is OK
   * 
   * @param nom to check for nominal attributes
   * @param num to check for numeric attributes
   * @param str to check for string attributes
   * @param dat to check for date attributes
   * @param rel to check for relational attributes
   * @param allowFail whether a junit fail can be executed
   * @see CheckClassifier#canPredict(boolean, boolean, boolean, boolean,
   *      boolean, boolean, int)
   * @see CheckClassifier#testsPerClassType(int, boolean, boolean, boolean)
   */
  protected void checkAttributes(boolean nom, boolean num, boolean str,
    boolean dat, boolean rel, boolean allowFail) {
    boolean[] result;
    String att;
    int i;

    // determine text for type of attributes
    att = "";
    if (nom) {
      att = "nominal";
    } else if (num) {
      att = "numeric";
    } else if (str) {
      att = "string";
    } else if (dat) {
      att = "date";
    } else if (rel) {
      att = "relational";
    }

    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
      result = m_Tester.canPredict(nom, num, str, dat, rel,
        m_multiInstanceHandler, i);
      if (nom) {
        m_NominalPredictors[i] = result[0];
      } else if (num) {
        m_NumericPredictors[i] = result[0];
      } else if (str) {
        m_StringPredictors[i] = result[0];
      } else if (dat) {
        m_DatePredictors[i] = result[0];
      } else if (rel) {
        m_RelationalPredictors[i] = result[0];
      }

      if (!result[0] && !result[1] && allowFail) {
        fail("Error handling " + att + " attributes (" + getClassTypeString(i)
          + " class)!");
      }
    }
  }

  /**
   * tests whether the toString method of the classifier works even though the
   * classifier hasn't been built yet.
   */
  public void testToString() {
    boolean[] result;

    result = m_Tester.testToString();

    if (!result[0]) {
      fail("Error in toString() method!");
    }
  }

  /**
   * tests whether the scheme declares a serialVersionUID.
   */
  public void testSerialVersionUID() {
    boolean[] result;

    result = m_Tester.declaresSerialVersionUID();

    if (!result[0]) {
      fail("Doesn't declare serialVersionUID!");
    }
  }

  /**
   * tests whether the classifier can handle different types of attributes and
   * if not, if the exception is OK
   * 
   * @see #checkAttributes(boolean, boolean, boolean, boolean, boolean, boolean,
   *      boolean)
   */
  public void testAttributes() {
    // nominal
    checkAttributes(true, false, false, false, false, true);
    // numeric
    checkAttributes(false, true, false, false, false, true);
    // string
    checkAttributes(false, false, true, false, false, true);
    // date
    checkAttributes(false, false, false, true, false, true);
    // relational
    if (!m_multiInstanceHandler) {
      checkAttributes(false, false, false, false, true, true);
    }
  }

  /**
   * tests whether the classifier handles instance weights correctly
   * 
   * @see CheckClassifier#instanceWeights(boolean, boolean, boolean, boolean,
   *      boolean, boolean, int)
   * @see CheckClassifier#testsPerClassType(int, boolean, boolean, boolean)
   */
  public void testInstanceWeights() {
    boolean[] result;
    int i;

    if (m_weightedInstancesHandler) {
      for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
        // does the classifier support this type of class at all?
        if (!canPredict(i)) {
          continue;
        }

        result = m_Tester.instanceWeights(m_NominalPredictors[i],
          m_NumericPredictors[i], m_StringPredictors[i], m_DatePredictors[i],
          m_RelationalPredictors[i], m_multiInstanceHandler, i);

        if (!result[0]) {
          System.err.println("Error handling instance weights ("
            + getClassTypeString(i) + " class)!");
        }
      }
    }
  }

  /**
   * tests whether classifier handles data containing only a class attribute
   * 
   * @see CheckClassifier#canHandleOnlyClass(boolean, boolean, boolean, boolean,
   *      boolean, int)
   * @see CheckClassifier#testsPerClassType(int, boolean, boolean, boolean)
   */
  public void testOnlyClass() {
    boolean[] result;
    int i;

    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i)) {
        continue;
      }

      result = m_Tester.canHandleOnlyClass(m_NominalPredictors[i],
        m_NumericPredictors[i], m_StringPredictors[i], m_DatePredictors[i],
        m_RelationalPredictors[i], i);

      if (!result[0] && !result[1]) {
        fail("Error handling data containing only the class!");
      }
    }
  }

  /**
   * tests whether classifier handles N classes
   * 
   * @see CheckClassifier#canHandleNClasses(boolean, boolean, boolean, boolean,
   *      boolean, boolean, int)
   * @see CheckClassifier#testsPerClassType(int, boolean, boolean, boolean)
   * @see #m_NClasses
   */
  public void testNClasses() {
    boolean[] result;

    if (!canPredict(Attribute.NOMINAL)) {
      return;
    }

    result = m_Tester.canHandleNClasses(m_NominalPredictors[Attribute.NOMINAL],
      m_NumericPredictors[Attribute.NOMINAL],
      m_StringPredictors[Attribute.NOMINAL],
      m_DatePredictors[Attribute.NOMINAL],
      m_RelationalPredictors[Attribute.NOMINAL], m_multiInstanceHandler,
      m_NClasses);

    if (!result[0] && !result[1]) {
      fail("Error handling " + m_NClasses + " classes!");
    }
  }

  /**
   * checks whether the classifier can handle the class attribute at a given
   * position (0-based index, -1 means last).
   * 
   * @param type the class type
   * @param position the position of the class attribute (0-based, -1 means
   *          last)
   * @return true if the classifier can handle it
   */
  protected boolean checkClassAsNthAttribute(int type, int position) {
    boolean[] result;
    String indexStr;

    result = m_Tester.canHandleClassAsNthAttribute(m_NominalPredictors[type],
      m_NumericPredictors[type], m_StringPredictors[type],
      m_DatePredictors[type], m_RelationalPredictors[type],
      m_multiInstanceHandler, type, position);

    if (position == -1) {
      indexStr = "last";
    } else {
      indexStr = (position + 1) + ".";
    }

    if (!result[0] && !result[1]) {
      fail("Error handling class as " + indexStr + " attribute ("
        + getClassTypeString(type) + " class)!");
    }

    return result[0];
  }

  /**
   * Tests whether the classifier can handle class attributes as Nth attribute.
   * In case of multi-instance classifiers it performs no tests, since the
   * multi-instance data has a fixed format (bagID,bag,class).
   * 
   * @see CheckClassifier#canHandleClassAsNthAttribute(boolean, boolean,
   *      boolean, boolean, boolean, boolean, int, int)
   * @see CheckClassifier#testsPerClassType(int, boolean, boolean, boolean)
   */
  public void testClassAsNthAttribute() {
    int i;

    // multi-Instance data has fixed format!
    if (m_multiInstanceHandler) {
      return;
    }

    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i)) {
        continue;
      }

      // first attribute
      m_handleClassAsFirstAttribute[i] = checkClassAsNthAttribute(i, 0);

      // second attribute
      m_handleClassAsSecondAttribute[i] = checkClassAsNthAttribute(i, 1);
    }
  }

  /**
   * tests whether the classifier can handle zero training instances
   * 
   * @see CheckClassifier#canHandleZeroTraining(boolean, boolean, boolean,
   *      boolean, boolean, boolean, int)
   * @see CheckClassifier#testsPerClassType(int, boolean, boolean, boolean)
   */
  public void testZeroTraining() {
    boolean[] result;
    int i;

    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i)) {
        continue;
      }

      result = m_Tester.canHandleZeroTraining(m_NominalPredictors[i],
        m_NumericPredictors[i], m_StringPredictors[i], m_DatePredictors[i],
        m_RelationalPredictors[i], m_multiInstanceHandler, i);

      if (!result[0] && !result[1]) {
        fail("Error handling zero training instances (" + getClassTypeString(i)
          + " class)!");
      }
    }
  }

  /**
   * checks whether the classifier can handle the given percentage of missing
   * predictors
   * 
   * @param type the class type
   * @param percent the percentage of missing predictors
   * @param allowFail if true a fail statement may be executed
   * @return true if the classifier can handle it
   */
  protected boolean checkMissingPredictors(int type, int percent,
    boolean allowFail) {
    boolean[] result;

    result = m_Tester.canHandleMissing(m_NominalPredictors[type],
      m_NumericPredictors[type], m_StringPredictors[type],
      m_DatePredictors[type], m_RelationalPredictors[type],
      m_multiInstanceHandler, type, true, false, percent);

    if (allowFail) {
      if (!result[0] && !result[1]) {
        fail("Error handling " + percent + "% missing predictors ("
          + getClassTypeString(type) + " class)!");
      }
    }

    return result[0];
  }

  /**
   * tests whether the classifier can handle missing predictors (20% and 100%)
   * 
   * @see CheckClassifier#canHandleMissing(boolean, boolean, boolean, boolean,
   *      boolean, boolean, int, boolean, boolean, int)
   * @see CheckClassifier#testsPerClassType(int, boolean, boolean, boolean)
   */
  public void testMissingPredictors() {
    int i;

    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i)) {
        continue;
      }

      // 20% missing
      checkMissingPredictors(i, 20, true);

      // 100% missing
      if (m_handleMissingPredictors[i]) {
        checkMissingPredictors(i, 100, true);
      }
    }
  }

  /**
   * checks whether the classifier can handle the given percentage of missing
   * class labels
   * 
   * @param type the class type
   * @param percent the percentage of missing class labels
   * @param allowFail if true a fail statement may be executed
   * @return true if the classifier can handle it
   */
  protected boolean checkMissingClass(int type, int percent, boolean allowFail) {
    boolean[] result;

    result = m_Tester.canHandleMissing(m_NominalPredictors[type],
      m_NumericPredictors[type], m_StringPredictors[type],
      m_DatePredictors[type], m_RelationalPredictors[type],
      m_multiInstanceHandler, type, false, true, percent);

    if (allowFail) {
      if (!result[0] && !result[1]) {
        fail("Error handling " + percent + "% missing class labels ("
          + getClassTypeString(type) + " class)!");
      }
    }

    return result[0];
  }

  /**
   * tests whether the classifier can handle missing class values (20% and 100%)
   * 
   * @see CheckClassifier#canHandleMissing(boolean, boolean, boolean, boolean,
   *      boolean, boolean, int, boolean, boolean, int)
   * @see CheckClassifier#testsPerClassType(int, boolean, boolean, boolean)
   */
  public void testMissingClass() {
    int i;

    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i)) {
        continue;
      }

      // 20% missing
      checkMissingClass(i, 20, true);

      // 100% missing
      if (m_handleMissingClass[i]) {
        checkMissingClass(i, 100, true);
      }
    }
  }

  /**
   * tests whether the classifier correctly initializes in the buildClassifier
   * method
   * 
   * @see CheckClassifier#correctBuildInitialisation(boolean, boolean, boolean,
   *      boolean, boolean, boolean, int)
   * @see CheckClassifier#testsPerClassType(int, boolean, boolean, boolean)
   */
  public void testBuildInitialization() {
    boolean[] result;
    int i;

    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i)) {
        continue;
      }

      result = m_Tester.correctBuildInitialisation(m_NominalPredictors[i],
        m_NumericPredictors[i], m_StringPredictors[i], m_DatePredictors[i],
        m_RelationalPredictors[i], m_multiInstanceHandler, i);

      if (!result[0] && !result[1]) {
        fail("Incorrect build initialization (" + getClassTypeString(i)
          + " class)!");
      }
    }
  }

  /**
   * tests whether the classifier alters the training set during training.
   * 
   * @see CheckClassifier#datasetIntegrity(boolean, boolean, boolean, boolean,
   *      boolean, boolean, int, boolean, boolean)
   * @see CheckClassifier#testsPerClassType(int, boolean, boolean, boolean)
   */
  public void testDatasetIntegrity() {
    boolean[] result;
    int i;

    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i)) {
        continue;
      }

      result = m_Tester.datasetIntegrity(m_NominalPredictors[i],
        m_NumericPredictors[i], m_StringPredictors[i], m_DatePredictors[i],
        m_RelationalPredictors[i], m_multiInstanceHandler, i,
        m_handleMissingPredictors[i], m_handleMissingClass[i]);

      if (!result[0] && !result[1]) {
        fail("Training set is altered during training ("
          + getClassTypeString(i) + " class)!");
      }
    }
  }

  /**
   * tests whether the classifier erroneously uses the class value of test
   * instances (if provided)
   * 
   * @see CheckClassifier#doesntUseTestClassVal(boolean, boolean, boolean,
   *      boolean, boolean, boolean, int)
   * @see CheckClassifier#testsPerClassType(int, boolean, boolean, boolean)
   */
  public void testUseOfTestClassValue() {
    boolean[] result;
    int i;

    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i)) {
        continue;
      }

      result = m_Tester.doesntUseTestClassVal(m_NominalPredictors[i],
        m_NumericPredictors[i], m_StringPredictors[i], m_DatePredictors[i],
        m_RelationalPredictors[i], m_multiInstanceHandler, i);

      if (!result[0]) {
        fail("Uses test class values (" + getClassTypeString(i) + " class)!");
      }
    }
  }

  /**
   * tests whether the classifier produces the same model when trained
   * incrementally as when batch trained.
   * 
   * @see CheckClassifier#updatingEquality(boolean, boolean, boolean, boolean,
   *      boolean, boolean, int)
   * @see CheckClassifier#testsPerClassType(int, boolean, boolean, boolean)
   */
  public void testUpdatingEquality() {
    boolean[] result;
    int i;

    if (m_updateableClassifier) {
      for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
        // does the classifier support this type of class at all?
        if (!canPredict(i)) {
          continue;
        }

        result = m_Tester.updatingEquality(m_NominalPredictors[i],
          m_NumericPredictors[i], m_StringPredictors[i], m_DatePredictors[i],
          m_RelationalPredictors[i], m_multiInstanceHandler, i);

        if (!result[0]) {
          System.err
            .println("Incremental training does not produce same result as "
              + "batch training (" + getClassTypeString(i) + " class)!");
        }
      }
    }
  }

  /**
   * Builds a model using the current classifier using the first half of the
   * current data for training, and generates a bunch of predictions using the
   * remaining half of the data for testing.
   * 
   * @param data the instances to test the classifier on
   * @return a <code>FastVector</code> containing the predictions.
   */
  protected ArrayList<Prediction> useClassifier(Instances data)
    throws Exception {
    Classifier dc = null;
    int tot = data.numInstances();
    int mid = tot / 2;
    Instances train = null;
    Instances test = null;
    EvaluationUtils evaluation = new EvaluationUtils();

    try {
      train = new Instances(data, 0, mid);
      test = new Instances(data, mid, tot - mid);
      dc = m_Classifier;
    } catch (Exception e) {
      e.printStackTrace();
      fail("Problem setting up to use classifier: " + e);
    }

    do {
      try {
        return evaluation.getTrainTestPredictions(dc, train, test);
      } catch (IllegalArgumentException e) {
        String msg = e.getMessage();
        if (msg.indexOf("Not enough instances") != -1) {
          System.err.println("\nInflating training data.");
          Instances trainNew = new Instances(train);
          for (int i = 0; i < train.numInstances(); i++) {
            trainNew.add(train.instance(i));
          }
          train = trainNew;
        } else {
          throw e;
        }
      }
    } while (true);
  }

  /**
   * Returns a string containing all the predictions.
   * 
   * @param predictions a <code>FastVector</code> containing the predictions
   * @return a <code>String</code> representing the vector of predictions.
   */
  public static String predictionsToString(ArrayList<Prediction> predictions) {
    StringBuffer sb = new StringBuffer();
    sb.append(predictions.size()).append(" predictions\n");
    for (int i = 0; i < predictions.size(); i++) {
      sb.append(predictions.get(i)).append('\n');
    }
    return sb.toString();
  }

  /**
   * Provides a hook for derived classes to further modify the data. Currently,
   * the data is just passed through.
   * 
   * @param data the data to process
   * @return the processed data
   */
  protected Instances process(Instances data) {
    return data;
  }

  /**
   * Runs a regression test -- this checks that the output of the tested object
   * matches that in a reference version. When this test is run without any
   * pre-existing reference output, the reference version is created.
   */
  public void testRegression() throws Exception {
    int i;
    boolean succeeded;
    Regression reg;
    Instances train;

    // don't bother if not working correctly
    if (m_Tester.hasClasspathProblems()) {
      return;
    }

    reg = new Regression(this.getClass());
    succeeded = false;
    train = null;

    for (i = FIRST_CLASSTYPE; i <= LAST_CLASSTYPE; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i)) {
        continue;
      }

      train = m_Tester.makeTestDataset(42, m_Tester.getNumInstances(),
        m_NominalPredictors[i] ? m_Tester.getNumNominal() : 0,
        m_NumericPredictors[i] ? m_Tester.getNumNumeric() : 0,
        m_StringPredictors[i] ? m_Tester.getNumString() : 0,
        m_DatePredictors[i] ? m_Tester.getNumDate() : 0,
        m_RelationalPredictors[i] ? m_Tester.getNumRelational() : 0, 2, i,
        m_multiInstanceHandler);

      try {
        m_RegressionResults[i] = useClassifier(train);
        succeeded = true;
        reg.println(predictionsToString(m_RegressionResults[i]));
      } catch (Exception e) {
        String msg = e.getMessage().toLowerCase();
        if (msg.indexOf("not in classpath") > -1) {
          return;
        }

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
    } catch (java.io.IOException ex) {
      fail("Problem during regression testing.\n" + ex);
    }
  }

  /**
   * tests the listing of the options
   */
  public void testListOptions() {
    if (!m_OptionTester.checkListOptions()) {
      fail("Options cannot be listed via listOptions.");
    }
  }

  /**
   * tests the setting of the options
   */
  public void testSetOptions() {
    if (!m_OptionTester.checkSetOptions()) {
      fail("setOptions method failed.");
    }
  }

  /**
   * tests whether the default settings are processed correctly
   */
  public void testDefaultOptions() {
    if (!m_OptionTester.checkDefaultOptions()) {
      fail("Default options were not processed correctly.");
    }
  }

  /**
   * tests whether there are any remaining options
   */
  public void testRemainingOptions() {
    if (!m_OptionTester.checkRemainingOptions()) {
      fail("There were 'left-over' options.");
    }
  }

  /**
   * tests the whether the user-supplied options stay the same after setting.
   * getting, and re-setting again.
   * 
   * @see #getOptionTester()
   */
  public void testCanonicalUserOptions() {
    if (!m_OptionTester.checkCanonicalUserOptions()) {
      fail("setOptions method failed");
    }
  }

  /**
   * tests the resetting of the options to the default ones
   */
  public void testResettingOptions() {
    if (!m_OptionTester.checkSetOptions()) {
      fail("Resetting of options failed");
    }
  }

  /**
   * tests for a globalInfo method
   */
  public void testGlobalInfo() {
    if (!m_GOETester.checkGlobalInfo()) {
      fail("No globalInfo method");
    }
  }

  /**
   * tests the tool tips
   */
  public void testToolTips() {
    if (!m_GOETester.checkToolTips()) {
      fail("Tool tips inconsistent");
    }
  }
}
