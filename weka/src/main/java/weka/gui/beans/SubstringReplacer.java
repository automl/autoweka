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
 *    SubstringReplacer.java
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
import weka.gui.Logger;

/**
 * A bean that can replace substrings in the values of string attributes.
 * Multiple match and replace "rules" can be specified - these get applied in
 * the order that they are defined. Each rule can be applied to one or more
 * user-specified input String attributes. Attributes can be specified using
 * either a range list (e.g 1,2-10,last) or by a comma separated list of
 * attribute names (where "/first" and "/last" are special strings indicating
 * the first and last attribute respectively).
 * 
 * Matching can be by string literal or by regular expression.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 11929 $
 */
@KFStep(category = "Tools", toolTipText = "Replace substrings in String attributes")
public class SubstringReplacer extends JPanel implements BeanCommon, Visible,
  Serializable, InstanceListener, EventConstraints, EnvironmentHandler,
  DataSource {

  /** For serialization */
  private static final long serialVersionUID = 5636877747903965818L;

  /** Environment variables */
  protected transient Environment m_env;

  /** Internally encoded list of match-replace rules */
  protected String m_matchReplaceDetails = "";

  /** Temporary list of match-replace rules */
  // protected transient List<SubstringReplacerMatchRule> m_mr;

  protected transient SubstringReplacerRules m_mr;

  /** Logging */
  protected transient Logger m_log;

  /** Busy indicator */
  protected transient boolean m_busy;

  /** Component sending us instances */
  protected Object m_listenee;

  /** Downstream steps listening to instance events */
  protected ArrayList<InstanceListener> m_instanceListeners = new ArrayList<InstanceListener>();

  /** Instance event to use */
  protected InstanceEvent m_ie = new InstanceEvent(this);

  /**
   * Default visual filters
   */
  protected BeanVisual m_visual = new BeanVisual("SubstringReplacer",
    BeanVisual.ICON_PATH + "DefaultFilter.gif", BeanVisual.ICON_PATH
      + "DefaultFilter_animated.gif");

  /**
   * Constructs a new SubstringReplacer
   */
  public SubstringReplacer() {
    useDefaultVisual();
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);

    m_env = Environment.getSystemWide();
  }

  /**
   * About information
   * 
   * @return about information
   */
  public String globalInfo() {
    return "Replaces substrings in String attribute values "
      + "using either literal match and replace or "
      + "regular expression matching. The attributes"
      + "to apply the match and replace rules to "
      + "can be selected via a range string (e.g "
      + "1-5,6,last) or by a comma separated list "
      + "of attribute names (/first and /last can be"
      + " used to indicate the first and last attribute " + "respectively)";
  }

  /**
   * Set internally encoded list of match-replace rules
   * 
   * @param details the list of match-replace rules
   */
  public void setMatchReplaceDetails(String details) {
    m_matchReplaceDetails = details;
  }

  /**
   * Get the internally encoded list of match-replace rules
   * 
   * @return the match-replace rules
   */
  public String getMatchReplaceDetails() {
    return m_matchReplaceDetails;
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

    if (!eventName.equals("instance")) {
      return false;
    }

    if (m_listenee instanceof EventConstraints) {
      if (!((EventConstraints) m_listenee).eventGeneratable(eventName)) {
        return false;
      }
    }

    return true;
  }

  protected transient StreamThroughput m_throughput;

  /**
   * Accept and process an instance event
   * 
   * @param e an <code>InstanceEvent</code> value
   */
  @Override
  public synchronized void acceptInstance(InstanceEvent e) {
    m_busy = true;
    if (e.getStatus() == InstanceEvent.FORMAT_AVAILABLE) {
      m_throughput = new StreamThroughput(statusMessagePrefix());
      Instances structure = e.getStructure();

      m_mr = new SubstringReplacerRules(m_matchReplaceDetails, structure,
        statusMessagePrefix(), m_log, m_env);

      // m_mr = new ArrayList<SubstringReplacerMatchRule>();
      // if (m_matchReplaceDetails != null && m_matchReplaceDetails.length() >
      // 0) {
      //
      // String[] mrParts = m_matchReplaceDetails.split("@@match-replace@@");
      // for (String p : mrParts) {
      // SubstringReplacerMatchRule mr = new SubstringReplacerMatchRule(
      // p.trim());
      // mr.m_statusMessagePrefix = statusMessagePrefix();
      // mr.m_logger = m_log;
      // mr.init(m_env, structure);
      // m_mr.add(mr);
      // }
      // }

      if (!e.m_formatNotificationOnly) {
        if (m_log != null) {
          m_log.statusMessage(statusMessagePrefix() + "Processing stream...");
        }
      }

      // pass structure on downstream
      m_ie.setStructure(structure);
      m_ie.m_formatNotificationOnly = e.m_formatNotificationOnly;
      notifyInstanceListeners(m_ie);
    } else {
      Instance inst = e.getInstance();
      // System.err.println("got : " + inst.toString());
      if (inst != null) {
        m_throughput.updateStart();
        inst = m_mr.makeOutputInstance(inst);
        // m_mr.applyRules(inst);
        // for (SubstringReplacerMatchRule mr : m_mr) {
        // mr.apply(inst);
        // }
        m_throughput.updateEnd(m_log);
      }

      // notify listeners
      m_ie.setInstance(inst);
      m_ie.setStatus(e.getStatus());
      notifyInstanceListeners(m_ie);

      if (e.getStatus() == InstanceEvent.BATCH_FINISHED || inst == null) {
        // we're done
        m_throughput.finished(m_log);
      }
    }

    m_busy = false;
  }

  /**
   * Use the default visual representation
   */
  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "DefaultFilter.gif",
      BeanVisual.ICON_PATH + "DefaultFilter_animated.gif");
    m_visual.setText("SubstringReplacer");

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

    if (!eventName.equals("instance")) {
      return false;
    }

    if (m_listenee != null) {
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
   * Set environment variables to use
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  protected String statusMessagePrefix() {
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
   * Add a data source listener
   * 
   * @param dsl a <code>DataSourceListener</code> value
   */
  @Override
  public void addDataSourceListener(DataSourceListener dsl) {
  }

  /**
   * Remove a data source listener
   * 
   * @param dsl a <code>DataSourceListener</code> value
   */
  @Override
  public void removeDataSourceListener(DataSourceListener dsl) {
  }
}
