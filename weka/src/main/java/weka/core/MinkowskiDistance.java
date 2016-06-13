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
 *    MinkowskiDistance.java
 *    Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.neighboursearch.PerformanceStats;

/**
 * <!-- globalinfo-start --> Implementing Minkowski distance (or similarity)
 * function.<br/>
 * <br/>
 * One object defines not one distance but the data model in which the distances
 * between objects of that data model can be computed.<br/>
 * <br/>
 * Attention: For efficiency reasons the use of consistency checks (like are the
 * data models of the two instances exactly the same), is low.<br/>
 * <br/>
 * For more information, see:<br/>
 * <br/>
 * Wikipedia. Minkowski distance. URL
 * http://en.wikipedia.org/wiki/Minkowski_distance.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;misc{missing_id,
 *    author = {Wikipedia},
 *    title = {Minkowski distance},
 *    URL = {http://en.wikipedia.org/wiki/Minkowski_distance}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -P &lt;order&gt;
 *  The order 'p'. With '1' being the Manhattan distance and '2'
 *  the Euclidean distance.
 *  (default: 2)
 * </pre>
 * 
 * <pre>
 * -D
 *  Turns off the normalization of attribute 
 *  values in distance calculation.
 * </pre>
 * 
 * <pre>
 * -R &lt;col1,col2-col4,...&gt;
 *  Specifies list of columns to used in the calculation of the 
 *  distance. 'first' and 'last' are valid indices.
 *  (default: first-last)
 * </pre>
 * 
 * <pre>
 * -V
 *  Invert matching sense of column indices.
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10203 $
 */
public class MinkowskiDistance extends NormalizableDistance implements
  Cloneable, TechnicalInformationHandler {

  /** for serialization. */
  private static final long serialVersionUID = -7446019339455453893L;

  /** the order of the minkowski distance. */
  protected double m_Order = 2;

  /**
   * Constructs an Minkowski Distance object, Instances must be still set.
   */
  public MinkowskiDistance() {
    super();
  }

  /**
   * Constructs an Minkowski Distance object and automatically initializes the
   * ranges.
   * 
   * @param data the instances the distance function should work on
   */
  public MinkowskiDistance(Instances data) {
    super(data);
  }

  /**
   * Returns a string describing this object.
   * 
   * @return a description of the evaluator suitable for displaying in the
   *         explorer/experimenter gui
   */
  @Override
  public String globalInfo() {
    return "Implementing Minkowski distance (or similarity) function.\n\n"
      + "One object defines not one distance but the data model in which "
      + "the distances between objects of that data model can be computed.\n\n"
      + "Attention: For efficiency reasons the use of consistency checks "
      + "(like are the data models of the two instances exactly the same), "
      + "is low.\n\n" + "For more information, see:\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.MISC);
    result.setValue(Field.AUTHOR, "Wikipedia");
    result.setValue(Field.TITLE, "Minkowski distance");
    result.setValue(Field.URL,
      "http://en.wikipedia.org/wiki/Minkowski_distance");

    return result;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option(
      "\tThe order 'p'. With '1' being the Manhattan distance and '2'\n"
        + "\tthe Euclidean distance.\n" + "\t(default: 2)", "P", 1,
      "-P <order>"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    tmpStr = Utils.getOption('P', options);
    if (tmpStr.length() > 0) {
      setOrder(Double.parseDouble(tmpStr));
    } else {
      setOrder(2);
    }

    super.setOptions(options);
  }

  /**
   * Gets the current settings of this object.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    Vector<String> result;

    result = new Vector<String>();

    result.add("-P");
    result.add("" + getOrder());

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String orderTipText() {
    return "The order of the Minkowski distance ('1' is Manhattan distance and "
      + "'2' the Euclidean distance).";
  }

  /**
   * Sets the order.
   * 
   * @param value the new order
   */
  public void setOrder(double value) {
    if (m_Order != 0.0) {
      m_Order = value;
      invalidate();
    } else {
      System.err.println("Order cannot be zero!");
    }
  }

  /**
   * Gets the order.
   * 
   * @return the order
   */
  public double getOrder() {
    return m_Order;
  }

  /**
   * Calculates the distance between two instances.
   * 
   * @param first the first instance
   * @param second the second instance
   * @return the distance between the two given instances
   */
  @Override
  public double distance(Instance first, Instance second) {
    return Math.pow(distance(first, second, Double.POSITIVE_INFINITY),
      1 / m_Order);
  }

  /**
   * Calculates the distance (or similarity) between two instances. Need to pass
   * this returned distance later on to postprocess method to set it on correct
   * scale. <br/>
   * P.S.: Please don't mix the use of this function with distance(Instance
   * first, Instance second), as that already does post processing. Please
   * consider passing Double.POSITIVE_INFINITY as the cutOffValue to this
   * function and then later on do the post processing on all the distances.
   * 
   * @param first the first instance
   * @param second the second instance
   * @param stats the structure for storing performance statistics.
   * @return the distance between the two given instances or
   *         Double.POSITIVE_INFINITY.
   */
  @Override
  public double distance(Instance first, Instance second, PerformanceStats stats) { // debug
                                                                                    // method
                                                                                    // pls
                                                                                    // remove
                                                                                    // after
                                                                                    // use
    return Math.pow(distance(first, second, Double.POSITIVE_INFINITY, stats),
      1 / m_Order);
  }

  /**
   * Updates the current distance calculated so far with the new difference
   * between two attributes. The difference between the attributes was
   * calculated with the difference(int,double,double) method.
   * 
   * @param currDist the current distance calculated so far
   * @param diff the difference between two new attributes
   * @return the update distance
   * @see #difference(int, double, double)
   */
  @Override
  protected double updateDistance(double currDist, double diff) {
    double result;

    result = currDist;
    result += Math.pow(Math.abs(diff), m_Order);

    return result;
  }

  /**
   * Does post processing of the distances (if necessary) returned by
   * distance(distance(Instance first, Instance second, double cutOffValue). It
   * is necessary to do so to get the correct distances if
   * distance(distance(Instance first, Instance second, double cutOffValue) is
   * used. This is because that function actually returns the squared distance
   * to avoid inaccuracies arising from floating point comparison.
   * 
   * @param distances the distances to post-process
   */
  @Override
  public void postProcessDistances(double distances[]) {
    for (int i = 0; i < distances.length; i++) {
      distances[i] = Math.pow(distances[i], 1 / m_Order);
    }
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 0$");
  }
}
