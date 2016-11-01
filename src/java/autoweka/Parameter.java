package autoweka;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Captures all the data about if this parameter is categorical, numeric (and what the ranges are), and some other helper functions to get some data out of them
 */
public class Parameter
{
    /**
     * Creates a Parameter from a given line in a .param file.
     * @param line The line.
     */
    public Parameter(String line)
    {
        Matcher m = paramPattern.matcher(line);
        if(!m.matches())
        {
            throw new RuntimeException("Failed to match parameter line '" + line + "'");
        }
        name = m.group(1);
        if(m.group(2).equals("{"))
        {
            type = ParamType.CATEGORICAL;
            defaultCategorical = m.group(4).trim();
            String[] innards = m.group(3).split(",");
            categoricalInnards = new ArrayList<String>(innards.length);
            //Figure out if the default even makes sense
            boolean foundDefault = false;
            for(int i = 0; i < innards.length; i++)
            {
                String innard = innards[i].trim();
                categoricalInnards.add(innard);
                foundDefault |= innard.equals(defaultCategorical);
                defaultCategoricalIndex = i;
            }
            if(!foundDefault)
                throw new RuntimeException("Default not found in set in " + line);
        }
        else
        {
            defaultNumeric = Float.parseFloat(m.group(4));
            type = ParamType.NUMERIC;

            //Get the min and max
            String[] vals = m.group(3).split(",");
            if(vals.length != 2)
                throw new RuntimeException("Expected two values in '" + m.group(3) + "'");
            minNumeric = Float.parseFloat(vals[0]);
            maxNumeric = Float.parseFloat(vals[1]);

            //Figure out what type we are
            if(m.group(5) != null)
            {
                if(m.group(5).contains("i"))
                {
                    if(m.group(5).contains("l"))
                        type = ParamType.LOG_INTEGER;
                    else
                        type = ParamType.INTEGER;
                }
                else
                {
                    if(m.group(5).contains("l"))
                        type = ParamType.LOG_NUMERIC;
                    else
                        type = ParamType.NUMERIC;
                }
            }

            if(type == ParamType.LOG_INTEGER || type == ParamType.LOG_NUMERIC)
            {
                if(minNumeric <= 0 || maxNumeric <= 0)
                {
                    throw new RuntimeException("For parameter '" + line + "', bounds must be positive for logarithmic priors");
                }
            }
        }
    }
    
    public Parameter(String name, String categoricalOption)
    {
        this(name, Collections.singletonList(categoricalOption));
    }
    
    public Parameter(String name, List<String> categoricalOptions)
    {
        this(name, categoricalOptions, categoricalOptions.get(0));
    }

    public Parameter(String name, List<String> categoricalOptions, String defaultOption)
    {
        this.name =  name;
        type =  ParamType.CATEGORICAL;
        defaultCategorical = defaultOption;
        categoricalInnards = new ArrayList<String>();
        int i = 0;
        for(String opt : categoricalOptions){
            categoricalInnards.add(opt);
            if(opt.equals(defaultOption)){
                defaultCategoricalIndex = i;
            }
        }
        if(defaultCategoricalIndex == -1)
            throw new RuntimeException("Could not find default option '" + defaultOption + "'");
    }

    public Parameter(String newName, Parameter clone)
    {
        name = newName;
        type = clone.type;
        defaultCategorical = clone.defaultCategorical;
        defaultCategoricalIndex = clone.defaultCategoricalIndex;
        categoricalInnards = clone.categoricalInnards;
        if(categoricalInnards != null)
            categoricalInnards = new ArrayList(categoricalInnards);
        defaultNumeric = clone.defaultNumeric;
        minNumeric = clone.minNumeric;
        maxNumeric = clone.maxNumeric;
    }

    //public float getDistanceFromDefault(String value)
    //{
        //switch(type)
        //{
        //case CATEGORICAL:
            //return value.equals(defaultCategorical) ? 0f : 1.0f;
        //case NUMERIC:
        //case INTEGER:
            //{
                //float val = Float.parseFloat(value);
                //val = Math.max(Math.min(val, maxNumeric), minNumeric);
                //return val - defaultNumeric;
            //}
        //case LOG_NUMERIC:
        //case LOG_INTEGER:
            //{
                //float val = Float.parseFloat(value);
                //val = Math.max(Math.min(val, maxNumeric), minNumeric);
                //return (float)(Math.log(val) - Math.log(defaultNumeric));
            //}
        //}
        //return 0;
    //}

    //public float getNormalizedDistanceFromDefault(String value)
    //{
        //switch(type)
        //{
        //case CATEGORICAL:
            //return value.equals(defaultCategorical) ? 0f : 1.0f;
        //case NUMERIC:
        //case INTEGER:
            //{
                //float val = Float.parseFloat(value);
                //if(val > defaultNumeric)
                //{
                    //return (val - defaultNumeric) / (maxNumeric - defaultNumeric);
                //}
                //else
                //{
                    //return (val - defaultNumeric) / (defaultNumeric - minNumeric);
                //}
            //}
        //case LOG_NUMERIC:
        //case LOG_INTEGER:
            //{
                //float val = Float.parseFloat(value);
                //if(val > defaultNumeric)
                //{
                    //return (float) ((Math.log(val) - Math.log(defaultNumeric)) / (Math.log(maxNumeric) - Math.log(defaultNumeric)));
                //}
                //else
                //{
                    //return (float) ((Math.log(val) - Math.log(defaultNumeric)) / (Math.log(defaultNumeric) - Math.log(minNumeric)));
                //}
            //}
        //}
        //return 0;
    //}

