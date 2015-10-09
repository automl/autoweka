package autoweka.tools;

import weka.core.Attribute;
import weka.core.Instances;

import autoweka.InstanceGenerator;


class DatasetStats
{
    public static void main(String[] args)
    {
        //Go through each arg and check if it's a zip
        for(String zip : args){
            System.out.println("Dataset: " + zip);
            InstanceGenerator  generator;
            try{
                generator = InstanceGenerator.create("autoweka.instancegenerators.Default", "type=zipFile:zipFile=" + zip);
            }catch(Exception e){
                e.printStackTrace();
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

            System.out.println(" Num Training: " + training.size());
            System.out.println(" Num Testing:  " + testing.size());
            System.out.println("   Num Numeric:  " + numNumeric);
            System.out.println("   Num Nominal:  " + numNominal);
            System.out.println("   Num Date:     " + numDate);
            System.out.println("   Num String:   " + numString);
        }
    }
}
