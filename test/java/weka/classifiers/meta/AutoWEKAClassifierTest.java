package weka.classifiers.meta;

import weka.classifiers.Evaluation;
import weka.classifiers.meta.AutoWEKAClassifier;

import org.junit.Test;
import static org.junit.Assert.*;

public class AutoWEKAClassifierTest {
    @Test
    public void testClassifier() throws Exception {
        String[] args = {"-t", "test/iris.arff", "-no-cv", "-timeLimit", "1"};
        String out = Evaluation.evaluateModel(new AutoWEKAClassifier(), args);

        assertNotNull(out);
        assertTrue(out.indexOf("classifier: weka.classifiers.") > -1);
        assertTrue(out.indexOf("Error on training data") > -1);
        assertTrue(out.indexOf("Total Number of Instances              150") > -1);
    }
}
