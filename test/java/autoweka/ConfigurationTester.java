package autoweka;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import java.util.Random;

import org.junit.Test;
import org.junit.Ignore;

import weka.core.Instances;
import static org.junit.Assert.*;

public class ConfigurationTester{

  @Test
  public void compareToTest(){

    //Tests exception for when types are different
    Configuration c1 = new Configuration("args_placeholder");
    Integer huehue = new Integer(4815+162342);
    boolean caught=false;
    try{
      c1.compareTo(huehue);
    }catch(Exception e){
      caught=true;
    }
    assertTrue(caught);

    //Tests priorization of configs evaluated over more folds
    Configuration c2 = new Configuration("args_placeholder");
    c1.update(0,10);
    c1.update(1,20);
    c1.update(2,10);
    c2.update(0,0);
    c2.update(1,0);
    assertTrue(c1.compareTo(c2)>0);
    assertFalse(c2.compareTo(c1)>0);

    //Tests comparison within same number of folds
    c2.update(2,0);
    assertTrue(c2.compareTo(c1)>0);

    Configuration c3 = new Configuration("args_placeholder");
    c3.update(0,0);
    c3.update(1,0);
    c3.update(2,0);

    assertTrue(c2.compareTo(c3)==0);

    c2.update(3,0);
    c3.update(3,1000);
    assertTrue(c2.compareTo(c3)>0);
  }

  @Test
  public void settersAndAveragerTest(){

    Configuration c1 = new Configuration("args_placeholder");

    Random rg = new Random();
    int amtIterations = rg.nextInt(21);

    double sum = 0;

    for (int i = 0; i<amtIterations; i++){
      double score = rg.nextDouble()%20;
      c1.update(rg.nextInt(amtIterations),score); //its okay if we get two identical folds
      sum+=score;
    }

    assertEquals(sum/amtIterations, c1.getAverageScore(), 0.1);

  }

  @Test
  public void rankerTest(){

    String sortedTestLog = "test/experiment_folder/Auto-WEKA/EnsemblerLogging/configuration_ranking.xml" ;


    try{
      Util.initializeFile(sortedTestLog);
    }catch(Exception e){
      System.out.println("Couldn't initialize sortedTestLog.xml");
    }



    ConfigurationCollection cc = ConfigurationCollection.fromXML(sortedTestLog, ConfigurationCollection.class);
	 cc.rank("test/experiment_folder/Auto-WEKA");
    double bestScore = cc.get(0).getAverageScore();
    int    amtFolds  = cc.get(0).getFolds().size();

    for(int i=0;i<cc.size();i++){
      assertTrue(amtFolds >= cc.get(i).getFolds().size());
      if(amtFolds==cc.get(i).getFolds().size()){
        assertTrue(bestScore <= cc.get(i).getAverageScore());
      }
    }
  }

}
