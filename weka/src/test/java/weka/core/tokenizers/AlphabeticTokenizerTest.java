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
 * Tests AlphabeticTokenizer. Run from the command line with:<p>
 * java weka.core.tokenizers.AlphabeticTokenizerTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class AlphabeticTokenizerTest
  extends AbstractTokenizerTest {

  public AlphabeticTokenizerTest(String name) {
    super(name);
  }

  /** Creates a default AlphabeticTokenizer */
  public Tokenizer getTokenizer() {
    return new AlphabeticTokenizer();
  }

  /**
   * tests the number of generated tokens
   */
  public void testNumberOfGeneratedTokens() {
    String 	s;
    String[]	result;
    
    // no numbers included
    s = "HOWEVER, the egg only got larger and larger, and more and more human";
    try {
      result = Tokenizer.tokenize(m_Tokenizer, new String[]{s});
      assertEquals("number of tokens differ (1)", 13, result.length);
    }
    catch (Exception e) {
      fail("Error tokenizing string '" + s + "'!");
    }
    
    // numbers included
    s = "The planet Mars, I scarcely need remind the reader, revolves about the sun at a mean distance of 140,000,000 miles";
    try {
      result = Tokenizer.tokenize(m_Tokenizer, new String[]{s});
      assertEquals("number of tokens differ (2)", 19, result.length);
    }
    catch (Exception e) {
      fail("Error tokenizing string '" + s + "'!");
    }
  }

  public static Test suite() {
    return new TestSuite(AlphabeticTokenizerTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
