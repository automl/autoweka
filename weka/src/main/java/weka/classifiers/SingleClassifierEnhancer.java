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
 *    SingleClassifierEnhancer.java
 *    Copyright (C) 2004-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.rules.ZeroR;
import weka.core.*;
import weka.core.Capabilities.Capability;

/**
 * Abstract utility class for handling settings common to meta
 * classifiers that use a single base learner.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 12204 $
 */
public abstract class SingleClassifierEnhancer extends AbstractClassifier {

  /** for serialization */
  private static final long serialVersionUID = -3665885256363525164L;

  /** The base classifier to use */
  protected Classifier m_Classifier = new ZeroR();

  /**
   * String describing default classifier.
   */
  protected String defaultClassifierString() {

    return "weka.classifiers.rules.ZeroR";
  }

  /**
   * String describing options for default classifier.
   */
  protected String[] defaultClassifierOptions() {

    return new String[0];
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(3);

    newVector.addElement(new Option(
          "\tFull name of base classifier.\n"
          + "\t(default: " + defaultClassifierString() + 
          ((defaultClassifierOptions().length > 0) ? 
           " with options " + Utils.joinOptions(defaultClassifierOptions()) + ")" : ")"),
          "W", 1, "-W"));
    
    newVector.addAll(Collections.list(super.listOptions()));

    newVector.addElement(new Option(
          "",
          "", 0, "\nOptions specific to classifier "
          + m_Classifier.getClass().getName() + ":"));
    newVector.addAll(Collections.list(((OptionHandler)m_Classifier).listOptions()));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -W classname <br>
   * Specify the full class name of the base learner.<p>
   *
   * Options after -- are passed to the designated classifier.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    super.setOptions(options);

    String classifierName = Utils.getOption('W', options);

    if (classifierName.length() > 0) {
      setClassifier(AbstractClassifier.forName(classifierName, null));
      setClassifier(AbstractClassifier.forName(classifierName,
            Utils.partitionOptions(options)));
    } else {
      setClassifier(AbstractClassifier.forName(defaultClassifierString(), null));
      String[] classifierOptions = Utils.partitionOptions(options);
      if (classifierOptions.length > 0) {
        setClassifier(AbstractClassifier.forName(defaultClassifierString(),
                                                 classifierOptions));
      } else {
        setClassifier(AbstractClassifier.forName(defaultClassifierString(),
                                                 defaultClassifierOptions()));
      }
    }
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    Vector<String> options = new Vector<String>();
       
    options.add("-W");
    options.add(getClassifier().getClass().getName());
    
    Collections.addAll(options, super.getOptions());
    
    String[] classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    if (classifierOptions.length > 0) {
      options.add("--");
      Collections.addAll(options, classifierOptions);
    }

    return options.toArray(new String[0]);
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String classifierTipText() {
    return "The base classifier to be used.";
  }

  /**
   * Returns default capabilities of the base classifier.
   *
   * @return      the capabilities of the base classifier
   */
  public Capabilities getCapabilities() {
    Capabilities        result;

    if (getClassifier() != null) {
      result = getClassifier().getCapabilities();
    } else {
      result = new Capabilities(this);
      result.disableAll();
    }

    // set dependencies
    for (Capability cap: Capability.values())
      result.enableDependency(cap);

    result.setOwner(this);

    return result;
  }

  /**
   * Set the base learner.
   *
   * @param newClassifier the classifier to use.
   */
  public void setClassifier(Classifier newClassifier) {

    m_Classifier = newClassifier;
  }

  /**
   * Get the classifier used as the base learner.
   *
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier() {

    return m_Classifier;
  }

  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @return the classifier string
   */
  protected String getClassifierSpec() {

    Classifier c = getClassifier();
    return c.getClass().getName() + " "
      + Utils.joinOptions(((OptionHandler)c).getOptions());
  }

  @Override
  public void preExecution() throws Exception {
    if (getClassifier() instanceof CommandlineRunnable) {
      ((CommandlineRunnable) getClassifier()).preExecution();
    }
  }

  @Override
  public void postExecution() throws Exception {
    if (getClassifier() instanceof CommandlineRunnable) {
      ((CommandlineRunnable) getClassifier()).postExecution();
    }
  }
}
