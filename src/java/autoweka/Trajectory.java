package autoweka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.annotation.*;

/** Class that captures a trajectory of an optimisation run
 */
@XmlRootElement(name="trajectory")
@XmlAccessorType(XmlAccessType.NONE)
public class Trajectory
{

    /**
     * Representation of a single point along the optimisation run
     */
    @XmlRootElement(name="point")
    @XmlAccessorType(XmlAccessType.NONE)
    public static class Point
    {
        /**
         * The time that this point was found by the SMBO method
         */
        @XmlElement(name="time")
        public double mTime;
        /**
         * The estimate of the error at this point in the trajectory that the SMBO method believes is true
         */
        @XmlElement(name="errorEstimate")
        public double mErrorEstimate;
        /**
         * The arg string that was used for the wrapper on this trajectory point
         */
        @XmlElement(name="args")
        public String mArgs;
        /**
         * Any extra data (aka trajectoryPointExtras) on given instance strings for this point
         */
        @XmlElement(name="specificInstanceInfo")
        public ArrayList<SpecificInstanceInfo> specificInstanceInfo = new ArrayList<SpecificInstanceInfo>();

        /**
         * Class used to store this extra data
         */
        @XmlRootElement(name="specific")
        @XmlAccessorType(XmlAccessType.NONE)
        public static class SpecificInstanceInfo
        {
            public SpecificInstanceInfo(){}
            public SpecificInstanceInfo(String _name, String _instance, double _error, double _time)
            {
                name = _name;
                instance = _instance;
                error = _error;
                time = _time;
            }
            @XmlElement(name="name")
            public String name;
            @XmlElement(name="instance")
            public String instance;
            @XmlElement(name="error")
            public double error;
            @XmlElement(name="time")
            public double time;
        }


        //Needed for the XML stuff
        private Point(){}

        public Point(double time, double errorEstimate, String args)
        {
            mTime = time;
            mErrorEstimate = errorEstimate;
            mArgs = args;
        }

        public double getTime() { return mTime; }
        public double getErrorEstimate() { return mErrorEstimate; }
        public String getArgs() { return mArgs; }

        public String toString() {
            return "time: " + mTime + ", error estimate: " + mErrorEstimate + ", args: " + mArgs;
        }
    }



    /**
     * The points in this trajectory
     */
    @XmlElement(name="point")
    private List<Point> mPoints = new ArrayList<Point>();

    /**
     * The seed of this trajectory
     */
    @XmlElement(name="seed")
    private String mSeed = "0";

    /**
     * The number of evaluations that were evaluated total in this run
     */
    @XmlElement(name="numEvaluatedEvaluations")
    private int mNumEvaluations = -1;

    /**
     * The number of evaluations that hit their memory limit in this run
     */
    @XmlElement(name="numMemOutEvaluations")
    private int mNumMemOutEvaluations = -1;

    /**
     * The number of evaluations that timed out in this run
     */
    @XmlElement(name="numTimeOutEvaluations")
    private int mNumTimeOutEvaluations = -1;

    //Needed for the XML stuff
    private Trajectory() {}

    /**
     * makes a new empty trajectory with the given seed
     * @param seed The seed.
     */
    public Trajectory(String seed)
    {
        mPoints = new ArrayList<Point>();
        mSeed = seed;
    }

    /**
     * Gets the seed for this trajectory
     * @return The seed.
     */
    public String getSeed() {
        return mSeed;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Point p: mPoints) {
            sb.append(p);
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Gets all the points inside this trajectory
     * @return The list of points.
     */
    public List<Trajectory.Point> getPoints()
    {
        return new ArrayList<Point>(mPoints);
    }

    /**
     * Inserts a new point onto the end of this trajectory
     * @param newPoint The new point.
     */
    public void addPoint(Point newPoint)
    {
        for(Point p: mPoints)
        {
            if(p.getTime() > newPoint.getTime())
                throw new RuntimeException("Tried to add a point that occured earlier in time");

        }
        mPoints.add(newPoint);
    }

    /**
     * Gets the point in the trajectory that's closest to this time
     * @param time The time for the point.
     * @return The point closest to the time.
     */
    public Point getPointAtTime(double time)
    {
        //Find the point who doesn't pass the time
        for(int i = 0; i < mPoints.size() - 1; i++)
        {
            if(mPoints.get(i+1).getTime() > time)
                return mPoints.get(i);
        }
        //The time must be the last point
        return mPoints.get(mPoints.size() - 1);
    }

    /**
     * Gets the next point in the trajectory after the given time
     * @param time The time before the point.
     * @return The point.
     */
    public Point getNextPoint(double time)
    {
        //Find the point who doesn't pass the time
        for(int i = 0; i < mPoints.size() - 1; i++)
        {
            if(mPoints.get(i+1).getTime() > time)
                return mPoints.get(i+1);
        }
        //The time must be the last point
        return null;
    }

    /**
     * Gets the last point in the trajectory, ie the best one
     * @return The best point.
     */
    public Point getLastPoint() {
        if (mPoints.isEmpty())
            return null;
        return mPoints.get(mPoints.size()-1);
    }

    public void truncateToTime(float maxTime) {
        int i = 0;
        while(i < mPoints.size()){
            if(mPoints.get(i).getTime() > maxTime){
                mPoints.remove(i);
            } else {
                i++;
            }
        }
    }

    /**
     * Set all the statistics regarding the number of evaluations performed for this trajectory
     * @param numTotalEvaluations The total number of evaluations.
     * @param numMemOutEvaluations The number of evaluations with memout errors.
     * @param numTimeOutEvaluations The number of evaluations with timeout
     * errors.
     */
    public void setEvaluationCounts(int numTotalEvaluations, int numMemOutEvaluations, int numTimeOutEvaluations) {
        mNumEvaluations = numTotalEvaluations;
        mNumMemOutEvaluations = numMemOutEvaluations;
        mNumTimeOutEvaluations = numTimeOutEvaluations;
    }

    /**
     * Gets the total number of evaluations that were performed
     * @return The total number of evaluations.
     */
    public int getNumEvaluations() {
        return mNumEvaluations;
    }

    /**
     * Gets the number of evaluations that hit the memory limit (-1 if not recorded)
     * @return The number of evaluations that exceeded the memory limit.
     */
    public int getNumMemOutEvaluations() {
        return mNumMemOutEvaluations;
    }

    /**
     * Gets the number of evaluations that timed out (-1 if not recorded)
     * @return The number of evaluations that timed out.
     */
    public int getNumTimedOutEvaluations() {
        return mNumTimeOutEvaluations;
    }
}
