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
 *    VisualizePanelListener.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */


package weka.gui.visualize;

/**
 * Interface implemented by a class that is interested in receiving
 * submited shapes from a visualize panel.
 * @author Malcolm Ware (mfw4@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public interface VisualizePanelListener {

  /**
   * This method receives an object containing the shapes, instances
   * inside and outside these shapes and the attributes these shapes were
   * created in.
   * @param e The Event containing the data.
   */
  void userDataEvent(VisualizePanelEvent e);


}
