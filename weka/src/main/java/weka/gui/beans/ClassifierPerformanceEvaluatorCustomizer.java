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
 *    ClassifierPerformanceEvaluatorCustomizer.java
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import weka.gui.EvaluationMetricSelectionDialog;
import weka.gui.PropertySheetPanel;

/**
 * GUI customizer for the classifier performance evaluator component
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 9320 $
 */
public class ClassifierPerformanceEvaluatorCustomizer extends JPanel implements
    BeanCustomizer, CustomizerCloseRequester, CustomizerClosingListener {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -1055460295546483853L;

  private final PropertyChangeSupport m_pcSupport = new PropertyChangeSupport(
      this);

  private final PropertySheetPanel m_splitEditor = new PropertySheetPanel();

  private ClassifierPerformanceEvaluator m_cpe;
  private ModifyListener m_modifyListener;
  private int m_executionSlotsBackup;

  private Window m_parent;

  private final JButton m_evalMetricsBut = new JButton("Evaluation metrics...");
  private List<String> m_evaluationMetrics;

  /**
   * Constructor
   */
  public ClassifierPerformanceEvaluatorCustomizer() {
    setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 5, 5));

    setLayout(new BorderLayout());

    JPanel holder = new JPanel();
    holder.setLayout(new BorderLayout());

    holder.add(m_splitEditor, BorderLayout.NORTH);
    holder.add(m_evalMetricsBut, BorderLayout.SOUTH);
    add(holder, BorderLayout.NORTH);
    m_evalMetricsBut
        .setToolTipText("Enable/disable output of specific evaluation metrics");

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
        if (m_modifyListener != null) {
          m_modifyListener.setModifiedStatus(
              ClassifierPerformanceEvaluatorCustomizer.this, true);
        }

        if (m_evaluationMetrics.size() > 0) {
          StringBuilder b = new StringBuilder();
          for (String s : m_evaluationMetrics) {
            b.append(s).append(",");
          }
          String newList = b.substring(0, b.length() - 1);
          m_cpe.setEvaluationMetricsToOutput(newList);
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
   * Set the ClassifierPerformanceEvaluator to be customized
   * 
   * @param object an <code>Object</code> value
   */
  @Override
  public void setObject(Object object) {
    m_cpe = (ClassifierPerformanceEvaluator) object;
    m_executionSlotsBackup = m_cpe.getExecutionSlots();
    m_splitEditor.setTarget(m_cpe);

    String list = m_cpe.getEvaluationMetricsToOutput();
    m_evaluationMetrics = new ArrayList<String>();
    if (list != null && list.length() > 0) {
      String[] parts = list.split(",");
      for (String s : parts) {
        m_evaluationMetrics.add(s.trim());
      }
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
  public void setModifiedListener(ModifyListener l) {
    m_modifyListener = l;
  }

  @Override
  public void customizerClosing() {
    // restore the backup
    m_cpe.setExecutionSlots(m_executionSlotsBackup);
  }

  @Override
  public void setParentWindow(Window parent) {
    m_parent = parent;

    m_evalMetricsBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EvaluationMetricSelectionDialog esd = new EvaluationMetricSelectionDialog(
            m_parent, m_evaluationMetrics);
        esd.setLocation(m_evalMetricsBut.getLocationOnScreen());
        esd.pack();
        esd.setVisible(true);
        m_evaluationMetrics = esd.getSelectedEvalMetrics();
      }
    });

  }

}
