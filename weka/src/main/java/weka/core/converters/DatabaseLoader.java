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
 *    DatabaseLoader.java
 *    Copyright (C) 2004-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.core.RevisionUtils;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.experiment.InstanceQuery;
import weka.gui.FilePropertyMetadata;
import weka.gui.PasswordProperty;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * <!-- globalinfo-start --> Reads Instances from a Database. Can read a
 * database in batch or incremental mode.<br/>
 * In inremental mode MySQL and HSQLDB are supported.<br/>
 * For all other DBMS set a pseudoincremental mode is used:<br/>
 * In pseudo incremental mode the instances are read into main memory all at
 * once and then incrementally provided to the user.<br/>
 * For incremental loading the rows in the database table have to be ordered
 * uniquely.<br/>
 * The reason for this is that every time only a single row is fetched by
 * extending the user query by a LIMIT clause.<br/>
 * If this extension is impossible instances will be loaded pseudoincrementally.
 * To ensure that every row is fetched exaclty once, they have to ordered.<br/>
 * Therefore a (primary) key is necessary.This approach is chosen, instead of
 * using JDBC driver facilities, because the latter one differ betweeen
 * different drivers.<br/>
 * If you use the DatabaseSaver and save instances by generating automatically a
 * primary key (its name is defined in DtabaseUtils), this primary key will be
 * used for ordering but will not be part of the output. The user defined SQL
 * query to extract the instances should not contain LIMIT and ORDER BY clauses
 * (see -Q option).<br/>
 * In addition, for incremental loading, you can define in the DatabaseUtils
 * file how many distinct values a nominal attribute is allowed to have. If this
 * number is exceeded, the column will become a string attribute.<br/>
 * In batch mode no string attributes will be created.
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
 * -Q &lt;query&gt;
 *  SQL query of the form
 *   SELECT &lt;list of columns&gt;|* FROM &lt;table&gt; [WHERE]
 *  to execute.
 *  (default: Select * From Results0)
 * </pre>
 * 
 * <pre>
 * -P &lt;list of column names&gt;
 *  List of column names uniquely defining a DB row
 *  (separated by ', ').
 *  Used for incremental loading.
 *  If not specified, the key will be determined automatically,
 *  if possible with the used JDBC driver.
 *  The auto ID column created by the DatabaseSaver won't be loaded.
 * </pre>
 * 
 * <pre>
 * -I
 *  Sets incremental loading
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 12418 $
 * @see Loader
 */
