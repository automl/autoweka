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
 *    GraphConstants.java
 *    Copyright (C) 2003-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.graphvisualizer;


/**
 * GraphConstants.java
 *
 *
 * @author Ashraf M. Kibriya (amk14@cs.waikato.ac.nz)
 * @version $Revision: 8034 $ - 24 Apr 2003 - Initial version (Ashraf M. Kibriya)
 */
public interface GraphConstants  {
  /** Types of Edges */
  int DIRECTED=1,  REVERSED=2, DOUBLE=3;
  
  //Node types
  /** SINGULAR_DUMMY node - node with only one outgoing edge
   * i.e. one which represents a single edge and is inserted to close a gap */
  int SINGULAR_DUMMY=1;
  /** PLURAL_DUMMY node - node with more than one outgoing edge
   * i.e. which represents an edge split and is inserted to close a gap */
  int PLURAL_DUMMY=2;
  /** NORMAL node - node actually contained in graphs description  */
  int NORMAL=3;
  
} // GraphConstants
