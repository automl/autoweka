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
		System.out.println("@the ensemble \n\n");
		// for (Configuration c: lc){
		//   System.out.println("hash: "+c.hashCode()+"\nargs: "+c.getArgStrings());
		// }
		long endTime = System.nanoTime();
		long totalTime = endTime-startTime;
		long totalTimeSeconds = totalTime/1000000000;
		System.out.println("@end time in nanosecs: "+totalTime+" end time in secs: "+totalTimeSeconds);
	}
	// @Test
	// public void randMaxTester(){
	//
	// 	int[] x = {10,2,3,4,10,10,4,5,10};
	// 	int[] y = {10,2,3,11,10,10,4,5,10};
	// 	int[] z = {10,2,1,1,10,10,4,1,10};
	// 	int[] t = {10,2,1,1,0,10,4,1,10};
	//
	// 	for(int i = 0; i<100;i++){
	// 		System.out.println("x max index:"+Util.indexMax(x));
	// 	}
	// 	for(int i = 0; i<100;i++){
	// 		System.out.println("y max index:"+Util.indexMax(y));
	// 	}
	// 	for(int i = 0; i<100;i++){
	// 		System.out.println("z min index:"+Util.randomizedIndexMin(z));
	// 	}
	// 	for(int i = 0; i<100;i++){
	// 		System.out.println("t min index:"+Util.randomizedIndexMin(t));
	// 	}
	//
	// }
}
