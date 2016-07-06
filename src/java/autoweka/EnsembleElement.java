package autoweka;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EnsembleElement{

	final static Logger log = LoggerFactory.getLogger(EnsembleElement.class);

	private Configuration mModel;
	private double mWeight;

	private int [] mPredictions;
	private int [] mFoldSizes;
	private int mAmtInstances;

	public EnsembleElement(Configuration model, int amtInstances, int[] foldSizes, double weight){
		this(model,amtInstances,foldSizes);
		mWeight = weight;
	}

	public EnsembleElement(Configuration model, int amtInstances, int[] foldSizes){
		mModel = model;
		mFoldSizes = foldSizes;
		mAmtInstances = amtInstances;
		mPredictions = new int[amtInstances];
		mWeight=1;
	}

	public void parseInstancewiseInfo(String iwpPath) throws FileNotFoundException,IOException{ //TODO get instancewiseLogPath from global, maybe have foldSizes globally too
		//TODO change the way iwpPath is provided

		//Iterating over folds
		int totalInstanceIndex=0;
		for(int i = 0; i<mFoldSizes.length;i++){ //iterating over instancewise logs for each fold
			String path = iwpPath+"hash:"+mModel.hashCode()+"_fold:"+i+".txt";
			File ciFile = new File(path);

			FileReader 	   ciFR = null;
			BufferedReader ciBR = null ;

			if(ciFile.exists()){
				try{
					ciFR = new FileReader(path);
					ciBR = new BufferedReader(ciFR); //hue
				}catch (FileNotFoundException e){
					System.out.println("Couldn't initialize ciBR");
					throw e;
				}

				//Iterating over lines
				try{
						ciBR.readLine(); //skipping first line of csv file
					for( String currentLine = ciBR.readLine() ; currentLine!=null ; currentLine = ciBR.readLine()){ //iterating over lines
						mPredictions[totalInstanceIndex]= Integer.parseInt(Util.parseInstancewiseLine(currentLine,"PREDICT_CODE"));
						//			System.out.println("@prediction for hash "+mModel.hashCode()+" on instance "+totalInstanceIndex+" is "+mPredictions[totalInstanceIndex]);
						totalInstanceIndex++;
					}
				}catch(IOException e){
					log.debug("Couldn't read a line in file "+path);
					throw e;
				}

			}else{ //TODO define standard behaviour for this case
				for(int j=0;j<mFoldSizes[i];j++,totalInstanceIndex++)	mPredictions[totalInstanceIndex]=-1;
			}
		}
	}

	public String toString()                  {  return mModel.getArgStrings();}
	public int hashCode()                     {	return mModel.hashCode();}

	public int getPrediction(int instanceNum) {  return mPredictions[instanceNum];}
	public Configuration getModel()           {	return mModel;}

}
