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
 * InstanceInfo.java
 * Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.visualize;

import java.util.Vector;

import weka.core.Instances;

/**
 * Interface for JFrames that display instance info.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public interface InstanceInfo {
  
  /**
   * Sets the text to display.
   * 
   * @param text	the text to display
   */
  public void setInfoText(String text);
  
  /**
   * Returns the currently displayed info text.
   * 
   * @return		the info text
   */
  public String getInfoText();
  
  /**
   * Sets the underlying data.
   * 
   * @param data	the data of the info text
   */
  public void setInfoData(Vector<Instances> data);
  
  /**
   * Returns the underlying data.
   * 
   * @return		the data of the info text, can be null
   */
  public Vector<Instances> getInfoData();
}
