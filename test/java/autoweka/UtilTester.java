package autoweka;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.junit.Ignore;

import weka.core.Instances;
import static org.junit.Assert.*;

public class UtilTester 
{
    //@Ignore
    @Test
    public void playpen(){
        Properties props = new Properties();
        props.setProperty("prop", "value=something:else=foo");
        props.setProperty("bar", "foo");
        System.out.println(Util.propertiesToString(props));
        props = Util.parsePropertyString("prop=value=something\\:else=foo");
        //props = Util.parsePropertyString(Util.propertiesToString(props));
        System.out.println("Result " + props.getProperty("prop"));
    }

    @Test
    public void splitTest(){
        for(String s : Util.splitQuotedString(" This is \"my split\" string\" \"with lotsC:\\foath\\fishof\\ o\' fish \\\"and even escaped\\\" ")){
            System.out.println(s + ",");
        }
    }

    @Test
    public void expandPathTest(){
        System.out.println(Util.expandPath(".\\"));
    }

    public static void main(String[] args){
        UtilTester t = new UtilTester();
        t.playpen();
    }
}
