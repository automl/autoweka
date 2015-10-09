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
 *    Matchable.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

/** 
 * Interface to something that can be matched with tree matching
 * algorithms.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public interface Matchable {

  /**
   * Returns a string that describes a tree representing
   * the object in prefix order.
   *
   * @return the tree described as a string
   * @exception Exception if the tree can't be computed
   */
  String prefix() throws Exception;
}








