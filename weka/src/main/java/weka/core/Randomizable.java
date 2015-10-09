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
 *    Randomizable.java
 *    Copyright (C) 2003-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

/** 
 * Interface to something that has random behaviour that is able to be
 * seeded with an integer.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public interface Randomizable {

  /**
   * Set the seed for random number generation.
   *
   * @param seed the seed 
   */
  void setSeed(int seed);
  
  /**
   * Gets the seed for the random number generations
   *
   * @return the seed for the random number generation
   */
  int getSeed();
}
