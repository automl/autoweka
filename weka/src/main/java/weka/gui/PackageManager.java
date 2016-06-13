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
 *    PackageManager.java
 *    Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.gui;

import weka.core.Environment;
import weka.core.Utils;
import weka.core.Version;
import weka.core.WekaPackageManager;
import weka.core.packageManagement.Dependency;
import weka.core.packageManagement.Package;
import weka.core.packageManagement.PackageConstraint;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A GUI interface the the package management system.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 12591 $
 */
public class PackageManager extends JPanel {

  /** For serialization */
  private static final long serialVersionUID = -7463821313750352385L;

  protected static final String PACKAGE_COLUMN = "Package";
  protected static final String CATEGORY_COLUMN = "Category";
  protected static final String INSTALLED_COLUMN = "Installed version";
  protected static final String REPOSITORY_COLUMN = "Repository version";
  protected static final String LOADED_COLUMN = "Loaded";

  /** The JTable for displaying the package names and version numbers */
  protected JTable m_table = new ETable();

  protected JSplitPane m_splitP;

  // protected JTextArea m_packageDescription;

  /** An editor pane to display package information */
  protected JEditorPane m_infoPane;

  /** Installed radio button */
  protected JRadioButton m_installedBut = new JRadioButton("Installed");

  /** Available radio button */
  protected JRadioButton m_availableBut = new JRadioButton("Available");

  /** All radio button */
  protected JRadioButton m_allBut = new JRadioButton("All");

  /** Button for installing the selected package */
  protected JButton m_installBut = new JButton("Install");
  protected JCheckBox m_forceBut = new JCheckBox(
    "Ignore dependencies/conflicts");

  /** Button for uninstalling the selected package */
  protected JButton m_uninstallBut = new JButton("Uninstall");

  /** Button for refreshing the package meta data cache */
  protected JButton m_refreshCacheBut = new JButton("Refresh repository cache");

  /** Button for toggling the load status of an installed package */
  protected JButton m_toggleLoad = new JButton("Toggle load");

  protected JProgressBar m_progress = new JProgressBar(0, 100);
  protected JLabel m_detailLabel = new JLabel();

  protected JButton m_backB;
  protected LinkedList<URL> m_browserHistory = new LinkedList<URL>();
  protected static final String BROWSER_HOME =
    "http://www.cs.waikato.ac.nz/ml/weka/index_home_pm.html";
  protected JButton m_homeB;

  protected JToolBar m_browserTools;

  protected JLabel m_newPackagesAvailableL;

  protected DefaultTableModel m_model;

  protected Map<String, List<Object>> m_packageLookupInfo;

  protected List<Package> m_allPackages;
  protected List<Package> m_installedPackages;
  protected List<Package> m_availablePackages;

  protected Map<String, String> m_packageDescriptions =
    new HashMap<String, String>();
  protected List<Package> m_searchResults = new ArrayList<Package>();
  protected JTextField m_searchField = new JTextField(15);
  protected JLabel m_searchHitsLab = new JLabel("");

  /** The column in the table to sort the entries by */
  protected int m_sortColumn = 0;

  /** Reverse the sort order if the user clicks the same column header twice */
  protected boolean m_reverseSort = false;

  /** Button to pop up the file environment field widget */
  protected JButton m_unofficialBut = new JButton("File/URL");

  /** Widget for specifying a URL or path to an unofficial package to install */
  protected FileEnvironmentField m_unofficialChooser =
    new FileEnvironmentField("File/URL", Environment.getSystemWide());
  protected JFrame m_unofficialFrame = null;

  public static boolean s_atLeastOnePackageUpgradeHasOccurredInThisSession =
    false;

  protected Comparator<Package> m_packageComparator =
    new Comparator<Package>() {

      @Override
      public int compare(Package o1, Package o2) {
        String meta1 = "";
        String meta2 = "";
        if (m_sortColumn == 0) {
          meta1 = o1.getName();
          meta2 = o2.getName();
        } else {
          if (o1.getPackageMetaDataElement("Category") != null) {
            meta1 = o1.getPackageMetaDataElement("Category").toString();
          }

          if (o2.getPackageMetaDataElement("Category") != null) {
            meta2 = o2.getPackageMetaDataElement("Category").toString();
          }
        }

        int result = meta1.compareTo(meta2);
        if (m_reverseSort) {
          result = -result;
        }
        return result;
      }
    };

  protected boolean m_installing = false;

  class ProgressPrintStream extends PrintStream {

    private final Progressable m_listener;

    public ProgressPrintStream(Progressable listener) {
      // have to invoke a super class constructor
      super(System.out);
      m_listener = listener;
    }

    @Override
    public void println(String string) {
      boolean messageOnly = false;
      if (string.startsWith("%%")) {
        string = string.substring(2);
        messageOnly = true;
      }
      if (!messageOnly) {
        System.out.println(string); // make sure the log picks it up
        m_listener.makeProgress(string);
      } else {
        m_listener.makeProgressMessageOnly(string);
      }
    }

    @Override
    public void println(Object obj) {
      println(obj.toString());
    }

    @Override
    public void print(String string) {
      boolean messageOnly = false;
      if (string.startsWith("%%")) {
        string = string.substring(2);
        messageOnly = true;
      }

      if (!messageOnly) {
        System.out.print(string); // make sure the log picks it up
        m_listener.makeProgress(string);
      } else {
        m_listener.makeProgressMessageOnly(string);
      }
    }

    @Override
    public void print(Object obj) {
      print(obj.toString());
    }
  }

  interface Progressable {
    void makeProgress(String progressMessage);

    void makeProgressMessageOnly(String progressMessage);
  }

  class EstablishCache extends SwingWorker<Void, Void> implements Progressable {
    private int m_progressCount = 0;
    private Exception m_error = null;

    private javax.swing.ProgressMonitor m_progress;

    @Override
    public void makeProgress(String progressMessage) {
      m_progress.setNote(progressMessage);
      m_progressCount++;
      m_progress.setProgress(m_progressCount);
    }

    @Override
    public void makeProgressMessageOnly(String progressMessage) {
      m_progress.setNote(progressMessage);
    }

    @Override
    public Void doInBackground() {
      int numPackages = WekaPackageManager.numRepositoryPackages();
      if (numPackages < 0) {
        // there was some problem getting the file that holds this
        // information from the repository server - try to continue
        // anyway with a max value of 100 for the number of packages
        // (since all we use this for is setting the upper bound on
        // the progress bar).
        numPackages = 100;
      }
      m_progress =
        new javax.swing.ProgressMonitor(PackageManager.this,
          "Establising cache...", "", 0, numPackages);
      ProgressPrintStream pps = new ProgressPrintStream(this);
      m_error = WekaPackageManager.establishCacheIfNeeded(pps);

      m_cacheEstablished = true;
      return null;
    }

    @Override
    public void done() {
      m_progress.close();
      if (m_error != null) {
        displayErrorDialog("There was a problem establishing the package\n"
          + "meta data cache. We'll try to use the repository" + "directly.",
          m_error);
      }
    }
  }

  class CheckForNewPackages extends SwingWorker<Void, Void> {

    @Override
    public Void doInBackground() {
      Map<String, String> localPackageNameList =
        WekaPackageManager.getPackageList(true);

      if (localPackageNameList == null) {
        // quietly return and see if we can continue anyway
        return null;
      }

      Map<String, String> repositoryPackageNameList =
        WekaPackageManager.getPackageList(false);

      if (repositoryPackageNameList == null) {
        // quietly return and see if we can continue anyway
        return null;
      }

      if (repositoryPackageNameList.keySet().size() < localPackageNameList
        .keySet().size()) {
        // package(s) have disappeared from the repository.
        // Force a cache refresh...
        RefreshCache r = new RefreshCache();
        r.execute();

        return null;
      }

      StringBuffer newPackagesBuff = new StringBuffer();
      StringBuffer updatedPackagesBuff = new StringBuffer();

      for (String s : repositoryPackageNameList.keySet()) {
        if (!localPackageNameList.containsKey(s)) {
          newPackagesBuff.append(s + "<br>");
        }
      }

      for (String localPackage : localPackageNameList.keySet()) {
        String localVersion = localPackageNameList.get(localPackage);

        String repoVersion = repositoryPackageNameList.get(localPackage);
        if (repoVersion == null) {
          continue;
        }

        // a difference here indicates a newer version on the server
        if (!localVersion.equals(repoVersion)) {
          updatedPackagesBuff.append(localPackage + " (" + repoVersion
            + ")<br>");
        }
      }

      if (newPackagesBuff.length() > 0 || updatedPackagesBuff.length() > 0) {
        String information =
          "<html><font size=-2>There are new and/or updated packages available "
            + "on the server (do a cache refresh for more " + "information):";
        if (newPackagesBuff.length() > 0) {
          information += "<br><br><b>New:</b><br>" + newPackagesBuff.toString();
        }
        if (updatedPackagesBuff.length() > 0) {
          information +=
            "<br><br><b>Updated:</b><br>" + updatedPackagesBuff.toString()
              + "<br><br>";
        }
        information += "</font></html>";
        m_newPackagesAvailableL.setToolTipText(information);
        m_browserTools.add(m_newPackagesAvailableL);

        m_browserTools.revalidate();
      }

      return null;
    }
  }

  class RefreshCache extends SwingWorker<Void, Void> implements Progressable {
    private int m_progressCount = 0;
    private Exception m_error = null;

    @Override
    public void makeProgress(String progressMessage) {
      m_detailLabel.setText(progressMessage);
      if (progressMessage.startsWith("[Default")) {
        // We're using the new refresh mechanism - extract the number
        // of KB read from the message
        String kbs =
          progressMessage.replace("[DefaultPackageManager] downloaded ", "");
        kbs = kbs.replace(" KB", "");
        m_progressCount = Integer.parseInt(kbs);
      } else {
        m_progressCount++;
      }
      m_progress.setValue(m_progressCount);
    }

