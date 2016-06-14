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
 *    ClassifierPanel.java
 *    Copyright (C) 1999-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.explorer;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.Sourcable;
import weka.classifiers.evaluation.CostCurve;
import weka.classifiers.evaluation.MarginCurve;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.evaluation.output.prediction.AbstractOutput;
import weka.classifiers.evaluation.output.prediction.Null;
import weka.classifiers.pmml.consumer.PMMLClassifier;
import weka.classifiers.rules.ZeroR;
import weka.core.Attribute;
import weka.core.BatchPredictor;
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.Defaults;
import weka.core.Drawable;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.SerializedObject;
import weka.core.Settings;
import weka.core.Utils;
import weka.core.Version;
import weka.core.converters.ArffLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.converters.IncrementalConverter;
import weka.core.converters.Loader;
import weka.core.pmml.PMMLFactory;
import weka.core.pmml.PMMLModel;
import weka.gui.AbstractPerspective;
import weka.gui.CostMatrixEditor;
import weka.gui.EvaluationMetricSelectionDialog;
import weka.gui.ExtensionFileFilter;
import weka.gui.GenericObjectEditor;
import weka.gui.Logger;
import weka.gui.Perspective;
import weka.gui.PerspectiveInfo;
import weka.gui.PropertyDialog;
import weka.gui.PropertyPanel;
import weka.gui.ResultHistoryPanel;
import weka.gui.SaveBuffer;
import weka.gui.SetInstancesPanel;
import weka.gui.SysErrLog;
import weka.gui.TaskLogger;
import weka.gui.beans.CostBenefitAnalysis;
import weka.gui.explorer.Explorer.CapabilitiesFilterChangeEvent;
import weka.gui.explorer.Explorer.CapabilitiesFilterChangeListener;
import weka.gui.explorer.Explorer.ExplorerPanel;
import weka.gui.explorer.Explorer.LogHandler;
import weka.gui.graphvisualizer.BIFFormatException;
import weka.gui.graphvisualizer.GraphVisualizer;
import weka.gui.treevisualizer.PlaceNode2;
import weka.gui.treevisualizer.TreeVisualizer;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;
import weka.gui.visualize.VisualizePanel;
import weka.gui.visualize.plugins.ErrorVisualizePlugin;
import weka.gui.visualize.plugins.GraphVisualizePlugin;
import weka.gui.visualize.plugins.TreeVisualizePlugin;
import weka.gui.visualize.plugins.VisualizePlugin;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * This panel allows the user to select and configure a classifier, set the
 * attribute of the current dataset to be used as the class, and evaluate the
 * classifier using a number of testing modes (test on the training data,
 * train/test on a percentage split, n-fold cross-validation, test on a separate
 * split). The results of classification runs are stored in a result history so
 * that previous results are accessible.
 * 
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 12720 $
 */
@PerspectiveInfo(ID = "weka.gui.explorer.classifierpanel", title = "Classify",
  toolTipText = "Classify instances",
  iconPath = "weka/gui/weka_icon_new_small.png")
