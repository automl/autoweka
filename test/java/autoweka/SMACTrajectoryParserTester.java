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

  public static void parseTrajectoryTest(){
    File folderFile = new File("test/experiment_folder");
    Experiment e = Experiment.createFromFolder(folderFile);


    SMACTrajectoryParser stp = new SMACTrajectoryParser();
    Trajectory t = stp.parseTrajectory(e, folderFile, String.valueOf(123));

    assertEquals(t.getPoints().size(),15);

  }

  public static void main(String args[]){
    parseTrajectoryTest();
  }

}
