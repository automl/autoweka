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
 *    AbstractDataSourceBeanInfo.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.beans.EventSetDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * Bean info class for AbstractDataSource. All beans that extend
 * AbstractDataSource might want to extend this class
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 8034 $
 * @since 1.0
 * @see SimpleBeanInfo
 */
public class AbstractDataSourceBeanInfo extends SimpleBeanInfo {

  /**
   * Get the event set descriptors pertinent to data sources
   *
   * @return an <code>EventSetDescriptor[]</code> value
   */
  public EventSetDescriptor [] getEventSetDescriptors() {
    try {
      EventSetDescriptor [] esds =  
      { new EventSetDescriptor(DataSource.class, 
			       "dataSet",
			       DataSourceListener.class,
			       "acceptDataSet"),
	new EventSetDescriptor(DataSource.class, 
			       "instance",
			       InstanceListener.class,
			       "acceptInstance")
         };
      return esds;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
}
