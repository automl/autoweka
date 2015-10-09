package autoweka.ui.experimentconstructors;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JTextField;

import org.javabuilders.swing.SwingJavaBuilder;

import autoweka.ui.PropertyPanel;
import autoweka.ui.StringComboOption;

public class TPE extends PropertyPanel
{
    private JTextField mExecutableText;
    private JTextField mPythonPathText;
    private JTextField mTPERunnerText;
    private JFileChooser mFileChooser = new JFileChooser(new File(System.getProperty("user.dir")));

    public TPE(Frame owner)
    {
        super(owner);
        SwingJavaBuilder.build(this);

        //Load up the properties file if it's there
        Properties props = new Properties();
        try{
            props.load(new java.io.FileInputStream(new File("autoweka.tpe.TPEExperimentConstructor.properties")));
            String prop;
            prop = props.getProperty("python");
            if(prop != null)
                mExecutableText.setText(prop);
            prop = props.getProperty("pythonpath");
            if(prop != null)
                mPythonPathText.setText(prop);
            prop = props.getProperty("tperunner");
            if(prop != null)
                mTPERunnerText.setText(prop);
        }catch(IOException e){
            //Don't care
        }
    }

    @Override
    public String getClassName()
    {
        return "autoweka.tpe.TPEExperimentConstructor";
    }

    @Override
    public Properties getProperties()
    {
        Properties props = new Properties();
        return props;
    }

    public void openExecutable()
    {
        mFileChooser.setDialogTitle("Python Executable");
        mFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if(mFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
            File f = mFileChooser.getSelectedFile();
            mExecutableText.setText(f.getAbsolutePath());            
        }
    }
    
    public void openTPERunner()
    {
        mFileChooser.setDialogTitle("TPE Runner Script");
        mFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if(mFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
            File f = mFileChooser.getSelectedFile();
            mTPERunnerText.setText(f.getAbsolutePath());            
        }
    }

    public Properties getGlobalProperties()
    {
        Properties props = new Properties();
        if(!mExecutableText.getText().isEmpty())
            props.setProperty("python", mExecutableText.getText());
        if(!mPythonPathText.getText().isEmpty())
            props.setProperty("pythonpath", mPythonPathText.getText());
        if(!mTPERunnerText.getText().isEmpty())
            props.setProperty("tperunner", mTPERunnerText.getText());
        return props;
    }

    public String toString()
    {
        return "TPE";
    }
}

