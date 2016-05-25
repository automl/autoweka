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
 *    GainRatioSplitCrit.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.j48;

import weka.core.ContingencyTables;
import weka.core.RevisionUtils;
import weka.core.Utils;

/**
 * Class for computing the gain ratio for a given distribution.
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 10169 $
 */
public final class GainRatioSplitCrit extends EntropyBasedSplitCrit {

  /** for serialization */
  private static final long serialVersionUID = -433336694718670930L;

  /**
   * This method is a straightforward implementation of the gain ratio criterion
   * for the given distribution.
   */
  @Override
  public final double splitCritValue(Distribution bags) {

    double numerator;
    double denumerator;

    numerator = oldEnt(bags) - newEnt(bags);

    // Splits with no gain are useless.
    if (Utils.eq(numerator, 0)) {
      return Double.MAX_VALUE;
    }
    denumerator = splitEnt(bags);

    // Test if split is trivial.
    if (Utils.eq(denumerator, 0)) {
      return Double.MAX_VALUE;
    }

    // We take the reciprocal value because we want to minimize the
    // splitting criterion's value.
    return denumerator / numerator;
  }

  /**
   * This method computes the gain ratio in the same way C4.5 does.
   * 
   * @param bags the distribution
   * @param totalnoInst the weight of ALL instances
   * @param numerator the info gain
   */
  public final double splitCritValue(Distribution bags, double totalnoInst,
    double numerator) {

    double denumerator;
    // Compute split info.
    denumerator = splitEnt(bags, totalnoInst);

    // Test if split is trivial.
    if (Utils.eq(denumerator, 0)) {
      return 0;
    }
    denumerator = denumerator / totalnoInst;

    return numerator / denumerator;
  }

  /**
   * Help method for computing the split entropy.
   */
  private final double splitEnt(Distribution bags, double totalnoInst) {

    double returnValue = 0;
    double noUnknown;
    int i;

    noUnknown = totalnoInst - bags.total();
    if (Utils.gr(bags.total(), 0)) {
      for (i = 0; i < bags.numBags(); i++) {
        returnValue = returnValue - lnFunc(bags.perBag(i));
      }
      returnValue = returnValue - lnFunc(noUnknown);
      returnValue = returnValue + lnFunc(totalnoInst);
    }
    return returnValue / ContingencyTables.log2;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10169 $");
  }
}
