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
 *    Stats.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.experiment;

import java.io.Serializable;

import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;


/**
 * A class to store simple statistics.<p>
 * 
 * Upon initialization the variables take the following values:<p>
 * <code>
 * {@link #count} = {@link #sum} = {@link #sumSq} = 0 <br />
 * {@link #mean} = {@link #stdDev} = {@link #min} = {@link #max} = Double.NaN
 * </code><p>
 * This is called the initial state. <p>
 * 
 * For signaling that a Stats object has been provided with values that hint
 * that something is either wrong with the data used or the algorithm used there
 * is also the invalid state where the variables take the following values: <p>
 * <code>
 * {@link #count} = {@link #sum} = {@link #sumSq} = {@link #mean} =
 * {@link #stdDev} = {@link #min} = {@link #max} = Double.NaN
 * <code><p>
 * Once a Stats object goes into the invalid state it can't change its state
 * anymore. <p>
 * 
 * A Stats object assumes that only values are subtracted (by using the 
 * {@link #subtract(double)} or {@link #subtract(double, double)} methods)
 * that have previously been added (by using the {@link #add(double)} or 
 * {@link #add(double, double)} methods) and the weights must be the same
 * too.<br />
 * Otherwise the Stats object's fields' values are implementation defined.<p>
 * 
 * If the implementation detects a problem then the Stats object goes into the
 * invalid state.<p>
 * 
 * The fields {@link #count}, {@link #sum}, {@link #sumSq}, {@link #min} and
 * {@link #max} are always updated whereas the field {@link #mean} and
 * {@link #stdDev} are only guaranteed to be updated after a call to
 * {@link #calculateDerived()}.<p>
 * 
 * For the fields {@link #min} and {@link #max} the following rules apply:<p>
 * <code>
 * min(values_added \ values_subtracted) >= {@link #min} >= min(values_added)<br>
 * max(values_added \ values_subtracted) <= {@link #max} <= max(values_added)
 * </code><p>
 * Where \ is the set difference.<p>
 * 
 * For the field {@link #stdDev} the following rules apply:<p>
 * <ol>
 * <li>If count <= 1 then
 *     {@link #stdDev}=Double.NaN.</li>
 * <li>Otherwise {@link #stdDev} >= 0 and it should take on the value by best
 *     effort of the implementation.</li>
 * </ol>
 * 
 * For the methods {@link #add(double)}, {@link #add(double, double)},
 * {@link #subtract(double)} and {@link #subtract(double, double)} the following
 * rules apply:<p>
 * 
 * <ol>
 * <li>if weight < 0 then {@link #subtract(double, double)} is used instead of
 *     {@link #add(double, double)} with weight = -weight and vice versa.</li>
 * <li>if weight = +-inf or weight = NaN then the Stats object goes into the
 *     invalid state.</li>
 * <li>if value = +-inf or value = NaN then the Stats object goes into the
 *     invalid state.</li>
 * <li>if weight = 0 then the value gets ignored.</li>
 * <li>Otherwise the fields get updated by the implementation's best effort.</li>
 * </ol>
 * 
 * For {@link #count} the following rules apply<p>
 * 
 * <ol>
 * <li>If {@link #count} goes below zero then all fields are set to
 * <code>Double.NaN</code> except the {@link #count} field which gets tracked
 * normally.</li>
 * <li>If {@link #count} = 0 then the Stats object goes into the initial state.
 * </li>
 * <li>If {@link #count} > 0 for the first time, then the Stats object goes into
 *     initial state and gets updated with the corresponding value and weight.
 * </li>
 * </ol>
 * 
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 11424 $
 */
