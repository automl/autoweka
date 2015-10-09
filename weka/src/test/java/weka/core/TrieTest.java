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
 * TrieTest.java
 * Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests Trie. Run from the command line with:<p/>
 * java weka.core.TrieTest
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class TrieTest
  extends TestCase {
  
  /** holds the data for testing the trie */
  protected String[] m_Data;

  /** the default trie built from m_Data
   * @see #m_Data */
  protected Trie m_Trie;
  
  /**
   * Constructs the <code>TrieTest</code>.
   *
   * @param name the name of the test class
   */
  public TrieTest(String name) { 
    super(name); 
  }
  
  /**
   * Called by JUnit before each test method.
   *
   * @throws Exception if an error occurs
   */
  protected void setUp() throws Exception {
    super.setUp();

    m_Data = new String[]{
		"this is a test", 
		"this is another test",
		"and something else"};
    m_Trie = buildTrie(m_Data);
  }

  /**
   * Called by JUnit after each test method
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * builds a new trie from the given data and returns it
   * 
   * @param data	the data to use for initializing the Trie
   * @return		the built Trie
   */
  public Trie buildTrie(String data) {
    return buildTrie(new String[]{data});
  }

  /**
   * builds a new trie from the given data and returns it
   * 
   * @param data	the data to use for initializing the Trie
   * @return		the built Trie
   */
  public Trie buildTrie(String[] data) {
    Trie	result;
    int		i;
    
    result = new Trie();
    for (i = 0; i < data.length; i++)
      result.add(data[i]);
    
    return result;
  }
  
  /**
   * tests whether all the string the Trie got built with can be retrieved
   * again (tests building and retrieval via iterator).
   */
  public void testCorrectBuild() {
    // retrieve data
    Iterator<String> iter = m_Trie.iterator();
    HashSet<String> set = new HashSet<String>();
    while (iter.hasNext())
      set.add(iter.next());

    // correct size?
    assertEquals(
	"size() does not reflect number of added strings", 
	m_Data.length, m_Trie.size());
    
    // different size?
    assertEquals(
	"Iterator returns different number of strings", 
	m_Data.length, set.size());
    
    // different elements?
    for (int i = 0; i < m_Data.length; i++) {
      if (!set.contains(m_Data[i]))
	fail("Cannot find string '" + m_Data[i] + "'");
    }
  }
  
  /**
   * tests whether a different order of strings presented to the Trie will
   * result in a different Trie (= error).
   */
  public void testDifferentBuildOrder() {
    // build 2. trie
    String[] newData = new String[m_Data.length];
    for (int i = 0; i < m_Data.length; i++)
      newData[i] = m_Data[m_Data.length - i - 1];
    Trie t2 = buildTrie(m_Data);
    
    if (!m_Trie.equals(t2))
      fail("Tries differ");
  }
  
  /**
   * tests the cloning of a trie
   */
  public void testClone() {
    // clone trie
    Trie clone = (Trie) m_Trie.clone();
    
    if (!m_Trie.equals(clone))
      fail("Tries differ");
  }
  
  /**
   * tests the remove all method (only a few elements get removed)
   */
  public void testRemoveAllPartial() {
    Trie remove = buildTrie(m_Data[0]);
    Trie clone = (Trie) m_Trie.clone();
    m_Trie.removeAll(remove);
    assertEquals("Removing of 1 string", clone.size(), m_Trie.size() + 1);
  }
  
  /**
   * tests the remove all method (all elements get removed)
   */
  public void testRemoveAllFull() {
    Trie remove = buildTrie(m_Data);
    Trie clone = (Trie) m_Trie.clone();
    m_Trie.removeAll(remove);
    assertEquals("Removing all strings", clone.size(), m_Trie.size() + m_Data.length);
  }
  
  /**
   * tests the retain all method (retains a few elements)
   */
  public void testRetainAllPartial() {
    Trie retain = buildTrie(m_Data[0]);
    m_Trie.retainAll(retain);
    assertEquals("Retaining of 1 string", 1, m_Trie.size());
  }
  
  /**
   * tests the retain all method (retains all elements)
   */
  public void testRetainAllFull() {
    Trie retain = buildTrie(m_Data);
    Trie clone = (Trie) m_Trie.clone();
    m_Trie.retainAll(retain);
    assertEquals("Retaining all strings", clone.size(), m_Trie.size());
  }
  
  /**
   * tests whether the common prefix is determined correctly
   */
  public void testCommonPrefix() {
    String returned = m_Trie.getCommonPrefix();
    assertEquals("Common prefixes differ", 0, returned.length());

    String expected = "this is a";
    Trie t = buildTrie(new String[]{m_Data[0], m_Data[1]});
    returned = t.getCommonPrefix();
    assertEquals("Common prefixes differ", expected.length(), returned.length());
  }

  /**
   * tests the finding of prefixes
   */
  public void testFindPrefixes() {
    Vector<String> prefixes = m_Trie.getWithPrefix("this");
    assertEquals("Different number of prefixes returned", 2, prefixes.size());

    prefixes = m_Trie.getWithPrefix("blah");
    assertEquals("Different number of prefixes returned", 0, prefixes.size());
  }
  
  public static Test suite() {
    return new TestSuite(TrieTest.class);
  }

  /**
   * Runs the test.
   * 
   * @param args ignored
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
