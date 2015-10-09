package autoweka.randomsearch;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import autoweka.SubProcessWrapper;
import autoweka.Util;
import autoweka.XmlSerializable;

//@XmlElement(name="experiment")
@XmlRootElement(name="randomSearchResult")
@XmlAccessorType(XmlAccessType.NONE)
class RandomSearchResult extends XmlSerializable
{
    @XmlElement(name="argstring")
    public final String argString;
    public final String argHash;
    @XmlElement(name="instanceResult")
    public ArrayList<InstanceResult> results = new ArrayList<InstanceResult>();
    
    private static final String msOutputPrefix = File.separator + "points" + File.separator;
    private static final int msHashFolderPrefixLength = 2;

    protected RandomSearchResult() {
        argString = null;
        argHash = null;
    }

    public RandomSearchResult(String _argString)
    {
        argString = _argString;

        //Generate the hash
        try{
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(argString.getBytes("utf8"));
            argHash = new BigInteger(1, crypt.digest()).toString(16);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void addInstanceResult(String instance, SubProcessWrapper.ErrorAndTime errAndTime)
    {
        results.add(new InstanceResult(instance, errAndTime));
    }

    @XmlRootElement(name="instanceResult")
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

    private File getOutputFile(File outputDir)
    {
        return new File(outputDir.getAbsolutePath() + msOutputPrefix + argHash.substring(0, msHashFolderPrefixLength) + File.separator + argHash.substring(msHashFolderPrefixLength) + ".result");
    }

    public boolean resultExists(File outputDir)
    {
        return getOutputFile(outputDir).exists();
    }

    public void touchResultFile(File outputDir)
    {
        File file = getOutputFile(outputDir);
        Util.makePath(file.getParent());
        try {
            if (!file.exists())
                new FileOutputStream(file).close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveResultFile(File outputDir)
    {
       this.toXML(getOutputFile(outputDir).getAbsolutePath()); 
    }
};
