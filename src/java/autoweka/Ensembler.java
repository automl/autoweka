package autoweka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.classifiers.meta.AutoWEKAClassifier.Metric;


import static weka.classifiers.meta.AutoWEKAClassifier.metricsToMax;

public class Ensembler {

    final static Logger log = LoggerFactory.getLogger(Ensembler.class);


    private List<Configuration> cfgList;  //List of configurations read from the configuration ranking file at rPath
    private List<EnsembleElement> availableEnsembleElements;

    private List<Ensemble> partialEnsembleTrajectory;
    private List<Double>   partialEnsembleTrajectoryScores; //Temporary, i guess. For making the NO_IMPROVEMENT check faster.
    private Ensemble incumbentEnsemble;

    private Instances trainingSet;
    private Instances validationSet;

    static final String DEFAULT_STOP_CRITERION = "ENSEMBLE_SIZE";
    static final int    DEFAULT_NO_IMPROVEMENT_LIMIT = 3;
    static final int    DEFAULT_MAX_ENSEMBLE_SIZE = 10;

    static final String DEFAULT_HILLCLIMB_ALGORITHM = "BASIC";
    static final String DEFAULT_ENSEMBLE_EVALUATION_ALGORITHM = "MAJORITY_VOTING";


    private String currentStopCriterion;
    private int maxEnsembleSize;
    private int noImprovementLimit;
    private String currentHillclimbAlgorithm;
    private String currentEnsembleEvaluationAlgorithm;



    public int getMaxEnsembleSize() {
        return maxEnsembleSize;
    }

    public void setMaxEnsembleSize(int maxEnsembleSize) {
        this.maxEnsembleSize = maxEnsembleSize;
    }

    public int getNoImprovementLimit() {
        return noImprovementLimit;
    }

    public void setNoImprovementLimit(int noImprovementLimit) {
        this.noImprovementLimit = noImprovementLimit;
    }

    public String getStopCriterion() {
        return currentStopCriterion;
    }

    public void setStopCriterion(String currentStopCriterion) {
        this.currentStopCriterion = currentStopCriterion;
    }

    public String getHillclimbAlgorithm() {
        return currentHillclimbAlgorithm;
    }

    public void setHillclimbAlgorithm(String currentHillclimbAlgorithm) {
        this.currentHillclimbAlgorithm = currentHillclimbAlgorithm;
    }

    public String getEnsembleEvaluationAlgorithm() {
        return currentEnsembleEvaluationAlgorithm;
    }

    public void setEnsembleEvaluationAlgorithm(String currentEnsembleEvaluationAlgorithm) {
        this.currentEnsembleEvaluationAlgorithm = currentEnsembleEvaluationAlgorithm;
    }

    //TODO make factory DP for hillclimbing options
    //TODO allow for embedded CV
    //TODO allow for backtracking
    //TODO allow for disk storage
    //TODO write the remaining stop criteria

    //TODO some constraints:
    // If no repetition allowed, max ensemble size has to be size of list
    // Cant do sorted initialization with more elements than we have in list
    // No improvement can't be longer than list length if no repetitions are allowed


    public Ensembler(Instances trainingSet, Instances validationSet, ConfigurationCollection cc){

        this.currentHillclimbAlgorithm = DEFAULT_HILLCLIMB_ALGORITHM;
        this.currentEnsembleEvaluationAlgorithm = DEFAULT_ENSEMBLE_EVALUATION_ALGORITHM;
        this.currentStopCriterion = DEFAULT_STOP_CRITERION;
        this.maxEnsembleSize = DEFAULT_MAX_ENSEMBLE_SIZE;
        this.noImprovementLimit = DEFAULT_NO_IMPROVEMENT_LIMIT;



        this.partialEnsembleTrajectory = new ArrayList<Ensemble>();
        this.partialEnsembleTrajectoryScores = new ArrayList<Double>();
        this.availableEnsembleElements = new ArrayList<EnsembleElement>();

        this.trainingSet = trainingSet;
        this.validationSet = validationSet;
        this.cfgList = cc.asArrayList(); //TODO we assume cc is sorted, since in AutoWEKAClassifier we use ConfigurationRanker before the Ensembler. Fix it so that we don't assume anything.

        //TODO temp
        this.maxEnsembleSize = (cfgList.size()>5)?cfgList.size():5;// this.cfgList.size()-1;
        //TODO write a toString for the weka output
    }

    private EnsembleElement setUpEnsembleElement(Configuration c){

       EnsembleElement rv = new EnsembleElement(c);
       rv.train(this.trainingSet);
       rv.cachePredictions(this.validationSet);
       return rv;

    }

