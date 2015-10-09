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
 *    ModelPerformanceChartBeanInfo.java
 *    Copyright (C) 2004-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.beans.BeanDescriptor;
import java.beans.EventSetDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * Bean info class for the model performance chart
 *
 * @author Mark Hall
 * @version $Revision: 8034 $
 */
public class ModelPerformanceChartBeanInfo extends SimpleBeanInfo {
  
  /**
   * Get the event set descriptors for this bean
   *
   * @return an <code>EventSetDescriptor[]</code> value
   */
  public EventSetDescriptor [] getEventSetDescriptors() {
    // hide all gui events
    try {
      EventSetDescriptor [] esds = { 
          new EventSetDescriptor(ModelPerformanceChart.class, 
              "image", 
              ImageListener.class, 
          "acceptImage")
      };
      return esds;
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }
  
  /**
   * Get the bean descriptor for this bean
   *
   * @return a <code>BeanDescriptor</code> value
   */
  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(weka.gui.beans.ModelPerformanceChart.class,
                              ModelPerformanceChartCustomizer.class);
  }
}

