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
 *    Visible.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

/**
 * Interface to something that has a visible (via BeanVisual) reprentation
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 8034 $
 * @since 1.0
 */
public interface Visible {

  /**
   * Use the default visual representation
   */
  void useDefaultVisual();

  /**
   * Set a new visual representation
   *
   * @param newVisual a <code>BeanVisual</code> value
   */
  void setVisual(BeanVisual newVisual);

  /**
   * Get the visual representation
   *
   * @return a <code>BeanVisual</code> value
   */
  BeanVisual getVisual();
}
