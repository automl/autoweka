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
 *    ClassifierPerformanceEvaluator.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import weka.classifiers.AggregateableEvaluation;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.BatchPredictor;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.experiment.Task;
import weka.experiment.TaskStatusInfo;
import weka.gui.explorer.ClassifierErrorsPlotInstances;
import weka.gui.explorer.ExplorerDefaults;
import weka.gui.visualize.PlotData2D;

/**
 * A bean that evaluates the performance of batch trained classifiers
 * 
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 12704 $
 */
public class ClassifierPerformanceEvaluator extends AbstractEvaluator implements
  BatchClassifierListener, Serializable, UserRequestAcceptor, EventConstraints {

  /** for serialization */
  private static final long serialVersionUID = -3511801418192148690L;

  /**
   * Evaluation object used for evaluating a classifier
   */
  private transient AggregateableEvaluation m_eval;
  private transient Instances m_aggregatedPlotInstances = null;
  private transient ArrayList<Object> m_aggregatedPlotSizes = null;
  private transient ArrayList<Integer> m_aggregatedPlotShapes = null;

  // private transient Thread m_evaluateThread = null;

  private transient long m_currentBatchIdentifier;
  private transient int m_setsComplete;

  private final Vector<TextListener> m_textListeners =
    new Vector<TextListener>();
  private final Vector<ThresholdDataListener> m_thresholdListeners =
    new Vector<ThresholdDataListener>();
  private final Vector<VisualizableErrorListener> m_visualizableErrorListeners =
    new Vector<VisualizableErrorListener>();

  protected transient ThreadPoolExecutor m_executorPool;
  protected transient List<EvaluationTask> m_tasks;

  protected boolean m_errorPlotPointSizeProportionalToMargin;

  /**
   * Number of threads to use to train models with
   */
  protected int m_executionSlots = 2;

  /** Evaluation metrics to output */
  protected String m_selectedEvalMetrics = "";
  protected List<String> m_metricsList = new ArrayList<String>();

  public ClassifierPerformanceEvaluator() {
    m_visual.loadIcons(BeanVisual.ICON_PATH
      + "ClassifierPerformanceEvaluator.gif", BeanVisual.ICON_PATH
      + "ClassifierPerformanceEvaluator_animated.gif");
    m_visual.setText("ClassifierPerformanceEvaluator");

    m_metricsList = Evaluation.getAllEvaluationMetricNames();
    m_metricsList.remove("Coverage");
    m_metricsList.remove("Region size");
    StringBuilder b = new StringBuilder();
    for (String s : m_metricsList) {
      b.append(s).append(",");
    }
    m_selectedEvalMetrics = b.substring(0, b.length() - 1);
  }

  protected void stringToList(String l) {
    if (l != null && l.length() > 0) {
      String[] parts = l.split(",");
      m_metricsList.clear();
      for (String s : parts) {
        m_metricsList.add(s.trim());
      }
    }
  }

  /**
   * Set the evaluation metrics to output (as a comma-separated list).
   * 
   * @param m the evaluation metrics to output
   */
  public void setEvaluationMetricsToOutput(String m) {
    m_selectedEvalMetrics = m;
    stringToList(m);
  }

  /**
   * Get the evaluation metrics to output (as a comma-separated list).
   * 
   * @return the evaluation metrics to output
   */
  public String getEvaluationMetricsToOutput() {
    return m_selectedEvalMetrics;
  }

  /**
   * Get the tip text for this property.
   * 
   * @return the tip text for this property.
   */
  public String evaluationMetricsToOutputTipText() {
    return "A comma-separated list of evaluation metrics to output";
  }

  /**
   * Set whether the point size on classification error plots should be
   * proportional to the prediction margin.
   * 
   * @param e true if the point size is to be proportional to the margin.
   */
  public void setErrorPlotPointSizeProportionalToMargin(boolean e) {
    m_errorPlotPointSizeProportionalToMargin = e;
  }

  /**
   * Get whether the point size on classification error plots should be
   * proportional to the prediction margin.
   * 
   * @return true if the point size is to be proportional to the margin.
   */
  public boolean getErrorPlotPointSizeProportionalToMargin() {
    return m_errorPlotPointSizeProportionalToMargin;
  }

  /**
   * Get the tip text for this property.
   * 
   * @return the tip text for this property.
   */
  public String errorPlotPointSizeProportionalToMarginTipText() {
    return "Set the point size proportional to the prediction "
      + "margin for classification error plots";
  }

  /**
   * Get the number of execution slots to use.
   * 
   * @return the number of execution slots to use
   */
  public int getExecutionSlots() {
    return m_executionSlots;
  }

  /**
   * Set the number of executions slots to use.
   * 
   * @param slots the number of execution slots to use
   */
  public void setExecutionSlots(int slots) {
    m_executionSlots = slots;
  }

  /**
   * Get the tip text for this property.
   * 
   * @return the tip text for this property.
   */
  public String executionSlotsTipText() {
    return "Set the number of evaluation tasks to run in parallel.";
  }

  private void startExecutorPool() {

    if (m_executorPool != null) {
      m_executorPool.shutdownNow();
    }

    m_executorPool =
      new ThreadPoolExecutor(m_executionSlots, m_executionSlots, 120,
        TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
  }

  /**
   * Set a custom (descriptive) name for this bean
   * 
   * @param name the name to use
   */
  @Override
  public void setCustomName(String name) {
    m_visual.setText(name);
  }

  /**
   * Get the custom (descriptive) name for this bean (if one has been set)
   * 
   * @return the custom name (or the default name)
   */
  @Override
  public String getCustomName() {
    return m_visual.getText();
  }

  /**
   * Global info for this bean
   * 
   * @return a <code>String</code> value
   */
  public String globalInfo() {
    return "Evaluate the performance of batch trained classifiers.";
  }

  /** for generating plottable instance with predictions appended. */
  private transient ClassifierErrorsPlotInstances m_PlotInstances = null;

  protected static Evaluation adjustForInputMappedClassifier(Evaluation eval,
    weka.classifiers.Classifier classifier, Instances inst,
    ClassifierErrorsPlotInstances plotInstances) throws Exception {

    if (classifier instanceof weka.classifiers.misc.InputMappedClassifier) {
      Instances mappedClassifierHeader =
        ((weka.classifiers.misc.InputMappedClassifier) classifier)
          .getModelHeader(new Instances(inst, 0));

      eval = new Evaluation(new Instances(mappedClassifierHeader, 0));

      if (!eval.getHeader().equalHeaders(inst)) {
        // When the InputMappedClassifier is loading a model,
        // we need to make a new dataset that maps the test instances to
        // the structure expected by the mapped classifier - this is only
        // to ensure that the ClassifierPlotInstances object is configured
        // in accordance with what the embeded classifier was trained with
        Instances mappedClassifierDataset =
          ((weka.classifiers.misc.InputMappedClassifier) classifier)
            .getModelHeader(new Instances(mappedClassifierHeader, 0));
        for (int zz = 0; zz < inst.numInstances(); zz++) {
          Instance mapped =
            ((weka.classifiers.misc.InputMappedClassifier) classifier)
              .constructMappedInstance(inst.instance(zz));
          mappedClassifierDataset.add(mapped);
        }

        eval.setPriors(mappedClassifierDataset);
        plotInstances.setInstances(mappedClassifierDataset);
        plotInstances.setClassifier(classifier);
        plotInstances.setClassIndex(mappedClassifierDataset.classIndex());
        plotInstances.setEvaluation(eval);
      }
    }

    return eval;
  }

  /**
   * Inner class for running an evaluation on a split
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision: 12704 $
   */
  protected class EvaluationTask implements Runnable, Task {

    private static final long serialVersionUID = -8939077467030259059L;
    protected Instances m_testData;
    protected Instances m_trainData;
    protected int m_setNum;
    protected int m_maxSetNum;
    protected Classifier m_classifier;
    protected boolean m_stopped;
    protected String m_evalLabel = "";

    /**
     * Constructor for an EvaluationTask
     * 
     * @param classifier the classifier being evaluated
     * @param trainData the training data
     * @param testData the test data
     * @param setNum the set number
     * @param maxSetNum maximum number of sets
     * @param eventLabel the label to associate with this evaluation (for
     *          charting)
     */
    public EvaluationTask(Classifier classifier, Instances trainData,
      Instances testData, int setNum, int maxSetNum, String evalLabel) {
      m_classifier = classifier;
      m_setNum = setNum;
      m_maxSetNum = maxSetNum;
      m_testData = testData;
      m_trainData = trainData;
      if (evalLabel != null) {
        m_evalLabel = evalLabel;
      }
    }

    public void setStopped() {
      m_stopped = true;
    }

    @Override
    public void run() {
      execute();
    }

    @Override
    public void execute() {
      if (m_stopped) {
        return;
      }

      if (m_logger != null) {
        m_logger.statusMessage(statusMessagePrefix() + "Evaluating ("
          + m_setNum + ")...");
      }
      try {

        ClassifierErrorsPlotInstances plotInstances =
          ExplorerDefaults.getClassifierErrorsPlotInstances();
        Evaluation eval = null;

        if (m_trainData == null || m_trainData.numInstances() == 0) {
          eval = new Evaluation(m_testData);
          plotInstances.setInstances(m_testData);
          plotInstances.setClassifier(m_classifier);
          plotInstances.setClassIndex(m_testData.classIndex());
          plotInstances.setEvaluation(eval);
          plotInstances
            .setPointSizeProportionalToMargin(m_errorPlotPointSizeProportionalToMargin);
          eval =
            adjustForInputMappedClassifier(eval, m_classifier, m_testData,
              plotInstances);

          eval.useNoPriors();
          eval.setMetricsToDisplay(m_metricsList);
        } else {
          eval = new Evaluation(m_trainData);
          plotInstances.setInstances(m_trainData);
          plotInstances.setClassifier(m_classifier);
          plotInstances.setClassIndex(m_trainData.classIndex());
          plotInstances.setEvaluation(eval);
          plotInstances
            .setPointSizeProportionalToMargin(m_errorPlotPointSizeProportionalToMargin);
          eval =
            adjustForInputMappedClassifier(eval, m_classifier, m_trainData,
              plotInstances);
          eval.setMetricsToDisplay(m_metricsList);
        }

        plotInstances.setUp();

        if (m_classifier instanceof BatchPredictor
          && ((BatchPredictor) m_classifier)
            .implementsMoreEfficientBatchPrediction()) {
          double[][] predictions =
            ((BatchPredictor) m_classifier)
              .distributionsForInstances(m_testData);
          plotInstances.process(m_testData, predictions, eval);
        } else {

          for (int i = 0; i < m_testData.numInstances(); i++) {
            if (m_stopped) {
              break;
            }
            Instance temp = m_testData.instance(i);
            plotInstances.process(temp, m_classifier, eval);
          }
        }

        if (m_stopped) {
          return;
        }

        aggregateEvalTask(eval, m_classifier, m_testData, plotInstances,
          m_setNum, m_maxSetNum, m_evalLabel);

      } catch (Exception ex) {
        ClassifierPerformanceEvaluator.this.stop(); // stop all processing
        if (m_logger != null) {
          m_logger.logMessage("[ClassifierPerformanceEvaluator] "
            + statusMessagePrefix() + " problem evaluating classifier. "
            + ex.getMessage());
        }
        ex.printStackTrace();
      }
    }

    @Override
    public TaskStatusInfo getTaskStatus() {
      // TODO Auto-generated method stub
      return null;
    }
  }

  /**
   * Subclass of ClassifierErrorsPlotInstances to allow plot point sizes to be
   * scaled according to global min/max values.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected static class AggregateableClassifierErrorsPlotInstances extends
    ClassifierErrorsPlotInstances {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 2012744784036684168L;

    /**
     * Set the vector of plot shapes to use;
     * 
     * @param plotShapes
     */
    @Override
    public void setPlotShapes(ArrayList<Integer> plotShapes) {
      m_PlotShapes = plotShapes;
    }

    /**
     * Set the vector of plot sizes to use
     * 
     * @param plotSizes the plot sizes to use
     */
    @Override
    public void setPlotSizes(ArrayList<Object> plotSizes) {
      m_PlotSizes = plotSizes;
    }

    public void setPlotInstances(Instances inst) {
      m_PlotInstances = inst;
    }

    @Override
    protected void finishUp() {
      m_FinishUpCalled = true;

      if (!m_SaveForVisualization) {
        return;
      }

      if (m_Instances.classAttribute().isNumeric()
        || m_pointSizeProportionalToMargin) {
        scaleNumericPredictions();
      }
    }
  }

  /**
   * Takes an evaluation object from a task and aggregates it with the overall
   * one.
   * 
   * @param eval the evaluation object to aggregate
   * @param classifier the classifier used by the task
   * @param testData the testData from the task
   * @param plotInstances the ClassifierErrorsPlotInstances object from the task
   * @param setNum the set number processed by the task
   * @param maxSetNum the maximum number of sets in this batch
   * @param evalLabel the label to associate with the results of this evaluation
   */
  @SuppressWarnings({ "deprecation", "unchecked" })
  protected synchronized void aggregateEvalTask(Evaluation eval,
    Classifier classifier, Instances testData,
    ClassifierErrorsPlotInstances plotInstances, int setNum, int maxSetNum,
    String evalLabel) {

    m_eval.aggregate(eval);

    if (m_aggregatedPlotInstances == null) {
      // get these first so that the post-processing does not scale the sizes!!
      m_aggregatedPlotShapes =
        (ArrayList<Integer>) plotInstances.getPlotShapes().clone();
      m_aggregatedPlotSizes =
        (ArrayList<Object>) plotInstances.getPlotSizes().clone();

      // this calls the post-processing, so do this last
      m_aggregatedPlotInstances =
        new Instances(plotInstances.getPlotInstances());
    } else {
      // get these first so that post-processing does not scale sizes
      ArrayList<Object> tmpSizes =
        (ArrayList<Object>) plotInstances.getPlotSizes().clone();
      ArrayList<Integer> tmpShapes =
        (ArrayList<Integer>) plotInstances.getPlotShapes().clone();

      Instances temp = plotInstances.getPlotInstances();
      for (int i = 0; i < temp.numInstances(); i++) {
        m_aggregatedPlotInstances.add(temp.get(i));
        m_aggregatedPlotShapes.add(tmpShapes.get(i));
        m_aggregatedPlotSizes.add(tmpSizes.get(i));
      }
    }
    m_setsComplete++;

    if (m_logger != null) {
      if (m_setsComplete < maxSetNum) {
        m_logger.statusMessage(statusMessagePrefix() + "Completed ("
          + m_setsComplete + ").");
      }
    }

    // if (ce.getSetNumber() == ce.getMaxSetNumber()) {
    if (m_setsComplete == maxSetNum) {
      try {
        AggregateableClassifierErrorsPlotInstances aggPlot =
          new AggregateableClassifierErrorsPlotInstances();
        aggPlot.setInstances(testData);
        aggPlot.setPlotInstances(m_aggregatedPlotInstances);
        aggPlot.setPlotShapes(m_aggregatedPlotShapes);
        aggPlot.setPlotSizes(m_aggregatedPlotSizes);
        aggPlot
          .setPointSizeProportionalToMargin(m_errorPlotPointSizeProportionalToMargin);

        // triggers scaling of shape sizes
        aggPlot.getPlotInstances();

        String textTitle = "";
        textTitle += classifier.getClass().getName();
        String textOptions = "";
        if (classifier instanceof OptionHandler) {
          textOptions =
            Utils.joinOptions(((OptionHandler) classifier).getOptions());
        }
        textTitle =
          textTitle.substring(textTitle.lastIndexOf('.') + 1,
            textTitle.length());
        if (evalLabel != null && evalLabel.length() > 0) {
          if (!textTitle.toLowerCase().startsWith(evalLabel.toLowerCase())) {
            textTitle = evalLabel + " : " + textTitle;
          }
        }
        String resultT =
          "=== Evaluation result ===\n\n"
            + "Scheme: "
            + textTitle
            + "\n"
            + ((textOptions.length() > 0) ? "Options: " + textOptions + "\n"
              : "") + "Relation: " + testData.relationName() + "\n\n"
            + m_eval.toSummaryString();

        if (testData.classAttribute().isNominal()) {
          resultT +=
            "\n" + m_eval.toClassDetailsString() + "\n"
              + m_eval.toMatrixString();
        }

        TextEvent te =
          new TextEvent(ClassifierPerformanceEvaluator.this, resultT, textTitle);
        notifyTextListeners(te);

        // set up visualizable errors
        if (m_visualizableErrorListeners.size() > 0) {
          PlotData2D errorD = new PlotData2D(m_aggregatedPlotInstances);
          errorD.setShapeSize(m_aggregatedPlotSizes);
          errorD.setShapeType(m_aggregatedPlotShapes);
          errorD.setPlotName(textTitle + " " + textOptions);

          /*
           * PlotData2D errorD = m_PlotInstances.getPlotData( textTitle + " " +
           * textOptions);
           */
          VisualizableErrorEvent vel =
            new VisualizableErrorEvent(ClassifierPerformanceEvaluator.this,
              errorD);
          notifyVisualizableErrorListeners(vel);
          m_PlotInstances.cleanUp();
        }

        if (testData.classAttribute().isNominal()
          && m_thresholdListeners.size() > 0) {
          ThresholdCurve tc = new ThresholdCurve();
          Instances result = tc.getCurve(m_eval.predictions(), 0);
          result.setRelationName(testData.relationName());
          PlotData2D pd = new PlotData2D(result);
          String htmlTitle = "<html><font size=-2>" + textTitle;
          String newOptions = "";
          if (classifier instanceof OptionHandler) {
            String[] options = ((OptionHandler) classifier).getOptions();
            if (options.length > 0) {
              for (int ii = 0; ii < options.length; ii++) {
                if (options[ii].length() == 0) {
                  continue;
                }
                if (options[ii].charAt(0) == '-'
                  && !(options[ii].charAt(1) >= '0' && options[ii].charAt(1) <= '9')) {
                  newOptions += "<br>";
                }
                newOptions += options[ii];
              }
            }
          }

          htmlTitle +=
            " " + newOptions + "<br>" + " (class: "
              + testData.classAttribute().value(0) + ")" + "</font></html>";
          pd.setPlotName(textTitle + " (class: "
            + testData.classAttribute().value(0) + ")");
          pd.setPlotNameHTML(htmlTitle);
          boolean[] connectPoints = new boolean[result.numInstances()];
          for (int jj = 1; jj < connectPoints.length; jj++) {
            connectPoints[jj] = true;
          }

          pd.setConnectPoints(connectPoints);

          ThresholdDataEvent rde =
            new ThresholdDataEvent(ClassifierPerformanceEvaluator.this, pd,
              testData.classAttribute());
          notifyThresholdListeners(rde);
        }
        if (m_logger != null) {
          m_logger.statusMessage(statusMessagePrefix() + "Finished.");
        }

      } catch (Exception ex) {
        if (m_logger != null) {
          m_logger.logMessage("[ClassifierPerformanceEvaluator] "
            + statusMessagePrefix()
            + " problem constructing evaluation results. " + ex.getMessage());
        }
        ex.printStackTrace();
      } finally {
        m_visual.setStatic();
        // save memory
        m_PlotInstances = null;
        m_setsComplete = 0;
        m_tasks = null;
        m_aggregatedPlotInstances = null;
      }
    }
  }

  /**
   * Accept a classifier to be evaluated.
   * 
   * @param ce a <code>BatchClassifierEvent</code> value
   */
  @Override
  public void acceptClassifier(BatchClassifierEvent ce) {
    if (ce.getTestSet() == null || ce.getTestSet().isStructureOnly()) {
      return; // can't evaluate empty/non-existent test instances
    }

    Classifier classifier = ce.getClassifier();

    try {
      if (ce.getGroupIdentifier() != m_currentBatchIdentifier) {
        if (m_setsComplete > 0) {
          if (m_logger != null) {
            m_logger.statusMessage(statusMessagePrefix()
              + "BUSY. Can't accept data " + "at this time.");
            m_logger.logMessage("[ClassifierPerformanceEvaluator] "
              + statusMessagePrefix()
              + " BUSY. Can't accept data at this time.");
          }
          return;
        }
        if (ce.getTrainSet().getDataSet() == null
          || ce.getTrainSet().getDataSet().numInstances() == 0) {
          // we have no training set to estimate majority class
          // or mean of target from
          Evaluation eval = new Evaluation(ce.getTestSet().getDataSet());
          m_PlotInstances = ExplorerDefaults.getClassifierErrorsPlotInstances();
          m_PlotInstances.setInstances(ce.getTestSet().getDataSet());
          m_PlotInstances.setClassifier(ce.getClassifier());
          m_PlotInstances.setClassIndex(ce.getTestSet().getDataSet()
            .classIndex());
          m_PlotInstances.setEvaluation(eval);

          eval =
            adjustForInputMappedClassifier(eval, ce.getClassifier(), ce
              .getTestSet().getDataSet(), m_PlotInstances);
          eval.useNoPriors();
          m_eval = new AggregateableEvaluation(eval);
          m_eval.setMetricsToDisplay(m_metricsList);
        } else {
          // we can set up with the training set here
          Evaluation eval = new Evaluation(ce.getTrainSet().getDataSet());
          m_PlotInstances = ExplorerDefaults.getClassifierErrorsPlotInstances();
          m_PlotInstances.setInstances(ce.getTrainSet().getDataSet());
          m_PlotInstances.setClassifier(ce.getClassifier());
          m_PlotInstances.setClassIndex(ce.getTestSet().getDataSet()
            .classIndex());
          m_PlotInstances.setEvaluation(eval);

          eval =
            adjustForInputMappedClassifier(eval, ce.getClassifier(), ce
              .getTrainSet().getDataSet(), m_PlotInstances);
          m_eval = new AggregateableEvaluation(eval);
          m_eval.setMetricsToDisplay(m_metricsList);
        }

        m_PlotInstances.setUp();

        m_currentBatchIdentifier = ce.getGroupIdentifier();
        m_setsComplete = 0;

        m_aggregatedPlotInstances = null;

        String msg =
          "[ClassifierPerformanceEvaluator] " + statusMessagePrefix()
            + " starting executor pool (" + getExecutionSlots() + " slots)...";

        // start the execution pool
        startExecutorPool();

        m_tasks = new ArrayList<EvaluationTask>();

        if (m_logger != null) {
          m_logger.logMessage(msg);
        } else {
          System.out.println(msg);
        }
      }

      // if m_tasks == null then we've been stopped
      if (m_setsComplete < ce.getMaxSetNumber() && m_tasks != null) {
        EvaluationTask newTask =
          new EvaluationTask(classifier, ce.getTrainSet().getDataSet(), ce
            .getTestSet().getDataSet(), ce.getSetNumber(),
            ce.getMaxSetNumber(), ce.getLabel());
        String msg =
          "[ClassifierPerformanceEvaluator] " + statusMessagePrefix()
            + " scheduling " + " evaluation of fold " + ce.getSetNumber()
            + " for execution...";
        if (m_logger != null) {
          m_logger.logMessage(msg);
        } else {
          System.out.println(msg);
        }

        m_tasks.add(newTask);
        m_executorPool.execute(newTask);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      // stop everything
      stop();
    }
  }

  /**
   * Returns true if. at this time, the bean is busy with some (i.e. perhaps a
   * worker thread is performing some calculation).
   * 
   * @return true if the bean is busy.
   */
  @Override
  public boolean isBusy() {
    // return (m_evaluateThread != null);
    if (m_executorPool == null
      || (m_executorPool.getQueue().size() == 0 && m_executorPool
        .getActiveCount() == 0) && m_setsComplete == 0) {
      return false;
    }

    return true;
  }

  /**
   * Try and stop any action
   */
  @SuppressWarnings("deprecation")
  @Override
  public void stop() {
    // tell the listenee (upstream bean) to stop
    if (m_listenee instanceof BeanCommon) {
      // System.err.println("Listener is BeanCommon");
      ((BeanCommon) m_listenee).stop();
    }

    if (m_tasks != null) {
      for (EvaluationTask t : m_tasks) {
        t.setStopped();
      }
    }
    m_tasks = null;
    m_visual.setStatic();
    m_setsComplete = 0;

    // shutdown the executor pool and reclaim storage
    if (m_executorPool != null) {
      m_executorPool.shutdownNow();
      m_executorPool.purge();
      m_executorPool = null;
    }

    // stop the evaluate thread
    /*
     * if (m_evaluateThread != null) { m_evaluateThread.interrupt();
     * m_evaluateThread.stop(); m_evaluateThread = null; m_visual.setStatic(); }
     */
  }

  /**
   * Function used to stop code that calls acceptClassifier. This is needed as
   * classifier evaluation is performed inside a separate thread of execution.
   * 
   * @param tf a <code>boolean</code> value
   * 
   *          private synchronized void block(boolean tf) { if (tf) { try { //
   *          only block if thread is still doing something useful! if
   *          (m_evaluateThread != null && m_evaluateThread.isAlive()) { wait();
   *          } } catch (InterruptedException ex) { } } else { notifyAll(); } }
   */

  /**
   * Return an enumeration of user activated requests for this bean
   * 
   * @return an <code>Enumeration</code> value
   */
  @Override
  public Enumeration<String> enumerateRequests() {
    Vector<String> newVector = new Vector<String>(0);
    /*
     * if (m_evaluateThread != null) { newVector.addElement("Stop"); }
     */
    if (m_executorPool != null
      && (m_executorPool.getQueue().size() > 0 || m_executorPool
        .getActiveCount() > 0)) {
      newVector.addElement("Stop");
    }

    return newVector.elements();
  }

  /**
   * Perform the named request
   * 
   * @param request the request to perform
   * @exception IllegalArgumentException if an error occurs
   */
  @Override
  public void performRequest(String request) {
    if (request.compareTo("Stop") == 0) {
      stop();
    } else {
      throw new IllegalArgumentException(request

      + " not supported (ClassifierPerformanceEvaluator)");
    }
  }

  /**
   * Add a text listener
   * 
   * @param cl a <code>TextListener</code> value
   */
  public synchronized void addTextListener(TextListener cl) {
    m_textListeners.addElement(cl);
  }

  /**
   * Remove a text listener
   * 
   * @param cl a <code>TextListener</code> value
   */
  public synchronized void removeTextListener(TextListener cl) {
    m_textListeners.remove(cl);
  }

  /**
   * Add a threshold data listener
   * 
   * @param cl a <code>ThresholdDataListener</code> value
   */
  public synchronized void addThresholdDataListener(ThresholdDataListener cl) {
    m_thresholdListeners.addElement(cl);
  }

  /**
   * Remove a Threshold data listener
   * 
   * @param cl a <code>ThresholdDataListener</code> value
   */
  public synchronized void
    removeThresholdDataListener(ThresholdDataListener cl) {
    m_thresholdListeners.remove(cl);
  }

  /**
   * Add a visualizable error listener
   * 
   * @param vel a <code>VisualizableErrorListener</code> value
   */
  public synchronized void addVisualizableErrorListener(
    VisualizableErrorListener vel) {
    m_visualizableErrorListeners.add(vel);
  }

  /**
   * Remove a visualizable error listener
   * 
   * @param vel a <code>VisualizableErrorListener</code> value
   */
  public synchronized void removeVisualizableErrorListener(
    VisualizableErrorListener vel) {
    m_visualizableErrorListeners.remove(vel);
  }

  /**
   * Notify all text listeners of a TextEvent
   * 
   * @param te a <code>TextEvent</code> value
   */
  @SuppressWarnings("unchecked")
  private void notifyTextListeners(TextEvent te) {
    Vector<TextListener> l;
    synchronized (this) {
      l = (Vector<TextListener>) m_textListeners.clone();
    }
    if (l.size() > 0) {
      for (int i = 0; i < l.size(); i++) {
        // System.err.println("Notifying text listeners "
        // +"(ClassifierPerformanceEvaluator)");
        l.elementAt(i).acceptText(te);
      }
    }
  }

  /**
   * Notify all ThresholdDataListeners of a ThresholdDataEvent
   * 
   * @param te a <code>ThresholdDataEvent</code> value
   */
  @SuppressWarnings("unchecked")
  private void notifyThresholdListeners(ThresholdDataEvent re) {
    Vector<ThresholdDataListener> l;
    synchronized (this) {
      l = (Vector<ThresholdDataListener>) m_thresholdListeners.clone();
    }
    if (l.size() > 0) {
      for (int i = 0; i < l.size(); i++) {
        // System.err.println("Notifying text listeners "
        // +"(ClassifierPerformanceEvaluator)");
        l.elementAt(i).acceptDataSet(re);
      }
    }
  }

  /**
   * Notify all VisualizableErrorListeners of a VisualizableErrorEvent
   * 
   * @param te a <code>VisualizableErrorEvent</code> value
   */
  @SuppressWarnings("unchecked")
  private void notifyVisualizableErrorListeners(VisualizableErrorEvent re) {
    Vector<VisualizableErrorListener> l;
    synchronized (this) {
      l =
        (Vector<VisualizableErrorListener>) m_visualizableErrorListeners
          .clone();
    }
    if (l.size() > 0) {
      for (int i = 0; i < l.size(); i++) {
        // System.err.println("Notifying text listeners "
        // +"(ClassifierPerformanceEvaluator)");
        l.elementAt(i).acceptDataSet(re);
      }
    }
  }

  /**
   * Returns true, if at the current time, the named event could be generated.
   * Assumes that supplied event names are names of events that could be
   * generated by this bean.
   * 
   * @param eventName the name of the event in question
   * @return true if the named event could be generated at this point in time
   */
  @Override
  public boolean eventGeneratable(String eventName) {
    if (m_listenee == null) {
      return false;
    }

    if (m_listenee instanceof EventConstraints) {
      if (!((EventConstraints) m_listenee).eventGeneratable("batchClassifier")) {
        return false;
      }
    }
    return true;
  }

  private String statusMessagePrefix() {
    return getCustomName() + "$" + hashCode() + "|";
  }
}
