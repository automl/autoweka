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
 *    ReplaceMissingWithUserConstant.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
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
 <!-- globalinfo-start -->
 * Replaces all missing values for nominal, string, numeric and date attributes in the dataset with user-supplied constant values.
 * <p/>
 <!-- globalinfo-end -->
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 9197 $
 */
public class ReplaceMissingWithUserConstant extends PotentialClassIgnorer
    implements UnsupervisedFilter, StreamableFilter, EnvironmentHandler {

  /** For serialization */
  private static final long serialVersionUID = -7334039452189350356L;

  /** Environment variables */
  protected transient Environment m_env;

  /** Range of columns to consider */
  protected Range m_selectedRange;

  protected String m_range = "first-last";

  protected String m_resolvedRange = "";

  /** Constant for replacing missing values in nominal/string atts with */
  protected String m_nominalStringConstant = "";

  /** Replacement value for nominal/string atts after resolving environment vars */
  protected String m_resolvedNominalStringConstant = "";

  /** Constant for replacing missing values in numeric attributes with */
  protected String m_numericConstant = "0";

  /** Replacement value for numeric atts after resolving environment vars */
  protected String m_resolvedNumericConstant = "";

  /** Parsed numeric constant value */
  protected double m_numericConstVal = 0;

  /** Constant for replacing missing values in date attributes with */
  protected String m_dateConstant = "";

  /** Replacement value for date atts after resolving environment vars */
  protected String m_resolvedDateConstant = "";

  /** Parsed date value as a double */
  protected double m_dateConstVal = 0;

  /** Formatting string to use for parsing the date constant */
  protected String m_defaultDateFormat = "yyyy-MM-dd'T'HH:mm:ss";

  /** Formatting string after resolving environment vars */
  protected String m_resolvedDateFormat = "";

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {

    return "Replaces all missing values for nominal, string, numeric and date "
        + "attributes in the dataset with user-supplied constant values.";
  }

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

  @Override
  public Enumeration listOptions() {

    Vector<Option> opts = new Vector<Option>();

    opts.addElement(new Option(
        "\tSpecify list of attributes to replace missing values for "
            + "\n\t(as weka range list of indices or a comma separated list of attribute names).\n"
            + "\t(default: consider all attributes)", "R", 1,
        "-A <index1,index2-index4,... | att-name1,att-name2,...>"));

    opts.addElement(new Option(
        "\tSpecify the replacement constant for nominal/string attributes",
        "N", 1, "-N"));
    opts.addElement(new Option(
        "\tSpecify the replacement constant for numeric attributes"
            + "\n\t(default: 0)", "R", 1, "-R"));
    opts.addElement(new Option(
        "\tSpecify the replacement constant for date attributes", "D", 1, "-D"));
    opts.addElement(new Option(
        "\tSpecify the date format for parsing the replacement date constant"
            + "\n\t(default: yyyy-MM-dd'T'HH:mm:ss)", "F", 1, "-F"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      opts.addElement((Option) enu.nextElement());
    }
    return opts.elements();
  }

  /**
   * 
   * Parses a given list of options.
   * <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -A &lt;index1,index2-index4,... | att-name1,att-name2,...&gt;
   *  Specify list of attributes to replace missing values for 
   *  (as weka range list of indices or a comma separated list of attribute names).
   *  (default: consider all attributes)</pre>
   * 
   * <pre> -N
   *  Specify the replacement constant for nominal/string attributes</pre>
   * 
   * <pre> -R
   *  Specify the replacement constant for numeric attributes
   *  (default: 0)</pre>
   * 
   * <pre> -D
   *  Specify the replacement constant for date attributes</pre>
   * 
   * <pre> -F
   *  Specify the date format for parsing the replacement date constant
   *  (default: yyyy-MM-dd'T'HH:mm:ss)</pre>
   * 
   * <pre> -unset-class-temporarily
   *  Unsets the class index temporarily before the filter is
   *  applied to the data.
   *  (default: no)</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String atts = Utils.getOption('A', options);
    if (atts.length() > 0) {
      setAttributes(atts);
    }

    String nomString = Utils.getOption('N', options);
    if (nomString.length() > 0) {
      setNominalStringReplacementValue(nomString);
    }
    String numString = Utils.getOption('R', options);
    if (numString.length() > 0) {
      setNumericReplacementValue(numString);
    }
    String dateString = Utils.getOption('D', options);
    if (dateString.length() > 0) {
      setDateReplacementValue(dateString);
    }
    String formatString = Utils.getOption('F', options);
    if (formatString.length() > 0) {
      setDateFormat(formatString);
    }
  }

  @Override
  public String[] getOptions() {
    ArrayList<String> options = new ArrayList<String>();

    if (getAttributes().length() > 0) {
      options.add("-A");
      options.add(getAttributes());
    }

    if (getNominalStringReplacementValue().length() > 0) {
      options.add("-N");
      options.add(getNominalStringReplacementValue());
    }

    if (getNumericReplacementValue().length() > 0) {
      options.add("-R");
      options.add(getNumericReplacementValue());
    }

    if (getDateReplacementValue().length() > 0) {
      options.add("-D");
      options.add(getDateReplacementValue());
    }

    if (getDateFormat().length() > 0) {
      options.add("-F");
      options.add(getDateFormat());
    }

    return options.toArray(new String[1]);
  }

  /**
   * Tip text for this property suitable for displaying in the GUI.
   * 
   * @return the tip text for this property.
   */
  public String attributesTipText() {
    return "Specify range of attributes to act on."
        + " This is a comma separated list of attribute indices, with"
        + " \"first\" and \"last\" valid values. Specify an inclusive"
        + " range with \"-\". E.g: \"first-3,5,6-10,last\". Can alternatively"
        + " specify a comma separated list of attribute names. Note that "
        + " you can't mix indices and attribute names in the same list";
  }

  /**
   * Set the list of attributes to consider for replacing missing values
   * 
   * @param range the list of attributes to consider
   */
  public void setAttributes(String range) {
    m_range = range;
  }

  /**
   * Get the list of attributes to consider for replacing missing values
   * 
   * @return the list of attributes to consider
   */
  public String getAttributes() {
    return m_range;
  }

  /**
   * Tip text for this property suitable for displaying in the GUI.
   * 
   * @return the tip text for this property.
   */
  public String nominalStringReplacementValueTipText() {
    return "The constant to replace missing values in nominal/string attributes with";
  }

  /**
   * Get the nominal/string replacement value
   * 
   * @return the nominal/string replacement value
   */
  public String getNominalStringReplacementValue() {
    return m_nominalStringConstant;
  }

  /**
   * Set the nominal/string replacement value
   * 
   * @param m_nominalStringConstant the nominal/string constant to use
   */
  public void setNominalStringReplacementValue(String nominalStringConstant) {
    m_nominalStringConstant = nominalStringConstant;
  }

  /**
   * Tip text for this property suitable for displaying in the GUI.
   * 
   * @return the tip text for this property.
   */
  public String numericReplacementValueTipText() {
    return "The constant to replace missing values in numeric attributes with";
  }

  /**
   * Get the numeric replacement value
   * 
   * @return the numeric replacement value
   */
  public String getNumericReplacementValue() {
    return m_numericConstant;
  }

  /**
   * Set the numeric replacement value
   * 
   * @param numericConstant the numeric replacement value
   */
  public void setNumericReplacementValue(String numericConstant) {
    m_numericConstant = numericConstant;
  }

  /**
   * Tip text for this property suitable for displaying in the GUI.
   * 
   * @return the tip text for this property.
   */
  public String dateReplacementValueTipText() {
    return "The constant to replace missing values in date attributes with";
  }

  /**
   * Set the date replacement value
   * 
   * @param dateConstant the date replacement value
   */
  public void setDateReplacementValue(String dateConstant) {
    m_dateConstant = dateConstant;
  }

  /**
   * Get the date replacement value
   * 
   * @return the date replacement value
   */
  public String getDateReplacementValue() {
    return m_dateConstant;
  }

  /**
   * Tip text for this property suitable for displaying in the GUI.
   * 
   * @return the tip text for this property.
   */
  public String dateFormatTipText() {
    return "The formatting string to use for parsing the date replacement value";
  }

  /**
   * Set the date format to use for parsing the date replacement constant
   * 
   * @param dateFormat the date format to use
   */
  public void setDateFormat(String dateFormat) {
    m_defaultDateFormat = dateFormat;
  }

  /**
   * Get the date format to use for parsing the date replacement constant
   * 
   * @return the date format to use
   */
  public String getDateFormat() {
    return m_defaultDateFormat;
  }

  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);

    m_resolvedNominalStringConstant = m_nominalStringConstant;
    m_resolvedNumericConstant = m_numericConstant;
    m_resolvedDateConstant = m_dateConstant;
    m_resolvedDateFormat = m_defaultDateFormat;
    m_resolvedRange = m_range;

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    try {
      if (m_resolvedNominalStringConstant != null
          && m_resolvedNominalStringConstant.length() > 0) {
        m_resolvedNominalStringConstant = m_env
            .substitute(m_resolvedNominalStringConstant);
      }

      if (m_resolvedNumericConstant != null
          && m_resolvedNumericConstant.length() > 0) {
        m_resolvedNumericConstant = m_env.substitute(m_resolvedNumericConstant);
      }

      if (m_resolvedDateConstant != null && m_resolvedDateConstant.length() > 0) {
        m_resolvedDateConstant = m_env.substitute(m_resolvedDateConstant);
      }

      if (m_resolvedDateFormat != null && m_resolvedDateFormat.length() > 0) {
        m_resolvedDateFormat = m_env.substitute(m_resolvedDateFormat);
      }

      if (m_resolvedRange != null && m_resolvedRange.length() > 0) {
        m_resolvedRange = m_env.substitute(m_resolvedRange);
      }
    } catch (Exception ex) {
    }

    // try and set up a Range first directly from the supplied string
    m_selectedRange = new Range(m_resolvedRange);
    try {
      m_selectedRange.setUpper(instanceInfo.numAttributes() - 1);
    } catch (IllegalArgumentException e) {
      // now try as a list of named attributes
      String[] parts = m_resolvedRange.split(",");
      if (parts.length == 0) {
        throw new Exception(
            "Must specify which attributes to replace missing values for!");
      }

      StringBuffer indexList = new StringBuffer();
      for (String att : parts) {
        att = att.trim();
        Attribute a = instanceInfo.attribute(att);
        if (a == null) {
          throw new Exception("I can't find the requested attribute '" + att
              + "' in the incoming instances.");
        }
        indexList.append(",").append(a.index() + 1);
      }
      String result = indexList.toString();
      result = result.substring(1, result.length());
      m_selectedRange = new Range(result);
      m_selectedRange.setUpper(instanceInfo.numAttributes() - 1);
    }

    boolean hasNominal = false;
    boolean hasString = false;
    boolean hasNumeric = false;
    boolean hasDate = false;

    for (int i = 0; i < instanceInfo.numAttributes(); i++) {
      if (m_selectedRange.isInRange(i)) {
        if (instanceInfo.attribute(i).isNominal()) {
          hasNominal = true;
        } else if (instanceInfo.attribute(i).isString()) {
          hasString = true;
        } else if (instanceInfo.attribute(i).isNumeric()) {
          hasNumeric = true;
        } else if (instanceInfo.attribute(i).isDate()) {
          hasDate = true;
        }
      }
    }

    if (hasNominal || hasString) {
      if (m_resolvedNominalStringConstant == null
          || m_resolvedNominalStringConstant.length() == 0) {
        if (m_resolvedNumericConstant != null
            && m_resolvedNumericConstant.length() > 0) {
          // use the supplied numeric constant as a nominal value
          m_resolvedNominalStringConstant = "" + m_resolvedNumericConstant;
        } else {
          throw new Exception("Data contains nominal/string attributes and no "
              + "replacement constant has been supplied");
        }
      }
    }

    if (hasNumeric
        && (m_numericConstant == null || m_numericConstant.length() == 0)) {
      if (m_resolvedNominalStringConstant != null
          && m_resolvedNominalStringConstant.length() > 0) {
        // use the supplied nominal constant as numeric replacement
        // value (if we can parse it as a number)
        try {
          Double.parseDouble(m_resolvedNominalStringConstant);
          m_resolvedNumericConstant = m_resolvedNominalStringConstant;
        } catch (NumberFormatException e) {
          throw new Exception(
              "Data contains numeric attributes and no numeric "
                  + "constant has been supplied. Unable to parse nominal "
                  + "constant as a number either.");
        }
      } else {
        throw new Exception("Data contains numeric attributes and no "
            + "replacement constant has been supplied");
      }

      try {
        m_numericConstVal = Double.parseDouble(m_resolvedNumericConstant);
      } catch (NumberFormatException e) {
        throw new Exception("Unable to parse numeric constant");
      }
    }

    if (hasDate) {
      if (m_resolvedDateConstant == null
          || m_resolvedDateConstant.length() == 0) {
        throw new Exception(
            "Data contains date attributes and no replacement constant has been "
                + "supplied");
      }

      SimpleDateFormat sdf = new SimpleDateFormat(m_resolvedDateFormat);
      Date d = sdf.parse(m_resolvedDateConstant);
      m_dateConstVal = d.getTime();
    }

    Instances outputFormat = new Instances(instanceInfo, 0);

    // check for nominal attributes and add the supplied constant to the
    // list of legal values (if necessary)
    ArrayList<Attribute> updatedNoms = new ArrayList<Attribute>();
    for (int i = 0; i < instanceInfo.numAttributes(); i++) {
      if (i != instanceInfo.classIndex() && m_selectedRange.isInRange(i)) {
        Attribute temp = instanceInfo.attribute(i);
        if (temp.isNominal()) {
          if (temp.indexOfValue(m_resolvedNominalStringConstant) < 0) {
            List<String> values = new ArrayList<String>();
            for (int j = 0; j < temp.numValues(); j++) {
              values.add(temp.value(j));
            }
            values.add(m_resolvedNominalStringConstant);
            Attribute newAtt = new Attribute(temp.name(), values);
            newAtt.setWeight(temp.weight());
            updatedNoms.add(newAtt);
          }
        }
      }
    }

    if (updatedNoms.size() > 0) {
      int nomCount = 0;
      ArrayList<Attribute> atts = new ArrayList<Attribute>();

      for (int i = 0; i < instanceInfo.numAttributes(); i++) {
        if (i != instanceInfo.classIndex() && m_selectedRange.isInRange(i)) {
          if (instanceInfo.attribute(i).isNominal()) {
            atts.add(updatedNoms.get(nomCount++));
          } else {
            atts.add((Attribute) instanceInfo.attribute(i).copy());
          }
        } else {
          atts.add((Attribute) instanceInfo.attribute(i).copy());
        }
      }

      outputFormat = new Instances(instanceInfo.relationName(), atts, 0);
      outputFormat.setClassIndex(getInputFormat().classIndex());
    }

    setOutputFormat(outputFormat);

    return true;
  }

  @Override
  public boolean input(Instance inst) throws Exception {
    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    double[] vals = new double[inst.numAttributes()];

    for (int i = 0; i < inst.numAttributes(); i++) {
      if (inst.isMissing(i) && m_selectedRange.isInRange(i)) {
        if (i != inst.classIndex()) {
          if (inst.attribute(i).isNumeric()) {
            vals[i] = m_numericConstVal;
          } else if (inst.attribute(i).isNominal()) {
            // vals[i] = inst.attribute(i).numValues();
            int temp = inst.attribute(i).indexOfValue(
                m_resolvedNominalStringConstant);
            vals[i] = (temp >= 0) ? temp : inst.attribute(i).numValues();
          } else if (inst.attribute(i).isString()) {
            // a bit of a hack here to try and detect if we're running in
            // streaming
            // mode or batch mode. If the string attribute has only one value in
            // the
            // header then it is likely that we're running in streaming mode
            // (where only one
            // value, the current instance's value, is maintained in memory)
            if (inst.attribute(i).numValues() <= 1) {
              outputFormatPeek().attribute(i).setStringValue(
                  m_resolvedNominalStringConstant);
              vals[i] = 0;
            } else {
              vals[i] = outputFormatPeek().attribute(i).addStringValue(
                  m_resolvedNominalStringConstant);
            }
          } else if (inst.attribute(i).isDate()) {
            vals[i] = m_dateConstVal;
          } else {
            vals[i] = inst.value(i);
          }
        } else {
          vals[i] = inst.value(i);
        }
      } else {
        if (inst.attribute(i).isString()) {
          // a bit of a hack here to try and detect if we're running in
          // streaming
          // mode or batch mode. If the string attribute has only one value in
          // the
          // header then it is likely that we're running in streaming mode
          // (where only one
          // value, the current instance's value, is maintained in memory)
          if (inst.attribute(i).numValues() <= 1) {
            outputFormatPeek().attribute(i).setStringValue(inst.stringValue(i));
          } else {
            outputFormatPeek().attribute(i).addStringValue(inst.stringValue(i));
          }
          vals[i] = outputFormatPeek().attribute(i).indexOfValue(
              inst.stringValue(i));
        } else {
          vals[i] = inst.value(i);
        }
      }
    }

    Instance newInst = null;
    if (inst instanceof SparseInstance) {
      newInst = new SparseInstance(inst.weight(), vals);
    } else {
      newInst = new DenseInstance(inst.weight(), vals);
    }

    newInst.setDataset(getOutputFormat());
    /*
     * copyValues(newInst, false, inst.dataset(), getOutputFormat());
     * newInst.setDataset(getOutputFormat());
     */
    push(newInst);

    return true;
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 9197 $");
  }

  /**
   * Main method for testing this class.
   * 
   * @param args should contain arguments to the filter: use -h for help
   */
  public static void main(String[] args) {
    runFilter(new ReplaceMissingWithUserConstant(), args);
  }
}

