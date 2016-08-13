package autoweka.smac;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import autoweka.Trajectory;
import autoweka.Experiment;

import org.junit.Test;
import org.junit.Ignore;

import weka.core.Instances;
import static org.junit.Assert.*;


public class SMACTrajectoryParserTester{

  @Test
  public void parseTrajectoryTest(){
    File folderFile = new File("test/experiment_folder/Auto-WEKA");
    Experiment e = Experiment.createFromFolder(folderFile);


    SMACTrajectoryParser stp = new SMACTrajectoryParser();

    Trajectory t = stp.parseTrajectory(e, folderFile, String.valueOf(1500)); //1500 arbitrarily picked as a seed for the test case

    assertEquals(t.getPoints().size(),16);


  }



}
