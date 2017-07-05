/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    AutoWEKAPanel.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.explorer;

import weka.classifiers.meta.AutoWEKAClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Range;
import weka.core.Utils;
import weka.gui.GenericObjectEditor;
import weka.gui.Logger;
import weka.gui.PropertyPanel;
import weka.gui.ResultHistoryPanel;
import weka.gui.SaveBuffer;
import weka.gui.SysErrLog;
import weka.gui.TaskLogger;
import weka.gui.explorer.Explorer.ExplorerPanel;
import weka.gui.explorer.Explorer.LogHandler;
import weka.gui.explorer.ClassifierPanel;
import weka.gui.explorer.Explorer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Desktop;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This class provides a special, simple panel for Auto-WEKA in the Explorer.
 *
 * @author EibeFrank and Lars Kotthoff
 * @version $Revision: 908 $
 */
public class AutoWEKAPanel extends ClassifierPanel implements ExplorerPanel, LogHandler {

  /** for serialization. */
  private static final long serialVersionUID = 3089066653508312179L;

  /** the parent frame. */
  protected Explorer m_Explorer = null;

  /** The output area for classification results. */
  protected JTextArea m_OutText = new JTextArea(20, 40);

  /** The destination for log/status messages. */
  protected Logger m_Log = new SysErrLog();

  /** The buffer saving object for saving output. */
  protected SaveBuffer m_SaveOut = new SaveBuffer(m_Log, this);

  /** A panel controlling results viewing. */
  protected ResultHistoryPanel m_History = new ResultHistoryPanel(m_OutText);

  /** Lets the user select the class column. */
  protected JComboBox m_ClassCombo = new JComboBox();

  /** Click to download manual. */
  protected JButton m_ManualBut = new JButton("Auto-WEKA Manual");

  /** Click to start running the experiment. */
  protected JButton m_StartBut = new JButton("Start");

  /** Click to stop a running experiment. */
  protected JButton m_StopBut = new JButton("Stop");

  /** Lets the user configure the classifier. */
  protected GenericObjectEditor m_ClassifierEditor = new GenericObjectEditor();

  /** The panel showing the current classifier selection. */
  protected PropertyPanel m_CEPanel = new PropertyPanel(m_ClassifierEditor, true);

  /** The main set of instances we're playing with. */
  protected Instances m_Instances;

  /** A thread that classification runs in. */
  protected Thread m_RunThread;

  /** The Auto-WEKA classifier. */
  protected AutoWEKAClassifier aw;

