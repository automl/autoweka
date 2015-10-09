package autoweka.ui;

import java.awt.Component;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import autoweka.Util;

class UIUtil
{
    public static FileNameExtensionFilter msArffFileFilter = new FileNameExtensionFilter("ARFF File", "arff");

    public static void main(String[] args)
    {
        showExceptionDialog(null, "This is a test", new RuntimeException("Satan"));
    }

    public static void showExceptionDialog(java.awt.Component comp, String message, Throwable e)
    {
        showExceptionDialog(comp, message, e, message);
    }
    
    public static String getGlobalProp(String property)
    {
        return getGlobalProp(property, null);
    }

    public static String getGlobalProp(String property, String def)
    {
        if(msGlobalProperties == null)
        {
            loadGlobalProperties();
        }
        return msGlobalProperties.getProperty(property, def); 
    }

    public static void setGlobalProp(String property, String value)
    {
        if(msGlobalProperties == null)
        {
            loadGlobalProperties();
        }
        msGlobalProperties.setProperty(property, value);
    }

    private static Properties msGlobalProperties = null;
    private static void loadGlobalProperties()
    {
        msGlobalProperties= new Properties();
        String propsFilePath = Util.getAutoWekaDistributionPath() + File.separator + "autoweka.ui.properties";
        if(new File(propsFilePath).exists()){
            try {
                msGlobalProperties.load(new FileInputStream(new File(propsFilePath)));
            }catch(Exception e){
                showExceptionDialog(null, "Cannot load UI Properties file", e);
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(){ 
            @Override
            public void run()
            {
                try {
                    msGlobalProperties.store(new FileOutputStream(new File(Util.getAutoWekaDistributionPath() + File.separator + "autoweka.ui.properties")), "AutoWEKA UI Properties");
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void setNoBoldJLabels(Container parent)
    {
        for(Component comp : parent.getComponents()) {
            if(comp instanceof JLabel){
                JLabel label = (JLabel)comp;
                Font f = label.getFont();
                label.setFont(f.deriveFont(f.getStyle() & ~Font.BOLD));
            }else if (comp instanceof Container){
                setNoBoldJLabels((Container)comp);
            }
        }
    }

    public static int runFileChooser(JFileChooser chooser, JFrame parent, JTextField textField, String propertyStorageName)
    {
        //Do we have something to pre-load?
        String textInField = "";
        if(textField != null)
            textInField = textField.getText();
        if(textInField.length() > 0){
            File selectedFile = new File(textInField).getAbsoluteFile();
            chooser.setCurrentDirectory(selectedFile.getParentFile());
            chooser.setSelectedFile(new File(selectedFile.getName()));
        } else  {
            //Fallback to the propertyStorageName's path if it's there, otherwise use the CWD
            String storedPath = getGlobalProp(propertyStorageName);
            if(storedPath != null){
                chooser.setCurrentDirectory(new File(storedPath).getAbsoluteFile());
            }else{
                chooser.setCurrentDirectory(new File("./").getAbsoluteFile());
            }
        }

        int returnStatus = chooser.showOpenDialog(parent);
        if(returnStatus == JFileChooser.APPROVE_OPTION){
            File f = chooser.getSelectedFile();
            if(textField != null)
                textField.setText(f.getAbsolutePath());            

            //Update the property storage with this new magical CWD
            setGlobalProp(propertyStorageName, f.getParent());
        }

        return returnStatus;
    }

    public static void showExceptionDialog(Component comp, String message, Throwable e, String title)
    {
        String exceptionMsg = null;
        Throwable eTrav = e;
        while(eTrav != null){
            if(eTrav.getCause() != null && eTrav.getCause().toString() != null){
                eTrav = eTrav.getCause();
            }else{
                break;
            }
        }
        if(eTrav != null){
            exceptionMsg = eTrav.getMessage();
        }

        String dialogText = message;
        if(exceptionMsg != null)
            dialogText += "\n" + exceptionMsg;
        if(JOptionPane.showOptionDialog(comp, dialogText, title, JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, new Object[]{"OK", "Show Stack Trace"}, "OK") == 1)
        {
            StackTraceDialog.showDialog(comp, e);
            
        }
    }

    static class StackTraceDialog extends JDialog
    {
        private StackTraceDialog(Component comp, Throwable e)
        {
            super(comp != null ? SwingUtilities.windowForComponent(comp) : null, "Stack Trace", Dialog.ModalityType.APPLICATION_MODAL);
            //They asked to see the exception stack trace....
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            //Also dump it to stdout
            e.printStackTrace();

            JPanel pan = new JPanel();
            pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
            add(pan);

            JTextArea text = new JTextArea(stringWriter.toString());
            text.setEditable(false);
            pan.add(new JScrollPane(text));

            setSize(500,500);
            
            JButton btn = new JButton("OK");
            btn.setAlignmentX(0.5f);
            btn.addActionListener(new Closer(this));
            pan.add(btn);
        }

        private static class Closer implements ActionListener
        {
            public StackTraceDialog parent;
            public Closer(StackTraceDialog p){
                parent = p;
            }
            public void actionPerformed(ActionEvent e){
                parent.dispose();
            };
        }

        public static void showDialog(Component comp, Throwable e)
        {
            //Blah
            StackTraceDialog diag = new StackTraceDialog(comp, e);
            diag.show();
        }
    }
};
