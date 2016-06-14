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
 *    CSVSaver.java
 *    Copyright (C) 2004-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.AbstractInstance;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.SparseInstance;
import weka.core.Utils;

/**
 * <!-- globalinfo-start --> Writes to a destination that is in CSV
 * (comma-separated values) format. The column separator can be chosen (default
 * is ',') as well as the value representing missing values (default is '?').
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -F &lt;separator&gt;
 *  The field separator to be used.
 *  '\t' can be used as well.
 *  (default: ',')
 * </pre>
 * 
 * <pre>
 * -M &lt;str&gt;
 *  The string representing a missing value.
 *  (default: ?)
 * </pre>
 * 
 * <pre>
 * -N
 *  Don't write a header row.
 * </pre>
 * 
 * <pre>
 * -decimal &lt;num&gt;
 *  The maximum number of digits to print after the decimal
 *  place for numeric values (default: 6)
 * </pre>
 * 
 * <pre>
 * -i &lt;the input file&gt;
 *  The input file
 * </pre>
 * 
 * <pre>
 * -o &lt;the output file&gt;
 *  The output file
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 10203 $
 * @see Saver
 */
public class CSVSaver extends AbstractFileSaver implements BatchConverter,
  IncrementalConverter, FileSourcedConverter {

  /** for serialization. */
  static final long serialVersionUID = 476636654410701807L;

  /** the field separator. */
  protected String m_FieldSeparator = ",";

  /** The placeholder for missing values. */
  protected String m_MissingValue = "?";

  /** Max number of decimal places for numeric values */
  protected int m_MaxDecimalPlaces = AbstractInstance.s_numericAfterDecimalPoint;

  /** Set to true to not write the header row */
  protected boolean m_noHeaderRow = false;

  /**
   * Constructor.
   */
  public CSVSaver() {
    resetOptions();
  }

  /**
   * Returns a string describing this Saver.
   * 
   * @return a description of the Saver suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Writes to a destination that is in CSV (comma-separated values) format. "
      + "The column separator can be chosen (default is ',') as well as the value "
      + "representing missing values (default is '?').";
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tThe field separator to be used.\n"
      + "\t'\\t' can be used as well.\n" + "\t(default: ',')", "F", 1,
      "-F <separator>"));

    result.addElement(new Option("\tThe string representing a missing value.\n"
      + "\t(default: ?)", "M", 1, "-M <str>"));

    result.addElement(new Option("\tDon't write a header row.", "N", 0, "-N"));

    result.addElement(new Option(
      "\tThe maximum number of digits to print after the decimal\n"
        + "\tplace for numeric values (default: 6)", "decimal", 1,
      "-decimal <num>"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -F &lt;separator&gt;
   *  The field separator to be used.
   *  '\t' can be used as well.
   *  (default: ',')
   * </pre>
   * 
   * <pre>
   * -M &lt;str&gt;
   *  The string representing a missing value.
   *  (default: ?)
   * </pre>
   * 
   * <pre>
   * -N
   *  Don't write a header row.
   * </pre>
   * 
   * <pre>
   * -decimal &lt;num&gt;
   *  The maximum number of digits to print after the decimal
   *  place for numeric values (default: 6)
   * </pre>
   * 
   * <pre>
   * -i &lt;the input file&gt;
   *  The input file
   * </pre>
   * 
   * <pre>
   * -o &lt;the output file&gt;
   *  The output file
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    tmpStr = Utils.getOption('F', options);
    if (tmpStr.length() != 0) {
      setFieldSeparator(tmpStr);
    } else {
      setFieldSeparator(",");
    }

    tmpStr = Utils.getOption('M', options);
    if (tmpStr.length() != 0) {
      setMissingValue(tmpStr);
    } else {
      setMissingValue("?");
    }

    setNoHeaderRow(Utils.getFlag('N', options));

    tmpStr = Utils.getOption("decimal", options);
    if (tmpStr.length() > 0) {
      setMaxDecimalPlaces(Integer.parseInt(tmpStr));
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    result.add("-F");
    result.add(getFieldSeparator());

    result.add("-M");
    result.add(getMissingValue());

    if (getNoHeaderRow()) {
      result.add("-N");
    }

    result.add("-decimal");
    result.add("" + getMaxDecimalPlaces());

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String noHeaderRowTipText() {
    return "If true then the header row is not written";
  }

  /**
   * Set whether to not write the header row
   * 
   * @param b true if no header row is to be written
   */
  public void setNoHeaderRow(boolean b) {
    m_noHeaderRow = b;
  }

  /**
   * Get whether to not write the header row
   * 
   * @return true if no header row is to be written
   */
  public boolean getNoHeaderRow() {
    return m_noHeaderRow;
  }

  /**
   * Set the maximum number of decimal places to print
   * 
   * @param maxDecimal the maximum number of decimal places to print
   */
  public void setMaxDecimalPlaces(int maxDecimal) {
    m_MaxDecimalPlaces = maxDecimal;
  }

  /**
   * Get the maximum number of decimal places to print
   * 
   * @return the maximum number of decimal places to print
   */
  public int getMaxDecimalPlaces() {
    return m_MaxDecimalPlaces;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String maxDecimalPlacesTipText() {
    return "The maximum number of digits to print after the decimal "
      + "point for numeric values";
  }

  /**
   * Sets the character used as column separator.
   * 
   * @param value the character to use
   */
  public void setFieldSeparator(String value) {
    m_FieldSeparator = Utils.unbackQuoteChars(value);
    /*
     * if (m_FieldSeparator.length() != 1) { m_FieldSeparator = ","; System.err
     * .println(
     * "Field separator can only be a single character (exception being '\t'), "
     * + "defaulting back to '" + m_FieldSeparator + "'!"); }
     */
  }

  /**
   * Returns the character used as column separator.
   * 
   * @return the character to use
   */
  public String getFieldSeparator() {
    return Utils.backQuoteChars(m_FieldSeparator);
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String fieldSeparatorTipText() {
    return "The character to use as separator for the columns/fields (use '\\t' for TAB).";
  }

  /**
   * Sets the placeholder for missing values.
   * 
   * @param value the placeholder
   */
  public void setMissingValue(String value) {
    m_MissingValue = value;
  }

  /**
   * Returns the current placeholder for missing values.
   * 
   * @return the placeholder
   */
  public String getMissingValue() {
    return m_MissingValue;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String missingValueTipText() {
    return "The placeholder for missing values, default is '?'.";
  }

  /**
   * Returns a description of the file type.
   * 
   * @return a short file description
   */
  @Override
  public String getFileDescription() {
    return "CSV file: comma separated files";
  }

  /**
   * Resets the Saver.
   */
  @Override
  public void resetOptions() {
    super.resetOptions();

    setFileExtension(".csv");
  }

  /**
   * Returns the Capabilities of this saver.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.STRING_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.STRING_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Saves an instances incrementally. Structure has to be set by using the
   * setStructure() method or setInstances() method.
   * 
   * @param inst the instance to save
   * @throws IOException throws IOEXception if an instance cannot be saved
   *           incrementally.
   */
  @Override
  public void writeIncremental(Instance inst) throws IOException {

    int writeMode = getWriteMode();
    Instances structure = getInstances();
    PrintWriter outW = null;

    if (getRetrieval() == BATCH || getRetrieval() == NONE) {
      throw new IOException("Batch and incremental saving cannot be mixed.");
    }
    if (getWriter() != null) {
      outW = new PrintWriter(getWriter());
    }

    if (writeMode == WAIT) {
      if (structure == null) {
        setWriteMode(CANCEL);
        if (inst != null) {
          System.err
            .println("Structure(Header Information) has to be set in advance");
        }
      } else {
        setWriteMode(STRUCTURE_READY);
      }
      writeMode = getWriteMode();
    }
    if (writeMode == CANCEL) {
      if (outW != null) {
        outW.close();
      }
      cancel();
    }
    if (writeMode == STRUCTURE_READY) {
      setWriteMode(WRITE);
      // write header
      if (!getNoHeaderRow()) {
        if (retrieveFile() == null && outW == null) {
          // print out attribute names as first row
          for (int i = 0; i < structure.numAttributes(); i++) {
            System.out.print(structure.attribute(i).name());
            if (i < structure.numAttributes() - 1) {
              System.out.print(m_FieldSeparator);
            } else {
              System.out.println();
            }
          }
        } else {
          for (int i = 0; i < structure.numAttributes(); i++) {
            outW.print(structure.attribute(i).name());
            if (i < structure.numAttributes() - 1) {
              outW.print(m_FieldSeparator);
            } else {
              outW.println();
            }
          }
          outW.flush();
        }
      }
      writeMode = getWriteMode();
    }
    if (writeMode == WRITE) {
      if (structure == null) {
        throw new IOException("No instances information available.");
      }
      if (inst != null) {
        // write instance
        if (retrieveFile() == null && outW == null) {
          System.out.println(inst);
        } else {
          outW.println(instanceToString(inst));
          // flushes every 100 instances
          m_incrementalCounter++;
          if (m_incrementalCounter > 100) {
            m_incrementalCounter = 0;
            outW.flush();
          }
        }
      } else {
        // close
        if (outW != null) {
          outW.flush();
          outW.close();
        }
        m_incrementalCounter = 0;
        resetStructure();
        outW = null;
        resetWriter();
      }
    }
  }

  /**
   * Writes a Batch of instances.
   * 
   * @throws IOException throws IOException if saving in batch mode is not
   *           possible
   */
  @Override
  public void writeBatch() throws IOException {

    if (getInstances() == null) {
      throw new IOException("No instances to save");
    }
    if (getRetrieval() == INCREMENTAL) {
      throw new IOException("Batch and incremental saving cannot be mixed.");
    }
    setRetrieval(BATCH);
    setWriteMode(WRITE);
    if (retrieveFile() == null && getWriter() == null) {

      if (!getNoHeaderRow()) {
        // print out attribute names as first row
        for (int i = 0; i < getInstances().numAttributes(); i++) {
          System.out.print(getInstances().attribute(i).name());
          if (i < getInstances().numAttributes() - 1) {
            System.out.print(m_FieldSeparator);
          } else {
            System.out.println();
          }
        }
      }
      for (int i = 0; i < getInstances().numInstances(); i++) {
        System.out.println(instanceToString(getInstances().instance(i)));
      }
      setWriteMode(WAIT);
      return;
    }
    PrintWriter outW = new PrintWriter(getWriter());
    if (!getNoHeaderRow()) {
      // print out attribute names as first row
      for (int i = 0; i < getInstances().numAttributes(); i++) {
        outW.print(Utils.quote(getInstances().attribute(i).name()));
        if (i < getInstances().numAttributes() - 1) {
          outW.print(m_FieldSeparator);
        } else {
          outW.println();
        }
      }
    }
    for (int i = 0; i < getInstances().numInstances(); i++) {
      outW.println(instanceToString((getInstances().instance(i))));
    }
    outW.flush();
    outW.close();
    setWriteMode(WAIT);
    outW = null;
    resetWriter();
    setWriteMode(CANCEL);
  }

  /**
   * turns an instance into a string. takes care of sparse instances as well.
   * 
   * @param inst the instance to turn into a string
   * @return the generated string
   */
  protected String instanceToString(Instance inst) {
    StringBuffer result;
    Instance outInst;
    int i;
    String field;

    result = new StringBuffer();

    if (inst instanceof SparseInstance) {
      outInst = new DenseInstance(inst.weight(), inst.toDoubleArray());
      outInst.setDataset(inst.dataset());
    } else {
      outInst = inst;
    }

    for (i = 0; i < outInst.numAttributes(); i++) {
      if (i > 0) {
        result.append(m_FieldSeparator);
      }

      if (outInst.isMissing(i)) {
        field = m_MissingValue;
      } else {
        field = outInst.toString(i, m_MaxDecimalPlaces);
      }

      // make sure that custom field separators, like ";" get quoted correctly
      // as well (but only for single character field separators)
      if (m_FieldSeparator.length() == 1
        && (field.indexOf(m_FieldSeparator) > -1) && !field.startsWith("'")
        && !field.endsWith("'")) {
        field = "'" + field + "'";
      }

      result.append(field);
    }

    return result.toString();
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10203 $");
  }

  /**
   * Main method.
   * 
   * @param args should contain the options of a Saver.
   */
  public static void main(String[] args) {
    runFileSaver(new CSVSaver(), args);
  }
}
