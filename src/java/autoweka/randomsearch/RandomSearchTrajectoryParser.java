package autoweka.randomsearch;

import java.io.File;

import autoweka.Experiment;
import autoweka.Trajectory;
import autoweka.TrajectoryParser;

/** Nop trajectory parser */
public class RandomSearchTrajectoryParser extends TrajectoryParser
{
    public Trajectory parseTrajectory(Experiment experiment, File folder, String seed)
    {
        return new Trajectory(seed);
    }
}
