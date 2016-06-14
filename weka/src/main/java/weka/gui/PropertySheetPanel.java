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
 *    PropertySheet.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.BeanInfo;
import java.beans.Beans;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.beans.PropertyVetoException;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.gui.beans.GOECustomizer;

/**
 * Displays a property sheet where (supported) properties of the target object
 * may be edited.
 * 
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 12016 $
 */
public class PropertySheetPanel extends JPanel implements
  PropertyChangeListener, EnvironmentHandler {

  /** for serialization. */
  private static final long serialVersionUID = -8939835593429918345L;

  /**
   * A specialized dialog for displaying the capabilities.
   */
  protected class CapabilitiesHelpDialog extends JDialog implements
    PropertyChangeListener {

    /** for serialization. */
    private static final long serialVersionUID = -1404770987103289858L;

    /** the dialog itself. */
    private CapabilitiesHelpDialog m_Self;

    /**
     * default constructor.
     *
     * @param owner the owning frame
     */
    public CapabilitiesHelpDialog(Frame owner) {
      super(owner);

      initialize();
    }

    /**
     * default constructor.
     *
     * @param owner the owning dialog
     */
    public CapabilitiesHelpDialog(Dialog owner) {
      super(owner);

      initialize();
    }

    /**
     * Initializes the dialog.
     */
    protected void initialize() {
      setTitle("Information about Capabilities");

      m_Self = this;

      m_CapabilitiesText = new JTextArea();
      m_CapabilitiesText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      m_CapabilitiesText.setLineWrap(true);
      m_CapabilitiesText.setWrapStyleWord(true);
      m_CapabilitiesText.setEditable(false);
      updateText();
      addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          m_Self.dispose();
          if (m_CapabilitiesDialog == m_Self) {
            m_CapabilitiesBut.setEnabled(true);
          }
        }
      });
      getContentPane().setLayout(new BorderLayout());
      getContentPane().add(new JScrollPane(m_CapabilitiesText),
        BorderLayout.CENTER);
      pack();
    }

    /**
     * updates the content of the capabilities help dialog.
     */
    protected void updateText() {
      StringBuffer helpText = new StringBuffer();

      if (m_Target instanceof CapabilitiesHandler) {
        helpText.append(addCapabilities("CAPABILITIES",
          ((CapabilitiesHandler) m_Target).getCapabilities()));
      }

      if (m_Target instanceof MultiInstanceCapabilitiesHandler) {
        helpText.append(addCapabilities("MI CAPABILITIES",
          ((MultiInstanceCapabilitiesHandler) m_Target)
            .getMultiInstanceCapabilities()));
      }

      m_CapabilitiesText.setText(helpText.toString());
      m_CapabilitiesText.setCaretPosition(0);
    }

    /**
     * This method gets called when a bound property is changed.
     *
     * @param evt the change event
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      updateText();
    }
  }

  /**
   * returns a comma-separated list of all the capabilities.
   *
   * @param c the capabilities to get a string representation from
   * @return the string describing the capabilities
   */
  public static String listCapabilities(Capabilities c) {
    String result;
    Iterator<Capability> iter;

    result = "";
    iter = c.capabilities();
    while (iter.hasNext()) {
      if (result.length() != 0) {
        result += ", ";
      }
      result += iter.next().toString();
    }

    return result;
  }

  /**
   * generates a string from the capapbilities, suitable to add to the help
   * text.
   *
   * @param title the title for the capabilities
   * @param c the capabilities
   * @return a string describing the capabilities
   */
  public static String addCapabilities(String title, Capabilities c) {
    String result;
    String caps;

    result = title + "\n";

    // class
    caps = listCapabilities(c.getClassCapabilities());
    if (caps.length() != 0) {
      result += "Class -- ";
      result += caps;
      result += "\n\n";
    }

    // attribute
    caps = listCapabilities(c.getAttributeCapabilities());
    if (caps.length() != 0) {
      result += "Attributes -- ";
      result += caps;
      result += "\n\n";
    }

    // other capabilities
    caps = listCapabilities(c.getOtherCapabilities());
    if (caps.length() != 0) {
      result += "Other -- ";
      result += caps;
      result += "\n\n";
    }

    // additional stuff
    result += "Additional\n";
    result += "min # of instances: " + c.getMinimumNumberInstances() + "\n";
    result += "\n";

    return result;
  }

  /** The target object being edited. */
  private Object m_Target;

  /** Whether to show the about panel */
  private boolean m_showAboutPanel = true;

  /** Holds the customizer (if one exists) for the object being edited */
  private GOECustomizer m_Customizer;

  /** Holds properties of the target. */
  private PropertyDescriptor m_Properties[];

  /** Holds the methods of the target. */
  private MethodDescriptor m_Methods[];

  /** Holds property editors of the object. */
  private PropertyEditor m_Editors[];

  /** Holds current object values for each property. */
  private Object m_Values[];

  /** Stores GUI components containing each editing component. */
  private JComponent m_Views[];

  /** The labels for each property. */
  private JLabel m_Labels[];

  /** The tool tip text for each property. */
  private String m_TipTexts[];

  /** StringBuffer containing help text for the object being edited. */
  private StringBuffer m_HelpText;

  /** Help dialog. */
  private JDialog m_HelpDialog;

  /** Capabilities Help dialog. */
  private CapabilitiesHelpDialog m_CapabilitiesDialog;

  /** Button to pop up the full help text in a separate dialog. */
  private JButton m_HelpBut;

  /** Button to pop up the capabilities in a separate dialog. */
  private JButton m_CapabilitiesBut;

  /** the TextArea of the Capabilities help dialog. */
  private JTextArea m_CapabilitiesText;

  /** A count of the number of properties we have an editor for. */
  private int m_NumEditable = 0;

  /**
   * The panel holding global info and help, if provided by the object being
   * editied.
   */
  private JPanel m_aboutPanel;

  /** Environment variables to pass on to any editors that can handle them */
  private transient Environment m_env;

  /**
   * Whether to use EnvironmentField and FileEnvironmentField for text and
   * file properties respectively */
  private boolean m_useEnvironmentPropertyEditors;

  /**
   * Creates the property sheet panel with an about panel.
   */
  public PropertySheetPanel() {

    // setBorder(BorderFactory.createLineBorder(Color.red));
    setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
    m_env = Environment.getSystemWide();
  }

  /**
   * Creates the property sheet panel
   *
   * @param showAboutPanel true if the about panel is to be shown
   */
  public PropertySheetPanel(boolean showAboutPanel) {
    super();
    m_showAboutPanel = showAboutPanel;
  }

  /**
   * Set whether to use environment property editors for string and
   * file properties
   *
   * @param u true to use environment property editors
   */
  public void setUseEnvironmentPropertyEditors(boolean u) {
    m_useEnvironmentPropertyEditors = u;
  }

  /**
   * Get whether to use environment property editors for string and
   * file properties
   *
   * @return true to use environment property editors
   */
  public boolean getUseEnvironmentPropertyEditors() {
    return m_useEnvironmentPropertyEditors;
  }

  /**
   * Return the panel containing global info and help for the object being
   * edited. May return null if the edited object provides no global info or tip
   * text.
   *
   * @return the about panel.
   */
  public JPanel getAboutPanel() {
    return m_aboutPanel;
  }

  /** A support object for handling property change listeners. */
  private final PropertyChangeSupport support = new PropertyChangeSupport(this);

  /**
   * Updates the property sheet panel with a changed property and also passed
   * the event along.
   *
   * @param evt a value of type 'PropertyChangeEvent'
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    wasModified(evt); // Let our panel update before guys downstream
    support.firePropertyChange("", null, null);
  }

  /**
   * Adds a PropertyChangeListener.
   *
   * @param l a value of type 'PropertyChangeListener'
   */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {

    if (support != null && l != null) {
      support.addPropertyChangeListener(l);
    }
  }

  /**
   * Removes a PropertyChangeListener.
   *
   * @param l a value of type 'PropertyChangeListener'
   */
  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {

    if (support != null && l != null) {
      support.removePropertyChangeListener(l);
    }
  }

  /**
   * Sets a new target object for customisation.
   *
   * @param targ a value of type 'Object'
   */
  public synchronized void setTarget(Object targ) {

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    // used to offset the components for the properties of targ
    // if there happens to be globalInfo available in targ
    int componentOffset = 0;

    // Close any child windows at this point
    removeAll();

    setLayout(new BorderLayout());
    JPanel scrollablePanel = new JPanel();
    JScrollPane scrollPane = new JScrollPane(scrollablePanel);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    add(scrollPane, BorderLayout.CENTER);

    GridBagLayout gbLayout = new GridBagLayout();

    scrollablePanel.setLayout(gbLayout);
    setVisible(false);
    m_NumEditable = 0;
    m_Target = targ;
    Class<?> custClass = null;
    try {
      BeanInfo bi = Introspector.getBeanInfo(m_Target.getClass());
      m_Properties = bi.getPropertyDescriptors();
      m_Methods = bi.getMethodDescriptors();
      custClass = Introspector.getBeanInfo(m_Target.getClass())
        .getBeanDescriptor().getCustomizerClass();
    } catch (IntrospectionException ex) {
      System.err.println("PropertySheet: Couldn't introspect");
      return;
    }

    JTextArea jt = new JTextArea();
    m_HelpText = null;
    // Look for a globalInfo method that returns a string
    // describing the target
    Object args[] = {};
    boolean firstTip = true;
    StringBuffer optionsBuff = new StringBuffer();
    for (MethodDescriptor m_Method : m_Methods) {
      String name = m_Method.getDisplayName();
      Method meth = m_Method.getMethod();
      OptionMetadata o = meth.getAnnotation(OptionMetadata.class);

      if (name.endsWith("TipText") || o != null) {
        if (meth.getReturnType().equals(String.class) || o != null) {
          try {
            String tempTip = o != null ? o.description() : (String) (meth.invoke(m_Target, args));
            // int ci = tempTip.indexOf('.');
            name = o != null ? o.displayName() : name;

            if (firstTip) {
              optionsBuff.append("OPTIONS\n");
              firstTip = false;
            }
            tempTip = tempTip.replace("<html>", "").replace("</html>", "")
              .replace("<br>", "\n").replace("<p>", "\n\n");
            optionsBuff.append(name.replace("TipText", "")).append(" -- ");
            optionsBuff.append(tempTip).append("\n\n");
            // jt.setText(m_HelpText.toString());

          } catch (Exception ex) {

          }
          // break;
        }
      }

      if (name.equals("globalInfo")) {
        if (meth.getReturnType().equals(String.class)) {
          try {
            // Object args[] = { };
            String globalInfo = (String) (meth.invoke(m_Target, args));
            String summary = globalInfo;
            int ci = globalInfo.indexOf('.');
            if (ci != -1) {
              summary = globalInfo.substring(0, ci + 1);
            }
            final String className = targ.getClass().getName();
            m_HelpText = new StringBuffer("NAME\n");
            m_HelpText.append(className).append("\n\n");
            m_HelpText.append("SYNOPSIS\n").append(globalInfo).append("\n\n");
            m_HelpBut = new JButton("More");
            m_HelpBut.setToolTipText("More information about " + className);

            m_HelpBut.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent a) {
                openHelpFrame();
                m_HelpBut.setEnabled(false);
              }
            });

            if (m_Target instanceof CapabilitiesHandler) {
              m_CapabilitiesBut = new JButton("Capabilities");
              m_CapabilitiesBut.setToolTipText("The capabilities of "
                + className);

              m_CapabilitiesBut.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent a) {
                  openCapabilitiesHelpDialog();
                  m_CapabilitiesBut.setEnabled(false);
                }
              });
            } else {
              m_CapabilitiesBut = null;
            }

            jt.setColumns(30);
            jt.setFont(new Font("SansSerif", Font.PLAIN, 12));
            jt.setEditable(false);
            jt.setLineWrap(true);
            jt.setWrapStyleWord(true);
            jt.setText(summary);
            jt.setBackground(getBackground());
            JPanel jp = new JPanel();
            jp.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder("About"),
              BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            jp.setLayout(new BorderLayout());
            jp.add(jt, BorderLayout.CENTER);
            JPanel p2 = new JPanel();
            p2.setLayout(new BorderLayout());
            p2.add(m_HelpBut, BorderLayout.NORTH);
            if (m_CapabilitiesBut != null) {
              JPanel p3 = new JPanel();
              p3.setLayout(new BorderLayout());
              p3.add(m_CapabilitiesBut, BorderLayout.NORTH);
              p2.add(p3, BorderLayout.CENTER);
            }
            jp.add(p2, BorderLayout.EAST);
            GridBagConstraints gbConstraints = new GridBagConstraints();
            // gbConstraints.anchor = GridBagConstraints.EAST;
            gbConstraints.fill = GridBagConstraints.BOTH;
            // gbConstraints.gridy = 0; gbConstraints.gridx = 0;
            gbConstraints.gridwidth = 2;
            gbConstraints.insets = new Insets(0, 5, 0, 5);
            gbLayout.setConstraints(jp, gbConstraints);
            m_aboutPanel = jp;
            if (m_showAboutPanel) {
              scrollablePanel.add(m_aboutPanel);
            }
            componentOffset = 1;

            // break;
          } catch (Exception ex) {

          }
        }
      }
    }

    if (m_HelpText != null) {
      m_HelpText.append(optionsBuff.toString());
    }

    if (custClass != null) {
      // System.out.println("**** We've found a customizer for this object!");
      try {
        Object customizer = custClass.newInstance();

        if (customizer instanceof JComponent
          && customizer instanceof GOECustomizer) {
          m_Customizer = (GOECustomizer) customizer;

          m_Customizer.dontShowOKCancelButtons();
          m_Customizer.setObject(m_Target);

          GridBagConstraints gbc = new GridBagConstraints();
          gbc.fill = GridBagConstraints.BOTH;
          gbc.gridwidth = 2;
          gbc.gridy = componentOffset;
          gbc.gridx = 0;
          gbc.insets = new Insets(0, 5, 0, 5);
          gbLayout.setConstraints((JComponent) m_Customizer, gbc);
          scrollablePanel.add((JComponent) m_Customizer);

          validate();

          // sometimes, the calculated dimensions seem to be too small and the
          // scrollbars show up, though there is still plenty of space on the
          // screen. hence we increase the dimensions a bit to fix this.
          Dimension dim = scrollablePanel.getPreferredSize();
          dim.height += 20;
          dim.width += 20;
          scrollPane.setPreferredSize(dim);
          validate();

          setVisible(true);
          return;
        }
      } catch (InstantiationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    int[] propOrdering = new int[m_Properties.length];
    for (int i = 0; i < propOrdering.length; i++) {
      propOrdering[i] = Integer.MAX_VALUE;
    }
    for (int i = 0; i < m_Properties.length; i++) {
      Method getter = m_Properties[i].getReadMethod();
      Method setter = m_Properties[i].getWriteMethod();
      if (getter == null || setter == null) {
        continue;
      }
      List<Annotation> annotations = new ArrayList<Annotation>();
      if (setter.getDeclaredAnnotations().length > 0) {
        annotations.addAll(Arrays.asList(setter.getDeclaredAnnotations()));
      }
      if (getter.getDeclaredAnnotations().length > 0) {
        annotations.addAll(Arrays.asList(getter.getDeclaredAnnotations()));
      }
      for (Annotation a : annotations) {
        if (a instanceof OptionMetadata) {
          propOrdering[i] = ((OptionMetadata)a).displayOrder();
          break;
        }
      }
    }
    int[] sortedPropOrderings = Utils.sort(propOrdering);
    m_Editors = new PropertyEditor[m_Properties.length];
    m_Values = new Object[m_Properties.length];
    m_Views = new JComponent[m_Properties.length];
    m_Labels = new JLabel[m_Properties.length];
    m_TipTexts = new String[m_Properties.length];
    // boolean firstTip = true;
    for (int i = 0; i < m_Properties.length; i++) {

      // Don't display hidden or expert properties.
      if (m_Properties[sortedPropOrderings[i]].isHidden() ||
        m_Properties[sortedPropOrderings[i]].isExpert()) {
        continue;
      }

      String name = m_Properties[sortedPropOrderings[i]].getDisplayName();
      String origName = name;
      Class<?> type = m_Properties[sortedPropOrderings[i]].getPropertyType();
      Method getter = m_Properties[sortedPropOrderings[i]].getReadMethod();
      Method setter = m_Properties[sortedPropOrderings[i]].getWriteMethod();

      // Only display read/write properties.
      if (getter == null || setter == null) {
        continue;
      }

      List<Annotation> annotations = new ArrayList<Annotation>();
      if (setter.getDeclaredAnnotations().length > 0) {
        annotations.addAll(Arrays.asList(setter.getDeclaredAnnotations()));
      }
      if (getter.getDeclaredAnnotations().length > 0) {
        annotations.addAll(Arrays.asList(getter.getDeclaredAnnotations()));
      }

      boolean skip = false;
      boolean password = false;
      FilePropertyMetadata fileProp = null;
      for (Annotation a : annotations) {
        if (a instanceof ProgrammaticProperty) {
          skip = true; // skip property that is only supposed to be manipulated programatically
          break;
        }

        if (a instanceof OptionMetadata) {
          name = ((OptionMetadata) a).displayName();
          String tempTip = ((OptionMetadata)a).description();
          int ci = tempTip.indexOf( '.' );
          if ( ci < 0 ) {
            m_TipTexts[sortedPropOrderings[i]] = tempTip;
          } else {
            m_TipTexts[sortedPropOrderings[i]] = tempTip.substring( 0, ci );
          }
        }

        if (a instanceof PasswordProperty) {
          password = true;
        }

        if (a instanceof FilePropertyMetadata) {
          fileProp = (FilePropertyMetadata) a;
        }
      }
      if (skip) {
        continue;
      }


      JComponent view = null;

      try {
        // Object args[] = { };
        Object value = getter.invoke(m_Target, args);
        m_Values[sortedPropOrderings[i]] = value;

        PropertyEditor editor = null;
        Class<?> pec = m_Properties[sortedPropOrderings[i]].getPropertyEditorClass();
        if (pec != null) {
          try {
            editor = (PropertyEditor) pec.newInstance();
          } catch (Exception ex) {
            // Drop through.
          }
        }
        if (editor == null) {
          if (password && String.class.isAssignableFrom(type)) {
            editor = new PasswordField();
          } else if (m_useEnvironmentPropertyEditors && String.class.isAssignableFrom(
            type)) {
            editor = new EnvironmentField();
          } else if ((m_useEnvironmentPropertyEditors || fileProp != null) && File.class.isAssignableFrom(
            type)) {
            if (fileProp != null) {
              editor = new FileEnvironmentField("", fileProp.fileChooserDialogType(), fileProp.directoriesOnly());
            } else {
              editor = new FileEnvironmentField();
            }
          } else {
            editor = PropertyEditorManager.findEditor(type);
          }
        }
        m_Editors[sortedPropOrderings[i]] = editor;

        // If we can't edit this component, skip it.
        if (editor == null) {
          // If it's a user-defined property we give a warning.
          // String getterClass = m_Properties[i].getReadMethod()
          // .getDeclaringClass().getName();
          /*
           * System.err.println("Warning: Can't find public property editor" +
           * " for property \"" + name + "\" (class \"" + type.getName() +
           * "\").  Skipping.");
           */
          continue;
        }
        if (editor instanceof GenericObjectEditor) {
          ((GenericObjectEditor) editor).setClassType(type);
        }

        if (editor instanceof EnvironmentHandler) {
          ((EnvironmentHandler) editor).setEnvironment(m_env);
        }

        // Don't try to set null values:
        if (value == null) {
          // If it's a user-defined property we give a warning.
          // String getterClass = m_Properties[i].getReadMethod()
          // .getDeclaringClass().getName();
          /*
           * if (getterClass.indexOf("java.") != 0) {
           * System.err.println("Warning: Property \"" + name +
           * "\" has null initial value.  Skipping."); }
           */
          continue;
        }

        editor.setValue(value);

        if (m_TipTexts[sortedPropOrderings[i]] == null) {
          // now look for a TipText method for this property
          String tipName = origName + "TipText";
          for ( MethodDescriptor m_Method : m_Methods ) {
            String mname = m_Method.getDisplayName();
            Method meth = m_Method.getMethod();
            if ( mname.equals( tipName ) ) {
              if ( meth.getReturnType().equals( String.class ) ) {
                try {
                  String tempTip = (String) ( meth.invoke( m_Target, args ) );
                  int ci = tempTip.indexOf( '.' );
                  if ( ci < 0 ) {
                    m_TipTexts[sortedPropOrderings[i]] = tempTip;
                  } else {
                    m_TipTexts[sortedPropOrderings[i]] = tempTip.substring( 0, ci );
                  }
                /*
                 * if (m_HelpText != null) { if (firstTip) {
                 * m_HelpText.append("OPTIONS\n"); firstTip = false; }
                 * m_HelpText.append(name).append(" -- ");
                 * m_HelpText.append(tempTip).append("\n\n");
                 * //jt.setText(m_HelpText.toString()); }
                 */
                } catch ( Exception ex ) {

                }
                break;
              }
            }
          }
        }

        // Now figure out how to display it...
        if (editor.isPaintable() && editor.supportsCustomEditor()) {
          view = new PropertyPanel(editor);
        } else if (editor.supportsCustomEditor()
          && (editor.getCustomEditor() instanceof JComponent)) {
          view = (JComponent) editor.getCustomEditor();
        } else if (editor.getTags() != null) {
          view = new PropertyValueSelector(editor);
        } else if (editor.getAsText() != null) {
          view = new PropertyText(editor);
        } else {
          System.err.println("Warning: Property \"" + name
            + "\" has non-displayabale editor.  Skipping.");
          continue;
        }

        editor.addPropertyChangeListener(this);

      } catch (InvocationTargetException ex) {
        System.err.println("Skipping property " + name
          + " ; exception on target: " + ex.getTargetException());
        ex.getTargetException().printStackTrace();
        continue;
      } catch (Exception ex) {
        System.err.println("Skipping property " + name + " ; exception: " + ex);
        ex.printStackTrace();
        continue;
      }

      m_Labels[sortedPropOrderings[i]] = new JLabel(name, SwingConstants.RIGHT);
      m_Labels[sortedPropOrderings[i]].setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 5));
      m_Views[sortedPropOrderings[i]] = view;
      GridBagConstraints gbConstraints = new GridBagConstraints();
      gbConstraints.anchor = GridBagConstraints.EAST;
      gbConstraints.fill = GridBagConstraints.HORIZONTAL;
      gbConstraints.gridy = i + componentOffset;
      gbConstraints.gridx = 0;
      gbLayout.setConstraints(m_Labels[sortedPropOrderings[i]], gbConstraints);
      scrollablePanel.add(m_Labels[sortedPropOrderings[i]]);
      JPanel newPanel = new JPanel();
      if (m_TipTexts[sortedPropOrderings[i]] != null) {
        m_Views[sortedPropOrderings[i]].setToolTipText(m_TipTexts[sortedPropOrderings[i]]);
        m_Labels[sortedPropOrderings[i]].setToolTipText(m_TipTexts[sortedPropOrderings[i]]);
      }
      newPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 10));
      newPanel.setLayout(new BorderLayout());
      newPanel.add(m_Views[sortedPropOrderings[i]], BorderLayout.CENTER);
      gbConstraints = new GridBagConstraints();
      gbConstraints.anchor = GridBagConstraints.WEST;
      gbConstraints.fill = GridBagConstraints.BOTH;
      gbConstraints.gridy = i + componentOffset;
      gbConstraints.gridx = 1;
      gbConstraints.weightx = 100;
      gbLayout.setConstraints(newPanel, gbConstraints);
      scrollablePanel.add(newPanel);
      m_NumEditable++;
    }

    if (m_NumEditable == 0) {
      JLabel empty = new JLabel("No editable properties", SwingConstants.CENTER);
      Dimension d = empty.getPreferredSize();
      empty.setPreferredSize(new Dimension(d.width * 2, d.height * 2));
      empty.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 10));
      GridBagConstraints gbConstraints = new GridBagConstraints();
      gbConstraints.anchor = GridBagConstraints.CENTER;
      gbConstraints.fill = GridBagConstraints.HORIZONTAL;
      gbConstraints.gridy = componentOffset;
      gbConstraints.gridx = 0;
      gbLayout.setConstraints(empty, gbConstraints);
      scrollablePanel.add(empty);
    }

    validate();

    // sometimes, the calculated dimensions seem to be too small and the
    // scrollbars show up, though there is still plenty of space on the
    // screen. hence we increase the dimensions a bit to fix this.
    Dimension dim = scrollablePanel.getPreferredSize();
    dim.height += 20;
    dim.width += 20;
    scrollPane.setPreferredSize(dim);
    validate();

    setVisible(true);
  }

  /**
   * opens the help dialog.
   */
  protected void openHelpFrame() {

    JTextArea ta = new JTextArea();
    ta.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    // ta.setBackground(getBackground());
    ta.setEditable(false);
    ta.setText(m_HelpText.toString());
    ta.setCaretPosition(0);
    JDialog jdtmp;
    if (PropertyDialog.getParentDialog(this) != null) {
      jdtmp = new JDialog(PropertyDialog.getParentDialog(this), "Information");
    } else if (PropertyDialog.getParentFrame(this) != null) {
      jdtmp = new JDialog(PropertyDialog.getParentFrame(this), "Information");
    } else {
      jdtmp = new JDialog(PropertyDialog.getParentDialog(m_aboutPanel),
        "Information");
    }
    final JDialog jd = jdtmp;
    jd.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        jd.dispose();
        if (m_HelpDialog == jd) {
          m_HelpBut.setEnabled(true);
        }
      }
    });
    jd.getContentPane().setLayout(new BorderLayout());
    jd.getContentPane().add(new JScrollPane(ta), BorderLayout.CENTER);
    jd.pack();
    jd.setSize(400, 350);
    jd.setLocation(m_aboutPanel.getTopLevelAncestor().getLocationOnScreen().x
      + m_aboutPanel.getTopLevelAncestor().getSize().width, m_aboutPanel
      .getTopLevelAncestor().getLocationOnScreen().y);
    jd.setVisible(true);
    m_HelpDialog = jd;
  }

  /**
   * opens the help dialog for the capabilities.
   */
  protected void openCapabilitiesHelpDialog() {
    if (PropertyDialog.getParentDialog(this) != null) {
      m_CapabilitiesDialog = new CapabilitiesHelpDialog(
        PropertyDialog.getParentDialog(this));
    } else {
      m_CapabilitiesDialog = new CapabilitiesHelpDialog(
        PropertyDialog.getParentFrame(this));
    }
    m_CapabilitiesDialog.setSize(400, 350);
    m_CapabilitiesDialog.setLocation(m_aboutPanel.getTopLevelAncestor()
      .getLocationOnScreen().x
      + m_aboutPanel.getTopLevelAncestor().getSize().width, m_aboutPanel
      .getTopLevelAncestor().getLocationOnScreen().y);
    m_CapabilitiesDialog.setVisible(true);
    addPropertyChangeListener(m_CapabilitiesDialog);
  }

  /**
   * Gets the number of editable properties for the current target.
   *
   * @return the number of editable properties.
   */
  public int editableProperties() {

    return m_NumEditable;
  }

  /**
   * Returns true if the object being edited has a customizer
   *
   * @return true if the object being edited has a customizer
   */
  public boolean hasCustomizer() {
    return m_Customizer != null;
  }

  /**
   * Updates the propertysheet when a value has been changed (from outside the
   * propertysheet?).
   *
   * @param evt a value of type 'PropertyChangeEvent'
   */
  synchronized void wasModified(PropertyChangeEvent evt) {

    // System.err.println("wasModified");
    if (evt.getSource() instanceof PropertyEditor) {
      PropertyEditor editor = (PropertyEditor) evt.getSource();
      for (int i = 0; i < m_Editors.length; i++) {
        if (m_Editors[i] == editor) {
          PropertyDescriptor property = m_Properties[i];
          Object value = editor.getValue();
          m_Values[i] = value;
          Method setter = property.getWriteMethod();
          try {
            Object args[] = { value };
            args[0] = value;
            setter.invoke(m_Target, args);
          } catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof PropertyVetoException) {
              String message = "WARNING: Vetoed; reason is: "
                + ex.getTargetException().getMessage();
              System.err.println(message);

              Component jf;
              if (evt.getSource() instanceof JPanel) {
                jf = ((JPanel) evt.getSource()).getParent();
              } else {
                jf = new JFrame();
              }
              JOptionPane.showMessageDialog(jf, message, "error",
                JOptionPane.WARNING_MESSAGE);
              if (jf instanceof JFrame) {
                ((JFrame) jf).dispose();
              }

            } else {
              System.err.println(ex.getTargetException().getClass().getName()
                + " while updating " + property.getName() + ": "
                + ex.getTargetException().getMessage());
              Component jf;
              if (evt.getSource() instanceof JPanel) {
                jf = ((JPanel) evt.getSource()).getParent();
              } else {
                jf = new JFrame();
              }
              JOptionPane.showMessageDialog(jf, ex.getTargetException()
                .getClass().getName()
                + " while updating "
                + property.getName()
                + ":\n"
                + ex.getTargetException().getMessage(), "error",
                JOptionPane.WARNING_MESSAGE);
              if (jf instanceof JFrame) {
                ((JFrame) jf).dispose();
              }

            }
          } catch (Exception ex) {
            System.err.println("Unexpected exception while updating "
              + property.getName());
          }
          if (m_Views[i] != null && m_Views[i] instanceof PropertyPanel) {
            // System.err.println("Trying to repaint the property canvas");
            m_Views[i].repaint();
            revalidate();
          }
          break;
        }
      }
    }

    // Now re-read all the properties and update the editors
    // for any other properties that have changed.
    for (int i = 0; i < m_Properties.length; i++) {
      Object o;
      try {
        Method getter = m_Properties[i].getReadMethod();
        Method setter = m_Properties[i].getWriteMethod();

        if (getter == null || setter == null) {
          // ignore set/get only properties
          continue;
        }

        Object args[] = {};
        o = getter.invoke(m_Target, args);
      } catch (Exception ex) {
        o = null;
      }
      if (o == m_Values[i] || (o != null && o.equals(m_Values[i]))) {
        // The property is equal to its old value.
        continue;
      }
      m_Values[i] = o;
      // Make sure we have an editor for this property...
      if (m_Editors[i] == null) {
        continue;
      }
      // The property has changed! Update the editor.
      m_Editors[i].removePropertyChangeListener(this);
      m_Editors[i].setValue(o);
      m_Editors[i].addPropertyChangeListener(this);
      if (m_Views[i] != null) {
        // System.err.println("Trying to repaint " + (i + 1));
        m_Views[i].repaint();
      }
    }

    // Make sure the target bean gets repainted.
    if (Beans.isInstanceOf(m_Target, Component.class)) {
      ((Component) (Beans.getInstanceOf(m_Target, Component.class))).repaint();
    }
  }

  /**
   * Set environment variables to pass on to any editor that can use them
   *
   * @param env the variables to pass on to individual property editors
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Pass on an OK closing notification to the customizer (if one is in use)
   */
  public void closingOK() {
    if (m_Customizer != null) {
      // pass on the notification to the customizer so that
      // it can copy values out of its GUI widgets into the object
      // being customized, if necessary
      m_Customizer.closingOK();
    }
  }

  /**
   * Pass on a CANCEL closing notificiation to the customizer (if one is in
   * use).
   */
  public void closingCancel() {
    // pass on the notification to the customizer so that
    // it can revert to previous settings for the object being
    // edited, if neccessary
    if (m_Customizer != null) {
      m_Customizer.closingCancel();
    }
  }
}
