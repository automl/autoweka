/*
 * This software is a cooperative product of The MathWorks and the National
 * Institute of Standards and Technology (NIST) which has been released to the
 * public domain. Neither The MathWorks nor NIST assumes any responsibility
 * whatsoever for its use by other parties, and makes no guarantees, expressed
 * or implied, about its quality, reliability, or any other characteristic.
 */

/*
 * LinearRegression.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.matrix;

import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;

/**
 * Class for performing (ridged) linear regression using Tikhonov
 * regularization.
 * 
 * @author Fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 9768 $
 */
 
public class LinearRegression
  implements RevisionHandler {

  /** the coefficients */
  protected double[] m_Coefficients = null;

  /**
   * Performs a (ridged) linear regression.
   *
   * @param a the matrix to perform the regression on
   * @param y the dependent variable vector
   * @param ridge the ridge parameter
   * @throws IllegalArgumentException if not successful
   */
  public LinearRegression(Matrix a, Matrix y, double ridge) {
    calculate(a, y, ridge);
  }

  /**
   * Performs a weighted (ridged) linear regression. 
   *
   * @param a the matrix to perform the regression on
   * @param y the dependent variable vector
   * @param w the array of data point weights
   * @param ridge the ridge parameter
   * @throws IllegalArgumentException if the wrong number of weights were
   * provided.
   */
  public LinearRegression(Matrix a, Matrix y, double[] w, double ridge) {

    if (w.length != a.getRowDimension())
      throw new IllegalArgumentException("Incorrect number of weights provided");
    Matrix weightedThis = new Matrix(
                              a.getRowDimension(), a.getColumnDimension());
    Matrix weightedDep = new Matrix(a.getRowDimension(), 1);
    for (int i = 0; i < w.length; i++) {
      double sqrt_weight = Math.sqrt(w[i]);
      for (int j = 0; j < a.getColumnDimension(); j++)
        weightedThis.set(i, j, a.get(i, j) * sqrt_weight);
      weightedDep.set(i, 0, y.get(i, 0) * sqrt_weight);
    }

    calculate(weightedThis, weightedDep, ridge);
  }

  /**
   * performs the actual regression.
   *
   * @param a the matrix to perform the regression on
   * @param y the dependent variable vector
   * @param ridge the ridge parameter
   * @throws IllegalArgumentException if not successful
   */
  protected void calculate(Matrix a, Matrix y, double ridge) {

    if (y.getColumnDimension() > 1)
      throw new IllegalArgumentException("Only one dependent variable allowed");

    int nc = a.getColumnDimension();
    m_Coefficients = new double[nc];
    Matrix solution;

    Matrix ss = aTa(a);
    Matrix bb = aTy(a, y);

    boolean success = true;

    do {
      // Set ridge regression adjustment
      Matrix ssWithRidge = ss.copy();
      for (int i = 0; i < nc; i++)
        ssWithRidge.set(i, i, ssWithRidge.get(i, i) + ridge);

      // Carry out the regression
      try {
        solution = ssWithRidge.solve(bb);
        for (int i = 0; i < nc; i++)
          m_Coefficients[i] = solution.get(i, 0);
        success = true;
      } catch (Exception ex) {
        ridge *= 10;
        success = false;
      }
    } while (!success);
  }
  
  /**
   * Return aTa (a' * a)
   */
  private static Matrix aTa(Matrix a) {
    int cols = a.getColumnDimension();
    double[][] A = a.getArray();
    Matrix x = new Matrix(cols, cols);
    double[][] X = x.getArray();
    double[] Acol = new double[a.getRowDimension()];
    for (int col1 = 0; col1 < cols; col1++) {
      // cache the column for faster access later
      for (int row = 0; row < Acol.length; row++) {
        Acol[row] = A[row][col1];
      }
      // reference the row for faster lookup
      double[] Xrow = X[col1];
      for (int row = 0; row < Acol.length; row++) {
        double[] Arow = A[row];
        for (int col2 = col1; col2 < Xrow.length; col2++) {
          Xrow[col2] += Acol[row] * Arow[col2];
        }
      }
      // result is symmetric
      for (int col2 = col1 + 1; col2 < Xrow.length; col2++) {
        X[col2][col1] = Xrow[col2];
      }
    }
    return x;
  }

  /**
   * Return aTy (a' * y)
   */
  private static Matrix aTy(Matrix a, Matrix y) {
    double[][] A = a.getArray();
    double[][] Y = y.getArray();
    Matrix x = new Matrix(a.getColumnDimension(), 1);
    double[][] X = x.getArray();
    for (int row = 0; row < A.length; row++) {
      // reference the rows for faster lookup
      double[] Arow = A[row];
      double[] Yrow = Y[row];
      for (int col = 0; col < Arow.length; col++) {
        X[col][0] += Arow[col] * Yrow[0];
      }
    }
    return x;
  }

  /**
   * returns the calculated coefficients
   *
   * @return the coefficients
   */
  public final double[] getCoefficients() {
    return m_Coefficients;
  }

  /**
   * returns the coefficients in a string representation
   */
  public String toString() {
    return Utils.arrayToString(getCoefficients());
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 9768 $");
  }
}
