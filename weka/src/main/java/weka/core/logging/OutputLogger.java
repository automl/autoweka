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
 * OutputLogger.java
 * Copyright (C) 2008-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.core.logging;

import java.io.PrintStream;
import java.util.Date;

import weka.core.RevisionUtils;
import weka.core.Tee;

/**
 * A logger that logs all output on stdout and stderr to a file.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class OutputLogger
  extends FileLogger {

  /**
   * A print stream class to capture all data from stdout and stderr.
   * 
   * @author  fracpete (fracpete at waikato dot ac dot nz)
   * @version $Revision: 8034 $
   */
  public static class OutputPrintStream
    extends PrintStream {
    
    /** the owning logger. */
    protected OutputLogger m_Owner;
    
    /** the line feed. */
    protected String m_LineFeed;
    
    /**
     * Default constructor.
     * 
     * @param owner		the owning logger
     * @param stream		the stream
     * @throws Exception	if something goes wrong
     */
    public OutputPrintStream(OutputLogger owner, PrintStream stream) throws Exception {
      super(stream);
      
      m_Owner    = owner;
      m_LineFeed = System.getProperty("line.separator");
    }

    /**
     * ignored.
     */
    public void flush() {
    }

    /**
     * prints the given int to the streams.
     * 
     * @param x 	the object to print
     */
    public void print(int x) {
      m_Owner.append("" + x);
    }

    /**
     * prints the given boolean to the streams.
     * 
     * @param x 	the object to print
     */
    public void print(boolean x) {
      m_Owner.append("" + x);
    }

    /**
     * prints the given string to the streams.
     * 
     * @param x 	the object to print
     */
    public void print(String x) {
      m_Owner.append("" + x);
    }

    /**
     * prints the given object to the streams.
     * 
     * @param x 	the object to print
     */
    public void print(Object x) {
      m_Owner.append("" + x);
    }

    /**
     * prints a new line to the streams.
     */
    public void println() {
      m_Owner.append(m_LineFeed);
    }

    /**
     * prints the given int to the streams.
     * 
     * @param x 	the object to print
     */
    public void println(int x) {
      m_Owner.append(x + m_LineFeed);
    }

    /**
     * prints the given boolean to the streams.
     * 
     * @param x 	the object to print
     */
    public void println(boolean x) {
      m_Owner.append(x + m_LineFeed);
    }

    /**
     * prints the given string to the streams.
     * 
     * @param x 	the object to print
     */
    public void println(String x) {
      m_Owner.append(x + m_LineFeed);
    }

    /**
     * prints the given object to the streams (for Throwables we print the stack
     * trace).
     * 
     * @param x 	the object to print
     */
    public void println(Object x) {
      m_Owner.append(x + m_LineFeed);
    }
  }
  
  /** the stream object used for logging stdout. */
  protected OutputPrintStream m_StreamOut;
  
  /** the stream object used for logging stderr. */
  protected OutputPrintStream m_StreamErr;

  /** the Tee instance to redirect stdout. */
  protected Tee m_StdOut;

  /** the Tee instance to redirect stderr. */
  protected Tee m_StdErr;
  
  /**
   * Initializes the logger.
   */
  protected void initialize() {
    super.initialize();
    
    try {
      m_StdOut = new Tee(System.out);
      System.setOut(m_StdOut);
      m_StreamOut = new OutputPrintStream(this, m_StdOut.getDefault());
      m_StdOut.add(m_StreamOut);
      
      m_StdErr = new Tee(System.err);
      System.setErr(m_StdErr);
      m_StreamErr = new OutputPrintStream(this, m_StdErr.getDefault());
      m_StdErr.add(m_StreamErr);
    }
    catch (Exception e) {
      // ignored
    }
  }
  
  /**
   * Performs the actual logging. 
   * 
   * @param level	the level of the message
   * @param msg		the message to log
   * @param cls		the classname originating the log event
   * @param method	the method originating the log event
   * @param lineno	the line number originating the log event
   */
  protected void doLog(Level level, String msg, String cls, String method, int lineno) {
    // append output to file
    append(
	m_DateFormat.format(new Date()) + " " + cls + " " + method + m_LineFeed
	+ level + ": " + msg + m_LineFeed);
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 8034 $");
  }
}