    @Override
    public void makeProgressMessageOnly(String progressMessage) {
      m_detailLabel.setText(progressMessage);
    }

    @Override
    public Void doInBackground() {
      m_cacheRefreshInProgress = true;
      int progressUpper = WekaPackageManager.repoZipArchiveSize();
      if (progressUpper == -1) {
        // revert to legacy approach
        progressUpper = WekaPackageManager.numRepositoryPackages();
      }

      if (progressUpper < 0) {
        // there was some problem getting the file that holds this
        // information from the repository server - try to continue
        // anyway with a max value of 100 for the number of packages
        // (since all we use this for is setting the upper bound on
        // the progress bar).
        progressUpper = 100;
      }

      // number of KBs for the archive is approx 6 x # packages
      m_progress.setMaximum(progressUpper);
      m_refreshCacheBut.setEnabled(false);
      m_installBut.setEnabled(false);
      m_unofficialBut.setEnabled(false);
      m_installedBut.setEnabled(false);
      m_availableBut.setEnabled(false);
      m_allBut.setEnabled(false);
      ProgressPrintStream pps = new ProgressPrintStream(this);
      m_error = WekaPackageManager.refreshCache(pps);
      getAllPackages();
      return null;
    }

    @Override
    public void done() {
      m_progress.setValue(m_progress.getMinimum());
      if (m_error != null) {
        displayErrorDialog("There was a problem refreshing the package\n"
          + "meta data cache. We'll try to use the repository" + "directly.",
          m_error);
        m_detailLabel.setText("");
      } else {
        m_detailLabel.setText("Cache refresh completed");
      }

      m_installBut.setEnabled(true && !WekaPackageManager.m_offline);
      m_unofficialBut.setEnabled(true);
      m_refreshCacheBut.setEnabled(true && !WekaPackageManager.m_offline);
      m_installedBut.setEnabled(true);
      m_availableBut.setEnabled(true);
      m_allBut.setEnabled(true);

      m_availablePackages = null;
      updateTable();

      try {
        m_browserTools.remove(m_newPackagesAvailableL);
        m_browserTools.revalidate();
      } catch (Exception ex) {
      }

      m_cacheRefreshInProgress = false;
    }
  }

  private void pleaseCloseAppWindowsPopUp() {
    if (!Utils
      .getDontShowDialog("weka.gui.PackageManager.PleaseCloseApplicationWindows")) {
      JCheckBox dontShow = new JCheckBox("Do not show this message again");
      Object[] stuff = new Object[2];
      stuff[0] =
        "Please close any open Weka application windows\n"
          + "(Explorer, Experimenter, KnowledgeFlow, SimpleCLI)\n"
          + "before proceeding.\n";
      stuff[1] = dontShow;

      JOptionPane.showMessageDialog(PackageManager.this, stuff,
        "Weka Package Manager", JOptionPane.OK_OPTION);

      if (dontShow.isSelected()) {
        try {
          Utils
            .setDontShowDialog("weka.gui.PackageManager.PleaseCloseApplicationWindows");
        } catch (Exception ex) {
          // quietly ignore
        }
      }
    }
  }

  private void toggleLoadStatusRequiresRestartPopUp() {
    if (!Utils
      .getDontShowDialog("weka.gui.PackageManager.ToggleLoadStatusRequiresRestart")) {
      JCheckBox dontShow = new JCheckBox("Do not show this message again");
      Object[] stuff = new Object[2];
      stuff[0] =
        "Changing a package's load status will require a restart for the change to take affect\n";

      stuff[1] = dontShow;

      JOptionPane.showMessageDialog(PackageManager.this, stuff,
        "Weka Package Manager", JOptionPane.OK_OPTION);

      if (dontShow.isSelected()) {
        try {
          Utils
            .setDontShowDialog("weka.gui.PackageManager.ToggleLoadStatusRequiresRestart");
        } catch (Exception ex) {
          // quietly ignore
        }
      }
    }
  }

  class UninstallTask extends SwingWorker<Void, Void> implements Progressable {

    private List<String> m_packageNamesToUninstall;
    // private String m_packageName;
    // private boolean m_successfulUninstall = false;
    private final List<String> m_unsuccessfulUninstalls =
      new ArrayList<String>();

    private int m_progressCount = 0;

    public void setPackages(List<String> packageNames) {
      m_packageNamesToUninstall = packageNames;
    }

    @Override
    public void makeProgress(String progressMessage) {
      m_detailLabel.setText(progressMessage);
      m_progressCount++;
      m_progress.setValue(m_progressCount);
      if (m_progressCount == m_progress.getMaximum()) {
        m_progress.setMaximum(m_progressCount + 5);
      }
    }

    @Override
    public void makeProgressMessageOnly(String progressMessage) {
      m_detailLabel.setText(progressMessage);
    }

    @Override
    public Void doInBackground() {
      m_installing = true;
      m_installBut.setEnabled(false);
      m_unofficialBut.setEnabled(false);
      m_uninstallBut.setEnabled(false);
      m_refreshCacheBut.setEnabled(false);
      m_toggleLoad.setEnabled(false);
      m_availableBut.setEnabled(false);
      m_allBut.setEnabled(false);
      m_installedBut.setEnabled(false);

      ProgressPrintStream pps = new ProgressPrintStream(this);
      m_progress.setMaximum(m_packageNamesToUninstall.size() * 5);

      for (int zz = 0; zz < m_packageNamesToUninstall.size(); zz++) {

        String packageName = m_packageNamesToUninstall.get(zz);

        boolean explorerPropertiesExist =
          WekaPackageManager.installedPackageResourceExists(packageName,
            "Explorer.props");

        if (!m_forceBut.isSelected()) {
          List<Package> compromised = new ArrayList<Package>();

          // Now check to see which other installed packages depend on this one
          List<Package> installedPackages;
          try {
            installedPackages = WekaPackageManager.getInstalledPackages();
          } catch (Exception e) {
            e.printStackTrace();
            displayErrorDialog("Can't determine which packages are installed!",
              e);
            // return null; // can't proceed
            m_unsuccessfulUninstalls.add(packageName);
            continue;
          }
          for (Package p : installedPackages) {
            List<Dependency> tempDeps;
            try {
              tempDeps = p.getDependencies();
            } catch (Exception e) {
              e.printStackTrace();
              displayErrorDialog(
                "Problem determining dependencies for package : " + p.getName(),
                e);
              // return null; // can't proceed
              m_unsuccessfulUninstalls.add(packageName);
              continue;
            }

            for (Dependency d : tempDeps) {
              if (d.getTarget().getPackage().getName().equals(packageName)) {
                // add this installed package to the list
                compromised.add(p);
                break;
              }
            }
          }

          if (compromised.size() > 0) {
            StringBuffer message = new StringBuffer();
            message.append("The following installed packages depend on "
              + packageName + " :\n\n");
            for (Package p : compromised) {
              message.append("\t" + p.getName() + "\n");
            }

            message.append("\nDo you wish to proceed?");
            int result =
              JOptionPane.showConfirmDialog(PackageManager.this,
                message.toString(), "Weka Package Manager",
                JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.NO_OPTION) {
              // bail out here
              // return null;
              continue;
            }
          }
        }

        // m_progress.setMaximum(10);
        try {
          if (explorerPropertiesExist) {
            // need to remove any set Explorer properties first
            WekaPackageManager.removeExplorerProps(packageName);
          }
          WekaPackageManager.uninstallPackage(packageName, true, pps);

        } catch (Exception e) {
          e.printStackTrace();
          displayErrorDialog("Unable to uninstall package: " + packageName, e);
          // return null;
          m_unsuccessfulUninstalls.add(packageName);
          continue;
        }
      }

      WekaPackageManager.refreshGOEProperties();
      // m_successfulUninstall = true;

      return null;
    }

    @Override
    public void done() {
      m_progress.setValue(m_progress.getMinimum());
      if (m_unsuccessfulUninstalls.size() == 0) {
        m_detailLabel.setText("Packages removed successfully.");

        if (!Utils
          .getDontShowDialog("weka.gui.PackageManager.RestartAfterUninstall")) {
          JCheckBox dontShow = new JCheckBox("Do not show this message again");
          Object[] stuff = new Object[2];
          stuff[0] =
            "Weka might need to be restarted for\n"
              + "the changes to come into effect.\n";
          stuff[1] = dontShow;

          JOptionPane.showMessageDialog(PackageManager.this, stuff,
            "Weka Package Manager", JOptionPane.OK_OPTION);

          if (dontShow.isSelected()) {
            try {
              Utils
                .setDontShowDialog("weka.gui.PackageManager.RestartAfterUninstall");
            } catch (Exception ex) {
              // quietly ignore
            }
          }
        }
      } else {
        StringBuffer failedPackageNames = new StringBuffer();
        for (String p : m_unsuccessfulUninstalls) {
          failedPackageNames.append(p + "\n");
        }
        displayErrorDialog(
          "The following package(s) could not be uninstalled\n"
            + "for some reason (check the log)\n"
            + failedPackageNames.toString(), "");
        m_detailLabel.setText("Finished uninstalling.");
      }

      m_unofficialBut.setEnabled(true);
      m_refreshCacheBut.setEnabled(true);
      m_availableBut.setEnabled(true);
      m_allBut.setEnabled(true);
      m_installedBut.setEnabled(true);

      // force refresh of installed and available packages
      m_installedPackages = null;
      m_availablePackages = null;
      // m_installBut.setEnabled(true);
      m_installing = false;
      updateTable();
      if (m_table.getSelectedRow() >= 0) {
        // mainly to update the install/uninstall button status
        // displayPackageInfo(m_table.getSelectedRow());
        updateInstallUninstallButtonEnablement();
      }
    }
  }

