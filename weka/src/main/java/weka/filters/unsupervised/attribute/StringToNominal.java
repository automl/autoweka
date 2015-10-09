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
 *    StringToNominal.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import java.util.Enumeration;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.RevisionUtils;
import weka.core.UnsupportedAttributeTypeException;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;

/**
 <!-- globalinfo-start -->
 * Converts a range of string attributes (unspecified number of values) to nominal (set number of values). You should ensure that all string values that will appear are represented in the first batch of the data.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -R &lt;col&gt;
 *  Sets the range of attribute indices (default last).</pre>
 * 
 * <pre> -V &lt;col&gt;
 *  Invert the range specified by -R.</pre>
 * 
 <!-- options-end -->
 * 
 * @author Len Trigg (len@reeltwo.com)
 * @version $Revision: 9179 $
 */
public class StringToNominal extends Filter implements UnsupervisedFilter,
    OptionHandler {

  /** for serialization */
  private static final long serialVersionUID = 4864084427902797605L;

  /** The attribute's range indices setting. */
  private final Range m_AttIndices = new Range("last");

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {

    return "Converts a range of string attributes (unspecified number of values) to nominal "
        + "(set number of values). You should ensure that all string values that "
        + "will appear are represented in the first batch of the data.";
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
   * @return true if the outputFormat may be collected immediately.
   * @throws UnsupportedAttributeTypeException if the selected attribute a
   *           string attribute.
   * @throws Exception if the input format can't be set successfully.
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);
    m_AttIndices.setUpper(instanceInfo.numAttributes() - 1);
    return false;
  }

  /**
   * Input an instance for filtering. The instance is processed and made
   * available for output immediately.
   * 
   * @param instance the input instance.
   * @return true if the filtered instance may now be collected with output().
   * @throws IllegalStateException if no input structure has been defined.
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

    if (isOutputFormatDefined()) {
      Instance newInstance = (Instance) instance.copy();

      // make sure that we get the right indexes set for the converted
      // string attributes when operating on a second batch of instances
      for (int i = 0; i < newInstance.numAttributes(); i++) {
        if (newInstance.attribute(i).isString() && !newInstance.isMissing(i)
            && m_AttIndices.isInRange(i)) {
          Attribute outAtt = getOutputFormat().attribute(
              newInstance.attribute(i).name());
          String inVal = newInstance.stringValue(i);
          int outIndex = outAtt.indexOfValue(inVal);
          if (outIndex < 0) {
            newInstance.setMissing(i);
          } else {
            newInstance.setValue(i, outIndex);
          }
        }
      }
      push(newInstance);
      return true;
    }

    bufferInput(instance);
    return false;
  }

  /**
   * Signifies that this batch of input to the filter is finished. If the filter
   * requires all instances prior to filtering, output() may now be called to
   * retrieve the filtered instances.
   * 
   * @return true if there are instances pending output.
   * @throws IllegalStateException if no input structure has been defined.
   */
  @Override
  public boolean batchFinished() {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (!isOutputFormatDefined()) {

      setOutputFormat();

      // Convert pending input instances
      for (int i = 0; i < getInputFormat().numInstances(); i++) {
        Instance temp = (Instance) getInputFormat().instance(i).copy();
        for (int j = 0; j < temp.numAttributes(); j++) {
          if (temp.attribute(j).isString() && !temp.isMissing(j)
              && m_AttIndices.isInRange(j)) {

            // adjust indexes for the removal of the dummy string value
            temp.setValue(j, temp.value(j) - 1);
          }
        }
        push(temp);
      }
    }

    flushInput();
    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(1);

    newVector.addElement(new Option(
        "\tSets the range of attribute indices (default last).", "R", 1,
        "-R <col>"));

    newVector.addElement(new Option("\tInvert the range specified by -R.", "V",
        1, "-V <col>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -R &lt;col&gt;
   *  Sets the range of attribute indices (default last).</pre>
   * 
   * <pre> -V &lt;col&gt;
   *  Invert the range specified by -R.</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String attIndices = Utils.getOption('R', options);
    if (attIndices.length() != 0) {
      setAttributeRange(attIndices);
    } else {
      setAttributeRange("last");
    }

    String invertSelection = Utils.getOption('V', options);
    if (invertSelection.length() != 0) {
      m_AttIndices.setInvert(true);
    } else {
      m_AttIndices.setInvert(false);
    }

    if (getInputFormat() != null) {
      setInputFormat(getInputFormat());
    }
  }

  /**
   * Gets the current settings of the filter.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    String[] options = new String[this.m_AttIndices.getInvert() ? 7 : 6];
    int current = 0;

    options[current++] = "-R";
    options[current++] = "" + (getAttributeRange());

    while (current < options.length) {
      options[current++] = "";
    }

    if (this.m_AttIndices.getInvert()) {
      options[current++] = "-V";
    }

    return options;
  }

  /**
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String attributeRangeTipText() {

    return "Sets which attributes to process. This attributes "
        + "must be string attributes (\"first\" and \"last\" are valid values "
        + "as well as ranges and lists)";
  }

  /**
   * Get the range of indices of the attributes used.
   * 
   * @return the index of the attribute
   */
  public String getAttributeRange() {

    return m_AttIndices.getRanges();
  }

  /**
   * Sets range of indices of the attributes used.
   * 
   * @param rangeList the list of attribute indices
   */
  public void setAttributeRange(String rangeList) {

    m_AttIndices.setRanges(rangeList);
  }

  /**
   * Set the output format. Takes the current average class values and
   * m_InputFormat and calls setOutputFormat(Instances) appropriately.
   */
  private void setOutputFormat() {

    Instances newData;
    FastVector newAtts, newVals;

    // Compute new attributes

    newAtts = new FastVector(getInputFormat().numAttributes());
    for (int j = 0; j < getInputFormat().numAttributes(); j++) {
      Attribute att = getInputFormat().attribute(j);
      if (!m_AttIndices.isInRange(j) || !att.isString()) {

        // We don't have to copy the attribute because the
        // attribute index remains unchanged.
        newAtts.addElement(att);
      } else {

        // Compute list of attribute values - skipping the dummy string value
        newVals = new FastVector(att.numValues());
        for (int i = 1; i < att.numValues(); i++) {
          newVals.addElement(att.value(i));
        }
        Attribute newAtt = new Attribute(att.name(), newVals);
        newAtt.setWeight(att.weight());
        newAtts.addElement(newAtt);
      }
    }

    // Construct new header
    newData = new Instances(getInputFormat().relationName(), newAtts, 0);
    newData.setClassIndex(getInputFormat().classIndex());
    setOutputFormat(newData);
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 9179 $");
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String[] argv) {
    runFilter(new StringToNominal(), argv);
  }
}