public class Stats
implements Serializable, RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = -8610544539090024102L;

  /** The number of values seen */
  public double count = 0;

  /** The sum of values seen */
  public double sum = 0;

  /** The sum of values squared seen */
  public double sumSq = 0;

  /** The std deviation of values at the last calculateDerived() call */    
  public double stdDev = Double.NaN;

  /** The mean of values at the last calculateDerived() call */    
  public double mean = Double.NaN;

  /** The minimum value seen, or Double.NaN if no values seen */
  public double min = Double.NaN;

  /** The maximum value seen, or Double.NaN if no values seen */
  public double max = Double.NaN;

  /** an important factor to calculate the standard deviation incrementally */
  private double stdDevFactor = 0;

  private void reset() {
    count = 0;
    sum = 0;
    sumSq = 0;
    stdDev = Double.NaN;
    mean = Double.NaN;
    min = Double.NaN;
    max = Double.NaN;
    stdDevFactor = 0;
  }

  private void negativeCount() {
    sum = Double.NaN;
    sumSq = Double.NaN;
    stdDev = Double.NaN;
    mean = Double.NaN;
    min = Double.NaN;
    max = Double.NaN;
  }

  private void goInvalid() {
    count = Double.NaN;
    negativeCount();
  }

  private boolean isInvalid() {
    return Double.isNaN(count);
  }

  /**
   * Adds a value to the observed values<p>
   * 
   * It's equivalent to <code>add(value, 1)</code><p>
   *
   * @param value the observed value
   */
  public void add(double value) {

    add(value, 1);
  }

  /**
   * Adds a weighted value to the observed values
   *
   * @param value the observed value
   * @param weight the weight of the observed value
   */
  public void add(double value, double weight) {

    // treat as subtract
    if (weight < 0) {
      subtract(value, -weight);
      return;
    }

    // don't leave invalid state
    if (isInvalid())
      return;

    // go invalid
    if (Double.isInfinite(weight) || Double.isNaN(weight) ||
      Double.isInfinite(value) || Double.isNaN(value)) {
      goInvalid();
      return;
    }

    // ignore
    if (weight == 0)
      return;

    double newCount = count + weight;
    if (count < 0 && (newCount > 0 || Utils.eq(newCount, 0))) {
      reset();
      return;
    }

    count = newCount;

    if (count < 0) {
      return;
    }

    double weightedValue = value*weight;
    sum += weightedValue;
    sumSq += value * weightedValue;
    if (Double.isNaN(mean)) {
      // For the first value the mean can suffer from loss of precision
      // so we treat it separately and make sure the calculation stays accurate
      mean = value;
      stdDevFactor = 0;
    } else {
      double delta = weight*(value - mean);
      mean += delta/count;
      stdDevFactor += delta*(value - mean);
    }

    if (Double.isNaN(min)) {
      min = max = value;
    } else if (value < min) {
      min = value;
    } else if (value > max) {
      max = value;
    }
  }

  /**
   * Removes a value to the observed values (no checking is done
   * that the value being removed was actually added).<p>
   * 
   * It's equivalent to <code>subtract(value, 1)</code><p>
   *
   * @param value the observed value
   */
  public void subtract(double value) {
    subtract(value, 1);
  }

  /**
   * Subtracts a weighted value from the observed values
   *
   * @param value the observed value
   * @param weight the weight of the observed value
   */
  public void subtract(double value, double weight) {

    // treat as add
    if (weight < 0) {
      add(value, -weight);
      return;
    }

    // don't leave invalid state
    if (isInvalid())
      return;

    // go invalid
    if (Double.isInfinite(weight) || Double.isNaN(weight) ||
      Double.isInfinite(value) || Double.isNaN(value)) {
      goInvalid();
      return;
    }

    // ignore
    if (weight == 0)
      return;

    count -= weight;

    if (Utils.eq(count, 0)) {
      reset();
      return;
    } else if (count < 0) {
      negativeCount();
      return;
    }

    double weightedValue = value*weight;
    sum -= weightedValue;
    sumSq -= value * weightedValue;
    double delta = weight*(value - mean);
    mean -= delta/count;
    stdDevFactor -= delta*(value - mean);
  }

  /**
   * Tells the object to calculate any statistics that don't have their
   * values automatically updated during add. Currently updates the mean
   * and standard deviation.
   */
  public void calculateDerived() {

    if (count <= 1) {
      stdDev = Double.NaN;
      return;
    }
    stdDev = stdDevFactor/(count - 1);
    if (stdDev < 0) {
      stdDev = 0;
      return;
    }
    stdDev = Math.sqrt(stdDev);

  }

  /**
   * Returns a string summarising the stats so far.
   *
   * @return the summary string
   */
  public String toString() {

    return
      "Count   " + Utils.doubleToString(count, 8) + '\n'
      + "Min     " + Utils.doubleToString(min, 8) + '\n'
      + "Max     " + Utils.doubleToString(max, 8) + '\n'
      + "Sum     " + Utils.doubleToString(sum, 8) + '\n'
      + "SumSq   " + Utils.doubleToString(sumSq, 8) + '\n'
      + "Mean    " + Utils.doubleToString(mean, 8) + '\n'
      + "StdDev  " + Utils.doubleToString(stdDev, 8) + '\n';
  }

  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11424 $");
  }

  /**
   * Tests the paired stats object from the command line.
   * reads line from stdin, expecting two values per line.
   *
   * @param args ignored.
   */
  public static void main(String [] args) {

    try {
      Stats ps = new Stats();
      java.io.LineNumberReader r = new java.io.LineNumberReader(
        new java.io.InputStreamReader(System.in));
      String line;
      while ((line = r.readLine()) != null) {
        line = line.trim();
        if (line.equals("") || line.startsWith("@") || line.startsWith("%")) {
          continue;
        }
        java.util.StringTokenizer s 
        = new java.util.StringTokenizer(line, " ,\t\n\r\f");
        int count = 0;
        double v1 = 0;
        while (s.hasMoreTokens()) {
          double val = (new Double(s.nextToken())).doubleValue();
          if (count == 0) {
            v1 = val;
          } else {
            System.err.println("MSG: Too many values in line \"" 
              + line + "\", skipped.");
            break;
          }
          count++;
        }
        if (count == 1) {
          ps.add(v1);
        }
      }
      ps.calculateDerived();
      System.err.println(ps);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }

} // Stats

