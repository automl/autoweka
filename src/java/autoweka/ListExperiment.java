package autoweka;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.io.InputStream;
import java.util.List;
import java.io.File;
import java.net.URLDecoder;

@XmlRootElement(name="listsexperiment")
@XmlAccessorType(XmlAccessType.NONE)
class ListExperiment extends XmlSerializable 
{
    @XmlElement(name="name")
    public String name;
    @XmlElement(name="resultMetric")
    public String resultMetric = "errorRate";
    @XmlElement(name="instanceGenerator")
    public String instanceGenerator;
    @XmlElement(name="instanceGeneratorArgs")
    public String instanceGeneratorArgs;
    @XmlElement(name="datasetString")
    public String datasetString;
    @XmlElement(name="trainTimeout")
    public float trainTimeout = -1;
    @XmlElement(name="memory")
    public String memory;
    @XmlElement(name="extraProps")
    public String extraPropsString;
    @XmlElement(name="trajectoryPointExtras")
    public List<Experiment.TrajectoryPointExtra> trajectoryPointExtras = new ArrayList<Experiment.TrajectoryPointExtra>();
    @XmlElement(name="argstrings")
    public ArrayList<String> argStrings = new ArrayList<String>();
    @XmlElement(name="seed")
    public String seed = "0";

    public ListExperiment()
    {}

    public static ListExperiment fromXML(String filename)
    {
        return XmlSerializable.fromXML(filename, ListExperiment.class);
    }
    public static ListExperiment fromXML(InputStream xml)
    {
        return XmlSerializable.fromXML(xml, ListExperiment.class);
    }

    public void validate(){
        if(this.name == null)
            throw new RuntimeException("No experiment -name was defined!");
        if(this.trainTimeout < 0 )
            throw new RuntimeException("Need a -trainTimeout > 0!");
        if(this.datasetString == null)
            throw new RuntimeException("Need an -datasetString!");
        if(this.instanceGenerator == null)
            throw new RuntimeException("Need an -instanceGenerator");
    }

    public static void main(String[] args)
    {
        int target = -1;
        boolean perInstance = false;
        String listExperimentName = null;
        File experimentDir = null;
        ListExperiment experiment = null;
        String outputFileName = null;
        for(int i = 0; i < args.length; i++)
        {
            if(args[i].equals("-target"))
            {
                target = Integer.parseInt(args[++i]);
            }
            else if(args[i].equals("-perInstance"))
            {
                perInstance = true;
                throw new RuntimeException("Per instance batching is not implemented yet");
            }
            else if(args[i].startsWith("-"))
            {
                throw new RuntimeException("Unknown arg: " + args[i]);
            }
            else
            {
                if(experiment != null)
                    throw new RuntimeException("Only one ListExperiment can be specified at a time");
                experimentDir = new File(args[i]).getAbsoluteFile();
                experiment = ListExperiment.fromXML(URLDecoder.decode(experimentDir.getAbsolutePath()) + File.separator + experimentDir.getName() + ".listexperiment");
            }
        }

        //Get a list of all the instance that we are going to be operating on
        ArrayList<String> instanceStrings = new ArrayList<String>();
        instanceStrings.add("default");
        for(String s: InstanceGenerator.create(experiment.instanceGenerator, "__dummy__").getAllInstanceStrings(experiment.instanceGeneratorArgs))
            instanceStrings.add(s);
        outputFileName = URLDecoder.decode(experimentDir.getAbsolutePath()) + File.separator + experimentDir.getName() + ".listresults";

        ArrayList<String> argStrings = new ArrayList<String>(experiment.argStrings);

        //Have we been given a specific target run?
        if(target != -1)
        {
            listExperimentName = listExperimentName + "." + target;
            String argString = argStrings.get(target);
            argStrings = new ArrayList<String>();
            argStrings.add(argString);
            //instanceStrings = new String[]{instanceStrings[target]};
            outputFileName += "." + target;
        }
        ListResultGroup resGroup = new ListResultGroup(experiment);

        //For every trajectory, we need to compute the best score
        for(String argString: argStrings)
        {
            ListResultGroup.ListResult res = new ListResultGroup.ListResult(argString);
            resGroup.results.add(res);
            for(String instanceString: instanceStrings)
            {
                //We need to run this instance
                res.results.add(new ListResultGroup.ListResult.InstanceResult(instanceString, SubProcessWrapper.getErrorAndTime(experimentDir, experiment, instanceString, argString, experiment.seed)));
            }
        }

        resGroup.toXML(outputFileName);
    }
};
