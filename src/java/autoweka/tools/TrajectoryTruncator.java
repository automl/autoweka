package autoweka.tools;

import autoweka.Trajectory;
import autoweka.TrajectoryGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TrajectoryTruncator
{
    final static Logger log = LoggerFactory.getLogger(TrajectoryTruncator.class);

    public static void main(String[] args)
    {
        //The first argument is the number of seconds to clip at
        if(args.length < 2){
            log.error("Usage: [time to truncate trajectory at] [trajectory group file] ...");
            System.exit(1);
        }

        float maxTime = 0;
        try
        {
            maxTime = Float.parseFloat(args[0]);
        }catch(NumberFormatException e){
            log.error("The first argument does not appear to be a number");
            System.exit(1);
        }
        if(maxTime <= 0){
            log.error("The truncation time must be greater than 0");
            System.exit(1);
        }

        for(int i = 1; i < args.length; i++){
            clipTrajectoryGroupFile(args[i], maxTime);
        }
    }

    public static void clipTrajectoryGroupFile(String filename, float maxTime)
    {
        log.info(filename);
        TrajectoryGroup trajGroup = TrajectoryGroup.fromXML(filename);
        for(Trajectory traj : trajGroup.getTrajectories()){
            traj.truncateToTime(maxTime);
        }
        trajGroup.toXML(filename);
    }
}
