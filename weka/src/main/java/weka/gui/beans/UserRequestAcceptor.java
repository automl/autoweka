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
 *    UserRequestAcceptor.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.util.Enumeration;

/**
 * Interface to something that can accept requests from a user to perform some
 * action
 * 
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 10216 $
 * @since 1.0
 */
public interface UserRequestAcceptor {

  /**
   * Get a list of performable requests
   * 
   * @return an <code>Enumeration</code> value
   */
  Enumeration<String> enumerateRequests();

  /**
   * Perform the named request
   * 
   * @param requestName a <code>String</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  void performRequest(String requestName);
}
