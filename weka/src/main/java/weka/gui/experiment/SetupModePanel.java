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
 *    SetupModePanel.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.experiment;

import weka.experiment.Experiment;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

/** 
 * This panel switches between simple and advanced experiment setup panels.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 12016 $
 */
public class SetupModePanel
  extends JPanel {

  /** for serialization */
  private static final long serialVersionUID = -3758035565520727822L;

  /** the available panels. */
  protected AbstractSetupPanel[] m_Panels = AbstractSetupPanel.getPanels();

  /** the combobox with all available setup panels. */
  protected JComboBox m_ComboBoxPanels;

  /** The simple setup panel */
  protected AbstractSetupPanel m_defaultPanel = null;

  /** The advanced setup panel */
  protected AbstractSetupPanel m_advancedPanel = null;

  /** the current panel. */
  protected AbstractSetupPanel m_CurrentPanel;

  /**
   * Creates the setup panel with no initial experiment.
   */
  public SetupModePanel() {

    // no panels discovered?
    if (m_Panels.length == 0) {
      System.err.println("No experimenter setup panels discovered? Using fallback (simple, advanced).");
      m_Panels = new AbstractSetupPanel[]{
	new SetupPanel(),
	new SimpleSetupPanel()
      };
    }

    for (AbstractSetupPanel panel: m_Panels) {
      if (panel.getClass().getName().equals(ExperimenterDefaults.getSetupPanel()))
	m_defaultPanel = panel;
      if (panel instanceof SetupPanel)
	m_advancedPanel = panel;
      panel.setModePanel(this);
    }

    // fallback on simple setup panel
    if (m_defaultPanel == null) {
      for (AbstractSetupPanel panel: m_Panels) {
	if (panel instanceof SimpleSetupPanel)
	  m_defaultPanel = panel;
      }
    }

    m_CurrentPanel = m_defaultPanel;

    m_ComboBoxPanels = new JComboBox(m_Panels);
    m_ComboBoxPanels.setSelectedItem(m_defaultPanel);
    m_ComboBoxPanels.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
	if (m_ComboBoxPanels.getSelectedIndex() == -1)
	  return;
	AbstractSetupPanel panel = (AbstractSetupPanel) m_ComboBoxPanels.getSelectedItem();
	switchTo(panel, null);
      }
    });

    JPanel switchPanel = new JPanel();
    switchPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    switchPanel.add(new JLabel("Experiment Configuration Mode"));
    switchPanel.add(m_ComboBoxPanels);

    setLayout(new BorderLayout());
    add(switchPanel, BorderLayout.NORTH);
    add(m_defaultPanel, BorderLayout.CENTER);
  }

  /**
   * Switches to the advanced panel.
   *
   * @param exp the experiment to configure
   */
  public void switchToAdvanced(Experiment exp) {
    switchTo(m_advancedPanel, exp);
  }

  /**
   * Switches to the specified panel.
   *
   * @param panel the panel to switch to
   * @param exp the experiment to configure
   */
  public void switchTo(AbstractSetupPanel panel, Experiment exp) {
    if (exp == null)
      exp = m_CurrentPanel.getExperiment();

    remove(m_CurrentPanel);
    m_CurrentPanel.cleanUpAfterSwitch();

    if (exp != null)
      panel.setExperiment(exp);
    add(panel, BorderLayout.CENTER);
    validate();
    repaint();

    m_CurrentPanel = panel;
  }

  /**
   * Adds a PropertyChangeListener who will be notified of value changes.
   *
   * @param l a value of type 'PropertyChangeListener'
   */
  public void addPropertyChangeListener(PropertyChangeListener l) {
    if (m_Panels != null) {
      for (AbstractSetupPanel panel : m_Panels)
        panel.addPropertyChangeListener(l);
    }
  }

  /**
   * Removes a PropertyChangeListener who will be notified of value changes.
   *
   * @param l a value of type 'PropertyChangeListener'
   */
  public void removePropertyChangeListener(PropertyChangeListener l) {
    if (m_Panels != null) {
      for (AbstractSetupPanel panel : m_Panels)
        panel.removePropertyChangeListener(l);
    }
  }

  /**
   * Gets the currently configured experiment.
   *
   * @return the currently configured experiment.
   */
  public Experiment getExperiment() {
    return m_CurrentPanel.getExperiment();
  }
}
