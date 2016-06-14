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
 *    AbstractClusterer.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.clusterers;

import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.CapabilitiesIgnorer;
import weka.core.CommandlineRunnable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.SerializedObject;
import weka.core.Utils;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Abstract clusterer.
 * 
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 12201 $
 */
public abstract class AbstractClusterer
  implements Clusterer, Cloneable, Serializable, CapabilitiesHandler,
  RevisionHandler, OptionHandler, CapabilitiesIgnorer, CommandlineRunnable {

  /** for serialization */
  private static final long serialVersionUID = -6099962589663877632L;

  /** Whether the clusterer is run in debug mode. */
  protected boolean m_Debug = false;

  /** Whether capabilities should not be checked before clusterer is built. */
  protected boolean m_DoNotCheckCapabilities = false;

  // ===============
  // Public methods.
  // ===============

  /**
   * Generates a clusterer. Has to initialize all fields of the clusterer that
   * are not being set via options.
   * 
   * @param data set of instances serving as training data
   * @exception Exception if the clusterer has not been generated successfully
   */
  @Override
  public abstract void buildClusterer(Instances data) throws Exception;

  /**
   * Classifies a given instance. Either this or distributionForInstance() needs
   * to be implemented by subclasses.
   * 
   * @param instance the instance to be assigned to a cluster
   * @return the number of the assigned cluster as an integer
   * @exception Exception if instance could not be clustered successfully
   */
  @Override
  public int clusterInstance(Instance instance) throws Exception {

    double[] dist = distributionForInstance(instance);

    if (dist == null) {
      throw new Exception("Null distribution predicted");
    }

    if (Utils.sum(dist) <= 0) {
      throw new Exception("Unable to cluster instance");
    }
    return Utils.maxIndex(dist);
  }

  /**
   * Predicts the cluster memberships for a given instance. Either this or
   * clusterInstance() needs to be implemented by subclasses.
   * 
   * @param instance the instance to be assigned a cluster.
   * @return an array containing the estimated membership probabilities of the
   *         test instance in each cluster (this should sum to at most 1)
   * @exception Exception if distribution could not be computed successfully
   */
  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {

    double[] d = new double[numberOfClusters()];

    d[clusterInstance(instance)] = 1.0;

    return d;
  }

  /**
   * Returns the number of clusters.
   * 
   * @return the number of clusters generated for a training dataset.
   * @exception Exception if number of clusters could not be returned
   *              successfully
   */
  @Override
  public abstract int numberOfClusters() throws Exception;

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = Option
      .listOptionsForClassHierarchy(this.getClass(), AbstractClusterer.class);

    newVector.addElement(new Option(
      "\tIf set, clusterer is run in debug mode and\n"
        + "\tmay output additional info to the console",
      "output-debug-info", 0, "-output-debug-info"));
    newVector.addElement(new Option(
      "\tIf set, clusterer capabilities are not checked before clusterer is built\n"
        + "\t(use with caution).",
      "-do-not-check-capabilities", 0, "-do-not-check-capabilities"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:
   * <p>
   * 
   * -D <br>
   * If set, clusterer is run in debug mode and may output additional info to
   * the console.
   * <p>
   * 
   * -do-not-check-capabilities <br>
   * If set, clusterer capabilities are not checked before clusterer is built
   * (use with caution).
   * <p>
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    Option.setOptionsForHierarchy(options, this, AbstractClusterer.class);
    setDebug(Utils.getFlag("output-debug-info", options));
    setDoNotCheckCapabilities(
      Utils.getFlag("do-not-check-capabilities", options));
  }

  /**
   * Set debugging mode.
   * 
   * @param debug true if debug output should be printed
   */
  public void setDebug(boolean debug) {

    m_Debug = debug;
  }

  /**
   * Get whether debugging is turned on.
   * 
   * @return true if debugging output is on
   */
  public boolean getDebug() {

    return m_Debug;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String debugTipText() {
    return "If set to true, clusterer may output additional info to "
      + "the console.";
  }

  /**
   * Set whether not to check capabilities.
   * 
   * @param doNotCheckCapabilities true if capabilities are not to be checked.
   */
  public void setDoNotCheckCapabilities(boolean doNotCheckCapabilities) {

    m_DoNotCheckCapabilities = doNotCheckCapabilities;
  }

  /**
   * Get whether capabilities checking is turned off.
   * 
   * @return true if capabilities checking is turned off.
   */
  public boolean getDoNotCheckCapabilities() {

    return m_DoNotCheckCapabilities;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String doNotCheckCapabilitiesTipText() {
    return "If set, clusterer capabilities are not checked before clusterer is built"
      + " (Use with caution to reduce runtime).";
  }

  /**
   * Gets the current settings of the clusterer.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();
    for (String s : Option.getOptionsForHierarchy(this,
      AbstractClusterer.class)) {
      options.add(s);
    }

    if (getDebug()) {
      options.add("-output-debug-info");
    }
    if (getDoNotCheckCapabilities()) {
      options.add("-do-not-check-capabilities");
    }

    return options.toArray(new String[0]);
  }

  /**
   * Creates a new instance of a clusterer given it's class name and (optional)
   * arguments to pass to it's setOptions method. If the clusterer implements
   * OptionHandler and the options parameter is non-null, the clusterer will
   * have it's options set.
   * 
   * @param clustererName the fully qualified class name of the clusterer
   * @param options an array of options suitable for passing to setOptions. May
   *          be null.
   * @return the newly created search object, ready for use.
   * @exception Exception if the clusterer class name is invalid, or the options
   *              supplied are not acceptable to the clusterer.
   */
  public static Clusterer forName(String clustererName, String[] options)
    throws Exception {
    return (Clusterer) Utils.forName(Clusterer.class, clustererName, options);
  }

  /**
   * Creates a deep copy of the given clusterer using serialization.
   * 
   * @param model the clusterer to copy
   * @return a deep copy of the clusterer
   * @exception Exception if an error occurs
   */
  public static Clusterer makeCopy(Clusterer model) throws Exception {
    return (Clusterer) new SerializedObject(model).getObject();
  }

  /**
   * Creates copies of the current clusterer. Note that this method now uses
   * Serialization to perform a deep copy, so the Clusterer object must be fully
   * Serializable. Any currently built model will now be copied as well.
   * 
   * @param model an example clusterer to copy
   * @param num the number of clusterer copies to create.
   * @return an array of clusterers.
   * @exception Exception if an error occurs
   */
  public static Clusterer[] makeCopies(Clusterer model, int num)
    throws Exception {
    if (model == null) {
      throw new Exception("No model clusterer set");
    }
    Clusterer[] clusterers = new Clusterer[num];
    SerializedObject so = new SerializedObject(model);
    for (int i = 0; i < clusterers.length; i++) {
      clusterers[i] = (Clusterer) so.getObject();
    }
    return clusterers;
  }

  /**
   * Returns the Capabilities of this clusterer. Derived clusterers have to
   * override this method to enable capabilities.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result;

    result = new Capabilities(this);
    result.enableAll();

    return result;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12201 $");
  }

  /**
   * runs the clusterer instance with the given options.
   * 
   * @param clusterer the clusterer to run
   * @param options the commandline options
   */
  public static void runClusterer(Clusterer clusterer, String[] options) {
    try {
      if (clusterer instanceof CommandlineRunnable) {
        ((CommandlineRunnable) clusterer).preExecution();
      }
      System.out
        .println(ClusterEvaluation.evaluateClusterer(clusterer, options));
    } catch (Exception e) {
      if ((e.getMessage() == null) || ((e.getMessage() != null)
        && (e.getMessage().indexOf("General options") == -1))) {
        e.printStackTrace();
      } else {
        System.err.println(e.getMessage());
      }
    }
    try {
      if (clusterer instanceof CommandlineRunnable) {
        ((CommandlineRunnable) clusterer).postExecution();
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Perform any setup stuff that might need to happen before commandline
   * execution. Subclasses should override if they need to do something here
   *
   * @throws Exception if a problem occurs during setup
   */
  @Override
  public void preExecution() throws Exception {
  }

  /**
   * Execute the supplied object. Subclasses need to override this method.
   *
   * @param toRun the object to execute
   * @param options any options to pass to the object
   * @throws Exception if the object if a problem occurs
   */
  @Override
  public void run(Object toRun, String[] options) throws Exception {
    if (!(toRun instanceof Clusterer)) {
      throw new IllegalArgumentException(
        "Object to execute is not a Clusterer!");
    }

    runClusterer((Clusterer) toRun, options);
  }

  /**
   * Perform any teardown stuff that might need to happen after execution.
   * Subclasses should override if they need to do something here
   *
   * @throws Exception if a problem occurs during teardown
   */
  @Override
  public void postExecution() throws Exception {
  }
}
