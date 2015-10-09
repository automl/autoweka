package autoweka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ParameterConditionalGroup
{
    private Map<String, Parameter> mParams = new HashMap<String, Parameter>();
    private Map<String, List<Conditional>> mConditionals = new HashMap<String, List<Conditional>>();


    public void add(Parameter param)
    {
        mParams.put(param.name, param);
    }

    public Parameter getParameter(String name)
    {
        return mParams.get(name);
    }

    public Collection<Parameter> getParameters()
    {
        return mParams.values();
    }

    public void add(Conditional conditional)
    {
        if(!mConditionals.containsKey(conditional.parameter.name)){
            mConditionals.put(conditional.parameter.name, new ArrayList<Conditional>());
        }
        mConditionals.get(conditional.parameter.name).add(conditional);
    }

    public List<Conditional> getConditionalsForParameter(Parameter param)
    {
        return getConditionalsForParameter(param.name);
    }

    public List<Conditional> getConditionalsForParameter(String name)
    {
        if(mConditionals.containsKey(name))
            return mConditionals.get(name);
        return Collections.<Conditional>emptyList();
    }
}
