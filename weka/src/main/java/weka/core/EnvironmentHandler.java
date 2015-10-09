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
 * EnvironmentHandler.java
 * Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.core;


/**
 * Interface for something that can utilize environment
 * variables. NOTE: since environment variables should
 * be transient, the implementer needs to be careful
 * of state after de-serialization. Default system-wide
 * environment variables can be got via a call to
 * <code>weka.core.Environment.getSystemWide()</code>
 * 
 * @author mhall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 8034 $
 */
public interface EnvironmentHandler {
  
  /**
   * Set environment variables to use.
   * 
   * @param env the environment variables to
   * use
   */
  void setEnvironment(Environment env);
}
