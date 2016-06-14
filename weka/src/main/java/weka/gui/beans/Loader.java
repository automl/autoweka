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
 *    Loader.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.EventSetDescriptor;
import java.beans.beancontext.BeanContext;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.util.Vector;

import javax.swing.JButton;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.SerializedObject;
import weka.core.Utils;
import weka.core.converters.ArffLoader;
import weka.core.converters.DatabaseLoader;
import weka.core.converters.FileSourcedConverter;
import weka.core.converters.Loader.StructureNotReadyException;
import weka.gui.Logger;

/**
 * Loads data sets using weka.core.converter classes
 * 
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 10221 $
 * @since 1.0
 * @see AbstractDataSource
 * @see UserRequestAcceptor
 */
public class Loader extends AbstractDataSource implements Startable,
  WekaWrapper, EventConstraints, BeanCommon, EnvironmentHandler,
  StructureProducer {

  /** for serialization */
  private static final long serialVersionUID = 1993738191961163027L;

  /**
   * Holds the instances loaded
   */
  private transient Instances m_dataSet;

  /**
   * Holds the format of the last loaded data set
   */
  private transient Instances m_dataFormat;

  /**
   * Global info for the wrapped loader (if it exists).
   */
  protected String m_globalInfo;

  /**
   * Thread for doing IO in
   */
  private LoadThread m_ioThread;

  private static int IDLE = 0;
  private static int BATCH_LOADING = 1;
  private static int INCREMENTAL_LOADING = 2;
  private int m_state = IDLE;

  /**
   * Loader
   */
  private weka.core.converters.Loader m_Loader = new ArffLoader();

  private final InstanceEvent m_ie = new InstanceEvent(this);

  /**
   * Keep track of how many listeners for different types of events there are.
   */
  private int m_instanceEventTargets = 0;
  private int m_dataSetEventTargets = 0;

  /** Flag indicating that a database has already been configured */
  private boolean m_dbSet = false;

  /**
   * Logging
   */
  protected transient Logger m_log;

  /**
   * The environment variables.
   */
  protected transient Environment m_env;

  /**
   * Asked to stop?
   */
  protected boolean m_stopped = false;

  private class LoadThread extends Thread {
    private final DataSource m_DP;
    private StreamThroughput m_throughput;
    private StreamThroughput m_flowThroughput;

    public LoadThread(DataSource dp) {
      m_DP = dp;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run() {
      String stm = getCustomName() + "$" + hashCode() + 99
        + "| - overall flow throughput -|";
      try {
        m_visual.setAnimated();
        // m_visual.setText("Loading...");

        boolean instanceGeneration = true;
        // determine if we are going to produce data set or instance events
        /*
         * for (int i = 0; i < m_listeners.size(); i++) { if
         * (m_listeners.elementAt(i) instanceof DataSourceListener) {
         * instanceGeneration = false; break; } }
         */
        if (m_dataSetEventTargets > 0) {
          instanceGeneration = false;
          m_state = BATCH_LOADING;
        }

        // Set environment variables
        if (m_Loader instanceof EnvironmentHandler && m_env != null) {
          ((EnvironmentHandler) m_Loader).setEnvironment(m_env);
        }

        String msg = statusMessagePrefix();
        if (m_Loader instanceof FileSourcedConverter) {
          msg += "Loading "
            + ((FileSourcedConverter) m_Loader).retrieveFile().getName();
        } else {
          msg += "Loading...";
        }
        if (m_log != null) {
          m_log.statusMessage(msg);
        }

        if (instanceGeneration) {
          m_throughput = new StreamThroughput(statusMessagePrefix());

          m_flowThroughput = new StreamThroughput(stm, "Starting flow...",
            m_log);

          m_state = INCREMENTAL_LOADING;
          // boolean start = true;
          Instance nextInstance = null;
          // load and pass on the structure first
          Instances structure = null;
          Instances structureCopy = null;
          Instances currentStructure = null;
          boolean stringAttsPresent = false;
          try {
            m_Loader.reset();
            m_Loader.setRetrieval(weka.core.converters.Loader.INCREMENTAL);
            // System.err.println("NOTIFYING STRUCTURE AVAIL");
            structure = m_Loader.getStructure();
            if (structure.checkForStringAttributes()) {
              structureCopy = (Instances) (new SerializedObject(structure)
                .getObject());
              stringAttsPresent = true;
            }
            currentStructure = structure;
            m_ie.m_formatNotificationOnly = false;
            notifyStructureAvailable(structure);
          } catch (IOException e) {
            if (m_log != null) {
              m_log.statusMessage(statusMessagePrefix()
                + "ERROR (See log for details");
              m_log.logMessage("[Loader] " + statusMessagePrefix() + " "
                + e.getMessage());
            }
            e.printStackTrace();
          }
          try {
            nextInstance = m_Loader.getNextInstance(structure);
          } catch (IOException e) {
            if (m_log != null) {
              m_log.statusMessage(statusMessagePrefix()
                + "ERROR (See log for details");
              m_log.logMessage("[Loader] " + statusMessagePrefix() + " "
                + e.getMessage());
            }
            e.printStackTrace();
          }

          while (nextInstance != null) {
            if (m_stopped) {
              break;
            }
            m_throughput.updateStart();
            m_flowThroughput.updateStart();
            // nextInstance.setDataset(structure);
            // format.add(nextInstance);
            /*
             * InstanceEvent ie = (start) ? new InstanceEvent(m_DP,
             * nextInstance, InstanceEvent.FORMAT_AVAILABLE) : new
             * InstanceEvent(m_DP, nextInstance,
             * InstanceEvent.INSTANCE_AVAILABLE);
             */
            // if (start) {
            // m_ie.setStatus(InstanceEvent.FORMAT_AVAILABLE);
            // } else {
            m_ie.setStatus(InstanceEvent.INSTANCE_AVAILABLE);
            // }
            m_ie.setInstance(nextInstance);
            // start = false;
            // System.err.println(z);

            // a little jiggery pokery to ensure that our
            // one instance lookahead to determine whether
            // this instance is the end of the batch doesn't
            // clobber any string values in the current
            // instance, if the loader is loading them
            // incrementally (i.e. only retaining one
            // value in the header at any one time).
            if (stringAttsPresent) {
              if (currentStructure == structure) {
                currentStructure = structureCopy;
              } else {
                currentStructure = structure;
              }
            }
            nextInstance = m_Loader.getNextInstance(currentStructure);

            if (nextInstance == null) {
              m_ie.setStatus(InstanceEvent.BATCH_FINISHED);
            }
            m_throughput.updateEnd(m_log);
            notifyInstanceLoaded(m_ie);
            m_flowThroughput.updateEnd(m_log);
          }
          m_visual.setStatic();
          // m_visual.setText(structure.relationName());
        } else {
          m_Loader.reset();
          m_Loader.setRetrieval(weka.core.converters.Loader.BATCH);
          m_dataSet = m_Loader.getDataSet();
          m_visual.setStatic();
          if (m_log != null) {
            m_log.logMessage("[Loader] " + statusMessagePrefix() + " loaded "
              + m_dataSet.relationName());
          }
          // m_visual.setText(m_dataSet.relationName());
          notifyDataSetLoaded(new DataSetEvent(m_DP, m_dataSet));
        }
      } catch (Exception ex) {
        if (m_log != null) {
          m_log.statusMessage(statusMessagePrefix()
            + "ERROR (See log for details");
          m_log.logMessage("[Loader] " + statusMessagePrefix() + " "
            + ex.getMessage());
        }
        ex.printStackTrace();
      } finally {
        if (Thread.currentThread().isInterrupted()) {
          if (m_log != null) {
            m_log.logMessage("[Loader] " + statusMessagePrefix()
              + " loading interrupted!");
          }
        }
        m_ioThread = null;
        // m_visual.setText("Finished");
        // m_visual.setIcon(m_inactive.getVisual());
        m_visual.setStatic();
        m_state = IDLE;
        m_stopped = false;
        if (m_log != null) {
          if (m_throughput != null) {
            String finalMessage = m_throughput.finished() + " (read speed); ";
            m_flowThroughput.finished(m_log);
            m_log.statusMessage(stm + "remove");
            int flowSpeed = m_flowThroughput.getAverageInstancesPerSecond();
            finalMessage += ("" + flowSpeed + " insts/sec (flow throughput)");
            m_log.statusMessage(statusMessagePrefix() + finalMessage);
          } else {
            m_log.statusMessage(statusMessagePrefix() + "Finished.");
          }
        }
        block(false);
      }
    }
  }

  /**
   * Global info (if it exists) for the wrapped loader
   * 
   * @return the global info
   */
  public String globalInfo() {
    return m_globalInfo;
  }

  public Loader() {
    super();
    setLoader(m_Loader);
    appearanceFinal();
  }

  public void setDB(boolean flag) {

    m_dbSet = flag;
    if (m_dbSet) {
      try {
        newStructure();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  protected void appearanceFinal() {
    removeAll();
    setLayout(new BorderLayout());
    JButton goButton = new JButton("Start...");
    add(goButton, BorderLayout.CENTER);
    goButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        startLoading();
      }
    });
  }

  protected void appearanceDesign() {
    removeAll();
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);
  }

  /**
   * Set a bean context for this bean
   * 
   * @param bc a <code>BeanContext</code> value
   */
  @Override
  public void setBeanContext(BeanContext bc) {
    super.setBeanContext(bc);
    if (m_design) {
      appearanceDesign();
    } else {
      appearanceFinal();
    }
  }

  /**
   * Set the loader to use
   * 
   * @param loader a <code>weka.core.converters.Loader</code> value
   */
  public void setLoader(weka.core.converters.Loader loader) {
    boolean loadImages = true;
    if (loader.getClass().getName().compareTo(m_Loader.getClass().getName()) == 0) {
      loadImages = false;
    }
    m_Loader = loader;
    String loaderName = loader.getClass().toString();
    loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1,
      loaderName.length());
    if (loadImages) {
      if (m_Loader instanceof Visible) {
        m_visual = ((Visible) m_Loader).getVisual();
      } else {

        if (!m_visual.loadIcons(BeanVisual.ICON_PATH + loaderName + ".gif",
          BeanVisual.ICON_PATH + loaderName + "_animated.gif")) {
          useDefaultVisual();
        }
      }
    }
    m_visual.setText(loaderName);

    // get global info
    m_globalInfo = KnowledgeFlowApp.getGlobalInfo(m_Loader);
  }

  protected void newFileSelected() throws Exception {
    if (!(m_Loader instanceof DatabaseLoader)) {
      newStructure(true);
      /*
       * // try to load structure (if possible) and notify any listeners
       * 
       * // Set environment variables if (m_Loader instanceof EnvironmentHandler
       * && m_env != null) { try {
       * ((EnvironmentHandler)m_Loader).setEnvironment(m_env); }catch (Exception
       * ex) { } } m_dataFormat = m_Loader.getStructure(); //
       * System.err.println(m_dataFormat); System.out.println(
       * "[Loader] Notifying listeners of instance structure avail.");
       * notifyStructureAvailable(m_dataFormat);
       */
    }
  }

  protected void newStructure(boolean... notificationOnly) throws Exception {

    if (notificationOnly != null && notificationOnly.length > 0) {
      // If incremental then specify whether this FORMAT_AVAILABLE
      // event is actually the start of stream processing or just
      // due to a file/source change
      m_ie.m_formatNotificationOnly = notificationOnly[0];
    } else {
      m_ie.m_formatNotificationOnly = false;
    }

    try {
      m_Loader.reset();

      // Set environment variables
      if (m_Loader instanceof EnvironmentHandler && m_env != null) {
        try {
          ((EnvironmentHandler) m_Loader).setEnvironment(m_env);
        } catch (Exception ex) {
        }
      }
      m_dataFormat = m_Loader.getStructure();
      System.out
        .println("[Loader] Notifying listeners of instance structure avail.");
      notifyStructureAvailable(m_dataFormat);
    } catch (StructureNotReadyException e) {
      if (m_log != null) {
        m_log.statusMessage(statusMessagePrefix() + "WARNING: "
          + e.getMessage());
        m_log.logMessage("[Loader] " + statusMessagePrefix() + " "
          + e.getMessage());
      }
    }
  }

  /**
   * Get the structure of the output encapsulated in the named event. If the
   * structure can't be determined in advance of seeing input, or this
   * StructureProducer does not generate the named event, null should be
   * returned.
   * 
   * @param eventName the name of the output event that encapsulates the
   *          requested output.
   * 
   * @return the structure of the output encapsulated in the named event or null
   *         if it can't be determined in advance of seeing input or the named
   *         event is not generated by this StructureProduce.
   */
  @Override
  public Instances getStructure(String eventName) {

    if (!eventName.equals("dataSet") && !eventName.equals("instance")) {
      return null;
    }
    if (m_dataSetEventTargets > 0 && !eventName.equals("dataSet")) {
      return null;
    }
    if (m_dataSetEventTargets == 0 && !eventName.equals("instance")) {
      return null;
    }

    try {
      newStructure();

    } catch (Exception ex) {
      // ex.printStackTrace();
      System.err.println("[KnowledgeFlow/Loader] Warning: " + ex.getMessage());
      m_dataFormat = null;
    }
    return m_dataFormat;
  }

  /**
   * Get the loader
   * 
   * @return a <code>weka.core.converters.Loader</code> value
   */
  public weka.core.converters.Loader getLoader() {
    return m_Loader;
  }

  /**
   * Set the loader
   * 
   * @param algorithm a Loader
   * @exception IllegalArgumentException if an error occurs
   */
  @Override
  public void setWrappedAlgorithm(Object algorithm) {

    if (!(algorithm instanceof weka.core.converters.Loader)) {
      throw new IllegalArgumentException(algorithm.getClass() + " : incorrect "
        + "type of algorithm (Loader)");
    }
    setLoader((weka.core.converters.Loader) algorithm);
  }

  /**
   * Get the loader
   * 
   * @return a Loader
   */
  @Override
  public Object getWrappedAlgorithm() {
    return getLoader();
  }

  /**
   * Notify all listeners that the structure of a data set is available.
   * 
   * @param structure an <code>Instances</code> value
   */
  protected void notifyStructureAvailable(Instances structure) {
    if (m_dataSetEventTargets > 0 && structure != null) {
      DataSetEvent dse = new DataSetEvent(this, structure);
      notifyDataSetLoaded(dse);
    } else if (m_instanceEventTargets > 0 && structure != null) {
      m_ie.setStructure(structure);
      notifyInstanceLoaded(m_ie);
    }
  }

  /**
   * Notify all Data source listeners that a data set has been loaded
   * 
   * @param e a <code>DataSetEvent</code> value
   */
  @SuppressWarnings("unchecked")
  protected void notifyDataSetLoaded(DataSetEvent e) {
    Vector<DataSourceListener> l;
    synchronized (this) {
      l = (Vector<DataSourceListener>) m_listeners.clone();
    }

    if (l.size() > 0) {
      for (int i = 0; i < l.size(); i++) {
        l.elementAt(i).acceptDataSet(e);
      }
      m_dataSet = null;
    }
  }

  /**
   * Notify all instance listeners that a new instance is available
   * 
   * @param e an <code>InstanceEvent</code> value
   */
  @SuppressWarnings("unchecked")
  protected void notifyInstanceLoaded(InstanceEvent e) {
    Vector<InstanceListener> l;
    synchronized (this) {
      l = (Vector<InstanceListener>) m_listeners.clone();
    }

    if (l.size() > 0) {
      for (int i = 0; i < l.size(); i++) {
        l.elementAt(i).acceptInstance(e);
      }
      m_dataSet = null;
    }
  }

  /**
   * Start loading data
   */
  public void startLoading() {
    if (m_ioThread == null) {
      // m_visual.setText(m_dataSetFile.getName());
      m_state = BATCH_LOADING;
      m_stopped = false;
      m_ioThread = new LoadThread(Loader.this);
      m_ioThread.setPriority(Thread.MIN_PRIORITY);
      m_ioThread.start();
    } else {
      m_ioThread = null;
      m_state = IDLE;
    }
  }

  /**
   * Get a list of user requests
   * 
   * @return an <code>Enumeration</code> value
   */
  /*
   * public Enumeration enumerateRequests() { Vector newVector = new Vector(0);
   * boolean ok = true; if (m_ioThread == null) { if (m_Loader instanceof
   * FileSourcedConverter) { String temp = ((FileSourcedConverter)
   * m_Loader).retrieveFile().getPath(); Environment env = (m_env == null) ?
   * Environment.getSystemWide() : m_env; try { temp = env.substitute(temp); }
   * catch (Exception ex) {} File tempF = new File(temp); if (!tempF.isFile()) {
   * ok = false; } } String entry = "Start loading"; if (!ok) { entry =
   * "$"+entry; } newVector.addElement(entry); } return newVector.elements(); }
   */

  /**
   * Perform the named request
   * 
   * @param request a <code>String</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  /*
   * public void performRequest(String request) { if
   * (request.compareTo("Start loading") == 0) { startLoading(); } else { throw
   * new IllegalArgumentException(request + " not supported (Loader)"); } }
   */

  /**
   * Start loading
   * 
   * @exception Exception if something goes wrong
   */
  @Override
  public void start() throws Exception {
    startLoading();
    block(true);
  }

  /**
   * Gets a string that describes the start action. The KnowledgeFlow uses this
   * in the popup contextual menu for the component. The string can be proceeded
   * by a '$' character to indicate that the component can't be started at
   * present.
   * 
   * @return a string describing the start action.
   */
  @Override
  public String getStartMessage() {
    boolean ok = true;
    String entry = "Start loading";
    if (m_ioThread == null) {
      if (m_Loader instanceof FileSourcedConverter) {
        String temp = ((FileSourcedConverter) m_Loader).retrieveFile()
          .getPath();
        Environment env = (m_env == null) ? Environment.getSystemWide() : m_env;
        try {
          temp = env.substitute(temp);
        } catch (Exception ex) {
        }
        File tempF = new File(temp);

        // forward slashes are platform independent for resources read from the
        // classpath
        String tempFixedPathSepForResource = temp.replace(File.separatorChar,
          '/');
        if (!tempF.isFile()
          && this.getClass().getClassLoader()
            .getResource(tempFixedPathSepForResource) == null) {
          ok = false;
        }
      }
      if (!ok) {
        entry = "$" + entry;
      }
    }
    return entry;
  }

  /**
   * Function used to stop code that calls acceptTrainingSet. This is needed as
   * classifier construction is performed inside a separate thread of execution.
   * 
   * @param tf a <code>boolean</code> value
   */
  private synchronized void block(boolean tf) {

    if (tf) {
      try {
        // only block if thread is still doing something useful!
        if (m_ioThread.isAlive() && m_state != IDLE) {
          wait();
        }
      } catch (InterruptedException ex) {
      }
    } else {
      notifyAll();
    }
  }

  /**
   * Returns true if the named event can be generated at this time
   * 
   * @param eventName the event
   * @return a <code>boolean</code> value
   */
  @Override
  public boolean eventGeneratable(String eventName) {
    if (eventName.compareTo("instance") == 0) {
      if (!(m_Loader instanceof weka.core.converters.IncrementalConverter)) {
        return false;
      }
      if (m_dataSetEventTargets > 0) {
        return false;
      }
      /*
       * for (int i = 0; i < m_listeners.size(); i++) { if
       * (m_listeners.elementAt(i) instanceof DataSourceListener) { return
       * false; } }
       */
    }

    if (eventName.compareTo("dataSet") == 0) {
      if (!(m_Loader instanceof weka.core.converters.BatchConverter)) {
        return false;
      }
      if (m_instanceEventTargets > 0) {
        return false;
      }
      /*
       * for (int i = 0; i < m_listeners.size(); i++) { if
       * (m_listeners.elementAt(i) instanceof InstanceListener) { return false;
       * } }
       */
    }
    return true;
  }

  /**
   * Add a listener
   * 
   * @param dsl a <code>DataSourceListener</code> value
   */
  @Override
  public synchronized void addDataSourceListener(DataSourceListener dsl) {
    super.addDataSourceListener(dsl);
    m_dataSetEventTargets++;
    // pass on any current instance format
    try {
      if ((m_Loader instanceof DatabaseLoader && m_dbSet && m_dataFormat == null)
        || (!(m_Loader instanceof DatabaseLoader) && m_dataFormat == null)) {
        m_dataFormat = m_Loader.getStructure();
        m_dbSet = false;
      }
    } catch (Exception ex) {
    }
    notifyStructureAvailable(m_dataFormat);
  }

  /**
   * Remove a listener
   * 
   * @param dsl a <code>DataSourceListener</code> value
   */
  @Override
  public synchronized void removeDataSourceListener(DataSourceListener dsl) {
    super.removeDataSourceListener(dsl);
    m_dataSetEventTargets--;
  }

  /**
   * Add an instance listener
   * 
   * @param dsl a <code>InstanceListener</code> value
   */
  @Override
  public synchronized void addInstanceListener(InstanceListener dsl) {
    super.addInstanceListener(dsl);
    m_instanceEventTargets++;
    try {
      if ((m_Loader instanceof DatabaseLoader && m_dbSet && m_dataFormat == null)
        || (!(m_Loader instanceof DatabaseLoader) && m_dataFormat == null)) {
        m_dataFormat = m_Loader.getStructure();
        m_dbSet = false;
      }
    } catch (Exception ex) {
    }
    // pass on any current instance format
    m_ie.m_formatNotificationOnly = true;
    notifyStructureAvailable(m_dataFormat);
  }

  /**
   * Remove an instance listener
   * 
   * @param dsl a <code>InstanceListener</code> value
   */
  @Override
  public synchronized void removeInstanceListener(InstanceListener dsl) {
    super.removeInstanceListener(dsl);
    m_instanceEventTargets--;
  }

  public static void main(String[] args) {
    try {
      final javax.swing.JFrame jf = new javax.swing.JFrame();
      jf.getContentPane().setLayout(new java.awt.BorderLayout());

      final Loader tv = new Loader();

      jf.getContentPane().add(tv, java.awt.BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
          jf.dispose();
          System.exit(0);
        }
      });
      jf.setSize(800, 600);
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private Object readResolve() throws ObjectStreamException {
    // try and reset the Loader
    if (m_Loader != null) {
      try {
        m_Loader.reset();
      } catch (Exception ex) {
      }
    }
    return this;
  }

  /**
   * Set a custom (descriptive) name for this bean
   * 
   * @param name the name to use
   */
  @Override
  public void setCustomName(String name) {
    m_visual.setText(name);
  }

  /**
   * Get the custom (descriptive) name for this bean (if one has been set)
   * 
   * @return the custom name (or the default name)
   */
  @Override
  public String getCustomName() {
    return m_visual.getText();
  }

  /**
   * Set a logger
   * 
   * @param logger a <code>weka.gui.Logger</code> value
   */
  @Override
  public void setLog(Logger logger) {
    m_log = logger;
  }

  /**
   * Set environment variables to use.
   * 
   * @param env the environment variables to use
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Returns true if, at this time, the object will accept a connection via the
   * supplied EventSetDescriptor. Always returns false for loader.
   * 
   * @param esd the EventSetDescriptor
   * @return true if the object will accept a connection
   */
  @Override
  public boolean connectionAllowed(EventSetDescriptor esd) {
    return false;
  }

  /**
   * Returns true if, at this time, the object will accept a connection via the
   * named event
   * 
   * @param eventName the name of the event
   * @return true if the object will accept a connection
   */
  @Override
  public boolean connectionAllowed(String eventName) {
    return false;
  }

  /**
   * Notify this object that it has been registered as a listener with a source
   * for receiving events described by the named event This object is
   * responsible for recording this fact.
   * 
   * @param eventName the event
   * @param source the source with which this object has been registered as a
   *          listener
   */
  @Override
  public void connectionNotification(String eventName, Object source) {
    // this should never get called for us.
  }

  /**
   * Notify this object that it has been deregistered as a listener with a
   * source for named event. This object is responsible for recording this fact.
   * 
   * @param eventName the event
   * @param source the source with which this object has been registered as a
   *          listener
   */
  @Override
  public void disconnectionNotification(String eventName, Object source) {
    // this should never get called for us.
  }

  /**
   * Stop any loading action.
   */
  @Override
  public void stop() {
    m_stopped = true;
  }

  /**
   * Returns true if. at this time, the bean is busy with some (i.e. perhaps a
   * worker thread is performing some calculation).
   * 
   * @return true if the bean is busy.
   */
  @Override
  public boolean isBusy() {
    return (m_ioThread != null);
  }

  private String statusMessagePrefix() {
    return getCustomName()
      + "$"
      + hashCode()
      + "|"
      + ((m_Loader instanceof OptionHandler) ? Utils
        .joinOptions(((OptionHandler) m_Loader).getOptions()) + "|" : "");
  }

  // Custom de-serialization in order to set default
  // environment variables on de-serialization
  private void readObject(ObjectInputStream aStream) throws IOException,
    ClassNotFoundException {
    aStream.defaultReadObject();

    // set a default environment to use
    m_env = Environment.getSystemWide();
  }
}
