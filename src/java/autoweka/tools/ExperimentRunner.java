package autoweka.tools;

import java.io.File;

import autoweka.Experiment;
import autoweka.TrajectoryParser;
import autoweka.TrajectoryPointPredictionRunner;

/**
 * Utility class that just combines running an experiment, extracting a trajectory, then saving the model of the incumbent
 */
class ExperimentRunner
{
    public static void main(String[] args)
    {
        if(args.length != 2){
            System.out.println("ExperimentRunner requires 2 arguments - the experiment folder and the seed");
            System.exit(1);
        }
        File expFolderFile = new File(args[0]);
        String seed = args[1];

        if(!expFolderFile.exists() || !expFolderFile.isDirectory()){
            System.out.println("The first argument does not appear to be an experiment folder");
            System.exit(1);
        }
        String expFolder = expFolderFile.getAbsolutePath();
        String expName = expFolderFile.getName();

        System.out.println(expFolder + " " + expName);

        //Remove the old trajectory and model
        String[] oldFiles = new String[]{
            expName + ".trajectories." + seed ,
            "trained." + seed + ".model",
            "trained." + seed + ".attributeselection",
            "predictions." + seed
        };
        for(String path : oldFiles) {
            File tmpFile = new File(expFolder + File.separator + path);
            if(tmpFile.exists())
                tmpFile.delete();
        }

        //Run the experiment
        String[] expArgs = new String[]{"-noexit", expFolder, seed};
        Experiment.main(expArgs);

        //Extract the trajectory
        String[] trajParseArgs = new String[]{"-single", expFolder, seed};
        TrajectoryParser.main(trajParseArgs);

        //And get some predictions/train the model
        String[] runnerArgs = new String[]{expFolder + File.separator + expName + ".trajectories." + seed, "-savemodel"};
        TrajectoryPointPredictionRunner.main(runnerArgs);
    }
};
