package autoweka;

import java.io.File;
import java.util.Properties;

public class TrajectoryPointPredictionRunner
{
    public static void main(String[] args)
    {
        String targetSeed = null;
        String trajGroupName = null;
        String instance = "default";
        String dataset = null;
        boolean saveModel = false;
        boolean skipEvaluation = false;
        for(int i = 0; i < args.length; i++)
        {
            if(args[i].equals("-seed"))
            {
                targetSeed = args[++i];
            }
            else if (args[i].equals("-instance"))
            {
                instance = args[++i];
            }
            else if (args[i].equals("-dataset"))
            {
                dataset = args[++i];
            }
            else if (args[i].equals("-savemodel"))
            {
               saveModel = true; 
            }
            else if (args[i].equals("-skipevaluation"))
            {
               skipEvaluation = true; 
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

        //Do we need to override the dataset?
        if(dataset != null)
        {
            experiment.datasetString = dataset;
        }

        //Have we been given a specific seed?
        if(targetSeed != null)
        {
            Trajectory singleTraj = trajGroup.getTrajectory(targetSeed);
            trajGroup = new TrajectoryGroup(experiment);
            trajGroup.addTrajectory(singleTraj);
        }

        //For every trajectory, we need to compute the best score
        for(Trajectory traj: trajGroup.getTrajectories())
        {
            //Get the point where the tuner time hits us
            Trajectory.Point point = traj.getPointAtTime(timeout);
            Properties props = new Properties();
            props.put("predictionsFileName", experimentDir.getAbsolutePath() + "/predictions." + traj.getSeed() + ".csv");
            if(saveModel)
                props.put("modelOutputFilePrefix", experimentDir.getAbsolutePath() + "/trained." + traj.getSeed());
            SubProcessWrapper.getErrorAndTime(experimentDir, experiment, instance, point.getArgs(), traj.getSeed(), props);
        }
    }
}
