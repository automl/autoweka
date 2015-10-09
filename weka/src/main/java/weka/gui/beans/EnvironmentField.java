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
 *    EnvironmentField.java
 *    Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.gui.CustomPanelSupplier;

/**
 * Widget that displays a label and a combo box for selecting environment
 * variables. The enter arbitrary text, select an environment variable or a
 * combination of both. Any variables are resolved (if possible) and resolved
 * values are displayed in a tip-text.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 9113 $
 */
public class EnvironmentField extends JPanel implements EnvironmentHandler,
    PropertyEditor, CustomPanelSupplier {

  /** For serialization */
  private static final long serialVersionUID = -3125404573324734121L;

  /** The label for the widget */
  protected JLabel m_label;

  /** The combo box */
  protected JComboBox m_combo;

  /** The current environment variables */
  protected Environment m_env;

  protected String m_currentContents = "";
  protected int m_firstCaretPos = 0;
  protected int m_previousCaretPos = 0;
  protected int m_currentCaretPos = 0;

  protected PropertyChangeSupport m_support = new PropertyChangeSupport(this);

  /**
   * Combo box that allows the drop-down list to be wider than the component
   * itself.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class WideComboBox extends JComboBox {

    /**
     * For serialization
     */
    private static final long serialVersionUID = -6512065375459733517L;

    public WideComboBox() {
    }

    public WideComboBox(final Object items[]) {
      super(items);
    }

    public WideComboBox(Vector items) {
      super(items);
    }

    public WideComboBox(ComboBoxModel aModel) {
      super(aModel);
    }

    private boolean m_layingOut = false;

    @Override
    public void doLayout() {
      try {
        m_layingOut = true;
        super.doLayout();
      } finally {
        m_layingOut = false;
      }
    }

    @Override
    public Dimension getSize() {
      Dimension dim = super.getSize();
      if (!m_layingOut) {
        dim.width = Math.max(dim.width, getPreferredSize().width);
      }
      return dim;
    }
  }

  /**
   * Construct an EnvironmentField with no label.
   */
  public EnvironmentField() {
    this("");
    setEnvironment(Environment.getSystemWide());
  }

  /**
   * Construct an EnvironmentField with no label.
   * 
   * @param env the environment variables to display in the drop-down box
   */
  public EnvironmentField(Environment env) {
    this("");
    setEnvironment(env);
  }

  /**
   * Constructor.
   * 
   * @param label the label to use
   * @param env the environment variables to display in the drop-down box
   */
  public EnvironmentField(String label, Environment env) {
    this(label);
    setEnvironment(env);
  }

  /**
   * Constructor.
   * 
   * @param label the label to use
   */
  public EnvironmentField(String label) {
    setLayout(new BorderLayout());
    m_label = new JLabel(label);
    if (label.length() > 0) {
      m_label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
    }
    add(m_label, BorderLayout.WEST);

    m_combo = new WideComboBox();
    m_combo.setEditable(true);
    // m_combo.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    java.awt.Component theEditor = m_combo.getEditor().getEditorComponent();
    if (theEditor instanceof JTextField) {
      ((JTextField) m_combo.getEditor().getEditorComponent())
          .addCaretListener(new CaretListener() {

            @Override
            public void caretUpdate(CaretEvent e) {
              m_firstCaretPos = m_previousCaretPos;
              m_previousCaretPos = m_currentCaretPos;
              m_currentCaretPos = e.getDot();
            }
          });

      m_combo.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
          m_support.firePropertyChange("", null, null);
        }
      });

      ((JTextField) m_combo.getEditor().getEditorComponent())
          .addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
              m_support.firePropertyChange("", null, null);
            }
          });
    }
    add(m_combo, BorderLayout.CENTER);

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    Dimension d = getPreferredSize();
    setPreferredSize(new Dimension(250, d.height));
  }

  /**
   * Set the label for this widget.
   * 
   * @param label the label to use
   */
  public void setLabel(String label) {
    m_label.setText(label);
  }

  /**
   * Set the text to display in the editable combo box.
   * 
   * @param text the text to display
   */
  public void setText(String text) {
    m_currentContents = text;
    java.awt.Component theEditor = m_combo.getEditor().getEditorComponent();
    if (theEditor instanceof JTextField) {
      ((JTextField) theEditor).setText(text);
    } else {
      m_combo.setSelectedItem(m_currentContents);
    }
    m_support.firePropertyChange("", null, null);
  }

  /**
   * Return the text from the combo box.
   * 
   * @return the text from the combo box
   */
  public String getText() {
    java.awt.Component theEditor = m_combo.getEditor().getEditorComponent();
    String text = m_combo.getSelectedItem().toString();
    if (theEditor instanceof JTextField) {
      text = ((JTextField) theEditor).getText();
    }
    return text;
  }

  @Override
  public void setAsText(String s) {
    setText(s);
  }

  @Override
  public String getAsText() {
    return getText();
  }

  @Override
  public void setValue(Object o) {
    setAsText((String) o);
  }

  @Override
  public Object getValue() {
    return getAsText();
  }

  @Override
  public String getJavaInitializationString() {
    return null;
  }

  @Override
  public boolean isPaintable() {
    return true; // we don't want to appear in a separate popup
  }

  @Override
  public String[] getTags() {
    return null;
  }

  @Override
  public boolean supportsCustomEditor() {
    return true;
  }

  @Override
  public Component getCustomEditor() {
    return this;
  }

  @Override
  public JPanel getCustomPanel() {
    return this;
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener pcl) {
    m_support.addPropertyChangeListener(pcl);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener pcl) {
    m_support.removePropertyChangeListener(pcl);
  }

  @Override
  public void paintValue(Graphics gfx, Rectangle box) {
    // TODO Auto-generated method stub

  }

  private String processSelected(String selected) {
    if (selected.equals(m_currentContents)) {
      // don't do anything if the user has just pressed return
      // without adding anything new
      return selected;
    }
    if (m_firstCaretPos == 0) {
      m_currentContents = selected + m_currentContents;
    } else if (m_firstCaretPos >= m_currentContents.length()) {
      m_currentContents = m_currentContents + selected;
    } else {
      String left = m_currentContents.substring(0, m_firstCaretPos);
      String right = m_currentContents.substring(m_firstCaretPos,
          m_currentContents.length());

      m_currentContents = left + selected + right;
    }

    /*
     * java.awt.Component theEditor = m_combo.getEditor().getEditorComponent();
     * if (theEditor instanceof JTextField) {
     * System.err.println("Setting current contents..." + m_currentContents);
     * ((JTextField)theEditor).setText(m_currentContents); }
     */
    m_combo.setSelectedItem(m_currentContents);
    m_support.firePropertyChange("", null, null);

    return m_currentContents;
  }

  /**
   * Set the environment variables to display in the drop down list.
   * 
   * @param env the environment variables to display
   */
  @Override
  public void setEnvironment(final Environment env) {
    m_env = env;
    Vector<String> varKeys = new Vector<String>(env.getVariableNames());

    DefaultComboBoxModel dm = new DefaultComboBoxModel(varKeys) {
      @Override
      public Object getSelectedItem() {
        Object item = super.getSelectedItem();
        if (item instanceof String) {
          if (env.getVariableValue((String) item) != null) {
            String newS = "${" + (String) item + "}";
            item = newS;
          }
        }
        return item;
      }
    };
    m_combo.setModel(dm);
    m_combo.setSelectedItem("");
    m_combo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String selected = (String) m_combo.getSelectedItem();
        try {
          selected = processSelected(selected);

          selected = m_env.substitute(selected);
        } catch (Exception ex) {
          // quietly ignore unresolved variables
        }
        m_combo.setToolTipText(selected);
      }
    });

    m_combo.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        java.awt.Component theEditor = m_combo.getEditor().getEditorComponent();
        if (theEditor instanceof JTextField) {
          String selected = ((JTextField) theEditor).getText();
          m_currentContents = selected;
          if (m_env != null) {
            try {
              selected = m_env.substitute(selected);
            } catch (Exception ex) {
              // quietly ignore unresolved variables
            }
          }
          m_combo.setToolTipText(selected);
        }
      }
    });
  }

  /**
   * Set the enabled status of the combo box.
   * 
   * @param enabled true if the combo box is enabled
   */
  @Override
  public void setEnabled(boolean enabled) {
    m_combo.setEnabled(enabled);
  }

  /**
   * Set the editable status of the combo box.
   * 
   * @param editable true if the combo box is editable
   */
  public void setEditable(boolean editable) {
    m_combo.setEditable(editable);
  }

  /**
   * Main method for testing this class
   * 
   * @param args command line args (ignored)
   */
  public static void main(String[] args) {
    try {
      final javax.swing.JFrame jf = new javax.swing.JFrame("EnvironmentField");
      jf.getContentPane().setLayout(new BorderLayout());
      final EnvironmentField f = new EnvironmentField("A label here");
      jf.getContentPane().add(f, BorderLayout.CENTER);
      Environment env = Environment.getSystemWide();
      f.setEnvironment(env);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
          jf.dispose();
          System.exit(0);
        }
      });
      jf.pack();
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
