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
 *    AddUserFieldsCustomizer.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.filters;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.filters.unsupervised.attribute.AddUserFields;
import weka.gui.JListHelper;
import weka.gui.beans.EnvironmentField;
import weka.gui.beans.GOECustomizer;

/**
 * Customizer for the AddUserFields filter.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 8999 $
 */
public class AddUserFieldsCustomizer extends JPanel implements
    EnvironmentHandler, GOECustomizer {

  /** For serialization */
  private static final long serialVersionUID = -3823417827142174931L;
  protected ModifyListener m_modifyL = null;
  protected Environment m_env = Environment.getSystemWide();

  /** THe filter being customized */
  protected AddUserFields m_filter = null;

  protected EnvironmentField m_nameField;
  protected JComboBox m_typeField;
  protected EnvironmentField m_dateFormatField;
  protected EnvironmentField m_valueField;

  protected JList m_list = new JList();
  protected DefaultListModel m_listModel;

  protected JButton m_newBut = new JButton("New");
  protected JButton m_deleteBut = new JButton("Delete");
  protected JButton m_upBut = new JButton("Move up");
  protected JButton m_downBut = new JButton("Move down");

  protected boolean m_dontShowButs = false;

  /**
   * Constructor
   */
  public AddUserFieldsCustomizer() {
    setLayout(new BorderLayout());
  }

  private void setup() {
    JPanel fieldHolder = new JPanel();
    JPanel namePanel = new JPanel();
    namePanel.setLayout(new BorderLayout());
    namePanel.setBorder(BorderFactory.createTitledBorder("Attribute name"));
    m_nameField = new EnvironmentField(m_env);
    namePanel.add(m_nameField, BorderLayout.CENTER);
    namePanel.setToolTipText("Name of the new attribute");

    JPanel typePanel = new JPanel();
    typePanel.setLayout(new BorderLayout());
    typePanel.setBorder(BorderFactory.createTitledBorder("Attribute type"));
    m_typeField = new JComboBox();
    m_typeField.addItem("numeric");
    m_typeField.addItem("nominal");
    m_typeField.addItem("string");
    m_typeField.addItem("date");
    typePanel.add(m_typeField, BorderLayout.CENTER);
    m_typeField.setToolTipText("Attribute type");
    typePanel.setToolTipText("Attribute type");

    JPanel formatPanel = new JPanel();
    formatPanel.setLayout(new BorderLayout());
    formatPanel.setBorder(BorderFactory.createTitledBorder("Date format"));
    m_dateFormatField = new EnvironmentField(m_env);
    formatPanel.add(m_dateFormatField, BorderLayout.CENTER);
    formatPanel.setToolTipText("Date format (date attributes only)");

    JPanel valuePanel = new JPanel();
    valuePanel.setLayout(new BorderLayout());
    valuePanel.setBorder(BorderFactory.createTitledBorder("Attribute value"));
    m_valueField = new EnvironmentField(m_env);
    valuePanel.add(m_valueField, BorderLayout.CENTER);
    valuePanel
        .setToolTipText("<html>Constant value (number, string or date)<br>"
            + "for field. Special value \"now\" can be<br>"
            + "used for date attributes for the current<br>"
            + "time stamp</html>");

    fieldHolder.add(namePanel);
    fieldHolder.add(typePanel);
    fieldHolder.add(formatPanel);
    fieldHolder.add(valuePanel);

    add(fieldHolder, BorderLayout.NORTH);

    m_list.setVisibleRowCount(5);
    m_deleteBut.setEnabled(false);
    JPanel listPanel = new JPanel();
    listPanel.setLayout(new BorderLayout());
    JPanel butHolder = new JPanel();
    butHolder.setLayout(new GridLayout(1, 0));
    butHolder.add(m_newBut);
    butHolder.add(m_deleteBut);
    butHolder.add(m_upBut);
    butHolder.add(m_downBut);
    m_upBut.setEnabled(false);
    m_downBut.setEnabled(false);

    listPanel.add(butHolder, BorderLayout.NORTH);
    JScrollPane js = new JScrollPane(m_list);
    js.setBorder(BorderFactory
        .createTitledBorder("Match-list list (rows applied in order)"));
    listPanel.add(js, BorderLayout.CENTER);
    add(listPanel, BorderLayout.CENTER);

    m_list.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          if (!m_deleteBut.isEnabled()) {
            m_deleteBut.setEnabled(true);
          }

          Object entry = m_list.getSelectedValue();
          if (entry != null) {
            AddUserFields.AttributeSpec m = (AddUserFields.AttributeSpec) entry;
            m_nameField.setText(m.getName());
            m_valueField.setText(m.getValue());
            String type = m.getType();
            String format = "";
            if (type.startsWith("date") && type.indexOf(":") > 0) {
              format = type.substring(type.indexOf(":") + 1, type.length());
              type = type.substring(0, type.indexOf(":"));
            }

            m_typeField.setSelectedItem(type.trim());
            m_dateFormatField.setText(format);
          }
        }
      }
    });

    m_nameField.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        Object a = m_list.getSelectedValue();
        if (a != null) {
          ((AddUserFields.AttributeSpec) a).setName(m_nameField.getText());
          m_list.repaint();
        }
      }
    });

    m_typeField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object a = m_list.getSelectedValue();
        if (a != null) {
          String type = m_typeField.getSelectedItem().toString();
          if (type.startsWith("date")) {
            // check to see if there is a date format specified
            String format = m_dateFormatField.getText();
            if (format != null && format.length() > 0) {
              type += ":" + format;
            }
          }
          ((AddUserFields.AttributeSpec) a).setType(type);
          m_list.repaint();
        }
      }
    });

    m_valueField.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        Object a = m_list.getSelectedValue();
        if (a != null) {
          ((AddUserFields.AttributeSpec) a).setValue(m_valueField.getText());
          m_list.repaint();
        }
      }
    });

    m_newBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        AddUserFields.AttributeSpec a = new AddUserFields.AttributeSpec();
        String name = (m_nameField.getText() != null && m_nameField.getText()
            .length() > 0) ? m_nameField.getText() : "newAtt";
        a.setName(name);
        String type = m_typeField.getSelectedItem().toString();
        if (type.startsWith("date")) {
          if (m_dateFormatField.getText() != null
              && m_dateFormatField.getText().length() > 0) {
            type += ":" + m_dateFormatField.getText();
          }
        }
        a.setType(type);
        String value = (m_valueField.getText() != null) ? m_valueField
            .getText() : "";
        a.setValue(value);
        m_listModel.addElement(a);

        if (m_listModel.size() > 1) {
          m_upBut.setEnabled(true);
          m_downBut.setEnabled(true);
        }

        if (m_listModel.size() > 0) {
          m_nameField.setEnabled(true);
          m_typeField.setEnabled(true);
          m_dateFormatField.setEnabled(true);
          m_valueField.setEnabled(true);
        }
        m_list.setSelectedIndex(m_listModel.size() - 1);
      }
    });

    m_deleteBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int selected = m_list.getSelectedIndex();
        if (selected >= 0) {
          m_listModel.removeElementAt(selected);

          if (m_listModel.size() <= 1) {
            m_upBut.setEnabled(false);
            m_downBut.setEnabled(false);
          }

          if (m_listModel.size() == 0) {
            m_nameField.setEnabled(false);
            m_typeField.setEnabled(false);
            m_dateFormatField.setEnabled(false);
            m_valueField.setEnabled(false);
          }
        }
      }
    });

    m_upBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JListHelper.moveUp(m_list);
      }
    });

    m_downBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JListHelper.moveDown(m_list);
      }
    });

    if (m_dontShowButs) {
      return;
    }
    addButtons();
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
        closingOK();
      }
    });

    cancelBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        closingCancel();
      }
    });
  }

  /**
   * Set environment variables to use
   * 
   * @param env the variables to use
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Set an object that is interested in knowing when we make a change to the
   * object that we're editing
   */
  @Override
  public void setModifiedListener(ModifyListener l) {
    m_modifyL = l;
  }

  /**
   * Initialize the GUI with settings from the filter being edited.
   */
  protected void initialize() {
    List<AddUserFields.AttributeSpec> specs = m_filter.getAttributeSpecs();
    m_listModel = new DefaultListModel();
    m_list.setModel(m_listModel);

    if (specs.size() > 0) {
      m_upBut.setEnabled(true);
      m_downBut.setEnabled(true);
      for (AddUserFields.AttributeSpec s : specs) {
        AddUserFields.AttributeSpec specCopy = new AddUserFields.AttributeSpec(
            s.toStringInternal());
        m_listModel.addElement(specCopy);
      }

      m_list.repaint();
    } else {
      // disable editing fields until the user has clicked new
      m_nameField.setEnabled(false);
      m_typeField.setEnabled(false);
      m_dateFormatField.setEnabled(false);
      m_valueField.setEnabled(false);
    }
  }

  /**
   * Set the filter to edit.
   * 
   * @param o the filter to edit
   */
  @Override
  public void setObject(Object o) {
    if (o instanceof AddUserFields) {
      m_filter = (AddUserFields) o;
      setup();
      initialize();
    }
  }

  /**
   * Tell this customizer not to show its own OK and Cancel buttons.
   */
  @Override
  public void dontShowOKCancelButtons() {
    m_dontShowButs = true;
  }

  /**
   * Actions to perform when the user has closed the dialog with the OK button.
   */
  @Override
  public void closingOK() {
    List<AddUserFields.AttributeSpec> specs = new ArrayList<AddUserFields.AttributeSpec>();
    for (int i = 0; i < m_listModel.size(); i++) {
      AddUserFields.AttributeSpec a = (AddUserFields.AttributeSpec) m_listModel
          .elementAt(i);
      specs.add(a);
    }

    if (m_modifyL != null) {
      m_modifyL.setModifiedStatus(AddUserFieldsCustomizer.this, true);
    }

    m_filter.setAttributeSpecs(specs);
  }

  /**
   * Actions to perform when the user has closed the dialog with the Cancel
   * button
   */
  @Override
  public void closingCancel() {
    // Nothing to do
  }
}
