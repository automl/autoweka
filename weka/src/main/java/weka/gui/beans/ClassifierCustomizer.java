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
 *    ClassifierCustomizer.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import weka.classifiers.Classifier;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertySheetPanel;

/**
 * GUI customizer for the classifier wrapper bean
 * 
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 9213 $
 */
public class ClassifierCustomizer extends JPanel implements BeanCustomizer,
    CustomizerClosingListener, CustomizerCloseRequester, EnvironmentHandler {

  /** for serialization */
  private static final long serialVersionUID = -6688000820160821429L;

  static {
    GenericObjectEditor.registerEditors();
  }

  private final PropertyChangeSupport m_pcSupport = new PropertyChangeSupport(
      this);

  private weka.gui.beans.Classifier m_dsClassifier;
  /*
   * private GenericObjectEditor m_ClassifierEditor = new
   * GenericObjectEditor(true);
   */
  private final PropertySheetPanel m_ClassifierEditor = new PropertySheetPanel();

  private final JPanel m_incrementalPanel = new JPanel();
  private final JCheckBox m_resetIncrementalClassifier = new JCheckBox(
      "Reset classifier at the start of the stream");
  private final JCheckBox m_updateIncrementalClassifier = new JCheckBox(
      "Update classifier on incoming instance stream");
  private boolean m_panelVisible = false;

  private final JPanel m_holderPanel = new JPanel();
  private final JTextField m_executionSlotsText = new JTextField();
  private final JLabel m_executionSlotsLabel;
  private final JPanel m_executionSlotsPanel;

  private final JCheckBox m_blockOnLastFold = new JCheckBox(
      "Block on last fold of last run");

  private FileEnvironmentField m_loadModelField;

  private Window m_parentWindow;

  /** Copy of the current classifier in case cancel is selected */
  protected weka.classifiers.Classifier m_backup;

  private Environment m_env = Environment.getSystemWide();

  /**
   * Listener that wants to know the the modified status of the object that
   * we're customizing
   */
  private ModifyListener m_modifyListener;

  public ClassifierCustomizer() {

    m_ClassifierEditor.setBorder(BorderFactory
        .createTitledBorder("Classifier options"));

    m_incrementalPanel.setLayout(new GridLayout(0, 1));
    m_resetIncrementalClassifier.setToolTipText("Reset the classifier "
        + "before processing the first incoming instance");
    m_updateIncrementalClassifier.setToolTipText("Train the classifier on "
        + "each individual incoming streamed instance.");
    m_updateIncrementalClassifier.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_dsClassifier != null) {
          m_dsClassifier
              .setUpdateIncrementalClassifier(m_updateIncrementalClassifier
                  .isSelected());
        }
      }
    });
    m_resetIncrementalClassifier.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_dsClassifier != null) {
          m_dsClassifier
              .setResetIncrementalClassifier(m_resetIncrementalClassifier
                  .isSelected());
        }
      }
    });

    m_incrementalPanel.add(m_resetIncrementalClassifier);
    m_incrementalPanel.add(m_updateIncrementalClassifier);

    m_executionSlotsText.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_dsClassifier != null
            && m_executionSlotsText.getText().length() > 0) {
          int newSlots = Integer.parseInt(m_executionSlotsText.getText());
          m_dsClassifier.setExecutionSlots(newSlots);
        }
      }
    });

    m_executionSlotsText.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
      }

      @Override
      public void focusLost(FocusEvent e) {
        if (m_dsClassifier != null
            && m_executionSlotsText.getText().length() > 0) {
          int newSlots = Integer.parseInt(m_executionSlotsText.getText());
          m_dsClassifier.setExecutionSlots(newSlots);
        }
      }
    });

    m_blockOnLastFold.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_dsClassifier != null) {
          m_dsClassifier.setBlockOnLastFold(m_blockOnLastFold.isSelected());
        }
      }
    });

    m_executionSlotsPanel = new JPanel();
    m_executionSlotsPanel
        .setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    m_executionSlotsLabel = new JLabel("Execution slots");
    m_executionSlotsPanel.setLayout(new BorderLayout());
    m_executionSlotsPanel.add(m_executionSlotsLabel, BorderLayout.WEST);
    m_executionSlotsPanel.add(m_executionSlotsText, BorderLayout.CENTER);
    m_holderPanel.setBorder(BorderFactory.createTitledBorder("More options"));
    m_holderPanel.setLayout(new BorderLayout());
    m_holderPanel.add(m_executionSlotsPanel, BorderLayout.NORTH);
    // m_blockOnLastFold.setHorizontalTextPosition(SwingConstants.RIGHT);
    m_holderPanel.add(m_blockOnLastFold, BorderLayout.SOUTH);

    JPanel holder2 = new JPanel();
    holder2.setLayout(new BorderLayout());
    holder2.add(m_holderPanel, BorderLayout.NORTH);
    JButton OKBut = new JButton("OK");
    JButton CancelBut = new JButton("Cancel");
    OKBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // forces the template to be deep copied to the actual classifier.
        // necessary for InputMappedClassifier that is loading from a file
        m_dsClassifier.setClassifierTemplate(m_dsClassifier
            .getClassifierTemplate());
        if (m_loadModelField != null) {
          String loadFName = m_loadModelField.getText();
          if (loadFName != null && loadFName.length() > 0) {
            m_dsClassifier.setLoadClassifierFileName(m_loadModelField.getText());
          } else {
            m_dsClassifier.setLoadClassifierFileName("");
          }
        }

        if (m_modifyListener != null) {
          m_modifyListener.setModifiedStatus(ClassifierCustomizer.this, true);
        }

        m_parentWindow.dispose();
      }
    });

    CancelBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // cancel requested, so revert to backup and then
        // close the dialog
        if (m_backup != null) {
          m_dsClassifier.setClassifierTemplate(m_backup);
        }

        if (m_modifyListener != null) {
          m_modifyListener.setModifiedStatus(ClassifierCustomizer.this, false);
        }

        m_parentWindow.dispose();
      }
    });

    JPanel butHolder = new JPanel();
    butHolder.setLayout(new GridLayout(1, 2));
    butHolder.add(OKBut);
    butHolder.add(CancelBut);
    holder2.add(butHolder, BorderLayout.SOUTH);

    setLayout(new BorderLayout());
    add(m_ClassifierEditor, BorderLayout.CENTER);
    add(holder2, BorderLayout.SOUTH);
  }

  private void checkOnClassifierType() {
    Classifier editedC = m_dsClassifier.getClassifierTemplate();
    if (editedC instanceof weka.classifiers.UpdateableClassifier
        && m_dsClassifier.hasIncomingStreamInstances()) {
      if (!m_panelVisible) {
        m_holderPanel.add(m_incrementalPanel, BorderLayout.SOUTH);
        m_panelVisible = true;
        m_executionSlotsText.setEnabled(false);
        m_loadModelField = new FileEnvironmentField("Load model from file",
            m_env);
        m_incrementalPanel.add(m_loadModelField);
        m_loadModelField.setText(m_dsClassifier.getLoadClassifierFileName());
      }
    } else {
      if (m_panelVisible) {
        m_holderPanel.remove(m_incrementalPanel);
      }

      if (m_dsClassifier.hasIncomingStreamInstances()) {
        m_loadModelField = new FileEnvironmentField("Load model from file",
            m_env);
        m_executionSlotsPanel.add(m_loadModelField, BorderLayout.SOUTH);
        m_executionSlotsText.setEnabled(false);
        m_blockOnLastFold.setEnabled(false);
        m_loadModelField.setText(m_dsClassifier.getLoadClassifierFileName());
      } else {
        m_executionSlotsText.setEnabled(true);
        m_blockOnLastFold.setEnabled(true);
      }
      m_panelVisible = false;

      if (m_dsClassifier.hasIncomingBatchInstances()
          && !m_dsClassifier.m_listenees.containsKey("trainingSet")) {
        m_holderPanel.remove(m_blockOnLastFold);
        m_holderPanel.remove(m_executionSlotsPanel);
        m_loadModelField = new FileEnvironmentField("Load model from file",
            m_env);
        m_holderPanel.add(m_loadModelField, BorderLayout.SOUTH);
        m_loadModelField.setText(m_dsClassifier.getLoadClassifierFileName());
      }
    }
  }

  /**
   * Set the classifier object to be edited
   * 
   * @param object an <code>Object</code> value
   */
  @Override
  public void setObject(Object object) {
    m_dsClassifier = (weka.gui.beans.Classifier) object;
    // System.err.println(Utils.joinOptions(((OptionHandler)m_dsClassifier.getClassifier()).getOptions()));
    try {
      m_backup = (weka.classifiers.Classifier) GenericObjectEditor
          .makeCopy(m_dsClassifier.getClassifierTemplate());
    } catch (Exception ex) {
      // ignore
    }
    m_ClassifierEditor.setEnvironment(m_env);
    m_ClassifierEditor.setTarget(m_dsClassifier.getClassifierTemplate());
    m_resetIncrementalClassifier.setSelected(m_dsClassifier
        .getResetIncrementalClassifier());
    m_updateIncrementalClassifier.setSelected(m_dsClassifier
        .getUpdateIncrementalClassifier());
    m_executionSlotsText.setText("" + m_dsClassifier.getExecutionSlots());
    m_blockOnLastFold.setSelected(m_dsClassifier.getBlockOnLastFold());
    checkOnClassifierType();
  }

  /*
   * (non-Javadoc)
   * 
   * @see weka.gui.beans.CustomizerClosingListener#customizerClosing()
   */
  @Override
  public void customizerClosing() {
    if (m_executionSlotsText.getText().length() > 0) {
      int newSlots = Integer.parseInt(m_executionSlotsText.getText());
      m_dsClassifier.setExecutionSlots(newSlots);
    }
  }

  /**
   * Add a property change listener
   * 
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener pcl) {
    m_pcSupport.addPropertyChangeListener(pcl);
  }

  /**
   * Remove a property change listener
   * 
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  @Override
  public void removePropertyChangeListener(PropertyChangeListener pcl) {
    m_pcSupport.removePropertyChangeListener(pcl);
  }

  @Override
  public void setParentWindow(Window parent) {
    m_parentWindow = parent;
  }

  /**
   * Set any environment variables to pass to the PropertySheetPanel
   * 
   * @param env environment variables to pass to the property sheet panel
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  @Override
  public void setModifiedListener(ModifyListener l) {
    m_modifyListener = l;
  }
}
