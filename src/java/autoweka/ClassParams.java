package autoweka;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;

/**
 * Captures all the Parameters and Conditionals for a specific classifier that Auto-WEKA will search over
 */
public class ClassParams
{
    private static final Pattern targetClassPattern = Pattern.compile("(.*)\\.params");

    /**
     * Parses a given .param file to extract all the Parameter and Conditional objects
     *
     * @param fileName The file name.
     */
    public ClassParams(String fileName)
    {
        //Get the target class name
        File f = new File(fileName);
        Matcher m = targetClassPattern.matcher(f.getName());
        if(!m.matches())
            throw new RuntimeException("Failed to get target class name from '" + fileName + "' (Sould be (name).params");

        mTargetClass = m.group(1);
        if(mTargetClass.isEmpty())
            throw new RuntimeException("Failed to extract targetclass from " + fileName);

        //Open up this file
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(fileName));

            String line;
            boolean parsingParams = true;
            while((line = in.readLine()) != null)
            {
                //Parse out any comments
                int hashIndex = line.indexOf('#');
                if(hashIndex != -1)
                {
                    line = line.substring(0, hashIndex);
                }
                //Trim it
                line = line.trim();

                //No string? Die
                if(line.isEmpty())
                    continue;

                //Are we switching modes?
                if(line.contains("Conditionals"))
                {
                    parsingParams = false;
                    continue;
                }

                //Process this line
                if(parsingParams)
                {
                    //Try and read this in
                    Parameter param = new Parameter(line);
                    mParameters.add(param);
                    mParameterMap.put(param.name, param);
                }
                else
                {
                    mConditionals.add(new Conditional(line, mParameterMap));
                }
            }
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Given a map of parameter,value pairs, only returns a new map that has only params that are active based on the conditional rules
     *
     * @param inputMap The map consisting of parameter name to values that need to be filtered
     * @return Map with only the active parameters in it
     */
    public Map<String, String> filterParams(Map<String, String> inputMap){
        Map<String, String> argMap = new HashMap<String, String>(inputMap);
        boolean updateMade = true;
        while(updateMade)
        {
            updateMade = false;
            for(Conditional cond: mConditionals)
            {
                //Check to make sure that this thing is here
                if(argMap.get(cond.parameter.name)== null)
                    continue;

                String parentVal = argMap.get(cond.parent.name);
                if(parentVal == null)
                {
                    //This parameter is inactive - remove it!
                    argMap.remove(cond.parameter.name);
                    updateMade = true;
                    continue;
                }
                //Is the parent in one of the good values?
                if(!cond.domain.contains(parentVal))
                {
                    argMap.remove(cond.parameter.name);
                    updateMade = true;
                }
            }
        }
        return argMap;
    }

    /**
     * Gets the canonical name of the class that these parameters capture.
     * @return The class name.
     */
    public String getTargetClass()
    {
        return mTargetClass;
    }

    /**
     * Gets the list of all parameters for this class.
     * @return The list of all parameters.
     */
    public ArrayList<Parameter> getParameters()
    {
        return new ArrayList<Parameter>(mParameters);
    }

    /**
     * Gets the list of all conditionals for this class.
     * @return The list of conditionals.
     */
    public ArrayList<Conditional> getConditionals()
    {
        return new ArrayList<Conditional>(mConditionals);
    }

    /**
     * Gets a map between the parameter names and the actual parameters for this class
     * @return The mapping of parameter name to actual parameter.
     */
    public HashMap<String, Parameter> getParameterMap()
    {
        return new HashMap<String, Parameter>(mParameterMap);
    }

    private String mTargetClass;
    private HashMap<String, Parameter> mParameterMap = new HashMap<String, Parameter>();
    private ArrayList<Parameter> mParameters = new ArrayList<Parameter>();
    private ArrayList<Conditional> mConditionals = new ArrayList<Conditional>();
}
