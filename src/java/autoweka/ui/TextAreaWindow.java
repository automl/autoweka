package autoweka.ui;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.javabuilders.swing.SwingBuilder;

class TextAreaWindow extends JFrame
{
    private JScrollPane mScrollPane;
    private JTextArea mTextArea;
    public TextAreaWindow(String title, String text, int width, int height)
    {
        SwingBuilder.build(this);
        mTextArea.setText(text);
        setTitle(title);
        setSize(width, height);
        mScrollPane.getVerticalScrollBar().setValue(0);
        mScrollPane.getHorizontalScrollBar().setValue(0);
    }
}
