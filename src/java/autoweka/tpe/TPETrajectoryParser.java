package autoweka.tpe;

import autoweka.Trajectory;
import autoweka.TrajectoryParser;
import autoweka.Experiment;
import java.util.Scanner;
import java.io.FileInputStream;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TPETrajectoryParser extends TrajectoryParser
{
    private Pattern mArgPattern = Pattern.compile("ArgString (.*)");
    private Pattern mTimePattern = Pattern.compile("Training time: ([\\.\\d]*).*");
    private Pattern mScorePattern = Pattern.compile("Computed score ([\\.\\deEnaNAinftyINFTY\\+\\-]*).*");

    private enum State
    {
        ARGSTRING,
        TIME,
        SCORE,
    }

    public Trajectory parseTrajectory(Experiment experiment, File folder, String seed)
    {
        Trajectory traj = new Trajectory(seed);
        int linen = 0;

        State state = State.ARGSTRING;
        try
        {
            Scanner scanner = new Scanner(new FileInputStream(folder.getAbsolutePath() + File.separator + "out" + File.separator + "logs" + File.separator + seed + ".log"));

            String line;
            Matcher matcher;
            double currentBest = Float.MAX_VALUE;
            String argString = null;
            double time = 0;
            double score = Float.MAX_VALUE;

            int numEvals = 0;
            int numTimeOut = 0;
            int numMemOut = 0;

            while(scanner.hasNextLine())
            {
                line = scanner.nextLine();
                numEvals++;
                //What state are we in
                switch(state)
                {
                case ARGSTRING:
                    matcher = mArgPattern.matcher(line);
                    if(matcher.matches())
                    {
                        argString = matcher.group(1);
                        state = State.TIME;
                    }
                    break;
                case TIME:
                    matcher = mTimePattern.matcher(line);
                    if(matcher.matches())
                    {
                        time = Float.parseFloat(matcher.group(1));
                        state = State.SCORE;
                        if(time > 1.1 * experiment.trainTimeout)
                            numTimeOut++;
                    }
                    break;
                case SCORE:
                    matcher = mScorePattern.matcher(line);
                    if(matcher.matches())
                    {
                        score = 100;
                        try
                        {
                            if(matcher.group(1).equals("inf")){
                                score = Float.POSITIVE_INFINITY;
                            } else {
                                score = Float.parseFloat(matcher.group(1));
                            }
                        }
                        catch(NumberFormatException e)
                        {
                            if(!matcher.group(1).equals("nan"))
                                throw e;
                        }
                        state = State.ARGSTRING;
                        //Should we insert this point?
                        if(score <= currentBest)
                        {
                            traj.addPoint(new Trajectory.Point(time, score, argString));
                            currentBest = score;
                        }
                    }
                    break;
                }

                if(line.contains("MEMOUT")){
                    numMemOut++;
                }
                linen++;
            }
            traj.setEvaluationCounts(numEvals, numMemOut, numTimeOut);
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to parse trajectory" + linen, e);
        }
        return traj;
    }
}
