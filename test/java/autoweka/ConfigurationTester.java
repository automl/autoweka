package autoweka;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.junit.Ignore;

import weka.core.Instances;
import static org.junit.Assert.*;

public class ConfigurationTester{


  //rankTest


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
      scores[i]=rg.nextDouble();
      sum+=scores[i];
      c1.setEvaluationValues(scores[i],i);
    }
    for(int i = 5; i < 10; i++){
      scores[i]=rg.nextDouble();
      c3.setEvaluationValues(scores[i],i);
    }

    //Tests averaging
    c1.mergeWith(c3);
    assertEquals(c1.getAverageScore(),sum/10);

    //Tests fold counting
    int [] c1Folds = c1.getFolds();
    assertEquals(c1Folds.length,10);
    Arrays.sort(c1Folds);
    for(int i=0; i<10; i++){
      assertEquals(i,c1Folds[i]);
    }
  }

  public void compareToTest(){

    //Tests exception for when types are different
    Configuration c1 = new Configuration("args_placeholder");
    Integer huehue = new Integer(4815162342);
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
    assertTrue(c1>c2);
    assertFalse(c2>c1);

    //Tests comparison within same number of folds
    c2.setEvaluationValues(0,2);
    assertTrue(c2>c1);

    Configuration c3 = new Configuration("args_placeholder");
    c3.setEvaluationValues(0,0);
    c3.setEvaluationValues(0,1);
    c3.setEvaluationValues(0,2);
    assertTrue(c2==c1);

    c2.setEvaluationValues(0,3);
    c3.setEvaluationValues(1000,3);
    assertTrue(c2>c3);
  }

  public void settersAndAveragerTest(){
    Configuration c1 = new Configuration("args_placeholder");

    Random rg = new Random();
    int amtIterations = rg.nextInt(21);

    double sum = 0;

    for (int i = 0; i<amtIterations; i++){
      double score = rg.nextDouble(20);
      c1.setEvaluationValues(score,rand.nextInt(amtIterations)); //its okay if we get two identical folds
      sum+=score;
    }

    assertEquals(sum/amtIterations, c1.getAverageScore());

  }

  public void rankerTest(){

    try{
      Util.initializeFile("temporaryTestLog.xml");
    }catch(Exception e){
      System.out.println("Couldn't initialize temporaryTestLog.xml");
    }
    try{
      Util.initializeFile("sortedTestLog.xml");
    }catch(Exception e){
      System.out.println("Couldn't initialize sortedTestLog.xml");
    }

    ConfigurationCollection cc = new ConfigurationCollection();
    Random rg = new Random();
    for(i=0;i<100;i++){
      Configuration c = new Configuration("config_no_"+i%10);
      c.setEvaluationValues(rg.nextDouble(20),rg.nextInt(10));
      cc.add(c);
    }
    cc.toXML("temporaryTestLog.xml"));

    ConfigurationRanker.rank(100,"temporaryTestLog.xml","sortedTestLog.xml");

    cc = fromXML("sortedTestLog");
    double currentscore = cc.get(0).getAverageScore();
    for(i=0;i<cc.size();i++){
      assertTrue(currentscore <= cc.get(i).getAverageScore());
    }

  }

  public static main void(String args[]){

    settersAndAveragerTest();
    compareToTest();
    mergeTest();
    rankerTest();

  }


}
