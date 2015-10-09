package autoweka.tpe;

import java.util.Queue;
import autoweka.Wrapper;
import autoweka.ClassifierResult;

class TPEWrapper extends Wrapper
{
    public static void main(String[] args)
    {
        TPEWrapper wrapper = new TPEWrapper();
        wrapper.run(args);
    }

    @Override
    protected void _processWrapperParameterStart(Queue<String> args)
    {
        //First argument is the instance file - smac adds a path here
        mInstance = args.poll();
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
        String status = res.getCompleted() ? "GOOD" : "FAILED";
        //Print the result string
        String extra = "";
        if(res.getMemOut()){
            extra += "MEMOUT";
        }
        System.out.println("Result for TPE: " + status + ", time=" + res.getTime() + ", score=" + res.getScore() + ", penalty=" + res.getNormalizationPenalty() + ", rawScore=" + res.getRawScore() + extra);
    }
}

