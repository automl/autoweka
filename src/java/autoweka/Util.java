package autoweka;

import java.util.ArrayList;
import java.util.Properties;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.regex.Pattern;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.Loader;
import weka.core.WekaPackageManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bunches of random stuff that seems to be handy in all sorts of places
 */
public class Util
{
    final static Logger log = LoggerFactory.getLogger(Util.class);

    /**
     * Given a property string (var1=val1:var2=val2:....) convert it to a property object.
     *
     * Note that this honours escaped colons
     *
     * @param propStr The property string.
     * @return The properties object.
     */
    static public Properties parsePropertyString(String propStr)
    {
        Properties props = new Properties();
        parsePropertyString(props, propStr);
        return props;
    }

    //Because some people don't like backslashes....
    //private static Pattern msPropertyStringPattern = Pattern.compile("(?<!\\\\):");
    private static Pattern msPropertyStringPattern = Pattern.compile("(?<!\\[\\]|\\[\\@\\]|\\\\|__COLONESCAPE__):");

    /**
     * Given a property string (var1=val1:var2=val2:....) convert it to a property object.
     *
     * Note that this honours escaped colons
     *
     * @param props The properties object.
     * @param propStr The property string.
     */
    static public void parsePropertyString(Properties props, String propStr)
    {
        //Deal with silly users
        if(propStr == null || propStr.isEmpty())
            return;

        String[] strArray = msPropertyStringPattern.split(propStr);
        for(String str:strArray)
        {
            int equalsIndex = str.indexOf("=");
            if(equalsIndex == -1)
            {
                throw new RuntimeException("Invalid property '" + str + "'");
            }
            String key = str.substring(0,equalsIndex);
            //Still to do with those pesky backslashes
            //String value = str.substring(equalsIndex+1).replace("\\:", ":");
            //String value = str.substring(equalsIndex+1).replace("[@]:", ":");
            String value = str.substring(equalsIndex+1).replace("[@]:", ":").replace("\\:", ":").replace("[]:", ":").replace("__COLONESCAPE__:", ":");
            props.setProperty(key, value);
        }
    }

    /**
     * Converts a Properties object to a property string, escaping ':' as needed
     *
     * @param props The properties object.
     * @return The property string.
     */
    static public String propertiesToString(Properties props){
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for(String key: props.stringPropertyNames()){
            if(!first){
                sb.append(":");
            }
            first = false;
            sb.append(key);
            sb.append("=");
            //More backslashes
            //sb.append(props.getProperty(key).replaceAll("\\:", "\\\\:"));
            //sb.append(props.getProperty(key).replaceAll("\\:", "[\\@]:"));
            //sb.append(props.getProperty(key).replaceAll("\\:", "[]:"));
            sb.append(props.getProperty(key).replaceAll("\\:", "__COLONESCAPE__:"));
        }
        return sb.toString();
    }

    /**
     * Looks for command line arguments of the form -prop PROPERTYSTRING and adds them to the given properties object
     *
     * @param props The properties object.
     * @param cmdLineArgs The arguments.
     */
    static public void parseCommandLineProperties(Properties props, String[] cmdLineArgs)
    {
        parseCommandLineProperties(props, Arrays.asList(cmdLineArgs));
    }

    /**
     * Looks for command line arguments of the form -prop PROPERTYSTRING and adds them to the given properties object
     *
     * @param props The properties object.
     * @param cmdLineArgs The arguments.
     */
    static public void parseCommandLineProperties(Properties props, List<String> cmdLineArgs)
    {
        for(int i = 0; i < cmdLineArgs.size(); i++)
        {
            if(cmdLineArgs.get(i).startsWith("-prop"))
            {
                parsePropertyString(props, cmdLineArgs.get(++i));
            }
        }
    }

    /**
     * Initializes an empty file at the given path if it doesn't already exist.
     *
     * @param aLogPath The path to the file.
     */
   	public static void initializeFile(String aLogPath){

   		try{
   			File logFile = new File(aLogPath);
   			if(!logFile.exists()){
   				logFile.createNewFile();
   			}
   		}catch(Exception e){
   			System.out.println("Couldn't initialize file at this path: "+aLogPath);
   		}

   	}

