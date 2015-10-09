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
 *    AssociationRulesEvent.java
 *    Copyright (C) 2010-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.util.EventObject;

import weka.associations.AssociationRules;

/**
 * Class encapsulating a set of association rules.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 8034 $
 */
public class BatchAssociationRulesEvent extends EventObject {
  
  /** For serialization */
  private static final long serialVersionUID = 6332614648885439492L;
  
  /** The encapsulated rules */
  protected AssociationRules m_rules;
  
  /**
   * Creates a new <code>BatchAssociationRulesEvent</code> instance.
   * 
   * @param source the source object.
   * @param rules the association rules.
   */
  public BatchAssociationRulesEvent(Object source, AssociationRules rules) {
    super(source);
    
    m_rules = rules;
  }
  
  /**
   * Get the encapsulated association rules.
   * 
   * @return the encapsulated association rules.
   */
  public AssociationRules getRules() {
    return m_rules;
  }
}
