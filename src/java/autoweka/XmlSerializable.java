package autoweka;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.xml.bind.annotation.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Deque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to make objects easily be read to/from XML, as well as specifying them on the command line
 */
public class XmlSerializable
{
    final static Logger log = LoggerFactory.getLogger(XmlSerializable.class);

    /**
     * Spits out the class to the given XML file
     * @param filename The name of the file to write.
     */
    public void toXML(String filename)
    {
        try
        {
            toXML(new PrintStream(new java.io.File(filename)));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Spits out the class to the given XML file
     * @param stream The stream to write to.
     */
    public void toXML(OutputStream stream)
    {
        try
        {
            JAXBContext context = JAXBContext.newInstance(this.getClass());
            Marshaller m = context.createMarshaller();
            m.setProperty("jaxb.formatted.output", Boolean.TRUE);
            m.marshal(this, stream);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error marshalling XML response", e);
        }
    }

    /**
     * Spits out an object from the given XML file with the given class
     * @param filename The name of the file to read.
     * @param c The class.
     * @param <T> The type of the class.
     * @return The read object.
     */
    protected static <T extends XmlSerializable> T fromXML(String filename, Class<T> c)
    {
        try
        {
            return XmlSerializable.<T>fromXML(new FileInputStream(new File(filename)), c);
        }
        catch(Exception e)
        {
            log.error(e.getMessage(), e);
            throw new RuntimeException("Failed to load from XML file");
        }
    }
    
    /**
     * Spits out an object from the given XML file with the given class
     * @param xml The input stream to read.
     * @param c The class.
     * @param <T> The type of the class.
     * @return The read object.
     */
    public static <T extends XmlSerializable> T fromXML(InputStream xml, Class<T> c)
    {
        try
        {
            JAXBContext context = JAXBContext.newInstance(c);
            Unmarshaller um = context.createUnmarshaller();
            return (T)um.unmarshal(xml);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error unmarshalling XML response", e);
        }
    }

    /**
     * Given a list of command line arguments which have the same name as the XML nodes, populate me an object with the values filled in
     * @param obj The object to populate.
     * @param args The arguments.
     * @return The number of arguments.
     */
    public static int populateObjectFromCMDParams(Object obj, String[] args)
    {
        LinkedList<String> list = new LinkedList<String>(Arrays.asList(args));
        populateObjectFromCMDParams(obj, list);
        return list.size();
    }
    
    /**
     * Given a list of command line arguments which have the same name as the XML nodes, populate me an object with the values filled in
     * @param obj The object to populate.
     * @param args The arguments.
     */
    public static void populateObjectFromCMDParams(Object obj, Deque<String> args)
    {
        try
        {
            //This thing better be an XML object
            XmlRootElement rootAnnotation = obj.getClass().getAnnotation(XmlRootElement.class);
            if(rootAnnotation == null)
                throw new IllegalStateException("Class " + obj.getClass().getName() + "does not have an XmlRootElement annotation");

            //First pass, go through the list of fields in the obj looking for XML elements     
            Map<String, ParamParser> parserMap = new HashMap<String, ParamParser>();
            for(Field f: Arrays.asList(obj.getClass().getFields())){
                XmlElement element = f.getAnnotation(XmlElement.class);
                if(element == null)
                    continue;
                
                Class<?> parserClass = msParserMap.get(f.getType());
                if(parserClass == null){
                    log.debug("{}", f.getType());
                    continue;
                }

                ParamParser parser = (ParamParser)parserClass.getDeclaredConstructor(Field.class).newInstance(f);
                parserMap.put(element.name(), parser);
            }

            //Second pass, go through our args and pass stuff off
            while(!args.isEmpty()){
                String arg = args.poll();
                //Only do stuff on objects with a -- or -
                if(!arg.startsWith("-") && !arg.startsWith("--"))
                    continue;
                //Strip them off
                arg = arg.replaceFirst("-", "").replaceFirst("-", "");
                ParamParser parserForField = parserMap.get(arg);
                if(parserForField == null){
                    args.addFirst("--" + arg);
                    return;
                }
                //Go tell the parser to deal with this critter
                parserForField.setParameter(obj, args);
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    //All the inner classes that deal with parsing out partiular params
    private static abstract class ParamParser
    {
        protected Field mField;
        public ParamParser(Field field){
            mField = field;
        }
        abstract void setParameter(Object obj, Deque<String> args) throws IllegalAccessException;
    };

    private static class IntParser extends ParamParser
    {
        public IntParser(Field field){
            super(field);
        }

        void setParameter(Object obj, Deque<String> args) throws IllegalAccessException
        {
            mField.set(obj, Integer.parseInt(args.poll()));
        }
    };

    private static class BooleanParser extends ParamParser
    {
        public BooleanParser(Field field){
            super(field);
        }

        void setParameter(Object obj, Deque<String> args) throws IllegalAccessException
        {
            mField.set(obj, Boolean.parseBoolean(args.poll()));
        }
    };

    
    private static class FloatParser extends ParamParser
    {
        public FloatParser(Field field){
            super(field);
        }

        void setParameter(Object obj, Deque<String> args) throws IllegalAccessException
        {
            mField.set(obj, Float.parseFloat(args.poll()));
        }
    };
    
    private static class StringParser extends ParamParser
    {
        public StringParser(Field field){
            super(field);
        }

        void setParameter(Object obj, Deque<String> args) throws IllegalAccessException
        {
            mField.set(obj, args.poll());
        }
    };

    private static class ListParser extends ParamParser
    {
        public ListParser(Field field){
            super(field);
        }

        void setParameter(Object obj, Deque<String> args) throws IllegalAccessException
        {
            try{
                Class<?> innerType = (Class<?>)((ParameterizedType)mField.getGenericType()).getActualTypeArguments()[0];
                List<Object> list = (List<Object>)mField.get(obj);
                if(list == null){
                    list = new ArrayList();
                    mField.set(obj, list);
                }
                //Check to see if this an object we have a map for
                Class<?> parserClass = msParserMap.get(innerType);

                if(parserClass != null){
                    class TempClass{
                        public Object var;
                    }
                    TempClass t = new TempClass();
                        ParamParser parser = (ParamParser)parserClass.getDeclaredConstructor(Field.class).newInstance(t.getClass().getField("var"));
                        parser.setParameter(t, args);
                    list.add(t.var);
                }else if (innerType.getAnnotation(XmlRootElement.class) != null){
                    //Time for a recursive call on the sub object
                    Object o = innerType.newInstance();
                    populateObjectFromCMDParams(o, args);
                    list.add(o);
                }else{
                    throw new RuntimeException("Can't parse an inner class of type " + innerType.getName());
                }
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    private static Map<Class<?>, Class<?>> msParserMap;

    //Populate the map of our parsers for each type
    static { 
        msParserMap = new HashMap<Class<?>, Class<?>>();
    
        msParserMap.put(short.class, IntParser.class);
        msParserMap.put(int.class, IntParser.class);
        msParserMap.put(long.class, IntParser.class);

        msParserMap.put(boolean.class, BooleanParser.class);
        
        msParserMap.put(float.class, FloatParser.class);
        msParserMap.put(double.class, FloatParser.class);
        
        msParserMap.put(String.class, StringParser.class);
        msParserMap.put(ArrayList.class, ListParser.class);
        msParserMap.put(List.class, ListParser.class);
    }
}