    /**
     * Makes folders along the specified path.
     *
     * @param basePath The path.
     */
    static public void makePath(String basePath)
    {
        File path = new File(basePath);
        try
        {
            if(!path.exists())
            {
                if(!path.mkdirs())
                    //Did someone sneaky make it?
                    if(!path.exists())
                        throw new RuntimeException("Failed to create directory:" + basePath);
            }
            else if(!path.isDirectory())
            {
                throw new RuntimeException("Something is already has this name, and it's not a dir: " + basePath);
            }
        }
        catch(Exception e)
        {
            log.error(e.getMessage(), e);
            throw new RuntimeException("Failed to create directory:" + basePath);
        }
    }

    static public String getAbsoluteClasspath()
    {
        return System.getProperty("java.class.path") + java.io.File.pathSeparatorChar + URLDecoder.decode(Util.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    }

    /**
     * Tries to get the full path to the Java Executable that we're running.
     *
     * @return The string describing the path to the executable.
     */
    public static String getJavaExecutable()
    {
        return System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }

    /**
     * Tries to find the given executable on the path.
     *
     * @param executableName The executable to look for.
     * @return The executable file.
     */
    public static File findExecutableOnPath(String executableName)
    {
        //Check, did they specify something we can resolve?
        File fullyQualifiedExecutable = null;

        fullyQualifiedExecutable = new File(expandPath(executableName));
        //if(1 > 0)
        //    return new File(executableName);
        if(fullyQualifiedExecutable.isFile())
        {
            return fullyQualifiedExecutable;
        }
        //Otherwise, keep looking
        String systemPath = System.getenv("PATH");
        String[] pathDirs = systemPath.split(File.pathSeparator);

        for (String pathDir : pathDirs)
        {
            fullyQualifiedExecutable = new File(pathDir, executableName);
            if (fullyQualifiedExecutable.isFile())
            {
                return fullyQualifiedExecutable;
            }
        }
        //We failed to find it
        return null;
    }

    /**
     * Tries to evaluate the environment variable into something useful.
     *
     * Note, this probably only works on posix systems now...
     *
     * @param var The environment variable to expand.
     * @return The expanded environment variable.
     */
    public static String expandEnvironmentVariable(String var)
    {
        if(System.getProperty("os.name").toLowerCase().indexOf("win") == 0){
            try
            {
                String[] splitVar = var.split(java.io.File.pathSeparator);
                String expanded = "";
                for(int i = 0; i < splitVar.length; i++)
                {
                    Process shellExec = Runtime.getRuntime().exec( new String[]{"cmd.exe", "/C", "echo", splitVar[i]});

                    java.io.BufferedReader reader = new java.io.BufferedReader( new java.io.InputStreamReader(shellExec.getInputStream()));
                    String res = reader.readLine();
                    if (res != null)
                    {
                       expanded += res;
                       if(i < splitVar.length-1)
                           expanded += java.io.File.pathSeparator;
                    }
                }

                return expanded;
            }
            catch (java.io.IOException ex)
            {
                // Crap....
                log.warn("Failed to evaluate environment variable contents {}", var);
            }
        }
        else //We're probably on a posix system....
        {
            try
            {
                String[] splitVar = var.split(java.io.File.pathSeparator);
                String expanded = "";
                for(int i = 0; i < splitVar.length; i++)
                {
                    String v = splitVar[i];

                    String command = "eval echo \"" + v + "\"";
                    Process shellExec = Runtime.getRuntime().exec( new String[]{"bash", "-c", command});

                    java.io.BufferedReader reader = new java.io.BufferedReader( new java.io.InputStreamReader(shellExec.getInputStream()));
                    String res = reader.readLine();
                    if (res != null)
                    {
                       expanded += res;
                       if(i < splitVar.length-1)
                           expanded += java.io.File.pathSeparator;
                    }
                }

                return expanded;
            }
            catch (java.io.IOException ex)
            {
                // Crap....
                log.warn("Failed to evaluate environment variable contents {} - probably due to a lack of unix...", var);
            }
        }

        return var;
    }

    /**
     * Tries to resolve any relative paths/symlinks on a given path.
     *
     * @param path Th path to expand.
     * @return The expanded path.
     */
    public static String expandPath(String path)
    {
        if(System.getProperty("os.name").toLowerCase().indexOf("win") == 0){
            String exp = path;
            try
            {
                Process shellExec = Runtime.getRuntime().exec("cmd.exe /C echo " + exp);

                java.io.BufferedReader reader = new java.io.BufferedReader( new java.io.InputStreamReader(shellExec.getInputStream()));
                String expandedPath = reader.readLine();

                // Only return a new value if expansion worked.
                // We're reading from stdin. If there was a problem, it was written
                // to stderr and our result will be null.
                if (expandedPath != null)
                {
                    exp = expandedPath;
                }
            }
            catch (java.io.IOException ex)
            {
                // Just consider it unexpandable and return original path.
                log.warn("Failed to expand path {}", path, ex);
            }
            File f = new File(exp);
            path = URLDecoder.decode(f.getAbsolutePath());
        }
        else //We're probably on a posix system....
        {
            try
            {
                String command = "ls -d " + path;
                Process shellExec = Runtime.getRuntime().exec( new String[]{"bash", "-c", command});

                java.io.BufferedReader reader = new java.io.BufferedReader( new java.io.InputStreamReader(shellExec.getInputStream()));
                String expandedPath = reader.readLine();


                // Only return a new value if expansion worked.
                // We're reading from stdin. If there was a problem, it was written
                // to stderr and our result will be null.
                if (expandedPath != null)
                {
                    path = expandedPath;
                }
            }
            catch (java.io.IOException ex)
            {
                // Just consider it unexpandable and return original path.
                log.warn("Failed to expand path {}", path, ex);
            }
        }
        return path;
    }

    /**
     * Utility to class to make a void print stream.
     */
    static public class NullPrintStream extends PrintStream {
        public NullPrintStream(){
            super((OutputStream)null);
        }
        public void write(byte[] buf, int off, int len) {}
        public void write(int b) {}
        public void write(byte [] b) {}
    }

    /**
     * Have a list of list of strings and need the Cartesian product? Look no further!
     *
     * @param sets The list of list of strings to compute the Cartesian product
     * for.
     * @return The Cartesian product.
     */
    public static List<List<String>> cartesianProduct(List<List<String>> sets) {
        if (sets.size() < 2)
            return sets;
            //throw new IllegalArgumentException("Can't have a product of fewer than two sets (got " + sets.size() + ")");

        return _cartesianProduct(sets.size()-1, sets);
    }

    private static List<List<String>> _cartesianProduct(int index, List<List<String>> sets) {
        List<List<String>> ret = new ArrayList<List<String>>();
        if (index < 0 ) {
            ret.add(new ArrayList<String>());
        } else {
            for (String obj : sets.get(index)) {
                for (List<String> set : _cartesianProduct(index-1, sets)) {
                    set.add(obj);
                    ret.add(set);
                }
            }
        }
        return ret;
    }

    /**
     * Returns a string containing the arguments sorted by their name and joined together.
     *
     * @param argMap The map of arguments.
     * @return The string representation of the arguments.
     */
    public static String argMapToString(Map<String, String> argMap){
        ArrayList<String> argKeys = new ArrayList<String>(argMap.keySet());
        java.util.Collections.sort(argKeys);

        StringBuilder sb = new StringBuilder();
        for(String k: argKeys)
        {
            sb.append("-");
            sb.append(k);
            sb.append(" ");
            sb.append(argMap.get(k));
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Removes any arguments in the inputMap that happen to have a HIDDEN in their name.
     *
     * @param inputMap The input map of arguments.
     * @return The output map of arguments with hidden arguments removed.
     */
    public static Map<String, String> removeHidden(Map<String, String> inputMap){
        Map<String, String> argMap = new HashMap<String, String>(inputMap);

        HashSet<String> keys = new HashSet<String>(argMap.keySet());
        for(String key : keys){
            if(key.contains("HIDDEN")){
                argMap.remove(key);
            }
        }
        return argMap;
    }

    /**
     * Removes the extension from a file name string.
     *
     * @param s The file name string.
     * @return The file name string without extension.
     */
    public static String removeExtension(String s)
    {

        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = s.lastIndexOf(System.getProperty("file.separator"));
        if (lastSeparatorIndex == -1) {
            filename = s;
        } else {
            filename = s.substring(lastSeparatorIndex + 1);
        }

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1)
            return filename;

        return filename.substring(0, extensionIndex);
    }

    /**
     * Joins a list of strings together separated by a delim in the middle.
     *
     * @param delim The delimiter used to join the strings.
     * @param strs The strings to join.
     * @return The joined string.
     */
    static public String joinStrings(String delim, String ... strs)
    {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < strs.length-1; i++){
            sb.append(strs[i]);
            sb.append(delim);
        }
        sb.append(strs[strs.length-1]);
        return sb.toString();
    }

    /**
     * Joins a collection of strings together separated by a delim in the
     * middle.
     *
     * @param delim The delimiter used to join the strings.
     * @param strs The strings to join.
     * @return The joined string.
     */
    static public String joinStrings(String delim, Collection<String> strs)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(String str : strs){
            if(first){
                first = false;
            }else{
                sb.append(delim);
            }
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * Looks for any strings that have spaces in them, and surrounds them with quotes.
     *
     * @param oldStrs The strings with spaces to escape.
     * @return The escaped strings.
     */
    static public List<String> quoteStrings(Collection<String> oldStrs)
    {
        List<String> strs = new ArrayList<String>(oldStrs);
        for(int i = 0; i < strs.size(); i++){
            if(strs.get(i).contains(" ")){
                strs.set(i, "\"" + strs.get(i) + "\"");
            }
        }
        return strs;
    }

    /**
     * Splits a string, returning the string separated by the delimiter, with only the numLeft and numRight taken from the appropriate sides.
     *
     * @param params The string to split.
     * @param delimStr The delimiter to split at.
     * @param numLeft Characters to the left to take.
     * @param numRight Characters to the right to take.
     * @return The list of split strings.
     */
    static public List<String> splitNestedString(String params, String delimStr, int numLeft, int numRight)
    {
        ArrayList<String> args = new ArrayList<String>(numLeft);
        ArrayList<String> endArgs = new ArrayList<String>(numRight);

        //We need to rip chunks out of the left side
        int startIndex, endIndex;
        startIndex = 0;
        endIndex = 0;
        for(int i = 0; i < numLeft; i++){
            endIndex = params.indexOf(delimStr, startIndex);
            if(endIndex == -1)
                throw new IllegalArgumentException("Could only find " + i + " left args before not finding the delimiter");
            args.add(params.substring(startIndex, endIndex));
            startIndex = endIndex+delimStr.length();
        }
        int middleStart = startIndex;

        //Now go find stuff on the right
        endIndex = params.length();
        startIndex = endIndex;
        for(int i = 0; i < numRight; i++){
            startIndex = params.lastIndexOf(delimStr, endIndex-1);
            if(startIndex == -1)
                throw new IllegalArgumentException("Could only find " + i + " right args before not finding the delimeter");
            endArgs.add(params.substring(startIndex+delimStr.length(), endIndex));
            endIndex = startIndex;
        }
        int middleEnd = startIndex;

        if(middleStart >= middleEnd){
            throw new IllegalArgumentException("The number of left and right args doesn't leave anything in the middle");
        }

        args.add(params.substring(middleStart, middleEnd));
        for(int i = endArgs.size()-1; i >= 0; i--){
            args.add(endArgs.get(i));
        }

        return args;
    }


    /**
     * Makes a simple real valued dataset of a given size, with the class determined by index
     *
     * @param numInstances The number of instances.
     * @param numClasses The number of classes.
     * @param numUsefulNumeric The number of correlated numeric attributes.
     * @param numUsefulCategorical The number of correlated categoric
     * attributes.
     * @param numRandomNumeric The number of uncorrelated numeric attributes.
     * @param numRandomCategorical The number of uncorrelated categoric
     * attributes.
     * @param randomCategoricalSize The number of categories in the uncorrelated
     * categoric attributes.
     * @param seed The random seed.
     * @return The instances.
     */
    static public Instances createDummyInstances(int numInstances, int numClasses, int numUsefulNumeric, int numUsefulCategorical, int numRandomNumeric, int numRandomCategorical, int randomCategoricalSize, int seed)
    {
        ArrayList<Attribute> attInfo = new ArrayList<Attribute>();
        ArrayList<String> classes = new ArrayList<String>();
        for(int i = 0; i < numClasses; i++){
            classes.add(Integer.toString(i));
        }

        //Make our random categorical domain
        ArrayList<String> randomCategorical = new ArrayList<String>();
        for(int i = 0; i < randomCategoricalSize; i++){
            randomCategorical.add("rcat" + Integer.toString(i));
        }

        for(int i = 0; i < numUsefulNumeric; i++){
            attInfo.add(new Attribute("usefulNumeric" + Integer.toString(i)));
        }
        for(int i = 0; i < numUsefulCategorical; i++){
            attInfo.add(new Attribute("usefulCategorical" + Integer.toString(i), classes));
        }
        for(int i = 0; i < numRandomNumeric; i++){
            attInfo.add(new Attribute("randomNumeric" + Integer.toString(i)));
        }
        for(int i = 0; i < numRandomCategorical; i++){
            attInfo.add(new Attribute("randomCategorical" + Integer.toString(i), randomCategorical));
        }
        attInfo.add(new Attribute("class", classes));

        Random rand = new Random(seed);

        Instances instances = new Instances("dummy", attInfo, numInstances);
        instances.setClassIndex(attInfo.size()-1);
        for(int j = 0; j < numInstances; j++){
            double[] features = new double[attInfo.size()];
            int classValue = j % numClasses;

            int index = 0;
            for(int i = 0; i < numUsefulNumeric; i++){
                features[index++] = classValue * (i+1);
            }
            for(int i = 0; i < numUsefulCategorical; i++){
                features[index++] = (classValue * (i+1)) % numClasses;
            }
            for(int i = 0; i < numRandomNumeric; i++){
                features[index++] = rand.nextGaussian();
            }
            for(int i = 0; i < numRandomCategorical; i++){
                features[index++] = rand.nextInt(randomCategoricalSize);
            }
            features[index++] = classValue;

            instances.add(new DenseInstance(1, features));
        }
        return instances;
    }

    /**
     * Gets the root Auto-WEKA location - or at least comes close.
     *
     * Prints an error message if it fails
     */
    public static String getAutoWekaDistributionPath()
    {
        return WekaPackageManager.PACKAGES_DIR.getAbsolutePath() + File.separator + "Auto-WEKA";
    }

    /**
     * Splits a string based on spaces, grouping atoms if they are inside non escaped double quotes.
     *
     * @param str The string to split.
     * @return The list of split strings.
     */
    static public List<String> splitQuotedString(String str)
    {
        List<String> strings = new ArrayList<String>();
        int level = 0;
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < str.length(); i++){
            char c = str.charAt(i);
            if(c == '"') {
                if(i == 0 || str.charAt(i-1) == ' ') {
                    // start quote
                    level++;
                    // don't need to append quotes for top-level things
                    if(level == 1) continue;
                }
                if(i == str.length() - 1 || str.charAt(i+1) == ' ' || str.charAt(i+1) == '"') {
                    // end quote
                    level--;
                    if(level == 0) continue;
                }
            }
            //Peek at the next character - if we have a \", we need to only insert a "
            if(c == '\\' && i < str.length()-1 && str.charAt(i+1) == '"')
            {
                c = '"';
                i++;
            }

            if(level == 0 && str.charAt(i) == ' ') {
                // done with this part
                if(buffer.length() > 0) {
                    strings.add(buffer.toString());
                    buffer.setLength(0);
                }
            } else {
                buffer.append(c);
            }
        }
        //Add on the last string if needed
        if(buffer.length() > 0){
            strings.add(buffer.toString());
        }

        return strings;
    }

    public static class ProcessKillerShutdownHook extends Thread
    {
        private Process mProc;
        public ProcessKillerShutdownHook(Process proc)
        {
            mProc = proc;
        }

        @Override
        public void run()
        {
            mProc.destroy();
        }
    }


    public static Instances loadDataSource(java.io.InputStream stream) throws Exception
    {
        ArffLoader loader = new ArffLoader();
        loader.setSource(stream);
        return loader.getDataSet();
    }

    public static void copyFile(File sourceFile, File destFile) {
        try
        {
            if(!destFile.exists()) {
                destFile.createNewFile();
            }

            FileChannel source = null;
            FileChannel destination = null;

            try {
                source = new FileInputStream(sourceFile).getChannel();
                destination = new FileOutputStream(destFile).getChannel();
                destination.transferFrom(source, 0, source.size());
            }
            finally {
                if(source != null) {
                    source.close();
                }
                if(destination != null) {
                    destination.close();
                }
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
