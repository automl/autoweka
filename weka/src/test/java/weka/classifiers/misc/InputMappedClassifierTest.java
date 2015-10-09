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
 * Copyright 2010 University of Waikato
 */

package weka.classifiers.misc;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.Classifier;
import weka.classifiers.misc.InputMappedClassifier;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.TestInstances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Reorder;
import weka.filters.unsupervised.attribute.SwapValues;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests InputMappedClassifier. Run from the command line with:<p>
 * java weka.classifiers.misc.InputMappedClassifierTest
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 8034 $
 */
public class InputMappedClassifierTest extends AbstractClassifierTest {

  public InputMappedClassifierTest(String name) { super(name);  }

  /** Creates a default InputMappedClassifier */
  public Classifier getClassifier() {
    InputMappedClassifier toUse = new InputMappedClassifier();
    toUse.setClassifier(new weka.classifiers.trees.J48());
    toUse.setSuppressMappingReport(true);
    return toUse;
  }
  
  protected Instances reorderAtts(Instances data) throws Exception {
    Reorder r = new Reorder();
    String range = "last";
    for (int i = data.numAttributes() - 1; i > 0; i--) {
      range += "," + i;
    }
    r.setAttributeIndices(range);
    r.setInputFormat(data);
    data = Filter.useFilter(data, r);
    
    return data;
  }
  
  protected Instances swapValues(int attIndex, Instances data) throws Exception {
    SwapValues s = new SwapValues();
    s.setAttributeIndex("" + attIndex);
    s.setFirstValueIndex("first");
    s.setSecondValueIndex("last");
    s.setInputFormat(data);
    data = Filter.useFilter(data, s);
    
    return data;
  }
  
  protected Instances generateData(boolean nomClass, int numClasses, 
      int numNominal, int numNumeric) throws Exception {
    
    TestInstances generator = new TestInstances();
    
    if (nomClass) {
      generator.setClassType(Attribute.NOMINAL);
      generator.setNumClasses(numClasses);
    } else {
      generator.setClassType(Attribute.NUMERIC);
    }
    
    generator.setNumNominal(numNominal);
    generator.setNumNumeric(numNumeric);
    
    generator.setNumDate(0);
    generator.setNumString(0);
    generator.setNumRelational(0);
    generator.setNumInstances(100);
    
    generator.setClassIndex(TestInstances.CLASS_IS_LAST);
    Instances data = generator.generate();
        
    return data;
  }
  
  protected void performTest(boolean nomClass, int numClassesTrain,
      int numTrainAtts,
      boolean reorderAtts, boolean reorderNomLabels,
      boolean reorderClassLabels) {
    Instances train = null;
    Instances test = null;
    
    try {
      train = generateData(nomClass, numClassesTrain, numTrainAtts, 3);
    } catch (Exception ex) {
      fail("Generating training data failed: " + ex);
    }
    
    test = new Instances(train);
    
    if (reorderNomLabels) {
      // do the first attribute
      try {
        test = swapValues(1, test);
      } catch (Exception ex) {
        fail("Reordering nominal labels failed: " + ex);
      }
    }
    
    if (reorderClassLabels && nomClass) {
      try {
        test = swapValues(7, test);
      } catch (Exception ex) {
        fail("Reordering class labels failed: " + ex);
      }
    }

    if (reorderAtts) {
      try {
        test = reorderAtts(test);
      } catch (Exception ex) {
        fail("Reordering test data failed: " + ex);
      }
    }
    
    InputMappedClassifier toUse = null;
    
    try {
      toUse = trainClassifier(train, nomClass);
    } catch (Exception ex) {
      fail("Training classifier failed: " + ex);
    }
    
    double[] resultsOnTrainingStructure = null;
    try {
      resultsOnTrainingStructure = testClassifier(train, toUse);
    } catch (Exception ex) {
      fail("Testing classifier on training data failed: " + ex);
    }
    
    double[] resultsOnTestStructure = null;
    try {
      resultsOnTestStructure = testClassifier(test, toUse);
    } catch (Exception ex) {
      fail("Testing classifier on test data failed: " + ex);
    }
    
    try {
      for (int i = 0; i < resultsOnTrainingStructure.length; i++) {
        if (resultsOnTrainingStructure[i] != resultsOnTestStructure[i]) {
          throw new Exception("Result #" + (i+1) + " differs!");
        }
      }
    } catch (Exception ex) {
      fail("Comparing results failed " + ex);
    }
  }
  
  public void testNominaClass() {
    performTest(true, 4, 3, false, false, false);        
  }
  
/*  public void testNominalClassDifferingNumClassValues() {
    performTest(true, 4, 6, 3, 3, false, false, false);        
  } */
  
  public void testNominaClassReorderedAtts() {    
    performTest(true, 4, 3, true, false, false);        
  }
  
  public void testNominalClassSwapNominalValues() {
    performTest(true, 4, 3, false, true, false);
  }
  
  public void testNominalClassSwapNominalValuesReorderAtts() {
    performTest(true, 4, 3, true, true, false);
  }
  
  public void testNominalClassSwapClassValues() {
    performTest(true, 4, 3, false, false, true);        
  }
  
  public void testNominalClassSwapNominalValuesSwapClassValues() {
    performTest(true, 4, 3, false, true, true);        
  }
  
  public void testNominalClassSwapNominalValuesSwapClassValuesReorderAtts() {
    performTest(true, 4, 3, true, true, true);        
  }
  
  public void testNumericClass() {
    performTest(false, 4, 3, false, false, false);        
  }
  
  public void testNumericClassReorderedAtts() {    
    performTest(false, 4, 3, true, false, false);        
  }
  
  public void testNumericClassSwapNominalValues() {
    performTest(false, 4, 3, false, true, false);
  }
  
  public void testNumericClassSwapNominalValuesReorderAtts() {
    performTest(false, 4, 3, true, true, false);
  }
  
  protected InputMappedClassifier trainClassifier(Instances data, boolean nominalClass) {
    InputMappedClassifier toUse = new InputMappedClassifier();
    if (nominalClass) {
      toUse.setClassifier(new weka.classifiers.trees.J48());
    } else {
      toUse.setClassifier(new weka.classifiers.functions.LinearRegression());
    }
    toUse.setSuppressMappingReport(true);
    
    try {
      toUse.buildClassifier(data);
    } catch (Exception ex) {
      fail("Training InputMappedClassifier failed: " + ex);
      return null;
    }
    
    return toUse;
  }
  
  protected double[] testClassifier(Instances test, InputMappedClassifier classifier) {
    double[] result = new double[test.numInstances()];
    
    try {
    for (int i = 0; i < test.numInstances(); i++) {
      result[i] = classifier.classifyInstance(test.instance(i));
    }
    
    } catch (Exception ex) {
      fail("Testing InputMappedClassifier failed: " + ex);
      return null;
    }
    
    return result;
  }

  public static Test suite() {
    return new TestSuite(InputMappedClassifierTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
