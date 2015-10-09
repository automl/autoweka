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
 * Copyright (C) 2007 University of Waikato 
 */

package weka.core.tokenizers;

import weka.core.CheckGOE;
import weka.core.CheckOptionHandler;
import weka.core.FastVector;
import weka.core.OptionHandler;
import weka.core.SerializationHelper;
import weka.core.CheckScheme.PostProcessor;
import weka.test.Regression;

import junit.framework.TestCase;

/**
 * Abstract Test class for Tokenizers.
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 *
 * @see PostProcessor
 */
public abstract class AbstractTokenizerTest 
  extends TestCase {
  
  /** data for the regression tests */
  protected String[] m_Data;
  
  /** The tokenizer to be tested */
  protected Tokenizer m_Tokenizer;
  
  /** the results of the regression tests */
  protected FastVector[] m_RegressionResults;
  
  /** the OptionHandler tester */
  protected CheckOptionHandler m_OptionTester;
  
  /** for testing GOE stuff */
  protected CheckGOE m_GOETester;
  
  /**
   * Constructs the <code>AbstractTokenizerTest</code>. Called by subclasses.
   *
   * @param name the name of the test class
   */
  public AbstractTokenizerTest(String name) { 
    super(name); 
  }

  /**
   * returns the data to use in the tests
   * 
   * @return		the data to use in the tests
   */
  protected String[] getData() {
    return new String[]{
      "Humpty Dumpty was sitting, with his legs crossed like a Turk, on the top of a high wall -- such a narrow one that Alice quite wondered how he could keep his balance -- and, as his eyes were steadily fixed in the opposite direction, and he didn't take the least notice of her, she thought he must be a stuffed figure, after all.",
      "The planet Mars, I scarcely need remind the reader, revolves about the sun at a mean distance of 140,000,000 miles, and the light and heat it receives from the sun is barely half of that received by this world.",
      "I've studied now Philosophy And Jurisprudence, Medicine, And even, alas! Theology All through and through with ardour keen! Here now I stand, poor fool, and see I'm just as wise as formerly."
    };
  }
  
  /**
   * Configures the CheckOptionHandler uses for testing the option handling.
   * Sets the tokenizer returned from the getTokenizer() method if that can
   * handle options.
   * 
   * @return	the fully configured CheckOptionHandler
   * @see	#getTokenizer()
   */
  protected CheckOptionHandler getOptionTester() {
    CheckOptionHandler		result;
    
    result = new CheckOptionHandler();
    if (getTokenizer() instanceof OptionHandler)
      result.setOptionHandler((OptionHandler) getTokenizer());
    else
      result.setOptionHandler(null);
    result.setUserOptions(new String[0]);
    result.setSilent(true);
    
    return result;
  }
  
  /**
   * Configures the CheckGOE used for testing GOE stuff.
   * Sets the Tokenizer returned from the getTokenizer() method.
   * 
   * @return	the fully configured CheckGOE
   * @see	#getTokenizer()
   */
  protected CheckGOE getGOETester() {
    CheckGOE		result;
    
    result = new CheckGOE();
    result.setObject(getTokenizer());
    result.setSilent(true);
    
    return result;
  }
  
  /**
   * Called by JUnit before each test method. This implementation creates
   * the default tokenizer to test and loads a test set of Instances.
   *
   * @exception Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    m_Tokenizer         = getTokenizer();
    m_OptionTester      = getOptionTester();
    m_GOETester         = getGOETester();
    m_Data              = getData();
    m_RegressionResults = new FastVector[m_Data.length];
  }

  /** Called by JUnit after each test method */
  protected void tearDown() {
    m_Tokenizer         = null;
    m_OptionTester      = null;
    m_GOETester         = null;
    m_Data              = null;
    m_RegressionResults = null;
  }

  /**
   * Used to create an instance of a specific tokenizer.
   *
   * @return a suitably configured <code>Tokenizer</code> value
   */
  public abstract Tokenizer getTokenizer();

  /**
   * tests whether the scheme declares a serialVersionUID.
   */
  public void testSerialVersionUID() {
    boolean     result;

    result = !SerializationHelper.needsUID(m_Tokenizer.getClass());

    if (!result)
      fail("Doesn't declare serialVersionUID!");
  }

  /**
   * tests whether the tokenizer correctly initializes in the
   * buildTokenizer method
   */
  public void testBuildInitialization() {
    boolean		result;
    int			i;
    int			n;
    String[][][]	processed;
    String		msg;
    
    // process data twice
    processed = new String[2][m_Data.length][];
    for (n = 0; n < 2; n++) {
      for (i = 0; i < m_Data.length; i++) {
	try {
	  processed[n][i] = Tokenizer.tokenize(m_Tokenizer, new String[]{m_Data[i]});
	}
	catch (Exception e) {
	  processed[n][i] = new String[0];
	}
      }
    }
    
    // was the same data produced?
    result = true;
    msg    = "";
    for (i = 0; i < m_Data.length; i++) {
      if (processed[0].length == processed[1].length) {
	for (n = 0; n < processed[0][i].length; n++) {
	  if (!processed[0][i][n].equals(processed[1][i][n])) {
	    result = false;
	    msg    = "different substrings";
	    break;
	  }
	}
      }
      else {
	result = false;
	msg    = "different number of substrings";
	break;
      }
    }

    if (!result)
      fail("Incorrect build initialization (" + msg + ")!");
  }

  /**
   * Runs the tokenizer over the given string and returns the generated 
   * tokens.
   *
   * @param s		the string to tokenize
   * @return 		a <code>FastVector</code> containing the tokens.
   * @throws Exception	if tokenization fails
   */
  protected FastVector useTokenizer(String s) throws Exception {
    String[]	tokens;
    FastVector	result;
    int		i;
    
    tokens = Tokenizer.tokenize(m_Tokenizer, new String[]{s});
    
    result = new FastVector();
    for (i = 0; i < tokens.length; i++)
      result.addElement(tokens[i]);
    
    return result;
      
  }

  /**
   * Returns a string containing all the tokens.
   *
   * @param tokens 	a <code>FastVector</code> containing the tokens
   * @return 		a <code>String</code> representing the vector of tokens.
   */
  protected String predictionsToString(FastVector tokens) {
    StringBuffer sb = new StringBuffer();
    
    sb.append(tokens.size()).append(" tokens\n");
    for (int i = 0; i < tokens.size(); i++)
      sb.append(tokens.elementAt(i)).append('\n');
    
    return sb.toString();
  }

  /**
   * Runs a regression test -- this checks that the output of the tested
   * object matches that in a reference version. When this test is
   * run without any pre-existing reference output, the reference version
   * is created.
   */
  public void testRegression() {
    int		i;
    boolean	succeeded;
    Regression	reg;
    
    reg       = new Regression(this.getClass());
    succeeded = false;
    
    for (i = 0; i < m_Data.length; i++) {
      try {
        m_RegressionResults[i] = useTokenizer(m_Data[i]);
        succeeded = true;
        reg.println(predictionsToString(m_RegressionResults[i]));
      }
      catch (Exception e) {
	m_RegressionResults[i] = null;
      }
    }
    
    if (!succeeded) {
      fail("Problem during regression testing: no successful tokens generated for any string");
    }

    try {
      String diff = reg.diff();
      if (diff == null) {
        System.err.println("Warning: No reference available, creating."); 
      } else if (!diff.equals("")) {
        fail("Regression test failed. Difference:\n" + diff);
      }
    } 
    catch (java.io.IOException ex) {
      fail("Problem during regression testing.\n" + ex);
    }
  }
  
  /**
   * tests the listing of the options
   */
  public void testListOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkListOptions())
	fail("Options cannot be listed via listOptions.");
    }
  }
  
  /**
   * tests the setting of the options
   */
  public void testSetOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkSetOptions())
	fail("setOptions method failed.");
    }
  }
  
  /**
   * tests whether the default settings are processed correctly
   */
  public void testDefaultOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkDefaultOptions())
	fail("Default options were not processed correctly.");
    }
  }
  
  /**
   * tests whether there are any remaining options
   */
  public void testRemainingOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkRemainingOptions())
	fail("There were 'left-over' options.");
    }
  }
  
  /**
   * tests the whether the user-supplied options stay the same after setting.
   * getting, and re-setting again.
   * 
   * @see 	#getOptionTester()
   */
  public void testCanonicalUserOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkCanonicalUserOptions())
	fail("setOptions method failed");
    }
  }
  
  /**
   * tests the resetting of the options to the default ones
   */
  public void testResettingOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkSetOptions())
	fail("Resetting of options failed");
    }
  }
  
  /**
   * tests for a globalInfo method
   */
  public void testGlobalInfo() {
    if (!m_GOETester.checkGlobalInfo())
      fail("No globalInfo method");
  }
  
  /**
   * tests the tool tips
   */
  public void testToolTips() {
    if (!m_GOETester.checkToolTips())
      fail("Tool tips inconsistent");
  }
}
