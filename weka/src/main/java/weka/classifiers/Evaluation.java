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
 *    Evaluation.java
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import weka.classifiers.evaluation.AbstractEvaluationMetric;
import weka.classifiers.evaluation.Prediction;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.Summarizable;

/**
 * Class for evaluating machine learning models. Delegates to the actual
 * implementation in weka.classifiers.evaluation.Evaluation.
 * 
 * <p/>
 * 
 * -------------------------------------------------------------------
 * <p/>
 * 
 * General options when evaluating a learning scheme from the command-line:
 * <p/>
 * 
 * -t filename <br/>
 * Name of the file with the training data. (required)
 * <p/>
 * 
 * -T filename <br/>
 * Name of the file with the test data. If missing a cross-validation is
 * performed.
 * <p/>
 * 
 * -c index <br/>
 * Index of the class attribute (1, 2, ...; default: last).
 * <p/>
 * 
 * -x number <br/>
 * The number of folds for the cross-validation (default: 10).
 * <p/>
 * 
 * -no-cv <br/>
 * No cross validation. If no test file is provided, no evaluation is done.
 * <p/>
 * 
 * -split-percentage percentage <br/>
 * Sets the percentage for the train/test set split, e.g., 66.
 * <p/>
 * 
 * -preserve-order <br/>
 * Preserves the order in the percentage split instead of randomizing the data
 * first with the seed value ('-s').
 * <p/>
 * 
 * -s seed <br/>
 * Random number seed for the cross-validation and percentage split (default:
 * 1).
 * <p/>
 * 
 * -m filename <br/>
 * The name of a file containing a cost matrix.
 * <p/>
 * 
 * -l filename <br/>
 * Loads classifier from the given file. In case the filename ends with ".xml",
 * a PMML file is loaded or, if that fails, options are loaded from XML.
 * <p/>
 * 
 * -d filename <br/>
 * Saves classifier built from the training data into the given file. In case
 * the filename ends with ".xml" the options are saved XML, not the model.
 * <p/>
 * 
 * -v <br/>
 * Outputs no statistics for the training data.
 * <p/>
 * 
 * -o <br/>
 * Outputs statistics only, not the classifier.
 * <p/>
 * 
 * -i <br/>
 * Outputs information-retrieval statistics per class.
 * <p/>
 * 
 * -k <br/>
 * Outputs information-theoretic statistics.
 * <p/>
 * 
 * -classifications
 * "weka.classifiers.evaluation.output.prediction.AbstractOutput + options" <br/>
 * Uses the specified class for generating the classification output. E.g.:
 * weka.classifiers.evaluation.output.prediction.PlainText or :
 * weka.classifiers.evaluation.output.prediction.CSV
 * 
 * -p range <br/>
 * Outputs predictions for test instances (or the train instances if no test
 * instances provided and -no-cv is used), along with the attributes in the
 * specified range (and nothing else). Use '-p 0' if no attributes are desired.
 * <p/>
 * Deprecated: use "-classifications ..." instead.
 * <p/>
 * 
 * -distribution <br/>
 * Outputs the distribution instead of only the prediction in conjunction with
 * the '-p' option (only nominal classes).
 * <p/>
 * Deprecated: use "-classifications ..." instead.
 * <p/>
 * 
 * -no-predictions <br/>
 * Turns off the collection of predictions in order to conserve memory.
 * <p/>
 * 
 * -r <br/>
 * Outputs cumulative margin distribution (and nothing else).
 * <p/>
 * 
 * -g <br/>
 * Only for classifiers that implement "Graphable." Outputs the graph
 * representation of the classifier (and nothing else).
 * <p/>
 * 
 * -xml filename | xml-string <br/>
 * Retrieves the options from the XML-data instead of the command line.
 * <p/>
 * 
 * -threshold-file file <br/>
 * The file to save the threshold data to. The format is determined by the
 * extensions, e.g., '.arff' for ARFF format or '.csv' for CSV.
 * <p/>
 * 
 * -threshold-label label <br/>
 * The class label to determine the threshold data for (default is the first
 * label)
 * <p/>
 * 
 * -------------------------------------------------------------------
 * <p/>
 * 
 * Example usage as the main of a classifier (called FunkyClassifier):
 * <code> <pre>
 * public static void main(String [] args) {
 *   runClassifier(new FunkyClassifier(), args);
 * }
 * </pre> </code>
 * <p/>
 * 
 * ------------------------------------------------------------------
 * <p/>
 * 
 * Example usage from within an application: <code> <pre>
 * Instances trainInstances = ... instances got from somewhere
 * Instances testInstances = ... instances got from somewhere
 * Classifier scheme = ... scheme got from somewhere
 * 
 * Evaluation evaluation = new Evaluation(trainInstances);
 * evaluation.evaluateModel(scheme, testInstances);
 * System.out.println(evaluation.toSummaryString());
 * </pre> </code>
 * 
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 12705 $
 */
public class Evaluation implements Serializable, Summarizable, RevisionHandler {

  /** For serialization */
  private static final long serialVersionUID = -170766452472965668L;

  public static final String[] BUILT_IN_EVAL_METRICS = weka.classifiers.evaluation.Evaluation.BUILT_IN_EVAL_METRICS;

  /** The actual evaluation object that we delegate to */
  protected weka.classifiers.evaluation.Evaluation m_delegate;