public class DatabaseLoader extends AbstractLoader implements BatchConverter,
  IncrementalConverter, DatabaseConverter, OptionHandler, EnvironmentHandler {

  /** for serialization */
  static final long serialVersionUID = -7936159015338318659L;

  /**
   * The header information that is retrieved in the beginning of incremental
   * loading
   */
  protected Instances m_structure;

  /**
   * Used in pseudoincremental mode. The whole dataset from which instances will
   * be read incrementally.
   */
  protected Instances m_datasetPseudoInc;

  /**
   * Set of instances that equals m_structure except that the auto_generated_id
   * column is not included as an attribute
   */
  protected Instances m_oldStructure;

  /** The database connection */
  protected DatabaseConnection m_DataBaseConnection;

  /**
   * The user defined query to load instances. (form: SELECT
   * *|&ltcolumn-list&gt; FROM &lttable&gt; [WHERE &lt;condition&gt;])
   */
  protected String m_query = "Select * from Results0";

  /**
   * Flag indicating that pseudo incremental mode is used (all instances load at
   * once into main memeory and then incrementally from main memory instead of
   * the database)
   */
  protected boolean m_pseudoIncremental;

  /**
   * If true it checks whether or not the table exists in the database before
   * loading depending on jdbc metadata information. Set flag to false if no
   * check is required or if jdbc metadata is not complete.
   */
  protected boolean m_checkForTable;

  /**
   * Limit when an attribute is treated as string attribute and not as a nominal
   * one because it has to many values.
   */
  protected int m_nominalToStringLimit;

  /**
   * The number of rows obtained by m_query, eg the size of the ResultSet to
   * load
   */
  protected int m_rowCount;

  /** Indicates how many rows has already been loaded incrementally */
  protected int m_counter;

  /**
   * Decides which SQL statement to limit the number of rows should be used.
   * DBMS dependent. Algorithm just tries several possibilities.
   */
  protected int m_choice;

  /** Flag indicating that incremental process wants to read first instance */
  protected boolean m_firstTime;

  /**
   * Flag indicating that incremental mode is chosen (for command line use only)
   */
  protected boolean m_inc;

  /**
   * Contains the name of the columns that uniquely define a row in the
   * ResultSet. Ensures a unique ordering of instances for indremental loading.
   */
  protected ArrayList<String> m_orderBy;

  /** Stores the index of a nominal value */
  protected Hashtable<String, Double>[] m_nominalIndexes;

  /** Stores the nominal value */
  protected ArrayList<String>[] m_nominalStrings;

  /**
   * Name of the primary key column that will allow unique ordering necessary
   * for incremental loading. The name is specified in the DatabaseUtils file.
   */
  protected String m_idColumn;

  /** the JDBC URL to use */
  protected String m_URL = null;

  /** the database user to use */
  protected String m_User = "";

  /** the database password to use */
  protected String m_Password = "";

  /** the keys for unique ordering */
  protected String m_Keys = "";

  /** the custom props file to use instead of default one. */
  protected File m_CustomPropsFile = new File("${user.home}");

  /** Determines whether sparse data is created */
  protected boolean m_CreateSparseData = false;

  /** Environment variables */
  protected transient Environment m_env;

  /**
   * Constructor
   * 
   * @throws Exception if initialization fails
   */
  public DatabaseLoader() throws Exception {

    resetOptions();
  }

  /**
   * Returns a string describing this Loader
   * 
   * @return a description of the Loader suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Reads Instances from a Database. "
      + "Can read a database in batch or incremental mode.\n"
      + "In inremental mode MySQL and HSQLDB are supported.\n"
      + "For all other DBMS set a pseudoincremental mode is used:\n"
      + "In pseudo incremental mode the instances are read into main memory all at once and then incrementally provided to the user.\n"
      + "For incremental loading the rows in the database table have to be ordered uniquely.\n"
      + "The reason for this is that every time only a single row is fetched by extending the user query by a LIMIT clause.\n"
      + "If this extension is impossible instances will be loaded pseudoincrementally. To ensure that every row is fetched exaclty once, they have to ordered.\n"
      + "Therefore a (primary) key is necessary.This approach is chosen, instead of using JDBC driver facilities, because the latter one differ betweeen different drivers.\n"
      + "If you use the DatabaseSaver and save instances by generating automatically a primary key (its name is defined in DtabaseUtils), this primary key will "
      + "be used for ordering but will not be part of the output. The user defined SQL query to extract the instances should not contain LIMIT and ORDER BY clauses (see -Q option).\n"
      + "In addition, for incremental loading,  you can define in the DatabaseUtils file how many distinct values a nominal attribute is allowed to have. If this number is exceeded, the column will become a string attribute.\n"
      + "In batch mode no string attributes will be created.";
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
      setUser(m_User);
      setPassword(m_Password);
    } catch (Exception ex) {
      // we won't complain about it here...
    }
  }

  private void checkEnv() {
    if (m_env == null) {
      m_env = Environment.getSystemWide();
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
    DatabaseConnection result;

    checkEnv();

    if (m_CustomPropsFile != null) {
      File pFile = new File(m_CustomPropsFile.getPath());
      String pPath = m_CustomPropsFile.getPath();
      try {
        pPath = m_env.substitute(pPath);
        pFile = new File(pPath);
      } catch (Exception ex) {
      }
      result = new DatabaseConnection(pFile);
    } else {
      result = new DatabaseConnection();
    }

    m_pseudoIncremental = false;
    m_checkForTable = true;
    String props = result.getProperties().getProperty("nominalToStringLimit");
    m_nominalToStringLimit = Integer.parseInt(props);
    m_idColumn = result.getProperties().getProperty("idColumn");
    if (result.getProperties().getProperty("checkForTable", "")
      .equalsIgnoreCase("FALSE")) {
      m_checkForTable = false;
    }

    return result;
  }

  /**
   * Resets the Loader to the settings in either the default DatabaseUtils.props
   * or any property file that the user has specified via setCustomPropsFile().
   */
  public void resetOptions() {
    resetStructure();
    try {
      if (m_DataBaseConnection != null && m_DataBaseConnection.isConnected()) {
        m_DataBaseConnection.disconnectFromDatabase();
      }
      m_DataBaseConnection = newDatabaseConnection();
    } catch (Exception ex) {
      printException(ex);
    }

    m_URL = m_DataBaseConnection.getDatabaseURL();
    if (m_URL == null) {
      m_URL = "none set!";
    }
    m_User = m_DataBaseConnection.getUsername();
    if (m_User == null) {
      m_User = "";
    }
    m_Password = m_DataBaseConnection.getPassword();
    if (m_Password == null) {
      m_Password = "";
    }
    m_orderBy = new ArrayList<String>();
  }

  /**
   * Resets the Loader ready to read a new data set using set options
   * 
   * @throws Exception if an error occurs while disconnecting from the database
   */
  @Override
  public void reset() {

    resetStructure();
    try {
      if (m_DataBaseConnection != null && m_DataBaseConnection.isConnected()) {
        m_DataBaseConnection.disconnectFromDatabase();
      }
      m_DataBaseConnection = newDatabaseConnection();
    } catch (Exception ex) {
      printException(ex);
    }

    // don't lose previously set connection data!
    if (m_URL != null) {
      setUrl(m_URL);
    }

    if (m_User != null) {
      setUser(m_User);
    }

    if (m_Password != null) {
      setPassword(m_Password);
    }

    m_orderBy = new ArrayList<String>();
    // don't lose previously set key columns!
    if (m_Keys != null) {
      String k = m_Keys;
      try {
        k = m_env.substitute(k);
      } catch (Exception ex) {
      }
      setKeys(k);
    }

    m_inc = false;
  }

  /**
   * Resets the structure of instances
   */
  public void resetStructure() {

    m_structure = null;
    m_datasetPseudoInc = null;
    m_oldStructure = null;
    m_rowCount = 0;
    m_counter = 0;
    m_choice = 0;
    m_firstTime = true;
    setRetrieval(NONE);
  }

  /**
   * Sets the query to execute against the database
   * 
   * @param q the query to execute
   */
  public void setQuery(String q) {
    q = q.replaceAll("[fF][rR][oO][mM]", "FROM");
    q = q.replaceFirst("[sS][eE][lL][eE][cC][tT]", "SELECT");
    m_query = q;
  }

  /**
   * Gets the query to execute against the database
   * 
   * @return the query
   */
  @OptionMetadata(displayName = "Query", description = "The query to execute",
    displayOrder = 4)
  public String getQuery() {
    return m_query;
  }

  /**
   * the tip text for this property
   * 
   * @return the tip text
   */
  public String queryTipText() {

    return "The query that should load the instances."
      + "\n The query has to be of the form SELECT <column-list>|* FROM <table> [WHERE <conditions>]";
  }

  /**
   * Sets the key columns of a database table
   * 
   * @param keys a String containing the key columns in a comma separated list.
   */
  public void setKeys(String keys) {

    m_Keys = keys;
    m_orderBy.clear();
    StringTokenizer st = new StringTokenizer(keys, ",");
    while (st.hasMoreTokens()) {
      String column = st.nextToken();
      column = column.replaceAll(" ", "");
      m_orderBy.add(column);
    }
  }

  /**
   * Gets the key columns' name
   * 
   * @return name of the key columns'
   */
  @OptionMetadata(
    displayName = "Key columns",
    description = "Specific key columns to use if "
      + "a primary key cannot be automatically detected. Used in incremental loading.",
    displayOrder = 5)
  public
    String getKeys() {

    StringBuffer key = new StringBuffer();
    for (int i = 0; i < m_orderBy.size(); i++) {
      key.append(m_orderBy.get(i));
      if (i != m_orderBy.size() - 1) {
        key.append(", ");
      }
    }
    return key.toString();
  }

  /**
   * the tip text for this property
   * 
   * @return the tip text
   */
  public String keysTipText() {

    return "For incremental loading a unique identiefer has to be specified."
      + "\nIf the query includes all columns of a table (SELECT *...) a primary key"
      + "\ncan be detected automatically depending on the JDBC driver. If that is not possible"
      + "\nspecify the key columns here in a comma separated list.";
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
   * The tip text for this property.
   * 
   * @return the tip text
   */
  public String customPropsFileTipText() {
    return "The custom properties that the user can use to override the default ones.";
  }

  /**
   * Sets the database URL
   * 
   * @param url string with the database URL
   */
  @Override
  public void setUrl(String url) {
    checkEnv();

    m_URL = url;
    String dbU = m_URL;
    try {
      dbU = m_env.substitute(dbU);
    } catch (Exception ex) {
    }

    m_DataBaseConnection.setDatabaseURL(dbU);
  }

  /**
   * Gets the URL
   * 
   * @return the URL
   */
  @OptionMetadata(displayName = "Database URL",
    description = "The URL of the database", displayOrder = 1)
  @Override
  public String getUrl() {

    // return m_DataBaseConnection.getDatabaseURL();
    return m_URL;
  }

  /**
   * the tip text for this property
   * 
   * @return the tip text
   */
  public String urlTipText() {

    return "The URL of the database";
  }

  /**
   * Sets the database user
   * 
   * @param user the database user name
   */
  @Override
  public void setUser(String user) {
    checkEnv();

    m_User = user;
    String userCopy = user;
    try {
      userCopy = m_env.substitute(userCopy);
    } catch (Exception ex) {
    }
    m_DataBaseConnection.setUsername(userCopy);
  }

  /**
   * Gets the user name
   * 
   * @return name of database user
   */
  @OptionMetadata(displayName = "Username",
    description = "The user name for the database", displayOrder = 2)
  @Override
  public String getUser() {

    // return m_DataBaseConnection.getUsername();
    return m_User;
  }

  /**
   * the tip text for this property
   * 
   * @return the tip text
   */
  public String userTipText() {

    return "The user name for the database";
  }

  /**
   * Sets user password for the database
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
   * Returns the database password
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
   * the tip text for this property
   * 
   * @return the tip text
   */
  public String passwordTipText() {

    return "The database password";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String sparseDataTipText() {
    return "Encode data as sparse instances.";
  }

  /**
   * Sets whether data should be encoded as sparse instances
   * 
   * @param s true if data should be encoded as a set of sparse instances
   */
  public void setSparseData(boolean s) {
    m_CreateSparseData = s;
  }

  /**
   * Gets whether data is to be returned as a set of sparse instances
   * 
   * @return true if data is to be encoded as sparse instances
   */
  @OptionMetadata(displayName = "Create sparse instances", description = "Return sparse "
    + "rather than normal instances", displayOrder = 6)
  public boolean getSparseData() {
    return m_CreateSparseData;
  }

  /**
   * Sets the database url, user and pw
   * 
   * @param url the database url
   * @param userName the user name
   * @param password the password
   */
  public void setSource(String url, String userName, String password) {

    try {
      m_DataBaseConnection = newDatabaseConnection();
      setUrl(url);
      setUser(userName);
      setPassword(password);
    } catch (Exception ex) {
      printException(ex);
    }
  }

  /**
   * Sets the database url
   * 
   * @param url the database url
   */
  public void setSource(String url) {

    try {
      m_DataBaseConnection = newDatabaseConnection();
      setUrl(url);
      m_User = m_DataBaseConnection.getUsername();
      m_Password = m_DataBaseConnection.getPassword();
    } catch (Exception ex) {
      printException(ex);
    }
  }

  /**
   * Sets the database url using the DatabaseUtils file
   * 
   * @throws Exception if something goes wrong
   */
  public void setSource() throws Exception {

    m_DataBaseConnection = newDatabaseConnection();
    m_URL = m_DataBaseConnection.getDatabaseURL();
    m_User = m_DataBaseConnection.getUsername();
    m_Password = m_DataBaseConnection.getPassword();
  }

  /**
   * Opens a connection to the database
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
   * Returns the table name or all after the FROM clause of the user specified
   * query to retrieve instances.
   * 
   * @param onlyTableName true if only the table name should be returned, false
   *          otherwise
   * @return the end of the query
   */
  private String endOfQuery(boolean onlyTableName) {
    String table;
    int beginIndex, endIndex;

    beginIndex = m_query.indexOf("FROM ") + 5;
    while (m_query.charAt(beginIndex) == ' ') {
      beginIndex++;
    }
    endIndex = m_query.indexOf(" ", beginIndex);
    if (endIndex != -1 && onlyTableName) {
      table = m_query.substring(beginIndex, endIndex);
    } else {
      table = m_query.substring(beginIndex);
    }
    if (m_DataBaseConnection.getUpperCase()) {
      table = table.toUpperCase();
    }
    return table;
  }

  /**
   * Checks for a unique key using the JDBC driver's method: getPrimaryKey(),
   * getBestRowIdentifier(). Depending on their implementation a key can be
   * detected. The key is needed to order the instances uniquely for an
   * inremental loading. If an existing key cannot be detected, use -P option.
   * 
   * @throws Exception if database error occurs
   * @return true, if a key could have been detected, false otherwise
   */
  private boolean checkForKey() throws Exception {

    String query = m_query;

    query = query.replaceAll(" +", " ");
    // query has to use all columns
    if (!query.startsWith("SELECT *")) {
      return false;
    }
    m_orderBy.clear();
    if (!m_DataBaseConnection.isConnected()) {
      m_DataBaseConnection.connectToDatabase();
    }
    DatabaseMetaData dmd = m_DataBaseConnection.getMetaData();
    String table = endOfQuery(true);
    // System.out.println(table);
    // check for primary keys
    ResultSet rs = dmd.getPrimaryKeys(null, null, table);
    while (rs.next()) {
      m_orderBy.add(rs.getString(4));
    }
    rs.close();
    if (m_orderBy.size() != 0) {
      return true;
    }
    // check for unique keys
    rs =
      dmd.getBestRowIdentifier(null, null, table,
        DatabaseMetaData.bestRowSession, false);
    ResultSetMetaData rmd = rs.getMetaData();
    int help = 0;
    while (rs.next()) {
      m_orderBy.add(rs.getString(2));
      help++;
    }
    rs.close();
    if (help == rmd.getColumnCount()) {
      m_orderBy.clear();
    }
    if (m_orderBy.size() != 0) {
      return true;
    }

    return false;
  }

  /**
   * Converts string attribute into nominal ones for an instance read during
   * incremental loading
   * 
   * @param rs The result set
   * @param i the index of the nominal attribute
   * @throws Exception exception if it cannot be converted
   */
  private void stringToNominal(ResultSet rs, int i) throws Exception {

    while (rs.next()) {
      String str = rs.getString(1);
      if (!rs.wasNull()) {
        Double index = m_nominalIndexes[i - 1].get(str);
        if (index == null) {
          index = new Double(m_nominalStrings[i - 1].size());
          m_nominalIndexes[i - 1].put(str, index);
          m_nominalStrings[i - 1].add(str);
        }
      }
    }
  }

  /**
   * Used in incremental loading. Modifies the SQL statement, so that only one
   * instance per time is tretieved and the instances are ordered uniquely.
   * 
   * @param query the query to modify for incremental loading
   * @param offset sets which tuple out of the uniquely ordered ones should be
   *          returned
   * @param choice the kind of query that is suitable for the used DBMS
   * @return the modified query that returns only one result tuple.
   */
  private String limitQuery(String query, int offset, int choice) {

    String limitedQuery;
    StringBuffer order = new StringBuffer();
    String orderByString = "";

    if (m_orderBy.size() != 0) {
      order.append(" ORDER BY ");
      for (int i = 0; i < m_orderBy.size() - 1; i++) {
        if (m_DataBaseConnection.getUpperCase()) {
          order.append(m_orderBy.get(i).toUpperCase());
        } else {
          order.append(m_orderBy.get(i));
        }
        order.append(", ");
      }
      if (m_DataBaseConnection.getUpperCase()) {
        order.append(m_orderBy.get(m_orderBy.size() - 1).toUpperCase());
      } else {
        order.append(m_orderBy.get(m_orderBy.size() - 1));
      }
      orderByString = order.toString();
    }
    if (choice == 0) {
      limitedQuery =
        query.replaceFirst("SELECT", "SELECT LIMIT " + offset + " 1");
      limitedQuery = limitedQuery.concat(orderByString);
      return limitedQuery;
    }
    if (choice == 1) {
      limitedQuery = query.concat(orderByString + " LIMIT 1 OFFSET " + offset);
      return limitedQuery;
    }
    limitedQuery = query.concat(orderByString + " LIMIT " + offset + ", 1");
    // System.out.println(limitedQuery);
    return limitedQuery;
  }

  /**
   * Counts the number of rows that are loaded from the database
   * 
   * @throws Exception if the number of rows cannot be calculated
   * @return the entire number of rows
   */
  private int getRowCount() throws Exception {

    String query = "SELECT COUNT(*) FROM " + endOfQuery(false);
    if (m_DataBaseConnection.execute(query) == false) {
      throw new Exception("Cannot count results tuples.");
    }
    ResultSet rs = m_DataBaseConnection.getResultSet();
    rs.next();
    int i = rs.getInt(1);
    rs.close();
    return i;
  }

  /**
   * Determines and returns (if possible) the structure (internally the header)
   * of the data set as an empty set of instances.
   * 
   * @return the structure of the data set as an empty set of Instances
   * @throws IOException if an error occurs
   */
  @Override
  public Instances getStructure() throws IOException {

    if (m_DataBaseConnection == null) {
      throw new IOException("No source database has been specified");
    }

    connectToDatabase();
    pseudo: try {
      if (m_pseudoIncremental && m_structure == null) {
        if (getRetrieval() == BATCH) {
          throw new IOException(
            "Cannot mix getting instances in both incremental and batch modes");
        }
        setRetrieval(NONE);
        m_datasetPseudoInc = getDataSet();
        m_structure = new Instances(m_datasetPseudoInc, 0);
        setRetrieval(NONE);
        return m_structure;
      }
      if (m_structure == null) {
        if (m_checkForTable) {
          if (!m_DataBaseConnection.tableExists(endOfQuery(true))) {
            throw new IOException(
              "Table does not exist according to metadata from JDBC driver. "
                + "If you are convinced the table exists, set 'checkForTable' "
                + "to 'False' in your DatabaseUtils.props file and try again.");
          }
        }

        // finds out which SQL statement to use for the DBMS to limit the number
        // of resulting rows to one
        int choice = 0;
        boolean rightChoice = false;
        while (!rightChoice) {
          try {
            String limitQ = limitQuery(m_query, 0, choice);
            if (m_DataBaseConnection.execute(limitQ) == false) {
              throw new IOException("Query didn't produce results");
            }
            m_choice = choice;
            rightChoice = true;
          } catch (SQLException ex) {
            choice++;
            if (choice == 3) {
              System.out
                .println("Incremental loading not supported for that DBMS. Pseudoincremental mode is used if you use incremental loading.\nAll rows are loaded into memory once and retrieved incrementally from memory instead of from the database.");
              m_pseudoIncremental = true;
              break pseudo;
            }
          }
        }

        String end = endOfQuery(false);
        ResultSet rs = m_DataBaseConnection.getResultSet();

        ResultSetMetaData md = rs.getMetaData();
        // rs.close();
        int numAttributes = md.getColumnCount();
        int[] attributeTypes = new int[numAttributes];
        m_nominalIndexes = Utils.cast(new Hashtable[numAttributes]);
        m_nominalStrings = Utils.cast(new ArrayList[numAttributes]);
        for (int i = 1; i <= numAttributes; i++) {
          switch (m_DataBaseConnection.translateDBColumnType(md
            .getColumnTypeName(i))) {
          case DatabaseConnection.STRING:

            String columnName = md.getColumnLabel(i);
            if (m_DataBaseConnection.getUpperCase()) {
              columnName = columnName.toUpperCase();
            }

            m_nominalIndexes[i - 1] = new Hashtable<String, Double>();
            m_nominalStrings[i - 1] = new ArrayList<String>();

            // fast incomplete structure for batch mode - actual
            // structure is determined by InstanceQuery in getDataSet()
            if (getRetrieval() != INCREMENTAL) {
              attributeTypes[i - 1] = Attribute.STRING;
              break;
            }
            // System.err.println("String --> nominal");
            ResultSet rs1;

            String query =
              "SELECT COUNT(DISTINCT( " + columnName + " )) FROM " + end;
            if (m_DataBaseConnection.execute(query) == true) {
              rs1 = m_DataBaseConnection.getResultSet();
              rs1.next();
              int count = rs1.getInt(1);
              rs1.close();
              // if(count > m_nominalToStringLimit ||
              // m_DataBaseConnection.execute("SELECT DISTINCT ( "+columnName+" ) FROM "+
              // end) == false){
              if (count > m_nominalToStringLimit
                || m_DataBaseConnection.execute("SELECT DISTINCT ( "
                  + columnName + " ) FROM " + end + " ORDER BY " + columnName) == false) {
                attributeTypes[i - 1] = Attribute.STRING;
                break;
              }
              rs1 = m_DataBaseConnection.getResultSet();
            } else {
              // System.err.println("Count for nominal values cannot be calculated. Attribute "+columnName+" treated as String.");
              attributeTypes[i - 1] = Attribute.STRING;
              break;
            }
            attributeTypes[i - 1] = Attribute.NOMINAL;
            stringToNominal(rs1, i);
            rs1.close();
            break;
          case DatabaseConnection.TEXT:
            // System.err.println("boolean --> string");

            columnName = md.getColumnLabel(i);
            if (m_DataBaseConnection.getUpperCase()) {
              columnName = columnName.toUpperCase();
            }

            m_nominalIndexes[i - 1] = new Hashtable<String, Double>();
            m_nominalStrings[i - 1] = new ArrayList<String>();

            // fast incomplete structure for batch mode - actual
            // structure is determined by InstanceQuery in getDataSet()
            if (getRetrieval() != INCREMENTAL) {
              attributeTypes[i - 1] = Attribute.STRING;
              break;
            }

            query = "SELECT COUNT(DISTINCT( " + columnName + " )) FROM " + end;
            if (m_DataBaseConnection.execute(query) == true) {
              rs1 = m_DataBaseConnection.getResultSet();
              stringToNominal(rs1, i);
              rs1.close();
            }
            attributeTypes[i - 1] = Attribute.STRING;
            break;
          case DatabaseConnection.BOOL:
            // System.err.println("boolean --> nominal");
            attributeTypes[i - 1] = Attribute.NOMINAL;
            m_nominalIndexes[i - 1] = new Hashtable<String, Double>();
            m_nominalIndexes[i - 1].put("false", new Double(0));
            m_nominalIndexes[i - 1].put("true", new Double(1));
            m_nominalStrings[i - 1] = new ArrayList<String>();
            m_nominalStrings[i - 1].add("false");
            m_nominalStrings[i - 1].add("true");
            break;
          case DatabaseConnection.DOUBLE:
            // System.err.println("BigDecimal --> numeric");
            attributeTypes[i - 1] = Attribute.NUMERIC;
            break;
          case DatabaseConnection.BYTE:
            // System.err.println("byte --> numeric");
            attributeTypes[i - 1] = Attribute.NUMERIC;
            break;
          case DatabaseConnection.SHORT:
            // System.err.println("short --> numeric");
            attributeTypes[i - 1] = Attribute.NUMERIC;
            break;
          case DatabaseConnection.INTEGER:
            // System.err.println("int --> numeric");
            attributeTypes[i - 1] = Attribute.NUMERIC;
            break;
          case DatabaseConnection.LONG:
            // System.err.println("long --> numeric");
            attributeTypes[i - 1] = Attribute.NUMERIC;
            break;
          case DatabaseConnection.FLOAT:
            // System.err.println("float --> numeric");
            attributeTypes[i - 1] = Attribute.NUMERIC;
            break;
          case DatabaseConnection.DATE:
            attributeTypes[i - 1] = Attribute.DATE;
            break;
          case DatabaseConnection.TIME:
            attributeTypes[i - 1] = Attribute.DATE;
            break;
          default:
            // System.err.println("Unknown column type");
            attributeTypes[i - 1] = Attribute.STRING;
          }
        }
        ArrayList<Attribute> attribInfo = new ArrayList<Attribute>();
        for (int i = 0; i < numAttributes; i++) {
          /* Fix for databases that uppercase column names */
          // String attribName = attributeCaseFix(md.getColumnName(i + 1));
          String attribName = md.getColumnLabel(i + 1);
          switch (attributeTypes[i]) {
          case Attribute.NOMINAL:
            attribInfo.add(new Attribute(attribName, m_nominalStrings[i]));
            break;
          case Attribute.NUMERIC:
            attribInfo.add(new Attribute(attribName));
            break;
          case Attribute.STRING:
            Attribute att = new Attribute(attribName, (ArrayList<String>) null);
            for (int n = 0; n < m_nominalStrings[i].size(); n++) {
              att.addStringValue(m_nominalStrings[i].get(n));
            }
            attribInfo.add(att);
            break;
          case Attribute.DATE:
            attribInfo.add(new Attribute(attribName, (String) null));
            break;
          default:
            throw new IOException("Unknown attribute type");
          }
        }
        m_structure = new Instances(endOfQuery(true), attribInfo, 0);
        // get rid of m_idColumn
        if (m_DataBaseConnection.getUpperCase()) {
          m_idColumn = m_idColumn.toUpperCase();
        }
        // System.out.println(m_structure.attribute(0).name().equals(idColumn));
        if (m_structure.attribute(0).name().equals(m_idColumn)) {
          m_oldStructure = new Instances(m_structure, 0);
          m_oldStructure.deleteAttributeAt(0);
          // System.out.println(m_structure);
        } else {
          m_oldStructure = new Instances(m_structure, 0);
        }

        if (m_DataBaseConnection.getResultSet() != null) {
          rs.close();
        }
      } else {
        if (m_oldStructure == null) {
          m_oldStructure = new Instances(m_structure, 0);
        }
      }
      m_DataBaseConnection.disconnectFromDatabase();
    } catch (Exception ex) {
      ex.printStackTrace();
      printException(ex);
    }
    return m_oldStructure;

  }

  /**
   * Return the full data set in batch mode (header and all intances at once).
   * 
   * @return the structure of the data set as an empty set of Instances
   * @throws IOException if there is no source or parsing fails
   */
  @Override
  public Instances getDataSet() throws IOException {

    if (m_DataBaseConnection == null) {
      throw new IOException("No source database has been specified");
    }
    if (getRetrieval() == INCREMENTAL) {
      throw new IOException(
        "Cannot mix getting Instances in both incremental and batch modes");
    }
    setRetrieval(BATCH);

    Instances result = null;
    checkEnv();
    try {
      InstanceQuery iq = new InstanceQuery();
      iq.initialize(m_CustomPropsFile);
      String realURL = m_URL;
      try {
        realURL = m_env.substitute(realURL);
      } catch (Exception ex) {
      }
      iq.setDatabaseURL(realURL);
      String realUser = m_User;
      try {
        realUser = m_env.substitute(realUser);
      } catch (Exception ex) {
      }
      iq.setUsername(realUser);
      String realPass = m_Password;
      try {
        realPass = m_env.substitute(realPass);
      } catch (Exception ex) {
      }
      iq.setPassword(realPass);
      String realQuery = m_query;
      try {
        realQuery = m_env.substitute(realQuery);
      } catch (Exception ex) {
      }
      iq.setQuery(realQuery);
      iq.setSparseData(m_CreateSparseData);

      result = iq.retrieveInstances();

      if (m_DataBaseConnection.getUpperCase()) {
        m_idColumn = m_idColumn.toUpperCase();
      }

      if (result.attribute(0).name().equals(m_idColumn)) {
        result.deleteAttributeAt(0);
      }

      m_structure = new Instances(result, 0);
      iq.disconnectFromDatabase();

    } catch (Exception ex) {
      printException(ex);
      StringBuffer text = new StringBuffer();
      if (m_query.equals("Select * from Results0")) {
        text.append("\n\nDatabaseLoader options:\n");
        Enumeration<Option> enumi = listOptions();

        while (enumi.hasMoreElements()) {
          Option option = enumi.nextElement();
          text.append(option.synopsis() + '\n');
          text.append(option.description() + '\n');
        }
        System.out.println(text);
      }
    }

    return result;
  }

  /**
   * Reads an instance from a database.
   * 
   * @param rs the ReusltSet to load
   * @throws Exception if instance cannot be read
   * @return an instance read from the database
   */
  private Instance readInstance(ResultSet rs) throws Exception {

    ResultSetMetaData md = rs.getMetaData();
    int numAttributes = md.getColumnCount();
    double[] vals = new double[numAttributes];
    m_structure.delete();
    for (int i = 1; i <= numAttributes; i++) {
      switch (m_DataBaseConnection.translateDBColumnType(md
        .getColumnTypeName(i))) {
      case DatabaseConnection.STRING:
        String str = rs.getString(i);
        if (rs.wasNull()) {
          vals[i - 1] = Utils.missingValue();
        } else {
          Double index = m_nominalIndexes[i - 1].get(str);
          if (index == null) {
            index =
              new Double(m_structure.attribute(i - 1).addStringValue(str));
          }
          vals[i - 1] = index.doubleValue();
        }
        break;
      case DatabaseConnection.TEXT:
        str = rs.getString(i);
        if (rs.wasNull()) {
          vals[i - 1] = Utils.missingValue();
        } else {
          Double index = m_nominalIndexes[i - 1].get(str);
          if (index == null) {
            index =
              new Double(m_structure.attribute(i - 1).addStringValue(str));
          }
          vals[i - 1] = index.doubleValue();
        }
        break;
      case DatabaseConnection.BOOL:
        boolean boo = rs.getBoolean(i);
        if (rs.wasNull()) {
          vals[i - 1] = Utils.missingValue();
        } else {
          vals[i - 1] = (boo ? 1.0 : 0.0);
        }
        break;
      case DatabaseConnection.DOUBLE:
        // BigDecimal bd = rs.getBigDecimal(i, 4);
        double dd = rs.getDouble(i);
        // Use the column precision instead of 4?
        if (rs.wasNull()) {
          vals[i - 1] = Utils.missingValue();
        } else {
          // newInst.setValue(i - 1, bd.doubleValue());
          vals[i - 1] = dd;
        }
        break;
      case DatabaseConnection.BYTE:
        byte by = rs.getByte(i);
        if (rs.wasNull()) {
          vals[i - 1] = Utils.missingValue();
        } else {
          vals[i - 1] = by;
        }
        break;
      case DatabaseConnection.SHORT:
        short sh = rs.getShort(i);
        if (rs.wasNull()) {
          vals[i - 1] = Utils.missingValue();
        } else {
          vals[i - 1] = sh;
        }
        break;
      case DatabaseConnection.INTEGER:
        int in = rs.getInt(i);
        if (rs.wasNull()) {
          vals[i - 1] = Utils.missingValue();
        } else {
          vals[i - 1] = in;
        }
        break;
      case DatabaseConnection.LONG:
        long lo = rs.getLong(i);
        if (rs.wasNull()) {
          vals[i - 1] = Utils.missingValue();
        } else {
          vals[i - 1] = lo;
        }
        break;
      case DatabaseConnection.FLOAT:
        float fl = rs.getFloat(i);
        if (rs.wasNull()) {
          vals[i - 1] = Utils.missingValue();
        } else {
          vals[i - 1] = fl;
        }
        break;
      case DatabaseConnection.DATE:
        Date date = rs.getDate(i);
        if (rs.wasNull()) {
          vals[i - 1] = Utils.missingValue();
        } else {
          // TODO: Do a value check here.
          vals[i - 1] = date.getTime();
        }
        break;
      case DatabaseConnection.TIME:
        Time time = rs.getTime(i);
        if (rs.wasNull()) {
          vals[i - 1] = Utils.missingValue();
        } else {
          // TODO: Do a value check here.
          vals[i - 1] = time.getTime();
        }
        break;
      default:
        vals[i - 1] = Utils.missingValue();
      }
    }
    Instance inst;
    if (m_CreateSparseData) {
      inst = new SparseInstance(1.0, vals);
    } else {
      inst = new DenseInstance(1.0, vals);
    }
    // get rid of m_idColumn
    if (m_DataBaseConnection.getUpperCase()) {
      m_idColumn = m_idColumn.toUpperCase();
    }
    if (m_structure.attribute(0).name().equals(m_idColumn)) {
      inst.deleteAttributeAt(0);
      m_oldStructure.add(inst);
      inst = m_oldStructure.instance(0);
      m_oldStructure.delete(0);
    } else {
      // instances is added to and deleted from the structure to get the true
      // nominal values instead of the index of the values.
      m_structure.add(inst);
      inst = m_structure.instance(0);
      m_structure.delete(0);
    }
    return inst;

  }

  /**
   * Read the data set incrementally---get the next instance in the data set or
   * returns null if there are no more instances to get. If the structure hasn't
   * yet been determined by a call to getStructure then method does so before
   * returning the next instance in the data set.
   * 
   * @param structure the dataset header information, will get updated in case
   *          of string or relational attributes
   * @return the next instance in the data set as an Instance object or null if
   *         there are no more instances to be read
   * @throws IOException if there is an error during parsing
   */
  @Override
  public Instance getNextInstance(Instances structure) throws IOException {

    m_structure = structure;

    if (m_DataBaseConnection == null) {
      throw new IOException("No source database has been specified");
    }
    if (getRetrieval() == BATCH) {
      throw new IOException(
        "Cannot mix getting Instances in both incremental and batch modes");
    }
    // pseudoInremental: Load all instances into main memory in batch mode and
    // give them incrementally to user
    if (m_pseudoIncremental) {
      setRetrieval(INCREMENTAL);
      if (m_datasetPseudoInc.numInstances() > 0) {
        Instance current = m_datasetPseudoInc.instance(0);
        m_datasetPseudoInc.delete(0);
        return current;
      } else {
        resetStructure();
        return null;
      }
    }
    // real incremental mode. At the moment(version 1.0) only for MySQL and
    // HSQLDB (Postgres not tested, should work)
    setRetrieval(INCREMENTAL);
    try {
      if (!m_DataBaseConnection.isConnected()) {
        connectToDatabase();
      }
      // if no key columns specified by user, try to detect automatically
      if (m_firstTime && m_orderBy.size() == 0) {
        if (!checkForKey()) {
          throw new Exception(
            "A unique order cannot be detected automatically.\nYou have to use SELECT * in your query to enable this feature.\nMaybe JDBC driver is not able to detect key.\nDefine primary key in your database or use -P option (command line) or enter key columns in the GUI.");
        }
      }
      if (m_firstTime) {
        m_firstTime = false;
        m_rowCount = getRowCount();
      }
      // as long as not all rows has been loaded
      if (m_counter < m_rowCount) {
        if (m_DataBaseConnection.execute(limitQuery(m_query, m_counter,
          m_choice)) == false) {
          throw new Exception("Tuple could not be retrieved.");
        }
        m_counter++;
        ResultSet rs = m_DataBaseConnection.getResultSet();
        rs.next();
        Instance current = readInstance(rs);
        rs.close();
        return current;
      } else {
        m_DataBaseConnection.disconnectFromDatabase();
        resetStructure();
        return null;
      }
    } catch (Exception ex) {
      printException(ex);
    }
    return null;
  }

  /**
   * Gets the setting
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

    options.add("-Q");
    options.add(getQuery());

    StringBuffer text = new StringBuffer();
    for (int i = 0; i < m_orderBy.size(); i++) {
      if (i > 0) {
        text.append(", ");
      }
      text.append(m_orderBy.get(i));
    }

    if (text.length() > 0) {
      options.add("-P");
      options.add(text.toString());
    }

    if (m_inc) {
      options.add("-I");
    }

    if ((m_CustomPropsFile != null) && !m_CustomPropsFile.isDirectory()) {
      options.add("-custom-props");
      options.add(m_CustomPropsFile.toString());
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Lists the available options
   * 
   * @return an enumeration of the available options
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>();

    newVector.add(new Option("\tThe JDBC URL to connect to.\n"
      + "\t(default: from DatabaseUtils.props file)", "url", 1,
      "-url <JDBC URL>"));

    newVector.add(new Option("\tThe user to connect with to the database.\n"
      + "\t(default: none)", "user", 1, "-user <name>"));

    newVector
      .add(new Option("\tThe password to connect with to the database.\n"
        + "\t(default: none)", "password", 1, "-password <password>"));

    newVector.add(new Option("\tSQL query of the form\n"
      + "\t\tSELECT <list of columns>|* FROM <table> [WHERE]\n"
      + "\tto execute.\n" + "\t(default: Select * From Results0)", "Q", 1,
      "-Q <query>"));

    newVector.add(new Option(
      "\tList of column names uniquely defining a DB row\n"
        + "\t(separated by ', ').\n" + "\tUsed for incremental loading.\n"
        + "\tIf not specified, the key will be determined automatically,\n"
        + "\tif possible with the used JDBC driver.\n"
        + "\tThe auto ID column created by the DatabaseSaver won't be loaded.",
      "P", 1, "-P <list of column names>"));

    newVector.add(new Option("\tSets incremental loading", "I", 0, "-I"));

    newVector.addElement(new Option(
      "\tReturn sparse rather than normal instances.", "S", 0, "-S"));

    newVector.add(new Option(
      "\tThe custom properties file to use instead of default ones,\n"
        + "\tcontaining the database parameters.\n" + "\t(default: none)",
      "custom-props", 1, "-custom-props <file>"));

    return newVector.elements();
  }

  /**
   * Sets the options.
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
   * -Q &lt;query&gt;
   *  SQL query of the form
   *   SELECT &lt;list of columns&gt;|* FROM &lt;table&gt; [WHERE]
   *  to execute.
   *  (default: Select * From Results0)
   * </pre>
   * 
   * <pre>
   * -P &lt;list of column names&gt;
   *  List of column names uniquely defining a DB row
   *  (separated by ', ').
   *  Used for incremental loading.
   *  If not specified, the key will be determined automatically,
   *  if possible with the used JDBC driver.
   *  The auto ID column created by the DatabaseSaver won't be loaded.
   * </pre>
   * 
   * <pre>
   * -I
   *  Sets incremental loading
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the options
   * @throws Exception if options cannot be set
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String optionString, keyString, tmpStr;

    optionString = Utils.getOption('Q', options);

    keyString = Utils.getOption('P', options);

    reset();

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

    if (optionString.length() != 0) {
      setQuery(optionString);
    }

    m_orderBy.clear();

    m_inc = Utils.getFlag('I', options);

    if (m_inc) {
      StringTokenizer st = new StringTokenizer(keyString, ",");
      while (st.hasMoreTokens()) {
        String column = st.nextToken();
        column = column.replaceAll(" ", "");
        m_orderBy.add(column);
      }
    }

    tmpStr = Utils.getOption("custom-props", options);
    if (tmpStr.length() == 0) {
      setCustomPropsFile(null);
    } else {
      setCustomPropsFile(new File(tmpStr));
    }
  }

  /**
   * Prints an exception
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
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12418 $");
  }

  /**
   * Main method.
   * 
   * @param options the options
   */
  public static void main(String[] options) {

    DatabaseLoader atf;
    try {
      atf = new DatabaseLoader();
      atf.setOptions(options);
      atf.setSource(atf.getUrl(), atf.getUser(), atf.getPassword());
      if (!atf.m_inc) {
        System.out.println(atf.getDataSet());
      } else {
        Instances structure = atf.getStructure();
        System.out.println(structure);
        Instance temp;
        do {
          temp = atf.getNextInstance(structure);
          if (temp != null) {
            System.out.println(temp);
          }
        } while (temp != null);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("\n" + e.getMessage());
    }
  }
}
