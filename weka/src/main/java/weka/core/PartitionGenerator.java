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
 *    PartitionGenerator.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.core;

/**
 * This interface can be implemented by algorithms that generate
 * a partition of the instance space (e.g., decision trees).
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 9117 $
 */
public interface PartitionGenerator extends CapabilitiesHandler {

  /**
   * Builds the classifier to generate a partition.
   */
  public void generatePartition(Instances data) throws Exception;
  
  /**
   * Computes an array that has a value for each element in the partition.
   */
  public double[] getMembershipValues(Instance inst) throws Exception;
  
  /**
   * Returns the number of elements in the partition.
   */
  public int numElements() throws Exception;
}
