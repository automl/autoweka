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
 * WekaTestSuite.java
 * Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.test;

import java.util.Collections;
import java.util.StringTokenizer;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.core.ClassDiscovery;
import weka.gui.GenericPropertiesCreator;

/**
 * Extends the standard TestSuite class wtih some additional methods for
 * automatic generation of a series of tests.
 *
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 * @see     GenericPropertiesCreator
 * @see     ClassDiscovery
 */
public class WekaTestSuite
  extends TestSuite {

  /**
   * checks whether the classname is a valid one, i.e., from a public class
   *
   * @param classname   the classname to check
   * @return            whether the classname is a valid one
   */
  protected static boolean isValidClassname(String classname) {
    return (classname.indexOf("$") == -1);
  }
  
  /**
   * determines all the classes derived from the given superclass in the
   * specified packages
   *
   * @param superclass  the class to find subclasses for
   * @param pacakges    the packages to search in for subclasses
   * @return            the classes that were found
   */
  protected static Vector getClassnames(String superclass, Vector packages) {
    Vector        result;
    Vector        names;
    int           i;
    int           n;

    result = new Vector();

    for (i = 0; i < packages.size(); i++) {
      names = ClassDiscovery.find(superclass, (String) packages.get(i));
      for (n = 0; n < names.size(); n++) {
        // skip non-public classes
        if (isValidClassname((String) names.get(n)))
          result.add(names.get(n));
      }
    }
    
    return result;
  }

  /**
   * returns a Vector with all the classnames of the specified property in 
   * the GenericPropertiesCreator.
   *
   * @param property    the property to get the classnames for
   * @return            the classnames of the given property
   * @see               GenericPropertiesCreator
   */
  protected static Vector getClassnames(String property) {
    GenericPropertiesCreator  gpc;
    String                    classes;
    Vector                    result;
    StringTokenizer           tok;
    String                    classname;
    
    result = new Vector();

    try {
      gpc = new GenericPropertiesCreator();
      gpc.execute(false);
      
      classes = gpc.getOutputProperties().getProperty(property);
      tok     = new StringTokenizer(classes, ",");
      
      while (tok.hasMoreTokens()) {
        classname = tok.nextToken();
        // skip non-public classes
        if (isValidClassname(classname))
          result.add(classname);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  /**
   * generates a Test class name for a given "regular" class
   * 
   * @param classname   the class to generate the Test class name for
   * @return            the classname of the test
   */
  protected static String getTestClassname(String classname) {
    if (!classname.endsWith("Test"))
      return classname + "Test";
    else
      return classname;
  }

  /**
   * returns the test class if it exists, for the given class, otherwise
   * null
   *
   * @param classname   the class to retrieve the Test class for
   * @return            non-null, if the Test class exists
   */
  protected static Class testClassFor(String classname) {
    Class         result;
    
    result = null;
    
    try {
      result = Class.forName(getTestClassname(classname));
    }
    catch (Exception e) {
      // ignore it
    }

    return result;
  }

  /**
   * tries to find Test classes for all the given classnames, if successful
   * they're added to the Test
   *
   * @param classnames    the classnames to get 
   */
  protected static TestSuite addAll(Vector classnames) {
    int           i;
    Class         tc;
    TestSuite     result;

    result = new TestSuite();
    
    for (i = 0; i < classnames.size(); i++) {
      tc = testClassFor((String) classnames.get(i));
      if (tc != null)
        result.addTest(new TestSuite(tc));
    }

    return result;
  }
  
  /**
   * adds all Tests for classes that are available via the
   * GenericPropertiesCreator's property to the Test and returns that
   *
   * @param property    the GPC property to add all the classes to the Test
   * @return            the generated Test
   * @see               GenericPropertiesCreator
   */
  public static TestSuite addAll(String property) {
    return addAll(getClassnames(property));
  }

  /**
   * adds all available Tests for a given superclass and the packages to 
   * check in
   *
   * @param superclass  the superclass to find tests of subclasses for
   * @param packages    the packages (strings) to search in
   * @return            the generated Test
   */
  public static TestSuite addAll(String superclass, Vector packages) {
    return addAll(getClassnames(superclass, packages));
  }

  /**
   * determines all the test classes that are missing for the given 
   * classnames.
   *
   * @param classnames  the classnames that are to be checked
   * @return            the missing Test classes
   */
  protected static Vector getMissing(Vector classnames) {
    int           i;
    Vector        result;

    result = new Vector();
    
    for (i = 0; i < classnames.size(); i++) {
      if (testClassFor((String) classnames.get(i)) == null)
        result.add(getTestClassname((String) classnames.get(i)));
    }

    return result;
  }

  /**
   * determines all the test classes that are missing for the given
   * GenericPropertiesCreator's property's elements
   *
   * @param property    the GPC property to determine the missing Tests for
   * @return            the classnames of the missing Tests
   */
  public static Vector getMissing(String property) {
    return getMissing(getClassnames(property));
  }

  /**
   * determines all the test classes of subclasses that are missing for the 
   * given superclass and packages to look in for
   *
   * @param superclass  the superclass to check for tests of derived classes
   * @param packages    the packages to search in
   * @return            the classnames of the missing Tests
   */
  public static Vector getMissing(String superclass, Vector packages) {
    return getMissing(getClassnames(superclass, packages));
  }

  /**
   * outputs the missing Test classes (if any) and returns the given TestSuite
   * 
   * @param t           the generated test suite, is only passed through
   * @param missing     the missing test classes, if any
   * @return            the previously generate test suite
   */
  protected static Test suite(Test t, Vector missing) {
    if (missing.size() > 0) {
      Collections.sort(missing);

      System.out.println("Missing Test classes:");
      for (int i = 0; i < missing.size(); i++)
        System.out.println("- " + missing.get(i));
      System.out.println();
    }

    return t;
  }

  /**
   * Generates a TestSuite for the given GenericPropertiesCreator property
   * and returns it. Potentially missing test classes are output.
   *
   * @param property  the GPC property to generate a test suite from
   * @return          the generated test suite
   */
  public static Test suite(String property) {
    return suite(addAll(property), getMissing(property));
  }

  /**
   * Generates a TestSuite for all the Test class of subclasses of the given
   * superclasses. The given package names are used in the search.
   * Potentially missing test classes are output.
   *
   * @param superclass  the class to generate the test suite for
   * @param packages    the packages to look for test classes
   * @return            the generated test suite
   */
  public static Test suite(String superclass, Vector packages) {
    return suite(addAll(superclass, packages), getMissing(superclass, packages));
  }
}
