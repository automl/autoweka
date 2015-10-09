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
 *    Sourcable.java
 *    Copyright (C) 2007-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters;

import weka.core.Instances;

/** 
 * Interface for filters that can be converted to Java source.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public interface Sourcable {

  /**
   * Returns a string that describes the filter as source. The
   * filter will be contained in a class with the given name (there may
   * be auxiliary classes),
   * and will contain two methods with these signatures:
   * <pre><code>
   * // converts one row
   * public static Object[] filter(Object[] i);
   * // converts a full dataset (first dimension is row index)
   * public static Object[][] filter(Object[][] i);
   * </code></pre>
   * where the array <code>i</code> contains elements that are either
   * Double, String, with missing values represented as null. The generated
   * code is public domain and comes with no warranty.
   *
   * @param className   the name that should be given to the source class.
   * @param data	the dataset used for initializing the filter
   * @return            the object source described by a string
   * @throws Exception  if the source can't be computed
   */
  public String toSource(String className, Instances data) throws Exception;
}
