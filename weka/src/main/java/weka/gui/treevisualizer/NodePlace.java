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
 *    NodePlace.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.treevisualizer;

/**
 * This is an interface for classes that wish to take a node structure and 
 * arrange them
 *
 * @author Malcolm F Ware (mfw4@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public interface NodePlace {
 
  /**
   * The function to call to postion the tree that starts at Node r
   *
   * @param r The top of the tree.
   */
   void place(Node r);
  
} 
