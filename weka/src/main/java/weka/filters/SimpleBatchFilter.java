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
 * SimpleBatchFilter.java
 * Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters;

import weka.core.Instance;
import weka.core.Instances;

/**
 * This filter is a superclass for simple batch filters.
 * <p/>
 * 
 * <b>General notes:</b><br/>
 * <ul>
 * <li>After adding instances to the filter via input(Instance) one always has
 * to call batchFinished() to make them available via output().</li>
 * <li>After the first call of batchFinished() the field m_FirstBatchDone is set
 * to <code>true</code>.</li>
 * </ul>
 * <p/>
 * 
 * <b>Example:</b><br/>
 * The following code snippet uses the filter <code>SomeFilter</code> on a
 * dataset that is loaded from <code>filename</code>.
 * 
 * <pre>
 * import weka.core.*;
 * import weka.filters.*;
 * import java.io.*;
 * ...
 * SomeFilter filter = new SomeFilter();
 * // set necessary options for the filter
 * Instances data = new Instances(
 *                    new BufferedReader(
 *                      new FileReader(filename)));
 * Instances filteredData = Filter.useFilter(data, filter);
 * </pre>
 * 
 * <b>Implementation:</b><br/>
 * Only the following abstract methods need to be implemented:
 * <ul>
 * <li>globalInfo()</li>
 * <li>determineOutputFormat(Instances)</li>
 * <li>process(Instances)</li>
 * </ul>
 * <br/>
 * And the <b>getCapabilities()</b> method must return what kind of attributes
 * and classes the filter can handle.
 * <p/>
 * 
 * If more options are necessary, then the following methods need to be
 * overriden:
 * <ul>
 * <li>listOptions()</li>
 * <li>setOptions(String[])</li>
 * <li>getOptions()</li>
 * </ul>
 * <p/>
 * 
 * To make the filter available from commandline one must add the following main
 * method for correct execution (&lt;Filtername&gt; must be replaced with the
 * actual filter classname):
 * 
 * <pre>
 *  public static void main(String[] args) {
 *    runFilter(new &lt;Filtername&gt;(), args);
 *  }
 * </pre>
 * <p/>
 * 
 * <b>Example implementation:</b><br/>
 * 
 * <pre>
 * import weka.core.*;
 * import weka.core.Capabilities.*;
 * import weka.filters.*;
 * 
 * public class SimpleBatch extends SimpleBatchFilter {
 * 
 *   public String globalInfo() {
 *     return &quot;A simple batch filter that adds an additional attribute 'bla' at the end containing the index of the processed instance.&quot;;
 *   }
 * 
 *   public Capabilities getCapabilities() {
 *     Capabilities result = super.getCapabilities();
 *     result.enableAllAttributes();
 *     result.enableAllClasses();
 *     result.enable(Capability.NO_CLASS); // filter doesn't need class to be set
 *     return result;
 *   }
 * 
 *   protected Instances determineOutputFormat(Instances inputFormat) {
 *     Instances result = new Instances(inputFormat, 0);
 *     result.insertAttributeAt(new Attribute(&quot;bla&quot;), result.numAttributes());
 *     return result;
 *   }
 * 
 *   protected Instances process(Instances inst) {
 *     Instances result = new Instances(determineOutputFormat(inst), 0);
 *     for (int i = 0; i &lt; inst.numInstances(); i++) {
 *       double[] values = new double[result.numAttributes()];
 *       for (int n = 0; n &lt; inst.numAttributes(); n++)
 *         values[n] = inst.instance(i).value(n);
 *       values[values.length - 1] = i;
 *       result.add(new DenseInstance(1, values));
 *     }
 *     return result;
 *   }
 * 
 *   public static void main(String[] args) {
 *     runFilter(new SimpleBatch(), args);
 *   }
 * }
 * 
 * </pre>
 * <p/>
 * 
 * <b>Options:</b><br/>
 * Valid filter-specific options are:
 * <p/>
 * 
 * -D <br/>
 * Turns on output of debugging information.
 * <p/>
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 12037 $
 * @see SimpleStreamFilter
 * @see #input(Instance)
 * @see #batchFinished()
 * @see #m_FirstBatchDone
 */
public abstract class SimpleBatchFilter extends SimpleFilter {

  /** for serialization */
  private static final long serialVersionUID = 8102908673378055114L;

  /**
   * returns true if the output format is immediately available after the input
   * format has been set and not only after all the data has been seen (see
   * batchFinished())
   * 
   * @return true if the output format is immediately available
   * @see #batchFinished()
   * @see #setInputFormat(Instances)
   */
  @Override
  protected boolean hasImmediateOutputFormat() {
    return false;
  }

  /**
   * Returns whether to allow the determineOutputFormat(Instances) method access
   * to the full dataset rather than just the header.
   * <p/>
   * Default implementation returns false.
   * 
   * @return whether determineOutputFormat has access to the full input dataset
   */
  public boolean allowAccessToFullInputFormat() {
    return false;
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

    bufferInput(instance); // bufferInput() makes a copy of the instance.

    if (isFirstBatchDone()) {
      Instances inst = new Instances(getInputFormat());
      inst = process(inst);
      for (int i = 0; i < inst.numInstances(); i++) {
        push(inst.instance(i), false); // No need to copy instance
      }
      flushInput();
    }

    return m_FirstBatchDone;
  }

  /**
   * Signify that this batch of input to the filter is finished. If the filter
   * requires all instances prior to filtering, output() may now be called to
   * retrieve the filtered instances. Any subsequent instances filtered should
   * be filtered based on setting obtained from the first batch (unless the
   * setInputFormat has been re-assigned or new options have been set). Sets
   * m_FirstBatchDone and m_NewBatch to true.
   * 
   * @return true if there are instances pending output
   * @throws IllegalStateException if no input format has been set.
   * @throws Exception if something goes wrong
   * @see #m_NewBatch
   * @see #m_FirstBatchDone
   */
  @Override
  public boolean batchFinished() throws Exception {
    int i;
    Instances inst;

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    // get data
    inst = new Instances(getInputFormat());

    // if output format hasn't been set yet, do it now
    if (!hasImmediateOutputFormat() && !isFirstBatchDone()) {
      if (allowAccessToFullInputFormat()) {
        setOutputFormat(determineOutputFormat(inst));
      } else {
        setOutputFormat(determineOutputFormat(new Instances(inst, 0)));
      }
    }

    // don't do anything in case there are no instances pending.
    // in case of second batch, they may have already been processed
    // directly by the input method and added to the output queue
    if (inst.numInstances() > 0) {
      // process data
      inst = process(inst);

      // clear input queue
      flushInput();

      // move it to the output
      for (i = 0; i < inst.numInstances(); i++) {
        push(inst.instance(i), false); // No need to copy instance
      }
    }

    m_NewBatch = true;
    m_FirstBatchDone = true;

    return (numPendingOutput() != 0);
  }
}
