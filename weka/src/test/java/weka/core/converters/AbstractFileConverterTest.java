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
 * Copyright (C) 2006 University of Waikato 
 */

package weka.core.converters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * Abstract Test class for file converters.
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10222 $
 */
public abstract class AbstractFileConverterTest extends AbstractConverterTest {

  /** the filename used for the data in ARFF format. */
  protected String m_SourceFilename;

  /** the filename used for loading/saving in the export file format. */
  protected String m_ExportFilename;

  /** the command line options. */
  protected String[] m_CommandlineOptions;

  /** the maximum different for attribute values. */
  protected double m_MaxDiffValues;

  /** the maximum different for attribute weights. */
  protected double m_MaxDiffWeights;

  /** whether to check the header when comparing datasets. */
  protected boolean m_CheckHeader;

  /** whether to compare the attribute values as string. */
  protected boolean m_CompareValuesAsString;

  /**
   * Constructs the <code>AbstractFileConverterTest</code>. Called by
   * subclasses.
   * 
   * @param name the name of the test class
   */
  public AbstractFileConverterTest(String name) {
    super(name);
  }

  /**
   * returns a filename in ARFF format which can be used for loading and saving.
   * 
   * @return the filename
   */
  protected String getSourceFilename() {
    String result;
    File file;

    result = null;

    try {
      file = File.createTempFile("weka_core_converters", ".arff");
      file.deleteOnExit();
      result = file.getAbsolutePath();
    } catch (Exception e) {
      result = null;
    }

    return result;
  }

  /**
   * returns a filename in the export format which can be used for loading and
   * saving.
   * 
   * @return the filename
   */
  protected String getExportFilename() {
    String result;
    File file;

    result = null;

    try {
      file = File.createTempFile("weka_core_converters",
        ((FileSourcedConverter) getLoader()).getFileExtension());
      file.deleteOnExit();
      result = file.getAbsolutePath();
    } catch (Exception e) {
      result = null;
    }

    return result;
  }

  /**
   * returns the command line options, either for the loader or the saver.
   * 
   * @param loader if true the options for the loader will be returned,
   *          otherwise the ones for the saver
   * @return the command line options
   */
  protected String[] getCommandlineOptions(boolean loader) {
    if (loader) {
      return new String[] { m_ExportFilename };
    } else {
      return new String[] { "-i", m_SourceFilename, "-o", m_ExportFilename };
    }
  }

  /**
   * Compare two datasets to see if they differ.
   * 
   * @param data1 one set of instances
   * @param data2 the other set of instances
   * @throws Exception if the datasets differ
   */
  protected void compareDatasets(Instances data1, Instances data2)
    throws Exception {

    if (m_CheckHeader) {
      if (!data2.equalHeaders(data1)) {
        throw new Exception("header has been modified\n"
          + data2.equalHeadersMsg(data1));
      }
    }
    if (!(data2.numInstances() == data1.numInstances())) {
      throw new Exception("number of instances has changed");
    }
    for (int i = 0; i < data2.numInstances(); i++) {
      Instance orig = data1.instance(i);
      Instance copy = data2.instance(i);
      for (int j = 0; j < orig.numAttributes(); j++) {
        if (orig.isMissing(j)) {
          if (!copy.isMissing(j)) {
            throw new Exception("instances have changed");
          }
        } else {
          if (m_CompareValuesAsString) {
            if (!orig.toString(j).equals(copy.toString(j))) {
              throw new Exception("instances have changed");
            }
          } else {
            if (Math.abs(orig.value(j) - copy.value(j)) > m_MaxDiffValues) {
              throw new Exception("instances have changed");
            }
          }
        }
        if (Math.abs(orig.weight() - copy.weight()) > m_MaxDiffWeights) {
          throw new Exception("instance weights have changed");
        }
      }
    }
  }

  /**
   * Called by JUnit before each test method. This implementation creates the
   * default loader/saver to test and generates a test set of Instances.
   * 
   * @throws Exception if an error occurs reading the example instances.
   */
  @Override
  protected void setUp() throws Exception {
    BufferedWriter writer;

    super.setUp();

    // how to compare datasets
    // see compareDatasets(Instances,Instances)
    m_MaxDiffValues = 0.0;
    m_MaxDiffWeights = 0.0;
    m_CheckHeader = true;
    m_CompareValuesAsString = false;

    m_SourceFilename = getSourceFilename();
    m_ExportFilename = getExportFilename();

    // generate source file
    writer = new BufferedWriter(new FileWriter(m_SourceFilename));
    writer.write(m_Instances.toString());
    writer.newLine();
    writer.flush();
    writer.close();
  }

  /**
   * Called by JUnit after each test method.
   * 
   * @throws Exception if fails
   */
  @Override
  protected void tearDown() throws Exception {
    File file;

    // delete temp. files
    file = new File(m_SourceFilename);
    if (file.exists()) {
      file.delete();
    }
    file = new File(m_ExportFilename);
    if (file.exists()) {
      file.delete();
    }

    m_SourceFilename = null;
    m_ExportFilename = null;

    super.tearDown();
  }

