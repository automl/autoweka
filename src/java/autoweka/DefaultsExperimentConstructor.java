package autoweka;

/**
 * Builds a list experiment that only tries to run the defaults for all valid classifiers
 */
public class DefaultsExperimentConstructor extends ListExperimentConstructor
{
    public DefaultsExperimentConstructor(){
        mExperimentPath = "./defaults/";
    }

    public void addArgStrings()
    {
        for(ClassParams params: mClassifierParams){
            mExperiment.argStrings.add("-targetclass " + params.getTargetClass());
        }
    }
};

