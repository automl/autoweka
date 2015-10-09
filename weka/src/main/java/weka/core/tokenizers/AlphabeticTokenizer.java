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
 * AlphabeticStringTokenizer.java
 * Copyright (C) 2003-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.core.tokenizers;

import java.util.NoSuchElementException;

import weka.core.RevisionUtils;

/**
 <!-- globalinfo-start -->
 * Alphabetic string tokenizer, tokens are to be formed only from contiguous alphabetic sequences.
 * <p/>
 <!-- globalinfo-end -->
 * 
 * @author  Asrhaf M. Kibriya (amk14@cs.waikato.ac.nz)
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class AlphabeticTokenizer
  extends Tokenizer {

  /** for serialization */
  private static final long serialVersionUID = 6705199562609861697L;

  /** the characters of the string */
  protected char[] m_Str;
  
  /** the current position */
  protected int m_CurrentPos;
  
  /**
   * Returns a string describing the stemmer
   * 
   * @return 		a description suitable for displaying in the 
   * 			explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "Alphabetic string tokenizer, tokens are to be formed only from "
      + "contiguous alphabetic sequences.";
  }
  
  /**
   * returns whether there are more elements still
   * 
   * @return true 	if there are still more elements
   */
  public boolean hasMoreElements() {
    int beginpos = m_CurrentPos;

    while ( (beginpos < m_Str.length) && 
	((m_Str[beginpos] < 'a') || (m_Str[beginpos] > 'z')) &&
	((m_Str[beginpos] < 'A') || (m_Str[beginpos] > 'Z')) ) {
      beginpos++;    
    }
    m_CurrentPos = beginpos;

    if ( (beginpos < m_Str.length) && 
	(((m_Str[beginpos] >= 'a') && (m_Str[beginpos] <= 'z')) ||
	 ((m_Str[beginpos] >= 'A') && (m_Str[beginpos] <= 'Z'))) ) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * returns the next element
   * 
   * @return 		the next element
   */
  public Object nextElement() {
    int beginpos, endpos;
    
    beginpos = m_CurrentPos;

    while ( (beginpos < m_Str.length) && 
	((m_Str[beginpos] < 'a') && (m_Str[beginpos] > 'z')) &&
	((m_Str[beginpos] < 'A') && (m_Str[beginpos] > 'Z')) ) {
      beginpos++;    
    }
    m_CurrentPos = endpos = beginpos;

    if (beginpos >= m_Str.length)
      throw new NoSuchElementException("No more tokens present");

    while ((endpos < m_Str.length) &&
	( ((m_Str[endpos] >= 'a') && (m_Str[endpos]<='z')) ||
	  ((m_Str[endpos] >= 'A') && (m_Str[endpos]<='Z'))) ) {
      endpos++;
    }

    String s = new String(m_Str, beginpos, endpos - m_CurrentPos);
    m_CurrentPos = endpos;

    return s;
  }      

  /**
   * Sets the string to tokenize. Tokenization happens immediately.
   * 
   * @param s		the string to tokenize
   */
  public void tokenize(String s) {
    m_CurrentPos = 0;
    m_Str = new char[s.length()];
    s.getChars(0, s.length(), m_Str, 0);
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
   * Runs the tokenizer with the given options and strings to tokenize.
   * The tokens are printed to stdout.
   * 
   * @param args	the commandline options and strings to tokenize
   */
  public static void main(String[] args) {
    runTokenizer(new AlphabeticTokenizer(), args);
  }
}