    private boolean isMetricToMax(Metric metric){
        return Arrays.asList(metricsToMax).contains(metric);
    }

    public Ensemble hillclimb(Metric metric, boolean repetitionsAllowed, int sortInitialization){
        if(currentHillclimbAlgorithm.equals("BASIC")){
            return this.basicHillclimb(metric, repetitionsAllowed, sortInitialization);
        }else if (currentHillclimbAlgorithm.equals("BAGGING")){
            return null; //TODO
        }else{
            throw new RuntimeException("Using an unknown hillclimb algorithm in hillclimb()");
        }
    }

    private boolean reachedStopCriterion(Metric metric){
        boolean metricToMax = isMetricToMax(metric);
        if(currentStopCriterion.equals("ENSEMBLE_SIZE")){

            //checks if ensemble has at least N elements, where N == value
            return (partialEnsembleTrajectory.size()>=maxEnsembleSize);

        }else if (currentStopCriterion.equals("NO_IMPROVEMENT")){

            //checks if current max/min is in the last N elements, where N == value
            if(metricToMax){
                Double currentMax = Collections.max(partialEnsembleTrajectoryScores);
                for(int i=1; i<=noImprovementLimit;i++){
                    if(partialEnsembleTrajectoryScores.get(partialEnsembleTrajectoryScores.size()-i).equals(currentMax)){
                        return false;
                    }
                }
                return true;
            }else{
                Double currentMin = Collections.min(partialEnsembleTrajectoryScores);
                for(int i=1; i<=noImprovementLimit;i++){
                    if(partialEnsembleTrajectoryScores.get(partialEnsembleTrajectoryScores.size()-i).equals(currentMin)){
                        return false;
                    }
                }
                return true;
            }

        }else if (currentStopCriterion.equals("SIZE_AND_IMPROVEMENT")){
            return false; //TODO
        }else{

            throw new RuntimeException("Invalid Stop Criterion type");

        }
    }


    private Ensemble getBestFromTrajectory(Metric metric){
        boolean metricToMax = isMetricToMax(metric);
        Ensemble incumbent = partialEnsembleTrajectory.get(0);

        for (Ensemble e : partialEnsembleTrajectory) {
            //TODO have a metric-generic compareTo in Ensemble
            Double incumbentPerformance = incumbent.getLastMeasuredPerformance(metric);
            Double ePerformance = e.getLastMeasuredPerformance(metric);
            if ((metricToMax && ePerformance > incumbentPerformance) || (!metricToMax && ePerformance < incumbentPerformance)) {
                incumbent = e;
            }
        }
        return incumbent;
    }

    private Ensemble basicHillclimb(Metric metric, boolean repetitionsAllowed, int sortInitialization){

        for (Configuration c: cfgList){
            availableEnsembleElements.add(this.setUpEnsembleElement(c));
        } //Already sorted in the constructor so we should be fine

        Ensemble currentPartialEnsemble = new Ensemble();
        List<EnsembleElement> localAvailableElements = new ArrayList<EnsembleElement>(availableEnsembleElements); //shallow copying
        System.out.println("LAE:"+localAvailableElements.toString());
        System.out.println("LAE length:"+localAvailableElements.toString());
        //Sorted initialization
        if(sortInitialization>0){
//
            for(int i = 0; i < sortInitialization ; i++){
                //TODO check if it makes sense to repeat models here in case repetition is allowed
                currentPartialEnsemble.appendElement(localAvailableElements.get(i));
                partialEnsembleTrajectory.add(currentPartialEnsemble);
                partialEnsembleTrajectoryScores.add(currentPartialEnsemble.measureEnsemblePerformance(this.validationSet,metric,"MAJORITY_VOTING"));
                currentPartialEnsemble = currentPartialEnsemble.shallowCopy();
            }
            if(!repetitionsAllowed){
                for (EnsembleElement ee : currentPartialEnsemble.getElements()){
                    localAvailableElements.remove(ee);
                }
            }

        } //leave this here unless youre sure it applies to any hillclimb in the same fashion

        //Taking steps in the hillclimbing
        while(!reachedStopCriterion(metric)){
            Ensemble nextStep = basicTakeStep(currentPartialEnsemble,localAvailableElements,metric); // cPE Will be empty or a shallow copy in iteration 1
            partialEnsembleTrajectory.add(nextStep);
            partialEnsembleTrajectoryScores.add(nextStep.getLastMeasuredPerformance(metric));
            if(!repetitionsAllowed){
                localAvailableElements.remove(nextStep.getElements().get(nextStep.getElements().size()-1));
            }
        }
        System.out.println("\nHillclimbing output:\n"+partialEnsembleTrajectory.toString()+"\n"+partialEnsembleTrajectoryScores.toString()+"\n-----\n");

        //Returning the output of the ES hillclimbing process
        this.incumbentEnsemble = this.getBestFromTrajectory(metric);

        System.out.println(incumbentEnsemble.getElements().toString());

        return incumbentEnsemble;
    }

