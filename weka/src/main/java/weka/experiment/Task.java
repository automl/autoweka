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
 *    Task.java
 *    Copyright (C) 2000-2012 University of Waikato, Hamilton, New Zealand
 *
 */


package weka.experiment;

import java.io.Serializable;

/**
 * Interface to something that can be remotely executed as a task.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public interface Task extends Serializable {
  
  /**
   * Execute this task.
   */
  void execute();

  /**
   * Clients should be able to call this method at any time to obtain
   * information on a current task.
   *
   * @return a TaskStatusInfo object holding info and result (if available) for
   * this task
   */
  TaskStatusInfo getTaskStatus();
}
