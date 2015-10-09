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
 *    GOECustomizer.java
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

/**
 * Extends BeanCustomizer. Exists primarily for those customizers
 * that can be displayed in the GenericObjectEditor (in preference
 * to individual property editors). Provides a method to tell
 * the customizer not to show any OK and CANCEL buttons if being 
 * displayed in the GOE (since the GOE provides those). Also specifies
 * the methods for handling closing under OK or CANCEL conditions.
 * 
 * Implementers of this interface should *not* use the GOE internally 
 * to implement the customizer because this will result in an cyclic 
 * loop of initializations that will end up in a stack overflow when
 * the customizer is displayed by the GOE at the top level.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 8034 $
 */
public interface GOECustomizer extends BeanCustomizer {
  
  /**
   * Tells the customizer not to display its own OK and
   * CANCEL buttons
   */
  void dontShowOKCancelButtons();
  
  /**
   * Gets called when the customizer is closing under an OK
   * condition 
   */
  void closingOK();
  
  /**
   * Gets called when the customizer is closing under a
   * CANCEL condition
   */
  void closingCancel();
}
