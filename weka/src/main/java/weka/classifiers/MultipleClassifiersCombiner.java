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
 *    MultipleClassifiersCombiner.java
 *    Copyright (C) 2004-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.*;
import weka.core.Capabilities.Capability;

/**
 * Abstract utility class for handling settings common to
 * meta classifiers that build an ensemble from multiple classifiers.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 12204 $
 */
public abstract class MultipleClassifiersCombiner extends AbstractClassifier {

  /** for serialization */
  private static final long serialVersionUID = 2776436621129422119L;

  /** Array for storing the generated base classifiers. */
  protected Classifier[] m_Classifiers = {
    new weka.classifiers.rules.ZeroR()
  };

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(1);

    newVector.addElement(new Option(
          "\tFull class name of classifier to include, followed\n"
          + "\tby scheme options. May be specified multiple times.\n"
          + "\t(default: \"weka.classifiers.rules.ZeroR\")",
          "B", 1, "-B <classifier specification>"));

    newVector.addAll(Collections.list(super.listOptions()));
    
    for (Classifier classifier : getClassifiers()) {
      if (classifier instanceof OptionHandler) {
        newVector.addElement(new Option(
          "",
          "", 0, "\nOptions specific to classifier "
            + classifier.getClass().getName() + ":"));
        newVector.addAll(Collections.list(((OptionHandler)classifier).listOptions()));
      }
    }
    
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -B classifierstring <br>
   * Classifierstring should contain the full class name of a scheme
   * included for selection followed by options to the classifier
   * (required, option should be used once for each classifier).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    // Iterate through the schemes
    Vector<Classifier> classifiers = new Vector<Classifier>();
    while (true) {
      String classifierString = Utils.getOption('B', options);
      if (classifierString.length() == 0) {
        break;
      }
      String [] classifierSpec = Utils.splitOptions(classifierString);
      if (classifierSpec.length == 0) {
        throw new IllegalArgumentException("Invalid classifier specification string");
      }
      String classifierName = classifierSpec[0];
      classifierSpec[0] = "";
      classifiers.addElement(AbstractClassifier.forName(classifierName,
            classifierSpec));
    }
    if (classifiers.size() == 0) {
      classifiers.addElement(new weka.classifiers.rules.ZeroR());
    }
    Classifier [] classifiersArray = new Classifier [classifiers.size()];
    for (int i = 0; i < classifiersArray.length; i++) {
      classifiersArray[i] = (Classifier) classifiers.elementAt(i);
    }
    setClassifiers(classifiersArray);

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    Vector<String> options = new Vector<String>();
    for (int i = 0; i < m_Classifiers.length; i++) {
      options.add("-B");
      options.add("" + getClassifierSpec(i));
    }
    Collections.addAll(options, super.getOptions());
  
    return options.toArray(new String[0]);
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String classifiersTipText() {
    return "The base classifiers to be used.";
  }

  /**
   * Sets the list of possible classifers to choose from.
   *
   * @param classifiers an array of classifiers with all options set.
   */
  public void setClassifiers(Classifier [] classifiers) {

    m_Classifiers = classifiers;
  }

  /**
   * Gets the list of possible classifers to choose from.
   *
   * @return the array of Classifiers
   */
  public Classifier [] getClassifiers() {

    return m_Classifiers;
  }

  /**
   * Gets a single classifier from the set of available classifiers.
   *
   * @param index the index of the classifier wanted
   * @return the Classifier
   */
  public Classifier getClassifier(int index) {

    return m_Classifiers[index];
  }

  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @param index the index of the classifier string to retrieve, starting from
   * 0.
   * @return the classifier string, or the empty string if no classifier
   * has been assigned (or the index given is out of range).
   */
  protected String getClassifierSpec(int index) {

    if (m_Classifiers.length < index) {
      return "";
    }
    Classifier c = getClassifier(index);
    return c.getClass().getName() + " "
      + Utils.joinOptions(((OptionHandler)c).getOptions());
  }

  /**
   * Returns combined capabilities of the base classifiers, i.e., the
   * capabilities all of them have in common.
   *
   * @return      the capabilities of the base classifiers
   */
  public Capabilities getCapabilities() {
    Capabilities      result;
    int               i;

    if (getClassifiers().length == 0) {
      result = new Capabilities(this);
      result.disableAll();
    }
    else {
      result = (Capabilities) getClassifier(0).getCapabilities().clone();
      for (i = 1; i < getClassifiers().length; i++)
        result.and(getClassifier(i).getCapabilities());
    }

    // set dependencies
    for (Capability cap: Capability.values())
      result.enableDependency(cap);

    result.setOwner(this);

    return result;
  }

  @Override
  public void preExecution() throws Exception {
    for (Classifier classifier : getClassifiers()) {
      if (classifier instanceof CommandlineRunnable) {
        ((CommandlineRunnable) classifier).preExecution();
      }
    }
  }

  @Override
  public void postExecution() throws Exception {
    for (Classifier classifier : getClassifiers()) {
      if (classifier instanceof CommandlineRunnable) {
        ((CommandlineRunnable) classifier).postExecution();
      }
    }
  }
}
