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
 *    DatabaseSaver.java
 *    Copyright (C) 2004-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.gui.FilePropertyMetadata;
import weka.gui.PasswordProperty;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Vector;

/**
 * <!-- globalinfo-start --> Writes to a database (tested with MySQL, InstantDB,
 * HSQLDB).
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -url &lt;JDBC URL&gt;
 *  The JDBC URL to connect to.
 *  (default: from DatabaseUtils.props file)
 * </pre>
 * 
 * <pre>
 * -user &lt;name&gt;
 *  The user to connect with to the database.
 *  (default: none)
 * </pre>
 * 
 * <pre>
 * -password &lt;password&gt;
 *  The password to connect with to the database.
 *  (default: none)
 * </pre>
 * 
 * <pre>
 * -T &lt;table name&gt;
 *  The name of the table.
 *  (default: the relation name)
 * </pre>
 * 
 * <pre>
 * -truncate
 *  Truncate (i.e. delete any data) in table before inserting
 * </pre>
 * 
 * <pre>
 * -P
 *  Add an ID column as primary key. The name is specified
 *  in the DatabaseUtils file ('idColumn'). The DatabaseLoader
 *  won't load this column.
 * </pre>
 * 
 * <pre>
 * -custom-props &lt;file&gt;
 *  The custom properties file to use instead of default ones,
 *  containing the database parameters.
 *  (default: none)
 * </pre>
 * 
 * <pre>
 * -i &lt;input file name&gt;
 *  Input file in arff format that should be saved in database.
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 12418 $
 */
