package autoweka;

import org.junit.Test;
import org.junit.Ignore;
import java.util.List;

import weka.core.Instances;
import static org.junit.Assert.*;

public class EnsemblerTester{

	@Test
	public void ensemblerTest(){
		long startTime= System.nanoTime();
		Ensembler e;
		List<Configuration> lc=null;
		try{
			e = new Ensembler("test/hillclimber_test/Auto-WEKA/");
			lc = e.hillclimb();
		}catch(Exception e2){
			e2.printStackTrace();
		}
		long endTime = System.nanoTime();
		long totalTime = endTime-startTime;
		long totalTimeSeconds = totalTime/1000000000;
		System.out.println("@end time in nanosecs: "+totalTime+" end time in secs: "+totalTimeSeconds);
		assertTrue(lc.size()>1);
	}

}
