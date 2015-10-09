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
 *    LayoutCompleteEvent.java
 *    Copyright (C) 2003-2012 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.gui.graphvisualizer;

import java.util.EventObject;

/**
 * This is an event which is fired by a LayoutEngine once
 * a LayoutEngine finishes laying out the graph, so
 * that the Visualizer can repaint the screen to show
 * the changes.
 *
 * @author Ashraf M. Kibriya (amk14@cs.waikato.ac.nz)
 * @version $Revision: 8034 $ - 24 Apr 2003 - Initial version (Ashraf M. Kibriya)
 */
public class LayoutCompleteEvent
  extends EventObject {

  /** for serialization */
  private static final long serialVersionUID = 6172467234026258427L;
  
  public LayoutCompleteEvent(Object source) {
    super(source);
  }
  
}
