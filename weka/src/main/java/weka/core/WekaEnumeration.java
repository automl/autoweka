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
 *    WekaEnumeration.java
 *    Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.core;

import java.util.Enumeration;
import java.util.List;

/**
 * Class for enumerating an array list's elements.
 */
public class WekaEnumeration<E> implements Enumeration<E>, RevisionHandler {

  /** The counter. */
  private int m_Counter;
  // These JML commands say how m_Counter implements Enumeration
  // @ in moreElements;
  // @ private represents moreElements = m_Counter < m_Vector.size();
  // @ private invariant 0 <= m_Counter && m_Counter <= m_Vector.size();

  /** The vector. */
  private final/* @non_null@ */List<E> m_Vector;

  /** Special element. Skipped during enumeration. */
  private final int m_SpecialElement;

  // @ private invariant -1 <= m_SpecialElement;
  // @ private invariant m_SpecialElement < m_Vector.size();
  // @ private invariant m_SpecialElement>=0 ==> m_Counter!=m_SpecialElement;

  /**
   * Constructs an enumeration.
   * 
   * @param vector the vector which is to be enumerated
   */
  public WekaEnumeration(/* @non_null@ */List<E> vector) {

    m_Counter = 0;
    m_Vector = vector;
    m_SpecialElement = -1;
  }

  /**
   * Constructs an enumeration with a special element. The special element is
   * skipped during the enumeration.
   * 
   * @param vector the vector which is to be enumerated
   * @param special the index of the special element
   */
  // @ requires 0 <= special && special < vector.size();
  public WekaEnumeration(/* @non_null@ */List<E> vector, int special) {

    m_Vector = vector;
    m_SpecialElement = special;
    if (special == 0) {
      m_Counter = 1;
    } else {
      m_Counter = 0;
    }
  }

  /**
   * Tests if there are any more elements to enumerate.
   * 
   * @return true if there are some elements left
   */
  @Override
  public final/* @pure@ */boolean hasMoreElements() {

    if (m_Counter < m_Vector.size()) {
      return true;
    }
    return false;
  }

  /**
   * Returns the next element.
   * 
   * @return the next element to be enumerated
   */
  // @ also requires hasMoreElements();
  @Override
  public final E nextElement() {

    E result = m_Vector.get(m_Counter);

    m_Counter++;
    if (m_Counter == m_SpecialElement) {
      m_Counter++;
    }
    return result;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10203 $");
  }
}
