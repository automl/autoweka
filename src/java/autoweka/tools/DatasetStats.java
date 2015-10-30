package autoweka.tools;

import weka.core.Attribute;
import weka.core.Instances;

import autoweka.InstanceGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DatasetStats
{
    final static Logger log = LoggerFactory.getLogger(DatasetStats.class);

    public static void main(String[] args)
    {
        //Go through each arg and check if it's a zip
        for(String zip : args){
            log.info("Dataset: {}", zip);
            InstanceGenerator  generator;
            try{
                generator = InstanceGenerator.create("autoweka.instancegenerators.Default", "type=zipFile:zipFile=" + zip);
            }catch(Exception e){
                log.error(e.getMessage(), e);
                continue;
            }

            //Get some stats about the types of stuff in it
            Instances training = generator.getTraining();
            Instances testing = generator.getTesting();
            int numString = 0;
            int numDate = 0;
            int numNominal = 0;
            int numNumeric = 0;
            for(int i = 0; i < training.numAttributes(); i++){
                if(i == training.classIndex())
                    continue;

                Attribute at = training.attribute(i);
                switch(at.type()){
                    case Attribute.NUMERIC:
                        numNumeric++;
                        break;
                    case Attribute.NOMINAL:
                        numNominal++;
                        break;
                    case Attribute.DATE:
                        numDate++;
                        break;
                    case Attribute.STRING:
                        numString++;
                        break;
                    default:
                        throw new RuntimeException("Invalid attribute type '" + at.type() + "'");
                }
            }

            log.info(" Num Training: {}", training.size());
            log.info(" Num Testing:  {}", testing.size());
            log.info("   Num Numeric:  {}", numNumeric);
            log.info("   Num Nominal:  {}", numNominal);
            log.info("   Num Date:     {}", numDate);
            log.info("   Num String:   {}", numString);
        }
    }
}
