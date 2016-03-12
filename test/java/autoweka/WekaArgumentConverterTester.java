package autoweka;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import autoweka.WekaArgumentConverter.Arguments;

public class WekaArgumentConverterTester 
{
    
    @Test
    public void convertTester(){
        
        List<String> args1 = Arrays.asList("-Key", "Value1", "-Key2", "Value2", "-Key3", "Value3", "-Key4", "Value4");
        Arguments Result1 = WekaArgumentConverter.convert(args1);
        System.out.println("Test # 1 : Testing simpe key value arguments");
        assertTrue(Result1.argMap.toString().equals("{attributeeval=[], classifier=[-Key, Value1, -Key2, Value2, -Key3, Value3, -Key4, Value4], attributesearch=[]}"));
        assertTrue(Result1.propertyMap.toString().equals("{}"));
        List<String> args2 = Arrays.asList("-attributeeval", "Value1", "-classifier", "Value2", "-targetclass", "Value3", "-attributesearch", "Value4");
        Arguments Result2 = WekaArgumentConverter.convert(args2);
        System.out.println("Test # 2: keywords specific arguments");
        assertTrue(Result2.argMap.toString().equals("{attributeeval=[], classifier=[-classifier, Value2], attributesearch=[]}"));
        assertTrue(Result2.propertyMap.toString().equals("{attributeeval=Value1, targetclass=Value3, attributesearch=Value4}"));
        
        List<String> args3 = Arrays.asList("REMOVED", "Value1", "-attributeeval", "Value2", "REMOVED", "Value3", "-HIDDEN", "Value4", "-classifier",
                "Value5", "-Key", "Value6", "-assearch_key1", "Value7", "-assearch_key2", "REMOVE_PREV", "-aseval_key1", "Value9", "-aseval_key2", "Value10");
        Arguments Result3 = WekaArgumentConverter.convert(args3);
        System.out.println("Test # 3: ignored arguments");
        assertTrue(Result3.argMap.toString().equals("{attributeeval=[-key1, Value9, -key2, Value10], classifier=[-Key, Value6, -classifier, Value5], attributesearch=[-key1, Value7]}"));
        assertTrue(Result3.propertyMap.toString().equals("{attributeeval=Value2}"));
        List<String> args4 = Arrays.asList("-targetclass", "targetclassValue", "-attributeeval", "attributeevalValue", "-attributesearch", "attributesearchValue",
                "-attributetime", "attributetimeValue", "-classifier", "classifier", "-LOG_Key1", "0.69", "-INT_Key2", "68.55", "-DASHDASHArgument", "SomeValuetoWaste",
                "-assearch_key1", "assearchValue1", "-assearch_key2", "assearchValue2", "-aseval_DASHDASHkey1", "Value1", "-aseval_key2", "Value2");
        Arguments Result4 = WekaArgumentConverter.convert(args4);
        System.out.println("Test # 4: processing all arguments");
        assertTrue(Result4.argMap.toString().equals("{attributeeval=[--, -key2, Value2], classifier=[--, -Key2, 69, -Key1, 4.897788, -classifier, classifier], attributesearch=[-key1, assearchValue1, -key2, assearchValue2]}"));
        assertTrue(Result4.propertyMap.toString().equals("{attributeeval=attributeevalValue, targetclass=targetclassValue, attributetime=attributetimeValue, attributesearch=attributesearchValue}"));       
    }

    public static void main(String[] args){
        WekaArgumentConverterTester t = new WekaArgumentConverterTester();
        t.convertTester();
    }
}
