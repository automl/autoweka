/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    SGDText.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.functions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import weka.classifiers.RandomizableClassifier;
import weka.classifiers.UpdateableBatchProcessor;
import weka.classifiers.UpdateableClassifier;
import weka.core.Aggregateable;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.stemmers.NullStemmer;
import weka.core.stemmers.Stemmer;
import weka.core.stopwords.Null;
import weka.core.stopwords.StopwordsHandler;
import weka.core.tokenizers.Tokenizer;
import weka.core.tokenizers.WordTokenizer;

/**
 <!-- globalinfo-start -->
 * Implements stochastic gradient descent for learning a linear binary class SVM or binary class logistic regression on text data. Operates directly (and only) on String attributes. Other types of input attributes are accepted but ignored during training and classification.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -F
 *  Set the loss function to minimize. 0 = hinge loss (SVM), 1 = log loss (logistic regression)
 *  (default = 0)</pre>
 * 
 * <pre> -outputProbs
 *  Output probabilities for SVMs (fits a logsitic
 *  model to the output of the SVM)</pre>
 * 
 * <pre> -L
 *  The learning rate (default = 0.01).</pre>
 * 
 * <pre> -R &lt;double&gt;
 *  The lambda regularization constant (default = 0.0001)</pre>
 * 
 * <pre> -E &lt;integer&gt;
 *  The number of epochs to perform (batch learning only, default = 500)</pre>
 * 
 * <pre> -W
 *  Use word frequencies instead of binary bag of words.</pre>
 * 
 * <pre> -P &lt;# instances&gt;
 *  How often to prune the dictionary of low frequency words (default = 0, i.e. don't prune)</pre>
 * 
 * <pre> -M &lt;double&gt;
 *  Minimum word frequency. Words with less than this frequence are ignored.
 *  If periodic pruning is turned on then this is also used to determine which
 *  words to remove from the dictionary (default = 3).</pre>
 * 
 * <pre> -min-coeff &lt;double&gt;
 *  Minimum absolute value of coefficients in the model.
 *  If periodic pruning is turned on then this
 *  is also used to prune words from the dictionary
 *  (default = 0.001</pre>
 * 
 * <pre> -normalize
 *  Normalize document length (use in conjunction with -norm and -lnorm)</pre>
 * 
 * <pre> -norm &lt;num&gt;
 *  Specify the norm that each instance must have (default 1.0)</pre>
 * 
 * <pre> -lnorm &lt;num&gt;
 *  Specify L-norm to use (default 2.0)</pre>
 * 
 * <pre> -lowercase
 *  Convert all tokens to lowercase before adding to the dictionary.</pre>
 * 
 * <pre> -stopwords-handler
 *  The stopwords handler to use (default Null).</pre>
 * 
 * <pre> -tokenizer &lt;spec&gt;
 *  The tokenizing algorihtm (classname plus parameters) to use.
 *  (default: weka.core.tokenizers.WordTokenizer)</pre>
 * 
 * <pre> -stemmer &lt;spec&gt;
 *  The stemmering algorihtm (classname plus parameters) to use.</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 * 
 <!-- options-end -->
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @author Eibe Frank (eibe{[at]}cs{[dot]}waikato{[dot]}ac{[dot]}nz)
 * 
 */
