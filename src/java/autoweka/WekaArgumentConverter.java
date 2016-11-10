package autoweka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that can convert arguments from Auto-WEKA to WEKA
 */
public class WekaArgumentConverter
{
    final static Logger log = LoggerFactory.getLogger(WekaArgumentConverter.class);

    /**
     * Converts arguments from the Auto-WEKA format into something that WEKA can actually understand.
     *
     * @param args The list of arguments.
     * @return The WEKA arguments.
     */
    public static Arguments convert(List<String> args){
        List<ArgumentPair> sortedArgPairs = sortArgs(args);
        return processArgs(sortedArgPairs);
    }

    public static class Arguments{
        private Arguments(Map<String, String> _propertyMap, Map<String, List<String>> _argMap){
            propertyMap = _propertyMap;
            argMap = _argMap;
        }
        /**
         * Contains a bunch of properties like the 'targetclass', 'attributeeval', 'attributesearch', and 'attributetime'
         */
        public Map<String, String> propertyMap;
        /**
         * Contains up to three elements, 'attributesearch' for all params related to the search process, 'attributeeval' for the evaluators, and 'classifier' which has all the arguments for the classifier
         */
        public Map<String, List<String>> argMap;

        public String toString(){
            return "Property Map:"+this.propertyMap.toString()+" ||| Arg Map:"+this.argMap.toString();
        }
    }
    
    private static Arguments processArgs(List<ArgumentPair> argList)
    {
        String quotedString = null;
        int quoteDepth = 0;
        Map<String, List<String>> argMap = new HashMap<String, List<String>>();
        HashMap<String, String> propertyMap = new HashMap<String, String>();

        PrefixElement[] prefixElements = new PrefixElement[]{ new PrefixElement("assearch_", "attributesearch"), 
                                                              new PrefixElement("aseval_", "attributeeval"),
                                                              new PrefixElement("", "classifier") };

        String[] propertyNames = new String[]{"targetclass", "attributeeval", "attributesearch", "attributetime"};
        //Make sure that the dest map has everything that we'd want
        for(PrefixElement ele: prefixElements){
            if(argMap.get(ele.mapName) == null){
                argMap.put(ele.mapName, new ArrayList<String>());
            }
        }

        for(ArgumentPair arg: argList)
        {
            //What is the current argument?
            if(arg.name.equals("REMOVED") || arg.name.contains("HIDDEN") || arg.value.equals("REMOVE_PREV"))
            {
                //We don't want to do anything with this arg
                continue;
            }

            boolean gobbeled = false;
            for(String property : propertyNames){
                if(arg.name.equals("-" + property)){
                    propertyMap.put(property, arg.value);
                    gobbeled = true;
                }
            }
            if(gobbeled)
                continue;

            //Figure out what array list we should be inserting into
            List<String> dest = null;
            for(PrefixElement ele : prefixElements) 
            {
                if(arg.name.startsWith(ele.prefix) || arg.name.startsWith(ele.prefix, 1)){
                    //We made sure earlier that this already exists
                    dest = argMap.get(ele.mapName);
                    break;
                }
            }
            //Check to make sure we have something
            if(dest == null)
            {
                //Well crap, we don't
                throw new RuntimeException("Couldn't find a home for the arg '" + arg.name + "'");
            }

            if(arg.name.contains("LOG_"))
            {
                //Undo the log_10
                arg.value = String.format("%f", Math.pow(10, Float.parseFloat(arg.value)));
            }

            if(arg.name.contains("INT_"))
            {
                int val = (int)Math.round(Float.parseFloat(arg.value));
                arg.value = String.format("%d", val);
            }


            String sanitizedName = arg.name;
            if(arg.name.lastIndexOf('_') != -1)
                sanitizedName = "-" + arg.name.substring(1+arg.name.lastIndexOf('_'));

            if(quotedString == null)
            {
                //Should we actually be the start of a quote?
                if(arg.name.endsWith("QUOTE_START"))
                {
                    quotedString = "";//"\"";
                    quoteDepth++;
                    continue;
                }
                else if(arg.name.contains("QUOTE_START"))
                {
                    //We need to add this parameter name, then start a quoted string
                    dest.add(sanitizedName);
                    quoteDepth++;
                    quotedString = "";//"\"";
                    if(!arg.value.equals("REMOVED"))
                        quotedString += arg.value + " ";
                }
                else if(arg.name.contains("DASHDASH")){
                    dest.add("--");
                }
                else
                {
                    //Actually push it back
                    dest.add(sanitizedName);
                    if(!arg.value.equals("REMOVED"))
                        dest.add(arg.value);
                }
            }
            else
            {
                //Should we pop this quote?
                if(arg.name.endsWith("QUOTE_END"))
                {
                    quotedString = quotedString.trim();
                    if(quoteDepth > 1) quotedString += "\" ";
                    quoteDepth--;
                    if(quoteDepth == 0)
                    {
                        dest.add(quotedString);
                        quotedString = null;
                    }
                    continue;
                }

                //Should we actually be the start of a quote?
                if(arg.name.endsWith("QUOTE_START"))
                {
                    quotedString += "\"";
                    quoteDepth++;
                    continue;
                }
                else if(arg.name.contains("QUOTE_START"))
                {
                    quotedString += sanitizedName + " \"";
                    quoteDepth++;
                }
                else
                {
                    quotedString += sanitizedName + " ";
                }

                if(!arg.value.equals("REMOVED"))
                    quotedString += arg.value + " ";
            }
        }
        for(String s: argMap.get("classifier"))
            log.trace("arg: {}", s);

        if(quotedString != null)
            throw new RuntimeException("Unbalanced QUOTE markers in arguments" + quoteDepth);


        return new Arguments(propertyMap, argMap);
    }



    private static List<ArgumentPair> sortArgs(List<String> args)
    {
        ArrayList<ArgumentPair> argPairs = new ArrayList<ArgumentPair>();
        for(int i = 0; i < args.size(); i+=2)
        {
            if(i + 1 == args.size()) break;
            ArgumentPair arg = new ArgumentPair(args.get(i), args.get(i+1));
            //Is the name actually a double dash?
            argPairs.add(arg);
        }
        java.util.Collections.sort(argPairs);
        return argPairs;
    }

    private static class ArgumentPair implements Comparable<ArgumentPair>
    {
        public ArgumentPair(String _name, String _value)
        {
            name = _name;
            value = _value;
        }

        public int compareTo(ArgumentPair rhs)
        {
            return name.compareTo(rhs.name);
        }

        public String toString() {
            return name + ": " + value;
        }
        
        public String name;
        public String value;
    }
    
    //Curse you java without your std::pair
    private static class PrefixElement{
        public PrefixElement(String _prefix, String _mapName){
            prefix = _prefix;
            mapName = _mapName;
        }
        public String prefix;
        public String mapName;
    }

}
