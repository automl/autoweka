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
 *    FlowByExpression.java
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.beans.EventSetDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import weka.core.Attribute;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.gui.Logger;

/**
 * A bean that splits incoming instances (or instance streams) according to the
 * evaluation of a logical expression. The expression can test the values of one
 * or more incoming attributes. The test can involve constants or comparing one
 * attribute's values to another. Inequalities along with string operations such
 * as contains, starts-with, ends-with and regular expressions may be used as
 * operators.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 10220 $
 */
@KFStep(category = "Flow", toolTipText = "Route instances according to a boolean expression")
public class FlowByExpression extends JPanel implements BeanCommon, Visible,
  Serializable, InstanceListener, TrainingSetListener, TestSetListener,
  DataSourceListener, EventConstraints, EnvironmentHandler, DataSource,
  StructureProducer {

  /** Added ID to avoid warning */
  private static final long serialVersionUID = 2492050246494259885L;

  /**
   * Abstract base class for parts of a boolean expression.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected static abstract class ExpressionNode implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = -8427857202322768762L;

    /** boolean operator for combining with result so far */
    protected boolean m_isAnOr;

    /** is this node negated? */
    protected boolean m_isNegated;

    /** Environment variables */
    protected transient Environment m_env;

    /** Whether to show the combination operator in the textual representation */
    protected boolean m_showAndOr = true;

    /**
     * Set whether this node is to be OR'ed to the result so far
     * 
     * @param isOr true if this node is to be OR'd
     */
    public void setIsOr(boolean isOr) {
      m_isAnOr = isOr;
    }

    /**
     * Get whether this node is to be OR'ed
     * 
     * @return true if this node is to be OR'ed with the result so far
     */
    public boolean isOr() {
      return m_isAnOr;
    }

    /**
     * Get whether this node is negated.
     * 
     * @return
     */
    public boolean isNegated() {
      return m_isNegated;
    }

    /**
     * Set whether this node is negated
     * 
     * @param negated true if this node is negated
     */
    public void setNegated(boolean negated) {
      m_isNegated = negated;
    }

    /**
     * Set whether to show the combination operator in the textual description
     * 
     * @param show true if the combination operator is to be shown
     */
    public void setShowAndOr(boolean show) {
      m_showAndOr = show;
    }

    /**
     * Initialize the node
     * 
     * @param structure the structure of the incoming instances
     * @param env Environment variables
     */
    public void init(Instances structure, Environment env) {
      m_env = env;
    }

    /**
     * Evaluate this node and combine with the result so far
     * 
     * @param inst the incoming instance to evalute with
     * @param result the result to combine with
     * @return the result after combining with this node
     */
    public abstract boolean evaluate(Instance inst, boolean result);

    /**
     * Get the internal representation of this node
     * 
     * @param buff the string buffer to append to
     */
    protected abstract void toStringInternal(StringBuffer buff);

    /**
     * Get the display representation of this node
     * 
     * @param buff the string buffer to append to
     */
    public abstract void toStringDisplay(StringBuffer buff);

    /**
     * Parse and initialize from the internal representation
     * 
     * @param expression the expression to parse in internal representation
     * @return the remaining parts of the expression after parsing and removing
     *         the part for this node
     */
    protected abstract String parseFromInternal(String expression);

    /**
     * Get a DefaultMutableTreeNode for this node
     * 
     * @param parent the parent of this node (if any)
     * @return the DefaultMutableTreeNode for this node
     */
    public abstract DefaultMutableTreeNode toJTree(DefaultMutableTreeNode parent);
  }

  /**
   * An expression node that encloses other expression nodes in brackets
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected static class BracketNode extends ExpressionNode implements
    Serializable {

    /** For serialization */
    private static final long serialVersionUID = 8732159083173001115L;

    protected List<ExpressionNode> m_children = new ArrayList<ExpressionNode>();

    @Override
    public void init(Instances structure, Environment env) {
      super.init(structure, env);

      for (ExpressionNode n : m_children) {
        n.init(structure, env);
      }
    }

    @Override
    public boolean evaluate(Instance inst, boolean result) {

      boolean thisNode = true;
      if (m_children.size() > 0) {
        for (ExpressionNode n : m_children) {
          thisNode = n.evaluate(inst, thisNode);
        }
        if (isNegated()) {
          thisNode = !thisNode;
        }
      }

      return (isOr() ? (result || thisNode) : (result && thisNode));
    }

    /**
     * Add a child to this bracket node
     * 
     * @param child the ExpressionNode to add
     */
    public void addChild(ExpressionNode child) {
      m_children.add(child);

      if (m_children.size() > 0) {
        m_children.get(0).setShowAndOr(false);
      }
    }

    /**
     * Remove a child from this bracket node
     * 
     * @param child the ExpressionNode to remove
     */
    public void removeChild(ExpressionNode child) {
      m_children.remove(child);

      if (m_children.size() > 0) {
        m_children.get(0).setShowAndOr(false);
      }
    }

    @Override
    public String toString() {
      // just the representation of this node (suitable for the abbreviated
      // JTree node label

      String result = "( )";
      if (isNegated()) {
        result = "!" + result;
      }

      if (m_showAndOr) {
        if (m_isAnOr) {
          result = "|| " + result;
        } else {
          result = "&& " + result;
        }
      }

      return result;
    }

    @Override
    public DefaultMutableTreeNode toJTree(DefaultMutableTreeNode parent) {

      DefaultMutableTreeNode current = new DefaultMutableTreeNode(this);
      if (parent != null) {
        parent.add(current);
      }

      for (ExpressionNode child : m_children) {
        child.toJTree(current);
      }

      return current;
    }

    private void toString(StringBuffer buff, boolean internal) {
      if (m_children.size() >= 0) {
        if (internal || m_showAndOr) {
          if (m_isAnOr) {
            buff.append("|| ");
          } else {
            buff.append("&& ");
          }
        }

        if (isNegated()) {
          buff.append("!");
        }
        buff.append("(");

        int count = 0;
        for (ExpressionNode child : m_children) {
          if (internal) {
            child.toStringInternal(buff);
          } else {
            child.toStringDisplay(buff);
          }
          count++;
          if (count != m_children.size()) {
            buff.append(" ");
          }
        }
        buff.append(")");
      }
    }

    @Override
    public void toStringDisplay(StringBuffer buff) {
      toString(buff, false);
    }

    @Override
    protected void toStringInternal(StringBuffer buff) {
      toString(buff, true);
    }

    @Override
    protected String parseFromInternal(String expression) {
      if (expression.startsWith("|| ")) {
        m_isAnOr = true;
      }

      if (expression.startsWith("|| ") || expression.startsWith("&& ")) {
        expression = expression.substring(3, expression.length());
      }

      if (expression.charAt(0) == '!') {
        setNegated(true);
        expression = expression.substring(1, expression.length());
      }

      if (expression.charAt(0) != '(') {
        throw new IllegalArgumentException(
          "Malformed expression! Was expecting a \"(\"");
      }

      expression = expression.substring(1, expression.length());

      while (expression.charAt(0) != ')') {
        int offset = 3;

        if (expression.charAt(offset) == '(') {
          ExpressionNode child = new BracketNode();
          expression = child.parseFromInternal(expression);
          m_children.add(child);
        } else {
          // must be an ExpressionClause
          ExpressionNode child = new ExpressionClause();
          expression = child.parseFromInternal(expression);
          m_children.add(child);
        }
      }

      if (m_children.size() > 0) {
        m_children.get(0).setShowAndOr(false);
      }

      return expression;
    }
  }

  protected static class ExpressionClause extends ExpressionNode implements
    Serializable {

    /** For serialization */
    private static final long serialVersionUID = 2754006654981248325L;

    /** The operator for this expression */
    protected ExpressionType m_operator;

    /** The name of the lhs attribute */
    protected String m_lhsAttributeName;

    /** The index of the lhs attribute */
    protected int m_lhsAttIndex = -1;

    /** The rhs operand (constant value or attribute name) */
    protected String m_rhsOperand;

    /** True if the rhs operand is an attribute */
    protected boolean m_rhsIsAttribute;

    /** index of the rhs if it is an attribute */
    protected int m_rhsAttIndex = -1;

    /** The name of the lhs attribute after resolving variables */
    protected String m_resolvedLhsName;

    /** The rhs operand after resolving variables */
    protected String m_resolvedRhsOperand;

    /** the compiled regex pattern (if the operator is REGEX) */
    protected Pattern m_regexPattern;

    /** The rhs operand (if constant and is a number ) */
    protected double m_numericOperand;

    public static enum ExpressionType {
      EQUALS(" = ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          if (rhsIsAttribute) {
            if (inst.isMissing(lhsAttIndex) && inst.isMissing(rhsAttIndex)) {
              return true;
            }
            if (inst.isMissing(lhsAttIndex) || inst.isMissing(rhsAttIndex)) {
              return false;
            }
            return Utils.eq(inst.value(lhsAttIndex), inst.value(rhsAttIndex));
          }

          if (inst.isMissing(lhsAttIndex)) {
            return false;
          }
          return (Utils.eq(inst.value(lhsAttIndex), numericOperand));
        }
      },
      NOTEQUAL(" != ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          return !EQUALS.evaluate(inst, lhsAttIndex, rhsOperand,
            numericOperand, regexPattern, rhsIsAttribute, rhsAttIndex);
        }
      },
      LESSTHAN(" < ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          if (rhsIsAttribute) {
            if (inst.isMissing(lhsAttIndex) || inst.isMissing(rhsAttIndex)) {
              return false;
            }
            return (inst.value(lhsAttIndex) < inst.value(rhsAttIndex));
          }

          if (inst.isMissing(lhsAttIndex)) {
            return false;
          }
          return (inst.value(lhsAttIndex) < numericOperand);
        }
      },
      LESSTHANEQUAL(" <= ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          if (rhsIsAttribute) {
            if (inst.isMissing(lhsAttIndex) || inst.isMissing(rhsAttIndex)) {
              return false;
            }
            return (inst.value(lhsAttIndex) <= inst.value(rhsAttIndex));
          }

          if (inst.isMissing(lhsAttIndex)) {
            return false;
          }
          return (inst.value(lhsAttIndex) <= numericOperand);
        }
      },
      GREATERTHAN(" > ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          return !LESSTHANEQUAL.evaluate(inst, lhsAttIndex, rhsOperand,
            numericOperand, regexPattern, rhsIsAttribute, rhsAttIndex);
        }
      },
      GREATERTHANEQUAL(" >= ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          return !LESSTHAN.evaluate(inst, lhsAttIndex, rhsOperand,
            numericOperand, regexPattern, rhsIsAttribute, rhsAttIndex);
        }
      },
      ISMISSING(" isMissing ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          return (inst.isMissing(lhsAttIndex));
        }
      },
      CONTAINS(" contains ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          if (inst.isMissing(lhsAttIndex)) {
            return false;
          }

          String lhsString = "";
          try {
            lhsString = inst.stringValue(lhsAttIndex);
          } catch (IllegalArgumentException ex) {
            return false;
          }

          if (rhsIsAttribute) {
            if (inst.isMissing(rhsAttIndex)) {
              return false;
            }

            try {
              String rhsString = inst.stringValue(rhsAttIndex);

              return lhsString.contains(rhsString);
            } catch (IllegalArgumentException ex) {
              return false;
            }
          }

          return lhsString.contains(rhsOperand);
        }
      },
      STARTSWITH(" startsWith ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          if (inst.isMissing(lhsAttIndex)) {
            return false;
          }

          String lhsString = "";
          try {
            lhsString = inst.stringValue(lhsAttIndex);
          } catch (IllegalArgumentException ex) {
            return false;
          }

          if (rhsIsAttribute) {
            if (inst.isMissing(rhsAttIndex)) {
              return false;
            }

            try {
              String rhsString = inst.stringValue(rhsAttIndex);

              return lhsString.startsWith(rhsString);
            } catch (IllegalArgumentException ex) {
              return false;
            }
          }

          return lhsString.startsWith(rhsOperand);
        }
      },
      ENDSWITH(" endsWith ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          if (inst.isMissing(lhsAttIndex)) {
            return false;
          }

          String lhsString = "";
          try {
            lhsString = inst.stringValue(lhsAttIndex);
          } catch (IllegalArgumentException ex) {
            return false;
          }

          if (rhsIsAttribute) {
            if (inst.isMissing(rhsAttIndex)) {
              return false;
            }

            try {
              String rhsString = inst.stringValue(rhsAttIndex);

              return lhsString.endsWith(rhsString);
            } catch (IllegalArgumentException ex) {
              return false;
            }
          }

          return lhsString.endsWith(rhsOperand);
        }
      },
      REGEX(" regex ") {
        @Override
        boolean evaluate(Instance inst, int lhsAttIndex, String rhsOperand,
          double numericOperand, Pattern regexPattern, boolean rhsIsAttribute,
          int rhsAttIndex) {

          if (inst.isMissing(lhsAttIndex)) {
            return false;
          }

          if (regexPattern == null) {
            return false;
          }

          String lhsString = "";
          try {
            lhsString = inst.stringValue(lhsAttIndex);
          } catch (IllegalArgumentException ex) {
            return false;
          }

          return regexPattern.matcher(lhsString).matches();
        }
      };

      abstract boolean evaluate(Instance inst, int lhsAttIndex,
        String rhsOperand, double numericOperand, Pattern regexPattern,
        boolean rhsIsAttribute, int rhsAttIndex);

      private final String m_stringVal;

      ExpressionType(String name) {
        m_stringVal = name;
      }

      @Override
      public String toString() {
        return m_stringVal;
      }
    }

    public ExpressionClause() {
    }

    /**
     * Construct a new ExpressionClause
     * 
     * @param operator the operator to use
     * @param lhsAttributeName the lhs attribute name
     * @param rhsOperand the rhs operand
     * @param rhsIsAttribute true if the rhs operand is an attribute
     * @param isAnOr true if the result of this expression is to be OR'ed with
     *          the result so far
     */
    public ExpressionClause(ExpressionType operator, String lhsAttributeName,
      String rhsOperand, boolean rhsIsAttribute, boolean isAnOr) {
      m_operator = operator;
      m_lhsAttributeName = lhsAttributeName;
      m_rhsOperand = rhsOperand;
      m_rhsIsAttribute = rhsIsAttribute;
      m_isAnOr = isAnOr;
    }

    @Override
    public void init(Instances structure, Environment env) {
      super.init(structure, env);

      m_resolvedLhsName = m_lhsAttributeName;
      m_resolvedRhsOperand = m_rhsOperand;
      try {
        m_resolvedLhsName = m_env.substitute(m_resolvedLhsName);
        m_resolvedRhsOperand = m_env.substitute(m_resolvedRhsOperand);
      } catch (Exception ex) {
      }

      Attribute lhs = null;
      // try as an index or "special" label first
      if (m_resolvedLhsName.toLowerCase().startsWith("/first")) {
        lhs = structure.attribute(0);
      } else if (m_resolvedLhsName.toLowerCase().startsWith("/last")) {
        lhs = structure.attribute(structure.numAttributes() - 1);
      } else {
        // try as an index
        try {
          int indx = Integer.parseInt(m_resolvedLhsName);
          indx--;
          lhs = structure.attribute(indx);
        } catch (NumberFormatException ex) {
        }
      }

      if (lhs == null) {
        lhs = structure.attribute(m_resolvedLhsName);
      }
      if (lhs == null) {
        throw new IllegalArgumentException("Data does not contain attribute "
          + "\"" + m_resolvedLhsName + "\"");
      }
      m_lhsAttIndex = lhs.index();

      if (m_rhsIsAttribute) {
        Attribute rhs = null;

        // try as an index or "special" label first
        if (m_resolvedRhsOperand.toLowerCase().equals("/first")) {
          rhs = structure.attribute(0);
        } else if (m_resolvedRhsOperand.toLowerCase().equals("/last")) {
          rhs = structure.attribute(structure.numAttributes() - 1);
        } else {
          // try as an index
          try {
            int indx = Integer.parseInt(m_resolvedRhsOperand);
            indx--;
            rhs = structure.attribute(indx);
          } catch (NumberFormatException ex) {
          }
        }

        if (rhs == null) {
          rhs = structure.attribute(m_resolvedRhsOperand);
        }
        if (rhs == null) {
          throw new IllegalArgumentException("Data does not contain attribute "
            + "\"" + m_resolvedRhsOperand + "\"");
        }
        m_rhsAttIndex = rhs.index();
      } else if (m_operator != ExpressionType.CONTAINS
        && m_operator != ExpressionType.STARTSWITH
        && m_operator != ExpressionType.ENDSWITH
        && m_operator != ExpressionType.REGEX
        && m_operator != ExpressionType.ISMISSING) {
        // make sure the operand is parseable as a number (unless missing has
        // been specified - equals only)
        if (lhs.isNominal()) {
          m_numericOperand = lhs.indexOfValue(m_resolvedRhsOperand);

          if (m_numericOperand < 0) {
            throw new IllegalArgumentException("Unknown nominal value '"
              + m_resolvedRhsOperand + "' for attribute '" + lhs.name() + "'");
          }
        } else {
          try {
            m_numericOperand = Double.parseDouble(m_resolvedRhsOperand);
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + m_resolvedRhsOperand
              + "\" is not parseable as a number!");
          }
        }
      }

      if (m_operator == ExpressionType.REGEX) {
        m_regexPattern = Pattern.compile(m_resolvedRhsOperand);
      }
    }

    @Override
    public boolean evaluate(Instance inst, boolean result) {

      boolean thisNode = m_operator.evaluate(inst, m_lhsAttIndex, m_rhsOperand,
        m_numericOperand, m_regexPattern, m_rhsIsAttribute, m_rhsAttIndex);

      if (isNegated()) {
        thisNode = !thisNode;
      }

      return (isOr() ? (result || thisNode) : (result && thisNode));
    }

    @Override
    public String toString() {
      StringBuffer buff = new StringBuffer();
      toStringDisplay(buff);

      return buff.toString();
    }

    @Override
    public void toStringDisplay(StringBuffer buff) {
      toString(buff, false);
    }

    @Override
    protected void toStringInternal(StringBuffer buff) {
      toString(buff, true);
    }

    @Override
    public DefaultMutableTreeNode toJTree(DefaultMutableTreeNode parent) {
      parent.add(new DefaultMutableTreeNode(this));

      return parent;
    }

    private void toString(StringBuffer buff, boolean internal) {
      if (internal || m_showAndOr) {
        if (m_isAnOr) {
          buff.append("|| ");
        } else {
          buff.append("&& ");
        }
      }
      if (isNegated()) {
        buff.append("!");
      }

      buff.append("[");

      buff.append(m_lhsAttributeName);
      if (internal) {
        buff.append("@EC@" + m_operator.toString());
      } else {
        buff.append(" " + m_operator.toString());
      }

      if (m_operator != ExpressionType.ISMISSING) {
        // @@ indicates that the rhs is an attribute
        if (internal) {
          buff.append("@EC@" + (m_rhsIsAttribute ? "@@" : "") + m_rhsOperand);
        } else {
          buff.append(" " + (m_rhsIsAttribute ? "ATT: " : "") + m_rhsOperand);
        }
      } else {
        if (internal) {
          buff.append("@EC@");
        } else {
          buff.append(" ");
        }
      }

      buff.append("]");
    }

    @Override
    protected String parseFromInternal(String expression) {

      // first the boolean operator for this clause
      if (expression.startsWith("|| ")) {
        m_isAnOr = true;
      }

      if (expression.startsWith("|| ") || expression.startsWith("&& ")) {
        // strip the boolean operator
        expression = expression.substring(3, expression.length());
      }

      if (expression.charAt(0) == '!') {
        setNegated(true);
        expression = expression.substring(1, expression.length());
      }

      if (expression.charAt(0) != '[') {
        throw new IllegalArgumentException(
          "Was expecting a \"[\" to start this ExpressionClause!");
      }
      expression = expression.substring(1, expression.length());
      m_lhsAttributeName = expression.substring(0, expression.indexOf("@EC@"));
      expression = expression.substring(expression.indexOf("@EC@") + 4,
        expression.length());
      String oppName = expression.substring(0, expression.indexOf("@EC@"));
      expression = expression.substring(expression.indexOf("@EC@") + 4,
        expression.length());
      for (ExpressionType n : ExpressionType.values()) {
        if (n.toString().equals(oppName)) {
          m_operator = n;
          break;
        }
      }

      if (expression.startsWith("@@")) {
        // rhs is an attribute
        expression = expression.substring(2, expression.length()); // strip off
        // "@@"
        m_rhsIsAttribute = true;
      }
      m_rhsOperand = expression.substring(0, expression.indexOf(']'));

      expression = expression.substring(expression.indexOf(']') + 1,
        expression.length()); // remove "]"
      if (expression.charAt(0) == ' ') {
        expression = expression.substring(1, expression.length());
      }

      return expression;
    }
  }

  /** The root of the expression tree */
  protected ExpressionNode m_root;

  /** The expression tree to use in internal textual format */
  protected String m_expressionString = "";

  /**
   * The one or two downstream steps - one for instances that match the
   * expression and the other for instances that don't
   */
  protected Object[] m_downstream;

  /**
   * Name of the step to receive instances that evaluate to true via the
   * expression
   */
  protected String m_customNameOfTrueStep = "";

  /**
   * Name of the step to receive instances that evaluate to false via the
   * expression
   */
  protected String m_customNameOfFalseStep = "";

  protected int m_indexOfTrueStep;
  protected int m_indexOfFalseStep;

  /** Logging */
  protected transient Logger m_log;

  /** Busy indicator */
  protected transient boolean m_busy;

  /** Component talking to us */
  protected Object m_listenee;

  /** The type of the incoming connection */
  protected String m_connectionType;

  /** format of instances for current incoming connection (if any) */
  private Instances m_connectedFormat;

  protected transient Environment m_env;

  /** Instance event to use */
  protected InstanceEvent m_ie = new InstanceEvent(this);

  /**
   * Default visual filters
   */
  protected BeanVisual m_visual = new BeanVisual("FlowByExpression",
    BeanVisual.ICON_PATH + "FlowByExpression.png", BeanVisual.ICON_PATH
      + "FlowByExpression.png");

  /**
   * Constructor
   */
  public FlowByExpression() {
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);

    m_env = Environment.getSystemWide();
  }

  public String globalInfo() {
    return "Splits incoming instances (or instance stream) according to the "
      + "evaluation of a logical expression. The expression can test the values of "
      + "one or more incoming attributes. The test can involve constants or comparing "
      + "one attribute's values to another. Inequalities along with string operations "
      + "such as contains, starts-with, ends-with and regular expressions may be used "
      + "as operators. \"True\" instances can be sent to one downstream step and "
      + "\"False\" instances sent to another.";
  }

  /**
   * Set the expression (in internal format)
   * 
   * @param expressionString the expression to use (in internal format)
   */
  public void setExpressionString(String expressionString) {
    m_expressionString = expressionString;
  }

  /**
   * Get the current expression (in internal format)
   * 
   * @return the current expression (in internal format)
   */
  public String getExpressionString() {
    return m_expressionString;
  }

  /**
   * Set the name of the connected step to send "true" instances to
   * 
   * @param trueStep the name of the step to send "true" instances to
   */
  public void setTrueStepName(String trueStep) {
    m_customNameOfTrueStep = trueStep;
  }

  /**
   * Get the name of the connected step to send "true" instances to
   * 
   * @return the name of the step to send "true" instances to
   */
  public String getTrueStepName() {
    return m_customNameOfTrueStep;
  }

  /**
   * Set the name of the connected step to send "false" instances to
   * 
   * @param falseStep the name of the step to send "false" instances to
   */
  public void setFalseStepName(String falseStep) {
    m_customNameOfFalseStep = falseStep;
  }

  /**
   * Get the name of the connected step to send "false" instances to
   * 
   * @return the name of the step to send "false" instances to
   */
  public String getFalseStepName() {
    return m_customNameOfFalseStep;
  }

  @Override
  public void addDataSourceListener(DataSourceListener dsl) {
    if (m_downstream == null) {
      m_downstream = new Object[2];
    }

    if (m_downstream[0] == null && m_downstream[1] == null) {
      m_downstream[0] = dsl;
      return;
    }

    if (m_downstream[0] == null || m_downstream[1] == null) {
      if (m_downstream[0] == null
        && m_downstream[1] instanceof DataSourceListener) {
        m_downstream[0] = dsl;
        return;
      } else if (m_downstream[1] == null
        && m_downstream[0] instanceof DataSourceListener) {
        m_downstream[1] = dsl;
        return;
      }
    }
  }

  protected void remove(Object dsl) {
    if (m_downstream[0] == dsl) {
      m_downstream[0] = null;
      return;
    }

    if (m_downstream[1] == dsl) {
      m_downstream[1] = null;
    }
  }

  @Override
  public void removeDataSourceListener(DataSourceListener dsl) {
    if (m_downstream == null) {
      m_downstream = new Object[2];
    }

    remove(dsl);
  }

  @Override
  public void addInstanceListener(InstanceListener dsl) {
    if (m_downstream == null) {
      m_downstream = new Object[2];
    }

    if (m_downstream[0] == null && m_downstream[1] == null) {
      m_downstream[0] = dsl;
      return;
    }

    if (m_downstream[0] == null || m_downstream[1] == null) {
      if (m_downstream[0] == null
        && m_downstream[1] instanceof InstanceListener) {
        m_downstream[0] = dsl;
        return;
      } else if (m_downstream[1] == null
        && m_downstream[0] instanceof InstanceListener) {
        m_downstream[1] = dsl;
        return;
      }
    }
  }

  @Override
  public void removeInstanceListener(InstanceListener dsl) {
    if (m_downstream == null) {
      m_downstream = new Object[2];
    }

    remove(dsl);

  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  @Override
  public boolean eventGeneratable(String eventName) {
    if (m_listenee == null) {
      return false;
    }

    if (m_listenee instanceof EventConstraints) {

      if (eventName.equals("dataSet")) {
        return ((EventConstraints) m_listenee).eventGeneratable(eventName)
          || ((EventConstraints) m_listenee).eventGeneratable("trainingSet")
          || ((EventConstraints) m_listenee).eventGeneratable("testSet");
      }

      return ((EventConstraints) m_listenee).eventGeneratable(eventName);
    }

    return true;
  }

  /**
   * Initialize with respect to the incoming instance format
   * 
   * @param data the incoming instance format
   */
  protected void init(Instances data) {
    m_indexOfTrueStep = -1;
    m_indexOfFalseStep = -1;
    m_connectedFormat = data;

    if (m_downstream == null) {
      return;
    }

    if (m_downstream[0] != null
      && ((BeanCommon) m_downstream[0]).getCustomName().equals(
        m_customNameOfTrueStep)) {
      m_indexOfTrueStep = 0;
    }
    if (m_downstream[0] != null
      && ((BeanCommon) m_downstream[0]).getCustomName().equals(
        m_customNameOfFalseStep)) {
      m_indexOfFalseStep = 0;
    }

    if (m_downstream[1] != null
      && ((BeanCommon) m_downstream[1]).getCustomName().equals(
        m_customNameOfTrueStep)) {
      m_indexOfTrueStep = 1;
    }
    if (m_downstream[1] != null
      && ((BeanCommon) m_downstream[1]).getCustomName().equals(
        m_customNameOfFalseStep)) {
      m_indexOfFalseStep = 1;
    }

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    try {
      if (m_expressionString != null && m_expressionString.length() > 0) {
        m_root = new BracketNode();
        m_root.parseFromInternal(m_expressionString);
      }
      if (m_root != null) {
        m_root.init(data, m_env);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      stop();
      m_busy = false;
    }
  }

  @Override
  public void acceptDataSet(DataSetEvent e) {

    m_busy = true;
    if (m_log != null && !e.isStructureOnly()) {
      m_log.statusMessage(statusMessagePrefix() + "Processing batch...");
    }

    init(new Instances(e.getDataSet(), 0));

    if (m_root != null) {
      Instances trueBatch = new Instances(e.getDataSet(), 0);
      Instances falseBatch = new Instances(e.getDataSet(), 0);

      for (int i = 0; i < e.getDataSet().numInstances(); i++) {
        Instance current = e.getDataSet().instance(i);

        boolean result = m_root.evaluate(current, true);

        if (result) {
          if (m_indexOfTrueStep >= 0) {
            trueBatch.add(current);
          }
        } else {
          if (m_indexOfFalseStep >= 0) {
            falseBatch.add(current);
          }
        }
      }

      if (m_indexOfTrueStep >= 0) {
        DataSetEvent d = new DataSetEvent(this, trueBatch);
        ((DataSourceListener) m_downstream[m_indexOfTrueStep]).acceptDataSet(d);
      }

      if (m_indexOfFalseStep >= 0) {
        DataSetEvent d = new DataSetEvent(this, falseBatch);
        ((DataSourceListener) m_downstream[m_indexOfFalseStep])
          .acceptDataSet(d);
      }
    } else {
      if (m_indexOfTrueStep >= 0) {
        DataSetEvent d = new DataSetEvent(this, e.getDataSet());
        ((DataSourceListener) m_downstream[m_indexOfTrueStep]).acceptDataSet(d);
      }
    }

    if (m_log != null && !e.isStructureOnly()) {
      m_log.statusMessage(statusMessagePrefix() + "Finished");
    }

    m_busy = false;
  }

  @Override
  public void acceptTestSet(TestSetEvent e) {
    Instances test = e.getTestSet();
    DataSetEvent d = new DataSetEvent(this, test);
    acceptDataSet(d);
  }

  @Override
  public void acceptTrainingSet(TrainingSetEvent e) {
    Instances train = e.getTrainingSet();
    DataSetEvent d = new DataSetEvent(this, train);
    acceptDataSet(d);
  }

  @Override
  public void acceptInstance(InstanceEvent e) {
    m_busy = true;

    if (e.getStatus() == InstanceEvent.FORMAT_AVAILABLE) {
      Instances structure = e.getStructure();
      init(structure);

      if (m_log != null) {
        m_log.statusMessage(statusMessagePrefix() + "Processing stream...");
      }

      // notify listeners of structure
      m_ie.setStructure(structure);
      if (m_indexOfTrueStep >= 0) {
        ((InstanceListener) m_downstream[m_indexOfTrueStep])
          .acceptInstance(m_ie);
      }
      if (m_indexOfFalseStep >= 0) {
        ((InstanceListener) m_downstream[m_indexOfFalseStep])
          .acceptInstance(m_ie);
      }
    } else {
      Instance inst = e.getInstance();
      m_ie.setStatus(e.getStatus());

      if (inst == null || e.getStatus() == InstanceEvent.BATCH_FINISHED) {
        if (inst != null) {
          // evaluate and notify
          boolean result = true;
          if (m_root != null) {
            result = m_root.evaluate(inst, true);
          }

          if (result) {
            if (m_indexOfTrueStep >= 0) {
              m_ie.setInstance(inst);
              ((InstanceListener) m_downstream[m_indexOfTrueStep])
                .acceptInstance(m_ie);
            }
            if (m_indexOfFalseStep >= 0) {
              m_ie.setInstance(null);
              ((InstanceListener) m_downstream[m_indexOfFalseStep])
                .acceptInstance(m_ie);
            }
          } else {
            if (m_indexOfFalseStep >= 0) {
              m_ie.setInstance(inst);
              ((InstanceListener) m_downstream[m_indexOfFalseStep])
                .acceptInstance(m_ie);
            }
            if (m_indexOfTrueStep >= 0) {
              m_ie.setInstance(null);
              ((InstanceListener) m_downstream[m_indexOfTrueStep])
                .acceptInstance(m_ie);
            }
          }
        } else {
          // notify both of end of stream
          m_ie.setInstance(null);
          if (m_indexOfTrueStep >= 0) {
            ((InstanceListener) m_downstream[m_indexOfTrueStep])
              .acceptInstance(m_ie);
          }
          if (m_indexOfFalseStep >= 0) {
            ((InstanceListener) m_downstream[m_indexOfFalseStep])
              .acceptInstance(m_ie);
          }
        }

        if (m_log != null) {
          m_log.statusMessage(statusMessagePrefix() + "Finished");
        }
      } else {
        boolean result = true;
        if (m_root != null) {
          result = m_root.evaluate(inst, true);
        }
        m_ie.setInstance(inst);
        if (result) {
          if (m_indexOfTrueStep >= 0) {
            ((InstanceListener) m_downstream[m_indexOfTrueStep])
              .acceptInstance(m_ie);
          }
        } else {
          if (m_indexOfFalseStep >= 0) {
            ((InstanceListener) m_downstream[m_indexOfFalseStep])
              .acceptInstance(m_ie);
          }
        }
      }
    }

    m_busy = false;
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "FlowByExpression.png",
      BeanVisual.ICON_PATH + "FlowByExpression.png");
    m_visual.setText("FlowByExpression");
  }

  @Override
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  @Override
  public BeanVisual getVisual() {
    return m_visual;
  }

  @Override
  public void setCustomName(String name) {
    m_visual.setText(name);
  }

  @Override
  public String getCustomName() {
    return m_visual.getText();
  }

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

  @Override
  public boolean isBusy() {
    return m_busy;
  }

  @Override
  public void setLog(Logger logger) {
    m_log = logger;
  }

  @Override
  public boolean connectionAllowed(EventSetDescriptor esd) {
    return connectionAllowed(esd.getName());
  }

  @Override
  public boolean connectionAllowed(String eventName) {
    if (m_listenee != null) {
      return false;
    }

    return true;
  }

  @Override
  public void connectionNotification(String eventName, Object source) {
    if (connectionAllowed(eventName)) {
      m_listenee = source;
      m_connectionType = eventName;
    }
  }

  @Override
  public void disconnectionNotification(String eventName, Object source) {
    if (source == m_listenee) {
      m_listenee = null;
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

    if (eventName.equals("dataSet")
      && (m_downstream == null || m_downstream.length == 0)) {
      return null;
    }

    if (eventName.equals("instance")
      && (m_downstream == null || m_downstream.length == 0)) {
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
}
