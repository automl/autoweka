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
 * SubsetByExpression.java
 * Copyright (C) 2008-2014 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.instance;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.expressionlanguage.common.IfElseMacro;
import weka.core.expressionlanguage.common.JavaMacro;
import weka.core.expressionlanguage.common.MacroDeclarationsCompositor;
import weka.core.expressionlanguage.common.MathFunctions;
import weka.core.expressionlanguage.common.Primitives.BooleanExpression;
import weka.core.expressionlanguage.core.Node;
import weka.core.expressionlanguage.parser.Parser;
import weka.core.expressionlanguage.weka.InstancesHelper;
import weka.filters.SimpleBatchFilter;

/**
 * <!-- globalinfo-start -->
 * Filters instances according to a user-specified expression.<br/>
 * <br/>
 * Examples:<br/>
 * - extracting only mammals and birds from the 'zoo' UCI dataset:<br/>
 *   (CLASS is 'mammal') or (CLASS is 'bird')<br/>
 * - extracting only animals with at least 2 legs from the 'zoo' UCI dataset:<br/>
 *   (ATT14 &gt;= 2)<br/>
 * - extracting only instances with non-missing 'wage-increase-second-year'<br/>
 *   from the 'labor' UCI dataset:<br/>
 *   not ismissing(ATT3)<br/>
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -E &lt;expr&gt;
 *  The expression to use for filtering
 *  (default: true).</pre>
 * 
 * <pre> -F
 *  Apply the filter to instances that arrive after the first
 *  (training) batch. The default is to not apply the filter (i.e.
 *  always return the instance)</pre>
 * 
 * <pre> -output-debug-info
 *  If set, filter is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, filter capabilities are not checked when input format is set
 *  (use with caution).</pre>
 * 
 * <!-- options-end -->
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 12037 $
 */
public class SubsetByExpression extends SimpleBatchFilter {

  /** for serialization. */
  private static final long serialVersionUID = 5628686110979589602L;

  /** the expresion to use for filtering. */
  protected String m_Expression = "true";

  /** Whether to filter instances after the first batch has been processed */
  protected boolean m_filterAfterFirstBatch = false;

  /**
   * Returns a string describing this filter.
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  @Override
  public String globalInfo() {
    return "Filters instances according to a user-specified expression.\n\n"
      + "\n"
      + "Examples:\n"
      + "- extracting only mammals and birds from the 'zoo' UCI dataset:\n"
      + "  (CLASS is 'mammal') or (CLASS is 'bird')\n"
      + "- extracting only animals with at least 2 legs from the 'zoo' UCI dataset:\n"
      + "  (ATT14 >= 2)\n"
      + "- extracting only instances with non-missing 'wage-increase-second-year'\n"
      + "  from the 'labor' UCI dataset:\n" + "  not ismissing(ATT3)\n";
  }

  /**
   * SubsetByExpression may return false from input() (thus not making an
   * instance available immediately) even after the first batch has been
   * completed if the user has opted to apply the filter to instances after the
   * first batch (rather than just passing them through).
   * 
   * @return true this filter may remove (consume) input instances after the
   *         first batch has been completed.
   */
  @Override
  public boolean mayRemoveInstanceAfterFirstBatchDone() {
    return true;
  }

