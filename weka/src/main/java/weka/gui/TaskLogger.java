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
 *    TaskLogger.java
 *    Copyright (C) 2000-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

/** 
 * Interface for objects that display log and display information on
 * running tasks.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public interface TaskLogger {
  
  /**
   * Tells the task logger that a new task has been started
   */
  void taskStarted();

  /**
   * Tells the task logger that a task has completed
   */
  void taskFinished();
}
