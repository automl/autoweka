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
 * CheckKernel.java
 * Copyright (C) 2006-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.functions.supportVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.CheckScheme;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SerializationHelper;
import weka.core.TestInstances;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

/**
 * Class for examining the capabilities and finding problems with kernels. If
 * you implement an kernels using the WEKA.libraries, you should run the checks
 * on it to ensure robustness and correct operation. Passing all the tests of
 * this object does not mean bugs in the kernels don't exist, but this will help
 * find some common ones.
 * <p/>
 * 
 * Typical usage:
 * <p/>
 * <code>java weka.classifiers.functions.supportVector.CheckKernel -W kernel_name 
 * -- kernel_options </code>
 * <p/>
 * 
 * CheckKernel reports on the following:
 * <ul>
 * <li>Kernel abilities
 * <ul>
 * <li>Possible command line options to the kernels</li>
 * <li>Whether the kernels can predict nominal, numeric, string, date or
 * relational class attributes.</li>
 * <li>Whether the kernels can handle numeric predictor attributes</li>
 * <li>Whether the kernels can handle nominal predictor attributes</li>
 * <li>Whether the kernels can handle string predictor attributes</li>
 * <li>Whether the kernels can handle date predictor attributes</li>
 * <li>Whether the kernels can handle relational predictor attributes</li>
 * <li>Whether the kernels can handle multi-instance data</li>
 * <li>Whether the kernels can handle missing predictor values</li>
 * <li>Whether the kernels can handle missing class values</li>
 * <li>Whether a nominal kernels only handles 2 class problems</li>
 * <li>Whether the kernels can handle instance weights</li>
 * </ul>
 * </li>
 * <li>Correct functioning
 * <ul>
 * <li>Correct initialisation during buildKernel (i.e. no result changes when
 * buildKernel called repeatedly)</li>
 * <li>Whether the kernels alters the data passed to it (number of instances,
 * instance order, instance weights, etc)</li>
 * </ul>
 * </li>
 * <li>Degenerate cases
 * <ul>
 * <li>building kernels with zero training instances</li>
 * <li>all but one predictor attribute values missing</li>
 * <li>all predictor attribute values missing</li>
 * <li>all but one class values missing</li>
 * <li>all class values missing</li>
 * </ul>
 * </li>
 * </ul>
 * Running CheckKernel with the debug option set will output the training and
 * test datasets for any failed tests.
 * <p/>
 * 
 * The <code>weka.classifiers.AbstractKernelTest</code> uses this class to test
 * all the kernels. Any changes here, have to be checked in that abstract test
 * class, too.
 * <p/>
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -D
 *  Turn on debugging output.
 * </pre>
 * 
 * <pre>
 * -S
 *  Silent mode - prints nothing to stdout.
 * </pre>
 * 
 * <pre>
 * -N &lt;num&gt;
 *  The number of instances in the datasets (default 20).
 * </pre>
 * 
 * <pre>
 * -nominal &lt;num&gt;
 *  The number of nominal attributes (default 2).
 * </pre>
 * 
 * <pre>
 * -nominal-values &lt;num&gt;
 *  The number of values for nominal attributes (default 1).
 * </pre>
 * 
 * <pre>
 * -numeric &lt;num&gt;
 *  The number of numeric attributes (default 1).
 * </pre>
 * 
 * <pre>
 * -string &lt;num&gt;
 *  The number of string attributes (default 1).
 * </pre>
 * 
 * <pre>
 * -date &lt;num&gt;
 *  The number of date attributes (default 1).
 * </pre>
 * 
 * <pre>
 * -relational &lt;num&gt;
 *  The number of relational attributes (default 1).
 * </pre>
 * 
 * <pre>
 * -num-instances-relational &lt;num&gt;
 *  The number of instances in relational/bag attributes (default 10).
 * </pre>
 * 
 * <pre>
 * -words &lt;comma-separated-list&gt;
 *  The words to use in string attributes.
 * </pre>
 * 
 * <pre>
 * -word-separators &lt;chars&gt;
 *  The word separators to use in string attributes.
 * </pre>
 * 
 * <pre>
 * -W
 *  Full name of the kernel analysed.
 *  eg: weka.classifiers.functions.supportVector.RBFKernel
 *  (default weka.classifiers.functions.supportVector.RBFKernel)
 * </pre>
 * 
 * <pre>
 * Options specific to kernel weka.classifiers.functions.supportVector.RBFKernel:
 * </pre>
 * 
 * <pre>
 * -D
 *  Enables debugging output (if available) to be printed.
 *  (default: off)
 * </pre>
 * 
 * <pre>
 * -no-checks
 *  Turns off all checks - use with caution!
 *  (default: checks on)
 * </pre>
 * 
 * <pre>
 * -C &lt;num&gt;
 *  The size of the cache (a prime number), 0 for full cache and 
 *  -1 to turn it off.
 *  (default: 250007)
 * </pre>
 * 
 * <pre>
 * -G &lt;num&gt;
 *  The Gamma parameter.
 *  (default: 0.01)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * Options after -- are passed to the designated kernel.
 * <p/>
 * 
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 11247 $
 * @see TestInstances
 */
