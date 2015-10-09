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
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.core.converters;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests C45Loader/C45Saver. Run from the command line with:<p/>
 * java weka.core.converters.C45Test
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class C45Test 
  extends AbstractFileConverterTest {

  /** the name of the data file */
  protected String m_ExportFilenameData;
  
  /**
   * Constructs the <code>C45Test</code>.
   *
   * @param name the name of the test class
   */
  public C45Test(String name) { 
    super(name);  
  }

  /**
   * returns the loader used in the tests
   * 
   * @return the configured loader
   */
  public AbstractLoader getLoader() {
    return new C45Loader();
  }

  /**
   * returns the saver used in the tests
   * 
   * @return the configured saver
   */
  public AbstractSaver getSaver() {
    return new C45Saver();
  }
  
  /**
   * returns the filename for the data file.
   * 
   * @return the filename
   */
  protected String getExportFilenameData() {
    return m_ExportFilename.replaceAll("\\.names", ".data");
  }
  
  /**
   * returns the command line options, either for the loader or the saver
   * 
   * @param loader	if true the options for the loader will be returned,
   * 			otherwise the ones for the saver
   * @return		the command line options
   */
  protected String[] getCommandlineOptions(boolean loader) {
    if (loader)
      return super.getCommandlineOptions(loader);
    else
      return new String[]{"-i", m_SourceFilename, "-o", m_ExportFilename, "-c", "last"};
  }
  
  /**
   * Called by JUnit before each test method.
   *
   * @throws Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    File 	file;

    super.setUp();
    
    m_ExportFilenameData = getExportFilenameData();

    // delete temp. files
    file = new File(m_ExportFilenameData);
    if (file.exists())
      file.delete();
  }

  /** 
   * Called by JUnit after each test method
   */
  protected void tearDown() throws Exception {
    File 	file;

    // delete temp. files
    file = new File(m_ExportFilenameData);
    if (file.exists())
      file.delete();
    
    m_ExportFilenameData = null;
    
    super.tearDown();
  }
  
  /**
   * ignored, since not supported!
   */
  public void testLoaderWithStream() {
    System.out.println("testLoaderWithStream is ignored!");
  }

  /**
   * returns a test suite
   * 
   * @return the test suite
   */
  public static Test suite() {
    return new TestSuite(C45Test.class);
  }

  /**
   * for running the test from commandline
   * 
   * @param args the commandline arguments - ignored
   */
  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}

