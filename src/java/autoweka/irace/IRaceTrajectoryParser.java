package autoweka.irace;

import autoweka.ClassParams;
import autoweka.Parameter;
import autoweka.Trajectory;
import autoweka.TrajectoryParser;
import autoweka.Experiment;
import autoweka.Util;

import java.util.Scanner;
import java.io.FileInputStream;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class IRaceTrajectoryParser extends TrajectoryParser
{
    private static Pattern msParamReaderPattern = Pattern.compile("([\\S]+) \"-([\\S]+) \".*");
    private static Pattern msSelectedPattern = Pattern.compile("Selected candidate:[\\s]*([\\S]+)[\\s]*mean value:[\\s]*([\\S]+).*");

    public Trajectory parseTrajectory(Experiment experiment, File folder, String seed)
    {
        Map<String, String> paramMap = new HashMap<String, String>();

        try
        {
            //Load up the parameters file, to get a map between irace names and autoweka names
            Scanner paramFile = new Scanner(new FileInputStream(folder.getAbsolutePath() + File.separator + "parameters.txt"));
            String line;
            Matcher matcher;
            while(paramFile.hasNextLine()){
                line = paramFile.nextLine();
                //Rip out the parameter on this line
                matcher = msParamReaderPattern.matcher(line);
                if(matcher.matches()) {
                    paramMap.put(matcher.group(1), matcher.group(2));
                }else{
                    //This line didn't match?
                    System.out.println("Failed to match parameter line '" + line + "'");
                }
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }


        Trajectory traj = new Trajectory(seed);
        try
        {
            Scanner scanner = new Scanner(new FileInputStream(folder.getAbsolutePath() + File.separator + "out" + File.separator + "logs" + File.separator + seed + ".log"));
            
            String line;
            Matcher matcher;
            float time = 0;
            float score = 0;
            Map<String, String> argMap = new HashMap<String, String>();
            List<String> headers = new ArrayList<String>();
            while(scanner.hasNextLine())
            {
                line = scanner.nextLine();
                matcher = msSelectedPattern.matcher(line);
                if(matcher.matches())
                {
                    //Setup a bunch of stuff before we get into finding the parameter values
                    argMap.clear();
                    score = Float.parseFloat(matcher.group(2));
                    time += 1;
                    //Skip three lines
                    line = scanner.nextLine();
                    line = scanner.nextLine();
                    line = scanner.nextLine();

                    while(!line.isEmpty()){
                        if(line.charAt(0) == ' '){
                            //We're in a header row!
                            headers.clear();
                            //Add a nop to deal with the
                            for(String h : line.split("[\\s]+")){
                                headers.add(h);
                            }
                        }else{
                            //We're in a data row
                            String[] elements = line.split("[\\s]+");
                            for(int i = 1; i < elements.length; i++){
                                if(elements[i].equals("NA") || elements[i].equals("<NA>"))
                                    continue;

                                if(!paramMap.containsKey(headers.get(i)))
                                    throw new RuntimeException("looking '" + headers.get(i) + "'" + i + " " );
                                argMap.put(paramMap.get(headers.get(i)), elements[i]);
                            }
                        }
                        //And advance
                        line = scanner.nextLine();
                    }
                    //Time to convert this into something meaningful
                    traj.addPoint(new Trajectory.Point(time, score, Util.argMapToString(argMap)));
                }
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to parse trajectory", e);
        }
        return traj;
        //return autoweka.Util.argMapToString(params.filterParams(argMap));
    }
}
