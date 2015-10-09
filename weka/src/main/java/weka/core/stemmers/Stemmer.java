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
 * Stemmer.java
 * Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.stemmers;

import java.io.Serializable;

import weka.core.RevisionHandler;

/**
 * Interface for all stemming algorithms.
 *
 * @author    FracPete (fracpete at waikato dot ac dot nz)
 * @version   $Revision: 8034 $
 */
public interface Stemmer 
  extends Serializable, RevisionHandler {

  /**
   * Stems the given word and returns the stemmed version
   *
   * @param word      the unstemmed word
   * @return          the stemmed word
   */
  public String stem(String word);
}
