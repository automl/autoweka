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
 * TextSaver.java
 * 
 * Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 */
package weka.gui.beans;

import java.awt.BorderLayout;
import java.beans.EventSetDescriptor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;

import javax.swing.JPanel;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.gui.Logger;

/**
 * Simple component to save the text carried in text events out to a file
 * 
 * @author thuvh (thuvh87{[at]}gmail{[dot]}com)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 9250 $
 */
@KFStep(category = "DataSinks", toolTipText = "Save text output to a file")
public class TextSaver extends JPanel implements TextListener, BeanCommon,
    Visible, Serializable, EnvironmentHandler {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 6363577506969809332L;

  /**
   * Default visual for data sources
   */
  protected BeanVisual m_visual = new BeanVisual("TextSaver",
      BeanVisual.ICON_PATH + "DefaultText.gif", BeanVisual.ICON_PATH
          + "DefaultText_animated.gif");

  /**
   * The log for this bean
   */
  protected transient weka.gui.Logger m_logger = null;

  /**
   * The environment variables.
   */
  protected transient Environment m_env;

  /** The file to save to */
  protected String m_fileName;

  /** Whether to append to the file or not */
  protected boolean m_append = true;

  /**
   * Global info for this bean
   * 
   * @return a <code>String</code> value
   */
  public String globalInfo() {
    return "Save/append static text to a file.";
  }

  /**
   * Default constructors a new TextSaver
   */
  public TextSaver() {
    useDefaultVisual();
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);

    m_env = Environment.getSystemWide();
  }

  /**
   * Set the filename to save to
   * 
   * @param filename the filename to save to
   */
  public void setFilename(String filename) {
    m_fileName = filename;
  }

  /**
   * Get the filename to save to
   * 
   * @return the filename to save to
   */
  public String getFilename() {
    return m_fileName;
  }

  public void setAppend(boolean append) {
    m_append = append;
  }

  public boolean getAppend() {
    return m_append;
  }

  /**
   * Set environment variables to use
   * 
   * @param env the environment variables to use
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "DefaultText.gif",
        BeanVisual.ICON_PATH + "DefaultText_animated.gif");
    m_visual.setText("TextSaver");
  }

  @Override
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  @Override
  public BeanVisual getVisual() {
    return m_visual;
  }

  @Override
  public void setCustomName(String name) {
    m_visual.setText(name);
  }

  @Override
  public String getCustomName() {
    return m_visual.getText();
  }

  @Override
  public void stop() {
  }

  @Override
  public boolean isBusy() {
    return false;
  }

  @Override
  public void setLog(Logger logger) {
    m_logger = logger;
  }

  @Override
  public boolean connectionAllowed(EventSetDescriptor esd) {
    return connectionAllowed(esd.getName());
  }

  @Override
  public boolean connectionAllowed(String eventName) {
    return true;
  }

  @Override
  public void connectionNotification(String eventName, Object source) {
  }

  @Override
  public void disconnectionNotification(String eventName, Object source) {
  }

  /**
   * Accept and process an TextEvent
   * 
   * @param textEvent the TextEvent to process
   */
  @Override
  public synchronized void acceptText(TextEvent textEvent) {
    String content = textEvent.getText();

    if (m_fileName != null && m_fileName.length() > 0) {
      if (m_env == null) {
        m_env = Environment.getSystemWide();
      }
      String filename = m_fileName;
      try {
        filename = m_env.substitute(m_fileName);
      } catch (Exception ex) {
      }

      // append .txt if necessary
      if (filename.toLowerCase().indexOf(".txt") < 0) {
        filename += ".txt";
      }

      File file = new File(filename);
      if (!file.isDirectory()) {
        BufferedWriter writer = null;

        try {
          writer = new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream(file, m_append), "utf-8"));
          writer.write(content);
          writer.close();
        } catch (IOException e) {
          if (m_logger != null) {
            m_logger.statusMessage(statusMessagePrefix() + "WARNING: "
                + "an error occurred whilte trying to write text (see log)");
            m_logger.logMessage("[" + getCustomName() + "] "
                + "an error occurred whilte trying to write text: "
                + e.getMessage());
          } else {
            e.printStackTrace();
          }
        }

      } else {
        String message = "Can't write text to file because supplied filename"
            + " is a directory!";
        if (m_logger != null) {
          m_logger.statusMessage(statusMessagePrefix() + "WARNING: " + message);
          m_logger.logMessage("[" + getCustomName() + "] " + message);
        }
      }

    } else {
      String message = "Can't write text because no file has been supplied is a directory!";
      if (m_logger != null) {
        m_logger.statusMessage(statusMessagePrefix() + "WARNING: " + message);
        m_logger.logMessage("[" + getCustomName() + "] " + message);
      }
    }
  }

  private String statusMessagePrefix() {
    return getCustomName() + "$" + hashCode() + "|";
  }
}