public class DatabaseSaver extends AbstractSaver implements BatchConverter,
  IncrementalConverter, DatabaseConverter, OptionHandler, EnvironmentHandler {

  /** for serialization. */
  static final long serialVersionUID = 863971733782624956L;

  /** The database connection. */
  protected DatabaseConnection m_DataBaseConnection;

  /** The name of the table in which the instances should be stored. */
  protected String m_tableName;

  /** Table name with any environment variables resolved */
  protected String m_resolvedTableName;

  /** An input arff file (for command line use). */
  protected String m_inputFile;

  /**
   * The database specific type for a string (read in from the properties file).
   */
  protected String m_createText;

  /**
   * The database specific type for a double (read in from the properties file).
   */
  protected String m_createDouble;

  /** The database specific type for an int (read in from the properties file). */
  protected String m_createInt;

  /** The database specific type for a date (read in from the properties file). */
  protected String m_createDate;

  /** For converting the date value into a database string. */
  protected SimpleDateFormat m_DateFormat;

  /**
   * The name of the primary key column that will be automatically generated (if
   * enabled). The name is read from DatabaseUtils.
   */
  protected String m_idColumn;

  /** counts the rows and used as a primary key value. */
  protected int m_count;

  /** Flag indicating if a primary key column should be added. */
  protected boolean m_id;

  /**
   * Flag indicating whether the default name of the table is the relaion name
   * or not.
   */
  protected boolean m_tabName;

  /** the database URL. */
  protected String m_URL;

  /** the user name for the database. */
  protected String m_Username;

  /** the password for the database. */
  protected String m_Password = "";

  /** the custom props file to use instead of default one. */
  protected File m_CustomPropsFile = new File("${user.home}");

  /**
   * Whether to truncate (i.e. drop and then recreate) the table if it already
   * exists
   */
  protected boolean m_truncate;

  /** Environment variables to use */
  protected transient Environment m_env;

  /**
   * Constructor.
   * 
   * @throws Exception throws Exception if property file cannot be read
   */
  public DatabaseSaver() throws Exception {

    resetOptions();
  }

  /**
   * Main method.
   *
   * @param options should contain the options of a Saver.
   */
  public static void main(String[] options) {

    StringBuffer text = new StringBuffer();
    text.append("\n\nDatabaseSaver options:\n");
    try {
      DatabaseSaver asv = new DatabaseSaver();
      try {
        Enumeration<Option> enumi = asv.listOptions();
        while (enumi.hasMoreElements()) {
          Option option = enumi.nextElement();
          text.append(option.synopsis() + '\n');
          text.append(option.description() + '\n');
        }
        asv.setOptions(options);
        asv.setDestination(asv.getUrl());
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      // incremental

      /*
       * asv.setRetrieval(INCREMENTAL); Instances instances =
       * asv.getInstances(); asv.setStructure(instances); for(int i = 0; i <
       * instances.numInstances(); i++){ //last instance is null and finishes
       * incremental saving asv.writeIncremental(instances.instance(i)); }
       * asv.writeIncremental(null);
       */

      // batch
      asv.writeBatch();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.out.println(text);
    }

  }

  private void checkEnv() {
    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }
  }

  /**
   * Set the environment variables to use.
   *
   * @param env the environment variables to use
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
    try {
      // force a new connection and setting of all parameters
      // with environment variables resolved
      m_DataBaseConnection = newDatabaseConnection();
      setUrl(m_URL);
      setUser(m_Username);
      setPassword(m_Password);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Initializes a new DatabaseConnection object, either default one or from
   * custom props file.
   *
   * @return the DatabaseConnection object
   * @see #m_CustomPropsFile
   */
  protected DatabaseConnection newDatabaseConnection() throws Exception {
    DatabaseConnection result = new DatabaseConnection();
    checkEnv();

    if (m_CustomPropsFile != null) {
      File pFile = new File(m_CustomPropsFile.getPath());
      String pPath = m_CustomPropsFile.getPath();

      try {
        pPath = m_env.substitute(pPath);
        pFile = new File(pPath);
      } catch (Exception ex) {
      }
      if (pFile.isFile()) {
        result = new DatabaseConnection(pFile);
      }
    }

    m_createText = result.getProperties().getProperty("CREATE_STRING");
    m_createDouble = result.getProperties().getProperty("CREATE_DOUBLE");
    m_createInt = result.getProperties().getProperty("CREATE_INT");
    m_createDate =
      result.getProperties().getProperty("CREATE_DATE", "DATETIME");
    m_DateFormat =
      new SimpleDateFormat(result.getProperties().getProperty("DateFormat",
        "yyyy-MM-dd HH:mm:ss"));
    m_idColumn = result.getProperties().getProperty("idColumn");

    return result;
  }

  /**
   * Resets the Saver ready to save a new data set.
   */
  @Override
  public void resetOptions() {

    super.resetOptions();

    setRetrieval(NONE);

    try {
      if (m_DataBaseConnection != null && m_DataBaseConnection.isConnected()) {
        m_DataBaseConnection.disconnectFromDatabase();
      }
      m_DataBaseConnection = newDatabaseConnection();
    } catch (Exception ex) {
      printException(ex);
    }

    m_URL = m_DataBaseConnection.getDatabaseURL();
    m_tableName = "";
    m_Username = m_DataBaseConnection.getUsername();
    m_Password = m_DataBaseConnection.getPassword();
    m_count = 1;
    m_id = false;
    m_tabName = true;

    /*
     * m_createText =
     * m_DataBaseConnection.getProperties().getProperty("CREATE_STRING");
     * m_createDouble =
     * m_DataBaseConnection.getProperties().getProperty("CREATE_DOUBLE");
     * m_createInt =
     * m_DataBaseConnection.getProperties().getProperty("CREATE_INT");
     * m_createDate =
     * m_DataBaseConnection.getProperties().getProperty("CREATE_DATE",
     * "DATETIME"); m_DateFormat = new
     * SimpleDateFormat(m_DataBaseConnection.getProperties
     * ().getProperty("DateFormat", "yyyy-MM-dd HH:mm:ss")); m_idColumn =
     * m_DataBaseConnection.getProperties().getProperty("idColumn");
     */
  }

  /**
   * Cancels the incremental saving process and tries to drop the table if the
   * write mode is CANCEL.
   */
  @Override
  public void cancel() {

    if (getWriteMode() == CANCEL) {
      try {
        m_DataBaseConnection.update("DROP TABLE " + m_resolvedTableName);
        if (m_DataBaseConnection.tableExists(m_resolvedTableName)) {
          System.err.println("Table cannot be dropped.");
        }
      } catch (Exception ex) {
        printException(ex);
      }
      resetOptions();
    }
  }

  /**
   * Returns a string describing this Saver.
   *
   * @return a description of the Saver suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Writes to a database (tested with MySQL, InstantDB, HSQLDB).";
  }

  /**
   * Gets the table's name.
   *
   * @return the table's name
   */
  @OptionMetadata(displayName = "Table name",
    description = "Sets the name of the table", displayOrder = 4)
  public String getTableName() {

    return m_tableName;
  }

  /**
   * Sets the table's name.
   *
   * @param tn the name of the table
   */
  public void setTableName(String tn) {

    m_tableName = tn;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return the tip text for this property
   */
  public String tableNameTipText() {

    return "Sets the name of the table.";
  }

  /**
   * Get whether to truncate (i.e. drop and recreate) the table if it already
   * exits. If false, then new data is appended to the table.
   *
   * @return true if the table should be truncated first (if it exists).
   */
  @OptionMetadata(displayName = "Truncate table",
    description = "Truncate (i.e. drop and recreate) table if it already exists",
    displayOrder = 6)
  public boolean getTruncate() {
    return m_truncate;
  }

  /**
   * Set whether to truncate (i.e. drop and recreate) the table if it already
   * exits. If false, then new data is appended to the table.
   *
   * @param t true if the table should be truncated first (if it exists).
   */
  public void setTruncate(boolean t) {
    m_truncate = t;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return the tip text for this property
   */
  public String truncateTipText() {
    return "Truncate (i.e. drop and recreate) table if it already exists";
  }

  /**
   * Gets whether or not a primary key will be generated automatically.
   *
   * @return true if a primary key column will be generated, false otherwise
   */
  @OptionMetadata(
    displayName = "Automatic primary key",
    description = "If set to true, a primary key column is generated automatically (containing the row number as INTEGER). The name of the key is read from DatabaseUtils (idColumn)"
      + " This primary key can be used for incremental loading (requires an unique key). This primary key will not be loaded as an attribute.",
    displayOrder = 7)
  public
    boolean getAutoKeyGeneration() {

    return m_id;
  }

  /**
   * En/Dis-ables the automatic generation of a primary key.
   *
   * @param flag flag for automatic key-genereration
   */
  public void setAutoKeyGeneration(boolean flag) {

    m_id = flag;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property
   */
  public String autoKeyGenerationTipText() {

    return "If set to true, a primary key column is generated automatically (containing the row number as INTEGER). The name of the key is read from DatabaseUtils (idColumn)"
      + " This primary key can be used for incremental loading (requires an unique key). This primary key will not be loaded as an attribute.";
  }

  /**
   * Gets whether or not the relation name is used as name of the table.
   *
   * @return true if the relation name is used as the name of the table, false
   *         otherwise
   */
  @OptionMetadata(
    displayName = "Use relation name",
    description = "If set to true, the relation name will be used as name for the database table. Otherwise the user has to provide a table name.",
    displayOrder = 5)
  public
    boolean getRelationForTableName() {

    return m_tabName;
  }

  /**
   * En/Dis-ables that the relation name is used for the name of the table
   * (default enabled).
   *
   * @param flag if true the relation name is used as table name
   */
  public void setRelationForTableName(boolean flag) {

    m_tabName = flag;
  }

  /**
   * Returns the tip text fo this property.
   *
   * @return the tip text for this property
   */
  public String relationForTableNameTipText() {

    return "If set to true, the relation name will be used as name for the database table. Otherwise the user has to provide a table name.";
  }

  /**
   * Gets the database URL.
   *
   * @return the URL
   */
  @OptionMetadata(displayName = "Database URL",
    description = "The URL of the database", displayOrder = 1)
  @Override
  public String getUrl() {

    return m_URL;
  }

  /**
   * Sets the database URL.
   *
   * @param url the URL
   */
  @Override
  public void setUrl(String url) {
    checkEnv();

    m_URL = url;
    String uCopy = m_URL;
    try {
      uCopy = m_env.substitute(uCopy);
    } catch (Exception ex) {
    }
    m_DataBaseConnection.setDatabaseURL(uCopy);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return the tip text for this property
   */
  public String urlTipText() {

    return "The URL of the database";
  }

  /**
   * Gets the database user.
   *
   * @return the user name
   */
  @Override
  public String getUser() {

    // return m_DataBaseConnection.getUsername();
    return m_Username;
  }

  /**
   * Sets the database user.
   *
   * @param user the user name
   */
  @OptionMetadata(displayName = "Username",
    description = "The user name for the database", displayOrder = 2)
  @Override
  public void setUser(String user) {
    checkEnv();
    m_Username = user;
    String userCopy = user;
    try {
      userCopy = m_env.substitute(userCopy);
    } catch (Exception ex) {
    }

    m_DataBaseConnection.setUsername(userCopy);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return the tip text for this property
   */
  public String userTipText() {

    return "The user name for the database";
  }

  /**
   * Returns the database password.
   *
   * @return the database password
   */
  @OptionMetadata(displayName = "Password",
    description = "The database password", displayOrder = 3)
  @PasswordProperty
  public String getPassword() {
    // return m_DataBaseConnection.getPassword();
    return m_Password;
  }

  /**
   * Sets the database password.
   *
   * @param password the password
   */
  @Override
  public void setPassword(String password) {
    checkEnv();
    m_Password = password;
    String passCopy = password;
    try {
      passCopy = m_env.substitute(passCopy);
    } catch (Exception ex) {
    }
    m_DataBaseConnection.setPassword(password);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return the tip text for this property
   */
  public String passwordTipText() {

    return "The database password";
  }

  /**
   * Returns the custom properties file in use, if any.
   *
   * @return the custom props file, null if none used
   */
  @OptionMetadata(
    displayName = "DB config file",
    description = "The custom properties that the user can use to override the default ones.",
    displayOrder = 8)
  @FilePropertyMetadata(fileChooserDialogType = JFileChooser.OPEN_DIALOG,
    directoriesOnly = false)
  public
    File getCustomPropsFile() {
    return m_CustomPropsFile;
  }

  /**
   * Sets the custom properties file to use.
   *
   * @param value the custom props file to load database parameters from, use
   *          null or directory to disable custom properties.
   */
  public void setCustomPropsFile(File value) {
    m_CustomPropsFile = value;
  }

  /**
   * The tip text for this property.
   *
   * @return the tip text
   */
  public String customPropsFileTipText() {
    return "The custom properties that the user can use to override the default ones.";
  }

  /**
   * Sets the database url.
   *
   * @param url the database url
   * @param userName the user name
   * @param password the password
   */
  public void setDestination(String url, String userName, String password) {

    try {
      checkEnv();

      m_DataBaseConnection = newDatabaseConnection();
      setUrl(url);
      setUser(userName);
      setPassword(password);
      // m_DataBaseConnection.setDatabaseURL(url);
      // m_DataBaseConnection.setUsername(userName);
      // m_DataBaseConnection.setPassword(password);
    } catch (Exception ex) {
      printException(ex);
    }
  }

  /**
   * Sets the database url.
   *
   * @param url the database url
   */
  public void setDestination(String url) {

    try {
      checkEnv();

      m_DataBaseConnection = newDatabaseConnection();
      // m_DataBaseConnection.setDatabaseURL(url);
      setUrl(url);
      setUser(m_Username);
      setPassword(m_Password);
      // m_DataBaseConnection.setUsername(m_Username);
      // m_DataBaseConnection.setPassword(m_Password);
    } catch (Exception ex) {
      printException(ex);
    }
  }

  /** Sets the database url using the DatabaseUtils file. */
  public void setDestination() {

    try {
      checkEnv();

      m_DataBaseConnection = newDatabaseConnection();
      setUser(m_Username);
      setPassword(m_Password);
      // m_DataBaseConnection.setUsername(m_Username);
      // m_DataBaseConnection.setPassword(m_Password);
    } catch (Exception ex) {
      printException(ex);
    }
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
    result.enable(Capability.MISSING_VALUES);
    result.enable(Capability.STRING_ATTRIBUTES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.STRING_CLASS);
    result.enable(Capability.NO_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Opens a connection to the database.
   *
   */
  public void connectToDatabase() {

    try {
      if (!m_DataBaseConnection.isConnected()) {
        m_DataBaseConnection.connectToDatabase();
      }
    } catch (Exception ex) {
      printException(ex);
    }
  }

  /**
   * Writes the structure (header information) to a database by creating a new
   * table.
   *
   * @throws Exception if something goes wrong
   */
  private void writeStructure() throws Exception {

    StringBuffer query = new StringBuffer();
    Instances structure = getInstances();
    query.append("CREATE TABLE ");
    m_resolvedTableName = m_env.substitute(m_tableName);
    if (m_tabName || m_resolvedTableName.equals("")) {
      m_resolvedTableName =
        m_DataBaseConnection.maskKeyword(structure.relationName());
    }
    if (m_DataBaseConnection.getUpperCase()) {
      m_resolvedTableName = m_resolvedTableName.toUpperCase();
      m_createInt = m_createInt.toUpperCase();
      m_createDouble = m_createDouble.toUpperCase();
      m_createText = m_createText.toUpperCase();
      m_createDate = m_createDate.toUpperCase();
    }
    m_resolvedTableName = m_resolvedTableName.replaceAll("[^\\w]", "_");
    m_resolvedTableName = m_DataBaseConnection.maskKeyword(m_resolvedTableName);
    query.append(m_resolvedTableName);
    if (structure.numAttributes() == 0) {
      throw new Exception("Instances have no attribute.");
    }
    query.append(" ( ");

    if (m_DataBaseConnection.tableExists(m_resolvedTableName)) {
      if (!m_truncate) {
        System.err.println("[DatabaseSaver] Table '" + m_resolvedTableName
          + "' already exists - will append data...");

        // if incremental and using primary key set the correct start value
        // for count
        if (getRetrieval() == INCREMENTAL && m_id) {
          String countS = "SELECT COUNT(*) FROM " + m_resolvedTableName;
          m_DataBaseConnection.execute(countS);
          ResultSet countRS = m_DataBaseConnection.getResultSet();
          countRS.next();
          m_count = countRS.getInt(1);
          countRS.close();
          m_count++;
        }

        return;
      }
      String trunc = "DROP TABLE " + m_resolvedTableName;
      m_DataBaseConnection.execute(trunc);
    }

    if (m_id) {
      if (m_DataBaseConnection.getUpperCase()) {
        m_idColumn = m_idColumn.toUpperCase();
      }
      query.append(m_DataBaseConnection.maskKeyword(m_idColumn));
      query.append(" ");
      query.append(m_createInt);
      query.append(" PRIMARY KEY,");
    }
    for (int i = 0; i < structure.numAttributes(); i++) {
      Attribute att = structure.attribute(i);
      String attName = att.name();
      attName = attName.replaceAll("[^\\w]", "_");
      attName = m_DataBaseConnection.maskKeyword(attName);
      if (m_DataBaseConnection.getUpperCase()) {
        query.append(attName.toUpperCase());
      } else {
        query.append(attName);
      }
      if (att.isDate()) {
        query.append(" " + m_createDate);
      } else {
        if (att.isNumeric()) {
          query.append(" " + m_createDouble);
        } else {
          query.append(" " + m_createText);
        }
      }
      if (i != structure.numAttributes() - 1) {
        query.append(", ");
      }
    }
    query.append(" )");
    // System.out.println(query.toString());
    m_DataBaseConnection.update(query.toString());
    m_DataBaseConnection.close();
    if (!m_DataBaseConnection.tableExists(m_resolvedTableName)) {
      throw new IOException("Table cannot be built.");
    }
  }

  /**
   * inserts the given instance into the table.
   *
   * @param inst the instance to insert
   * @throws Exception if something goes wrong
   */
  private void writeInstance(Instance inst) throws Exception {

    StringBuffer insert = new StringBuffer();
    insert.append("INSERT INTO ");
    insert.append(m_resolvedTableName);
    insert.append(" VALUES ( ");
    if (m_id) {
      insert.append(m_count);
      insert.append(", ");
      m_count++;
    }
    for (int j = 0; j < inst.numAttributes(); j++) {
      if (inst.isMissing(j)) {
        insert.append("NULL");
      } else {
        if ((inst.attribute(j)).isDate()) {
          insert.append("'" + m_DateFormat.format((long) inst.value(j)) + "'");
        } else if ((inst.attribute(j)).isNumeric()) {
          insert.append(inst.value(j));
        } else {
          String stringInsert = "'" + inst.stringValue(j) + "'";
          if (stringInsert.length() > 2) {
            stringInsert = stringInsert.replaceAll("''", "'");
          }
          insert.append(stringInsert);
        }
      }
      if (j != inst.numAttributes() - 1) {
        insert.append(", ");
      }
    }
    insert.append(" )");
    // System.out.println(insert.toString());
    if (m_DataBaseConnection.update(insert.toString()) < 1) {
      throw new IOException("Tuple cannot be inserted.");
    } else {
      m_DataBaseConnection.close();
    }
  }

  /**
   * Saves an instances incrementally. Structure has to be set by using the
   * setStructure() method or setInstances() method. When a structure is set, a
   * table is created.
   *
   * @param inst the instance to save
   * @throws IOException throws IOEXception.
   */
  @Override
  public void writeIncremental(Instance inst) throws IOException {

    int writeMode = getWriteMode();
    Instances structure = getInstances();

    if (m_DataBaseConnection == null) {
      throw new IOException("No database has been set up.");
    }
    if (getRetrieval() == BATCH) {
      throw new IOException("Batch and incremental saving cannot be mixed.");
    }
    setRetrieval(INCREMENTAL);

    try {
      if (!m_DataBaseConnection.isConnected()) {
        connectToDatabase();
      }
      if (writeMode == WAIT) {
        if (structure == null) {
          setWriteMode(CANCEL);
          if (inst != null) {
            throw new Exception(
              "Structure(Header Information) has to be set in advance");
          }
        } else {
          setWriteMode(STRUCTURE_READY);
        }
        writeMode = getWriteMode();
      }
      if (writeMode == CANCEL) {
        cancel();
      }
      if (writeMode == STRUCTURE_READY) {
        setWriteMode(WRITE);
        writeStructure();
        writeMode = getWriteMode();
      }
      if (writeMode == WRITE) {
        if (structure == null) {
          throw new IOException("No instances information available.");
        }
        if (inst != null) {
          // write instance
          writeInstance(inst);
        } else {
          // close
          m_DataBaseConnection.disconnectFromDatabase();
          resetStructure();
          m_count = 1;
        }
      }
    } catch (Exception ex) {
      printException(ex);
    }
  }

  /**
   * Writes a Batch of instances.
   *
   * @throws IOException throws IOException
   */
  @Override
  public void writeBatch() throws IOException {

    Instances instances = getInstances();
    if (instances == null) {
      throw new IOException("No instances to save");
    }
    if (getRetrieval() == INCREMENTAL) {
      throw new IOException("Batch and incremental saving cannot be mixed.");
    }
    if (m_DataBaseConnection == null) {
      throw new IOException("No database has been set up.");
    }
    setRetrieval(BATCH);
    try {
      if (!m_DataBaseConnection.isConnected()) {
        connectToDatabase();
      }
      setWriteMode(WRITE);
      writeStructure();
      for (int i = 0; i < instances.numInstances(); i++) {
        writeInstance(instances.instance(i));
      }
      m_DataBaseConnection.disconnectFromDatabase();
      setWriteMode(WAIT);
      resetStructure();
      m_count = 1;
    } catch (Exception ex) {
      printException(ex);
    }
  }

  /**
   * Prints an exception.
   *
   * @param ex the exception to print
   */
  private void printException(Exception ex) {

    System.out.println("\n--- Exception caught ---\n");
    while (ex != null) {
      System.out.println("Message:   " + ex.getMessage());
      if (ex instanceof SQLException) {
        System.out.println("SQLState:  " + ((SQLException) ex).getSQLState());
        System.out.println("ErrorCode: " + ((SQLException) ex).getErrorCode());
        ex = ((SQLException) ex).getNextException();
      } else {
        ex = null;
      }
      System.out.println("");
    }

  }

  /**
   * Gets the setting.
   *
   * @return the current setting
   */
  @Override
  public String[] getOptions() {
    Vector<String> options = new Vector<String>();

    if ((getUrl() != null) && (getUrl().length() != 0)) {
      options.add("-url");
      options.add(getUrl());
    }

    if ((getUser() != null) && (getUser().length() != 0)) {
      options.add("-user");
      options.add(getUser());
    }

    if ((getPassword() != null) && (getPassword().length() != 0)) {
      options.add("-password");
      options.add(getPassword());
    }

    if ((m_tableName != null) && (m_tableName.length() != 0)) {
      options.add("-T");
      options.add(m_tableName);
    }

    if (m_truncate) {
      options.add("-truncate");
    }

    if (m_id) {
      options.add("-P");
    }

    if ((m_inputFile != null) && (m_inputFile.length() != 0)) {
      options.add("-i");
      options.add(m_inputFile);
    }

    if ((m_CustomPropsFile != null) && !m_CustomPropsFile.isDirectory()) {
      options.add("-custom-props");
      options.add(m_CustomPropsFile.toString());
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Sets the options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -url &lt;JDBC URL&gt;
   *  The JDBC URL to connect to.
   *  (default: from DatabaseUtils.props file)
   * </pre>
   * 
   * <pre>
   * -user &lt;name&gt;
   *  The user to connect with to the database.
   *  (default: none)
   * </pre>
   * 
   * <pre>
   * -password &lt;password&gt;
   *  The password to connect with to the database.
   *  (default: none)
   * </pre>
   * 
   * <pre>
   * -T &lt;table name&gt;
   *  The name of the table.
   *  (default: the relation name)
   * </pre>
   * 
   * <pre>
   * -truncate
   *  Truncate (i.e. delete any data) in table before inserting
   * </pre>
   * 
   * <pre>
   * -P
   *  Add an ID column as primary key. The name is specified
   *  in the DatabaseUtils file ('idColumn'). The DatabaseLoader
   *  won't load this column.
   * </pre>
   * 
   * <pre>
   * -custom-props &lt;file&gt;
   *  The custom properties file to use instead of default ones,
   *  containing the database parameters.
   *  (default: none)
   * </pre>
   * 
   * <pre>
   * -i &lt;input file name&gt;
   *  Input file in arff format that should be saved in database.
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the options
   * @throws Exception if options cannot be set
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String tableString, inputString, tmpStr;

    resetOptions();

    tmpStr = Utils.getOption("url", options);
    if (tmpStr.length() != 0) {
      setUrl(tmpStr);
    }

    tmpStr = Utils.getOption("user", options);
    if (tmpStr.length() != 0) {
      setUser(tmpStr);
    }

    tmpStr = Utils.getOption("password", options);
    if (tmpStr.length() != 0) {
      setPassword(tmpStr);
    }

    tableString = Utils.getOption('T', options);

    m_truncate = Utils.getFlag("truncate", options);

    inputString = Utils.getOption('i', options);

    if (tableString.length() != 0) {
      m_tableName = tableString;
      m_tabName = false;
    }

    m_id = Utils.getFlag('P', options);

    if (inputString.length() != 0) {
      try {
        m_inputFile = inputString;
        ArffLoader al = new ArffLoader();
        File inputFile = new File(inputString);
        al.setSource(inputFile);
        setInstances(al.getDataSet());
        // System.out.println(getInstances());
        if (tableString.length() == 0) {
          m_tableName = getInstances().relationName();
        }
      } catch (Exception ex) {
        printException(ex);
        ex.printStackTrace();
      }
    }

    tmpStr = Utils.getOption("custom-props", options);
    if (tmpStr.length() == 0) {
      setCustomPropsFile(null);
    } else {
      setCustomPropsFile(new File(tmpStr));
    }

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Lists the available options.
   *
   * @return an enumeration of the available options
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>();

    newVector.addElement(new Option("\tThe JDBC URL to connect to.\n"
      + "\t(default: from DatabaseUtils.props file)", "url", 1,
      "-url <JDBC URL>"));

    newVector.addElement(new Option(
      "\tThe user to connect with to the database.\n" + "\t(default: none)",
      "user", 1, "-user <name>"));

    newVector
      .addElement(new Option(
        "\tThe password to connect with to the database.\n"
          + "\t(default: none)", "password", 1, "-password <password>"));

    newVector.addElement(new Option("\tThe name of the table.\n"
      + "\t(default: the relation name)", "T", 1, "-T <table name>"));

    newVector.addElement(new Option(
      "\tTruncate (i.e. delete any data) in table before inserting",
      "truncate", 0, "-truncate"));

    newVector.addElement(new Option(
      "\tAdd an ID column as primary key. The name is specified\n"
        + "\tin the DatabaseUtils file ('idColumn'). The DatabaseLoader\n"
        + "\twon't load this column.", "P", 0, "-P"));

    newVector.add(new Option(
      "\tThe custom properties file to use instead of default ones,\n"
        + "\tcontaining the database parameters.\n" + "\t(default: none)",
      "custom-props", 1, "-custom-props <file>"));

    newVector.addElement(new Option(
      "\tInput file in arff format that should be saved in database.", "i", 1,
      "-i <input file name>"));

    return newVector.elements();
  }

  /**
   * Returns the revision string.
   *
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12418 $");
  }
}
