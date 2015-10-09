package autoweka.ui;

import javax.swing.JFrame;

import org.javabuilders.swing.SwingJavaBuilder;

import weka.gui.GUIChooser;

public class Launcher extends JFrame
{
    public static void main(String[] args)
    {
        Launcher l = new Launcher();
        l.setVisible(true);
    }

    public Launcher()
    {
        SwingJavaBuilder.build(this);

    }

    public void wizardClicked()
    {
        Wizard wiz = new Wizard();
        wiz.setVisible(true);
    }

    public void builderClicked()
    {
        ExperimentBuilder builder = new ExperimentBuilder();
        builder.setVisible(true);
    }

    public void runnerClicked()
    {
        ExperimentRunner runner = new ExperimentRunner();
        runner.setVisible(true);
    }

    public void extractorClicked()
    {
        TrainedModelRunner runner = new TrainedModelRunner();
        runner.setVisible(true);
    }

    public void wekaClicked()
    {
        GUIChooser.main(new String[]{}); 
    }
};
