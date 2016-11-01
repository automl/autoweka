package autoweka.instancegenerators;

import autoweka.InstanceGenerator;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.supervised.instance.Resample;
import weka.filters.Filter;
import autoweka.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Experimental InstanceGenerator that splits the data up into random folds, with a fixed percentage used for training
 *
 * instanceGeneratorArguments: A property string with the following
 *   startingseed - the initial seed of the splits
 *   numSamples - the number of folds to gererate
 *   percent - the percentage of data to use in the training set
 *   bias - the bias towards a uniform partition
 *  
 * instance string format:
 *   seed - the seed used to split up data
 *   percent - the percentage of data to use in the training set
 *   bias - the bias towards a uniform partition
 */ 
public class RandomSubSampling extends InstanceGenerator
{
    final static Logger log = LoggerFactory.getLogger(RandomSubSampling.class);

    private static class RegressionResample extends Resample
    {
        public Capabilities getCapabilities() {
            Capabilities result = super.getCapabilities();
            result.disableAll();

            // attributes
            result.enableAllAttributes();
            result.enable(Capability.MISSING_VALUES);

            result.enable(Capability.NUMERIC_CLASS);

            return result;
        }

        public void createSubsampleWithReplacement(Random random, int origSize, int sampleSize, int ignored, int[] classIndices) {
            throw new RuntimeException("This should not happen....");
            /*
            for (int i = 0; i < sampleSize; i++) {
                int index = random.nextInt(origSize);
                push((Instance) getInputFormat().instance(index).copy());
            }*/
        }

        public void createSubsampleWithoutReplacement(Random random, int origSize, int sampleSize, int ignored, int[] classIndices) {
            if (sampleSize > origSize) {
                sampleSize = origSize;
                log.warn("Resampling without replacement can only use percentage <=100% - Using full dataset!");
            }

            int[] indices = new int[origSize];
            for(int i = 0; i < origSize; i++){
                indices[i] = i;
            }

            //Shuffle the indicies using the random
            for(int i = 0; i < origSize; i++){
                int targetIndex = random.nextInt(origSize - i) + i;
                int temp = indices[i];
                indices[i] = indices[targetIndex];
                indices[targetIndex] = temp;
            }
            
            //Take the part of the array that makes the most sense
            if(getInvertSelection()){
                for(int i = sampleSize; i < origSize; i++){
                    push((Instance) getInputFormat().instance(indices[i]).copy());
                }
            }else{
                for(int i = 0; i < sampleSize; i++){
                    push((Instance) getInputFormat().instance(indices[i]).copy());
                }
            }
            indices = null;
        }
        @Override
        protected void createSubsample() {
            int origSize = getInputFormat().numInstances();
            int sampleSize = (int) (origSize * m_SampleSizePercent / 100);

            // Create the new sample
            Random random = new Random(m_RandomSeed);

            // Convert pending input instances
            if (getNoReplacement())
                createSubsampleWithoutReplacement(random, origSize, sampleSize, 0, null);
            else
                createSubsampleWithReplacement(random, origSize, sampleSize, 0, null);
        }
    }

    public RandomSubSampling(InstanceGenerator generator)
    {
        super(generator);
    }

    public RandomSubSampling(Instances training, Instances testing)
    {
        super(training, testing);
    }

    public RandomSubSampling(String instanceFileName)
    {
        super(instanceFileName);
    }

    public Instances _getTrainingFromParams(String params)
    {
        Resample filter = newFilter();
        filter.setInvertSelection(false);
        setFilterParams(filter, params);
        return getInstances(getTraining(), filter);
    }

    public Instances _getTestingFromParams(String params)
    {
        //Get the instances filtered (Not we use the training data here)
        Resample filter = newFilter();
        filter.setInvertSelection(true);
        setFilterParams(filter, params);

        return getInstances(getTraining(), filter);
    }

    protected final Resample newFilter(){
        Resample filter = new Resample();
        try{
            filter.setInputFormat(getTraining());
        }catch(Exception e){
            filter = new RegressionResample();
        }
        return filter;
    }

    protected Instances getInstances(Instances data, Resample filter)
    {
        try{
            filter.setInputFormat(data);
        }catch(Exception e) {
            throw new RuntimeException("Failed to set input format", e);
        }
        Instances newData = filter.getOutputFormat();

        try
        {
            newData = Filter.useFilter(data, filter); 
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
        return newData;
    }

    private void setFilterParams(Resample filter, String paramStr)
    {
        Properties params = Util.parsePropertyString(paramStr);
        filter.setNoReplacement(true);
        filter.setRandomSeed(Integer.parseInt(params.getProperty("seed", "0")));
        filter.setSampleSizePercent(Double.parseDouble(params.getProperty("percent", "70")));
        filter.setBiasToUniformClass(Double.parseDouble(params.getProperty("base", "0")));
    }

    public List<String> getAllInstanceStrings(String paramStr)
    {
        Properties params = Util.parsePropertyString(paramStr);

        int seed, numSamples;
        double percent, bias;
        try{
            seed = Integer.parseInt(params.getProperty("startingSeed", "0"));
        }catch(Exception e){
            throw new RuntimeException("Failed to parse startingSeed", e);
        }
        try{
            numSamples = Integer.parseInt(params.getProperty("numSamples", "-1"));
        }catch(Exception e){
            throw new RuntimeException("Failed to parse numSamples", e);
        }
        try{
            percent = Integer.parseInt(params.getProperty("percent", "-1"));
        }catch(Exception e){
            throw new RuntimeException("Failed to parse percent", e);
        }
        try{
            bias = Double.parseDouble(params.getProperty("bias", "0"));
        }catch(Exception e){
            throw new RuntimeException("Failed to parse bias", e);
        }
        if(numSamples <= 0)
            throw new RuntimeException("numSamples must be set to something > 0");
        if(percent <= 0 || percent >= 100)
            throw new RuntimeException("percent must be set to something > 0 && < 100");
        if(bias < 0 || bias > 1)
            throw new RuntimeException("bias must be set to something > 0 && < 1");

        ArrayList<String> instanceStrings = new ArrayList<String>(numSamples);
        for(int i = 0; i < numSamples; i++)
        {
            instanceStrings.add("seed=" + (seed + i) + ":percent=" + percent + ":bias=" + bias);
        }
        return instanceStrings;
    }
}

