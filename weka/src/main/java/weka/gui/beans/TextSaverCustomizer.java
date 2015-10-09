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
 * TextSaverCustomizer.java
 * 
 * Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 */
package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import weka.core.Environment;
import weka.core.EnvironmentHandler;

/**
 * Customizer for the TextSaver component.
 * 
 * @author thuvh (thuvh87{[at]}gmail{[dot]}com)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 9238 $
 */
public class TextSaverCustomizer extends JPanel implements BeanCustomizer,
    EnvironmentHandler, CustomizerClosingListener, CustomizerCloseRequester {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -1012433373647714743L;

  private TextSaver m_textSaver;

  private FileEnvironmentField m_fileEditor;

  private final JCheckBox m_append = new JCheckBox("Append to file");

  private Environment m_env = Environment.getSystemWide();

  private ModifyListener m_modifyListener;

  private Window m_parent;

  private String m_fileBackup;

  /**
   * Default Constructor
   */
  public TextSaverCustomizer() {
    setLayout(new BorderLayout());

  }

  /**
   * Set the TextSaver object to customize.
   * 
   * @param object the TextSaver to customize
   */
  @Override
  public void setObject(Object object) {
    m_textSaver = (TextSaver) object;
    m_fileBackup = m_textSaver.getFilename();
    m_append.setSelected(m_textSaver.getAppend());

    setup();
  }

  private void setup() {
    JPanel holder = new JPanel();
    holder.setLayout(new BorderLayout());

    m_fileEditor = new FileEnvironmentField("Filename", m_env,
        JFileChooser.SAVE_DIALOG);
    m_fileEditor.resetFileFilters();
    JPanel temp = new JPanel();
    temp.setLayout(new GridLayout(2, 0));
    temp.add(m_fileEditor);
    temp.add(m_append);

    holder.add(temp, BorderLayout.SOUTH);

    String globalInfo = m_textSaver.globalInfo();

    JTextArea jt = new JTextArea();
    jt.setColumns(30);
    jt.setFont(new Font("SansSerif", Font.PLAIN, 12));
    jt.setEditable(false);
    jt.setLineWrap(true);
    jt.setWrapStyleWord(true);
    jt.setText(globalInfo);
    jt.setBackground(getBackground());
    JPanel jp = new JPanel();
    jp.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("About"),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    jp.setLayout(new BorderLayout());
    jp.add(jt, BorderLayout.CENTER);

    holder.add(jp, BorderLayout.NORTH);

    add(holder, BorderLayout.CENTER);

    addButtons();

    m_fileEditor.setText(m_textSaver.getFilename());
  }

  private void addButtons() {
    JButton okBut = new JButton("OK");
    JButton cancelBut = new JButton("Cancel");

    JPanel butHolder = new JPanel();
    butHolder.setLayout(new GridLayout(1, 2));
    butHolder.add(okBut);
    butHolder.add(cancelBut);
    add(butHolder, BorderLayout.SOUTH);

    okBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_textSaver.setFilename(m_fileEditor.getText());
        m_textSaver.setAppend(m_append.isSelected());

        if (m_modifyListener != null) {
          m_modifyListener.setModifiedStatus(TextSaverCustomizer.this, true);
        }
        if (m_parent != null) {
          m_parent.dispose();
        }
      }
    });

    cancelBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {

        customizerClosing();
        if (m_parent != null) {
          m_parent.dispose();
        }
      }
    });
  }

  /**
   * Set the environment variables to use
   * 
   * @param env the environment variables to use
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Set a listener interested in whether we've modified the TextSaver that
   * we're customizing
   * 
   * @param l the listener
   */
  @Override
  public void setModifiedListener(ModifyListener l) {
    m_modifyListener = l;
  }

  /**
   * Set the parent window of this dialog
   * 
   * @param parent the parent window
   */
  @Override
  public void setParentWindow(Window parent) {
    m_parent = parent;
  }

  /**
   * Gets called if the use closes the dialog via the close widget on the window
   * - is treated as cancel, so restores the TextSaver to its previous state.
   */
  @Override
  public void customizerClosing() {
    m_textSaver.setFilename(m_fileBackup);
  }
}
