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
 *    UnivariateIntervalEstimator.java
 *    Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.estimators;

/**
 * Interface that can be implemented by simple weighted univariate
 * interval estimators.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public interface UnivariateIntervalEstimator {

  /**
   * Adds a value to the interval estimator.
   *
   * @param value the value to add
   * @param weight the weight of the value
   */
  void addValue(double value, double weight);

  /**
   * Returns the intervals at the given confidence value. Each row has
   * one interval. The first element in each row is the lower bound,
   * the second element the upper one.
   *
   * @param confidenceValue the value at which to evaluate
   * @return the interval
   */
  double[][] predictIntervals(double confidenceValue);
}