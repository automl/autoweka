package autoweka.ui.instancegenerators;

import java.awt.Frame;
import java.util.Properties;

import javax.swing.JTextField;

import org.javabuilders.swing.SwingJavaBuilder;

import autoweka.ui.PropertyPanel;

public class CrossValidation extends PropertyPanel
{
    private JTextField mSeedText;
    private JTextField mNumFoldsText;
    public CrossValidation(Frame owner)
    {
        super(owner);
        SwingJavaBuilder.build(this);
    }
    
    @Override
    public String getClassName()
    {
        return "autoweka.instancegenerators.CrossValidation";
    }

    @Override
    public Properties getProperties()
    {
        Properties props = new Properties();
        props.setProperty("seed", mSeedText.getText());
        props.setProperty("numFolds", mNumFoldsText.getText());
        return props;
    }

    public String toString()
    {
        return "Cross-Validation";
    }
}

