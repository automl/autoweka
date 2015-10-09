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
 * InstanceComparator.java
 * Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;

/**
 * A comparator for the Instance class. it can be used with or without the
 * class label. Missing values are sorted at the beginning.</br>
 * Can be used as comparator in the sorting and binary search algorithms of
 * <code>Arrays</code> and <code>Collections</code>.
 * Relational values are compared instance by instance with a nested 
 * InstanceComparator.
 *
 * @see     Instance
 * @author  FracPete (fracpete at cs dot waikato dot ac dot nz)
 * @version $Revision: 8034 $
 * @see     java.util.Arrays
 * @see     java.util.Collections
 */
public class InstanceComparator
  implements Comparator<Instance>, Serializable, RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = -6589278678230949683L;
  
  /** whether to include the class in the comparison */
  protected boolean m_IncludeClass;
  
  /** the range of attributes to use for comparison. */
  protected Range m_Range;
  
  /**
   * Initializes the comparator and includes the class in the comparison
   * and all attributes included.
   */
  public InstanceComparator() {
    this(true);
  }
  
  /**
   * Initializes the comparator with all attributes included.
   * 
   * @param includeClass	whether to include the class in the comparison
   */
  public InstanceComparator(boolean includeClass) {
    this(includeClass, "first-last", false);
  }
  
  /**
   * Initializes the comparator.
   * 
   * @param includeClass	whether to include the class in the comparison
   * @param range		the attribute range string
   * @param inverted		whether to invert the matching sense of the att range
   */
  public InstanceComparator(boolean includeClass, String range, boolean invert) {
    super();
    
    m_Range = new Range();
    
    setIncludeClass(includeClass);
    setRange(range);
    setInvert(invert);
  }
  
  /**
   * Sets whether the class should be included in the comparison.
   * 
   * @param includeClass	true if to include the class in the comparison 
   */
  public void setIncludeClass(boolean includeClass) {
    m_IncludeClass = includeClass;
  }
  
  /**
   * Returns whether the class is included in the comparison.
   * 
   * @return			true if the class is included
   */
  public boolean getIncludeClass() {
    return m_IncludeClass;
  }
  
  /**
   * Sets the attribute range to use for comparison.
   * 
   * @param value		the attribute range
   */
  public void setRange(String value) {
    m_Range.setRanges(value);
  }
  
  /**
   * Returns the attribute range to use in the comparison.
   * 
   * @return			the attribute range
   */
  public String getRange() {
    return m_Range.getRanges();
  }
  
  /**
   * Sets whether to invert the matching sense of the attribute range.
   * 
   * @param invert		true if to invert the matching sense
   */
  public void setInvert(boolean value) {
    m_Range.setInvert(value);
  }
  
  /**
   * Returns whether the matching sense of the attribute range is inverted.
   * 
   * @return			true if the matching sense is inverted
   */
  public boolean getInvert() {
    return m_Range.getInvert();
  }

  /**
   * compares the two instances, returns -1 if o1 is smaller than o2, 0
   * if equal and +1 if greater. The method assumes that both instance objects
   * have the same attributes, they don't have to belong to the same dataset.
   * 
   * @param inst1	the first instance to compare
   * @param inst2	the second instance to compare
   * @return		returns -1 if inst1 is smaller than inst2, 0 if equal and +1 
   * 			if greater
   */
  public int compare(Instance inst1, Instance inst2) {
    int         	result;
    int         	classindex;
    int         	i;
    Instances		data1;
    Instances		data2;
    int			n;
    InstanceComparator	comp;
    
    m_Range.setUpper(inst1.numAttributes() - 1);
    
    // get class index
    if (inst1.classIndex() == -1)
      classindex = inst1.numAttributes() - 1;
    else
      classindex = inst1.classIndex();

    result = 0;
    for (i = 0; i < inst1.numAttributes(); i++) {
      // in selected range?
      if (!m_Range.isInRange(i))
	continue;
      
      // exclude class?
      if (!getIncludeClass() && (i == classindex))
        continue;
   
      // comparing attribute values
      // 1. special handling if missing value (NaN) is involved:
      if (inst1.isMissing(i) || inst2.isMissing(i)) {
        if (inst1.isMissing(i) && inst2.isMissing(i)) {
          continue;
        }
        else {
          if (inst1.isMissing(i))
            result = -1;
          else
            result = 1;
          break;
        }
      }
      // 2. regular values:
      else {
	switch (inst1.attribute(i).type()) {
	  case Attribute.STRING:
	    result = inst1.stringValue(i).compareTo(inst2.stringValue(i));
	    break;
	  case Attribute.RELATIONAL:
	    data1 = inst1.relationalValue(i);
	    data2 = inst2.relationalValue(i);
	    n     = 0;
	    comp  = new InstanceComparator();
	    while ((n < data1.numInstances()) && (n < data2.numInstances()) && (result == 0)) {
	      result = comp.compare(data1.instance(n), data2.instance(n));
	      n++;
	    }
	    break;
	  default:
	    if (Utils.eq(inst1.value(i), inst2.value(i))) { 
	      continue;
	    }
	    else {
	      if (inst1.value(i) < inst2.value(i))
		result = -1;
	      else
		result = 1;
	      break;
	    }
	}
      }
      if (result != 0)
	break;
    }
    
    return result;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 8034 $");
  }
  
  /**
   * for testing only. takes an ARFF-filename as first argument to perform
   * some tests. 
   */
  public static void main(String[] args) throws Exception {
    Instances       inst;
    Comparator<Instance>      comp;
    
    if (args.length == 0)
      return;
    
    // read instances
    inst = new Instances(new BufferedReader(new FileReader(args[0])));
    inst.setClassIndex(inst.numAttributes() - 1);
    
    // compare incl. class
    comp = new InstanceComparator();
    System.out.println("\nIncluding the class");
    System.out.println("comparing 1. instance with 1.: " + comp.compare(inst.instance(0), inst.instance(0)));
    System.out.println("comparing 1. instance with 2.: " + comp.compare(inst.instance(0), inst.instance(1)));
    System.out.println("comparing 2. instance with 1.: " + comp.compare(inst.instance(1), inst.instance(0)));
        
    // compare excl. class
    comp = new InstanceComparator(false);
    System.out.println("\nExcluding the class");
    System.out.println("comparing 1. instance with 1.: " + comp.compare(inst.instance(0), inst.instance(0)));
    System.out.println("comparing 1. instance with 2.: " + comp.compare(inst.instance(0), inst.instance(1)));
    System.out.println("comparing 2. instance with 1.: " + comp.compare(inst.instance(1), inst.instance(0)));
    
    // sort the data on all attributes
    Instances tmp = new Instances(inst);
    Collections.sort(tmp, new InstanceComparator(false));
    System.out.println("\nSorted on all attributes");
    System.out.println(tmp);
    
    // sort the data on 2nd attribute
    tmp = new Instances(inst);
    Collections.sort(tmp, new InstanceComparator(false, "2", false));
    System.out.println("\nSorted on 2nd attribute");
    System.out.println(tmp);
  }
}
