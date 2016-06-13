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
 *    Sorter.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;

import weka.core.Attribute;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.gui.Logger;

/**
 * <!-- globalinfo-start --> Sorts incoming instances in ascending or descending
 * order according to the values of user specified attributes. Instances can be
 * sorted according to multiple attributes (defined in order). Handles data sets
 * larger than can be fit into main memory via instance connections and
 * specifying the in-memory buffer size. Implements a merge-sort by writing the
 * sorted in-memory buffer to a file when full and then interleaving instances
 * from the disk based file(s) when the incoming stream has finished.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 11132 $
 */
@KFStep(category = "Tools",
  toolTipText = "Sort instances in ascending or descending order")
public class Sorter extends JPanel implements BeanCommon, Visible,
  Serializable, DataSource, DataSourceListener, TrainingSetListener,
  TestSetListener, InstanceListener, EventConstraints, StructureProducer,
  EnvironmentHandler {

  /** For serialization */
  private static final long serialVersionUID = 4978227384322482115L;

  /** Logging */
  protected transient Logger m_log;

  /** Step talking to us */
  protected Object m_listenee;

  /** The type of the incoming connection */
  protected String m_connectionType;

  /** For printing status updates in incremental mode */
  protected InstanceEvent m_ie = new InstanceEvent(this);

  /** True if we are busy */
  protected boolean m_busy;

  /** True if a stop has been requested */
  protected AtomicBoolean m_stopRequested;

  /** Holds the internal textual description of the sort definitions */
  protected String m_sortDetails;

  /** Environment variables */
  protected transient Environment m_env;

  /** Comparator that applies the sort rules */
  protected transient SortComparator m_sortComparator;

  /** In memory buffer for incremental operation */
  protected transient List<InstanceHolder> m_incrementalBuffer;

  /** List of sorted temp files for incremental operation */
  protected transient List<File> m_bufferFiles;

  /** Size of the in-memory buffer */
  protected String m_bufferSize = "10000";

  /** Size of the in-memory buffer after resolving any environment vars */
  protected int m_bufferSizeI = 10000;

  /** Holds indexes of string attributes, keyed by attribute name */
  protected Map<String, Integer> m_stringAttIndexes;

  /**
   * The directory to hold the temp files - if not set the system tmp directory
   * is used
   */
  protected String m_tempDirectory = "";

  protected transient int m_streamCounter = 0;

  /** format of instances for current incoming connection (if any) */
  private Instances m_connectedFormat;

  /**
   * Default visual for data sources
   */
  protected BeanVisual m_visual = new BeanVisual("Sorter", BeanVisual.ICON_PATH
    + "Sorter.gif", BeanVisual.ICON_PATH + "Sorter_animated.gif");

  /** Downstream steps listening to batch data events */
  protected ArrayList<DataSourceListener> m_dataListeners =
    new ArrayList<DataSourceListener>();

  /** Downstream steps listening to instance events */
  protected ArrayList<InstanceListener> m_instanceListeners =
    new ArrayList<InstanceListener>();

  /**
   * Inner class that holds instances and the index of the temp file that holds
   * them (if operating in incremental mode)
   */
  protected static class InstanceHolder implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = -3985730394250172995L;

    /** The instance */
    protected Instance m_instance;

    /** index into the list of files on disk */
    protected int m_fileNumber;

    /**
     * for incremental operation, if string attributes are present then we need
     * to store them with each instance - since incremental streaming in the
     * knowledge flow only maintains one string value in memory (and hence in
     * the header) at any one time
     */
    protected Map<String, String> m_stringVals;
  }

  /**
   * Comparator that applies the sort rules
   */
  protected static class SortComparator implements Comparator<InstanceHolder> {

    protected List<SortRule> m_sortRules;

    public SortComparator(List<SortRule> sortRules) {
      m_sortRules = sortRules;
    }

    @Override
    public int compare(InstanceHolder o1, InstanceHolder o2) {

      int cmp = 0;
      for (SortRule sr : m_sortRules) {
        cmp = sr.compare(o1, o2);
        if (cmp != 0) {
          return cmp;
        }
      }

      return 0;
    }
  }

  /**
   * Implements a sorting rule based on a single attribute
   */
  protected static class SortRule implements Comparator<InstanceHolder> {

    protected String m_attributeNameOrIndex;
    protected Attribute m_attribute;

    protected boolean m_descending;

    public SortRule(String att, boolean descending) {
      m_attributeNameOrIndex = att;
      m_descending = descending;
    }

    public SortRule() {
    }

    public SortRule(String setup) {
      parseFromInternal(setup);
    }

    protected void parseFromInternal(String setup) {
      String[] parts = setup.split("@@SR@@");

      if (parts.length != 2) {
        throw new IllegalArgumentException("Malformed sort rule: " + setup);
      }

      m_attributeNameOrIndex = parts[0].trim();
      m_descending = parts[1].equalsIgnoreCase("Y");
    }

    protected String toStringInternal() {
      return m_attributeNameOrIndex + "@@SR@@" + (m_descending ? "Y" : "N");
    }

    @Override
    public String toString() {
      StringBuffer res = new StringBuffer();

      res.append("Attribute: " + m_attributeNameOrIndex + " - sort "
        + (m_descending ? "descending" : "ascending"));

      return res.toString();
    }

    public void setAttribute(String att) {
      m_attributeNameOrIndex = att;
    }

    public String getAttribute() {
      return m_attributeNameOrIndex;
    }

    public void setDescending(boolean d) {
      m_descending = d;
    }

    public boolean getDescending() {
      return m_descending;
    }

    public void init(Environment env, Instances structure) {
      String attNameI = m_attributeNameOrIndex;
      try {
        attNameI = env.substitute(attNameI);
      } catch (Exception ex) {
      }

      if (attNameI.equalsIgnoreCase("/first")) {
        m_attribute = structure.attribute(0);
      } else if (attNameI.equalsIgnoreCase("/last")) {
        m_attribute = structure.attribute(structure.numAttributes() - 1);
      } else {
        // try actual attribute name
        m_attribute = structure.attribute(attNameI);

        if (m_attribute == null) {
          // try as an index
          try {
            int index = Integer.parseInt(attNameI);
            m_attribute = structure.attribute(index);
          } catch (NumberFormatException n) {
            throw new IllegalArgumentException("Unable to locate attribute "
              + attNameI + " as either a named attribute or as a valid "
              + "attribute index");
          }
        }
      }
    }

    @Override
    public int compare(InstanceHolder o1, InstanceHolder o2) {

      // both missing is equal
      if (o1.m_instance.isMissing(m_attribute)
        && o2.m_instance.isMissing(m_attribute)) {
        return 0;
      }

      // one missing - missing instances should all be at the end
      // regardless of whether order is ascending or descending
      if (o1.m_instance.isMissing(m_attribute)) {
        return 1;
      }

      if (o2.m_instance.isMissing(m_attribute)) {
        return -1;
      }

      int cmp = 0;

      if (!m_attribute.isString() && !m_attribute.isRelationValued()) {
        double val1 = o1.m_instance.value(m_attribute);
        double val2 = o2.m_instance.value(m_attribute);

        cmp = Double.compare(val1, val2);
      } else if (m_attribute.isString()) {
        String val1 = o1.m_stringVals.get(m_attribute.name());
        String val2 = o2.m_stringVals.get(m_attribute.name());

        /*
         * String val1 = o1.stringValue(m_attribute); String val2 =
         * o2.stringValue(m_attribute);
         */

        // TODO case insensitive?
        cmp = val1.compareTo(val2);
      } else {
        throw new IllegalArgumentException("Can't sort according to "
          + "relation-valued attribute values!");
      }

      if (m_descending) {
        return -cmp;
      }

      return cmp;
    }
  }

  /**
   * Constructs a new Sorter
   */
  public Sorter() {
    useDefaultVisual();
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);

    m_env = Environment.getSystemWide();
    m_stopRequested = new AtomicBoolean(false);
  }

  /**
   * Help information suitable for displaying in the GUI.
   * 
   * @return a description of this component
   */
  public String globalInfo() {
    return "Sorts incoming instances in ascending or descending order "
      + "according to the values of user specified attributes. Instances "
      + "can be sorted according to multiple attributes (defined in order). "
      + "Handles data sets larger than can be fit into main memory via "
      + "instance connections and specifying the in-memory buffer size. Implements "
      + "a merge-sort by writing the sorted in-memory buffer to a file when full "
      + "and then interleaving instances from the disk based file(s) when the "
      + "incoming stream has finished.";
  }

  /**
   * Returns true if, at the current time, the named event could be generated.
   * 
   * @param eventName the name of the event in question
   * @return true if the named event could be generated
   */
  @Override
  public boolean eventGeneratable(String eventName) {
    if (m_listenee == null) {
      return false;
    }

    if (!eventName.equals("instance") && !eventName.equals("dataSet")) {
      return false;
    }

    if (m_listenee instanceof DataSource) {
      if (m_listenee instanceof EventConstraints) {
        EventConstraints ec = (EventConstraints) m_listenee;
        return ec.eventGeneratable(eventName);
      }
    }

    if (m_listenee instanceof TrainingSetProducer) {
      if (m_listenee instanceof EventConstraints) {
        EventConstraints ec = (EventConstraints) m_listenee;

        if (!eventName.equals("dataSet")) {
          return false;
        }

        if (!ec.eventGeneratable("trainingSet")) {
          return false;
        }
      }
    }

    if (m_listenee instanceof TestSetProducer) {
      if (m_listenee instanceof EventConstraints) {
        EventConstraints ec = (EventConstraints) m_listenee;

        if (!eventName.equals("dataSet")) {
          return false;
        }

        if (!ec.eventGeneratable("testSet")) {
          return false;
        }
      }
    }

    return true;
  }

  private void copyStringAttVals(InstanceHolder holder) {
    for (String attName : m_stringAttIndexes.keySet()) {
      Attribute att = holder.m_instance.dataset().attribute(attName);
      String val = holder.m_instance.stringValue(att);

      if (holder.m_stringVals == null) {
        holder.m_stringVals = new HashMap<String, String>();
      }

      holder.m_stringVals.put(attName, val);
    }
  }

  /**
   * Accept and process an instance event
   * 
   * @param e an <code>InstanceEvent</code> value
   */
  @Override
  public void acceptInstance(InstanceEvent e) {

    if (e.getStatus() == InstanceEvent.FORMAT_AVAILABLE) {
      m_connectedFormat = e.getStructure();
      m_stopRequested.set(false);
      try {
        init(new Instances(e.getStructure(), 0));
      } catch (IllegalArgumentException ex) {
        if (m_log != null) {
          String message =
            "ERROR: There is a problem with the incoming instance structure";

          // m_log.statusMessage(statusMessagePrefix() + message
          // + " - see log for details");
          // m_log.logMessage(statusMessagePrefix() + message + " :"
          // + ex.getMessage());

          stopWithErrorMessage(message, ex);
          // m_busy = false;
          return;
        }
      }

      String buffSize = m_bufferSize;
      try {
        buffSize = m_env.substitute(buffSize);
        m_bufferSizeI = Integer.parseInt(buffSize);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      m_incrementalBuffer = new ArrayList<InstanceHolder>(m_bufferSizeI);
      m_bufferFiles = new ArrayList<File>();
      m_streamCounter = 0;

      return;
    }

    m_busy = true;

    if (e.getInstance() != null) {
      if (m_streamCounter == 0) {
        if (m_log != null) {
          m_log.statusMessage(statusMessagePrefix()
            + "Starting streaming sort...");
          m_log.logMessage("[Sorter] " + statusMessagePrefix()
            + " Using streaming buffer size: " + m_bufferSizeI);
        }
      }

      InstanceHolder tempH = new InstanceHolder();
      tempH.m_instance = e.getInstance();
      tempH.m_fileNumber = -1; // unused here
      if (m_stringAttIndexes != null) {
        copyStringAttVals(tempH);
      }
      m_incrementalBuffer.add(tempH);
      m_streamCounter++;
    }

    if (e.getInstance() == null
      || e.getStatus() == InstanceEvent.BATCH_FINISHED) {
      emitBufferedInstances();
      // thread will set busy to false and report done status when
      // complete
      return;
    } else if (m_incrementalBuffer.size() == m_bufferSizeI) {
      // time to sort and write this to a temp file
      try {
        sortBuffer(true);
      } catch (Exception ex) {
        String msg = statusMessagePrefix()
          + "ERROR: unable to write to temp file.";
        // if (m_log != null) {
        // m_log.statusMessage(msg);
        // m_log.logMessage("[" + getCustomName() + "] " + msg);
        // }
        stopWithErrorMessage(msg, ex);

        // ex.printStackTrace();
        m_busy = false;
        return;
      }
    }

    m_busy = false;
  }

  /**
   * Performs the merge stage of the merge sort by opening all temp files and
   * interleaving the instances.
   */
  protected void emitBufferedInstances() {
    Thread t = new Thread() {
      @Override
      public void run() {

        int mergeCount = 0;

        if (m_incrementalBuffer.size() > 0 && !m_stopRequested.get()) {
          try {
            sortBuffer(false);
          } catch (Exception ex) {
          }

          if (m_bufferFiles.size() == 0) {
            // we only have the in memory buffer...
            if (m_stopRequested.get()) {
              m_busy = false;
              return;
            }
            String msg = statusMessagePrefix()
              + "Emitting in memory buffer....";
            if (m_log != null) {
              m_log.statusMessage(msg);
              m_log.logMessage("[" + getCustomName() + "] " + msg);
            }

            Instances newHeader = new Instances(
              m_incrementalBuffer.get(0).m_instance.dataset(), 0);
            m_ie.setStructure(newHeader);
            notifyInstanceListeners(m_ie);
            for (int i = 0; i < m_incrementalBuffer.size(); i++) {
              InstanceHolder currentH = m_incrementalBuffer.get(i);
              currentH.m_instance.setDataset(newHeader);

              if (m_stringAttIndexes != null) {
                for (String attName : m_stringAttIndexes.keySet()) {
                  boolean setValToZero = (newHeader.attribute(attName)
                    .numValues() > 0);

                  String valToSetInHeader = currentH.m_stringVals.get(attName);
                  newHeader.attribute(attName).setStringValue(valToSetInHeader);

                  if (setValToZero) {
                    currentH.m_instance.setValue(newHeader.attribute(attName),
                      0);
                  }
                }
              }

              if (m_stopRequested.get()) {
                m_busy = false;
                return;
              }
              m_ie.setInstance(currentH.m_instance);
              m_ie.setStatus(InstanceEvent.INSTANCE_AVAILABLE);
              if (i == m_incrementalBuffer.size() - 1) {
                m_ie.setStatus(InstanceEvent.BATCH_FINISHED);
              }
              notifyInstanceListeners(m_ie);
            }

            msg = statusMessagePrefix() + "Finished.";
            if (m_log != null) {
              m_log.statusMessage(msg);
              m_log.logMessage("[" + getCustomName() + "] " + msg);
            }
            m_busy = false;

            return;
          }
        }

        List<ObjectInputStream> inputStreams =
          new ArrayList<ObjectInputStream>();
        // for the interleaving part of the merge sort
        List<InstanceHolder> merger = new ArrayList<InstanceHolder>();

        Instances tempHeader = new Instances(m_connectedFormat, 0);
        m_ie.setStructure(tempHeader);
        notifyInstanceListeners(m_ie);

        // add an instance from the in-memory buffer first
        if (m_incrementalBuffer.size() > 0) {
          InstanceHolder tempH = m_incrementalBuffer.remove(0);
          merger.add(tempH);
        }

        if (m_stopRequested.get()) {
          m_busy = false;
          return;
        }

        if (m_bufferFiles.size() > 0) {
          String msg = statusMessagePrefix() + "Merging temp files...";
          if (m_log != null) {
            m_log.statusMessage(msg);
            m_log.logMessage("[" + getCustomName() + "] " + msg);
          }
        }
        // open all temp buffer files and read one instance from each
        for (int i = 0; i < m_bufferFiles.size(); i++) {

          ObjectInputStream ois = null;

          try {
            FileInputStream fis = new FileInputStream(m_bufferFiles.get(i));
            // GZIPInputStream giz = new GZIPInputStream(fis);
            BufferedInputStream bis = new BufferedInputStream(fis, 50000);
            ois = new ObjectInputStream(bis);

            InstanceHolder tempH = (InstanceHolder) ois.readObject();
            if (tempH != null) {
              inputStreams.add(ois);

              tempH.m_fileNumber = i;
              merger.add(tempH);
            } else {
              // no instances?!??
              ois.close();
            }
          } catch (Exception ex) {
            ex.printStackTrace();
            if (ois != null) {
              try {
                ois.close();
              } catch (Exception e) {
              }
            }
          }
        }
        Collections.sort(merger, m_sortComparator);

        do {
          if (m_stopRequested.get()) {
            m_busy = false;
            break;
          }

          InstanceHolder holder = merger.remove(0);
          holder.m_instance.setDataset(tempHeader);

          if (m_stringAttIndexes != null) {
            for (String attName : m_stringAttIndexes.keySet()) {
              boolean setValToZero =
                (tempHeader.attribute(attName).numValues() > 1);
              String valToSetInHeader = holder.m_stringVals.get(attName);
              tempHeader.attribute(attName).setStringValue(valToSetInHeader);

              if (setValToZero) {
                holder.m_instance.setValue(tempHeader.attribute(attName), 0);
              }
            }
          }

          if (m_stopRequested.get()) {
            m_busy = false;
            break;
          }
          m_ie.setInstance(holder.m_instance);
          m_ie.setStatus(InstanceEvent.INSTANCE_AVAILABLE);
          mergeCount++;
          notifyInstanceListeners(m_ie);

          if (mergeCount % m_bufferSizeI == 0 && m_log != null) {
            String msg = statusMessagePrefix() + "Merged " + mergeCount
              + " instances";
            if (m_log != null) {
              m_log.statusMessage(msg);
            }
          }

          int smallest = holder.m_fileNumber;

          // now get another instance from the source of "smallest"
          InstanceHolder nextH = null;
          if (smallest == -1) {
            if (m_incrementalBuffer.size() > 0) {
              nextH = m_incrementalBuffer.remove(0);
              nextH.m_fileNumber = -1;
            }
          } else {
            ObjectInputStream tis = inputStreams.get(smallest);

            try {
              InstanceHolder tempH = (InstanceHolder) tis.readObject();
              if (tempH != null) {
                nextH = tempH;
                nextH.m_fileNumber = smallest;
              } else {
                throw new Exception("end of buffer");
              }
            } catch (Exception ex) {
              // EOF
              try {
                if (m_log != null) {
                  String msg = statusMessagePrefix() + "Closing temp file";
                  m_log.statusMessage(msg);
                }
                tis.close();
              } catch (Exception e) {
              }
              File file = m_bufferFiles.remove(smallest);
              file.delete();
              inputStreams.remove(smallest);

              // update file numbers
              for (InstanceHolder h : merger) {
                if (h.m_fileNumber != -1 && h.m_fileNumber > smallest) {
                  h.m_fileNumber--;
                }
              }
            }
          }

          if (nextH != null) {
            // find the correct position (i.e. interleave) for this new Instance
            int index = Collections.binarySearch(merger, nextH,
              m_sortComparator);

            if (index < 0) {
              merger.add(index * -1 - 1, nextH);
            } else {
              merger.add(index, nextH);
            }
            nextH = null;
          }
        } while (merger.size() > 0 && !m_stopRequested.get());

        if (!m_stopRequested.get()) {
          // signal the end of the stream
          m_ie.setInstance(null);
          m_ie.setStatus(InstanceEvent.BATCH_FINISHED);
          notifyInstanceListeners(m_ie);

          String msg = statusMessagePrefix() + "Finished.";
          if (m_log != null) {
            m_log.statusMessage(msg);
            m_log.logMessage("[" + getCustomName() + "] " + msg);
          }
          m_busy = false;
        } else {
          // try and close any input streams...
          for (ObjectInputStream is : inputStreams) {
            try {
              is.close();
            } catch (Exception ex) {
            }
          }
          m_busy = false;
        }
      }
    };

    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
  }

  /**
   * Sorts the in-memory buffer
   * 
   * @param write whether to write the sorted buffer to a temp file
   * @throws Exception if a problem occurs
   */
  protected void sortBuffer(boolean write) throws Exception {

    String msg = statusMessagePrefix() + "Sorting in memory buffer....";
    if (m_log != null) {
      m_log.statusMessage(msg);
      m_log.logMessage("[" + getCustomName() + "] " + msg);
    }

    Collections.sort(m_incrementalBuffer, m_sortComparator);

    if (!write) {
      return;
    }

    String tmpDir = m_tempDirectory;
    File tempFile = File.createTempFile("Sorter", ".tmp");

    if (tmpDir != null && tmpDir.length() > 0) {
      try {
        tmpDir = m_env.substitute(tmpDir);

        File tempDir = new File(tmpDir);
        if (tempDir.exists() && tempDir.canWrite()) {
          String filename = tempFile.getName();
          File newFile = new File(tmpDir + File.separator + filename);
          tempFile = newFile;
          tempFile.deleteOnExit();
        }
      } catch (Exception ex) {
      }
    }

    if (!m_stopRequested.get()) {

      m_bufferFiles.add(tempFile);
      FileOutputStream fos = new FileOutputStream(tempFile);
      // GZIPOutputStream gzo = new GZIPOutputStream(fos);
      BufferedOutputStream bos = new BufferedOutputStream(fos, 50000);
      ObjectOutputStream oos = new ObjectOutputStream(bos);

      msg = statusMessagePrefix() + "Writing buffer to temp file "
        + m_bufferFiles.size() + "...";
      if (m_log != null) {
        m_log.statusMessage(msg);
        m_log.logMessage("[" + getCustomName() + "] " + msg);
      }

      for (int i = 0; i < m_incrementalBuffer.size(); i++) {
        InstanceHolder temp = m_incrementalBuffer.get(i);
        temp.m_instance.setDataset(null);
        oos.writeObject(temp);
        if (i % (m_bufferSizeI / 10) == 0) {
          oos.reset();
        }
      }

      bos.flush();
      oos.close();
    }
    m_incrementalBuffer.clear();
  }

  /**
   * Accept and process a test set event
   * 
   * @param e a <code>TestSetEvent</code> value
   */
  @Override
  public void acceptTestSet(TestSetEvent e) {
    Instances test = e.getTestSet();
    DataSetEvent d = new DataSetEvent(this, test);
    acceptDataSet(d);
  }

  /**
   * Accept and process a training set event
   * 
   * @param e a <code>TrainingSetEvent</code> value
   */
  @Override
  public void acceptTrainingSet(TrainingSetEvent e) {
    Instances train = e.getTrainingSet();
    DataSetEvent d = new DataSetEvent(this, train);
    acceptDataSet(d);
  }

  protected void init(Instances structure) {
    List<SortRule> sortRules = new ArrayList<SortRule>();

    if (m_sortDetails != null && m_sortDetails.length() > 0) {
      String[] sortParts = m_sortDetails.split("@@sort-rule@@");

      for (String s : sortParts) {
        SortRule r = new SortRule(s.trim());

        r.init(m_env, structure);
        sortRules.add(r);
      }

      m_sortComparator = new SortComparator(sortRules);
    }

    // check for string attributes
    m_stringAttIndexes = new HashMap<String, Integer>();
    for (int i = 0; i < structure.numAttributes(); i++) {
      if (structure.attribute(i).isString()) {
        m_stringAttIndexes.put(structure.attribute(i).name(), new Integer(i));
      }
    }
    if (m_stringAttIndexes.size() == 0) {
      m_stringAttIndexes = null;
    }
  }

  /**
   * Get the size of the in-memory buffer
   * 
   * @return the size of the in-memory buffer
   */
  public String getBufferSize() {
    return m_bufferSize;
  }

  /**
   * Set the size of the in-memory buffer
   * 
   * @param buffSize the size of the in-memory buffer
   */
  public void setBufferSize(String buffSize) {
    m_bufferSize = buffSize;
  }

  /**
   * Set the directory to use for temporary files during incremental operation
   * 
   * @param tempDir the temp dir to use
   */
  public void setTempDirectory(String tempDir) {
    m_tempDirectory = tempDir;
  }

  /**
   * Get the directory to use for temporary files during incremental operation
   * 
   * @return the temp dir to use
   */
  public String getTempDirectory() {
    return m_tempDirectory;
  }

  /**
   * Set the sort rules to use
   * 
   * @param sortDetails the sort rules in internal string representation
   */
  public void setSortDetails(String sortDetails) {
    m_sortDetails = sortDetails;
  }

  /**
   * Get the sort rules to use
   * 
   * @return the sort rules in internal string representation
   */
  public String getSortDetails() {
    return m_sortDetails;
  }

  /**
   * Accept and process a data set event
   * 
   * @param e a <code>DataSetEvent</code> value
   */
  @Override
  public void acceptDataSet(DataSetEvent e) {
    m_busy = true;
    m_stopRequested.set(false);

    if (m_log != null && e.getDataSet().numInstances() > 0) {
      m_log.statusMessage(statusMessagePrefix() + "Sorting batch...");
    }

    if (e.isStructureOnly()) {
      // nothing to sort!

      // just notify listeners of structure
      DataSetEvent d = new DataSetEvent(this, e.getDataSet());
      notifyDataListeners(d);

      m_busy = false;
      return;
    }

    try {
      init(new Instances(e.getDataSet(), 0));
    } catch (IllegalArgumentException ex) {
      if (m_log != null) {
        String message =
          "ERROR: There is a problem with the incoming instance structure";

        // m_log.statusMessage(statusMessagePrefix() + message
        // + " - see log for details");
        // m_log.logMessage(statusMessagePrefix() + message + " :"
        // + ex.getMessage());
        stopWithErrorMessage(message, ex);
        m_busy = false;
        return;
      }
    }

    List<InstanceHolder> instances = new ArrayList<InstanceHolder>();
    for (int i = 0; i < e.getDataSet().numInstances(); i++) {
      InstanceHolder h = new InstanceHolder();
      h.m_instance = e.getDataSet().instance(i);
      instances.add(h);
    }
    Collections.sort(instances, m_sortComparator);
    Instances output = new Instances(e.getDataSet(), 0);
    for (int i = 0; i < instances.size(); i++) {
      output.add(instances.get(i).m_instance);
    }

    DataSetEvent d = new DataSetEvent(this, output);
    notifyDataListeners(d);

    if (m_log != null) {
      m_log.statusMessage(statusMessagePrefix() + "Finished.");
    }
    m_busy = false;
  }

  /**
   * Add a datasource listener
   * 
   * @param dsl the datasource listener to add
   */
  @Override
  public void addDataSourceListener(DataSourceListener dsl) {
    m_dataListeners.add(dsl);
  }

  /**
   * Remove a datasource listener
   * 
   * @param dsl the datasource listener to remove
   */
  @Override
  public void removeDataSourceListener(DataSourceListener dsl) {
    m_dataListeners.remove(dsl);
  }

  /**
   * Add an instance listener
   * 
   * @param dsl the instance listener to add
   */
  @Override
  public void addInstanceListener(InstanceListener dsl) {
    m_instanceListeners.add(dsl);
  }

  /**
   * Remove an instance listener
   * 
   * @param dsl the instance listener to remove
   */
  @Override
  public void removeInstanceListener(InstanceListener dsl) {
    m_instanceListeners.remove(dsl);
  }

  /**
   * Use the default visual representation
   */
  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "Sorter.gif",
      BeanVisual.ICON_PATH + "Sorter_animated.gif");
    m_visual.setText("Sorter");
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
    if (m_listenee != null) {
      if (m_listenee instanceof BeanCommon) {
        ((BeanCommon) m_listenee).stop();
      }
    }

    if (m_log != null) {
      m_log.statusMessage(statusMessagePrefix() + "Stopped");
    }

    m_busy = false;
    m_stopRequested.set(true);
  }

  /**
   * Stops the step (and upstream ones) and then prints an error message and
   * optional exception message
   * 
   * @param error the error message to print
   * @param ex the optional exception
   */
  protected void stopWithErrorMessage(String error, Exception ex) {
    stop();
    if (m_log != null) {
      m_log.statusMessage(statusMessagePrefix() + error
        + " - see log for details");
      m_log.logMessage(statusMessagePrefix() + error
        + (ex != null ? " " + ex.getMessage() : ""));
    }
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
    if (!eventName.equals("instance") && !eventName.equals("dataSet")
      && !eventName.equals("trainingSet") && !eventName.equals("testSet")) {
      return false;
    }

    if (m_listenee != null) {
      return false;
    }

    return true;
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
    if (connectionAllowed(eventName)) {
      m_listenee = source;
      m_connectionType = eventName;
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
    if (source == m_listenee) {
      m_listenee = null;
    }
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

  protected String statusMessagePrefix() {
    return getCustomName() + "$" + hashCode() + "|";
  }

  private Instances getUpstreamStructure() {
    if (m_listenee != null && m_listenee instanceof StructureProducer) {
      return ((StructureProducer) m_listenee).getStructure(m_connectionType);
    }
    return null;
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
   *         event is not generated by this StructureProducer.
   */
  @Override
  public Instances getStructure(String eventName) {
    if (!eventName.equals("dataSet") && !eventName.equals("instance")) {
      return null;
    }

    if (eventName.equals("dataSet") && m_dataListeners.size() == 0) {
      return null;
    }

    if (eventName.equals("instance") && m_instanceListeners.size() == 0) {
      return null;
    }

    if (m_connectedFormat == null) {
      m_connectedFormat = getUpstreamStructure();
    }

    return m_connectedFormat;
  }

  /**
   * Returns the structure of the incoming instances (if any)
   * 
   * @return an <code>Instances</code> value
   */
  public Instances getConnectedFormat() {
    if (m_connectedFormat == null) {
      m_connectedFormat = getUpstreamStructure();
    }

    return m_connectedFormat;
  }

  /**
   * Set environment variables to use
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }
}
