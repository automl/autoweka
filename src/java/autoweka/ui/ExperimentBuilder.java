package autoweka.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.javabuilders.BuildResult;
import org.javabuilders.swing.SwingJavaBuilder;

import weka.core.Attribute;
import weka.core.Instances;

import autoweka.ApplicabilityTester;
import autoweka.ClassParams;
import autoweka.Experiment;
import autoweka.ExperimentBatch;
import autoweka.ExperimentConstructor;
import autoweka.InstanceGenerator;
import autoweka.Util;

public class ExperimentBuilder extends JFrame
{
    private BuildResult mResult;

    private JTabbedPane mTabs;
    private JFileChooser mFileChooser = new JFileChooser(new File(System.getProperty("user.dir")));

    private JPanel mDatasetSelection;
    private JTextField mTrainingArffText;
    private JTextField mTestingArffText;
    private JComboBox mInstanceGeneratorCombo;
    private InstanceGenerator mInstanceGenerator;

    private JPanel mClassifierSelection;
    private JList mClassifierList;
    private Vector<String> mClassifiers = new Vector<String>();
    private ArrayList<String> mAllowedClassifiers = new ArrayList<String>();

    private JPanel mExperimentSettings;
    private JTextField mExperimentNameText;
    private JTextField mExperimentDirText;
    private JComboBox mResultMetricCombo;
    private JComboBox mOptimisationMethodCombo;
    private JTextField mOptimisationTimeoutText;
    private JTextField mClassifierMemoryLimitText;
    private JTextField mClassifierTimeoutText;
    private JCheckBox mAttributeSelectionCheckBox;
    private JTextField mAttributeSelectionTimeoutText;
    
    public static void main(String[] args){
        ExperimentBuilder eb = new ExperimentBuilder();
        eb.setVisible(true);
    }

    public ExperimentBuilder()
    {
        mResult = SwingJavaBuilder.build(this);
        
        //Populate all the instance generators;
        mInstanceGeneratorCombo.setModel(new DefaultComboBoxModel());
        mInstanceGeneratorCombo.addItem(new autoweka.ui.instancegenerators.CrossValidation(this));
        mInstanceGeneratorCombo.addItem(new autoweka.ui.instancegenerators.RandomSubSampling(this));
        mInstanceGeneratorCombo.addItem(new autoweka.ui.instancegenerators.Default(this));

        //Add in all the experiment types
        mOptimisationMethodCombo.setModel(new DefaultComboBoxModel());
        mOptimisationMethodCombo.addItem(new autoweka.ui.experimentconstructors.SMAC(this));
        mOptimisationMethodCombo.addItem(new autoweka.ui.experimentconstructors.TPE(this));

        activateDatasetSelection();
    }
    
    //-------------------------------------------------------------------------
    //-------------------------------------------------------------------------
    //-------------------------------------------------------------------------

    public void openTrainingFile()
    {
        mFileChooser.setDialogTitle("Training ARFF");
        mFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        mFileChooser.setFileFilter(UIUtil.msArffFileFilter);

        UIUtil.runFileChooser(mFileChooser, this, mTrainingArffText, "lastArffLocation"); 
    }
    
    public void openTestingFile()
    {
        mFileChooser.setDialogTitle("Testing ARFF");
        mFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        mFileChooser.setFileFilter(UIUtil.msArffFileFilter);

        UIUtil.runFileChooser(mFileChooser, this, mTestingArffText, "lastArffLocation"); 
    }

    public void activateDatasetSelection()
    {
        mTabs.setSelectedComponent(mDatasetSelection);
        
        //Add in all the instance generators that we know about - this sould probably be done with reflection or something
    }

    public void editInstanceGenerator()
    {
        ((PropertyPanel)mInstanceGeneratorCombo.getSelectedItem()).setVisible(true);
    }

