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
 * Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 */

package weka.core.tokenizers;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests NGramTokenizer. Run from the command line with:<p>
 * java weka.core.tokenizers.NGramTokenizerTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class NGramTokenizerTest
  extends AbstractTokenizerTest {

  public NGramTokenizerTest(String name) {
    super(name);
  }

  /** Creates a default NGramTokenizer */
  public Tokenizer getTokenizer() {
    return new NGramTokenizer();
  }

  /**
   * tests the number of generated tokens
   */
  public void testNumberOfGeneratedTokens() {
    String 	s;
    String[]	result;
    
    s = "HOWEVER, the egg only got larger and larger, and more and more human";

    // only 1-grams
    try {
      result = Tokenizer.tokenize(m_Tokenizer, new String[]{"-min", "1", "-max", "1", s});
      assertEquals("number of tokens differ (1)", 13, result.length);
    }
    catch (Exception e) {
      fail("Error tokenizing string '" + s + "'!");
    }

    // only 2-grams
    try {
      result = Tokenizer.tokenize(m_Tokenizer, new String[]{"-min", "2", "-max", "2", s});
      assertEquals("number of tokens differ (2)", 12, result.length);
    }
    catch (Exception e) {
      fail("Error tokenizing string '" + s + "'!");
    }

    // 1 to 3-grams
    try {
      result = Tokenizer.tokenize(m_Tokenizer, new String[]{"-min", "1", "-max", "3", s});
      assertEquals("number of tokens differ (3)", 36, result.length);
    }
    catch (Exception e) {
      fail("Error tokenizing string '" + s + "'!");
    }
  }

  public static Test suite() {
    return new TestSuite(NGramTokenizerTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
