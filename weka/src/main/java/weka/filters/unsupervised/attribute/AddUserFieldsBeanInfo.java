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
 *    AddUserFieldsBeanInfo.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import java.beans.BeanDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * Bean info class for the AddUserFields filter.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 9002 $
 */
public class AddUserFieldsBeanInfo extends SimpleBeanInfo {

  /**
   * Get the bean descriptor for this bean
   * 
   * @return a <code>BeanDescriptor</code> value
   */
  @Override
  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(
        weka.filters.unsupervised.attribute.AddUserFields.class,
        weka.gui.filters.AddUserFieldsCustomizer.class);
  }
}
