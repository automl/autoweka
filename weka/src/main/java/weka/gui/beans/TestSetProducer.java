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
 *    TestSetProducer.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;


/**
 * Interface to something that can produce test sets
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 8034 $
 */
public interface TestSetProducer {

  /**
   * Add a listener for test set events
   *
   * @param tsl a <code>TestSetListener</code> value
   */
  void addTestSetListener(TestSetListener tsl);

  /**
   * Remove a listener for test set events
   *
   * @param tsl a <code>TestSetListener</code> value
   */
  void removeTestSetListener(TestSetListener tsl);

}
