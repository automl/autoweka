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
 *    Kernel.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.functions.supportVector;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.CapabilitiesIgnorer;
import weka.core.Copyable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.SerializedObject;
import weka.core.Utils;

/**
 * Abstract kernel. Kernels implementing this class must respect Mercer's
 * condition in order to ensure a correct behaviour of SMOreg.
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 11013 $
 */
public abstract class Kernel implements Serializable, OptionHandler,
  CapabilitiesHandler, CapabilitiesIgnorer,
  RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = -6102771099905817064L;

  /** The dataset */
  protected Instances m_data;

  /** enables debugging output */
  protected boolean m_Debug = false;

  /** Turns off all checks */
  protected boolean m_ChecksTurnedOff = false;

  /** Whether capabilities should not be checked */
  protected boolean m_DoNotCheckCapabilities = false;

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String doNotCheckCapabilitiesTipText() {
    return "If set, associator capabilities are not checked before associator is built"
      + " (Use with caution to reduce runtime).";
  }

  /**
   * Set whether not to check capabilities.
   * 
   * @param doNotCheckCapabilities true if capabilities are not to be checked.
   */
  @Override
  public void setDoNotCheckCapabilities(boolean doNotCheckCapabilities) {

    m_DoNotCheckCapabilities = doNotCheckCapabilities;
  }

  /**
   * Get whether capabilities checking is turned off.
   * 
   * @return true if capabilities checking is turned off.
   */
  @Override
  public boolean getDoNotCheckCapabilities() {

    return m_DoNotCheckCapabilities;
  }

  /**
   * Returns a string describing the kernel
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public abstract String globalInfo();

  /**
   * Computes the result of the kernel function for two instances. If id1 == -1,
   * eval use inst1 instead of an instance in the dataset.
   * 
   * @param id1 the index of the first instance in the dataset
   * @param id2 the index of the second instance in the dataset
   * @param inst1 the instance corresponding to id1 (used if id1 == -1)
   * @return the result of the kernel function
   * @throws Exception if something goes wrong
   */
  public abstract double eval(int id1, int id2, Instance inst1)
    throws Exception;

  /**
   * Frees the memory used by the kernel. (Useful with kernels which use cache.)
   * This function is called when the training is done. i.e. after that, eval
   * will be called with id1 == -1.
   */
  public abstract void clean();

  /**
   * Returns the number of kernel evaluation performed.
   * 
   * @return the number of kernel evaluation performed.
   */
  public abstract int numEvals();

  /**
   * Returns the number of dot product cache hits.
   * 
   * @return the number of dot product cache hits, or -1 if not supported by
   *         this kernel.
   */
  public abstract int numCacheHits();

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option(
      "\tEnables debugging output (if available) to be printed.\n"
        + "\t(default: off)", "output-debug-info", 0, "-output-debug-info"));

    result.addElement(new Option("\tTurns off all checks - use with caution!\n"
      + "\t(default: checks on)", "no-checks", 0, "-no-checks"));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    setDebug(Utils.getFlag("output-debug-info", options));

    setChecksTurnedOff(Utils.getFlag("no-checks", options));

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the Kernel.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    if (getDebug()) {
      result.add("-output-debug-info");
    }

    if (getChecksTurnedOff()) {
      result.add("-no-checks");
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Enables or disables the output of debug information (if the derived kernel
   * supports that)
   * 
   * @param value whether to output debugging information
   */
  public void setDebug(boolean value) {
    m_Debug = value;
  }

  /**
   * Gets whether debugging output is turned on or not.
   * 
   * @return true if debugging output is produced.
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
    return "Turns on the output of debugging information.";
  }

  /**
   * Disables or enables the checks (which could be time-consuming). Use with
   * caution!
   * 
   * @param value if true turns off all checks
   */
  public void setChecksTurnedOff(boolean value) {
    m_ChecksTurnedOff = value;
  }

  /**
   * Returns whether the checks are turned off or not.
   * 
   * @return true if the checks are turned off
   */
  public boolean getChecksTurnedOff() {
    return m_ChecksTurnedOff;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String checksTurnedOffTipText() {
    return "Turns time-consuming checks off - use with caution.";
  }

  /**
   * initializes variables etc.
   * 
   * @param data the data to use
   */
  protected void initVars(Instances data) {
    m_data = data;
  }

  /**
   * Returns the Capabilities of this kernel. Derived kernels have to override
   * this method to enable capabilities.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = new Capabilities(this);
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
    return RevisionUtils.extract("$Revision: 11013 $");
  }

  /**
   * builds the kernel with the given data
   * 
   * @param data the data to base the kernel on
   * @throws Exception if something goes wrong
   */
  public void buildKernel(Instances data) throws Exception {
    // does kernel handle the data?
    if (!getChecksTurnedOff()) {
      getCapabilities().testWithFail(data);
    }

    initVars(data);
  }

  /**
   * Creates a shallow copy of the kernel (if it implements Copyable) otherwise
   * a deep copy using serialization.
   * 
   * @param kernel the kernel to copy
   * @return a shallow or deep copy of the kernel
   * @throws Exception if an error occurs
   */
  public static Kernel makeCopy(Kernel kernel) throws Exception {
    if (kernel instanceof Copyable) {
      return (Kernel) ((Copyable) kernel).copy();
    }
    return (Kernel) new SerializedObject(kernel).getObject();
  }

  /**
   * Creates a given number of deep or shallow (if the kernel implements
   * Copyable) copies of the given kernel using serialization.
   * 
   * @param model the kernel to copy
   * @param num the number of kernel copies to create.
   * @return an array of kernels.
   * @throws Exception if an error occurs
   */
  public static Kernel[] makeCopies(Kernel model, int num) throws Exception {
    if (model == null) {
      throw new Exception("No model kernel set");
    }

    Kernel[] kernels = new Kernel[num];
    if (model instanceof Copyable) {
      for (int i = 0; i < kernels.length; i++) {
        kernels[i] = (Kernel) ((Copyable) model).copy();
      }
    } else {
      SerializedObject so = new SerializedObject(model);
      for (int i = 0; i < kernels.length; i++) {
        kernels[i] = (Kernel) so.getObject();
      }
    }

    return kernels;
  }

  /**
   * Creates a new instance of a kernel given it's class name and (optional)
   * arguments to pass to it's setOptions method.
   * 
   * @param kernelName the fully qualified class name of the classifier
   * @param options an array of options suitable for passing to setOptions. May
   *          be null.
   * @return the newly created classifier, ready for use.
   * @throws Exception if the classifier name is invalid, or the options
   *           supplied are not acceptable to the classifier
   */
  public static Kernel forName(String kernelName, String[] options)
    throws Exception {

    return (Kernel) Utils.forName(Kernel.class, kernelName, options);
  }
}
