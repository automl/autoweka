package autoweka;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.io.InputStream;

@XmlRootElement(name="listresultgroup")
@XmlAccessorType(XmlAccessType.NONE)
class ListResultGroup extends XmlSerializable
{
    @XmlElement(name="experiment")
    public ListExperiment experiment;
    @XmlElement(name="results")
    public ArrayList<ListResult> results = new ArrayList<ListResult>();

    public ListResultGroup(){}
    public ListResultGroup(ListExperiment _experiment)
    {
        this.experiment = _experiment;
    }

    @XmlRootElement(name="listresult")
    @XmlAccessorType(XmlAccessType.NONE)
    static class ListResult
    {
        @XmlElement(name="argstring")
        public String argString = "";
        @XmlElement(name="results")
        public ArrayList<InstanceResult> results = new ArrayList<InstanceResult>();

        public ListResult(){}
        public ListResult(String _argString)
        {
            argString = _argString;
        }

        @XmlRootElement(name="result")
        @XmlAccessorType(XmlAccessType.NONE)
        static class InstanceResult
        {
            @XmlElement(name="instance")
            public String instance;
            @XmlElement(name="error")
            double error;
            @XmlElement(name="time")
            double time;

            public InstanceResult(){}
            public InstanceResult(String _instance, double _error)
            {
                instance = _instance;
                error = _error;
                time = 0;
            }
            public InstanceResult(String _instance, SubProcessWrapper.ErrorAndTime _errAndTime)
            {
                instance = _instance;
                error = _errAndTime.error;
                time = _errAndTime.time;
            }
        };
    };

    public static ListResultGroup fromXML(String filename)
    {
        return ListResultGroup.fromXML(filename, ListResultGroup.class);
    }
    public static ListResultGroup fromXML(InputStream xml)
    {
        return ListResultGroup.fromXML(xml, ListResultGroup.class);
    }
};
