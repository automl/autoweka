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
 *    NonSparseToSparse.java
 *    Copyright (C) 2000-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.instance;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.StreamableFilter;
import weka.filters.UnsupervisedFilter;

/**
 * <!-- globalinfo-start --> An instance filter that converts all incoming
 * instances into sparse format.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 12477 $
 */
public class NonSparseToSparse extends Filter implements UnsupervisedFilter,
  StreamableFilter, OptionHandler {

  /** for serialization */
  static final long serialVersionUID = 4694489111366063852L;

  protected boolean m_encodeMissingAsZero = false;

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "An instance filter that converts all incoming instances"
      + " into sparse format.";
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
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();
    result.add(new Option("\tTreat missing values as zero.", "M", 0, "-M"));

    return result.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {

    m_encodeMissingAsZero = Utils.getFlag('M', options);

    Utils.checkForRemainingOptions(options);
  }

  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    if (m_encodeMissingAsZero) {
      result.add("-M");
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Set whether missing values should be treated in the same way as zeros
   * 
   * @param m true if missing values are to be treated the same as zeros
   */
  public void setTreatMissingValuesAsZero(boolean m) {
    m_encodeMissingAsZero = m;
  }

  /**
   * Get whether missing values are to be treated in the same way as zeros
   * 
   * @return true if missing values are to be treated in the same way as zeros
   */
  public boolean getTreatMissingValuesAsZero() {
    return m_encodeMissingAsZero;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String treatMissingValuesAsZeroTipText() {
    return "Treat missing values in the same way as zeros.";
  }

  /**
   * Sets the format of the input instances.
   * 
   * @param instanceInfo an Instances object containing the input instance
   *          structure (any instances contained in the object are ignored -
   *          only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if format cannot be processed
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);
    Instances instNew = instanceInfo;

    setOutputFormat(instNew);
    return true;
  }

  /**
   * Input an instance for filtering. Ordinarily the instance is processed and
   * made available for output immediately. Some filters require all instances
   * be read before producing output.
   * 
   * @param instance the input instance.
   * @return true if the filtered instance may now be collected with output().
   * @throws IllegalStateException if no input format has been set.
   */
  @Override
  public boolean input(Instance instance) {

    Instance newInstance = null;

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    if (m_encodeMissingAsZero) {
      Instance tempInst = (Instance) instance.copy();
      tempInst.setDataset(getInputFormat());

      for (int i = 0; i < tempInst.numAttributes(); i++) {
        if (tempInst.isMissing(i)) {
          tempInst.setValue(i, 0);
        }
      }
      instance = tempInst;
    }

    newInstance = new SparseInstance(instance);
    newInstance.setDataset(instance.dataset());
    push(newInstance, false); // No need to copy


    /*
     * Instance inst = new SparseInstance(instance);
     * inst.setDataset(instance.dataset()); push(inst);
     */
    return true;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12477 $");
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String[] argv) {
    runFilter(new NonSparseToSparse(), argv);
  }
}
