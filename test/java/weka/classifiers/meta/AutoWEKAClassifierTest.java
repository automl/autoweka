package weka.classifiers.meta;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.meta.AutoWEKAClassifier;

import org.junit.Test;

public class AutoWEKAClassifierTest {
    @Test
    public void testClassifier() {
        String[] args = {"-t", "test/iris.arff", "-no-cv"};
        AbstractClassifier.runClassifier(new AutoWEKAClassifier(), args);
    }
}
