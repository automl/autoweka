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
  public void mergeTest(){

    //Tests exception for when args are different
    Configuration c1 = new Configuration("args_placeholder");
    Configuration c2 = new Configuration("different_args_placeholder");
    boolean caught=false;
    try{
      c1.mergeWith(c2);
    }catch(Exception e){
      caught=true;
    }
    assertTrue(caught);

    //Tests merging
    Configuration c3 = new Configuration("args_placeholder");
    double [] scores = new double[10];
    double sum = 0;

    Random rg = new Random();
    for(int i = 0; i < 5; i++){
      scores[i]=rg.nextDouble()%20;
      sum+=scores[i];
      c1.setEvaluationValues(scores[i],i);
    }
    for(int i = 5; i < 10; i++){
      scores[i]=rg.nextDouble()%20;
      sum+=scores[i];
      c3.setEvaluationValues(scores[i],i);
    }

    //Tests averaging
    c1.mergeWith(c3);
    assertEquals(c1.getAverageScore(),sum/10,0.1);

    //Tests fold counting
    List<String> strC1Folds = c1.getFolds();
    int [] c1Folds= new int[strC1Folds.size()];
    int j = 0;
    for (String s : strC1Folds){
      c1Folds[j]=Integer.parseInt(s);
      j++;
    }
    assertEquals(c1Folds.length,10);
    Arrays.sort(c1Folds);
    for(int i=0; i<10; i++){
      assertEquals(i,c1Folds[i]);
    }
  }

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
    c1.setEvaluationValues(10,0);
    c1.setEvaluationValues(20,1);
    c1.setEvaluationValues(10,2);
    c2.setEvaluationValues(0,0);
    c2.setEvaluationValues(0,1);
    assertTrue(c1.compareTo(c2)>0);
    assertFalse(c2.compareTo(c1)>0);

    //Tests comparison within same number of folds
    c2.setEvaluationValues(0,2);
    assertTrue(c2.compareTo(c1)>0);

    Configuration c3 = new Configuration("args_placeholder");
    c3.setEvaluationValues(0,0);
    c3.setEvaluationValues(0,1);
    c3.setEvaluationValues(0,2);

    assertTrue(c2.compareTo(c3)==0);

    c2.setEvaluationValues(0,3);
    c3.setEvaluationValues(1000,3);
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
      c1.setEvaluationValues(score,rg.nextInt(amtIterations)); //its okay if we get two identical folds
      sum+=score;
    }

    assertEquals(sum/amtIterations, c1.getAverageScore(), 0.1);

  }

  @Test
  public void rankerTest(){

    String configIndex   = "test/experiment_folder/Auto-WEKA/configIndex.txt";
    String sortedTestLog = "test/experiment_folder/Auto-WEKA/SortedConfigurationLog.xml" ;

    try{
      Util.initializeFile(configIndex);
    }catch(Exception e){
      System.out.println("Couldn't initialize temporaryTestLog.xml");
    }
    try{
      Util.initializeFile(sortedTestLog);
    }catch(Exception e){
      System.out.println("Couldn't initialize sortedTestLog.xml");
    }


    ConfigurationRanker.rank(1000,"test/experiment_folder/Auto-WEKA",sortedTestLog,configIndex,"IGNORE");

    ConfigurationCollection cc = ConfigurationCollection.fromXML(sortedTestLog, ConfigurationCollection.class);
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
