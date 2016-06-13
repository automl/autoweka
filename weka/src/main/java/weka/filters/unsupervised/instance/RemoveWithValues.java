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
 *    RemoveWithValues.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.instance;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.RevisionUtils;
import weka.core.SingleIndex;
import weka.core.UnsupportedAttributeTypeException;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.StreamableFilter;
import weka.filters.UnsupervisedFilter;

/**
 * <!-- globalinfo-start --> Filters instances according to the value of an
 * attribute.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -C &lt;num&gt;
 *  Choose attribute to be used for selection.
 * </pre>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  Numeric value to be used for selection on numeric
 *  attribute.
 *  Instances with values smaller than given value will
 *  be selected. (default 0)
 * </pre>
 * 
 * <pre>
 * -L &lt;index1,index2-index4,...&gt;
 *  Range of label indices to be used for selection on
 *  nominal attribute.
 *  First and last are valid indexes. (default all values)
 * </pre>
 * 
 * <pre>
 * -M
 *  Missing values count as a match. This setting is
 *  independent of the -V option.
 *  (default missing values don't match)
 * </pre>
 * 
 * <pre>
 * -V
 *  Invert matching sense.
 * </pre>
 * 
 * <pre>
 * -H
 *  When selecting on nominal attributes, removes header
 *  references to excluded values.
 * </pre>
 * 
 * <pre>
 * -F
 *  Do not apply the filter to instances that arrive after the first
 *  (training) batch. The default is to apply the filter (i.e.
 *  the filter may not return an instance if it matches the remove criteria)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 12037 $
 */
