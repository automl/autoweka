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
 * RegressionGenerator.java
 * Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.datagenerators;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Option;
import weka.core.Utils;

/**
 * Abstract class for data generators for regression classifiers.
 * <p/>
 * 
 * Example usage as the main of a datagenerator called RandomGenerator:
 * 
 * <pre>
 * public static void main(String[] args) {
 *   try {
 *     DataGenerator.makeData(new RandomGenerator(), args);
 *   } catch (Exception e) {
 *     e.printStackTrace();
 *     System.err.println(e.getMessage());
 *   }
 * }
 * </pre>
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10203 $
 */
public abstract class RegressionGenerator extends DataGenerator {

  /** for serialization */
  private static final long serialVersionUID = 3073254041275658221L;

  /** Number of instances */
  protected int m_NumExamples;

  /**
   * initializes the generator with default values
   */
  public RegressionGenerator() {
    super();

    setNumExamples(defaultNumExamples());
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = enumToVector(super.listOptions());

    result.addElement(new Option(
      "\tThe number of examples to generate (default " + defaultNumExamples()
        + ")", "n", 1, "-n <num>"));

    return result.elements();
  }

  /**
   * Sets the options.
   * 
   * @param options the options
   * @throws Exception if invalid option
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    super.setOptions(options);

    tmpStr = Utils.getOption('n', options);
    if (tmpStr.length() != 0) {
      setNumExamples(Integer.parseInt(tmpStr));
    } else {
      setNumExamples(defaultNumExamples());
    }
  }

  /**
   * Gets the current settings of the classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    Collections.addAll(result, super.getOptions());

    result.add("-n");
    result.add("" + getNumExamples());

    return result.toArray(new String[result.size()]);
  }

  /**
   * returns the default number of examples
   * 
   * @return the default number of examples
   */
  protected int defaultNumExamples() {
    return 100;
  }

  /**
   * Sets the number of examples, given by option.
   * 
   * @param numExamples the new number of examples
   */
  public void setNumExamples(int numExamples) {
    m_NumExamples = numExamples;
  }

  /**
   * Gets the number of examples, given by option.
   * 
   * @return the number of examples, given by option
   */
  public int getNumExamples() {
    return m_NumExamples;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numExamplesTipText() {
    return "The number of examples to generate.";
  }
}
