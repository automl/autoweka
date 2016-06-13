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
 * DelimitedTokenizer.java
 * Copyright (C) 2007-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.core.tokenizers;

import java.util.Enumeration;
import java.util.Vector;

import weka.core.Option;
import weka.core.Utils;

/**
 * Abstract superclass for tokenizers that take characters as delimiters.
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10203 $
 */
public abstract class CharacterDelimitedTokenizer extends Tokenizer {

  /** Added to prevent warning */
  private static final long serialVersionUID = -3091468793633408477L;

  /** Delimiters used in tokenization */
  protected String m_Delimiters = " \r\n\t.,;:'\"()?!";

  /**
   * Returns an enumeration of all the available options..
   * 
   * @return an enumeration of all available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tThe delimiters to use\n"
      + "\t(default ' \\r\\n\\t.,;:'\"()?!').", "delimiters", 1,
      "-delimiters <value>"));

    return result.elements();
  }

  /**
   * Gets the current option settings for the OptionHandler.
   * 
   * @return the list of current option settings as an array of strings
   */
  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    result.add("-delimiters");
    result.add(getDelimiters());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Sets the OptionHandler's options using the given list. All options will be
   * set (or reset) during this call (i.e. incremental setting of options is not
   * possible).
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String tmpStr = Utils.getOption("delimiters", options);
    if (tmpStr.length() != 0) {
      setDelimiters(tmpStr);
    } else {
      setDelimiters(" \r\n\t.,;:'\"()?!");
    }
  }

  /**
   * Get the value of delimiters (not backquoted).
   * 
   * @return Value of delimiters.
   */
  public String getDelimiters() {
    return m_Delimiters;
  }

  /**
   * Set the value of delimiters. For convenienve, the strings "\r", "\n", "\t",
   * "\'", "\\" get automatically translated into their character
   * representations '\r', '\n', '\t', '\'', '\\'. This means, one can either
   * use <code>setDelimiters("\r\n\t\\");</code> or
   * <code>setDelimiters("\\r\\n\\t\\\\");</code>.
   * 
   * @param value Value to assign to delimiters.
   * @see Utils#unbackQuoteChars(String)
   */
  public void setDelimiters(String value) {
    m_Delimiters = Utils.unbackQuoteChars(value);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String delimitersTipText() {
    return "Set of delimiter characters to use in tokenizing (\\r, \\n and \\t can be used for carriage-return, line-feed and tab)";
  }
}
