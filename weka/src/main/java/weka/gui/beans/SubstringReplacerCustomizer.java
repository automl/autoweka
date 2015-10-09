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
 *    SubstringReplacerCustomizer.java
 *    Copyright (C) 2011 Pentaho Corporation
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.gui.JListHelper;
import weka.gui.PropertySheetPanel;

/**
 * Customizer for the SubstringReplacer
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 8997 $
 */
public class SubstringReplacerCustomizer extends JPanel implements
    EnvironmentHandler, BeanCustomizer, CustomizerCloseRequester {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -1245155414691393809L;
  protected Environment m_env = Environment.getSystemWide();
  protected ModifyListener m_modifyL = null;
  protected SubstringReplacer m_replacer;

  protected EnvironmentField m_attListField;
  protected EnvironmentField m_matchField;
  protected EnvironmentField m_replaceField;
  protected JCheckBox m_regexCheck = new JCheckBox();
  protected JCheckBox m_ignoreCaseCheck = new JCheckBox();
  protected JList m_list = new JList();
  protected DefaultListModel m_listModel;

  protected JButton m_newBut = new JButton("New");
  protected JButton m_deleteBut = new JButton("Delete");
  protected JButton m_upBut = new JButton("Move up");
  protected JButton m_downBut = new JButton("Move down");

  protected Window m_parent;

  protected PropertySheetPanel m_tempEditor = new PropertySheetPanel();

  /**
   * Constructor
   */
  public SubstringReplacerCustomizer() {
    setLayout(new BorderLayout());
  }

  private void setup() {
    JPanel aboutAndControlHolder = new JPanel();
    aboutAndControlHolder.setLayout(new BorderLayout());

    JPanel controlHolder = new JPanel();
    controlHolder.setLayout(new BorderLayout());
    JPanel fieldHolder = new JPanel();
    JPanel attListP = new JPanel();
    attListP.setLayout(new BorderLayout());
    attListP.setBorder(BorderFactory.createTitledBorder("Apply to attributes"));
    m_attListField = new EnvironmentField(m_env);
    attListP.add(m_attListField, BorderLayout.CENTER);
    attListP
        .setToolTipText("<html>Accepts a range of indexes (e.g. '1,2,6-10')<br> "
            + "or a comma-separated list of named attributes</html>");
    JPanel matchP = new JPanel();
    matchP.setLayout(new BorderLayout());
    matchP.setBorder(BorderFactory.createTitledBorder("Match"));
    m_matchField = new EnvironmentField(m_env);
    matchP.add(m_matchField, BorderLayout.CENTER);
    JPanel replaceP = new JPanel();
    replaceP.setLayout(new BorderLayout());
    replaceP.setBorder(BorderFactory.createTitledBorder("Replace"));
    m_replaceField = new EnvironmentField(m_env);
    replaceP.add(m_replaceField, BorderLayout.CENTER);
    fieldHolder.add(attListP);
    fieldHolder.add(matchP);
    fieldHolder.add(replaceP);
    controlHolder.add(fieldHolder, BorderLayout.NORTH);

    JPanel checkHolder = new JPanel();
    checkHolder.setLayout(new GridLayout(0, 2));
    JLabel regexLab = new JLabel("Match using a regular expression",
        SwingConstants.RIGHT);
    regexLab
        .setToolTipText("Use a regular expression rather than literal match");
    checkHolder.add(regexLab);
    checkHolder.add(m_regexCheck);
    JLabel caseLab = new JLabel("Ignore case when matching",
        SwingConstants.RIGHT);
    checkHolder.add(caseLab);
    checkHolder.add(m_ignoreCaseCheck);

    controlHolder.add(checkHolder, BorderLayout.SOUTH);

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
    butHolder.add(m_newBut);
    butHolder.add(m_deleteBut);
    butHolder.add(m_upBut);
    butHolder.add(m_downBut);
    m_upBut.setEnabled(false);
    m_downBut.setEnabled(false);

    listPanel.add(butHolder, BorderLayout.NORTH);
    JScrollPane js = new JScrollPane(m_list);
    js.setBorder(BorderFactory
        .createTitledBorder("Match-replace list (rows applied in order)"));
    listPanel.add(js, BorderLayout.CENTER);
    add(listPanel, BorderLayout.CENTER);

    addButtons();

    m_attListField.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        Object mr = m_list.getSelectedValue();
        if (mr != null) {
          ((SubstringReplacer.MatchReplace) mr).setAttsToApplyTo(m_attListField
              .getText());
          m_list.repaint();
        }
      }
    });

    m_matchField.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        Object mr = m_list.getSelectedValue();
        if (mr != null) {
          ((SubstringReplacer.MatchReplace) mr).setMatch(m_matchField.getText());
          m_list.repaint();
        }
      }
    });

    m_replaceField.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        Object mr = m_list.getSelectedValue();
        if (mr != null) {
          ((SubstringReplacer.MatchReplace) mr).setReplace(m_replaceField
              .getText());
          m_list.repaint();
        }
      }
    });

    m_list.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          if (!m_deleteBut.isEnabled()) {
            m_deleteBut.setEnabled(true);
          }

          Object entry = m_list.getSelectedValue();
          if (entry != null) {
            SubstringReplacer.MatchReplace mr = (SubstringReplacer.MatchReplace) entry;
            m_attListField.setText(mr.getAttsToApplyTo());
            m_matchField.setText(mr.getMatch());
            m_replaceField.setText(mr.getReplace());
            m_regexCheck.setSelected(mr.getRegex());
            m_ignoreCaseCheck.setSelected(mr.getIgnoreCase());
          }
        }
      }
    });

    m_newBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        SubstringReplacer.MatchReplace mr = new SubstringReplacer.MatchReplace();

        String atts = (m_attListField.getText() != null) ? m_attListField
            .getText() : "";
        mr.setAttsToApplyTo(atts);
        String match = (m_matchField.getText() != null) ? m_matchField
            .getText() : "";
        mr.setMatch(match);
        String replace = (m_replaceField.getText() != null) ? m_replaceField
            .getText() : "";
        mr.setReplace(replace);
        mr.setRegex(m_regexCheck.isSelected());
        mr.setIgnoreCase(m_ignoreCaseCheck.isSelected());

        m_listModel.addElement(mr);

        if (m_listModel.size() > 1) {
          m_upBut.setEnabled(true);
          m_downBut.setEnabled(true);
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

    m_regexCheck.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object mr = m_list.getSelectedValue();
        if (mr != null) {
          ((SubstringReplacer.MatchReplace) mr).setRegex(m_regexCheck
              .isSelected());
          m_list.repaint();
        }
      }
    });

    m_ignoreCaseCheck.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object mr = m_list.getSelectedValue();
        if (mr != null) {
          ((SubstringReplacer.MatchReplace) mr).setIgnoreCase(m_ignoreCaseCheck
              .isSelected());
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
    butHolder.add(okBut);
    butHolder.add(cancelBut);
    add(butHolder, BorderLayout.SOUTH);

    okBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        closingOK();

        m_parent.dispose();
      }
    });

    cancelBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        closingCancel();

        m_parent.dispose();
      }
    });
  }

  /**
   * Set environment variables to use
   * 
   * @param env
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  protected void initialize() {
    String mrString = m_replacer.getMatchReplaceDetails();
    m_listModel = new DefaultListModel();
    m_list.setModel(m_listModel);

    if (mrString != null && mrString.length() > 0) {
      String[] parts = mrString.split("@@match-replace@@");

      if (parts.length > 0) {
        m_upBut.setEnabled(true);
        m_downBut.setEnabled(true);
        for (String mrPart : parts) {
          SubstringReplacer.MatchReplace mr = new SubstringReplacer.MatchReplace(
              mrPart);
          m_listModel.addElement(mr);
        }

        m_list.repaint();
      }
    }
  }

  /**
   * Set the SubstringReplacer to edit
   * 
   * @param o the SubtringReplacer to edit
   */
  @Override
  public void setObject(Object o) {
    if (o instanceof SubstringReplacer) {
      m_replacer = (SubstringReplacer) o;
      m_tempEditor.setTarget(o);
      setup();
      initialize();
    }
  }

  /**
   * Set a listener interested in knowing if the object being edited has
   * changed.
   * 
   * @param l the interested listener
   */
  @Override
  public void setModifiedListener(ModifyListener l) {
    m_modifyL = l;
  }

  /**
   * Handle a closing event under an OK condition
   */
  protected void closingOK() {
    StringBuffer buff = new StringBuffer();
    for (int i = 0; i < m_listModel.size(); i++) {
      SubstringReplacer.MatchReplace mr = (SubstringReplacer.MatchReplace) m_listModel
          .elementAt(i);

      buff.append(mr.toStringInternal());
      if (i < m_listModel.size() - 1) {
        buff.append("@@match-replace@@");
      }
    }

    m_replacer.setMatchReplaceDetails(buff.toString());
    if (m_modifyL != null) {
      m_modifyL.setModifiedStatus(SubstringReplacerCustomizer.this, true);
    }
  }

  /**
   * Handle a closing event under a CANCEL condition
   */
  protected void closingCancel() {
    // nothing to do
  }

  /**
   * Set a reference to the parent window/dialog containing this panel
   * 
   * @param parent the parent window
   */
  @Override
  public void setParentWindow(Window parent) {
    m_parent = parent;
  }
}