public class ClassifierPanel extends AbstractPerspective implements
  CapabilitiesFilterChangeListener, ExplorerPanel, LogHandler {

  /** for serialization. */
  static final long serialVersionUID = 6959973704963624003L;

  /** the parent frame. */
  protected Explorer m_Explorer = null;

  /** The filename extension that should be used for model files. */
  public static String MODEL_FILE_EXTENSION = ".model";

  /** The filename extension that should be used for PMML xml files. */
  public static String PMML_FILE_EXTENSION = ".xml";

  /** Lets the user configure the classifier. */
  protected GenericObjectEditor m_ClassifierEditor = new GenericObjectEditor();

  /** The panel showing the current classifier selection. */
  protected PropertyPanel m_CEPanel = new PropertyPanel(m_ClassifierEditor);

  /** The output area for classification results. */
  protected JTextArea m_OutText = new JTextArea(20, 40);

  /** The destination for log/status messages. */
  protected Logger m_Log = new SysErrLog();

  /** The buffer saving object for saving output. */
  SaveBuffer m_SaveOut = new SaveBuffer(m_Log, this);

  /** A panel controlling results viewing. */
  protected ResultHistoryPanel m_History = new ResultHistoryPanel(m_OutText);

  /** Lets the user select the class column. */
  protected JComboBox m_ClassCombo = new JComboBox();

  /** Click to set test mode to cross-validation. */
  protected JRadioButton m_CVBut = new JRadioButton("Cross-validation");

  /** Click to set test mode to generate a % split. */
  protected JRadioButton m_PercentBut = new JRadioButton("Percentage split");

  /** Click to set test mode to test on training data. */
  protected JRadioButton m_TrainBut = new JRadioButton("Use training set");

  /** Click to set test mode to a user-specified test set. */
  protected JRadioButton m_TestSplitBut = new JRadioButton("Supplied test set");

  /**
   * Check to save the predictions in the results list for visualizing later on.
   */
  protected JCheckBox m_StorePredictionsBut = new JCheckBox(
    "Store predictions for visualization");

  /**
   * Check to have the point size in error plots proportional to the prediction
   * margin (classification only)
   */
  protected JCheckBox m_errorPlotPointSizeProportionalToMargin = new JCheckBox(
    "Error plot point size proportional to margin");

  /** Check to output the model built from the training data. */
  protected JCheckBox m_OutputModelBut = new JCheckBox("Output model");

  /** Check to output true/false positives, precision/recall for each class. */
  protected JCheckBox m_OutputPerClassBut = new JCheckBox(
    "Output per-class stats");

  /** Check to output a confusion matrix. */
  protected JCheckBox m_OutputConfusionBut = new JCheckBox(
    "Output confusion matrix");

  /** Check to output entropy statistics. */
  protected JCheckBox m_OutputEntropyBut = new JCheckBox(
    "Output entropy evaluation measures");

  /** Lets the user configure the ClassificationOutput. */
  protected GenericObjectEditor m_ClassificationOutputEditor =
    new GenericObjectEditor(true);

  /** ClassificationOutput configuration. */
  protected PropertyPanel m_ClassificationOutputPanel = new PropertyPanel(
    m_ClassificationOutputEditor);

  /** the range of attributes to output. */
  protected Range m_OutputAdditionalAttributesRange = null;

  /** Check to evaluate w.r.t a cost matrix. */
  protected JCheckBox m_EvalWRTCostsBut = new JCheckBox(
    "Cost-sensitive evaluation");

  /** for the cost matrix. */
  protected JButton m_SetCostsBut = new JButton("Set...");

  /** Label by where the cv folds are entered. */
  protected JLabel m_CVLab = new JLabel("Folds", SwingConstants.RIGHT);

  /** The field where the cv folds are entered. */
  protected JTextField m_CVText = new JTextField("10", 3);

  /** Label by where the % split is entered. */
  protected JLabel m_PercentLab = new JLabel("%", SwingConstants.RIGHT);

  /** The field where the % split is entered. */
  protected JTextField m_PercentText = new JTextField("66", 3);

  /** The button used to open a separate test dataset. */
  protected JButton m_SetTestBut = new JButton("Set...");

  /** The frame used to show the test set selection panel. */
  protected JFrame m_SetTestFrame;

  /** The frame used to show the cost matrix editing panel. */
  protected PropertyDialog m_SetCostsFrame;

  /**
   * Alters the enabled/disabled status of elements associated with each radio
   * button.
   */
  ActionListener m_RadioListener = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      updateRadioLinks();
    }
  };

  /** Button for further output/visualize options. */
  JButton m_MoreOptions = new JButton("More options...");

  /** User specified random seed for cross validation or % split. */
  protected JTextField m_RandomSeedText = new JTextField("1", 3);

  /** the label for the random seed textfield. */
  protected JLabel m_RandomLab = new JLabel("Random seed for XVal / % Split",
    SwingConstants.RIGHT);

  /** Whether randomization is turned off to preserve order. */
  protected JCheckBox m_PreserveOrderBut = new JCheckBox(
    "Preserve order for % Split");

  /**
   * Whether to output the source code (only for classifiers importing
   * Sourcable).
   */
  protected JCheckBox m_OutputSourceCode = new JCheckBox("Output source code");

  /** The name of the generated class (only applicable to Sourcable schemes). */
  protected JTextField m_SourceCodeClass = new JTextField("WekaClassifier", 10);

  /** Click to start running the classifier. */
  protected JButton m_StartBut = new JButton("Start");

  /** Click to stop a running classifier. */
  protected JButton m_StopBut = new JButton("Stop");

  /** Stop the class combo from taking up to much space. */
  private final Dimension COMBO_SIZE = new Dimension(150,
    m_StartBut.getPreferredSize().height);

  /** The cost matrix editor for evaluation costs. */
  protected CostMatrixEditor m_CostMatrixEditor = new CostMatrixEditor();

  /** The main set of instances we're playing with. */
  protected Instances m_Instances;

  /** The loader used to load the user-supplied test set (if any). */
  protected Loader m_TestLoader;

  /** the class index for the supplied test set. */
  protected int m_TestClassIndex = -1;

  /** A thread that classification runs in. */
  protected Thread m_RunThread;

  /** The current visualization object. */
  protected VisualizePanel m_CurrentVis = null;

  /** Filter to ensure only model files are selected. */
  protected FileFilter m_ModelFilter = new ExtensionFileFilter(
    MODEL_FILE_EXTENSION, "Model object files");

  protected FileFilter m_PMMLModelFilter = new ExtensionFileFilter(
    PMML_FILE_EXTENSION, "PMML model files");

  /** The file chooser for selecting model files. */
  protected JFileChooser m_FileChooser = new JFileChooser(new File(
    System.getProperty("user.dir")));

  /** The user's list of selected evaluation metrics */
  protected List<String> m_selectedEvalMetrics = Evaluation
    .getAllEvaluationMetricNames();

  /**
   * Whether start-up settings have been applied (i.e. initial classifier to
   * use)
   */
  protected boolean m_initialSettingsSet;

  /* Register the property editors we need */
  static {
    GenericObjectEditor.registerEditors();
  }

  /**
   * Creates the classifier panel.
   */
  public ClassifierPanel() {
    m_selectedEvalMetrics.remove("Coverage");
    m_selectedEvalMetrics.remove("Region size");

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
    m_ClassifierEditor.setClassType(Classifier.class);
    m_ClassifierEditor.setValue(ExplorerDefaults.getClassifier());
    m_ClassifierEditor.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        m_StartBut.setEnabled(true);
        // Check capabilities
        Capabilities currentFilter = m_ClassifierEditor.getCapabilitiesFilter();
        Classifier classifier = (Classifier) m_ClassifierEditor.getValue();
        Capabilities currentSchemeCapabilities = null;
        if (classifier != null && currentFilter != null
          && (classifier instanceof CapabilitiesHandler)) {
          currentSchemeCapabilities =
            ((CapabilitiesHandler) classifier).getCapabilities();

          if (!currentSchemeCapabilities.supportsMaybe(currentFilter)
            && !currentSchemeCapabilities.supports(currentFilter)) {
            m_StartBut.setEnabled(false);
          }
        }
        repaint();
      }
    });

    m_ClassCombo.setToolTipText("Select the attribute to use as the class");
    m_TrainBut.setToolTipText("Test on the same set that the classifier"
      + " is trained on");
    m_CVBut.setToolTipText("Perform a n-fold cross-validation");
    m_PercentBut.setToolTipText("Train on a percentage of the data and"
      + " test on the remainder");
    m_TestSplitBut.setToolTipText("Test on a user-specified dataset");
    m_StartBut.setToolTipText("Starts the classification");
    m_StopBut.setToolTipText("Stops a running classification");
    m_StorePredictionsBut
      .setToolTipText("Store predictions in the result list for later "
        + "visualization");
    m_errorPlotPointSizeProportionalToMargin
      .setToolTipText("In classifier errors plots the point size will be "
        + "set proportional to the absolute value of the "
        + "prediction margin (affects classification only)");
    m_OutputModelBut
      .setToolTipText("Output the model obtained from the full training set");
    m_OutputPerClassBut.setToolTipText("Output precision/recall & true/false"
      + " positives for each class");
    m_OutputConfusionBut
      .setToolTipText("Output the matrix displaying class confusions");
    m_OutputEntropyBut
      .setToolTipText("Output entropy-based evaluation measures");
    m_EvalWRTCostsBut
      .setToolTipText("Evaluate errors with respect to a cost matrix");
    m_RandomLab.setToolTipText("The seed value for randomization");
    m_RandomSeedText.setToolTipText(m_RandomLab.getToolTipText());
    m_PreserveOrderBut
      .setToolTipText("Preserves the order in a percentage split");
    m_OutputSourceCode
      .setToolTipText("Whether to output the built classifier as Java source code");
    m_SourceCodeClass.setToolTipText("The classname of the built classifier");

    m_FileChooser.addChoosableFileFilter(m_PMMLModelFilter);
    m_FileChooser.setFileFilter(m_ModelFilter);

    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    m_ClassificationOutputEditor.setClassType(AbstractOutput.class);
    m_ClassificationOutputEditor.setValue(new Null());

    m_StorePredictionsBut.setSelected(ExplorerDefaults
      .getClassifierStorePredictionsForVis());
    m_OutputModelBut.setSelected(ExplorerDefaults.getClassifierOutputModel());
    m_OutputPerClassBut.setSelected(ExplorerDefaults
      .getClassifierOutputPerClassStats());
    m_OutputConfusionBut.setSelected(ExplorerDefaults
      .getClassifierOutputConfusionMatrix());
    m_EvalWRTCostsBut.setSelected(ExplorerDefaults
      .getClassifierCostSensitiveEval());
    m_OutputEntropyBut.setSelected(ExplorerDefaults
      .getClassifierOutputEntropyEvalMeasures());
    m_RandomSeedText.setText("" + ExplorerDefaults.getClassifierRandomSeed());
    m_PreserveOrderBut.setSelected(ExplorerDefaults
      .getClassifierPreserveOrder());
    m_OutputSourceCode.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_SourceCodeClass.setEnabled(m_OutputSourceCode.isSelected());
      }
    });
    m_OutputSourceCode.setSelected(ExplorerDefaults
      .getClassifierOutputSourceCode());
    m_SourceCodeClass.setText(ExplorerDefaults.getClassifierSourceCodeClass());
    m_SourceCodeClass.setEnabled(m_OutputSourceCode.isSelected());
    m_ClassCombo.setEnabled(false);
    m_ClassCombo.setPreferredSize(COMBO_SIZE);
    m_ClassCombo.setMaximumSize(COMBO_SIZE);
    m_ClassCombo.setMinimumSize(COMBO_SIZE);

    m_CVBut.setSelected(true);
    // see "testMode" variable in startClassifier
    m_CVBut.setSelected(ExplorerDefaults.getClassifierTestMode() == 1);
    m_PercentBut.setSelected(ExplorerDefaults.getClassifierTestMode() == 2);
    m_TrainBut.setSelected(ExplorerDefaults.getClassifierTestMode() == 3);
    m_TestSplitBut.setSelected(ExplorerDefaults.getClassifierTestMode() == 4);
    m_PercentText.setText("" + ExplorerDefaults.getClassifierPercentageSplit());
    m_CVText.setText("" + ExplorerDefaults.getClassifierCrossvalidationFolds());
    updateRadioLinks();
    ButtonGroup bg = new ButtonGroup();
    bg.add(m_TrainBut);
    bg.add(m_CVBut);
    bg.add(m_PercentBut);
    bg.add(m_TestSplitBut);
    m_TrainBut.addActionListener(m_RadioListener);
    m_CVBut.addActionListener(m_RadioListener);
    m_PercentBut.addActionListener(m_RadioListener);
    m_TestSplitBut.addActionListener(m_RadioListener);
    m_SetTestBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setTestSet();
      }
    });
    m_EvalWRTCostsBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_SetCostsBut.setEnabled(m_EvalWRTCostsBut.isSelected());
        if ((m_SetCostsFrame != null) && (!m_EvalWRTCostsBut.isSelected())) {
          m_SetCostsFrame.setVisible(false);
        }
      }
    });
    m_CostMatrixEditor.setValue(new CostMatrix(1));
    m_SetCostsBut.setEnabled(m_EvalWRTCostsBut.isSelected());
    m_SetCostsBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_SetCostsBut.setEnabled(false);
        if (m_SetCostsFrame == null) {
          if (PropertyDialog.getParentDialog(ClassifierPanel.this) != null) {
            m_SetCostsFrame =
              new PropertyDialog(PropertyDialog
                .getParentDialog(ClassifierPanel.this), m_CostMatrixEditor,
                100, 100);
          } else {
            m_SetCostsFrame =
              new PropertyDialog(PropertyDialog
                .getParentFrame(ClassifierPanel.this), m_CostMatrixEditor, 100,
                100);
          }
          m_SetCostsFrame.setTitle("Cost Matrix Editor");
          // pd.setSize(250,150);
          m_SetCostsFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent p) {
              m_SetCostsBut.setEnabled(m_EvalWRTCostsBut.isSelected());
              if ((m_SetCostsFrame != null)
                && (!m_EvalWRTCostsBut.isSelected())) {
                m_SetCostsFrame.setVisible(false);
              }
            }
          });
          m_SetCostsFrame.setVisible(true);
        }

        // do we need to change the size of the matrix?
        int classIndex = m_ClassCombo.getSelectedIndex();
        int numClasses = m_Instances.attribute(classIndex).numValues();
        if (numClasses != ((CostMatrix) m_CostMatrixEditor.getValue())
          .numColumns()) {
          m_CostMatrixEditor.setValue(new CostMatrix(numClasses));
        }

        m_SetCostsFrame.setVisible(true);
      }
    });

    m_StartBut.setEnabled(false);
    m_StopBut.setEnabled(false);
    m_StartBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean proceed = true;
        if (Explorer.m_Memory.memoryIsLow()) {
          proceed = Explorer.m_Memory.showMemoryIsLow();
        }
        if (proceed) {
          startClassifier();
        }
      }
    });
    m_StopBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        stopClassifier();
      }
    });

    m_ClassCombo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int selected = m_ClassCombo.getSelectedIndex();
        if (selected != -1) {
          boolean isNominal = m_Instances.attribute(selected).isNominal();
          m_OutputPerClassBut.setEnabled(isNominal);
          m_OutputConfusionBut.setEnabled(isNominal);
        }
        updateCapabilitiesFilter(m_ClassifierEditor.getCapabilitiesFilter());
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
            visualize(name, e.getX(), e.getY());
          } else {
            visualize(null, e.getX(), e.getY());
          }
        }
      }
    });

    m_MoreOptions.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_MoreOptions.setEnabled(false);
        JPanel moreOptionsPanel = new JPanel();
        moreOptionsPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        moreOptionsPanel.setLayout(new GridLayout(0, 1));
        moreOptionsPanel.add(m_OutputModelBut);
        moreOptionsPanel.add(m_OutputPerClassBut);
        moreOptionsPanel.add(m_OutputEntropyBut);
        moreOptionsPanel.add(m_OutputConfusionBut);
        moreOptionsPanel.add(m_StorePredictionsBut);
        moreOptionsPanel.add(m_errorPlotPointSizeProportionalToMargin);
        JPanel classOutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        classOutPanel.add(new JLabel("Output predictions"));
        classOutPanel.add(m_ClassificationOutputPanel);
        moreOptionsPanel.add(classOutPanel);
        JPanel costMatrixOption = new JPanel(new FlowLayout(FlowLayout.LEFT));
        costMatrixOption.add(m_EvalWRTCostsBut);
        costMatrixOption.add(m_SetCostsBut);
        moreOptionsPanel.add(costMatrixOption);
        JPanel seedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        seedPanel.add(m_RandomLab);
        seedPanel.add(m_RandomSeedText);
        moreOptionsPanel.add(seedPanel);
        moreOptionsPanel.add(m_PreserveOrderBut);
        JPanel sourcePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        m_OutputSourceCode.setEnabled(m_ClassifierEditor.getValue() instanceof Sourcable);
        m_SourceCodeClass.setEnabled(m_OutputSourceCode.isEnabled()
          && m_OutputSourceCode.isSelected());
        sourcePanel.add(m_OutputSourceCode);
        sourcePanel.add(m_SourceCodeClass);
        moreOptionsPanel.add(sourcePanel);

        JPanel all = new JPanel();
        all.setLayout(new BorderLayout());

        JButton oK = new JButton("OK");
        JPanel okP = new JPanel();
        okP.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        okP.setLayout(new GridLayout(1, 1, 5, 5));
        okP.add(oK);

        all.add(moreOptionsPanel, BorderLayout.CENTER);
        all.add(okP, BorderLayout.SOUTH);

        final JDialog jd =
          new JDialog(PropertyDialog.getParentFrame(ClassifierPanel.this),
            "Classifier evaluation options");
        jd.getContentPane().setLayout(new BorderLayout());
        jd.getContentPane().add(all, BorderLayout.CENTER);
        jd.addWindowListener(new java.awt.event.WindowAdapter() {
          @Override
          public void windowClosing(java.awt.event.WindowEvent w) {
            jd.dispose();
            m_MoreOptions.setEnabled(true);
          }
        });
        oK.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent a) {
            m_MoreOptions.setEnabled(true);
            jd.dispose();
          }
        });
        jd.pack();

        // panel height is only available now
        m_ClassificationOutputPanel.setPreferredSize(new Dimension(300,
          m_ClassificationOutputPanel.getHeight()));
        jd.pack();

        // final List<AbstractEvaluationMetric> pluginMetrics =
        // AbstractEvaluationMetric
        // .getPluginMetrics();

        final JButton editEvalMetrics = new JButton("Evaluation metrics...");
        JPanel evalP = new JPanel();
        evalP.setLayout(new BorderLayout());
        evalP.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        evalP.add(editEvalMetrics, BorderLayout.CENTER);
        editEvalMetrics
          .setToolTipText("Enable/disable output of specific evaluation metrics");
        moreOptionsPanel.add(evalP);

        editEvalMetrics.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            EvaluationMetricSelectionDialog esd =
              new EvaluationMetricSelectionDialog(jd, m_selectedEvalMetrics);
            esd.setLocation(m_MoreOptions.getLocationOnScreen());
            esd.pack();
            esd.setVisible(true);
            m_selectedEvalMetrics = esd.getSelectedEvalMetrics();
          }
        });

        jd.setLocation(m_MoreOptions.getLocationOnScreen());
        jd.setVisible(true);
      }
    });

    // Layout the GUI
    JPanel p1 = new JPanel();
    p1.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Classifier"),
      BorderFactory.createEmptyBorder(0, 5, 5, 5)));
    p1.setLayout(new BorderLayout());
    p1.add(m_CEPanel, BorderLayout.NORTH);

    JPanel p2 = new JPanel();
    GridBagLayout gbL = new GridBagLayout();
    p2.setLayout(gbL);
    p2.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Test options"),
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
    gbL.setConstraints(m_CVBut, gbC);
    p2.add(m_CVBut);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 2;
    gbC.gridx = 1;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(m_CVLab, gbC);
    p2.add(m_CVLab);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 2;
    gbC.gridx = 2;
    gbC.weightx = 100;
    gbC.ipadx = 20;
    gbL.setConstraints(m_CVText, gbC);
    p2.add(m_CVText);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.gridy = 3;
    gbC.gridx = 0;
    gbL.setConstraints(m_PercentBut, gbC);
    p2.add(m_PercentBut);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 3;
    gbC.gridx = 1;
    gbC.insets = new Insets(2, 10, 2, 10);
    gbL.setConstraints(m_PercentLab, gbC);
    p2.add(m_PercentLab);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 3;
    gbC.gridx = 2;
    gbC.weightx = 100;
    gbC.ipadx = 20;
    gbL.setConstraints(m_PercentText, gbC);
    p2.add(m_PercentText);

    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 4;
    gbC.gridx = 0;
    gbC.weightx = 100;
    gbC.gridwidth = 3;

    gbC.insets = new Insets(3, 0, 1, 0);
    gbL.setConstraints(m_MoreOptions, gbC);
    p2.add(m_MoreOptions);

    // Any launcher plugins?
    Vector<String> pluginsVector =
      GenericObjectEditor
        .getClassnames(ClassifierPanelLaunchHandlerPlugin.class.getName());
    JButton pluginBut = null;
    if (pluginsVector.size() == 1) {
      try {
        // Display as a single button
        String className = pluginsVector.elementAt(0);
        final ClassifierPanelLaunchHandlerPlugin plugin =
          (ClassifierPanelLaunchHandlerPlugin) Class.forName(className)
            .newInstance();
        if (plugin != null) {
          plugin.setClassifierPanel(this);
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
      // make a popu menu
      int okPluginCount = 0;
      final java.awt.PopupMenu pluginPopup = new java.awt.PopupMenu();

      for (int i = 0; i < pluginsVector.size(); i++) {
        String className = (pluginsVector.elementAt(i));
        try {
          final ClassifierPanelLaunchHandlerPlugin plugin =
            (ClassifierPanelLaunchHandlerPlugin) Class.forName(className)
              .newInstance();

          if (plugin == null) {
            continue;
          }
          okPluginCount++;
          plugin.setClassifierPanel(this);
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
    buttons.setLayout(new GridLayout(2, 2));
    buttons.add(m_ClassCombo);
    m_ClassCombo.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
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

    buttons.add(ssButs);

    JPanel p3 = new JPanel();
    p3.setBorder(BorderFactory.createTitledBorder("Classifier output"));
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
    m_CVText.setEnabled(m_CVBut.isSelected());
    m_CVLab.setEnabled(m_CVBut.isSelected());
    m_PercentText.setEnabled(m_PercentBut.isSelected());
    m_PercentLab.setEnabled(m_PercentBut.isSelected());
  }

  /**
   * Sets the Logger to receive informational messages.
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

    String[] attribNames = new String[m_Instances.numAttributes()];
    for (int i = 0; i < attribNames.length; i++) {
      String type =
        "(" + Attribute.typeToStringShort(m_Instances.attribute(i)) + ") ";
      attribNames[i] = type + m_Instances.attribute(i).name();
    }
    m_ClassCombo.setModel(new DefaultComboBoxModel(attribNames));
    if (attribNames.length > 0) {
      if (inst.classIndex() == -1) {
        m_ClassCombo.setSelectedIndex(attribNames.length - 1);
      } else {
        m_ClassCombo.setSelectedIndex(inst.classIndex());
      }
      m_ClassCombo.setEnabled(true);
      m_StartBut.setEnabled(m_RunThread == null);
      m_StopBut.setEnabled(m_RunThread != null);
    } else {
      m_StartBut.setEnabled(false);
      m_StopBut.setEnabled(false);
    }
  }

  /**
   * Sets the user test set. Information about the current test set is displayed
   * in an InstanceSummaryPanel and the user is given the ability to load
   * another set from a file or url.
   * 
   */
  protected void setTestSet() {

    if (m_SetTestFrame == null) {
      PreprocessPanel preprocessPanel = null;
      if (m_Explorer != null) {
        preprocessPanel = m_Explorer.getPreprocessPanel();
      } else if (getMainApplication() != null) {
        Perspective p =
          getMainApplication().getPerspectiveManager().getPerspective(
            PreprocessPanel.PreprocessDefaults.ID);
        preprocessPanel = (PreprocessPanel) p;
      } else {
        throw new IllegalStateException("We don't have access to a "
          + "PreprocessPanel!");
      }

      final SetInstancesPanel sp =
        new SetInstancesPanel(true, true, preprocessPanel.m_FileChooser);

      if (m_TestLoader != null) {
        try {
          if (m_TestLoader.getStructure() != null) {
            sp.setInstances(m_TestLoader.getStructure());
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
      sp.addPropertyChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent e) {
          m_TestLoader = sp.getLoader();
          m_TestClassIndex = sp.getClassIndex();
        }
      });
      // Add propertychangelistener to update m_TestLoader whenever
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
   * outputs the header for the predictions on the data.
   * 
   * @param outBuff the buffer to add the output to
   * @param classificationOutput for generating the classification output
   * @param title the title to print
   */
  protected void printPredictionsHeader(StringBuffer outBuff,
    AbstractOutput classificationOutput, String title) {
    if (classificationOutput.generatesOutput()) {
      outBuff.append("=== Predictions on " + title + " ===\n\n");
    }
    classificationOutput.printHeader();
  }

  protected static Evaluation setupEval(Evaluation eval, Classifier classifier,
    Instances inst, CostMatrix costMatrix,
    ClassifierErrorsPlotInstances plotInstances,
    AbstractOutput classificationOutput, boolean onlySetPriors)
    throws Exception {

    if (classifier instanceof weka.classifiers.misc.InputMappedClassifier) {
      Instances mappedClassifierHeader =
        ((weka.classifiers.misc.InputMappedClassifier) classifier)
          .getModelHeader(new Instances(inst, 0));

      if (classificationOutput != null) {
        classificationOutput.setHeader(mappedClassifierHeader);
      }

      if (!onlySetPriors) {
        if (costMatrix != null) {
          eval =
            new Evaluation(new Instances(mappedClassifierHeader, 0), costMatrix);
        } else {
          eval = new Evaluation(new Instances(mappedClassifierHeader, 0));
        }
      }

      if (!eval.getHeader().equalHeaders(inst)) {
        // When the InputMappedClassifier is loading a model,
        // we need to make a new dataset that maps the training instances to
        // the structure expected by the mapped classifier - this is only
        // to ensure that the structure and priors computed by
        // evaluation object is correct with respect to the mapped classifier
        Instances mappedClassifierDataset =
          ((weka.classifiers.misc.InputMappedClassifier) classifier)
            .getModelHeader(new Instances(mappedClassifierHeader, 0));
        for (int zz = 0; zz < inst.numInstances(); zz++) {
          Instance mapped =
            ((weka.classifiers.misc.InputMappedClassifier) classifier)
              .constructMappedInstance(inst.instance(zz));
          mappedClassifierDataset.add(mapped);
        }
        eval.setPriors(mappedClassifierDataset);
        if (!onlySetPriors) {
          if (plotInstances != null) {
            plotInstances.setInstances(mappedClassifierDataset);
            plotInstances.setClassifier(classifier);
            /*
             * int mappedClass =
             * ((weka.classifiers.misc.InputMappedClassifier)classifier
             * ).getMappedClassIndex(); System.err.println("Mapped class index "
             * + mappedClass);
             */
            plotInstances.setClassIndex(mappedClassifierDataset.classIndex());
            plotInstances.setEvaluation(eval);
          }
        }
      } else {
        eval.setPriors(inst);
        if (!onlySetPriors) {
          if (plotInstances != null) {
            plotInstances.setInstances(inst);
            plotInstances.setClassifier(classifier);
            plotInstances.setClassIndex(inst.classIndex());
            plotInstances.setEvaluation(eval);
          }
        }
      }
    } else {
      eval.setPriors(inst);
      if (!onlySetPriors) {
        if (plotInstances != null) {
          plotInstances.setInstances(inst);
          plotInstances.setClassifier(classifier);
          plotInstances.setClassIndex(inst.classIndex());
          plotInstances.setEvaluation(eval);
        }
      }
    }

    return eval;
  }

  /**
   * Starts running the currently configured classifier with the current
   * settings. This is run in a separate thread, and will only start if there is
   * no classifier already running. The classifier output is sent to the results
   * history panel.
   */
  protected void startClassifier() {

    if (m_RunThread == null) {
      synchronized (this) {
        m_StartBut.setEnabled(false);
        m_StopBut.setEnabled(true);
      }
      m_RunThread = new Thread() {
        @Override
        public void run() {
          m_CEPanel.addToHistory();

          // Copy the current state of things
          m_Log.statusMessage("Setting up...");
          CostMatrix costMatrix = null;
          Instances inst = new Instances(m_Instances);
          DataSource source = null;
          Instances userTestStructure = null;
          ClassifierErrorsPlotInstances plotInstances = null;

          // for timing
          long trainTimeStart = 0, trainTimeElapsed = 0;
          long testTimeStart = 0, testTimeElapsed = 0;

          try {
            if (m_TestLoader != null && m_TestLoader.getStructure() != null) {
              if (m_ClassifierEditor.getValue() instanceof BatchPredictor
                && ((BatchPredictor) m_ClassifierEditor.getValue())
                  .implementsMoreEfficientBatchPrediction()
                && m_TestLoader instanceof ArffLoader) {
                // we're not really streaming test instances in this case...
                ((ArffLoader) m_TestLoader).setRetainStringVals(true);
              }
              m_TestLoader.reset();
              source = new DataSource(m_TestLoader);
              userTestStructure = source.getStructure();
              userTestStructure.setClassIndex(m_TestClassIndex);
            }
          } catch (Exception ex) {
            ex.printStackTrace();
          }
          if (m_EvalWRTCostsBut.isSelected()) {
            costMatrix =
              new CostMatrix((CostMatrix) m_CostMatrixEditor.getValue());
          }
          boolean outputModel = m_OutputModelBut.isSelected();
          boolean outputConfusion = m_OutputConfusionBut.isSelected();
          boolean outputPerClass = m_OutputPerClassBut.isSelected();
          boolean outputSummary = true;
          boolean outputEntropy = m_OutputEntropyBut.isSelected();
          boolean saveVis = m_StorePredictionsBut.isSelected();
          boolean outputPredictionsText =
            (m_ClassificationOutputEditor.getValue().getClass() != Null.class);

          String grph = null;

          int testMode = 0;
          int numFolds = 10;
          double percent = 66;
          int classIndex = m_ClassCombo.getSelectedIndex();
          inst.setClassIndex(classIndex);
          Classifier classifier = (Classifier) m_ClassifierEditor.getValue();
          Classifier template = null;
          try {
            template = AbstractClassifier.makeCopy(classifier);
          } catch (Exception ex) {
            m_Log.logMessage("Problem copying classifier: " + ex.getMessage());
          }
          Classifier fullClassifier = null;
          StringBuffer outBuff = new StringBuffer();
          AbstractOutput classificationOutput = null;
          if (outputPredictionsText) {
            classificationOutput =
              (AbstractOutput) m_ClassificationOutputEditor.getValue();
            Instances header = new Instances(inst, 0);
            header.setClassIndex(classIndex);
            classificationOutput.setHeader(header);
            classificationOutput.setBuffer(outBuff);
          }
          String name =
            (new SimpleDateFormat("HH:mm:ss - ")).format(new Date());
          String cname = "";
          String cmd = "";
          Evaluation eval = null;
          try {
            if (m_CVBut.isSelected()) {
              testMode = 1;
              numFolds = Integer.parseInt(m_CVText.getText());
              if (numFolds <= 1) {
                throw new Exception("Number of folds must be greater than 1");
              }
            } else if (m_PercentBut.isSelected()) {
              testMode = 2;
              percent = Double.parseDouble(m_PercentText.getText());
              if ((percent <= 0) || (percent >= 100)) {
                throw new Exception("Percentage must be between 0 and 100");
              }
            } else if (m_TrainBut.isSelected()) {
              testMode = 3;
            } else if (m_TestSplitBut.isSelected()) {
              testMode = 4;
              // Check the test instance compatibility
              if (source == null) {
                throw new Exception("No user test set has been specified");
              }

              if (!(classifier instanceof weka.classifiers.misc.InputMappedClassifier)) {
                if (!inst.equalHeaders(userTestStructure)) {
                  boolean wrapClassifier = false;
                  if (!Utils
                    .getDontShowDialog("weka.gui.explorer.ClassifierPanel.AutoWrapInInputMappedClassifier")) {
                    JCheckBox dontShow =
                      new JCheckBox("Do not show this message again");
                    Object[] stuff = new Object[2];
                    stuff[0] =
                      "Train and test set are not compatible.\n"
                        + "Would you like to automatically wrap the classifier in\n"
                        + "an \"InputMappedClassifier\" before proceeding?.\n";
                    stuff[1] = dontShow;

                    int result =
                      JOptionPane.showConfirmDialog(ClassifierPanel.this,
                        stuff, "ClassifierPanel", JOptionPane.YES_OPTION);

                    if (result == JOptionPane.YES_OPTION) {
                      wrapClassifier = true;
                    }

                    if (dontShow.isSelected()) {
                      String response = (wrapClassifier) ? "yes" : "no";
                      Utils
                        .setDontShowDialogResponse(
                          "weka.gui.explorer.ClassifierPanel.AutoWrapInInputMappedClassifier",
                          response);
                    }

                  } else {
                    // What did the user say - do they want to autowrap or not?
                    String response =
                      Utils
                        .getDontShowDialogResponse("weka.gui.explorer.ClassifierPanel.AutoWrapInInputMappedClassifier");
                    if (response != null && response.equalsIgnoreCase("yes")) {
                      wrapClassifier = true;
                    }
                  }

                  if (wrapClassifier) {
                    weka.classifiers.misc.InputMappedClassifier temp =
                      new weka.classifiers.misc.InputMappedClassifier();

                    // pass on the known test structure so that we get the
                    // correct mapping report from the toString() method
                    // of InputMappedClassifier
                    temp.setClassifier(classifier);
                    temp.setTestStructure(userTestStructure);
                    classifier = temp;
                  } else {
                    throw new Exception(
                      "Train and test set are not compatible\n"
                        + inst.equalHeadersMsg(userTestStructure));
                  }
                }
              }

            } else {
              throw new Exception("Unknown test mode");
            }

            cname = classifier.getClass().getName();
            if (cname.startsWith("weka.classifiers.")) {
              name += cname.substring("weka.classifiers.".length());
            } else {
              name += cname;
            }
            cmd = classifier.getClass().getName();
            if (classifier instanceof OptionHandler) {
              cmd +=
                " "
                  + Utils
                    .joinOptions(((OptionHandler) classifier).getOptions());
            }

            // set up the structure of the plottable instances for
            // visualization
            plotInstances = ExplorerDefaults.getClassifierErrorsPlotInstances();
            plotInstances.setInstances(inst);
            plotInstances.setClassifier(classifier);
            plotInstances.setClassIndex(inst.classIndex());
            plotInstances.setSaveForVisualization(saveVis);
            plotInstances
              .setPointSizeProportionalToMargin(m_errorPlotPointSizeProportionalToMargin
                .isSelected());

            // Output some header information
            m_Log.logMessage("Started " + cname);
            m_Log.logMessage("Command: " + cmd);
            if (m_Log instanceof TaskLogger) {
              ((TaskLogger) m_Log).taskStarted();
            }
            outBuff.append("=== Run information ===\n\n");
            outBuff.append("Scheme:       " + cname);
            if (classifier instanceof OptionHandler) {
              String[] o = ((OptionHandler) classifier).getOptions();
              outBuff.append(" " + Utils.joinOptions(o));
            }
            outBuff.append("\n");
            outBuff.append("Relation:     " + inst.relationName() + '\n');
            outBuff.append("Instances:    " + inst.numInstances() + '\n');
            outBuff.append("Attributes:   " + inst.numAttributes() + '\n');
            if (inst.numAttributes() < 100) {
              for (int i = 0; i < inst.numAttributes(); i++) {
                outBuff.append("              " + inst.attribute(i).name()
                  + '\n');
              }
            } else {
              outBuff.append("              [list of attributes omitted]\n");
            }

            outBuff.append("Test mode:    ");
            switch (testMode) {
            case 3: // Test on training
              outBuff.append("evaluate on training data\n");
              break;
            case 1: // CV mode
              outBuff.append("" + numFolds + "-fold cross-validation\n");
              break;
            case 2: // Percent split
              outBuff.append("split " + percent + "% train, remainder test\n");
              break;
            case 4: // Test on user split
              if (source.isIncremental()) {
                outBuff.append("user supplied test set: "
                  + " size unknown (reading incrementally)\n");
              } else {
                outBuff.append("user supplied test set: "
                  + source.getDataSet().numInstances() + " instances\n");
              }
              break;
            }
            if (costMatrix != null) {
              outBuff.append("Evaluation cost matrix:\n")
                .append(costMatrix.toString()).append("\n");
            }
            outBuff.append("\n");
            m_History.addResult(name, outBuff);
            m_History.setSingle(name);

            // Build the model and output it.
            if (outputModel || (testMode == 3) || (testMode == 4)) {
              m_Log.statusMessage("Building model on training data...");

              trainTimeStart = System.currentTimeMillis();
              classifier.buildClassifier(inst);
              trainTimeElapsed = System.currentTimeMillis() - trainTimeStart;
            }

            if (outputModel) {
              outBuff
                .append("=== Classifier model (full training set) ===\n\n");
              outBuff.append(classifier.toString() + "\n");
              outBuff.append("\nTime taken to build model: "
                + Utils.doubleToString(trainTimeElapsed / 1000.0, 2)
                + " seconds\n\n");
              m_History.updateResult(name);
              if (classifier instanceof Drawable) {
                grph = null;
                try {
                  grph = ((Drawable) classifier).graph();
                } catch (Exception ex) {
                }
              }
              // copy full model for output
              SerializedObject so = new SerializedObject(classifier);
              fullClassifier = (Classifier) so.getObject();
            }

            switch (testMode) {
            case 3: // Test on training
              m_Log.statusMessage("Evaluating on training data...");
              eval = new Evaluation(inst, costMatrix);

              // make adjustments if the classifier is an InputMappedClassifier
              eval =
                setupEval(eval, classifier, inst, costMatrix, plotInstances,
                  classificationOutput, false);
              eval.setMetricsToDisplay(m_selectedEvalMetrics);

              // plotInstances.setEvaluation(eval);
              plotInstances.setUp();

              if (outputPredictionsText) {
                printPredictionsHeader(outBuff, classificationOutput,
                  "training set");
              }

              testTimeStart = System.currentTimeMillis();
              if (classifier instanceof BatchPredictor
                && ((BatchPredictor) classifier)
                  .implementsMoreEfficientBatchPrediction()) {
                Instances toPred = new Instances(inst);
                for (int i = 0; i < toPred.numInstances(); i++) {
                  toPred.instance(i).setClassMissing();
                }
                double[][] predictions =
                  ((BatchPredictor) classifier)
                    .distributionsForInstances(toPred);
                plotInstances.process(inst, predictions, eval);
                if (outputPredictionsText) {
                  for (int jj = 0; jj < inst.numInstances(); jj++) {
                    classificationOutput.printClassification(predictions[jj],
                      inst.instance(jj), jj);
                  }
                }
              } else {
                for (int jj = 0; jj < inst.numInstances(); jj++) {
                  plotInstances.process(inst.instance(jj), classifier, eval);

                  if (outputPredictionsText) {
                    classificationOutput.printClassification(classifier,
                      inst.instance(jj), jj);
                  }
                  if ((jj % 100) == 0) {
                    m_Log
                      .statusMessage("Evaluating on training data. Processed "
                        + jj + " instances...");
                  }
                }
              }
              testTimeElapsed = System.currentTimeMillis() - testTimeStart;
              if (outputPredictionsText) {
                classificationOutput.printFooter();
              }
              if (outputPredictionsText
                && classificationOutput.generatesOutput()) {
                outBuff.append("\n");
              }
              outBuff.append("=== Evaluation on training set ===\n");
              break;

            case 1: // CV mode
              m_Log.statusMessage("Randomizing instances...");
              int rnd = 1;
              try {
                rnd = Integer.parseInt(m_RandomSeedText.getText().trim());
                // System.err.println("Using random seed "+rnd);
              } catch (Exception ex) {
                m_Log.logMessage("Trouble parsing random seed value");
                rnd = 1;
              }
              Random random = new Random(rnd);
              inst.randomize(random);
              if (inst.attribute(classIndex).isNominal()) {
                m_Log.statusMessage("Stratifying instances...");
                inst.stratify(numFolds);
              }
              eval = new Evaluation(inst, costMatrix);

              // make adjustments if the classifier is an InputMappedClassifier
              eval =
                setupEval(eval, classifier, inst, costMatrix, plotInstances,
                  classificationOutput, false);
              eval.setMetricsToDisplay(m_selectedEvalMetrics);

              // plotInstances.setEvaluation(eval);
              plotInstances.setUp();

              if (outputPredictionsText) {
                printPredictionsHeader(outBuff, classificationOutput,
                  "test data");
              }

              // Make some splits and do a CV
              for (int fold = 0; fold < numFolds; fold++) {
                m_Log.statusMessage("Creating splits for fold " + (fold + 1)
                  + "...");
                Instances train = inst.trainCV(numFolds, fold, random);

                // make adjustments if the classifier is an
                // InputMappedClassifier
                eval =
                  setupEval(eval, classifier, train, costMatrix, plotInstances,
                    classificationOutput, true);
                eval.setMetricsToDisplay(m_selectedEvalMetrics);

                // eval.setPriors(train);
                m_Log.statusMessage("Building model for fold " + (fold + 1)
                  + "...");
                Classifier current = null;
                try {
                  current = AbstractClassifier.makeCopy(template);
                } catch (Exception ex) {
                  m_Log.logMessage("Problem copying classifier: "
                    + ex.getMessage());
                }
                current.buildClassifier(train);
                Instances test = inst.testCV(numFolds, fold);
                m_Log.statusMessage("Evaluating model for fold " + (fold + 1)
                  + "...");

                if (classifier instanceof BatchPredictor
                  && ((BatchPredictor) classifier)
                    .implementsMoreEfficientBatchPrediction()) {
                  Instances toPred = new Instances(test);
                  for (int i = 0; i < toPred.numInstances(); i++) {
                    toPred.instance(i).setClassMissing();
                  }
                  double[][] predictions =
                    ((BatchPredictor) current)
                      .distributionsForInstances(toPred);
                  plotInstances.process(test, predictions, eval);
                  if (outputPredictionsText) {
                    for (int jj = 0; jj < test.numInstances(); jj++) {
                      classificationOutput.printClassification(predictions[jj],
                        test.instance(jj), jj);
                    }
                  }
                } else {
                  for (int jj = 0; jj < test.numInstances(); jj++) {
                    plotInstances.process(test.instance(jj), current, eval);
                    if (outputPredictionsText) {
                      classificationOutput.printClassification(current,
                        test.instance(jj), jj);
                    }
                  }
                }
              }
              if (outputPredictionsText) {
                classificationOutput.printFooter();
              }
              if (outputPredictionsText) {
                outBuff.append("\n");
              }
              if (inst.attribute(classIndex).isNominal()) {
                outBuff.append("=== Stratified cross-validation ===\n");
              } else {
                outBuff.append("=== Cross-validation ===\n");
              }
              break;

            case 2: // Percent split
              if (!m_PreserveOrderBut.isSelected()) {
                m_Log.statusMessage("Randomizing instances...");
                try {
                  rnd = Integer.parseInt(m_RandomSeedText.getText().trim());
                } catch (Exception ex) {
                  m_Log.logMessage("Trouble parsing random seed value");
                  rnd = 1;
                }
                inst.randomize(new Random(rnd));
              }
              int trainSize =
                (int) Math.round(inst.numInstances() * percent / 100);
              int testSize = inst.numInstances() - trainSize;
              Instances train = new Instances(inst, 0, trainSize);
              Instances test = new Instances(inst, trainSize, testSize);
              m_Log.statusMessage("Building model on training split ("
                + trainSize + " instances)...");
              Classifier current = null;
              try {
                current = AbstractClassifier.makeCopy(template);
              } catch (Exception ex) {
                m_Log.logMessage("Problem copying classifier: "
                  + ex.getMessage());
              }
              current.buildClassifier(train);
              eval = new Evaluation(train, costMatrix);

              // make adjustments if the classifier is an InputMappedClassifier
              eval =
                setupEval(eval, classifier, train, costMatrix, plotInstances,
                  classificationOutput, false);
              eval.setMetricsToDisplay(m_selectedEvalMetrics);

              // plotInstances.setEvaluation(eval);
              plotInstances.setUp();
              m_Log.statusMessage("Evaluating on test split...");

              if (outputPredictionsText) {
                printPredictionsHeader(outBuff, classificationOutput,
                  "test split");
              }

              testTimeStart = System.currentTimeMillis();
              if (classifier instanceof BatchPredictor
                && ((BatchPredictor) classifier)
                  .implementsMoreEfficientBatchPrediction()) {
                Instances toPred = new Instances(test);
                for (int i = 0; i < toPred.numInstances(); i++) {
                  toPred.instance(i).setClassMissing();
                }

                double[][] predictions =
                  ((BatchPredictor) current).distributionsForInstances(toPred);
                plotInstances.process(test, predictions, eval);
                if (outputPredictionsText) {
                  for (int jj = 0; jj < test.numInstances(); jj++) {
                    classificationOutput.printClassification(predictions[jj],
                      test.instance(jj), jj);
                  }
                }
              } else {
                for (int jj = 0; jj < test.numInstances(); jj++) {
                  plotInstances.process(test.instance(jj), current, eval);
                  if (outputPredictionsText) {
                    classificationOutput.printClassification(current,
                      test.instance(jj), jj);
                  }
                  if ((jj % 100) == 0) {
                    m_Log.statusMessage("Evaluating on test split. Processed "
                      + jj + " instances...");
                  }
                }
              }
              testTimeElapsed = System.currentTimeMillis() - testTimeStart;
              if (outputPredictionsText) {
                classificationOutput.printFooter();
              }
              if (outputPredictionsText) {
                outBuff.append("\n");
              }
              outBuff.append("=== Evaluation on test split ===\n");
              break;

            case 4: // Test on user split
              m_Log.statusMessage("Evaluating on test data...");
              eval = new Evaluation(inst, costMatrix);
              // make adjustments if the classifier is an InputMappedClassifier
              eval =
                setupEval(eval, classifier, inst, costMatrix, plotInstances,
                  classificationOutput, false);
              eval.setMetricsToDisplay(m_selectedEvalMetrics);

              // plotInstances.setEvaluation(eval);
              plotInstances.setUp();

              if (outputPredictionsText) {
                printPredictionsHeader(outBuff, classificationOutput,
                  "test set");
              }

              Instance instance;
              int jj = 0;
              Instances batchInst = null;
              int batchSize = 100;
              if (classifier instanceof BatchPredictor
                && ((BatchPredictor) classifier)
                  .implementsMoreEfficientBatchPrediction()) {
                batchInst = new Instances(userTestStructure, 0);
                String batchSizeS =
                  ((BatchPredictor) classifier).getBatchSize();
                if (batchSizeS != null && batchSizeS.length() > 0) {
                  try {
                    batchSizeS =
                      Environment.getSystemWide().substitute(batchSizeS);
                  } catch (Exception ex) {
                  }

                  try {
                    batchSize = Integer.parseInt(batchSizeS);
                  } catch (NumberFormatException ex) {
                    // just go with the default
                  }
                }
              }
              testTimeStart = System.currentTimeMillis();
              while (source.hasMoreElements(userTestStructure)) {
                instance = source.nextElement(userTestStructure);

                if (classifier instanceof BatchPredictor
                  && ((BatchPredictor) classifier)
                    .implementsMoreEfficientBatchPrediction()) {
                  batchInst.add(instance);
                  if (batchInst.numInstances() == batchSize) {
                    Instances toPred = new Instances(batchInst);
                    for (int i = 0; i < toPred.numInstances(); i++) {
                      toPred.instance(i).setClassMissing();
                    }
                    double[][] predictions =
                      ((BatchPredictor) classifier)
                        .distributionsForInstances(toPred);
                    plotInstances.process(batchInst, predictions, eval);

                    if (outputPredictionsText) {
                      for (int kk = 0; kk < batchInst.numInstances(); kk++) {
                        classificationOutput.printClassification(
                          predictions[kk], batchInst.instance(kk), kk);
                      }
                    }
                    jj += batchInst.numInstances();
                    m_Log.statusMessage("Evaluating on test data. Processed "
                      + jj + " instances...");
                    batchInst.delete();
                  }
                } else {
                  plotInstances.process(instance, classifier, eval);
                  if (outputPredictionsText) {
                    classificationOutput.printClassification(classifier,
                      instance, jj);
                  }
                  if ((++jj % 100) == 0) {
                    m_Log.statusMessage("Evaluating on test data. Processed "
                      + jj + " instances...");
                  }
                }
              }

              if (classifier instanceof BatchPredictor
                && ((BatchPredictor) classifier)
                  .implementsMoreEfficientBatchPrediction()
                && batchInst.numInstances() > 0) {
                // finish the last batch

                Instances toPred = new Instances(batchInst);
                for (int i = 0; i < toPred.numInstances(); i++) {
                  toPred.instance(i).setClassMissing();
                }

                double[][] predictions =
                  ((BatchPredictor) classifier)
                    .distributionsForInstances(toPred);
                plotInstances.process(batchInst, predictions, eval);

                if (outputPredictionsText) {
                  for (int kk = 0; kk < batchInst.numInstances(); kk++) {
                    classificationOutput.printClassification(predictions[kk],
                      batchInst.instance(kk), kk);
                  }
                }
              }
              testTimeElapsed = System.currentTimeMillis() - testTimeStart;

              if (outputPredictionsText) {
                classificationOutput.printFooter();
              }
              if (outputPredictionsText) {
                outBuff.append("\n");
              }
              outBuff.append("=== Evaluation on test set ===\n");
              break;

            default:
              throw new Exception("Test mode not implemented");
            }

            if (testMode != 1) {
              String mode = "";
              if (testMode == 2) {
                mode = "training split";
              } else if (testMode == 3) {
                mode = "training data";
              } else if (testMode == 4) {
                mode = "supplied test set";
              }
              outBuff.append("\nTime taken to test model on " + mode + ": "
                + Utils.doubleToString(testTimeElapsed / 1000.0, 2)
                + " seconds\n\n");
            }

            if (outputSummary) {
              outBuff.append(eval.toSummaryString(outputEntropy) + "\n");
            }

            if (inst.attribute(classIndex).isNominal()) {

              if (outputPerClass) {
                outBuff.append(eval.toClassDetailsString() + "\n");
              }

              if (outputConfusion) {
                outBuff.append(eval.toMatrixString() + "\n");
              }
            }

            if ((fullClassifier instanceof Sourcable)
              && m_OutputSourceCode.isSelected()) {
              outBuff.append("=== Source code ===\n\n");
              outBuff.append(Evaluation.wekaStaticWrapper(
                ((Sourcable) fullClassifier), m_SourceCodeClass.getText()));
            }

            m_History.updateResult(name);
            m_Log.logMessage("Finished " + cname);
            m_Log.statusMessage("OK");
          } catch (Exception ex) {
            ex.printStackTrace();
            m_Log.logMessage(ex.getMessage());
            JOptionPane.showMessageDialog(ClassifierPanel.this,
              "Problem evaluating classifier:\n" + ex.getMessage(),
              "Evaluate classifier", JOptionPane.ERROR_MESSAGE);
            m_Log.statusMessage("Problem evaluating classifier");
          } finally {
            try {
              if (!saveVis && outputModel) {
                ArrayList<Object> vv = new ArrayList<Object>();
                vv.add(fullClassifier);
                Instances trainHeader = new Instances(m_Instances, 0);
                trainHeader.setClassIndex(classIndex);
                vv.add(trainHeader);
                if (grph != null) {
                  vv.add(grph);
                }
                m_History.addObject(name, vv);
              } else if (saveVis && plotInstances != null
                && plotInstances.canPlot(false)) {
                m_CurrentVis = new VisualizePanel();
                if (getMainApplication() != null) {
                  Settings settings = getMainApplication().getApplicationSettings();
                  m_CurrentVis.applySettings(settings,
                    weka.gui.explorer.VisualizePanel.ScatterDefaults.ID);
                }
                m_CurrentVis.setName(name + " (" + inst.relationName() + ")");
                m_CurrentVis.setLog(m_Log);
                m_CurrentVis.addPlot(plotInstances.getPlotData(cname));
                // m_CurrentVis.setColourIndex(plotInstances.getPlotInstances().classIndex()+1);
                m_CurrentVis.setColourIndex(plotInstances.getPlotInstances()
                  .classIndex());
                plotInstances.cleanUp();

                ArrayList<Object> vv = new ArrayList<Object>();
                if (outputModel) {
                  vv.add(fullClassifier);
                  Instances trainHeader = new Instances(m_Instances, 0);
                  trainHeader.setClassIndex(classIndex);
                  vv.add(trainHeader);
                  if (grph != null) {
                    vv.add(grph);
                  }
                }
                vv.add(m_CurrentVis);

                if ((eval != null) && (eval.predictions() != null)) {
                  vv.add(eval.predictions());
                  vv.add(inst.classAttribute());
                }
                m_History.addObject(name, vv);
              }
            } catch (Exception ex) {
              ex.printStackTrace();
            }

            if (isInterrupted()) {
              m_Log.logMessage("Interrupted " + cname);
              m_Log.statusMessage("Interrupted");
            }

            synchronized (this) {
              m_StartBut.setEnabled(true);
              m_StopBut.setEnabled(false);
              m_RunThread = null;
            }
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
   * Handles constructing a popup menu with visualization options.
   * 
   * @param name the name of the result history list entry clicked on by the
   *          user
   * @param x the x coordinate for popping up the menu
   * @param y the y coordinate for popping up the menu
   */
  @SuppressWarnings("unchecked")
  protected void visualize(String name, int x, int y) {
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
        loadClassifier();
      }
    });
    resultListMenu.add(loadModel);

    ArrayList<Object> o = null;
    if (selectedName != null) {
      o = (ArrayList<Object>) m_History.getNamedObject(selectedName);
    }

    VisualizePanel temp_vp = null;
    String temp_grph = null;
    ArrayList<Prediction> temp_preds = null;
    Attribute temp_classAtt = null;
    Classifier temp_classifier = null;
    Instances temp_trainHeader = null;

    if (o != null) {
      for (int i = 0; i < o.size(); i++) {
        Object temp = o.get(i);
        if (temp instanceof Classifier) {
          temp_classifier = (Classifier) temp;
        } else if (temp instanceof Instances) { // training header
          temp_trainHeader = (Instances) temp;
        } else if (temp instanceof VisualizePanel) { // normal errors
          temp_vp = (VisualizePanel) temp;
        } else if (temp instanceof String) { // graphable output
          temp_grph = (String) temp;
        } else if (temp instanceof ArrayList<?>) { // predictions
          temp_preds = (ArrayList<Prediction>) temp;
        } else if (temp instanceof Attribute) { // class attribute
          temp_classAtt = (Attribute) temp;
        }
      }
    }

    final VisualizePanel vp = temp_vp;
    final String grph = temp_grph;
    final ArrayList<Prediction> preds = temp_preds;
    final Attribute classAtt = temp_classAtt;
    final Classifier classifier = temp_classifier;
    final Instances trainHeader = temp_trainHeader;

    JMenuItem saveModel = new JMenuItem("Save model");
    if (classifier != null) {
      saveModel.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          saveClassifier(selectedName, classifier, trainHeader);
        }
      });
    } else {
      saveModel.setEnabled(false);
    }
    resultListMenu.add(saveModel);

    JMenuItem reEvaluate =
      new JMenuItem("Re-evaluate model on current test set");
    if (classifier != null && m_TestLoader != null) {
      reEvaluate.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          reevaluateModel(selectedName, classifier, trainHeader);
        }
      });
    } else {
      reEvaluate.setEnabled(false);
    }
    resultListMenu.add(reEvaluate);

    JMenuItem reApplyConfig =
      new JMenuItem("Re-apply this model's configuration");
    if (classifier != null) {
      reApplyConfig.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          m_ClassifierEditor.setValue(classifier);
        }
      });
    } else {
      reApplyConfig.setEnabled(false);
    }
    resultListMenu.add(reApplyConfig);

    resultListMenu.addSeparator();

    JMenuItem visErrors = new JMenuItem("Visualize classifier errors");
    if (vp != null) {
      if ((vp.getXIndex() == 0) && (vp.getYIndex() == 1)) {
        try {
          vp.setXIndex(vp.getInstances().classIndex()); // class
          vp.setYIndex(vp.getInstances().classIndex() - 1); // predicted class
        } catch (Exception e) {
          // ignored
        }
      }
      visErrors.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          visualizeClassifierErrors(vp);
        }
      });
    } else {
      visErrors.setEnabled(false);
    }
    resultListMenu.add(visErrors);

    JMenuItem visGrph = new JMenuItem("Visualize tree");
    if (grph != null) {
      if (((Drawable) temp_classifier).graphType() == Drawable.TREE) {
        visGrph.addActionListener(new ActionListener() {
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
      } else if (((Drawable) temp_classifier).graphType() == Drawable.BayesNet) {
        visGrph.setText("Visualize graph");
        visGrph.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Thread th = new Thread() {
              @Override
              public void run() {
                visualizeBayesNet(grph, selectedName);
              }
            };
            th.start();
          }
        });
      } else {
        visGrph.setEnabled(false);
      }
    } else {
      visGrph.setEnabled(false);
    }
    resultListMenu.add(visGrph);

    JMenuItem visMargin = new JMenuItem("Visualize margin curve");
    if ((preds != null) && (classAtt != null) && (classAtt.isNominal())) {
      visMargin.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            MarginCurve tc = new MarginCurve();
            Instances result = tc.getCurve(preds);
            VisualizePanel vmc = new VisualizePanel();
            if (getMainApplication() != null) {
              Settings settings = getMainApplication().getApplicationSettings();
              m_CurrentVis.applySettings(settings,
                weka.gui.explorer.VisualizePanel.ScatterDefaults.ID);
            }
            vmc.setName(result.relationName());
            vmc.setLog(m_Log);
            PlotData2D tempd = new PlotData2D(result);
            tempd.setPlotName(result.relationName());
            tempd.addInstanceNumberAttribute();
            vmc.addPlot(tempd);
            visualizeClassifierErrors(vmc);
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      });
    } else {
      visMargin.setEnabled(false);
    }
    resultListMenu.add(visMargin);

    JMenu visThreshold = new JMenu("Visualize threshold curve");
    if ((preds != null) && (classAtt != null) && (classAtt.isNominal())) {
      for (int i = 0; i < classAtt.numValues(); i++) {
        JMenuItem clv = new JMenuItem(classAtt.value(i));
        final int classValue = i;
        clv.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            try {
              ThresholdCurve tc = new ThresholdCurve();
              Instances result = tc.getCurve(preds, classValue);
              // VisualizePanel vmc = new VisualizePanel();
              ThresholdVisualizePanel vmc = new ThresholdVisualizePanel();
              vmc.setROCString("(Area under ROC = "
                + Utils.doubleToString(ThresholdCurve.getROCArea(result), 4)
                + ")");
              vmc.setLog(m_Log);
              vmc.setName(result.relationName() + ". (Class value "
                + classAtt.value(classValue) + ")");
              PlotData2D tempd = new PlotData2D(result);
              tempd.setPlotName(result.relationName());
              tempd.addInstanceNumberAttribute();
              // specify which points are connected
              boolean[] cp = new boolean[result.numInstances()];
              for (int n = 1; n < cp.length; n++) {
                cp[n] = true;
              }
              tempd.setConnectPoints(cp);
              // add plot
              vmc.addPlot(tempd);
              visualizeClassifierErrors(vmc);
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        });
        visThreshold.add(clv);
      }
    } else {
      visThreshold.setEnabled(false);
    }
    resultListMenu.add(visThreshold);

    JMenu visCostBenefit = new JMenu("Cost/Benefit analysis");
    if ((preds != null) && (classAtt != null) && (classAtt.isNominal())) {
      for (int i = 0; i < classAtt.numValues(); i++) {
        JMenuItem clv = new JMenuItem(classAtt.value(i));
        final int classValue = i;
        clv.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            try {
              ThresholdCurve tc = new ThresholdCurve();
              Instances result = tc.getCurve(preds, classValue);

              // Create a dummy class attribute with the chosen
              // class value as index 0 (if necessary).
              Attribute classAttToUse = classAtt;
              if (classValue != 0) {
                ArrayList<String> newNames = new ArrayList<String>();
                newNames.add(classAtt.value(classValue));
                for (int k = 0; k < classAtt.numValues(); k++) {
                  if (k != classValue) {
                    newNames.add(classAtt.value(k));
                  }
                }
                classAttToUse = new Attribute(classAtt.name(), newNames);
              }

              CostBenefitAnalysis cbAnalysis = new CostBenefitAnalysis();

              PlotData2D tempd = new PlotData2D(result);
              tempd.setPlotName(result.relationName());
              tempd.m_alwaysDisplayPointsOfThisSize = 10;
              // specify which points are connected
              boolean[] cp = new boolean[result.numInstances()];
              for (int n = 1; n < cp.length; n++) {
                cp[n] = true;
              }
              tempd.setConnectPoints(cp);

              String windowTitle = "";
              if (classifier != null) {
                String cname = classifier.getClass().getName();
                if (cname.startsWith("weka.classifiers.")) {
                  windowTitle =
                    "" + cname.substring("weka.classifiers.".length()) + " ";
                }
              }
              windowTitle += " (class = " + classAttToUse.value(0) + ")";

              // add plot
              cbAnalysis.setCurveData(tempd, classAttToUse);
              visualizeCostBenefitAnalysis(cbAnalysis, windowTitle);
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        });
        visCostBenefit.add(clv);
      }
    } else {
      visCostBenefit.setEnabled(false);
    }
    resultListMenu.add(visCostBenefit);

    JMenu visCost = new JMenu("Visualize cost curve");
    if ((preds != null) && (classAtt != null) && (classAtt.isNominal())) {
      for (int i = 0; i < classAtt.numValues(); i++) {
        JMenuItem clv = new JMenuItem(classAtt.value(i));
        final int classValue = i;
        clv.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            try {
              CostCurve cc = new CostCurve();
              Instances result = cc.getCurve(preds, classValue);
              VisualizePanel vmc = new VisualizePanel();
              if (getMainApplication() != null) {
                Settings settings = getMainApplication().getApplicationSettings();
                m_CurrentVis.applySettings(settings,
                  weka.gui.explorer.VisualizePanel.ScatterDefaults.ID);
              }
              vmc.setLog(m_Log);
              vmc.setName(result.relationName() + ". (Class value "
                + classAtt.value(classValue) + ")");
              PlotData2D tempd = new PlotData2D(result);
              tempd.m_displayAllPoints = true;
              tempd.setPlotName(result.relationName());
              boolean[] connectPoints = new boolean[result.numInstances()];
              for (int jj = 1; jj < connectPoints.length; jj += 2) {
                connectPoints[jj] = true;
              }
              tempd.setConnectPoints(connectPoints);
              // tempd.addInstanceNumberAttribute();
              vmc.addPlot(tempd);
              visualizeClassifierErrors(vmc);
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        });
        visCost.add(clv);
      }
    } else {
      visCost.setEnabled(false);
    }
    resultListMenu.add(visCost);

    // visualization plugins
    JMenu visPlugins = new JMenu("Plugins");
    boolean availablePlugins = false;

    // predictions
    Vector<String> pluginsVector =
      GenericObjectEditor.getClassnames(VisualizePlugin.class.getName());
    for (int i = 0; i < pluginsVector.size(); i++) {
      String className = (pluginsVector.elementAt(i));
      try {
        VisualizePlugin plugin =
          (VisualizePlugin) Class.forName(className).newInstance();
        if (plugin == null) {
          continue;
        }
        availablePlugins = true;
        JMenuItem pluginMenuItem = plugin.getVisualizeMenuItem(preds, classAtt);
        new Version();
        if (pluginMenuItem != null) {
          /*
           * if (version.compareTo(plugin.getMinVersion()) < 0)
           * pluginMenuItem.setText(pluginMenuItem.getText() +
           * " (weka outdated)"); if (version.compareTo(plugin.getMaxVersion())
           * >= 0) pluginMenuItem.setText(pluginMenuItem.getText() +
           * " (plugin outdated)");
           */
          visPlugins.add(pluginMenuItem);
        }
      } catch (Exception e) {
        // e.printStackTrace();
      }
    }

    // errros
    pluginsVector =
      GenericObjectEditor.getClassnames(ErrorVisualizePlugin.class.getName());
    for (int i = 0; i < pluginsVector.size(); i++) {
      String className = (pluginsVector.elementAt(i));
      try {
        ErrorVisualizePlugin plugin =
          (ErrorVisualizePlugin) Class.forName(className).newInstance();
        if (plugin == null) {
          continue;
        }
        availablePlugins = true;
        JMenuItem pluginMenuItem =
          plugin.getVisualizeMenuItem(vp.getInstances());
        new Version();
        if (pluginMenuItem != null) {
          /*
           * if (version.compareTo(plugin.getMinVersion()) < 0)
           * pluginMenuItem.setText(pluginMenuItem.getText() +
           * " (weka outdated)"); if (version.compareTo(plugin.getMaxVersion())
           * >= 0) pluginMenuItem.setText(pluginMenuItem.getText() +
           * " (plugin outdated)");
           */
          visPlugins.add(pluginMenuItem);
        }
      } catch (Exception e) {
        // e.printStackTrace();
      }
    }

    // graphs+trees
    if (grph != null) {
      // trees
      if (((Drawable) temp_classifier).graphType() == Drawable.TREE) {
        pluginsVector =
          GenericObjectEditor
            .getClassnames(TreeVisualizePlugin.class.getName());
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
            new Version();
            if (pluginMenuItem != null) {
              /*
               * if (version.compareTo(plugin.getMinVersion()) < 0)
               * pluginMenuItem.setText(pluginMenuItem.getText() +
               * " (weka outdated)"); if
               * (version.compareTo(plugin.getMaxVersion()) >= 0)
               * pluginMenuItem.setText(pluginMenuItem.getText() +
               * " (plugin outdated)");
               */
              visPlugins.add(pluginMenuItem);
            }
          } catch (Exception e) {
            // e.printStackTrace();
          }
        }
      }
      // graphs
      else {
        pluginsVector =
          GenericObjectEditor.getClassnames(GraphVisualizePlugin.class
            .getName());
        for (int i = 0; i < pluginsVector.size(); i++) {
          String className = (pluginsVector.elementAt(i));
          try {
            GraphVisualizePlugin plugin =
              (GraphVisualizePlugin) Class.forName(className).newInstance();
            if (plugin == null) {
              continue;
            }
            availablePlugins = true;
            JMenuItem pluginMenuItem =
              plugin.getVisualizeMenuItem(grph, selectedName);
            new Version();
            if (pluginMenuItem != null) {
              /*
               * if (version.compareTo(plugin.getMinVersion()) < 0)
               * pluginMenuItem.setText(pluginMenuItem.getText() +
               * " (weka outdated)"); if
               * (version.compareTo(plugin.getMaxVersion()) >= 0)
               * pluginMenuItem.setText(pluginMenuItem.getText() +
               * " (plugin outdated)");
               */
              visPlugins.add(pluginMenuItem);
            }
          } catch (Exception e) {
            // e.printStackTrace();
          }
        }
      }
    }

    if (availablePlugins) {
      resultListMenu.add(visPlugins);
    }

    resultListMenu.show(m_History.getList(), x, y);
  }

  /**
   * Pops up a TreeVisualizer for the classifier from the currently selected
   * item in the results list.
   * 
   * @param dottyString the description of the tree in dotty format
   * @param treeName the title to assign to the display
   */
  protected void visualizeTree(String dottyString, String treeName) {
    final javax.swing.JFrame jf =
      new javax.swing.JFrame("Weka Classifier Tree Visualizer: " + treeName);
    jf.setSize(500, 400);
    jf.getContentPane().setLayout(new BorderLayout());
    TreeVisualizer tv = new TreeVisualizer(null, dottyString, new PlaceNode2());
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

  /**
   * Pops up a GraphVisualizer for the BayesNet classifier from the currently
   * selected item in the results list.
   * 
   * @param XMLBIF the description of the graph in XMLBIF ver. 0.3
   * @param graphName the name of the graph
   */
  protected void visualizeBayesNet(String XMLBIF, String graphName) {
    final javax.swing.JFrame jf =
      new javax.swing.JFrame("Weka Classifier Graph Visualizer: " + graphName);
    jf.setSize(500, 400);
    jf.getContentPane().setLayout(new BorderLayout());
    GraphVisualizer gv = new GraphVisualizer();
    try {
      gv.readBIF(XMLBIF);
    } catch (BIFFormatException be) {
      System.err.println("unable to visualize BayesNet");
      be.printStackTrace();
    }
    gv.layoutGraph();

    jf.getContentPane().add(gv, BorderLayout.CENTER);
    jf.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent e) {
        jf.dispose();
      }
    });

    jf.setVisible(true);
  }

  /**
   * Pops up the Cost/Benefit analysis panel.
   * 
   * @param cb the CostBenefitAnalysis panel to pop up
   */
  protected void visualizeCostBenefitAnalysis(CostBenefitAnalysis cb,
    String classifierAndRelationName) {
    if (cb != null) {
      String windowTitle = "Weka Classifier: Cost/Benefit Analysis ";
      if (classifierAndRelationName != null) {
        windowTitle += "- " + classifierAndRelationName;
      }
      final javax.swing.JFrame jf = new javax.swing.JFrame(windowTitle);
      jf.setSize(1000, 600);
      jf.getContentPane().setLayout(new BorderLayout());

      jf.getContentPane().add(cb, BorderLayout.CENTER);
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
   * Pops up a VisualizePanel for visualizing the data and errors for the
   * classifier from the currently selected item in the results list.
   * 
   * @param sp the VisualizePanel to pop up.
   */
  protected void visualizeClassifierErrors(VisualizePanel sp) {

    if (sp != null) {
      String plotName = sp.getName();
      final javax.swing.JFrame jf =
        new javax.swing.JFrame("Weka Classifier Visualize: " + plotName);
      jf.setSize(600, 400);
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
   * Save the currently selected classifier output to a file.
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

  /**
   * Stops the currently running classifier (if any).
   */
  @SuppressWarnings("deprecation")
  protected void stopClassifier() {

    if (m_RunThread != null) {
      m_RunThread.interrupt();

      // This is deprecated (and theoretically the interrupt should do).
      m_RunThread.stop();
    }
  }

  /**
   * Saves the currently selected classifier.
   * 
   * @param name the name of the run
   * @param classifier the classifier to save
   * @param trainHeader the header of the training instances
   */
  protected void saveClassifier(String name, Classifier classifier,
    Instances trainHeader) {

    File sFile = null;
    boolean saveOK = true;

    m_FileChooser.removeChoosableFileFilter(m_PMMLModelFilter);
    m_FileChooser.setFileFilter(m_ModelFilter);
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
        objectOutputStream.writeObject(classifier);
        trainHeader = trainHeader.stringFreeStructure();
        if (trainHeader != null) {
          objectOutputStream.writeObject(trainHeader);
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
   * Loads a classifier.
   */
  protected void loadClassifier() {

    m_FileChooser.addChoosableFileFilter(m_PMMLModelFilter);
    m_FileChooser.setFileFilter(m_ModelFilter);
    int returnVal = m_FileChooser.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File selected = m_FileChooser.getSelectedFile();
      Classifier classifier = null;
      Instances trainHeader = null;

      m_Log.statusMessage("Loading model from file...");

      try {
        InputStream is = new FileInputStream(selected);
        if (selected.getName().endsWith(PMML_FILE_EXTENSION)) {
          PMMLModel model = PMMLFactory.getPMMLModel(is, m_Log);
          if (model instanceof PMMLClassifier) {
            classifier = (PMMLClassifier) model;
            /*
             * trainHeader = ((PMMLClassifier)classifier).getMiningSchema().
             * getMiningSchemaAsInstances();
             */
          } else {
            throw new Exception(
              "PMML model is not a classification/regression model!");
          }
        } else {
          if (selected.getName().endsWith(".gz")) {
            is = new GZIPInputStream(is);
          }
          ObjectInputStream objectInputStream = new ObjectInputStream(is);
          classifier = (Classifier) objectInputStream.readObject();
          try { // see if we can load the header
            trainHeader = (Instances) objectInputStream.readObject();
          } catch (Exception e) {
          } // don't fuss if we can't
          objectInputStream.close();
        }
      } catch (Exception e) {

        JOptionPane.showMessageDialog(null, e, "Load Failed",
          JOptionPane.ERROR_MESSAGE);
      }

      m_Log.statusMessage("OK");

      if (classifier != null) {
        m_Log.logMessage("Loaded model from file '" + selected.getName() + "'");
        String name = (new SimpleDateFormat("HH:mm:ss - ")).format(new Date());
        String cname = classifier.getClass().getName();
        if (cname.startsWith("weka.classifiers.")) {
          cname = cname.substring("weka.classifiers.".length());
        }
        name += cname + " from file '" + selected.getName() + "'";
        StringBuffer outBuff = new StringBuffer();

        outBuff.append("=== Model information ===\n\n");
        outBuff.append("Filename:     " + selected.getName() + "\n");
        outBuff.append("Scheme:       " + classifier.getClass().getName());
        if (classifier instanceof OptionHandler) {
          String[] o = ((OptionHandler) classifier).getOptions();
          outBuff.append(" " + Utils.joinOptions(o));
        }
        outBuff.append("\n");
        if (trainHeader != null) {
          outBuff.append("Relation:     " + trainHeader.relationName() + '\n');
          outBuff.append("Attributes:   " + trainHeader.numAttributes() + '\n');
          if (trainHeader.numAttributes() < 100) {
            for (int i = 0; i < trainHeader.numAttributes(); i++) {
              outBuff.append("              " + trainHeader.attribute(i).name()
                + '\n');
            }
          } else {
            outBuff.append("              [list of attributes omitted]\n");
          }
        } else {
          outBuff.append("\nTraining data unknown\n");
        }

        outBuff.append("\n=== Classifier model ===\n\n");
        outBuff.append(classifier.toString() + "\n");

        m_History.addResult(name, outBuff);
        m_History.setSingle(name);
        ArrayList<Object> vv = new ArrayList<Object>();
        vv.add(classifier);
        if (trainHeader != null) {
          vv.add(trainHeader);
        }
        // allow visualization of graphable classifiers
        String grph = null;
        if (classifier instanceof Drawable) {
          try {
            grph = ((Drawable) classifier).graph();
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
   * Re-evaluates the named classifier with the current test set. Unpredictable
   * things will happen if the data set is not compatible with the classifier.
   * 
   * @param name the name of the classifier entry
   * @param classifier the classifier to evaluate
   * @param trainHeader the header of the training set
   */
  protected void reevaluateModel(final String name,
    final Classifier classifier, final Instances trainHeader) {

    if (m_RunThread == null) {
      synchronized (this) {
        m_StartBut.setEnabled(false);
        m_StopBut.setEnabled(true);
      }
      m_RunThread = new Thread() {
        @Override
        public void run() {
          // Copy the current state of things
          m_Log.statusMessage("Setting up...");
          Classifier classifierToUse = classifier;

          StringBuffer outBuff = m_History.getNamedBuffer(name);
          DataSource source = null;
          Instances userTestStructure = null;
          ClassifierErrorsPlotInstances plotInstances = null;

          CostMatrix costMatrix = null;
          if (m_EvalWRTCostsBut.isSelected()) {
            costMatrix =
              new CostMatrix((CostMatrix) m_CostMatrixEditor.getValue());
          }
          boolean outputConfusion = m_OutputConfusionBut.isSelected();
          boolean outputPerClass = m_OutputPerClassBut.isSelected();
          boolean outputSummary = true;
          boolean outputEntropy = m_OutputEntropyBut.isSelected();
          boolean saveVis = m_StorePredictionsBut.isSelected();
          boolean outputPredictionsText =
            (m_ClassificationOutputEditor.getValue().getClass() != Null.class);
          String grph = null;
          Evaluation eval = null;

          try {

            boolean incrementalLoader =
              (m_TestLoader instanceof IncrementalConverter);
            if (m_TestLoader != null && m_TestLoader.getStructure() != null) {
              m_TestLoader.reset();
              if (classifierToUse instanceof BatchPredictor
                && ((BatchPredictor) classifierToUse)
                  .implementsMoreEfficientBatchPrediction()
                && m_TestLoader instanceof ArffLoader) {
                ((ArffLoader) m_TestLoader).setRetainStringVals(true);
              }
              source = new DataSource(m_TestLoader);
              userTestStructure = source.getStructure();
              userTestStructure.setClassIndex(m_TestClassIndex);
            }
            // Check the test instance compatibility
            if (source == null) {
              throw new Exception("No user test set has been specified");
            }
            if (trainHeader != null) {
              if (!trainHeader.equalHeaders(userTestStructure)) {
                if (!(classifierToUse instanceof weka.classifiers.misc.InputMappedClassifier)) {

                  boolean wrapClassifier = false;
                  if (!Utils
                    .getDontShowDialog("weka.gui.explorer.ClassifierPanel.AutoWrapInInputMappedClassifier")) {
                    JCheckBox dontShow =
                      new JCheckBox("Do not show this message again");
                    Object[] stuff = new Object[2];
                    stuff[0] =
                      "Data used to train model and test set are not compatible.\n"
                        + "Would you like to automatically wrap the classifier in\n"
                        + "an \"InputMappedClassifier\" before proceeding?.\n";
                    stuff[1] = dontShow;

                    int result =
                      JOptionPane.showConfirmDialog(ClassifierPanel.this,
                        stuff, "ClassifierPanel", JOptionPane.YES_OPTION);

                    if (result == JOptionPane.YES_OPTION) {
                      wrapClassifier = true;
                    }

                    if (dontShow.isSelected()) {
                      String response = (wrapClassifier) ? "yes" : "no";
                      Utils
                        .setDontShowDialogResponse(
                          "weka.gui.explorer.ClassifierPanel.AutoWrapInInputMappedClassifier",
                          response);
                    }

                  } else {
                    // What did the user say - do they want to autowrap or not?
                    String response =
                      Utils
                        .getDontShowDialogResponse("weka.gui.explorer.ClassifierPanel.AutoWrapInInputMappedClassifier");
                    if (response != null && response.equalsIgnoreCase("yes")) {
                      wrapClassifier = true;
                    }
                  }

                  if (wrapClassifier) {
                    weka.classifiers.misc.InputMappedClassifier temp =
                      new weka.classifiers.misc.InputMappedClassifier();

                    temp.setClassifier(classifierToUse);
                    temp.setModelHeader(trainHeader);
                    temp.setTestStructure(userTestStructure);
                    classifierToUse = temp;
                  } else {
                    throw new Exception(
                      "Train and test set are not compatible\n"
                        + trainHeader.equalHeadersMsg(userTestStructure));
                  }
                }
              }
            } else {
              if (classifierToUse instanceof PMMLClassifier) {
                // set the class based on information in the mining schema
                Instances miningSchemaStructure =
                  ((PMMLClassifier) classifierToUse).getMiningSchema()
                    .getMiningSchemaAsInstances();
                String className =
                  miningSchemaStructure.classAttribute().name();
                Attribute classMatch = userTestStructure.attribute(className);
                if (classMatch == null) {
                  throw new Exception(
                    "Can't find a match for the PMML target field " + className
                      + " in the " + "test instances!");
                }
                userTestStructure.setClass(classMatch);
              } else {
                userTestStructure.setClassIndex(userTestStructure
                  .numAttributes() - 1);
              }
            }
            if (m_Log instanceof TaskLogger) {
              ((TaskLogger) m_Log).taskStarted();
            }
            m_Log.statusMessage("Evaluating on test data...");
            m_Log.logMessage("Re-evaluating classifier (" + name
              + ") on test set");
            eval = new Evaluation(userTestStructure, costMatrix);
            eval.setMetricsToDisplay(m_selectedEvalMetrics);

            // set up the structure of the plottable instances for
            // visualization if selected
            // if (saveVis) {
            plotInstances = ExplorerDefaults.getClassifierErrorsPlotInstances();
            plotInstances.setInstances(trainHeader != null ? trainHeader : userTestStructure);
            plotInstances.setClassifier(classifierToUse);
            plotInstances.setClassIndex(trainHeader != null ? trainHeader.classIndex() : userTestStructure.classIndex());
            plotInstances.setSaveForVisualization(saveVis);
            plotInstances.setEvaluation(eval);

            outBuff.append("\n=== Re-evaluation on test set ===\n\n");
            outBuff.append("User supplied test set\n");
            outBuff.append("Relation:     " + userTestStructure.relationName()
              + '\n');
            if (incrementalLoader) {
              outBuff
                .append("Instances:     unknown (yet). Reading incrementally\n");
            } else {
              outBuff.append("Instances:    "
                + source.getDataSet().numInstances() + "\n");
            }
            outBuff.append("Attributes:   " + userTestStructure.numAttributes()
              + "\n\n");
            if (trainHeader == null
              && !(classifierToUse instanceof weka.classifiers.pmml.consumer.PMMLClassifier)) {
              outBuff
                .append("NOTE - if test set is not compatible then results are "
                  + "unpredictable\n\n");
            }

            AbstractOutput classificationOutput = null;
            if (outputPredictionsText) {
              classificationOutput =
                (AbstractOutput) m_ClassificationOutputEditor.getValue();
              classificationOutput.setHeader(userTestStructure);
              classificationOutput.setBuffer(outBuff);
            }

            // make adjustments if the classifier is an InputMappedClassifier
            eval =
              setupEval(eval, classifierToUse, trainHeader != null ? trainHeader : userTestStructure, costMatrix,
                plotInstances, classificationOutput, false);
            eval.useNoPriors();
            plotInstances.setUp();

            if (outputPredictionsText) {
              printPredictionsHeader(outBuff, classificationOutput,
                "user test set");
            }

            int batchSize = 100;
            Instances batchInst = null;
            if (classifierToUse instanceof BatchPredictor
              && ((BatchPredictor) classifierToUse)
                .implementsMoreEfficientBatchPrediction()) {
              batchInst = new Instances(userTestStructure, 0);
              String batchSizeS =
                ((BatchPredictor) classifierToUse).getBatchSize();
              if (batchSizeS != null && batchSizeS.length() > 0) {
                try {
                  batchSizeS =
                    Environment.getSystemWide().substitute(batchSizeS);
                } catch (Exception ex) {
                }
                try {
                  batchSize = Integer.parseInt(batchSizeS);
                } catch (NumberFormatException e) {
                  // just go with the default
                }
              }
            }
            Instance instance;
            int jj = 0;
            while (source.hasMoreElements(userTestStructure)) {
              instance = source.nextElement(userTestStructure);

              if (classifierToUse instanceof BatchPredictor
                && ((BatchPredictor) classifierToUse)
                  .implementsMoreEfficientBatchPrediction()) {
                batchInst.add(instance);
                if (batchInst.numInstances() == batchSize) {
                  Instances toPred = new Instances(batchInst);
                  for (int i = 0; i < toPred.numInstances(); i++) {
                    toPred.instance(i).setClassMissing();
                  }
                  double[][] predictions =
                    ((BatchPredictor) classifierToUse)
                      .distributionsForInstances(toPred);
                  plotInstances.process(batchInst, predictions, eval);

                  if (outputPredictionsText) {
                    for (int kk = 0; kk < batchInst.numInstances(); kk++) {
                      classificationOutput.printClassification(predictions[kk],
                        batchInst.instance(kk), kk);
                    }
                  }
                  jj += batchInst.numInstances();
                  m_Log.statusMessage("Evaluating on test data. Processed "
                    + jj + " instances...");
                  batchInst.delete();
                }
              } else {
                plotInstances.process(instance, classifierToUse, eval);
                if (outputPredictionsText) {
                  classificationOutput.printClassification(classifierToUse,
                    instance, jj);
                }
              }
              if ((++jj % 100) == 0) {
                m_Log.statusMessage("Evaluating on test data. Processed " + jj
                  + " instances...");
              }
            }

            if (classifierToUse instanceof BatchPredictor
              && ((BatchPredictor) classifierToUse)
                .implementsMoreEfficientBatchPrediction()
              && batchInst.numInstances() > 0) {
              // finish the last batch

              Instances toPred = new Instances(batchInst);
              for (int i = 0; i < toPred.numInstances(); i++) {
                toPred.instance(i).setClassMissing();
              }

              double[][] predictions =
                ((BatchPredictor) classifierToUse)
                  .distributionsForInstances(toPred);
              plotInstances.process(batchInst, predictions, eval);

              if (outputPredictionsText) {
                for (int kk = 0; kk < batchInst.numInstances(); kk++) {
                  classificationOutput.printClassification(predictions[kk],
                    batchInst.instance(kk), kk);
                }
              }
            }

            if (outputPredictionsText) {
              classificationOutput.printFooter();
            }
            if (outputPredictionsText && classificationOutput.generatesOutput()) {
              outBuff.append("\n");
            }

            if (outputSummary) {
              outBuff.append(eval.toSummaryString(outputEntropy) + "\n");
            }

            if (userTestStructure.classAttribute().isNominal()) {

              if (outputPerClass) {
                outBuff.append(eval.toClassDetailsString() + "\n");
              }

              if (outputConfusion) {
                outBuff.append(eval.toMatrixString() + "\n");
              }
            }

            m_History.updateResult(name);
            m_Log.logMessage("Finished re-evaluation");
            m_Log.statusMessage("OK");
          } catch (Exception ex) {
            ex.printStackTrace();
            m_Log.logMessage(ex.getMessage());
            m_Log.statusMessage("See error log");

            ex.printStackTrace();
            m_Log.logMessage(ex.getMessage());
            JOptionPane.showMessageDialog(ClassifierPanel.this,
              "Problem evaluating classifier:\n" + ex.getMessage(),
              "Evaluate classifier", JOptionPane.ERROR_MESSAGE);
            m_Log.statusMessage("Problem evaluating classifier");
          } finally {
            try {
              if (classifierToUse instanceof PMMLClassifier) {
                // signal the end of the scoring run so
                // that the initialized state can be reset
                // (forces the field mapping to be recomputed
                // for the next scoring run).
                ((PMMLClassifier) classifierToUse).done();
              }

              if (plotInstances != null
                && plotInstances.getPlotInstances() != null
                && plotInstances.getPlotInstances().numInstances() > 0) {
                m_CurrentVis = new VisualizePanel();
                if (getMainApplication() != null) {
                  Settings settings = getMainApplication().getApplicationSettings();
                  m_CurrentVis.applySettings(settings,
                    weka.gui.explorer.VisualizePanel.ScatterDefaults.ID);
                }
                m_CurrentVis.setName(name + " ("
                  + userTestStructure.relationName() + ")");
                m_CurrentVis.setLog(m_Log);
                m_CurrentVis.addPlot(plotInstances.getPlotData(name));
                // m_CurrentVis.setColourIndex(plotInstances.getPlotInstances().classIndex()+1);
                m_CurrentVis.setColourIndex(plotInstances.getPlotInstances()
                  .classIndex());
                plotInstances.cleanUp();

                if (classifierToUse instanceof Drawable) {
                  try {
                    grph = ((Drawable) classifierToUse).graph();
                  } catch (Exception ex) {
                  }
                }

                if (saveVis) {
                  ArrayList<Object> vv = new ArrayList<Object>();
                  vv.add(classifier);
                  if (trainHeader != null) {
                    vv.add(trainHeader);
                  }
                  vv.add(m_CurrentVis);
                  if (grph != null) {
                    vv.add(grph);
                  }
                  if ((eval != null) && (eval.predictions() != null)) {
                    vv.add(eval.predictions());
                    vv.add(userTestStructure.classAttribute());
                  }
                  m_History.addObject(name, vv);
                } else {
                  ArrayList<Object> vv = new ArrayList<Object>();
                  vv.add(classifierToUse);
                  if (trainHeader != null) {
                    vv.add(trainHeader);
                  }
                  m_History.addObject(name, vv);
                }
              }
            } catch (Exception ex) {
              ex.printStackTrace();
            }
            if (isInterrupted()) {
              m_Log.logMessage("Interrupted reevaluate model");
              m_Log.statusMessage("Interrupted");
            }

            synchronized (this) {
              m_StartBut.setEnabled(true);
              m_StopBut.setEnabled(false);
              m_RunThread = null;
            }

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
   * updates the capabilities filter of the GOE.
   * 
   * @param filter the new filter to use
   */
  protected void updateCapabilitiesFilter(Capabilities filter) {
    Instances tempInst;
    Capabilities filterClass;

    if (filter == null) {
      m_ClassifierEditor.setCapabilitiesFilter(new Capabilities(null));
      return;
    }

    if (!ExplorerDefaults.getInitGenericObjectEditorFilter()) {
      tempInst = new Instances(m_Instances, 0);
    } else {
      tempInst = new Instances(m_Instances);
    }
    tempInst.setClassIndex(m_ClassCombo.getSelectedIndex());

    try {
      filterClass = Capabilities.forInstances(tempInst);
    } catch (Exception e) {
      filterClass = new Capabilities(null);
    }

    // set new filter
    m_ClassifierEditor.setCapabilitiesFilter(filterClass);

    // Check capabilities
    m_StartBut.setEnabled(true);
    Capabilities currentFilter = m_ClassifierEditor.getCapabilitiesFilter();
    Classifier classifier = (Classifier) m_ClassifierEditor.getValue();
    Capabilities currentSchemeCapabilities = null;
    if (classifier != null && currentFilter != null
      && (classifier instanceof CapabilitiesHandler)) {
      currentSchemeCapabilities =
        ((CapabilitiesHandler) classifier).getCapabilities();

      if (!currentSchemeCapabilities.supportsMaybe(currentFilter)
        && !currentSchemeCapabilities.supports(currentFilter)) {
        m_StartBut.setEnabled(false);
      }
    }
  }

  /**
   * method gets called in case of a change event.
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
   * about changes in the data).
   * 
   * @param parent the parent frame
   */
  @Override
  public void setExplorer(Explorer parent) {
    m_Explorer = parent;
  }

  /**
   * returns the parent Explorer frame.
   * 
   * @return the parent
   */
  @Override
  public Explorer getExplorer() {
    return m_Explorer;
  }

  /**
   * Returns the title for the tab in the Explorer.
   * 
   * @return the title of this tab
   */
  @Override
  public String getTabTitle() {
    return "Classify";
  }

  /**
   * Returns the tooltip for the tab in the Explorer.
   * 
   * @return the tooltip of this tab
   */
  @Override
  public String getTabTitleToolTip() {
    return "Classify instances";
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
    return new ClassifierPanelDefaults();
  }

  @Override
  public boolean okToBeActive() {
    return m_Instances != null;
  }

  @Override
  public void setActive(boolean active) {
    super.setActive(active);
    if (m_isActive) {
      // make sure initial settings get applied
      settingsChanged();
    }
  }

  @Override
  public void settingsChanged() {
    if (getMainApplication() != null) {
      if (!m_initialSettingsSet) {
        Object initialC =
          getMainApplication().getApplicationSettings().getSetting(
            getPerspectiveID(), ClassifierPanelDefaults.CLASSIFIER_KEY,
            ClassifierPanelDefaults.CLASSIFIER, Environment.getSystemWide());
        m_ClassifierEditor.setValue(initialC);

        TestMode initialTestMode =
          getMainApplication().getApplicationSettings().getSetting(
            getPerspectiveID(), ClassifierPanelDefaults.TEST_MODE_KEY,
            ClassifierPanelDefaults.TEST_MODE, Environment.getSystemWide());

        m_CVBut.setSelected(initialTestMode == TestMode.CROSS_VALIDATION);
        m_PercentBut.setSelected(initialTestMode == TestMode.PERCENTAGE_SPLIT);
        m_TrainBut.setSelected(initialTestMode == TestMode.USE_TRAINING_SET);
        m_TestSplitBut
          .setSelected(initialTestMode == TestMode.SEPARATE_TEST_SET);
        m_CVText.setEnabled(m_CVBut.isSelected());
        m_PercentText.setEnabled(m_PercentBut.isSelected());
        m_CVText.setText(""
          + getMainApplication().getApplicationSettings().getSetting(
            getPerspectiveID(),
            ClassifierPanelDefaults.CROSS_VALIDATION_FOLDS_KEY,
            ClassifierPanelDefaults.CROSS_VALIDATION_FOLDS,
            Environment.getSystemWide()));
        m_PercentText.setText(""
          + getMainApplication().getApplicationSettings().getSetting(
            getPerspectiveID(), ClassifierPanelDefaults.PERCENTAGE_SPLIT_KEY,
            ClassifierPanelDefaults.PERCENTAGE_SPLIT,
            Environment.getSystemWide()));

        // TODO these widgets will disapear, as the "More options" dialog will
        // not be necessary
        m_OutputModelBut.setSelected(getMainApplication()
          .getApplicationSettings().getSetting(getPerspectiveID(),
            ClassifierPanelDefaults.OUTPUT_MODEL_KEY,
            ClassifierPanelDefaults.OUTPUT_MODEL, Environment.getSystemWide()));
        m_OutputPerClassBut.setSelected(getMainApplication()
          .getApplicationSettings().getSetting(getPerspectiveID(),
            ClassifierPanelDefaults.OUTPUT_PER_CLASS_STATS_KEY,
            ClassifierPanelDefaults.OUTPUT_PER_CLASS_STATS,
            Environment.getSystemWide()));
        m_OutputEntropyBut.setSelected(getMainApplication()
          .getApplicationSettings().getSetting(getPerspectiveID(),
            ClassifierPanelDefaults.OUTPUT_ENTROPY_EVAL_METRICS_KEY,
            ClassifierPanelDefaults.OUTPUT_ENTROPY_EVAL_METRICS,
            Environment.getSystemWide()));
        m_OutputConfusionBut.setSelected(getMainApplication()
          .getApplicationSettings().getSetting(getPerspectiveID(),
            ClassifierPanelDefaults.OUTPUT_CONFUSION_MATRIX_KEY,
            ClassifierPanelDefaults.OUTPUT_CONFUSION_MATRIX,
            Environment.getSystemWide()));
        m_StorePredictionsBut.setSelected(getMainApplication()
          .getApplicationSettings().getSetting(getPerspectiveID(),
            ClassifierPanelDefaults.STORE_PREDICTIONS_FOR_VIS_KEY,
            ClassifierPanelDefaults.STORE_PREDICTIONS_FOR_VIS,
            Environment.getSystemWide()));
        m_errorPlotPointSizeProportionalToMargin
          .setSelected(getMainApplication().getApplicationSettings()
            .getSetting(getPerspectiveID(),
              ClassifierPanelDefaults.ERROR_PLOT_POINT_SIZE_PROP_TO_MARGIN_KEY,
              ClassifierPanelDefaults.ERROR_PLOT_POINT_SIZE_PROP_TO_MARGIN,
              Environment.getSystemWide()));
        m_RandomSeedText.setText(""
          + getMainApplication().getApplicationSettings().getSetting(
            getPerspectiveID(), ClassifierPanelDefaults.RANDOM_SEED_KEY,
            ClassifierPanelDefaults.RANDOM_SEED, Environment.getSystemWide()));
      }
      m_initialSettingsSet = true;

      Font outputFont =
        getMainApplication().getApplicationSettings().getSetting(
          getPerspectiveID(), ClassifierPanelDefaults.OUTPUT_FONT_KEY,
          ClassifierPanelDefaults.OUTPUT_FONT, Environment.getSystemWide());
      m_OutText.setFont(outputFont);
      m_History.setFont(outputFont);
      Color textColor =
        getMainApplication().getApplicationSettings().getSetting(
          getPerspectiveID(), ClassifierPanelDefaults.OUTPUT_TEXT_COLOR_KEY,
          ClassifierPanelDefaults.OUTPUT_TEXT_COLOR,
          Environment.getSystemWide());
      m_OutText.setForeground(textColor);
      m_History.setForeground(textColor);
      Color outputBackgroundColor =
        getMainApplication().getApplicationSettings().getSetting(
          getPerspectiveID(),
          ClassifierPanelDefaults.OUTPUT_BACKGROUND_COLOR_KEY,
          ClassifierPanelDefaults.OUTPUT_BACKGROUND_COLOR,
          Environment.getSystemWide());
      m_OutText.setBackground(outputBackgroundColor);
      m_History.setBackground(outputBackgroundColor);
    }
  }

  public static enum TestMode {
    CROSS_VALIDATION, PERCENTAGE_SPLIT, USE_TRAINING_SET, SEPARATE_TEST_SET;
  }

  /**
   * Default settings for the classifier panel
   */
  protected static final class ClassifierPanelDefaults extends Defaults {
    public static final String ID = "weka.gui.explorer.classifierpanel";

    protected static final Settings.SettingKey CLASSIFIER_KEY =
      new Settings.SettingKey(ID + ".initialClassifier", "Initial classifier",
        "On startup, set this classifier as the default one");
    protected static final Classifier CLASSIFIER = new ZeroR();

    protected static final Settings.SettingKey TEST_MODE_KEY =
      new Settings.SettingKey(ID + ".initialTestMode", "Default test mode", "");
    protected static final TestMode TEST_MODE = TestMode.CROSS_VALIDATION;

    protected static final Settings.SettingKey CROSS_VALIDATION_FOLDS_KEY =
      new Settings.SettingKey(ID + ".crossValidationFolds",
        "Default cross validation folds", "");
    protected static final int CROSS_VALIDATION_FOLDS = 10;

    protected static final Settings.SettingKey PERCENTAGE_SPLIT_KEY =
      new Settings.SettingKey(ID + ".percentageSplit",
        "Default percentage split", "");
    protected static final int PERCENTAGE_SPLIT = 66;

    protected static final Settings.SettingKey OUTPUT_MODEL_KEY =
      new Settings.SettingKey(ID + ".outputModel", "Output model obtained from"
        + " the full training set", "");
    protected static final boolean OUTPUT_MODEL = true;

    protected static final Settings.SettingKey OUTPUT_PER_CLASS_STATS_KEY =
      new Settings.SettingKey(ID + ".outputPerClassStats",
        "Output per-class statistics", "");
    protected static final boolean OUTPUT_PER_CLASS_STATS = true;

    protected static final Settings.SettingKey OUTPUT_ENTROPY_EVAL_METRICS_KEY =
      new Settings.SettingKey(ID + ".outputEntropyMetrics", "Output entropy "
        + "evaluation metrics", "");
    protected static final boolean OUTPUT_ENTROPY_EVAL_METRICS = false;

    protected static final Settings.SettingKey OUTPUT_CONFUSION_MATRIX_KEY =
      new Settings.SettingKey(ID + ".outputConfusionMatrix",
        "Output confusion " + "matrix", "");
    protected static final boolean OUTPUT_CONFUSION_MATRIX = true;

    protected static final Settings.SettingKey STORE_PREDICTIONS_FOR_VIS_KEY =
      new Settings.SettingKey(ID + ".storePredsForVis", "Store predictions for"
        + " visualization", "");
    protected static final boolean STORE_PREDICTIONS_FOR_VIS = true;

    /*
     * protected static final Settings.SettingKey OUTPUT_PREDICTIONS_KEY = new
     * Settings.SettingKey(ID + ".outputPredictions", "Output predictions", "");
     * protected static final boolean OUTPUT_PREDICTIONS = false;
     */

    protected static final Settings.SettingKey PREDICTION_FORMATTER_KEY =
      new Settings.SettingKey(ID + ".predictionFormatter",
        "Prediction formatter", "");
    protected static final AbstractOutput PREDICTION_FORMATTER = new Null();

    protected static final Settings.SettingKey ERROR_PLOT_POINT_SIZE_PROP_TO_MARGIN_KEY =
      new Settings.SettingKey(
        ID + ".errorPlotPointSizePropToMargin",
        "Error plot point size proportional to margin",
        "In classifier error plots the point size will be set proportional to "
          + "the absolute value of the prediction margin (affects classification "
          + "only)");
    protected static final boolean ERROR_PLOT_POINT_SIZE_PROP_TO_MARGIN = false;

    protected static final Settings.SettingKey COST_SENSITIVE_EVALUATION_KEY =
      new Settings.SettingKey(ID + ".costSensitiveEval",
        "Cost sensitive evaluation",
        "Evaluate errors with respect to a cost matrix");
    protected static final boolean COST_SENSITIVE_EVALUATION = false;

    protected static final Settings.SettingKey COST_MATRIX_KEY =
      new Settings.SettingKey(ID + ".costMatrix",
        "Cost matrix for cost sensitive " + "evaluation", "");
    protected static final CostMatrix COST_MATRIX = new CostMatrix(1);

    protected static final Settings.SettingKey RANDOM_SEED_KEY =
      new Settings.SettingKey(ID + ".randomSeed",
        "Random seed for XVal / % Split", "The seed for randomization");
    protected static final int RANDOM_SEED = 1;

    protected static final Settings.SettingKey PRESERVE_ORDER_FOR_PERCENT_SPLIT_KEY =
      new Settings.SettingKey(ID + ".preserveOrder",
        "Preserve order for % Split",
        "Preserves the order in a percentage split");
    protected static final boolean PRESERVE_ORDER_FOR_PERCENT_SPLIT = false;

    protected static final Settings.SettingKey SOURCE_CODE_CLASS_NAME_KEY =
      new Settings.SettingKey(ID + ".sourceCodeClassName", "Source code class "
        + "name", "Default classname of a Sourcable classifier");
    protected static final String SOURCE_CODE_CLASS_NAME = "WekaClassifier";

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
    private static final long serialVersionUID = 7109938811150596359L;

    public ClassifierPanelDefaults() {
      super(ID);

      m_defaults.put(CLASSIFIER_KEY, CLASSIFIER);
      m_defaults.put(TEST_MODE_KEY, TEST_MODE);
      m_defaults.put(CROSS_VALIDATION_FOLDS_KEY, CROSS_VALIDATION_FOLDS);
      m_defaults.put(PERCENTAGE_SPLIT_KEY, PERCENTAGE_SPLIT);
      m_defaults.put(OUTPUT_MODEL_KEY, OUTPUT_MODEL);
      m_defaults.put(OUTPUT_PER_CLASS_STATS_KEY, OUTPUT_PER_CLASS_STATS);
      m_defaults.put(OUTPUT_ENTROPY_EVAL_METRICS_KEY,
        OUTPUT_ENTROPY_EVAL_METRICS);
      m_defaults.put(OUTPUT_CONFUSION_MATRIX_KEY, OUTPUT_CONFUSION_MATRIX);
      m_defaults.put(STORE_PREDICTIONS_FOR_VIS_KEY, STORE_PREDICTIONS_FOR_VIS);
      // m_defaults.put(OUTPUT_PREDICTIONS_KEY, OUTPUT_PREDICTIONS);
      m_defaults.put(PREDICTION_FORMATTER_KEY, PREDICTION_FORMATTER);
      m_defaults.put(ERROR_PLOT_POINT_SIZE_PROP_TO_MARGIN_KEY,
        ERROR_PLOT_POINT_SIZE_PROP_TO_MARGIN);
      m_defaults.put(COST_SENSITIVE_EVALUATION_KEY, COST_SENSITIVE_EVALUATION);
      m_defaults.put(COST_MATRIX_KEY, COST_MATRIX);
      m_defaults.put(RANDOM_SEED_KEY, RANDOM_SEED);
      m_defaults.put(PRESERVE_ORDER_FOR_PERCENT_SPLIT_KEY,
        PRESERVE_ORDER_FOR_PERCENT_SPLIT);
      m_defaults.put(SOURCE_CODE_CLASS_NAME_KEY, SOURCE_CODE_CLASS_NAME);
      m_defaults.put(OUTPUT_FONT_KEY, OUTPUT_FONT);
      m_defaults.put(OUTPUT_TEXT_COLOR_KEY, OUTPUT_TEXT_COLOR);
      m_defaults.put(OUTPUT_BACKGROUND_COLOR_KEY, OUTPUT_BACKGROUND_COLOR);
    }
  }

  /**
   * Tests out the classifier panel from the command line.
   * 
   * @param args may optionally contain the name of a dataset to load.
   */
  public static void main(String[] args) {

    try {
      final javax.swing.JFrame jf =
        new javax.swing.JFrame("Weka Explorer: Classifier");
      jf.getContentPane().setLayout(new BorderLayout());
      final ClassifierPanel sp = new ClassifierPanel();
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
