package autoweka.instancegenerators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import weka.core.Instance;
import weka.core.Instances;

import autoweka.InstanceGenerator;
import autoweka.Util;
import static org.junit.Assert.*;

public class MultiLevelTester
{
    @Test
    public void levelSizes()
    {
        for(int numInstances = 512; numInstances <= 2048; numInstances *= 2){
            for(float levelPercent = 0.25f; levelPercent <= 1; levelPercent += 0.25){
                for(int numLevels = 1; numLevels <= 4; numLevels++) {
                    Instances data = Util.createDummyInstances(numInstances, 2, 1, 0, 0, 0, 0, 0);

                    MultiLevel generator = new MultiLevel(data, data);
                    InstanceGenerator.NestedArgs args = new InstanceGenerator.NestedArgs("numLevels=" + numLevels + ":levelPercent=" + (levelPercent*100), "autoweka.instancegenerators.CrossValidation", "numFolds=2");
                    List<String> instances = generator.getAllInstanceStrings(args.toString());
                    for(String instance: instances){
                        int level = Integer.parseInt(Util.parsePropertyString(new InstanceGenerator.NestedArgs(instance).current).getProperty("level"));
                        float targetInstances = numInstances;
                        for(int i = 0; i <= level; i++) { targetInstances *= levelPercent; }

                        //System.out.println(instance);
                        //System.out.println(generator.getTrainingFromParams(instance).size() + generator.getTestingFromParams(instance).size());
                        assertEquals("Level size missmatch for level " + (level) + " deep in " + (numLevels-1) + ", with " + numInstances + " instances at " + levelPercent, targetInstances, generator.getTrainingFromParams(instance).size() + generator.getTestingFromParams(instance).size(), 1);
                    }
                }
            }
        }
    }

    @Test
    @Ignore
    public void levelSubsets()
    {
        for(int numInstances = 512; numInstances <= 2048; numInstances *= 2){
            for(float levelPercent = 0.25f; levelPercent <= 1; levelPercent += 0.25){
                for(int numLevels = 1; numLevels <= 4; numLevels++) {
                    Instances data = Util.createDummyInstances(numInstances, numInstances, 1, 0, 0, 0, 0, 0);

                    MultiLevel generator = new MultiLevel(data, data);
                    InstanceGenerator.NestedArgs args = new InstanceGenerator.NestedArgs("numLevels=" + numLevels + ":levelPercent=" + (levelPercent*100), "autoweka.instancegenerators.CrossValidation", "numFolds=2");

                    Map<Integer, Set<String>> levelClasses = new HashMap<Integer, Set<String>>();
                    List<String> instances = generator.getAllInstanceStrings(args.toString());
                    for(int i = 0; i < numLevels; i++){
                        levelClasses.put(i, new HashSet<String>());
                    }

                    for(String instance: instances){
                        int level = Integer.parseInt(Util.parsePropertyString(new InstanceGenerator.NestedArgs(instance).current).getProperty("level"));
                        for(Instance i : generator.getTrainingFromParams(instance)){
                            levelClasses.get(level).add(Double.toString(i.classValue()));
                        }
                        for(Instance i : generator.getTestingFromParams(instance)){
                            levelClasses.get(level).add(Double.toString(i.classValue()));
                        }
                    }

                    //Make sure that everything is there
                    for(int i = numLevels-1; i >= 0; i--){
                        for(int j = i-1; j >= 0; j--){
                            for(String c: levelClasses.get(i)) 
                                assertTrue("Level " + j + " does not contain instance found in level " + i + " with " + numInstances + " instances at " + levelPercent, levelClasses.get(j).contains(c));
                        }
                    }
                }
            }
        }
    }
}
