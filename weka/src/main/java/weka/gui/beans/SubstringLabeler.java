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

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Range;
import weka.core.SerializedObject;
import weka.core.Utils;
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
 * @version $Revision: 9219 $
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

  /**
   * Inner class encapsulating the logic for matching
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected static class Match {
    /** The substring literal/regex to use for matching */
    protected String m_match = "";

    protected String m_label = "";

    /** True if a regular expression match is to be used */
    protected boolean m_regex;

    /** True if case should be ignored when matching */
    protected boolean m_ignoreCase;

    /** Precompiled regex pattern */
    protected Pattern m_regexPattern;

    /** The attributes to apply the match-replace rule to */
    protected String m_attsToApplyTo = "";

    protected String m_matchS;
    protected String m_labelS;

    protected int[] m_selectedAtts;

    protected String m_statusMessagePrefix;
    protected Logger m_logger;

    /**
     * Constructor
     */
    public Match() {
    }

    /**
     * Constructor
     * 
     * @param setup an internally encoded representation of all the match
     *          information for this rule
     */
    public Match(String setup) {
      parseFromInternal(setup);
    }

    /**
     * Constructor
     * 
     * @param match the match string
     * @param regex true if this is a regular expression match
     * @param ignoreCase true if case is to be ignored
     * @param selectedAtts the attributes to apply the rule to
     */
    public Match(String match, boolean regex, boolean ignoreCase,
        String selectedAtts) {
      m_match = match;
      m_regex = regex;
      m_ignoreCase = ignoreCase;
      m_attsToApplyTo = selectedAtts;
    }

    protected void parseFromInternal(String setup) {
      String[] parts = setup.split("@@MR@@");
      if (parts.length < 4 || parts.length > 5) {
        throw new IllegalArgumentException("Malformed match definition: "
            + setup);
      }

      m_attsToApplyTo = parts[0].trim();
      m_regex = parts[1].trim().toLowerCase().equals("t");
      m_ignoreCase = parts[2].trim().toLowerCase().equals("t");
      m_match = parts[3].trim();

      if (m_match == null || m_match.length() == 0) {
        throw new IllegalArgumentException("Must provide something to match!");
      }

      if (parts.length == 5) {
        m_label = parts[4].trim();
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
     * Set the label to assign if this rule matches, or empty string if binary
     * flag attribute is being created.
     * 
     * @param label the label string or empty string
     */
    public void setLabel(String label) {
      m_label = label;
    }

    /**
     * Get the label to assign if this rule matches, or empty string if binary
     * flag attribute is being created.
     * 
     * @return the label string or empty string
     */
    public String getLabel() {
      return m_label;
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
     * Initialize this match rule by substituting any environment variables in
     * the attributes, match and label strings. Sets up the attribute indices to
     * apply to and validates that the selected attributes are all String
     * attributes
     * 
     * @param env the environment variables
     * @param structure the structure of the incoming instances
     */
    public void init(Environment env, Instances structure) {
      m_matchS = m_match;
      m_labelS = m_label;
      String attsToApplyToS = m_attsToApplyTo;

      try {
        m_matchS = env.substitute(m_matchS);
        m_labelS = env.substitute(m_labelS);
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
     * 
     * @return the label (or empty string) if this rule matches (empty string is
     *         used to indicate a match in the case that a binary flag attribute
     *         is being created), or null if the rule doesn't match.
     */
    public String apply(Instance inst) {
      for (int i = 0; i < m_selectedAtts.length; i++) {
        if (!inst.isMissing(m_selectedAtts[i])) {
          String value = inst.stringValue(m_selectedAtts[i]);

          String result = apply(value);
          if (result != null) {
            // first match is good enough
            return result;
          }
        }
      }

      return null;
    }

    /**
     * Apply this rule to the supplied string
     * 
     * @param source the string to apply to
     * @return the label (or empty string) if this rule matches (empty string is
     *         used to indicate a match in the case that a binary flag attribute
     *         is being created), or null if the rule doesn't match.
     */
    protected String apply(String source) {
      String result = source;
      String match = m_matchS;
      boolean ruleMatches = false;
      if (m_ignoreCase) {
        result = result.toLowerCase();
        match = match.toLowerCase();
      }
      if (result != null && result.length() > 0) {
        if (m_regex) {
          if (m_regexPattern.matcher(result).matches()) {
            // if (result.matches(match)) {
            ruleMatches = true;
          }
        } else {
          ruleMatches = (result.indexOf(match) >= 0);
        }
      }

      return (ruleMatches) ? m_label : null;
    }

    /**
     * Return a textual description of this match rule
     * 
     * @return a textual description of this match rule
     */
    @Override
    public String toString() {
      // return a nicely formatted string for display
      // that shows all the details

      StringBuffer buff = new StringBuffer();
      buff.append((m_regex) ? "Regex: " : "Substring: ");
      buff.append(m_match).append("  ");
      buff.append((m_ignoreCase) ? "[ignore case]" : "").append("  ");
      if (m_label != null && m_label.length() > 0) {
        buff.append("Label: ").append(m_label).append("  ");
      }
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
      buff.append(m_label);

      return buff.toString();
    }
  }

  /** Environment variables */
  protected transient Environment m_env;

  /** Internally encoded list of match rules */
  protected String m_matchDetails = "";

  /** Temporary list of match-replace rules */
  protected transient List<Match> m_matchRules;

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

  /**
   * Whether the match rules all have labels or not. If not, then the new
   * attribute is a binary match/no-match one
   */
  protected boolean m_hasLabels;

  /** Add filter for adding the new attribute */
  protected Add m_addFilter;

  /** Name of the new attribute */
  protected String m_attName = "Match";

  /** The output structure */
  protected Instances m_outputStructure;

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

    m_matchRules = new ArrayList<Match>();
    if (m_matchDetails != null && m_matchDetails.length() > 0) {

      String[] matchParts = m_matchDetails.split("@@match-rule@@");
      for (String p : matchParts) {
        Match m = new Match(p.trim());
        m.m_statusMessagePrefix = statusMessagePrefix();
        m.m_logger = m_log;
        m.init(m_env, inputStructure);
        m_matchRules.add(m);
      }

      int labelCount = 0;
      // StringBuffer labelList = new StringBuffer();
      HashSet<String> uniqueLabels = new HashSet<String>();
      FastVector labelVec = new FastVector();
      for (Match m : m_matchRules) {
        if (m.getLabel() != null && m.getLabel().length() > 0) {
          if (!uniqueLabels.contains(m.getLabel())) {
            /*
             * if (labelCount > 0) { labelList.append(","); }
             */
            // labelList.append(m.getLabel());
            uniqueLabels.add(m.getLabel());
            labelVec.addElement(m.getLabel());
          }
          labelCount++;
        }
      }

      if (labelCount > 0) {
        if (labelCount == m_matchRules.size()) {
          m_hasLabels = true;
        } else {
          throw new Exception("Can't have only some rules with a label!");
        }
      }

      m_outputStructure = (Instances) (new SerializedObject(inputStructure)
          .getObject());
      Attribute newAtt = null;
      if (m_hasLabels) {
        newAtt = new Attribute(m_attName, labelVec);
      } else if (getNominalBinary()) {
        labelVec.addElement("0");
        labelVec.addElement("1");
        newAtt = new Attribute(m_attName, labelVec);
      } else {
        newAtt = new Attribute(m_attName);
      }

      m_outputStructure.insertAttributeAt(newAtt,
          m_outputStructure.numAttributes());

      /*
       * // make the output structure m_addFilter = new Add();
       * m_addFilter.setAttributeName(m_attName); if (m_hasLabels) {
       * m_addFilter.setNominalLabels(labelList.toString()); } else if
       * (getNominalBinary()) { m_addFilter.setNominalLabels("0,1"); }
       * m_addFilter.setInputFormat(inputStructure); m_outputStructure =
       * Filter.useFilter(inputStructure, m_addFilter);
       */

      return;
    }

    m_outputStructure = new Instances(inputStructure);
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

      m_ie.setStructure(m_outputStructure);
      m_ie.m_formatNotificationOnly = e.m_formatNotificationOnly;
      notifyInstanceListeners(m_ie);
    } else {
      Instance inst = e.getInstance();
      Instance out = null;
      if (inst != null) {
        m_throughput.updateStart();
        out = makeOutputInstance(inst, false);
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
   * Process and input instance and return an output instance
   * 
   * @param inputI the incoming instance
   * @param batch whether this is being processed as part of a batch of
   *          instances
   * 
   * @return the output instance
   */
  protected Instance makeOutputInstance(Instance inputI, boolean batch) {
    int newAttIndex = m_outputStructure.numAttributes() - 1;

    Instance result = inputI;
    if (m_matchRules.size() > 0) {
      String label = null;
      for (Match m : m_matchRules) {
        label = m.apply(inputI);

        if (label != null) {
          break;
        }
      }

      double[] vals = new double[m_outputStructure.numAttributes()];
      for (int i = 0; i < inputI.numAttributes(); i++) {
        if (!inputI.attribute(i).isString()) {
          vals[i] = inputI.value(i);
        } else {
          if (!batch) {
            vals[i] = 0;
            String v = inputI.stringValue(i);
            m_outputStructure.attribute(i).setStringValue(v);
          } else {
            String v = inputI.stringValue(i);
            vals[i] = m_outputStructure.attribute(i).addStringValue(v);
          }
        }
      }

      if (label != null) {
        if (m_hasLabels) {
          vals[newAttIndex] = m_outputStructure.attribute(m_attName)
              .indexOfValue(label);
        } else {
          vals[newAttIndex] = 1;
        }
      } else { // non match
        if (m_hasLabels) {
          if (!getConsumeNonMatching()) {
            vals[newAttIndex] = Utils.missingValue();
          } else {
            return null;
          }
        } else {
          vals[newAttIndex] = 0;
        }
      }

      result = new DenseInstance(1.0, vals);
      result.setDataset(m_outputStructure);
    }

    return result;
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
      Instance result = makeOutputInstance(current, true);

      if (result != null) {
        m_outputStructure.add(result);
      }
    }

    if (m_log != null) {
      m_log.statusMessage(statusMessagePrefix() + "Finished.");
    }

    // notify listeners
    DataSetEvent d = new DataSetEvent(this, m_outputStructure);
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