  /**
   * Utility method to get a list of the names of all built-in and plugin
   * evaluation metrics
   * 
   * @return the complete list of available evaluation metrics
   */
  public static List<String> getAllEvaluationMetricNames() {
    return weka.classifiers.evaluation.Evaluation.getAllEvaluationMetricNames();
  }

  public Evaluation(Instances data) throws Exception {
    m_delegate = new weka.classifiers.evaluation.Evaluation(data);
  }

  public Evaluation(Instances data, CostMatrix costMatrix) throws Exception {
    m_delegate = new weka.classifiers.evaluation.Evaluation(data, costMatrix);
  }

  /**
   * Returns the header of the underlying dataset.
   * 
   * @return the header information
   */
  public Instances getHeader() {
    return m_delegate.getHeader();
  }

  /**
   * Returns the list of plugin metrics in use (or null if there are none)
   * 
   * @return the list of plugin metrics
   */
  public List<AbstractEvaluationMetric> getPluginMetrics() {
    return m_delegate.getPluginMetrics();
  }

  /**
   * Get the named plugin evaluation metric
   * 
   * @param name the name of the metric (as returned by
   *          AbstractEvaluationMetric.getName()) or the fully qualified class
   *          name of the metric to find
   * 
   * @return the metric or null if the metric is not in the list of plugin
   *         metrics
   */
  public AbstractEvaluationMetric getPluginMetric(String name) {
    return m_delegate.getPluginMetric(name);
  }

  /**
   * Set a list of the names of metrics to have appear in the output. The
   * default is to display all built in metrics and plugin metrics that haven't
   * been globally disabled.
   * 
   * @param display a list of metric names to have appear in the output
   */
  public void setMetricsToDisplay(List<String> display) {
    m_delegate.setMetricsToDisplay(display);
  }

  /**
   * Get a list of the names of metrics to have appear in the output The default
   * is to display all built in metrics and plugin metrics that haven't been
   * globally disabled.
   * 
   * @return a list of metric names to have appear in the output
   */
  public List<String> getMetricsToDisplay() {
    return m_delegate.getMetricsToDisplay();
  }
  
  /**
   * Toggle the output of the metrics specified in the supplied list.
   *
   * @param metricsToToggle a list of metrics to toggle
   */
  public void toggleEvalMetrics(List<String> metricsToToggle) {
    m_delegate.toggleEvalMetrics(metricsToToggle);
  }

  /**
   * Sets whether to discard predictions, ie, not storing them for future
   * reference via predictions() method in order to conserve memory.
   * 
   * @param value true if to discard the predictions
   * @see #predictions()
   */
  public void setDiscardPredictions(boolean value) {
    m_delegate.setDiscardPredictions(value);
  }

  /**
   * Returns whether predictions are not recorded at all, in order to conserve
   * memory.
   * 
   * @return true if predictions are not recorded
   * @see #predictions()
   */
  public boolean getDiscardPredictions() {
    return m_delegate.getDiscardPredictions();
  }

  /**
   * Returns the area under ROC for those predictions that have been collected
   * in the evaluateClassifier(Classifier, Instances) method. Returns
   * Utils.missingValue() if the area is not available.
   * 
   * @param classIndex the index of the class to consider as "positive"
   * @return the area under the ROC curve or not a number
   */
  public double areaUnderROC(int classIndex) {
    return m_delegate.areaUnderROC(classIndex);
  }

  /**
   * Calculates the weighted (by class size) AUC.
   * 
   * @return the weighted AUC.
   */
  public double weightedAreaUnderROC() {
    return m_delegate.weightedAreaUnderROC();
  }

  /**
   * Returns the area under precision-recall curve (AUPRC) for those predictions
   * that have been collected in the evaluateClassifier(Classifier, Instances)
   * method. Returns Utils.missingValue() if the area is not available.
   * 
   * @param classIndex the index of the class to consider as "positive"
   * @return the area under the precision-recall curve or not a number
   */
  public double areaUnderPRC(int classIndex) {
    return m_delegate.areaUnderPRC(classIndex);
  }

  /**
   * Calculates the weighted (by class size) AUPRC.
   * 
   * @return the weighted AUPRC.
   */
  public double weightedAreaUnderPRC() {
    return m_delegate.weightedAreaUnderPRC();
  }

  /**
   * Returns a copy of the confusion matrix.
   * 
   * @return a copy of the confusion matrix as a two-dimensional array
   */
  public double[][] confusionMatrix() {
    return m_delegate.confusionMatrix();
  }

  /**
   * Performs a (stratified if class is nominal) cross-validation for a
   * classifier on a set of instances. Now performs a deep copy of the
   * classifier before each call to buildClassifier() (just in case the
   * classifier is not initialized properly).
   * 
   * @param classifier the classifier with any options set.
   * @param data the data on which the cross-validation is to be performed
   * @param numFolds the number of folds for the cross-validation
   * @param random random number generator for randomization
   * @param forPredictionsPrinting varargs parameter that, if supplied, is
   *          expected to hold a
   *          weka.classifiers.evaluation.output.prediction.AbstractOutput
   *          object
   * @throws Exception if a classifier could not be generated successfully or
   *           the class is not defined
   */
  public void crossValidateModel(Classifier classifier, Instances data,
    int numFolds, Random random, Object... forPredictionsPrinting)
    throws Exception {
    m_delegate.crossValidateModel(classifier, data, numFolds, random,
      forPredictionsPrinting);
  }

