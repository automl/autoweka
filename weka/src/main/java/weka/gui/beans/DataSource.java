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
 *    DataSource.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;


/**
 * Interface to something that is capable of being a source for data - 
 * either batch or incremental data
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 8034 $
 * @since 1.0
 */
public interface DataSource {

  /**
   * Add a data source listener
   *
   * @param dsl a <code>DataSourceListener</code> value
   */
  void addDataSourceListener(DataSourceListener dsl);

  /**
   * Remove a data source listener
   *
   * @param dsl a <code>DataSourceListener</code> value
   */
  void removeDataSourceListener(DataSourceListener dsl);

  /**
   * Add an instance listener
   *
   * @param dsl an <code>InstanceListener</code> value
   */
  void addInstanceListener(InstanceListener dsl);

  /**
   * Remove an instance listener
   *
   * @param dsl an <code>InstanceListener</code> value
   */
  void removeInstanceListener(InstanceListener dsl);
}
