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
 *    ClustererPanel.java
 *    Copyright (C) 1999-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.explorer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.Defaults;
import weka.core.Drawable;
import weka.core.Environment;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.SerializedObject;
import weka.core.Settings;
import weka.core.Utils;
import weka.core.Version;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.gui.AbstractPerspective;
import weka.gui.ExtensionFileFilter;
import weka.gui.GenericObjectEditor;
import weka.gui.InstancesSummaryPanel;
import weka.gui.ListSelectorDialog;
import weka.gui.Logger;
import weka.gui.PerspectiveInfo;
import weka.gui.PropertyPanel;
import weka.gui.ResultHistoryPanel;
import weka.gui.SaveBuffer;
import weka.gui.SetInstancesPanel;
import weka.gui.SysErrLog;
import weka.gui.TaskLogger;
import weka.gui.explorer.Explorer.CapabilitiesFilterChangeEvent;
import weka.gui.explorer.Explorer.CapabilitiesFilterChangeListener;
import weka.gui.explorer.Explorer.ExplorerPanel;
import weka.gui.explorer.Explorer.LogHandler;
import weka.gui.hierarchyvisualizer.HierarchyVisualizer;
import weka.gui.treevisualizer.PlaceNode2;
import weka.gui.treevisualizer.TreeVisualizer;
import weka.gui.visualize.VisualizePanel;
import weka.gui.visualize.plugins.TreeVisualizePlugin;

/**
 * This panel allows the user to select and configure a clusterer, and evaluate
 * the clusterer using a number of testing modes (test on the training data,
 * train/test on a percentage split, test on a separate split). The results of
 * clustering runs are stored in a result history so that previous results are
 * accessible.
 * 
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 12722 $
 */
@PerspectiveInfo(ID = "weka.gui.explorer.clustererpanel", title = "Cluster",
  toolTipText = "Cluster instances",
  iconPath = "weka/gui/weka_icon_new_small.png")