  /**
   * Performs a (stratified if class is nominal) cross-validation for a
   * classifier on a set of instances.
   * 
   * @param classifierString a string naming the class of the classifier
   * @param data the data on which the cross-validation is to be performed
   * @param numFolds the number of folds for the cross-validation
   * @param options the options to the classifier. Any options
   * @param random the random number generator for randomizing the data accepted
   *          by the classifier will be removed from this array.
   * @throws Exception if a classifier could not be generated successfully or
   *           the class is not defined
   */
  public void crossValidateModel(String classifierString, Instances data,
    int numFolds, String[] options, Random random) throws Exception {
    m_delegate.crossValidateModel(classifierString, data, numFolds, options,
      random);
  }

  /**
   * Evaluates a classifier with the options given in an array of strings.
   * <p/>
   * 
   * Valid options are:
   * <p/>
   * 
   * -t filename <br/>
   * Name of the file with the training data. (required)
   * <p/>
   * 
   * -T filename <br/>
   * Name of the file with the test data. If missing a cross-validation is
   * performed.
   * <p/>
   * 
   * -c index <br/>
   * Index of the class attribute (1, 2, ...; default: last).
   * <p/>
   * 
   * -x number <br/>
   * The number of folds for the cross-validation (default: 10).
   * <p/>
   * 
   * -no-cv <br/>
   * No cross validation. If no test file is provided, no evaluation is done.
   * <p/>
   * 
   * -split-percentage percentage <br/>
   * Sets the percentage for the train/test set split, e.g., 66.
   * <p/>
   * 
   * -preserve-order <br/>
   * Preserves the order in the percentage split instead of randomizing the data
   * first with the seed value ('-s').
   * <p/>
   * 
   * -s seed <br/>
   * Random number seed for the cross-validation and percentage split (default:
   * 1).
   * <p/>
   * 
   * -m filename <br/>
   * The name of a file containing a cost matrix.
   * <p/>
   * 
   * -l filename <br/>
   * Loads classifier from the given file. In case the filename ends with
   * ".xml",a PMML file is loaded or, if that fails, options are loaded from
   * XML.
   * <p/>
   * 
   * -d filename <br/>
   * Saves classifier built from the training data into the given file. In case
   * the filename ends with ".xml" the options are saved XML, not the model.
   * <p/>
   * 
   * -v <br/>
   * Outputs no statistics for the training data.
   * <p/>
   * 
   * -o <br/>
   * Outputs statistics only, not the classifier.
   * <p/>
   * 
   * -i <br/>
   * Outputs detailed information-retrieval statistics per class.
   * <p/>
   * 
   * -k <br/>
   * Outputs information-theoretic statistics.
   * <p/>
   * 
   * -classifications
   * "weka.classifiers.evaluation.output.prediction.AbstractOutput + options" <br/>
   * Uses the specified class for generating the classification output. E.g.:
   * weka.classifiers.evaluation.output.prediction.PlainText or :
   * weka.classifiers.evaluation.output.prediction.CSV
   * 
   * -p range <br/>
   * Outputs predictions for test instances (or the train instances if no test
   * instances provided and -no-cv is used), along with the attributes in the
   * specified range (and nothing else). Use '-p 0' if no attributes are
   * desired.
   * <p/>
   * Deprecated: use "-classifications ..." instead.
   * <p/>
   * 
   * -distribution <br/>
   * Outputs the distribution instead of only the prediction in conjunction with
   * the '-p' option (only nominal classes).
   * <p/>
   * Deprecated: use "-classifications ..." instead.
   * <p/>
   * 
   * -no-predictions <br/>
   * Turns off the collection of predictions in order to conserve memory.
   * <p/>
   * 
   * -r <br/>
   * Outputs cumulative margin distribution (and nothing else).
   * <p/>
   * 
   * -g <br/>
   * Only for classifiers that implement "Graphable." Outputs the graph
   * representation of the classifier (and nothing else).
   * <p/>
   * 
   * -xml filename | xml-string <br/>
   * Retrieves the options from the XML-data instead of the command line.
   * <p/>
   * 
   * -threshold-file file <br/>
   * The file to save the threshold data to. The format is determined by the
   * extensions, e.g., '.arff' for ARFF format or '.csv' for CSV.
   * <p/>
   * 
   * -threshold-label label <br/>
   * The class label to determine the threshold data for (default is the first
   * label)
   * <p/>
   * 
   * @param classifierString class of machine learning classifier as a string
   * @param options the array of string containing the options
   * @throws Exception if model could not be evaluated successfully
   * @return a string describing the results
   */
  public static String evaluateModel(String classifierString, String[] options)
    throws Exception {

    return weka.classifiers.evaluation.Evaluation.evaluateModel(
      classifierString, options);
  }

