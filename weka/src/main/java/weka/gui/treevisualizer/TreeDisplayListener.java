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
 *    TreeDisplayListener.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.treevisualizer;

//this is simply used to get some user changes from the displayer to an actual
//class
//that contains the actual structure of the data the displayer is displaying

/**
 * Interface implemented by classes that wish to recieve user selection events
 * from a tree displayer.
 *
 * @author Malcolm Ware (mfw4@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public interface TreeDisplayListener {

  /**
   * Gets called when the user selects something, in the tree display.
   * @param e Contains what the user selected with what it was selected for.
   */
  void userCommand(TreeDisplayEvent e);
}