    /**
     * Creates a List of Strings that contain the discretization of this parameter. 
     *
     * Numeric parameters will have at most numPoints values, while Categorical parameters have all elements in their domain
     *
     * @param numPoints The number of points.
     * @return The list of discretizations.
     */
    public List<String> getDiscretization(int numPoints){
        ArrayList<String> points = new ArrayList<String>();
        switch(type){
        case CATEGORICAL:
            for(String s: categoricalInnards){
                points.add(s);
            }
            break;
        case NUMERIC:
        case INTEGER:
            for(int i = 0; i < numPoints; i++){
                float val = (maxNumeric - minNumeric) / (numPoints-1) * i + minNumeric;
                if(type == ParamType.INTEGER){
                    points.add(Integer.toString(Math.round(val)));
                }else{
                    points.add(Float.toString(val));
                }
            }
            break;
        case LOG_NUMERIC:
        case LOG_INTEGER:
            for(int i = 0; i < numPoints; i++){
                float val = (float)Math.exp((Math.log(maxNumeric) - Math.log(minNumeric)) / (numPoints-1) * i + Math.log(minNumeric));
                if(type == ParamType.INTEGER){
                    points.add(Integer.toString(Math.round(val)));
                }else{
                    points.add(Float.toString(val));
                }
            }
            break;
        }
        return points;
    }

    public String getRandomValue(Random rand){
        switch (type){
            case CATEGORICAL:
                return categoricalInnards.get(rand.nextInt(categoricalInnards.size()));
            case NUMERIC:
            case INTEGER:
            {
                float val = rand.nextFloat()*(maxNumeric - minNumeric) + minNumeric;
                if(type == ParamType.INTEGER){
                    return Integer.toString((int)Math.round(val));
                }else{ //type == ParamType.NUMERIC
                    return Float.toString(val);
                }
            }
            case LOG_NUMERIC:
            case LOG_INTEGER:
            {
                float val = (float)Math.exp((Math.log(maxNumeric) - Math.log(minNumeric)) * rand.nextFloat() + Math.log(minNumeric));
                if(type == ParamType.LOG_INTEGER){
                    return Integer.toString((int)Math.round(val));
                }else{ //type == ParamType.NUMERIC
                    return Float.toString(val);
                }
            }
        }
        throw new RuntimeException("Unhandeld parameter type '" + type  + "'");
    }

    public String toString()
    {
        String repr = name + " ";
        if(type == ParamType.CATEGORICAL)
        {
            repr += "{" + Util.joinStrings(", ", categoricalInnards) + "} [" + defaultCategorical + "]";
        }
        else
        {
            if(type == ParamType.INTEGER || type == ParamType.LOG_INTEGER)
                //Stupid integer case....
                repr += "[" + (int)Math.round(minNumeric) + ", " + (int)Math.round(maxNumeric) + "] [" + (int)Math.round(defaultNumeric) + "]i";
            else
                repr += "[" + minNumeric + ", " + maxNumeric + "] [" + defaultNumeric + "]";

            //Add the log suffix if we need to
            if(type == ParamType.LOG_NUMERIC || type == ParamType.LOG_INTEGER)
                repr += "l";
        }
        return repr;
    }

    private static final Pattern paramPattern = Pattern.compile("\\s*([a-zA-Z0-9_-]*)\\s*([\\[{])(.*)[\\]}]\\s*\\[(.*)\\]([il]*).*");
    public enum ParamType { CATEGORICAL, NUMERIC, LOG_NUMERIC, INTEGER, LOG_INTEGER};

    /**
     * The name of this parameter
     */
    public String name = null;
    /**
     * The type of this parameter
     */
    public ParamType type = ParamType.CATEGORICAL;
    /**
     * The string containing the default categorical value - only valid if type == CATEGORICAL
     */
    public String defaultCategorical = null;
    /**
     * The index containing the default categorical value - only valid if type == CATEGORICAL
     */
    public int defaultCategoricalIndex = -1;
    /**
     * The list containing all the possible values of the parameter - only valid if type == CATEGORICAL
     */
    public List<String> categoricalInnards = null;
    /**
     * The default numeric value - Stored as a float but will be converted to an int if needed
     */
    public float defaultNumeric = 0;
    /**
     * The minimal numeric value - Stored as a float but will be converted to an int if needed
     */
    public float minNumeric = 0;
    /**
     * The maximal numeric value - Stored as a float but will be converted to an int if needed
     */
    public float maxNumeric = 0;
}
