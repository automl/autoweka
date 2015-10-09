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
 * TextSaverBeanInfo.java
 * 
 * Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 */
package weka.gui.beans;

import java.beans.BeanDescriptor;
import java.beans.EventSetDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * Bean info class for the serialized model saver bean
 * 
 * @author (thuvh87{[at]}gmail{[dot]}com)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 9238 $
 */
public class TextSaverBeanInfo extends SimpleBeanInfo {

  /**
   * Get the event set descriptors for this bean
   * 
   * @return an <code>EventSetDescriptor[]</code> value
   */
  @Override
  public EventSetDescriptor[] getEventSetDescriptors() {
    // hide all gui events
    EventSetDescriptor[] esds = {};
    return esds;
  }

  /**
   * Get BeanDescriptor for this bean
   * 
   * @return an <code>BeanDescriptor</code> value
   */
  @Override
  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(weka.gui.beans.TextSaver.class,
        TextSaverCustomizer.class);
  }
}
