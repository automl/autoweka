package autoweka.instancegenerators;

import autoweka.InstanceGenerator;
import autoweka.Util;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weka.core.Instances;
import weka.filters.supervised.instance.Resample;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Experimental InstanceGenerator that takes as input a child classifier, and holds back a bunch of data as a 'Termination' set from the SMBO methods.
 *
 * instanceGeneratorArguments:
 * This is a method that requires a child, instance generator, so the format is ARGS[$]CHILDCLASSNAME[$]CHILDARGS
 *   terminationSeed - the seed used to split up into the termination or not 
 *   terminationPercent - the percentage of data to use for a termination set
 *   terminationBias - the bias towards a uniform partition
 *  
 * instance string format:
 *   terminationSeed - the seed used to split up into the termination or not 
 *   terminationPercent - the percentage of data to use for a termination set
 *   terminationBias - the bias towards a uniform partition
 */ 
public class TerminationHoldout extends RandomSubSampling
{
    final Logger log = LoggerFactory.getLogger(TerminationHoldout.class);

    public TerminationHoldout(String instanceFileName)
    {
        super(instanceFileName);
    }

    public Instances _getTrainingFromParams(String params)
    {
        InstanceGenerator.NestedArgs args = new InstanceGenerator.NestedArgs(params);
        //The Termination set is backwards - you say how much you want to reserve for the termination set...
        InstanceGenerator child = InstanceGenerator.create(args.child, getInstancesFromParamsForSubClass(args.current, true), getInstancesFromParamsForSubClass(args.current, false));
        return child.getTrainingFromParams(args.instance);
    }

    public Instances _getTestingFromParams(String params)
    {
        InstanceGenerator.NestedArgs args = new InstanceGenerator.NestedArgs(params);
        //The Termination set is backwards - you say how much you want to reserve for the termination set...
        InstanceGenerator child = InstanceGenerator.create(args.child, getInstancesFromParamsForSubClass(args.current, true), getInstancesFromParamsForSubClass(args.current, false));
        log.debug("{} {} {}", getTraining().numInstances(), child.getTrainingFromParams(args.instance).numInstances(), child.getTestingFromParams(args.instance).numInstances());
        return child.getTestingFromParams(args.instance);
    }

    private Instances getInstancesFromParamsForSubClass(String params, boolean invert)
    {
        Resample filter = newFilter();
        filter.setInvertSelection(invert);
        setFilterParams(filter, params);

        return getInstances(getTraining(), filter);
    }

    private void setFilterParams(Resample filter, String paramStr)
    {
        Properties params = Util.parsePropertyString(paramStr);
        filter.setNoReplacement(true);

        if(!"{SEED}".equals(params.getProperty("terminationSeed"))) 
            filter.setRandomSeed(Integer.parseInt(params.getProperty("terminationSeed", "0")));
        filter.setSampleSizePercent(Double.parseDouble(params.getProperty("terminationPercent", "30")));
        filter.setBiasToUniformClass(Double.parseDouble(params.getProperty("terminationBias", "0")));
    }

    public List<String> getAllInstanceStrings(String params)
    {
        InstanceGenerator.NestedArgs args = new InstanceGenerator.NestedArgs(params);

        InstanceGenerator child = InstanceGenerator.create(args.child, getInstancesFromParamsForSubClass(args.current, false), getInstancesFromParamsForSubClass(args.current, true));

        List<String> res = child.getAllInstanceStrings(args.instance);
        for(int i = 0; i < res.size(); i++)
        {
            args.instance = res.get(i);
            res.set(i, args.toString());
        }
        return res;
    }
    
    public Map<String, Map<String, String>> getAllInstanceFeatures(String params)
    {
        InstanceGenerator.NestedArgs args = new InstanceGenerator.NestedArgs(params);
        InstanceGenerator child = InstanceGenerator.create(args.child, getInstancesFromParamsForSubClass(args.current, false), getInstancesFromParamsForSubClass(args.current, true));
        Map<String, Map<String, String>> childFeats = child.getAllInstanceFeatures(args.instance);
        Map<String, Map<String, String>> feats = new HashMap<String, Map<String, String>>();
        for(String instance: childFeats.keySet())
        {
            args.instance = instance;
            feats.put(args.toString(), childFeats.get(instance));
        }
        return feats;
    }
}