  class UnofficialInstallTask extends SwingWorker<Void, Void> implements
    Progressable {

    private String m_target;
    private int m_progressCount = 0;
    private boolean m_errorOccurred = false;

    public void setTargetToInstall(String target) {
      m_target = target;
    }

    @Override
    public void makeProgress(String progressMessage) {
      m_detailLabel.setText(progressMessage);
      m_progressCount++;
      m_progress.setValue(m_progressCount);
      if (m_progressCount == m_progress.getMaximum()) {
        m_progress.setMaximum(m_progressCount + 5);
      }
    }

    @Override
    public void makeProgressMessageOnly(String progressMessage) {
      m_detailLabel.setText(progressMessage);
    }

    @Override
    public Void doInBackground() {
      m_installing = true;
      m_installBut.setEnabled(false);
      m_uninstallBut.setEnabled(false);
      m_refreshCacheBut.setEnabled(false);
      m_toggleLoad.setEnabled(false);
      m_unofficialBut.setEnabled(false);
      m_availableBut.setEnabled(false);
      m_allBut.setEnabled(false);
      m_installedBut.setEnabled(false);
      ProgressPrintStream pps = new ProgressPrintStream(this);
      m_progress.setMaximum(30);

      Package installedPackage = null;
      String toInstall = m_target;
      try {
        toInstall = Environment.getSystemWide().substitute(m_target);
      } catch (Exception ex) {
      }

      try {
        if (toInstall.toLowerCase().startsWith("http://")
          || toInstall.toLowerCase().startsWith("https://")) {
          String packageName =
            WekaPackageManager.installPackageFromURL(new URL(toInstall), pps);
          installedPackage =
            WekaPackageManager.getInstalledPackageInfo(packageName);
        } else if (toInstall.toLowerCase().endsWith(".zip")) {
          String packageName =
            WekaPackageManager.installPackageFromArchive(toInstall, pps);
          installedPackage =
            WekaPackageManager.getInstalledPackageInfo(packageName);
        } else {
          displayErrorDialog("Unable to install package " + "\nfrom "
            + toInstall + ". Unrecognized as a URL or zip archive.",
            (String) null);
          m_errorOccurred = true;
          pps.close();
          return null;
        }
      } catch (Exception ex) {
        displayErrorDialog("Unable to install package " + "\nfrom " + m_target
          + ". Check the log for error messages.", ex);
        m_errorOccurred = true;
        return null;
      }

      if (installedPackage != null) {
        if (!Utils
          .getDontShowDialog("weka.gui.PackageManager.RestartAfterUpgrade")) {
          JCheckBox dontShow = new JCheckBox("Do not show this message again");
          Object[] stuff = new Object[2];
          stuff[0] =
            "Weka will need to be restared after installation for\n"
              + "the changes to come into effect.\n";
          stuff[1] = dontShow;

          JOptionPane.showMessageDialog(PackageManager.this, stuff,
            "Weka Package Manager", JOptionPane.OK_OPTION);

          if (dontShow.isSelected()) {
            try {
              Utils
                .setDontShowDialog("weka.gui.PackageManager.RestartAfterUpgrade");
            } catch (Exception ex) {
              // quietly ignore
            }
          }
        }

        try {
          File packageRoot =
            new File(WekaPackageManager.getPackageHome() + File.separator
              + installedPackage.getName());
          boolean loadCheck =
            WekaPackageManager.loadCheck(installedPackage, packageRoot, pps);

          if (!loadCheck) {
            displayErrorDialog("Package was installed correctly but could not "
              + "be loaded. Check log for details", (String) null);
          }
        } catch (Exception ex) {
          displayErrorDialog("Unable to install package " + "\nfrom "
            + m_target + ".", ex);
          m_errorOccurred = true;
        }

        // since we can't determine whether an unofficial package is installed
        // already before performing the install/upgrade (due to the fact that
        // the package name isn't known until the archive is unpacked) we will
        // not refresh the GOE properties and make the user restart Weka in
        // order
        // to be safe and avoid any conflicts between old and new versions of
        // classes
        // for this package
        // WekaPackageManager.refreshGOEProperties();
      }
      return null;
    }

    @Override
    public void done() {
      m_progress.setValue(m_progress.getMinimum());
      if (m_errorOccurred) {
        m_detailLabel.setText("Problem installing - check log.");
      } else {
        m_detailLabel.setText("Package installed successfully.");
      }

      m_unofficialBut.setEnabled(true);
      m_refreshCacheBut.setEnabled(true && !WekaPackageManager.m_offline);
      m_availableBut.setEnabled(true);
      m_allBut.setEnabled(true);
      m_installedBut.setEnabled(true);

      // force refresh of installed and available packages
      m_installedPackages = null;
      m_availablePackages = null;

      // m_installBut.setEnabled(true);
      m_installing = false;
      updateTable();
      if (m_table.getSelectedRow() >= 0) {
        // mainly to update the install/uninstall button status
        // displayPackageInfo(m_table.getSelectedRow());
        updateInstallUninstallButtonEnablement();
      }
    }
  }

  class InstallTask extends SwingWorker<Void, Void> implements Progressable {

    private List<String> m_packageNamesToInstall;
    private List<Object> m_versionsToInstall;

    // private boolean m_successfulInstall = false;
    private final List<Package> m_unsuccessfulInstalls =
      new ArrayList<Package>();

    private int m_progressCount = 0;

    public void setPackages(List<String> packagesToInstall) {
      m_packageNamesToInstall = packagesToInstall;
    }

    public void setVersions(List<Object> versionsToInstall) {
      m_versionsToInstall = versionsToInstall;
    }

    @Override
    public void makeProgress(String progressMessage) {
      m_detailLabel.setText(progressMessage);
      m_progressCount++;
      m_progress.setValue(m_progressCount);
      if (m_progressCount == m_progress.getMaximum()) {
        m_progress.setMaximum(m_progressCount + 5);
      }
    }

    @Override
    public void makeProgressMessageOnly(String progressMessage) {
      m_detailLabel.setText(progressMessage);
    }

