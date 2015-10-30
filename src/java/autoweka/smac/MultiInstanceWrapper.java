package autoweka.smac;

import java.util.List;
import java.util.Properties;

import autoweka.ClassifierResult;
import autoweka.Util;
import autoweka.SubProcessWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiInstanceWrapper extends SMACWrapper
{
    final Logger log = LoggerFactory.getLogger(MultiInstanceWrapper.class);

    public String mParams;

    public static void main(String[] args)
    {
        MultiInstanceWrapper wrapper = new MultiInstanceWrapper();
        wrapper.run(args);
    }

    @Override
    protected ClassifierResult _doRun(List<String> runnerArgs)
    {
        //Figure out what instances we're using
        List<String> instances = mRunner.getInstanceGenerator().getAllInstanceStrings(mInstance);
        
        ClassifierResult res = new ClassifierResult(mResultMetric);
        res.setCompleted(false);

        //TODO: not hack this
        String memory = "3000m";

        int executedCount = 0;
        int failedCount = 0;

        for(String instance : instances){
            log.debug("Instance: {}", instance);
            Properties props = new Properties();
            props.put("datasetString", mProperties.getProperty("datasetString"));
            props.put("instanceGenerator", mProperties.getProperty("instanceGenerator"));
            props.put("instanceGeneratorArgs", mInstance);
            props.put("resultMetric", mResultMetric);
            SubProcessWrapper.ErrorAndTime errTime = SubProcessWrapper.getErrorAndTime(null, memory, props, mTimeout, instance, Util.joinStrings(" ", runnerArgs), mExperimentSeed);
            //Update the res's time and estimate
            res._setRawScore((res.getRawScore()*executedCount + errTime.error)/(executedCount+1));
            res.setTrainingTime(res.getTrainingTime() + errTime.time);

            //TODO: Make this robust and check to see if we've hit the global timeout?
            if(errTime.error > 99.99){
                failedCount++;
                if(failedCount >= 3){
                    return res;
                }
            }
            executedCount++;
        }
        res.setCompleted(true);
        return res;
    }
}
