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
 *    SubstringLabeler.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.beans.EventSetDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.Add;
import weka.gui.Logger;

/**
 * A bean that finds matches in string attribute values (using either substring
 * or regular expression matches) and labels the instance (sets the value of a
 * new attribute) according to the supplied label for the matching rule. The new
 * label attribute can be either multivalued nominal (if each match rule
 * specified has an explicit label associated with it) or, binary
 * numeric/nominal to indicate that one of the match rules has matched or not
 * matched.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 11956 $
 * 
 */
@KFStep(category = "Tools", toolTipText = "Label instances according to substring matches in String attributes")
public class SubstringLabeler extends JPanel implements BeanCommon, Visible,
  Serializable, InstanceListener, TrainingSetListener, TestSetListener,
  DataSourceListener, EventConstraints, EnvironmentHandler, DataSource {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 6297059699297260134L;

  /** Environment variables */
  protected transient Environment m_env;

  /** Internally encoded list of match rules */
  protected String m_matchDetails = "";

  /** Encapsulates our match rules */
  protected transient SubstringLabelerRules m_matches;

  /** Logging */
  protected transient Logger m_log;

  /** Busy indicator */
  protected transient boolean m_busy;

  /** Component talking to us */
  protected Object m_listenee;

  /** Downstream steps listening to instance events */
  protected ArrayList<InstanceListener> m_instanceListeners = new ArrayList<InstanceListener>();

  /** Downstream steps listening to data set events */
  protected ArrayList<DataSourceListener> m_dataListeners = new ArrayList<DataSourceListener>();

  /**
   * Whether to make the binary match/non-match attribute a nominal (rather than
   * numeric) binary attribute.
   */
  protected boolean m_nominalBinary;

  /**
   * For multi-valued labeled rules, whether or not to consume non-matching
   * instances or output them with missing value for the match attribute.
   */
  protected boolean m_consumeNonMatchingInstances;

  /** Add filter for adding the new attribute */
  protected Add m_addFilter;

  /** Name of the new attribute */
  protected String m_attName = "Match";

  /** The output structure */
  // protected Instances m_outputStructure;

  /** Instance event to use */
  protected InstanceEvent m_ie = new InstanceEvent(this);

  /**
   * Default visual filters
   */
  protected BeanVisual m_visual = new BeanVisual("SubstringLabeler",
    BeanVisual.ICON_PATH + "DefaultFilter.gif", BeanVisual.ICON_PATH
      + "DefaultFilter_animated.gif");

  /**
   * Constructor
   */
  public SubstringLabeler() {
    useDefaultVisual();
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);

    m_env = Environment.getSystemWide();
  }

  /**
   * Help information suitable for displaying in the GUI.
   * 
   * @return a description of this component
   */
  public String globalInfo() {
    return "Matches substrings in String attributes using "
      + "either literal or regular expression matches. "
      + "The value of a new attribute is set to reflect"
      + " the status of the match. The new attribute can "
      + "be either binary (in which case values indicate "
      + "match or no match) or multi-valued nominal, "
      + "in which case a label must be associated with each "
      + "distinct matching rule. In the case of labeled matches, "
      + "the user can opt to have non matching instances output "
      + "with missing value set for the new attribute or not"
      + " output at all (i.e. consumed by the step).";
  }

  /**
   * Set internally encoded list of match rules
   *
   * @param details the list of match rules
   */
  public void setMatchDetails(String details) {
    m_matchDetails = details;
  }

  /**
   * Get the internally encoded list of match rules
   *
   * @return the match rules
   */
  public String getMatchDetails() {
    return m_matchDetails;
  }

  /**
   * Set whether the new attribute created should be a nominal binary attribute
   * rather than a numeric binary attribute.
   *
   * @param nom true if the attribute should be a nominal binary one
   */
  public void setNominalBinary(boolean nom) {
    m_nominalBinary = nom;
  }

  /**
   * Get whether the new attribute created should be a nominal binary attribute
   * rather than a numeric binary attribute.
   *
   * @return true if the attribute should be a nominal binary one
   */
  public boolean getNominalBinary() {
    return m_nominalBinary;
  }

  /**
   * Set whether instances that do not match any of the rules should be
   * "consumed" rather than output with a missing value set for the new
   * attribute.
   *
   * @param consume true if non matching instances should be consumed by the
   *          component.
   */
  public void setConsumeNonMatching(boolean consume) {
    m_consumeNonMatchingInstances = consume;
  }

  /**
   * Get whether instances that do not match any of the rules should be
   * "consumed" rather than output with a missing value set for the new
   * attribute.
   *
   * @return true if non matching instances should be consumed by the component.
   */
  public boolean getConsumeNonMatching() {
    return m_consumeNonMatchingInstances;
  }

  public void setMatchAttributeName(String name) {
    m_attName = name;
  }

  public String getMatchAttributeName() {
    return m_attName;
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
   * Set environment variables to use
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
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

  /**
   * Use the default visual representation
   */
  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "DefaultFilter.gif",
      BeanVisual.ICON_PATH + "DefaultFilter_animated.gif");
    m_visual.setText("SubstringLabeler");
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

  /**
   * Make the output instances structure
   * 
   * @param inputStructure the incoming instances structure
   * @throws Exception if a problem occurs
   */
  protected void makeOutputStructure(Instances inputStructure) throws Exception {

    m_matches = new SubstringLabelerRules(m_matchDetails, m_attName,
      getConsumeNonMatching(), getNominalBinary(), inputStructure,
      statusMessagePrefix(), m_log, m_env);
    // m_matches.makeOutputStructure();
  }

  protected transient StreamThroughput m_throughput;

  /**
   * Accept and process an instance event
   * 
   * @param e the instance event to process
   */
  @Override
  public void acceptInstance(InstanceEvent e) {
    m_busy = true;

    if (e.getStatus() == InstanceEvent.FORMAT_AVAILABLE) {
      m_throughput = new StreamThroughput(statusMessagePrefix());

      Instances structure = e.getStructure();

      try {
        makeOutputStructure(structure);
      } catch (Exception ex) {
        String msg = statusMessagePrefix()
          + "ERROR: unable to create output instances structure.";
        if (m_log != null) {
          m_log.statusMessage(msg);
          m_log.logMessage("[SubstringLabeler] " + ex.getMessage());
        }
        stop();

        ex.printStackTrace();
        m_busy = false;
        return;
      }

      if (!e.m_formatNotificationOnly) {
        if (m_log != null) {
          m_log.statusMessage(statusMessagePrefix() + "Processing stream...");
        }
      }

      m_ie.setStructure(m_matches.getOutputStructure());
      m_ie.m_formatNotificationOnly = e.m_formatNotificationOnly;
      notifyInstanceListeners(m_ie);
    } else {
      Instance inst = e.getInstance();
      Instance out = null;
      if (inst != null) {
        m_throughput.updateStart();
        try {
          out = m_matches.makeOutputInstance(inst, false);
        } catch (Exception e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
        m_throughput.updateEnd(m_log);
      }

      if (inst == null || out != null
        || e.getStatus() == InstanceEvent.BATCH_FINISHED) { // consumed
        // notify listeners
        m_ie.setInstance(out);
        m_ie.setStatus(e.getStatus());
        notifyInstanceListeners(m_ie);
      }

      if (e.getStatus() == InstanceEvent.BATCH_FINISHED || inst == null) {
        // we're done
        m_throughput.finished(m_log);
      }
    }

    m_busy = false;
  }

  /**
   * Accept and process a data set event
   * 
   * @param e the data set event to process
   */
  @Override
  public void acceptDataSet(DataSetEvent e) {

    m_busy = true;
    if (m_log != null) {
      m_log.statusMessage(statusMessagePrefix() + "Processing batch...");
    }

    try {
      makeOutputStructure(new Instances(e.getDataSet(), 0));
    } catch (Exception ex) {
      String msg = statusMessagePrefix()
        + "ERROR: unable to create output instances structure.";
      if (m_log != null) {
        m_log.statusMessage(msg);
        m_log.logMessage("[SubstringLabeler] " + ex.getMessage());
      }
      stop();

      ex.printStackTrace();
      m_busy = false;
      return;
    }

    Instances toProcess = e.getDataSet();

    for (int i = 0; i < toProcess.numInstances(); i++) {
      Instance current = toProcess.instance(i);
      Instance result = null;
      try {
        result = m_matches.makeOutputInstance(current, true);
      } catch (Exception ex) {
        ex.printStackTrace();
      }

      if (result != null) {
        // m_outputStructure.add(result);
        m_matches.getOutputStructure().add(result);
      }
    }

    if (m_log != null) {
      m_log.statusMessage(statusMessagePrefix() + "Finished.");
    }

    // notify listeners
    DataSetEvent d = new DataSetEvent(this, m_matches.getOutputStructure());
    notifyDataListeners(d);

    m_busy = false;
  }

  /**
   * Accept and process a test set event
   * 
   * @param e the test set event to process
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
   * @parame e the training set event to process
   */
  @Override
  public void acceptTrainingSet(TrainingSetEvent e) {

    Instances train = e.getTrainingSet();
    DataSetEvent d = new DataSetEvent(this, train);
    acceptDataSet(d);
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

  protected String statusMessagePrefix() {
    return getCustomName() + "$" + hashCode() + "|";
  }
}
