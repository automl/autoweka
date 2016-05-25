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
 * UpdateableClusterer.java
 * Copyright (C) 2006-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.clusterers;

import weka.core.Instance;

/**
 * Interface to incremental cluster models that can learn using one instance 
 * at a time.
 * 
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10829 $
 */
public interface UpdateableClusterer {

  /**
   * Adds an instance to the clusterer.
   *
   * @param newInstance the instance to be added
   * @throws Exception 	if something goes wrong
   */
  public void updateClusterer(Instance newInstance) throws Exception;

  /**
   * Signals the end of the updating.
   */
  public void updateFinished();
}
