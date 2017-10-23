package autoweka;

import weka.classifiers.meta.AutoWEKAClassifier;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ensemble {

    private List<EnsembleElement> elements;
    private Map<AutoWEKAClassifier.Metric,Double> lastMeasuredPerformances;

    @Override
    public String toString(){
        return elements.toString();
    }


    public Ensemble(){
        this.elements = new ArrayList<EnsembleElement>();
        this.lastMeasuredPerformances = new HashMap<AutoWEKAClassifier.Metric, Double>();
    }

    public Ensemble(List<EnsembleElement> elements){
        this.elements = new ArrayList<EnsembleElement>(elements); //shallow copying
        this.lastMeasuredPerformances = new HashMap<AutoWEKAClassifier.Metric, Double>();
        //  //For debugging TODO remove later
        //  //System.out.println("elements: "+this.elements.toString());
    }

    public Ensemble shallowCopy(){
        Ensemble rv = new Ensemble(this.elements);
        for(Map.Entry<AutoWEKAClassifier.Metric,Double> e : lastMeasuredPerformances.entrySet()){
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

    public Double measureEnsemblePerformance(Instances validationSet, AutoWEKAClassifier.Metric metric, String evaluateAlgorithm){
        //TODO implement the computation for other metrics we might want. So far it only supports error rate
        double incorrectAmount = 0;

        for(int i = 0; i<validationSet.numInstances(); i++){
            Instance currentInst = validationSet.instance(i);
            if(currentInst.classValue() != this.evaluateInstance(currentInst,evaluateAlgorithm)){
                incorrectAmount+=1;
            }
        }
        double errorRate = (incorrectAmount/(double)validationSet.numInstances());

//            For debugging TODO remove later
//            System.out.println("eR:"+errorRate);
        this.lastMeasuredPerformances.put(metric,errorRate);
        return errorRate;
    }

    public Double getLastMeasuredPerformance(AutoWEKAClassifier.Metric m){
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