    public void nextDatasetSelection()
    {
        try
        {
            Properties props = new Properties();
            props.setProperty("type", "trainTestArff");
            props.setProperty("trainArff", mTrainingArffText.getText());
            props.setProperty("testArff", "__dummy__");
            if(!mTestingArffText.getText().isEmpty())
                props.setProperty("testArff", mTestingArffText.getText());

            PropertyPanel generatorProps = ((PropertyPanel)mInstanceGeneratorCombo.getSelectedItem());

            //Get out instance generator
            mInstanceGenerator = InstanceGenerator.create(generatorProps.getClassName(), Util.propertiesToString(props));
            mInstanceGenerator.getAllInstanceStrings(Util.propertiesToString(generatorProps.getProperties()));

            activateClassifierSelection();
        }catch(Throwable e){
            UIUtil.showExceptionDialog(this, "Error setting dataset", e);
        }
    }

    //-------------------------------------------------------------------------
    //-------------------------------------------------------------------------
    //-------------------------------------------------------------------------
    
    public void activateClassifierSelection()
    {
        mTabs.setSelectedComponent(mClassifierSelection);
        
        Instances instances = mInstanceGenerator.getTraining();
        String paramDir = Util.getAutoWekaDistributionPath() + File.separator + "params" + File.separator;

        ApplicabilityTester.ApplicableClassifiers app = ApplicabilityTester.getApplicableClassifiers(instances, paramDir, null);

        for(ClassParams classifier : app.base) {
            mClassifiers.add(classifier.getTargetClass());
        }
        for(ClassParams classifier : app.meta) {
            mClassifiers.add(classifier.getTargetClass());
        }
        for(ClassParams classifier : app.ensemble) {
            mClassifiers.add(classifier.getTargetClass());
        }
        
        mClassifierList.setListData(mClassifiers);
        int[] selectedIndicies = new int[mClassifiers.size()];
        for(int i = 0; i < selectedIndicies.length; i++)
            selectedIndicies[i] = i;

        mClassifierList.setSelectedIndices(selectedIndicies);
    }

    public void backClassifierSelection()
    {
        activateDatasetSelection();
    }

    public void nextClassifierSelection()
    {
        mAllowedClassifiers.clear();
        for(int i : mClassifierList.getSelectedIndices()){
            mAllowedClassifiers.add(mClassifiers.get(i)); 
        }

        activateExperimentSettings();
    }


    //-------------------------------------------------------------------------
    //-------------------------------------------------------------------------
    //-------------------------------------------------------------------------

    public void activateExperimentSettings()
    {
        //Set the result metrics
        mResultMetricCombo.setModel(new DefaultComboBoxModel());
        Attribute classAttr = mInstanceGenerator.getTraining().classAttribute();
        if(classAttr.isNominal()){
            mResultMetricCombo.addItem(new StringComboOption("Error Rate (Classification)", "errorRate"));
        }else if(classAttr.isNumeric()) {
            mResultMetricCombo.addItem(new StringComboOption("Root Mean Squared Error (Regression)", "rmse"));
            mResultMetricCombo.addItem(new StringComboOption("Root Relative Squared Error (Regression)", "rrse"));
            mResultMetricCombo.addItem(new StringComboOption("Mean Absolute Error (Regression)", "meanAbsoluteErrorMetric"));
            mResultMetricCombo.addItem(new StringComboOption("Relative Absolute Error (Regression)", "relativeAbsoluteErrorMetric"));
        }


        mTabs.setSelectedComponent(mExperimentSettings);
    }

    public void backExperiment()
    {
        activateClassifierSelection();
    }

    public void openExperimentDir()
    {
        mFileChooser.setDialogTitle("Experiment Folder");
        mFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        mFileChooser.removeChoosableFileFilter(UIUtil.msArffFileFilter);
        UIUtil.runFileChooser(mFileChooser, this, mExperimentDirText, "lastExperimentDirLocation"); 
    }

    public void editOptimisationMethod()
    {
        ((PropertyPanel)mOptimisationMethodCombo.getSelectedItem()).setVisible(true);
    }

    public void validateNonNegativeFloat(String fieldName, JTextField text)
    {
        try{
            int val = Integer.parseInt(text.getText());
            if(val <= 0)
                throw new RuntimeException("must be greater than 0");
        }catch(Exception e){
            throw new RuntimeException("Error detected in " + fieldName + "", e);
        }
    }

