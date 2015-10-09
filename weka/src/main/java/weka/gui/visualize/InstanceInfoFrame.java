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
 * InstanceInfoFrame.java
 * Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.visualize;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import weka.core.Instances;

/**
 * Frame for displaying information on the displayed data.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class InstanceInfoFrame
  extends JFrame
  implements InstanceInfo {

  /** for serialization. */
  private static final long serialVersionUID = 4684184733677263009L;

  /** the underlying data. */
  protected Vector<Instances> m_Data;
  
  /** the text area for displaying the info. */
  protected JTextArea m_TextInfo;
  
  /**
   * Initializes the frame.
   */
  public InstanceInfoFrame() {
    super("Weka: Instance info");
    
    initialize();
    initGUI();
    initFinished();
  }
  
  /**
   * Initializes member variables.
   */
  protected void initialize() {
    m_Data = new Vector<Instances>();
  }
  
  /**
   * Sets up the GUI components.
   */
  protected void initGUI() {
    getContentPane().setLayout(new BorderLayout());
    
    m_TextInfo = new JTextArea();
    m_TextInfo.setEditable(false);
    m_TextInfo.setFont(new Font("Monospaced", Font.PLAIN,12));
    getContentPane().add(new JScrollPane(m_TextInfo), BorderLayout.CENTER);
    
    pack();
    setSize(320, 400);
  }
  
  /**
   * A hook method after initialize() and initGUI have been called.
   */
  protected void initFinished() {
  }
  
  /**
   * Sets the text to display.
   * 
   * @param text	the text to display
   */
  public void setInfoText(String text) {
    m_TextInfo.setText(text);
  }
  
  /**
   * Returns the currently displayed info text.
   * 
   * @return		the info text
   */
  public String getInfoText() {
    return m_TextInfo.getText();
  }
  
  /**
   * Sets the underlying data.
   * 
   * @param data	the data of the info text
   */
  public void setInfoData(Vector<Instances> data) {
    m_Data = new Vector<Instances>();
    if (data != null)
      m_Data.addAll(data);
  }
  
  /**
   * Returns the underlying data.
   * 
   * @return		the data of the info text, can be null
   */
  public Vector<Instances> getInfoData() {
    return m_Data;
  }
}
