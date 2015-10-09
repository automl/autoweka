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
 *    Compute.java
 *    Copyright (C) 2000-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.experiment;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface to something that can accept remote connections and execute
 * a task.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public interface Compute extends Remote {
  
  /**
   * Execute a task
   * @param t Task to be executed
   * @exception RemoteException if something goes wrong.
   * @return a unique ID for the task
   */
  Object executeTask(Task t) throws RemoteException;

  /**
   * Check on the status of a <code>Task</code>
   *
   * @param taskId the ID for the Task to be checked
   * @return the status of the Task
   * @exception Exception if an error occurs
   */
  Object checkStatus(Object taskId) throws Exception;
}

