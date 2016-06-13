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

package weka.classifiers.functions.supportVector;

import java.io.InputStreamReader;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.core.matrix.Matrix;

/**
 * Tests PrecomputedKernelMatrixKernel. Run from the command line with:
 * <p/>
 * java
 * weka.classifiers.functions.supportVector.PrecomputedKernelMatrixKernelTest
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com
 * @version $Revision: 10160 $
 */
public class PrecomputedKernelMatrixKernelTest extends AbstractKernelTest {

  public PrecomputedKernelMatrixKernelTest(String name) {
    super(name);
  }

  /** Creates a default PrecomputedKernelMatrixKernell */
  @Override
  public Kernel getKernel() {
    PrecomputedKernelMatrixKernel pc = new PrecomputedKernelMatrixKernel();

    // load kernel matrix
    try {
      pc.setKernelMatrix(new Matrix(new InputStreamReader(ClassLoader
        .getSystemResourceAsStream("weka/classifiers/data/test.matrix"))));
    } catch (Exception e) {
      e.printStackTrace();
    }

    return pc;
  }

  public static Test suite() {
    return new TestSuite(PrecomputedKernelMatrixKernelTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
