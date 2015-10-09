package autoweka;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import java.util.ArrayList;

/**
 * Creates a ListExperiment that does a grid search over the allowable classifiers
 */
public class GridSearchExperimentConstructor extends ListExperimentConstructor
{
    private int mNumGridPoints = 3;

    public GridSearchExperimentConstructor(){
        mExperimentPath = "./gridsearch";
        mIncludeMeta = false;
        mIncludeEnsemble = false;
    }

    public void addArgStrings()
    {
        for(ClassParams params: mClassifierParams){
            addGridSearchPoints(params);
        }
    }

    protected void processArg(String arg, Queue<String> args){
        if(arg.equals("-numPoints")){
            mNumGridPoints = Integer.parseInt(args.poll());
        }
    }

    protected void addGridSearchPoints(ClassParams classParams){
        List<List<String>> argPoints = new ArrayList<List<String>>();
        List<Parameter> params = classParams.getParameters();
        for(int i = 0; i < params.size(); i++){
            argPoints.add(params.get(i).getDiscretization(mNumGridPoints));
        }
        Set<String> argStrings = new HashSet<String>();

        for(List<String> args : Util.cartesianProduct(argPoints)){
            Map<String, String> argMap = new HashMap<String, String>();

            argMap.put("targetclass", classParams.getTargetClass());
            for(int i = 0; i < params.size(); i++){
                argMap.put(params.get(i).name, args.get(i)); 
            }

            argStrings.add( Util.argMapToString(Util.removeHidden( classParams.filterParams(argMap)) ) );
        }

        for(String s: argStrings){
            mExperiment.argStrings.add(s);
        }
    }
};


