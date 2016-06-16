package autoweka;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of a Wrapper that should ideally just be called from other Java objects - it performes the training/evaluation of a classifier on a sub process to preserve the JVM of the caller
 */
public class SubProcessWrapper extends Wrapper
{
    final static Logger log = LoggerFactory.getLogger(SubProcessWrapper.class);

    private static Pattern mResultPattern = Pattern.compile("SubProcessWrapper: Time\\(([\\.\\d]+)\\) Score\\(([\\.\\deE+-]+)\\)");

    /**
     * Calls the SubProcessWrapper using the given arguments 
     */
    public static void main(String [] args)
    {
        SubProcessWrapper wrapper = new SubProcessWrapper();
        try {
            wrapper.run(args);
        } catch(Exception e) {
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * The only two things that a SubProcessWrapper cares about are the timeouts and the seed
     */
    @Override
    protected void _processParameter(String arg, Queue<String> args)
    {
        if (arg.equals("-seed"))
        {
            // ignored
            args.poll();
        }
        else if (arg.equals("-timeout"))
        {
            mTimeout = Float.parseFloat(args.poll());
        }
    }

    /**
     * We only have to set the instance string
     */
    @Override
    protected void _processWrapperParameterStart(Queue<String> args)
    {
        //First argument is the instance file
        mInstance = args.poll();
    }

    /**
     * Emit the result from a sub process wrapper back up to whoever called us
     */
    @Override
    protected void _processResults(ClassifierResult res)
    {
        System.out.print("SubProcessWrapper: Time(" + res.getTime() + ") Score(" + res.getScore() + ")");
        String outputFilePrefix = mProperties.getProperty("modelOutputFilePrefix", null);
        if(outputFilePrefix != null){
            try{
                if(res.getAttributeSelection() != null){
                    weka.core.SerializationHelper.write(outputFilePrefix + ".attributeselection", res.getAttributeSelection());
                }else{
                    File oldFile = new File(outputFilePrefix + ".attributeselection");
                    if(oldFile.exists())
                        oldFile.delete();
                }
                weka.core.SerializationHelper.write(outputFilePrefix + ".model", res.getClassifier());
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Small class that just encapsulates the error and time of a sub process run
     * @TODO: Should really be changed to use a ClassifierResult instead
     */
    static public class ErrorAndTime
    {
        public ErrorAndTime(float _error, float _time)
        {
            error = _error;
            time = _time;
        }
        public float error;
        public float time;
    }
    
    /**
     * ErrorAndTime versions for ListExperiment
    */
    public static ErrorAndTime getErrorAndTime(File runDir, ListExperiment exp, String instance, String args, String autowekaSeed)
    {
        return getErrorAndTime(runDir, exp, instance, args, autowekaSeed, new Properties());
    }
    
    public static ErrorAndTime getErrorAndTime(File runDir, ListExperiment exp, String instance, String args, String autowekaSeed, Properties props)
    {
        props = new Properties(props);
        props.put("datasetString", exp.datasetString);
        props.put("instanceGenerator", exp.instanceGenerator);
        props.put("instanceGeneratorArgs", exp.instanceGeneratorArgs);
        props.put("resultMetric", exp.resultMetric);

        return getErrorAndTime(runDir, exp.memory, props, exp.trainTimeout, instance, args, autowekaSeed);
    }
    
    /**
     * ErrorAndTime versions for Experiment
    */
    public static ErrorAndTime getErrorAndTime(File runDir, Experiment exp, String instance, String args, String autowekaSeed)
    {
        return getErrorAndTime(runDir, exp, instance, args, autowekaSeed, new Properties());
    }
    
    public static ErrorAndTime getErrorAndTime(File runDir, Experiment exp, String instance, String args, String autowekaSeed, Properties props)
    {
        props = new Properties(props);
        props.put("datasetString", exp.datasetString);
        props.put("instanceGenerator", exp.instanceGenerator);
        props.put("instanceGeneratorArgs", exp.instanceGeneratorArgs);
        props.put("resultMetric", exp.resultMetric);

        return getErrorAndTime(runDir, exp.memory, props, exp.trainTimeout, instance, args, autowekaSeed);
    }

    /**
     * Calls the SubProcessWrapper as a SubProcess, and returns the result back up to the caller. 
     *
     * This method is super useful to ensure that leaking doesn't happen/memory limits are enforced, since all the work is done in a subprocess - if anything
     * bad happens, it dies down there, letting your process carry on willy nilly
     */
    public static ErrorAndTime getErrorAndTime(File runDir, String memory, Properties props, float trainTimeout, String instance, String args, String autowekaSeed)
    {
        try
        {
            List<String> wrapperCmd = new ArrayList<String>();
            wrapperCmd.add(autoweka.Util.getJavaExecutable());
            wrapperCmd.add("-Xmx" + memory);
            wrapperCmd.add("-cp");
            wrapperCmd.add(autoweka.Util.getAbsoluteClasspath());
            wrapperCmd.add("autoweka.SubProcessWrapper");
            wrapperCmd.add("-prop");
            wrapperCmd.add(Util.propertiesToString(props));
            wrapperCmd.add("-timeout");
            wrapperCmd.add(Float.toString(trainTimeout));
            wrapperCmd.add("-wrapper");
            wrapperCmd.add(instance);
            wrapperCmd.addAll(Arrays.asList(args.split(" ")));

            for(String c : wrapperCmd)
                log.debug("{}", c);

            ProcessBuilder pb = new ProcessBuilder(wrapperCmd);
            pb.environment().put("AUTOWEKA_EXPERIMENT_SEED", autowekaSeed);
            if(runDir != null)
                pb.directory(runDir);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            
            //Register a shutdown hook
            Thread killerHook = new Util.ProcessKillerShutdownHook(proc);
            Runtime.getRuntime().addShutdownHook(killerHook);

            String prevLine = null;
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            float error = 0;
            float time = 0;
            boolean foundMatch = false;

            while ((line = reader.readLine ()) != null) {
                // fix nested logging...
                if(line.matches(".*DEBUG.*")) {
                    log.debug(line);
                } else if(line.matches(".*WARN.*")) {
                    log.warn(line);
                } else if(line.matches(".*ERROR.*")) {
                    log.error(line);
                } else {
                    log.info(line);
                }
                Matcher matcher = mResultPattern.matcher(line);
                if(matcher.matches())
                {
                    time = Float.parseFloat(matcher.group(1));
                    error = Float.parseFloat(matcher.group(2));
                    foundMatch = true;
                }
            }
            proc.waitFor();
            if(!foundMatch)
                throw new RuntimeException("Failed to find output line from subprocess wrapper");

            Runtime.getRuntime().removeShutdownHook(killerHook);

            return new ErrorAndTime(error, time);
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to invoke child process", e);
        }
    }
}

