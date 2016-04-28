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
    public final static double INFINITY = 1e100d;

    /**
     * Generic interface for different Metrics
     *
     * You can define custom metrics and use them by using their full class name
     */
    public static interface Metric {
        public double getDefault();
        public double getScore(Evaluation eval, Instances testingData);
    }

    public static class ErrorRateMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData){
            return (double)(100 - eval.pctCorrect());
        }
    }

    public static class MeanAbsoluteErrorMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData){
            return eval.meanAbsoluteError();
        }
    }

    public static class RootMeanSquaredErrorMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData){
            return eval.rootMeanSquaredError();
        }
    }

    public static class RelativeAbsoluteErrorMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            try {
                return eval.relativeAbsoluteError();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static class RootRelativeSquaredErrorMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return eval.rootRelativeSquaredError();
        }
    }

    public static class AreaAboveROC implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return (1.0d - eval.areaUnderROC(1));
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

    private double mRawScore;
    private double mTrainingTime = 0;
    private double mEvaluationTime = 0;
    private double mAttributeSelectionTime = 0;
    private double mRegularizationPenalty = 1;
    private boolean mCompleted = false;
    private AbstractClassifier mClassifier = null;
    private AttributeSelection mAttributeSelection = null;
    private Metric mMetric = null;
    private double mPercentEvaluated = 0;
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

    public double getRawScore(){
        return mRawScore;
    }

    public void setScoreFromEval(Evaluation eval, Instances testingData) {
        mRawScore = mMetric.getScore(eval, testingData);
        if(Double.isInfinite(mRawScore)) {
            mRawScore = INFINITY;
        }
        if(mRawScore > INFINITY) {
            System.err.println("Score larger than our definition of infinity, adjusting.");
            mRawScore = INFINITY - 1;
        }
        setPercentEvaluated(eval);
    }

    public void setPercentEvaluated(Evaluation eval){
        mPercentEvaluated = 100.0 * (1.0 - eval.unclassified() / eval.numInstances());
    }

    public void setPercentEvaluated(double pct){
        mPercentEvaluated = pct;
    }
    public double getPercentEvaluated(){
        return mPercentEvaluated;
    }

    /**
     * Really shouldn't use this method
     */
    public void _setRawScore(double score) {
        mRawScore = score;
    }

    public double getNormalizationPenalty() {
        return mRegularizationPenalty;
    }

    public void setRegularizationPenalty(double penalty) {
        mRegularizationPenalty = penalty;
    }

    public double getTrainingTime(){
        return mTrainingTime;
    }

    public void setTrainingTime(double time){
        mTrainingTime = time;
    }

    public double getAttributeSelectionTime(){
        return mAttributeSelectionTime;
    }

    public void setAttributeSelectionTime(double time){
        mAttributeSelectionTime = time;
    }

    public double getEvaluationTime(){
        return mTrainingTime;
    }

    public void setEvaluationTime(double time){
        mEvaluationTime = time;
    }

    /**
     * Gets the total time spent dealing with this classifier
     */
    public double getTime(){
        return mAttributeSelectionTime + mTrainingTime + mEvaluationTime;
    }

    /**
     * Gets the 'score' for this result, which includes the regularization penalty plus the error rate
     */
    public double getScore()
    {
        //double score = mRegularizationPenalty * mRawScore;
        double score = mRawScore;
        return Double.isNaN(score) ? mMetric.getDefault() : score;
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

