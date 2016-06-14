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
 *    CostSensitiveClassifier.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.core.*;
import weka.core.Capabilities.Capability;

/**
 <!-- globalinfo-start -->
 * A metaclassifier that makes its base classifier cost-sensitive. Two methods can be used to introduce cost-sensitivity: reweighting training instances according to the total cost assigned to each class; or predicting the class with minimum expected misclassification cost (rather than the most likely class). Performance can often be improved by using a Bagged classifier to improve the probability estimates of the base classifier.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -M
 *  Minimize expected misclassification cost. Default is to
 *  reweight training instances according to costs per class</pre>
 * 
 * <pre> -C &lt;cost file name&gt;
 *  File name of a cost matrix to use. If this is not supplied,
 *  a cost matrix will be loaded on demand. The name of the
 *  on-demand file is the relation name of the training data
 *  plus ".cost", and the path to the on-demand file is
 *  specified with the -N option.</pre>
 * 
 * <pre> -N &lt;directory&gt;
 *  Name of a directory to search for cost files when loading
 *  costs on demand (default current directory).</pre>
 * 
 * <pre> -cost-matrix &lt;matrix&gt;
 *  The cost matrix in Matlab single line format.</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.rules.ZeroR)</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.rules.ZeroR:
 * </pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * Options after -- are passed to the designated classifier.<p>
 *
 * @author Len Trigg (len@reeltwo.com)
 * @version $Revision: 12180 $
 */
