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
 *    Rule.java
 *    Copyright (C) 2001-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.rules;

import java.io.Serializable;

import weka.core.Copyable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.WeightedInstancesHandler;

/**
 * Abstract class of generic rule
 *
 * @author Xin Xu (xx5@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public abstract class Rule 
    implements WeightedInstancesHandler, Copyable, Serializable, RevisionHandler {

    /** for serialization */
    private static final long serialVersionUID = 8815687740470471229L;
    
    /**
     * Get a shallow copy of this rule
     *
     * @return the copy
     */
    public Object copy(){ return this;}
    
    /**
     * Whether the instance covered by this rule
     * 
     * @param datum the instance in question
     * @return the boolean value indicating whether the instance 
     *         is covered by this rule
     */
    public abstract boolean covers(Instance datum);

    /**
     * Build this rule
     *
     * @param data the data used to build the rule
     * @exception Exception if rule cannot be built
     */    
    public abstract void grow(Instances data) throws Exception;    

    /**
     * Whether this rule has antecedents, i.e. whether it is a default rule
     * 
     * @return the boolean value indicating whether the rule has antecedents
     */
    public abstract boolean hasAntds();   

    /** 
     * Get the consequent of this rule, i.e. the predicted class 
     * 
     * @return the consequent
     */
    public abstract double getConsequent(); 

    /** 
     * The size of the rule.  Could be number of antecedents in the case
     * of conjunctive rule
     *
     * @return the size of the rule
     */
    public abstract double size(); 
}
