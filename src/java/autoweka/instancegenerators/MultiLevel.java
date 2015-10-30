package autoweka.instancegenerators;

import autoweka.InstanceGenerator;
import weka.core.Instances;
import weka.filters.supervised.instance.Resample;
import autoweka.Util;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Experimental InstanceGenerator that takes as input a child classifier, and creates multiple levels of training data.
 *
 * For example, when used with the CV Generator, this will produce instances that do 10-CV over 30% of the data, then instance that do 10-CV on 60%, then 10-CV on all of the data
 *
 * instanceGeneratorArguments:
 * This is a method that requires a child, instance generator, so the format is ARGS[$]CHILDCLASSNAME[$]CHILDARGS
 *   levelSeed - the seed used to split up into levels
 *   levelPercent - the percentage of data to use at each level
 *   levelBias - the bias towards a uniform partition
 *   numLevels  - the total number of levels to use
 *  
 * instance string format:
 *   levelSeed - the seed used to split up into levels
 *   levelPercent - the percentage of data to use at each level
 *   levelBias - the bias towards a uniform partition
 *   level - the current level before passing off to the child
 */ 
public class MultiLevel extends RandomSubSampling
{
    final Logger log = LoggerFactory.getLogger(MultiLevel.class);

    public MultiLevel(String instanceFileName)
    {
        super(instanceFileName);
    }
    
    public MultiLevel(InstanceGenerator generator)
    {
        super(generator);
    }

    public MultiLevel(Instances training, Instances testing)
    {
        super(training, testing);
    }

    public Instances _getTrainingFromParams(String params)
    {
        InstanceGenerator.NestedArgs args = new InstanceGenerator.NestedArgs(params);
        InstanceGenerator child = InstanceGenerator.create(args.child, getInstancesFromParamsForSubClass(args.current, false), getInstancesFromParamsForSubClass(args.current, true));
        return child.getTrainingFromParams(args.instance);
    }

    public Instances _getTestingFromParams(String params)
    {
        InstanceGenerator.NestedArgs args = new InstanceGenerator.NestedArgs(params);
        InstanceGenerator child = InstanceGenerator.create(args.child, getInstancesFromParamsForSubClass(args.current, false), getInstancesFromParamsForSubClass(args.current, true));
        log.debug("{} {} {}", getTraining().numInstances(), child.getTrainingFromParams(args.instance).numInstances(), child.getTestingFromParams(args.instance).numInstances());
        return child.getTestingFromParams(args.instance);
    }

    private Instances getInstancesFromParamsForSubClass(String params, boolean invert)
    {
        Resample filter = newFilter();
        filter.setInvertSelection(false);
        int level = setFilterParams(filter, params);

        Instances instances = getTraining();
        for(int i = 0; i <= level-1; i++)
            instances = getInstances(instances, filter);

        filter.setInvertSelection(invert);
        return getInstances(instances, filter);
    }

    private int setFilterParams(Resample filter, String paramStr)
    {
        Properties params = Util.parsePropertyString(paramStr);
        filter.setNoReplacement(true);
        if(!"{SEED}".equals(params.getProperty("levelSeed"))) 
            filter.setRandomSeed(Integer.parseInt(params.getProperty("levelSeed", "0")));
        filter.setSampleSizePercent(Double.parseDouble(params.getProperty("levelPercent", "70")));
        filter.setBiasToUniformClass(Double.parseDouble(params.getProperty("levelBias", "0")));
        int level = Integer.parseInt(params.getProperty("level", "-1"));
        if(level < 0)
            throw new RuntimeException("Invalid level '" + level+ "'");
        return level;
    }

    public List<String> getAllInstanceStrings(String params)
    {
        InstanceGenerator.NestedArgs args = new InstanceGenerator.NestedArgs(params);
        Properties levelParams = Util.parsePropertyString(args.current);
        int numLevels = Integer.parseInt(levelParams.getProperty("numLevels", "-1"));
        if(numLevels <= 0)
            throw new RuntimeException("Invalid number of levels");
        //We don't need numLevels in the child
        levelParams.remove("numLevels");

        ArrayList<String> instances = new ArrayList<String>();
        for(int level = numLevels-1; level >= 0; level--)
        {
            levelParams.setProperty("level", Integer.toString(level));
            args.current = Util.propertiesToString(levelParams);
            InstanceGenerator child = InstanceGenerator.create(args.child, getInstancesFromParamsForSubClass(args.current, false), getInstancesFromParamsForSubClass(args.current, true));

            for(String res : child.getAllInstanceStrings(args.instance))
            {
                args.instance = res;
                instances.add(args.toString());
            }
        }
        return instances;
    }
    
    public Map<String, Map<String, String>> getAllInstanceFeatures(String params)
    {
        Map<String, Map<String, String>> feats = new HashMap<String, Map<String, String>>();

        InstanceGenerator.NestedArgs args = new InstanceGenerator.NestedArgs(params);
        Properties levelParams = Util.parsePropertyString(args.current);
        int numLevels = Integer.parseInt(levelParams.getProperty("numLevels", "-1"));
        if(numLevels <= 0)
            throw new RuntimeException("Invalid number of levels");
        //We don't need numLevels in the child
        levelParams.remove("numLevels");

        for(int level = numLevels-1; level >= 0; level--)
        {
            levelParams.setProperty("level", Integer.toString(level));
            args.current = Util.propertiesToString(levelParams);
            InstanceGenerator child = InstanceGenerator.create(args.child, getInstancesFromParamsForSubClass(args.current, false), getInstancesFromParamsForSubClass(args.current, true));

            Map<String, Map<String, String>> childFeatures = child.getAllInstanceFeatures(args.instance);
            for(String instance : childFeatures.keySet())
            {
                args.instance = instance;
                String instName = args.toString();
                feats.put(instName, childFeatures.get(instance));
                feats.get(instName).put("level", Integer.toString(level));
            }
        }
        return feats;
    }
}

