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
 * CapabilitiesHandler.java
 * Copyright (C) 2006-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

/**
 * Classes implementing this interface return their capabilities in regards
 * to datasets.
 * 
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 * @see     Capabilities
 */
public interface CapabilitiesHandler {

  /**
   * Returns the capabilities of this object.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities();
}
