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
 * FromFile.java
 * Copyright (C) 2004-2012 University of Waikato, Hamilton, New Zealand
 * 
 */
package weka.classifiers.bayes.net.search.fixed;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;
import weka.classifiers.bayes.net.ParentSet;
import weka.classifiers.bayes.net.search.SearchAlgorithm;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Utils;

/**
 * <!-- globalinfo-start --> The FromFile reads the structure of a Bayes net
 * from a file in BIFF format.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -B &lt;BIF File&gt;
 *  Name of file containing network structure in BIF format
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Remco Bouckaert
 * @version $Revision: 10154 $
 */
public class FromFile extends SearchAlgorithm {

  /** for serialization */
  static final long serialVersionUID = 7334358169507619525L;

  /** name of file to read structure from **/
  String m_sBIFFile = "";

  /**
   * Returns a string describing this object
   * 
   * @return a description of the classifier suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "The FromFile reads the structure of a Bayes net from a file "
      + "in BIFF format.";
  }

  /**
   * 
   * @param bayesNet
   * @param instances the instances to work with
   * @throws Exception if attribute from BIF file could not be found
   */
  @Override
  public void buildStructure(BayesNet bayesNet, Instances instances)
    throws Exception {
    // read network structure in BIF format
    BIFReader bifReader = new BIFReader();
    bifReader.processFile(m_sBIFFile);
    // copy parent sets
    for (int iAttribute = 0; iAttribute < instances.numAttributes(); iAttribute++) {
      int iBIFAttribute = bifReader.getNode(bayesNet.getNodeName(iAttribute));
      ParentSet bifParentSet = bifReader.getParentSet(iBIFAttribute);
      for (int iBIFParent = 0; iBIFParent < bifParentSet.getNrOfParents(); iBIFParent++) {
        String sParent = bifReader.getNodeName(bifParentSet
          .getParent(iBIFParent));
        int iParent = 0;
        while (iParent < instances.numAttributes()
          && !bayesNet.getNodeName(iParent).equals(sParent)) {
          iParent++;
        }
        if (iParent >= instances.numAttributes()) {
          throw new Exception("Could not find attribute " + sParent
            + " from BIF file in data");
        }
        bayesNet.getParentSet(iAttribute).addParent(iParent, instances);
      }
    }
  } // buildStructure

  /**
   * Set name of network in BIF file to read structure from
   * 
   * @param sBIFFile the name of the BIF file
   */
  public void setBIFFile(String sBIFFile) {
    m_sBIFFile = sBIFFile;
  }

  /**
   * Get name of network in BIF file to read structure from
   * 
   * @return BIF file name
   */
  public String getBIFFile() {
    return m_sBIFFile;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> newVector = new Vector<Option>();

    newVector.addElement(new Option(
      "\tName of file containing network structure in BIF format\n", "B", 1,
      "-B <BIF File>"));

    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -B &lt;BIF File&gt;
   *  Name of file containing network structure in BIF format
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    setBIFFile(Utils.getOption('B', options));

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the search algorithm.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    options.add("-B");
    options.add("" + getBIFFile());

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10154 $");
  }

} // class FromFile