  /**
   * test the batch saving/loading (via setFile(File)).
   */
  public void testBatch() {
    Instances data;

    try {
      // save
      m_Saver.setInstances(m_Instances);
      m_Saver.setFile(new File(m_ExportFilename));
      m_Saver.writeBatch();

      // load
      ((AbstractFileLoader) m_Loader).setFile(new File(m_ExportFilename));
      data = m_Loader.getDataSet();

      // compare data
      try {
        if (m_Instances.classIndex() != data.classIndex()) {
          data.setClassIndex(m_Instances.classIndex());
        }
        compareDatasets(m_Instances, data);
      } catch (Exception e) {
        fail("Incremental load failed (datasets differ): " + e.toString());
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail("Batch save/load failed: " + e.toString());
    }
  }

  /**
   * test the incremental loading (via setFile(File)).
   */
  public void testIncrementalLoader() {
    Instance temp;
    Instances data;

    if (!(getLoader() instanceof IncrementalConverter)) {
      return;
    }

    try {
      // save
      m_Saver.setInstances(m_Instances);
      m_Saver.setFile(new File(m_ExportFilename));
      m_Saver.writeBatch();

      // load
      ((AbstractFileLoader) m_Loader).setFile(new File(m_ExportFilename));
      data = new Instances(m_Loader.getStructure());
      do {
        temp = m_Loader.getNextInstance(data);
        if (temp != null) {
          data.add(temp);
        }
      } while (temp != null);

      // compare data
      try {
        if (m_Instances.classIndex() != data.classIndex()) {
          data.setClassIndex(m_Instances.classIndex());
        }
        compareDatasets(m_Instances, data);
      } catch (Exception e) {
        fail("Incremental load failed (datasets differ): " + e.toString());
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail("Incremental load failed: " + e.toString());
    }
  }

  /**
   * test the incremental save (via setFile(File)).
   */
  public void testIncrementalSaver() {
    int i;
    File file;

    if (!(getSaver() instanceof IncrementalConverter)) {
      return;
    }

    try {
      // remove output file if it exists
      file = new File(m_ExportFilename);
      if (file.exists()) {
        file.delete();
      }

      // save
      m_Saver.setFile(new File(m_ExportFilename));
      m_Saver.setRetrieval(AbstractSaver.INCREMENTAL);
      m_Saver.setStructure(new Instances(m_Instances, 0));
      for (i = 0; i < m_Instances.numInstances(); i++) {
        m_Saver.writeIncremental(m_Instances.instance(i));
      }
      m_Saver.writeIncremental(null);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Incremental save failed: " + e.toString());
    }
  }

  /**
   * tests the commandline operation of the loader (via setFile(File)). does
   * nothing currently, but can be overridden by derived classes.
   */
  public void testLoaderCommandlineArgs() {
  }

  /**
   * tests the commandline operation of the saver.
   */
  public void testSaverCommandlineArgs() {
    String[] options;

    options = getCommandlineOptions(false);

    try {
      ((OptionHandler) m_Saver).setOptions(options);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Command line test failed ('" + Utils.arrayToString(options)
        + "'): " + e.toString());
    }
  }

  /**
   * tests whether a URL can be loaded (via setURL(URL)).
   */
  public void testURLSourcedLoader() {
    Instances data;

    if (!(getLoader() instanceof URLSourcedLoader)) {
      return;
    }

    try {
      // save
      m_Saver.setInstances(m_Instances);
      m_Saver.setFile(new File(m_ExportFilename));
      m_Saver.writeBatch();

      // load
      ((URLSourcedLoader) m_Loader).setURL(new File(m_ExportFilename).toURI()
        .toURL().toString());
      data = m_Loader.getDataSet();

      // compare data
      try {
        if (m_Instances.classIndex() != data.classIndex()) {
          data.setClassIndex(m_Instances.classIndex());
        }
        compareDatasets(m_Instances, data);
      } catch (Exception e) {
        fail("URL load failed (datasets differ): " + e.toString());
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail("URL load failed: " + e.toString());
    }
  }

  /**
   * tests whether data can be loaded via setSource() with a file stream.
   */
  public void testLoaderWithStream() {
    Instances data;

    try {
      // save
      m_Saver.setInstances(m_Instances);
      m_Saver.setFile(new File(m_ExportFilename));
      m_Saver.writeBatch();

      // load
      m_Loader.setSource(new FileInputStream(new File(m_ExportFilename)));
      data = m_Loader.getDataSet();

      // compare data
      try {
        if (m_Instances.classIndex() != data.classIndex()) {
          data.setClassIndex(m_Instances.classIndex());
        }
        compareDatasets(m_Instances, data);
      } catch (Exception e) {
        fail("File stream loading failed (datasets differ): " + e.toString());
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail("File stream loading failed: " + e.toString());
    }
  }
}
