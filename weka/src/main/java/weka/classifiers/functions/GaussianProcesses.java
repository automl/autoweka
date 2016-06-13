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
 *    GaussianProcesses.java
 *    Copyright (C) 2005-2012,2015 University of Waikato
 */

package weka.classifiers.functions;

import weka.classifiers.ConditionalDensityEstimator;
import weka.classifiers.IntervalEstimator;
import weka.classifiers.RandomizableClassifier;
import weka.classifiers.functions.supportVector.CachedKernel;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Statistics;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Standardize;

import no.uib.cipr.matrix.*;
import no.uib.cipr.matrix.Matrix;

import java.util.Collections;
import java.util.Enumeration;

/**
 * <!-- globalinfo-start -->
 * * Implements Gaussian processes for regression without hyperparameter-tuning. To make choosing an appropriate noise level easier, this implementation applies normalization/standardization to the target attribute as well as the other attributes (if  normalization/standardizaton is turned on). Missing values are replaced by the global mean/mode. Nominal attributes are converted to binary ones. Note that kernel caching is turned off if the kernel used implements CachedKernel.
 * * <br><br>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start -->
 * * BibTeX:
 * * <pre>
 * * &#64;misc{Mackay1998,
 * *    address = {Dept. of Physics, Cambridge University, UK},
 * *    author = {David J.C. Mackay},
 * *    title = {Introduction to Gaussian Processes},
 * *    year = {1998},
 * *    PS = {http://wol.ra.phy.cam.ac.uk/mackay/gpB.ps.gz}
 * * }
 * * </pre>
 * * <br><br>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start -->
 * * Valid options are: <p>
 * * 
 * * <pre> -L &lt;double&gt;
 * *  Level of Gaussian Noise wrt transformed target. (default 1)</pre>
 * * 
 * * <pre> -N
 * *  Whether to 0=normalize/1=standardize/2=neither. (default 0=normalize)</pre>
 * * 
 * * <pre> -K &lt;classname and parameters&gt;
 * *  The Kernel to use.
 * *  (default: weka.classifiers.functions.supportVector.PolyKernel)</pre>
 * * 
 * * <pre> -S &lt;num&gt;
 * *  Random number seed.
 * *  (default 1)</pre>
 * * 
 * * <pre> -output-debug-info
 * *  If set, classifier is run in debug mode and
 * *  may output additional info to the console</pre>
 * * 
 * * <pre> -do-not-check-capabilities
 * *  If set, classifier capabilities are not checked before classifier is built
 * *  (use with caution).</pre>
 * * 
 * * <pre> -num-decimal-places
 * *  The number of decimal places for the output of numbers in the model (default 2).</pre>
 * * 
 * * <pre> 
 * * Options specific to kernel weka.classifiers.functions.supportVector.PolyKernel:
 * * </pre>
 * * 
 * * <pre> -E &lt;num&gt;
 * *  The Exponent to use.
 * *  (default: 1.0)</pre>
 * * 
 * * <pre> -L
 * *  Use lower-order terms.
 * *  (default: no)</pre>
 * * 
 * * <pre> -C &lt;num&gt;
 * *  The size of the cache (a prime number), 0 for full cache and 
 * *  -1 to turn it off.
 * *  (default: 250007)</pre>
 * * 
 * * <pre> -output-debug-info
 * *  Enables debugging output (if available) to be printed.
 * *  (default: off)</pre>
 * * 
 * * <pre> -no-checks
 * *  Turns off all checks - use with caution!
 * *  (default: checks on)</pre>
 * * 
 * <!-- options-end -->
 * 
 * @author Kurt Driessens (kurtd@cs.waikato.ac.nz)
 * @author Remco Bouckaert (remco@cs.waikato.ac.nz)
 * @author Eibe Frank, University of Waikato
 * @version $Revision: 12745 $
 */
