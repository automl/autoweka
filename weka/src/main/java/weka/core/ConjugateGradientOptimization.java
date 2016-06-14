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
 *    ConjugateGradientOptimization.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.util.Arrays;

import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

/**
 * This subclass of Optimization.java implements conjugate gradient descent
 * rather than BFGS updates, by overriding findArgmin(), with the same tests for
 * convergence, and applies the same line search code. Note that constraints are
 * NOT actually supported. Using this class instead of Optimization.java can
 * reduce runtime when there are many parameters.
 * 
 * Uses the second hybrid method proposed in "An Efficient Hybrid Conjugate
 * Gradient Method for Unconstrained Optimization" by Dai and Yuan (2001). See
 * also information in the getTechnicalInformation() method.
 * 
 * @author Eibe Frank
 * @version $Revision: 10203 $
 */
public abstract class ConjugateGradientOptimization extends Optimization
  implements RevisionHandler {

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;
    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Y.H. Dai and Y. Yuan");
    result.setValue(Field.YEAR, "2001");
    result
      .setValue(Field.TITLE,
        "An Efficient Hybrid Conjugate Gradient Method for Unconstrained Optimization");
    result.setValue(Field.JOURNAL, "Annals of Operations Research");
    result.setValue(Field.VOLUME, "103");
    result.setValue(Field.PAGES, "33-47");

    result.add(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "W.W. Hager and H. Zhang");
    result.setValue(Field.YEAR, "2006");
    result.setValue(Field.TITLE,
      "A survey of nonlinear conjugate gradient methods");
    result.setValue(Field.JOURNAL, "Pacific Journal of Optimization");
    result.setValue(Field.VOLUME, "2");
    result.setValue(Field.PAGES, "35-58");

    return result;
  }

  /**
   * Constructor that sets MAXITS to 2000 by default and the parameter in the
   * second weak Wolfe condition to 0.1.
   */
  public ConjugateGradientOptimization() {
    setMaxIteration(2000);
    m_BETA = 0.1; // To make line search more exact, recommended for non-linear
                  // CGD
  }

  /**
   * Main algorithm. NOTE: constraints are not actually supported.
   * 
   * @param initX initial point of x, assuming no value's on the bound!
   * @param constraints both arrays must contain Double.NaN
   * @return the solution of x, null if number of iterations not enough
   * @throws Exception if an error occurs
   */
  @Override
  public double[] findArgmin(double[] initX, double[][] constraints)
    throws Exception {

    int l = initX.length;

    // Initial value of obj. function, gradient and inverse of the Hessian
    m_f = objectiveFunction(initX);
    if (Double.isNaN(m_f)) {
      throw new Exception("Objective function value is NaN!");
    }

    // Get gradient at initial point
    double[] grad = evaluateGradient(initX), oldGrad, oldX, deltaX = new double[l], direct = new double[l], x = new double[l];

    // Turn gradient into direction and calculate squared length
    double sum = 0;
    for (int i = 0; i < grad.length; i++) {
      direct[i] = -grad[i];
      sum += grad[i] * grad[i];
    }

    // Same as in Optimization.java
    double stpmax = m_STPMX * Math.max(Math.sqrt(sum), l);

    boolean[] isFixed = new boolean[initX.length];
    DynamicIntArray wsBdsIndx = new DynamicIntArray(initX.length);
    double[][] consts = new double[2][initX.length];
    for (int i = 0; i < initX.length; i++) {
      if (!Double.isNaN(constraints[0][i])
        || (!Double.isNaN(constraints[1][i]))) {
        throw new Exception("Cannot deal with constraints, sorry.");
      }
      consts[0][i] = constraints[0][i];
      consts[1][i] = constraints[1][i];
      x[i] = initX[i];
    }

    boolean finished = false;
    for (int step = 0; step < m_MAXITS; step++) {

      if (m_Debug) {
        System.err.println("\nIteration # " + step + ":");
      }

      oldX = x;
      oldGrad = grad;

      // Make a copy of direction vector because it may get modified in lnsrch
      double[] directB = Arrays.copyOf(direct, direct.length);

      // Perform a line search based on new direction
      m_IsZeroStep = false;
      x = lnsrch(x, grad, directB, stpmax, isFixed, constraints, wsBdsIndx);
      if (m_IsZeroStep) {
        throw new Exception("Exiting due to zero step.");
      }

      double test = 0.0;
      for (int h = 0; h < x.length; h++) {
        deltaX[h] = x[h] - oldX[h];
        double tmp = Math.abs(deltaX[h]) / Math.max(Math.abs(x[h]), 1.0);
        if (tmp > test) {
          test = tmp;
        }
      }
      if (test < m_Zero) {
        if (m_Debug) {
          System.err.println("\nDeltaX converged: " + test);
        }
        finished = true;
        break;
      }

      // Check zero gradient
      grad = evaluateGradient(x);
      test = 0.0;
      for (int g = 0; g < l; g++) {
        double tmp = Math.abs(grad[g]) * Math.max(Math.abs(directB[g]), 1.0)
          / Math.max(Math.abs(m_f), 1.0);
        if (tmp > test) {
          test = tmp;
        }
      }

      if (test < m_Zero) {
        if (m_Debug) {
          for (int i = 0; i < l; i++) {
            System.out.println(grad[i] + " " + directB[i] + " " + m_f);
          }
          System.err.println("Gradient converged: " + test);
        }
        finished = true;
        break;
      }

      // Calculate multiplier
      double betaHSNumerator = 0, betaDYNumerator = 0;
      double betaHSandDYDenominator = 0;
      for (int i = 0; i < grad.length; i++) {
        betaDYNumerator += grad[i] * grad[i];
        betaHSNumerator += (grad[i] - oldGrad[i]) * grad[i];
        betaHSandDYDenominator += (grad[i] - oldGrad[i]) * direct[i];
      }
      double betaHS = betaHSNumerator / betaHSandDYDenominator;
      double betaDY = betaDYNumerator / betaHSandDYDenominator;

      if (m_Debug) {
        System.err.println("Beta HS: " + betaHS);
        System.err.println("Beta DY: " + betaDY);
      }

      for (int i = 0; i < direct.length; i++) {
        direct[i] = -grad[i] + Math.max(0, Math.min(betaHS, betaDY))
          * direct[i];
      }
    }

    if (finished) {
      if (m_Debug) {
        System.err.println("Minimum found.");
      }
      m_f = objectiveFunction(x);
      if (Double.isNaN(m_f)) {
        throw new Exception("Objective function value is NaN!");
      }
      return x;
    }

    if (m_Debug) {
      System.err.println("Cannot find minimum -- too many iterations!");
    }
    m_X = x;
    return null;
  }
}
