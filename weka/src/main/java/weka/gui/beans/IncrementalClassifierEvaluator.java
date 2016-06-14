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
 *    IncrementalClassifierEvaluator.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.util.LinkedList;
import java.util.Vector;

import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Utils;

/**
 * Bean that evaluates incremental classifiers
 * 
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 10220 $
 */
public class IncrementalClassifierEvaluator extends AbstractEvaluator implements
  IncrementalClassifierListener, EventConstraints {

  /** for serialization */
  private static final long serialVersionUID = -3105419818939541291L;

  private transient Evaluation m_eval;

  private final Vector<ChartListener> m_listeners = new Vector<ChartListener>();
  private final Vector<TextListener> m_textListeners = new Vector<TextListener>();

  private Vector<String> m_dataLegend = new Vector<String>();

  private final ChartEvent m_ce = new ChartEvent(this);
  private double[] m_dataPoint = new double[1];
  private boolean m_reset = false;

  private double m_min = Double.MAX_VALUE;
  private double m_max = Double.MIN_VALUE;

  // how often (in milliseconds) to report throughput to the log
  private int m_statusFrequency = 2000;
  private int m_instanceCount = 0;

  // output info retrieval and auc stats for each class (if class is nominal)
  private boolean m_outputInfoRetrievalStats = false;

  // window size for computing performance metrics - 0 means no window, i.e
  // don't "forget" performance on any instances
  private int m_windowSize = 0;
  private Evaluation m_windowEval;
  private LinkedList<Instance> m_window;
  private LinkedList<double[]> m_windowedPreds;

  public IncrementalClassifierEvaluator() {
    m_visual.loadIcons(BeanVisual.ICON_PATH
      + "IncrementalClassifierEvaluator.gif", BeanVisual.ICON_PATH
      + "IncrementalClassifierEvaluator_animated.gif");
    m_visual.setText("IncrementalClassifierEvaluator");
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
    return "Evaluate the performance of incrementally trained classifiers.";
  }

  protected transient StreamThroughput m_throughput;

  /**
   * Accepts and processes a classifier encapsulated in an incremental
   * classifier event
   * 
   * @param ce an <code>IncrementalClassifierEvent</code> value
   */
  @Override
  public void acceptClassifier(final IncrementalClassifierEvent ce) {
    try {
      if (ce.getStatus() == IncrementalClassifierEvent.NEW_BATCH) {
        m_throughput = new StreamThroughput(statusMessagePrefix());
        m_throughput.setSamplePeriod(m_statusFrequency);

        // m_eval = new Evaluation(ce.getCurrentInstance().dataset());
        m_eval = new Evaluation(ce.getStructure());
        m_eval.useNoPriors();

        m_dataLegend = new Vector<String>();
        m_reset = true;
        m_dataPoint = new double[0];
        ce.getStructure();
        System.err.println("NEW BATCH");
        m_instanceCount = 0;

        if (m_windowSize > 0) {
          m_window = new LinkedList<Instance>();
          m_windowEval = new Evaluation(ce.getStructure());
          m_windowEval.useNoPriors();
          m_windowedPreds = new LinkedList<double[]>();

          if (m_logger != null) {
            m_logger.logMessage(statusMessagePrefix()
              + "[IncrementalClassifierEvaluator] Chart output using windowed "
              + "evaluation over " + m_windowSize + " instances");
          }
        }

        /*
         * if (m_logger != null) { m_logger.statusMessage(statusMessagePrefix()
         * + "IncrementalClassifierEvaluator: started processing...");
         * m_logger.logMessage(statusMessagePrefix() +
         * " [IncrementalClassifierEvaluator]" + statusMessagePrefix() +
         * " started processing..."); }
         */
      } else {
        Instance inst = ce.getCurrentInstance();
        if (inst != null) {
          m_throughput.updateStart();
          m_instanceCount++;
          // if (inst.attribute(inst.classIndex()).isNominal()) {
          double[] dist = ce.getClassifier().distributionForInstance(inst);
          double pred = 0;
          if (!inst.isMissing(inst.classIndex())) {
            if (m_outputInfoRetrievalStats) {
              // store predictions so AUC etc can be output.
              m_eval.evaluateModelOnceAndRecordPrediction(dist, inst);
            } else {
              m_eval.evaluateModelOnce(dist, inst);
            }

            if (m_windowSize > 0) {

              m_windowEval.evaluateModelOnce(dist, inst);
              m_window.addFirst(inst);
              m_windowedPreds.addFirst(dist);

              if (m_instanceCount > m_windowSize) {
                // "forget" the oldest prediction
                Instance oldest = m_window.removeLast();

                double[] oldDist = m_windowedPreds.removeLast();
                oldest.setWeight(-oldest.weight());
                m_windowEval.evaluateModelOnce(oldDist, oldest);
                oldest.setWeight(-oldest.weight());
              }
            }
          } else {
            pred = ce.getClassifier().classifyInstance(inst);
          }
          if (inst.classIndex() >= 0) {
            // need to check that the class is not missing
            if (inst.attribute(inst.classIndex()).isNominal()) {
              if (!inst.isMissing(inst.classIndex())) {
                if (m_dataPoint.length < 2) {
                  m_dataPoint = new double[3];
                  m_dataLegend.addElement("Accuracy");
                  m_dataLegend.addElement("RMSE (prob)");
                  m_dataLegend.addElement("Kappa");
                }
                // int classV = (int) inst.value(inst.classIndex());

                if (m_windowSize > 0) {
                  m_dataPoint[1] = m_windowEval.rootMeanSquaredError();
                  m_dataPoint[2] = m_windowEval.kappa();
                } else {
                  m_dataPoint[1] = m_eval.rootMeanSquaredError();
                  m_dataPoint[2] = m_eval.kappa();
                }
                // int maxO = Utils.maxIndex(dist);
                // if (maxO == classV) {
                // dist[classV] = -1;
                // maxO = Utils.maxIndex(dist);
                // }
                // m_dataPoint[1] -= dist[maxO];
              } else {
                if (m_dataPoint.length < 1) {
                  m_dataPoint = new double[1];
                  m_dataLegend.addElement("Confidence");
                }
              }
              double primaryMeasure = 0;
              if (!inst.isMissing(inst.classIndex())) {
                if (m_windowSize > 0) {
                  primaryMeasure = 1.0 - m_windowEval.errorRate();
                } else {
                  primaryMeasure = 1.0 - m_eval.errorRate();
                }
              } else {
                // record confidence as the primary measure
                // (another possibility would be entropy of
                // the distribution, or perhaps average
                // confidence)
                primaryMeasure = dist[Utils.maxIndex(dist)];
              }
              // double [] dataPoint = new double[1];
              m_dataPoint[0] = primaryMeasure;
              // double min = 0; double max = 100;
              /*
               * ChartEvent e = new
               * ChartEvent(IncrementalClassifierEvaluator.this, m_dataLegend,
               * min, max, dataPoint);
               */

              m_ce.setLegendText(m_dataLegend);
              m_ce.setMin(0);
              m_ce.setMax(1);
              m_ce.setDataPoint(m_dataPoint);
              m_ce.setReset(m_reset);
              m_reset = false;
            } else {
              // numeric class
              if (m_dataPoint.length < 1) {
                m_dataPoint = new double[1];
                if (inst.isMissing(inst.classIndex())) {
                  m_dataLegend.addElement("Prediction");
                } else {
                  m_dataLegend.addElement("RMSE");
                }
              }
              if (!inst.isMissing(inst.classIndex())) {
                double update;
                if (!inst.isMissing(inst.classIndex())) {
                  if (m_windowSize > 0) {
                    update = m_windowEval.rootMeanSquaredError();
                  } else {
                    update = m_eval.rootMeanSquaredError();
                  }
                } else {
                  update = pred;
                }
                m_dataPoint[0] = update;
                if (update > m_max) {
                  m_max = update;
                }
                if (update < m_min) {
                  m_min = update;
                }
              }

              m_ce.setLegendText(m_dataLegend);
              m_ce.setMin((inst.isMissing(inst.classIndex()) ? m_min : 0));
              m_ce.setMax(m_max);
              m_ce.setDataPoint(m_dataPoint);
              m_ce.setReset(m_reset);
              m_reset = false;
            }
            notifyChartListeners(m_ce);
          }
          m_throughput.updateEnd(m_logger);
        }

        if (ce.getStatus() == IncrementalClassifierEvent.BATCH_FINISHED
          || inst == null) {
          if (m_logger != null) {
            m_logger.logMessage("[IncrementalClassifierEvaluator]"
              + statusMessagePrefix() + " Finished processing.");
          }
          m_throughput.finished(m_logger);

          // save memory if using windowed evaluation for charting
          m_windowEval = null;
          m_window = null;
          m_windowedPreds = null;

          if (m_textListeners.size() > 0) {
            String textTitle = ce.getClassifier().getClass().getName();
            textTitle = textTitle.substring(textTitle.lastIndexOf('.') + 1,
              textTitle.length());
            String results = "=== Performance information ===\n\n"
              + "Scheme:   " + textTitle + "\n" + "Relation: "
              + m_eval.getHeader().relationName() + "\n\n"
              + m_eval.toSummaryString();
            if (m_eval.getHeader().classIndex() >= 0
              && m_eval.getHeader().classAttribute().isNominal()
              && (m_outputInfoRetrievalStats)) {
              results += "\n" + m_eval.toClassDetailsString();
            }

            if (m_eval.getHeader().classIndex() >= 0
              && m_eval.getHeader().classAttribute().isNominal()) {
              results += "\n" + m_eval.toMatrixString();
            }
            textTitle = "Results: " + textTitle;
            TextEvent te = new TextEvent(this, results, textTitle);
            notifyTextListeners(te);
          }
        }
      }
    } catch (Exception ex) {
      if (m_logger != null) {
        m_logger.logMessage("[IncrementalClassifierEvaluator]"
          + statusMessagePrefix() + " Error processing prediction "
          + ex.getMessage());
        m_logger.statusMessage(statusMessagePrefix()
          + "ERROR: problem processing prediction (see log for details)");
      }
      ex.printStackTrace();
      stop();
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
      if (!((EventConstraints) m_listenee)
        .eventGeneratable("incrementalClassifier")) {
        return false;
      }
    }
    return true;
  }

  /**
   * Stop all action
   */
  @Override
  public void stop() {
    // tell the listenee (upstream bean) to stop
    if (m_listenee instanceof BeanCommon) {
      // System.err.println("Listener is BeanCommon");
      ((BeanCommon) m_listenee).stop();
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
    return false;
  }

  @SuppressWarnings("unchecked")
  private void notifyChartListeners(ChartEvent ce) {
    Vector<ChartListener> l;
    synchronized (this) {
      l = (Vector<ChartListener>) m_listeners.clone();
    }
    if (l.size() > 0) {
      for (int i = 0; i < l.size(); i++) {
        l.elementAt(i).acceptDataPoint(ce);
      }
    }
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
   * Set how often progress is reported to the status bar.
   * 
   * @param s report progress every s instances
   */
  public void setStatusFrequency(int s) {
    m_statusFrequency = s;
  }

  /**
   * Get how often progress is reported to the status bar.
   * 
   * @return after how many instances, progress is reported to the status bar
   */
  public int getStatusFrequency() {
    return m_statusFrequency;
  }

  /**
   * Return a tip text string for this property
   * 
   * @return a string for the tip text
   */
  public String statusFrequencyTipText() {
    return "How often to report progress to the status bar.";
  }

  /**
   * Set whether to output per-class information retrieval statistics (nominal
   * class only).
   * 
   * @param i true if info retrieval stats are to be output
   */
  public void setOutputPerClassInfoRetrievalStats(boolean i) {
    m_outputInfoRetrievalStats = i;
  }

  /**
   * Get whether per-class information retrieval stats are to be output.
   * 
   * @return true if info retrieval stats are to be output
   */
  public boolean getOutputPerClassInfoRetrievalStats() {
    return m_outputInfoRetrievalStats;
  }

  /**
   * Return a tip text string for this property
   * 
   * @return a string for the tip text
   */
  public String outputPerClassInfoRetrievalStatsTipText() {
    return "Output per-class info retrieval stats. If set to true, predictions get "
      + "stored so that stats such as AUC can be computed. Note: this consumes some memory.";
  }

  /**
   * Set whether to compute evaluation for charting over a fixed sized window of
   * the most recent instances (rather than the whole stream).
   * 
   * @param windowSize the size of the window to use for computing the
   *          evaluation metrics used for charting. Setting a value of zero or
   *          less specifies that no windowing is to be used.
   */
  public void setChartingEvalWindowSize(int windowSize) {
    m_windowSize = windowSize;
  }

  /**
   * Get whether to compute evaluation for charting over a fixed sized window of
   * the most recent instances (rather than the whole stream).
   * 
   * @return the size of the window to use for computing the evaluation metrics
   *         used for charting. Setting a value of zero or less specifies that
   *         no windowing is to be used.
   */
  public int getChartingEvalWindowSize() {
    return m_windowSize;
  }

  /**
   * Return a tip text string for this property
   * 
   * @return a string for the tip text
   */
  public String chartingEvalWindowSizeTipText() {
    return "For charting only, specify a sliding window size over which to compute "
      + "performance stats. <= 0 means eval on whole stream";
  }

  /**
   * Add a chart listener
   * 
   * @param cl a <code>ChartListener</code> value
   */
  public synchronized void addChartListener(ChartListener cl) {
    m_listeners.addElement(cl);
  }

  /**
   * Remove a chart listener
   * 
   * @param cl a <code>ChartListener</code> value
   */
  public synchronized void removeChartListener(ChartListener cl) {
    m_listeners.remove(cl);
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

  private String statusMessagePrefix() {
    return getCustomName() + "$" + hashCode() + "|";
  }
}
