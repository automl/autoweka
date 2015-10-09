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
 *    Associator.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.associations;

import weka.core.Capabilities;
import weka.core.Instances;

public interface Associator {

  /**
   * Generates an associator. Must initialize all fields of the associator
   * that are not being set via options (ie. multiple calls of buildAssociator
   * must always lead to the same result). Must not change the dataset
   * in any way.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the associator has not been 
   * generated successfully
   */
  void buildAssociations(Instances data) throws Exception;

  /** 
   * Returns the Capabilities of this associator. Derived associators have to
   * override this method to enable capabilities.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  Capabilities getCapabilities();
}