  /**
   * Evaluates a classifier with the options given in an array of strings.
   * <p/>
   * 
   * Valid options are:
   * <p/>
   * 
   * -t name of training file <br/>
   * Name of the file with the training data. (required)
   * <p/>
   * 
   * -T name of test file <br/>
   * Name of the file with the test data. If missing a cross-validation is
   * performed.
   * <p/>
   * 
   * -c class index <br/>
   * Index of the class attribute (1, 2, ...; default: last).
   * <p/>
   * 
   * -x number of folds <br/>
   * The number of folds for the cross-validation (default: 10).
   * <p/>
   * 
   * -no-cv <br/>
   * No cross validation. If no test file is provided, no evaluation is done.
   * <p/>
   * 
   * -split-percentage percentage <br/>
   * Sets the percentage for the train/test set split, e.g., 66.
   * <p/>
   * 
   * -preserve-order <br/>
   * Preserves the order in the percentage split instead of randomizing the data
   * first with the seed value ('-s').
   * <p/>
   * 
   * -s seed <br/>
   * Random number seed for the cross-validation and percentage split (default:
   * 1).
   * <p/>
   * 
   * -m file with cost matrix <br/>
   * The name of a file containing a cost matrix.
   * <p/>
   * 
   * -l filename <br/>
   * Loads classifier from the given file. In case the filename ends with
   * ".xml",a PMML file is loaded or, if that fails, options are loaded from
   * XML.
   * <p/>
   * 
   * -d filename <br/>
   * Saves classifier built from the training data into the given file. In case
   * the filename ends with ".xml" the options are saved XML, not the model.
   * <p/>
   * 
   * -v <br/>
   * Outputs no statistics for the training data.
   * <p/>
   * 
   * -o <br/>
   * Outputs statistics only, not the classifier.
   * <p/>
   * 
   * -i <br/>
   * Outputs detailed information-retrieval statistics per class.
   * <p/>
   * 
   * -k <br/>
   * Outputs information-theoretic statistics.
   * <p/>
   * 
   * -classifications
   * "weka.classifiers.evaluation.output.prediction.AbstractOutput + options" <br/>
   * Uses the specified class for generating the classification output. E.g.:
   * weka.classifiers.evaluation.output.prediction.PlainText or :
   * weka.classifiers.evaluation.output.prediction.CSV
   * 
   * -p range <br/>
   * Outputs predictions for test instances (or the train instances if no test
   * instances provided and -no-cv is used), along with the attributes in the
   * specified range (and nothing else). Use '-p 0' if no attributes are
   * desired.
   * <p/>
   * Deprecated: use "-classifications ..." instead.
   * <p/>
   * 
   * -distribution <br/>
   * Outputs the distribution instead of only the prediction in conjunction with
   * the '-p' option (only nominal classes).
   * <p/>
   * Deprecated: use "-classifications ..." instead.
   * <p/>
   * 
   * -no-predictions <br/>
   * Turns off the collection of predictions in order to conserve memory.
   * <p/>
   * 
   * -r <br/>
   * Outputs cumulative margin distribution (and nothing else).
   * <p/>
   * 
   * -g <br/>
   * Only for classifiers that implement "Graphable." Outputs the graph
   * representation of the classifier (and nothing else).
   * <p/>
   * 
   * -xml filename | xml-string <br/>
   * Retrieves the options from the XML-data instead of the command line.
   * <p/>
   * 
   * @param classifier machine learning classifier
   * @param options the array of string containing the options
   * @throws Exception if model could not be evaluated successfully
   * @return a string describing the results
   */
  public static String evaluateModel(Classifier classifier, String[] options)
    throws Exception {
    return weka.classifiers.evaluation.Evaluation.evaluateModel(classifier,
      options);
  }

  /**
   * Evaluates the classifier on a given set of instances. Note that the data
   * must have exactly the same format (e.g. order of attributes) as the data
   * used to train the classifier! Otherwise the results will generally be
   * meaningless.
   * 
   * @param classifier machine learning classifier
   * @param data set of test instances for evaluation
   * @param forPredictionsPrinting varargs parameter that, if supplied, is
   *          expected to hold a
   *          weka.classifiers.evaluation.output.prediction.AbstractOutput
   *          object
   * @return the predictions
   * @throws Exception if model could not be evaluated successfully
   */
  public double[] evaluateModel(Classifier classifier, Instances data,
    Object... forPredictionsPrinting) throws Exception {
    return m_delegate.evaluateModel(classifier, data, forPredictionsPrinting);
  }

  /**
   * Evaluates the supplied distribution on a single instance.
   * 
   * @param dist the supplied distribution
   * @param instance the test instance to be classified
   * @param storePredictions whether to store predictions for nominal classifier
   * @return the prediction
   * @throws Exception if model could not be evaluated successfully
   */
  public double evaluationForSingleInstance(double[] dist, Instance instance,
    boolean storePredictions) throws Exception {
    return m_delegate.evaluationForSingleInstance(dist, instance,
      storePredictions);
  }

  /**
   * Evaluates the classifier on a single instance and records the prediction.
   * 
   * @param classifier machine learning classifier
   * @param instance the test instance to be classified
   * @return the prediction made by the clasifier
   * @throws Exception if model could not be evaluated successfully or the data
   *           contains string attributes
   */
  public double evaluateModelOnceAndRecordPrediction(Classifier classifier,
    Instance instance) throws Exception {
    return m_delegate
      .evaluateModelOnceAndRecordPrediction(classifier, instance);
  }

  /**
   * Evaluates the classifier on a single instance.
   * 
   * @param classifier machine learning classifier
   * @param instance the test instance to be classified
   * @return the prediction made by the clasifier
   * @throws Exception if model could not be evaluated successfully or the data
   *           contains string attributes
   */
  public double evaluateModelOnce(Classifier classifier, Instance instance)
    throws Exception {
    return m_delegate.evaluateModelOnce(classifier, instance);
  }

