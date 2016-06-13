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
 *    LayoutEngine.java
 *    Copyright (C) 2003-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.graphvisualizer;

import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * This interface class has been added to facilitate the addition of other
 * layout engines to this package. Any class that wants to lay out a graph
 * should implement this interface.
 * 
 * @author Ashraf M. Kibriya (amk14@cs.waikato.ac.nz)
 * @version $Revision: 10153 $ - 24 Apr 2003 - Initial version (Ashraf M.
 *          Kibriya)
 */
public interface LayoutEngine {
  /**
   * This method lays out the graph for better visualization
   */
  void layoutGraph();

  /**
   * This method sets the nodes and edges vectors of the LayoutEngine
   */
  void setNodesEdges(ArrayList<GraphNode> nodes, ArrayList<GraphEdge> edges);

  /**
   * This method sets the allowed size of the node
   */
  void setNodeSize(int nodeWidth, int nodeHeight);

  /** give access to set of graph nodes */
  ArrayList<GraphNode> getNodes();

  /**
   * This method returns the extra controls panel for the LayoutEngine, if there
   * is any.
   */
  JPanel getControlPanel();

  /**
   * This method returns the progress bar for the LayoutEngine, which shows the
   * progress of the layout process, if it takes a while to layout the graph
   */
  JProgressBar getProgressBar();

  /**
   * This method adds a LayoutCompleteEventListener to the LayoutEngine.
   * 
   * @param e - The LayoutCompleteEventListener to add
   */
  void addLayoutCompleteEventListener(LayoutCompleteEventListener e);

  /**
   * This method removes a LayoutCompleteEventListener from the LayoutEngine.
   * 
   * @param e - The LayoutCompleteEventListener to remove.
   */
  void removeLayoutCompleteEventListener(LayoutCompleteEventListener e);

  /**
   * This fires a LayoutCompleteEvent once a layout has been completed.
   */
  void fireLayoutCompleteEvent(LayoutCompleteEvent e);
}
