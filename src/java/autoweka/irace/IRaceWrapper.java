package autoweka.irace;

import java.util.Queue;
import java.io.File;
import autoweka.Wrapper;
import autoweka.ClassifierResult;

public class IRaceWrapper extends Wrapper
{
    public static void main(String[] args)
    {
        IRaceWrapper wrapper = new IRaceWrapper();
        wrapper.run(args);
    }

    @Override
    protected void _processWrapperParameterStart(Queue<String> args)
    {
        //First argument is the instance file - smac adds a path here
        mInstance = (new File(args.poll())).getName();
        args.poll();
    }

    @Override
    protected void _processParameter(String arg, Queue<String> args)
    {
        if (arg.equals("-seed"))
        {
            mSeed = args.poll();
        }
        else if (arg.equals("-timeout"))
        {
            mTimeout = Float.parseFloat(args.poll());
        }
    }

    @Override
    protected void _processResults(ClassifierResult res)
    {
        //Get the score
        System.out.println(res.getScore());
        System.exit(0);
    }
}
