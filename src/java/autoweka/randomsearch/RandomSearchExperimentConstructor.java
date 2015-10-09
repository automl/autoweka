package autoweka.randomsearch;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import autoweka.Conditional;
import autoweka.ExperimentConstructor;
import autoweka.Parameter;
import autoweka.ParameterConditionalGroup;
import autoweka.Util;

public class RandomSearchExperimentConstructor extends ExperimentConstructor
{
    public void prepareExperiment(String path)
    {
        try
        {
            //Print out the param file
            printParamFile(new PrintStream(new java.io.File(path + "autoweka.params")));

            autoweka.Util.makePath(path + "out");
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to prepare the experiment", e);
        }
    }

    public String getTrajectoryParserClassName()
    {
        return "autoweka.randomsearch.RandomSearchTrajectoryParser";
    }

    @Override
    public List<String> getCallString(String path)
    {
        List<String> args = new ArrayList<String>();
        args.add(Util.getJavaExecutable());
        args.add("-Xmx256m");
        args.add("-cp");
        args.add(Util.getAbsoluteClasspath());
        args.add("autoweka.randomsearch.RandomSearchWorker");
        args.add(mExperiment.name + ".experiment");
        args.add("{SEED}");

        return args;
    }

    public String getType()
    {
        return "RandomSearch";
    }

    private void printParamFile(PrintStream out)
    {
        ParameterConditionalGroup paramGroup = generateAlgorithmParameterConditionalGroupForDAG();
        List<String> parameters = new ArrayList<String>();
        List<String> conditionals = new ArrayList<String>();

        for(Parameter param: paramGroup.getParameters()){
            parameters.add(param.toString());
            for(Conditional cond: paramGroup.getConditionalsForParameter(param)){
                conditionals.add(cond.toString());
            }
        }

        //Sort them for sanity
        Collections.sort(parameters);
        Collections.sort(conditionals);

        //Dump 'em
        for(String param: parameters)
        {
            out.println(param);
        }
        out.println("Conditionals:");
        for(String cond: conditionals)
        {
            out.println(cond);
        }
    }
}
