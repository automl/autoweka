package autoweka;

import weka.attributeSelection.AttributeSelection;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

/**
 * Wraps all the data that comes from training a classifier so it can be passed back to a SMBO method
 */
public class ClassifierResult
{
    public final static float INFINITY = 1e10f;

    /**
     * Generic interface for different Metrics
     *
     * You can define custom metrics and use them by using their full class name
     */
    public static interface Metric {
        public float getDefault();
        public float getScore(Evaluation eval, Instances testingData);
    }

    public static class ErrorRateMetric implements Metric
    {
        public float getDefault() { return INFINITY; }
        public float getScore(Evaluation eval, Instances testingData){
            return (float)(100 - eval.pctCorrect());
        }
    }

    public static class MeanAbsoluteErrorMetric implements Metric
    {
        public float getDefault() { return INFINITY; }
        public float getScore(Evaluation eval, Instances testingData){
            return (float)eval.meanAbsoluteError();
        }
    }

    public static class RootMeanSquaredErrorMetric implements Metric
    {
        public float getDefault() { return INFINITY; }
        public float getScore(Evaluation eval, Instances testingData){
            return (float)eval.rootMeanSquaredError();
        }
    }

    public static class RelativeAbsoluteErrorMetric implements Metric
    {
        public float getDefault() { return INFINITY; }
        public float getScore(Evaluation eval, Instances testingData) {
            try {
                return (float)eval.relativeAbsoluteError();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static class RootRelativeSquaredErrorMetric implements Metric
    {
        public float getDefault() { return INFINITY; }
        public float getScore(Evaluation eval, Instances testingData) {
            return (float)eval.rootRelativeSquaredError();
        }
    }

    public static class AreaAboveROC implements Metric
    {
        public float getDefault() { return INFINITY; }
        public float getScore(Evaluation eval, Instances testingData) {
            return (float)(1.0 - eval.areaUnderROC(1));
        }
    }

    private static Metric getMetricFromString(String className){
        //Is it one of the names that we know about?
        if(className.equals("errorRate"))
            return new ErrorRateMetric();
        else if(className.equals("meanAbsoluteErrorMetric"))
            return new MeanAbsoluteErrorMetric();
        else if(className.equals("rmse"))
            return new RootMeanSquaredErrorMetric();
        else if(className.equals("relativeAbsoluteErrorMetric"))
            return new RelativeAbsoluteErrorMetric();
        else if(className.equals("rrse"))
            return new RootRelativeSquaredErrorMetric();
        // This portion of code added by Samantha Sanders 2014
        else if(className.equals("areaAboveROC"))
            return new AreaAboveROC();
        // End code added by Samantha Sanders

        //Nope, treat this as a class name
        try
        {
            className = className.trim();
            Class<?> cls = Class.forName(className);
            return (Metric)cls.newInstance();
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Could not find class '" + className + "'", e);
        }
        catch(Exception e)
        {
            if(e.getCause() != null)
                e.getCause().printStackTrace();
            throw new RuntimeException("Failed to instantiate '" + className + "'", e);
        }
    }

    private float mRawScore;
    private float mTrainingTime = 0;
    private float mEvaluationTime = 0;
    private float mAttributeSelectionTime = 0;
    private float mRegularizationPlenalty = 0;
    private boolean mCompleted = false;
    private AbstractClassifier mClassifier = null;
    private AttributeSelection mAttributeSelection = null;
    private Metric mMetric = null;
    private float mPercentEvaluated = 0;
    private boolean mMemOut;

    public ClassifierResult(String typeName)
    {
        this(getMetricFromString(typeName));
    }

    public ClassifierResult(Metric type)
    {
        mMetric = type;
        mRawScore = mMetric.getDefault();
    }

    public boolean getCompleted() {
        return mCompleted;
    }

    public void setCompleted(boolean completed){
        mCompleted = completed;
    }

    public float getRawScore(){
        return mRawScore;
    }

    public void setScoreFromEval(Evaluation eval, Instances testingData) {
        mRawScore = mMetric.getScore(eval, testingData);
        if(Float.isInfinite(mRawScore)) {
            mRawScore = INFINITY;
        }
        setPercentEvaluated(eval);
    }

    public void setPercentEvaluated(Evaluation eval){
        mPercentEvaluated = 100.0f*(float)(1.0f - eval.unclassified() / eval.numInstances());
    }

    public void setPercentEvaluated(float pct){
        mPercentEvaluated = pct;
    }
    public float getPercentEvaluated(){
        return mPercentEvaluated;
    }

    /**
     * Really shouldn't use this method
     */
    public void _setRawScore(float score) {
        mRawScore = score;
    }

    public float getNormalizationPenalty() {
        return mRegularizationPlenalty;
    }

    public void setRegularizationPenalty(float penalty) {
        mRegularizationPlenalty = penalty;
    }

    public float getTrainingTime(){
        return mTrainingTime;
    }

    public void setTrainingTime(float time){
        mTrainingTime = time;
    }

    public float getAttributeSelectionTime(){
        return mAttributeSelectionTime;
    }

    public void setAttributeSelectionTime(float time){
        mAttributeSelectionTime = time;
    }

    public float getEvaluationTime(){
        return mTrainingTime;
    }

    public void setEvaluationTime(float time){
        mEvaluationTime = time;
    }

    /**
     * Gets the total time spent dealing with this classifier
     */
    public float getTime(){
        return mAttributeSelectionTime + mTrainingTime + mEvaluationTime;
    }

    /**
     * Gets the 'score' for this result, which includes the regularization penalty plus the error rate
     */
    public float getScore()
    {
        float score = mRegularizationPlenalty + mRawScore;
        return Float.isNaN(score) ? mMetric.getDefault() : score;
    }

    public String getDescription()
    {
        return "Attribute Selection Time: " + mAttributeSelectionTime + " Training Time: " + mTrainingTime + " Evaluation Time: " + mEvaluationTime + " Score: " + getScore() + " Completed: " + mCompleted;
    }

    public void setClassifier(AbstractClassifier cls)
    {
        mClassifier = cls;
    }

    public AbstractClassifier getClassifier()
    {
        return mClassifier;
    }

    public void setAttributeSelection(AttributeSelection search)
    {
        mAttributeSelection = search;
    }

    public AttributeSelection getAttributeSelection()
    {
        return mAttributeSelection;
    }

    public void setMemOut(boolean memout)
    {
        mMemOut = memout;
    }

    public boolean getMemOut()
    {
        return mMemOut;
    }
}

