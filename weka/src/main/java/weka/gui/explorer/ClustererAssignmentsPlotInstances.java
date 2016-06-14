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
 * ClustererAssignmentsPlotInstances.java
 * Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.explorer;

import java.util.ArrayList;

import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Utils;
import weka.gui.visualize.Plot2D;
import weka.gui.visualize.PlotData2D;

/**
 * A class for generating plottable cluster assignments.
 * <p/>
 * Example usage:
 * 
 * <pre>
 * Instances train = ... // from somewhere
 * Instances test = ... // from somewhere
 * Clusterer cls = ... // from somewhere
 * // build and evaluate clusterer
 * cls.buildClusterer(train);
 * ClusterEvaluation eval = new ClusterEvaluation();
 * eval.setClusterer(cls);
 * eval.evaluateClusterer(test);
 * // generate plot instances
 * ClustererPlotInstances plotInstances = new ClustererPlotInstances();
 * plotInstances.setClusterer(cls);
 * plotInstances.setInstances(test);
 * plotInstances.setClusterer(cls);
 * plotInstances.setClusterEvaluation(eval);
 * plotInstances.setUp();
 * // generate visualization
 * VisualizePanel visPanel = new VisualizePanel();
 * visPanel.addPlot(plotInstances.getPlotData("plot name"));
 * // clean up
 * plotInstances.cleanUp();
 * </pre>
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10222 $
 */
public class ClustererAssignmentsPlotInstances extends AbstractPlotInstances {

  /** for serialization. */
  private static final long serialVersionUID = -4748134272046520423L;

  /** for storing the plot shapes. */
  protected int[] m_PlotShapes;

  /** the clusterer being used. */
  protected Clusterer m_Clusterer;

  /** the cluster evaluation to use. */
  protected ClusterEvaluation m_Evaluation;

  /**
   * Initializes the members.
   */
  @Override
  protected void initialize() {
    super.initialize();

    m_PlotShapes = null;
    m_Clusterer = null;
    m_Evaluation = null;
  }

  /**
   * Sets the classifier used for making the predictions.
   * 
   * @param value the clusterer to use
   */
  public void setClusterer(Clusterer value) {
    m_Clusterer = value;
  }

  /**
   * Returns the currently set clusterer.
   * 
   * @return the clusterer in use
   */
  public Clusterer getClusterer() {
    return m_Clusterer;
  }

  /**
   * Sets the cluster evaluation object to use.
   * 
   * @param value the evaluation object
   */
  public void setClusterEvaluation(ClusterEvaluation value) {
    m_Evaluation = value;
  }

  /**
   * Returns the cluster evaluation object in use.
   * 
   * @return the evaluation object
   */
  public ClusterEvaluation getClusterEvaluation() {
    return m_Evaluation;
  }

  /**
   * Checks whether clusterer and evaluation are provided.
   */
  @Override
  protected void check() {
    super.check();

    if (m_Clusterer == null) {
      throw new IllegalStateException("No clusterer set!");
    }

    if (m_Evaluation == null) {
      throw new IllegalStateException("No cluster evaluation set!");
    }
  }

  /**
   * Sets up the structure for the plot instances.
   */
  @Override
  protected void determineFormat() {
    int numClusters;
    ArrayList<Attribute> hv;
    Attribute predictedCluster;
    ArrayList<String> clustVals;
    int i;

    numClusters = m_Evaluation.getNumClusters();
    hv = new ArrayList<Attribute>();
    clustVals = new ArrayList<String>();

    for (i = 0; i < numClusters; i++) {
      clustVals.add("cluster" + /* (i+1) */i);
    }
    predictedCluster = new Attribute("Cluster", clustVals);
    for (i = 0; i < m_Instances.numAttributes(); i++) {
      hv.add((Attribute) m_Instances.attribute(i).copy());
    }
    hv.add(predictedCluster);

    m_PlotInstances = new Instances(m_Instances.relationName() + "_clustered",
      hv, m_Instances.numInstances());
  }

  /**
   * Generates the cluster assignments.
   * 
   * @see #m_PlotShapes
   * @see #m_PlotSizes
   * @see #m_PlotInstances
   */
  protected void process() {
    double[] clusterAssignments;
    int i;
    double[] values;
    int j;
    int[] classAssignments;

    clusterAssignments = m_Evaluation.getClusterAssignments();

    classAssignments = null;
    if (m_Instances.classIndex() >= 0) {
      classAssignments = m_Evaluation.getClassesToClusters();
      m_PlotShapes = new int[m_Instances.numInstances()];
      for (i = 0; i < m_Instances.numInstances(); i++) {
        m_PlotShapes[i] = Plot2D.CONST_AUTOMATIC_SHAPE;
      }
    }

    for (i = 0; i < m_Instances.numInstances(); i++) {
      values = new double[m_PlotInstances.numAttributes()];
      for (j = 0; j < m_Instances.numAttributes(); j++) {
        values[j] = m_Instances.instance(i).value(j);
      }
      if (clusterAssignments[i] < 0) {
        values[j] = Utils.missingValue();
      } else {
        values[j] = clusterAssignments[i];
      }
      m_PlotInstances.add(new DenseInstance(1.0, values));
      if (m_PlotShapes != null) {
        if (clusterAssignments[i] >= 0) {
          if ((int) m_Instances.instance(i).classValue() != classAssignments[(int) clusterAssignments[i]]) {
            m_PlotShapes[i] = Plot2D.ERROR_SHAPE;
          }
        } else {
          m_PlotShapes[i] = Plot2D.MISSING_SHAPE;
        }
      }
    }
  }

  /**
   * Performs optional post-processing.
   */
  @Override
  protected void finishUp() {
    super.finishUp();

    process();
  }

  /**
   * Assembles and returns the plot. The relation name of the dataset gets added
   * automatically.
   * 
   * @param name the name of the plot
   * @return the plot
   * @throws Exception if plot generation fails
   */
  @Override
  protected PlotData2D createPlotData(String name) throws Exception {
    PlotData2D result;

    result = new PlotData2D(m_PlotInstances);
    if (m_PlotShapes != null) {
      result.setShapeType(m_PlotShapes);
    }
    result.addInstanceNumberAttribute();
    result.setPlotName(name + " (" + m_Instances.relationName() + ")");

    return result;
  }

  /**
   * For freeing up memory. Plot data cannot be generated after this call!
   */
  @Override
  public void cleanUp() {
    super.cleanUp();

    m_Clusterer = null;
    m_Evaluation = null;
    m_PlotShapes = null;
  }
}