    /*
     * Main task. Executed in background thread.
     */
    @Override
    public Void doInBackground() {
      m_installing = true;
      m_installBut.setEnabled(false);
      m_unofficialBut.setEnabled(true);
      m_uninstallBut.setEnabled(false);
      m_refreshCacheBut.setEnabled(false);
      m_toggleLoad.setEnabled(false);
      m_availableBut.setEnabled(false);
      m_allBut.setEnabled(false);
      m_installedBut.setEnabled(false);
      ProgressPrintStream pps = new ProgressPrintStream(this);
      m_progress.setMaximum(m_packageNamesToInstall.size() * 30);

      for (int zz = 0; zz < m_packageNamesToInstall.size(); zz++) {
        Package packageToInstall = null;
        String packageName = m_packageNamesToInstall.get(zz);
        Object versionToInstall = m_versionsToInstall.get(zz);
        try {
          packageToInstall =
            WekaPackageManager.getRepositoryPackageInfo(packageName,
              versionToInstall.toString());
        } catch (Exception e) {
          e.printStackTrace();
          displayErrorDialog("Unable to obtain package info for package: "
            + packageName, e);
          // return null; // bail out here
          m_unsuccessfulInstalls.add(packageToInstall);
          continue;
        }

        // check for any special installation instructions
        Object specialInstallMessage =
          packageToInstall
            .getPackageMetaDataElement("MessageToDisplayOnInstallation");
        if (specialInstallMessage != null
          && specialInstallMessage.toString().length() > 0) {
          String siM = specialInstallMessage.toString();
          try {
            siM = Environment.getSystemWide().substitute(siM);
          } catch (Exception ex) {
            // quietly ignore
          }
          JOptionPane.showMessageDialog(PackageManager.this, packageToInstall
            + "\n\n" + siM, "Weka Package Manager", JOptionPane.OK_OPTION);
        }

        if (!m_forceBut.isSelected()) {
          try {
            if (!packageToInstall.isCompatibleBaseSystem()) {
              List<Dependency> baseSysDep =
                packageToInstall.getBaseSystemDependency();
              StringBuffer depList = new StringBuffer();
              for (Dependency bd : baseSysDep) {
                depList.append(bd.getTarget().toString() + " ");
              }

              JOptionPane.showMessageDialog(PackageManager.this,
                "Unable to install package " + "\n" + packageName
                  + " because it requires" + "\n" + depList.toString(),
                "Weka Package Manager", JOptionPane.ERROR_MESSAGE);
              // bail out here
              // return null;
              m_unsuccessfulInstalls.add(packageToInstall);
              continue;
            }
          } catch (Exception e) {
            e.printStackTrace();
            displayErrorDialog("Problem determining dependency on base system"
              + " for package: " + packageName, e);
            // return null; // can't proceed
            m_unsuccessfulInstalls.add(packageToInstall);
            continue;
          }

          // check to see if package is already installed
          if (packageToInstall.isInstalled()) {
            Package installedVersion = null;
            try {
              installedVersion =
                WekaPackageManager.getInstalledPackageInfo(packageName);
            } catch (Exception e) {
              e.printStackTrace();
              displayErrorDialog("Problem obtaining package info for package: "
                + packageName, e);
              // return null; // can't proceed
              m_unsuccessfulInstalls.add(packageToInstall);
              continue;
            }

            if (!packageToInstall.equals(installedVersion)) {
              int result =
                JOptionPane.showConfirmDialog(PackageManager.this, "Package "
                  + installedVersion + " is already installed. Replace with "
                  + packageToInstall + "?", "Weka Package Manager",
                  JOptionPane.YES_NO_OPTION);
              if (result == JOptionPane.NO_OPTION) {
                // bail out here
                // return null;
                m_unsuccessfulInstalls.add(packageToInstall);
                continue;
              }

              if (!Utils
                .getDontShowDialog("weka.gui.PackageManager.RestartAfterUpgrade")) {
                JCheckBox dontShow =
                  new JCheckBox("Do not show this message again");
                Object[] stuff = new Object[2];
                stuff[0] =
                  "Weka will need to be restared after installation for\n"
                    + "the changes to come into effect.\n";
                stuff[1] = dontShow;

                JOptionPane.showMessageDialog(PackageManager.this, stuff,
                  "Weka Package Manager", JOptionPane.OK_OPTION);

                if (dontShow.isSelected()) {
                  try {
                    Utils
                      .setDontShowDialog("weka.gui.PackageManager.RestartAfterUpgrade");
                  } catch (Exception ex) {
                    // quietly ignore
                  }
                }
              }
            } else {
              int result =
                JOptionPane.showConfirmDialog(PackageManager.this, "Package "
                  + installedVersion + " is already installed. Install again?",
                  "Weka Package Manager", JOptionPane.YES_NO_OPTION);
              if (result == JOptionPane.NO_OPTION) {
                // bail out here
                // return null;
                m_unsuccessfulInstalls.add(packageToInstall);
                continue;
              }
            }
          }

          // Now get a full list of dependencies for this package and
          // check for any conflicts
          Map<String, List<Dependency>> conflicts =
            new HashMap<String, List<Dependency>>();
          List<Dependency> dependencies = null;
          try {
            dependencies =
              WekaPackageManager.getAllDependenciesForPackage(packageToInstall,
                conflicts);
          } catch (Exception e) {
            e.printStackTrace();
            displayErrorDialog(
              "Problem determinining dependencies for package: "
                + packageToInstall.getName(), e);
            // return null; // can't proceed
            m_unsuccessfulInstalls.add(packageToInstall);
            continue;
          }

          if (conflicts.size() > 0) {
            StringBuffer message = new StringBuffer();
            message.append("Package " + packageName
              + " requires the following packages:\n\n");
            Iterator<Dependency> depI = dependencies.iterator();
            while (depI.hasNext()) {
              Dependency d = depI.next();
              message.append("\t" + d + "\n");
            }

            message.append("\nThere are conflicting dependencies:\n\n");
            Set<String> pNames = conflicts.keySet();
            Iterator<String> pNameI = pNames.iterator();
            while (pNameI.hasNext()) {
              String pName = pNameI.next();
              message.append("Conflicts for " + pName + "\n");
              List<Dependency> confsForPackage = conflicts.get(pName);
              Iterator<Dependency> confs = confsForPackage.iterator();
              while (confs.hasNext()) {
                Dependency problem = confs.next();
                message.append("\t" + problem + "\n");
              }
            }

            JOptionPane
              .showConfirmDialog(PackageManager.this, message.toString(),
                "Weka Package Manager", JOptionPane.OK_OPTION);

            // bail out here
            // return null;
            m_unsuccessfulInstalls.add(packageToInstall);
            continue;
          }

          // Next check all dependencies against what is installed and
          // inform the user about which installed packages will be altered.
          // Also
          // build the list of only those packages that need to be installed or
          // upgraded (excluding those that are already installed and are OK).
          List<PackageConstraint> needsUpgrade =
            new ArrayList<PackageConstraint>();
          List<Package> finalListToInstall = new ArrayList<Package>();

          Iterator<Dependency> depI = dependencies.iterator();
          boolean depsOk = true;
          while (depI.hasNext()) {
            Dependency toCheck = depI.next();
            if (toCheck.getTarget().getPackage().isInstalled()) {
              String toCheckName =
                toCheck.getTarget().getPackage()
                  .getPackageMetaDataElement("PackageName").toString();
              try {
                Package installedVersion =
                  WekaPackageManager.getInstalledPackageInfo(toCheckName);
                if (!toCheck.getTarget().checkConstraint(installedVersion)) {
                  needsUpgrade.add(toCheck.getTarget());
                  Package mostRecent = toCheck.getTarget().getPackage();
                  if (toCheck.getTarget() instanceof weka.core.packageManagement.VersionPackageConstraint) {
                    mostRecent =
                      WekaPackageManager
                        .mostRecentVersionWithRespectToConstraint(toCheck
                          .getTarget());
                  }
                  finalListToInstall.add(mostRecent);
                }
              } catch (Exception ex) {
                ex.printStackTrace();
                displayErrorDialog("An error has occurred while checking "
                  + "package dependencies", ex);
                // bail out here
                // return null;
                depsOk = false;
                break;
              }
            } else {
              try {
                Package mostRecent = toCheck.getTarget().getPackage();
                if (toCheck.getTarget() instanceof weka.core.packageManagement.VersionPackageConstraint) {
                  mostRecent =
                    WekaPackageManager
                      .mostRecentVersionWithRespectToConstraint(toCheck
                        .getTarget());
                }
                finalListToInstall.add(mostRecent);
              } catch (Exception ex) {
                ex.printStackTrace();
                displayErrorDialog("An error has occurred while checking "
                  + "package dependencies", ex);
                // bail out here
                // return null;
                depsOk = false;
                break;
              }
            }
          }

          if (!depsOk) {
            // bail out on this package
            m_unsuccessfulInstalls.add(packageToInstall);
            continue;
          }

          if (needsUpgrade.size() > 0) {
            StringBuffer temp = new StringBuffer();
            for (PackageConstraint pc : needsUpgrade) {
              temp.append(pc + "\n");
            }
            int result =
              JOptionPane.showConfirmDialog(PackageManager.this,
                "The following packages will be upgraded in order to install:\n\n"
                  + temp.toString(), "Weka Package Manager",
                JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.NO_OPTION) {
              // bail out here
              // return null;
              m_unsuccessfulInstalls.add(packageToInstall);
              continue;
            }

            // now take a look at the other installed packages and see if
            // any would have a problem when these ones are upgraded
            boolean conflictsAfterUpgrade = false;
            List<Package> installed = null;
            try {
              installed = WekaPackageManager.getInstalledPackages();
            } catch (Exception e) {
              e.printStackTrace();
              displayErrorDialog(
                "Unable to determine what packages are installed!", e);
              // return null; // can't proceed
              m_unsuccessfulInstalls.add(packageToInstall);
              continue;
            }
            List<Package> toUpgrade = new ArrayList<Package>();
            for (PackageConstraint pc : needsUpgrade) {
              toUpgrade.add(pc.getPackage());
            }

            // add the actual package the user is wanting to install if it
            // is going to be an up/downgrade rather than a first install since
            // other installed packages may depend on the currently installed
            // version
            // and thus could be affected after the up/downgrade
            toUpgrade.add(packageToInstall);

            StringBuffer tempM = new StringBuffer();
            depsOk = true;
            for (int i = 0; i < installed.size(); i++) {
              Package tempP = installed.get(i);
              String tempPName = tempP.getName();
              boolean checkIt = true;
              for (int j = 0; j < needsUpgrade.size(); j++) {
                if (tempPName
                  .equals(needsUpgrade.get(j).getPackage().getName())) {
                  checkIt = false;
                  break;
                }
              }

              if (checkIt) {
                List<Dependency> problem = null;
                try {
                  problem = tempP.getIncompatibleDependencies(toUpgrade);
                } catch (Exception e) {
                  e.printStackTrace();
                  displayErrorDialog("An error has occurred while checking "
                    + "package dependencies", e);
                  // return null; // can't continue
                  depsOk = false;
                  break;
                }
                if (problem.size() > 0) {
                  conflictsAfterUpgrade = true;

                  tempM
                    .append("Package "
                      + tempP.getName()
                      + " will have a compatibility"
                      + "problem with the following packages after upgrading them:\n");
                  Iterator<Dependency> dI = problem.iterator();
                  while (dI.hasNext()) {
                    tempM.append("\t" + dI.next().getTarget().getPackage()
                      + "\n");
                  }
                }
              }
            }

            if (!depsOk) {
              m_unsuccessfulInstalls.add(packageToInstall);
              continue;
            }

            if (conflictsAfterUpgrade) {
              JOptionPane.showConfirmDialog(PackageManager.this,
                tempM.toString() + "\n"
                  + "Unable to continue with installation.",
                "Weka Package Manager", JOptionPane.OK_OPTION);

              // return null; //bail out here
              m_unsuccessfulInstalls.add(packageToInstall);
              continue;
            }
          }

          if (finalListToInstall.size() > 0) {
            StringBuffer message = new StringBuffer();
            message.append("To install " + packageName
              + " the following packages will" + " be installed/upgraded:\n\n");
            for (Package p : finalListToInstall) {
              message.append("\t" + p + "\n");
            }

            int result =
              JOptionPane.showConfirmDialog(PackageManager.this,
                message.toString(), "Weka Package Manager",
                JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.NO_OPTION) {
              // bail out here
              // return null;
              m_unsuccessfulInstalls.add(packageToInstall);
              continue;
            }
            m_progress.setMaximum(m_progress.getMaximum()
              + (finalListToInstall.size() * 30));
          }

          // OK, now we can download and install everything

          // first install the final list of dependencies
          try {
            boolean tempB =
              WekaPackageManager.installPackages(finalListToInstall, pps);
            s_atLeastOnePackageUpgradeHasOccurredInThisSession =
              (s_atLeastOnePackageUpgradeHasOccurredInThisSession || tempB);
          } catch (Exception e) {
            e.printStackTrace();
            displayErrorDialog("An error has occurred while installing "
              + "dependent packages", e);
            // return null;
            m_unsuccessfulInstalls.add(packageToInstall);
            continue;
          }

          // Now install the package itself
          // m_progress.setMaximum(finalListToInstall.size() * 10 + 10);
          try {
            boolean tempB =
              WekaPackageManager.installPackageFromRepository(packageName,
                versionToInstall.toString(), pps);
            s_atLeastOnePackageUpgradeHasOccurredInThisSession =
              (s_atLeastOnePackageUpgradeHasOccurredInThisSession || tempB);
          } catch (Exception e) {
            e.printStackTrace();
            displayErrorDialog("Problem installing package: " + packageName, e);
            // return null;
            m_unsuccessfulInstalls.add(packageToInstall);
            continue;
          }
        } else {
          // m_progress.setMaximum(10);
          // just install this package without checking/downloading dependencies
          // etc.
          try {
            boolean tempB =
              WekaPackageManager.installPackageFromRepository(packageName,
                versionToInstall.toString(), pps);
            s_atLeastOnePackageUpgradeHasOccurredInThisSession =
              (s_atLeastOnePackageUpgradeHasOccurredInThisSession || tempB);
          } catch (Exception e) {
            e.printStackTrace();
            displayErrorDialog("Problem installing package: " + packageName, e);
            // return null;
            m_unsuccessfulInstalls.add(packageToInstall);
            continue;
          }
        }
      }

      // m_successfulInstall = true;

      // Make sure that the new stuff is available to all GUIs (as long as no
      // upgrades occurred).
      // If an upgrade has occurred then the user is told to restart Weka
      // anyway, so we won't
      // refresh in this case in order to avoid old/new class conflicts
      if (!s_atLeastOnePackageUpgradeHasOccurredInThisSession) {
        WekaPackageManager.refreshGOEProperties();
      }
      return null;
    }

