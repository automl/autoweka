package autoweka.tools;

import autoweka.Trajectory;
import autoweka.TrajectoryGroup;

class TrajectoryTruncator
{
    public static void main(String[] args)
    {
        //The first argument is the number of seconds to clip at
        if(args.length < 2){
            System.out.println("Usage: [time to truncate trajectory at] [trajectory group file] ...");
            System.exit(1);
        }

        float maxTime = 0;
        try
        {
            maxTime = Float.parseFloat(args[0]);
        }catch(NumberFormatException e){
            System.out.println("The first argument does not appear to be a number");
            System.exit(1);
        }
        if(maxTime <= 0){
            System.out.println("The truncation time must be greater than 0");
            System.exit(1);
        }

        for(int i = 1; i < args.length; i++){
            clipTrajectoryGroupFile(args[i], maxTime);
        }
    }

    public static void clipTrajectoryGroupFile(String filename, float maxTime)
    {
        System.out.println(filename);
        TrajectoryGroup trajGroup = TrajectoryGroup.fromXML(filename);
        for(Trajectory traj : trajGroup.getTrajectories()){
            traj.truncateToTime(maxTime);
        }
        trajGroup.toXML(filename);
    }
}
