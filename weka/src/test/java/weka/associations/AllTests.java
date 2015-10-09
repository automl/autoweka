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
 * Copyright (C) 2006 University of Waikato 
 */

package weka.associations;

import weka.test.WekaTestSuite;

import junit.framework.Test;

/**
 * Test class for all associators. Run from the command line with: <p/>
 * java weka.associations.AllTests
 *
 * @author FracPete (frapcete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class AllTests 
  extends WekaTestSuite {

  public static Test suite() {
    return suite("weka.associations.Associator");
  }

  public static void main(String []args) {
    junit.textui.TestRunner.run(suite());
  }
}
