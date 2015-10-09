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
 *    AssociatorBeanInfo.java
 *    Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.beans.BeanDescriptor;
import java.beans.EventSetDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo class for the Associator wrapper bean
 *
 * @author Mark Hall (mhall at cs dot waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class AssociatorBeanInfo extends SimpleBeanInfo {
 
  public EventSetDescriptor [] getEventSetDescriptors() {
    try {
      EventSetDescriptor [] esds = { 
	new EventSetDescriptor(Associator.class,
			       "text",
			       TextListener.class,
			       "acceptText"),
        new EventSetDescriptor(Associator.class,
			       "graph",
			       GraphListener.class,
			       "acceptGraph"),
	new EventSetDescriptor(Associator.class,
	                       "configuration",
	                       ConfigurationListener.class,
	                       "acceptConfiguration"),
	new EventSetDescriptor(Associator.class,
	                       "batchAssociationRules",
	                        BatchAssociationRulesListener.class,
	                        "acceptAssociationRules")	                       
      };
      return esds;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

  /**
   * Get the bean descriptor for this bean
   *
   * @return a <code>BeanDescriptor</code> value
   */
  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(weka.gui.beans.Associator.class, 
			      AssociatorCustomizer.class);
  }
}

