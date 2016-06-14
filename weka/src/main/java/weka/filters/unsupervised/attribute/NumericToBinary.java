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
 *    NumericToBinary.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Range;
import weka.core.RevisionUtils;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.filters.StreamableFilter;
import weka.filters.UnsupervisedFilter;

/**
 * <!-- globalinfo-start --> Converts all numeric attributes into binary
 * attributes (apart from the class attribute, if set): if the value of the
 * numeric attribute is exactly zero, the value of the new attribute will be
 * zero. If the value of the numeric attribute is missing, the value of the new
 * attribute will be missing. Otherwise, the value of the new attribute will be
 * one. The new attributes will be nominal.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -unset-class-temporarily
 *  Unsets the class index temporarily before the filter is
 *  applied to the data.
 *  (default: no)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 12037 $
 */
public class NumericToBinary extends PotentialClassIgnorer implements
  UnsupervisedFilter, StreamableFilter {
	
  /** Stores which columns to turn into binary */
  protected Range m_Cols = new Range("first-last");

  /** The default columns to turn into binary */
  protected String m_DefaultCols = "first-last";

  /** for serialization */
  static final long serialVersionUID = 2616879323359470802L;

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {

    return "Converts all numeric attributes into binary attributes (apart from "
      + "the class attribute, if set): if the value of the numeric attribute is "
      + "exactly zero, the value of the new attribute will be zero. If the "
      + "value of the numeric attribute is missing, the value of the new "
      + "attribute will be missing. Otherwise, the value of the new "
      + "attribute will be one. The new attributes will be nominal.";
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
   * Gets an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>(2);

    result.addElement(new Option(
      "\tSpecifies list of columns to binarize. First"
        + " and last are valid indexes.\n" + "\t(default: first-last)", "R", 1,
      "-R <col1,col2-col4,...>"));

    result.addElement(new Option("\tInvert matching sense of column indexes.",
      "V", 0, "-V"));

    return result.elements();
  }
  
  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -R &lt;col1,col2-col4,...&gt;
   *  Specifies list of columns to binarize. First and last are valid indexes.
   *  (default: first-last)
   * </pre>
   * 
   * <pre>
   * -V
   *  Invert matching sense of column indexes.
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    setInvertSelection(Utils.getFlag('V', options));

    String tmpStr = Utils.getOption('R', options);
    if (tmpStr.length() != 0) {
      setAttributeIndices(tmpStr);
    } else {
      setAttributeIndices(m_DefaultCols);
    }

    if (getInputFormat() != null) {
      setInputFormat(getInputFormat());
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }
  
  @Override
  public String[] getOptions() {
	  
    Vector<String> result = new Vector<String>();
	
    if ( !getAttributeIndices().equals("") ) {
        result.add("-R");
        result.add(getAttributeIndices());
    }
    
    if( getInvertSelection() ) {
    	result.add("-V");
    }
    
    return result.toArray( new String[result.size()] );
	
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String invertSelectionTipText() {
    return "Set attribute selection mode. If false, only selected"
      + " (numeric) attributes in the range will be 'binarized'; if"
      + " true, only non-selected attributes will be 'binarized'.";
  }

  /**
   * Gets whether the supplied columns are to be worked on or the others.
   * 
   * @return true if the supplied columns will be worked on
   */
  public boolean getInvertSelection() {
    return m_Cols.getInvert();
  }

  /**
   * Sets whether selected columns should be worked on or all the others apart
   * from these. If true all the other columns are considered for
   * "binarization".
   * 
   * @param value the new invert setting
   */
  public void setInvertSelection(boolean value) {
    m_Cols.setInvert(value);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String attributeIndicesTipText() {
    return "Specify range of attributes to act on."
      + " This is a comma separated list of attribute indices, with"
      + " \"first\" and \"last\" valid values. Specify an inclusive"
      + " range with \"-\". E.g: \"first-3,5,6-10,last\".";
  }

  /**
   * Gets the current range selection
   * 
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {
    return m_Cols.getRanges();
  }

  /**
   * Sets which attributes are to be "binarized" (only numeric attributes
   * among the selection will be transformed).
   * 
   * @param value a string representing the list of attributes. Since the string
   *          will typically come from a user, attributes are indexed from 1. <br>
   *          eg: first-3,5,6-last
   * @throws IllegalArgumentException if an invalid range list is supplied
   */
  public void setAttributeIndices(String value) {
    m_Cols.setRanges(value);
  }

  /**
   * Sets which attributes are to be transformed to binary. (only numeric
   * attributes among the selection will be transformed).
   * 
   * @param value an array containing indexes of attributes to binarize. Since
   *          the array will typically come from a program, attributes are
   *          indexed from 0.
   * @throws IllegalArgumentException if an invalid set of ranges is supplied
   */
  public void setAttributeIndicesArray(int[] value) {
    setAttributeIndices(Range.indicesToRangeList(value));
  }
  
  

  /**
   * Sets the format of the input instances.
   * 
   * @param instanceInfo an Instances object containing the input instance
   *          structure (any instances contained in the object are ignored -
   *          only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the input format can't be set successfully
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);
    setOutputFormat();
    return true;
  }

  /**
   * Input an instance for filtering.
   * 
   * @param instance the input instance
   * @return true if the filtered instance may now be collected with output().
   * @throws IllegalStateException if no input format has been defined.
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
    convertInstance(instance);
    return true;
  }

  /**
   * Set the output format.
   */
  private void setOutputFormat() {

    ArrayList<Attribute> newAtts;
    int newClassIndex;
    StringBuffer attributeName;
    Instances outputFormat;
    ArrayList<String> vals;
    
    m_Cols.setUpper(getInputFormat().numAttributes() - 1);

    // Compute new attributes
    newClassIndex = getInputFormat().classIndex();
    newAtts = new ArrayList<Attribute>();
    for (int j = 0; j < getInputFormat().numAttributes(); j++) {
      Attribute att = getInputFormat().attribute(j);
      if ((j == newClassIndex) || (!att.isNumeric()) || !m_Cols.isInRange(j) ) {
        newAtts.add((Attribute) att.copy());
      } else {
        attributeName = new StringBuffer(att.name() + "_binarized");
        vals = new ArrayList<String>(2);
        vals.add("0");
        vals.add("1");
        newAtts.add(new Attribute(attributeName.toString(), vals));
      }
    }
    outputFormat = new Instances(getInputFormat().relationName(), newAtts, 0);
    outputFormat.setClassIndex(newClassIndex);
    setOutputFormat(outputFormat);
  }

  /**
   * Convert a single instance over. The converted instance is added to the end
   * of the output queue.
   * 
   * @param instance the instance to convert
   */
  private void convertInstance(Instance instance) {

    Instance inst = null;
    if (instance instanceof SparseInstance) {
      double[] vals = new double[instance.numValues()];
      int[] newIndices = new int[instance.numValues()];
      for (int j = 0; j < instance.numValues(); j++) {
    	
        Attribute att = getInputFormat().attribute(instance.index(j));
        if ((!att.isNumeric())
          || (instance.index(j) == getInputFormat().classIndex())
          || !m_Cols.isInRange(instance.index(j))) {
          vals[j] = instance.valueSparse(j);
        } else {
          if (instance.isMissingSparse(j)) {
            vals[j] = instance.valueSparse(j);
          } else {
            vals[j] = 1;
          }
        }
        newIndices[j] = instance.index(j);
      }
      inst = new SparseInstance(instance.weight(), vals, newIndices,
        outputFormatPeek().numAttributes());
    } else {
      double[] vals = new double[outputFormatPeek().numAttributes()];
      for (int j = 0; j < getInputFormat().numAttributes(); j++) {
        Attribute att = getInputFormat().attribute(j);
        if ((!att.isNumeric()) 
          || (j == getInputFormat().classIndex())
          || !m_Cols.isInRange(j)) {
          vals[j] = instance.value(j);
        } else {
          if (instance.isMissing(j) || (instance.value(j) == 0)) {
            vals[j] = instance.value(j);
          } else {
            vals[j] = 1;
          }
        }
      }
      inst = new DenseInstance(instance.weight(), vals);
    }
    inst.setDataset(instance.dataset());
    push(inst, false); // No need to copy
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
    runFilter(new NumericToBinary(), argv);
  }
}
