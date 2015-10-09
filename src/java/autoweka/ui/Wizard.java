package autoweka.ui;

import java.io.File;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextField;

import org.javabuilders.BuildResult;
import org.javabuilders.swing.SwingJavaBuilder;

import weka.core.Attribute;

import autoweka.Experiment;
import autoweka.ExperimentConstructor;
import autoweka.InstanceGenerator;
import autoweka.Util;

class Wizard extends JFrame
{
    private BuildResult mResult;
    
    private JFileChooser mFileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
    private JTextField mExperimentNameText;
    private JTextField mTrainingArffText;
    private JComboBox mTimingPresetsCombo;
    private static String msExperimentPath = "wizardexperiments" + File.separator;
    private TrainedModelRunner mTrainedModelRunner;
    private ExperimentRunner mRunner;

    public static void main(String[] args){
        Wizard w = new Wizard();
        w.setVisible(true);
    }

    public Wizard()
    {
        mResult = SwingJavaBuilder.build(this);

        mTimingPresetsCombo.setModel(new DefaultComboBoxModel());
        mTimingPresetsCombo.addItem(new Preset(0.5f, 1, 1));
        mTimingPresetsCombo.addItem(new Preset(1, 2, 1));
        mTimingPresetsCombo.addItem(new Preset(6, 15, 3));
        //mTimingPresetsCombo.addItem(new Preset(0.02f, 1, 1));
        mTimingPresetsCombo.addItem(new Preset(24, 60, 10));
        mTimingPresetsCombo.addItem(new Preset(72, 90, 15));
       
        mTimingPresetsCombo.setSelectedIndex(3);

        mFileChooser.setFileFilter(UIUtil.msArffFileFilter);
    }
    
    public void openTrainingFile()
    {
        mFileChooser.setDialogTitle("Training ARFF");
        mFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        UIUtil.runFileChooser(mFileChooser, this, mTrainingArffText, "lastArffLocation"); 
    }

    public void runExperiment()
    {
        //First, we need to build the experiment
        if(buildExperiment()){
            this.dispose();

            mRunner = new ExperimentRunner();
            mRunner.setFolderSeedAndObserver(msExperimentPath + mExperimentNameText.getText(), "0", new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    mRunner.dispose();
                    mTrainedModelRunner.setVisible(true);
                    mTrainedModelRunner.openExperiment(msExperimentPath + mExperimentNameText.getText());
                }
            });
            mRunner.setVisible(true);
            mTrainedModelRunner = new TrainedModelRunner();
            mRunner.runClicked();
        }
    }

    public boolean buildExperiment()
    {
        try
        {
            //Our constants
            Properties props = Util.parsePropertyString("type=trainTestArff:testArff=__dummy__");
            props.setProperty("trainArff", mTrainingArffText.getText());
            String datasetString = Util.propertiesToString(props);
            String instanceGeneratorType = "autoweka.instancegenerators.CrossValidation";
            String instanceGeneratorArgs = "numFolds=10";
            
            //Populate the experiment fields
            Experiment exp = new Experiment();
            exp.name = mExperimentNameText.getText();

            InstanceGenerator instanceGenerator = InstanceGenerator.create(instanceGeneratorType, datasetString);
            instanceGenerator.getAllInstanceStrings(instanceGeneratorArgs);

            //Which result metric do we use?
            Attribute classAttr = instanceGenerator.getTraining().classAttribute();
            if(classAttr.isNominal()){
                exp.resultMetric = "errorRate"; 
            }else if(classAttr.isNumeric()) {
                exp.resultMetric = "rmse"; 
            }

            exp.instanceGenerator = instanceGeneratorType;
            exp.instanceGeneratorArgs = instanceGeneratorArgs;
            exp.datasetString = datasetString;
            exp.attributeSelection = true;


            Preset timingPreset = (Preset)mTimingPresetsCombo.getSelectedItem();

            exp.attributeSelectionTimeout = timingPreset.attributeTime * 60;
            exp.tunerTimeout = timingPreset.tunerTime * 3600;
            exp.trainTimeout = timingPreset.trainingTime * 60;

            exp.memory = "500m";
            exp.extraPropsString = "initialIncumbent=RANDOM:acq-func=EI";

            //Setup all the extra args
            LinkedList<String> args = new LinkedList<String>();
            args.add("-experimentpath");
            args.add(msExperimentPath);

            //Make the thing
            ExperimentConstructor.buildSingle("autoweka.smac.SMACExperimentConstructor", exp, args);

        }catch(Exception e) {
            UIUtil.showExceptionDialog(this, "Failed to create experiment", e);
            return false;
        }
        return true;
    }

    private static class Preset
    {
        public float tunerTime;
        public float trainingTime;
        public float attributeTime;

        private static NumberFormat msNumberFormat = new DecimalFormat("##.#");

        public Preset(float _tunerTime, float _trainingTime, float _attributeTime)
        {
            tunerTime = _tunerTime;
            trainingTime = _trainingTime;
            attributeTime = _attributeTime;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(msNumberFormat.format(tunerTime));
            sb.append(" hours total optimising (at most ");
            sb.append(msNumberFormat.format(trainingTime));
            sb.append(" minutes of training a model and ");
            sb.append(msNumberFormat.format(attributeTime));
            sb.append(" minutes selecting features)");
            return sb.toString();
        }


    }
}
