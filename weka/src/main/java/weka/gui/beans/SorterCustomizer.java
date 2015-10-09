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
 *    SorterCustomizer.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instances;
import weka.gui.JListHelper;
import weka.gui.PropertySheetPanel;

/**
 * Customizer for the Sorter step
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 8051 $
 *
 */
public class SorterCustomizer extends JPanel implements BeanCustomizer,
    EnvironmentHandler, CustomizerCloseRequester {
  
  /** For serialization */
  private static final long serialVersionUID = -4860246697276275408L;

  /** The Sorter we are editing */
  protected Sorter m_sorter;
  
  protected Environment m_env = Environment.getSystemWide();
  protected ModifyListener m_modifyL = null;
  
  protected JComboBox m_attCombo = new JComboBox();
  protected JComboBox m_descending = new JComboBox();
  protected EnvironmentField m_buffSize;
  protected FileEnvironmentField m_tempDir;
  
  protected Window m_parent;
  
  protected JList m_list = new JList();
  protected DefaultListModel m_listModel;
  
  protected JButton m_newBut = new JButton("New");
  protected JButton m_deleteBut = new JButton("Delete");
  protected JButton m_upBut = new JButton("Move up");
  protected JButton m_downBut = new JButton("Move down");
  
  protected PropertySheetPanel m_tempEditor =
    new PropertySheetPanel();
  
  /**
   * Constructor 
   */
  public SorterCustomizer() {
    setLayout(new BorderLayout());
  }
  
  private void setup() {
    JPanel aboutAndControlHolder = new JPanel();
    aboutAndControlHolder.setLayout(new BorderLayout());
    
    JPanel controlHolder = new JPanel();
    controlHolder.setLayout(new BorderLayout());
    JPanel fieldHolder = new JPanel();
    fieldHolder.setLayout(new GridLayout(0,2));
    JPanel attListP = new JPanel();
    attListP.setLayout(new BorderLayout());
    attListP.setBorder(BorderFactory.createTitledBorder("Sort on attribute"));
    attListP.add(m_attCombo, BorderLayout.CENTER);
    m_attCombo.setEditable(true);
    //m_attCombo.setFocusable();
    //m_attCombo.getEditor().getEditorComponent().setFocusable(true);
    m_attCombo.setToolTipText("<html>Accepts an attribute name, index or <br> "
        + "the special string \"/first\" and \"/last\"</html>");
    
    m_descending.addItem("No");
    m_descending.addItem("Yes");
    JPanel descendingP = new JPanel();
    descendingP.setLayout(new BorderLayout());
    descendingP.setBorder(BorderFactory.createTitledBorder("Sort descending"));
    descendingP.add(m_descending, BorderLayout.CENTER);
    
    fieldHolder.add(attListP); fieldHolder.add(descendingP);
    controlHolder.add(fieldHolder, BorderLayout.NORTH);
    
    JPanel otherControls = new JPanel();
    otherControls.setLayout(new GridLayout(0,2));
    JLabel bufferSizeLab = new JLabel("Size of in-mem streaming buffer", SwingConstants.RIGHT);
    bufferSizeLab.setToolTipText("<html>Number of instances to sort in memory " +
    		"<br>before writing to a temp file " +
    		"<br>(instance connections only).</html>");
    otherControls.add(bufferSizeLab);
    m_buffSize = new EnvironmentField(m_env);
    otherControls.add(m_buffSize);
    
    JLabel tempDirLab = new JLabel("Directory for temp files", SwingConstants.RIGHT);
    tempDirLab.setToolTipText("Will use system tmp dir if left blank");
    otherControls.add(tempDirLab);
    m_tempDir = new FileEnvironmentField("", m_env, JFileChooser.OPEN_DIALOG, true);
    m_tempDir.resetFileFilters();

    otherControls.add(m_tempDir);
    
    controlHolder.add(otherControls, BorderLayout.SOUTH);
    
    aboutAndControlHolder.add(controlHolder, BorderLayout.SOUTH);
    
    JPanel aboutP = m_tempEditor.getAboutPanel();
    aboutAndControlHolder.add(aboutP, BorderLayout.NORTH);
    add(aboutAndControlHolder, BorderLayout.NORTH);
    
    m_list.setVisibleRowCount(5);
    m_deleteBut.setEnabled(false);
    JPanel listPanel = new JPanel();
    listPanel.setLayout(new BorderLayout());
    JPanel butHolder = new JPanel();
    butHolder.setLayout(new GridLayout(1, 0));
    butHolder.add(m_newBut); butHolder.add(m_deleteBut);
    butHolder.add(m_upBut); butHolder.add(m_downBut);
    m_upBut.setEnabled(false); m_downBut.setEnabled(false);
    
    listPanel.add(butHolder, BorderLayout.NORTH);
    JScrollPane js = new JScrollPane(m_list);
    js.setBorder(BorderFactory.
        createTitledBorder("Sort-by list (rows applied in order)"));
    listPanel.add(js, BorderLayout.CENTER);
    add(listPanel, BorderLayout.CENTER);
    
    addButtons();
    
    m_list.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          if (!m_deleteBut.isEnabled()) {
            m_deleteBut.setEnabled(true);
          }
          
          Object entry = m_list.getSelectedValue();
          if (entry != null) {
            Sorter.SortRule m = (Sorter.SortRule)entry;
            m_attCombo.setSelectedItem(m.getAttribute());
            if (m.getDescending()) {
              m_descending.setSelectedIndex(1);
            } else {
              m_descending.setSelectedIndex(0);
            }
          }
        }
      }
    });
    
    m_newBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Sorter.SortRule m =
          new Sorter.SortRule();
        
        String att = (m_attCombo.getSelectedItem() != null) 
          ? m_attCombo.getSelectedItem().toString() : "";
        m.setAttribute(att);
        m.setDescending(m_descending.getSelectedIndex() == 1);        
        
        m_listModel.addElement(m);
        
        if (m_listModel.size() > 1) {
          m_upBut.setEnabled(true);
          m_downBut.setEnabled(true);
        }
        
        m_list.setSelectedIndex(m_listModel.size() - 1);
      }
    });
    
    m_deleteBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int selected = m_list.getSelectedIndex();
        if (selected >= 0) {
          m_listModel.removeElementAt(selected);
          
          if (m_listModel.size() <= 1) {
            m_upBut.setEnabled(false);
            m_downBut.setEnabled(false);
          }
        }
      }
    });
    
    m_upBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JListHelper.moveUp(m_list);
      }
    });
    
    m_downBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JListHelper.moveDown(m_list);
      }
    });
    
    m_attCombo.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {        
      public void keyReleased(KeyEvent e) {        
        Object m = m_list.getSelectedValue();
        String text = "";
        if (m_attCombo.getSelectedItem() != null) {
          text = m_attCombo.getSelectedItem().toString();
        }
        java.awt.Component theEditor = m_attCombo.getEditor().getEditorComponent();
        if (theEditor instanceof JTextField) {
          text = ((JTextField)theEditor).getText();
        }
        if (m != null) {
          ((Sorter.SortRule)m).
            setAttribute(text);
          m_list.repaint();
        }
      }
    });
    
    m_attCombo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Object m = m_list.getSelectedValue();
        Object selected = m_attCombo.getSelectedItem();
        if (m != null && selected != null) {
          ((Sorter.SortRule)m).
            setAttribute(selected.toString());
          m_list.repaint();
        }
      }
    });
    
    m_descending.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Object m = m_list.getSelectedValue();
        if (m != null) {
          ((Sorter.SortRule)m).setDescending(m_descending.getSelectedIndex() == 1);
          m_list.repaint();
        }
      }
    });
  }
  
  private void addButtons() {
    JButton okBut = new JButton("OK");
    JButton cancelBut = new JButton("Cancel");
    
    JPanel butHolder = new JPanel();
    butHolder.setLayout(new GridLayout(1, 2));
    butHolder.add(okBut); butHolder.add(cancelBut);
    add(butHolder, BorderLayout.SOUTH);        
    
    okBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {   
        closingOK();
        
        m_parent.dispose();
      }
    });
    
    cancelBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        closingCancel();
        
        m_parent.dispose();
      }
    });
  }
  
  /**
   * Handle a closing event under an OK condition 
   */
  protected void closingOK() {
    StringBuffer buff = new StringBuffer();
    for (int i = 0; i < m_listModel.size(); i++) {
      Sorter.SortRule m =
        (Sorter.SortRule)m_listModel.elementAt(i);
      
      buff.append(m.toStringInternal());
      if (i < m_listModel.size() - 1) {
        buff.append("@@sort-rule@@");
      }
    }
    
    if (m_sorter.getSortDetails() != null) {
      if (!m_sorter.getSortDetails().equals(buff.toString())) {
        m_modifyL.setModifiedStatus(SorterCustomizer.this, true);
      }
    } else {
      m_modifyL.setModifiedStatus(SorterCustomizer.this, true);
    }
    
    m_sorter.setSortDetails(buff.toString());
    if (m_buffSize.getText() != null && m_buffSize.getText().length() > 0) {
      if (m_sorter.getBufferSize() != null && 
          !m_sorter.getBufferSize().equals(m_buffSize.getText())) {
        m_modifyL.setModifiedStatus(SorterCustomizer.this, true);
      }
      m_sorter.setBufferSize(m_buffSize.getText());
    }
    
    if (m_tempDir.getText() != null && m_tempDir.getText().length() > 0) {
      if (m_sorter.getTempDirectory() != null && 
          !m_sorter.getTempDirectory().equals(m_tempDir.getText())) {
        m_modifyL.setModifiedStatus(SorterCustomizer.this, true);
      }
      
      m_sorter.setTempDirectory(m_tempDir.getText());
    }
  }
  
  /**
   * Handle a closing event under a CANCEL condition
   */
  protected void closingCancel() {
    // nothing to do
  }
  
  /**
   * Initialize widgets with values from Sorter
   */
  protected void initialize() {
    if (m_sorter.getBufferSize() != null && m_sorter.getBufferSize().length() > 0) {
      m_buffSize.setText(m_sorter.getBufferSize());
    }
    
    if (m_sorter.getTempDirectory() != null && m_sorter.getTempDirectory().length() > 0) {
      m_tempDir.setText(m_sorter.getTempDirectory());
    }
    
    String sString = m_sorter.getSortDetails();
    
    m_listModel = new DefaultListModel();
    m_list.setModel(m_listModel);
    
    if (sString != null && sString.length() > 0) {
      String[] parts = sString.split("@@sort-rule@@");
      
      if (parts.length > 0) {
        m_upBut.setEnabled(true);
        m_downBut.setEnabled(true);
        for (String sPart : parts) {
          Sorter.SortRule s = new Sorter.SortRule(sPart);
          m_listModel.addElement(s);
        }        
      }
      
      m_list.repaint();
    }
    
    // try and set up attribute combo
    if (m_sorter.getConnectedFormat() != null) {
      Instances incoming = m_sorter.getConnectedFormat();
      
      m_attCombo.removeAllItems();
      for (int i = 0; i < incoming.numAttributes(); i++) {
        m_attCombo.addItem(incoming.attribute(i).name());
      }
    }    
  }

  /**
   * Set the object to edit
   */
  public void setObject(Object o) {
    if (o instanceof Sorter) {
      m_sorter = (Sorter)o;
      m_tempEditor.setTarget(o);
      setup();
      initialize();
    }
  }

  /**
   * Set the parent window for this dialog
   * 
   * @param parent the parent window
   */
  public void setParentWindow(Window parent) {
    m_parent = parent;
  }

  /**
   * Set environment variables to use
   * 
   * @param env the environment variables to use
   */
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * The modify listener interested in any chages we might make
   */
  public void setModifiedListener(ModifyListener l) {
    m_modifyL = l;
  }
}
