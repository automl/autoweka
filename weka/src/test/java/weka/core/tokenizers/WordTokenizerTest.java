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
 * Tests WordTokenizer. Run from the command line with:<p>
 * java weka.core.tokenizers.WordTokenizerTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class WordTokenizerTest
  extends AbstractTokenizerTest {

  public WordTokenizerTest(String name) {
    super(name);
  }

  /** Creates a default WordTokenizer */
  public Tokenizer getTokenizer() {
    return new WordTokenizer();
  }

  /**
   * tests the number of generated tokens
   */
  public void testNumberOfGeneratedTokens() {
    String 	s;
    String[]	result;
    
    s = "HOWEVER, the egg only got larger and larger, and more and more human";
    try {
      result = Tokenizer.tokenize(m_Tokenizer, new String[]{s});
      assertEquals("number of tokens differ", 13, result.length);
    }
    catch (Exception e) {
      fail("Error tokenizing string '" + s + "'!");
    }
  }
  
  public static Test suite() {
    return new TestSuite(WordTokenizerTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
