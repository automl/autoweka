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
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.clusterers;

import weka.clusterers.AbstractClustererTest;
import weka.clusterers.Clusterer;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests FilteredClusterer. Run from the command line with:<p/>
 * java weka.clusterers.FilteredClustererTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class FilteredClustererTest 
  extends AbstractClustererTest {

  public FilteredClustererTest(String name) { 
    super(name);  
  }

  /** Creates a default FilteredClusterer */
  public Clusterer getClusterer() {
    return new FilteredClusterer();
  }

  public static Test suite() {
    return new TestSuite(FilteredClustererTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
