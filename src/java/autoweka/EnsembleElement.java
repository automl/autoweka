package autoweka;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EnsembleElement{


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

    public String toString(){
        return "Classifier Class: "+classifierClass+"\nArgs:[+\n"+String.join(",",classifierArgs)+"]\n";
    }

    public Configuration getConfiguration(){ return this.configuration; }

    public String getClassifierClass(){
        return this.classifierClass;
    }

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

            //For debugging TODO remove later
            long startTime = System.currentTimeMillis();

            trainingInstances = as.reduceDimensionality(trainingInstances);
            classifier.buildClassifier(trainingInstances);

            //For debugging TODO remove later
            long stopTime = System.currentTimeMillis();
            double finalTrainTime = (stopTime - startTime) / 1000.0;
            System.out.println("Trained Element!:[\n"+(String.join(" ",this.classifierArgs))+"\n]");
            System.out.println("Element had been trained in "+this.configuration.getAmtFolds()+" folds");
            System.out.println("final train time for this element: "+finalTrainTime+" s");

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
                throw new RuntimeException("Caught an exception while trying to cache predictions for the EnsembleElement with argstrings:"+configuration.getArgStrings()+"\nError message:"+e.toString());
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