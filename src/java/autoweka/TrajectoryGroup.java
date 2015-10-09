package autoweka;

import javax.xml.bind.annotation.*;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;

/** 
 * Wrapper class that provides a nice way to get at a bunch of trajectories from an experiment run with different seeds 
 */
@XmlRootElement(name="trajectorygroup")
@XmlAccessorType(XmlAccessType.NONE)
public class TrajectoryGroup extends XmlSerializable
{
    @XmlElement(name="experiment")
    private Experiment mExperiment;
    @XmlElement(name="trajectories")
    private ArrayList<Trajectory> mTrajectories = new ArrayList<Trajectory>();

    public TrajectoryGroup()
    {
    }

    public TrajectoryGroup(Experiment exp)
    {
        mExperiment = exp;
    }

    public Experiment getExperiment()
    {
        return mExperiment;
    }

    public void addTrajectory(Trajectory traj)
    {
        mTrajectories.add(traj);
    }

    public Trajectory getTrajectory(String seed)
    {
        for(Trajectory traj : mTrajectories)
        {
            if(traj.getSeed().equals(seed))
                return traj;
        }
        throw new RuntimeException("Seed not found");
    }

    public List<Trajectory> getTrajectories()
    {
        return new ArrayList<Trajectory>(mTrajectories);
    }

    public List<String> getSeeds()
    {
        ArrayList<String> seeds = new ArrayList<String>(mTrajectories.size());
        for(Trajectory traj : mTrajectories){
            seeds.add(traj.getSeed());
        }
        return seeds;
    }

    public static TrajectoryGroup fromXML(String filename)
    {
        return XmlSerializable.fromXML(filename, TrajectoryGroup.class);
    }

    public static TrajectoryGroup fromXML(InputStream xml)
    {
        return XmlSerializable.fromXML(xml, TrajectoryGroup.class);
    }
};
