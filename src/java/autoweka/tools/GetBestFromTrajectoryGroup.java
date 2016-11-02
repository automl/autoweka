package autoweka.tools;

import java.util.Arrays;

import autoweka.Experiment;
import autoweka.Trajectory;
import autoweka.TrajectoryGroup;
import autoweka.Util;
import autoweka.WekaArgumentConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetBestFromTrajectoryGroup
{
    final static Logger log = LoggerFactory.getLogger(GetBestFromTrajectoryGroup.class);

    /**
     * Point this main method at a .trajectory file, and be presented with what you should actually run your dataset on.
     *
     * @param args Any arguments.
     */
    public static void main(String[] args){
        GetBestFromTrajectoryGroup res = new GetBestFromTrajectoryGroup(args[0]);
        log.info("Experiment: {}", res.experiment.name);
        log.info("Num Trajectories: {}", res.numTrajectories);
        log.info("Best point seed: {}", res.seed);
        log.info("Best point error estimate: {}", res.errorEstimate);

        log.info("Classifier: {}", res.classifierClass);
        log.info(" Args: {}", res.classifierArgs);

        //Is there a AS search method?
        if(res.attributeSearchClass != null && res.attributeEvalClass != null){
            log.info("ASSearch: {}", res.attributeSearchClass);
            log.info(" Args: {}", res.attributeSearchArgs);
            
            log.info("ASEval: {}", res.attributeEvalClass);
            log.info(" Args: {}", res.attributeEvalArgs);
        }
    }

    public GetBestFromTrajectoryGroup(String trajGroupFileName)
    {
        //Go and open the trajectory, then pass that to our other constructor
        this(TrajectoryGroup.fromXML(trajGroupFileName));
    }

    public GetBestFromTrajectoryGroup(TrajectoryGroup trajGroup)
    {
        experiment = trajGroup.getExperiment();
        numTrajectories = trajGroup.getTrajectories().size();

        //Go find the best point
        Trajectory.Point bestPt = new Trajectory.Point(0, 1e30f, "");
        Trajectory bestTraj = new Trajectory("");

        for(Trajectory traj : trajGroup.getTrajectories()){
            Trajectory.Point pt = traj.getLastPoint();
            if(pt == null)
                continue;

            if(bestPt.mErrorEstimate > pt.mErrorEstimate){
                bestPt = pt;
                bestTraj = traj;
                seed = traj.getSeed();
                errorEstimate = (float)bestPt.mErrorEstimate; 
            }
        }
        rawArgs = bestPt.mArgs;

        WekaArgumentConverter.Arguments wekaArgs = WekaArgumentConverter.convert(Arrays.asList(bestPt.mArgs.split(" ")));

        classifierClass = wekaArgs.propertyMap.get("targetclass");
        classifierArgs  = Util.joinStrings(" ", Util.quoteStrings(wekaArgs.argMap.get("classifier")));

        //Is there a AS search method?
        if(wekaArgs.propertyMap.containsKey("attributesearch") && !"NONE".equals(wekaArgs.propertyMap.get("attributesearch"))){
            attributeSearchClass = wekaArgs.propertyMap.get("attributesearch");
            attributeSearchArgs  = Util.joinStrings(" ", Util.quoteStrings(wekaArgs.argMap.get("attributesearch")));
            
            attributeEvalClass = wekaArgs.propertyMap.get("attributeeval");
            attributeEvalArgs  = Util.joinStrings(" ", Util.quoteStrings(wekaArgs.argMap.get("attributeeval")));
        }

        numEval = bestTraj.getNumEvaluations();
        numTimeOut = bestTraj.getNumTimedOutEvaluations();
        numMemOut = bestTraj.getNumMemOutEvaluations();
    }

    public Experiment experiment = null;
    public int numTrajectories = 0;
    public float errorEstimate = -1; 
    public String seed = null;

    public String classifierClass = null;
    public String classifierArgs = null;

    public String attributeSearchClass = null;
    public String attributeSearchArgs = null;
    
    public String attributeEvalClass = null;
    public String attributeEvalArgs = null;

    public int numEval = -1;
    public int numTimeOut = -1;
    public int numMemOut = -1;

    public String rawArgs;
}