    private Ensemble basicTakeStep(Ensemble currentPartialEnsemble, List<EnsembleElement> localAvailableElements, Metric metric){
        System.out.println("Starting step");
        boolean metricToMax = isMetricToMax(metric);
        Map<EnsembleElement,Double> optionScores = new HashMap<EnsembleElement,Double>();

        //Testing every option
        for(EnsembleElement eeOption : localAvailableElements){
            currentPartialEnsemble.appendElement(eeOption);
            optionScores.put(eeOption,currentPartialEnsemble.measureEnsemblePerformance(this.validationSet,metric,"MAJORITY_VOTING"));//TODO unhardcode MV
            currentPartialEnsemble.removeLastElement();
        }

        //Choosing best option
        Map.Entry<EnsembleElement, Double> firstEntry = optionScores.entrySet().iterator().next();
        EnsembleElement incumbentElement = firstEntry.getKey();
        Double incumbentPerformance = firstEntry.getValue();
        for (Map.Entry<EnsembleElement,Double> entry : optionScores.entrySet()){
            if((metricToMax && entry.getValue() > incumbentPerformance) || (!metricToMax && entry.getValue() < incumbentPerformance)){
                incumbentElement = entry.getKey();
                incumbentPerformance = entry.getValue();
            }
        }

        //Shallow copies the input step, adds new element and returns the next step
        Ensemble rv = currentPartialEnsemble.shallowCopy();
        rv.appendElement(incumbentElement);
        System.out.println("Finishing step");
        return rv;
    }

    //TODO option for ES with bagging

    private class Ensemble{

        private List<EnsembleElement> elements;
        private Map<Metric,Double> lastMeasuredPerformances;



        public Ensemble(){
            this.elements = new ArrayList<EnsembleElement>();
            this.lastMeasuredPerformances = new HashMap<Metric, Double>();
        }

        public Ensemble(List<EnsembleElement> elements){
            this.elements = new ArrayList<EnsembleElement>(elements); //shallow copying
            this.lastMeasuredPerformances = new HashMap<Metric, Double>();
//            System.out.println("elements: "+this.elements.toString()); //TODO delete later
        }

        public Ensemble shallowCopy(){
            Ensemble rv = new Ensemble(this.elements);
            for(Map.Entry<Metric,Double> e : lastMeasuredPerformances.entrySet()){
                rv.lastMeasuredPerformances.put(e.getKey(),e.getValue());
            }
            return rv;
        }

        public void appendElement(EnsembleElement ee){
            this.elements.add(ee);
        }

        public void removeLastElement(){
            this.elements.remove(this.elements.size()-1);
        }

        public Double measureEnsemblePerformance(Instances validationSet, Metric metric, String evaluateAlgorithm){
            //TODO so far it doesn't support multiple metrics. Only supports errorRate
            double incorrectAmount = 0;

            for(int i = 0; i<validationSet.numInstances(); i++){
                Instance currentInst = validationSet.instance(i);
                if(currentInst.classValue() != this.evaluateInstance(currentInst,evaluateAlgorithm)){
                    incorrectAmount+=1;
                }
            }
            double errorRate = (incorrectAmount/(double)validationSet.numInstances());
            System.out.println("eR:"+errorRate);
            this.lastMeasuredPerformances.put(metric,errorRate);
            return errorRate;
        }

        public Double getLastMeasuredPerformance(Metric m){
            Double rv =lastMeasuredPerformances.get(m);
            System.out.println("gLMP:"+rv);
            if( rv==null ){
                throw new RuntimeException("Trying to check the last measured performance for a metric whose performance was never measured");
            }else{
                return rv;
            }
        }

        private Double evaluateInstance(Instance i, String evaluateAlgorithm){
            if(evaluateAlgorithm.equals("MAJORITY_VOTING")){
                return evaluateByMajorityVoting(i);
            }else{
                throw new RuntimeException("Invalid ensemble evaluation algorithm");
            }
        }

        private Double evaluateByMajorityVoting(Instance i){
            Map<Double,Integer> votes = new HashMap<Double, Integer>();

            for(EnsembleElement ee : elements){
                Double vote = ee.evaluateInstance(i);
                Integer amount = votes.get(vote);
                if (amount == null) {
                    votes.put(vote, 1);
                }else{
                    votes.put(vote, amount + 1);
                }
            }

            Map.Entry<Double, Integer> firstEntry = votes.entrySet().iterator().next();
            Double  rv = firstEntry.getKey();
            Integer maxVotes = firstEntry.getValue();
            for(Map.Entry<Double,Integer> entry : votes.entrySet()){
                if(entry.getValue()>=maxVotes){
                    maxVotes = entry.getValue();
                    rv = entry.getKey();
                }
            }
            return rv;
        }