public class RemoveWithValues extends Filter implements UnsupervisedFilter,
  StreamableFilter, OptionHandler {

  /** for serialization */
  static final long serialVersionUID = 4752870193679263361L;

  /** The attribute's index setting. */
  private final SingleIndex m_AttIndex = new SingleIndex("last");

  /** Stores which values of nominal attribute are to be used for filtering. */
  protected Range m_Values;

  /** Stores which value of a numeric attribute is to be used for filtering. */
  protected double m_Value = 0;

  /** True if missing values should count as a match */
  protected boolean m_MatchMissingValues = false;

  /** Modify header for nominal attributes? */
  protected boolean m_ModifyHeader = false;

  /** If m_ModifyHeader, stores a mapping from old to new indexes */
  protected int[] m_NominalMapping;

  /** Whether to filter instances after the first batch has been processed */
  protected boolean m_dontFilterAfterFirstBatch = false;

  /**
   * Returns a string describing this classifier
   * 
   * @return a description of the classifier suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Filters instances according to the value of an attribute.";
  }

  /** Default constructor */
  public RemoveWithValues() {

    m_Values = new Range("first-last");
    m_Values.setInvert(true);
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(7);

    newVector.addElement(new Option(
      "\tChoose attribute to be used for selection.", "C", 1, "-C <num>"));
    newVector.addElement(new Option(
      "\tNumeric value to be used for selection on numeric\n"
        + "\tattribute.\n"
        + "\tInstances with values smaller than given value will\n"
        + "\tbe selected. (default 0)", "S", 1, "-S <num>"));
    newVector.addElement(new Option(
      "\tRange of label indices to be used for selection on\n"
        + "\tnominal attribute.\n"
        + "\tFirst and last are valid indexes. (default all values)", "L", 1,
      "-L <index1,index2-index4,...>"));
    newVector.addElement(new Option(
      "\tMissing values count as a match. This setting is\n"
        + "\tindependent of the -V option.\n"
        + "\t(default missing values don't match)", "M", 0, "-M"));
    newVector.addElement(new Option("\tInvert matching sense.", "V", 0, "-V"));
    newVector.addElement(new Option(
      "\tWhen selecting on nominal attributes, removes header\n"
        + "\treferences to excluded values.", "H", 0, "-H"));
    newVector
      .addElement(new Option(
        "\tDo not apply the filter to instances that arrive after the first\n"
          + "\t(training) batch. The default is to apply the filter (i.e.\n"
          + "\tthe filter may not return an instance if it matches the remove criteria)",
        "F", 0, "-F"));

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
   * -C &lt;num&gt;
   *  Choose attribute to be used for selection.
   * </pre>
   * 
   * <pre>
   * -S &lt;num&gt;
   *  Numeric value to be used for selection on numeric
   *  attribute.
   *  Instances with values smaller than given value will
   *  be selected. (default 0)
   * </pre>
   * 
   * <pre>
   * -L &lt;index1,index2-index4,...&gt;
   *  Range of label indices to be used for selection on
   *  nominal attribute.
   *  First and last are valid indexes. (default all values)
   * </pre>
   * 
   * <pre>
   * -M
   *  Missing values count as a match. This setting is
   *  independent of the -V option.
   *  (default missing values don't match)
   * </pre>
   * 
   * <pre>
   * -V
   *  Invert matching sense.
   * </pre>
   * 
   * <pre>
   * -H
   *  When selecting on nominal attributes, removes header
   *  references to excluded values.
   * </pre>
   * 
   * <pre>
   * -F
   *  Do not apply the filter to instances that arrive after the first
   *  (training) batch. The default is to apply the filter (i.e.
   *  the filter may not return an instance if it matches the remove criteria)
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String attIndex = Utils.getOption('C', options);
    if (attIndex.length() != 0) {
      setAttributeIndex(attIndex);
    } else {
      setAttributeIndex("last");
    }

    String splitPoint = Utils.getOption('S', options);
    if (splitPoint.length() != 0) {
      setSplitPoint((new Double(splitPoint)).doubleValue());
    } else {
      setSplitPoint(0);
    }

    String convertList = Utils.getOption('L', options);
    if (convertList.length() != 0) {
      setNominalIndices(convertList);
    } else {
      setNominalIndices("first-last");
    }
    setInvertSelection(Utils.getFlag('V', options));
    setMatchMissingValues(Utils.getFlag('M', options));
    setModifyHeader(Utils.getFlag('H', options));
    setDontFilterAfterFirstBatch(Utils.getFlag('F', options));
    // Re-initialize output format according to new options

    if (getInputFormat() != null) {
      setInputFormat(getInputFormat());
    }

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the filter.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    options.add("-S");
    options.add("" + getSplitPoint());
    options.add("-C");
    options.add("" + (getAttributeIndex()));
    if (!getNominalIndices().equals("")) {
      options.add("-L");
      options.add(getNominalIndices());
    }
    if (getInvertSelection()) {
      options.add("-V");
    }
    if (getMatchMissingValues()) {
      options.add("-M");
    }
    if (getModifyHeader()) {
      options.add("-H");
    }
    if (getDontFilterAfterFirstBatch()) {
      options.add("-F");
    }

    return options.toArray(new String[0]);
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
   * @throws UnsupportedAttributeTypeException if the specified attribute is
   *           neither numeric or nominal.
   * @return true because outputFormat can be collected immediately
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);

    m_AttIndex.setUpper(instanceInfo.numAttributes() - 1);
    if (!isNumeric() && !isNominal()) {
      throw new UnsupportedAttributeTypeException("Can only handle numeric "
        + "or nominal attributes.");
    }
    m_Values
      .setUpper(instanceInfo.attribute(m_AttIndex.getIndex()).numValues() - 1);
    if (isNominal() && m_ModifyHeader) {
      instanceInfo = new Instances(instanceInfo, 0); // copy before modifying
      Attribute oldAtt = instanceInfo.attribute(m_AttIndex.getIndex());
      int[] selection = m_Values.getSelection();
      ArrayList<String> newVals = new ArrayList<String>();
      for (int element : selection) {
        newVals.add(oldAtt.value(element));
      }
      Attribute newAtt = new Attribute(oldAtt.name(), newVals);
      newAtt.setWeight(oldAtt.weight());
      instanceInfo.replaceAttributeAt(newAtt, m_AttIndex.getIndex());
      m_NominalMapping = new int[oldAtt.numValues()];
      for (int i = 0; i < m_NominalMapping.length; i++) {
        boolean found = false;
        for (int j = 0; j < selection.length; j++) {
          if (selection[j] == i) {
            m_NominalMapping[i] = j;
            found = true;
            break;
          }
        }
        if (!found) {
          m_NominalMapping[i] = -1;
        }
      }
    }
    setOutputFormat(instanceInfo);
    return true;
  }

  /**
   * Input an instance for filtering. Ordinarily the instance is processed and
   * made available for output immediately. Some filters require all instances
   * be read before producing output.
   * 
   * @param instance the input instance
   * @return true if the filtered instance may now be collected with output().
   * @throws IllegalStateException if no input format has been set.
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

    if (isFirstBatchDone() && m_dontFilterAfterFirstBatch) {
      push((Instance) instance.copy(), false); // No need to copy
      return true;
    }

    if (instance.isMissing(m_AttIndex.getIndex())) {
      if (!getMatchMissingValues()) {
        push((Instance) instance.copy(), false); // No need to copy
        return true;
      } else {
        return false;
      }
    }
    if (isNumeric()) {
      if (!m_Values.getInvert()) {
        if (instance.value(m_AttIndex.getIndex()) < m_Value) {
          push((Instance) instance.copy(), false); // No need to copy
          return true;
        }
      } else {
        if (instance.value(m_AttIndex.getIndex()) >= m_Value) {
          push((Instance) instance.copy(), false); // No need to copy
          return true;
        }
      }
    }
    if (isNominal()) {
      if (m_Values.isInRange((int) instance.value(m_AttIndex.getIndex()))) {
        Instance temp = (Instance) instance.copy();
        if (getModifyHeader()) {
          temp.setValue(m_AttIndex.getIndex(),
            m_NominalMapping[(int) instance.value(m_AttIndex.getIndex())]);
        }
        push(temp, false); // No need to copy
        return true;
      }
    }
    return false;
  }

  /**
   * RemoveWithValues may return false from input() (thus not making an instance
   * available immediately) even after the first batch has been completed due to
   * matching a value that the user wants to remove. Therefore this method
   * returns true.
   * 
   * @return true
   */
  @Override
  public boolean mayRemoveInstanceAfterFirstBatchDone() {
    return true;
  }

  /**
   * Returns true if selection attribute is nominal.
   * 
   * @return true if selection attribute is nominal
   */
  public boolean isNominal() {

    if (getInputFormat() == null) {
      return false;
    } else {
      return getInputFormat().attribute(m_AttIndex.getIndex()).isNominal();
    }
  }

  /**
   * Returns true if selection attribute is numeric.
   * 
   * @return true if selection attribute is numeric
   */
  public boolean isNumeric() {

    if (getInputFormat() == null) {
      return false;
    } else {
      return getInputFormat().attribute(m_AttIndex.getIndex()).isNumeric();
    }
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String modifyHeaderTipText() {
    return "When selecting on nominal attributes, removes header references to "
      + "excluded values.";
  }

  /**
   * Gets whether the header will be modified when selecting on nominal
   * attributes.
   * 
   * @return true if so.
   */
  public boolean getModifyHeader() {

    return m_ModifyHeader;
  }

  /**
   * Sets whether the header will be modified when selecting on nominal
   * attributes.
   * 
   * @param newModifyHeader true if so.
   */
  public void setModifyHeader(boolean newModifyHeader) {

    m_ModifyHeader = newModifyHeader;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String attributeIndexTipText() {
    return "Choose attribute to be used for selection (default last).";
  }

  /**
   * Get the index of the attribute used.
   * 
   * @return the index of the attribute
   */
  public String getAttributeIndex() {

    return m_AttIndex.getSingleIndex();
  }

  /**
   * Sets index of the attribute used.
   * 
   * @param attIndex the index of the attribute
   */
  public void setAttributeIndex(String attIndex) {

    m_AttIndex.setSingleIndex(attIndex);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String splitPointTipText() {
    return "Numeric value to be used for selection on numeric attribute. "
      + "Instances with values smaller than given value will be selected.";
  }

  /**
   * Get the split point used for numeric selection
   * 
   * @return the numeric split point
   */
  public double getSplitPoint() {

    return m_Value;
  }

  /**
   * Split point to be used for selection on numeric attribute.
   * 
   * @param value the split point
   */
  public void setSplitPoint(double value) {

    m_Value = value;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String matchMissingValuesTipText() {
    return "Missing values count as a match. This setting is independent of "
      + "the invertSelection option.";
  }

  /**
   * Gets whether missing values are counted as a match.
   * 
   * @return true if missing values are counted as a match.
   */
  public boolean getMatchMissingValues() {

    return m_MatchMissingValues;
  }

  /**
   * Sets whether missing values are counted as a match.
   * 
   * @param newMatchMissingValues true if missing values are counted as a match.
   */
  public void setMatchMissingValues(boolean newMatchMissingValues) {

    m_MatchMissingValues = newMatchMissingValues;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String invertSelectionTipText() {
    return "Invert matching sense.";
  }

  /**
   * Get whether the supplied columns are to be removed or kept
   * 
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {

    return !m_Values.getInvert();
  }

  /**
   * Set whether selected values should be removed or kept. If true the selected
   * values are kept and unselected values are deleted.
   * 
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {

    m_Values.setInvert(!invert);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String nominalIndicesTipText() {
    return "Range of label indices to be used for selection on nominal attribute. "
      + "First and last are valid indexes.";
  }

  /**
   * Get the set of nominal value indices that will be used for selection
   * 
   * @return rangeList a string representing the list of nominal indices.
   */
  public String getNominalIndices() {

    return m_Values.getRanges();
  }

  /**
   * Set which nominal labels are to be included in the selection.
   * 
   * @param rangeList a string representing the list of nominal indices. eg:
   *          first-3,5,6-last
   * @throws InvalidArgumentException if an invalid range list is supplied
   */
  public void setNominalIndices(String rangeList) {

    m_Values.setRanges(rangeList);
  }

  /**
   * Set whether to apply the filter to instances that arrive once the first
   * (training) batch has been seen. The default is to not apply the filter and
   * just return each instance input. This is so that, when used in the
   * FilteredClassifier, a test instance does not get "consumed" by the filter
   * and a prediction is always generated.
   * 
   * @param b true if the filter should *not* be applied to instances that
   *          arrive after the first (training) batch has been processed.
   */
  public void setDontFilterAfterFirstBatch(boolean b) {
    m_dontFilterAfterFirstBatch = b;
  }

  /**
   * Get whether to apply the filter to instances that arrive once the first
   * (training) batch has been seen. The default is to not apply the filter and
   * just return each instance input. This is so that, when used in the
   * FilteredClassifier, a test instance does not get "consumed" by the filter
   * and a prediction is always generated.
   * 
   * @return true if the filter should *not* be applied to instances that arrive
   *         after the first (training) batch has been processed.
   */
  public boolean getDontFilterAfterFirstBatch() {
    return m_dontFilterAfterFirstBatch;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String dontFilterAfterFirstBatchTipText() {
    return "Whether to apply the filtering process to instances that "
      + "are input after the first (training) batch. The default "
      + "is false so instances in subsequent batches can potentially "
      + "get 'consumed' by the filter.";
  }

  /**
   * Set which values of a nominal attribute are to be used for selection.
   * 
   * @param values an array containing indexes of values to be used for
   *          selection
   * @throws InvalidArgumentException if an invalid set of ranges is supplied
   */
  public void setNominalIndicesArr(int[] values) {

    String rangeList = "";
    for (int i = 0; i < values.length; i++) {
      if (i == 0) {
        rangeList = "" + (values[i] + 1);
      } else {
        rangeList += "," + (values[i] + 1);
      }
    }
    setNominalIndices(rangeList);
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
    runFilter(new RemoveWithValues(), argv);
  }
}
