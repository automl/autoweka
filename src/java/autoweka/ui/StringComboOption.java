package autoweka.ui;

public class StringComboOption
{
    public String mLabel;
    public String mData;

    public StringComboOption(String label, String data)
    {
        mLabel = label;
        mData = data;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    public String getData()
    {
        return mData;
    }
}
