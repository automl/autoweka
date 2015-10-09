package autoweka.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.javabuilders.annotations.DoInBackground;
import org.javabuilders.event.BackgroundEvent;
import org.javabuilders.swing.SwingJavaBuilder;

import autoweka.Experiment;
import autoweka.SubProcessWrapper;
import autoweka.TrajectoryGroup;
import autoweka.TrajectoryMerger;
import autoweka.TrajectoryParser;
import autoweka.tools.GetBestFromTrajectoryGroup;
import autoweka.tools.TrainedModelPredictionMaker;

class TrainedModelRunner extends JFrame
{

    private JTextField mExperimentText;
    private JLabel mSelectedErrorLabel;
    private JLabel mSelectedSeedLabel;
    private JLabel mNumEvaluationsLabel;
    private JLabel mNumTimeOutEvaluationsLabel;
    private JLabel mNumMemOutEvaluationsLabel;
    private JTextField mClassifierText;
    private JTextField mClassifierParamsText;
    private JTextField mAttributeSearchText;
    private JTextField mAttributeSearchParamsText;
    private JTextField mAttributeEvalText;
    private JTextField mAttributeEvalParamsText;
    private JFileChooser mFileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
    private JButton mMakePredictionsButton;

    private JPanel mSelectedPanel;
    private JPanel mDetailsPanel;
    
    private JTextField mTestingSetText = new JTextField();

    private GetBestFromTrajectoryGroup mBest;


    public static void main(String[] args)
    {
        TrainedModelRunner tmr = new TrainedModelRunner();
        tmr.setVisible(true);
    }

    public TrainedModelRunner()
    {
        SwingJavaBuilder.build(this);
        UIUtil.setNoBoldJLabels(mSelectedPanel);
        UIUtil.setNoBoldJLabels(mDetailsPanel);
    }

    public void openExperiment()
    {
        openExperiment(null);
    }

