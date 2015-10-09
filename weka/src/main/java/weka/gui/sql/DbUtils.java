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
 * DbUtils.java
 * Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.sql;

import java.sql.Connection;

import weka.core.RevisionUtils;
import weka.experiment.DatabaseUtils;

/**
 * A little bit extended DatabaseUtils class.
 *
 * @see       DatabaseUtils
 * @see       #execute(String)
 * @author    FracPete (fracpete at waikato dot ac dot nz)
 * @version   $Revision: 8034 $
 */
public class DbUtils
  extends DatabaseUtils {

  /** for serialization. */
  private static final long serialVersionUID = 103748569037426479L;
  
  /**
   * initializes the object.
   * 
   * @throws Exception      in case something goes wrong in the init of the
   *                        DatabaseUtils constructor
   * @see     DatabaseUtils
   */
  public DbUtils() throws Exception {
    super();
  }
  
  /**
   * returns the current database connection.
   * 
   * @return        the current connection instance
   */
  public Connection getConnection() {
    return m_Connection;
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
