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
 *    ThresholdDataEvent.java
 *    Copyright (C) 2003-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.util.EventObject;

import weka.core.Attribute;
import weka.gui.visualize.PlotData2D;

/**
 * Event encapsulating classifier performance data based on
 * varying a threshold over the classifier's predicted probabilities
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 8034 $
 * @see EventObject
 */
public class ThresholdDataEvent
  extends EventObject {

  /** for serialization */
  private static final long serialVersionUID = -8309334224492439644L;

  private PlotData2D m_dataSet;
  
  private Attribute m_classAttribute;

  public ThresholdDataEvent(Object source, PlotData2D dataSet) {
    this(source, dataSet, null);
  }
  
  public ThresholdDataEvent(Object source, PlotData2D dataSet, Attribute classAtt) {
    super(source);
    m_dataSet = dataSet;
    m_classAttribute = classAtt;
  }
  
  /**
   * Return the instances of the data set
   *
   * @return an <code>Instances</code> value
   */
  public PlotData2D getDataSet() {
    return m_dataSet;
  }
  
  /**
   * Return the class attribute for which the threshold data was generated
   * for.
   * 
   * @return the class attribute for the threshold data or null if not set.
   */
  public Attribute getClassAttribute() {
    return m_classAttribute;
  }
}
