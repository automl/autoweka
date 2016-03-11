package autoweka;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import autoweka.instancegenerators.*;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    UtilTester.class,
    MultiLevelTester.class,
    WekaArgumentConverterTester.class
})

public class AutoWEKATestSuite
{
}
