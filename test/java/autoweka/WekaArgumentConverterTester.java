package autoweka;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import autoweka.WekaArgumentConverter.Arguments;

public class WekaArgumentConverterTester 
{
    
    @Test
    public void argumentConversionTest1() {
        List<String> args1 = Arrays.asList("-Key", "Value1", "-Key2", "Value2", "-Key3", "Value3", "-Key4", "Value4");
        Arguments result1 = WekaArgumentConverter.convert(args1);
        assertEquals(0, result1.argMap.get("attributeeval").size());
        assertEquals(0, result1.argMap.get("attributesearch").size());
        assertEquals(8, result1.argMap.get("classifier").size());
        assertTrue(result1.argMap.get("classifier").contains("-Key"));
        assertTrue(result1.argMap.get("classifier").contains("-Key2"));
        assertTrue(result1.argMap.get("classifier").contains("-Key3"));
        assertTrue(result1.argMap.get("classifier").contains("-Key4"));
        assertTrue(result1.argMap.get("classifier").contains("Value1"));
        assertTrue(result1.argMap.get("classifier").contains("Value2"));
        assertTrue(result1.argMap.get("classifier").contains("Value3"));
        assertTrue(result1.argMap.get("classifier").contains("Value4"));

        assertTrue(result1.propertyMap.isEmpty());
    }

    @Test
    public void argumentConversionTest2() {
        List<String> args2 = Arrays.asList("-attributeeval", "Value1", "-classifier", "Value2", "-targetclass", "Value3", "-attributesearch", "Value4");
        Arguments result2 = WekaArgumentConverter.convert(args2);
        assertEquals(0, result2.argMap.get("attributeeval").size());
        assertEquals(0, result2.argMap.get("attributesearch").size());
        assertEquals(2, result2.argMap.get("classifier").size());
        assertTrue(result2.argMap.get("classifier").contains("-classifier"));
        assertTrue(result2.argMap.get("classifier").contains("Value2"));

        assertEquals(3, result2.propertyMap.size());
        assertEquals("Value1", result2.propertyMap.get("attributeeval"));
        assertEquals("Value4", result2.propertyMap.get("attributesearch"));
        assertEquals("Value3", result2.propertyMap.get("targetclass"));
    }
        
    @Test
    public void argumentConversionTest3() {
        List<String> args3 = Arrays.asList("REMOVED", "Value1", "-attributeeval", "Value2", "REMOVED", "Value3", "-HIDDEN", "Value4", "-classifier",
                "Value5", "-Key", "Value6", "-assearch_key1", "Value7", "-assearch_key2", "REMOVE_PREV", "-aseval_key1", "Value9", "-aseval_key2", "Value10");
        Arguments result3 = WekaArgumentConverter.convert(args3);
        assertEquals(4, result3.argMap.get("attributeeval").size());
        assertEquals(2, result3.argMap.get("attributesearch").size());
        assertEquals(4, result3.argMap.get("classifier").size());
        assertTrue(result3.argMap.get("attributeeval").contains("-key1"));
        assertTrue(result3.argMap.get("attributeeval").contains("Value9"));
        assertTrue(result3.argMap.get("attributeeval").contains("-key2"));
        assertTrue(result3.argMap.get("attributeeval").contains("Value10"));
        assertTrue(result3.argMap.get("classifier").contains("-Key"));
        assertTrue(result3.argMap.get("classifier").contains("Value6"));
        assertTrue(result3.argMap.get("classifier").contains("-classifier"));
        assertTrue(result3.argMap.get("classifier").contains("Value5"));
        assertTrue(result3.argMap.get("attributesearch").contains("-key1"));
        assertTrue(result3.argMap.get("attributesearch").contains("Value7"));

        assertEquals(1, result3.propertyMap.size());
        assertEquals("Value2", result3.propertyMap.get("attributeeval"));
    }

    @Test
    public void argumentConversionTest4() {
        List<String> args4 = Arrays.asList("-targetclass", "targetclassValue", "-attributeeval", "attributeevalValue", "-attributesearch", "attributesearchValue",
                "-attributetime", "attributetimeValue", "-classifier", "classifier", "-LOG_Key1", "0.69", "-INT_Key2", "68.55", "-DASHDASHArgument", "SomeValuetoWaste",
                "-assearch_key1", "assearchValue1", "-assearch_key2", "assearchValue2", "-aseval_DASHDASHkey1", "Value1", "-aseval_key2", "Value2");
        Arguments result4 = WekaArgumentConverter.convert(args4);

        assertEquals(3, result4.argMap.get("attributeeval").size());
        assertEquals(4, result4.argMap.get("attributesearch").size());
        assertEquals(7, result4.argMap.get("classifier").size());
        assertTrue(result4.argMap.get("attributeeval").contains("--"));
        assertTrue(result4.argMap.get("attributeeval").contains("-key2"));
        assertTrue(result4.argMap.get("attributeeval").contains("Value2"));
        assertTrue(result4.argMap.get("classifier").contains("--"));
        assertTrue(result4.argMap.get("classifier").contains("-Key2"));
        assertTrue(result4.argMap.get("classifier").contains("69"));
        assertTrue(result4.argMap.get("classifier").contains("-Key1"));
        assertTrue(result4.argMap.get("classifier").contains("4.897788"));
        assertTrue(result4.argMap.get("classifier").contains("-classifier"));
        assertTrue(result4.argMap.get("classifier").contains("classifier"));
        assertTrue(result4.argMap.get("attributesearch").contains("-key1"));
        assertTrue(result4.argMap.get("attributesearch").contains("assearchValue1"));
        assertTrue(result4.argMap.get("attributesearch").contains("-key2"));
        assertTrue(result4.argMap.get("attributesearch").contains("assearchValue2"));

        assertEquals(4, result4.propertyMap.size());
        assertEquals("attributeevalValue", result4.propertyMap.get("attributeeval"));
        assertEquals("targetclassValue", result4.propertyMap.get("targetclass"));
        assertEquals("attributetimeValue", result4.propertyMap.get("attributetime"));
        assertEquals("attributesearchValue", result4.propertyMap.get("attributesearch"));
    }
}
