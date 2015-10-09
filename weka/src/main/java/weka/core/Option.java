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
 *    Option.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

/** 
 * Class to store information about an option. <p>
 *
 * Typical usage: <p>
 *
 * <code>Option myOption = new Option("Uses extended mode.", "E", 0, "-E")); </code><p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public class Option
  implements RevisionHandler {

  /** What does this option do? */
  private String m_Description;

  /** The synopsis. */
  private String m_Synopsis;

  /** What's the option's name? */
  private String m_Name;

  /** How many arguments does it take? */
  private int m_NumArguments;

  /**
   * Creates new option with the given parameters.
   *
   * @param description the option's description
   * @param name the option's name
   * @param numArguments the number of arguments
   */
  public Option(String description, String name, 
		int numArguments, String synopsis) {
  
    m_Description = description;
    m_Name = name;
    m_NumArguments = numArguments;
    m_Synopsis = synopsis;
  }

  /**
   * Returns the option's description.
   *
   * @return the option's description
   */
  public String description() {
  
    return m_Description;
  }

  /**
   * Returns the option's name.
   *
   * @return the option's name
   */
  public String name() {

    return m_Name;
  }

  /**
   * Returns the option's number of arguments.
   *
   * @return the option's number of arguments
   */
  public int numArguments() {
  
    return m_NumArguments;
  }

  /**
   * Returns the option's synopsis.
   *
   * @return the option's synopsis
   */
  public String synopsis() {
  
    return m_Synopsis;
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

