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
 * ClassifierErrorsPlotInstances.java
 * Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.explorer;

import java.util.ArrayList;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.IntervalEstimator;
import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.gui.visualize.Plot2D;
import weka.gui.visualize.PlotData2D;

/**
 * A class for generating plottable visualization errors.
 * <p/>
 * Example usage:
 * 
 * <pre>
 * Instances train = ... // from somewhere
 * Instances test = ... // from somewhere
 * Classifier cls = ... // from somewhere
 * // build classifier
 * cls.buildClassifier(train);
 * // evaluate classifier and generate plot instances
 * ClassifierPlotInstances plotInstances = new ClassifierPlotInstances();
 * plotInstances.setClassifier(cls);
 * plotInstances.setInstances(train);
 * plotInstances.setClassIndex(train.classIndex());
 * plotInstances.setUp();
 * Evaluation eval = new Evaluation(train);
 * for (int i = 0; i &lt; test.numInstances(); i++)
 *   plotInstances.process(test.instance(i), cls, eval);
 * // generate visualization
 * VisualizePanel visPanel = new VisualizePanel();
 * visPanel.addPlot(plotInstances.getPlotData("plot name"));
 * visPanel.setColourIndex(plotInstances.getPlotInstances().classIndex()+1);
 * // clean up
 * plotInstances.cleanUp();
 * </pre>
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10220 $
 */
public class ClassifierErrorsPlotInstances extends AbstractPlotInstances {

  /** for serialization. */
  private static final long serialVersionUID = -3941976365792013279L;

  /** the minimum plot size for numeric errors. */
  protected int m_MinimumPlotSizeNumeric;

  /** the maximum plot size for numeric errors. */
  protected int m_MaximumPlotSizeNumeric;

  /**
   * whether to save the instances for visualization or just evaluate the
   * instance.
   */
  protected boolean m_SaveForVisualization;

  protected boolean m_pointSizeProportionalToMargin;

  /** for storing the plot shapes. */
  protected ArrayList<Integer> m_PlotShapes;

  /** for storing the plot sizes. */
  protected ArrayList<Object> m_PlotSizes;

  /** the classifier being used. */
  protected Classifier m_Classifier;

  /** the class index. */
  protected int m_ClassIndex;

  /** the Evaluation object to use. */
  protected Evaluation m_Evaluation;

  /**
   * Initializes the members.
   */
  @Override
  protected void initialize() {
    super.initialize();

    m_PlotShapes = new ArrayList<Integer>();
    m_PlotSizes = new ArrayList<Object>();
    m_Classifier = null;
    m_ClassIndex = -1;
    m_Evaluation = null;
    m_SaveForVisualization = true;
    m_MinimumPlotSizeNumeric = ExplorerDefaults
      .getClassifierErrorsMinimumPlotSizeNumeric();
    m_MaximumPlotSizeNumeric = ExplorerDefaults
      .getClassifierErrorsMaximumPlotSizeNumeric();
  }

  /**
   * Get the vector of plot shapes (see weka.gui.visualize.Plot2D).
   * 
   * @return the vector of plot shapes.
   */
  public ArrayList<Integer> getPlotShapes() {
    return m_PlotShapes;
  }

  /**
   * Get the vector of plot sizes (see weka.gui.visualize.Plot2D).
   * 
   * @return the vector of plot sizes.
   */
  public ArrayList<Object> getPlotSizes() {
    return m_PlotSizes;
  }

  /**
   * Set the vector of plot shapes to use;
   * 
   * @param plotShapes
   */
  public void setPlotShapes(ArrayList<Integer> plotShapes) {
    m_PlotShapes = plotShapes;
  }

  /**
   * Set the vector of plot sizes to use
   * 
   * @param plotSizes the plot sizes to use
   */
  public void setPlotSizes(ArrayList<Object> plotSizes) {
    m_PlotSizes = plotSizes;
  }

  /**
   * Sets the classifier used for making the predictions.
   * 
   * @param value the classifier to use
   */
  public void setClassifier(Classifier value) {
    m_Classifier = value;
  }

  /**
   * Returns the currently set classifier.
   * 
   * @return the classifier in use
   */
  public Classifier getClassifier() {
    return m_Classifier;
  }

  /**
   * Sets the 0-based class index.
   * 
   * @param index the class index
   */
  public void setClassIndex(int index) {
    m_ClassIndex = index;
  }

  /**
   * Returns the 0-based class index.
   * 
   * @return the class index
   */
  public int getClassIndex() {
    return m_ClassIndex;
  }