    public void saveExperiment()
    {
        //Validate the crap out of everything
        try
        {
            if(mExperimentNameText.getText().isEmpty())
                throw new RuntimeException("No experiment name defined");
            if(mExperimentDirText.getText().isEmpty())
                throw new RuntimeException("No output folder defined");
            validateNonNegativeFloat("optimisation timeout", mOptimisationTimeoutText);
            validateNonNegativeFloat("classifier memory limit", mClassifierMemoryLimitText);
            validateNonNegativeFloat("classifier run timeout", mClassifierTimeoutText);
            validateNonNegativeFloat("attribute selection timeout", mAttributeSelectionTimeoutText);
        }catch(Exception e){
            UIUtil.showExceptionDialog(this, "Error in Experiment Settings", e);
            return;
        }

        //If the experiment that we're about to construct exists, throw up a warning
        File outputFolder = new File(mExperimentDirText.getText() + File.separator + mExperimentNameText.getText());
        if(outputFolder.exists())
        {
            if(JOptionPane.showConfirmDialog(this, "Warning: The experiment " + mExperimentNameText.getText() + " already exists. Do you wish to overwrite it?", "Experiment Exists", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
                writeExperimentFile();
            }
        }
        else
        {
            writeExperimentFile();
        }
    }

    public void writeExperimentFile()
    {
        PropertyPanel instanceGenProps = (PropertyPanel)mInstanceGeneratorCombo.getSelectedItem();
        PropertyPanel optMethodProps = (PropertyPanel)mOptimisationMethodCombo.getSelectedItem();

        //Populate the experiment fields
        Experiment exp = new Experiment();
        exp.name = mExperimentNameText.getText();
        exp.resultMetric = ((StringComboOption)mResultMetricCombo.getSelectedItem()).getData();
        exp.instanceGenerator = instanceGenProps.getClassName(); 
        exp.instanceGeneratorArgs = Util.propertiesToString(instanceGenProps.getProperties());

        Properties datasetProps = new Properties();
        datasetProps.setProperty("type", "trainTestArff");
        datasetProps.setProperty("trainArff", mTrainingArffText.getText());
        datasetProps.setProperty("testArff", "__dummy__");
        if(!mTestingArffText.getText().isEmpty())
            datasetProps.setProperty("testArff", mTestingArffText.getText());
        
        exp.datasetString = Util.propertiesToString(datasetProps);
        exp.attributeSelection = mAttributeSelectionCheckBox.isSelected();
        if(exp.attributeSelection)
            exp.attributeSelectionTimeout = Float.parseFloat(mAttributeSelectionTimeoutText.getText()) * 60;

        exp.tunerTimeout = Float.parseFloat(mOptimisationTimeoutText.getText()) * 3600;
        exp.trainTimeout = Float.parseFloat(mClassifierTimeoutText.getText()) * 60;
        exp.memory = mClassifierMemoryLimitText.getText() + "m";
        exp.extraPropsString = Util.propertiesToString(optMethodProps.getProperties());
        exp.allowedClassifiers = new ArrayList<String>(mAllowedClassifiers);

        //Setup all the extra args
        LinkedList<String> args = new LinkedList<String>();
        args.add("-experimentpath");
        args.add(mExperimentDirText.getText());
        args.add("-propertyoverride");
        args.add(Util.propertiesToString(optMethodProps.getGlobalProperties()));

        //Make the thing
        try
        {
            ExperimentConstructor.buildSingle(optMethodProps.getClassName(), exp, args);
        }catch(Exception e){
            UIUtil.showExceptionDialog(this, "Failed to write experiment", e);
            return;
        }

        JOptionPane.showMessageDialog(this, "Experiment Created!\nReturn to the launcher and use the 'Experiment Runner' to begin your experiment", "Experiment Saved", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void saveExperimentBatch()
    {
        //Validate the crap out of everything
        try
        {
            if(mExperimentNameText.getText().isEmpty())
                throw new RuntimeException("No experiment name defined");
            if(mExperimentDirText.getText().isEmpty())
                throw new RuntimeException("No output folder defined");
            validateNonNegativeFloat("optimisation timeout", mOptimisationTimeoutText);
            validateNonNegativeFloat("classifier memory limit", mClassifierMemoryLimitText);
            validateNonNegativeFloat("classifier run timeout", mClassifierTimeoutText);
            validateNonNegativeFloat("attribute selection timeout", mAttributeSelectionTimeoutText);
        }catch(Exception e){
            UIUtil.showExceptionDialog(this, "Error in Experiment Settings", e);
            return;
        }

        //If the experiment that we're about to construct exists, throw up a warning
        File outputFolder = new File(mExperimentDirText.getText() + File.separator + mExperimentNameText.getText() + ".batch");
        if(outputFolder.exists())
        {
            if(JOptionPane.showConfirmDialog(this, "Warning: The experiment batch " + mExperimentNameText.getText() + ".batch already exists. Do you wish to overwrite it?", "Experiment Exists", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
                writeExperimentBatchFile();
            }
        }
        else
        {
            writeExperimentBatchFile();
        }
    }

    public void writeExperimentBatchFile()
    {
        try{
            ExperimentBatch batch = new ExperimentBatch();

            //Insert the dataset component
            ExperimentBatch.DatasetComponent datasetComp = new ExperimentBatch.DatasetComponent();
            datasetComp.name = "DefaultData";
            String testingArff = mTrainingArffText.getText();
            if(!mTestingArffText.getText().isEmpty())
                testingArff = mTestingArffText.getText();
            datasetComp.setTrainTestArff( mTrainingArffText.getText(), testingArff);
            batch.mDatasets.add(datasetComp);

            //Insert the experiment component
            PropertyPanel instanceGenProps = (PropertyPanel)mInstanceGeneratorCombo.getSelectedItem();
            PropertyPanel optMethodProps = (PropertyPanel)mOptimisationMethodCombo.getSelectedItem();

            ExperimentBatch.ExperimentComponent expComp = new ExperimentBatch.ExperimentComponent();
            batch.mExperiments.add(expComp);

            expComp.name = mExperimentNameText.getText();
            expComp.resultMetric = ((StringComboOption)mResultMetricCombo.getSelectedItem()).getData();
            expComp.instanceGenerator = instanceGenProps.getClassName(); 
            expComp.instanceGeneratorArgs = Util.propertiesToString(instanceGenProps.getProperties());

            expComp.attributeSelection = mAttributeSelectionCheckBox.isSelected();
            if(expComp.attributeSelection)
                expComp.attributeSelectionTimeout = Float.parseFloat(mAttributeSelectionTimeoutText.getText()) * 60;

            expComp.tunerTimeout = Float.parseFloat(mOptimisationTimeoutText.getText()) * 3600;
            expComp.trainTimeout = Float.parseFloat(mClassifierTimeoutText.getText()) * 60;
            expComp.memory = mClassifierMemoryLimitText.getText() + "m";
            expComp.extraProps = Util.propertiesToString(optMethodProps.getProperties());
            expComp.allowedClassifiers = new ArrayList<String>(mAllowedClassifiers);

            expComp.constructor = optMethodProps.getClassName();

            //Setup all the extra args
            expComp.constructorArgs.add("-experimentpath");
            expComp.constructorArgs.add(mExperimentDirText.getText());
            expComp.constructorArgs.add("-propertyoverride");
            expComp.constructorArgs.add(Util.propertiesToString(optMethodProps.getGlobalProperties()));

            Util.makePath(mExperimentDirText.getText());
            batch.toXML(mExperimentDirText.getText() + File.separator + mExperimentNameText.getText() + ".batch");
        }catch(Exception e){
            e.printStackTrace();
            UIUtil.showExceptionDialog(this, "Failed to write experiment batch", e);
            return;
        }
        JOptionPane.showMessageDialog(this, "Experiment Batch Created!\nUsing the command line you can work with the generated .batch file", "Batch Saved", JOptionPane.INFORMATION_MESSAGE);
    }
};
