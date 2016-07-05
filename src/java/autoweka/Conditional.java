package autoweka;

import java.awt.List;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** Simple struct that matains conditionality between params
 */
public class Conditional
{
    public Conditional(String line, HashMap<String, Parameter> paramMap)
    {
        Matcher m = conditionalPattern.matcher(line);
        if(!m.matches())
        {
            throw new RuntimeException("Failed to match parameter line '" + line + "'");
        }
        parameter = paramMap.get(m.group(1));
        parent = paramMap.get(m.group(2));
        if(parameter == null)
            throw new IllegalArgumentException("Can't find conditional target parameter with name '" + m.group(1) + "'");
        if(parent == null)
            throw new IllegalArgumentException("Can't find conditional parent parameter with name '" + m.group(2) + "'");
        String[] elements = m.group(3).split(",");
        for(int i = 0; i < elements.length; i++)
        {
            domain.add(elements[i].trim());
        }
    }

    public Conditional(Parameter _param, Parameter _parent, String _domain)
    {
        this(_param, _parent, Collections.singleton(_domain));
    }
    public Conditional(Parameter _param, Parameter _parent, Collection<String> _domain)
    {
        parameter = _param;
        parent = _parent;
        for(String s: _domain)
            domain.add(s);
    }

    public Conditional(Parameter _param, Parameter _parent, Conditional previous)
    {
        this(_param, _parent, previous.domain);
    }

    /**
     * The name of the 'child' parameter that is conditional on the parent taking on a particular value
     * (This class encodes the condition from the child param's perspective. @TODO rename to child rather than parameter, for clarity)
     */
    public Parameter parameter;
    /**
     * The name of the parameter that the child is conditional on
     */
    public Parameter parent;
    /**
     * The set of categorical values that the parent must take on in order for the child parameter to be active
     */
    public Collection<String> domain = new LinkedList<String>();;

    @Override
    public String toString()
    {
        return parameter.name + " | " + parent.name + " in {" + Util.joinStrings(", ", domain) + "}";
    }

    private static final Pattern conditionalPattern = Pattern.compile("\\s*([a-zA-Z0-9_-]*)\\s*\\|\\s*([a-zA-Z0-9_]*)\\s*in\\s*\\{(.*)\\}.*");
}
