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
 *    StreamThroughput.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.io.Serializable;

import weka.gui.Logger;

/**
 * Class for measuring throughput of an incremental Knowledge Flow step. Typical
 * usage is to construct a StreamThroughput object at the start of the stream
 * (i.e. FORMAT_AVAILABLE event) and then for each instance received call
 * updateStart() just before processing the instance and then updateEnd() just
 * after. If updateEnd() is called *before* sending any event to downstream
 * step(s) then throughput just with respect to work done by the step will be
 * measured.
 * 
 * Elapsed time to process each instance (along with the number of instances) is
 * accumulated over the sample time period. Instances per second is computed at
 * the end of each sample period and added to a running total. Average
 * instances/sec is reported to the status area of the log.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 9243 $
 */
public class StreamThroughput implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 2820675210555581676L;

  protected transient int m_avInstsPerSec = 0;
  protected transient double m_startTime;
  protected transient int m_instanceCount;
  protected transient int m_sampleCount;
  protected transient String m_statusMessagePrefix = "";

  /**
   * sample period over which to count instances processed and instances/sec
   * throughput
   */
  protected transient int m_sampleTime = 2000;

  protected transient double m_cumulativeTime;
  protected transient int m_numSamples;

  /**
   * Construct a new StreamThroughput
   * 
   * @param statusMessagePrefix the unique identifier of the Knowledge Flow
   *          component being measured. This enables the correct line in the
   *          status area to be updated. See any Knowledge Flow step for an
   *          example.
   */
  public StreamThroughput(String statusMessagePrefix) {
    m_instanceCount = 0;
    m_sampleCount = 0;
    m_numSamples = 0;
    m_cumulativeTime = 0;
    m_startTime = System.currentTimeMillis();
    m_statusMessagePrefix = statusMessagePrefix;
  }

  /**
   * Construct a new StreamThroughput
   * 
   * @param statusMessagePrefix the unique identifier of the Knowledge Flow
   *          component being measured. This enables the correct line in the
   *          status area to be updated. See any Knowledge Flow step for an
   *          example.
   * @param initialMessage an initial message to print to the status area for
   *          this step on construction
   * @param log the log to write status updates to
   */
  public StreamThroughput(String statusMessagePrefix, String initialMessage,
      Logger log) {
    this(statusMessagePrefix);
    if (log != null) {
      log.statusMessage(m_statusMessagePrefix + initialMessage);
    }
  }

  /**
   * Set the sampling period (in milliseconds) to compute througput over
   * 
   * @param period the sampling period in milliseconds
   */
  public void setSamplePeriod(int period) {
    m_sampleTime = period;
  }

  protected transient double m_updateStart;

  /**
   * Register a throughput measurement start point
   */
  public void updateStart() {
    m_updateStart = System.currentTimeMillis();
  }

  /**
   * Register a throughput measurement end point. Collects counts and
   * statistics. Will update the status area for the KF step in question if the
   * sample period has elapsed.
   * 
   * @param log the log to write status updates to
   */
  public void updateEnd(Logger log) {
    m_instanceCount++;
    m_sampleCount++;
    double end = System.currentTimeMillis();
    double temp = end - m_updateStart;
    m_cumulativeTime += temp;
    boolean toFastToMeasure = false;

    if ((end - m_startTime) >= m_sampleTime) {
      computeUpdate(end);

      if (log != null) {
        log.statusMessage(m_statusMessagePrefix + "Processed "
            + m_instanceCount + " insts @ " + m_avInstsPerSec / m_numSamples
            + " insts/sec" + (toFastToMeasure ? "*" : ""));
      }
      m_sampleCount = 0;
      m_cumulativeTime = 0;
      m_startTime = System.currentTimeMillis();
    }
  }

  protected boolean computeUpdate(double end) {
    boolean toFastToMeasure = false;
    int instsPerSec = 0;

    if (m_cumulativeTime == 0) {
      // all single instance updates have taken < 1 millisecond each!
      // the best we can do is compute the insts/sec based on the total
      // number of instances processed in the elapsed sample time
      // (rather than using the total number processed and the actual
      // cumulative elapsed processing time). This is going to be closer
      // to the throughput for the entire flow rather than for the component
      // itself
      double sampleTime = (end - m_startTime);
      instsPerSec = (int) (m_sampleCount / (sampleTime / 1000.0));
      toFastToMeasure = true;
    } else {
      instsPerSec = (int) (m_sampleCount / (m_cumulativeTime / 1000.0));
    }
    m_numSamples++;
    m_avInstsPerSec += instsPerSec;

    return toFastToMeasure;
  }

  /**
   * Get the average instances per second
   * 
   * @return the average instances per second processed
   */
  public int getAverageInstancesPerSecond() {
    int nS = m_numSamples > 0 ? m_numSamples : 1;
    return m_avInstsPerSec / nS;
  }

  /**
   * Register the end of measurement. Writes a "Finished" update (that includes
   * the final throughput info) to the status area of the log.
   * 
   * @param log the log to write to
   * @return the message written to the status area.
   */
  public String finished(Logger log) {
    if (m_avInstsPerSec == 0) {
      computeUpdate(System.currentTimeMillis());
    }

    int nS = m_numSamples > 0 ? m_numSamples : 1;
    String msg = "Finished - " + m_instanceCount + " insts @ "
        + m_avInstsPerSec / nS + " insts/sec";
    if (log != null) {
      log.statusMessage(m_statusMessagePrefix + msg);
    }

    return msg;
  }

  /**
   * Register the end of measurement. Does not write a "Finished" update to the
   * log
   * 
   * @return a message that contains the final throughput info.
   */
  public String finished() {
    if (m_avInstsPerSec == 0) {
      computeUpdate(System.currentTimeMillis());
    }

    int nS = m_numSamples > 0 ? m_numSamples : 1;
    String msg = "Finished - " + m_instanceCount + " insts @ "
        + m_avInstsPerSec / nS + " insts/sec";

    return msg;
  }
}