    @Override
    public void done() {
      m_progress.setValue(m_progress.getMinimum());
      if (m_unsuccessfulInstalls.size() == 0) {
        // if (m_successfulInstall) {
        m_detailLabel.setText("Package(s) installed successfully.");
      } else {
        StringBuffer failedPackageNames = new StringBuffer();
        for (Package p : m_unsuccessfulInstalls) {
          failedPackageNames.append(p.getName() + "\n");
        }
        displayErrorDialog(
          "The following package(s) could not be installed\n"
            + "for some reason (check the log)\n"
            + failedPackageNames.toString(), "");
        m_detailLabel.setText("Install complete.");
      }

      m_unofficialBut.setEnabled(true);
      m_refreshCacheBut.setEnabled(true && !WekaPackageManager.m_offline);
      m_availableBut.setEnabled(true);
      m_allBut.setEnabled(true);
      m_installedBut.setEnabled(true);

      // force refresh of installed and available packages
      m_installedPackages = null;
      m_availablePackages = null;

      // m_installBut.setEnabled(true);
      m_installing = false;
      updateTable();
      if (m_table.getSelectedRow() >= 0) {
        // mainly to update the install/uninstall button status
        // displayPackageInfo(m_table.getSelectedRow());
        updateInstallUninstallButtonEnablement();
      }
    }
  }

  /*
   * public class ComboBoxRenderer extends JComboBox implements
   * TableCellRenderer { public ComboBoxRenderer(String[] items) { super(items);
   * }
   * 
   * public Component getTableCellRendererComponent(JTable table, Object value,
   * boolean isSelected, boolean hasFocus, int row, int column) { if
   * (isSelected) { setForeground(table.getSelectionForeground());
   * super.setBackground(table.getSelectionBackground()); } else {
   * setForeground(table.getForeground()); setBackground(table.getBackground());
   * }
   * 
   * // Select the current value setSelectedItem(value); return this; } }
   */

  protected class ComboBoxEditor extends DefaultCellEditor {

    /** Added ID to avoid warning. */
    private static final long serialVersionUID = 5240331667759901966L;

    public ComboBoxEditor() {
      super(new JComboBox(new String[] { "one", "two" }));
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
      boolean isSelected, int row, int column) {
      String packageName =
        m_table.getValueAt(row, getColumnIndex(PACKAGE_COLUMN)).toString();
      List<Object> catAndVers = m_packageLookupInfo.get(packageName);
      @SuppressWarnings("unchecked")
      List<Object> repVersions = (List<Object>) catAndVers.get(1);

      String[] versions = repVersions.toArray(new String[1]);
      Component combo = getComponent();
      if (combo instanceof JComboBox) {
        ((JComboBox) combo).setModel(new DefaultComboBoxModel(versions));
        ((JComboBox) combo).setSelectedItem(value);
      } else {
        System.err.println("Uh oh!!!!!");
      }
      return combo;
    }
  }

  protected boolean m_cacheEstablished = false;
  protected boolean m_cacheRefreshInProgress = false;
  public static String PAGE_HEADER =
    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n"
      + "<html>\n<head>\n<title>Waikato Environment for Knowledge Analysis (WEKA)</title>\n"
      + "<!-- CSS Stylesheet -->\n<style>body\n{\nbackground: #ededed;\ncolor: #666666;\n"
      + "font: 14px Tahoma, Helvetica, sans-serif;;\nmargin: 5px 10px 5px 10px;\npadding: 0px;\n"
      + "}\n</style>\n\n</head>\n<body bgcolor=\"#ededed\" text=\"#666666\">\n";

  private static String initialPage() {
    StringBuffer initialPage = new StringBuffer();
    initialPage.append(PAGE_HEADER);
    initialPage.append("<h1>WEKA Package Manager</h1>\n\n</body></html>\n");
    return initialPage.toString();
  }

  protected class HomePageThread extends Thread {
    @Override
    public void run() {
      try {
        m_homeB.setEnabled(false);
        m_backB.setEnabled(false);
        URLConnection conn = null;
        URL homeURL = new URL(BROWSER_HOME);
        weka.core.packageManagement.PackageManager pm =
          WekaPackageManager.getUnderlyingPackageManager();
        if (pm.setProxyAuthentication(homeURL)) {
          conn = homeURL.openConnection(pm.getProxy());
        } else {
          conn = homeURL.openConnection();
        }

        // read the html for the home page - all we want to do here is make
        // sure that the web server is responding, so that we don't tie
        // up the JEditorPane indefinitely, since there seems to be no
        // way to set a timeout in JEditorPane
        conn.setConnectTimeout(10000); // 10 seconds
        BufferedReader bi =
          new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while (bi.readLine() != null) {
          //
        }

        m_infoPane.setPage(BROWSER_HOME);
      } catch (Exception ex) {
        // don't make a fuss
      } finally {
        m_homeB.setEnabled(true);
        m_backB.setEnabled(true);
      }
    }
  }

  private int getColumnIndex(String columnName) {
    return m_table.getColumn(columnName).getModelIndex();
  }

