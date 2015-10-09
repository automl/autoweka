package autoweka.instancegenerators;

import autoweka.InstanceGenerator;
import weka.core.Instances;
import java.util.List;
import java.util.Collections;

/** 
 * The most lame of InstanceGenerators, does nothing
 */ 
public class Default extends InstanceGenerator
{
    public Default(InstanceGenerator generator)
    {
        super(generator);
    }

    public Default(String instanceFileName)
    {
        super(instanceFileName);
    }

    public Instances _getTrainingFromParams(String params)
    {
        return getTraining();
    }
    public Instances _getTestingFromParams(String params)
    {
        return getTesting();
    }
    public List<String> getAllInstanceStrings(String paramStr)
    {
        return Collections.singletonList("default");
    }
}
