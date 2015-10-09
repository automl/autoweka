package autoweka;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

/**
 * Helper class that runs all the trajectoryPointExtras on a given trajectory - needed for analysis experiments
 */
public class TrajectoryPointExtraRunner
{
    public static void main(String[] args)
    {
        String targetSeed = null;
        boolean onlyBestPoint = false;
        boolean splitTrajFile = false;
        String trajGroupName = null;
        boolean reverse = false;
        String instanceOverride = null;
        boolean saveModel = false;
        boolean doPredictions = false;
        for(int i = 0; i < args.length; i++)
        {
            if(args[i].equals("-seed"))
            {
                targetSeed = args[++i];
            }
            else if (args[i].equals("-onlyBest"))
            {
                onlyBestPoint = true;
            }
            else if (args[i].equals("-reverse"))
            {
                reverse = true;
            }
            else if (args[i].equals("-splitTrajFile"))
            {
                splitTrajFile = true;
            }
            else if (args[i].equals("-instanceOverride"))
            {
                instanceOverride = args[++i];
            }
            else if (args[i].equals("-saveModel"))
            {
                saveModel = true;
            }
            else if (args[i].equals("-doPredictions"))
            {
                doPredictions = true;
            }
            else if(args[i].startsWith("-"))
            {
                throw new RuntimeException("Unknown arg: " + args[i]);
            }
            else
            {
                if(trajGroupName != null)
                    throw new RuntimeException("Only one trajectory group can be specified at a time");
                trajGroupName = args[i];
            }
        }

        TrajectoryGroup trajGroup = TrajectoryGroup.fromXML(trajGroupName);
        Experiment experiment = trajGroup.getExperiment();
        double timeout = experiment.tunerTimeout;
        File experimentDir = new File(trajGroupName).getParentFile();
        Map<String, SubProcessWrapper.ErrorAndTime> mResultCache = new HashMap<String, SubProcessWrapper.ErrorAndTime>();

        //Do we need to override the zip file?
        if(instanceOverride != null)
        {
            experiment.datasetString = instanceOverride;
            trajGroupName = trajGroupName + "." + new File(instanceOverride).getName() + ".traj";
        }

        //Have we been given a specific seed?
        if(targetSeed != null)
        {
            Trajectory singleTraj = trajGroup.getTrajectory(targetSeed);
            trajGroup = new TrajectoryGroup(experiment);
            trajGroup.addTrajectory(singleTraj);
            if(splitTrajFile)
                trajGroupName = trajGroupName + "." + targetSeed;
        }

        Properties runnerProps = new Properties();
        if(saveModel){
            if(!onlyBestPoint)
                throw new RuntimeException("Can't save the model unless looking at only the best");
            runnerProps.put("modelOutputFilePrefix", trajGroupName); 
        }

        //For every trajectory, we need to compute the best score
        for(Trajectory traj: trajGroup.getTrajectories())
        {
            //Get the point where the tuner time hits us
            List<Trajectory.Point> points;
            if(onlyBestPoint)
            {
                points = new ArrayList<Trajectory.Point>();
                points.add(traj.getPointAtTime(timeout));
            }
            else if(reverse)
            {
                points = new ArrayList<Trajectory.Point>(traj.getPoints());
                Collections.reverse(points);
            }
            else
            {
                points = traj.getPoints();
            }

            if(doPredictions)
            {
                if(!onlyBestPoint)
                    throw new RuntimeException("Can't do predictions unless looking at only the best");
                runnerProps.put("predictionsFileName", trajGroupName + ".predictions." + traj.getSeed() + ".csv");
            }

            for(Trajectory.Point point: points)
            {
                point.specificInstanceInfo.clear();
                for(Experiment.TrajectoryPointExtra extra: experiment.trajectoryPointExtras)
                {
                    String cacheLookup = extra.instance + "__" + point.getArgs();
                    SubProcessWrapper.ErrorAndTime errTime = new SubProcessWrapper.ErrorAndTime(0, 0);
                    if(mResultCache.get(cacheLookup) != null)
                    {
                        errTime = mResultCache.get(cacheLookup);
                    }
                    else
                    {
                        //We need to run this guy, but with the default instance generator
                        errTime = SubProcessWrapper.getErrorAndTime(experimentDir, experiment, extra.instance, point.getArgs(), traj.getSeed(), runnerProps);
                    }
                    mResultCache.put(cacheLookup, errTime);

                    point.specificInstanceInfo.add(new Trajectory.Point.SpecificInstanceInfo(extra.name, extra.instance, errTime.error, errTime.time));
                }
                trajGroup.toXML(trajGroupName);
            }
        }
    }
}
