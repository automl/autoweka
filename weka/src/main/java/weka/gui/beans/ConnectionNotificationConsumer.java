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
 *    ConnectionNotificationConsumer.java
 *    Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.beans;

/**
 * Interface for Beans that can receive (dis-)connection events generated when
 * (dis-)connecting data processing nodes in the Weka KnowledgeFlow.
 * 
 * This is useful, for example, for "intelligent" filters that are able to share
 * configuration information with preceding nodes in the processing chain.
 * 
 * @author Carsten Pohle (cp AT cpohle de)
 * @version $Revision: 8034 $
 */
public interface ConnectionNotificationConsumer {
  /**
   * Notify this object that it has been registered as a listener with a source
   * with respect to the supplied event name.
   * 
   * This method should be implemented <emph>synchronized</emph>.
   * 
   * @param eventName
   * @param source
   *          the source with which this object has been registered as a
   *          listener
   */
  public void connectionNotification(String eventName, Object source);

  /**
   * Notify this object that it has been deregistered as a listener with a
   * source with respect to the supplied event name
   * 
   * This method should be implemented <emph>synchronized</emph>.
   * 
   * @param eventName
   *          the event
   * @param source
   *          the source with which this object has been registered as a
   *          listener
   */
  public void disconnectionNotification(String eventName, Object source);

}
