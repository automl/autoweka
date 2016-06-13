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
 *    Obfuscate.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import java.util.ArrayList;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionUtils;
import weka.filters.Filter;
import weka.filters.StreamableFilter;
import weka.filters.UnsupervisedFilter;

/**
 * <!-- globalinfo-start --> A simple instance filter that renames the relation,
 * all attribute names and all nominal (and string) attribute values. For
 * exchanging sensitive datasets. Currently doesn't like string or relational
 * attributes.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * @author Len Trigg (len@reeltwo.com)
 * @version $Revision: 12037 $
 */
public class Obfuscate extends Filter implements UnsupervisedFilter,
  StreamableFilter {

  /** for serialization */
  static final long serialVersionUID = -343922772462971561L;

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "A simple instance filter that renames the relation, all attribute names "
      + "and all nominal (and string) attribute values. For exchanging sensitive "
      + "datasets. Currently doesn't like string or relational attributes.";
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
    result.enableAllAttributes();
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Sets the format of the input instances.
   * 
   * @param instanceInfo an Instances object containing the input instance
   *          structure (any instances contained in the object are ignored -
   *          only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);

    // Make the obfuscated header
    ArrayList<Attribute> v = new ArrayList<Attribute>();
    for (int i = 0; i < instanceInfo.numAttributes(); i++) {
      Attribute oldAtt = instanceInfo.attribute(i);
      Attribute newAtt = null;
      switch (oldAtt.type()) {
      case Attribute.NUMERIC:
        newAtt = new Attribute("A" + (i + 1));
        break;
      case Attribute.DATE:
        String format = oldAtt.getDateFormat();
        newAtt = new Attribute("A" + (i + 1), format);
        break;
      case Attribute.NOMINAL:
        ArrayList<String> vals = new ArrayList<String>();
        for (int j = 0; j < oldAtt.numValues(); j++) {
          vals.add("V" + (j + 1));
        }
        newAtt = new Attribute("A" + (i + 1), vals);
        break;
      case Attribute.STRING:
      case Attribute.RELATIONAL:
      default:
        newAtt = (Attribute) oldAtt.copy();
        System.err.println("Not converting attribute: " + oldAtt.name());
        break;
      }
      newAtt.setWeight(oldAtt.weight());
      v.add(newAtt);
    }
    Instances newHeader = new Instances("R", v, 10);
    newHeader.setClassIndex(instanceInfo.classIndex());
    setOutputFormat(newHeader);
    return true;
  }

  /**
   * Input an instance for filtering. Ordinarily the instance is processed and
   * made available for output immediately. Some filters require all instances
   * be read before producing output.
   * 
   * @param instance the input instance
   * @return true if the filtered instance may now be collected with output().
   * @throws IllegalStateException if no input format has been set.
   */
  @Override
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    push((Instance) instance.copy(), false); // No need to copy
    return true;
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
   * Main method for testing this class.
   * 
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String[] argv) {
    runFilter(new Obfuscate(), argv);
  }
}