  /**
   * Evaluates the supplied distribution on a single instance.
   * 
   * @param dist the supplied distribution
   * @param instance the test instance to be classified
   * @return the prediction
   * @throws Exception if model could not be evaluated successfully
   */
  public double evaluateModelOnce(double[] dist, Instance instance)
    throws Exception {
    return m_delegate.evaluateModelOnce(dist, instance);
  }

  /**
   * Evaluates the supplied distribution on a single instance.
   * 
   * @param dist the supplied distribution
   * @param instance the test instance to be classified
   * @return the prediction
   * @throws Exception if model could not be evaluated successfully
   */
  public double evaluateModelOnceAndRecordPrediction(double[] dist,
    Instance instance) throws Exception {
    return m_delegate.evaluateModelOnceAndRecordPrediction(dist, instance);
  }

  /**
   * Evaluates the supplied prediction on a single instance.
   * 
   * @param prediction the supplied prediction
   * @param instance the test instance to be classified
   * @throws Exception if model could not be evaluated successfully
   */
  public void evaluateModelOnce(double prediction, Instance instance)
    throws Exception {
    m_delegate.evaluateModelOnce(prediction, instance);
  }

  /**
   * Returns the predictions that have been collected.
   * 
   * @return a reference to the FastVector containing the predictions that have
   *         been collected. This should be null if no predictions have been
   *         collected.
   */
  public ArrayList<Prediction> predictions() {
    return m_delegate.predictions();
  }

  /**
   * Wraps a static classifier in enough source to test using the weka class
   * libraries.
   * 
   * @param classifier a Sourcable Classifier
   * @param className the name to give to the source code class
   * @return the source for a static classifier that can be tested with weka
   *         libraries.
   * @throws Exception if code-generation fails
   */
  public static String wekaStaticWrapper(Sourcable classifier, String className)
    throws Exception {
    return weka.classifiers.evaluation.Evaluation.wekaStaticWrapper(classifier,
      className);
  }

  /**
   * Gets the number of test instances that had a known class value (actually
   * the sum of the weights of test instances with known class value).
   * 
   * @return the number of test instances with known class
   */
  public final double numInstances() {
    return m_delegate.numInstances();
  }

  /**
   * Gets the coverage of the test cases by the predicted regions at the
   * confidence level specified when evaluation was performed.
   * 
   * @return the coverage of the test cases by the predicted regions
   */
  public final double coverageOfTestCasesByPredictedRegions() {
    return m_delegate.coverageOfTestCasesByPredictedRegions();
  }

  /**
   * Gets the average size of the predicted regions, relative to the range of
   * the target in the training data, at the confidence level specified when
   * evaluation was performed.
   * 
   * @return the average size of the predicted regions
   */
  public final double sizeOfPredictedRegions() {
    return m_delegate.sizeOfPredictedRegions();
  }

  /**
   * Gets the number of instances incorrectly classified (that is, for which an
   * incorrect prediction was made). (Actually the sum of the weights of these
   * instances)
   * 
   * @return the number of incorrectly classified instances
   */
  public final double incorrect() {
    return m_delegate.incorrect();
  }

  /**
   * Gets the percentage of instances incorrectly classified (that is, for which
   * an incorrect prediction was made).
   * 
   * @return the percent of incorrectly classified instances (between 0 and 100)
   */
  public final double pctIncorrect() {
    return m_delegate.pctIncorrect();
  }

  /**
   * Gets the total cost, that is, the cost of each prediction times the weight
   * of the instance, summed over all instances.
   * 
   * @return the total cost
   */
  public final double totalCost() {
    return m_delegate.totalCost();
  }

  /**
   * Gets the average cost, that is, total cost of misclassifications (incorrect
   * plus unclassified) over the total number of instances.
   * 
   * @return the average cost.
   */
  public final double avgCost() {
    return m_delegate.avgCost();
  }

  /**
   * Gets the number of instances correctly classified (that is, for which a
   * correct prediction was made). (Actually the sum of the weights of these
   * instances)
   * 
   * @return the number of correctly classified instances
   */
  public final double correct() {
    return m_delegate.correct();
  }

  /**
   * Gets the percentage of instances correctly classified (that is, for which a
   * correct prediction was made).
   * 
   * @return the percent of correctly classified instances (between 0 and 100)
   */
  public final double pctCorrect() {
    return m_delegate.pctCorrect();
  }

  /**
   * Gets the number of instances not classified (that is, for which no
   * prediction was made by the classifier). (Actually the sum of the weights of
   * these instances)
   * 
   * @return the number of unclassified instances
   */
  public final double unclassified() {
    return m_delegate.unclassified();
  }

  /**
   * Gets the percentage of instances not classified (that is, for which no
   * prediction was made by the classifier).
   * 
   * @return the percent of unclassified instances (between 0 and 100)
   */
  public final double pctUnclassified() {
    return m_delegate.pctUnclassified();
  }

  /**
   * Returns the estimated error rate or the root mean squared error (if the
   * class is numeric). If a cost matrix was given this error rate gives the
   * average cost.
   * 
   * @return the estimated error rate (between 0 and 1, or between 0 and maximum
   *         cost)
   */
  public final double errorRate() {
    return m_delegate.errorRate();
  }

  /**
   * Returns value of kappa statistic if class is nominal.
   * 
   * @return the value of the kappa statistic
   */
  public final double kappa() {
    return m_delegate.kappa();
  }

