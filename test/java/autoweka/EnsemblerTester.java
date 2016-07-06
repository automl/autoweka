package autoweka;

import org.junit.Test;
import org.junit.Ignore;
import java.util.List;

import weka.core.Instances;
import static org.junit.Assert.*;

public class EnsemblerTester{

	@Test
	public void parseTrajectoryTest(){
		long startTime= System.nanoTime();
		Ensembler e;
		List<Configuration> lc=null;
		try{
			e = new Ensembler("test/hillclimber_test/Auto-WEKA/");
			lc = e.hillclimb(true);
		}catch(Exception e2){
			e2.printStackTrace();
		}
		System.out.println("@the ensemble \n\n");
		for (Configuration c: lc){
		  System.out.println("hash: "+c.hashCode()+"\nargs: "+c.getArgStrings());
		}
		long endTime = System.nanoTime();
		long totalTime = endTime-startTime;
		long totalTimeSeconds = totalTime/1000000000;
		System.out.println("@end time in nanosecs: "+totalTime+" end time in secs: "+totalTimeSeconds);
	}

}
