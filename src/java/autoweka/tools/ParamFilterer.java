package autoweka.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.LinkedList;

import autoweka.ClassParams;
import autoweka.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ParamFilterer
{
    final static Logger log = LoggerFactory.getLogger(ParamFilterer.class);

    public static void main(String[] argv){
        //First argument is the .param file
        Queue<String> args = new LinkedList<String>(Arrays.asList(argv));

        ClassParams params = new ClassParams(args.poll());
        //Remaining arguments are the params in the properties format
        Properties argMap = new Properties();
        while(!args.isEmpty()){
           Util.parsePropertyString(argMap, args.poll());
        }
        Map<String, String> argStringMap = new HashMap<String, String>();
        for(Object k : argMap.keySet()){
            argStringMap.put((String)k, argMap.getProperty((String)k));
        }

        log.debug(Util.argMapToString(params.filterParams(argStringMap)));
    }
}