  @Override
  public String getRevision() {
    return m_delegate.getRevision();
  }

  /**
   * Returns the correlation coefficient if the class is numeric.
   * 
   * @return the correlation coefficient
   * @throws Exception if class is not numeric
   */
  public final double correlationCoefficient() throws Exception {
    return m_delegate.correlationCoefficient();
  }

  /**
   * Returns the mean absolute error. Refers to the error of the predicted
   * values for numeric classes, and the error of the predicted probability
   * distribution for nominal classes.
   * 
   * @return the mean absolute error
   */
  public final double meanAbsoluteError() {
    return m_delegate.meanAbsoluteError();
  }

  /**
   * Returns the mean absolute error of the prior.
   * 
   * @return the mean absolute error
   */
  public final double meanPriorAbsoluteError() {
    return m_delegate.meanPriorAbsoluteError();
  }

  /**
   * Returns the relative absolute error.
   * 
   * @return the relative absolute error
   * @throws Exception if it can't be computed
   */
  public final double relativeAbsoluteError() throws Exception {
    return m_delegate.relativeAbsoluteError();
  }

  /**
   * Returns the root mean squared error.
   * 
   * @return the root mean squared error
   */
  public final double rootMeanSquaredError() {
    return m_delegate.rootMeanSquaredError();
  }

  /**
   * Returns the root mean prior squared error.
   * 
   * @return the root mean prior squared error
   */
  public final double rootMeanPriorSquaredError() {
    return m_delegate.rootMeanPriorSquaredError();
  }

  /**
   * Returns the root relative squared error if the class is numeric.
   * 
   * @return the root relative squared error
   */
  public final double rootRelativeSquaredError() {
    return m_delegate.rootRelativeSquaredError();
  }

  /**
   * Calculate the entropy of the prior distribution.
   * 
   * @return the entropy of the prior distribution
   * @throws Exception if the class is not nominal
   */
  public final double priorEntropy() throws Exception {
    return m_delegate.priorEntropy();
  }

  /**
   * Return the total Kononenko & Bratko Information score in bits.
   * 
   * @return the K&B information score
   * @throws Exception if the class is not nominal
   */
  public final double KBInformation() throws Exception {
    return m_delegate.KBInformation();
  }

  /**
   * Return the Kononenko & Bratko Information score in bits per instance.
   * 
   * @return the K&B information score
   * @throws Exception if the class is not nominal
   */
  public final double KBMeanInformation() throws Exception {
    return m_delegate.KBMeanInformation();
  }

  /**
   * Return the Kononenko & Bratko Relative Information score.
   * 
   * @return the K&B relative information score
   * @throws Exception if the class is not nominal
   */
  public final double KBRelativeInformation() throws Exception {
    return m_delegate.KBRelativeInformation();
  }

  /**
   * Returns the total entropy for the null model.
   * 
   * @return the total null model entropy
   */
  public final double SFPriorEntropy() {
    return m_delegate.SFPriorEntropy();
  }

  /**
   * Returns the entropy per instance for the null model.
   * 
   * @return the null model entropy per instance
   */
  public final double SFMeanPriorEntropy() {
    return m_delegate.SFMeanPriorEntropy();
  }

  /**
   * Returns the total entropy for the scheme.
   * 
   * @return the total scheme entropy
   */
  public final double SFSchemeEntropy() {
    return m_delegate.SFSchemeEntropy();
  }

  /**
   * Returns the entropy per instance for the scheme.
   * 
   * @return the scheme entropy per instance
   */
  public final double SFMeanSchemeEntropy() {
    return m_delegate.SFMeanSchemeEntropy();
  }

  /**
   * Returns the total SF, which is the null model entropy minus the scheme
   * entropy.
   * 
   * @return the total SF
   */
  public final double SFEntropyGain() {
    return m_delegate.SFEntropyGain();
  }

  /**
   * Returns the SF per instance, which is the null model entropy minus the
   * scheme entropy, per instance.
   * 
   * @return the SF per instance
   */
  public final double SFMeanEntropyGain() {
    return m_delegate.SFMeanEntropyGain();
  }

  /**
   * Output the cumulative margin distribution as a string suitable for input
   * for gnuplot or similar package.
   * 
   * @return the cumulative margin distribution
   * @throws Exception if the class attribute is nominal
   */
  public String toCumulativeMarginDistributionString() throws Exception {
    return m_delegate.toCumulativeMarginDistributionString();
  }

  /**
   * Calls toSummaryString() with no title and no complexity stats.
   * 
   * @return a summary description of the classifier evaluation
   */
  @Override
  public String toSummaryString() {
    return m_delegate.toSummaryString();
  }

  /**
   * Calls toSummaryString() with a default title.
   * 
   * @param printComplexityStatistics if true, complexity statistics are
   *          returned as well
   * @return the summary string
   */
  public String toSummaryString(boolean printComplexityStatistics) {
    return m_delegate.toSummaryString(printComplexityStatistics);
  }

  /**
   * Outputs the performance statistics in summary form. Lists number (and
   * percentage) of instances classified correctly, incorrectly and
   * unclassified. Outputs the total number of instances classified, and the
   * number of instances (if any) that had no class value provided.
   * 
   * @param title the title for the statistics
   * @param printComplexityStatistics if true, complexity statistics are
   *          returned as well
   * @return the summary as a String
   */
  public String toSummaryString(String title, boolean printComplexityStatistics) {
    return m_delegate.toSummaryString(title, printComplexityStatistics);
  }

