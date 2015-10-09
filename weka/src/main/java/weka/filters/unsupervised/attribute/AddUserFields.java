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
 *    AddUserFields.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import java.io.Serializable;
import java.text.ParseException;
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
import weka.core.OptionHandler;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.filters.Filter;

/**
 * 
 <!-- options-start --> 
 Valid options are:
 * <p/>
 * 
 * <pre>
 * -A &lt;name:type:value&gt;
 *  New field specification (name&#64;type&#64;value).
 *   Environment variables may be used for any/all parts of the
 *  specification. Type can be one of (numeric, nominal, string or date).
 *  The value for date be a specific date string or the special string
 *  "now" to indicate the current date-time. A specific date format
 *  string for parsing specific date values can be specified by suffixing
 *  the type specification - e.g. "myTime&#64;date:MM-dd-yyyy&#64;08-23-2009".
 *  This option may be specified multiple times
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 9002 $
 */
public class AddUserFields extends Filter implements OptionHandler,
    EnvironmentHandler {

  /** For serialization */
  private static final long serialVersionUID = -2761427344847891585L;

  /** The new attributes to create */
  protected List<AttributeSpec> m_attributeSpecs;

  protected transient Environment m_env;

  /**
   * Inner class encapsulating a new user-specified attribute to create.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class AttributeSpec implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = -617328946241474608L;

    /** The name of the new attribute */
    protected String m_name = "";

    /** The constant value it should assume */
    protected String m_value = "";

    /** The type of the new attribute */
    protected String m_type = "";

    /** The name after resolving any environment variables */
    protected String m_nameS;

    /** The value after resolving any environment variables */
    protected String m_valueS;

    /** The type after resolving any environment variables */
    protected String m_typeS;

    /** The date format to use (if the new attribute is a date) */
    protected SimpleDateFormat m_dateFormat;

    /** Holds the parsed date value */
    protected Date m_parsedDate;

    /**
     * Default constructor
     */
    public AttributeSpec() {
    }

    /**
     * Constructor that takes an attribute specification in internal format
     * 
     * @param spec the attribute spec to use
     */
    public AttributeSpec(String spec) {
      parseFromInternal(spec);
    }

    /**
     * Set the name of the new attribute
     * 
     * @param name the name of the new attribute
     */
    public void setName(String name) {
      m_name = name;
    }

    /**
     * Get the name of the new attribute
     * 
     * @return the name of the new attribute
     */
    public String getName() {
      return m_name;
    }

    /**
     * Set the type of the new attribute
     * 
     * @param type the type of the new attribute
     */
    public void setType(String type) {
      m_type = type;
    }

    /**
     * Get the type of the new attribute
     * 
     * @return the type of the new attribute
     */
    public String getType() {
      return m_type;
    }

    /**
     * Set the value of the new attribute. Date attributes can assume a supplied
     * date value (parseable by either the default date format or a user
     * specified one) or the current time stamp if the user specifies the
     * special string "now".
     * 
     * @param value the value of the new attribute
     */
    public void setValue(String value) {
      m_value = value;
    }

    /**
     * Get the value of the new attribute. Date attributes can assume a supplied
     * date value (parseable by either the default date format or a user
     * specified one) or the current time stamp if the user specifies the
     * special string "now".
     * 
     * @return the value of the new attribute
     */
    public String getValue() {
      return m_value;
    }

    /**
     * Get the name of the attribute after substituting any environment
     * variables
     * 
     * @return the name of the attribute after environment variables have been
     *         substituted
     */
    public String getResolvedName() {
      return m_nameS;
    }

    /**
     * Get the value of the attribute after substituting any environment
     * variables
     * 
     * @return the value of the attribute after environment variables have been
     *         substituted
     */
    public String getResolvedValue() {
      return m_valueS;
    }

    /**
     * Get the type of the attribute after substituting any environment
     * variables
     * 
     * @return the tyep of the attribute after environment variables have been
     *         substituted
     */
    public String getResolvedType() {
      return m_typeS;
    }

    /**
     * Get the date formatting string (if any)
     * 
     * @return the date formatting string
     */
    public String getDateFormat() {
      if (m_dateFormat != null) {
        return m_dateFormat.toPattern();
      } else {
        return null;
      }
    }

    /**
     * Get the value of the attribute as a date or null if the attribute isn't
     * of type date.
     * 
     * @return the value as a date
     */
    public Date getDateValue() {
      if (m_parsedDate != null) {
        return m_parsedDate;
      }

      if (getResolvedType().toLowerCase().startsWith("date")) {
        return new Date(); // now
      }

      return null; // not a date attribute
    }

    /**
     * Get the value of the attribute as a number or Utils.missingValue() if the
     * attribute is not numeric.
     * 
     * @return the value of the attribute as a number
     */
    public double getNumericValue() {
      if (getResolvedType().toLowerCase().startsWith("numeric")) {
        return Double.parseDouble(getResolvedValue());
      }

      return Utils.missingValue(); // not a numeric attribute
    }

    /**
     * Get the value of the attribute as a string (nominal and string attribute)
     * or null if the attribute is not nominal or string
     * 
     * @return the value of the attribute as a string
     */
    public String getNominalOrStringValue() {
      if (getResolvedType().toLowerCase().startsWith("nominal")
          || getResolvedType().toLowerCase().startsWith("string")) {
        return getResolvedValue();
      }

      return null; // not a nominal or string attribute
    }

    protected void parseFromInternal(String spec) {
      String[] parts = spec.split("@");

      if (parts.length > 0) {
        m_name = parts[0].trim();
      }
      if (parts.length > 1) {
        m_type = parts[1].trim();
      }
      if (parts.length > 2) {
        m_value = parts[2].trim();
      }
    }

    /**
     * Initialize this attribute spec by resolving any environment variables and
     * setting up the date format (if necessary)
     * 
     * @param env environment variables to use
     */
    public void init(Environment env) {
      m_nameS = m_name;
      m_typeS = m_type;
      m_valueS = m_value;

      try {
        m_nameS = env.substitute(m_nameS);
        m_typeS = env.substitute(m_typeS);
        m_valueS = env.substitute(m_valueS);
      } catch (Exception ex) {
      }

      if (m_typeS.toLowerCase().startsWith("date") && m_typeS.indexOf(":") > 0) {
        String format = m_typeS.substring(m_typeS.indexOf(":") + 1,
            m_typeS.length());
        m_dateFormat = new SimpleDateFormat(format);
        if (!m_valueS.toLowerCase().equals("now")) {
          try {
            m_parsedDate = m_dateFormat.parse(m_valueS);
          } catch (ParseException e) {
            throw new IllegalArgumentException("Date value \"" + m_valueS
                + " \" can't be parsed with formatting string \"" + format
                + "\"");
          }
        }
      }
    }

    /**
     * Return a nicely formatted string for display
     * 
     * @return a textual description
     */
    @Override
    public String toString() {
      StringBuffer buff = new StringBuffer();

      buff.append("Name: ").append(m_name).append(" ");
      String type = m_type;
      if (type.toLowerCase().startsWith("date") && type.indexOf(":") > 0) {
        type = type.substring(0, type.indexOf(":"));
        String format = m_type.substring(m_type.indexOf(":" + 1,
            m_type.length()));
        buff.append("Type: ").append(type).append(" [").append(format)
            .append("] ");
      } else {
        buff.append("Type: ").append(type).append(" ");
      }
      buff.append("Value: ").append(m_value);

      return buff.toString();
    }

    public String toStringInternal() {
      StringBuffer buff = new StringBuffer();

      buff.append(m_name).append("@").append(m_type).append("@")
          .append(m_value);

      return buff.toString();
    }
  }

  /**
   * Constructs a new AddUserFields
   */
  public AddUserFields() {
    m_attributeSpecs = new ArrayList<AttributeSpec>();
  }

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "A filter that adds new attributes with user specified type and constant value. "
        + "Numeric, nominal, string and date attributes can be created. "
        + "Attribute name, and value can be set with environment variables. Date "
        + "attributes can also specify a formatting string by which to parse "
        + "the supplied date value. Alternatively, a current time stamp can "
        + "be specified by supplying the special string \"now\" as the value "
        + "for a date attribute.";
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
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Clear the list of attribute specifications
   */
  public void clearAttributeSpecs() {
    if (m_attributeSpecs == null) {
      m_attributeSpecs = new ArrayList<AttributeSpec>();
    }
    m_attributeSpecs.clear();
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration listOptions() {
    Vector<Option> newVector = new Vector<Option>();

    newVector
        .addElement(new Option(
            "\tNew field specification (name@type@value).\n"
                + "\t Environment variables may be used for any/all parts of the\n"
                + "\tspecification. Type can be one of (numeric, nominal, string or date).\n"
                + "\tThe value for date be a specific date string or the special string\n"
                + "\t\"now\" to indicate the current date-time. A specific date format\n"
                + "\tstring for parsing specific date values can be specified by suffixing\n"
                + "\tthe type specification - e.g. \"myTime@date:MM-dd-yyyy@08-23-2009\"."
                + "This option may be specified multiple times", "A", 1,
            "-A <name:type:value>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> 
   Valid options are:
   * <p/>
   * 
   * <pre>
   * -A &lt;name:type:value&gt;
   *  New field specification (name&#64;type&#64;value).
   *   Environment variables may be used for any/all parts of the
   *  specification. Type can be one of (numeric, nominal, string or date).
   *  The value for date be a specific date string or the special string
   *  "now" to indicate the current date-time. A specific date format
   *  string for parsing specific date values can be specified by suffixing
   *  the type specification - e.g. "myTime&#64;date:MM-dd-yyyy&#64;08-23-2009".
   *  This option may be specified multiple times
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param otions the list of options as an array of string
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    clearAttributeSpecs();

    String attS = "";
    while ((attS = Utils.getOption('A', options)).length() > 0) {
      addAttributeSpec(attS);
    }

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the filter
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    ArrayList<String> options = new ArrayList<String>();

    for (int i = 0; i < m_attributeSpecs.size(); i++) {
      options.add("-A");
      options.add(m_attributeSpecs.get(i).toStringInternal());
    }

    if (options.size() == 0) {
      return new String[0];
    }

    return options.toArray(new String[1]);
  }

  /**
   * Add an attribute spec to the list
   * 
   * @param spec the attribute spec to add
   */
  public void addAttributeSpec(String spec) {
    AttributeSpec newSpec = new AttributeSpec(spec);
    m_attributeSpecs.add(newSpec);
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String attributeSpecsTipText() {
    return "Specifications of the new attributes to create";
  }

  /**
   * Set the list of attribute specs to use to create the new attributes.
   * 
   * @param specs the list of attribute specs to use
   */
  public void setAttributeSpecs(List<AttributeSpec> specs) {
    m_attributeSpecs = specs;
  }

  /**
   * Get the list of attribute specs to use to create the new attributes.
   * 
   * @return the list of attribute specs to use
   */
  public List<AttributeSpec> getAttributeSpecs() {
    return m_attributeSpecs;
  }

  /**
   * Set environment varialbes to use
   * 
   * @param the environment variables to use
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
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
   * Input an instance for filtering. Ordinarily the instance is processed and
   * made available for output immediately. Some filters require all instances
   * be read before producing output.
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

    if (outputFormatPeek() == null) {
      setOutputFormat();
    }

    Instance inst = (Instance) instance.copy();

    // First copy string values from input to output
    copyValues(inst, true, inst.dataset(), getOutputFormat());

    convertInstance(inst);
    return true;
  }

  /**
   * Add the new attribute values to an instance
   * 
   * @param instance the instance to process
   */
  protected void convertInstance(Instance instance) {
    double[] vals = new double[outputFormatPeek().numAttributes()];

    // original values first
    for (int i = 0; i < instance.numAttributes(); i++) {
      vals[i] = instance.value(i);
    }

    // new user values
    Instances outputFormat = getOutputFormat();
    for (int i = instance.numAttributes(); i < outputFormatPeek()
        .numAttributes(); i++) {
      AttributeSpec spec = m_attributeSpecs.get(i - instance.numAttributes());
      Attribute outAtt = outputFormat.attribute(i);
      if (outAtt.isDate()) {
        vals[i] = spec.getDateValue().getTime();
      } else if (outAtt.isNumeric()) {
        vals[i] = spec.getNumericValue();
      } else if (outAtt.isNominal()) {
        String nomVal = spec.getNominalOrStringValue();
        vals[i] = outAtt.indexOfValue(nomVal);
      } else {
        // string attribute
        String nomVal = spec.getNominalOrStringValue();
        vals[i] = outAtt.addStringValue(nomVal);
      }
    }

    Instance inst = null;
    if (instance instanceof SparseInstance) {
      inst = new SparseInstance(instance.weight(), vals);
    } else {
      inst = new DenseInstance(instance.weight(), vals);
    }
    inst.setDataset(outputFormat);
    push(inst);
  }

  /**
   * Create and set the output format
   */
  protected void setOutputFormat() {
    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    Instances inputF = getInputFormat();
    ArrayList<Attribute> newAtts = new ArrayList<Attribute>();

    // existing attributes
    for (int i = 0; i < inputF.numAttributes(); i++) {
      newAtts.add((Attribute) inputF.attribute(i).copy());
    }

    // new user-defined attributes
    for (int i = 0; i < m_attributeSpecs.size(); i++) {
      AttributeSpec a = m_attributeSpecs.get(i);
      a.init(m_env);

      String type = a.getResolvedType();
      Attribute newAtt = null;
      if (type.toLowerCase().startsWith("date")) {
        String format = a.getDateFormat();
        if (format == null) {
          format = "yyyy-MM-dd'T'HH:mm:ss";
        }
        newAtt = new Attribute(a.getResolvedName(), format);
      } else if (type.toLowerCase().startsWith("string")) {
        newAtt = new Attribute(a.getResolvedName(), (List<String>) null);
      } else if (type.toLowerCase().startsWith("nominal")) {
        List<String> vals = new ArrayList<String>();
        vals.add(a.getResolvedValue());
        newAtt = new Attribute(a.getResolvedName(), vals);
      } else {
        // numeric
        newAtt = new Attribute(a.getResolvedName());
      }

      newAtts.add(newAtt);
    }

    Instances outputFormat = new Instances(inputF.relationName(), newAtts, 0);
    outputFormat.setClassIndex(inputF.classIndex());
    setOutputFormat(outputFormat);
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String[] argv) {
    runFilter(new AddUserFields(), argv);
  }
}
