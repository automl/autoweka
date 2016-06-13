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
 *    ProtectedProperties.java
 *    Copyright (C) 2001-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * Simple class that extends the Properties class so that the properties are
 * unable to be modified.
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 10203 $
 */
public class ProtectedProperties extends Properties implements RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = 3876658672657323985L;

  /** the properties need to be open during construction of the object */
  private boolean closed = false;

  /**
   * Creates a set of protected properties from a set of normal ones.
   * 
   * @param props the properties to be stored and protected.
   */
  public ProtectedProperties(Properties props) {

    Enumeration<?> propEnum = props.propertyNames();
    while (propEnum.hasMoreElements()) {
      String propName = (String) propEnum.nextElement();
      String propValue = props.getProperty(propName);
      super.setProperty(propName, propValue);
    }
    closed = true; // no modifications allowed from now on
  }

  /**
   * Overrides a method to prevent the properties from being modified.
   * 
   * @return never returns without throwing an exception.
   * @throws UnsupportedOperationException always.
   */
  @Override
  public Object setProperty(String key, String value) {

    if (closed) {
      throw new UnsupportedOperationException(
        "ProtectedProperties cannot be modified!");
    } else {
      return super.setProperty(key, value);
    }
  }

  /**
   * Overrides a method to prevent the properties from being modified.
   * 
   * @throws UnsupportedOperationException always.
   */
  @Override
  public void load(InputStream inStream) {

    throw new UnsupportedOperationException(
      "ProtectedProperties cannot be modified!");
  }

  /**
   * Overrides a method to prevent the properties from being modified.
   * 
   * @throws UnsupportedOperationException always.
   */
  @Override
  public void clear() {

    throw new UnsupportedOperationException(
      "ProtectedProperties cannot be modified!");
  }

  /**
   * Overrides a method to prevent the properties from being modified.
   * 
   * @return never returns without throwing an exception.
   * @throws UnsupportedOperationException always.
   */
  @Override
  public Object put(Object key, Object value) {

    if (closed) {
      throw new UnsupportedOperationException(
        "ProtectedProperties cannot be modified!");
    } else {
      return super.put(key, value);
    }
  }

  /**
   * Overrides a method to prevent the properties from being modified.
   * 
   * @throws UnsupportedOperationException always.
   */
  @Override
  public void putAll(Map<? extends Object, ? extends Object> t) {

    throw new UnsupportedOperationException(
      "ProtectedProperties cannot be modified!");
  }

  /**
   * Overrides a method to prevent the properties from being modified.
   * 
   * @return never returns without throwing an exception.
   * @throws UnsupportedOperationException always.
   */
  @Override
  public Object remove(Object key) {

    throw new UnsupportedOperationException(
      "ProtectedProperties cannot be modified!");
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