  /**
   * Calls toMatrixString() with a default title.
   * 
   * @return the confusion matrix as a string
   * @throws Exception if the class is numeric
   */
  public String toMatrixString() throws Exception {
    return m_delegate.toMatrixString();
  }

  /**
   * Outputs the performance statistics as a classification confusion matrix.
   * For each class value, shows the distribution of predicted class values.
   * 
   * @param title the title for the confusion matrix
   * @return the confusion matrix as a String
   * @throws Exception if the class is numeric
   */
  public String toMatrixString(String title) throws Exception {
    return m_delegate.toMatrixString(title);
  }

  /**
   * Generates a breakdown of the accuracy for each class (with default title),
   * incorporating various information-retrieval statistics, such as true/false
   * positive rate, precision/recall/F-Measure. Should be useful for ROC curves,
   * recall/precision curves.
   * 
   * @return the statistics presented as a string
   * @throws Exception if class is not nominal
   */
  public String toClassDetailsString() throws Exception {
    return m_delegate.toClassDetailsString();
  }

  /**
   * Generates a breakdown of the accuracy for each class, incorporating various
   * information-retrieval statistics, such as true/false positive rate,
   * precision/recall/F-Measure. Should be useful for ROC curves,
   * recall/precision curves.
   * 
   * @param title the title to prepend the stats string with
   * @return the statistics presented as a string
   * @throws Exception if class is not nominal
   */
  public String toClassDetailsString(String title) throws Exception {
    return m_delegate.toClassDetailsString(title);
  }

  /**
   * Calculate the number of true positives with respect to a particular class.
   * This is defined as
   * <p/>
   * 
   * <pre>
   * correctly classified positives
   * </pre>
   * 
   * @param classIndex the index of the class to consider as "positive"
   * @return the true positive rate
   */
  public double numTruePositives(int classIndex) {
    return m_delegate.numTruePositives(classIndex);
  }

  /**
   * Calculate the true positive rate with respect to a particular class. This
   * is defined as
   * <p/>
   * 
   * <pre>
   * correctly classified positives
   * ------------------------------
   *       total positives
   * </pre>
   * 
   * @param classIndex the index of the class to consider as "positive"
   * @return the true positive rate
   */
  public double truePositiveRate(int classIndex) {
    return m_delegate.truePositiveRate(classIndex);
  }

  /**
   * Calculates the weighted (by class size) true positive rate.
   * 
   * @return the weighted true positive rate.
   */
  public double weightedTruePositiveRate() {
    return m_delegate.weightedTruePositiveRate();
  }

  /**
   * Calculate the number of true negatives with respect to a particular class.
   * This is defined as
   * <p/>
   * 
   * <pre>
   * correctly classified negatives
   * </pre>
   * 
   * @param classIndex the index of the class to consider as "positive"
   * @return the true positive rate
   */
  public double numTrueNegatives(int classIndex) {
    return m_delegate.numTrueNegatives(classIndex);
  }

  /**
   * Calculate the true negative rate with respect to a particular class. This
   * is defined as
   * <p/>
   * 
   * <pre>
   * correctly classified negatives
   * ------------------------------
   *       total negatives
   * </pre>
   * 
   * @param classIndex the index of the class to consider as "positive"
   * @return the true positive rate
   */
  public double trueNegativeRate(int classIndex) {
    return m_delegate.trueNegativeRate(classIndex);
  }

  /**
   * Calculates the weighted (by class size) true negative rate.
   * 
   * @return the weighted true negative rate.
   */
  public double weightedTrueNegativeRate() {
    return m_delegate.weightedTrueNegativeRate();
  }

  /**
   * Calculate number of false positives with respect to a particular class.
   * This is defined as
   * <p/>
   * 
   * <pre>
   * incorrectly classified negatives
   * </pre>
   * 
   * @param classIndex the index of the class to consider as "positive"
   * @return the false positive rate
   */
  public double numFalsePositives(int classIndex) {
    return m_delegate.numFalsePositives(classIndex);
  }

  /**
   * Calculate the false positive rate with respect to a particular class. This
   * is defined as
   * <p/>
   * 
   * <pre>
   * incorrectly classified negatives
   * --------------------------------
   *        total negatives
   * </pre>
   * 
   * @param classIndex the index of the class to consider as "positive"
   * @return the false positive rate
   */
  public double falsePositiveRate(int classIndex) {
    return m_delegate.falsePositiveRate(classIndex);
  }

  /**
   * Calculates the weighted (by class size) false positive rate.
   * 
   * @return the weighted false positive rate.
   */
  public double weightedFalsePositiveRate() {
    return m_delegate.weightedFalsePositiveRate();
  }

  /**
   * Calculate number of false negatives with respect to a particular class.
   * This is defined as
   * <p/>
   * 
   * <pre>
   * incorrectly classified positives
   * </pre>
   * 
   * @param classIndex the index of the class to consider as "positive"
   * @return the false positive rate
   */
  public double numFalseNegatives(int classIndex) {
    return m_delegate.numFalseNegatives(classIndex);
  }

  /**
   * Calculate the false negative rate with respect to a particular class. This
   * is defined as
   * <p/>
   * 
   * <pre>
   * incorrectly classified positives
   * --------------------------------
   *        total positives
   * </pre>
   * 
   * @param classIndex the index of the class to consider as "positive"
   * @return the false positive rate
   */
  public double falseNegativeRate(int classIndex) {
    return m_delegate.falseNegativeRate(classIndex);
  }

