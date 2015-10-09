package autoweka.ui.experimentconstructors;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;

import org.javabuilders.swing.SwingJavaBuilder;

import autoweka.ui.PropertyPanel;
import autoweka.ui.StringComboOption;

public class SMAC extends PropertyPanel
{
    private JTextField mExecutableText;
    private JComboBox mInitialIncumbentCombo;
    private JComboBox mExecutionModeCombo;
    private JTextField mInitialNText;
    private JFileChooser mFileChooser = new JFileChooser(new File(System.getProperty("user.dir")));

    public SMAC(Frame owner)
    {
        super(owner);
        SwingJavaBuilder.build(this);

        //Load up the properties file if it's there
        Properties props = new Properties();
        try{
            props.load(new java.io.FileInputStream(new File("autoweka.smac.SMACExperimentConstructor.properties")));
            String exec = props.getProperty("smacexecutable");
            if(exec != null)
                mExecutableText.setText(exec);
        }catch(IOException e){
            //Don't care
        }

        mInitialIncumbentCombo.setModel(new DefaultComboBoxModel());
        mInitialIncumbentCombo.addItem(new StringComboOption("Random", "RANDOM"));
        mInitialIncumbentCombo.addItem(new StringComboOption("Default", "DEFAULT"));

        mExecutionModeCombo.setModel(new DefaultComboBoxModel());
        mExecutionModeCombo.addItem(new StringComboOption("SMAC", "SMAC"));
        mExecutionModeCombo.addItem(new StringComboOption("ROAR", "ROAR"));
    }

    @Override
    public String getClassName()
    {
        return "autoweka.smac.SMACExperimentConstructor";
    }

    @Override
    public Properties getProperties()
    {
        Properties props = new Properties();

        props.setProperty("executionMode", ((StringComboOption)mExecutionModeCombo.getSelectedItem()).getData());
        props.setProperty("initialIncumbent", ((StringComboOption)mInitialIncumbentCombo.getSelectedItem()).getData());
        props.setProperty("initialN", mInitialNText.getText());
        return props;
    }

    public void openExecutable()
    {
        mFileChooser.setDialogTitle("SMAC Executable");
        mFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if(mFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
            File f = mFileChooser.getSelectedFile();
            mExecutableText.setText(f.getAbsolutePath());            
        }
    }

    public Properties getGlobalProperties()
    {
        Properties props = new Properties();
        if(!mExecutableText.getText().isEmpty())
            props.setProperty("smacexecutable", mExecutableText.getText());
        return props;
    }

    public String toString()
    {
        return "SMAC";
    }
}
