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
 * Copyright (C) 2007 University of Waikato 
 */

package weka.core;

import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests AlgVector. Run from the command line with:<p/>
 * java weka.core.AlgVectorTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class AlgVectorTest 
  extends TestCase {

  /** for generating the datasets */
  protected Random m_Random;
  
  /**
   * Constructs the <code>AlgVectorTest</code>.
   *
   * @param name 	the name of the test class
   */
  public AlgVectorTest(String name) { 
    super(name); 
  }
  
  /**
   * Called by JUnit before each test method.
   *
   * @throws Exception 	if an error occurs
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    m_Random = new Random(1);
  }

  /**
   * Called by JUnit after each test method
   * 
   * @throws Exception 	if an error occurs
   */
  protected void tearDown() throws Exception {
    super.tearDown();

    m_Random = null;
  }

  /**
   * generates data with the given amount of nominal and numeric attributes
   * 
   * @param nominal	the number of nominal attributes
   * @param numeric	the number of numeric attributes
   * @param rows	the number of rows to generate
   * @return		the generated data
   */
  protected Instances generateData(int nominal, int numeric, int rows) {
    Instances		result;
    TestInstances	test;
    
    test = new TestInstances();
    test.setClassIndex(TestInstances.NO_CLASS);
    test.setNumNominal(nominal);
    test.setNumNumeric(numeric);
    test.setNumInstances(rows);
    
    try {
      result = test.generate();
    }
    catch (Exception e) {
      result = null;
    }
    
    return result;
  }
  
  /**
   * tests constructing a vector with a given length
   */
  public void testLengthConstructor() {
    int len = 22;
    AlgVector v = new AlgVector(len);
    assertEquals("Length differs", len, v.numElements());
  }

  /**
   * tests constructing a vector from an array
   */
  public void testArrayConstructor() {
    double[] data = {2.3, 1.2, 5.0};
    AlgVector v = new AlgVector(data);
    assertEquals("Length differs", data.length, v.numElements());
    for (int i = 0; i < data.length; i++)
      assertEquals((i+1) + ". value differs", data[i], v.getElement(i));
  }

  /**
   * runs tests with the given data
   * 
   * @param data	the data to test with
   */
  protected void runTestOnData(Instances data) {
    // count numeric atts
    int numeric = 0;
    for (int n = 0; n < data.numAttributes(); n++) {
      if (data.attribute(n).isNumeric())
	numeric++;
    }
    
    // perform tests
    for (int n = 0; n < data.numInstances(); n++) {
      try {
	AlgVector v = new AlgVector(data.instance(n));
	
	// 1. is length correct?
	assertEquals((n+1) + ": length differs", numeric, v.numElements());
	
	// 2. are values correct?
	int index = 0;
	for (int i = 0; i < data.numAttributes(); i++) {
	  if (!data.attribute(i).isNumeric())
	    continue;
	  assertEquals((n+1) + "/" + (i+1) + ": value differs", data.instance(n).value(i), v.getElement(index));
	  index++;
	}
	
	// 3. is instance returned correct?
	Instance inst = v.getAsInstance(data, new Random(1));
	for (int i = 0; i < data.numAttributes(); i++) {
	  if (!data.attribute(i).isNumeric())
	    continue;
	  assertEquals((n+1) + "/" + (i+1) + ": returned value differs", data.instance(n).value(i), inst.value(i));
	}
      }
      catch (Exception e) {
	if (!(e instanceof IllegalArgumentException))
	  fail(e.toString());
      }
    }
  }
  
  /**
   * tests constructing a vector from a purely numeric instance
   */
  public void testNumericInstances() {
    runTestOnData(generateData(0, 5, 5));
  }
  
  /**
   * tests constructing a vector from a purely nominal instance
   */
  public void testNominalInstances() {
    runTestOnData(generateData(5, 0, 5));
  }
  
  /**
   * tests constructing a vector from a mixed instance
   */
  public void testMixedInstances() {
    runTestOnData(generateData(5, 5, 5));
  }
  
  public static Test suite() {
    return new TestSuite(AlgVectorTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
