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
 *    RemoteExperimentEvent.java
 *    Copyright (C) 2000-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.experiment;

import java.io.Serializable;

/**
 * Class encapsulating information on progress of a remote experiment
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public class RemoteExperimentEvent
  implements Serializable {

  /** for serialization */
  private static final long serialVersionUID = 7000867987391866451L;

  /** A status type message */
  public boolean m_statusMessage;

  /** A log type message */
  public boolean m_logMessage;

  /** The message */
  public String m_messageString;

  /** True if a remote experiment has finished */
  public boolean m_experimentFinished;

  /**
   * Constructor
   * @param status true for status type messages
   * @param log true for log type messages
   * @param finished true if experiment has finished
   * @param message the message
   */
  public RemoteExperimentEvent(boolean status, boolean log, boolean finished,
			       String message) {
    m_statusMessage = status;
    m_logMessage = log;
    m_experimentFinished = finished;
    m_messageString = message;
  }
}
