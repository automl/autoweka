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
 *    SimpleKMeans.java
 *    Copyright (C) 2000-2012 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.clusterers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import weka.classifiers.rules.DecisionTableHashKey;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.ManhattanDistance;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 * <!-- globalinfo-start --> Cluster data using the k means algorithm. Can use
 * either the Euclidean distance (default) or the Manhattan distance. If the
 * Manhattan distance is used, then centroids are computed as the component-wise
 * median rather than mean. For more information see:<br/>
 * <br/>
 * D. Arthur, S. Vassilvitskii: k-means++: the advantages of carefull seeding.
 * In: Proceedings of the eighteenth annual ACM-SIAM symposium on Discrete
 * algorithms, 1027-1035, 2007.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;inproceedings{Arthur2007,
 *    author = {D. Arthur and S. Vassilvitskii},
 *    booktitle = {Proceedings of the eighteenth annual ACM-SIAM symposium on Discrete algorithms},
 *    pages = {1027-1035},
 *    title = {k-means++: the advantages of carefull seeding},
 *    year = {2007}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -N &lt;num&gt;
 *  Number of clusters.
 *  (default 2).
 * </pre>
 * 
 * <pre>
 * -init
 *  Initialization method to use.
 *  0 = random, 1 = k-means++, 2 = canopy, 3 = farthest first.
 *  (default = 0)
 * </pre>
 * 
 * <pre>
 * -C
 *  Use canopies to reduce the number of distance calculations.
 * </pre>
 * 
 * <pre>
 * -max-candidates &lt;num&gt;
 *  Maximum number of candidate canopies to retain in memory
 *  at any one time when using canopy clustering.
 *  T2 distance plus, data characteristics,
 *  will determine how many candidate canopies are formed before
 *  periodic and final pruning are performed, which might result
 *  in exceess memory consumption. This setting avoids large numbers
 *  of candidate canopies consuming memory. (default = 100)
 * </pre>
 * 
 * <pre>
 * -periodic-pruning &lt;num&gt;
 *  How often to prune low density canopies when using canopy clustering. 
 *  (default = every 10,000 training instances)
 * </pre>
 * 
 * <pre>
 * -min-density
 *  Minimum canopy density, when using canopy clustering, below which
 *   a canopy will be pruned during periodic pruning. (default = 2 instances)
 * </pre>
 * 
 * <pre>
 * -t2
 *  The T2 distance to use when using canopy clustering. Values &lt; 0 indicate that
 *  a heuristic based on attribute std. deviation should be used to set this.
 *  (default = -1.0)
 * </pre>
 * 
 * <pre>
 * -t1
 *  The T1 distance to use when using canopy clustering. A value &lt; 0 is taken as a
 *  positive multiplier for T2. (default = -1.5)
 * </pre>
 * 
 * <pre>
 * -V
 *  Display std. deviations for centroids.
 * </pre>
 * 
 * <pre>
 * -M
 *  Don't replace missing values with mean/mode.
 * </pre>
 * 
 * <pre>
 * -A &lt;classname and options&gt;
 *  Distance function to use.
 *  (default: weka.core.EuclideanDistance)
 * </pre>
 * 
 * <pre>
 * -I &lt;num&gt;
 *  Maximum number of iterations.
 * </pre>
 * 
 * <pre>
 * -O
 *  Preserve order of instances.
 * </pre>
 * 
 * <pre>
 * -fast
 *  Enables faster distance calculations, using cut-off values.
 *  Disables the calculation/output of squared errors/distances.
 * </pre>
 * 
 * <pre>
 * -num-slots &lt;num&gt;
 *  Number of execution slots.
 *  (default 1 - i.e. no parallelism)
 * </pre>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  Random number seed.
 *  (default 10)
 * </pre>
 * 
 * <pre>
 * -output-debug-info
 *  If set, clusterer is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <pre>
 * -do-not-check-capabilities
 *  If set, clusterer capabilities are not checked before clusterer is built
 *  (use with caution).
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 11444 $
 * @see RandomizableClusterer
 */
public class SimpleKMeans extends RandomizableClusterer implements
  NumberOfClustersRequestable, WeightedInstancesHandler,
  TechnicalInformationHandler {

  /** for serialization. */
  static final long serialVersionUID = -3235809600124455376L;

  /**
   * replace missing values in training instances.
   */
  protected ReplaceMissingValues m_ReplaceMissingFilter;

  /**
   * number of clusters to generate.
   */
  protected int m_NumClusters = 2;

  /**
   * Holds the initial start points, as supplied by the initialization method
   * used
   */
  protected Instances m_initialStartPoints;

  /**
   * holds the cluster centroids.
   */
  protected Instances m_ClusterCentroids;

  /**
   * Holds the standard deviations of the numeric attributes in each cluster.
   */
  protected Instances m_ClusterStdDevs;

  /**
   * For each cluster, holds the frequency counts for the values of each nominal
   * attribute.
   */
  protected double[][][] m_ClusterNominalCounts;
  protected double[][] m_ClusterMissingCounts;

  /**
   * Stats on the full data set for comparison purposes. In case the attribute
   * is numeric the value is the mean if is being used the Euclidian distance or
   * the median if Manhattan distance and if the attribute is nominal then it's
   * mode is saved.
   */
  protected double[] m_FullMeansOrMediansOrModes;
  protected double[] m_FullStdDevs;
  protected double[][] m_FullNominalCounts;
  protected double[] m_FullMissingCounts;

  /**
   * Display standard deviations for numeric atts.
   */
  protected boolean m_displayStdDevs;

  /**
   * Replace missing values globally?
   */
  protected boolean m_dontReplaceMissing = false;

  /**
   * The number of instances in each cluster.
   */
  protected double[] m_ClusterSizes;

  /**
   * Maximum number of iterations to be executed.
   */
  protected int m_MaxIterations = 500;

  /**
   * Keep track of the number of iterations completed before convergence.
   */
  protected int m_Iterations = 0;

  /**
   * Holds the squared errors for all clusters.
   */
  protected double[] m_squaredErrors;

  /** the distance function used. */
  protected DistanceFunction m_DistanceFunction = new EuclideanDistance();

  /**
   * Preserve order of instances.
   */
  protected boolean m_PreserveOrder = false;

  /**
   * Assignments obtained.
   */
  protected int[] m_Assignments = null;

  /** whether to use fast calculation of distances (using a cut-off). */
  protected boolean m_FastDistanceCalc = false;

  public static final int RANDOM = 0;
  public static final int KMEANS_PLUS_PLUS = 1;
  public static final int CANOPY = 2;
  public static final int FARTHEST_FIRST = 3;

  /** Initialization methods */
  public static final Tag[] TAGS_SELECTION = { new Tag(RANDOM, "Random"),
    new Tag(KMEANS_PLUS_PLUS, "k-means++"), new Tag(CANOPY, "Canopy"),
    new Tag(FARTHEST_FIRST, "Farthest first") };

  /** The initialization method to use */
  protected int m_initializationMethod = RANDOM;

  /**
   * Whether to reducet the number of distance calcs done by k-means with
   * canopies
   */
  protected boolean m_speedUpDistanceCompWithCanopies = false;

  /** Canopies that each centroid falls into (determined by T1 radius) */
  protected List<long[]> m_centroidCanopyAssignments;

  /** Canopies that each training instance falls into (determined by T1 radius) */
  protected List<long[]> m_dataPointCanopyAssignments;

  /** The canopy clusterer (if being used) */
  protected Canopy m_canopyClusters;

  /**
   * The maximum number of candidate canopies to hold in memory at any one time
   * (if using canopy clustering)
   */
  protected int m_maxCanopyCandidates = 100;

  /**
   * Prune low-density candidate canopies after every x instances have been seen
   * (if using canopy clustering)
   */
  protected int m_periodicPruningRate = 10000;

  /**
   * The minimum cluster density (according to T2 distance) allowed. Used when
   * periodically pruning candidate canopies (if using canopy clustering)
   */
  protected double m_minClusterDensity = 2;

  /** The t2 radius to pass through to Canopy */
  protected double m_t2 = Canopy.DEFAULT_T2;

  /** The t1 radius to pass through to Canopy */
  protected double m_t1 = Canopy.DEFAULT_T1;

  /** Number of threads to run */
  protected int m_executionSlots = 1;

  /** For parallel execution mode */
  protected transient ExecutorService m_executorPool;

  /**
   * the default constructor.
   */
  public SimpleKMeans() {
    super();

    m_SeedDefault = 10;
    setSeed(m_SeedDefault);
  }

  /**
   * Start the pool of execution threads
   */
  protected void startExecutorPool() {
    if (m_executorPool != null) {
      m_executorPool.shutdownNow();
    }

    m_executorPool = Executors.newFixedThreadPool(m_executionSlots);
  }

  protected int m_completed;
  protected int m_failed;

  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "D. Arthur and S. Vassilvitskii");
    result.setValue(Field.TITLE,
      "k-means++: the advantages of carefull seeding");
    result.setValue(Field.BOOKTITLE, "Proceedings of the eighteenth annual "
      + "ACM-SIAM symposium on Discrete algorithms");
    result.setValue(Field.YEAR, "2007");
    result.setValue(Field.PAGES, "1027-1035");

    return result;
  }

  /**
   * Returns a string describing this clusterer.
   * 
   * @return a description of the evaluator suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Cluster data using the k means algorithm. Can use either "
      + "the Euclidean distance (default) or the Manhattan distance."
      + " If the Manhattan distance is used, then centroids are computed "
      + "as the component-wise median rather than mean."
      + " For more information see:\n\n" + getTechnicalInformation().toString();
  }

  /**
   * Returns default capabilities of the clusterer.
   * 
   * @return the capabilities of this clusterer
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();
    result.enable(Capability.NO_CLASS);

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    return result;
  }

  private class KMeansComputeCentroidTask implements Callable<double[]> {

    protected Instances m_cluster;
    protected int m_centroidIndex;

    public KMeansComputeCentroidTask(int centroidIndex, Instances cluster) {
      m_cluster = cluster;
      m_centroidIndex = centroidIndex;
    }

    @Override
    public double[] call() {
      return moveCentroid(m_centroidIndex, m_cluster, true, false);
    }
  }

  /**
   * Launch the move centroids tasks
   * 
   * @param clusters the cluster centroids
   * @return the number of empty clusters
   */
  protected int launchMoveCentroids(Instances[] clusters) {
    int emptyClusterCount = 0;
    List<Future<double[]>> results = new ArrayList<Future<double[]>>();

    for (int i = 0; i < m_NumClusters; i++) {
      if (clusters[i].numInstances() == 0) {
        emptyClusterCount++;
      } else {
        Future<double[]> futureCentroid =
          m_executorPool.submit(new KMeansComputeCentroidTask(i, clusters[i]));
        results.add(futureCentroid);
      }
    }

    try {
      for (Future<double[]> d : results) {
        m_ClusterCentroids.add(new DenseInstance(1.0, d.get()));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return emptyClusterCount;
  }

  private class KMeansClusterTask implements Callable<Boolean> {

    protected int m_start;
    protected int m_end;
    protected Instances m_inst;
    protected int[] m_clusterAssignments;

    public KMeansClusterTask(Instances inst, int start, int end,
      int[] clusterAssignments) {
      m_start = start;
      m_end = end;
      m_inst = inst;
      m_clusterAssignments = clusterAssignments;
    }

    @Override
    public Boolean call() {
      boolean converged = true;
      for (int i = m_start; i < m_end; i++) {
        Instance toCluster = m_inst.instance(i);
        long[] instanceCanopies =
          m_speedUpDistanceCompWithCanopies ? m_dataPointCanopyAssignments
            .get(i) : null;
        int newC = clusterInstance(toCluster, instanceCanopies);
        if (newC != m_clusterAssignments[i]) {
          converged = false;
        }
        m_clusterAssignments[i] = newC;
      }

      return converged;
    }

    protected int clusterInstance(Instance inst, long[] instanceCanopies) {
      double minDist = Integer.MAX_VALUE;
      int bestCluster = 0;
      for (int i = 0; i < m_NumClusters; i++) {
        double dist;

        if (m_speedUpDistanceCompWithCanopies && instanceCanopies != null
          && instanceCanopies.length > 0) {
          try {
            if (!Canopy.nonEmptyCanopySetIntersection(
              m_centroidCanopyAssignments.get(i), instanceCanopies)) {
              // System.err.println("Skipping distance calc... "
              // + Canopy.printSingleAssignment(instanceCanopies));
              continue;
            }
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }

        dist =
          m_DistanceFunction.distance(inst, m_ClusterCentroids.instance(i),
            minDist);

        if (dist < minDist) {
          minDist = dist;
          bestCluster = i;
        }
      }

      return bestCluster;
    }
  }

  /**
   * Launch the tasks that assign instances to clusters
   * 
   * @param insts the instances to be clustered
   * @param clusterAssignments the array of cluster assignments
   * @return true if k means has converged
   * @throws Exception if a problem occurs
   */
  protected boolean launchAssignToClusters(Instances insts,
    int[] clusterAssignments) throws Exception {
    int numPerTask = insts.numInstances() / m_executionSlots;

    List<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
    for (int i = 0; i < m_executionSlots; i++) {
      int start = i * numPerTask;
      int end = start + numPerTask;
      if (i == m_executionSlots - 1) {
        end = insts.numInstances();
      }

      Future<Boolean> futureKM =
        m_executorPool.submit(new KMeansClusterTask(insts, start, end,
          clusterAssignments));
      results.add(futureKM);
    }

    boolean converged = true;
    for (Future<Boolean> f : results) {
      if (!f.get()) {
        converged = false;
      }
    }

    return converged;
  }

  /**
   * Generates a clusterer. Has to initialize all fields of the clusterer that
   * are not being set via options.
   * 
   * @param data set of instances serving as training data
   * @throws Exception if the clusterer has not been generated successfully
   */
  @Override
  public void buildClusterer(Instances data) throws Exception {

    m_canopyClusters = null;

    // can clusterer handle the data?
    getCapabilities().testWithFail(data);

    m_Iterations = 0;

    m_ReplaceMissingFilter = new ReplaceMissingValues();
    Instances instances = new Instances(data);

    instances.setClassIndex(-1);
    if (!m_dontReplaceMissing) {
      m_ReplaceMissingFilter.setInputFormat(instances);
      instances = Filter.useFilter(instances, m_ReplaceMissingFilter);
    }

    m_ClusterNominalCounts = new double[m_NumClusters][instances.numAttributes()][];
    m_ClusterMissingCounts = new double[m_NumClusters][instances.numAttributes()];
    if (m_displayStdDevs) {
      m_FullStdDevs = instances.variances();
    }
   
    m_FullMeansOrMediansOrModes = moveCentroid(0, instances, true, false);
    
    m_FullMissingCounts = m_ClusterMissingCounts[0];
    m_FullNominalCounts = m_ClusterNominalCounts[0];
    double sumOfWeights = instances.sumOfWeights();
    for (int i = 0; i < instances.numAttributes(); i++) {
      if (instances.attribute(i).isNumeric()) {
        if (m_displayStdDevs) {
          m_FullStdDevs[i] = Math.sqrt(m_FullStdDevs[i]);
        }
        if (m_FullMissingCounts[i] == sumOfWeights) {
          m_FullMeansOrMediansOrModes[i] = Double.NaN; // mark missing as mean
        }
      } else {
        if (m_FullMissingCounts[i] > m_FullNominalCounts[i][Utils
          .maxIndex(m_FullNominalCounts[i])]) {
          m_FullMeansOrMediansOrModes[i] = -1; // mark missing as most common
                                               // value
        }
      }
    }

    m_ClusterCentroids = new Instances(instances, m_NumClusters);
    int[] clusterAssignments = new int[instances.numInstances()];

    if (m_PreserveOrder) {
      m_Assignments = clusterAssignments;
    }

    m_DistanceFunction.setInstances(instances);

    Random RandomO = new Random(getSeed());
    int instIndex;
    HashMap<DecisionTableHashKey, Integer> initC =
      new HashMap<DecisionTableHashKey, Integer>();
    DecisionTableHashKey hk = null;

    Instances initInstances = null;
    if (m_PreserveOrder) {
      initInstances = new Instances(instances);
    } else {
      initInstances = instances;
    }

    if (m_speedUpDistanceCompWithCanopies) {
      m_canopyClusters = new Canopy();
      m_canopyClusters.setNumClusters(m_NumClusters);
      m_canopyClusters.setSeed(getSeed());
      m_canopyClusters.setT2(getCanopyT2());
      m_canopyClusters.setT1(getCanopyT1());
      m_canopyClusters
        .setMaxNumCandidateCanopiesToHoldInMemory(getCanopyMaxNumCanopiesToHoldInMemory());
      m_canopyClusters.setPeriodicPruningRate(getCanopyPeriodicPruningRate());
      m_canopyClusters.setMinimumCanopyDensity(getCanopyMinimumCanopyDensity());
      m_canopyClusters.setDebug(getDebug());
      m_canopyClusters.buildClusterer(initInstances);
      // System.err.println(m_canopyClusters);
      m_centroidCanopyAssignments = new ArrayList<long[]>();
      m_dataPointCanopyAssignments = new ArrayList<long[]>();
    }

    if (m_initializationMethod == KMEANS_PLUS_PLUS) {
      kMeansPlusPlusInit(initInstances);

      m_initialStartPoints = new Instances(m_ClusterCentroids);
    } else if (m_initializationMethod == CANOPY) {
      canopyInit(initInstances);

      m_initialStartPoints = new Instances(m_canopyClusters.getCanopies());
    } else if (m_initializationMethod == FARTHEST_FIRST) {
      farthestFirstInit(initInstances);

      m_initialStartPoints = new Instances(m_ClusterCentroids);
    } else {
      // random
      for (int j = initInstances.numInstances() - 1; j >= 0; j--) {
        instIndex = RandomO.nextInt(j + 1);
        hk =
          new DecisionTableHashKey(initInstances.instance(instIndex),
            initInstances.numAttributes(), true);
        if (!initC.containsKey(hk)) {
          m_ClusterCentroids.add(initInstances.instance(instIndex));
          initC.put(hk, null);
        }
        initInstances.swap(j, instIndex);

        if (m_ClusterCentroids.numInstances() == m_NumClusters) {
          break;
        }
      }

      m_initialStartPoints = new Instances(m_ClusterCentroids);
    }

    if (m_speedUpDistanceCompWithCanopies) {
      // assign canopies to training data
      for (int i = 0; i < instances.numInstances(); i++) {
        m_dataPointCanopyAssignments.add(m_canopyClusters
          .assignCanopies(instances.instance(i)));
      }
    }

    m_NumClusters = m_ClusterCentroids.numInstances();

    // removing reference
    initInstances = null;

    int i;
    boolean converged = false;
    int emptyClusterCount;
    Instances[] tempI = new Instances[m_NumClusters];
    m_squaredErrors = new double[m_NumClusters];
    m_ClusterNominalCounts = new double[m_NumClusters][instances.numAttributes()][0];
    m_ClusterMissingCounts = new double[m_NumClusters][instances.numAttributes()];
    startExecutorPool();

    while (!converged) {
      if (m_speedUpDistanceCompWithCanopies) {
        // re-assign canopies to the current cluster centers
        m_centroidCanopyAssignments.clear();
        for (int kk = 0; kk < m_ClusterCentroids.numInstances(); kk++) {
          m_centroidCanopyAssignments.add(m_canopyClusters
            .assignCanopies(m_ClusterCentroids.instance(kk)));
        }
      }

      emptyClusterCount = 0;
      m_Iterations++;
      converged = true;

      if (m_executionSlots <= 1
        || instances.numInstances() < 2 * m_executionSlots) {
        for (i = 0; i < instances.numInstances(); i++) {
          Instance toCluster = instances.instance(i);
          int newC =
            clusterProcessedInstance(
              toCluster,
              false,
              true,
              m_speedUpDistanceCompWithCanopies ? m_dataPointCanopyAssignments
                .get(i) : null);
          if (newC != clusterAssignments[i]) {
            converged = false;
          }
          clusterAssignments[i] = newC;
        }
      } else {
        converged = launchAssignToClusters(instances, clusterAssignments);
      }

      // update centroids
      m_ClusterCentroids = new Instances(instances, m_NumClusters);
      for (i = 0; i < m_NumClusters; i++) {
        tempI[i] = new Instances(instances, 0);
      }
      for (i = 0; i < instances.numInstances(); i++) {
        tempI[clusterAssignments[i]].add(instances.instance(i));
      }
      if (m_executionSlots <= 1
        || instances.numInstances() < 2 * m_executionSlots) {
        for (i = 0; i < m_NumClusters; i++) {
          if (tempI[i].numInstances() == 0) {
            // empty cluster
            emptyClusterCount++;
          } else {
            moveCentroid(i, tempI[i], true, true);
          }
        }
      } else {
        emptyClusterCount = launchMoveCentroids(tempI);
      }

      if (m_Iterations == m_MaxIterations) {
        converged = true;
      }

      if (emptyClusterCount > 0) {
        m_NumClusters -= emptyClusterCount;
        if (converged) {
          Instances[] t = new Instances[m_NumClusters];
          int index = 0;
          for (int k = 0; k < tempI.length; k++) {
            if (tempI[k].numInstances() > 0) {
              t[index] = tempI[k];

              for (i = 0; i < tempI[k].numAttributes(); i++) {
                m_ClusterNominalCounts[index][i] = m_ClusterNominalCounts[k][i];
              }
              index++;
            }
          }
          tempI = t;
        } else {
          tempI = new Instances[m_NumClusters];
        }
      }

      if (!converged) {
        m_ClusterNominalCounts = new double[m_NumClusters][instances.numAttributes()][0];
      }
    }

    // calculate errors
    if (!m_FastDistanceCalc) {
      for (i = 0; i < instances.numInstances(); i++) {
        clusterProcessedInstance(instances.instance(i), true, false, null);
      }
    }

    if (m_displayStdDevs) {
      m_ClusterStdDevs = new Instances(instances, m_NumClusters);
    }
    m_ClusterSizes = new double[m_NumClusters];
    for (i = 0; i < m_NumClusters; i++) {
      if (m_displayStdDevs) {
        double[] vals2 = tempI[i].variances();
        for (int j = 0; j < instances.numAttributes(); j++) {
          if (instances.attribute(j).isNumeric()) {
            vals2[j] = Math.sqrt(vals2[j]);
          } else {
            vals2[j] = Utils.missingValue();
          }
        }
        m_ClusterStdDevs.add(new DenseInstance(1.0, vals2));
      }
      m_ClusterSizes[i] = tempI[i].sumOfWeights();
    }

    m_executorPool.shutdown();

    // save memory!
    m_DistanceFunction.clean();
  }

  /**
   * Initialize with the canopy centers of the Canopy clustering method
   * 
   * @param data the training data
   * @throws Exception if a problem occurs
   */
  protected void canopyInit(Instances data) throws Exception {
    if (m_canopyClusters == null) {
      m_canopyClusters = new Canopy();
      m_canopyClusters.setNumClusters(m_NumClusters);
      m_canopyClusters.setSeed(getSeed());
      m_canopyClusters.setT2(getCanopyT2());
      m_canopyClusters.setT1(getCanopyT1());
      m_canopyClusters
        .setMaxNumCandidateCanopiesToHoldInMemory(getCanopyMaxNumCanopiesToHoldInMemory());
      m_canopyClusters.setPeriodicPruningRate(getCanopyPeriodicPruningRate());
      m_canopyClusters.setMinimumCanopyDensity(getCanopyMinimumCanopyDensity());
      m_canopyClusters.setDebug(getDebug());
      m_canopyClusters.buildClusterer(data);
    }
    m_ClusterCentroids = m_canopyClusters.getCanopies();
  }

  /**
   * Initialize with the fartherst first centers
   * 
   * @param data the training data
   * @throws Exception if a problem occurs
   */
  protected void farthestFirstInit(Instances data) throws Exception {
    FarthestFirst ff = new FarthestFirst();
    ff.setNumClusters(m_NumClusters);
    ff.buildClusterer(data);

    m_ClusterCentroids = ff.getClusterCentroids();
  }

  /**
   * Initialize using the k-means++ method
   * 
   * @param data the training data
   * @throws Exception if a problem occurs
   */
  protected void kMeansPlusPlusInit(Instances data) throws Exception {
    Random randomO = new Random(getSeed());
    HashMap<DecisionTableHashKey, String> initC =
      new HashMap<DecisionTableHashKey, String>();

    // choose initial center uniformly at random
    int index = randomO.nextInt(data.numInstances());
    m_ClusterCentroids.add(data.instance(index));
    DecisionTableHashKey hk =
      new DecisionTableHashKey(data.instance(index), data.numAttributes(), true);
    initC.put(hk, null);

    int iteration = 0;
    int remainingInstances = data.numInstances() - 1;
    if (m_NumClusters > 1) {
      // proceed with selecting the rest

      // distances to the initial randomly chose center
      double[] distances = new double[data.numInstances()];
      double[] cumProbs = new double[data.numInstances()];
      for (int i = 0; i < data.numInstances(); i++) {
        distances[i] =
          m_DistanceFunction.distance(data.instance(i),
            m_ClusterCentroids.instance(iteration));
      }

      // now choose the remaining cluster centers
      for (int i = 1; i < m_NumClusters; i++) {

        // distances converted to probabilities
        double[] weights = new double[data.numInstances()];
        System.arraycopy(distances, 0, weights, 0, distances.length);
        Utils.normalize(weights);

        double sumOfProbs = 0;
        for (int k = 0; k < data.numInstances(); k++) {
          sumOfProbs += weights[k];
          cumProbs[k] = sumOfProbs;
        }

        cumProbs[data.numInstances() - 1] = 1.0; // make sure there are no
                                                 // rounding issues

        // choose a random instance
        double prob = randomO.nextDouble();
        for (int k = 0; k < cumProbs.length; k++) {
          if (prob < cumProbs[k]) {
            Instance candidateCenter = data.instance(k);
            hk =
              new DecisionTableHashKey(candidateCenter, data.numAttributes(),
                true);
            if (!initC.containsKey(hk)) {
              initC.put(hk, null);
              m_ClusterCentroids.add(candidateCenter);
            } else {
              // we shouldn't get here because any instance that is a duplicate
              // of
              // an already chosen cluster center should have zero distance (and
              // hence
              // zero probability of getting chosen) to that center.
              System.err.println("We shouldn't get here....");
            }
            remainingInstances--;
            break;
          }
        }
        iteration++;

        if (remainingInstances == 0) {
          break;
        }

        // prepare to choose the next cluster center.
        // check distances against the new cluster center to see if it is closer
        for (int k = 0; k < data.numInstances(); k++) {
          if (distances[k] > 0) {
            double newDist =
              m_DistanceFunction.distance(data.instance(k),
                m_ClusterCentroids.instance(iteration));
            if (newDist < distances[k]) {
              distances[k] = newDist;
            }
          }
        }
      }
    }
  }

  /**
   * Move the centroid to it's new coordinates. Generate the centroid
   * coordinates based on it's members (objects assigned to the cluster of the
   * centroid) and the distance function being used.
   * 
   * @param centroidIndex index of the centroid which the coordinates will be
   *          computed
   * @param members the objects that are assigned to the cluster of this
   *          centroid
   * @param updateClusterInfo if the method is supposed to update the m_Cluster
   *          arrays
   * @param addToCentroidInstances true if the method is to add the computed
   *          coordinates to the Instances holding the centroids
   * @return the centroid coordinates
   */
  protected double[] moveCentroid(int centroidIndex, Instances members,
    boolean updateClusterInfo, boolean addToCentroidInstances) {
    
    double[] vals = new double[members.numAttributes()];
    double[][] nominalDists = new double[members.numAttributes()][];
    double[] weightMissing = new double[members.numAttributes()];
    double[] weightNonMissing = new double[members.numAttributes()];
    
    // Quickly calculate some relevant statistics 
    for (int j = 0; j < members.numAttributes(); j++) {
      if (members.attribute(j).isNominal()) {
        nominalDists[j] = new double[members.attribute(j).numValues()];
      }
    }
    for (Instance inst : members) {
      for (int j = 0; j < members.numAttributes(); j++) {
        if (inst.isMissing(j)) {
          weightMissing[j] += inst.weight(); 
        } else {
          weightNonMissing[j] += inst.weight();
          if (members.attribute(j).isNumeric()) {
            vals[j] += inst.weight() * inst.value(j); // Will be overwritten in Manhattan case
          } else {
            nominalDists[j][(int)inst.value(j)] += inst.weight();
          }
        }
      }      
    }
    for (int j = 0; j < members.numAttributes(); j++) {
      if (members.attribute(j).isNumeric()) {
        if  (weightNonMissing[j] > 0) {
          vals[j] /= weightNonMissing[j];
        } else {
          vals[j] = Utils.missingValue();
        }
      } else {
        double max = -Double.MAX_VALUE;
        double maxIndex = -1;
        for (int i = 0; i < nominalDists[j].length; i++) {
          if (nominalDists[j][i] > max) {
            max = nominalDists[j][i];
            maxIndex = i;
          }
          if (max < weightMissing[j]) {
            vals[j] = Utils.missingValue();
          } else {
            vals[j] = maxIndex;
          }
        }
      }
    }
    
    if (m_DistanceFunction instanceof ManhattanDistance) {
      
      // Need to replace means by medians
      Instances sortedMembers = null;
      int middle = (members.numInstances() - 1) / 2;
      boolean dataIsEven = ((members.numInstances() % 2) == 0);
      if (m_PreserveOrder) {
        sortedMembers = members;
      } else {
        sortedMembers = new Instances(members);
      }
      for (int j = 0; j < members.numAttributes(); j++) {
        if ((weightNonMissing[j] > 0) && members.attribute(j).isNumeric()) {
          // singleton special case
          if (members.numInstances() == 1) {
            vals[j] = members.instance(0).value(j);
          } else {
            vals[j] = sortedMembers.kthSmallestValue(j, middle + 1);
            if (dataIsEven) {
              vals[j] = (vals[j] + sortedMembers.kthSmallestValue(j, middle + 2)) / 2;
            }
          }
        }
      }
    }
      
    if (updateClusterInfo) {   
      for (int j = 0; j < members.numAttributes(); j++) {
        m_ClusterMissingCounts[centroidIndex][j] = weightMissing[j];
        m_ClusterNominalCounts[centroidIndex][j] = nominalDists[j];
      } 
    }
    
    if (addToCentroidInstances) {
      m_ClusterCentroids.add(new DenseInstance(1.0, vals));
    }
    
    return vals;
  }

  /**
   * clusters an instance that has been through the filters.
   * 
   * @param instance the instance to assign a cluster to
   * @param updateErrors if true, update the within clusters sum of errors
   * @param useFastDistCalc whether to use the fast distance calculation or not
   * @param instanceCanopies the canopies covering the instance to be clustered,
   *          or null if not using the option to reduce the number of distance
   *          computations via canopies
   * @return a cluster number
   */
  private int clusterProcessedInstance(Instance instance, boolean updateErrors,
    boolean useFastDistCalc, long[] instanceCanopies) {
    double minDist = Integer.MAX_VALUE;
    int bestCluster = 0;
    for (int i = 0; i < m_NumClusters; i++) {
      double dist;
      if (useFastDistCalc) {
        if (m_speedUpDistanceCompWithCanopies && instanceCanopies != null
          && instanceCanopies.length > 0) {
          try {
            if (!Canopy.nonEmptyCanopySetIntersection(
              m_centroidCanopyAssignments.get(i), instanceCanopies)) {
              continue;
            }
          } catch (Exception ex) {
            ex.printStackTrace();
          }
          dist =
            m_DistanceFunction.distance(instance,
              m_ClusterCentroids.instance(i), minDist);
        } else {
          dist =
            m_DistanceFunction.distance(instance,
              m_ClusterCentroids.instance(i), minDist);
        }
      } else {
        dist =
          m_DistanceFunction.distance(instance, m_ClusterCentroids.instance(i));
      }
      if (dist < minDist) {
        minDist = dist;
        bestCluster = i;
      }
    }
    if (updateErrors) {
      if (m_DistanceFunction instanceof EuclideanDistance) {
        // Euclidean distance to Squared Euclidean distance
        minDist *= minDist * instance.weight();
      }
      m_squaredErrors[bestCluster] += minDist;
    }
    return bestCluster;
  }

  /**
   * Classifies a given instance.
   * 
   * @param instance the instance to be assigned to a cluster
   * @return the number of the assigned cluster as an interger if the class is
   *         enumerated, otherwise the predicted value
   * @throws Exception if instance could not be classified successfully
   */
  @Override
  public int clusterInstance(Instance instance) throws Exception {
    Instance inst = null;
    if (!m_dontReplaceMissing) {
      m_ReplaceMissingFilter.input(instance);
      m_ReplaceMissingFilter.batchFinished();
      inst = m_ReplaceMissingFilter.output();
    } else {
      inst = instance;
    }

    return clusterProcessedInstance(inst, false, true, null);
  }

  /**
   * Returns the number of clusters.
   * 
   * @return the number of clusters generated for a training dataset.
   * @throws Exception if number of clusters could not be returned successfully
   */
  @Override
  public int numberOfClusters() throws Exception {
    return m_NumClusters;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tNumber of clusters.\n" + "\t(default 2).",
      "N", 1, "-N <num>"));

    result.addElement(new Option(
      "\tInitialization method to use.\n\t0 = random, 1 = k-means++, "
        + "2 = canopy, 3 = farthest first.\n\t(default = 0)", "init", 1,
      "-init"));

    result.addElement(new Option(
      "\tUse canopies to reduce the number of distance calculations.", "C", 0,
      "-C"));

    result
      .addElement(new Option(
        "\tMaximum number of candidate canopies to retain in memory\n\t"
          + "at any one time when using canopy clustering.\n\t"
          + "T2 distance plus, data characteristics,\n\t"
          + "will determine how many candidate canopies are formed before\n\t"
          + "periodic and final pruning are performed, which might result\n\t"
          + "in exceess memory consumption. This setting avoids large numbers\n\t"
          + "of candidate canopies consuming memory. (default = 100)",
        "-max-candidates", 1, "-max-candidates <num>"));

    result
      .addElement(new Option(
        "\tHow often to prune low density canopies when using canopy clustering. \n\t"
          + "(default = every 10,000 training instances)", "periodic-pruning",
        1,
        "-periodic-pruning <num>"));

    result
      .addElement(new Option(
        "\tMinimum canopy density, when using canopy clustering, below which\n\t"
          + " a canopy will be pruned during periodic pruning. (default = 2 instances)",
        "min-density", 1, "-min-density"));

    result
      .addElement(new Option(
        "\tThe T2 distance to use when using canopy clustering. Values < 0 indicate that\n\t"
          + "a heuristic based on attribute std. deviation should be used to set this.\n\t"
          + "(default = -1.0)", "t2", 1, "-t2"));

    result
      .addElement(new Option(
        "\tThe T1 distance to use when using canopy clustering. A value < 0 is taken as a\n\t"
          + "positive multiplier for T2. (default = -1.5)", "t1", 1, "-t1"));

    result.addElement(new Option("\tDisplay std. deviations for centroids.\n",
      "V", 0, "-V"));
    result.addElement(new Option(
      "\tDon't replace missing values with mean/mode.\n", "M", 0, "-M"));

    result.add(new Option("\tDistance function to use.\n"
      + "\t(default: weka.core.EuclideanDistance)", "A", 1,
      "-A <classname and options>"));

    result.add(new Option("\tMaximum number of iterations.\n", "I", 1,
      "-I <num>"));

    result.addElement(new Option("\tPreserve order of instances.\n", "O", 0,
      "-O"));

    result.addElement(new Option(
      "\tEnables faster distance calculations, using cut-off values.\n"
        + "\tDisables the calculation/output of squared errors/distances.\n",
      "fast", 0, "-fast"));

    result.addElement(new Option("\tNumber of execution slots.\n"
      + "\t(default 1 - i.e. no parallelism)", "num-slots", 1,
      "-num-slots <num>"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numClustersTipText() {
    return "set number of clusters";
  }

  /**
   * set the number of clusters to generate.
   * 
   * @param n the number of clusters to generate
   * @throws Exception if number of clusters is negative
   */
  @Override
  public void setNumClusters(int n) throws Exception {
    if (n <= 0) {
      throw new Exception("Number of clusters must be > 0");
    }
    m_NumClusters = n;
  }

  /**
   * gets the number of clusters to generate.
   * 
   * @return the number of clusters to generate
   */
  public int getNumClusters() {
    return m_NumClusters;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String initializationMethodTipText() {
    return "The initialization method to use. Random, k-means++, Canopy or farthest first";
  }

  /**
   * Set the initialization method to use
   * 
   * @param method the initialization method to use
   */
  public void setInitializationMethod(SelectedTag method) {
    if (method.getTags() == TAGS_SELECTION) {
      m_initializationMethod = method.getSelectedTag().getID();
    }
  }

  /**
   * Get the initialization method to use
   * 
   * @return method the initialization method to use
   */
  public SelectedTag getInitializationMethod() {
    return new SelectedTag(m_initializationMethod, TAGS_SELECTION);
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String reduceNumberOfDistanceCalcsViaCanopiesTipText() {
    return "Use canopy clustering to reduce the number of distance calculations "
      + "performed by k-means";
  }

  /**
   * Set whether to use canopies to reduce the number of distance computations
   * required
   * 
   * @param c true if canopies are to be used to reduce the number of distance
   *          computations
   */
  public void setReduceNumberOfDistanceCalcsViaCanopies(boolean c) {
    m_speedUpDistanceCompWithCanopies = c;
  }

  /**
   * Get whether to use canopies to reduce the number of distance computations
   * required
   * 
   * @return true if canopies are to be used to reduce the number of distance
   *         computations
   */
  public boolean getReduceNumberOfDistanceCalcsViaCanopies() {
    return m_speedUpDistanceCompWithCanopies;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String canopyPeriodicPruningRateTipText() {
    return "If using canopy clustering for initialization and/or speedup "
      + "this is how often to prune low density canopies during training";
  }

  /**
   * Set the how often to prune low density canopies during training (if using
   * canopy clustering)
   * 
   * @param p how often (every p instances) to prune low density canopies
   */
  public void setCanopyPeriodicPruningRate(int p) {
    m_periodicPruningRate = p;
  }

  /**
   * Get the how often to prune low density canopies during training (if using
   * canopy clustering)
   * 
   * @return how often (every p instances) to prune low density canopies
   */
  public int getCanopyPeriodicPruningRate() {
    return m_periodicPruningRate;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String canopyMinimumCanopyDensityTipText() {
    return "If using canopy clustering for initialization and/or speedup "
      + "this is the minimum T2-based density "
      + "below which a canopy will be pruned during periodic pruning";
  }

  /**
   * Set the minimum T2-based density below which a canopy will be pruned during
   * periodic pruning.
   * 
   * @param dens the minimum canopy density
   */
  public void setCanopyMinimumCanopyDensity(double dens) {
    m_minClusterDensity = dens;
  }

  /**
   * Get the minimum T2-based density below which a canopy will be pruned during
   * periodic pruning.
   * 
   * @return the minimum canopy density
   */
  public double getCanopyMinimumCanopyDensity() {
    return m_minClusterDensity;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String canopyMaxNumCanopiesToHoldInMemoryTipText() {
    return "If using canopy clustering for initialization and/or speedup "
      + "this is the maximum number of candidate canopies to "
      + "retain in main memory during training of the canopy clusterer. "
      + "T2 distance and data characteristics determine how many candidate "
      + "canopies are formed before periodic and final pruning are performed. There "
      + "may not be enough memory available if T2 is set too low.";
  }

  /**
   * Set the maximum number of candidate canopies to retain in memory during
   * training. T2 distance and data characteristics determine how many candidate
   * canopies are formed before periodic and final pruning are performed. There
   * may not be enough memory available if T2 is set too low.
   * 
   * @param max the maximum number of candidate canopies to retain in memory
   *          during training
   */
  public void setCanopyMaxNumCanopiesToHoldInMemory(int max) {
    m_maxCanopyCandidates = max;
  }

  /**
   * Get the maximum number of candidate canopies to retain in memory during
   * training. T2 distance and data characteristics determine how many candidate
   * canopies are formed before periodic and final pruning are performed. There
   * may not be enough memory available if T2 is set too low.
   * 
   * @return the maximum number of candidate canopies to retain in memory during
   *         training
   */
  public int getCanopyMaxNumCanopiesToHoldInMemory() {
    return m_maxCanopyCandidates;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String canopyT2TipText() {
    return "The T2 distance to use when using canopy clustering. Values < 0 indicate that this should be set using "
      + "a heuristic based on attribute standard deviation";
  }

  /**
   * Set the t2 radius to use when canopy clustering is being used as start
   * points and/or to reduce the number of distance calcs
   * 
   * @param t2 the t2 radius to use
   */
  public void setCanopyT2(double t2) {
    m_t2 = t2;
  }

  /**
   * Get the t2 radius to use when canopy clustering is being used as start
   * points and/or to reduce the number of distance calcs
   * 
   * @return the t2 radius to use
   */
  public double getCanopyT2() {
    return m_t2;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String canopyT1TipText() {
    return "The T1 distance to use when using canopy clustering. Values < 0 are taken as a positive "
      + "multiplier for the T2 distance";
  }

  /**
   * Set the t1 radius to use when canopy clustering is being used as start
   * points and/or to reduce the number of distance calcs
   * 
   * @param t1 the t1 radius to use
   */
  public void setCanopyT1(double t1) {
    m_t1 = t1;
  }

  /**
   * Get the t1 radius to use when canopy clustering is being used as start
   * points and/or to reduce the number of distance calcs
   * 
   * @return the t1 radius to use
   */
  public double getCanopyT1() {
    return m_t1;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String maxIterationsTipText() {
    return "set maximum number of iterations";
  }

  /**
   * set the maximum number of iterations to be executed.
   * 
   * @param n the maximum number of iterations
   * @throws Exception if maximum number of iteration is smaller than 1
   */
  public void setMaxIterations(int n) throws Exception {
    if (n <= 0) {
      throw new Exception("Maximum number of iterations must be > 0");
    }
    m_MaxIterations = n;
  }

  /**
   * gets the number of maximum iterations to be executed.
   * 
   * @return the number of clusters to generate
   */
  public int getMaxIterations() {
    return m_MaxIterations;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String displayStdDevsTipText() {
    return "Display std deviations of numeric attributes "
      + "and counts of nominal attributes.";
  }

  /**
   * Sets whether standard deviations and nominal count. Should be displayed in
   * the clustering output.
   * 
   * @param stdD true if std. devs and counts should be displayed
   */
  public void setDisplayStdDevs(boolean stdD) {
    m_displayStdDevs = stdD;
  }

  /**
   * Gets whether standard deviations and nominal count. Should be displayed in
   * the clustering output.
   * 
   * @return true if std. devs and counts should be displayed
   */
  public boolean getDisplayStdDevs() {
    return m_displayStdDevs;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String dontReplaceMissingValuesTipText() {
    return "Replace missing values globally with mean/mode.";
  }

  /**
   * Sets whether missing values are to be replaced.
   * 
   * @param r true if missing values are to be replaced
   */
  public void setDontReplaceMissingValues(boolean r) {
    m_dontReplaceMissing = r;
  }

  /**
   * Gets whether missing values are to be replaced.
   * 
   * @return true if missing values are to be replaced
   */
  public boolean getDontReplaceMissingValues() {
    return m_dontReplaceMissing;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String distanceFunctionTipText() {
    return "The distance function to use for instances comparison "
      + "(default: weka.core.EuclideanDistance). ";
  }

  /**
   * returns the distance function currently in use.
   * 
   * @return the distance function
   */
  public DistanceFunction getDistanceFunction() {
    return m_DistanceFunction;
  }

  /**
   * sets the distance function to use for instance comparison.
   * 
   * @param df the new distance function to use
   * @throws Exception if instances cannot be processed
   */
  public void setDistanceFunction(DistanceFunction df) throws Exception {
    if (!(df instanceof EuclideanDistance)
      && !(df instanceof ManhattanDistance)) {
      throw new Exception(
        "SimpleKMeans currently only supports the Euclidean and Manhattan distances.");
    }
    m_DistanceFunction = df;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String preserveInstancesOrderTipText() {
    return "Preserve order of instances.";
  }

  /**
   * Sets whether order of instances must be preserved.
   * 
   * @param r true if missing values are to be replaced
   */
  public void setPreserveInstancesOrder(boolean r) {
    m_PreserveOrder = r;
  }

  /**
   * Gets whether order of instances must be preserved.
   * 
   * @return true if missing values are to be replaced
   */
  public boolean getPreserveInstancesOrder() {
    return m_PreserveOrder;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String fastDistanceCalcTipText() {
    return "Uses cut-off values for speeding up distance calculation, but "
      + "suppresses also the calculation and output of the within cluster sum "
      + "of squared errors/sum of distances.";
  }

  /**
   * Sets whether to use faster distance calculation.
   * 
   * @param value true if faster calculation to be used
   */
  public void setFastDistanceCalc(boolean value) {
    m_FastDistanceCalc = value;
  }

  /**
   * Gets whether to use faster distance calculation.
   * 
   * @return true if faster calculation is used
   */
  public boolean getFastDistanceCalc() {
    return m_FastDistanceCalc;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numExecutionSlotsTipText() {
    return "The number of execution slots (threads) to use. "
      + "Set equal to the number of available cpu/cores";
  }

  /**
   * Set the degree of parallelism to use.
   * 
   * @param slots the number of tasks to run in parallel when computing the
   *          nearest neighbors and evaluating different values of k between the
   *          lower and upper bounds
   */
  public void setNumExecutionSlots(int slots) {
    m_executionSlots = slots;
  }

  /**
   * Get the degree of parallelism to use.
   * 
   * @return the number of tasks to run in parallel when computing the nearest
   *         neighbors and evaluating different values of k between the lower
   *         and upper bounds
   */
  public int getNumExecutionSlots() {
    return m_executionSlots;
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -N &lt;num&gt;
   *  Number of clusters.
   *  (default 2).
   * </pre>
   * 
   * <pre>
   * -init
   *  Initialization method to use.
   *  0 = random, 1 = k-means++, 2 = canopy, 3 = farthest first.
   *  (default = 0)
   * </pre>
   * 
   * <pre>
   * -C
   *  Use canopies to reduce the number of distance calculations.
   * </pre>
   * 
   * <pre>
   * -max-candidates &lt;num&gt;
   *  Maximum number of candidate canopies to retain in memory
   *  at any one time when using canopy clustering.
   *  T2 distance plus, data characteristics,
   *  will determine how many candidate canopies are formed before
   *  periodic and final pruning are performed, which might result
   *  in exceess memory consumption. This setting avoids large numbers
   *  of candidate canopies consuming memory. (default = 100)
   * </pre>
   * 
   * <pre>
   * -periodic-pruning &lt;num&gt;
   *  How often to prune low density canopies when using canopy clustering. 
   *  (default = every 10,000 training instances)
   * </pre>
   * 
   * <pre>
   * -min-density
   *  Minimum canopy density, when using canopy clustering, below which
   *   a canopy will be pruned during periodic pruning. (default = 2 instances)
   * </pre>
   * 
   * <pre>
   * -t2
   *  The T2 distance to use when using canopy clustering. Values &lt; 0 indicate that
   *  a heuristic based on attribute std. deviation should be used to set this.
   *  (default = -1.0)
   * </pre>
   * 
   * <pre>
   * -t1
   *  The T1 distance to use when using canopy clustering. A value &lt; 0 is taken as a
   *  positive multiplier for T2. (default = -1.5)
   * </pre>
   * 
   * <pre>
   * -V
   *  Display std. deviations for centroids.
   * </pre>
   * 
   * <pre>
   * -M
   *  Don't replace missing values with mean/mode.
   * </pre>
   * 
   * <pre>
   * -A &lt;classname and options&gt;
   *  Distance function to use.
   *  (default: weka.core.EuclideanDistance)
   * </pre>
   * 
   * <pre>
   * -I &lt;num&gt;
   *  Maximum number of iterations.
   * </pre>
   * 
   * <pre>
   * -O
   *  Preserve order of instances.
   * </pre>
   * 
   * <pre>
   * -fast
   *  Enables faster distance calculations, using cut-off values.
   *  Disables the calculation/output of squared errors/distances.
   * </pre>
   * 
   * <pre>
   * -num-slots &lt;num&gt;
   *  Number of execution slots.
   *  (default 1 - i.e. no parallelism)
   * </pre>
   * 
   * <pre>
   * -S &lt;num&gt;
   *  Random number seed.
   *  (default 10)
   * </pre>
   * 
   * <pre>
   * -output-debug-info
   *  If set, clusterer is run in debug mode and
   *  may output additional info to the console
   * </pre>
   * 
   * <pre>
   * -do-not-check-capabilities
   *  If set, clusterer capabilities are not checked before clusterer is built
   *  (use with caution).
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    m_displayStdDevs = Utils.getFlag("V", options);
    m_dontReplaceMissing = Utils.getFlag("M", options);

    String initM = Utils.getOption("init", options);
    if (initM.length() > 0) {
      setInitializationMethod(new SelectedTag(Integer.parseInt(initM),
        TAGS_SELECTION));
    }

    m_speedUpDistanceCompWithCanopies = Utils.getFlag('C', options);

    String temp = Utils.getOption("max-candidates", options);
    if (temp.length() > 0) {
      setCanopyMaxNumCanopiesToHoldInMemory(Integer.parseInt(temp));
    }

    temp = Utils.getOption("periodic-pruning", options);
    if (temp.length() > 0) {
      setCanopyPeriodicPruningRate(Integer.parseInt(temp));
    }

    temp = Utils.getOption("min-density", options);
    if (temp.length() > 0) {
      setCanopyMinimumCanopyDensity(Double.parseDouble(temp));
    }

    temp = Utils.getOption("t2", options);
    if (temp.length() > 0) {
      setCanopyT2(Double.parseDouble(temp));
    }

    temp = Utils.getOption("t1", options);
    if (temp.length() > 0) {
      setCanopyT1(Double.parseDouble(temp));
    }

    String optionString = Utils.getOption('N', options);

    if (optionString.length() != 0) {
      setNumClusters(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption("I", options);
    if (optionString.length() != 0) {
      setMaxIterations(Integer.parseInt(optionString));
    }

    String distFunctionClass = Utils.getOption('A', options);
    if (distFunctionClass.length() != 0) {
      String distFunctionClassSpec[] = Utils.splitOptions(distFunctionClass);
      if (distFunctionClassSpec.length == 0) {
        throw new Exception("Invalid DistanceFunction specification string.");
      }
      String className = distFunctionClassSpec[0];
      distFunctionClassSpec[0] = "";

      setDistanceFunction((DistanceFunction) Utils.forName(
        DistanceFunction.class, className, distFunctionClassSpec));
    } else {
      setDistanceFunction(new EuclideanDistance());
    }

    m_PreserveOrder = Utils.getFlag("O", options);

    m_FastDistanceCalc = Utils.getFlag("fast", options);

    String slotsS = Utils.getOption("num-slots", options);
    if (slotsS.length() > 0) {
      setNumExecutionSlots(Integer.parseInt(slotsS));
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of SimpleKMeans.
   * 
   * @return an array of strings suitable for passing to setOptions()
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    result.add("-init");
    result.add("" + getInitializationMethod().getSelectedTag().getID());

    if (m_speedUpDistanceCompWithCanopies) {
      result.add("-C");
    }

    result.add("-max-candidates");
    result.add("" + getCanopyMaxNumCanopiesToHoldInMemory());
    result.add("-periodic-pruning");
    result.add("" + getCanopyPeriodicPruningRate());
    result.add("-min-density");
    result.add("" + getCanopyMinimumCanopyDensity());
    result.add("-t1");
    result.add("" + getCanopyT1());
    result.add("-t2");
    result.add("" + getCanopyT2());

    if (m_displayStdDevs) {
      result.add("-V");
    }

    if (m_dontReplaceMissing) {
      result.add("-M");
    }

    result.add("-N");
    result.add("" + getNumClusters());

    result.add("-A");
    result.add((m_DistanceFunction.getClass().getName() + " " + Utils
      .joinOptions(m_DistanceFunction.getOptions())).trim());

    result.add("-I");
    result.add("" + getMaxIterations());

    if (m_PreserveOrder) {
      result.add("-O");
    }

    if (m_FastDistanceCalc) {
      result.add("-fast");
    }

    result.add("-num-slots");
    result.add("" + getNumExecutionSlots());

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * return a string describing this clusterer.
   * 
   * @return a description of the clusterer as a string
   */
  @Override
  public String toString() {
    if (m_ClusterCentroids == null) {
      return "No clusterer built yet!";
    }

    int maxWidth = 0;
    int maxAttWidth = 0;
    boolean containsNumeric = false;
    for (int i = 0; i < m_NumClusters; i++) {
      for (int j = 0; j < m_ClusterCentroids.numAttributes(); j++) {
        if (m_ClusterCentroids.attribute(j).name().length() > maxAttWidth) {
          maxAttWidth = m_ClusterCentroids.attribute(j).name().length();
        }
        if (m_ClusterCentroids.attribute(j).isNumeric()) {
          containsNumeric = true;
          double width =
            Math.log(Math.abs(m_ClusterCentroids.instance(i).value(j)))
              / Math.log(10.0);

          if (width < 0) {
            width = 1;
          }
          // decimal + # decimal places + 1
          width += 6.0;
          if ((int) width > maxWidth) {
            maxWidth = (int) width;
          }
        }
      }
    }

    for (int i = 0; i < m_ClusterCentroids.numAttributes(); i++) {
      if (m_ClusterCentroids.attribute(i).isNominal()) {
        Attribute a = m_ClusterCentroids.attribute(i);
        for (int j = 0; j < m_ClusterCentroids.numInstances(); j++) {
          String val = a.value((int) m_ClusterCentroids.instance(j).value(i));
          if (val.length() > maxWidth) {
            maxWidth = val.length();
          }
        }
        for (int j = 0; j < a.numValues(); j++) {
          String val = a.value(j) + " ";
          if (val.length() > maxAttWidth) {
            maxAttWidth = val.length();
          }
        }
      }
    }

    if (m_displayStdDevs) {
      // check for maximum width of maximum frequency count
      for (int i = 0; i < m_ClusterCentroids.numAttributes(); i++) {
        if (m_ClusterCentroids.attribute(i).isNominal()) {
          int maxV = Utils.maxIndex(m_FullNominalCounts[i]);
          /*
           * int percent = (int)((double)m_FullNominalCounts[i][maxV] /
           * Utils.sum(m_ClusterSizes) * 100.0);
           */
          int percent = 6; // max percent width (100%)
          String nomV = "" + m_FullNominalCounts[i][maxV];
          // + " (" + percent + "%)";
          if (nomV.length() + percent > maxWidth) {
            maxWidth = nomV.length() + 1;
          }
        }
      }
    }

    // check for size of cluster sizes
    for (double m_ClusterSize : m_ClusterSizes) {
      String size = "(" + m_ClusterSize + ")";
      if (size.length() > maxWidth) {
        maxWidth = size.length();
      }
    }

    if (m_displayStdDevs && maxAttWidth < "missing".length()) {
      maxAttWidth = "missing".length();
    }

    String plusMinus = "+/-";
    maxAttWidth += 2;
    if (m_displayStdDevs && containsNumeric) {
      maxWidth += plusMinus.length();
    }
    if (maxAttWidth < "Attribute".length() + 2) {
      maxAttWidth = "Attribute".length() + 2;
    }

    if (maxWidth < "Full Data".length()) {
      maxWidth = "Full Data".length() + 1;
    }

    if (maxWidth < "missing".length()) {
      maxWidth = "missing".length() + 1;
    }

    StringBuffer temp = new StringBuffer();
    temp.append("\nkMeans\n======\n");
    temp.append("\nNumber of iterations: " + m_Iterations);

    if (!m_FastDistanceCalc) {
      temp.append("\n");
      if (m_DistanceFunction instanceof EuclideanDistance) {
        temp.append("Within cluster sum of squared errors: "
          + Utils.sum(m_squaredErrors));
      } else {
        temp.append("Sum of within cluster distances: "
          + Utils.sum(m_squaredErrors));
      }
    }

    temp.append("\n\nInitial starting points (");
    switch (m_initializationMethod) {
    case FARTHEST_FIRST:
      temp.append("farthest first");
      break;
    case KMEANS_PLUS_PLUS:
      temp.append("k-means++");
      break;
    case CANOPY:
      temp.append("canopy");
      break;
    default:
      temp.append("random");
    }
    temp.append("):\n");
    if (m_initializationMethod != CANOPY) {
      temp.append("\n");
      for (int i = 0; i < m_initialStartPoints.numInstances(); i++) {
        temp.append("Cluster " + i + ": " + m_initialStartPoints.instance(i))
          .append("\n");
      }
    } else {
      temp.append(m_canopyClusters.toString(false));
    }

    if (m_speedUpDistanceCompWithCanopies) {
      temp
        .append("\nReduced number of distance calculations by using canopies.");
      if (m_initializationMethod != CANOPY) {
        temp.append("\nCanopy T2 radius: "
          + String.format("%-10.3f", m_canopyClusters.getActualT2()));
        temp.append(
          "\nCanopy T1 radius: "
            + String.format("%-10.3f", m_canopyClusters.getActualT1())).append(
          "\n");
      }
    }

    if (!m_dontReplaceMissing) {
      temp.append("\nMissing values globally replaced with mean/mode");
    }

    temp.append("\n\nFinal cluster centroids:\n");
    temp.append(pad("Cluster#", " ", (maxAttWidth + (maxWidth * 2 + 2))
      - "Cluster#".length(), true));

    temp.append("\n");
    temp
      .append(pad("Attribute", " ", maxAttWidth - "Attribute".length(), false));

    temp
      .append(pad("Full Data", " ", maxWidth + 1 - "Full Data".length(), true));

    // cluster numbers
    for (int i = 0; i < m_NumClusters; i++) {
      String clustNum = "" + i;
      temp.append(pad(clustNum, " ", maxWidth + 1 - clustNum.length(), true));
    }
    temp.append("\n");

    // cluster sizes
    String cSize = "(" + Utils.sum(m_ClusterSizes) + ")";
    temp.append(pad(cSize, " ", maxAttWidth + maxWidth + 1 - cSize.length(),
      true));
    for (int i = 0; i < m_NumClusters; i++) {
      cSize = "(" + m_ClusterSizes[i] + ")";
      temp.append(pad(cSize, " ", maxWidth + 1 - cSize.length(), true));
    }
    temp.append("\n");

    temp.append(pad("", "=",
      maxAttWidth
        + (maxWidth * (m_ClusterCentroids.numInstances() + 1)
          + m_ClusterCentroids.numInstances() + 1), true));
    temp.append("\n");

    for (int i = 0; i < m_ClusterCentroids.numAttributes(); i++) {
      String attName = m_ClusterCentroids.attribute(i).name();
      temp.append(attName);
      for (int j = 0; j < maxAttWidth - attName.length(); j++) {
        temp.append(" ");
      }

      String strVal;
      String valMeanMode;
      // full data
      if (m_ClusterCentroids.attribute(i).isNominal()) {
        if (m_FullMeansOrMediansOrModes[i] == -1) { // missing
          valMeanMode =
            pad("missing", " ", maxWidth + 1 - "missing".length(), true);
        } else {
          valMeanMode =
            pad(
              (strVal =
                m_ClusterCentroids.attribute(i).value(
                  (int) m_FullMeansOrMediansOrModes[i])), " ", maxWidth + 1
                - strVal.length(), true);
        }
      } else {
        if (Double.isNaN(m_FullMeansOrMediansOrModes[i])) {
          valMeanMode =
            pad("missing", " ", maxWidth + 1 - "missing".length(), true);
        } else {
          valMeanMode =
            pad(
              (strVal =
                Utils.doubleToString(m_FullMeansOrMediansOrModes[i], maxWidth,
                  4).trim()), " ", maxWidth + 1 - strVal.length(), true);
        }
      }
      temp.append(valMeanMode);

      for (int j = 0; j < m_NumClusters; j++) {
        if (m_ClusterCentroids.attribute(i).isNominal()) {
          if (m_ClusterCentroids.instance(j).isMissing(i)) {
            valMeanMode =
              pad("missing", " ", maxWidth + 1 - "missing".length(), true);
          } else {
            valMeanMode =
              pad(
                (strVal =
                  m_ClusterCentroids.attribute(i).value(
                    (int) m_ClusterCentroids.instance(j).value(i))), " ",
                maxWidth + 1 - strVal.length(), true);
          }
        } else {
          if (m_ClusterCentroids.instance(j).isMissing(i)) {
            valMeanMode =
              pad("missing", " ", maxWidth + 1 - "missing".length(), true);
          } else {
            valMeanMode =
              pad(
                (strVal =
                  Utils.doubleToString(m_ClusterCentroids.instance(j).value(i),
                    maxWidth, 4).trim()), " ", maxWidth + 1 - strVal.length(),
                true);
          }
        }
        temp.append(valMeanMode);
      }
      temp.append("\n");

      if (m_displayStdDevs) {
        // Std devs/max nominal
        String stdDevVal = "";

        if (m_ClusterCentroids.attribute(i).isNominal()) {
          // Do the values of the nominal attribute
          Attribute a = m_ClusterCentroids.attribute(i);
          for (int j = 0; j < a.numValues(); j++) {
            // full data
            String val = "  " + a.value(j);
            temp.append(pad(val, " ", maxAttWidth + 1 - val.length(), false));
            double count = m_FullNominalCounts[i][j];
            int percent =
              (int) ((double) m_FullNominalCounts[i][j]
                / Utils.sum(m_ClusterSizes) * 100.0);
            String percentS = "" + percent + "%)";
            percentS = pad(percentS, " ", 5 - percentS.length(), true);
            stdDevVal = "" + count + " (" + percentS;
            stdDevVal =
              pad(stdDevVal, " ", maxWidth + 1 - stdDevVal.length(), true);
            temp.append(stdDevVal);

            // Clusters
            for (int k = 0; k < m_NumClusters; k++) {
              percent =
                (int) ((double) m_ClusterNominalCounts[k][i][j]
                  / m_ClusterSizes[k] * 100.0);
              percentS = "" + percent + "%)";
              percentS = pad(percentS, " ", 5 - percentS.length(), true);
              stdDevVal = "" + m_ClusterNominalCounts[k][i][j] + " (" + percentS;
              stdDevVal =
                pad(stdDevVal, " ", maxWidth + 1 - stdDevVal.length(), true);
              temp.append(stdDevVal);
            }
            temp.append("\n");
          }
          // missing (if any)
          if (m_FullMissingCounts[i] > 0) {
            // Full data
            temp.append(pad("  missing", " ",
              maxAttWidth + 1 - "  missing".length(), false));
            double count = m_FullMissingCounts[i];
            int percent =
              (int) ((double) m_FullMissingCounts[i]
                / Utils.sum(m_ClusterSizes) * 100.0);
            String percentS = "" + percent + "%)";
            percentS = pad(percentS, " ", 5 - percentS.length(), true);
            stdDevVal = "" + count + " (" + percentS;
            stdDevVal =
              pad(stdDevVal, " ", maxWidth + 1 - stdDevVal.length(), true);
            temp.append(stdDevVal);

            // Clusters
            for (int k = 0; k < m_NumClusters; k++) {
              percent =
                (int) ((double) m_ClusterMissingCounts[k][i]
                  / m_ClusterSizes[k] * 100.0);
              percentS = "" + percent + "%)";
              percentS = pad(percentS, " ", 5 - percentS.length(), true);
              stdDevVal = "" + m_ClusterMissingCounts[k][i] + " (" + percentS;
              stdDevVal =
                pad(stdDevVal, " ", maxWidth + 1 - stdDevVal.length(), true);
              temp.append(stdDevVal);
            }

            temp.append("\n");
          }

          temp.append("\n");
        } else {
          // Full data
          if (Double.isNaN(m_FullMeansOrMediansOrModes[i])) {
            stdDevVal = pad("--", " ", maxAttWidth + maxWidth + 1 - 2, true);
          } else {
            stdDevVal =
              pad(
                (strVal =
                  plusMinus
                    + Utils.doubleToString(m_FullStdDevs[i], maxWidth, 4)
                      .trim()), " ",
                maxWidth + maxAttWidth + 1 - strVal.length(), true);
          }
          temp.append(stdDevVal);

          // Clusters
          for (int j = 0; j < m_NumClusters; j++) {
            if (m_ClusterCentroids.instance(j).isMissing(i)) {
              stdDevVal = pad("--", " ", maxWidth + 1 - 2, true);
            } else {
              stdDevVal =
                pad(
                  (strVal =
                    plusMinus
                      + Utils.doubleToString(
                        m_ClusterStdDevs.instance(j).value(i), maxWidth, 4)
                        .trim()), " ", maxWidth + 1 - strVal.length(), true);
            }
            temp.append(stdDevVal);
          }
          temp.append("\n\n");
        }
      }
    }

    temp.append("\n\n");
    return temp.toString();
  }

  private String
    pad(String source, String padChar, int length, boolean leftPad) {
    StringBuffer temp = new StringBuffer();

    if (leftPad) {
      for (int i = 0; i < length; i++) {
        temp.append(padChar);
      }
      temp.append(source);
    } else {
      temp.append(source);
      for (int i = 0; i < length; i++) {
        temp.append(padChar);
      }
    }
    return temp.toString();
  }

  /**
   * Gets the the cluster centroids.
   * 
   * @return the cluster centroids
   */
  public Instances getClusterCentroids() {
    return m_ClusterCentroids;
  }

  /**
   * Gets the standard deviations of the numeric attributes in each cluster.
   * 
   * @return the standard deviations of the numeric attributes in each cluster
   */
  public Instances getClusterStandardDevs() {
    return m_ClusterStdDevs;
  }

  /**
   * Returns for each cluster the weighted frequency counts for the values of each
   * nominal attribute.
   * 
   * @return the counts
   */
  public double[][][] getClusterNominalCounts() {
    return m_ClusterNominalCounts;
  }

  /**
   * Gets the squared error for all clusters.
   * 
   * @return the squared error, NaN if fast distance calculation is used
   * @see #m_FastDistanceCalc
   */
  public double getSquaredError() {
    if (m_FastDistanceCalc) {
      return Double.NaN;
    } else {
      return Utils.sum(m_squaredErrors);
    }
  }

  /**
   * Gets the sum of weights for all the instances in each cluster.
   * 
   * @return The number of instances in each cluster
   */
  public double[] getClusterSizes() {
    return m_ClusterSizes;
  }

  /**
   * Gets the assignments for each instance.
   * 
   * @return Array of indexes of the centroid assigned to each instance
   * @throws Exception if order of instances wasn't preserved or no assignments
   *           were made
   */
  public int[] getAssignments() throws Exception {
    if (!m_PreserveOrder) {
      throw new Exception(
        "The assignments are only available when order of instances is preserved (-O)");
    }
    if (m_Assignments == null) {
      throw new Exception("No assignments made.");
    }
    return m_Assignments;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11444 $");
  }

  /**
   * Main method for executing this class.
   * 
   * @param args use -h to list all parameters
   */
  public static void main(String[] args) {
    runClusterer(new SimpleKMeans(), args);
  }
}