public class SGDText extends RandomizableClassifier implements
  UpdateableClassifier, UpdateableBatchProcessor, 
  WeightedInstancesHandler, Aggregateable<SGDText> {

  /** For serialization */
  private static final long serialVersionUID = 7200171484002029584L;

  public static class Count implements Serializable {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 2104201532017340967L;

    public double m_count;

    public double m_weight;

    public Count(double c) {
      m_count = c;
    }
  }

  /**
   * The number of training instances at which to periodically prune the
   * dictionary of min frequency words. Empty or null string indicates don't
   * prune
   */
  protected int m_periodicP = 0;

  /**
   * Only consider dictionary words (features) that occur at least this many
   * times.
   */
  protected double m_minWordP = 3;

  /**
   * Prune terms from the model that have a coefficient smaller than this.
   */
  protected double m_minAbsCoefficient = 0.001;

  /** Use word frequencies rather than bag-of-words if true */
  protected boolean m_wordFrequencies = false;

  /** Whether to normalized document length or not */
  protected boolean m_normalize = false;

  /** The length that each document vector should have in the end */
  protected double m_norm = 1.0;

  /** The L-norm to use */
  protected double m_lnorm = 2.0;

  /** The dictionary (and term weights) */
  protected LinkedHashMap<String, Count> m_dictionary;

  /** Stopword handler to use. */
  protected StopwordsHandler m_StopwordsHandler = new Null();

  /** The tokenizer to use */
  protected Tokenizer m_tokenizer = new WordTokenizer();

  /** Whether or not to convert all tokens to lowercase */
  protected boolean m_lowercaseTokens;

  /** The stemming algorithm. */
  protected Stemmer m_stemmer = new NullStemmer();

  /** The regularization parameter */
  protected double m_lambda = 0.0001;

  /** The learning rate */
  protected double m_learningRate = 0.01;

  /** Holds the current iteration number */
  protected double m_t;

  /** Holds the bias term */
  protected double m_bias;

  /** The number of training instances */
  protected double m_numInstances;

  /** The header of the training data */
  protected Instances m_data;

  /**
   * The number of epochs to perform (batch learning). Total iterations is
   * m_epochs * num instances
   */
  protected int m_epochs = 500;

  /**
   * Holds the current document vector (LinkedHashMap is more efficient when
   * iterating over EntrySet than HashMap)
   */
  protected transient LinkedHashMap<String, Count> m_inputVector;

  /** the hinge loss function. */
  public static final int HINGE = 0;

  /** the log loss function. */
  public static final int LOGLOSS = 1;

  /** The current loss function to minimize */
  protected int m_loss = HINGE;

  /** Loss functions to choose from */
  public static final Tag[] TAGS_SELECTION = {
    new Tag(HINGE, "Hinge loss (SVM)"),
    new Tag(LOGLOSS, "Log loss (logistic regression)") };

  /** Used for producing probabilities for SVM via SGD logistic regression */
  protected SGD m_svmProbs;

  /**
   * True if a logistic regression is to be fit to the output of the SVM for
   * producing probability estimates
   */
  protected boolean m_fitLogistic = false;
  protected Instances m_fitLogisticStructure;

  protected double dloss(double z) {
    if (m_loss == HINGE) {
      return (z < 1) ? 1 : 0;
    } else {
      // log loss
      if (z < 0) {
        return 1.0 / (Math.exp(z) + 1.0);
      } else {
        double t = Math.exp(-z);
        return t / (t + 1);
      }
    }
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.STRING_ATTRIBUTES);
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    result.enable(Capability.BINARY_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * the stemming algorithm to use, null means no stemming at all (i.e., the
   * NullStemmer is used).
   * 
   * @param value the configured stemming algorithm, or null
   * @see NullStemmer
   */
  public void setStemmer(Stemmer value) {
    if (value != null) {
      m_stemmer = value;
    } else {
      m_stemmer = new NullStemmer();
    }
  }

  /**
   * Returns the current stemming algorithm, null if none is used.
   * 
   * @return the current stemming algorithm, null if none set
   */
  public Stemmer getStemmer() {
    return m_stemmer;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String stemmerTipText() {
    return "The stemming algorithm to use on the words.";
  }

  /**
   * the tokenizer algorithm to use.
   * 
   * @param value the configured tokenizing algorithm
   */
  public void setTokenizer(Tokenizer value) {
    m_tokenizer = value;
  }

  /**
   * Returns the current tokenizer algorithm.
   * 
   * @return the current tokenizer algorithm
   */
  public Tokenizer getTokenizer() {
    return m_tokenizer;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String tokenizerTipText() {
    return "The tokenizing algorithm to use on the strings.";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String useWordFrequenciesTipText() {
    return "Use word frequencies rather than binary "
      + "bag of words representation";
  }

  /**
   * Set whether to use word frequencies rather than binary bag of words
   * representation.
   * 
   * @param u true if word frequencies are to be used.
   */
  public void setUseWordFrequencies(boolean u) {
    m_wordFrequencies = u;
  }

  /**
   * Get whether to use word frequencies rather than binary bag of words
   * representation.
   * 
   * @return true if word frequencies are to be used.
   */
  public boolean getUseWordFrequencies() {
    return m_wordFrequencies;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String lowercaseTokensTipText() {
    return "Whether to convert all tokens to lowercase";
  }

  /**
   * Set whether to convert all tokens to lowercase
   * 
   * @param l true if all tokens are to be converted to lowercase
   */
  public void setLowercaseTokens(boolean l) {
    m_lowercaseTokens = l;
  }

  /**
   * Get whether to convert all tokens to lowercase
   * 
   * @return true true if all tokens are to be converted to lowercase
   */
  public boolean getLowercaseTokens() {
    return m_lowercaseTokens;
  }

  /**
   * Sets the stopwords handler to use.
   * 
   * @param value the stopwords handler, if null, Null is used
   */
  public void setStopwordsHandler(StopwordsHandler value) {
    if (value != null) {
      m_StopwordsHandler = value;
    } else {
      m_StopwordsHandler = new Null();
    }
  }

  /**
   * Gets the stopwords handler.
   * 
   * @return the stopwords handler
   */
  public StopwordsHandler getStopwordsHandler() {
    return m_StopwordsHandler;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String stopwordsHandlerTipText() {
    return "The stopwords handler to use (Null means no stopwords are used).";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String periodicPruningTipText() {
    return "How often (number of instances) to prune "
      + "the dictionary of low frequency terms. "
      + "0 means don't prune. Setting a positive "
      + "integer n means prune after every n instances";
  }

  /**
   * Set how often to prune the dictionary
   * 
   * @param p how often to prune
   */
  public void setPeriodicPruning(int p) {
    m_periodicP = p;
  }

  /**
   * Get how often to prune the dictionary
   * 
   * @return how often to prune the dictionary
   */
  public int getPeriodicPruning() {
    return m_periodicP;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String minWordFrequencyTipText() {
    return "Ignore any words that don't occur at least "
      + "min frequency times in the training data. If periodic "
      + "pruning is turned on, then the dictionary is pruned "
      + "according to this value";

  }

  /**
   * Set the minimum word frequency. Words that don't occur at least min freq
   * times are ignored when updating weights. If periodic pruning is turned on,
   * then min frequency is used when removing words from the dictionary.
   * 
   * @param minFreq the minimum word frequency to use
   */
  public void setMinWordFrequency(double minFreq) {
    m_minWordP = minFreq;
  }

  /**
   * Get the minimum word frequency. Words that don't occur at least min freq
   * times are ignored when updating weights. If periodic pruning is turned on,
   * then min frequency is used when removing words from the dictionary.
   * 
   * @return the minimum word frequency to use
   */
  public double getMinWordFrequency() {
    return m_minWordP;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String minAbsoluteCoefficientValueTipText() {
    return "The minimum absolute magnitude for model coefficients. Terms "
      + "with weights smaller than this value are ignored. If periodic "
      + "pruning is turned on then this is also used to determine if a "
      + "word should be removed from the dictionary.";
  }

  /**
   * Set the minimum absolute magnitude for model coefficients. Terms with
   * weights smaller than this value are ignored. If periodic pruning is turned
   * on then this is also used to determine if a word should be removed from the
   * dictionary
   * 
   * @param minCoeff the minimum absolute value of a model coefficient
   */
  public void setMinAbsoluteCoefficientValue(double minCoeff) {
    m_minAbsCoefficient = minCoeff;
  }

  /**
   * Get the minimum absolute magnitude for model coefficients. Terms with
   * weights smaller than this value are ignored. If periodic pruning is turned
   * on this then is also used to determine if a word should be removed from the
   * dictionary
   * 
   * @return the minimum absolute value of a model coefficient
   */
  public double getMinAbsoluteCoefficientValue() {
    return m_minAbsCoefficient;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String normalizeDocLengthTipText() {
    return "If true then document length is normalized according "
      + "to the settings for norm and lnorm";
  }

  /**
   * Set whether to normalize the length of each document
   * 
   * @param norm true if document lengths is to be normalized
   */
  public void setNormalizeDocLength(boolean norm) {
    m_normalize = norm;
  }

  /**
   * Get whether to normalize the length of each document
   * 
   * @return true if document lengths is to be normalized
   */
  public boolean getNormalizeDocLength() {
    return m_normalize;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String normTipText() {
    return "The norm of the instances after normalization.";
  }

  /**
   * Get the instance's Norm.
   * 
   * @return the Norm
   */
  public double getNorm() {
    return m_norm;
  }

  /**
   * Set the norm of the instances
   * 
   * @param newNorm the norm to wich the instances must be set
   */
  public void setNorm(double newNorm) {
    m_norm = newNorm;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String LNormTipText() {
    return "The LNorm to use for document length normalization.";
  }

  /**
   * Get the L Norm used.
   * 
   * @return the L-norm used
   */
  public double getLNorm() {
    return m_lnorm;
  }

  /**
   * Set the L-norm to used
   * 
   * @param newLNorm the L-norm
   */
  public void setLNorm(double newLNorm) {
    m_lnorm = newLNorm;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String lambdaTipText() {
    return "The regularization constant. (default = 0.0001)";
  }

  /**
   * Set the value of lambda to use
   * 
   * @param lambda the value of lambda to use
   */
  public void setLambda(double lambda) {
    m_lambda = lambda;
  }

  /**
   * Get the current value of lambda
   * 
   * @return the current value of lambda
   */
  public double getLambda() {
    return m_lambda;
  }

  /**
   * Set the learning rate.
   * 
   * @param lr the learning rate to use.
   */
  public void setLearningRate(double lr) {
    m_learningRate = lr;
  }

  /**
   * Get the learning rate.
   * 
   * @return the learning rate
   */
  public double getLearningRate() {
    return m_learningRate;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String learningRateTipText() {
    return "The learning rate.";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String epochsTipText() {
    return "The number of epochs to perform (batch learning). "
      + "The total number of iterations is epochs * num" + " instances.";
  }

  /**
   * Set the number of epochs to use
   * 
   * @param e the number of epochs to use
   */
  public void setEpochs(int e) {
    m_epochs = e;
  }

  /**
   * Get current number of epochs
   * 
   * @return the current number of epochs
   */
  public int getEpochs() {
    return m_epochs;
  }

  /**
   * Set the loss function to use.
   * 
   * @param function the loss function to use.
   */
  public void setLossFunction(SelectedTag function) {
    if (function.getTags() == TAGS_SELECTION) {
      m_loss = function.getSelectedTag().getID();
    }
  }

  /**
   * Get the current loss function.
   * 
   * @return the current loss function.
   */
  public SelectedTag getLossFunction() {
    return new SelectedTag(m_loss, TAGS_SELECTION);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String lossFunctionTipText() {
    return "The loss function to use. Hinge loss (SVM), "
      + "log loss (logistic regression) or " + "squared loss (regression).";
  }

  /**
   * Set whether to fit a logistic regression (itself trained using SGD) to the
   * outputs of the SVM (if an SVM is being learned).
   * 
   * @param o true if a logistic regression is to be fit to the output of the
   *          SVM to produce probability estimates.
   */
  public void setOutputProbsForSVM(boolean o) {
    m_fitLogistic = o;
  }

  /**
   * Get whether to fit a logistic regression (itself trained using SGD) to the
   * outputs of the SVM (if an SVM is being learned).
   * 
   * @return true if a logistic regression is to be fit to the output of the SVM
   *         to produce probability estimates.
   */
  public boolean getOutputProbsForSVM() {
    return m_fitLogistic;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String outputProbsForSVMTipText() {
    return "Fit a logistic regression to the output of SVM for "
      + "producing probability estimates";
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>();
    newVector.add(new Option("\tSet the loss function to minimize. 0 = "
      + "hinge loss (SVM), 1 = log loss (logistic regression)\n\t"
      + "(default = 0)", "F", 1, "-F"));
    newVector
      .add(new Option("\tOutput probabilities for SVMs (fits a logsitic\n\t"
        + "model to the output of the SVM)", "output-probs", 0, "-outputProbs"));
    newVector.add(new Option("\tThe learning rate (default = 0.01).", "L", 1,
      "-L"));
    newVector.add(new Option("\tThe lambda regularization constant "
      + "(default = 0.0001)", "R", 1, "-R <double>"));
    newVector.add(new Option("\tThe number of epochs to perform ("
      + "batch learning only, default = 500)", "E", 1, "-E <integer>"));
    newVector.add(new Option("\tUse word frequencies instead of "
      + "binary bag of words.", "W", 0, "-W"));
    newVector.add(new Option("\tHow often to prune the dictionary "
      + "of low frequency words (default = 0, i.e. don't prune)", "P", 1,
      "-P <# instances>"));
    newVector.add(new Option("\tMinimum word frequency. Words with less "
      + "than this frequence are ignored.\n\tIf periodic pruning "
      + "is turned on then this is also used to determine which\n\t"
      + "words to remove from the dictionary (default = 3).", "M", 1,
      "-M <double>"));

    newVector.add(new Option("\tMinimum absolute value of coefficients " +
      "in the model.\n\tIf periodic pruning is turned on then this\n\t"
      + "is also used to prune words from the dictionary\n\t"
      + "(default = 0.001", "min-coeff", 1, "-min-coeff <double>"));

    newVector.addElement(new Option(
      "\tNormalize document length (use in conjunction with -norm and "
        + "-lnorm)", "normalize", 0, "-normalize"));
    newVector.addElement(new Option(
      "\tSpecify the norm that each instance must have (default 1.0)", "norm",
      1, "-norm <num>"));
    newVector.addElement(new Option("\tSpecify L-norm to use (default 2.0)",
      "lnorm", 1, "-lnorm <num>"));
    newVector.addElement(new Option("\tConvert all tokens to lowercase "
      + "before adding to the dictionary.", "lowercase", 0, "-lowercase"));
    newVector.addElement(new Option(
      "\tThe stopwords handler to use (default Null).",
      "-stopwords-handler", 1, "-stopwords-handler"));
    newVector.addElement(new Option(
      "\tThe tokenizing algorihtm (classname plus parameters) to use.\n"
        + "\t(default: " + WordTokenizer.class.getName() + ")", "tokenizer", 1,
      "-tokenizer <spec>"));
    newVector.addElement(new Option(
      "\tThe stemmering algorihtm (classname plus parameters) to use.",
      "stemmer", 1, "-stemmer <spec>"));

    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -F
   *  Set the loss function to minimize. 0 = hinge loss (SVM), 1 = log loss (logistic regression)
   *  (default = 0)</pre>
   * 
   * <pre> -outputProbs
   *  Output probabilities for SVMs (fits a logsitic
   *  model to the output of the SVM)</pre>
   * 
   * <pre> -L
   *  The learning rate (default = 0.01).</pre>
   * 
   * <pre> -R &lt;double&gt;
   *  The lambda regularization constant (default = 0.0001)</pre>
   * 
   * <pre> -E &lt;integer&gt;
   *  The number of epochs to perform (batch learning only, default = 500)</pre>
   * 
   * <pre> -W
   *  Use word frequencies instead of binary bag of words.</pre>
   * 
   * <pre> -P &lt;# instances&gt;
   *  How often to prune the dictionary of low frequency words (default = 0, i.e. don't prune)</pre>
   * 
   * <pre> -M &lt;double&gt;
   *  Minimum word frequency. Words with less than this frequence are ignored.
   *  If periodic pruning is turned on then this is also used to determine which
   *  words to remove from the dictionary (default = 3).</pre>
   * 
   * <pre> -min-coeff &lt;double&gt;
   *  Minimum absolute value of coefficients in the model.
   *  If periodic pruning is turned on then this
   *  is also used to prune words from the dictionary
   *  (default = 0.001</pre>
   * 
   * <pre> -normalize
   *  Normalize document length (use in conjunction with -norm and -lnorm)</pre>
   * 
   * <pre> -norm &lt;num&gt;
   *  Specify the norm that each instance must have (default 1.0)</pre>
   * 
   * <pre> -lnorm &lt;num&gt;
   *  Specify L-norm to use (default 2.0)</pre>
   * 
   * <pre> -lowercase
   *  Convert all tokens to lowercase before adding to the dictionary.</pre>
   * 
   * <pre> -stopwords-handler
   *  The stopwords handler to use (default Null).</pre>
   * 
   * <pre> -tokenizer &lt;spec&gt;
   *  The tokenizing algorihtm (classname plus parameters) to use.
   *  (default: weka.core.tokenizers.WordTokenizer)</pre>
   * 
   * <pre> -stemmer &lt;spec&gt;
   *  The stemmering algorihtm (classname plus parameters) to use.</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)</pre>
   * 
   * <pre> -output-debug-info
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -do-not-check-capabilities
   *  If set, classifier capabilities are not checked before classifier is built
   *  (use with caution).</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    reset();

    String lossString = Utils.getOption('F', options);
    if (lossString.length() != 0) {
      setLossFunction(new SelectedTag(Integer.parseInt(lossString),
        TAGS_SELECTION));
    }

    setOutputProbsForSVM(Utils.getFlag("output-probs", options));

    String lambdaString = Utils.getOption('R', options);
    if (lambdaString.length() > 0) {
      setLambda(Double.parseDouble(lambdaString));
    }

    String learningRateString = Utils.getOption('L', options);
    if (learningRateString.length() > 0) {
      setLearningRate(Double.parseDouble(learningRateString));
    }

    String epochsString = Utils.getOption("E", options);
    if (epochsString.length() > 0) {
      setEpochs(Integer.parseInt(epochsString));
    }

    setUseWordFrequencies(Utils.getFlag("W", options));

    String pruneFreqS = Utils.getOption("P", options);
    if (pruneFreqS.length() > 0) {
      setPeriodicPruning(Integer.parseInt(pruneFreqS));
    }
    String minFreq = Utils.getOption("M", options);
    if (minFreq.length() > 0) {
      setMinWordFrequency(Double.parseDouble(minFreq));
    }

    String minCoeff = Utils.getOption("min-coeff", options);
    if (minCoeff.length() > 0) {
      setMinAbsoluteCoefficientValue(Double.parseDouble(minCoeff));
    }

    setNormalizeDocLength(Utils.getFlag("normalize", options));

    String normFreqS = Utils.getOption("norm", options);
    if (normFreqS.length() > 0) {
      setNorm(Double.parseDouble(normFreqS));
    }
    String lnormFreqS = Utils.getOption("lnorm", options);
    if (lnormFreqS.length() > 0) {
      setLNorm(Double.parseDouble(lnormFreqS));
    }

    setLowercaseTokens(Utils.getFlag("lowercase", options));

    String stemmerString = Utils.getOption("stemmer", options);
    if (stemmerString.length() == 0) {
      setStemmer(null);
    } else {
      String[] stemmerSpec = Utils.splitOptions(stemmerString);
      if (stemmerSpec.length == 0) {
        throw new Exception("Invalid stemmer specification string");
      }
      String stemmerName = stemmerSpec[0];
      stemmerSpec[0] = "";
      Stemmer stemmer = (Stemmer) Utils.forName(Class.forName("weka.core.stemmers.Stemmer"), stemmerName, stemmerSpec);
      setStemmer(stemmer);
    }

    String stopwordsHandlerString = Utils.getOption("stopwords-handler", options);
    if (stopwordsHandlerString.length() == 0) {
      setStopwordsHandler(null);
    } else {
      String[] stopwordsHandlerSpec = Utils.splitOptions(stopwordsHandlerString);
      if (stopwordsHandlerSpec.length == 0) {
        throw new Exception("Invalid StopwordsHandler specification string");
      }
      String stopwordsHandlerName = stopwordsHandlerSpec[0];
      stopwordsHandlerSpec[0] = "";
      StopwordsHandler stopwordsHandler =
              (StopwordsHandler) Utils.forName(Class.forName("weka.core.stopwords.StopwordsHandler"),
                      stopwordsHandlerName, stopwordsHandlerSpec);
      setStopwordsHandler(stopwordsHandler);
    }

    String tokenizerString = Utils.getOption("tokenizer", options);
    if (tokenizerString.length() == 0) {
      setTokenizer(new WordTokenizer());
    } else {
      String[] tokenizerSpec = Utils.splitOptions(tokenizerString);
      if (tokenizerSpec.length == 0) {
        throw new Exception("Invalid tokenizer specification string");
      }
      String tokenizerName = tokenizerSpec[0];
      tokenizerSpec[0] = "";
      Tokenizer tokenizer = (Tokenizer) Utils.forName(Class.forName("weka.core.tokenizers.Tokenizer"), tokenizerName,
              tokenizerSpec);
      setTokenizer(tokenizer);
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    ArrayList<String> options = new ArrayList<String>();

    options.add("-F");
    options.add("" + getLossFunction().getSelectedTag().getID());
    if (getOutputProbsForSVM()) {
      options.add("-output-probs");
    }
    options.add("-L");
    options.add("" + getLearningRate());
    options.add("-R");
    options.add("" + getLambda());
    options.add("-E");
    options.add("" + getEpochs());
    if (getUseWordFrequencies()) {
      options.add("-W");
    }
    options.add("-P");
    options.add("" + getPeriodicPruning());
    options.add("-M");
    options.add("" + getMinWordFrequency());

    options.add("-min-coeff");
    options.add("" + getMinAbsoluteCoefficientValue());

    if (getNormalizeDocLength()) {
      options.add("-normalize");
    }
    options.add("-norm");
    options.add("" + getNorm());
    options.add("-lnorm");
    options.add("" + getLNorm());
    if (getLowercaseTokens()) {
      options.add("-lowercase");
    }

    if (getStopwordsHandler() != null) {
      options.add("-stopwords-handler");
      String spec = getStopwordsHandler().getClass().getName();
      if (getStopwordsHandler() instanceof OptionHandler) {
        spec +=
          " "
            + Utils.joinOptions(((OptionHandler) getStopwordsHandler())
              .getOptions());
      }
      options.add(spec.trim());
    }

    options.add("-tokenizer");
    String spec = getTokenizer().getClass().getName();
    if (getTokenizer() instanceof OptionHandler) {
      spec += " "
        + Utils.joinOptions(((OptionHandler) getTokenizer()).getOptions());
    }
    options.add(spec.trim());

    if (getStemmer() != null) {
      options.add("-stemmer");
      spec = getStemmer().getClass().getName();
      if (getStemmer() instanceof OptionHandler) {
        spec += " "
          + Utils.joinOptions(((OptionHandler) getStemmer()).getOptions());
      }

      options.add(spec.trim());
    }

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[1]);
  }

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {
    return "Implements stochastic gradient descent for learning"
      + " a linear binary class SVM or binary class"
      + " logistic regression on text data. Operates directly (and only) "
      + "on String attributes. Other types of input attributes are accepted "
      + "but ignored during training and classification.";
  }

  /**
   * Reset the classifier.
   */
  public void reset() {
    m_t = 1;
    m_bias = 0;
    m_dictionary = null;
  }

  /**
   * Method for building the classifier.
   * 
   * @param data the set of training instances.
   * @throws Exception if the classifier can't be built successfully.
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {
    reset();

    /*
     * boolean hasString = false; for (int i = 0; i < data.numAttributes(); i++)
     * { if (data.attribute(i).isString() && data.classIndex() != i) { hasString
     * = true; break; } }
     * 
     * if (!hasString) { throw new
     * Exception("Incoming data does not have any string attributes!"); }
     */

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    m_dictionary = new LinkedHashMap<String, Count>(10000);

    m_numInstances = data.numInstances();
    m_data = new Instances(data, 0);
    data = new Instances(data);

    if (m_fitLogistic && m_loss == HINGE) {
      initializeSVMProbs(data);
    }

    if (data.numInstances() > 0) {
      data.randomize(new Random(getSeed()));
      train(data);
      pruneDictionary(true);
    }
  }

  protected void initializeSVMProbs(Instances data) throws Exception {
    m_svmProbs = new SGD();
    m_svmProbs.setLossFunction(new SelectedTag(SGD.LOGLOSS, TAGS_SELECTION));
    m_svmProbs.setLearningRate(m_learningRate);
    m_svmProbs.setLambda(m_lambda);
    m_svmProbs.setEpochs(m_epochs);
    ArrayList<Attribute> atts = new ArrayList<Attribute>(2);
    atts.add(new Attribute("pred"));
    ArrayList<String> attVals = new ArrayList<String>(2);
    attVals.add(data.classAttribute().value(0));
    attVals.add(data.classAttribute().value(1));
    atts.add(new Attribute("class", attVals));
    m_fitLogisticStructure = new Instances("data", atts, 0);
    m_fitLogisticStructure.setClassIndex(1);

    m_svmProbs.buildClassifier(m_fitLogisticStructure);
  }

  protected void train(Instances data) throws Exception {
    for (int e = 0; e < m_epochs; e++) {
      for (int i = 0; i < data.numInstances(); i++) {
        if (e == 0) {
          updateClassifier(data.instance(i), true);
        } else {
          updateClassifier(data.instance(i), false);
        }
      }
    }
  }

  /**
   * Updates the classifier with the given instance.
   * 
   * @param instance the new training instance to include in the model
   * @exception Exception if the instance could not be incorporated in the
   *              model.
   */
  @Override
  public void updateClassifier(Instance instance) throws Exception {
    updateClassifier(instance, true);
  }

  protected void updateClassifier(Instance instance, boolean updateDictionary)
    throws Exception {

    if (!instance.classIsMissing()) {

      // tokenize
      tokenizeInstance(instance, updateDictionary);

      // make a meta instance for the logistic model before we update
      // the SVM
      if (m_loss == HINGE && m_fitLogistic) {
        double pred = svmOutput();
        double[] vals = new double[2];
        vals[0] = pred;
        vals[1] = instance.classValue();
        DenseInstance metaI = new DenseInstance(instance.weight(), vals);
        metaI.setDataset(m_fitLogisticStructure);
        m_svmProbs.updateClassifier(metaI);
      }

      // ---
      double wx = dotProd(m_inputVector);
      double y = (instance.classValue() == 0) ? -1 : 1;
      double z = y * (wx + m_bias);

      // Compute multiplier for weight decay
      double multiplier = 1.0;
      if (m_numInstances == 0) {
        multiplier = 1.0 - (m_learningRate * m_lambda) / m_t;
      } else {
        multiplier = 1.0 - (m_learningRate * m_lambda) / m_numInstances;
      }

      for (Map.Entry<String, Count> c : m_dictionary.entrySet()) {
        c.getValue().m_weight *= multiplier;
      }

      // Only need to do the following if the loss is non-zero
      if (m_loss != HINGE || (z < 1)) {
        // Compute Factor for updates
        double dloss = dloss(z);
        double factor = m_learningRate * y * dloss;

        // Update coefficients for attributes
        for (Map.Entry<String, Count> feature : m_inputVector.entrySet()) {
          String word = feature.getKey();
          double value = (m_wordFrequencies) ? feature.getValue().m_count : 1;

          Count c = m_dictionary.get(word);
          if (c != null) {
            c.m_weight += factor * value;
          }
        }

        // update the bias
        m_bias += factor;
      }

      m_t++;
    }
  }

  protected void tokenizeInstance(Instance instance, boolean updateDictionary) {
    if (m_inputVector == null) {
      m_inputVector = new LinkedHashMap<String, Count>();
    } else {
      m_inputVector.clear();
    }

    for (int i = 0; i < instance.numAttributes(); i++) {
      if (instance.attribute(i).isString() && !instance.isMissing(i)) {
        m_tokenizer.tokenize(instance.stringValue(i));

        while (m_tokenizer.hasMoreElements()) {
          String word = m_tokenizer.nextElement();
          if (m_lowercaseTokens) {
            word = word.toLowerCase();
          }

          word = m_stemmer.stem(word);

          if (m_StopwordsHandler.isStopword(word)) {
            continue;
          }

          Count docCount = m_inputVector.get(word);
          if (docCount == null) {
            m_inputVector.put(word, new Count(instance.weight()));
          } else {
            docCount.m_count += instance.weight();
          }

          if (updateDictionary) {
            Count count = m_dictionary.get(word);
            if (count == null) {
              m_dictionary.put(word, new Count(instance.weight()));
            } else {
              count.m_count += instance.weight();
            }
          }

        }
      }
    }

    if (updateDictionary) {
      pruneDictionary(false);
    }
  }

  protected void pruneDictionary(boolean force) {
    if ((m_periodicP <= 0 || m_t % m_periodicP > 0) && !force) {
      return;
    }

    Iterator<Map.Entry<String, Count>> entries = m_dictionary.entrySet()
      .iterator();
    while (entries.hasNext()) {
      Map.Entry<String, Count> entry = entries.next();
      if (entry.getValue().m_count < m_minWordP
        || Math.abs(entry.getValue().m_weight) < m_minAbsCoefficient) {
        entries.remove();
      }
    }
  }

  protected double svmOutput() {
    double wx = dotProd(m_inputVector);
    double z = (wx + m_bias);

    return z;
  }

  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {
    double[] result = new double[2];

    tokenizeInstance(inst, false);
    double wx = dotProd(m_inputVector);
    double z = (wx + m_bias);

    if (m_loss == HINGE && m_fitLogistic) {
      double pred = z;
      double[] vals = new double[2];
      vals[0] = pred;
      vals[1] = Utils.missingValue();
      DenseInstance metaI = new DenseInstance(inst.weight(), vals);
      metaI.setDataset(m_fitLogisticStructure);
      return m_svmProbs.distributionForInstance(metaI);
    }

    if (z <= 0) {
      if (m_loss == LOGLOSS) {
        result[0] = 1.0 / (1.0 + Math.exp(z));
        result[1] = 1.0 - result[0];
      } else {
        result[0] = 1;
      }
    } else {
      if (m_loss == LOGLOSS) {
        result[1] = 1.0 / (1.0 + Math.exp(-z));
        result[0] = 1.0 - result[1];
      } else {
        result[1] = 1;
      }
    }

    return result;
  }

  protected double dotProd(Map<String, Count> document) {
    double result = 0;

    // document normalization
    double iNorm = 0;
    double fv = 0;
    if (m_normalize) {
      for (Count c : document.values()) {
        // word counts or bag-of-words?
        fv = (m_wordFrequencies) ? c.m_count : 1.0;
        iNorm += Math.pow(Math.abs(fv), m_lnorm);
      }
      iNorm = Math.pow(iNorm, 1.0 / m_lnorm);
    }

    for (Map.Entry<String, Count> feature : document.entrySet()) {
      String word = feature.getKey();
      double freq = (m_wordFrequencies) ? feature.getValue().m_count : 1.0;
      // double freq = (feature.getValue().m_count / iNorm * m_norm);
      if (m_normalize) {
        freq /= iNorm * m_norm;
      }

      Count weight = m_dictionary.get(word);

      if (weight != null && weight.m_count >= m_minWordP
        && Math.abs(weight.m_weight) >= m_minAbsCoefficient) {
        result += freq * weight.m_weight;
      }
    }

    return result;
  }

  @Override
  public String toString() {
    if (m_dictionary == null) {
      return "SGDText: No model built yet.\n";
    }

    StringBuffer buff = new StringBuffer();
    buff.append("SGDText:\n\n");
    buff.append("Loss function: ");
    if (m_loss == HINGE) {
      buff.append("Hinge loss (SVM)\n\n");
    } else {
      buff.append("Log loss (logistic regression)\n\n");
    }

    int dictSize = 0;
    Iterator<Map.Entry<String, Count>> entries = m_dictionary.entrySet()
      .iterator();
    while (entries.hasNext()) {
      Map.Entry<String, Count> entry = entries.next();
      if (entry.getValue().m_count >= m_minWordP
        && Math.abs(entry.getValue().m_weight) >= m_minAbsCoefficient) {
        dictSize++;
      }
    }

    buff.append("Dictionary size: " + dictSize + "\n\n");

    buff.append(m_data.classAttribute().name() + " = \n\n");
    int printed = 0;

    entries = m_dictionary.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry<String, Count> entry = entries.next();

      if (entry.getValue().m_count >= m_minWordP
        && Math.abs(entry.getValue().m_weight) >= m_minAbsCoefficient) {
        if (printed > 0) {
          buff.append(" + ");
        } else {
          buff.append("   ");
        }

        buff.append(Utils.doubleToString(entry.getValue().m_weight, 12, 4)
          + " " + entry.getKey() + " " + entry.getValue().m_count + "\n");
        printed++;
      }
    }

    if (m_bias > 0) {
      buff.append(" + " + Utils.doubleToString(m_bias, 12, 4));
    } else {
      buff.append(" - " + Utils.doubleToString(-m_bias, 12, 4));
    }

    return buff.toString();
  }

  /**
   * Get this model's dictionary (including term weights).
   * 
   * @return this model's dictionary.
   */
  public LinkedHashMap<String, Count> getDictionary() {
    return m_dictionary;
  }

  /**
   * Return the size of the dictionary (minus any low frequency terms that are
   * below the threshold but haven't been pruned yet).
   * 
   * @return the size of the dictionary.
   */
  public int getDictionarySize() {
    int size = 0;
    if (m_dictionary != null) {
      Iterator<Map.Entry<String, Count>> entries = m_dictionary.entrySet()
        .iterator();
      while (entries.hasNext()) {
        Map.Entry<String, Count> entry = entries.next();
        if (entry.getValue().m_count >= m_minWordP
          && Math.abs(entry.getValue().m_weight) >= m_minAbsCoefficient) {
          size++;
        }
      }
    }

    return size;
  }

  public double bias() {
    return m_bias;
  }

  public void setBias(double bias) {
    m_bias = bias;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12049 $");
  }

  protected int m_numModels = 0;

  /**
   * Aggregate an object with this one
   * 
   * @param toAggregate the object to aggregate
   * @return the result of aggregation
   * @throws Exception if the supplied object can't be aggregated for some
   *           reason
   */
  @Override
  public SGDText aggregate(SGDText toAggregate) throws Exception {

    if (m_dictionary == null) {
      throw new Exception("No model built yet, can't aggregate");
    }
    LinkedHashMap<String, SGDText.Count> tempDict = toAggregate.getDictionary();

    Iterator<Map.Entry<String, SGDText.Count>> entries = tempDict.entrySet()
      .iterator();
    while (entries.hasNext()) {
      Map.Entry<String, SGDText.Count> entry = entries.next();

      Count masterCount = m_dictionary.get(entry.getKey());
      if (masterCount == null) {
        // we havent seen this term (or it's been pruned)
        masterCount = new Count(entry.getValue().m_count);
        masterCount.m_weight = entry.getValue().m_weight;
        m_dictionary.put(entry.getKey(), masterCount);
      } else {
        // add up
        masterCount.m_count += entry.getValue().m_count;
        masterCount.m_weight += entry.getValue().m_weight;

      }
    }

    m_bias += toAggregate.bias();

    m_numModels++;

    return this;
  }

  /**
   * Call to complete the aggregation process. Allows implementers to do any
   * final processing based on how many objects were aggregated.
   * 
   * @throws Exception if the aggregation can't be finalized for some reason
   */
  @Override
  public void finalizeAggregation() throws Exception {

    if (m_numModels == 0) {
      throw new Exception("Unable to finalize aggregation - "
        + "haven't seen any models to aggregate");
    }
    
    pruneDictionary(true);

    Iterator<Map.Entry<String, SGDText.Count>> entries = m_dictionary
      .entrySet().iterator();

    while (entries.hasNext()) {
      Map.Entry<String, Count> entry = entries.next();
      entry.getValue().m_count /= (m_numModels + 1); // plus one for us
      entry.getValue().m_weight /= (m_numModels + 1);
    }

    m_bias /= (m_numModels + 1);

    // aggregation complete
    m_numModels = 0;
  }
  
  @Override
  public void batchFinished() throws Exception {
    pruneDictionary(true);
  }

  /**
   * Main method for testing this class.
   */
  public static void main(String[] args) {
    runClassifier(new SGDText(), args);
  }
}