public class CostSensitiveClassifier 
  extends RandomizableSingleClassifierEnhancer
  implements OptionHandler, Drawable, BatchPredictor {

  /** for serialization */
  static final long serialVersionUID = -110658209263002404L;
  
  /** load cost matrix on demand */
  public static final int MATRIX_ON_DEMAND = 1;
  /** use explicit cost matrix */
  public static final int MATRIX_SUPPLIED = 2;
  /** Specify possible sources of the cost matrix */
  public static final Tag [] TAGS_MATRIX_SOURCE = {
    new Tag(MATRIX_ON_DEMAND, "Load cost matrix on demand"),
    new Tag(MATRIX_SUPPLIED, "Use explicit cost matrix")
  };

  /** Indicates the current cost matrix source */
  protected int m_MatrixSource = MATRIX_ON_DEMAND;

  /** 
   * The directory used when loading cost files on demand, null indicates
   * current directory 
   */
  protected File m_OnDemandDirectory = new File(System.getProperty("user.dir"));

  /** The name of the cost file, for command line options */
  protected String m_CostFile;

  /** The cost matrix */
  protected CostMatrix m_CostMatrix = new CostMatrix(1);

  /** 
   * True if the costs should be used by selecting the minimum expected
   * cost (false means weight training data by the costs)
   */
  protected boolean m_MinimizeExpectedCost;
  
  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname 
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.rules.ZeroR";
  }

  /**
   * Default constructor.
   */
  public CostSensitiveClassifier() {
    m_Classifier = new weka.classifiers.rules.ZeroR();
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(4);

    newVector.addElement(new Option(
	      "\tMinimize expected misclassification cost. Default is to\n"
	      +"\treweight training instances according to costs per class",
	      "M", 0, "-M"));
    newVector.addElement(new Option(
	      "\tFile name of a cost matrix to use. If this is not supplied,\n"
              +"\ta cost matrix will be loaded on demand. The name of the\n"
              +"\ton-demand file is the relation name of the training data\n"
              +"\tplus \".cost\", and the path to the on-demand file is\n"
              +"\tspecified with the -N option.",
	      "C", 1, "-C <cost file name>"));
    newVector.addElement(new Option(
              "\tName of a directory to search for cost files when loading\n"
              +"\tcosts on demand (default current directory).",
              "N", 1, "-N <directory>"));
    newVector.addElement(new Option(
              "\tThe cost matrix in Matlab single line format.",
              "cost-matrix", 1, "-cost-matrix <matrix>"));

    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -M
   *  Minimize expected misclassification cost. Default is to
   *  reweight training instances according to costs per class</pre>
   * 
   * <pre> -C &lt;cost file name&gt;
   *  File name of a cost matrix to use. If this is not supplied,
   *  a cost matrix will be loaded on demand. The name of the
   *  on-demand file is the relation name of the training data
   *  plus ".cost", and the path to the on-demand file is
   *  specified with the -N option.</pre>
   * 
   * <pre> -N &lt;directory&gt;
   *  Name of a directory to search for cost files when loading
   *  costs on demand (default current directory).</pre>
   * 
   * <pre> -cost-matrix &lt;matrix&gt;
   *  The cost matrix in Matlab single line format.</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.rules.ZeroR)</pre>
   * 
   * <pre> 
   * Options specific to classifier weka.classifiers.rules.ZeroR:
   * </pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   <!-- options-end -->
   *
   * Options after -- are passed to the designated classifier.<p>
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setMinimizeExpectedCost(Utils.getFlag('M', options));

    String costFile = Utils.getOption('C', options);
    if (costFile.length() != 0) {
      try {
	setCostMatrix(new CostMatrix(new BufferedReader(
				     new FileReader(costFile))));
      } catch (Exception ex) {
	// now flag as possible old format cost matrix. Delay cost matrix
	// loading until buildClassifer is called
	setCostMatrix(null);
      }
      setCostMatrixSource(new SelectedTag(MATRIX_SUPPLIED,
                                          TAGS_MATRIX_SOURCE));
      m_CostFile = costFile;
    } else {
      setCostMatrixSource(new SelectedTag(MATRIX_ON_DEMAND, 
                                          TAGS_MATRIX_SOURCE));
    }
    
    String demandDir = Utils.getOption('N', options);
    if (demandDir.length() != 0) {
      setOnDemandDirectory(new File(demandDir));
    }

    String cost_matrix = Utils.getOption("cost-matrix", options);
    if (cost_matrix.length() != 0) {
      StringWriter writer = new StringWriter();
      CostMatrix.parseMatlab(cost_matrix).write(writer);
      setCostMatrix(new CostMatrix(new StringReader(writer.toString())));
      setCostMatrixSource(new SelectedTag(MATRIX_SUPPLIED,
                                          TAGS_MATRIX_SOURCE));
    }
    
    super.setOptions(options);
    
    Utils.checkForRemainingOptions(options);
  }


  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    
    Vector<String> options = new Vector<String>();

    if (m_MatrixSource == MATRIX_SUPPLIED) {
      if (m_CostFile != null) {
          options.add("-C");
          options.add("" + m_CostFile);
      }
      else {
          options.add("-cost-matrix");
          options.add(getCostMatrix().toMatlab());
      }
    } else {
        options.add("-N");
        options.add("" + getOnDemandDirectory());
    }

    if (getMinimizeExpectedCost()) {
        options.add("-M");
    }

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }

  /**
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "A metaclassifier that makes its base classifier cost-sensitive. "
      + "Two methods can be used to introduce cost-sensitivity: reweighting "
      + "training instances according to the total cost assigned to each "
      + "class; or predicting the class with minimum expected "
      + "misclassification cost (rather than the most likely class). "
      + "Performance can often be "
      + "improved by using a Bagged classifier to improve the probability "
      + "estimates of the base classifier.";
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String costMatrixSourceTipText() {

    return "Sets where to get the cost matrix. The two options are"
      + "to use the supplied explicit cost matrix (the setting of the "
      + "costMatrix property), or to load a cost matrix from a file when "
      + "required (this file will be loaded from the directory set by the "
      + "onDemandDirectory property and will be named relation_name" 
      + CostMatrix.FILE_EXTENSION + ").";
  }

  /**
   * Gets the source location method of the cost matrix. Will be one of
   * MATRIX_ON_DEMAND or MATRIX_SUPPLIED.
   *
   * @return the cost matrix source.
   */
  public SelectedTag getCostMatrixSource() {

    return new SelectedTag(m_MatrixSource, TAGS_MATRIX_SOURCE);
  }
  
  /**
   * Sets the source location of the cost matrix. Values other than
   * MATRIX_ON_DEMAND or MATRIX_SUPPLIED will be ignored.
   *
   * @param newMethod the cost matrix location method.
   */
  public void setCostMatrixSource(SelectedTag newMethod) {
    
    if (newMethod.getTags() == TAGS_MATRIX_SOURCE) {
      m_MatrixSource = newMethod.getSelectedTag().getID();
    }
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String onDemandDirectoryTipText() {

    return "Sets the directory where cost files are loaded from. This option "
      + "is used when the costMatrixSource is set to \"On Demand\".";
  }

  /**
   * Returns the directory that will be searched for cost files when
   * loading on demand.
   *
   * @return The cost file search directory.
   */
  public File getOnDemandDirectory() {

    return m_OnDemandDirectory;
  }

  /**
   * Sets the directory that will be searched for cost files when
   * loading on demand.
   *
   * @param newDir The cost file search directory.
   */
  public void setOnDemandDirectory(File newDir) {

    if (newDir.isDirectory()) {
      m_OnDemandDirectory = newDir;
    } else {
      m_OnDemandDirectory = new File(newDir.getParent());
    }
    m_MatrixSource = MATRIX_ON_DEMAND;
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String minimizeExpectedCostTipText() {

    return "Sets whether the minimum expected cost criteria will be used. If "
      + "this is false, the training data will be reweighted according to the "
      + "costs assigned to each class. If true, the minimum expected cost "
      + "criteria will be used.";
  }

  /**
   * Gets the value of MinimizeExpectedCost.
   *
   * @return Value of MinimizeExpectedCost.
   */
  public boolean getMinimizeExpectedCost() {
    
    return m_MinimizeExpectedCost;
  }
  
  /**
   * Set the value of MinimizeExpectedCost.
   *
   * @param newMinimizeExpectedCost Value to assign to MinimizeExpectedCost.
   */
  public void setMinimizeExpectedCost(boolean newMinimizeExpectedCost) {
    
    m_MinimizeExpectedCost = newMinimizeExpectedCost;
  }
  
  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @return the classifier string.
   */
  protected String getClassifierSpec() {
    
    Classifier c = getClassifier();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }
  
  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String costMatrixTipText() {
    return "Sets the cost matrix explicitly. This matrix is used if the "
      + "costMatrixSource property is set to \"Supplied\".";
  }

  /**
   * Gets the misclassification cost matrix.
   *
   * @return the cost matrix
   */
  public CostMatrix getCostMatrix() {
    
    return m_CostMatrix;
  }
  
  /**
   * Sets the misclassification cost matrix.
   *
   * @param newCostMatrix the cost matrix
   */
  public void setCostMatrix(CostMatrix newCostMatrix) {
    
    m_CostMatrix = newCostMatrix;
    m_MatrixSource = MATRIX_SUPPLIED;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    result.enable(Capability.NOMINAL_CLASS);
    
    return result;
  }

  /**
   * Builds the model of the base learner.
   *
   * @param data the training data
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();
    
    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }
    if (m_MatrixSource == MATRIX_ON_DEMAND) {
      String costName = data.relationName() + CostMatrix.FILE_EXTENSION;
      File costFile = new File(getOnDemandDirectory(), costName);
      if (!costFile.exists()) {
        throw new Exception("On-demand cost file doesn't exist: " + costFile);
      }
      setCostMatrix(new CostMatrix(new BufferedReader(
                                   new FileReader(costFile))));
    } else if (m_CostMatrix == null) {
      // try loading an old format cost file
      m_CostMatrix = new CostMatrix(data.numClasses());
      m_CostMatrix.readOldFormat(new BufferedReader(
			       new FileReader(m_CostFile)));
    }

    if (!m_MinimizeExpectedCost) {
      Random random = null;
      if (!(m_Classifier instanceof WeightedInstancesHandler)) {
	random = new Random(m_Seed);
      }
      data = m_CostMatrix.applyCostMatrix(data, random);      
    }
    m_Classifier.buildClassifier(data);
  }

  /**
   * Returns class probabilities. When minimum expected cost approach is chosen,
   * returns probability one for class with the minimum expected misclassification
   * cost. Otherwise it returns the probability distribution returned by
   * the base classifier.
   *
   * @param instance the instance to be classified
   * @return the computed distribution for the given instance
   * @throws Exception if instance could not be classified
   * successfully */
  public double[] distributionForInstance(Instance instance) throws Exception {

    if (!m_MinimizeExpectedCost) {
      return m_Classifier.distributionForInstance(instance);
    } else {
      return convertDistribution(m_Classifier.distributionForInstance(instance), instance);
    }
  }

  /**
   * Convert distribution using minimum expected cost approach. The incoming
   * array is modified and returned!
   *
   * @param pred the predicted distribution
   * @param instance the instance
   * @return the modified distribution
   */
  protected double[] convertDistribution(double[] pred, Instance instance) throws Exception {

    double [] costs = m_CostMatrix.expectedCosts(pred, instance);

    // This is probably not ideal
    int classIndex = Utils.minIndex(costs);
    for (int i = 0; i  < pred.length; i++) {
      if (i == classIndex) {
        pred[i] = 1.0;
      } else {
        pred[i] = 0.0;
      }
    }
    return pred;
  }

  /**
   * Batch scoring method. Calls the appropriate method for the base learner if
   * it implements BatchPredictor. Otherwise it simply calls the
   * distributionForInstance() method repeatedly.
   *
   * @param insts the instances to get predictions for
   * @return an array of probability distributions, one for each instance
   * @throws Exception if a problem occurs
   */
  public double[][] distributionsForInstances(Instances insts) throws Exception {

    if (getClassifier() instanceof BatchPredictor) {
      double[][] dists = ((BatchPredictor) getClassifier()).distributionsForInstances(insts);
      if (!m_MinimizeExpectedCost) {
        return dists;
      } else {
        for (int i = 0; i < dists.length; i++) {
          dists[i] = convertDistribution(dists[i], insts.instance(i));
        }
        return dists;
      }
    } else {
      double[][] result = new double[insts.numInstances()][insts.numClasses()];
      for (int i = 0; i < insts.numInstances(); i++) {
        result[i] = distributionForInstance(insts.instance(i));
      }
      return result;
    }
  }

  /**
   * Tool tip text for this property
   *
   * @return the tool tip for this property
   */
  public String batchSizeTipText() {
    return "Batch size to use if base learner is a BatchPredictor";
  }

  /**
   * Set the batch size to use. Gets passed through to the base learner if it
   * implements BatchPrecitor. Otherwise it is just ignored.
   *
   * @param size the batch size to use
   */
  public void setBatchSize(String size) {

    if (getClassifier() instanceof BatchPredictor) {
      ((BatchPredictor) getClassifier()).setBatchSize(size);
    }
  }

  /**
   * Gets the preferred batch size from the base learner if it implements
   * BatchPredictor. Returns 1 as the preferred batch size otherwise.
   *
   * @return the batch size to use
   */
  public String getBatchSize() {

    if (getClassifier() instanceof BatchPredictor) {
      return ((BatchPredictor) getClassifier()).getBatchSize();
    } else {
      return "1";
    }
  }

  /**
   * Returns true if the base classifier implements BatchPredictor and is able
   * to generate batch predictions efficiently
   *
   * @return true if the base classifier can generate batch predictions
   *         efficiently
   */
  public boolean implementsMoreEfficientBatchPrediction() {
    if (!(getClassifier() instanceof BatchPredictor)) {
      return false;
    }

    return ((BatchPredictor) getClassifier())
            .implementsMoreEfficientBatchPrediction();
  }

  /**
   *  Returns the type of graph this classifier
   *  represents.
   *  
   *  @return the type of graph this classifier represents
   */   
  public int graphType() {
    
    if (m_Classifier instanceof Drawable)
      return ((Drawable)m_Classifier).graphType();
    else 
      return Drawable.NOT_DRAWABLE;
  }

  /**
   * Returns graph describing the classifier (if possible).
   *
   * @return the graph of the classifier in dotty format
   * @throws Exception if the classifier cannot be graphed
   */
  public String graph() throws Exception {
    
    if (m_Classifier instanceof Drawable)
      return ((Drawable)m_Classifier).graph();
    else throw new Exception("Classifier: " + getClassifierSpec()
			     + " cannot be graphed");
  }

  /**
   * Output a representation of this classifier
   * 
   * @return a string representation of the classifier
   */
  public String toString() {

    if (m_Classifier == null) {
      return "CostSensitiveClassifier: No model built yet.";
    }

    String result = "CostSensitiveClassifier using ";
      if (m_MinimizeExpectedCost) {
	result += "minimized expected misclasification cost\n";
      } else {
	result += "reweighted training instances\n";
      }
      result += "\n" + getClassifierSpec()
	+ "\n\nClassifier Model\n"
	+ m_Classifier.toString()
	+ "\n\nCost Matrix\n"
	+ m_CostMatrix.toString();

    return result;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12180 $");
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {
    runClassifier(new CostSensitiveClassifier(), argv);
  }
}