    public void openExperiment(String experimentPath)
    {
        mFileChooser.setDialogTitle("Experiment");

        if(experimentPath == null)
        {
            mFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            mFileChooser.setDialogTitle("Experiment to run");
            mFileChooser.setFileFilter(null);
            if(UIUtil.runFileChooser(mFileChooser, this, mExperimentText, "lastExperimentDirLocation") == JFileChooser.CANCEL_OPTION) {
                return;
            }
        }
        else
        {
            mFileChooser.setSelectedFile(new File(experimentPath));
            mExperimentText.setEnabled(false);
            mExperimentText.setEditable(false);
            mExperimentText.setText(experimentPath);
            mMakePredictionsButton.setEnabled(false);
        }

        try
        {
            File expFolder = mFileChooser.getSelectedFile();
            File exp = new File(expFolder.getAbsolutePath() + File.separator + expFolder.getName() + ".experiment");

            if(!exp.exists() || !exp.isFile()){
                throw new RuntimeException(expFolder.getAbsolutePath() + " does not appear to contain a .experiment");
            }
            TrajectoryGroup group = TrajectoryMerger.mergeExperimentFolder(expFolder.getAbsolutePath());

            ArrayList<String> inProgressSeeds = new ArrayList<String>();
            //Go through all the logs and see if we can parse any other partial trajectories
            File[] files = new File(expFolder.getAbsolutePath() + File.separator + "out" + File.separator + "logs").listFiles();
            for(File f: files) {
                String fName = f.getName();
                if(fName.endsWith(".log")){
                    String seed = fName.substring(0, fName.length()-4);
                    if(!group.getSeeds().contains(seed)){
                        inProgressSeeds.add(seed);
                    }
                }
            }

            //Do we want use these inprogress seeds
            if(!inProgressSeeds.isEmpty()){
                Collections.sort(inProgressSeeds);
                StringBuilder msgSB = new StringBuilder("It looks like there are partial runs for the following seeds:\n");
                for(String s: inProgressSeeds){
                    msgSB.append(s);
                    msgSB.append("\n");
                }
                msgSB.append("Do you wish to use these experiments?\nYou may not be able to make predictions from\nthese experiments without training a model first.");
                //We want to use these partial runs if we're in force open mode
                if(experimentPath != null || JOptionPane.showConfirmDialog(this, msgSB.toString(), "Use Partial Runs", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    for(String s: inProgressSeeds){
                        group.addTrajectory(TrajectoryParser.getTrajectory(Experiment.createFromFolder(expFolder), expFolder, s));
                    }
                }
            }

            mBest = new GetBestFromTrajectoryGroup(group);

            //Set all the text blocks
            mExperimentText.setText(expFolder.getAbsolutePath());            
            mSelectedErrorLabel.setText(Float.toString(mBest.errorEstimate));
            mSelectedSeedLabel.setText(mBest.seed);

            mNumEvaluationsLabel.setText(mBest.numEval == -1 ? "" : Integer.toString(mBest.numEval));
            mNumTimeOutEvaluationsLabel.setText(mBest.numTimeOut == -1 ? "" : Integer.toString(mBest.numTimeOut));
            mNumMemOutEvaluationsLabel.setText(mBest.numMemOut == -1 ? "" : Integer.toString(mBest.numMemOut));
            
            setText(mClassifierText, mBest.classifierClass);
            setText(mClassifierParamsText, mBest.classifierArgs);
            setText(mAttributeSearchText, mBest.attributeSearchClass);
            setText(mAttributeSearchParamsText, mBest.attributeSearchArgs);
            setText(mAttributeEvalText, mBest.attributeEvalClass);
            setText(mAttributeEvalParamsText, mBest.attributeEvalArgs);

            //Enable the dataset button
            mMakePredictionsButton.setEnabled(true); 
        }catch(Exception e){
            UIUtil.showExceptionDialog(this, "Failed to open experiment",  e);
        }

    }
    
    private void setText(JTextComponent comp, String text)
    {
        if(text == null){
            comp.setText("");
            comp.setEnabled(false);
            comp.setEditable(false);
        }else{
            comp.setText(text);
            comp.setEnabled(true);
            comp.setEditable(true);
        }
        comp.setCaretPosition(0);
    }

    public boolean openTestingSet()
    {
        mFileChooser.setDialogTitle("Testing Set ARFF");
        mFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        mFileChooser.setFileFilter(UIUtil.msArffFileFilter);
        if(UIUtil.runFileChooser(mFileChooser, this, mTestingSetText, "lastArffLocation") == JFileChooser.APPROVE_OPTION){
            //Time to do the predictions
            return true;
        }
        return false;
    }

    @DoInBackground(indeterminateProgress=true, cancelable=false)
    public void makePredictions(BackgroundEvent evt)
    {
        //Go grab the experiment, and see what stuff we should end up loading
        try
        {
            //Get the model 
            File modelFile = new File(mExperimentText.getText() + File.separator + "trained." + mSelectedSeedLabel.getText() + ".model");
            if(!modelFile.exists()) {
                Properties props = new Properties();
                String seed = mSelectedSeedLabel.getText();
                File experimentDir = new File(mExperimentText.getText());
                props.put("modelOutputFilePrefix", experimentDir.getAbsolutePath() + "/trained." + seed);
                SubProcessWrapper.getErrorAndTime(experimentDir, Experiment.createFromFolder(experimentDir), "default", mBest.rawArgs, seed, props);
            }

            File attribSelectFile = new File(mExperimentText.getText() + File.separator + "trained." + mSelectedSeedLabel.getText() + ".attributeselection");
            String attribSelectPath = null;
            if(attribSelectFile.exists())
                attribSelectPath = attribSelectFile.getAbsolutePath();
            
            
            File tmpFile = File.createTempFile("predictions", ".tmp");
            tmpFile.deleteOnExit();
            TrainedModelPredictionMaker tmpm = new TrainedModelPredictionMaker(attribSelectPath, modelFile.getAbsolutePath(), mTestingSetText.getText(), "last", tmpFile.getAbsolutePath());

            ResultsWindow resWindow= new ResultsWindow(tmpm.eval.toSummaryString(), tmpFile);
            resWindow.setVisible(true);
        }catch(Exception e){
            UIUtil.showExceptionDialog(this, "Failed to get predictions", e);
        }
    }
    
    private class ResultsWindow extends JFrame
    {
        private File mOutputFile;
        private ResultsWindow(String displayText, File outputFile)
        {
            setTitle("Results");
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setSize(500,500);
            mOutputFile = outputFile;
            JPanel pan = new JPanel();
            pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
            add(pan);

            JTextArea text = new JTextArea(displayText);
            text.setEditable(false);
            JScrollPane scroll = new JScrollPane(text);
            pan.add(scroll);
            scroll.getVerticalScrollBar().setValue(0);
            scroll.getHorizontalScrollBar().setValue(0);

            JButton btn = new JButton("Save predictions");
            btn.setAlignmentX(0.5f);
            btn.addActionListener(new Saver(this));
            pan.add(btn);
        }
        
        private class Saver implements ActionListener
        {
            private JFrame mParent;
            public Saver(JFrame parent){
                mParent = parent;
            }
            public void actionPerformed(ActionEvent e){
                mFileChooser.setDialogTitle("Predictions CSV");
                mFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                mFileChooser.setSelectedFile(new File("predictions.csv"));
                mFileChooser.setFileFilter(null);
                if(UIUtil.runFileChooser(mFileChooser, mParent, null, "lastPredictionLocation") == JFileChooser.APPROVE_OPTION){
                    //Time to copy the predictions
                    autoweka.Util.copyFile(mOutputFile, mFileChooser.getSelectedFile());
                }
            };
        }
    }
}
