package autoweka;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Queue;
import java.io.FileInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import autoweka.smac.SMACWrapper;

/**
 *  Generic class that gets called from an SMBO method, completes the evaluation, and returns the result back up to the SMBO method.
 *
 *  The command line arguments for these wrappers consist of two portions, a number of options that specify seeds/properties that relate to the wrapper itself,
 *  followed by the -wrapper argument. Depending on the SMBO method, this is where you specify the datasetString that you want to pass off to the InstanceGenerator,
 *  along with any other options that are required. The remainder of the arguments after the sub classes have processed the -wrapper are going to be passed off to the classifier
 *  runner for evaluation.
 *
 *  There are a number of 'events' that occur during the run of a wrapper - if you need to do something special here you should just be able to override these and inject the correct
 *  behaviour as needed
 */
public class Wrapper
{
    protected String mExperimentSeed = null;
    protected String mInstance = null; //instance string
    protected float mTimeout = 0;
    protected ClassifierRunner mRunner;
    protected Properties mProperties;
    protected String mResultMetric = null;

    final Logger log = LoggerFactory.getLogger(Wrapper.class);

    /**
     * Runs the wrapper with the given command line arguments - see the class description for full details
     */
    public void run(String[] argsArray)
    {
        LinkedList<String> args = new LinkedList<String>(Arrays.asList(argsArray));
        ArrayList<String> wrapperConstructorArgs = new ArrayList<String>();
        ArrayList<String> wrapperArgs = new ArrayList<String>();
        String configFileName = null;

        //Get the experiment seed
        mExperimentSeed = System.getenv().get("AUTOWEKA_EXPERIMENT_SEED");

        //First, we need to scan through the args list
        boolean inWrapper = false;
        while(!args.isEmpty())
        {
            String arg = args.poll();
            if(!inWrapper && arg.equals("-experimentseed"))
            {
                //See if it's telling us to get it from an env variable
                mExperimentSeed = args.poll();
            }
            else if(!inWrapper && arg.equals("-wrapper"))
            {
                inWrapper = true;
                //First, we need to extract a bunch of things
                _processWrapperParameterStart(args);
            }
            else if(inWrapper)
            {
                //Strip out the single quotes if they are there
                if(arg.startsWith("'") && arg.endsWith("'")){
                    wrapperArgs.add(arg.substring(1, arg.length()-1));
                }
                else{
                    wrapperArgs.add(arg);
                }
            }
            else
            {
                if(arg.equals("-propsfile"))
                {
                    configFileName = args.poll();
                    continue;
                }
                else if(arg.equals("-prop"))
                {
                    wrapperConstructorArgs.add(arg);
                    wrapperConstructorArgs.add(args.poll());
                    continue;
                }
                //Otherwise, ask the subclass if they want this
                _processParameter(arg, args);
            }
        }

        if(mExperimentSeed == null){
            log.warn("No experiment seed defined, using default of 0");
            mExperimentSeed = "0";
        }

        //Make sure we have stuff
        if(mInstance == null)
        {
            throw new RuntimeException("Subclass did not set the instance string");
        }

        //Replace all the {SEED}s we can find in the instance string
        //TODO: Should we repalce all args?
        mInstance = mInstance.replace("{SEED}", mExperimentSeed);

        mProperties = new Properties();
        if(configFileName != null)
        {
            try
            {
                mProperties.load(new FileInputStream(configFileName));
            }
            catch(Exception e)
            {
                throw new RuntimeException("Failed to load config file: " + e.getMessage(), e);
            }
        }

        //Get the properties that were specified on the command line
        Util.parseCommandLineProperties(mProperties, wrapperConstructorArgs);

        //What kind of evaluation type are we using?
        mResultMetric = mProperties.getProperty("resultMetric", null);
        if(mResultMetric == null){
            log.warn("No evaluation method specified, defaulting to error rate");
            mResultMetric = "errorRate";
        }

        //Let the wrapper do anything ahead of time that would be good
        _preRun();

        //Build the classifier runner
        mRunner = new ClassifierRunner(mProperties);

        ClassifierResult res = _doRun(wrapperArgs);

        if(res == null) {
            throw new RuntimeException("Failed compute result!");
        }

        //Post event
        _postRun();

        //Process the result
      //  System.out.println("Wrapper: About to check if I'm a SMACWrapper");
         if(this instanceof SMACWrapper){
             //@TODO: Rather than checking if its smac, have a proper flag the user can set.
             //If we wanna spit out the N best configs, we need to know what those configs look like.
           //  System.out.println("Wrapper: Yes I am a SMACWrapper");
             _processResults(res,wrapperArgs,mInstance);
         }else{
        //  System.out.println("Wrapper: No I'm not a SMACWrapper");
            _processResults(res);
        }

    }

    /**
     * Actually does the run of the Classifier Runner.
     *
     * You should only override this if you need to do a number of different runs for each wrapper invocation
     */
    protected ClassifierResult _doRun(List<String> runnerArgs)
    {
        //Run it

        ClassifierResult res = new ClassifierResult(mResultMetric);
        res.setCompleted(false);
        com.sun.management.OperatingSystemMXBean OSBean = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        long startTime = OSBean.getProcessCpuTime();
        for(String s: runnerArgs){
            log.trace("Adding arg {}", s);
        }

        try {
            res = mRunner.run(mInstance, mResultMetric, mTimeout, mExperimentSeed, runnerArgs);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            long stopTime = OSBean.getProcessCpuTime();
            res.setTrainingTime(1.0f + ((stopTime - startTime) * 1e-9f));
        }

        return res;
    }

    /**
     * Override this when you want to get at parameters as soon as you hit the -wrapper.
     *
     * Consume any extra arguments from the Queue that should not be passed on to the ClassifierRunner
     */
    protected void _processWrapperParameterStart(Queue<String> args)
    {
        //Just move this one along - we don't consume anything
    }

    /**
     * Override this when you want to get at parameters before the -wrapper.
     *
     * Consume any extra arguments from the Queue that you don't want to process again
     */
    protected void _processParameter(String arg, Queue<String> args)
    {
        //Just move this one along
    }

    /**
     * Called just before _doRun();
     */
    protected void _preRun()
    {
    }

    /**
     * Called just after _doRun();
     */
    protected void _postRun()
    {
    }

    /**
     * Called once the run has completed (or been terminated), the results should be sent back to the SMBO method here
     */
    protected void _processResults(ClassifierResult res)
    {
    }

    /**
     * Overrloading in case we wanna save N best results
     */
    protected void _processResults(ClassifierResult res,  ArrayList<String> wrapperArgs, String instanceString)
    {
    }
}
