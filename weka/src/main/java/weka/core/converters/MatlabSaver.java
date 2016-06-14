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
 * MatlabSaver.java
 * Copyright (C) 2009-2012 University of Waikato, Hamilton, NZ
 *
 */

package weka.core.converters;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.Version;

/**
 * <!-- globalinfo-start --> Writes Matlab ASCII files, in single or double
 * precision format.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
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
 * <pre>
 * -double
 *  Use double precision format.
 *  (default: single precision)
 * </pre>
 * 
 * <pre>
 * -tabs
 *  Use tabs as separator.
 *  (default: blanks)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 11211 $
 * @see Saver
 */
public class MatlabSaver extends AbstractFileSaver implements BatchConverter,
  IncrementalConverter {

  /** for serialization. */
  private static final long serialVersionUID = 4118356803697172614L;

  /** the file extension. */
  public static String FILE_EXTENSION = MatlabLoader.FILE_EXTENSION;

  /** whether to save in double instead of single precision format. */
  protected boolean m_UseDouble;

  /** whether to use tabs instead of blanks. */
  protected boolean m_UseTabs;

  /** whether the header was written already. */
  protected boolean m_HeaderWritten;

  /** for formatting the numbers. */
  protected DecimalFormat m_Format;

  /**
   * Constructor.
   */
  public MatlabSaver() {
    resetOptions();
  }

  /**
   * Returns a string describing this Saver.
   * 
   * @return a description of the Saver suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Writes Matlab ASCII files, in single or double precision format.";
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tUse double precision format.\n"
      + "\t(default: single precision)", "double", 0, "-double"));

    result.addElement(new Option("\tUse tabs as separator.\n"
      + "\t(default: blanks)", "tabs", 0, "-tabs"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * returns the options of the current setup.
   * 
   * @return the current options
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    if (getUseDouble()) {
      result.add("-double");
    }

    if (getUseTabs()) {
      result.add("-tabs");
    }

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Parses the options for this object.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
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
   * <pre>
   * -double
   *  Use double precision format.
   *  (default: single precision)
   * </pre>
   * 
   * <pre>
   * -tabs
   *  Use tabs as separator.
   *  (default: blanks)
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the options to use
   * @throws Exception if setting of options fails
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    setUseDouble(Utils.getFlag("double", options));

    setUseTabs(Utils.getFlag("tabs", options));

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Returns a description of the file type.
   * 
   * @return a short file description
   */
  @Override
  public String getFileDescription() {
    return "Matlab ASCII files";
  }

  /**
   * Resets the Saver.
   */
  @Override
  public void resetOptions() {
    super.resetOptions();

    setFileExtension(MatlabLoader.FILE_EXTENSION);
    setUseDouble(false);
    setUseTabs(false);

    m_HeaderWritten = false;
  }

  /**
   * Sets whether to use double or single precision.
   * 
   * @param value if true then double precision is used
   */
  public void setUseDouble(boolean value) {
    m_UseDouble = value;
    
    m_Format = (DecimalFormat) NumberFormat.getInstance(Locale.US);
    if (m_UseDouble) {
      
      //m_Format = new DecimalFormat(
      m_Format.applyPattern("   0.0000000000000000E00;  -0.0000000000000000E00");
    } else {
      // m_Format = new DecimalFormat("   0.00000000E00;  -0.00000000E00");
      m_Format.applyPattern("   0.00000000E00;  -0.00000000E00");
    }
  }

  /**
   * Returns whether double or single precision is used.
   * 
   * @return true if double precision is used
   */
  public boolean getUseDouble() {
    return m_UseDouble;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String useDoubleTipText() {
    return "Sets whether to use double instead of single precision.";
  }

  /**
   * Sets whether to use tabs instead of blanks.
   * 
   * @param value if true then tabs are used
   */
  public void setUseTabs(boolean value) {
    m_UseTabs = value;
  }

  /**
   * Returns whether tabs are used instead of blanks.
   * 
   * @return true if tabs are used
   */
  public boolean getUseTabs() {
    return m_UseTabs;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String useTabsTipText() {
    return "Sets whether to use tabs as separators instead of blanks.";
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
    result.enable(Capability.NUMERIC_ATTRIBUTES);

    // class
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Generates a comment header.
   * 
   * @return the header
   */
  protected String matlabHeader() {
    StringBuffer result;
    int i;

    result = new StringBuffer();
    result.append("% Relation: " + getInstances().relationName() + "\n");
    result.append("% Generated on: " + new Date() + "\n");
    result.append("% Generated by: WEKA " + Version.VERSION + "\n");
    result.append("%\n");

    result.append("%  ");
    for (i = 0; i < getInstances().numAttributes(); i++) {
      if (i > 0) {
        result.append((m_UseTabs ? "\t   " : "    "));
      }
      result.append(Utils.padRight(getInstances().attribute(i).name(),
        (m_UseDouble ? 16 : 8) + 5));
    }

    return result.toString();
  }

  /**
   * turns the instance into a Matlab row.
   * 
   * @param inst the instance to transform
   * @return the generated Matlab row
   */
  protected String instanceToMatlab(Instance inst) {
    StringBuffer result;
    int i;

    result = new StringBuffer();

    // attributes
    for (i = 0; i < inst.numAttributes(); i++) {
      if (i > 0) {
        result.append((m_UseTabs ? "\t" : " "));
      }
      result.append(m_Format.format(inst.value(i)));
    }

    return result.toString();
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

    if ((getRetrieval() == BATCH) || (getRetrieval() == NONE)) {
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
            .println("Structure (Header Information) has to be set in advance");
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

    // header
    if (writeMode == STRUCTURE_READY) {
      setWriteMode(WRITE);
      if ((retrieveFile() == null) && (outW == null)) {
        System.out.println(matlabHeader());
      } else {
        outW.println(matlabHeader());
      }
      writeMode = getWriteMode();
    }

    // row
    if (writeMode == WRITE) {
      if (structure == null) {
        throw new IOException("No instances information available.");
      }

      if (inst != null) {
        // write instance
        if ((retrieveFile() == null) && (outW == null)) {
          System.out.println(instanceToMatlab(inst));
        } else {
          outW.println(instanceToMatlab(inst));
          m_incrementalCounter++;
          // flush every 100 instances
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

    if ((retrieveFile() == null) && (getWriter() == null)) {
      System.out.println(matlabHeader());
      for (int i = 0; i < getInstances().numInstances(); i++) {
        System.out.println(instanceToMatlab(getInstances().instance(i)));
      }
      setWriteMode(WAIT);
    } else {
      PrintWriter outW = new PrintWriter(getWriter());
      outW.println(matlabHeader());
      for (int i = 0; i < getInstances().numInstances(); i++) {
        outW.println(instanceToMatlab(getInstances().instance(i)));
      }
      outW.flush();
      outW.close();
      setWriteMode(WAIT);
      outW = null;
      resetWriter();
      setWriteMode(CANCEL);
    }
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11211 $");
  }

  /**
   * Main method.
   * 
   * @param args should contain the options of a Saver.
   */
  public static void main(String[] args) {
    runFileSaver(new MatlabSaver(), args);
  }
}