public class ClustererPanel extends AbstractPerspective implements
  CapabilitiesFilterChangeListener, ExplorerPanel, LogHandler {

  /** for serialization */
  static final long serialVersionUID = -2474932792950820990L;

  /** the parent frame */
  protected Explorer m_Explorer = null;

  /** The filename extension that should be used for model files */
  public static String MODEL_FILE_EXTENSION = ".model";

  /** Lets the user configure the clusterer */
  protected GenericObjectEditor m_ClustererEditor = new GenericObjectEditor();

  /** The panel showing the current clusterer selection */
  protected PropertyPanel m_CLPanel = new PropertyPanel(m_ClustererEditor);

  /** The output area for classification results */
  protected JTextArea m_OutText = new JTextArea(20, 40);

  /** The destination for log/status messages */
  protected Logger m_Log = new SysErrLog();

  /** The buffer saving object for saving output */
  SaveBuffer m_SaveOut = new SaveBuffer(m_Log, this);

  /** A panel controlling results viewing */
  protected ResultHistoryPanel m_History = new ResultHistoryPanel(m_OutText);

  /** Click to set test mode to generate a % split */
  protected JRadioButton m_PercentBut = new JRadioButton("Percentage split");

  /** Click to set test mode to test on training data */
  protected JRadioButton m_TrainBut = new JRadioButton("Use training set");

  /** Click to set test mode to a user-specified test set */
  protected JRadioButton m_TestSplitBut = new JRadioButton("Supplied test set");

  /** Click to set test mode to classes to clusters based evaluation */
  protected JRadioButton m_ClassesToClustersBut = new JRadioButton(
    "Classes to clusters evaluation");

  /**
   * Lets the user select the class column for classes to clusters based
   * evaluation
   */
  protected JComboBox m_ClassCombo = new JComboBox();

  /** Label by where the % split is entered */
  protected JLabel m_PercentLab = new JLabel("%", SwingConstants.RIGHT);

  /** The field where the % split is entered */
  protected JTextField m_PercentText = new JTextField("66");

  /** The button used to open a separate test dataset */
  protected JButton m_SetTestBut = new JButton("Set...");

  /** The frame used to show the test set selection panel */
  protected JFrame m_SetTestFrame;

  /**
   * The button used to popup a list for choosing attributes to ignore while
   * clustering
   */
  protected JButton m_ignoreBut = new JButton("Ignore attributes");

  protected DefaultListModel m_ignoreKeyModel = new DefaultListModel();
  protected JList m_ignoreKeyList = new JList(m_ignoreKeyModel);

  // protected Remove m_ignoreFilter = null;

  /**
   * Alters the enabled/disabled status of elements associated with each radio
   * button
   */
  ActionListener m_RadioListener = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      updateRadioLinks();
    }
  };

  /** Click to start running the clusterer */
  protected JButton m_StartBut = new JButton("Start");

  /** Stop the class combo from taking up to much space */
  private final Dimension COMBO_SIZE = new Dimension(250,
    m_StartBut.getPreferredSize().height);

  /** Click to stop a running clusterer */
  protected JButton m_StopBut = new JButton("Stop");

  /** The main set of instances we're playing with */
  protected Instances m_Instances;

  /** The user-supplied test set (if any) */
  protected Instances m_TestInstances;

  /** The current visualization object */
  protected VisualizePanel m_CurrentVis = null;

  /**
   * Check to save the predictions in the results list for visualizing later on
   */
  protected JCheckBox m_StorePredictionsBut = new JCheckBox(
    "Store clusters for visualization");

  /** A thread that clustering runs in */
  protected Thread m_RunThread;

  /** The instances summary panel displayed by m_SetTestFrame */
  protected InstancesSummaryPanel m_Summary;

  /** Filter to ensure only model files are selected */
  protected FileFilter m_ModelFilter = new ExtensionFileFilter(
    MODEL_FILE_EXTENSION, "Model object files");

  /** The file chooser for selecting model files */
  protected JFileChooser m_FileChooser = new JFileChooser(new File(
    System.getProperty("user.dir")));

  /** Whether startup settings have been applied yet or not */
  protected boolean m_initialSettingsSet;

  /* Register the property editors we need */
  static {
    GenericObjectEditor.registerEditors();
  }

  /**
   * Creates the clusterer panel
   */
  public ClustererPanel() {

    // Connect / configure the components
    m_OutText.setEditable(false);
    m_OutText.setFont(new Font("Monospaced", Font.PLAIN, 12));
    m_OutText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    m_OutText.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != InputEvent.BUTTON1_MASK) {
          m_OutText.selectAll();
        }
      }
    });
    JPanel historyHolder = new JPanel(new BorderLayout());
    historyHolder.setBorder(BorderFactory
      .createTitledBorder("Result list (right-click for options)"));
    historyHolder.add(m_History, BorderLayout.CENTER);
    m_ClustererEditor.setClassType(Clusterer.class);
    m_ClustererEditor.setValue(ExplorerDefaults.getClusterer());
    m_ClustererEditor.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        m_StartBut.setEnabled(true);
        Capabilities currentFilter = m_ClustererEditor.getCapabilitiesFilter();
        Clusterer clusterer = (Clusterer) m_ClustererEditor.getValue();
        Capabilities currentSchemeCapabilities = null;
        if (clusterer != null && currentFilter != null
          && (clusterer instanceof CapabilitiesHandler)) {
          currentSchemeCapabilities =
            ((CapabilitiesHandler) clusterer).getCapabilities();

          if (!currentSchemeCapabilities.supportsMaybe(currentFilter)
            && !currentSchemeCapabilities.supports(currentFilter)) {
            m_StartBut.setEnabled(false);
          }
        }
        repaint();
      }
    });

    m_TrainBut.setToolTipText("Cluster the same set that the clusterer"
      + " is trained on");
    m_PercentBut.setToolTipText("Train on a percentage of the data and"
      + " cluster the remainder");
    m_TestSplitBut.setToolTipText("Cluster a user-specified dataset");
    m_ClassesToClustersBut.setToolTipText("Evaluate clusters with respect to a"
      + " class");
    m_ClassCombo.setToolTipText("Select the class attribute for class based"
      + " evaluation");
    m_StartBut.setToolTipText("Starts the clustering");
    m_StopBut.setToolTipText("Stops a running clusterer");
    m_StorePredictionsBut
      .setToolTipText("Store predictions in the result list for later "
        + "visualization");
    m_ignoreBut.setToolTipText("Ignore attributes during clustering");

    m_FileChooser.setFileFilter(m_ModelFilter);
    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    m_ClassCombo.setPreferredSize(COMBO_SIZE);
    m_ClassCombo.setMaximumSize(COMBO_SIZE);
    m_ClassCombo.setMinimumSize(COMBO_SIZE);
    m_ClassCombo.setEnabled(false);

    m_PercentBut.setSelected(ExplorerDefaults.getClustererTestMode() == 2);
    m_TrainBut.setSelected(ExplorerDefaults.getClustererTestMode() == 3);
    m_TestSplitBut.setSelected(ExplorerDefaults.getClustererTestMode() == 4);
    m_ClassesToClustersBut
      .setSelected(ExplorerDefaults.getClustererTestMode() == 5);
    m_StorePredictionsBut.setSelected(ExplorerDefaults
      .getClustererStoreClustersForVis());
    updateRadioLinks();
    ButtonGroup bg = new ButtonGroup();
    bg.add(m_TrainBut);
    bg.add(m_PercentBut);
    bg.add(m_TestSplitBut);
    bg.add(m_ClassesToClustersBut);
    m_TrainBut.addActionListener(m_RadioListener);
    m_PercentBut.addActionListener(m_RadioListener);
    m_TestSplitBut.addActionListener(m_RadioListener);
    m_ClassesToClustersBut.addActionListener(m_RadioListener);
    m_SetTestBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setTestSet();
      }
    });

    m_StartBut.setEnabled(false);
    m_StopBut.setEnabled(false);
    m_ignoreBut.setEnabled(false);
    m_StartBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean proceed = true;
        if (Explorer.m_Memory.memoryIsLow()) {
          proceed = Explorer.m_Memory.showMemoryIsLow();
        }

        if (proceed) {
          startClusterer();
        }
      }
    });
    m_StopBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        stopClusterer();
      }
    });

    m_ignoreBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setIgnoreColumns();
      }
    });

    m_History.setHandleRightClicks(false);
    // see if we can popup a menu for the selected result
    m_History.getList().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (((e.getModifiers() & InputEvent.BUTTON1_MASK) != InputEvent.BUTTON1_MASK)
          || e.isAltDown()) {
          int index = m_History.getList().locationToIndex(e.getPoint());
          if (index != -1) {
            String name = m_History.getNameAtIndex(index);
            visualizeClusterer(name, e.getX(), e.getY());
          } else {
            visualizeClusterer(null, e.getX(), e.getY());
          }
        }
      }
    });

    m_ClassCombo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateCapabilitiesFilter(m_ClustererEditor.getCapabilitiesFilter());
      }
    });

    // Layout the GUI
    JPanel p1 = new JPanel();
    p1.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Clusterer"),
      BorderFactory.createEmptyBorder(0, 5, 5, 5)));
    p1.setLayout(new BorderLayout());
    p1.add(m_CLPanel, BorderLayout.NORTH);

    JPanel p2 = new JPanel();
    GridBagLayout gbL = new GridBagLayout();
    p2.setLayout(gbL);
    p2.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Cluster mode"),
      BorderFactory.createEmptyBorder(0, 5, 5, 5)));
    GridBagConstraints gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 0;
    gbC.gridx = 0;
    gbL.setConstraints(m_TrainBut, gbC);
    p2.add(m_TrainBut);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 1;
    gbC.gridx = 0;
    gbL.setConstraints(m_TestSplitBut, gbC);
    p2.add(m_TestSplitBut);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 1;
    gbC.gridx = 1;
    gbC.gridwidth = 2;
    gbC.insets = new Insets(2, 10, 2, 0);
    gbL.setConstraints(m_SetTestBut, gbC);
    p2.add(m_SetTestBut);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 2;
    gbC.gridx = 0;
    gbL.setConstraints(m_PercentBut, gbC);
    p2.add(m_PercentBut);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 2;
    gbC.gridx = 1;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(m_PercentLab, gbC);
    p2.add(m_PercentLab);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 2;
    gbC.gridx = 2;
    gbC.weightx = 100;
    gbC.ipadx = 20;
    gbL.setConstraints(m_PercentText, gbC);
    p2.add(m_PercentText);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 3;
    gbC.gridx = 0;
    gbC.gridwidth = 2;
    gbL.setConstraints(m_ClassesToClustersBut, gbC);
    p2.add(m_ClassesToClustersBut);

    m_ClassCombo.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 4;
    gbC.gridx = 0;
    gbC.gridwidth = 2;
    gbL.setConstraints(m_ClassCombo, gbC);
    p2.add(m_ClassCombo);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 5;
    gbC.gridx = 0;
    gbC.gridwidth = 2;
    gbL.setConstraints(m_StorePredictionsBut, gbC);
    p2.add(m_StorePredictionsBut);

    // Any launcher plugins
    Vector<String> pluginsVector =
      GenericObjectEditor.getClassnames(ClustererPanelLaunchHandlerPlugin.class
        .getName());
    JButton pluginBut = null;
    if (pluginsVector.size() == 1) {
      try {
        // display a single button
        String className = pluginsVector.elementAt(0);
        final ClustererPanelLaunchHandlerPlugin plugin =
          (ClustererPanelLaunchHandlerPlugin) Class.forName(className)
            .newInstance();
        if (plugin != null) {
          plugin.setClustererPanel(this);
          pluginBut = new JButton(plugin.getLaunchCommand());
          pluginBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              plugin.launch();
            }
          });
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } else if (pluginsVector.size() > 1) {
      // make a popup menu
      int okPluginCount = 0;
      final java.awt.PopupMenu pluginPopup = new java.awt.PopupMenu();

      for (int i = 0; i < pluginsVector.size(); i++) {
        String className = (pluginsVector.elementAt(i));
        try {
          final ClustererPanelLaunchHandlerPlugin plugin =
            (ClustererPanelLaunchHandlerPlugin) Class.forName(className)
              .newInstance();

          if (plugin == null) {
            continue;
          }
          okPluginCount++;
          plugin.setClustererPanel(this);
          java.awt.MenuItem popI =
            new java.awt.MenuItem(plugin.getLaunchCommand());
          popI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              // pluginPopup.setVisible(false);
              plugin.launch();
            }
          });
          pluginPopup.add(popI);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }

      if (okPluginCount > 0) {
        pluginBut = new JButton("Launchers...");
        final JButton copyB = pluginBut;
        copyB.add(pluginPopup);
        pluginBut.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            pluginPopup.show(copyB, 0, 0);
          }
        });
      } else {
        pluginBut = null;
      }
    }

    JPanel buttons = new JPanel();
    buttons.setLayout(new GridLayout(2, 1));
    JPanel ssButs = new JPanel();
    ssButs.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    if (pluginBut == null) {
      ssButs.setLayout(new GridLayout(1, 2, 5, 5));
    } else {
      ssButs.setLayout(new FlowLayout(FlowLayout.LEFT));
    }
    ssButs.add(m_StartBut);
    ssButs.add(m_StopBut);
    if (pluginBut != null) {
      ssButs.add(pluginBut);
    }

    JPanel ib = new JPanel();
    ib.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    ib.setLayout(new GridLayout(1, 1, 5, 5));
    ib.add(m_ignoreBut);
    buttons.add(ib);
    buttons.add(ssButs);

    JPanel p3 = new JPanel();
    p3.setBorder(BorderFactory.createTitledBorder("Clusterer output"));
    p3.setLayout(new BorderLayout());
    final JScrollPane js = new JScrollPane(m_OutText);
    p3.add(js, BorderLayout.CENTER);
    js.getViewport().addChangeListener(new ChangeListener() {
      private int lastHeight;

      @Override
      public void stateChanged(ChangeEvent e) {
        JViewport vp = (JViewport) e.getSource();
        int h = vp.getViewSize().height;
        if (h != lastHeight) { // i.e. an addition not just a user scrolling
          lastHeight = h;
          int x = h - vp.getExtentSize().height;
          vp.setViewPosition(new Point(0, x));
        }
      }
    });

    JPanel mondo = new JPanel();
    gbL = new GridBagLayout();
    mondo.setLayout(gbL);
    gbC = new GridBagConstraints();
    // gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 0;
    gbC.gridx = 0;
    gbL.setConstraints(p2, gbC);
    mondo.add(p2);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.NORTH;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 1;
    gbC.gridx = 0;
    gbL.setConstraints(buttons, gbC);
    mondo.add(buttons);
    gbC = new GridBagConstraints();
    // gbC.anchor = GridBagConstraints.NORTH;
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 2;
    gbC.gridx = 0;
    gbC.weightx = 0;
    gbL.setConstraints(historyHolder, gbC);
    mondo.add(historyHolder);
    gbC = new GridBagConstraints();
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 0;
    gbC.gridx = 1;
    gbC.gridheight = 3;
    gbC.weightx = 100;
    gbC.weighty = 100;
    gbL.setConstraints(p3, gbC);
    mondo.add(p3);

    setLayout(new BorderLayout());
    add(p1, BorderLayout.NORTH);
    add(mondo, BorderLayout.CENTER);
  }

  /**
   * Updates the enabled status of the input fields and labels.
   */
  protected void updateRadioLinks() {

    m_SetTestBut.setEnabled(m_TestSplitBut.isSelected());
    if ((m_SetTestFrame != null) && (!m_TestSplitBut.isSelected())) {
      m_SetTestFrame.setVisible(false);
    }
    m_PercentText.setEnabled(m_PercentBut.isSelected());
    m_PercentLab.setEnabled(m_PercentBut.isSelected());
    m_ClassCombo.setEnabled(m_ClassesToClustersBut.isSelected());

    updateCapabilitiesFilter(m_ClustererEditor.getCapabilitiesFilter());
  }

  /**
   * Sets the Logger to receive informational messages
   * 
   * @param newLog the Logger that will now get info messages
   */
  @Override
  public void setLog(Logger newLog) {

    m_Log = newLog;
  }

  /**
   * Tells the panel to use a new set of instances.
   * 
   * @param inst a set of Instances
   */
  @Override
  public void setInstances(Instances inst) {

    m_Instances = inst;

    m_ignoreKeyModel.removeAllElements();

    String[] attribNames = new String[m_Instances.numAttributes()];
    for (int i = 0; i < m_Instances.numAttributes(); i++) {
      String name = m_Instances.attribute(i).name();
      m_ignoreKeyModel.addElement(name);
      String type =
        "(" + Attribute.typeToStringShort(m_Instances.attribute(i)) + ") ";
      String attnm = m_Instances.attribute(i).name();
      attribNames[i] = type + attnm;
    }

    m_StartBut.setEnabled(m_RunThread == null);
    m_StopBut.setEnabled(m_RunThread != null);
    m_ignoreBut.setEnabled(true);
    m_ClassCombo.setModel(new DefaultComboBoxModel(attribNames));
    if (inst.classIndex() == -1) {
      m_ClassCombo.setSelectedIndex(attribNames.length - 1);
    } else {
      m_ClassCombo.setSelectedIndex(inst.classIndex());
    }
    updateRadioLinks();
  }

  /**
   * Sets the user test set. Information about the current test set is displayed
   * in an InstanceSummaryPanel and the user is given the ability to load
   * another set from a file or url.
   * 
   */
  protected void setTestSet() {

    if (m_SetTestFrame == null) {
      final SetInstancesPanel sp = new SetInstancesPanel();
      sp.setReadIncrementally(false);
      m_Summary = sp.getSummary();
      if (m_TestInstances != null) {
        sp.setInstances(m_TestInstances);
      }
      sp.addPropertyChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent e) {
          m_TestInstances = sp.getInstances();
          m_TestInstances.setClassIndex(-1); // make sure that no class
                                             // attribute is set!
        }
      });
      // Add propertychangelistener to update m_TestInstances whenever
      // it changes in the settestframe
      m_SetTestFrame = new JFrame("Test Instances");
      sp.setParentFrame(m_SetTestFrame); // enable Close-Button
      m_SetTestFrame.getContentPane().setLayout(new BorderLayout());
      m_SetTestFrame.getContentPane().add(sp, BorderLayout.CENTER);
      m_SetTestFrame.pack();
    }
    m_SetTestFrame.setVisible(true);
  }

  /**
   * Starts running the currently configured clusterer with the current
   * settings. This is run in a separate thread, and will only start if there is
   * no clusterer already running. The clusterer output is sent to the results
   * history panel.
   */
  protected void startClusterer() {

    if (m_RunThread == null) {
      m_StartBut.setEnabled(false);
      m_StopBut.setEnabled(true);
      m_ignoreBut.setEnabled(false);
      m_RunThread = new Thread() {
        @Override
        public void run() {
          m_CLPanel.addToHistory();

          // for timing
          long trainTimeStart = 0, trainTimeElapsed = 0;

          // Copy the current state of things
          m_Log.statusMessage("Setting up...");
          Instances inst = new Instances(m_Instances);
          inst.setClassIndex(-1);
          Instances userTest = null;
          ClustererAssignmentsPlotInstances plotInstances =
            ExplorerDefaults.getClustererAssignmentsPlotInstances();
          plotInstances.setClusterer((Clusterer) m_ClustererEditor.getValue());
          if (m_TestInstances != null) {
            userTest = new Instances(m_TestInstances);
          }

          boolean saveVis = m_StorePredictionsBut.isSelected();
          String grph = null;
          int[] ignoredAtts = null;

          int testMode = 0;
          int percent = 66;
          Clusterer clusterer = (Clusterer) m_ClustererEditor.getValue();
          Clusterer fullClusterer = null;
          StringBuffer outBuff = new StringBuffer();
          String name =
            (new SimpleDateFormat("HH:mm:ss - ")).format(new Date());
          String cname = clusterer.getClass().getName();
          if (cname.startsWith("weka.clusterers.")) {
            name += cname.substring("weka.clusterers.".length());
          } else {
            name += cname;
          }
          String cmd = m_ClustererEditor.getValue().getClass().getName();
          if (m_ClustererEditor.getValue() instanceof OptionHandler) {
            cmd +=
              " "
                + Utils.joinOptions(((OptionHandler) m_ClustererEditor
                  .getValue()).getOptions());
          }
          try {
            m_Log.logMessage("Started " + cname);
            m_Log.logMessage("Command: " + cmd);
            if (m_Log instanceof TaskLogger) {
              ((TaskLogger) m_Log).taskStarted();
            }
            if (m_PercentBut.isSelected()) {
              testMode = 2;
              percent = Integer.parseInt(m_PercentText.getText());
              if ((percent <= 0) || (percent >= 100)) {
                throw new Exception("Percentage must be between 0 and 100");
              }
            } else if (m_TrainBut.isSelected()) {
              testMode = 3;
            } else if (m_TestSplitBut.isSelected()) {
              testMode = 4;
              // Check the test instance compatibility
              if (userTest == null) {
                throw new Exception("No user test set has been opened");
              }
              if (!inst.equalHeaders(userTest)) {
                throw new Exception("Train and test set are not compatible\n"
                  + inst.equalHeadersMsg(userTest));
              }
            } else if (m_ClassesToClustersBut.isSelected()) {
              testMode = 5;
            } else {
              throw new Exception("Unknown test mode");
            }

            Instances trainInst = new Instances(inst);
            if (m_ClassesToClustersBut.isSelected()) {
              trainInst.setClassIndex(m_ClassCombo.getSelectedIndex());
              inst.setClassIndex(m_ClassCombo.getSelectedIndex());
              if (inst.classAttribute().isNumeric()) {
                throw new Exception("Class must be nominal for class based "
                  + "evaluation!");
              }
            }
            if (!m_ignoreKeyList.isSelectionEmpty()) {
              trainInst = removeIgnoreCols(trainInst);
            }

            // Output some header information
            outBuff.append("=== Run information ===\n\n");
            outBuff.append("Scheme:       " + cname);
            if (clusterer instanceof OptionHandler) {
              String[] o = ((OptionHandler) clusterer).getOptions();
              outBuff.append(" " + Utils.joinOptions(o));
            }
            outBuff.append("\n");
            outBuff.append("Relation:     " + inst.relationName() + '\n');
            outBuff.append("Instances:    " + inst.numInstances() + '\n');
            outBuff.append("Attributes:   " + inst.numAttributes() + '\n');
            if (inst.numAttributes() < 100) {
              boolean[] selected = new boolean[inst.numAttributes()];
              for (int i = 0; i < inst.numAttributes(); i++) {
                selected[i] = true;
              }
              if (!m_ignoreKeyList.isSelectionEmpty()) {
                int[] indices = m_ignoreKeyList.getSelectedIndices();
                for (int i = 0; i < indices.length; i++) {
                  selected[indices[i]] = false;
                }
              }
              if (m_ClassesToClustersBut.isSelected()) {
                selected[m_ClassCombo.getSelectedIndex()] = false;
              }
              for (int i = 0; i < inst.numAttributes(); i++) {
                if (selected[i]) {
                  outBuff.append("              " + inst.attribute(i).name()
                    + '\n');
                }
              }
              if (!m_ignoreKeyList.isSelectionEmpty()
                || m_ClassesToClustersBut.isSelected()) {
                outBuff.append("Ignored:\n");
                for (int i = 0; i < inst.numAttributes(); i++) {
                  if (!selected[i]) {
                    outBuff.append("              " + inst.attribute(i).name()
                      + '\n');
                  }
                }
              }
            } else {
              outBuff.append("              [list of attributes omitted]\n");
            }

            if (!m_ignoreKeyList.isSelectionEmpty()) {
              ignoredAtts = m_ignoreKeyList.getSelectedIndices();
            }

            if (m_ClassesToClustersBut.isSelected()) {
              // add class to ignored list
              if (ignoredAtts == null) {
                ignoredAtts = new int[1];
                ignoredAtts[0] = m_ClassCombo.getSelectedIndex();
              } else {
                int[] newIgnoredAtts = new int[ignoredAtts.length + 1];
                System.arraycopy(ignoredAtts, 0, newIgnoredAtts, 0,
                  ignoredAtts.length);
                newIgnoredAtts[ignoredAtts.length] =
                  m_ClassCombo.getSelectedIndex();
                ignoredAtts = newIgnoredAtts;
              }
            }

            outBuff.append("Test mode:    ");
            switch (testMode) {
            case 3: // Test on training
              outBuff.append("evaluate on training data\n");
              break;
            case 2: // Percent split
              outBuff.append("split " + percent + "% train, remainder test\n");
              break;
            case 4: // Test on user split
              outBuff.append("user supplied test set: "
                + userTest.numInstances() + " instances\n");
              break;
            case 5: // Classes to clusters evaluation on training
              outBuff.append("Classes to clusters evaluation on training data");

              break;
            }
            outBuff.append("\n");
            m_History.addResult(name, outBuff);
            m_History.setSingle(name);

            // Build the model and output it.
            m_Log.statusMessage("Building model on training data...");

            // remove the class attribute (if set) and build the clusterer
            trainTimeStart = System.currentTimeMillis();
            clusterer.buildClusterer(removeClass(trainInst));
            trainTimeElapsed = System.currentTimeMillis() - trainTimeStart;

            // if (testMode == 2) {
            outBuff
              .append("\n=== Clustering model (full training set) ===\n\n");

            outBuff.append(clusterer.toString() + '\n');
            outBuff
              .append("\nTime taken to build model (full training data) : "
                + Utils.doubleToString(trainTimeElapsed / 1000.0, 2)
                + " seconds\n\n");
            // }
            m_History.updateResult(name);
            if (clusterer instanceof Drawable) {
              try {
                grph = ((Drawable) clusterer).graph();
              } catch (Exception ex) {
              }
            }
            // copy full model for output
            SerializedObject so = new SerializedObject(clusterer);
            fullClusterer = (Clusterer) so.getObject();

            ClusterEvaluation eval = new ClusterEvaluation();
            eval.setClusterer(clusterer);
            switch (testMode) {
            case 3:
            case 5: // Test on training
              m_Log.statusMessage("Clustering training data...");
              eval.evaluateClusterer(trainInst, "", false);
              plotInstances.setInstances(inst);
              plotInstances.setClusterEvaluation(eval);
              outBuff
                .append("=== Model and evaluation on training set ===\n\n");
              break;

            case 2: // Percent split
              m_Log.statusMessage("Randomizing instances...");
              inst.randomize(new Random(1));
              trainInst.randomize(new Random(1));
              int trainSize = trainInst.numInstances() * percent / 100;
              int testSize = trainInst.numInstances() - trainSize;
              Instances train = new Instances(trainInst, 0, trainSize);
              Instances test = new Instances(trainInst, trainSize, testSize);
              Instances testVis = new Instances(inst, trainSize, testSize);
              m_Log.statusMessage("Building model on training split...");
              trainTimeStart = System.currentTimeMillis();
              clusterer.buildClusterer(train);
              trainTimeElapsed = System.currentTimeMillis() - trainTimeStart;
              m_Log.statusMessage("Evaluating on test split...");
              eval.evaluateClusterer(test, "", false);
              plotInstances.setInstances(testVis);
              plotInstances.setClusterEvaluation(eval);
              outBuff.append("=== Model and evaluation on test split ===\n");
              outBuff.append(clusterer.toString() + "\n");
              outBuff
                .append("\nTime taken to build model (percentage split) : "
                  + Utils.doubleToString(trainTimeElapsed / 1000.0, 2)
                  + " seconds\n\n");
              break;

            case 4: // Test on user split
              m_Log.statusMessage("Evaluating on test data...");
              Instances userTestT = new Instances(userTest);
              if (!m_ignoreKeyList.isSelectionEmpty()) {
                userTestT = removeIgnoreCols(userTestT);
              }
              eval.evaluateClusterer(userTestT, "", false);
              plotInstances.setInstances(userTest);
              plotInstances.setClusterEvaluation(eval);
              outBuff.append("=== Evaluation on test set ===\n");
              break;

            default:
              throw new Exception("Test mode not implemented");
            }
            outBuff.append(eval.clusterResultsToString());
            outBuff.append("\n");
            m_History.updateResult(name);
            m_Log.logMessage("Finished " + cname);
            m_Log.statusMessage("OK");
          } catch (Exception ex) {
            ex.printStackTrace();
            m_Log.logMessage(ex.getMessage());
            JOptionPane.showMessageDialog(ClustererPanel.this,
              "Problem evaluating clusterer:\n" + ex.getMessage(),
              "Evaluate clusterer", JOptionPane.ERROR_MESSAGE);
            m_Log.statusMessage("Problem evaluating clusterer");
          } finally {
            if ((plotInstances != null) && plotInstances.canPlot(true)) {
              m_CurrentVis = new VisualizePanel();
              if (getMainApplication() != null) {
                Settings settings = getMainApplication().getApplicationSettings();
                m_CurrentVis.applySettings(settings,
                  weka.gui.explorer.VisualizePanel.ScatterDefaults.ID);
              }
              m_CurrentVis.setName(name + " (" + inst.relationName() + ")");
              m_CurrentVis.setLog(m_Log);
              try {
                m_CurrentVis.addPlot(plotInstances.getPlotData(name));
              } catch (Exception ex) {
                System.err.println(ex);
              }
              plotInstances.cleanUp();

              ArrayList<Object> vv = new ArrayList<Object>();
              vv.add(fullClusterer);
              Instances trainHeader = new Instances(m_Instances, 0);
              vv.add(trainHeader);
              if (ignoredAtts != null) {
                vv.add(ignoredAtts);
              }
              if (saveVis) {
                vv.add(m_CurrentVis);
                if (grph != null) {
                  vv.add(grph);
                }

              }
              m_History.addObject(name, vv);
            }
            if (isInterrupted()) {
              m_Log.logMessage("Interrupted " + cname);
              m_Log.statusMessage("See error log");
            }
            m_RunThread = null;
            m_StartBut.setEnabled(true);
            m_StopBut.setEnabled(false);
            m_ignoreBut.setEnabled(true);
            if (m_Log instanceof TaskLogger) {
              ((TaskLogger) m_Log).taskFinished();
            }
          }
        }
      };
      m_RunThread.setPriority(Thread.MIN_PRIORITY);
      m_RunThread.start();
    }
  }

  private Instances removeClass(Instances inst) {
    Remove af = new Remove();
    Instances retI = null;

    try {
      if (inst.classIndex() < 0) {
        retI = inst;
      } else {
        af.setAttributeIndices("" + (inst.classIndex() + 1));
        af.setInvertSelection(false);
        af.setInputFormat(inst);
        retI = Filter.useFilter(inst, af);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return retI;
  }

  private Instances removeIgnoreCols(Instances inst) {

    // If the user is doing classes to clusters evaluation and
    // they have opted to ignore the class, then unselect the class in
    // the ignore list
    if (m_ClassesToClustersBut.isSelected()) {
      int classIndex = m_ClassCombo.getSelectedIndex();
      if (m_ignoreKeyList.isSelectedIndex(classIndex)) {
        m_ignoreKeyList.removeSelectionInterval(classIndex, classIndex);
      }
    }
    int[] selected = m_ignoreKeyList.getSelectedIndices();
    Remove af = new Remove();
    Instances retI = null;

    try {
      af.setAttributeIndicesArray(selected);
      af.setInvertSelection(false);
      af.setInputFormat(inst);
      retI = Filter.useFilter(inst, af);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return retI;
  }

  private Instances removeIgnoreCols(Instances inst, int[] toIgnore) {

    Remove af = new Remove();
    Instances retI = null;

    try {
      af.setAttributeIndicesArray(toIgnore);
      af.setInvertSelection(false);
      af.setInputFormat(inst);
      retI = Filter.useFilter(inst, af);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return retI;
  }

  /**
   * Stops the currently running clusterer (if any).
   */
  @SuppressWarnings("deprecation")
  protected void stopClusterer() {

    if (m_RunThread != null) {
      m_RunThread.interrupt();

      // This is deprecated (and theoretically the interrupt should do).
      m_RunThread.stop();

    }
  }

  /**
   * Pops up a TreeVisualizer for the clusterer from the currently selected item
   * in the results list
   * 
   * @param graphString the description of the tree in dotty format
   * @param treeName the title to assign to the display
   */
  protected void visualizeTree(String graphString, String treeName) {
    final javax.swing.JFrame jf =
      new javax.swing.JFrame("Weka Classifier Tree Visualizer: " + treeName);
    jf.setSize(500, 400);
    jf.getContentPane().setLayout(new BorderLayout());
    if (graphString.contains("digraph")) {
      TreeVisualizer tv =
        new TreeVisualizer(null, graphString, new PlaceNode2());
      jf.getContentPane().add(tv, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
          jf.dispose();
        }
      });
      jf.setVisible(true);
      tv.fitToScreen();
    } else if (graphString.startsWith("Newick:")) {
      HierarchyVisualizer tv =
        new HierarchyVisualizer(graphString.substring(7));
      jf.getContentPane().add(tv, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
          jf.dispose();
        }
      });
      jf.setVisible(true);
      tv.fitToScreen();
    }
  }

  /**
   * Pops up a visualize panel to display cluster assignments
   * 
   * @param sp the visualize panel to display
   */
  protected void visualizeClusterAssignments(VisualizePanel sp) {
    if (sp != null) {
      String plotName = sp.getName();
      final javax.swing.JFrame jf =
        new javax.swing.JFrame("Weka Clusterer Visualize: " + plotName);
      jf.setSize(500, 400);
      jf.getContentPane().setLayout(new BorderLayout());
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
          jf.dispose();
        }
      });

      jf.setVisible(true);
    }
  }

  /**
   * Handles constructing a popup menu with visualization options
   * 
   * @param name the name of the result history list entry clicked on by the
   *          user
   * @param x the x coordinate for popping up the menu
   * @param y the y coordinate for popping up the menu
   */
  @SuppressWarnings("unchecked")
  protected void visualizeClusterer(String name, int x, int y) {
    final String selectedName = name;
    JPopupMenu resultListMenu = new JPopupMenu();

    JMenuItem visMainBuffer = new JMenuItem("View in main window");
    if (selectedName != null) {
      visMainBuffer.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          m_History.setSingle(selectedName);
        }
      });
    } else {
      visMainBuffer.setEnabled(false);
    }
    resultListMenu.add(visMainBuffer);

    JMenuItem visSepBuffer = new JMenuItem("View in separate window");
    if (selectedName != null) {
      visSepBuffer.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          m_History.openFrame(selectedName);
        }
      });
    } else {
      visSepBuffer.setEnabled(false);
    }
    resultListMenu.add(visSepBuffer);

    JMenuItem saveOutput = new JMenuItem("Save result buffer");
    if (selectedName != null) {
      saveOutput.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          saveBuffer(selectedName);
        }
      });
    } else {
      saveOutput.setEnabled(false);
    }
    resultListMenu.add(saveOutput);

    JMenuItem deleteOutput = new JMenuItem("Delete result buffer");
    if (selectedName != null) {
      deleteOutput.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          m_History.removeResult(selectedName);
        }
      });
    } else {
      deleteOutput.setEnabled(false);
    }
    resultListMenu.add(deleteOutput);

    resultListMenu.addSeparator();

    JMenuItem loadModel = new JMenuItem("Load model");
    loadModel.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        loadClusterer();
      }
    });
    resultListMenu.add(loadModel);

    ArrayList<Object> o = null;
    if (selectedName != null) {
      o = (ArrayList<Object>) m_History.getNamedObject(selectedName);
    }

    VisualizePanel temp_vp = null;
    String temp_grph = null;
    Clusterer temp_clusterer = null;
    Instances temp_trainHeader = null;
    int[] temp_ignoreAtts = null;

    if (o != null) {
      for (int i = 0; i < o.size(); i++) {
        Object temp = o.get(i);
        if (temp instanceof Clusterer) {
          temp_clusterer = (Clusterer) temp;
        } else if (temp instanceof Instances) { // training header
          temp_trainHeader = (Instances) temp;
        } else if (temp instanceof int[]) { // ignored attributes
          temp_ignoreAtts = (int[]) temp;
        } else if (temp instanceof VisualizePanel) { // normal errors
          temp_vp = (VisualizePanel) temp;
        } else if (temp instanceof String) { // graphable output
          temp_grph = (String) temp;
        }
      }
    }

    final VisualizePanel vp = temp_vp;
    final String grph = temp_grph;
    final Clusterer clusterer = temp_clusterer;
    final Instances trainHeader = temp_trainHeader;
    final int[] ignoreAtts = temp_ignoreAtts;

    JMenuItem saveModel = new JMenuItem("Save model");
    if (clusterer != null) {
      saveModel.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          saveClusterer(selectedName, clusterer, trainHeader, ignoreAtts);
        }
      });
    } else {
      saveModel.setEnabled(false);
    }
    resultListMenu.add(saveModel);

    JMenuItem reEvaluate =
      new JMenuItem("Re-evaluate model on current test set");
    if (clusterer != null && m_TestInstances != null) {
      reEvaluate.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          reevaluateModel(selectedName, clusterer, trainHeader, ignoreAtts);
        }
      });
    } else {
      reEvaluate.setEnabled(false);
    }
    resultListMenu.add(reEvaluate);

    JMenuItem reApplyConfig =
      new JMenuItem("Re-apply this model's configuration");
    if (clusterer != null) {
      reApplyConfig.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          m_ClustererEditor.setValue(clusterer);
        }
      });
    } else {
      reApplyConfig.setEnabled(false);
    }
    resultListMenu.add(reApplyConfig);

    resultListMenu.addSeparator();

    JMenuItem visClusts = new JMenuItem("Visualize cluster assignments");
    if (vp != null) {
      visClusts.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          visualizeClusterAssignments(vp);
        }
      });

    } else {
      visClusts.setEnabled(false);
    }
    resultListMenu.add(visClusts);

    JMenuItem visTree = new JMenuItem("Visualize tree");
    if (grph != null) {
      visTree.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          String title;
          if (vp != null) {
            title = vp.getName();
          } else {
            title = selectedName;
          }
          visualizeTree(grph, title);
        }
      });
    } else {
      visTree.setEnabled(false);
    }
    resultListMenu.add(visTree);

    // visualization plugins
    JMenu visPlugins = new JMenu("Plugins");
    boolean availablePlugins = false;

    // trees
    if (grph != null) {
      // trees
      Vector<String> pluginsVector =
        GenericObjectEditor.getClassnames(TreeVisualizePlugin.class.getName());
      for (int i = 0; i < pluginsVector.size(); i++) {
        String className = (pluginsVector.elementAt(i));
        try {
          TreeVisualizePlugin plugin =
            (TreeVisualizePlugin) Class.forName(className).newInstance();
          if (plugin == null) {
            continue;
          }
          availablePlugins = true;
          JMenuItem pluginMenuItem =
            plugin.getVisualizeMenuItem(grph, selectedName);
          Version version = new Version();
          if (pluginMenuItem != null) {
            if (version.compareTo(plugin.getMinVersion()) < 0) {
              pluginMenuItem.setText(pluginMenuItem.getText()
                + " (weka outdated)");
            }
            if (version.compareTo(plugin.getMaxVersion()) >= 0) {
              pluginMenuItem.setText(pluginMenuItem.getText()
                + " (plugin outdated)");
            }
            visPlugins.add(pluginMenuItem);
          }
        } catch (Exception e) {
          // e.printStackTrace();
        }
      }
    }

    if (availablePlugins) {
      resultListMenu.add(visPlugins);
    }

    resultListMenu.show(m_History.getList(), x, y);
  }

  /**
   * Save the currently selected clusterer output to a file.
   * 
   * @param name the name of the buffer to save
   */
  protected void saveBuffer(String name) {
    StringBuffer sb = m_History.getNamedBuffer(name);
    if (sb != null) {
      if (m_SaveOut.save(sb)) {
        m_Log.logMessage("Save successful.");
      }
    }
  }

  private void setIgnoreColumns() {
    ListSelectorDialog jd = new ListSelectorDialog(null, m_ignoreKeyList);

    // Open the dialog
    int result = jd.showDialog();

    if (result != ListSelectorDialog.APPROVE_OPTION) {
      // clear selected indices
      m_ignoreKeyList.clearSelection();
    }
    updateCapabilitiesFilter(m_ClustererEditor.getCapabilitiesFilter());
  }

  /**
   * Saves the currently selected clusterer
   */
  protected void saveClusterer(String name, Clusterer clusterer,
    Instances trainHeader, int[] ignoredAtts) {

    File sFile = null;
    boolean saveOK = true;

    int returnVal = m_FileChooser.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      sFile = m_FileChooser.getSelectedFile();
      if (!sFile.getName().toLowerCase().endsWith(MODEL_FILE_EXTENSION)) {
        sFile =
          new File(sFile.getParent(), sFile.getName() + MODEL_FILE_EXTENSION);
      }
      m_Log.statusMessage("Saving model to file...");

      try {
        OutputStream os = new FileOutputStream(sFile);
        if (sFile.getName().endsWith(".gz")) {
          os = new GZIPOutputStream(os);
        }
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(os);
        objectOutputStream.writeObject(clusterer);
        if (trainHeader != null) {
          objectOutputStream.writeObject(trainHeader);
        }
        if (ignoredAtts != null) {
          objectOutputStream.writeObject(ignoredAtts);
        }
        objectOutputStream.flush();
        objectOutputStream.close();
      } catch (Exception e) {

        JOptionPane.showMessageDialog(null, e, "Save Failed",
          JOptionPane.ERROR_MESSAGE);
        saveOK = false;
      }
      if (saveOK) {
        m_Log.logMessage("Saved model (" + name + ") to file '"
          + sFile.getName() + "'");
      }
      m_Log.statusMessage("OK");
    }
  }

  /**
   * Loads a clusterer
   */
  protected void loadClusterer() {

    int returnVal = m_FileChooser.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File selected = m_FileChooser.getSelectedFile();
      Clusterer clusterer = null;
      Instances trainHeader = null;
      int[] ignoredAtts = null;

      m_Log.statusMessage("Loading model from file...");

      try {
        InputStream is = new FileInputStream(selected);
        if (selected.getName().endsWith(".gz")) {
          is = new GZIPInputStream(is);
        }
        ObjectInputStream objectInputStream = new ObjectInputStream(is);
        clusterer = (Clusterer) objectInputStream.readObject();
        try { // see if we can load the header & ignored attribute info
          trainHeader = (Instances) objectInputStream.readObject();
          ignoredAtts = (int[]) objectInputStream.readObject();
        } catch (Exception e) {
        } // don't fuss if we can't
        objectInputStream.close();
      } catch (Exception e) {

        JOptionPane.showMessageDialog(null, e, "Load Failed",
          JOptionPane.ERROR_MESSAGE);
      }

      m_Log.statusMessage("OK");

      if (clusterer != null) {
        m_Log.logMessage("Loaded model from file '" + selected.getName() + "'");
        String name = (new SimpleDateFormat("HH:mm:ss - ")).format(new Date());
        String cname = clusterer.getClass().getName();
        if (cname.startsWith("weka.clusterers.")) {
          cname = cname.substring("weka.clusterers.".length());
        }
        name += cname + " from file '" + selected.getName() + "'";
        StringBuffer outBuff = new StringBuffer();

        outBuff.append("=== Model information ===\n\n");
        outBuff.append("Filename:     " + selected.getName() + "\n");
        outBuff.append("Scheme:       " + clusterer.getClass().getName());
        if (clusterer instanceof OptionHandler) {
          String[] o = ((OptionHandler) clusterer).getOptions();
          outBuff.append(" " + Utils.joinOptions(o));
        }
        outBuff.append("\n");

        if (trainHeader != null) {

          outBuff.append("Relation:     " + trainHeader.relationName() + '\n');
          outBuff.append("Attributes:   " + trainHeader.numAttributes() + '\n');
          if (trainHeader.numAttributes() < 100) {
            boolean[] selectedAtts = new boolean[trainHeader.numAttributes()];
            for (int i = 0; i < trainHeader.numAttributes(); i++) {
              selectedAtts[i] = true;
            }

            if (ignoredAtts != null) {
              for (int i = 0; i < ignoredAtts.length; i++) {
                selectedAtts[ignoredAtts[i]] = false;
              }
            }

            for (int i = 0; i < trainHeader.numAttributes(); i++) {
              if (selectedAtts[i]) {
                outBuff.append("              "
                  + trainHeader.attribute(i).name() + '\n');
              }
            }
            if (ignoredAtts != null) {
              outBuff.append("Ignored:\n");
              for (int ignoredAtt : ignoredAtts) {
                outBuff.append("              "
                  + trainHeader.attribute(ignoredAtt).name() + '\n');
              }
            }
          } else {
            outBuff.append("              [list of attributes omitted]\n");
          }
        } else {
          outBuff.append("\nTraining data unknown\n");
        }

        outBuff.append("\n=== Clustering model ===\n\n");
        outBuff.append(clusterer.toString() + "\n");

        m_History.addResult(name, outBuff);
        m_History.setSingle(name);
        ArrayList<Object> vv = new ArrayList<Object>();
        vv.add(clusterer);
        if (trainHeader != null) {
          vv.add(trainHeader);
        }
        if (ignoredAtts != null) {
          vv.add(ignoredAtts);
        }
        // allow visualization of graphable classifiers
        String grph = null;
        if (clusterer instanceof Drawable) {
          try {
            grph = ((Drawable) clusterer).graph();
          } catch (Exception ex) {
          }
        }
        if (grph != null) {
          vv.add(grph);
        }

        m_History.addObject(name, vv);

      }
    }
  }

  /**
   * Re-evaluates the named clusterer with the current test set. Unpredictable
   * things will happen if the data set is not compatible with the clusterer.
   * 
   * @param name the name of the clusterer entry
   * @param clusterer the clusterer to evaluate
   * @param trainHeader the header of the training set
   * @param ignoredAtts ignored attributes
   */
  protected void reevaluateModel(final String name, final Clusterer clusterer,
    final Instances trainHeader, final int[] ignoredAtts) {

    if (m_RunThread == null) {
      m_StartBut.setEnabled(false);
      m_StopBut.setEnabled(true);
      m_ignoreBut.setEnabled(false);
      m_RunThread = new Thread() {
        @Override
        public void run() {
          // Copy the current state of things
          m_Log.statusMessage("Setting up...");

          StringBuffer outBuff = m_History.getNamedBuffer(name);
          Instances userTest = null;

          ClustererAssignmentsPlotInstances plotInstances =
            ExplorerDefaults.getClustererAssignmentsPlotInstances();
          plotInstances.setClusterer(clusterer);
          if (m_TestInstances != null) {
            userTest = new Instances(m_TestInstances);
          }

          boolean saveVis = m_StorePredictionsBut.isSelected();
          String grph = null;

          try {
            if (userTest == null) {
              throw new Exception("No user test set has been opened");
            }
            if (trainHeader != null && !trainHeader.equalHeaders(userTest)) {
              throw new Exception("Train and test set are not compatible\n"
                + trainHeader.equalHeadersMsg(userTest));
            }

            m_Log.statusMessage("Evaluating on test data...");
            m_Log.logMessage("Re-evaluating clusterer (" + name
              + ") on test set");

            m_Log.logMessage("Started reevaluate model");
            if (m_Log instanceof TaskLogger) {
              ((TaskLogger) m_Log).taskStarted();
            }
            ClusterEvaluation eval = new ClusterEvaluation();
            eval.setClusterer(clusterer);

            Instances userTestT = new Instances(userTest);
            if (ignoredAtts != null) {
              userTestT = removeIgnoreCols(userTestT, ignoredAtts);
            }

            eval.evaluateClusterer(userTestT);

            plotInstances.setClusterEvaluation(eval);
            plotInstances.setInstances(userTest);
            plotInstances.setUp();

            outBuff.append("\n=== Re-evaluation on test set ===\n\n");
            outBuff.append("User supplied test set\n");
            outBuff.append("Relation:     " + userTest.relationName() + '\n');
            outBuff.append("Instances:    " + userTest.numInstances() + '\n');
            outBuff
              .append("Attributes:   " + userTest.numAttributes() + "\n\n");
            if (trainHeader == null) {
              outBuff
                .append("NOTE - if test set is not compatible then results are "
                  + "unpredictable\n\n");
            }

            outBuff.append(eval.clusterResultsToString());
            outBuff.append("\n");
            m_History.updateResult(name);
            m_Log.logMessage("Finished re-evaluation");
            m_Log.statusMessage("OK");
          } catch (Exception ex) {
            ex.printStackTrace();
            m_Log.logMessage(ex.getMessage());
            JOptionPane.showMessageDialog(ClustererPanel.this,
              "Problem evaluating clusterer:\n" + ex.getMessage(),
              "Evaluate clusterer", JOptionPane.ERROR_MESSAGE);
            m_Log.statusMessage("Problem evaluating clusterer");

          } finally {
            if (plotInstances != null) {
              m_CurrentVis = new VisualizePanel();
              if (getMainApplication() != null) {
                Settings settings = getMainApplication().getApplicationSettings();
                m_CurrentVis.applySettings(settings,
                  weka.gui.explorer.VisualizePanel.ScatterDefaults.ID);
              }
              m_CurrentVis.setName(name + " (" + userTest.relationName() + ")");
              m_CurrentVis.setLog(m_Log);
              try {
                m_CurrentVis.addPlot(plotInstances.getPlotData(name));
              } catch (Exception ex) {
                System.err.println(ex);
              }

              ArrayList<Object> vv = new ArrayList<Object>();
              vv.add(clusterer);
              if (trainHeader != null) {
                vv.add(trainHeader);
              }
              if (ignoredAtts != null) {
                vv.add(ignoredAtts);
              }
              if (saveVis) {
                vv.add(m_CurrentVis);
                if (grph != null) {
                  vv.add(grph);
                }

              }
              m_History.addObject(name, vv);

            }
            if (isInterrupted()) {
              m_Log.logMessage("Interrupted reevaluate model");
              m_Log.statusMessage("See error log");
            }
            m_RunThread = null;
            m_StartBut.setEnabled(true);
            m_StopBut.setEnabled(false);
            m_ignoreBut.setEnabled(true);
            if (m_Log instanceof TaskLogger) {
              ((TaskLogger) m_Log).taskFinished();
            }
          }
        }

      };
      m_RunThread.setPriority(Thread.MIN_PRIORITY);
      m_RunThread.start();
    }
  }

  /**
   * updates the capabilities filter of the GOE
   * 
   * @param filter the new filter to use
   */
  protected void updateCapabilitiesFilter(Capabilities filter) {
    Instances tempInst;
    Capabilities filterClass;

    if (filter == null) {
      m_ClustererEditor.setCapabilitiesFilter(new Capabilities(null));
      return;
    }

    if (!ExplorerDefaults.getInitGenericObjectEditorFilter()) {
      tempInst = new Instances(m_Instances, 0);
    } else {
      tempInst = new Instances(m_Instances);
    }
    tempInst.setClassIndex(-1);

    if (!m_ignoreKeyList.isSelectionEmpty()) {
      tempInst = removeIgnoreCols(tempInst);
    }

    if (m_ClassesToClustersBut.isSelected()) {
      // remove the class too
      String classSelection = m_ClassCombo.getSelectedItem().toString();
      classSelection =
        classSelection.substring(classSelection.indexOf(")") + 1).trim();
      int classIndex = tempInst.attribute(classSelection).index();

      Remove rm = new Remove();
      rm.setAttributeIndices("" + (classIndex + 1));
      try {
        rm.setInputFormat(tempInst);
        tempInst = Filter.useFilter(tempInst, rm);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    try {
      filterClass = Capabilities.forInstances(tempInst);
    } catch (Exception e) {
      filterClass = new Capabilities(null);
    }

    m_ClustererEditor.setCapabilitiesFilter(filterClass);

    // check capabilities
    m_StartBut.setEnabled(true);
    Capabilities currentFilter = m_ClustererEditor.getCapabilitiesFilter();
    Clusterer clusterer = (Clusterer) m_ClustererEditor.getValue();
    Capabilities currentSchemeCapabilities = null;
    if (clusterer != null && currentFilter != null
      && (clusterer instanceof CapabilitiesHandler)) {
      currentSchemeCapabilities =
        ((CapabilitiesHandler) clusterer).getCapabilities();

      if (!currentSchemeCapabilities.supportsMaybe(currentFilter)
        && !currentSchemeCapabilities.supports(currentFilter)) {
        m_StartBut.setEnabled(false);
      }
    }
  }

  /**
   * method gets called in case of a change event
   * 
   * @param e the associated change event
   */
  @Override
  public void capabilitiesFilterChanged(CapabilitiesFilterChangeEvent e) {
    if (e.getFilter() == null) {
      updateCapabilitiesFilter(null);
    } else {
      updateCapabilitiesFilter((Capabilities) e.getFilter().clone());
    }
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
    return "Cluster";
  }

  /**
   * Returns the tooltip for the tab in the Explorer
   * 
   * @return the tooltip of this tab
   */
  @Override
  public String getTabTitleToolTip() {
    return "Identify instance clusters";
  }

  @Override
  public boolean requiresLog() {
    return true;
  }

  @Override
  public boolean acceptsInstances() {
    return true;
  }

  @Override
  public Defaults getDefaultSettings() {
    return new ClustererPanelDefaults();
  }

  @Override
  public boolean okToBeActive() {
    return m_Instances != null;
  }

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
      if (!m_initialSettingsSet) {
        m_initialSettingsSet = true;
        Object initialC =
          getMainApplication().getApplicationSettings().getSetting(
            getPerspectiveID(), ClustererPanelDefaults.CLUSTERER_KEY,
            ClustererPanelDefaults.CLUSTERER, Environment.getSystemWide());
        m_ClustererEditor.setValue(initialC);

        TestMode iniitalTestMode =
          getMainApplication().getApplicationSettings().getSetting(
            getPerspectiveID(), ClustererPanelDefaults.TEST_MODE_KEY,
            ClustererPanelDefaults.TEST_MODE, Environment.getSystemWide());
        m_TrainBut.setSelected(iniitalTestMode == TestMode.USE_TRAINING_SET);
        m_PercentBut.setSelected(iniitalTestMode == TestMode.PERCENTAGE_SPLIT);
        m_TestSplitBut
          .setSelected(iniitalTestMode == TestMode.SUPPLIED_TEST_SET);
        m_ClassesToClustersBut
          .setSelected(iniitalTestMode == TestMode.CLASSES_TO_CLUSTERS_EVAL);
        m_StorePredictionsBut.setSelected(getMainApplication()
          .getApplicationSettings().getSetting(getPerspectiveID(),
            ClustererPanelDefaults.STORE_CLUSTERS_FOR_VIS_KEY,
            ClustererPanelDefaults.STORE_CLUSTERS_VIS,
            Environment.getSystemWide()));
      }
      Font outputFont =
        getMainApplication().getApplicationSettings().getSetting(
          getPerspectiveID(), ClustererPanelDefaults.OUTPUT_FONT_KEY,
          ClustererPanelDefaults.OUTPUT_FONT, Environment.getSystemWide());
      m_OutText.setFont(outputFont);
      Color textColor =
        getMainApplication().getApplicationSettings()
          .getSetting(getPerspectiveID(),
            ClustererPanelDefaults.OUTPUT_TEXT_COLOR_KEY,
            ClustererPanelDefaults.OUTPUT_TEXT_COLOR,
            Environment.getSystemWide());
      m_OutText.setForeground(textColor);
      Color outputBackgroundColor =
        getMainApplication().getApplicationSettings().getSetting(
          getPerspectiveID(),
          ClustererPanelDefaults.OUTPUT_BACKGROUND_COLOR_KEY,
          ClustererPanelDefaults.OUTPUT_BACKGROUND_COLOR,
          Environment.getSystemWide());
      m_OutText.setBackground(outputBackgroundColor);
      m_History.setBackground(outputBackgroundColor);
    }
  }

  public static enum TestMode {
    PERCENTAGE_SPLIT, USE_TRAINING_SET, SUPPLIED_TEST_SET,
    CLASSES_TO_CLUSTERS_EVAL;
  }

  /**
   * Default settings for the clusterer panel
   */
  protected static final class ClustererPanelDefaults extends Defaults {
    public static final String ID = "weka.gui.explorer.clustererpanel";

    protected static final Settings.SettingKey CLUSTERER_KEY =
      new Settings.SettingKey(ID + ".initialClusterer", "Initial clusterer",
        "On startup, set this clusterer as the default one");
    protected static final Clusterer CLUSTERER = new SimpleKMeans();

    protected static final Settings.SettingKey TEST_MODE_KEY =
      new Settings.SettingKey(ID + ".initialTestMode", "Default test mode", "");
    protected static final TestMode TEST_MODE = TestMode.USE_TRAINING_SET;

    protected static final Settings.SettingKey STORE_CLUSTERS_FOR_VIS_KEY =
      new Settings.SettingKey(ID + ".storeClusterersForVis", "Store clusters "
        + "for visualization", "");
    protected static final boolean STORE_CLUSTERS_VIS = true;

    protected static final Settings.SettingKey OUTPUT_FONT_KEY =
      new Settings.SettingKey(ID + ".outputFont", "Font for text output",
        "Font to " + "use in the output area");
    protected static final Font OUTPUT_FONT = new Font("Monospaced",
      Font.PLAIN, 12);

    protected static final Settings.SettingKey OUTPUT_TEXT_COLOR_KEY =
      new Settings.SettingKey(ID + ".outputFontColor", "Output text color",
        "Color " + "of output text");
    protected static final Color OUTPUT_TEXT_COLOR = Color.black;

    protected static final Settings.SettingKey OUTPUT_BACKGROUND_COLOR_KEY =
      new Settings.SettingKey(ID + ".outputBackgroundColor",
        "Output background color", "Output background color");
    protected static final Color OUTPUT_BACKGROUND_COLOR = Color.white;

    private static final long serialVersionUID = 2708388782229179493L;

    public ClustererPanelDefaults() {
      super(ID);

      m_defaults.put(CLUSTERER_KEY, CLUSTERER);
      m_defaults.put(TEST_MODE_KEY, TEST_MODE);
      m_defaults.put(STORE_CLUSTERS_FOR_VIS_KEY, STORE_CLUSTERS_VIS);
      m_defaults.put(OUTPUT_FONT_KEY, OUTPUT_FONT);
      m_defaults.put(OUTPUT_TEXT_COLOR_KEY, OUTPUT_TEXT_COLOR);
      m_defaults.put(OUTPUT_BACKGROUND_COLOR_KEY, OUTPUT_BACKGROUND_COLOR);
    }
  }

  /**
   * Tests out the clusterer panel from the command line.
   * 
   * @param args may optionally contain the name of a dataset to load.
   */
  public static void main(String[] args) {

    try {
      final javax.swing.JFrame jf =
        new javax.swing.JFrame("Weka Explorer: Cluster");
      jf.getContentPane().setLayout(new BorderLayout());
      final ClustererPanel sp = new ClustererPanel();
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      weka.gui.LogPanel lp = new weka.gui.LogPanel();
      sp.setLog(lp);
      jf.getContentPane().add(lp, BorderLayout.SOUTH);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
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
