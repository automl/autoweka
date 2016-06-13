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
 *    LookAndFeel.java
 *    Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import weka.core.Environment;
import weka.core.Settings;
import weka.core.Utils;

import javax.swing.JOptionPane;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * A little helper class for setting the Look and Feel of the user interface.
 * Was necessary, since Java 1.5 sometimes crashed the WEKA GUI (e.g. under
 * Linux/Gnome). Running this class from the commandline will print all
 * available Look and Feel themes.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 12702 $
 */
public class LookAndFeel {

  /** The name of the properties file */
  public static String PROPERTY_FILE = "weka/gui/LookAndFeel.props";

  /** Contains the look and feel properties */
  protected static Properties LOOKANDFEEL_PROPERTIES;

  static {
    try {
      LOOKANDFEEL_PROPERTIES = Utils.readProperties(PROPERTY_FILE);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(null,
        "LookAndFeel: Could not read a LookAndFeel configuration file.\n"
          + "An example file is included in the Weka distribution.\n"
          + "This file should be named \"" + PROPERTY_FILE + "\"  and\n"
          + "should be placed either in your user home (which is set\n"
          + "to \"" + System.getProperties().getProperty("user.home") + "\")\n"
          + "or the directory that java was started from\n", "LookAndFeel",
        JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Get a list of fully qualified class names of available look and feels
   * 
   * @return a list of look and feel class names that are available on this
   *         platform
   */
  public static List<String> getAvailableLookAndFeelClasses() {
    List<String> lafs = new LinkedList<String>();

    for (UIManager.LookAndFeelInfo i : UIManager.getInstalledLookAndFeels()) {
      lafs.add(i.getClassName());
    }

    return lafs;
  }

  /**
   * sets the look and feel to the specified class
   * 
   * @param classname the look and feel to use
   * @return whether setting was successful
   */
  public static boolean setLookAndFeel(String classname) {
    boolean result;

    try {
      UIManager.setLookAndFeel(classname);
      result = true;

      if (System.getProperty("os.name").toLowerCase().contains("mac os x")
        && !classname.contains("com.apple.laf")) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
          .addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
              if (!e.isConsumed()) {
                if (e.isMetaDown()) {
                  if (e.getKeyCode() == KeyEvent.VK_V
                    || e.getKeyCode() == KeyEvent.VK_A
                    || e.getKeyCode() == KeyEvent.VK_C
                    || e.getKeyCode() == KeyEvent.VK_X) {
                    e.setModifiers(KeyEvent.CTRL_DOWN_MASK);
                  }
                }
              }
              return false;
            }
          });
      }

      // workaround for scrollbar handle disappearing bug in Nimbus LAF:
      // https://bugs.openjdk.java.net/browse/JDK-8134828
      if (classname.toLowerCase().contains("nimbus")) {
        javax.swing.LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
        UIDefaults defaults = lookAndFeel.getDefaults();
        defaults.put("ScrollBar.minimumThumbSize", new Dimension(30, 30));
      }
    } catch (Exception e) {
      e.printStackTrace();
      result = false;
    }

    return result;
  }

  /**
   * Set the look and feel from loaded settings
   *
   * @param appID the ID of the application to load settings for
   * @param lookAndFeelKey the key to look up the look and feel in the settings
   * @throws IOException if a problem occurs when loading settings
   */
  public static void setLookAndFeel(String appID, String lookAndFeelKey, String defaultLookAndFeel)
    throws IOException {
    Settings forLookAndFeelOnly = new Settings("weka", appID);

    String laf =
      forLookAndFeelOnly.getSetting(appID, lookAndFeelKey, defaultLookAndFeel,
        Environment.getSystemWide());

    if (laf.length() > 0 && laf.contains(".")
      && LookAndFeel.setLookAndFeel(laf)) {
    } else {
      LookAndFeel.setLookAndFeel();
    }
  }

  /**
   * sets the look and feel to the one in the props-file or if not set the
   * default one of the system
   * 
   * @return whether setting was successful
   */
  public static boolean setLookAndFeel() {
    String classname;

    classname = LOOKANDFEEL_PROPERTIES.getProperty("Theme", "");
    if (classname.equals("")) {
      // Java 1.5 crashes under Gnome if one sets it to the GTKLookAndFeel
      // theme, hence we don't set any theme by default if we're on a Linux
      // box.
      if (System.getProperty("os.name").equalsIgnoreCase("linux")) {
        return true;
      } else {
        classname = getSystemLookAndFeel();
      }
    }

    return setLookAndFeel(classname);
  }

  /**
   * returns the system LnF classname
   * 
   * @return the name of the System LnF class
   */
  public static String getSystemLookAndFeel() {
    return UIManager.getSystemLookAndFeelClassName();
  }

  /**
   * returns an array with the classnames of all the installed LnFs
   * 
   * @return the installed LnFs
   */
  public static String[] getInstalledLookAndFeels() {
    String[] result;
    LookAndFeelInfo[] laf;
    int i;

    laf = UIManager.getInstalledLookAndFeels();
    result = new String[laf.length];
    for (i = 0; i < laf.length; i++)
      result[i] = laf[i].getClassName();

    return result;
  }

  /**
   * prints all the available LnFs to stdout
   * 
   * @param args the commandline options
   */
  public static void main(String[] args) {
    String[] list;
    int i;

    System.out.println("\nInstalled Look and Feel themes:");
    list = getInstalledLookAndFeels();
    for (i = 0; i < list.length; i++)
      System.out.println((i + 1) + ". " + list[i]);

    System.out
      .println("\nNote: a theme can be set in '" + PROPERTY_FILE + "'.");
  }
}
