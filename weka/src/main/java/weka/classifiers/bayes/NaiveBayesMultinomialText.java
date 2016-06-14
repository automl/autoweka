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
 *    NaiveBayesMultinomialText.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.bayes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.UpdateableBatchProcessor;
import weka.classifiers.UpdateableClassifier;
import weka.core.Aggregateable;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
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
 * Multinomial naive bayes for text data. Operates directly (and only) on String attributes. Other types of input attributes are accepted but ignored during training and classification
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
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
 * @author Andrew Golightly (acg4@cs.waikato.ac.nz)
 * @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 *
 */
public class NaiveBayesMultinomialText extends AbstractClassifier implements
  UpdateableClassifier, UpdateableBatchProcessor, WeightedInstancesHandler,
  Aggregateable<NaiveBayesMultinomialText> {

  /** For serialization */
  private static final long serialVersionUID = 2139025532014821394L;

  private static class Count implements Serializable {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 2104201532017340967L;

    public double m_count;

    public Count(double c) {
      m_count = c;
    }
  }

  /** The header of the training data */
  protected Instances m_data;

  protected double[] m_probOfClass;
  protected double[] m_wordsPerClass;
  protected Map<Integer, LinkedHashMap<String, Count>> m_probOfWordGivenClass;

  /**
   * Holds the current document vector (LinkedHashMap is more efficient when
   * iterating over EntrySet than HashMap)
   */
  protected transient LinkedHashMap<String, Count> m_inputVector;

  /** Stopword handler to use. */
  protected StopwordsHandler m_StopwordsHandler = new Null();

  /** The tokenizer to use */
  protected Tokenizer m_tokenizer = new WordTokenizer();

  /** Whether or not to convert all tokens to lowercase */
  protected boolean m_lowercaseTokens;

  /** The stemming algorithm. */
  protected Stemmer m_stemmer = new NullStemmer();

  /**
   * The number of training instances at which to periodically prune the
   * dictionary of min frequency words. Empty or null string indicates don't
   * prune
   */
  protected int m_periodicP = 0;

  /**
   * Only consider dictionary words (features) that occur at least this many
   * times
   */
  protected double m_minWordP = 3;

  /** Use word frequencies rather than bag-of-words if true */
  protected boolean m_wordFrequencies = false;

  /** normailize document length ? */
  protected boolean m_normalize = false;

  /** The length that each document vector should have in the end */
  protected double m_norm = 1.0;

  /** The L-norm to use */
  protected double m_lnorm = 2.0;

  /** Leplace-like correction factor for zero frequency */
  protected double m_leplace = 1.0;

  /** Holds the current instance number */
  protected double m_t;

  /**
   * Returns a string describing classifier
   *
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {
    return "Multinomial naive bayes for text data. Operates "
      + "directly (and only) on String attributes. "
      + "Other types of input attributes are accepted but "
      + "ignored during training and classification";
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

    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NOMINAL_CLASS);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * Generates the classifier.
   *
   * @param data set of instances serving as training data
   * @throws Exception if the classifier has not been generated successfully
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {
    reset();

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    m_data = new Instances(data, 0);
    data = new Instances(data);

    m_wordsPerClass = new double[data.numClasses()];
    m_probOfClass = new double[data.numClasses()];
    m_probOfWordGivenClass =
      new HashMap<Integer, LinkedHashMap<String, Count>>();

    double laplace = 1.0;
    for (int i = 0; i < data.numClasses(); i++) {
      LinkedHashMap<String, Count> dict =
        new LinkedHashMap<String, Count>(10000 / data.numClasses());
      m_probOfWordGivenClass.put(i, dict);
      m_probOfClass[i] = laplace;

      // this needs to be updated for laplace correction every time we see a new
      // word (attribute)
      m_wordsPerClass[i] = 0;
    }

    for (int i = 0; i < data.numInstances(); i++) {
      updateClassifier(data.instance(i));
    }

    if (data.numInstances() > 0) {
      pruneDictionary(true);
    }
  }

  /**
   * Updates the classifier with the given instance.
   *
   * @param instance the new training instance to include in the model
   * @throws Exception if the instance could not be incorporated in the model.
   */
  @Override
  public void updateClassifier(Instance instance) throws Exception {
    updateClassifier(instance, true);
  }

  protected void updateClassifier(Instance instance, boolean updateDictionary)
    throws Exception {

    if (!instance.classIsMissing()) {
      int classIndex = (int) instance.classValue();
      m_probOfClass[classIndex] += instance.weight();

      tokenizeInstance(instance, updateDictionary);
      m_t++;
    }
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @throws Exception if there is a problem generating the prediction
   */
  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {

    tokenizeInstance( instance, false );

    double[] probOfClassGivenDoc = new double[m_data.numClasses()];

    double[] logDocGivenClass = new double[m_data.numClasses()];
    for (int i = 0; i < m_data.numClasses(); i++) {
      logDocGivenClass[i] += Math.log(m_probOfClass[i]);

      LinkedHashMap<String, Count> dictForClass = m_probOfWordGivenClass.get(i);

      int allWords = 0;
      // for document normalization (if in use)
      double iNorm = 0;
      double fv = 0;

      if (m_normalize) {
        for (Map.Entry<String, Count> feature : m_inputVector.entrySet()) {
          String word = feature.getKey();
          Count c = feature.getValue();

          // check the word against all the dictionaries (all classes)
          boolean ok = false;
          for (int clss = 0; clss < m_data.numClasses(); clss++) {
            if (m_probOfWordGivenClass.get(clss).get(word) != null) {
              ok = true;
              break;
            }
          }

          // only normalize with respect to those words that we've seen during
          // training
          // (i.e. dictionary over all classes)
          if (ok) {
            // word counts or bag-of-words?
            fv = (m_wordFrequencies) ? c.m_count : 1.0;
            iNorm += Math.pow(Math.abs(fv), m_lnorm);
          }
        }
        iNorm = Math.pow(iNorm, 1.0 / m_lnorm);
      }

      // System.out.println("---- " + m_inputVector.size());
      for (Map.Entry<String, Count> feature : m_inputVector.entrySet()) {
        String word = feature.getKey();
        Count dictCount = dictForClass.get(word);
        // System.out.print(word + " ");
        /*
         * if (dictCount != null) { System.out.println(dictCount.m_count); }
         * else { System.out.println("*1"); }
         */
        // check the word against all the dictionaries (all classes)
        boolean ok = false;
        for (int clss = 0; clss < m_data.numClasses(); clss++) {
          if (m_probOfWordGivenClass.get(clss).get(word) != null) {
            ok = true;
            break;
          }
        }

        // ignore words we haven't seen in the training data
        if (ok) {
          double freq = (m_wordFrequencies) ? feature.getValue().m_count : 1.0;
          // double freq = (feature.getValue().m_count / iNorm * m_norm);
          if (m_normalize) {
            freq /= iNorm * m_norm;
          }
          allWords += freq;

          if (dictCount != null) {
            logDocGivenClass[i] += freq * Math.log(dictCount.m_count);
          } else {
            // leplace for zero frequency
            logDocGivenClass[i] += freq * Math.log(m_leplace);
          }
        }
      }

      if (m_wordsPerClass[i] > 0) {
        logDocGivenClass[i] -= allWords * Math.log(m_wordsPerClass[i]);
      }
    }

    double max = logDocGivenClass[Utils.maxIndex(logDocGivenClass)];

    for (int i = 0; i < m_data.numClasses(); i++) {
      probOfClassGivenDoc[i] = Math.exp(logDocGivenClass[i] - max);
    }

    Utils.normalize(probOfClassGivenDoc);

    return probOfClassGivenDoc;
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
        }
      }
    }

    if (updateDictionary) {
      int classValue = (int) instance.classValue();
      LinkedHashMap<String, Count> dictForClass =
        m_probOfWordGivenClass.get(classValue);

      // document normalization
      double iNorm = 0;
      double fv = 0;

      if (m_normalize) {
        for (Count c : m_inputVector.values()) {
          // word counts or bag-of-words?
          fv = (m_wordFrequencies) ? c.m_count : 1.0;
          iNorm += Math.pow(Math.abs(fv), m_lnorm);
        }
        iNorm = Math.pow(iNorm, 1.0 / m_lnorm);
      }

      for (Map.Entry<String, Count> feature : m_inputVector.entrySet()) {
        String word = feature.getKey();
        double freq = (m_wordFrequencies) ? feature.getValue().m_count : 1.0;
        // double freq = (feature.getValue().m_count / iNorm * m_norm);

        if (m_normalize) {
          freq /= (iNorm * m_norm);
        }

        // check all classes
        for (int i = 0; i < m_data.numClasses(); i++) {
          LinkedHashMap<String, Count> dict = m_probOfWordGivenClass.get(i);
          if (dict.get(word) == null) {
            dict.put(word, new Count(m_leplace));
            m_wordsPerClass[i] += m_leplace;
          }
        }

        Count dictCount = dictForClass.get(word);
        /*
         * if (dictCount == null) { dictForClass.put(word, new Count(m_leplace +
         * freq)); m_wordsPerClass[classValue] += (m_leplace + freq); } else {
         */
        dictCount.m_count += freq;
        m_wordsPerClass[classValue] += freq;
        // }
      }

      pruneDictionary(false);
    }
  }

  protected void pruneDictionary(boolean force) {
    if ((m_periodicP <= 0 || m_t % m_periodicP > 0) && !force) {
      return;
    }

    Set<Integer> classesSet = m_probOfWordGivenClass.keySet();
    for (Integer classIndex : classesSet) {
      LinkedHashMap<String, Count> dictForClass =
        m_probOfWordGivenClass.get(classIndex);

      Iterator<Map.Entry<String, Count>> entries =
        dictForClass.entrySet().iterator();
      while (entries.hasNext()) {
        Map.Entry<String, Count> entry = entries.next();
        if (entry.getValue().m_count < m_minWordP) {
          m_wordsPerClass[classIndex] -= entry.getValue().m_count;
          entries.remove();
        }
      }
    }
  }

  /**
   * Reset the classifier.
   */
  public void reset() {
    m_t = 1;
    m_wordsPerClass = null;
    m_probOfWordGivenClass = null;
    m_probOfClass = null;
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
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>();

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
    newVector.addElement(new Option(
      "\tNormalize document length (use in conjunction with -norm and "
        + "-lnorm)", "normalize", 0, "-normalize"));
    newVector.addElement(new Option(
      "\tSpecify the norm that each instance must have (default 1.0)", "norm",
      1, "-norm <num>"));
    newVector.addElement(
      new Option("\tSpecify L-norm to use (default 2.0)", "lnorm", 1,
        "-lnorm <num>"));
    newVector.addElement(new Option("\tConvert all tokens to lowercase "
      + "before adding to the dictionary.", "lowercase", 0, "-lowercase"));
    newVector.addElement(
      new Option("\tThe stopwords handler to use (default Null).",
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

    super.setOptions(options);

    setUseWordFrequencies(Utils.getFlag("W", options));

    String pruneFreqS = Utils.getOption("P", options);
    if (pruneFreqS.length() > 0) {
      setPeriodicPruning(Integer.parseInt(pruneFreqS));
    }
    String minFreq = Utils.getOption("M", options);
    if (minFreq.length() > 0) {
      setMinWordFrequency(Double.parseDouble(minFreq));
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

    if (getUseWordFrequencies()) {
      options.add("-W");
    }
    options.add("-P");
    options.add("" + getPeriodicPruning());
    options.add("-M");
    options.add("" + getMinWordFrequency());

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
      spec +=
        " " + Utils.joinOptions(((OptionHandler) getTokenizer()).getOptions());
    }
    options.add(spec.trim());

    if (getStemmer() != null) {
      options.add("-stemmer");
      spec = getStemmer().getClass().getName();
      if (getStemmer() instanceof OptionHandler) {
        spec +=
          " " + Utils.joinOptions(((OptionHandler) getStemmer()).getOptions());
      }

      options.add(spec.trim());
    }

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[1]);
  }

  /**
   * Returns a textual description of this classifier.
   *
   * @return a textual description of this classifier.
   */
  @Override
  public String toString() {

    if (m_probOfClass == null) {
      return "NaiveBayesMultinomialText: No model built yet.\n";
    }

    StringBuffer result = new StringBuffer();

    // build a master dictionary over all classes
    HashSet<String> master = new HashSet<String>();
    for (int i = 0; i < m_data.numClasses(); i++) {
      LinkedHashMap<String, Count> classDict = m_probOfWordGivenClass.get(i);

      for (String key : classDict.keySet()) {
        master.add(key);
      }
    }

    result.append("Dictionary size: " + master.size()).append("\n\n");

    result.append("The independent frequency of a class\n");
    result.append("--------------------------------------\n");

    for (int i = 0; i < m_data.numClasses(); i++) {
      result.append(m_data.classAttribute().value(i)).append("\t")
        .append(Double.toString(m_probOfClass[i])).append("\n");
    }

    if (master.size() > 150000) {
      result.append("\nFrequency table ommitted due to size\n");
      return result.toString();
    }

    result.append("\nThe frequency of a word given the class\n");
    result.append("-----------------------------------------\n");

    for (int i = 0; i < m_data.numClasses(); i++) {
      result.append(Utils.padLeft(m_data.classAttribute().value(i), 11))
        .append("\t");
    }

    result.append("\n");

    Iterator<String> masterIter = master.iterator();
    while (masterIter.hasNext()) {
      String word = masterIter.next();

      for (int i = 0; i < m_data.numClasses(); i++) {
        LinkedHashMap<String, Count> classDict = m_probOfWordGivenClass.get(i);
        Count c = classDict.get(word);
        if (c == null) {
          result.append("<laplace=1>\t");
        } else {
          result.append(Utils.padLeft(Double.toString(c.m_count), 11)).append(
            "\t");
        }
      }
      result.append(word);
      result.append("\n");
    }

    return result.toString();
  }

  /**
   * Returns the revision string.
   *
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11973 $");
  }

  protected int m_numModels = 0;

  @Override
  public NaiveBayesMultinomialText aggregate(
    NaiveBayesMultinomialText toAggregate) throws Exception {
    if (m_numModels == Integer.MIN_VALUE) {
      throw new Exception("Can't aggregate further - model has already been "
        + "aggregated and finalized");
    }

    if (m_probOfClass == null) {
      throw new Exception("No model built yet, can't aggregate");
    }

    // just check the class attribute for compatibility as we will be
    // merging dictionaries
    if (!m_data.classAttribute().equals(toAggregate.m_data.classAttribute())) {
      throw new Exception("Can't aggregate - class attribute in data headers "
        + "does not match: "
        + m_data.classAttribute()
          .equalsMsg(toAggregate.m_data.classAttribute()));
    }

    for (int i = 0; i < m_probOfClass.length; i++) {
      // we already have a laplace correction, so -1
      m_probOfClass[i] += toAggregate.m_probOfClass[i] - 1;

      m_wordsPerClass[i] += toAggregate.m_wordsPerClass[i];
    }

    Map<Integer, LinkedHashMap<String, Count>> dicts =
      toAggregate.m_probOfWordGivenClass;
    Iterator<Map.Entry<Integer, LinkedHashMap<String, Count>>> perClass =
      dicts.entrySet().iterator();
    while (perClass.hasNext()) {
      Map.Entry<Integer, LinkedHashMap<String, Count>> currentClassDict =
        perClass.next();

      LinkedHashMap<String, Count> masterDict =
        m_probOfWordGivenClass.get(currentClassDict.getKey());

      if (masterDict == null) {
        // we haven't seen this class during our training
        masterDict = new LinkedHashMap<String, Count>();
        m_probOfWordGivenClass.put(currentClassDict.getKey(), masterDict);
      }

      // now process words seen for this class
      Iterator<Map.Entry<String, Count>> perClassEntries =
        currentClassDict.getValue().entrySet().iterator();
      while (perClassEntries.hasNext()) {
        Map.Entry<String, Count> entry = perClassEntries.next();

        Count masterCount = masterDict.get(entry.getKey());

        if (masterCount == null) {
          // we haven't seen this entry (or its been pruned)
          masterCount = new Count(entry.getValue().m_count);
          masterDict.put(entry.getKey(), masterCount);
        } else {
          // add up
          masterCount.m_count += entry.getValue().m_count - 1;
        }
      }
    }

    m_t += toAggregate.m_t;
    m_numModels++;

    return this;
  }

  @Override
  public void finalizeAggregation() throws Exception {
    if (m_numModels == 0) {
      throw new Exception("Unable to finalize aggregation - "
        + "haven't seen any models to aggregate");
    }

    if (m_periodicP > 0 && m_t > m_periodicP) {
      pruneDictionary(true);
      m_t = 0;
    }
  }

  @Override
  public void batchFinished() throws Exception {
    pruneDictionary(true);
  }

  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main(String[] args) {
    runClassifier(new NaiveBayesMultinomialText(), args);
  }
}

