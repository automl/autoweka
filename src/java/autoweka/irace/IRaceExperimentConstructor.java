package autoweka.irace;

import java.util.ArrayList;
import java.io.File;
import java.io.PrintStream;
import autoweka.Conditional;
import autoweka.ExperimentConstructor;
import autoweka.Parameter;
import autoweka.Util;

import autoweka.ParameterConditionalGroup;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class IRaceExperimentConstructor extends ExperimentConstructor
{
    public void prepareExperiment(String path)
    {
        try
        {
            //Print out the param file
            printParamFile(new PrintStream(new File(path + "parameters.txt")));

            //Write out the instance file
            printInstanceFile(new PrintStream(new File(path + "instances-list")));

            //Write out the scenario file
            printConfigFile(new PrintStream(new File(path + "tune-conf")));

            //Write out the hook
            File hookFile = new File(path + "hook-run");
            printRunHook(new PrintStream(hookFile));
            hookFile.setExecutable(true);


            autoweka.Util.makePath(path + "out");
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to prepare the experiment", e);
        }
    }

    public String getTrajectoryParserClassName()
    {
        return "autoweka.irace.IRaceTrajectoryParser";
    }

    @Override
    public List<String> getCallString(String path)
    {
        //Make sure that the properties we have tell us where the executable for smac lives
        if(mProperties.getProperty("iraceexecutable") == null)
            throw new RuntimeException("The 'iracexecutable' property was not defined");

        List<String> args = new ArrayList<String>();
        args.add(mProperties.getProperty("iraceexecutable"));
        args.add("--seed");
        args.add("{SEED}");
        args.add("-l");
        args.add("out/run{SEED}.Rdata");

        return args;
    }

    public String getType()
    {
        return "IRace";
    }

    public void printInstanceFile(PrintStream out)
    {
        List<String> instanceStrings = mInstanceGenerator.getAllInstanceStrings(mExperiment.instanceGeneratorArgs);
        for(String s:instanceStrings)
        {
            out.println(s);
        }
    }

    public void printConfigFile(PrintStream out)
    {
        out.println("instanceFile <- \"instances-list\"");
        out.println("maxExperiments <- " + String.format("%d", (int)mExperiment.tunerTimeout));
        
        Properties props = autoweka.Util.parsePropertyString(mExperiment.extraPropsString);
        if(props.containsKey("firstTest")){
            out.println("firstTest <- " + props.getProperty("firstTest"));
        }
        if(props.containsKey("minNbSurvival")){
            out.println("minNbSurvival <- " + props.getProperty("minNbSurvival"));
        }
        if(props.containsKey("parallel")){
            out.println("parallel <- " + props.getProperty("parallel"));
        }
        if(props.containsKey("mpi")){
            out.println("mpi <- " + props.getProperty("mpi"));
        }
    }

    public void printRunHook(PrintStream out)
    {
        out.println("#!/bin/bash");
        out.println("result=`" + autoweka.Util.getJavaExecutable() + " -Xmx" + mExperiment.memory + " -cp " + autoweka.Util.getAbsoluteClasspath() + " autoweka.irace.IRaceWrapper -prop '" + getWrapperPropString().replace("\\", "\\\\") + "' -timeout " + mExperiment.trainTimeout + " -wrapper $@ | tail -n 1`");

        out.println();
        out.println("if ! [[ \"$result\" =~ ^[0-9]+([.][0-9]+)?$ ]] ; then");
        out.println("    echo \"100\"");
        out.println("else");
        out.println("    echo $result");
        out.println("fi");
        out.println("exit 0");
    }

    public void printParamFile(PrintStream out)
    {
        ParameterConditionalGroup paramGroup = generateAlgorithmParameterConditionalGroupForDAG();

        for(Parameter param: paramGroup.getParameters()){
            //Get the parameter after the necessary transforms
            Parameter transformedParam = transformParameter(param);

            //The internal name for irace
            out.print(nameTranslator(transformedParam.name));
            out.print(" ");

            //The string that is passed to the wrapper
            out.print("\"-");
            out.print(transformedParam.name);
            out.print(" \"");
            out.print(" ");
            
            //The type/domain of the categorical
            switch(transformedParam.type){
            case CATEGORICAL:
                out.print("c (");
                out.print(categoricalDomainInnards(transformedParam.categoricalInnards));
                out.print(")");
                break;
            case NUMERIC:
                out.print("r (" + transformedParam.minNumeric + ", " + transformedParam.maxNumeric + ")");
                break;
            case INTEGER:
                out.print("i (" + ((int)Math.round(transformedParam.minNumeric)) + ", " + ((int)Math.round(transformedParam.maxNumeric)) + ")");
                break;
            default:
                throw new RuntimeException("iRace does not support parameters of type '" + transformedParam.type.toString());
            }
            
            //Figure out the conditions to append to it
            List<String> conditionalStrings = new ArrayList<String>();
            for(Conditional cond: paramGroup.getConditionalsForParameter(param)){
                Parameter transformedParent = transformParameter(cond.parent);
                StringBuilder sb = new StringBuilder();
                sb.append(nameTranslator(transformedParent.name));
                sb.append(" %in% c(");
                //TODO: Support transformed domains???
                sb.append(categoricalDomainInnards(cond.domain));
                sb.append(")");
                conditionalStrings.add(sb.toString());
            }
            if(!conditionalStrings.isEmpty()){
                out.print(" | ");
                out.print(Util.joinStrings(" && ", conditionalStrings));
            }
            out.println();
        }
    }

    /**
     * Categorical domains must be quoted, or irace tries to evaluate them
     */
    private String categoricalDomainInnards(Collection<String> domain)
    {
        List<String> quotedStrings = new ArrayList<String>();
        for(String d : domain){
            quotedStrings.add("\"" + d + "\"");
        }
        return Util.joinStrings(", ", quotedStrings);
    }

    /**
     * irace does not like underscores, or leading numbers...
     */
    private String nameTranslator(String name)
    {
        return "p" + name.replace("_", "");
    }

    private Parameter transformParameter(Parameter param)
    {
        switch(param.type)
        {
            case CATEGORICAL:
            case INTEGER:
            case NUMERIC:
            {
                return param;
            }
            case LOG_INTEGER:
                //Make sure that the name contains the INTification
                if(!param.name.contains("_INT_")){
                    throw new RuntimeException("the parameter '" + param.name + "' does not have the _INT_ that it should....");
                }
            case LOG_NUMERIC:
            {
                //We need to make a new parameter that has been logged
                String newName = param.name;
                int insertIndex = newName.lastIndexOf("_")+1;
                newName = newName.substring(0, insertIndex) + "LOG_" + newName.substring(insertIndex);
                Parameter newParam = new Parameter(newName, param);

                //Logificate the parameters, and set us to NUMERIC
                newParam.defaultNumeric = (float)Math.log10(newParam.defaultNumeric);
                newParam.minNumeric = (float)Math.log10(newParam.minNumeric);
                newParam.maxNumeric = (float)Math.log10(newParam.maxNumeric);
                newParam.type = Parameter.ParamType.NUMERIC;

                return newParam;
            }
        }
        throw new RuntimeException("Don't know how to transform a " + param.type.toString());

    }
}