  /**
   * Sets the Evaluation object to use.
   * 
   * @param value the evaluation to use
   */
  public void setEvaluation(Evaluation value) {
    m_Evaluation = value;
  }

  /**
   * Returns the Evaluation object in use.
   * 
   * @return the evaluation object
   */
  public Evaluation getEvaluation() {
    return m_Evaluation;
  }

  /**
   * Sets whether the instances are saved for visualization or only evaluation
   * of the prediction is to happen.
   * 
   * @param value if true then the instances will be saved
   */
  public void setSaveForVisualization(boolean value) {
    m_SaveForVisualization = value;
  }

  /**
   * Returns whether the instances are saved for visualization for only
   * evaluation of the prediction is to happen.
   * 
   * @return true if the instances are saved
   */
  public boolean getSaveForVisualization() {
    return m_SaveForVisualization;
  }

  /**
   * Set whether the point size should be proportional to the prediction margin
   * (classification only).
   * 
   * @param b true if the point size should be proportional to the margin
   */
  public void setPointSizeProportionalToMargin(boolean b) {
    m_pointSizeProportionalToMargin = b;
  }

  /**
   * Get whether the point size should be proportional to the prediction margin
   * (classification only).
   * 
   * @return true if the point size should be proportional to the margin
   */
  public boolean getPointSizeProportionalToMargin() {
    return m_pointSizeProportionalToMargin;
  }

  /**
   * Checks whether classifier, class index and evaluation are provided.
   */
  @Override
  protected void check() {
    super.check();

    if (m_Classifier == null) {
      throw new IllegalStateException("No classifier set!");
    }

    if (m_ClassIndex == -1) {
      throw new IllegalStateException("No class index set!");
    }

    if (m_Evaluation == null) {
      throw new IllegalStateException("No evaluation set");
    }
  }

  /**
   * Sets up the structure for the plot instances. Sets m_PlotInstances to null
   * if instances are not saved for visualization.
   * 
   * @see #getSaveForVisualization()
   */
  @Override
  protected void determineFormat() {
    ArrayList<Attribute> hv;
    Attribute predictedClass;
    Attribute classAt;
    Attribute margin = null;
    ArrayList<String> attVals;
    int i;

    if (!m_SaveForVisualization) {
      m_PlotInstances = null;
      return;
    }

    hv = new ArrayList<Attribute>();

    classAt = m_Instances.attribute(m_ClassIndex);
    if (classAt.isNominal()) {
      attVals = new ArrayList<String>();
      for (i = 0; i < classAt.numValues(); i++) {
        attVals.add(classAt.value(i));
      }
      predictedClass = new Attribute("predicted " + classAt.name(), attVals);
      margin = new Attribute("prediction margin");
    } else {
      predictedClass = new Attribute("predicted" + classAt.name());
    }

    for (i = 0; i < m_Instances.numAttributes(); i++) {
      if (i == m_Instances.classIndex()) {
        if (classAt.isNominal()) {
          hv.add(margin);
        }
        hv.add(predictedClass);
      }
      hv.add((Attribute) m_Instances.attribute(i).copy());
    }

    m_PlotInstances = new Instances(m_Instances.relationName() + "_predicted",
      hv, m_Instances.numInstances());
    if (classAt.isNominal()) {
      m_PlotInstances.setClassIndex(m_ClassIndex + 2);
    } else {
      m_PlotInstances.setClassIndex(m_ClassIndex + 1);
    }
  }

