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
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.instance;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests RemoveRange. Run from the command line with:<p>
 * java weka.filters.unsupervised.instance.RemoveRangeTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public class RemoveRangeTest extends AbstractFilterTest {
  
  public RemoveRangeTest(String name) { super(name);  }

  /** Creates a default RemoveRange */
  public Filter getFilter() {
    RemoveRange f = new RemoveRange();
    return f;
  }

  public void testSpecifiedRange() {
    
    ((RemoveRange)m_Filter).setInstancesIndices("1-10");
    ((RemoveRange)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(10,  result.numInstances());
    for (int i = 0; i < 10; i++) {
      assertEquals(m_Instances.instance(i).toString(), result.instance(i).toString());
    }
  }

  public static Test suite() {
    return new TestSuite(RemoveRangeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
