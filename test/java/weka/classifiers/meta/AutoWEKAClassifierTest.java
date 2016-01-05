package weka.classifiers.meta;

import weka.classifiers.Evaluation;
import weka.classifiers.meta.AutoWEKAClassifier;

import org.junit.Test;
import static org.junit.Assert.*;

public class AutoWEKAClassifierTest {
    @Test
    public void testDefaults() throws Exception {
        String[] args = {"-t", "test/iris.arff", "-seed", "1", "-no-cv", "-timeLimit", "1"};
        String out = Evaluation.evaluateModel(new AutoWEKAClassifier(), args);

        assertNotNull(out);
        assertTrue(out.indexOf("classifier: weka.classifiers.") > -1);
        assertTrue(out.indexOf("Error on training data") > -1);
        assertTrue(out.indexOf("Total Number of Instances              150") > -1);
    }

    @Test
    public void testCV() throws Exception {
        String[] args = {"-t", "test/iris.arff", "-seed", "1", "-no-cv", "-timeLimit", "1", "-resampling", "CrossValidation"};
        String out = Evaluation.evaluateModel(new AutoWEKAClassifier(), args);

        assertNotNull(out);
        assertTrue(out.indexOf("classifier: weka.classifiers.") > -1);
        assertTrue(out.indexOf("Error on training data") > -1);
        assertTrue(out.indexOf("Total Number of Instances              150") > -1);
    }

    @Test
    public void testML() throws Exception {
        String[] args = {"-t", "test/iris.arff", "-seed", "1", "-no-cv", "-timeLimit", "1", "-resampling", "MultiLevel"};
        String out = Evaluation.evaluateModel(new AutoWEKAClassifier(), args);

        assertNotNull(out);
        assertTrue(out.indexOf("classifier: weka.classifiers.") > -1);
        assertTrue(out.indexOf("Error on training data") > -1);
        assertTrue(out.indexOf("Total Number of Instances              150") > -1);
    }

    @Test
    public void testRSS() throws Exception {
        String[] args = {"-t", "test/iris.arff", "-seed", "1", "-no-cv", "-timeLimit", "1", "-resampling", "RandomSubSampling"};
        String out = Evaluation.evaluateModel(new AutoWEKAClassifier(), args);

        assertNotNull(out);
        assertTrue(out.indexOf("classifier: weka.classifiers.") > -1);
        assertTrue(out.indexOf("Error on training data") > -1);
        assertTrue(out.indexOf("Total Number of Instances              150") > -1);
    }

    @Test
    public void testTH() throws Exception {
        String[] args = {"-t", "test/iris.arff", "-seed", "1", "-no-cv", "-timeLimit", "1", "-resampling", "TerminationHoldout"};
        String out = Evaluation.evaluateModel(new AutoWEKAClassifier(), args);

        assertNotNull(out);
        assertTrue(out.indexOf("classifier: weka.classifiers.") > -1);
        assertTrue(out.indexOf("Error on training data") > -1);
        assertTrue(out.indexOf("Total Number of Instances              150") > -1);
    }

    @Test
    public void testExtraArgs() throws Exception {
        String[] args = {"-t", "test/iris.arff", "-seed", "1", "-no-cv", "-timeLimit", "1", "-extraArgs", "initialIncumbent=RANDOM:acq-func=EI"};
        String out = Evaluation.evaluateModel(new AutoWEKAClassifier(), args);

        assertNotNull(out);
        assertTrue(out.indexOf("classifier: weka.classifiers.") > -1);
        assertTrue(out.indexOf("Error on training data") > -1);
        assertTrue(out.indexOf("Total Number of Instances              150") > -1);
    }
}