  /**
   * Input an instance for filtering. Filter requires all training instances be
   * read before producing output (calling the method batchFinished() makes the
   * data available). If this instance is part of a new batch, m_NewBatch is set
   * to false.
   * 
   * @param instance the input instance
   * @return true if the filtered instance may now be collected with output().
   * @throws IllegalStateException if no input structure has been defined
   * @throws Exception if something goes wrong
   * @see #batchFinished()
   */
  @Override
  public boolean input(Instance instance) throws Exception {
    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    bufferInput(instance);

    int numReturnedFromParser = 0;
    if (isFirstBatchDone()) {
      Instances inst = new Instances(getInputFormat());
      inst = process(inst);
      numReturnedFromParser = inst.numInstances();
      for (int i = 0; i < inst.numInstances(); i++) {
        push(inst.instance(i), false); // No need to copy instance
      }
      flushInput();
    }

    return (numReturnedFromParser > 0);
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tThe expression to use for filtering\n"
      + "\t(default: true).", "E", 1, "-E <expr>"));

    result.addElement(new Option(
      "\tApply the filter to instances that arrive after the first\n"
        + "\t(training) batch. The default is to not apply the filter (i.e.\n"
        + "\talways return the instance)", "F", 0, "-F"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -E &lt;expr&gt;
   *  The expression to use for filtering
   *  (default: true).</pre>
   * 
   * <pre> -F
   *  Apply the filter to instances that arrive after the first
   *  (training) batch. The default is to not apply the filter (i.e.
   *  always return the instance)</pre>
   * 
   * <pre> -output-debug-info
   *  If set, filter is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -do-not-check-capabilities
   *  If set, filter capabilities are not checked when input format is set
   *  (use with caution).</pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String tmpStr = Utils.getOption('E', options);
    if (tmpStr.length() != 0) {
      setExpression(tmpStr);
    } else {
      setExpression("true");
    }

    m_filterAfterFirstBatch = Utils.getFlag('F', options);

    if (getInputFormat() != null) {
      setInputFormat(getInputFormat());
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the filter.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    result.add("-E");
    result.add("" + getExpression());

    if (m_filterAfterFirstBatch) {
      result.add("-F");
    }

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the Capabilities of this filter.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.STRING_ATTRIBUTES);
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.STRING_CLASS);
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Sets the expression used for filtering.
   * 
   * @param value the expression
   */
  public void setExpression(String value) {
    m_Expression = value;
  }

  /**
   * Returns the expression used for filtering.
   * 
   * @return the expression
   */
  public String getExpression() {
    return m_Expression;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String expressionTipText() {
    return "The expression to used for filtering the dataset.";
  }

  /**
   * Set whether to apply the filter to instances that arrive once the first
   * (training) batch has been seen. The default is to not apply the filter and
   * just return each instance input. This is so that, when used in the
   * FilteredClassifier, a test instance does not get "consumed" by the filter
   * and a prediction is always generated.
   * 
   * @param b true if the filter should be applied to instances that arrive
   *          after the first (training) batch has been processed.
   */
  public void setFilterAfterFirstBatch(boolean b) {
    m_filterAfterFirstBatch = b;
  }

  /**
   * Get whether to apply the filter to instances that arrive once the first
   * (training) batch has been seen. The default is to not apply the filter and
   * just return each instance input. This is so that, when used in the
   * FilteredClassifier, a test instance does not get "consumed" by the filter
   * and a prediction is always generated.
   * 
   * @return true if the filter should be applied to instances that arrive after
   *         the first (training) batch has been processed.
   */
  public boolean getFilterAfterFirstBatch() {
    return m_filterAfterFirstBatch;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String filterAfterFirstBatchTipText() {
    return "Whether to apply the filtering process to instances that "
      + "are input after the first (training) batch. The default "
      + "is false so that, when used in a FilteredClassifier, test"
      + " instances do not potentially get 'consumed' by the filter "
      + "an a prediction is always made.";
  }

  /**
   * Determines the output format based on the input format and returns this.
   * 
   * @param inputFormat the input format to base the output format on
   * @return the output format
   * @throws Exception in case the determination goes wrong
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat)
    throws Exception {

    return new Instances(inputFormat, 0);
  }

  /**
   * Processes the given data (may change the provided dataset) and returns the
   * modified version. This method is called in batchFinished().
   * 
   * @param instances the data to process
   * @return the modified data
   * @throws Exception in case the processing goes wrong
   * @see #batchFinished()
   */
  @Override
  protected Instances process(Instances instances) throws Exception {
    if (!isFirstBatchDone() || m_filterAfterFirstBatch) {

      // setup output
      Instances output = new Instances(instances, 0);
      
      // compile expression
      InstancesHelper instancesHelper = new InstancesHelper(instances);
      Node node = Parser.parse(
          // expression
          m_Expression,
          // variables
          instancesHelper,
          // macros
          new MacroDeclarationsCompositor(
              instancesHelper,
              new MathFunctions(),
              new IfElseMacro(),
              new JavaMacro()
              )
          );

      if (!(node instanceof BooleanExpression))
        throw new Exception("Expression must be of boolean type!");
      
      BooleanExpression condition = (BooleanExpression) node;

      // filter dataset
      for (int i = 0; i < instances.numInstances(); i++) {
        Instance instance = instances.get(i);
        
        instancesHelper.setInstance(instance);

        // evaluate expression
        if (condition.evaluate())
          output.add((Instance) instance.copy());
      }

      return output;
    } else {
      return instances;
    }
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12037 $");
  }

  /**
   * Main method for running this filter.
   * 
   * @param args arguments for the filter: use -h for help
   */
  public static void main(String[] args) {
    runFilter(new SubsetByExpression(), args);
  }
}