  /**
   * Creates the Auto-WEKA panel.
   */
  public AutoWEKAPanel() {
    m_OutText.setEditable(false);
    m_OutText.setFont(new Font("Monospaced", Font.PLAIN, 12));
    m_OutText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    m_OutText.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
    if ((e.getModifiers() & InputEvent.BUTTON1_MASK)
        != InputEvent.BUTTON1_MASK) {
      m_OutText.selectAll();
    }
      }
    });

    m_History.setBorder(BorderFactory.createTitledBorder("Result list (right-click for options)"));

    m_ClassifierEditor.setClassType(Classifier.class);
    m_ClassifierEditor.setValue(new weka.classifiers.meta.AutoWEKAClassifier());
    m_ClassifierEditor.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
        repaint();
        }
    });

    m_StartBut.setToolTipText("Starts Auto-WEKA");
    m_StartBut.setEnabled(false);
    m_StartBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        startAutoWEKA();
      }
    });

    m_ManualBut.setToolTipText("Starts a browser to the Auto-WEKA manual");
    m_ManualBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
            Desktop.getDesktop().browse(new java.net.URI("http://www.cs.ubc.ca/labs/beta/Projects/autoweka/manual.pdf"));
        } catch(Exception ex) {
        }
      }
    });

    m_StopBut.setToolTipText("Stops Auto-WEKA");
    m_StopBut.setEnabled(false);
    m_StopBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        stopAutoWEKA();
      }
    });

    m_History.setHandleRightClicks(false);
    // see if we can popup a menu for the selected result
    m_History.getList().addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
    if (((e.getModifiers() & InputEvent.BUTTON1_MASK)
        != InputEvent.BUTTON1_MASK) || e.isAltDown()) {
      int index = m_History.getList().locationToIndex(e.getPoint());
      if (index != -1) {
        String name = m_History.getNameAtIndex(index);
        showPopup(name, e.getX(), e.getY());
      } else {
        showPopup(null, e.getX(), e.getY());
      }
    }
      }
    });

    // Layout the GUI
    GridBagConstraints gbC;
    JLabel label;

    JPanel buttons = new JPanel();
    buttons.setLayout(new BorderLayout());
    buttons.add(m_ClassCombo, BorderLayout.NORTH);
    m_ClassCombo.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JPanel ssButs = new JPanel();
    ssButs.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    ssButs.setLayout(new GridLayout(1, 2, 5, 5));
    ssButs.add(m_StartBut);
    ssButs.add(m_StopBut);

    buttons.add(ssButs, BorderLayout.SOUTH);

    JPanel p3 = new JPanel();
    p3.setBorder(BorderFactory.createTitledBorder("Auto-WEKA output"));
    p3.setLayout(new BorderLayout());
    final JScrollPane js = new JScrollPane(m_OutText);
    p3.add(js, BorderLayout.CENTER);
    js.getViewport().addChangeListener(new ChangeListener() {
      private int lastHeight;
      public void stateChanged(ChangeEvent e) {
    JViewport vp = (JViewport)e.getSource();
    int h = vp.getViewSize().height;
    if (h != lastHeight) { // i.e. an addition not just a user scrolling
      lastHeight = h;
      int x = h - vp.getExtentSize().height;
      vp.setViewPosition(new Point(0, x));
    }
      }
    });

    JPanel mondo = new JPanel();
    GridBagLayout gbL = new GridBagLayout();
    mondo.setLayout(gbL);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.NORTH;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 1;     gbC.gridx = 0;
    gbL.setConstraints(buttons, gbC);
    mondo.add(buttons);
    gbC = new GridBagConstraints();
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 2;     gbC.gridx = 0; gbC.weightx = 0;
    gbL.setConstraints(m_History, gbC);
    mondo.add(m_History);
    gbC = new GridBagConstraints();
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 0;     gbC.gridx = 1;
    gbC.gridheight = 3;
    gbC.weightx = 100; gbC.weighty = 100;
    gbL.setConstraints(p3, gbC);
    mondo.add(p3);

    JPanel top = new JPanel();
    top.setLayout(new BorderLayout());
    top.add(m_CEPanel, BorderLayout.NORTH);
    top.add(m_ManualBut, BorderLayout.SOUTH);

    setLayout(new BorderLayout());
    add(top, BorderLayout.NORTH);
    add(mondo, BorderLayout.CENTER);
    Dimension pref = m_CEPanel.getPreferredSize();
    pref.height = pref.height + 5;
    m_CEPanel.setPreferredSize(pref);
  }

  /**
   * Sets the Logger to receive informational messages.
   *
   * @param newLog  the Logger that will now get info messages
   */
  public void setLog(Logger newLog) {
    m_Log = newLog;
  }

  /**
   * Tells the panel to use a new set of instances.
   *
   * @param inst    a set of Instances
   */
  public void setInstances(Instances inst) {
    m_Instances = inst;

    String[] attribNames = new String [m_Instances.numAttributes()];
    for (int i = 0; i < attribNames.length; i++) {
      String type = "";
      switch (m_Instances.attribute(i).type()) {
      case Attribute.NOMINAL:
    type = "(Nom) ";
    break;
      case Attribute.NUMERIC:
    type = "(Num) ";
    break;
      case Attribute.STRING:
    type = "(Str) ";
    break;
      case Attribute.DATE:
    type = "(Dat) ";
    break;
      case Attribute.RELATIONAL:
    type = "(Rel) ";
    break;
      default:
    type = "(???) ";
      }
      attribNames[i] = type + m_Instances.attribute(i).name();
    }
    m_ClassCombo.setModel(new DefaultComboBoxModel(attribNames));
    if (attribNames.length > 0) {
      if (inst.classIndex() == -1)
    m_ClassCombo.setSelectedIndex(attribNames.length - 1);
      else
    m_ClassCombo.setSelectedIndex(inst.classIndex());
      m_ClassCombo.setEnabled(true);
      m_StartBut.setEnabled(m_RunThread == null);
      m_StopBut.setEnabled(m_RunThread != null);
    }
    else {
      m_StartBut.setEnabled(false);
      m_StopBut.setEnabled(false);
    }
  }

  /**
   * Handles constructing a popup menu with visualization options.
   *
   * @param name    the name of the result history list entry clicked on by
   *            the user
   * @param x       the x coordinate for popping up the menu
   * @param y       the y coordinate for popping up the menu
   */
  protected void showPopup(String name, int x, int y) {
    final String selectedName = name;
    JPopupMenu resultListMenu = new JPopupMenu();

    JMenuItem viewMainBuffer = new JMenuItem("View in main window");
    if (selectedName != null) {
      viewMainBuffer.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      m_History.setSingle(selectedName);
    }
      });
    }
    else {
      viewMainBuffer.setEnabled(false);
    }
    resultListMenu.add(viewMainBuffer);

    JMenuItem viewSepBuffer = new JMenuItem("View in separate window");
    if (selectedName != null) {
      viewSepBuffer.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      m_History.openFrame(selectedName);
    }
      });
    }
    else {
      viewSepBuffer.setEnabled(false);
    }
    resultListMenu.add(viewSepBuffer);

    JMenuItem saveOutput = new JMenuItem("Save result buffer");
    if (selectedName != null) {
      saveOutput.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      saveBuffer(selectedName);
    }
      });
    }
    else {
      saveOutput.setEnabled(false);
    }
    resultListMenu.add(saveOutput);

    JMenuItem deleteOutput = new JMenuItem("Delete result buffer");
    if (selectedName != null) {
      deleteOutput.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      m_History.removeResult(selectedName);
    }
      });
    }
    else {
      deleteOutput.setEnabled(false);
    }
    resultListMenu.add(deleteOutput);

    final Classifier classifier = aw;
    final Instances trainHeader = m_Instances;

    JMenuItem saveModel = new JMenuItem("Save model");
    if(selectedName != null) {
      saveModel.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          saveClassifier("Auto-WEKA", classifier, trainHeader);
        }
      });
    } else {
      saveModel.setEnabled(false);
    }
    resultListMenu.add(saveModel);

    resultListMenu.show(m_History.getList(), x, y);
  }

  /**
   * Starts Auto-WEKA. This is run in a separate thread, and will
   * only start if there is no Auto-WEKA thread already running. The
   * output is sent to the results history panel.
   */
  protected void startAutoWEKA() {
    if (m_RunThread == null) {
      synchronized (this) {
        m_StartBut.setEnabled(false);
        m_StopBut.setEnabled(true);
      }

      m_RunThread = new Thread() {
    public void run() {
      // set up everything:
      m_Log.statusMessage("Setting up...");

      try {
        m_Log.logMessage("Started Auto-WEKA for " + m_Instances.relationName());
        if (m_Log instanceof TaskLogger)
          ((TaskLogger)m_Log).taskStarted();

        // running the experiment
        m_Log.statusMessage("Auto-WEKA started...");
        String name = "Auto-WEKA: " + m_Instances.relationName();
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        name = df.format(new Date()) + " - " + name;
        aw = (AutoWEKAClassifier) AbstractClassifier.makeCopy((AutoWEKAClassifier)(m_ClassifierEditor.getValue()));
        m_Instances.setClassIndex(m_ClassCombo.getSelectedIndex());
        aw.setLog(m_Log);
        aw.buildClassifier(m_Instances);

        // pad and assemble values
        StringBuffer outBuff = new StringBuffer();
        outBuff.append("Auto-WEKA result:\n");
        outBuff.append(aw.toString());

        m_History.addResult(name, outBuff);
        m_History.setSingle(name);
        m_Log.logMessage("Auto-WEKA finished for " + m_Instances.relationName());
        m_Log.statusMessage("OK");
      }
      catch (Exception ex) {
        ex.printStackTrace();
        m_Log.logMessage(ex.getMessage());
        JOptionPane.showMessageDialog(
          AutoWEKAPanel.this,
          "Problem running Auto-WEKA:\n" + ex.getMessage(),
          "Running Auto-WEKA",
          JOptionPane.ERROR_MESSAGE);
        m_Log.statusMessage("Problem running Auto-WEKA!");
      }
      finally {
        synchronized (this) {
          m_StartBut.setEnabled(true);
          m_StopBut.setEnabled(false);
          m_RunThread = null;
        }

        if (m_Log instanceof TaskLogger)
              ((TaskLogger)m_Log).taskFinished();
      }
    }
      };
      m_RunThread.setPriority(Thread.MIN_PRIORITY);
      m_RunThread.start();
    }
  }

  /**
   * Save the currently selected experiment output to a file.
   *
   * @param name    the name of the buffer to save
   */
  protected void saveBuffer(String name) {
    StringBuffer sb = m_History.getNamedBuffer(name);
    if (sb != null) {
      if (m_SaveOut.save(sb))
    m_Log.logMessage("Save successful.");
    }
  }

  /**
   * Stops the currently running Auto-WEKA (if any).
   */
  protected void stopAutoWEKA() {
    if (m_RunThread != null) {
      m_RunThread.interrupt();
    }
  }

  /**
   * Sets the Explorer to use as parent frame (used for sending notifications
   * about changes in the data).
   *
   * @param parent  the parent frame
   */
  public void setExplorer(Explorer parent) {
    m_Explorer = parent;
  }

  /**
   * returns the parent Explorer frame.
   *
   * @return        the parent
   */
  public Explorer getExplorer() {
    return m_Explorer;
  }

  /**
   * Returns the title for the tab in the Explorer.
   *
   * @return        the title of this tab
   */
  public String getTabTitle() {
    return "Auto-WEKA";
  }

  /**
   * Returns the tooltip for the tab in the Explorer.
   *
   * @return        the tooltip of this tab
   */
  public String getTabTitleToolTip() {
    return "Run Auto-WEKA";
  }

  /**
   * Tests out the Auto-WEKA panel from the command line.
   *
   * @param args    may optionally contain the name of a dataset to load.
   */
  public static void main(String[] args) {
    try {
      final javax.swing.JFrame jf = new javax.swing.JFrame("Auto-WEKA");
      jf.getContentPane().setLayout(new BorderLayout());
      final AutoWEKAPanel sp = new AutoWEKAPanel();
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      weka.gui.LogPanel lp = new weka.gui.LogPanel();
      sp.setLog(lp);
      jf.getContentPane().add(lp, BorderLayout.SOUTH);
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
    Reader r = new java.io.BufferedReader(new java.io.FileReader(args[0]));
    Instances i = new Instances(r);
    sp.setInstances(i);
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
