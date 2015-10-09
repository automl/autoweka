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
 *    SpecialFunctions.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;


/**
 * Class implementing some mathematical functions.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public final class SpecialFunctions
  implements RevisionHandler {

  /** Some constants */
  private static double log2 = Math.log(2);

  /**
   * Returns natural logarithm of factorial using gamma function.
   *
   * @param x the value
   * @return natural logarithm of factorial
   */
  public static double lnFactorial(double x){

    return Statistics.lnGamma(x+1);
  }

  /**
   * Returns base 2 logarithm of binomial coefficient using gamma function.
   *
   * @param a upper part of binomial coefficient
   * @param b lower part
   * @return the base 2 logarithm of the binominal coefficient a over b
   */
  public static double log2Binomial(double a, double b) {
    
    if (Utils.gr(b,a)) {
      throw new ArithmeticException("Can't compute binomial coefficient.");
    }
    return (lnFactorial(a)-lnFactorial(b)-lnFactorial(a-b))/log2;
  }

  /**
   * Returns base 2 logarithm of multinomial using gamma function.
   *
   * @param a upper part of multinomial coefficient
   * @param bs lower part
   * @return multinomial coefficient of a over the bs
   */
  public static double log2Multinomial(double a, double[] bs)
       {
    
    double sum = 0;
    int i;
    
    for (i=0;i<bs.length;i++) {
      if (Utils.gr(bs[i],a)) {
	throw 
	  new ArithmeticException("Can't compute multinomial coefficient.");
      } else {
	sum = sum+lnFactorial(bs[i]);
      }
    }
    return (lnFactorial(a)-sum)/log2;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 8034 $");
  }

  /**
   * Main method for testing this class.
   */
  public static void main(String[] ops) {

    double[] doubles = {1, 2, 3};

    System.out.println("6!: " + Math.exp(SpecialFunctions.lnFactorial(6)));
    System.out.println("Binomial 6 over 2: " +
		       Math.pow(2, SpecialFunctions.log2Binomial(6, 2)));
    System.out.println("Multinomial 6 over 1, 2, 3: " +
		       Math.pow(2, SpecialFunctions.log2Multinomial(6, doubles)));
  }    
}
