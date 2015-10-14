package weka.classifiers.meta;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.core.Capabilities;
import weka.core.Drawable;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.io.Serializable;

public class AutoWEKAClassifier extends AbstractClassifier {

    /** for serialization */
    static final long serialVersionUID = 2907034203562786373L;

    /* The Chosen One. */
    private Classifier classifier;

    /**
     * Main method for testing this class.
     *
     * @param argv should contain command line options (see setOptions)
     */
    public static void main(String [] argv) {
        runClassifier(new AutoWEKAClassifier(), argv);
    }

    public AutoWEKAClassifier() {
    }

    public void buildClassifier(Instances is) throws Exception {
        getCapabilities().testWithFail(is);
    }

    public double classifyInstance(Instance i) throws Exception {
        return 0;
    }

    /**
     * Returns default capabilities of the classifier.
     *
     * @return      the capabilities of this classifier
     */
    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();
        result.disableAll();

        // attributes
        result.enable(Capability.NOMINAL_ATTRIBUTES);
        result.enable(Capability.NUMERIC_ATTRIBUTES);
        result.enable(Capability.DATE_ATTRIBUTES);
        result.enable(Capability.STRING_ATTRIBUTES);
        result.enable(Capability.RELATIONAL_ATTRIBUTES);
        result.enable(Capability.MISSING_VALUES);

        // class
        result.enable(Capability.NOMINAL_CLASS);
        result.enable(Capability.NUMERIC_CLASS);
        result.enable(Capability.DATE_CLASS);
        result.enable(Capability.MISSING_CLASS_VALUES);

        // instances
        result.setMinimumNumberInstances(1);

        return result;
    }

    /**
     * Returns an instance of a TechnicalInformation object, containing 
     * detailed information about the technical background of this class,
     * e.g., paper reference or book this class is based on.
     * 
     * @return the technical information about this class
     */
    public TechnicalInformation getTechnicalInformation() {
        TechnicalInformation result = new TechnicalInformation(Type.INPROCEEDINGS);
        result.setValue(Field.AUTHOR, "Chris Thornton, Frank Hutter, Holger Hoos, and Kevin Leyton-Brown");
        result.setValue(Field.YEAR, "2013");
        result.setValue(Field.TITLE, "Auto-WEKA: Combined Selection and Hyperparameter Optimization of Classifiaction Algorithms");
        result.setValue(Field.BOOKTITLE, "Proc. of KDD 2013");

        return result;
    }

    /**
     * This will return a string describing the classifier.
     * @return The string.
     */
    public String globalInfo() {
        return "Automatically finds the best model with its best parameter settings for a given classification task.\n\n"
            + "For more information see:\n\n"
            + getTechnicalInformation().toString();
    }

    public String toString() {
        return classifier.toString();
    }
}