public class CheckKernel extends CheckScheme {

  /*
   * Note about test methods: - methods return array of booleans - first index:
   * success or not - second index: acceptable or not (e.g., Exception is OK)
   * 
   * FracPete (fracpete at waikato dot ac dot nz)
   */

  /*** The kernel to be examined */
  protected Kernel m_Kernel = new weka.classifiers.functions.supportVector.RBFKernel();

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tFull name of the kernel analysed.\n"
      + "\teg: weka.classifiers.functions.supportVector.RBFKernel\n"
      + "\t(default weka.classifiers.functions.supportVector.RBFKernel)", "W",
      1, "-W"));

    result.addAll(Collections.list(super.listOptions()));

    if ((m_Kernel != null) && (m_Kernel instanceof OptionHandler)) {
      result.addElement(new Option("", "", 0, "\nOptions specific to kernel "
        + m_Kernel.getClass().getName() + ":"));
      result.addAll(Collections.list(((OptionHandler) m_Kernel).listOptions()));
    }

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -D
   *  Turn on debugging output.
   * </pre>
   * 
   * <pre>
   * -S
   *  Silent mode - prints nothing to stdout.
   * </pre>
   * 
   * <pre>
   * -N &lt;num&gt;
   *  The number of instances in the datasets (default 20).
   * </pre>
   * 
   * <pre>
   * -nominal &lt;num&gt;
   *  The number of nominal attributes (default 2).
   * </pre>
   * 
   * <pre>
   * -nominal-values &lt;num&gt;
   *  The number of values for nominal attributes (default 1).
   * </pre>
   * 
   * <pre>
   * -numeric &lt;num&gt;
   *  The number of numeric attributes (default 1).
   * </pre>
   * 
   * <pre>
   * -string &lt;num&gt;
   *  The number of string attributes (default 1).
   * </pre>
   * 
   * <pre>
   * -date &lt;num&gt;
   *  The number of date attributes (default 1).
   * </pre>
   * 
   * <pre>
   * -relational &lt;num&gt;
   *  The number of relational attributes (default 1).
   * </pre>
   * 
   * <pre>
   * -num-instances-relational &lt;num&gt;
   *  The number of instances in relational/bag attributes (default 10).
   * </pre>
   * 
   * <pre>
   * -words &lt;comma-separated-list&gt;
   *  The words to use in string attributes.
   * </pre>
   * 
   * <pre>
   * -word-separators &lt;chars&gt;
   *  The word separators to use in string attributes.
   * </pre>
   * 
   * <pre>
   * -W
   *  Full name of the kernel analysed.
   *  eg: weka.classifiers.functions.supportVector.RBFKernel
   *  (default weka.classifiers.functions.supportVector.RBFKernel)
   * </pre>
   * 
   * <pre>
   * Options specific to kernel weka.classifiers.functions.supportVector.RBFKernel:
   * </pre>
   * 
   * <pre>
   * -D
   *  Enables debugging output (if available) to be printed.
   *  (default: off)
   * </pre>
   * 
   * <pre>
   * -no-checks
   *  Turns off all checks - use with caution!
   *  (default: checks on)
   * </pre>
   * 
   * <pre>
   * -C &lt;num&gt;
   *  The size of the cache (a prime number), 0 for full cache and 
   *  -1 to turn it off.
   *  (default: 250007)
   * </pre>
   * 
   * <pre>
   * -G &lt;num&gt;
   *  The Gamma parameter.
   *  (default: 0.01)
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    super.setOptions(options);

    tmpStr = Utils.getOption('W', options);
    if (tmpStr.length() == 0) {
      tmpStr = weka.classifiers.functions.supportVector.RBFKernel.class
        .getName();
    }
    setKernel((Kernel) forName("weka.classifiers.functions.supportVector",
      Kernel.class, tmpStr, Utils.partitionOptions(options)));
  }

  /**
   * Gets the current settings of the CheckKernel.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    Collections.addAll(result, super.getOptions());

    if (getKernel() != null) {
      result.add("-W");
      result.add(getKernel().getClass().getName());
    }

    if ((m_Kernel != null) && (m_Kernel instanceof OptionHandler)) {
      String[] options = ((OptionHandler) m_Kernel).getOptions();
      if (options.length > 0) {
        result.add("--");
      }
      Collections.addAll(result, options);
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Begin the tests, reporting results to System.out
   */
  @Override
  public void doTests() {

    if (getKernel() == null) {
      println("\n=== No kernel set ===");
      return;
    }
    println("\n=== Check on kernel: " + getKernel().getClass().getName()
      + " ===\n");

    // Start tests
    m_ClasspathProblems = false;
    println("--> Checking for interfaces");
    canTakeOptions();
    boolean weightedInstancesHandler = weightedInstancesHandler()[0];
    boolean multiInstanceHandler = multiInstanceHandler()[0];
    println("--> Kernel tests");
    declaresSerialVersionUID();
    testsPerClassType(Attribute.NOMINAL, weightedInstancesHandler,
      multiInstanceHandler);
    testsPerClassType(Attribute.NUMERIC, weightedInstancesHandler,
      multiInstanceHandler);
    testsPerClassType(Attribute.DATE, weightedInstancesHandler,
      multiInstanceHandler);
    testsPerClassType(Attribute.STRING, weightedInstancesHandler,
      multiInstanceHandler);
    testsPerClassType(Attribute.RELATIONAL, weightedInstancesHandler,
      multiInstanceHandler);
  }

  /**
   * Set the lernel to test.
   * 
   * @param value the kernel to use.
   */
  public void setKernel(Kernel value) {
    m_Kernel = value;
  }

  /**
   * Get the kernel being tested
   * 
   * @return the kernel being tested
   */
  public Kernel getKernel() {
    return m_Kernel;
  }

  /**
   * Run a battery of tests for a given class attribute type
   * 
   * @param classType true if the class attribute should be numeric
   * @param weighted true if the kernel says it handles weights
   * @param multiInstance true if the kernel is a multi-instance kernel
   */
  protected void testsPerClassType(int classType, boolean weighted,
    boolean multiInstance) {

    boolean PNom = canPredict(true, false, false, false, false, multiInstance,
      classType)[0];
    boolean PNum = canPredict(false, true, false, false, false, multiInstance,
      classType)[0];
    boolean PStr = canPredict(false, false, true, false, false, multiInstance,
      classType)[0];
    boolean PDat = canPredict(false, false, false, true, false, multiInstance,
      classType)[0];
    boolean PRel;
    if (!multiInstance) {
      PRel = canPredict(false, false, false, false, true, multiInstance,
        classType)[0];
    } else {
      PRel = false;
    }

    if (PNom || PNum || PStr || PDat || PRel) {
      if (weighted) {
        instanceWeights(PNom, PNum, PStr, PDat, PRel, multiInstance, classType);
      }

      if (classType == Attribute.NOMINAL) {
        canHandleNClasses(PNom, PNum, PStr, PDat, PRel, multiInstance, 4);
      }

      if (!multiInstance) {
        canHandleClassAsNthAttribute(PNom, PNum, PStr, PDat, PRel,
          multiInstance, classType, 0);
        canHandleClassAsNthAttribute(PNom, PNum, PStr, PDat, PRel,
          multiInstance, classType, 1);
      }

      canHandleZeroTraining(PNom, PNum, PStr, PDat, PRel, multiInstance,
        classType);
      boolean handleMissingPredictors = canHandleMissing(PNom, PNum, PStr,
        PDat, PRel, multiInstance, classType, true, false, 20)[0];
      if (handleMissingPredictors) {
        canHandleMissing(PNom, PNum, PStr, PDat, PRel, multiInstance,
          classType, true, false, 100);
      }

      boolean handleMissingClass = canHandleMissing(PNom, PNum, PStr, PDat,
        PRel, multiInstance, classType, false, true, 20)[0];
      if (handleMissingClass) {
        canHandleMissing(PNom, PNum, PStr, PDat, PRel, multiInstance,
          classType, false, true, 100);
      }

      correctBuildInitialisation(PNom, PNum, PStr, PDat, PRel, multiInstance,
        classType);
      datasetIntegrity(PNom, PNum, PStr, PDat, PRel, multiInstance, classType,
        handleMissingPredictors, handleMissingClass);
    }
  }

  /**
   * Checks whether the scheme can take command line options.
   * 
   * @return index 0 is true if the kernel can take options
   */
  protected boolean[] canTakeOptions() {

    boolean[] result = new boolean[2];

    print("options...");
    if (m_Kernel instanceof OptionHandler) {
      println("yes");
      if (m_Debug) {
        println("\n=== Full report ===");
        Enumeration<Option> enu = ((OptionHandler) m_Kernel).listOptions();
        while (enu.hasMoreElements()) {
          Option option = enu.nextElement();
          print(option.synopsis() + "\n" + option.description() + "\n");
        }
        println("\n");
      }
      result[0] = true;
    } else {
      println("no");
      result[0] = false;
    }

    return result;
  }

  /**
   * Checks whether the scheme says it can handle instance weights.
   * 
   * @return true if the kernel handles instance weights
   */
  protected boolean[] weightedInstancesHandler() {

    boolean[] result = new boolean[2];

    print("weighted instances kernel...");
    if (m_Kernel instanceof WeightedInstancesHandler) {
      println("yes");
      result[0] = true;
    } else {
      println("no");
      result[0] = false;
    }

    return result;
  }

  /**
   * Checks whether the scheme handles multi-instance data.
   * 
   * @return true if the kernel handles multi-instance data
   */
  protected boolean[] multiInstanceHandler() {
    boolean[] result = new boolean[2];

    print("multi-instance kernel...");
    if (m_Kernel instanceof MultiInstanceCapabilitiesHandler) {
      println("yes");
      result[0] = true;
    } else {
      println("no");
      result[0] = false;
    }

    return result;
  }

  /**
   * tests for a serialVersionUID. Fails in case the scheme doesn't declare a
   * UID.
   * 
   * @return index 0 is true if the scheme declares a UID
   */
  protected boolean[] declaresSerialVersionUID() {
    boolean[] result = new boolean[2];

    print("serialVersionUID...");

    result[0] = !SerializationHelper.needsUID(m_Kernel.getClass());

    if (result[0]) {
      println("yes");
    } else {
      println("no");
    }

    return result;
  }

  /**
   * Checks basic prediction of the scheme, for simple non-troublesome datasets.
   * 
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @param classType the class type (NOMINAL, NUMERIC, etc.)
   * @return index 0 is true if the test was passed, index 1 is true if test was
   *         acceptable
   */
  protected boolean[] canPredict(boolean nominalPredictor,
    boolean numericPredictor, boolean stringPredictor, boolean datePredictor,
    boolean relationalPredictor, boolean multiInstance, int classType) {

    print("basic predict");
    printAttributeSummary(nominalPredictor, numericPredictor, stringPredictor,
      datePredictor, relationalPredictor, multiInstance, classType);
    print("...");
    ArrayList<String> accepts = new ArrayList<String>();
    accepts.add("unary");
    accepts.add("binary");
    accepts.add("nominal");
    accepts.add("numeric");
    accepts.add("string");
    accepts.add("date");
    accepts.add("relational");
    accepts.add("multi-instance");
    accepts.add("not in classpath");
    int numTrain = getNumInstances(), numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    return runBasicTest(nominalPredictor, numericPredictor, stringPredictor,
      datePredictor, relationalPredictor, multiInstance, classType,
      missingLevel, predictorMissing, classMissing, numTrain, numClasses,
      accepts);
  }

  /**
   * Checks whether nominal schemes can handle more than two classes. If a
   * scheme is only designed for two-class problems it should throw an
   * appropriate exception for multi-class problems.
   * 
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @param numClasses the number of classes to test
   * @return index 0 is true if the test was passed, index 1 is true if test was
   *         acceptable
   */
  protected boolean[] canHandleNClasses(boolean nominalPredictor,
    boolean numericPredictor, boolean stringPredictor, boolean datePredictor,
    boolean relationalPredictor, boolean multiInstance, int numClasses) {

    print("more than two class problems");
    printAttributeSummary(nominalPredictor, numericPredictor, stringPredictor,
      datePredictor, relationalPredictor, multiInstance, Attribute.NOMINAL);
    print("...");
    ArrayList<String> accepts = new ArrayList<String>();
    accepts.add("number");
    accepts.add("class");
    int numTrain = getNumInstances(), missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    return runBasicTest(nominalPredictor, numericPredictor, stringPredictor,
      datePredictor, relationalPredictor, multiInstance, Attribute.NOMINAL,
      missingLevel, predictorMissing, classMissing, numTrain, numClasses,
      accepts);
  }

  /**
   * Checks whether the scheme can handle class attributes as Nth attribute.
   * 
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @param classType the class type (NUMERIC, NOMINAL, etc.)
   * @param classIndex the index of the class attribute (0-based, -1 means last
   *          attribute)
   * @return index 0 is true if the test was passed, index 1 is true if test was
   *         acceptable
   * @see TestInstances#CLASS_IS_LAST
   */
  protected boolean[] canHandleClassAsNthAttribute(boolean nominalPredictor,
    boolean numericPredictor, boolean stringPredictor, boolean datePredictor,
    boolean relationalPredictor, boolean multiInstance, int classType,
    int classIndex) {

    if (classIndex == TestInstances.CLASS_IS_LAST) {
      print("class attribute as last attribute");
    } else {
      print("class attribute as " + (classIndex + 1) + ". attribute");
    }
    printAttributeSummary(nominalPredictor, numericPredictor, stringPredictor,
      datePredictor, relationalPredictor, multiInstance, classType);
    print("...");
    ArrayList<String> accepts = new ArrayList<String>();
    int numTrain = getNumInstances(), numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    return runBasicTest(nominalPredictor, numericPredictor, stringPredictor,
      datePredictor, relationalPredictor, multiInstance, classType, classIndex,
      missingLevel, predictorMissing, classMissing, numTrain, numClasses,
      accepts);
  }

  /**
   * Checks whether the scheme can handle zero training instances.
   * 
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @param classType the class type (NUMERIC, NOMINAL, etc.)
   * @return index 0 is true if the test was passed, index 1 is true if test was
   *         acceptable
   */
  protected boolean[] canHandleZeroTraining(boolean nominalPredictor,
    boolean numericPredictor, boolean stringPredictor, boolean datePredictor,
    boolean relationalPredictor, boolean multiInstance, int classType) {

    print("handle zero training instances");
    printAttributeSummary(nominalPredictor, numericPredictor, stringPredictor,
      datePredictor, relationalPredictor, multiInstance, classType);
    print("...");
    ArrayList<String> accepts = new ArrayList<String>();
    accepts.add("train");
    accepts.add("value");
    int numTrain = 0, numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    return runBasicTest(nominalPredictor, numericPredictor, stringPredictor,
      datePredictor, relationalPredictor, multiInstance, classType,
      missingLevel, predictorMissing, classMissing, numTrain, numClasses,
      accepts);
  }

  /**
   * Checks whether the scheme correctly initialises models when buildKernel is
   * called. This test calls buildKernel with one training dataset. buildKernel
   * is then called on a training set with different structure, and then again
   * with the original training set. If the equals method of the
   * KernelEvaluation class returns false, this is noted as incorrect build
   * initialisation.
   * 
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @param classType the class type (NUMERIC, NOMINAL, etc.)
   * @return index 0 is true if the test was passed
   */
  protected boolean[] correctBuildInitialisation(boolean nominalPredictor,
    boolean numericPredictor, boolean stringPredictor, boolean datePredictor,
    boolean relationalPredictor, boolean multiInstance, int classType) {

    boolean[] result = new boolean[2];

    print("correct initialisation during buildKernel");
    printAttributeSummary(nominalPredictor, numericPredictor, stringPredictor,
      datePredictor, relationalPredictor, multiInstance, classType);
    print("...");
    int numTrain = getNumInstances(), numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    Instances train1 = null;
    Instances train2 = null;
    Kernel kernel = null;
    KernelEvaluation evaluation1A = null;
    KernelEvaluation evaluation1B = null;
    KernelEvaluation evaluation2 = null;
    int stage = 0;
    try {

      // Make two sets of train/test splits with different
      // numbers of attributes
      train1 = makeTestDataset(42, numTrain, nominalPredictor ? getNumNominal()
        : 0, numericPredictor ? getNumNumeric() : 0,
        stringPredictor ? getNumString() : 0, datePredictor ? getNumDate() : 0,
        relationalPredictor ? getNumRelational() : 0, numClasses, classType,
        multiInstance);
      train2 = makeTestDataset(84, numTrain,
        nominalPredictor ? getNumNominal() + 1 : 0,
        numericPredictor ? getNumNumeric() + 1 : 0,
        stringPredictor ? getNumString() + 1 : 0,
        datePredictor ? getNumDate() + 1 : 0,
        relationalPredictor ? getNumRelational() + 1 : 0, numClasses,
        classType, multiInstance);
      if (missingLevel > 0) {
        addMissing(train1, missingLevel, predictorMissing, classMissing);
        addMissing(train2, missingLevel, predictorMissing, classMissing);
      }

      kernel = Kernel.makeCopy(getKernel());
      evaluation1A = new KernelEvaluation();
      evaluation1B = new KernelEvaluation();
      evaluation2 = new KernelEvaluation();
    } catch (Exception ex) {
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {
      stage = 0;
      evaluation1A.evaluate(kernel, train1);

      stage = 1;
      evaluation2.evaluate(kernel, train2);

      stage = 2;
      evaluation1B.evaluate(kernel, train1);

      stage = 3;
      if (!evaluation1A.equals(evaluation1B)) {
        if (m_Debug) {
          println("\n=== Full report ===\n"
            + evaluation1A.toSummaryString("\nFirst buildKernel()") + "\n\n");
          println(evaluation1B.toSummaryString("\nSecond buildKernel()")
            + "\n\n");
        }
        throw new Exception("Results differ between buildKernel calls");
      }
      println("yes");
      result[0] = true;

    } catch (Exception ex) {
      println("no");
      result[0] = false;

      if (m_Debug) {
        println("\n=== Full Report ===");
        print("Problem during building");
        switch (stage) {
        case 0:
          print(" of dataset 1");
          break;
        case 1:
          print(" of dataset 2");
          break;
        case 2:
          print(" of dataset 1 (2nd build)");
          break;
        case 3:
          print(", comparing results from builds of dataset 1");
          break;
        }
        println(": " + ex.getMessage() + "\n");
        println("here are the datasets:\n");
        println("=== Train1 Dataset ===\n" + train1.toString() + "\n");
        println("=== Train2 Dataset ===\n" + train2.toString() + "\n");
      }
    }

    return result;
  }

  /**
   * Checks basic missing value handling of the scheme. If the missing values
   * cause an exception to be thrown by the scheme, this will be recorded.
   * 
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @param classType the class type (NUMERIC, NOMINAL, etc.)
   * @param predictorMissing true if the missing values may be in the predictors
   * @param classMissing true if the missing values may be in the class
   * @param missingLevel the percentage of missing values
   * @return index 0 is true if the test was passed, index 1 is true if test was
   *         acceptable
   */
  protected boolean[] canHandleMissing(boolean nominalPredictor,
    boolean numericPredictor, boolean stringPredictor, boolean datePredictor,
    boolean relationalPredictor, boolean multiInstance, int classType,
    boolean predictorMissing, boolean classMissing, int missingLevel) {

    if (missingLevel == 100) {
      print("100% ");
    }
    print("missing");
    if (predictorMissing) {
      print(" predictor");
      if (classMissing) {
        print(" and");
      }
    }
    if (classMissing) {
      print(" class");
    }
    print(" values");
    printAttributeSummary(nominalPredictor, numericPredictor, stringPredictor,
      datePredictor, relationalPredictor, multiInstance, classType);
    print("...");
    ArrayList<String> accepts = new ArrayList<String>();
    accepts.add("missing");
    accepts.add("value");
    accepts.add("train");
    int numTrain = getNumInstances(), numClasses = 2;

    return runBasicTest(nominalPredictor, numericPredictor, stringPredictor,
      datePredictor, relationalPredictor, multiInstance, classType,
      missingLevel, predictorMissing, classMissing, numTrain, numClasses,
      accepts);
  }

  /**
   * Checks whether the kernel can handle instance weights. This test compares
   * the kernel performance on two datasets that are identical except for the
   * training weights. If the results change, then the kernel must be using the
   * weights. It may be possible to get a false positive from this test if the
   * weight changes aren't significant enough to induce a change in kernel
   * performance (but the weights are chosen to minimize the likelihood of
   * this).
   * 
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @param classType the class type (NUMERIC, NOMINAL, etc.)
   * @return index 0 true if the test was passed
   */
  protected boolean[] instanceWeights(boolean nominalPredictor,
    boolean numericPredictor, boolean stringPredictor, boolean datePredictor,
    boolean relationalPredictor, boolean multiInstance, int classType) {

    print("kernel uses instance weights");
    printAttributeSummary(nominalPredictor, numericPredictor, stringPredictor,
      datePredictor, relationalPredictor, multiInstance, classType);
    print("...");
    int numTrain = 2 * getNumInstances(), numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    boolean[] result = new boolean[2];
    Instances train = null;
    Kernel[] kernels = null;
    KernelEvaluation evaluationB = null;
    KernelEvaluation evaluationI = null;
    boolean evalFail = false;
    try {
      train = makeTestDataset(42, numTrain,
        nominalPredictor ? getNumNominal() + 1 : 0,
        numericPredictor ? getNumNumeric() + 1 : 0,
        stringPredictor ? getNumString() : 0, datePredictor ? getNumDate() : 0,
        relationalPredictor ? getNumRelational() : 0, numClasses, classType,
        multiInstance);
      if (missingLevel > 0) {
        addMissing(train, missingLevel, predictorMissing, classMissing);
      }
      kernels = Kernel.makeCopies(getKernel(), 2);
      evaluationB = new KernelEvaluation();
      evaluationI = new KernelEvaluation();
      evaluationB.evaluate(kernels[0], train);
    } catch (Exception ex) {
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {

      // Now modify instance weights and re-built/test
      for (int i = 0; i < train.numInstances(); i++) {
        train.instance(i).setWeight(0);
      }
      Random random = new Random(1);
      for (int i = 0; i < train.numInstances() / 2; i++) {
        int inst = random.nextInt(train.numInstances());
        int weight = random.nextInt(10) + 1;
        train.instance(inst).setWeight(weight);
      }
      evaluationI.evaluate(kernels[1], train);
      if (evaluationB.equals(evaluationI)) {
        // println("no");
        evalFail = true;
        throw new Exception("evalFail");
      }

      println("yes");
      result[0] = true;
    } catch (Exception ex) {
      println("no");
      result[0] = false;

      if (m_Debug) {
        println("\n=== Full Report ===");

        if (evalFail) {
          println("Results don't differ between non-weighted and "
            + "weighted instance models.");
          println("Here are the results:\n");
          println(evaluationB.toSummaryString("\nboth methods\n"));
        } else {
          print("Problem during building");
          println(": " + ex.getMessage() + "\n");
        }
        println("Here is the dataset:\n");
        println("=== Train Dataset ===\n" + train.toString() + "\n");
        println("=== Train Weights ===\n");
        for (int i = 0; i < train.numInstances(); i++) {
          println(" " + (i + 1) + "    " + train.instance(i).weight());
        }
      }
    }

    return result;
  }

  /**
   * Checks whether the scheme alters the training dataset during building. If
   * the scheme needs to modify the data it should take a copy of the training
   * data. Currently checks for changes to header structure, number of
   * instances, order of instances, instance weights.
   * 
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @param classType the class type (NUMERIC, NOMINAL, etc.)
   * @param predictorMissing true if we know the kernel can handle (at least)
   *          moderate missing predictor values
   * @param classMissing true if we know the kernel can handle (at least)
   *          moderate missing class values
   * @return index 0 is true if the test was passed
   */
  protected boolean[] datasetIntegrity(boolean nominalPredictor,
    boolean numericPredictor, boolean stringPredictor, boolean datePredictor,
    boolean relationalPredictor, boolean multiInstance, int classType,
    boolean predictorMissing, boolean classMissing) {

    print("kernel doesn't alter original datasets");
    printAttributeSummary(nominalPredictor, numericPredictor, stringPredictor,
      datePredictor, relationalPredictor, multiInstance, classType);
    print("...");
    int numTrain = getNumInstances(), numClasses = 2, missingLevel = 20;

    boolean[] result = new boolean[2];
    Instances train = null;
    Kernel kernel = null;
    try {
      train = makeTestDataset(42, numTrain, nominalPredictor ? getNumNominal()
        : 0, numericPredictor ? getNumNumeric() : 0,
        stringPredictor ? getNumString() : 0, datePredictor ? getNumDate() : 0,
        relationalPredictor ? getNumRelational() : 0, numClasses, classType,
        multiInstance);
      if (missingLevel > 0) {
        addMissing(train, missingLevel, predictorMissing, classMissing);
      }
      kernel = Kernel.makeCopies(getKernel(), 1)[0];
    } catch (Exception ex) {
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {
      Instances trainCopy = new Instances(train);
      kernel.buildKernel(trainCopy);
      compareDatasets(train, trainCopy);

      println("yes");
      result[0] = true;
    } catch (Exception ex) {
      println("no");
      result[0] = false;

      if (m_Debug) {
        println("\n=== Full Report ===");
        print("Problem during building");
        println(": " + ex.getMessage() + "\n");
        println("Here is the dataset:\n");
        println("=== Train Dataset ===\n" + train.toString() + "\n");
      }
    }

    return result;
  }

  /**
   * Runs a text on the datasets with the given characteristics.
   * 
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @param classType the class type (NUMERIC, NOMINAL, etc.)
   * @param missingLevel the percentage of missing values
   * @param predictorMissing true if the missing values may be in the predictors
   * @param classMissing true if the missing values may be in the class
   * @param numTrain the number of instances in the training set
   * @param numClasses the number of classes
   * @param accepts the acceptable string in an exception
   * @return index 0 is true if the test was passed, index 1 is true if test was
   *         acceptable
   */
  protected boolean[] runBasicTest(boolean nominalPredictor,
    boolean numericPredictor, boolean stringPredictor, boolean datePredictor,
    boolean relationalPredictor, boolean multiInstance, int classType,
    int missingLevel, boolean predictorMissing, boolean classMissing,
    int numTrain, int numClasses, ArrayList<String> accepts) {

    return runBasicTest(nominalPredictor, numericPredictor, stringPredictor,
      datePredictor, relationalPredictor, multiInstance, classType,
      TestInstances.CLASS_IS_LAST, missingLevel, predictorMissing,
      classMissing, numTrain, numClasses, accepts);
  }

  /**
   * Runs a text on the datasets with the given characteristics.
   * 
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @param classType the class type (NUMERIC, NOMINAL, etc.)
   * @param classIndex the attribute index of the class
   * @param missingLevel the percentage of missing values
   * @param predictorMissing true if the missing values may be in the predictors
   * @param classMissing true if the missing values may be in the class
   * @param numTrain the number of instances in the training set
   * @param numClasses the number of classes
   * @param accepts the acceptable string in an exception
   * @return index 0 is true if the test was passed, index 1 is true if test was
   *         acceptable
   */
  protected boolean[] runBasicTest(boolean nominalPredictor,
    boolean numericPredictor, boolean stringPredictor, boolean datePredictor,
    boolean relationalPredictor, boolean multiInstance, int classType,
    int classIndex, int missingLevel, boolean predictorMissing,
    boolean classMissing, int numTrain, int numClasses,
    ArrayList<String> accepts) {

    boolean[] result = new boolean[2];
    Instances train = null;
    Kernel kernel = null;
    try {
      train = makeTestDataset(42, numTrain, nominalPredictor ? getNumNominal()
        : 0, numericPredictor ? getNumNumeric() : 0,
        stringPredictor ? getNumString() : 0, datePredictor ? getNumDate() : 0,
        relationalPredictor ? getNumRelational() : 0, numClasses, classType,
        classIndex, multiInstance);
      if (missingLevel > 0) {
        addMissing(train, missingLevel, predictorMissing, classMissing);
      }
      kernel = Kernel.makeCopies(getKernel(), 1)[0];
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {
      kernel.buildKernel(train);
      println("yes");
      result[0] = true;
    } catch (Exception ex) {
      boolean acceptable = false;
      String msg;
      if (ex.getMessage() == null) {
        msg = "";
      } else {
        msg = ex.getMessage().toLowerCase();
      }
      if (msg.indexOf("not in classpath") > -1) {
        m_ClasspathProblems = true;
      }

      for (int i = 0; i < accepts.size(); i++) {
        if (msg.indexOf(accepts.get(i)) >= 0) {
          acceptable = true;
        }
      }

      println("no" + (acceptable ? " (OK error message)" : ""));
      result[1] = acceptable;

      if (m_Debug) {
        println("\n=== Full Report ===");
        print("Problem during building");
        println(": " + ex.getMessage() + "\n");
        if (!acceptable) {
          if (accepts.size() > 0) {
            print("Error message doesn't mention ");
            for (int i = 0; i < accepts.size(); i++) {
              if (i != 0) {
                print(" or ");
              }
              print('"' + accepts.get(i) + '"');
            }
          }
          println("here is the dataset:\n");
          println("=== Train Dataset ===\n" + train.toString() + "\n");
        }
      }
    }

    return result;
  }

  /**
   * Make a simple set of instances, which can later be modified for use in
   * specific tests.
   * 
   * @param seed the random number seed
   * @param numInstances the number of instances to generate
   * @param numNominal the number of nominal attributes
   * @param numNumeric the number of numeric attributes
   * @param numString the number of string attributes
   * @param numDate the number of date attributes
   * @param numRelational the number of relational attributes
   * @param numClasses the number of classes (if nominal class)
   * @param classType the class type (NUMERIC, NOMINAL, etc.)
   * @param multiInstance whether the dataset should a multi-instance dataset
   * @return the test dataset
   * @throws Exception if the dataset couldn't be generated
   * @see #process(Instances)
   */
  protected Instances makeTestDataset(int seed, int numInstances,
    int numNominal, int numNumeric, int numString, int numDate,
    int numRelational, int numClasses, int classType, boolean multiInstance)
    throws Exception {

    return makeTestDataset(seed, numInstances, numNominal, numNumeric,
      numString, numDate, numRelational, numClasses, classType,
      TestInstances.CLASS_IS_LAST, multiInstance);
  }

  /**
   * Make a simple set of instances with variable position of the class
   * attribute, which can later be modified for use in specific tests.
   * 
   * @param seed the random number seed
   * @param numInstances the number of instances to generate
   * @param numNominal the number of nominal attributes
   * @param numNumeric the number of numeric attributes
   * @param numString the number of string attributes
   * @param numDate the number of date attributes
   * @param numRelational the number of relational attributes
   * @param numClasses the number of classes (if nominal class)
   * @param classType the class type (NUMERIC, NOMINAL, etc.)
   * @param classIndex the index of the class (0-based, -1 as last)
   * @param multiInstance whether the dataset should a multi-instance dataset
   * @return the test dataset
   * @throws Exception if the dataset couldn't be generated
   * @see TestInstances#CLASS_IS_LAST
   * @see #process(Instances)
   */
  protected Instances makeTestDataset(int seed, int numInstances,
    int numNominal, int numNumeric, int numString, int numDate,
    int numRelational, int numClasses, int classType, int classIndex,
    boolean multiInstance) throws Exception {

    TestInstances dataset = new TestInstances();

    dataset.setSeed(seed);
    dataset.setNumInstances(numInstances);
    dataset.setNumNominal(numNominal);
    dataset.setNumNumeric(numNumeric);
    dataset.setNumString(numString);
    dataset.setNumDate(numDate);
    dataset.setNumRelational(numRelational);
    dataset.setNumClasses(numClasses);
    dataset.setClassType(classType);
    dataset.setClassIndex(classIndex);
    dataset.setNumClasses(numClasses);
    dataset.setMultiInstance(multiInstance);
    dataset.setWords(getWords());
    dataset.setWordSeparators(getWordSeparators());

    return process(dataset.generate());
  }

  /**
   * Print out a short summary string for the dataset characteristics
   * 
   * @param nominalPredictor true if nominal predictor attributes are present
   * @param numericPredictor true if numeric predictor attributes are present
   * @param stringPredictor true if string predictor attributes are present
   * @param datePredictor true if date predictor attributes are present
   * @param relationalPredictor true if relational predictor attributes are
   *          present
   * @param multiInstance whether multi-instance is needed
   * @param classType the class type (NUMERIC, NOMINAL, etc.)
   */
  protected void printAttributeSummary(boolean nominalPredictor,
    boolean numericPredictor, boolean stringPredictor, boolean datePredictor,
    boolean relationalPredictor, boolean multiInstance, int classType) {

    String str = "";

    if (numericPredictor) {
      str += " numeric";
    }

    if (nominalPredictor) {
      if (str.length() > 0) {
        str += " &";
      }
      str += " nominal";
    }

    if (stringPredictor) {
      if (str.length() > 0) {
        str += " &";
      }
      str += " string";
    }

    if (datePredictor) {
      if (str.length() > 0) {
        str += " &";
      }
      str += " date";
    }

    if (relationalPredictor) {
      if (str.length() > 0) {
        str += " &";
      }
      str += " relational";
    }

    str += " predictors)";

    switch (classType) {
    case Attribute.NUMERIC:
      str = " (numeric class," + str;
      break;
    case Attribute.NOMINAL:
      str = " (nominal class," + str;
      break;
    case Attribute.STRING:
      str = " (string class," + str;
      break;
    case Attribute.DATE:
      str = " (date class," + str;
      break;
    case Attribute.RELATIONAL:
      str = " (relational class," + str;
      break;
    }

    print(str);
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11247 $");
  }

  /**
   * Test method for this class
   * 
   * @param args the commandline parameters
   */
  public static void main(String[] args) {
    runCheck(new CheckKernel(), args);
  }
}