public class GaussianProcesses extends RandomizableClassifier implements
  IntervalEstimator, ConditionalDensityEstimator,
  TechnicalInformationHandler, WeightedInstancesHandler {

  /** for serialization */
  static final long serialVersionUID = -8620066949967678545L;

  /** The filter used to make attributes numeric. */
  protected NominalToBinary m_NominalToBinary;

  /** normalizes the data */
  public static final int FILTER_NORMALIZE = 0;

  /** standardizes the data */
  public static final int FILTER_STANDARDIZE = 1;

  /** no filter */
  public static final int FILTER_NONE = 2;

  /** The filter to apply to the training data */
  public static final Tag[] TAGS_FILTER = {
    new Tag(FILTER_NORMALIZE, "Normalize training data"),
    new Tag(FILTER_STANDARDIZE, "Standardize training data"),
    new Tag(FILTER_NONE, "No normalization/standardization"), };

  /** The filter used to standardize/normalize all values. */
  protected Filter m_Filter = null;

  /** Whether to normalize/standardize/neither */
  protected int m_filterType = FILTER_NORMALIZE;

  /** The filter used to get rid of missing values. */
  protected ReplaceMissingValues m_Missing;

  /**
   * Turn off all checks and conversions? Turning them off assumes that data is
   * purely numeric, doesn't contain any missing values, and has a numeric
   * class.
   */
  protected boolean m_checksTurnedOff = false;

  /** Gaussian Noise Value. */
  protected double m_delta = 1;

  /** The squared noise value. */
  protected double m_deltaSquared = 1;

  /**
   * The parameters of the linear transformation realized by the filter on the
   * class attribute
   */
  protected double m_Alin;
  protected double m_Blin;

  /** Template of kernel to use */
  protected Kernel m_kernel = new PolyKernel();

  /** Actual kernel object to use */
  protected Kernel m_actualKernel;

  /** The number of training instances */
  protected int m_NumTrain = 0;

  /** The training data. */
  protected double m_avg_target;

  /** (negative) covariance matrix in symmetric matrix representation **/
  public Matrix m_L;

  /** The vector of target values. */
  protected Vector m_t;
  
  /** The weight of the training instances. */
  protected double[] m_weights;

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {

    return " Implements Gaussian processes for "
      + "regression without hyperparameter-tuning. To make choosing an "
      + "appropriate noise level easier, this implementation applies "
      + "normalization/standardization to the target attribute as well "
      + "as the other attributes (if "
      + " normalization/standardizaton is turned on). Missing values "
      + "are replaced by the global mean/mode. Nominal attributes are "
      + "converted to binary ones. Note that kernel caching is turned off "
      + "if the kernel used implements CachedKernel.";
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.MISC);
    result.setValue(Field.AUTHOR, "David J.C. Mackay");
    result.setValue(Field.YEAR, "1998");
    result.setValue(Field.TITLE, "Introduction to Gaussian Processes");
    result
      .setValue(Field.ADDRESS, "Dept. of Physics, Cambridge University, UK");
    result.setValue(Field.PS, "http://wol.ra.phy.cam.ac.uk/mackay/gpB.ps.gz");

    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = getKernel().getCapabilities();
    result.setOwner(this);

    // attribute
    result.enableAllAttributeDependencies();
    // with NominalToBinary we can also handle nominal attributes, but only
    // if the kernel can handle numeric attributes
    if (result.handles(Capability.NUMERIC_ATTRIBUTES)) {
      result.enable(Capability.NOMINAL_ATTRIBUTES);
    }
    result.enable(Capability.MISSING_VALUES);

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    result.disable(Capability.NO_CLASS);
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Method for building the classifier.
   * 
   * @param insts the set of training instances
   * @throws Exception if the classifier can't be built successfully
   */
  @Override
  public void buildClassifier(Instances insts) throws Exception {

    // check the set of training instances
    if (!m_checksTurnedOff) {
      // can classifier handle the data?
      getCapabilities().testWithFail(insts);

      // remove instances with missing class
      insts = new Instances(insts);
      insts.deleteWithMissingClass();
      m_Missing = new ReplaceMissingValues();
      m_Missing.setInputFormat(insts);
      insts = Filter.useFilter(insts, m_Missing);
    } else {
      m_Missing = null;
    }

    if (getCapabilities().handles(Capability.NUMERIC_ATTRIBUTES)) {
      boolean onlyNumeric = true;
      if (!m_checksTurnedOff) {
        for (int i = 0; i < insts.numAttributes(); i++) {
          if (i != insts.classIndex()) {
            if (!insts.attribute(i).isNumeric()) {
              onlyNumeric = false;
              break;
            }
          }
        }
      }

      if (!onlyNumeric) {
        m_NominalToBinary = new NominalToBinary();
        m_NominalToBinary.setInputFormat(insts);
        insts = Filter.useFilter(insts, m_NominalToBinary);
      } else {
        m_NominalToBinary = null;
      }
    } else {
      m_NominalToBinary = null;
    }

    if (m_filterType == FILTER_STANDARDIZE) {
      m_Filter = new Standardize();
      ((Standardize) m_Filter).setIgnoreClass(true);
      m_Filter.setInputFormat(insts);
      insts = Filter.useFilter(insts, m_Filter);
    } else if (m_filterType == FILTER_NORMALIZE) {
      m_Filter = new Normalize();
      ((Normalize) m_Filter).setIgnoreClass(true);
      m_Filter.setInputFormat(insts);
      insts = Filter.useFilter(insts, m_Filter);
    } else {
      m_Filter = null;
    }

    m_NumTrain = insts.numInstances();

    // determine which linear transformation has been
    // applied to the class by the filter
    if (m_Filter != null) {
      Instance witness = (Instance) insts.instance(0).copy();
      witness.setValue(insts.classIndex(), 0);
      m_Filter.input(witness);
      m_Filter.batchFinished();
      Instance res = m_Filter.output();
      m_Blin = res.value(insts.classIndex());
      witness.setValue(insts.classIndex(), 1);
      m_Filter.input(witness);
      m_Filter.batchFinished();
      res = m_Filter.output();
      m_Alin = res.value(insts.classIndex()) - m_Blin;
    } else {
      m_Alin = 1.0;
      m_Blin = 0.0;
    }

    // Initialize kernel
    m_actualKernel = Kernel.makeCopy(m_kernel);
    if (m_kernel instanceof CachedKernel) {
      ((CachedKernel)m_actualKernel).setCacheSize(-1); // We don't need a cache at all
    }
    m_actualKernel.buildKernel(insts);

    // Compute average target value
    double sum = 0.0;
    for (int i = 0; i < insts.numInstances(); i++) {
      sum += insts.instance(i).weight() * insts.instance(i).classValue();
    }
    m_avg_target = sum / insts.sumOfWeights();

    // Store squared noise level
    m_deltaSquared = m_delta * m_delta;

    // Store square roots of instance m_weights
    m_weights = new double[insts.numInstances()];
    for (int i  = 0; i < insts.numInstances(); i++) {
      m_weights[i] = Math.sqrt(insts.instance(i).weight());
    }

    // initialize kernel matrix/covariance matrix
    int n = insts.numInstances();
    m_L = new UpperSPDDenseMatrix(n);
    for (int i = 0; i < n; i++) {
      for (int j = i + 1; j < n; j++) {
        m_L.set(i, j, m_weights[i] * m_weights[j] * m_actualKernel.eval(i, j, insts.instance(i)));
      }
      m_L.set(i, i, m_weights[i] * m_weights[i] * m_actualKernel.eval(i, i, insts.instance(i)) + m_deltaSquared);
    }

    // Compute inverse of kernel matrix
    m_L = new DenseCholesky(n, true).factor((UpperSPDDenseMatrix)m_L).solve(Matrices.identity(n));
    m_L = new UpperSPDDenseMatrix(m_L); // Convert from DenseMatrix

    // Compute t
    Vector tt = new DenseVector(n);
    for (int i = 0; i < n; i++) {
      tt.set(i, m_weights[i] * (insts.instance(i).classValue() - m_avg_target));
    }
    m_t = m_L.mult(tt, new DenseVector(insts.numInstances()));

  } // buildClassifier

  /**
   * Classifies a given instance.
   * 
   * @param inst the instance to be classified
   * @return the classification
   * @throws Exception if instance could not be classified successfully
   */
  @Override
  public double classifyInstance(Instance inst) throws Exception {

    // Filter instance
    inst = filterInstance(inst);

    // Build K vector
    Vector k = new DenseVector(m_NumTrain);
    for (int i = 0; i < m_NumTrain; i++) {
      k.set(i, m_weights[i] * m_actualKernel.eval(-1, i, inst));
    }

    double result = (k.dot(m_t) + m_avg_target - m_Blin) / m_Alin;

    return result;

  }

  /**
   * Filters an instance.
   */
  protected Instance filterInstance(Instance inst) throws Exception {

    if (!m_checksTurnedOff) {
      m_Missing.input(inst);
      m_Missing.batchFinished();
      inst = m_Missing.output();
    }

    if (m_NominalToBinary != null) {
      m_NominalToBinary.input(inst);
      m_NominalToBinary.batchFinished();
      inst = m_NominalToBinary.output();
    }

    if (m_Filter != null) {
      m_Filter.input(inst);
      m_Filter.batchFinished();
      inst = m_Filter.output();
    }
    return inst;
  }

  /**
   * Computes standard deviation for given instance, without transforming target
   * back into original space.
   */
  protected double computeStdDev(Instance inst, Vector k) throws Exception {

    double kappa = m_actualKernel.eval(-1, -1, inst) + m_deltaSquared;

    double s = m_L.mult(k, new DenseVector(k.size())).dot(k);

    double sigma = m_delta;
    if (kappa > s) {
      sigma = Math.sqrt(kappa - s);
    }

    return sigma;
  }

  /**
   * Computes a prediction interval for the given instance and confidence level.
   * 
   * @param inst the instance to make the prediction for
   * @param confidenceLevel the percentage of cases the interval should cover
   * @return a 1*2 array that contains the boundaries of the interval
   * @throws Exception if interval could not be estimated successfully
   */
  @Override
  public double[][] predictIntervals(Instance inst, double confidenceLevel)
    throws Exception {

    inst = filterInstance(inst);

    // Build K vector (and Kappa)
    Vector k = new DenseVector(m_NumTrain);
    for (int i = 0; i < m_NumTrain; i++) {
      k.set(i, m_weights[i] * m_actualKernel.eval(-1, i, inst));
    }

    double estimate = k.dot(m_t) + m_avg_target;

    double sigma = computeStdDev(inst, k);

    confidenceLevel = 1.0 - ((1.0 - confidenceLevel) / 2.0);

    double z = Statistics.normalInverse(confidenceLevel);

    double[][] interval = new double[1][2];

    interval[0][0] = estimate - z * sigma;
    interval[0][1] = estimate + z * sigma;

    interval[0][0] = (interval[0][0] - m_Blin) / m_Alin;
    interval[0][1] = (interval[0][1] - m_Blin) / m_Alin;

    return interval;

  }

  /**
   * Gives standard deviation of the prediction at the given instance.
   * 
   * @param inst the instance to get the standard deviation for
   * @return the standard deviation
   * @throws Exception if computation fails
   */
  public double getStandardDeviation(Instance inst) throws Exception {

    inst = filterInstance(inst);

    // Build K vector (and Kappa)
    Vector k = new DenseVector(m_NumTrain);
    for (int i = 0; i < m_NumTrain; i++) {
      k.set(i, m_weights[i] * m_actualKernel.eval(-1, i, inst));
    }

    return computeStdDev(inst, k) / m_Alin;
  }

  /**
   * Returns natural logarithm of density estimate for given value based on
   * given instance.
   * 
   * @param inst the instance to make the prediction for.
   * @param value the value to make the prediction for.
   * @return the natural logarithm of the density estimate
   * @exception Exception if the density cannot be computed
   */
  @Override
  public double logDensity(Instance inst, double value) throws Exception {

    inst = filterInstance(inst);

    // Build K vector (and Kappa)
    Vector k = new DenseVector(m_NumTrain);
    for (int i = 0; i < m_NumTrain; i++) {
      k.set(i, m_weights[i] * m_actualKernel.eval(-1, i, inst));
    }

    double estimate = k.dot(m_t) + m_avg_target;

    double sigma = computeStdDev(inst, k);

    // transform to GP space
    value = value * m_Alin + m_Blin;
    // center around estimate
    value = value - estimate;
    double z = -Math.log(sigma * Math.sqrt(2 * Math.PI)) - value * value
      / (2.0 * sigma * sigma);

    return z + Math.log(m_Alin);
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    java.util.Vector<Option> result = new java.util.Vector<Option>();

    result.addElement(new Option(
      "\tLevel of Gaussian Noise wrt transformed target." + " (default 1)",
      "L", 1, "-L <double>"));

    result.addElement(new Option(
      "\tWhether to 0=normalize/1=standardize/2=neither. "
        + "(default 0=normalize)", "N", 1, "-N"));

    result.addElement(new Option("\tThe Kernel to use.\n"
      + "\t(default: weka.classifiers.functions.supportVector.PolyKernel)",
      "K", 1, "-K <classname and parameters>"));

    result.addAll(Collections.list(super.listOptions()));

    result.addElement(new Option("", "", 0, "\nOptions specific to kernel "
      + getKernel().getClass().getName() + ":"));

    result
      .addAll(Collections.list(((OptionHandler) getKernel()).listOptions()));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start -->
   * * Valid options are: <p>
   * * 
   * * <pre> -L &lt;double&gt;
   * *  Level of Gaussian Noise wrt transformed target. (default 1)</pre>
   * * 
   * * <pre> -N
   * *  Whether to 0=normalize/1=standardize/2=neither. (default 0=normalize)</pre>
   * * 
   * * <pre> -K &lt;classname and parameters&gt;
   * *  The Kernel to use.
   * *  (default: weka.classifiers.functions.supportVector.PolyKernel)</pre>
   * * 
   * * <pre> -S &lt;num&gt;
   * *  Random number seed.
   * *  (default 1)</pre>
   * * 
   * * <pre> -output-debug-info
   * *  If set, classifier is run in debug mode and
   * *  may output additional info to the console</pre>
   * * 
   * * <pre> -do-not-check-capabilities
   * *  If set, classifier capabilities are not checked before classifier is built
   * *  (use with caution).</pre>
   * * 
   * * <pre> -num-decimal-places
   * *  The number of decimal places for the output of numbers in the model (default 2).</pre>
   * * 
   * * <pre> 
   * * Options specific to kernel weka.classifiers.functions.supportVector.PolyKernel:
   * * </pre>
   * * 
   * * <pre> -E &lt;num&gt;
   * *  The Exponent to use.
   * *  (default: 1.0)</pre>
   * * 
   * * <pre> -L
   * *  Use lower-order terms.
   * *  (default: no)</pre>
   * * 
   * * <pre> -C &lt;num&gt;
   * *  The size of the cache (a prime number), 0 for full cache and 
   * *  -1 to turn it off.
   * *  (default: 250007)</pre>
   * * 
   * * <pre> -output-debug-info
   * *  Enables debugging output (if available) to be printed.
   * *  (default: off)</pre>
   * * 
   * * <pre> -no-checks
   * *  Turns off all checks - use with caution!
   * *  (default: checks on)</pre>
   * * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;
    String[] tmpOptions;

    tmpStr = Utils.getOption('L', options);
    if (tmpStr.length() != 0) {
      setNoise(Double.parseDouble(tmpStr));
    } else {
      setNoise(1);
    }

    tmpStr = Utils.getOption('N', options);
    if (tmpStr.length() != 0) {
      setFilterType(new SelectedTag(Integer.parseInt(tmpStr), TAGS_FILTER));
    } else {
      setFilterType(new SelectedTag(FILTER_NORMALIZE, TAGS_FILTER));
    }

    tmpStr = Utils.getOption('K', options);
    tmpOptions = Utils.splitOptions(tmpStr);
    if (tmpOptions.length != 0) {
      tmpStr = tmpOptions[0];
      tmpOptions[0] = "";
      setKernel(Kernel.forName(tmpStr, tmpOptions));
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    java.util.Vector<String> result = new java.util.Vector<String>();

    result.addElement("-L");
    result.addElement("" + getNoise());

    result.addElement("-N");
    result.addElement("" + m_filterType);

    result.addElement("-K");
    result.addElement("" + m_kernel.getClass().getName() + " "
      + Utils.joinOptions(m_kernel.getOptions()));

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String kernelTipText() {
    return "The kernel to use.";
  }

  /**
   * Gets the kernel to use.
   * 
   * @return the kernel
   */
  public Kernel getKernel() {
    return m_kernel;
  }

  /**
   * Sets the kernel to use.
   * 
   * @param value the new kernel
   */
  public void setKernel(Kernel value) {
    m_kernel = value;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String filterTypeTipText() {
    return "Determines how/if the data will be transformed.";
  }

  /**
   * Gets how the training data will be transformed. Will be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.
   * 
   * @return the filtering mode
   */
  public SelectedTag getFilterType() {

    return new SelectedTag(m_filterType, TAGS_FILTER);
  }

  /**
   * Sets how the training data will be transformed. Should be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.
   * 
   * @param newType the new filtering mode
   */
  public void setFilterType(SelectedTag newType) {

    if (newType.getTags() == TAGS_FILTER) {
      m_filterType = newType.getSelectedTag().getID();
    }
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String noiseTipText() {
    return "The level of Gaussian Noise (added to the diagonal of the Covariance Matrix), after the "
      + "target has been normalized/standardized/left unchanged).";
  }

  /**
   * Get the value of noise.
   * 
   * @return Value of noise.
   */
  public double getNoise() {
    return m_delta;
  }

  /**
   * Set the level of Gaussian Noise.
   * 
   * @param v Value to assign to noise.
   */
  public void setNoise(double v) {
    m_delta = v;
  }

  /**
   * Prints out the classifier.
   * 
   * @return a description of the classifier as a string
   */
  @Override
  public String toString() {

    StringBuffer text = new StringBuffer();

    if (m_t == null) {
      return "Gaussian Processes: No model built yet.";
    }

    try {

      text.append("Gaussian Processes\n\n");
      text.append("Kernel used:\n  " + m_kernel.toString() + "\n\n");

      text.append("All values shown based on: "
        + TAGS_FILTER[m_filterType].getReadable() + "\n\n");

      text.append("Average Target Value : " + m_avg_target + "\n");

      text.append("Inverted Covariance Matrix:\n");
      double min = m_L.get(0, 0);
      double max = m_L.get(0, 0);
      for (int i = 0; i < m_NumTrain; i++) {
        for (int j = 0; j <= i; j++) {
          if (m_L.get(i, j) < min) {
            min = m_L.get(i, j);
          } else if (m_L.get(i, j) > max) {
            max = m_L.get(i, j);
          }
        }
      }
      text.append("    Lowest Value = " + min + "\n");
      text.append("    Highest Value = " + max + "\n");
      text.append("Inverted Covariance Matrix * Target-value Vector:\n");
      min = m_t.get(0);
      max = m_t.get(0);
      for (int i = 0; i < m_NumTrain; i++) {
        if (m_t.get(i) < min) {
          min = m_t.get(i);
        } else if (m_t.get(i) > max) {
          max = m_t.get(i);
        }
      }
      text.append("    Lowest Value = " + min + "\n");
      text.append("    Highest Value = " + max + "\n \n");

    } catch (Exception e) {
      return "Can't print the classifier.";
    }

    return text.toString();
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv the commandline parameters
   */
  public static void main(String[] argv) {

    runClassifier(new GaussianProcesses(), argv);
  }
}
