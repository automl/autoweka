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
 *    StringToWordVector.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DictionaryBuilder;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.SparseInstance;
import weka.core.stopwords.StopwordsHandler;
import weka.core.stopwords.Null;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.stemmers.NullStemmer;
import weka.core.stemmers.Stemmer;
import weka.core.tokenizers.Tokenizer;
import weka.core.tokenizers.WordTokenizer;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;

/**
 <!-- globalinfo-start -->
 * Converts String attributes into a set of attributes representing word occurrence (depending on the tokenizer) information from the text contained in the strings. The set of words (attributes) is determined by the first batch filtered (typically training data).
 * <br><br>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -C
 *  Output word counts rather than boolean word presence.
 * </pre>
 * 
 * <pre> -R &lt;index1,index2-index4,...&gt;
 *  Specify list of string attributes to convert to words (as weka Range).
 *  (default: select all string attributes)</pre>
 * 
 * <pre> -V
 *  Invert matching sense of column indexes.</pre>
 * 
 * <pre> -P &lt;attribute name prefix&gt;
 *  Specify a prefix for the created attribute names.
 *  (default: "")</pre>
 * 
 * <pre> -W &lt;number of words to keep&gt;
 *  Specify approximate number of word fields to create.
 *  Surplus words will be discarded..
 *  (default: 1000)</pre>
 * 
 * <pre> -prune-rate &lt;rate as a percentage of dataset&gt;
 *  Specify the rate (e.g., every 10% of the input dataset) at which to periodically prune the dictionary.
 *  -W prunes after creating a full dictionary. You may not have enough memory for this approach.
 *  (default: no periodic pruning)</pre>
 * 
 * <pre> -T
 *  Transform the word frequencies into log(1+fij)
 *  where fij is the frequency of word i in jth document(instance).
 * </pre>
 * 
 * <pre> -I
 *  Transform each word frequency into:
 *  fij*log(num of Documents/num of documents containing word i)
 *    where fij if frequency of word i in jth document(instance)</pre>
 * 
 * <pre> -N
 *  Whether to 0=not normalize/1=normalize all data/2=normalize test data only
 *  to average length of training documents (default 0=don't normalize).</pre>
 * 
 * <pre> -L
 *  Convert all tokens to lowercase before adding to the dictionary.</pre>
 * 
 * <pre> -stopwords-handler
 *  The stopwords handler to use (default Null).</pre>
 * 
 * <pre> -stemmer &lt;spec&gt;
 *  The stemming algorithm (classname plus parameters) to use.</pre>
 * 
 * <pre> -M &lt;int&gt;
 *  The minimum term frequency (default = 1).</pre>
 * 
 * <pre> -O
 *  If this is set, the maximum number of words and the 
 *  minimum term frequency is not enforced on a per-class 
 *  basis but based on the documents in all the classes 
 *  (even if a class attribute is set).</pre>
 * 
 * <pre> -tokenizer &lt;spec&gt;
 *  The tokenizing algorihtm (classname plus parameters) to use.
 *  (default: weka.core.tokenizers.WordTokenizer)</pre>
 * 
 * <pre> -dictionary &lt;path to save to&gt;
 *  The file to save the dictionary to.
 *  (default is not to save the dictionary)</pre>
 * 
 * <pre> -binary-dict
 *  Save the dictionary file as a binary serialized object
 *  instead of in plain text form. Use in conjunction with
 *  -dictionary</pre>
 * 
 <!-- options-end -->
 *
 * @author Len Trigg (len@reeltwo.com)
 * @author Stuart Inglis (stuart@reeltwo.com)
 * @author Gordon Paynter (gordon.paynter@ucr.edu)
 * @author Asrhaf M. Kibriya (amk14@cs.waikato.ac.nz)
 * @version $Revision: 12074 $
 */