        public List<EnsembleElement> getElements() {
            return elements;
        }
    }

    private class EnsembleElement{


        private Configuration configuration;

        /** The chosen classifier. */
        protected Classifier classifier;
        /** The chosen attribute selection method. */
        protected AttributeSelection as;

        /** The class of the chosen classifier. */
        protected String   classifierClass;
        /** The arguments of the chosen classifier. */
        protected String[] classifierArgs;

        /** The class of the chosen attribute search method. */
        protected String   attributeSearchClass;
        /** The arguments of the chosen attribute search method. */
        protected String[] attributeSearchArgs;

        /** The class of the chosen attribute evaluation. */
        protected String   attributeEvalClass;
        /** The arguments of the chosen attribute evaluation method. */
        protected String[] attributeEvalArgs;


        private Map<Instance,Double> cachedPredictions;

        public EnsembleElement(Configuration configuration){
            this.configuration = configuration;
            this.cachedPredictions = new HashMap<Instance, Double>();

            WekaArgumentConverter.Arguments wekaArgs = WekaArgumentConverter.convert(Arrays.asList(configuration.getArgStrings().split(" ")));
            classifierClass = wekaArgs.propertyMap.get("targetclass");
            String  tempClassifierArgs = Util.joinStrings(" ", Util.quoteStrings(wekaArgs.argMap.get("classifier")));
            classifierArgs = Util.splitQuotedString(tempClassifierArgs).toArray(new String[0]);

            if(wekaArgs.propertyMap.containsKey("attributesearch") && !"NONE".equals(wekaArgs.propertyMap.get("attributesearch"))){
                attributeSearchClass = wekaArgs.propertyMap.get("attributesearch");
                String tempAttributeSearchArgs  = Util.joinStrings(" ", Util.quoteStrings(wekaArgs.argMap.get("attributesearch")));
                if(tempAttributeSearchArgs != null) {
                    attributeSearchArgs = Util.splitQuotedString(tempAttributeSearchArgs).toArray(new String[0]);
                }

                attributeEvalClass = wekaArgs.propertyMap.get("attributeeval");
                String tempAttributeEvalArgs  = Util.joinStrings(" ", Util.quoteStrings(wekaArgs.argMap.get("attributeeval")));
                if(tempAttributeEvalArgs != null) {
                    attributeEvalArgs = Util.splitQuotedString(tempAttributeEvalArgs).toArray(new String[0]);
                }
            }
        }

        public void train(Instances trainingInstances){

            //Training
            try{
                as = new AttributeSelection();

                if(attributeSearchClass != null) {
                    ASSearch asSearch = ASSearch.forName(attributeSearchClass, attributeSearchArgs.clone());
                    as.setSearch(asSearch);
                }
                if(attributeEvalClass != null) {
                    ASEvaluation asEval = ASEvaluation.forName(attributeEvalClass, attributeEvalArgs.clone());
                    as.setEvaluator(asEval);
                }
                as.SelectAttributes(trainingInstances);

                classifier = AbstractClassifier.forName(classifierClass, classifierArgs.clone());

                long startTime = System.currentTimeMillis();
                trainingInstances = as.reduceDimensionality(trainingInstances);
                classifier.buildClassifier(trainingInstances);
                long stopTime = System.currentTimeMillis();
                double finalTrainTime = (stopTime - startTime) / 1000.0;
            }catch (Exception e){
                throw new RuntimeException("Caught an exception while trying to train an EnsembleElement with argstrings:"+ configuration.getArgStrings());
            }

        }

        public void cachePredictions(Instances validationInstances){

            for(int i = 0; i<validationInstances.numInstances(); i++){
                Instance inst = validationInstances.instance(i);
                try{
                    Instance inst_withReduction = as.reduceDimensionality(inst);
                    cachedPredictions.put(inst,classifier.classifyInstance(inst_withReduction));
                }catch(Exception e){
//                    System.out.println(e.toString());
                    throw new RuntimeException("Caught an exception while trying to cache predictions for the EnsembleElement with argstrings:"+configuration.getArgStrings());
                }
            }

        }


        public double evaluateInstance(Instance i){
            Double rv = cachedPredictions.get(i);
            if(rv == null){
                throw new RuntimeException("Something wrong with the instance pointers. Trying to evaluate an instance whose prediction by the classifier wasn't cached");
            }else{
                return rv;
            }
        }
    }

}

