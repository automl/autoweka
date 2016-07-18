package autoweka;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import autoweka.instancegenerators.*;

import autoweka.smac.SMACTrajectoryParserTester;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    //UtilTester.class,
    //MultiLevelTester.class,
    //WekaArgumentConverterTester.class,
    //ConfigurationTester.class,
    //SMACTrajectoryParserTester.class,
	 EnsemblerTester.class
})

public class AutoWEKATestSuite
{
}
