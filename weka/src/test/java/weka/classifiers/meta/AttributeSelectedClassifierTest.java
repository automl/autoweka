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
 * Copyright 2002-2006 University of Waikato
 */

package weka.classifiers.meta;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.Classifier;
import weka.core.CheckOptionHandler;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests AttributeSelectedClassifier. Run from the command line with:<p>
 * java weka.classifiers.meta.AttributeSelectedClassifierTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class AttributeSelectedClassifierTest
  extends AbstractClassifierTest {

  public AttributeSelectedClassifierTest(String name) { 
    super(name);
  }

  /** Creates a default AttributeSelectedClassifier */
  public Classifier getClassifier() {
    return new AttributeSelectedClassifier();
  }
  
  /**
   * Configures the CheckOptionHandler uses for testing the optionhandling.
   * Sets the classifier return from the getClassifier() method.
   * 
   * @return	the fully configured CheckOptionHandler
   * @see	#getClassifier()
   */
  protected CheckOptionHandler getOptionTester() {
    CheckOptionHandler		result;
    
    result = super.getOptionTester();
    result.setUserOptions(new String[]{
	"-E",
	"weka.attributeSelection.CfsSubsetEval",
	"-S",
	"weka.attributeSelection.BestFirst"});
    
    return result;
  }

  public static Test suite() {
    return new TestSuite(AttributeSelectedClassifierTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
