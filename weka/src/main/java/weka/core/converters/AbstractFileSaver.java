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
 *    AbstractFileSaver.java
 *    Copyright (C) 2004-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * Abstract class for Savers that save to a file
 * 
 * Valid options are:
 * 
 * -i input arff file <br>
 * The input filw in arff format.
 * <p>
 * 
 * -o the output file <br>
 * The output file. The prefix of the output file is sufficient. If no output
 * file is given, Saver tries to use standard out.
 * <p>
 * 
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 12074 $
 */
public abstract class AbstractFileSaver extends AbstractSaver implements
  OptionHandler, FileSourcedConverter, EnvironmentHandler {

  /** ID to avoid warning */
  private static final long serialVersionUID = 2399441762235754491L;

  /** The destination file. */
  private File m_outputFile;

  /** The writer. */
  private transient BufferedWriter m_writer;

  /** The file extension of the destination file. */
  private String FILE_EXTENSION;

  /** the extension for compressed files */
  private final String FILE_EXTENSION_COMPRESSED = ".gz";

  /** The prefix for the filename (chosen in the GUI). */
  private String m_prefix;

  /** The directory of the file (chosen in the GUI). */
  private String m_dir;

  /**
   * Counter. In incremental mode after reading 100 instances they will be
   * written to a file.
   */
  protected int m_incrementalCounter;

  /** use relative file paths */
  protected boolean m_useRelativePath = false;

  /** Environment variables */
  protected transient Environment m_env;

  /**
   * resets the options
   * 
   */
  @Override
  public void resetOptions() {

    super.resetOptions();
    m_outputFile = null;
    m_writer = null;
    m_prefix = "";
    m_dir = "";
    m_incrementalCounter = 0;
  }

  /**
   * Gets the writer
   * 
   * @return the BufferedWriter
   */
  public BufferedWriter getWriter() {

    return m_writer;
  }

  /** Sets the writer to null. */
  public void resetWriter() {

    m_writer = null;
  }

  /**
   * Gets ihe file extension.
   * 
   * @return the file extension as a string.
   */
  @Override
  public String getFileExtension() {

    return FILE_EXTENSION;
  }

  /**
   * Gets all the file extensions used for this type of file
   * 
   * @return the file extensions
   */
  @Override
  public String[] getFileExtensions() {
    return new String[] { getFileExtension() };
  }

  /**
   * Sets ihe file extension.
   * 
   * @param ext the file extension as a string startin with '.'.
   */
  protected void setFileExtension(String ext) {

    FILE_EXTENSION = ext;
  }

  /**
   * Gets the destination file.
   * 
   * @return the destination file.
   */
  @Override
  public File retrieveFile() {

    return m_outputFile;
  }

  /**
   * Sets the destination file.
   * 
   * @param outputFile the destination file.
   * @throws IOException throws an IOException if file cannot be set
   */
  @Override
  public void setFile(File outputFile) throws IOException {

    m_outputFile = outputFile;
    setDestination(outputFile);

  }

  /**
   * Sets the file name prefix
   * 
   * @param prefix the file name prefix
   */
  @Override
  public void setFilePrefix(String prefix) {

    m_prefix = prefix;
  }

  /**
   * Gets the file name prefix
   * 
   * @return the prefix of the filename
   */
  @Override
  public String filePrefix() {

    return m_prefix;
  }

  /**
   * Sets the directory where the instances should be stored
   * 
   * @param dir a string containing the directory path and name
   */
  @Override
  public void setDir(String dir) {

    m_dir = dir;
  }

  /**
   * Gets the directory
   * 
   * @return a string with the file name
   */
  @Override
  public String retrieveDir() {

    return m_dir;
  }

  /**
   * Set the environment variables to use.
   * 
   * @param env the environment variables to use
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
    if (m_outputFile != null) {
      try {
        // try and resolve any new environment variables
        setFile(m_outputFile);
      } catch (IOException ex) {
        // we won't complain about it here...
      }
    }
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector =
      Option.listOptionsForClassHierarchy(this.getClass(),
        AbstractFileSaver.class);
    // Vector<Option> newVector = new Vector<Option>();

    newVector.addElement(new Option("\tThe input file", "i", 1,
      "-i <the input file>"));

    newVector.addElement(new Option("\tThe output file", "o", 1,
      "-o <the output file>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid option is:
   * <p>
   * 
   * -i input arff file <br>
   * The input filw in arff format.
   * <p>
   * 
   * -o the output file <br>
   * The output file. The prefix of the output file is sufficient. If no output
   * file is given, Saver tries to use standard out.
   * <p>
   * 
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    Option.setOptionsForHierarchy(options, this, AbstractFileSaver.class);
    String outputString = Utils.getOption('o', options);
    String inputString = Utils.getOption('i', options);

    ArffLoader loader = new ArffLoader();

    resetOptions();

    if (inputString.length() != 0) {
      try {
        File input = new File(inputString);
        loader.setFile(input);
        setInstances(loader.getDataSet());
      } catch (Exception ex) {
        ex.printStackTrace();
        throw new IOException(
          "No data set loaded. Data set has to be in ARFF format.");
      }
    }
    if (outputString.length() != 0) {
      boolean validExt = false;
      for (String ext : getFileExtensions()) {
        if (outputString.endsWith(ext)) {
          validExt = true;
          break;
        }
      }
      // add appropriate file extension
      if (!validExt) {
        if (outputString.lastIndexOf('.') != -1) {
          outputString =
            (outputString.substring(0, outputString.lastIndexOf('.')))
              + FILE_EXTENSION;
        } else {
          outputString = outputString + FILE_EXTENSION;
        }
      }
      try {
        File output = new File(outputString);
        setFile(output);
      } catch (Exception ex) {
        throw new IOException("Cannot create output file (Reason: "
          + ex.toString() + "). Standard out is used.");
      }
    }
  }

  /**
   * Gets the current settings of the Saver object.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    Vector<String> result;

    result = new Vector<String>();
    for (String s : Option
      .getOptionsForHierarchy(this, AbstractFileSaver.class)) {
      result.add(s);
    }

    if (m_outputFile != null) {
      result.add("-o");
      result.add("" + m_outputFile);
    }

    if (getInstances() != null) {
      result.add("-i");
      result.add("" + getInstances().relationName());
    }

    return result.toArray(new String[result.size()]);
  }

  /** Cancels the incremental saving process. */
  @Override
  public void cancel() {

    if (getWriteMode() == CANCEL) {
      if (m_outputFile != null && m_outputFile.exists()) {
        if (m_outputFile.delete()) {
          System.out.println("File deleted.");
        }
      }
      resetOptions();
    }
  }

  /**
   * Sets the destination file (and directories if necessary).
   * 
   * @param file the File
   * @exception IOException always
   */
  @Override
  public void setDestination(File file) throws IOException {

    boolean success = false;
    String tempOut = file.getPath();
    try {
      if (m_env == null) {
        m_env = Environment.getSystemWide();
      }
      tempOut = m_env.substitute(tempOut);
    } catch (Exception ex) {
      // don't complain about it here...
      // throw new IOException("[AbstractFileSaver]: " + ex.getMessage());
    }
    file = new File(tempOut);
    String out = file.getAbsolutePath();
    if (m_outputFile != null) {
      try {
        if (file.exists()) {
          if (!file.delete()) {
            throw new IOException("File already exists.");
          }
        }
        if (out.lastIndexOf(File.separatorChar) == -1) {
          success = file.createNewFile();
        } else {
          String outPath =
            out.substring(0, out.lastIndexOf(File.separatorChar));
          File dir = new File(outPath);
          if (dir.exists()) {
            success = file.createNewFile();
          } else {
            dir.mkdirs();
            success = file.createNewFile();
          }
        }
        if (success) {
          if (m_useRelativePath) {
            try {
              m_outputFile = Utils.convertToRelativePath(file);
            } catch (Exception e) {
              m_outputFile = file;
            }
          } else {
            m_outputFile = file;
          }
          setDestination(new FileOutputStream(m_outputFile));
        }
      } catch (Exception ex) {
        throw new IOException("Cannot create a new output file (Reason: "
          + ex.toString() + "). Standard out is used.");
      } finally {
        if (!success) {
          System.err
            .println("Cannot create a new output file. Standard out is used.");
          m_outputFile = null; // use standard out
        }
      }
    }
  }

  /**
   * Sets the destination output stream.
   * 
   * @param output the output stream.
   * @throws IOException throws an IOException if destination cannot be set
   */
  @Override
  public void setDestination(OutputStream output) throws IOException {

    m_writer = new BufferedWriter(new OutputStreamWriter(output));
  }

  /**
   * Sets the directory and the file prefix. This method is used in the
   * KnowledgeFlow GUI
   * 
   * @param relationName the name of the relation to save
   * @param add additional string which should be part of the filename
   */
  @Override
  public void setDirAndPrefix(String relationName, String add) {

    try {
      if (m_dir.equals("")) {
        setDir(System.getProperty("user.dir"));
      }
      if (m_prefix.equals("")) {
        if (relationName.length() == 0) {
          throw new IOException("[Saver] Empty filename!!");
        }
        String concat =
          (m_dir + File.separator + relationName + add + FILE_EXTENSION);
        if (!concat.toLowerCase().endsWith(FILE_EXTENSION)
          && !concat.toLowerCase().endsWith(
            FILE_EXTENSION + FILE_EXTENSION_COMPRESSED)) {
          concat += FILE_EXTENSION;
        }
        setFile(new File(concat));
      } else {
        if (relationName.length() > 0) {
          relationName = "_" + relationName;
        }
        String concat =
          (m_dir + File.separator + m_prefix + relationName + add);
        if (!concat.toLowerCase().endsWith(FILE_EXTENSION)
          && !concat.toLowerCase().endsWith(
            FILE_EXTENSION + FILE_EXTENSION_COMPRESSED)) {
          concat += FILE_EXTENSION;
        }
        setFile(new File(concat));
      }
    } catch (Exception ex) {
      System.err
        .println("File prefix and/or directory could not have been set.");
      ex.printStackTrace();
    }
  }

  /**
   * to be pverridden
   * 
   * @return the file type description.
   */
  @Override
  public abstract String getFileDescription();

  /**
   * Tip text suitable for displaying int the GUI
   * 
   * @return a description of this property as a String
   */
  public String useRelativePathTipText() {
    return "Use relative rather than absolute paths";
  }

  /**
   * Set whether to use relative rather than absolute paths
   * 
   * @param rp true if relative paths are to be used
   */
  @Override
  public void setUseRelativePath(boolean rp) {
    m_useRelativePath = rp;
  }

  /**
   * Gets whether relative paths are to be used
   * 
   * @return true if relative paths are to be used
   */
  @Override
  public boolean getUseRelativePath() {
    return m_useRelativePath;
  }

  /**
   * generates a string suitable for output on the command line displaying all
   * available options.
   * 
   * @param saver the saver to create the option string for
   * @return the option string
   */
  protected static String makeOptionStr(AbstractFileSaver saver) {
    StringBuffer result;
    Option option;

    result = new StringBuffer();

    // build option string
    result.append("\n");
    result.append(saver.getClass().getName().replaceAll(".*\\.", ""));
    result.append(" options:\n\n");
    Enumeration<Option> enm = saver.listOptions();
    while (enm.hasMoreElements()) {
      option = enm.nextElement();
      result.append(option.synopsis() + "\n");
      result.append(option.description() + "\n");
    }

    return result.toString();
  }

  /**
   * runs the given saver with the specified options
   * 
   * @param saver the saver to run
   * @param options the commandline options
   */
  public static void runFileSaver(AbstractFileSaver saver, String[] options) {
    // help request?
    try {
      String[] tmpOptions = options.clone();
      if (Utils.getFlag('h', tmpOptions)) {
        System.err.println("\nHelp requested\n" + makeOptionStr(saver));
        return;
      }
    } catch (Exception e) {
      // ignore it
    }

    try {
      // set options
      try {
        saver.setOptions(options);
      } catch (Exception ex) {
        System.err.println(makeOptionStr(saver));
        System.exit(1);
      }

      saver.writeBatch();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