  public PackageManager() {

    if (WekaPackageManager.m_noPackageMetaDataAvailable) {
      JOptionPane
        .showMessageDialog(
          this,
          "The package manager is unavailable "
            + "due to the fact that there is no cached package meta data and we are offline",
          "Package manager unavailable", JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    EstablishCache ec = new EstablishCache();
    ec.execute();

    while (!m_cacheEstablished) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
    }

    // first try and get the full list of packages
    getAllPackages();

    setLayout(new BorderLayout());

    ButtonGroup bGroup = new ButtonGroup();
    bGroup.add(m_installedBut);
    bGroup.add(m_availableBut);
    bGroup.add(m_allBut);
    m_installedBut.setToolTipText("Installed packages");
    m_availableBut.setToolTipText("Available packages compatible with Weka "
      + Version.VERSION);
    m_allBut.setToolTipText("All packages");

    JPanel butPanel = new JPanel();
    butPanel.setLayout(new BorderLayout());

    JPanel packageDisplayP = new JPanel();
    packageDisplayP.setLayout(new BorderLayout());
    JPanel packageDHolder = new JPanel();
    packageDHolder.setLayout(new FlowLayout());
    packageDHolder.add(m_installedBut);
    packageDHolder.add(m_availableBut);
    packageDHolder.add(m_allBut);
    packageDisplayP.add(packageDHolder, BorderLayout.SOUTH);
    packageDisplayP.add(m_refreshCacheBut, BorderLayout.NORTH);
    JPanel officialHolder = new JPanel();
    officialHolder.setLayout(new BorderLayout());
    officialHolder.setBorder(BorderFactory.createTitledBorder("Official"));
    officialHolder.add(packageDisplayP, BorderLayout.WEST);

    butPanel.add(officialHolder, BorderLayout.WEST);

    m_refreshCacheBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        RefreshCache r = new RefreshCache();
        r.execute();
      }
    });

    JPanel unofficialHolder = new JPanel();
    unofficialHolder.setLayout(new BorderLayout());
    unofficialHolder.setBorder(BorderFactory.createTitledBorder("Unofficial"));
    unofficialHolder.add(m_unofficialBut, BorderLayout.NORTH);
    butPanel.add(unofficialHolder, BorderLayout.EAST);

    JPanel installP = new JPanel();
    JPanel buttP = new JPanel();
    buttP.setLayout(new GridLayout(1, 3));
    installP.setLayout(new BorderLayout());
    buttP.add(m_installBut);
    buttP.add(m_uninstallBut);
    buttP.add(m_toggleLoad);
    m_installBut.setEnabled(false);
    m_uninstallBut.setEnabled(false);
    m_toggleLoad.setEnabled(false);
    installP.add(buttP, BorderLayout.NORTH);
    installP.add(m_forceBut, BorderLayout.SOUTH);
    m_forceBut.setEnabled(false);
    // butPanel.add(installP, BorderLayout.EAST);
    officialHolder.add(installP, BorderLayout.EAST);

    m_installBut.setToolTipText("Install the selected official package(s) "
      + "from the list");
    m_uninstallBut
      .setToolTipText("Uninstall the selected package(s) from the list");
    m_toggleLoad.setToolTipText("Toggle installed package(s) load status ("
      + "note - changes take affect after a restart)");
    m_unofficialBut
      .setToolTipText("Install an unofficial package from a file or URL");
    m_unofficialChooser.resetFileFilters();
    m_unofficialChooser.addFileFilter(new ExtensionFileFilter(".zip",
      "Package archive file"));

    m_unofficialBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_unofficialFrame == null) {
          final JFrame jf = new JFrame("Unofficial package install");
          jf.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
              jf.dispose();
              m_unofficialBut.setEnabled(true);
              m_unofficialFrame = null;
            }
          });
          jf.setLayout(new BorderLayout());
          JButton okBut = new JButton("OK");
          JButton cancelBut = new JButton("Cancel");
          JPanel butHolder = new JPanel();
          butHolder.setLayout(new GridLayout(1, 2));
          butHolder.add(okBut);
          butHolder.add(cancelBut);
          jf.add(m_unofficialChooser, BorderLayout.CENTER);
          jf.add(butHolder, BorderLayout.SOUTH);
          jf.pack();
          jf.setVisible(true);
          m_unofficialFrame = jf;
          m_unofficialBut.setEnabled(false);
          cancelBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              if (m_unofficialFrame != null) {
                jf.dispose();
                m_unofficialBut.setEnabled(true);
                m_unofficialFrame = null;
              }
            }
          });

          okBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              String target = m_unofficialChooser.getText();
              UnofficialInstallTask t = new UnofficialInstallTask();
              t.setTargetToInstall(target);
              t.execute();
              if (m_unofficialFrame != null) {
                jf.dispose();
                m_unofficialBut.setEnabled(true);
                m_unofficialFrame = null;
              }
            }
          });
        }
      }
    });

    m_toggleLoad.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int[] selectedRows = m_table.getSelectedRows();
        List<Integer> alteredRows = new ArrayList<Integer>();

        if (selectedRows.length > 0) {
          List<String> packageNames = new ArrayList<String>();
          for (int selectedRow : selectedRows) {
            String packageName =
              m_table.getValueAt(selectedRow, getColumnIndex(PACKAGE_COLUMN))
                .toString();
            try {
              if (WekaPackageManager.getInstalledPackageInfo(packageName) != null) {
                // TODO
                List<Object> catAndVers = m_packageLookupInfo.get(packageName);
                if (!catAndVers.get(2).toString().equals("No - check log")) {
                  packageNames.add(packageName);
                  alteredRows.add(selectedRow);
                }
              }
            } catch (Exception ex) {
              ex.printStackTrace();
            }

          }

          if (packageNames.size() > 0) {
            try {
              WekaPackageManager.toggleLoadStatus(packageNames);
              for (String packageName : packageNames) {
                List<Object> catAndVers = m_packageLookupInfo.get(packageName);
                String loadStatus = catAndVers.get(2).toString();
                if (loadStatus.startsWith("Yes")) {
                  loadStatus = "No - user flagged (pending restart)";
                } else {
                  loadStatus = "Yes - user flagged (pending restart)";
                }

                catAndVers.set(2, loadStatus);
              }
              updateTable();
            } catch (Exception e1) {
              e1.printStackTrace();
            }
          }
          toggleLoadStatusRequiresRestartPopUp();
        }
      }
    });

    m_installBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int[] selectedRows = m_table.getSelectedRows();

        if (selectedRows.length > 0) {

          // int selected = m_table.getSelectedRow();
          // if (selected != -1) {
          List<String> packageNames = new ArrayList<String>();
          List<Object> versions = new ArrayList<Object>();
          StringBuffer confirmList = new StringBuffer();
          for (int selectedRow : selectedRows) {
            String packageName =
              m_table.getValueAt(selectedRow, getColumnIndex(PACKAGE_COLUMN))
                .toString();
            packageNames.add(packageName);
            Object packageVersion =
              m_table
                .getValueAt(selectedRow, getColumnIndex(REPOSITORY_COLUMN));
            versions.add(packageVersion);
            confirmList.append(packageName + " " + packageVersion.toString()
              + "\n");
          }

          JTextArea jt =
            new JTextArea("The following packages will be "
              + "installed/upgraded:\n\n" + confirmList.toString(), 10, 40);
          int result =
            JOptionPane.showConfirmDialog(PackageManager.this, new JScrollPane(
              jt), "Weka Package Manager", JOptionPane.YES_NO_OPTION);

          if (result == JOptionPane.YES_OPTION) {
            pleaseCloseAppWindowsPopUp();

            InstallTask task = new InstallTask();
            task.setPackages(packageNames);
            task.setVersions(versions);
            task.execute();
          }
        }
      }
    });

    m_uninstallBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // int selected = m_table.getSelectedRow();

        int[] selectedRows = m_table.getSelectedRows();

        if (selectedRows.length > 0) {
          List<String> packageNames = new ArrayList<String>();
          StringBuffer confirmList = new StringBuffer();

          for (int selectedRow : selectedRows) {
            String packageName =
              m_table.getValueAt(selectedRow, getColumnIndex(PACKAGE_COLUMN))
                .toString();
            Package p = null;
            try {
              p = WekaPackageManager.getRepositoryPackageInfo(packageName);
            } catch (Exception e1) {
              // e1.printStackTrace();
              // continue;
              // see if we can get installed package info
              try {
                p = WekaPackageManager.getInstalledPackageInfo(packageName);
              } catch (Exception e2) {
                e2.printStackTrace();
                continue;
              }
            }

            if (p.isInstalled()) {
              packageNames.add(packageName);
              confirmList.append(packageName + "\n");
            }
          }

          if (packageNames.size() > 0) {
            JTextArea jt =
              new JTextArea("The following packages will be "
                + "uninstalled:\n" + confirmList.toString(), 10, 40);
            int result =
              JOptionPane.showConfirmDialog(PackageManager.this,
                new JScrollPane(jt), "Weka Package Manager",
                JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
              pleaseCloseAppWindowsPopUp();
              UninstallTask task = new UninstallTask();
              task.setPackages(packageNames);
              task.execute();
            }
          }
        }

        /*
         * if (selected != -1) { String packageName =
         * m_table.getValueAt(selected,
         * getColumnIndex(PACKAGE_COLUMN)).toString();
         * 
         * pleaseCloseAppWindowsPopUp(); UninstallTask task = new
         * UninstallTask(); task.setPackage(packageName); task.execute(); }
         */
      }
    });

    JPanel progressP = new JPanel();
    progressP.setLayout(new BorderLayout());
    progressP.setBorder(BorderFactory
      .createTitledBorder("Install/Uninstall/Refresh progress"));
    progressP.add(m_progress, BorderLayout.NORTH);
    progressP.add(m_detailLabel, BorderLayout.CENTER);
    butPanel.add(progressP, BorderLayout.CENTER);

    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BorderLayout());
    // topPanel.setBorder(BorderFactory.createTitledBorder("Packages"));
    topPanel.add(butPanel, BorderLayout.NORTH);
    m_availableBut.setSelected(true);

    m_allBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_searchResults.clear();
        m_searchField.setText("");
        m_searchHitsLab.setText("");
        m_table.clearSelection();
        updateTable();
        updateInstallUninstallButtonEnablement();
      }
    });

    m_availableBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_searchResults.clear();
        m_searchField.setText("");
        m_searchHitsLab.setText("");
        m_table.clearSelection();
        updateTable();
        updateInstallUninstallButtonEnablement();
      }
    });

    m_installedBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_searchResults.clear();
        m_searchField.setText("");
        m_searchHitsLab.setText("");
        m_table.clearSelection();
        updateTable();
        updateInstallUninstallButtonEnablement();
      }
    });

    m_model =
      new DefaultTableModel(new String[] { PACKAGE_COLUMN, CATEGORY_COLUMN,
        INSTALLED_COLUMN, REPOSITORY_COLUMN, LOADED_COLUMN }, 15) {

        private static final long serialVersionUID = -2886328542412471039L;

        @Override
        public boolean isCellEditable(int row, int col) {
          if (col != 3) {
            return false;
          } else {
            return true;
          }
        }
      };

    m_table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    m_table.setColumnSelectionAllowed(false);
    m_table.setPreferredScrollableViewportSize(new Dimension(550, 200));
    m_table.setModel(m_model);
    if (System.getProperty("os.name").contains("Mac")) {
      m_table.setShowVerticalLines(true);
    } else {
      m_table.setShowVerticalLines(false);
    }
    m_table.setShowHorizontalLines(false);
    m_table.getColumn("Repository version").setCellEditor(new ComboBoxEditor());
    m_table.getSelectionModel().addListSelectionListener(
      new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (!e.getValueIsAdjusting() && !m_cacheRefreshInProgress) {
            ListSelectionModel lm = (ListSelectionModel) e.getSource();
            boolean infoDisplayed = false;
            for (int i = e.getFirstIndex(); i <= e.getLastIndex(); i++) {
              if (lm.isSelectedIndex(i)) {
                if (!infoDisplayed) {
                  // display package info for the first one in the list
                  displayPackageInfo(i);
                  infoDisplayed = true;
                  break;
                }
              }
            }
            updateInstallUninstallButtonEnablement();
          }
        }
      });

    JTableHeader header = m_table.getTableHeader();
    header.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent evt) {
        TableColumnModel colModel = m_table.getColumnModel();

        // The index of the column whose header was clicked
        int vColIndex = colModel.getColumnIndexAtX(evt.getX());

        // Return if not clicked on any column header or
        // clicked on the version number cols
        if (vColIndex == -1 || vColIndex > 1) {
          return;
        }

        if (vColIndex == m_sortColumn) {
          // toggle the sort order
          m_reverseSort = !m_reverseSort;
        } else {
          m_reverseSort = false;
        }
        m_sortColumn = vColIndex;
        updateTable();
      }
    });

    topPanel.add(new JScrollPane(m_table), BorderLayout.CENTER);

    // add(topPanel, BorderLayout.NORTH);

    /*
     * m_packageDescription = new JTextArea(10,10);
     * m_packageDescription.setLineWrap(true);
     */

    try {
      // m_infoPane = new JEditorPane(BROWSER_HOME);
      String initialPage = initialPage();
      m_infoPane = new JEditorPane("text/html", initialPage);
    } catch (Exception ex) {
      m_infoPane = new JEditorPane();
    }

    m_infoPane.setEditable(false);
    m_infoPane.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          try {
            if (event.getURL().toExternalForm().endsWith(".zip")
              || event.getURL().toExternalForm().endsWith(".jar")) {
              // don't render archives!
            } else {
              if (m_browserHistory.size() == 0) {
                m_backB.setEnabled(true);
              }
              m_browserHistory.add(m_infoPane.getPage());
              m_infoPane.setPage(event.getURL());
            }
          } catch (IOException ioe) {

          }
        }
      }
    });

    // JScrollPane sp = new JScrollPane(m_packageDescription);
    // JScrollPane sp = new JScrollPane(m_infoPane);
    // sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    // sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    JPanel browserP = new JPanel();
    browserP.setLayout(new BorderLayout());
    m_backB = new JButton(new ImageIcon(loadImage("weka/gui/images/back.gif")));
    m_backB.setToolTipText("Back");
    m_backB.setEnabled(false);
    m_backB.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
    m_homeB = new JButton(new ImageIcon(loadImage("weka/gui/images/home.gif")));
    m_homeB.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
    m_homeB.setToolTipText("Home");
    m_browserTools = new JToolBar();
    m_browserTools.add(m_backB);
    m_browserTools.add(m_homeB);

    m_searchField = new JTextField(15);
    JPanel searchHolder = new JPanel(new BorderLayout());
    JPanel temp = new JPanel(new BorderLayout());
    JLabel searchLab = new JLabel("Package search ");
    searchLab
      .setToolTipText("Type search terms (comma separated) and hit <Enter>");
    temp.add(searchLab, BorderLayout.WEST);
    temp.add(m_searchField, BorderLayout.CENTER);
    searchHolder.add(temp, BorderLayout.WEST);
    JButton clearSearchBut = new JButton("Clear");
    clearSearchBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_searchField.setText("");
        m_searchHitsLab.setText("");
        updateTable();
      }
    });
    JPanel clearAndHitsHolder = new JPanel(new BorderLayout());
    clearAndHitsHolder.add(clearSearchBut, BorderLayout.WEST);
    clearAndHitsHolder.add(m_searchHitsLab, BorderLayout.EAST);
    temp.add(clearAndHitsHolder, BorderLayout.EAST);
    m_browserTools.addSeparator();
    m_browserTools.add(searchHolder);
    Dimension d = m_searchField.getSize();
    m_searchField.setMaximumSize(new Dimension(150, 20));
    m_searchField.setEnabled(m_packageDescriptions.size() > 0);

    m_searchField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        List<Package> toSearch =
          m_allBut.isSelected() ? m_allPackages
            : m_availableBut.isSelected() ? m_availablePackages
              : m_installedPackages;

        m_searchResults.clear();
        String searchString = m_searchField.getText();
        if (searchString != null && searchString.length() > 0) {
          String[] terms = searchString.split(",");
          for (Package p : toSearch) {
            String name = p.getName();
            String description = m_packageDescriptions.get(name);
            if (description != null) {
              for (String t : terms) {
                if (description.contains(t.trim().toLowerCase())) {
                  m_searchResults.add(p);
                  break;
                }
              }
            }
          }

          m_searchHitsLab.setText(" (Search hits: " + m_searchResults.size()
            + ")");
        } else {
          m_searchHitsLab.setText("");
        }
        updateTable();
      }
    });

    m_browserTools.setFloatable(false);

    // create the new packages available icon
    m_newPackagesAvailableL =
      new JLabel(new ImageIcon(loadImage("weka/gui/images/information.gif")));

    // Start loading the home page
    Thread homePageThread = new HomePageThread();

    homePageThread.setPriority(Thread.MIN_PRIORITY);
    homePageThread.start();

    m_backB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        URL previous = m_browserHistory.removeLast();
        try {
          m_infoPane.setPage(previous);
          if (m_browserHistory.size() == 0) {
            m_backB.setEnabled(false);
          }
        } catch (IOException ex) {
          //
        }
      }
    });

    m_homeB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          URL back = m_infoPane.getPage();
          if (back != null) {
            m_browserHistory.add(back);
          }

          String initialPage = initialPage();
          m_infoPane.setContentType("text/html");
          m_infoPane.setText(initialPage);
          HomePageThread hp = new HomePageThread();
          hp.setPriority(Thread.MIN_PRIORITY);
          hp.start();
        } catch (Exception ex) {
          // don't make a fuss
        }
      }
    });

    browserP.add(m_browserTools, BorderLayout.NORTH);
    browserP.add(new JScrollPane(m_infoPane), BorderLayout.CENTER);
    // add(browserP, BorderLayout.CENTER);

    m_splitP = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, browserP);
    m_splitP.setOneTouchExpandable(true);

    add(m_splitP, BorderLayout.CENTER);

    updateTable();

    // check for any new packages on the server (if possible)
    if (!WekaPackageManager.m_offline) {
      System.err.println("Checking for new packages...");
      CheckForNewPackages cp = new CheckForNewPackages();
      cp.execute();
    } else {
      // disable cache refresh and install buttons
      m_installBut.setEnabled(false);
      m_refreshCacheBut.setEnabled(false);
    }
  }

  private void updateInstallUninstallButtonEnablement() {
    boolean enableInstall = false;
    boolean enableUninstall = false;
    boolean enableToggleLoadStatus = false;

    m_unofficialBut.setEnabled(true);

    if (!m_installing) {
      int[] selectedRows = m_table.getSelectedRows();
      // check the package to see whether we should enable the
      // install button or uninstall button. Once we've determined
      // that the list contains at least one package to be installed
      // and uninstalled we don't have to check any further

      for (int selectedRow : selectedRows) {
        if (!enableInstall || !enableUninstall) {
          enableInstall = true; // we should always be able to install an
                                // already installed package
          String packageName =
            m_table.getValueAt(selectedRow, getColumnIndex(PACKAGE_COLUMN))
              .toString();
          try {
            Package p =
              WekaPackageManager.getRepositoryPackageInfo(packageName);
            if (!enableUninstall) {
              enableUninstall = p.isInstalled();
            }

            if (!enableToggleLoadStatus) {
              enableToggleLoadStatus = p.isInstalled();
            }

            /*
             * if (!enableInstall) { enableInstall = !p.isInstalled(); }
             */
          } catch (Exception e1) {
            // not a repository package - just enable the uninstall button
            enableUninstall = true;
            enableInstall = false;
          }
        }
      }
    } else {
      m_unofficialBut.setEnabled(false);
    }

    // now set the button enablement
    m_installBut.setEnabled(enableInstall && !WekaPackageManager.m_offline);
    m_forceBut.setEnabled(enableInstall);
    m_uninstallBut.setEnabled(enableUninstall);
    m_toggleLoad.setEnabled(enableToggleLoadStatus);
  }

  private Image loadImage(String path) {
    Image pic = null;
    URL imageURL = this.getClass().getClassLoader().getResource(path);
    if (imageURL == null) {
      // ignore
    } else {
      pic = Toolkit.getDefaultToolkit().getImage(imageURL);
    }

    return pic;
  }

  private void updateTableForPackageList(List<Package> packageList) {
    m_table.clearSelection();
    m_model.setRowCount(packageList.size());
    int row = 0;
    for (Package p : packageList) {
      m_model.setValueAt(p.getName(), row, getColumnIndex(PACKAGE_COLUMN));
      String installedV = "";
      if (p.isInstalled()) {
        try {
          Package installed =
            WekaPackageManager.getInstalledPackageInfo(p.getName());
          installedV =
            installed.getPackageMetaDataElement("Version").toString();
        } catch (Exception ex) {
          ex.printStackTrace();
          displayErrorDialog("An error has occurred while trying to obtain"
            + " installed package info", ex);
        }
      }

      String category = "";
      if (p.getPackageMetaDataElement("Category") != null) {
        category = p.getPackageMetaDataElement("Category").toString();
      }

      List<Object> catAndVers = m_packageLookupInfo.get(p.getName());
      Object repositoryV = "-----";
      if (catAndVers != null) {
        // handle non-repository packages
        @SuppressWarnings("unchecked")
        List<Object> repVersions = (List<Object>) catAndVers.get(1);
        repositoryV = repVersions.get(0);
      }
      // String repString = getRepVersions(p.getName(), repositoryV);
      // repositoryV = repositoryV + " " + repString;

      m_model.setValueAt(category, row, getColumnIndex(CATEGORY_COLUMN));
      m_model.setValueAt(installedV, row, getColumnIndex(INSTALLED_COLUMN));
      m_model.setValueAt(repositoryV, row, getColumnIndex(REPOSITORY_COLUMN));
      if (catAndVers != null) {
        String loadStatus = (String) catAndVers.get(2);
        m_model.setValueAt(loadStatus, row, getColumnIndex(LOADED_COLUMN));
      } else {
        // handle non-repository packages
        File packageRoot =
          new File(WekaPackageManager.getPackageHome().toString()
            + File.separator + p.getName());
        boolean loaded = WekaPackageManager.loadCheck(p, packageRoot);
        String loadStatus = loaded ? "Yes" : "No - check log";
        m_model.setValueAt(loadStatus, row, getColumnIndex(LOADED_COLUMN));
      }
      row++;
    }
  }

  private void updateTable() {

    if (m_installedPackages == null || m_availablePackages == null) {
      // update the loaded status
      for (Package p : m_allPackages) {
        List<Object> catAndVers = m_packageLookupInfo.get(p.getName());
        String loadStatus = catAndVers.get(2).toString();
        if (p.isInstalled()) {
          File packageRoot =
            new File(WekaPackageManager.getPackageHome().toString()
              + File.separator + p.getName());
          boolean loaded = WekaPackageManager.loadCheck(p, packageRoot);
          boolean userNoLoad =
            WekaPackageManager.m_doNotLoadList.contains(p.getName());
          if (!loadStatus.contains("pending")) {
            loadStatus =
              (loaded) ? "Yes" : userNoLoad ? "No - user flagged"
                : "No - check log";
          }
        }

        catAndVers.set(2, loadStatus);
      }
    }

    if (m_searchField.getText() != null && m_searchField.getText().length() > 0) {
      updateTableForPackageList(m_searchResults);
      return;
    }

    if (m_allBut.isSelected()) {
      Collections.sort(m_allPackages, m_packageComparator);
      updateTableForPackageList(m_allPackages);
    } else if (m_installedBut.isSelected()) {
      try {
        if (m_installedPackages == null) {
          m_installedPackages = WekaPackageManager.getInstalledPackages();
        }

        updateTableForPackageList(m_installedPackages);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } else {
      try {
        if (m_availablePackages == null) {
          m_availablePackages =
            WekaPackageManager.getAvailableCompatiblePackages();
        }
        updateTableForPackageList(m_availablePackages);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  private void displayPackageInfo(int i) {
    String packageName =
      m_table.getValueAt(i, getColumnIndex(PACKAGE_COLUMN)).toString();

    boolean repositoryPackage = true;
    try {
      WekaPackageManager.getRepositoryPackageInfo(packageName);
    } catch (Exception ex) {
      repositoryPackage = false;
    }
    String versionURL =
      WekaPackageManager.getPackageRepositoryURL().toString() + "/"
        + packageName + "/index.html";

    try {
      URL back = m_infoPane.getPage();
      if (m_browserHistory.size() == 0 && back != null) {
        m_backB.setEnabled(true);
      }
      if (back != null) {
        m_browserHistory.add(back);
      }

      if (repositoryPackage) {
        m_infoPane.setPage(new URL(versionURL));
      } else {
        // try and display something on this non-official package
        try {
          Package p = WekaPackageManager.getInstalledPackageInfo(packageName);
          Map<?, ?> meta = p.getPackageMetaData();
          Set<?> keys = meta.keySet();
          StringBuffer sb = new StringBuffer();
          sb.append(weka.core.RepositoryIndexGenerator.HEADER);
          sb.append("<H1>" + packageName + " (Unofficial) </H1>");
          for (Object k : keys) {
            if (!k.toString().equals("PackageName")) {
              Object value = meta.get(k);
              sb.append(k + " : " + value + "<p>");
            }
          }
          sb.append("</html>\n");
          m_infoPane.setText(sb.toString());
        } catch (Exception e) {
          // ignore
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    updateInstallUninstallButtonEnablement();
    if (m_availableBut.isSelected()) {
      m_uninstallBut.setEnabled(false);
    }

    /*
     * if (m_installing) { m_installBut.setEnabled(false);
     * m_uninstallBut.setEnabled(false); } else { m_installBut.setEnabled(true);
     * if (m_availableBut.isSelected()) { m_uninstallBut.setEnabled(false); }
     * else { try { Package p =
     * WekaPackageManager.getRepositoryPackageInfo(packageName);
     * m_uninstallBut.setEnabled(p.isInstalled()); } catch (Exception ex) {
     * m_uninstallBut.setEnabled(false); } } }
     */
  }

  private void getPackagesAndEstablishLookup() throws Exception {
    m_allPackages = WekaPackageManager.getAllPackages();
    m_installedPackages = WekaPackageManager.getInstalledPackages();

    // now fill the lookup map
    m_packageLookupInfo = new TreeMap<String, List<Object>>();
    // Iterator<Package> i = allP.iterator();

    for (Package p : m_allPackages) {
      // Package p = i.next();
      String packageName = p.getName();
      String category = "";
      if (p.getPackageMetaDataElement("Category") != null) {
        category = p.getPackageMetaDataElement("Category").toString();
      }

      // check the load status of this package (if installed)
      String loadStatus = "";
      if (p.isInstalled()) {
        File packageRoot =
          new File(WekaPackageManager.getPackageHome().toString());
        boolean loaded = WekaPackageManager.loadCheck(p, packageRoot);
        loadStatus = (loaded) ? "Yes" : "No - check log";
      }

      List<Object> versions =
        WekaPackageManager.getRepositoryPackageVersions(packageName);
      List<Object> catAndVers = new ArrayList<Object>();
      catAndVers.add(category);
      catAndVers.add(versions);
      catAndVers.add(loadStatus);
      m_packageLookupInfo.put(packageName, catAndVers);
    }

    // Load all repCache package descriptions into the search lookup
    for (Package p : m_allPackages) {
      String name = p.getName();
      File repLatest =
        new File(WekaPackageManager.WEKA_HOME.toString() + File.separator
          + "repCache" + File.separator + name + File.separator
          + "Latest.props");
      if (repLatest.exists() && repLatest.isFile()) {
        String packageDescription = loadPropsText(repLatest);

        m_packageDescriptions.put(name, packageDescription);
      }
    }

    // Now process all installed packages and add to the search
    // just in case there are some unofficial packages
    for (Package p : m_installedPackages) {
      if (!m_packageDescriptions.containsKey(p.getName())) {
        String name = p.getName();
        File instDesc =
          new File(WekaPackageManager.PACKAGES_DIR.toString() + File.separator
            + name + File.separator + "Description.props");
        if (instDesc.exists() && instDesc.isFile()) {
          m_packageDescriptions.put(name, loadPropsText(instDesc));
        }
      }
    }
  }

  private String loadPropsText(File propsToLoad) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(propsToLoad));
    StringBuilder builder = new StringBuilder();
    String line = null;
    try {
      while ((line = br.readLine()) != null) {
        if (!line.startsWith("#")) {
          builder.append(line.toLowerCase()).append("\n");
        }
      }
    } finally {
      br.close();
    }

    return builder.toString();
  }

  private void getAllPackages() {
    try {
      getPackagesAndEstablishLookup();
    } catch (Exception ex) {
      // warn the user that we were unable to get the list of packages
      // from the repository
      ex.printStackTrace();
      System.err.println("A problem has occurred whilst trying to get all "
        + "package information. Trying a cache refresh...");
      WekaPackageManager.refreshCache(System.out);
      try {
        // try again
        getPackagesAndEstablishLookup();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void displayErrorDialog(String message, Exception e) {
    java.io.StringWriter sw = new java.io.StringWriter();
    e.printStackTrace(new java.io.PrintWriter(sw));

    String result = sw.toString();
    displayErrorDialog(message, result);
  }

  private void displayErrorDialog(String message, String stackTrace) {
    Object[] options = null;

    if (stackTrace != null && stackTrace.length() > 0) {
      options = new Object[2];
      options[0] = "OK";
      options[1] = "Show error";
    } else {
      options = new Object[1];
      options[0] = "OK";
    }
    int result =
      JOptionPane.showOptionDialog(this, message, "Weka Package Manager",
        JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, options,
        options[0]);

    if (result == 1) {
      JTextArea jt = new JTextArea(stackTrace, 10, 40);
      JOptionPane.showMessageDialog(PackageManager.this, new JScrollPane(jt),
        "Weka Package Manager", JOptionPane.OK_OPTION);
    }
  }

  /**
   * Setting the initial placement of the divider line on a JSplitPane is
   * problematic. Most of the time it positions itself just fine based on the
   * preferred and minimum sizes of the two things it divides. However,
   * sometimes it seems to set itself such that the top component is not visible
   * without manually setting the position. This method can be called (after the
   * containing frame is visible) to set the divider location to 40% of the way
   * down the window.
   */
  public void setInitialSplitPaneDividerLocation() {
    m_splitP.setDividerLocation(0.4);
  }

  public static void main(String[] args) {
    weka.core.logging.Logger.log(weka.core.logging.Logger.Level.INFO,
      "Logging started");
    LookAndFeel.setLookAndFeel();

    PackageManager pm = new PackageManager();

    if (!WekaPackageManager.m_noPackageMetaDataAvailable) {
      String offline = "";
      if (WekaPackageManager.m_offline) {
        offline = " (offline)";
      }
      final javax.swing.JFrame jf =
        new javax.swing.JFrame("Weka Package Manager" + offline);
      jf.getContentPane().setLayout(new BorderLayout());
      jf.getContentPane().add(pm, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
          jf.dispose();
          System.exit(0);
        }
      });
      Dimension screenSize = jf.getToolkit().getScreenSize();
      int width = screenSize.width * 8 / 10;
      int height = screenSize.height * 8 / 10;
      jf.setBounds(width / 8, height / 8, width, height);
      jf.setVisible(true);
      pm.setInitialSplitPaneDividerLocation();
    }
  }

}
