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
 *    BatchAssociationRulesListener.java
 *    Copyright (C) 2010-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

/**
 * Interface to something that can process a BatchAssociationRulesEvent.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 *
 */
public interface BatchAssociationRulesListener {

  /**
   * Accept a <code>BatchAssociationRulesEvent</code>
   * 
   * @param e a <code>BatchAssociationRulesEvent</code>
   */
  void acceptAssociationRules(BatchAssociationRulesEvent e);
}
