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
 *    RemoveRange.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.instance;

import java.util.Enumeration;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;

/**
 * <!-- globalinfo-start --> A filter that removes a given range of instances of
 * a dataset.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -R &lt;inst1,inst2-inst4,...&gt;
 *  Specifies list of instances to select. First and last
 *  are valid indexes. (required)
 * </pre>
 * 
 * <pre>
 * -V
 *  Specifies if inverse of selection is to be output.
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 12037 $
 */
public class RemoveRange extends Filter implements UnsupervisedFilter,
  OptionHandler {

  /** for serialization */
  static final long serialVersionUID = -3064641215340828695L;

  /** Range of instances requested by the user. */
  private final Range m_Range = new Range("first-last");

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(2);

    newVector.addElement(new Option(
      "\tSpecifies list of instances to select. First and last\n"
        + "\tare valid indexes. (required)\n", "R", 1,
      "-R <inst1,inst2-inst4,...>"));

    newVector.addElement(new Option(
      "\tSpecifies if inverse of selection is to be output.\n", "V", 0, "-V"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -R &lt;inst1,inst2-inst4,...&gt;
   *  Specifies list of instances to select. First and last
   *  are valid indexes. (required)
   * </pre>
   * 
   * <pre>
   * -V
   *  Specifies if inverse of selection is to be output.
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of string.s
   * @throws Exception if an option is not supported.
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String str = Utils.getOption('R', options);
    if (str.length() != 0) {
      setInstancesIndices(str);
    } else {
      setInstancesIndices("first-last");
    }
    setInvertSelection(Utils.getFlag('V', options));

    if (getInputFormat() != null) {
      setInputFormat(getInputFormat());
    }

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the filter.
   * 
   * @return an array of strings suitable for passing to setOptions.
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    if (getInvertSelection()) {
      options.add("-V");
    }
    options.add("-R");
    options.add(getInstancesIndices());

    return options.toArray(new String[0]);
  }

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the GUI.
   */
  public String globalInfo() {

    return "A filter that removes a given range of instances of a dataset.";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String instancesIndicesTipText() {

    return "The range of instances to select. First and last are valid indexes.";
  }

  /**
   * Gets ranges of instances selected.
   * 
   * @return a string containing a comma-separated list of ranges
   */
  public String getInstancesIndices() {

    return m_Range.getRanges();
  }

  /**
   * Sets the ranges of instances to be selected. If provided string is null,
   * ranges won't be used for selecting instances.
   * 
   * @param rangeList a string representing the list of instances. eg:
   *          first-3,5,6-last
   * @throws IllegalArgumentException if an invalid range list is supplied
   */
  public void setInstancesIndices(String rangeList) {

    m_Range.setRanges(rangeList);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String invertSelectionTipText() {

    return "Whether to invert the selection.";
  }

  /**
   * Gets if selection is to be inverted.
   * 
   * @return true if the selection is to be inverted
   */
  public boolean getInvertSelection() {

    return m_Range.getInvert();
  }

  /**
   * Sets if selection is to be inverted.
   * 
   * @param inverse true if inversion is to be performed
   */
  public void setInvertSelection(boolean inverse) {

    m_Range.setInvert(inverse);
  }

  /**
   * Returns the Capabilities of this filter.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enableAllAttributes();
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Sets the format of the input instances.
   * 
   * @param instanceInfo an Instances object containing the input instance
   *          structure (any instances contained in the object are ignored -
   *          only the structure is required).
   * @return true because outputFormat can be collected immediately
   * @throws Exception if the input format can't be set successfully
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);
    setOutputFormat(instanceInfo);
    return true;
  }

  /**
   * Input an instance for filtering. Filter requires all training instances be
   * read before producing output.
   * 
   * @param instance the input instance
   * @return true if the filtered instance may now be collected with output().
   * @throws IllegalStateException if no input structure has been defined
   */
  @Override
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    if (isFirstBatchDone()) {
      push(instance);
      return true;
    } else {
      bufferInput(instance);
      return false;
    }
  }

  /**
   * Signify that this batch of input to the filter is finished. Output() may
   * now be called to retrieve the filtered instances.
   * 
   * @return true if there are instances pending output
   * @throws IllegalStateException if no input structure has been defined
   */
  @Override
  public boolean batchFinished() {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    if (!isFirstBatchDone()) {
      // Push instances for output into output queue
      m_Range.setUpper(getInputFormat().numInstances() - 1);
      for (int i = 0; i < getInputFormat().numInstances(); i++) {
        if (!m_Range.isInRange(i)) {
          push(getInputFormat().instance(i), false); // No need to copy
        }
      }
    } else {
      for (int i = 0; i < getInputFormat().numInstances(); i++) {
        push(getInputFormat().instance(i), false); // No need to copy
      }
    }

    flushInput();

    m_NewBatch = true;
    m_FirstBatchDone = true;

    return (numPendingOutput() != 0);
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12037 $");
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String[] argv) {
    runFilter(new RemoveRange(), argv);
  }
}
