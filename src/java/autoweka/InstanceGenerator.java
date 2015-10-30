package autoweka;

import weka.core.converters.ConverterUtils.DataSource;
import weka.core.Instances;
import weka.core.DenseInstance;
import weka.core.Attribute;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for partitioning a dataset (consisting of training and testing data) into 'instances' that are given to the SMBO method
 */
public abstract class InstanceGenerator
{
    final static Logger log = LoggerFactory.getLogger(InstanceGenerator.class);

    private Instances mTraining = null;
    private Instances mTesting = null;

    /**
     * Clones an InstanceGenerator from another
     */
    public InstanceGenerator(InstanceGenerator generator)
    {
        mTraining = new Instances(generator.mTraining);
        mTesting  = new Instances(generator.mTesting);
    }

    /**
     * Build an instance generator from the given training and testing data
     */
    public InstanceGenerator(Instances training, Instances testing)
    {
        mTraining = training;
        mTesting = testing;
    }

    /**
     * Builds an InstanceGenerator from a datasetFileName, ie a zip file containing exactly two files, 'train.arff' and 'test.arff'
     */
    public InstanceGenerator(String datasetString)
    {
        if(datasetString.equals("__dummy__")){
            mTraining = Util.createDummyInstances(50, 2, 1, 0, 0, 0, 0, 0);
            mTesting = Util.createDummyInstances(50, 2, 1, 0, 0, 0, 0, 1);
        }else{
            //It's a property string - parse it
            Properties props;
            try
            {
                props = Util.parsePropertyString(datasetString);
            }catch(Exception e){
                log.warn("It looks like you're using an old experiment that doesn't indicate the type of dataset that it is using");
                loadZipFile(datasetString, "last");
                return;
            }

            String type = props.getProperty("type");

            if(type == null){
                throw new RuntimeException("Dataset string does not contain a type");
            }else if(type.equals("zipFile")){
                loadZipFile(props.getProperty("zipFile"), props.getProperty("classIndex", "last"));
            }else if(type.equals("trainTestArff")){
                loadTrainTestArff(props.getProperty("trainArff"), props.getProperty("testArff"), props.getProperty("classIndex", "last"));
            }else{
                throw new RuntimeException("Unhandled type data set type '" + type  + "'");
            }
        }
    }

    /**
     * Loads up the training and testing data from a zip file
     */
    private void loadZipFile(String zipFileName, String classIndex)
    {
        InputStream trainSource = null, testSource = null;
        //The instance file is a zipped file containing a file called 'train' and one called 'test'
        try {
            ZipFile zipFile = new ZipFile(zipFileName);
            Enumeration entries = zipFile.entries();
            while(entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)entries.nextElement();

                String name = entry.getName();
                if (name.equals("train.arff"))
                {
                    trainSource = zipFile.getInputStream(entry);
                }
                else if(name.equals("test.arff"))
                {
                    testSource = zipFile.getInputStream(entry);
                }
                else
                {
                    //What is this?
                    throw new RuntimeException("Unknown file in zip dataset '" + name + "'");
                }
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("IO Operation failed", e);
        }

        //Get the training data
        try {
            mTraining = Util.loadDataSource(trainSource);
            if (mTraining.classIndex() == -1){
                if(classIndex.equals("last"))
                    mTraining.setClassIndex(mTraining.numAttributes() - 1);
                else
                    mTraining.setClassIndex(Integer.parseInt(classIndex));
            }
        } catch(Exception e) {
            throw new RuntimeException("Failed to load training data provided in zip", e);
        }

        //Get the testing data
        try {
            mTesting  = Util.loadDataSource(testSource);
            if (mTesting.classIndex() == -1){
                if(classIndex.equals("last"))
                    mTesting.setClassIndex(mTesting.numAttributes() - 1);
                else
                    mTesting.setClassIndex(Integer.parseInt(classIndex));
            }
        } catch(Exception e) {
            throw new RuntimeException("Failed to load testing data provided in zip", e);
        }

    }
    
