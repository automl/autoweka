package autoweka.tools;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.LinkedList;

import weka.attributeSelection.AttributeSelection;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import autoweka.ClassifierRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrainedModelPredictionMaker
{

    final static Logger log = LoggerFactory.getLogger(TrainedModelPredictionMaker.class);

    public static void main(String[] argsArray)
    {
        String attributeSelectionObjPath = null;
        String modelObjPath = null;
        String instancesPath = null;
        String classIndex = "last";
        String predictionPath = "null";

        //Start reading in arguments
        LinkedList<String> args = new LinkedList<String>(Arrays.asList(argsArray));
        while(!args.isEmpty()){
            String arg = args.poll();
            if(arg.equals("-attributeselection")){
                attributeSelectionObjPath = args.poll();
            }else if(arg.equals("-model")){
                modelObjPath = args.poll();
            }else if(arg.equals("-dataset")){
                instancesPath = args.poll();
            }else if(args.equals("-classindex")){
                classIndex = args.poll();
            }else if(arg.equals("-predictionpath")){
                predictionPath = args.poll();
            }else{
                throw new RuntimeException("Unknown argument '" + arg + "'");
            }
        }

        if(modelObjPath == null)
            throw new RuntimeException("No trained model file provided");
        if(instancesPath == null)
            throw new RuntimeException("No dataset arff provided");

        TrainedModelPredictionMaker tmpm = new TrainedModelPredictionMaker(attributeSelectionObjPath, modelObjPath, instancesPath, classIndex, predictionPath);
        
        log.info(tmpm.eval.toSummaryString("\nResults\n======\n", false));
    }

    public Evaluation eval;
    
    public TrainedModelPredictionMaker(String attributeSelectionObjPath, String modelObjPath, String instancesPath, String classIndex, String predictionPath)
    {
        //Go forth and load some instances
        try
        {
            DataSource dataSource = new DataSource(new FileInputStream(instancesPath));
            Instances instances = dataSource.getDataSet();

            //Make sure to 
            if (instances.classIndex() == -1){
                if(classIndex.equals("last"))
                    instances.setClassIndex(instances.numAttributes() - 1);
                else
                    instances.setClassIndex(Integer.parseInt(classIndex));
            }

            //Load up the attribute selection if we need to
            if(attributeSelectionObjPath != null){
                AttributeSelection as = (AttributeSelection)weka.core.SerializationHelper.read(attributeSelectionObjPath);
                instances = as.reduceDimensionality(instances);
            }

            //Load up yonder classifier
            AbstractClassifier classifier = (AbstractClassifier)weka.core.SerializationHelper.read(modelObjPath);
            
            //Make the evaluation
            eval = new Evaluation(instances);
            ClassifierRunner.EvaluatorThread thrd = new ClassifierRunner.EvaluatorThread(eval, classifier, instances, predictionPath);
            thrd.run();
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
