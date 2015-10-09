package autoweka.ui.instancegenerators;

import java.awt.Frame;
import java.util.Properties;

import javax.swing.JTextField;

import org.javabuilders.swing.SwingJavaBuilder;

import autoweka.ui.PropertyPanel;

public class RandomSubSampling extends PropertyPanel
{
    private JTextField mStartingSeedText;
    private JTextField mNumSamplesText;
    private JTextField mPercentText;
    private JTextField mBiasText;

    public RandomSubSampling(Frame owner)
    {
        super(owner);
        SwingJavaBuilder.build(this);
    }
    
    @Override
    public String getClassName()
    {
        return "autoweka.instancegenerators.RandomSubSampling";
    }

    @Override
    public Properties getProperties()
    {
        Properties props = new Properties();
        props.setProperty("startingSeed", mStartingSeedText.getText());
        props.setProperty("numSamples", mNumSamplesText.getText());
        props.setProperty("percent", mPercentText.getText());
        props.setProperty("bias", mBiasText.getText());
        
        return props;
    }

    public String toString()
    {
        return "Random Sub-Sampling";
    }
}


