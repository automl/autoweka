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
 *    GUIChooserApp.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import weka.core.Copyright;
import weka.core.Version;

import javax.swing.JMenuBar;
import java.util.Arrays;
import java.util.List;

/**
 * Launcher class for the Weka GUIChooser. Displays a splash window and
 * launches the actual GUIChooser app (GUIChooserApp).
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class GUIChooser {

  /**
   * Interface for plugin components that can be accessed from either the
   * Visualization or Tools menu.
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static interface GUIChooserMenuPlugin {

    /** Enum listing possible menus that plugins can appear in */
    public static enum Menu {
      TOOLS, VISUALIZATION
    };

    /**
     * Get the name to display in title bar of the enclosing JFrame for the
     * plugin
     *
     * @return the name to display in the title bar
     */
    String getApplicationName();

    /**
     * Get the menu that the plugin is to be listed in
     *
     * @return the menu that the plugin is to be listed in
     */
    Menu getMenuToDisplayIn();

    /**
     * Get the text entry to appear in the menu
     *
     * @return the text entry to appear in the menu
     */
    String getMenuEntryText();

    /**
     * Return the menu bar for this plugin
     *
     * @return the menu bar for this plugin or null if it does not use a menu
     *         bar
     */
    JMenuBar getMenuBar();
  }

  public static void main(String[] args) {
    List<String> message =
      Arrays.asList("Waikato Environment for Knowledge Analysis",
        "Version " + Version.VERSION,
        "(c) " + Copyright.getFromYear() + " - " + Copyright.getToYear(),
        "The University of Waikato", "Hamilton, New Zealand");
    weka.gui.SplashWindow.splash(
      ClassLoader.getSystemResource("weka/gui/weka_icon_new.png"), message);
    weka.gui.SplashWindow.invokeMain("weka.gui.GUIChooserApp", args);
    weka.gui.SplashWindow.disposeSplash();
  }
}
