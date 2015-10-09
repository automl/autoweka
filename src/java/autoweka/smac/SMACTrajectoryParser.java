package autoweka.smac;

import autoweka.ClassParams;
import autoweka.Parameter;
import autoweka.Trajectory;
import autoweka.TrajectoryParser;
import autoweka.Experiment;
import java.util.Scanner;
import java.io.FileInputStream;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.HashMap;

public class SMACTrajectoryParser extends TrajectoryParser
{
    private Pattern mTrajPattern = Pattern.compile("([\\-\\.\\d]+), ([\\-\\.\\dEef]+), [\\-\\.\\d]+, [\\-\\.\\d]+, [\\-\\.\\d]+, (.*)");
    //private Pattern mTrajPattern = Pattern.compile("([\\-\\.\\d]+),\\s*([\\-\\.\\d]+),\\s*[\\-\\.\\d]+,\\s*[\\-\\.\\d]+,\\s*[\\-\\.\\d]+,\\s*(.*)");
    private Pattern mRunsAndResultFileNamePattern = Pattern.compile("runs_and_results-it(\\d+).csv");

    public Trajectory parseTrajectory(Experiment experiment, File folder, String seed)
    {
        //Load up the conditional params
        ClassParams params = new ClassParams(folder.getAbsolutePath() + File.separator + "autoweka.params");

        Trajectory traj = new Trajectory(seed);

        try
        {
            //We need to go get this trajectory file
            String trajFileName = "";
            File[] files = new File(folder.getAbsolutePath() + File.separator + "out" + File.separator + "autoweka").listFiles();
            for(File f: files)
            {
                String s = f.getName();
                if(s.startsWith("traj") && s.endsWith("-" + seed + ".txt"))
                {
                    trajFileName = f.getAbsolutePath();
                    break;
                }
            }
            System.out.println(trajFileName);
            Scanner scanner = new Scanner(new FileInputStream(trajFileName));

            String line;
            Matcher matcher;
            //SMAC starts with a crazy large number - let's ignore that
            double currentBest = 1e100;
            String argString = null;
            double time = 0;
            double score = Float.MAX_VALUE;
            while(scanner.hasNextLine())
            {
                line = scanner.nextLine();
                matcher = mTrajPattern.matcher(line);
                if(matcher.matches())
                {
                    time = Float.parseFloat(matcher.group(1));
                    score = Float.parseFloat(matcher.group(2));

                    //System.out.println(time + " " +score);
                    if(score != currentBest)
                    {
                        currentBest = score;
                        argString = filterArgString(params, matcher.group(3));
                        traj.addPoint(new Trajectory.Point(time, score, argString));
                    }
                }
                else
                {
                    //This line didn't match...
                    //System.out.println(line);
                }
            }

            //Now, we need to parse the runs_and_results file to get some other statistics
            String runsAndResultsFileName = null;
            int runsAndResultsIteration = -1;
            files = new File(folder.getAbsolutePath() + File.separator + "out" + File.separator + "autoweka" + File.separator + "state-run" + seed + File.separator).listFiles();
            for(File f: files)
            {
                String s = f.getName();
                matcher = mRunsAndResultFileNamePattern.matcher(s);
                if(matcher.matches())
                {
                    int itr = Integer.parseInt(matcher.group(1));
                    if(itr > runsAndResultsIteration){
                        runsAndResultsFileName = f.getAbsolutePath();
                        runsAndResultsIteration = itr;
                    }
                }
            }

            //We actually found something
            if(runsAndResultsFileName != null)
            {
                int numEvals = 0;
                int numMemOut = 0;
                int numTimeOut = 0;
                System.out.println(runsAndResultsFileName);
                scanner = new Scanner(new FileInputStream(runsAndResultsFileName));

                //Skip the header
                scanner.nextLine();
                String[] row;
                while(scanner.hasNextLine())
                {
                    //Split the line
                    row = scanner.nextLine().split(",");
                    try {
                        //We've got an eval, ++ that number
                        numEvals++;

                        //Figure out if this one timed out
                        if(Float.parseFloat(row[7]) >= 1.1 * experiment.trainTimeout) {
                            numTimeOut++;
                        }

                        //Did we get a timeout
                        if(row[14].contains("MEMOUT")) {
                            numMemOut++;
                        }
                    } catch (Exception e) {
                        //Whatevs... it's wrong
                        e.printStackTrace();
                    }
                }
                traj.setEvaluationCounts(numEvals, numMemOut, numTimeOut); 
            }
            else
            {
                System.out.println("Could not find runs_and_results file");
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to parse trajectory", e);
        }
        return traj;
    }

    private String filterArgString(ClassParams params, String args)
    {
        //First, we need to make a map out of everything
        Map<String, Parameter> paramMap = params.getParameterMap();
        Map<String, String> argMap = new HashMap<String, String>();
        String[] splitArgs = args.split(", ");
        for(String argPair: splitArgs)
        {
            //System.out.println(argPair);
            String[] splitArg = argPair.split("=", 2);
            String arg = splitArg[0].trim();
            String value = splitArg[1].trim();
            if(paramMap.get(arg) == null)
                throw new RuntimeException("Unknown argument found in trajectory '" + arg + "'");
            if(value.startsWith("'") && value.endsWith("'"))
            {
                value = value.substring(1, value.length()-1);
            }
            argMap.put(arg, value);
        }

        return autoweka.Util.argMapToString(params.filterParams(argMap));
    }
}

