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
 *    BatchPredictor.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand.
 *
 */

package weka.core;

/**
 * Interface to something that can produce predictions in a batch manner 
 * when presented with a set of Instances.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 11958 $
 */
public interface BatchPredictor {

  /**
   * Set the batch size to use. The implementer will  
   * prefer (but not necessarily expect) this many instances 
   * to be passed in to distributionsForInstances().
   *
   * @param size the batch size to use
   */
  void setBatchSize(String size);

  /**
   * Get the batch size to use. The implementer will prefer (but not  
   * necessarily expect) this many instances to be passed in to 
   * distributionsForInstances(). Allows the preferred batch size 
   * to be encapsulated with the client.
   *
   * @return the batch size to use
   */
  String getBatchSize();

  /**
   * Batch scoring method
   * 
   * @param insts the instances to get predictions for
   * @return an array of probability distributions, one for each instance
   * @throws Exception if a problem occurs
   */
  double[][] distributionsForInstances(Instances insts) throws Exception;

  /**
   * Returns true if this BatchPredictor can generate batch predictions
   * in an efficient manner.
   *
   * @return true if batch predictions can be generated efficiently
   */
  boolean implementsMoreEfficientBatchPrediction();
}
