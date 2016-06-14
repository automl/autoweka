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
 * SingleClustererEnhancer.java
 * Copyright (C) 2006-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.clusterers;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * Meta-clusterer for enhancing a base clusterer.
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10203 $
 */
public abstract class SingleClustererEnhancer extends AbstractClusterer
  implements OptionHandler {

  /** for serialization */
  private static final long serialVersionUID = 4893928362926428671L;

  /** the clusterer */
  protected Clusterer m_Clusterer = new SimpleKMeans();

  /**
   * String describing default clusterer.
   * 
   * @return the default clusterer classname
   */
  protected String defaultClustererString() {
    return SimpleKMeans.class.getName();
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tFull name of base clusterer.\n"
      + "\t(default: " + defaultClustererString() + ")", "W", 1, "-W"));

    result.addAll(Collections.list(super.listOptions()));

    if (m_Clusterer instanceof OptionHandler) {
      result.addElement(new Option("", "", 0,
        "\nOptions specific to clusterer " + m_Clusterer.getClass().getName()
          + ":"));

      result.addAll(Collections.list(((OptionHandler) m_Clusterer)
        .listOptions()));
    }

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

    tmpStr = Utils.getOption('W', options);
    super.setOptions(options);
    if (tmpStr.length() > 0) {
      setClusterer(AbstractClusterer.forName(tmpStr, null));
      setClusterer(AbstractClusterer.forName(tmpStr,
        Utils.partitionOptions(options)));
    } else {
      setClusterer(AbstractClusterer.forName(defaultClustererString(), null));
      setClusterer(AbstractClusterer.forName(defaultClustererString(),
        Utils.partitionOptions(options)));
    }
  }

  /**
   * Gets the current settings of the clusterer.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    result.add("-W");
    result.add(getClusterer().getClass().getName());

    Collections.addAll(result, super.getOptions());

    if (getClusterer() instanceof OptionHandler) {
      String[] options = ((OptionHandler) getClusterer()).getOptions();

      if (options.length > 0) {
        result.add("--");
      }
      Collections.addAll(result, options);
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String clustererTipText() {
    return "The base clusterer to be used.";
  }

  /**
   * Set the base clusterer.
   * 
   * @param value the classifier to use.
   */
  public void setClusterer(Clusterer value) {
    m_Clusterer = value;
  }

  /**
   * Get the clusterer used as the base clusterer.
   * 
   * @return the base clusterer
   */
  public Clusterer getClusterer() {
    return m_Clusterer;
  }

  /**
   * Gets the clusterer specification string, which contains the class name of
   * the clusterer and any options to the clusterer
   * 
   * @return the clusterer string
   */
  protected String getClustererSpec() {
    String result;
    Clusterer clusterer;

    clusterer = getClusterer();
    result = clusterer.getClass().getName();

    if (clusterer instanceof OptionHandler) {
      result += " "
        + Utils.joinOptions(((OptionHandler) clusterer).getOptions());
    }

    return result;
  }

  /**
   * Returns default capabilities of the clusterer.
   * 
   * @return the capabilities of this clusterer
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result;

    if (getClusterer() == null) {
      result = super.getCapabilities();
    } else {
      result = getClusterer().getCapabilities();
    }

    // set dependencies
    for (Capability cap : Capability.values()) {
      result.enableDependency(cap);
    }

    return result;
  }

  /**
   * Returns the number of clusters.
   * 
   * @return the number of clusters generated for a training dataset.
   * @throws Exception if number of clusters could not be returned successfully
   */
  @Override
  public int numberOfClusters() throws Exception {
    return m_Clusterer.numberOfClusters();
  }
}
