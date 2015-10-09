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
 *    IterativeClassifier.java
 *    Copyright (C) 2001-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers;

import weka.core.Instances;

/**
 * Interface for classifiers that can induce models of growing
 * complexity one step at a time.
 *
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */

public interface IterativeClassifier {

  /**
   * Inits an iterative classifier.
   *
   * @param instances the instances to be used in induction
   * @exception Exception if the model cannot be initialized
   */
  void initClassifier(Instances instances) throws Exception;

  /**
   * Performs one iteration.
   *
   * @param iteration the index of the current iteration (0-based)
   * @exception Exception if this iteration fails
   */
  void next(int iteration) throws Exception;

  /**
   * Signal end of iterating, useful for any house-keeping/cleanup
   *
   * @exception Exception if cleanup fails
   */
  void done() throws Exception;

  /**
    * Performs a deep copy of the classifier, and a reference copy
    * of the training instances (or a deep copy if required).
    *
    * @return a clone of the classifier
    */
  Object clone() throws CloneNotSupportedException;

}
