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
 *    Appender.java
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.beans.EventSetDescriptor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JPanel;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.SerializedInstancesLoader;
import weka.gui.Logger;

/**
 * A bean that appends multiple incoming data connections into a single data
 * set. The incoming connections can be either all instance connections or all
 * batch-oriented connections (i.e. data set, training set and test set).
 * Instance and batch connections can't be mixed. An amalgamated output is
 * created that is a combination of all the incoming attributes. Missing values
 * are used to fill columns that don't exist in a particular incoming data set.
 * If all incoming connections are instance connections, then the outgoing
 * connection must be an instance connection (and vice versa for incoming batch
 * connections).
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 9131 $
 */
@KFStep(category = "Flow", toolTipText = "Append multiple sets of instances")
public class Appender extends JPanel implements BeanCommon, Visible,
    Serializable, DataSource, DataSourceListener, TrainingSetListener,
    TestSetListener, InstanceListener, EventConstraints {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 9177433051794199463L;

  /** Logging */
  protected transient Logger m_log;

  /** Upstream components sending us data */
  protected Set<String> m_listeneeTypes = new HashSet<String>();
  protected Map<Object, Object> m_listenees = new HashMap<Object, Object>();

  /**
   * Used to keep track of how many have sent us complete data sets (batch) or
   * structure available events (incremental) so far + store headers from each
   */
  protected transient Map<Object, Instances> m_completed;

  /** Handles on temp files used to store batches of instances in batch mode */
  protected transient Map<Object, File> m_tempBatchFiles;

  /** Used to hold the final header in the case of incremental operation */
  protected transient Instances m_completeHeader;

  /**
   * Holds savers used for incrementally saving incoming instance streams. After
   * we've seen the structure from each incoming connection we can create the
   * final output structure, pull any saved instances from the temp files and
   * discard these savers as they will no longer be needed.
   */
  protected transient Map<Object, ArffSaver> m_incrementalSavers;

  /** Instance event to use for incremental mode */
  protected InstanceEvent m_ie = new InstanceEvent(this);

  /** Keeps track of how many incoming instance streams have finished */
  protected int m_finishedCount;

  /** For printing status updates in incremental mode */
  protected transient int m_incrementalCounter;

  /** True if we are busy */
  protected boolean m_busy;

  /**
   * Default visual for data sources
   */
  protected BeanVisual m_visual = new BeanVisual("Appender",
      BeanVisual.ICON_PATH + "Appender.png", BeanVisual.ICON_PATH
          + "Appender.png");

  /** Downstream steps listening to batch data events */
  protected ArrayList<DataSourceListener> m_dataListeners = new ArrayList<DataSourceListener>();

  /** Downstream steps listening to instance events */
  protected ArrayList<InstanceListener> m_instanceListeners = new ArrayList<InstanceListener>();

  /**
   * Constructs a new Appender.
   */
  public Appender() {
    useDefaultVisual();
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);
  }

  /**
   * Returns true if, at the current time, the named event could be generated.
   * 
   * @param eventName the name of the event in question
   * @return true if the named event could be generated
   */
  @Override
  public boolean eventGeneratable(String eventName) {

    if (!m_listeneeTypes.contains(eventName)) {
      return false;
    }

    for (Object listenee : m_listenees.values()) {
      if (listenee instanceof EventConstraints) {
        if (!((EventConstraints) listenee).eventGeneratable(eventName)) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Accept and process an instance event
   * 
   * @param e an <code>InstanceEvent</code> value
   */
  @Override
  public synchronized void acceptInstance(InstanceEvent e) {
    m_busy = true;
    if (m_completed == null) {
      m_completed = new HashMap<Object, Instances>();

      // until we have a header from each incoming connection, we'll have
      // to store instances to temp files. If sequential start points are
      // being used, or the operation of the flow results in all instances
      // from one input path getting passed in before any subsequent input
      // paths are processed, then this will be inefficient. Parallel start
      // points will be most efficient

      m_incrementalSavers = new HashMap<Object, ArffSaver>();
      m_finishedCount = 0;
      m_incrementalCounter = 0;
    }

    if (e.getStatus() == InstanceEvent.FORMAT_AVAILABLE) {

      // reset if we get a new start of stream from one of streams that
      // we've seen a FORMAT_AVAILABLE from previously
      if (m_completed.containsKey(e.getSource())) {
        if (m_log != null) {
          String msg = statusMessagePrefix() + "Resetting appender.";
          m_log.statusMessage(msg);
          m_log.logMessage("[Appender] " + msg
              + " New start of stream detected before "
              + "all incoming streams have finished!");
        }

        m_completed = new HashMap<Object, Instances>();
        m_incrementalSavers = new HashMap<Object, ArffSaver>();
        m_incrementalCounter = 0;
        m_completeHeader = null;
        m_finishedCount = 0;
      }

      m_completed.put(e.getSource(), e.getStructure());

      if (m_completed.size() == m_listenees.size()) {
        // create mondo header...
        try {
          if (m_log != null) {
            String msg = statusMessagePrefix() + "Making output header";
            m_log.statusMessage(msg);
            m_log.logMessage("[Appender] " + msg);
          }

          m_completeHeader = makeOutputHeader();
          // notify listeners of output format
          m_ie.setStructure(m_completeHeader);
          notifyInstanceListeners(m_ie);

          // now check for any buffered instances...
          if (m_incrementalSavers.size() > 0) {
            // read in and convert these instances now
            for (ArffSaver s : m_incrementalSavers.values()) {
              // finish off the saving process first
              s.writeIncremental(null);

              File tmpFile = s.retrieveFile();
              ArffLoader loader = new ArffLoader();
              loader.setFile(tmpFile);
              Instances tempStructure = loader.getStructure();
              Instance tempLoaded = loader.getNextInstance(tempStructure);
              while (tempLoaded != null) {
                Instance converted = makeOutputInstance(m_completeHeader,
                    tempLoaded);
                m_ie.setStatus(InstanceEvent.INSTANCE_AVAILABLE);
                m_ie.setInstance(converted);
                notifyInstanceListeners(m_ie);

                m_incrementalCounter++;
                if (m_incrementalCounter % 10000 == 0) {
                  if (m_log != null) {
                    m_log.statusMessage(statusMessagePrefix() + "Processed "
                        + m_incrementalCounter + " instances");
                  }
                }
                tempLoaded = loader.getNextInstance(tempStructure);
              }
            }
            m_incrementalSavers.clear();
          }
        } catch (Exception e1) {
          String msg = statusMessagePrefix()
              + "ERROR: unable to create output instances structure.";
          if (m_log != null) {
            m_log.statusMessage(msg);
            m_log.logMessage("[Appender] " + e1.getMessage());
          }
          stop();

          e1.printStackTrace();
          m_busy = false;
          return;
        }
      }
      m_busy = false;
      return;
    }

    if (e.getStatus() == InstanceEvent.BATCH_FINISHED
        || e.getStatus() == InstanceEvent.INSTANCE_AVAILABLE) {
      // get the instance (if available)
      Instance currentI = e.getInstance();
      if (m_completeHeader == null) {
        if (currentI != null) {
          // save this instance to a temp file
          ArffSaver saver = m_incrementalSavers.get(e.getSource());
          if (saver == null) {
            saver = new ArffSaver();
            try {
              File tmpFile = File.createTempFile("weka", ".arff");
              saver.setFile(tmpFile);
              saver.setRetrieval(weka.core.converters.Saver.INCREMENTAL);
              saver.setInstances(new Instances(currentI.dataset(), 0));
              m_incrementalSavers.put(e.getSource(), saver);
            } catch (IOException e1) {
              stop();
              e1.printStackTrace();
              String msg = statusMessagePrefix()
                  + "ERROR: unable to save instance to temp file";
              if (m_log != null) {
                m_log.statusMessage(msg);
                m_log.logMessage("[Appender] " + e1.getMessage());
              }
              m_busy = false;
              return;
            }
          }
          try {
            saver.writeIncremental(currentI);

            if (e.getStatus() == InstanceEvent.BATCH_FINISHED) {
              m_finishedCount++;
            }
          } catch (IOException e1) {
            stop();
            e1.printStackTrace();

            String msg = statusMessagePrefix()
                + "ERROR: unable to save instance to temp file";
            if (m_log != null) {
              m_log.statusMessage(msg);
              m_log.logMessage("[Appender] " + e1.getMessage());
            }

            m_busy = false;
            return;
          }
        }
      } else {
        if (currentI != null) {
          int code = InstanceEvent.INSTANCE_AVAILABLE;
          if (e.getStatus() == InstanceEvent.BATCH_FINISHED) {
            m_finishedCount++;
            if (m_finishedCount == m_listenees.size()) {
              // We're all done!
              code = InstanceEvent.BATCH_FINISHED;
            }
          }

          // convert instance and output immediately
          Instance newI = makeOutputInstance(m_completeHeader, currentI);
          m_ie.setStatus(code);
          m_ie.setInstance(newI);
          notifyInstanceListeners(m_ie);

          m_incrementalCounter++;
          if (m_incrementalCounter % 10000 == 0) {
            if (m_log != null) {
              m_log.statusMessage(statusMessagePrefix() + "Processed "
                  + m_incrementalCounter + " instances");
            }
          }

          if (code == InstanceEvent.BATCH_FINISHED) {
            if (m_log != null) {
              m_log.statusMessage(statusMessagePrefix() + "Finished");
            }
            m_completed = null;
            m_incrementalSavers = null;
            m_incrementalCounter = 0;
            m_completeHeader = null;
            m_finishedCount = 0;
          }
        }
      }
    }

    m_busy = false;
  }

  /**
   * Accept and process a test set event
   * 
   * @param e a <code>TestSetEvent</code> value
   */
  @Override
  public void acceptTestSet(TestSetEvent e) {
    DataSetEvent de = new DataSetEvent(e.getSource(), e.getTestSet());
    acceptDataSet(de);
  }

  /**
   * Accept and process a training set event
   * 
   * @param e a <code>TrainingSetEvent</code> value
   */
  @Override
  public void acceptTrainingSet(TrainingSetEvent e) {
    DataSetEvent de = new DataSetEvent(e.getSource(), e.getTrainingSet());
    acceptDataSet(de);
  }

  /**
   * Accept and process a data set event
   * 
   * @param e a <code>DataSetEvent</code> value
   */
  @Override
  public synchronized void acceptDataSet(DataSetEvent e) {

    m_busy = true;

    if (m_completed == null) {
      // new batch of batches
      m_completed = new HashMap<Object, Instances>();
      m_tempBatchFiles = new HashMap<Object, File>();
    }

    // who is this that's sent us data?
    Object source = e.getSource();
    if (m_completed.containsKey(source)) {
      // Can't accept more than one data set from a particular source
      if (m_log != null && !e.isStructureOnly()) {
        String msg = statusMessagePrefix() + "Resetting appender.";
        m_log.statusMessage(msg);
        m_log.logMessage("[Appender] " + msg
            + " New batch for an incoming connection " + "detected before "
            + "all incoming connections have sent data!");
      }

      m_completed = new HashMap<Object, Instances>();
      m_tempBatchFiles = new HashMap<Object, File>();
    }

    Instances header = new Instances(e.getDataSet(), 0);
    m_completed.put(source, header);
    // write these instances (serialized) to a tmp file.
    try {
      File tmpF = File.createTempFile("weka",
          SerializedInstancesLoader.FILE_EXTENSION);
      tmpF.deleteOnExit();
      ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(
          new FileOutputStream(tmpF)));
      oos.writeObject(e.getDataSet());
      oos.flush();
      oos.close();

      m_tempBatchFiles.put(source, tmpF);
    } catch (IOException e1) {
      stop();
      e1.printStackTrace();

      String msg = statusMessagePrefix()
          + "ERROR: unable to save batch instances to temp file";
      if (m_log != null) {
        m_log.statusMessage(msg);
        m_log.logMessage("[Appender] " + e1.getMessage());
      }

      m_busy = false;
      return;
    }

    // check to see if we've had one from everyone.
    // Not much we can do if one source fails somewhere - won't know this
    // fact...
    if (m_completed.size() == m_listenees.size()) {
      // process all headers and create mongo header for new output.
      // missing values will fill columns that don't exist in particular data
      // sets
      try {
        Instances output = makeOutputHeader();
        if (m_log != null) {
          String msg = statusMessagePrefix() + "Making output header";
          m_log.statusMessage(msg);
          m_log.logMessage("[Appender] " + msg);
        }

        for (File f : m_tempBatchFiles.values()) {
          ObjectInputStream ois = new ObjectInputStream(
              new BufferedInputStream(new FileInputStream(f)));
          Instances temp = (Instances) ois.readObject();
          ois.close();

          // copy each instance over
          for (int i = 0; i < temp.numInstances(); i++) {
            Instance converted = makeOutputInstance(output, temp.instance(i));
            output.add(converted);
          }
        }

        DataSetEvent d = new DataSetEvent(this, output);
        notifyDataListeners(d);
      } catch (Exception ex) {
        stop();
        ex.printStackTrace();

        String msg = statusMessagePrefix()
            + "ERROR: unable to output appended data set";
        if (m_log != null) {
          m_log.statusMessage(msg);
          m_log.logMessage("[Appender] " + ex.getMessage());
        }
      }

      // finished
      m_completed = null;
      m_tempBatchFiles = null;

      if (m_log != null) {
        m_log.statusMessage(statusMessagePrefix() + "Finished");
      }
    }
    m_busy = false;
  }

  private Instance makeOutputInstance(Instances output, Instance source) {

    double[] newVals = new double[output.numAttributes()];
    for (int i = 0; i < newVals.length; i++) {
      newVals[i] = Utils.missingValue();
    }

    for (int i = 0; i < source.numAttributes(); i++) {
      if (!source.isMissing(i)) {
        Attribute s = source.attribute(i);
        int outputIndex = output.attribute(s.name()).index();
        if (s.isNumeric()) {
          newVals[outputIndex] = source.value(s);
        } else if (s.isString()) {
          String sVal = source.stringValue(s);
          newVals[outputIndex] = output.attribute(outputIndex).addStringValue(
              sVal);
        } else if (s.isRelationValued()) {
          Instances rVal = source.relationalValue(s);
          newVals[outputIndex] = output.attribute(outputIndex)
              .addRelation(rVal);
        } else if (s.isNominal()) {
          String nomVal = source.stringValue(s);
          newVals[outputIndex] = output.attribute(outputIndex).indexOfValue(
              nomVal);
        }
      }
    }

    Instance newInst = new DenseInstance(source.weight(), newVals);
    newInst.setDataset(output);

    return newInst;
  }

  private Instances makeOutputHeader() throws Exception {
    // process each header in turn...
    Map<String, Attribute> attLookup = new HashMap<String, Attribute>();
    List<Attribute> attList = new ArrayList<Attribute>();
    Map<String, Set<String>> nominalLookups = new HashMap<String, Set<String>>();
    for (Instances h : m_completed.values()) {
      for (int i = 0; i < h.numAttributes(); i++) {
        Attribute a = h.attribute(i);
        if (!attLookup.containsKey(a.name())) {
          attLookup.put(a.name(), a);
          attList.add(a);
          if (a.isNominal()) {
            TreeSet<String> nVals = new TreeSet<String>();
            for (int j = 0; j < a.numValues(); j++) {
              nVals.add(a.value(j));
            }
            nominalLookups.put(a.name(), nVals);
          }
        } else {
          Attribute storedVersion = attLookup.get(a.name());
          if (storedVersion.type() != a.type()) {
            // mismatched types between headers - can't continue
            throw new Exception("Conflicting types for attribute " + "name '"
                + a.name() + "' between incoming " + "instance sets");
          }

          if (storedVersion.isNominal()) {
            Set<String> storedVals = nominalLookups.get(a.name());
            for (int j = 0; j < a.numValues(); j++) {
              storedVals.add(a.value(j));
            }
          }
        }
      }
    }

    ArrayList<Attribute> finalAttList = new ArrayList<Attribute>();
    for (Attribute a : attList) {
      Attribute newAtt = null;
      if (a.isDate()) {
        newAtt = new Attribute(a.name(), a.getDateFormat());
      } else if (a.isNumeric()) {
        newAtt = new Attribute(a.name());
      } else if (a.isRelationValued()) {
        newAtt = new Attribute(a.name(), a.relation());
      } else if (a.isNominal()) {
        Set<String> vals = nominalLookups.get(a.name());
        List<String> newVals = new ArrayList<String>();
        for (String v : vals) {
          newVals.add(v);
        }
        newAtt = new Attribute(a.name(), newVals);
      } else if (a.isString()) {
        newAtt = new Attribute(a.name(), (List<String>) null);
        // transfer all string values
        /*
         * for (int i = 0; i < a.numValues(); i++) {
         * newAtt.addStringValue(a.value(i)); }
         */
      }

      finalAttList.add(newAtt);
    }

    Instances outputHeader = new Instances("Appended_" + m_listenees.size()
        + "_sets", finalAttList, 0);

    return outputHeader;
  }

  /**
   * Add a data source listener
   * 
   * @param dsl a <code>DataSourceListener</code> value
   */
  @Override
  public synchronized void addDataSourceListener(DataSourceListener dsl) {
    m_dataListeners.add(dsl);
  }

  /**
   * Remove a data source listener
   * 
   * @param dsl a <code>DataSourceListener</code> value
   */
  @Override
  public synchronized void removeDataSourceListener(DataSourceListener dsl) {
    m_dataListeners.remove(dsl);
  }

  /**
   * Add an instance listener
   * 
   * @param tsl an <code>InstanceListener</code> value
   */
  @Override
  public synchronized void addInstanceListener(InstanceListener tsl) {
    m_instanceListeners.add(tsl);
  }

  /**
   * Remove an instance listener
   * 
   * @param tsl an <code>InstanceListener</code> value
   */
  @Override
  public synchronized void removeInstanceListener(InstanceListener tsl) {
    m_instanceListeners.remove(tsl);
  }

  /**
   * Use the default visual representation
   */
  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "Appender.png",
        BeanVisual.ICON_PATH + "Appender.png");
    m_visual.setText("Appender");
  }

  /**
   * Set a new visual representation
   * 
   * @param newVisual a <code>BeanVisual</code> value
   */
  @Override
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  /**
   * Get the visual representation
   * 
   * @return a <code>BeanVisual</code> value
   */
  @Override
  public BeanVisual getVisual() {
    return m_visual;
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
   * Stop any processing that the bean might be doing.
   */
  @Override
  public void stop() {
    // tell any upstream listenees to stop
    if (m_listenees != null && m_listenees.size() > 0) {
      for (Object l : m_listenees.values()) {
        if (l instanceof BeanCommon) {
          ((BeanCommon) l).stop();
        }
      }
    }

    m_busy = false;
  }

  /**
   * Returns true if. at this time, the bean is busy with some (i.e. perhaps a
   * worker thread is performing some calculation).
   * 
   * @return true if the bean is busy.
   */
  @Override
  public boolean isBusy() {
    return m_busy;
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
   * Returns true if, at this time, the object will accept a connection via the
   * named event
   * 
   * @param esd the EventSetDescriptor for the event in question
   * @return true if the object will accept a connection
   */
  @Override
  public boolean connectionAllowed(EventSetDescriptor esd) {
    return connectionAllowed(esd.getName());
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
    if (!eventName.equals("dataSet") && !eventName.equals("trainingSet")
        && !eventName.equals("testSet") && !eventName.equals("instance")) {
      return false;
    }

    if (m_listeneeTypes.size() == 0) {
      return true;
    }

    if (m_listeneeTypes.contains("instance") && !eventName.equals("instance")) {
      return false;
    }

    if (!m_listeneeTypes.contains("instance") && eventName.equals("instance")) {
      return false;
    }

    return true;
  }

  /**
   * Notify this object that it has been registered as a listener with a source
   * for recieving events described by the named event This object is
   * responsible for recording this fact.
   * 
   * @param eventName the event
   * @param source the source with which this object has been registered as a
   *          listener
   */
  @Override
  public void connectionNotification(String eventName, Object source) {
    if (connectionAllowed(eventName)) {
      m_listeneeTypes.add(eventName);
      m_listenees.put(source, source);
    }
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
    m_listenees.remove(source);
    if (m_listenees.size() == 0) {
      m_listeneeTypes.clear();
    }
  }

  private String statusMessagePrefix() {
    return getCustomName() + "$" + hashCode() + "|";
  }

  @SuppressWarnings("unchecked")
  private void notifyInstanceListeners(InstanceEvent e) {
    List<InstanceListener> l;
    synchronized (this) {
      l = (List<InstanceListener>) m_instanceListeners.clone();
    }
    if (l.size() > 0) {
      for (InstanceListener il : l) {
        il.acceptInstance(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void notifyDataListeners(DataSetEvent e) {
    List<DataSourceListener> l;
    synchronized (this) {
      l = (List<DataSourceListener>) m_dataListeners.clone();
    }
    if (l.size() > 0) {
      for (DataSourceListener ds : l) {
        ds.acceptDataSet(e);
      }
    }
  }
}