  /**
   * Calculates the weighted (by class size) false negative rate.
   * 
   * @return the weighted false negative rate.
   */
  public double weightedFalseNegativeRate() {
    return m_delegate.weightedFalseNegativeRate();
  }

  /**
   * Calculates the matthews correlation coefficient (sometimes called phi
   * coefficient) for the supplied class
   * 
   * @param classIndex the index of the class to compute the matthews
   *          correlation coefficient for
   * 
   * @return the mathews correlation coefficient
   */
  public double matthewsCorrelationCoefficient(int classIndex) {
    return m_delegate.matthewsCorrelationCoefficient(classIndex);
  }

  /**
   * Calculates the weighted (by class size) matthews correlation coefficient.
   * 
   * @return the weighted matthews correlation coefficient.
   */
  public double weightedMatthewsCorrelation() {
    return m_delegate.weightedMatthewsCorrelation();
  }

  /**
   * Calculate the recall with respect to a particular class. This is defined as
   * <p/>
   * 
   * <pre>
   * correctly classified positives
   * ------------------------------
   *       total positives
   * </pre>
   * <p/>
   * (Which is also the same as the truePositiveRate.)
   * 
   * @param classIndex the index of the class to consider as "positive"
   * @return the recall
   */
  public double recall(int classIndex) {
    return m_delegate.recall(classIndex);
  }

  /**
   * Calculates the weighted (by class size) recall.
   * 
   * @return the weighted recall.
   */
  public double weightedRecall() {
    return m_delegate.weightedRecall();
  }

  /**
   * Calculate the precision with respect to a particular class. This is defined
   * as
   * <p/>
   * 
   * <pre>
   * correctly classified positives
   * ------------------------------
   *  total predicted as positive
   * </pre>
   * 
   * @param classIndex the index of the class to consider as "positive"
   * @return the precision
   */
  public double precision(int classIndex) {
    return m_delegate.precision(classIndex);
  }

  /**
   * Calculates the weighted (by class size) precision.
   * 
   * @return the weighted precision.
   */
  public double weightedPrecision() {
    return m_delegate.weightedPrecision();
  }

  /**
   * Calculate the F-Measure with respect to a particular class. This is defined
   * as
   * <p/>
   * 
   * <pre>
   * 2 * recall * precision
   * ----------------------
   *   recall + precision
   * </pre>
   * 
   * @param classIndex the index of the class to consider as "positive"
   * @return the F-Measure
   */
  public double fMeasure(int classIndex) {
    return m_delegate.fMeasure(classIndex);
  }

  /**
   * Calculates the macro weighted (by class size) average F-Measure.
   * 
   * @return the weighted F-Measure.
   */
  public double weightedFMeasure() {
    return m_delegate.weightedFMeasure();
  }

  /**
   * Unweighted macro-averaged F-measure. If some classes not present in the
   * test set, they're just skipped (since recall is undefined there anyway) .
   * 
   * @return unweighted macro-averaged F-measure.
   * */
  public double unweightedMacroFmeasure() {
    return m_delegate.unweightedMacroFmeasure();
  }

  /**
   * Unweighted micro-averaged F-measure. If some classes not present in the
   * test set, they have no effect.
   * 
   * Note: if the test set is *single-label*, then this is the same as accuracy.
   * 
   * @return unweighted micro-averaged F-measure.
   */
  public double unweightedMicroFmeasure() {
    return m_delegate.unweightedMicroFmeasure();
  }

  /**
   * Sets the class prior probabilities.
   * 
   * @param train the training instances used to determine the prior
   *          probabilities
   * @throws Exception if the class attribute of the instances is not set
   */
  public void setPriors(Instances train) throws Exception {
    m_delegate.setPriors(train);
  }

  /**
   * Get the current weighted class counts.
   * 
   * @return the weighted class counts
   */
  public double[] getClassPriors() {
    return m_delegate.getClassPriors();
  }

  /**
   * Updates the class prior probabilities or the mean respectively (when
   * incrementally training).
   * 
   * @param instance the new training instance seen
   * @throws Exception if the class of the instance is not set
   */
  public void updatePriors(Instance instance) throws Exception {
    m_delegate.updatePriors(instance);
  }

  /**
   * disables the use of priors, e.g., in case of de-serialized schemes that
   * have no access to the original training set, but are evaluated on a set
   * set.
   */
  public void useNoPriors() {
    m_delegate.useNoPriors();
  }

  /**
   * Tests whether the current evaluation object is equal to another evaluation
   * object.
   * 
   * @param obj the object to compare against
   * @return true if the two objects are equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof weka.classifiers.Evaluation) {
      obj = ((weka.classifiers.Evaluation) obj).m_delegate;
    }
    return m_delegate.equals(obj);
  }

  /**
   * A test method for this class. Just extracts the first command line argument
   * as a classifier class name and calls evaluateModel.
   * 
   * @param args an array of command line arguments, the first of which must be
   *          the class name of a classifier.
   */
  public static void main(String[] args) {

    try {
      if (args.length == 0) {
        throw new Exception("The first argument must be the class name"
          + " of a classifier");
      }
      String classifier = args[0];
      args[0] = "";
      System.out.println(evaluateModel(classifier, args));
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }

}
