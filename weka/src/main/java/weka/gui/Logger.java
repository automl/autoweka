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
 *    Logger.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */


package weka.gui;

/** 
 * Interface for objects that display log (permanent historical) and
 * status (transient) messages.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public interface Logger {

  /**
   * Sends the supplied message to the log area. These message will typically
   * have the current timestamp prepended, and be viewable as a history.
   *
   * @param message the log message
   */
  void logMessage(String message);
  
  /**
   * Sends the supplied message to the status line. These messages are
   * typically one-line status messages to inform the user of progress
   * during processing (i.e. it doesn't matter if the user doesn't happen
   * to look at each message)
   *
   * @param message the status message.
   */
  void statusMessage(String message);
  
}
