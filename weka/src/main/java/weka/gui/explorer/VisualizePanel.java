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
 * VisualizePanel.java
 * Copyright (C) 2007-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.explorer;

import weka.core.Defaults;
import weka.core.Instances;
import weka.core.Settings;
import weka.gui.AbstractPerspective;
import weka.gui.PerspectiveInfo;
import weka.gui.explorer.Explorer.ExplorerPanel;
import weka.gui.visualize.MatrixPanel;
import weka.gui.visualize.VisualizeUtils;

import java.awt.BorderLayout;

/**
 * A slightly extended MatrixPanel for better support in the Explorer.
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 12391 $
 * @see MatrixPanel
 */
@PerspectiveInfo(ID = "weka.gui.workbench.visualizepanel", title = "Visualize",
  toolTipText = "Explore the data",
  iconPath = "weka/gui/weka_icon_new_small.png")
public class VisualizePanel extends AbstractPerspective implements
  ExplorerPanel {

  /** for serialization */
  private static final long serialVersionUID = 6084015036853918846L;

  /** the parent frame */
  protected Explorer m_Explorer = null;

  protected MatrixPanel m_matrixPanel = new MatrixPanel();

  /** True if a set of instances has been set on the panel */
  protected boolean m_hasInstancesSet;

  public VisualizePanel() {
    setLayout(new BorderLayout());
    add(m_matrixPanel, BorderLayout.CENTER);
  }

  @Override
  public void setInstances(Instances instances) {
    m_matrixPanel.setInstances(instances);
    m_hasInstancesSet = true;
  }

  /**
   * Sets the Explorer to use as parent frame (used for sending notifications
   * about changes in the data)
   * 
   * @param parent the parent frame
   */
  @Override
  public void setExplorer(Explorer parent) {
    m_Explorer = parent;
  }

  /**
   * returns the parent Explorer frame
   * 
   * @return the parent
   */
  @Override
  public Explorer getExplorer() {
    return m_Explorer;
  }

  /**
   * Returns the title for the tab in the Explorer
   * 
   * @return the title of this tab
   */
  @Override
  public String getTabTitle() {
    return "Visualize";
  }

  /**
   * Returns the tooltip for the tab in the Explorer
   * 
   * @return the tooltip of this tab
   */
  @Override
  public String getTabTitleToolTip() {
    return "Explore the data";
  }

  /**
   * This perspective processes instances
   *
   * @return true, as this perspective accepts instances
   */
  @Override
  public boolean acceptsInstances() {
    return true;
  }

  /**
   * Default settings for the scatter plot
   *
   * @return default settings
   */
  @Override
  public Defaults getDefaultSettings() {
    Defaults d = new ScatterDefaults();
    d.add(new VisualizeUtils.VisualizeDefaults());
    return d;
  }

  @Override
  public boolean okToBeActive() {
    return m_hasInstancesSet;
  }

  /**
   * Make sure current settings are applied when this panel becomes active
   *
   * @param active true if this panel is the visible (active) one
   */
  @Override
  public void setActive(boolean active) {
    super.setActive(active);
    if (m_isActive) {
      settingsChanged();
    }
  }

  @Override
  public void settingsChanged() {
    if (getMainApplication() != null) {
      m_matrixPanel.applySettings(m_mainApplication.getApplicationSettings(),
        ScatterDefaults.ID);
      if (m_isActive) {
        m_matrixPanel.updatePanel();
      }
    }
  }

  /**
   * Default settings specific to the {@code MatrixPanel} that provides the
   * scatter plot matrix
   */
  public static class ScatterDefaults extends Defaults {
    public static final String ID = "weka.gui.workbench.visualizepanel";

    public static final Settings.SettingKey POINT_SIZE_KEY =
      new Settings.SettingKey(ID + ".pointSize",
        "Point size for scatter plots", "");
    public static final int POINT_SIZE = 1;

    public static final Settings.SettingKey PLOT_SIZE_KEY =
      new Settings.SettingKey(ID + ".plotSize",
        "Size (in pixels) of the cells in the matrix", "");
    public static final int PLOT_SIZE = 100;

    public static final long serialVersionUID = -6890761195767034507L;

    public ScatterDefaults() {
      super(ID);

      m_defaults.put(POINT_SIZE_KEY, POINT_SIZE);
      m_defaults.put(PLOT_SIZE_KEY, PLOT_SIZE);
    }
  }

  /**
   * Tests out the visualize panel from the command line.
   *
   * @param args may optionally contain the name of a dataset to load.
   */
  public static void main(String[] args) {

    try {
      final javax.swing.JFrame jf =
        new javax.swing.JFrame("Weka Explorer: Visualize");
      jf.getContentPane().setLayout(new BorderLayout());
      final VisualizePanel sp = new VisualizePanel();
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent e) {
          jf.dispose();
          System.exit(0);
        }
      });
      jf.pack();
      jf.setSize(800, 600);
      jf.setVisible(true);
      if (args.length == 1) {
        System.err.println("Loading instances from " + args[0]);
        java.io.Reader r =
          new java.io.BufferedReader(new java.io.FileReader(args[0]));
        Instances i = new Instances(r);
        sp.setInstances(i);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
