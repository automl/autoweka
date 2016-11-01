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
            return eval.errorRate();
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

    public static class AreaAboveROCMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return 1.0d - eval.areaUnderROC(1);
        }
    }

    public static class AreaUnderROCMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return -eval.areaUnderROC(1);
        }
    }

    public static class AvgCostMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return eval.avgCost();
        }
    }

    public static class CorrectMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return -eval.correct();
        }
    }

    public static class IncorrectMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return eval.incorrect();
        }
    }

    public static class CorrelationCoefficientMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            try {
                return -eval.correlationCoefficient();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class FalseNegativeRateMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return eval.falseNegativeRate(1);
        }
    }

    public static class FalsePositiveRateMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return eval.falsePositiveRate(1);
        }
    }

    public static class FMeasureMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return -eval.fMeasure(1);
        }
    }

    public static class KappaMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return -eval.kappa();
        }
    }

    public static class KBInformationMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            try {
                return -eval.KBInformation();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class KBMeanInformationMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            try {
                return -eval.KBMeanInformation();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class KBRelativeInformationMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            try {
                return -eval.KBRelativeInformation();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class PctCorrectMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return -eval.pctCorrect();
        }
    }

    public static class PctIncorrectMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return eval.pctIncorrect();
        }
    }

    public static class PrecisionMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return -eval.precision(1);
        }
    }

    public static class WeightedAreaUnderROCMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return -eval.weightedAreaUnderROC();
        }
    }

    public static class WeightedFalseNegativeRateMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return eval.weightedFalseNegativeRate();
        }
    }

    public static class WeightedFalsePositiveRateMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return eval.weightedFalsePositiveRate();
        }
    }

    public static class WeightedFMeasureMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return -eval.weightedFMeasure();
        }
    }

    public static class WeightedPrecisionMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return -eval.weightedPrecision();
        }
    }

    public static class WeightedRecallMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return -eval.weightedRecall();
        }
    }

    public static class WeightedTrueNegativeRateMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return -eval.weightedTrueNegativeRate();
        }
    }

    public static class WeightedTruePositiveRateMetric implements Metric
    {
        public double getDefault() { return INFINITY; }
        public double getScore(Evaluation eval, Instances testingData) {
            return -eval.weightedTruePositiveRate();
        }
    }

    private static Metric getMetricFromString(String className){
        className = className.trim();
        String metricName = "autoweka.ClassifierResult$" + className.substring(0, 1).toUpperCase() + className.substring(1) + "Metric";
        try
        {
            Class<?> cls = Class.forName(metricName);
            return (Metric)cls.newInstance();
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Could not find class '" + metricName + "'", e);
        }
        catch(Exception e)
        {
            if(e.getCause() != null)
                e.getCause().printStackTrace();
            throw new RuntimeException("Failed to instantiate '" + metricName + "'", e);
        }
    }

    private double mRawScore;
    private double mTrainingTime = 0;
    private double mEvaluationTime = 0;
    private double mAttributeSelectionTime = 0;
    private double mRegularizationPlenalty = 0;
    private boolean mCompleted = false;
    private AbstractClassifier mClassifier = null;
    private AttributeSelection mAttributeSelection = null;
    private Metric mMetric = null;
    private double mPercentEvaluated = 0;
    private boolean mMemOut;

    public ClassifierResult(String str)
    {
        this(getMetricFromString(str));
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

    /*
     * Really shouldn't use this method
     */
    public void _setRawScore(double score) {
        mRawScore = score;
    }

    public double getNormalizationPenalty() {
        return mRegularizationPlenalty;
    }

    public void setRegularizationPenalty(double penalty) {
        mRegularizationPlenalty = penalty;
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
     * @return The total time.
     */
    public double getTime(){
        return mAttributeSelectionTime + mTrainingTime + mEvaluationTime;
    }

    /**
     * Gets the 'score' for this result, which includes the regularization penalty plus the metric value
     * @return The score.
     */
    public double getScore()
    {
        double score = mRegularizationPlenalty + mRawScore;
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
