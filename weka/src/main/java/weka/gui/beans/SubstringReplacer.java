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
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.beans.EventSetDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.JPanel;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Range;
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
 * @version $Revision: 9219 $
 */
@KFStep(category = "Tools", toolTipText = "Replace substrings in String attributes")
public class SubstringReplacer extends JPanel implements BeanCommon, Visible,
    Serializable, InstanceListener, EventConstraints, EnvironmentHandler,
    DataSource {

  /** For serialization */
  private static final long serialVersionUID = 5636877747903965818L;

  /**
   * Inner class encapsulating the logic for matching and replacing.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected static class MatchReplace {

    /** The substring literal/regex to use for matching */
    protected String m_match = "";

    /** The string to replace with */
    protected String m_replace = "";

    /** True if a regular expression match is to be used */
    protected boolean m_regex;

    /** Precompiled regex */
    protected Pattern m_regexPattern;

    /** True if case should be ignored when matching */
    protected boolean m_ignoreCase;

    /** The attributes to apply the match-replace rule to */
    protected String m_attsToApplyTo = "";

    protected String m_matchS;
    protected String m_replaceS;

    protected int[] m_selectedAtts;

    protected String m_statusMessagePrefix;
    protected Logger m_logger;

    /**
     * Constructor
     */
    public MatchReplace() {
    }

    /**
     * Constructor
     * 
     * @param setup an internally encoded representation of all the match and
     *          replace information for this rule
     */
    public MatchReplace(String setup) {
      parseFromInternal(setup);
    }

    /**
     * Constructor
     * 
     * @param match the match string
     * @param replace the replace string
     * @param regex true if this is a regular expression match
     * @param ignoreCase true if case is to be ignored
     * @param selectedAtts the attributes to apply the rule to
     */
    public MatchReplace(String match, String replace, boolean regex,
        boolean ignoreCase, String selectedAtts) {
      m_match = match;
      m_replace = replace;
      m_regex = regex;
      m_ignoreCase = ignoreCase;
      m_attsToApplyTo = selectedAtts;
    }

    protected void parseFromInternal(String setup) {
      String[] parts = setup.split("@@MR@@");
      if (parts.length < 4 || parts.length > 5) {
        throw new IllegalArgumentException(
            "Malformed match-replace definition: " + setup);
      }

      m_attsToApplyTo = parts[0].trim();
      m_regex = parts[1].trim().toLowerCase().equals("t");
      m_ignoreCase = parts[2].trim().toLowerCase().equals("t");
      m_match = parts[3].trim();

      if (m_match == null || m_match.length() == 0) {
        throw new IllegalArgumentException("Must provide something to match!");
      }

      if (parts.length == 5) {
        m_replace = parts[4];
      }
    }

    /**
     * Set the string/regex to use for matching
     * 
     * @param match the match string
     */
    public void setMatch(String match) {
      m_match = match;
    }

    /**
     * Get the string/regex to use for matching
     * 
     * @return the match string
     */
    public String getMatch() {
      return m_match;
    }

    /**
     * Set the replace string
     * 
     * @param replace the replace string
     */
    public void setReplace(String replace) {
      m_replace = replace;
    }

    /**
     * Get the replace string
     * 
     * @return the replace string
     */
    public String getReplace() {
      return m_replace;
    }

    /**
     * Set whether this is a regular expression match or not
     * 
     * @param regex true if this is a regular expression match
     */
    public void setRegex(boolean regex) {
      m_regex = regex;
    }

    /**
     * Get whether this is a regular expression match or not
     * 
     * @return true if this is a regular expression match
     */
    public boolean getRegex() {
      return m_regex;
    }

    /**
     * Set whether to ignore case when matching
     * 
     * @param ignore true if case is to be ignored
     */
    public void setIgnoreCase(boolean ignore) {
      m_ignoreCase = ignore;
    }

    /**
     * Get whether to ignore case when matching
     * 
     * @return true if case is to be ignored
     */
    public boolean getIgnoreCase() {
      return m_ignoreCase;
    }

    /**
     * Set the attributes to apply the rule to
     * 
     * @param a the attributes to apply the rule to.
     */
    public void setAttsToApplyTo(String a) {
      m_attsToApplyTo = a;
    }

    /**
     * Get the attributes to apply the rule to
     * 
     * @return the attributes to apply the rule to.
     */
    public String getAttsToApplyTo() {
      return m_attsToApplyTo;
    }

    /**
     * Initialize this match replace rule by substituting any environment
     * variables in the attributes, match and replace strings. Sets up the
     * attribute indices to apply to and validates that the selected attributes
     * are all String attributes
     * 
     * @param env the environment variables
     * @param structure the structure of the incoming instances
     */
    public void init(Environment env, Instances structure) {
      m_matchS = m_match;
      m_replaceS = m_replace;
      String attsToApplyToS = m_attsToApplyTo;

      try {
        m_matchS = env.substitute(m_matchS);
        m_replaceS = env.substitute(m_replace);
        attsToApplyToS = env.substitute(attsToApplyToS);
      } catch (Exception ex) {
      }

      if (m_regex) {
        String match = m_matchS;
        if (m_ignoreCase) {
          match = match.toLowerCase();
        }

        // precompile regular expression for speed
        m_regexPattern = Pattern.compile(match);
      }

      // Try a range first for the attributes
      String tempRangeS = attsToApplyToS;
      tempRangeS = tempRangeS.replace("/first", "first").replace("/last",
          "last");
      Range tempR = new Range();
      tempR.setRanges(attsToApplyToS);
      try {
        tempR.setUpper(structure.numAttributes() - 1);
        m_selectedAtts = tempR.getSelection();
      } catch (IllegalArgumentException ex) {
        // probably contains attribute names then
        m_selectedAtts = null;
      }

      if (m_selectedAtts == null) {
        // parse the comma separated list of attribute names
        Set<Integer> indexes = new HashSet<Integer>();
        String[] attParts = m_attsToApplyTo.split(",");
        for (String att : attParts) {
          att = att.trim();
          if (att.toLowerCase().equals("/first")) {
            indexes.add(0);
          } else if (att.toLowerCase().equals("/last")) {
            indexes.add((structure.numAttributes() - 1));
          } else {
            // try and find attribute
            if (structure.attribute(att) != null) {
              indexes.add(new Integer(structure.attribute(att).index()));
            } else {
              if (m_logger != null) {
                String msg = m_statusMessagePrefix + "Can't find attribute '"
                    + att + "in the incoming instances - ignoring";
                m_logger.logMessage(msg);
              }
            }
          }
        }

        m_selectedAtts = new int[indexes.size()];
        int c = 0;
        for (Integer i : indexes) {
          m_selectedAtts[c++] = i.intValue();
        }
      }

      // validate the types of the selected atts
      Set<Integer> indexes = new HashSet<Integer>();
      for (int i = 0; i < m_selectedAtts.length; i++) {
        if (structure.attribute(m_selectedAtts[i]).isString()) {
          indexes.add(m_selectedAtts[i]);
        } else {
          if (m_logger != null) {
            String msg = m_statusMessagePrefix + "Attribute '"
                + structure.attribute(m_selectedAtts[i]).name()
                + "is not a string attribute - " + "ignoring";
            m_logger.logMessage(msg);
          }
        }
      }

      // final array
      m_selectedAtts = new int[indexes.size()];
      int c = 0;
      for (Integer i : indexes) {
        m_selectedAtts[c++] = i.intValue();
      }
    }

    /**
     * Apply this rule to the supplied instance
     * 
     * @param inst the instance to apply to
     */
    public void apply(Instance inst) {

      for (int i = 0; i < m_selectedAtts.length; i++) {
        int numStringVals = inst.attribute(m_selectedAtts[i]).numValues();
        if (!inst.isMissing(m_selectedAtts[i])) {
          String value = inst.stringValue(m_selectedAtts[i]);
          value = apply(value);
          inst.dataset().attribute(m_selectedAtts[i]).setStringValue(value);

          // only set the index to zero if there were more than 1 string values
          // for this string attribute (meaning that although the data is
          // streaming
          // in, the user has opted to retain all string values in the header.
          // We
          // only operate in pure streaming - one string value in memory at any
          // one time - mode).

          // this check saves time (no new attribute vector created) if there is
          // only one value (i.e. index is already zero).
          if (numStringVals > 1) {
            inst.setValue(m_selectedAtts[i], 0);
          }
        }
      }
    }

    /**
     * Apply this rule to the supplied string
     * 
     * @param source the string to apply to
     * @return the source string with any matching substrings replaced.
     */
    protected String apply(String source) {
      String result = source;
      String match = m_matchS;
      if (m_ignoreCase) {
        result = result.toLowerCase();
        match = match.toLowerCase();
      }
      if (result != null && result.length() > 0) {
        if (m_regex) {
          // result = result.replaceAll(match, m_replaceS);
          result = m_regexPattern.matcher(result).replaceAll(m_replaceS);
        } else {
          result = result.replace(match, m_replaceS);
        }
      }

      return result;
    }

    /**
     * Return a textual description of this rule
     * 
     * @param a textual description of this rule
     */
    @Override
    public String toString() {
      // return a nicely formatted string for display
      // that shows all the details

      StringBuffer buff = new StringBuffer();
      buff.append((m_regex) ? "Regex: " : "Substring: ");
      buff.append(m_match).append(" --> ").append(m_replace).append("  ");
      buff.append((m_ignoreCase) ? "[ignore case]" : "").append("  ");
      buff.append("[Atts: " + m_attsToApplyTo + "]");

      return buff.toString();
    }

    protected String toStringInternal() {

      // return a string in internal format that is
      // easy to parse all the data out of
      StringBuffer buff = new StringBuffer();
      buff.append(m_attsToApplyTo).append("@@MR@@");
      buff.append((m_regex) ? "t" : "f").append("@@MR@@");
      buff.append((m_ignoreCase) ? "t" : "f").append("@@MR@@");
      buff.append(m_match).append("@@MR@@");
      buff.append(m_replace);

      return buff.toString();
    }
  }

  /** Environment variables */
  protected transient Environment m_env;

  /** Internally encoded list of match-replace rules */
  protected String m_matchReplaceDetails = "";

  /** Temporary list of match-replace rules */
  protected transient List<MatchReplace> m_mr;

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

      /*
       * if (m_matchReplaceDetails == null || m_matchReplaceDetails.length() ==
       * 0) { stop(); String msg = statusMessagePrefix() +
       * "ERROR No match and replace details have been " + "specified!"; if
       * (m_log != null) { m_log.statusMessage(msg);
       * m_log.logMessage("[SubstringReplacer] " + msg); } m_busy = false;
       * return; }
       */
      m_mr = new ArrayList<MatchReplace>();
      if (m_matchReplaceDetails != null && m_matchReplaceDetails.length() > 0) {

        String[] mrParts = m_matchReplaceDetails.split("@@match-replace@@");
        for (String p : mrParts) {
          MatchReplace mr = new MatchReplace(p.trim());
          mr.m_statusMessagePrefix = statusMessagePrefix();
          mr.m_logger = m_log;
          mr.init(m_env, structure);
          m_mr.add(mr);
        }
      }

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
        for (MatchReplace mr : m_mr) {
          mr.apply(inst);
        }
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