  public void process(Instances batch, double[][] predictions, Evaluation eval) {
    try {

      for (int j = 0; j < batch.numInstances(); j++) {
        Instance toPredict = batch.instance(j);
        double[] preds = predictions[j];
        double probActual = 0;
        double probNext = 0;

        double pred = 0;
        if (batch.classAttribute().isNominal()) {
          pred = (Utils.sum(preds) == 0) ? Utils.missingValue() : Utils
            .maxIndex(preds);

          probActual = (Utils.sum(preds) == 0) ? Utils.missingValue() : (!Utils
            .isMissingValue(toPredict.classIndex()) ? preds[(int) toPredict
            .classValue()] : preds[Utils.maxIndex(preds)]);

          for (int i = 0; i < toPredict.classAttribute().numValues(); i++) {
            if (i != (int) toPredict.classValue() && preds[i] > probNext) {
              probNext = preds[i];
            }
          }
        } else {
          pred = preds[0];
        }

        eval.evaluationForSingleInstance(preds, toPredict, true);

        if (!m_SaveForVisualization) {
          continue;
        }

        if (m_PlotInstances != null) {
          double[] values = new double[m_PlotInstances.numAttributes()];
          boolean isNominal = toPredict.classAttribute().isNominal();
          for (int i = 0; i < m_PlotInstances.numAttributes(); i++) {
            if (i < toPredict.classIndex()) {
              values[i] = toPredict.value(i);
            } else if (i == toPredict.classIndex()) {
              if (isNominal) {
                values[i] = probActual - probNext;
                values[i + 1] = pred;
                values[i + 2] = toPredict.value(i);
                i += 2;
              } else {
                values[i] = pred;
                values[i + 1] = toPredict.value(i);
                i++;
              }
            } else {
              if (isNominal) {
                values[i] = toPredict.value(i - 2);
              } else {
                values[i] = toPredict.value(i - 1);
              }
            }
          }

          m_PlotInstances.add(new DenseInstance(1.0, values));

          if (toPredict.classAttribute().isNominal()) {
            if (toPredict.isMissing(toPredict.classIndex())
              || Utils.isMissingValue(pred)) {
              m_PlotShapes.add(new Integer(Plot2D.MISSING_SHAPE));
            } else if (pred != toPredict.classValue()) {
              // set to default error point shape
              m_PlotShapes.add(new Integer(Plot2D.ERROR_SHAPE));
            } else {
              // otherwise set to constant (automatically assigned) point shape
              m_PlotShapes.add(new Integer(Plot2D.CONST_AUTOMATIC_SHAPE));
            }

            if (m_pointSizeProportionalToMargin) {
              // margin
              m_PlotSizes.add(new Double(probActual - probNext));
            } else {
              int sizeAdj = 0;
              if (pred != toPredict.classValue()) {
                sizeAdj = 1;
              }
              m_PlotSizes.add(new Integer(Plot2D.DEFAULT_SHAPE_SIZE + sizeAdj));
            }
          } else {
            // store the error (to be converted to a point size later)
            Double errd = null;
            if (!toPredict.isMissing(toPredict.classIndex())
              && !Utils.isMissingValue(pred)) {
              errd = new Double(pred - toPredict.classValue());
              m_PlotShapes.add(new Integer(Plot2D.CONST_AUTOMATIC_SHAPE));
            } else {
              // missing shape if actual class not present or prediction is
              // missing
              m_PlotShapes.add(new Integer(Plot2D.MISSING_SHAPE));
            }
            m_PlotSizes.add(errd);
          }
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Process a classifier's prediction for an instance and update a set of
   * plotting instances and additional plotting info. m_PlotShape for nominal
   * class datasets holds shape types (actual data points have automatic shape
   * type assignment; classifier error data points have box shape type). For
   * numeric class datasets, the actual data points are stored in
   * m_PlotInstances and m_PlotSize stores the error (which is later converted
   * to shape size values).
   * 
   * @param toPredict the actual data point
   * @param classifier the classifier
   * @param eval the evaluation object to use for evaluating the classifier on
   *          the instance to predict
   * @see #m_PlotShapes
   * @see #m_PlotSizes
   * @see #m_PlotInstances
   */
  public void process(Instance toPredict, Classifier classifier, Evaluation eval) {
    double pred;
    double[] values;
    int i;

    try {
      pred = 0;

      double[] preds = null;
      double probActual = 0;
      double probNext = 0;
      int mappedClass = -1;

      Instance classMissing = (Instance) toPredict.copy();
      classMissing.setDataset(toPredict.dataset());

      // Only need to do this if the class is nominal, since we call
      // evalForSingleInstance()
      // which only takes a prob array
      if (classifier instanceof weka.classifiers.misc.InputMappedClassifier
        && toPredict.classAttribute().isNominal()) {
        toPredict = (Instance) toPredict.copy();
        toPredict = ((weka.classifiers.misc.InputMappedClassifier) classifier)
          .constructMappedInstance(toPredict);
        mappedClass = ((weka.classifiers.misc.InputMappedClassifier) classifier)
          .getMappedClassIndex();
        classMissing.setMissing(mappedClass);
      } else {
        classMissing.setClassMissing();
      }

      if (toPredict.classAttribute().isNominal()) {
        preds = classifier.distributionForInstance(classMissing);

        pred = (Utils.sum(preds) == 0) ? Utils.missingValue() : Utils
          .maxIndex(preds);

        probActual = (Utils.sum(preds) == 0) ? Utils.missingValue() : (!Utils
          .isMissingValue(toPredict.classIndex()) ? preds[(int) toPredict
          .classValue()] : preds[Utils.maxIndex(preds)]);

        for (i = 0; i < toPredict.classAttribute().numValues(); i++) {
          if (i != (int) toPredict.classValue() && preds[i] > probNext) {
            probNext = preds[i];
          }
        }

        eval.evaluationForSingleInstance(preds, toPredict, true);
      } else {
        // Numeric class. evalModelOnceAndRecordPrediciton() does the
        // InputMappedClassifier
        // transformation for us.
        pred = eval.evaluateModelOnceAndRecordPrediction(classifier, toPredict);
      }

      //

      if (!m_SaveForVisualization) {
        return;
      }

      if (m_PlotInstances != null) {
        boolean isNominal = toPredict.classAttribute().isNominal();
        values = new double[m_PlotInstances.numAttributes()];
        for (i = 0; i < m_PlotInstances.numAttributes(); i++) {
          if (i < toPredict.classIndex()) {
            values[i] = toPredict.value(i);
          } else if (i == toPredict.classIndex()) {
            if (isNominal) {
              values[i] = probActual - probNext;
              values[i + 1] = pred;
              values[i + 2] = toPredict.value(i);
              i += 2;
            } else {
              values[i] = pred;
              values[i + 1] = toPredict.value(i);
              i++;
            }
          } else {
            if (isNominal) {
              values[i] = toPredict.value(i - 2);
            } else {
              values[i] = toPredict.value(i - 1);
            }
          }
        }

        m_PlotInstances.add(new DenseInstance(1.0, values));

        if (toPredict.classAttribute().isNominal()) {
          if (toPredict.isMissing(toPredict.classIndex())
            || Utils.isMissingValue(pred)) {
            m_PlotShapes.add(new Integer(Plot2D.MISSING_SHAPE));
          } else if (pred != toPredict.classValue()) {
            // set to default error point shape
            m_PlotShapes.add(new Integer(Plot2D.ERROR_SHAPE));
          } else {
            // otherwise set to constant (automatically assigned) point shape
            m_PlotShapes.add(new Integer(Plot2D.CONST_AUTOMATIC_SHAPE));
          }
          if (m_pointSizeProportionalToMargin) {
            // margin
            m_PlotSizes.add(new Double(probActual - probNext));
          } else {
            int sizeAdj = 0;
            if (pred != toPredict.classValue()) {
              sizeAdj = 1;
            }
            m_PlotSizes.add(new Integer(Plot2D.DEFAULT_SHAPE_SIZE + sizeAdj));
          }
        } else {
          // store the error (to be converted to a point size later)
          Double errd = null;
          if (!toPredict.isMissing(toPredict.classIndex())
            && !Utils.isMissingValue(pred)) {

            errd = new Double(pred - toPredict.classValue());
            m_PlotShapes.add(new Integer(Plot2D.CONST_AUTOMATIC_SHAPE));
          } else {
            // missing shape if actual class not present or prediction is
            // missing
            m_PlotShapes.add(new Integer(Plot2D.MISSING_SHAPE));
          }
          m_PlotSizes.add(errd);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Scales numeric class predictions into shape sizes for plotting in the
   * visualize panel.
   */
  protected void scaleNumericPredictions() {
    double maxErr;
    double minErr;
    double err;
    int i;
    Double errd;
    double temp;

    maxErr = Double.NEGATIVE_INFINITY;
    minErr = Double.POSITIVE_INFINITY;

    if (m_Instances.classAttribute().isNominal()) {
      maxErr = 1;
      minErr = 0;
    } else {

      // find min/max errors
      for (i = 0; i < m_PlotSizes.size(); i++) {
        errd = (Double) m_PlotSizes.get(i);
        if (errd != null) {
          err = Math.abs(errd.doubleValue());
          if (err < minErr) {
            minErr = err;
          }
          if (err > maxErr) {
            maxErr = err;
          }
        }
      }
    }

    // scale errors
    for (i = 0; i < m_PlotSizes.size(); i++) {
      errd = (Double) m_PlotSizes.get(i);
      if (errd != null) {
        err = Math.abs(errd.doubleValue());
        if (maxErr - minErr > 0) {
          temp = (((err - minErr) / (maxErr - minErr)) * (m_MaximumPlotSizeNumeric
            - m_MinimumPlotSizeNumeric + 1));
          m_PlotSizes
            .set(i, new Integer((int) temp) + m_MinimumPlotSizeNumeric);
        } else {
          m_PlotSizes.set(i, new Integer(m_MinimumPlotSizeNumeric));
        }
      } else {
        m_PlotSizes.set(i, new Integer(m_MinimumPlotSizeNumeric));
      }
    }
  }

  /**
   * Adds the prediction intervals as additional attributes at the end. Since
   * classifiers can returns varying number of intervals per instance, the
   * dataset is filled with missing values for non-existing intervals.
   */
  protected void addPredictionIntervals() {
    int maxNum;
    int num;
    int i;
    int n;
    ArrayList<Prediction> preds;
    ArrayList<Attribute> atts;
    Instances data;
    Instance inst;
    Instance newInst;
    double[] values;
    double[][] predInt;

    // determine the maximum number of intervals
    maxNum = 0;
    preds = m_Evaluation.predictions();
    for (i = 0; i < preds.size(); i++) {
      num = ((NumericPrediction) preds.get(i)).predictionIntervals().length;
      if (num > maxNum) {
        maxNum = num;
      }
    }

    // create new header
    atts = new ArrayList<Attribute>();
    for (i = 0; i < m_PlotInstances.numAttributes(); i++) {
      atts.add(m_PlotInstances.attribute(i));
    }
    for (i = 0; i < maxNum; i++) {
      atts
        .add(new Attribute("predictionInterval_" + (i + 1) + "-lowerBoundary"));
      atts
        .add(new Attribute("predictionInterval_" + (i + 1) + "-upperBoundary"));
      atts.add(new Attribute("predictionInterval_" + (i + 1) + "-width"));
    }
    data = new Instances(m_PlotInstances.relationName(), atts,
      m_PlotInstances.numInstances());
    data.setClassIndex(m_PlotInstances.classIndex());

    // update data
    for (i = 0; i < m_PlotInstances.numInstances(); i++) {
      inst = m_PlotInstances.instance(i);
      // copy old values
      values = new double[data.numAttributes()];
      System
        .arraycopy(inst.toDoubleArray(), 0, values, 0, inst.numAttributes());
      // add interval data
      predInt = ((NumericPrediction) preds.get(i)).predictionIntervals();
      for (n = 0; n < maxNum; n++) {
        if (n < predInt.length) {
          values[m_PlotInstances.numAttributes() + n * 3 + 0] = predInt[n][0];
          values[m_PlotInstances.numAttributes() + n * 3 + 1] = predInt[n][1];
          values[m_PlotInstances.numAttributes() + n * 3 + 2] = predInt[n][1]
            - predInt[n][0];
        } else {
          values[m_PlotInstances.numAttributes() + n * 3 + 0] = Utils
            .missingValue();
          values[m_PlotInstances.numAttributes() + n * 3 + 1] = Utils
            .missingValue();
          values[m_PlotInstances.numAttributes() + n * 3 + 2] = Utils
            .missingValue();
        }
      }
      // create new Instance
      newInst = new DenseInstance(inst.weight(), values);
      data.add(newInst);
    }

    m_PlotInstances = data;
  }

  /**
   * Performs optional post-processing.
   * 
   * @see #scaleNumericPredictions()
   * @see #addPredictionIntervals()
   */
  @Override
  protected void finishUp() {
    super.finishUp();

    if (!m_SaveForVisualization) {
      return;
    }

    if (m_Instances.classAttribute().isNumeric()
      || m_pointSizeProportionalToMargin) {
      scaleNumericPredictions(); // now handles point sizes based on the margin
                                 // too
    }

    if (m_Instances.attribute(m_ClassIndex).isNumeric()) {
      if (m_Classifier instanceof IntervalEstimator) {
        addPredictionIntervals();
      }
    }
  }

  /**
   * Assembles and returns the plot. The relation name of the dataset gets added
   * automatically.
   * 
   * @param name the name of the plot
   * @return the plot or null if plot instances weren't saved for visualization
   * @throws Exception if plot generation fails
   */
  @Override
  protected PlotData2D createPlotData(String name) throws Exception {
    PlotData2D result;
    if (!m_SaveForVisualization) {
      return null;
    }

    result = new PlotData2D(m_PlotInstances);
    result.setShapeSize(m_PlotSizes);
    result.setShapeType(m_PlotShapes);
    result.setPlotName(name + " (" + m_Instances.relationName() + ")");
    // result.addInstanceNumberAttribute();

    return result;
  }

  /**
   * For freeing up memory. Plot data cannot be generated after this call!
   */
  @Override
  public void cleanUp() {
    super.cleanUp();

    m_Classifier = null;
    m_PlotShapes = null;
    m_PlotSizes = null;
    m_Evaluation = null;
  }
}
