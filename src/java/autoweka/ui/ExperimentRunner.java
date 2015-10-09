package autoweka.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.Document;

import org.javabuilders.swing.SwingJavaBuilder;

class ExperimentRunner extends JFrame
{
    private JTextField mExpFolderText;
    private JTextField mSeedText;
    private JButton mOpenExpFolderButton;
    private JButton mRunButton;
    private JButton mStopButton;
    private JTextArea mOutputText;
    private JScrollPane mTextScroll;
    private JFileChooser mFileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
    private Observer mObserver;
    private Process mProc;
    private String mCurrentExperimentFolder;
    private String mCurrentSeed;
    
    public static void main(String[] args)
    {
        ExperimentRunner er = new ExperimentRunner();
        er.setVisible(true);
    }

    public ExperimentRunner()
    {
        SwingJavaBuilder.build(this);
    }

    public void openExpFolder()
    {
        mFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        mFileChooser.setDialogTitle("Experiment Folder");
        UIUtil.runFileChooser(mFileChooser, this, mExpFolderText, "lastExperimentDirLocation");
    }

    public void setFolderSeedAndObserver(String folder, String seed, Observer observer)
    {
        mExpFolderText.setText(folder);
        mSeedText.setText(seed);
        mSeedText.setEditable(false);
        mSeedText.setEnabled(false);
        mExpFolderText.setEditable(false);
        mExpFolderText.setEnabled(false);
        mOpenExpFolderButton.setEnabled(false);
        mObserver = observer;
    }

    public void runClicked()
    {
        try
        {
            //Check to make sure that an experiment file exists in there
            if(mExpFolderText.getText().isEmpty())
                throw new RuntimeException("No Experiment Folder has been set");
            File expFolder = new File(mExpFolderText.getText());
            File exp = new File(expFolder.getAbsolutePath() + File.separator + expFolder.getName() + ".experiment");
            if(!exp.exists() || !exp.isFile())
                throw new RuntimeException(exp.getAbsolutePath() + " does not appear to be a valid experiment");
            String seedText = mSeedText.getText();
            if(seedText.isEmpty())
                throw new RuntimeException("No seed defined");

            File seedLog = new File(expFolder + File.separator + "out" + File.separator + "logs" + File.separator + seedText + ".log");            
            if(seedLog.exists())
            {
                if(JOptionPane.showConfirmDialog(this, "Warning: The experiment " + expFolder.getName() + " with seed " + seedText + " has already been run. Do you wish to run it again?", "Experiment Run Exists", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION){
                    return;
                }
            }

            //Store these for the stopButton being pressed....
            mCurrentExperimentFolder = expFolder.getAbsolutePath();
            mCurrentSeed = mSeedText.getText();
            
            //We passed all the checks, actually run it
            new Thread( new Runnable() {
                public void run() {
                    try {
                        ProcessBuilder pb = new ProcessBuilder(autoweka.Util.getJavaExecutable(), "-Xmx128m", "-cp", autoweka.Util.getAbsoluteClasspath(), "autoweka.tools.ExperimentRunner", mExpFolderText.getText(), mSeedText.getText());
                        pb.redirectErrorStream(true);

                        //Get it going 
                        mProc = pb.start();
                        
                        //Make sure we try and kill this when we need to
                        Thread killerHook = new autoweka.Util.ProcessKillerShutdownHook(mProc);
                        Runtime.getRuntime().addShutdownHook(killerHook);
                        
                        //Get some input to throw up in the text area
                        BufferedReader reader = new BufferedReader(new InputStreamReader(mProc.getInputStream()));
                        String line;
                        mOutputText.setText("");
                        Document doc = mOutputText.getDocument();
                        while ((line = reader.readLine ()) != null) {
                            doc.insertString(doc.getLength(), line + "\n", null);
                            mTextScroll.getVerticalScrollBar().setValue(mTextScroll.getVerticalScrollBar().getMaximum());
                        }
                        mRunButton.setEnabled(true);
                        mStopButton.setEnabled(false);

                        Runtime.getRuntime().removeShutdownHook(killerHook);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if(mObserver != null){
                            mObserver.update(null, null); 
                        }
                    }
                } }).start();
            mRunButton.setEnabled(false);
            mStopButton.setEnabled(true);
        }catch(Exception e){
            mStopButton.setEnabled(false);
            //Something went wrong...
            UIUtil.showExceptionDialog(this, "Failed to run experiment", e);
            if(mObserver != null){
                mObserver.update(null, null); 
            }
            mRunButton.setEnabled(true);
        }
    }

    public void stopClicked()
    {
        if(mProc != null)
        {
            mStopButton.setEnabled(false);
            while(true)
            {
                try{
                    try {
                        new File(mCurrentExperimentFolder + File.separator + "out" + File.separator + "runstamps" + File.separator + mCurrentSeed + ".stamp").delete();
                        mProc.getInputStream().close();
                    }catch(Exception e){
                        //UIUtil.showExceptionDialog(this, "Error closing process", e);
                    }
                    mProc.destroy();
                    mProc.waitFor();

                    try {
                        mProc.exitValue();
                        break;
                    }catch(Exception e) { }
                }catch(InterruptedException e){}
            }
            mRunButton.setEnabled(true);
            mProc = null;
        }
    }
}