    private void loadTrainTestArff(String trainArff, String testArff, String classIndex)
    {
        //Get the training data
        try {
            mTraining = Util.loadDataSource(new FileInputStream(trainArff));
            if (mTraining.classIndex() == -1){
                if(classIndex.equals("last"))
                    mTraining.setClassIndex(mTraining.numAttributes() - 1);
                else
                    mTraining.setClassIndex(Integer.parseInt(classIndex));
            }
        } catch(Exception e) {
            throw new RuntimeException("Failed to open training arff", e);
        }

        //Get the testing data
        if(!testArff.equals("__dummy__"))
        {
            try {
                mTesting = Util.loadDataSource(new FileInputStream(testArff));
                if (mTesting.classIndex() == -1){
                    if(classIndex.equals("last"))
                        mTesting.setClassIndex(mTesting.numAttributes() - 1);
                    else
                        mTesting.setClassIndex(Integer.parseInt(classIndex));
                }
            } catch(Exception e) {
                throw new RuntimeException("Failed to open testing arff", e);
            }
        }else{
            mTesting = mTraining;
        }
    }

    /**
     * Given a parameter string (generally in the form of a property string), get the training data; if params is 'default', then this method returns the raw training data
     */
    public final Instances getTrainingFromParams(String params)
    {
        if(params.equals("default"))
            return getTraining();
        return _getTrainingFromParams(params);
    }

    /**
     * Given a parameter string (generally in the form of a property string), get the testing data; if params is 'default', then this method returns the raw testing data
     */
    public final Instances getTestingFromParams(String params)
    {
        if(params.equals("default"))
            return getTesting();
        return _getTestingFromParams(params);
    }

    /**
     * Subclass implementation for getting the training data given the param string
     */
    public abstract Instances _getTrainingFromParams(String params);
    /**
     * Subclass implementation for getting the testing data given the param string
     */
    public abstract Instances _getTestingFromParams(String params);

    /**
     * Gets a copy of the training data
     */
    public Instances getTraining()
    {
        return new Instances(mTraining);
    }

    /**
     * Gets a copy of the testing data
     */
    public Instances getTesting()
    {
        return new Instances(mTesting);
    }

    /**
     * Gets a list of all the 'params' Strings that can be used with this InstanceGenerator.
     *
     * For example, the for 10-fold Cross Validation this method would return 10 Strings defining each fold
     */
    public abstract List<String> getAllInstanceStrings(String params);

    /**
     * Some SMBO methods can leverage extra features about an Instance, this method should return a map of feature/value pairs for all instances
     */
    public Map<String, Map<String, String>> getAllInstanceFeatures(String params)
    {
        Map<String, Map<String, String>> feats = new HashMap<String, Map<String, String>>();
        List<String> instanceNames = getAllInstanceStrings(params);
        for(String name: instanceNames)
        {
            feats.put(name, new HashMap<String, String>());
        }
        return feats;
    }

    /**
     * Creates an instance of an InstanceGenerator given a class name and the datasetFileName
     */
    static public InstanceGenerator create(String className, String datasetFileName)
    {
        if(className == null || className.isEmpty() || className.equals("null"))
        {
            log.warn("No instance generator set, using default");
            className = "autoweka.instancegenerators.Default";
        }

        //Get one of these classifiers
        Class<?> cls;
        try
        {
            className = className.trim();
            cls = Class.forName(className);
            return (InstanceGenerator)cls.getDeclaredConstructor(String.class).newInstance(datasetFileName);
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Could not find class '" + className + "': " + e, e);
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to instantiate '" + className + "': " + e, e);
        }
    }

    /**
     * Creates an instance of an InstanceGenerator given a class name, training data and the testing data  
     */
    static public InstanceGenerator create(String className, Instances training, Instances testing)
    {
        if(className == null || className.isEmpty() || className.equals("null"))
        {
            log.warn("No instance generator set, using default");
            className = "autoweka.instancegenerators.Default";
        }

        //Get one of these classifiers
        Class<?> cls;
        try
        {
            className = className.trim();
            cls = Class.forName(className);
            return (InstanceGenerator)cls.getDeclaredConstructor(Instances.class, Instances.class).newInstance(training, testing);
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Could not find class '" + className + "': " + e, e);
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to instantiate '" + className + "': " + e, e);
        }
    }

    /** 
     * Struct for dealing with nested args on InstanceGenerators that are chained together.
     *
     * Some InstanceGenerators work as a filter on another, this Stuct provides an easy way to split up the arguments to each method
     */
    public static class NestedArgs{
        //Constructor that splits
        public NestedArgs(String params){
            List<String> args = Util.splitNestedString(params, "[$]", 2, 0);
            current = args.get(0);
            child = args.get(1);
            instance = args.get(2);
        }

        public NestedArgs(String current, String child, String instance){
            this.current = current;
            this.child = child;
            this.instance = instance;
        }

        public String toString(){
            return Util.joinStrings("[$]", current, child, instance);
        }

        public String current;
        public String child;
        public String instance;
    }
}
