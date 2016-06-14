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
 *    CommandlineRunnable.java
 *    Copyright (C) 2010-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

/**
 * Interface to something that can be run from the command line.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 12184 $
 */
public interface CommandlineRunnable {

  /**
   * Perform any setup stuff that might need to happen before execution.
   *
   * @throws Exception if a problem occurs during setup
   */
  void preExecution() throws Exception;
  
  /**
   * Execute the supplied object.
   * 
   * @param toRun the object to execute
   * @param options any options to pass to the object
   * @throws Exception if a problem occurs.
   */
  void run(Object toRun, String[] options) throws Exception;

  /**
   * Perform any teardown stuff that might need to happen after execution.
   *
   * @throws Exception if a problem occurs during teardown
   */
  void postExecution() throws Exception;
}