public class StringToWordVector extends Filter implements UnsupervisedFilter,
  OptionHandler {

  /** Used to build and manage the dictionary + vectorization */
  protected DictionaryBuilder m_dictionaryBuilder = new DictionaryBuilder();

  /** for serialization. */
  static final long serialVersionUID = 8249106275278565424L;

  /**
   * The percentage at which to periodically prune the dictionary.
   */
  private double m_PeriodicPruningRate = -1;

  /** The normalization to apply. */
  protected int m_filterType = FILTER_NONE;

  /** normalization: No normalization. */
  public static final int FILTER_NONE = 0;
  /** normalization: Normalize all data. */
  public static final int FILTER_NORMALIZE_ALL = 1;
  /** normalization: Normalize test data only. */
  public static final int FILTER_NORMALIZE_TEST_ONLY = 2;

  /**
   * Specifies whether document's (instance's) word frequencies are to be
   * normalized. The are normalized to average length of documents specified as
   * input format.
   */
  public static final Tag[] TAGS_FILTER = {
    new Tag(FILTER_NONE, "No normalization"),
    new Tag(FILTER_NORMALIZE_ALL, "Normalize all data"),
    new Tag(FILTER_NORMALIZE_TEST_ONLY, "Normalize test data only"), };

  /** File to save the dictionary to */
  protected File m_dictionaryFile = new File("-- set me --");

  /**
   * Whether to save the dictionary in serialized form rather than
   * as plain text
   */
  protected boolean m_dictionaryIsBinary;


  /**
   * Default constructor. Targets 1000 words in the output.
   */
  public StringToWordVector() {
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result
      .addElement(new Option(
        "\tOutput word counts rather than boolean word presence.\n", "C", 0,
        "-C"));

    result.addElement(new Option(
      "\tSpecify list of string attributes to convert to words (as weka Range).\n"
        + "\t(default: select all string attributes)", "R", 1,
      "-R <index1,index2-index4,...>"));

    result.addElement(new Option("\tInvert matching sense of column indexes.",
      "V", 0, "-V"));

    result.addElement(new Option(
      "\tSpecify a prefix for the created attribute names.\n"
        + "\t(default: \"\")", "P", 1, "-P <attribute name prefix>"));

    result.addElement(new Option(
      "\tSpecify approximate number of word fields to create.\n"
        + "\tSurplus words will be discarded..\n" + "\t(default: 1000)", "W",
      1, "-W <number of words to keep>"));

    result
      .addElement(new Option(
        "\tSpecify the rate (e.g., every 10% of the input dataset) at which to periodically prune the dictionary.\n"
          + "\t-W prunes after creating a full dictionary. You may not have enough memory for this approach.\n"
          + "\t(default: no periodic pruning)", "prune-rate", 1,
        "-prune-rate <rate as a percentage of dataset>"));

    result
      .addElement(new Option(
        "\tTransform the word frequencies into log(1+fij)\n"
          + "\twhere fij is the frequency of word i in jth document(instance).\n",
        "T", 0, "-T"));

    result.addElement(new Option("\tTransform each word frequency into:\n"
      + "\tfij*log(num of Documents/num of documents containing word i)\n"
      + "\t  where fij if frequency of word i in jth document(instance)", "I",
      0, "-I"));

    result
      .addElement(new Option(
        "\tWhether to 0=not normalize/1=normalize all data/2=normalize test data only\n"
          + "\tto average length of training documents "
          + "(default 0=don\'t normalize).", "N", 1, "-N"));

    result.addElement(new Option("\tConvert all tokens to lowercase before "
      + "adding to the dictionary.", "L", 0, "-L"));

    result.addElement(new Option("\tThe stopwords handler to use (default Null).",
      "-stopwords-handler", 1, "-stopwords-handler"));

    result.addElement(new Option(
      "\tThe stemming algorithm (classname plus parameters) to use.",
      "stemmer", 1, "-stemmer <spec>"));

    result.addElement(new Option("\tThe minimum term frequency (default = 1).",
      "M", 1, "-M <int>"));

    result.addElement(new Option(
      "\tIf this is set, the maximum number of words and the \n"
        + "\tminimum term frequency is not enforced on a per-class \n"
        + "\tbasis but based on the documents in all the classes \n"
        + "\t(even if a class attribute is set).", "O", 0, "-O"));

    result.addElement(new Option(
      "\tThe tokenizing algorithm (classname plus parameters) to use.\n"
        + "\t(default: " + WordTokenizer.class.getName() + ")", "tokenizer", 1,
      "-tokenizer <spec>"));

    result.addElement(new Option("\tThe file to save the dictionary to.\n"
      + "\t(default is not to save the dictionary)", "dictionary", 1,
      "-dictionary <path to save to>"));

    result.addElement(new Option("\tSave the dictionary file as a binary "
      + "serialized object\n\tinstead of in plain text form. Use in conjunction "
      + "with\n\t-dictionary", "binary-dict", 0, "-binary-dict"));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   *
   <!-- options-start -->
   * Valid options are: <p>
   * 
   * <pre> -C
   *  Output word counts rather than boolean word presence.
   * </pre>
   * 
   * <pre> -R &lt;index1,index2-index4,...&gt;
   *  Specify list of string attributes to convert to words (as weka Range).
   *  (default: select all string attributes)</pre>
   * 
   * <pre> -V
   *  Invert matching sense of column indexes.</pre>
   * 
   * <pre> -P &lt;attribute name prefix&gt;
   *  Specify a prefix for the created attribute names.
   *  (default: "")</pre>
   * 
   * <pre> -W &lt;number of words to keep&gt;
   *  Specify approximate number of word fields to create.
   *  Surplus words will be discarded..
   *  (default: 1000)</pre>
   * 
   * <pre> -prune-rate &lt;rate as a percentage of dataset&gt;
   *  Specify the rate (e.g., every 10% of the input dataset) at which to periodically prune the dictionary.
   *  -W prunes after creating a full dictionary. You may not have enough memory for this approach.
   *  (default: no periodic pruning)</pre>
   * 
   * <pre> -T
   *  Transform the word frequencies into log(1+fij)
   *  where fij is the frequency of word i in jth document(instance).
   * </pre>
   * 
   * <pre> -I
   *  Transform each word frequency into:
   *  fij*log(num of Documents/num of documents containing word i)
   *    where fij if frequency of word i in jth document(instance)</pre>
   * 
   * <pre> -N
   *  Whether to 0=not normalize/1=normalize all data/2=normalize test data only
   *  to average length of training documents (default 0=don't normalize).</pre>
   * 
   * <pre> -L
   *  Convert all tokens to lowercase before adding to the dictionary.</pre>
   * 
   * <pre> -stopwords-handler
   *  The stopwords handler to use (default Null).</pre>
   * 
   * <pre> -stemmer &lt;spec&gt;
   *  The stemming algorithm (classname plus parameters) to use.</pre>
   * 
   * <pre> -M &lt;int&gt;
   *  The minimum term frequency (default = 1).</pre>
   * 
   * <pre> -O
   *  If this is set, the maximum number of words and the 
   *  minimum term frequency is not enforced on a per-class 
   *  basis but based on the documents in all the classes 
   *  (even if a class attribute is set).</pre>
   * 
   * <pre> -tokenizer &lt;spec&gt;
   *  The tokenizing algorihtm (classname plus parameters) to use.
   *  (default: weka.core.tokenizers.WordTokenizer)</pre>
   * 
   * <pre> -dictionary &lt;path to save to&gt;
   *  The file to save the dictionary to.
   *  (default is not to save the dictionary)</pre>
   * 
   * <pre> -binary-dict
   *  Save the dictionary file as a binary serialized object
   *  instead of in plain text form. Use in conjunction with
   *  -dictionary</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String value = Utils.getOption('R', options);
    if (value.length() != 0) {
      setSelectedRange(value);
    } else {
      setSelectedRange("first-last");
    }

    setInvertSelection(Utils.getFlag('V', options));

    value = Utils.getOption('P', options);
    if (value.length() != 0) {
      setAttributeNamePrefix(value);
    } else {
      setAttributeNamePrefix("");
    }

    value = Utils.getOption('W', options);
    if (value.length() != 0) {
      setWordsToKeep(Integer.valueOf(value).intValue());
    } else {
      setWordsToKeep(1000);
    }

    value = Utils.getOption("prune-rate", options);
    if (value.length() > 0) {
      setPeriodicPruning(Double.parseDouble(value));
    } else {
      setPeriodicPruning(-1);
    }

    value = Utils.getOption('M', options);
    if (value.length() != 0) {
      setMinTermFreq(Integer.valueOf(value).intValue());
    } else {
      setMinTermFreq(1);
    }

    setOutputWordCounts(Utils.getFlag('C', options));

    setTFTransform(Utils.getFlag('T', options));

    setIDFTransform(Utils.getFlag('I', options));

    setDoNotOperateOnPerClassBasis(Utils.getFlag('O', options));

    String nString = Utils.getOption('N', options);
    if (nString.length() != 0) {
      setNormalizeDocLength(new SelectedTag(Integer.parseInt(nString),
        TAGS_FILTER));
    } else {
      setNormalizeDocLength(new SelectedTag(FILTER_NONE, TAGS_FILTER));
    }

    setLowerCaseTokens(Utils.getFlag('L', options));

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

    String dictFile = Utils.getOption("dictionary", options);
    setDictionaryFileToSaveTo(new File(dictFile));

    setSaveDictionaryInBinaryForm(Utils.getFlag("binary-dict", options));

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    result.add("-R");
    result.add(getSelectedRange().getRanges());

    if (getInvertSelection()) {
      result.add("-V");
    }

    if (!"".equals(getAttributeNamePrefix())) {
      result.add("-P");
      result.add(getAttributeNamePrefix());
    }

    result.add("-W");
    result.add(String.valueOf(getWordsToKeep()));

    result.add("-prune-rate");
    result.add(String.valueOf(getPeriodicPruning()));

    if (getOutputWordCounts()) {
      result.add("-C");
    }

    if (getTFTransform()) {
      result.add("-T");
    }

    if (getIDFTransform()) {
      result.add("-I");
    }

    result.add("-N");
    result.add("" + m_filterType);

    if (getLowerCaseTokens()) {
      result.add("-L");
    }

    if (getStemmer() != null) {
      result.add("-stemmer");
      String spec = getStemmer().getClass().getName();
      if (getStemmer() instanceof OptionHandler) {
        spec += " "
          + Utils.joinOptions(((OptionHandler) getStemmer()).getOptions());
      }
      result.add(spec.trim());
    }

    if (getStopwordsHandler() != null) {
      result.add("-stopwords-handler");
      String spec = getStopwordsHandler().getClass().getName();
      if (getStopwordsHandler() instanceof OptionHandler) {
        spec += " "
          + Utils.joinOptions(((OptionHandler) getStopwordsHandler()).getOptions());
      }
      result.add(spec.trim());
    }

    result.add("-M");
    result.add(String.valueOf(getMinTermFreq()));

    if (getDoNotOperateOnPerClassBasis()) {
      result.add("-O");
    }

    result.add("-tokenizer");
    String spec = getTokenizer().getClass().getName();
    if (getTokenizer() instanceof OptionHandler) {
      spec += " "
        + Utils.joinOptions(((OptionHandler) getTokenizer()).getOptions());
    }
    result.add(spec.trim());

    if (m_dictionaryFile != null && m_dictionaryFile.toString().length() > 0 &&
      !m_dictionaryFile.toString().equalsIgnoreCase("-- set me --")) {
      result.add("-dictionary");
      result.add(m_dictionaryFile.toString());

      if (getSaveDictionaryInBinaryForm()) {
        result.add("-binary-dict");
      }
    }


    return result.toArray(new String[result.size()]);
  }

  /**
   * Constructor that allows specification of the target number of words in the
   * output.
   *
   * @param wordsToKeep the number of words in the output vector (per class if
   *          assigned).
   */
  public StringToWordVector(int wordsToKeep) {
    m_dictionaryBuilder.setWordsToKeep(wordsToKeep);
  }

  /**
   * Returns the Capabilities of this filter.
   *
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enableAllAttributes();
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   *          structure (any instances contained in the object are ignored -
   *          only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the input format can't be set successfully
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);

    m_dictionaryBuilder.reset();
    m_dictionaryBuilder.setSortDictionary(true);
    m_dictionaryBuilder.setNormalize(false);
    m_dictionaryBuilder.setup(instanceInfo);

    return false;
  }

  /**
   * Input an instance for filtering. Filter requires all training instances be
   * read before producing output.
   *
   * @param instance the input instance.
   * @return true if the filtered instance may now be collected with output().
   * @throws IllegalStateException if no input structure has been defined.
   */
  @Override
  public boolean input(Instance instance) throws Exception {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    if (isFirstBatchDone()) {
      Instance inst = m_dictionaryBuilder.vectorizeInstance(instance);
      push(inst, false); // No need to copy
      return true;
    } else {
      bufferInput(instance);
      return false;
    }
  }

  /**
   * Signify that this batch of input to the filter is finished. If the filter
   * requires all instances prior to filtering, output() may now be called to
   * retrieve the filtered instances.
   *
   * @return true if there are instances pending output.
   * @throws IllegalStateException if no input structure has been defined.
   */
  @Override
  public boolean batchFinished() throws Exception {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    // We only need to do something in this method
    // if the first batch hasn't been processed. Otherwise
    // input() has already done all the work.
    if (!isFirstBatchDone()) {

      long pruneRate = Math.round((m_PeriodicPruningRate / 100.0)
        * getInputFormat().numInstances());
      m_dictionaryBuilder.setPeriodicPruning(pruneRate);
      // m_dictionaryBuilder.setNormalize(m_filterType == FILTER_NORMALIZE_ALL);

      for (int i = 0; i < getInputFormat().numInstances(); i++) {
        Instance toProcess = getInputFormat().instance(i);
        m_dictionaryBuilder.processInstance(toProcess);
      }
      m_dictionaryBuilder.finalizeDictionary();

      setOutputFormat(m_dictionaryBuilder.getVectorizedFormat());

      m_dictionaryBuilder.setNormalize(m_filterType != FILTER_NONE);
      Instances converted = m_dictionaryBuilder.vectorizeBatch( getInputFormat(),
        m_filterType != FILTER_NONE);

      // save the dictionary?
      if (m_dictionaryFile != null && m_dictionaryFile.toString().length() > 0 &&
        !m_dictionaryFile.toString().equalsIgnoreCase("-- set me --")) {
        m_dictionaryBuilder.saveDictionary(m_dictionaryFile, !m_dictionaryIsBinary);
      }

      // push all instances into the output queue
      for (int i = 0; i < converted.numInstances(); i++) {
        push(converted.instance(i), false);
      }
    }

    // Flush the input
    flushInput();

    m_NewBatch = true;
    m_FirstBatchDone = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String dictionaryFileToSaveToTipText() {
    return "The path to save the dictionary file to - "
      + "an empty path or a path '-- set me --' means "
      + "do not save the dictionary.";
  }

  /**
   * Set the dictionary file to save the dictionary to. A file with an
   * empty path or a path "-- set me --" means do not save the dictionary.
   *
   * @param toSaveTo the path to save the dictionary to
   */
  public void setDictionaryFileToSaveTo(File toSaveTo) {
    m_dictionaryFile = toSaveTo;
  }

  /**
   * Set the dictionary file to save the dictionary to. A file with an
   * empty path or a path "-- set me --" means do not save the dictionary.
   *
   * @return the path to save the dictionary to
   */
  public File getDictionaryFileToSaveTo() {
    return m_dictionaryFile;
  }

  public String saveDictionaryInBinaryFormTipText() {
    return "Save the dictionary as a binary serialized java object instead of "
      + "in plain text form.";
  }

  /**
   * Set whether to save the dictionary in binary serialized form rather than
   * as plain text
   *
   * @param saveAsBinary true to save the dictionary in binary form
   */
  public void setSaveDictionaryInBinaryForm(boolean saveAsBinary) {
    m_dictionaryIsBinary = saveAsBinary;
  }

  /**
   * Set whether to save the dictionary in binary serialized form rather than
   * as plain text
   *
   * @return true to save the dictionary in binary form
   */
  public boolean getSaveDictionaryInBinaryForm() {
    return m_dictionaryIsBinary;
  }

  /**
   * Returns a string describing this filter.
   *
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Converts String attributes into a set of attributes representing "
      + "word occurrence (depending on the tokenizer) information from the "
      + "text contained in the strings. The set of words (attributes) is "
      + "determined by the first batch filtered (typically training data).";
  }

  /**
   * Gets whether output instances contain 0 or 1 indicating word presence, or
   * word counts.
   *
   * @return true if word counts should be output.
   */
  public boolean getOutputWordCounts() {
    return m_dictionaryBuilder.getOutputWordCounts();
  }

  /**
   * Sets whether output instances contain 0 or 1 indicating word presence, or
   * word counts.
   *
   * @param outputWordCounts true if word counts should be output.
   */
  public void setOutputWordCounts(boolean outputWordCounts) {
    m_dictionaryBuilder.setOutputWordCounts(outputWordCounts);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String outputWordCountsTipText() {
    return "Output word counts rather than boolean 0 or 1"
      + "(indicating presence or absence of a word).";
  }

  /**
   * Get the value of m_SelectedRange.
   *
   * @return Value of m_SelectedRange.
   */
  public Range getSelectedRange() {
    return m_dictionaryBuilder.getSelectedRange();
  }

  /**
   * Set the value of m_SelectedRange.
   *
   * @param newSelectedRange Value to assign to m_SelectedRange.
   */
  public void setSelectedRange(String newSelectedRange) {
    m_dictionaryBuilder.setSelectedRange(newSelectedRange);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String attributeIndicesTipText() {
    return "Specify range of attributes to act on."
      + " This is a comma separated list of attribute indices, with"
      + " \"first\" and \"last\" valid values. Specify an inclusive"
      + " range with \"-\". E.g: \"first-3,5,6-10,last\".";
  }

  /**
   * Gets the current range selection.
   *
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {
    return m_dictionaryBuilder.getAttributeIndices();
  }

  /**
   * Sets which attributes are to be worked on.
   *
   * @param rangeList a string representing the list of attributes. Since the
   *          string will typically come from a user, attributes are indexed
   *          from 1. <br>
   *          eg: first-3,5,6-last
   * @throws IllegalArgumentException if an invalid range list is supplied
   */
  public void setAttributeIndices(String rangeList) {
    m_dictionaryBuilder.setAttributeIndices(rangeList);
  }

  /**
   * Sets which attributes are to be processed.
   *
   * @param attributes an array containing indexes of attributes to process.
   *          Since the array will typically come from a program, attributes are
   *          indexed from 0.
   * @throws IllegalArgumentException if an invalid set of ranges is supplied
   */
  public void setAttributeIndicesArray(int[] attributes) {
    m_dictionaryBuilder.setAttributeIndicesArray(attributes);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String invertSelectionTipText() {
    return "Set attribute selection mode. If false, only selected"
      + " attributes in the range will be worked on; if"
      + " true, only non-selected attributes will be processed.";
  }

  /**
   * Gets whether the supplied columns are to be processed or skipped.
   *
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {
    return m_dictionaryBuilder.getInvertSelection();
  }

  /**
   * Sets whether selected columns should be processed or skipped.
   *
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {
    m_dictionaryBuilder.setInvertSelection(invert);
  }

  /**
   * Get the attribute name prefix.
   *
   * @return The current attribute name prefix.
   */
  public String getAttributeNamePrefix() {
    return m_dictionaryBuilder.getAttributeNamePrefix();
  }

  /**
   * Set the attribute name prefix.
   *
   * @param newPrefix String to use as the attribute name prefix.
   */
  public void setAttributeNamePrefix(String newPrefix) {
    m_dictionaryBuilder.setAttributeNamePrefix(newPrefix);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String attributeNamePrefixTipText() {
    return "Prefix for the created attribute names. " + "(default: \"\")";
  }

  /**
   * Gets the number of words (per class if there is a class attribute assigned)
   * to attempt to keep.
   *
   * @return the target number of words in the output vector (per class if
   *         assigned).
   */
  public int getWordsToKeep() {
    return m_dictionaryBuilder.getWordsToKeep();
  }

  /**
   * Sets the number of words (per class if there is a class attribute assigned)
   * to attempt to keep.
   *
   * @param newWordsToKeep the target number of words in the output vector (per
   *          class if assigned).
   */
  public void setWordsToKeep(int newWordsToKeep) {
    m_dictionaryBuilder.setWordsToKeep(newWordsToKeep);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String wordsToKeepTipText() {
    return "The number of words (per class if there is a class attribute "
      + "assigned) to attempt to keep.";
  }

  /**
   * Gets the rate at which the dictionary is periodically pruned, as a
   * percentage of the dataset size.
   *
   * @return the rate at which the dictionary is periodically pruned
   */
  public double getPeriodicPruning() {
    return m_PeriodicPruningRate;
  }

  /**
   * Sets the rate at which the dictionary is periodically pruned, as a
   * percentage of the dataset size.
   *
   * @param newPeriodicPruning the rate at which the dictionary is periodically
   *          pruned
   */
  public void setPeriodicPruning(double newPeriodicPruning) {
    m_PeriodicPruningRate = newPeriodicPruning;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String periodicPruningTipText() {
    return "Specify the rate (x% of the input dataset) at which to periodically prune the dictionary. "
      + "wordsToKeep prunes after creating a full dictionary. You may not have enough "
      + "memory for this approach.";
  }

  /**
   * Gets whether if the word frequencies should be transformed into log(1+fij)
   * where fij is the frequency of word i in document(instance) j.
   *
   * @return true if word frequencies are to be transformed.
   */
  public boolean getTFTransform() {
    return m_dictionaryBuilder.getTFTransform();
  }

  /**
   * Sets whether if the word frequencies should be transformed into log(1+fij)
   * where fij is the frequency of word i in document(instance) j.
   *
   * @param TFTransform true if word frequencies are to be transformed.
   */
  public void setTFTransform(boolean TFTransform) {
    m_dictionaryBuilder.setTFTransform(TFTransform);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String TFTransformTipText() {
    return "Sets whether if the word frequencies should be transformed into:\n "
      + "   log(1+fij) \n"
      + "       where fij is the frequency of word i in document (instance) j.";
  }

  /**
   * Sets whether if the word frequencies in a document should be transformed
   * into: <br>
   * fij*log(num of Docs/num of Docs with word i) <br>
   * where fij is the frequency of word i in document(instance) j.
   *
   * @return true if the word frequencies are to be transformed.
   */
  public boolean getIDFTransform() {
    return m_dictionaryBuilder.getIDFTransform();
  }

  /**
   * Sets whether if the word frequencies in a document should be transformed
   * into: <br>
   * fij*log(num of Docs/num of Docs with word i) <br>
   * where fij is the frequency of word i in document(instance) j.
   *
   * @param IDFTransform true if the word frequecies are to be transformed
   */
  public void setIDFTransform(boolean IDFTransform) {
    m_dictionaryBuilder.setIDFTransform(IDFTransform);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String IDFTransformTipText() {
    return "Sets whether if the word frequencies in a document should be "
      + "transformed into: \n"
      + "   fij*log(num of Docs/num of Docs with word i) \n"
      + "      where fij is the frequency of word i in document (instance) j.";
  }

  /**
   * Gets whether if the word frequencies for a document (instance) should be
   * normalized or not.
   *
   * @return true if word frequencies are to be normalized.
   */
  public SelectedTag getNormalizeDocLength() {
    return new SelectedTag(m_filterType, TAGS_FILTER);
  }

  /**
   * Sets whether if the word frequencies for a document (instance) should be
   * normalized or not.
   *
   * @param newType the new type.
   */
  public void setNormalizeDocLength(SelectedTag newType) {

    if (newType.getTags() == TAGS_FILTER) {
      m_filterType = newType.getSelectedTag().getID();
    }
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String normalizeDocLengthTipText() {
    return "Sets whether if the word frequencies for a document (instance) "
      + "should be normalized or not.";
  }

  /**
   * Gets whether if the tokens are to be downcased or not.
   *
   * @return true if the tokens are to be downcased.
   */
  public boolean getLowerCaseTokens() {
    return m_dictionaryBuilder.getLowerCaseTokens();
  }

  /**
   * Sets whether if the tokens are to be downcased or not. (Doesn't affect
   * non-alphabetic characters in tokens).
   *
   * @param downCaseTokens should be true if only lower case tokens are to be
   *          formed.
   */
  public void setLowerCaseTokens(boolean downCaseTokens) {
    m_dictionaryBuilder.setLowerCaseTokens(downCaseTokens);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String doNotOperateOnPerClassBasisTipText() {
    return "If this is set, the maximum number of words and the "
      + "minimum term frequency is not enforced on a per-class "
      + "basis but based on the documents in all the classes "
      + "(even if a class attribute is set).";
  }

  /**
   * Get the DoNotOperateOnPerClassBasis value.
   *
   * @return the DoNotOperateOnPerClassBasis value.
   */
  public boolean getDoNotOperateOnPerClassBasis() {
    return m_dictionaryBuilder.getDoNotOperateOnPerClassBasis();
  }

  /**
   * Set the DoNotOperateOnPerClassBasis value.
   *
   * @param newDoNotOperateOnPerClassBasis The new DoNotOperateOnPerClassBasis
   *          value.
   */
  public void setDoNotOperateOnPerClassBasis(
    boolean newDoNotOperateOnPerClassBasis) {
    m_dictionaryBuilder.setDoNotOperateOnPerClassBasis(newDoNotOperateOnPerClassBasis);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String minTermFreqTipText() {
    return "Sets the minimum term frequency. This is enforced "
      + "on a per-class basis.";
  }

  /**
   * Get the MinTermFreq value.
   *
   * @return the MinTermFreq value.
   */
  public int getMinTermFreq() {
    return m_dictionaryBuilder.getMinTermFreq();
  }

  /**
   * Set the MinTermFreq value.
   *
   * @param newMinTermFreq The new MinTermFreq value.
   */
  public void setMinTermFreq(int newMinTermFreq) {
    m_dictionaryBuilder.setMinTermFreq(newMinTermFreq);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String lowerCaseTokensTipText() {
    return "If set then all the word tokens are converted to lower case "
      + "before being added to the dictionary.";
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
      m_dictionaryBuilder.setStemmer(value);
    } else {
      m_dictionaryBuilder.setStemmer(new NullStemmer());
    }
  }

  /**
   * Returns the current stemming algorithm, null if none is used.
   *
   * @return the current stemming algorithm, null if none set
   */
  public Stemmer getStemmer() {
    return m_dictionaryBuilder.getStemmer();
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
   * Sets the stopwords handler to use.
   *
   * @param value the stopwords handler, if null, Null is used
   */
  public void setStopwordsHandler(StopwordsHandler value) {
    if (value != null) {
      m_dictionaryBuilder.setStopwordsHandler(value);
    } else {
      m_dictionaryBuilder.setStopwordsHandler(new Null());
    }
  }

  /**
   * Gets the stopwords handler.
   *
   * @return the stopwords handler
   */
  public StopwordsHandler getStopwordsHandler() {
    return m_dictionaryBuilder.getStopwordsHandler();
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
   * the tokenizer algorithm to use.
   *
   * @param value the configured tokenizing algorithm
   */
  public void setTokenizer(Tokenizer value) {
    m_dictionaryBuilder.setTokenizer(value);
  }

  /**
   * Returns the current tokenizer algorithm.
   *
   * @return the current tokenizer algorithm
   */
  public Tokenizer getTokenizer() {
    return m_dictionaryBuilder.getTokenizer();
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
   * Returns the revision string.
   *
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12074 $");
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String[] argv) {
    runFilter(new StringToWordVector(), argv);
  }
}

