package autoweka.tpe;

import java.util.ArrayList;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import autoweka.ExperimentConstructor;
import autoweka.Conditional;
import autoweka.Parameter;
import autoweka.ClassParams;

public class TPEExperimentConstructor extends ExperimentConstructor
{
    static int msUniqueID = 0;
    private static String getUniqueName()
    {
        msUniqueID++;
        return "auto_param_" + msUniqueID;
    }

    private HyperoptGroup mRootParam = new HyperoptGroup(false, null, new ArrayList<HyperoptNode>() );

    public void prepareExperiment(String path)
    {
        //Generate all the params
        generateAlgorithmParams();

        try
        {
            //Print out the param file
            printParamFile(new PrintStream(new java.io.File(path + "tpe.params")));
            //Print out the instance list
            printInstanceFile(new PrintStream(new java.io.File(path + "tpe.instances")));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public String getTrajectoryParserClassName()
    {
        return "autoweka.tpe.TPETrajectoryParser";
    }

    @Override
    public List<String> getCallString(String experimentPath)
    {
        //Which python to use
        String pythonexec = mProperties.getProperty("python", "python2");
        //Where is the tperunner.py
        String tpeRunnerFileName = mProperties.getProperty("tperunner");
        if(tpeRunnerFileName == null)
            throw new RuntimeException("The property 'tperunner' was not specified to point at a file");
        java.io.File tpeRunnerFile = new java.io.File(autoweka.Util.expandPath(tpeRunnerFileName));
        if(!tpeRunnerFile.isFile())
            throw new RuntimeException("'" + tpeRunnerFileName + "' does not exist");
        
        tpeRunnerFileName = tpeRunnerFile.getAbsolutePath();

        String wrapperCmd = autoweka.Util.getJavaExecutable() + " -Xmx" + mExperiment.memory + " -cp " + autoweka.Util.getAbsoluteClasspath() + " autoweka.tpe.TPEWrapper -prop " + getWrapperPropString().replace("\\", "\\\\") + " -timeout " + mExperiment.trainTimeout + " -wrapper";
        List<String> callString = new ArrayList<String>();
        callString.add(pythonexec);
        callString.add(tpeRunnerFileName);
        callString.add("-s");
        callString.add("{SEED}");
        callString.add("-t");
        callString.add(Float.toString(mExperiment.tunerTimeout));
        callString.add("-i");
        callString.add("tpe.instances");
        callString.add("-p"); 
        callString.add("tpe.params");
        callString.add("-e");
        callString.add(wrapperCmd);
        return callString;
    }

    public List<String> getEnvVariables()
    {
        //What do we need to add to the PYTHON_PATH
        String pythonPath = mProperties.getProperty("pythonpath");
        if(pythonPath != null)
        {
            return Collections.singletonList("PYTHONPATH=" + autoweka.Util.expandEnvironmentVariable(pythonPath));
        }
        else
            return Collections.emptyList(); 
    }
    public String getType()
    {
        return "TPE";
    }

    public void printInstanceFile(PrintStream out)
    {
        List<String> instanceStrings = mInstanceGenerator.getAllInstanceStrings(mExperiment.instanceGeneratorArgs);
        for(String s:instanceStrings)
        {
            out.println(s);
        }
    }

    public void generateAlgorithmParams()
    {
        //Sanity check time!
        if(mBaseClassParams.size() == 0)
        {
            throw new RuntimeException("Could not find any base classifiers for this dataset");
        }
        mIncludeMeta &= mMetaClassParams.size() > 0;
        mIncludeEnsemble &= mEnsembleClassParams.size() > 0;

        //Do we need to do attribute selection?
        if(mExperiment.attributeSelection){
            HyperoptChoice searchChoice = new HyperoptChoice(false, null);
            searchChoice.choices.add(new HyperoptString(true, "attributesearch", "NONE"));

            HyperoptGroup grp = new HyperoptGroup(false, null);
            searchChoice.choices.add(grp);

            HyperoptChoice searchChoiceRoot = new HyperoptChoice(false, null);
            grp.children.add(searchChoiceRoot);

            for(ClassParams searchParams : mAttribSearchClassParams){
                HyperoptGroup searchGroup = new HyperoptGroup(false, null);
                searchGroup.containsNamed = true;
                searchGroup.prefix = "assearch_" + getPrefix(searchParams.getTargetClass()) + "_";

                HyperoptString searchClassStr = new HyperoptString(true, "attributesearch", searchParams.getTargetClass());
                searchClassStr.ignorePrefix = true;
                searchGroup.children.add(searchClassStr);

                addClassifierParameters(searchGroup, searchParams);

                searchChoiceRoot.choices.add(searchGroup);
            }

            HyperoptChoice evalChoiceRoot = new HyperoptChoice(false, null);
            grp.children.add(evalChoiceRoot);
            for(ClassParams evalParams : mAttribEvalClassParams){
                HyperoptGroup evalGroup = new HyperoptGroup(false, null);
                evalGroup.containsNamed = true;
                evalGroup.prefix = "aseval_" + getPrefix(evalParams.getTargetClass()) + "_";

                HyperoptString evalClassStr = new HyperoptString(true, "attributeeval", evalParams.getTargetClass());
                evalClassStr.ignorePrefix = true;
                evalGroup.children.add(evalClassStr);

                addClassifierParameters(evalGroup, evalParams);

                evalChoiceRoot.choices.add(evalGroup);
            }

            //grp.children.add(searchChoice);
            grp.children.add(new HyperoptString(true, "attributetime", Float.toString(mExperiment.attributeSelectionTimeout)));
            mRootParam.children.add(searchChoice);
        }
        
        
        //First, insert the top level choice about which classifer is going to be selected
        HyperoptChoice rootChoice = new HyperoptChoice(false, "is_base");
        mRootParam.children.add(rootChoice);

        if(mIncludeBase)
        {
            HyperoptChoice baseChoice = new HyperoptChoice(false, "base_choice");
            rootChoice.choices.add(baseChoice);

            for(ClassParams clsParams: mBaseClassParams)
            {
                if(!mAllowedClassifiers.contains(clsParams.getTargetClass()))
                    continue;

                HyperoptGroup clsGroup = new HyperoptGroup(false, null);
                clsGroup.containsNamed = true;
                clsGroup.prefix = "_0_" + getPrefix(clsParams.getTargetClass()) + "_";
                //clsGroup.prefix = prefix + "_" + getPrefix(clsParams.getTargetClass()) + "_"
                HyperoptString targetclassStr = new HyperoptString(true, "targetclass", clsParams.getTargetClass());
                targetclassStr.ignorePrefix = true;
                clsGroup.children.add(targetclassStr);

                addClassifierParameters(clsGroup, clsParams);

                baseChoice.choices.add(clsGroup);
            }
        }

        if(mIncludeMeta || mIncludeEnsemble)
        {
            //The choice node choosing between meta/ensemble
            HyperoptChoice metaSwitch = new HyperoptChoice(false, "is_meta");
            rootChoice.choices.add(metaSwitch);

            if(mIncludeMeta)
            {
                HyperoptGroup metaGroup = new HyperoptGroup(false, null);

                HyperoptChoice metaChoice = new HyperoptChoice(false, "meta_choice");
                metaGroup.children.add(metaChoice);

                boolean didMeta = false;
                for(ClassParams clsParams: mMetaClassParams)
                {
                    if(!mAllowedClassifiers.contains(clsParams.getTargetClass()))
                        continue;
                    didMeta = true;
                    HyperoptGroup clsGroup = new HyperoptGroup(false, null);
                    clsGroup.containsNamed = true;
                    clsGroup.prefix = "_0_" + getPrefix(clsParams.getTargetClass()) + "_";
                    HyperoptString targetclassStr = new HyperoptString(true, "targetclass", clsParams.getTargetClass());
                    targetclassStr.ignorePrefix = true;
                    clsGroup.children.add(targetclassStr);

                    addClassifierParameters(clsGroup, clsParams);

                    metaChoice.choices.add(clsGroup);
                    didMeta = true;
                }

                if(didMeta)
                {
                    //Clip it in
                    metaSwitch.choices.add(metaGroup);
                    //Now add the base classifiers
                    HyperoptChoice metaBaseChoice = new HyperoptChoice(false, "meta_base_choice");
                    metaGroup.children.add(metaBaseChoice);

                    for(ClassParams clsParams: mBaseClassParams)
                    {
                        HyperoptGroup clsGroup = new HyperoptGroup(false, null);
                        clsGroup.containsNamed = true;
                        clsGroup.prefix = "_1_" + getPrefix(clsParams.getTargetClass()) + "_W";
                        HyperoptString targetclassStr = new HyperoptString(true, clsGroup.prefix, clsParams.getTargetClass());
                        targetclassStr.ignorePrefix = true;
                        clsGroup.children.add(targetclassStr);
                        HyperoptString dashStr = new HyperoptString(true, clsGroup.prefix+"-", "REMOVED");
                        dashStr.ignorePrefix = true;
                        clsGroup.children.add(dashStr);

                        addClassifierParameters(clsGroup, clsParams);

                        metaBaseChoice.choices.add(clsGroup);
                    }
                }
            }

            if(mIncludeEnsemble)
            {
                HyperoptGroup ensembleGroup = new HyperoptGroup(false, null);

                HyperoptChoice ensembleChoice = new HyperoptChoice(false, "ensemble_choice");
                ensembleGroup.children.add(ensembleChoice);

                boolean didEnsemble = false;
                for(ClassParams clsParams: mEnsembleClassParams)
                {
                    if(!mAllowedClassifiers.contains(clsParams.getTargetClass()))
                        continue;
                    HyperoptGroup clsGroup = new HyperoptGroup(false, null);
                    clsGroup.containsNamed = true;
                    clsGroup.prefix = "_0_" + getPrefix(clsParams.getTargetClass()) + "_";
                    HyperoptString targetclassStr = new HyperoptString(true, "targetclass", clsParams.getTargetClass());
                    targetclassStr.ignorePrefix = true;
                    clsGroup.children.add(targetclassStr);

                    addClassifierParameters(clsGroup, clsParams);

                    ensembleChoice.choices.add(clsGroup);
                    didEnsemble = true;
                }

                if(didEnsemble)
                {
                    //Tack this critter in
                    metaSwitch.choices.add(ensembleGroup);
                    //Now add the base classifiers
                    HyperoptGroup ensembleBaseGroup = new HyperoptGroup(false, null);
                    ensembleGroup.children.add(ensembleBaseGroup);
                    HyperoptChoice ensembleBaseChoice = new HyperoptChoice(false, "ensemble_base_choice");
                    ensembleBaseGroup.children.add(ensembleBaseChoice);


                    for(int i = 0; i < mEnsembleMaxNum; i++)
                    {
                        for(ClassParams clsParams: mBaseClassParams)
                        {
                            HyperoptGroup clsGroup = new HyperoptGroup(false, null);
                            clsGroup.containsNamed = true;
                            clsGroup.prefix = "_1_" + String.format("%02d", i) + "_" + getPrefix(clsParams.getTargetClass()) + "_1_";
                            HyperoptString targetclassStr = new HyperoptString(true, "_1_" +String.format("%02d", i) + "_" + getPrefix(clsParams.getTargetClass()) + "_0_QUOTE_START_B", clsParams.getTargetClass());
                            targetclassStr.ignorePrefix = true;
                            clsGroup.children.add(targetclassStr);

                            addClassifierParameters(clsGroup, clsParams);

                            HyperoptString dashStr = new HyperoptString(true, "_1_" + String.format("%02d", i) + "_" + getPrefix(clsParams.getTargetClass()) + "_2_QUOTE_END", "REMOVED");
                            dashStr.ignorePrefix = true;
                            clsGroup.children.add(dashStr);

                            ensembleBaseChoice.choices.add(clsGroup);
                        }
                        //Do we need to add a new choice down a level?
                        if(i < mEnsembleMaxNum-1)
                        {
                            HyperoptChoice nextBaseChoice = new HyperoptChoice(false, null);
                            ensembleBaseGroup.children.add(nextBaseChoice);
                            nextBaseChoice.choices.add(new HyperoptGroup(false, null));
                            HyperoptGroup nextBaseGroup = new HyperoptGroup(false, null);
                            nextBaseChoice.choices.add(nextBaseGroup);
                            ensembleBaseGroup = nextBaseGroup;
                            //And setup the choice for the next level
                            ensembleBaseChoice = new HyperoptChoice(false, null);
                            ensembleBaseGroup.children.add(ensembleBaseChoice);
                        }
                    }
                }
            }
        }

    }

    /*Creates the parameter tree given a ClassParams*/
    public void addClassifierParameters(HyperoptGroup group, ClassParams clsParams)
    {
        ArrayList<Parameter> params = clsParams.getParameters();
        ArrayList<Conditional> conditionals = clsParams.getConditionals();

        createParameterTree(group, params, conditionals);
    }

    private void createParameterTree(HyperoptGroup group, ArrayList<Parameter> params, ArrayList<Conditional> conditionals)
    {
        //From all the parameters at this level, strip out everything relating to our value
        ArrayList<Parameter> branches = new ArrayList<Parameter>();
        ArrayList<Parameter> children = new ArrayList<Parameter>();

        int index = 0;
        while(index < params.size())
        {
            Parameter current = params.get(index);
            //Make sure that no one depends on this, and we depend on nothing

            boolean isParent = false;
            boolean isConditional = false;
            for(Conditional c: conditionals)
            {
                isConditional |= c.parameter == current;
                isParent      |= c.parent == current;
            }

            if(!isParent && !isConditional)
            {
                group.children.add(new HyperoptParam(current));
                params.remove(index);
            }
            else
            {
                if(isParent && !isConditional)
                {
                    branches.add(current);
                }
                else if(!isParent && isConditional)
                {
                    children.add(current);
                }
                index++;
            }
        }

        //We've gotten super messed up - maybe this isn't a tree?
        if(branches.size() == 0 && children.size() > 0)
            throw new RuntimeException("Children identified with no satisfiable parent");

        for(Parameter parent : branches)
        {
            if(parent.type != Parameter.ParamType.CATEGORICAL)
            {
                throw new RuntimeException("Conditionals can only be made on categorical parameters");
            }

            HyperoptChoice choice = new HyperoptChoice(false, null);
            group.children.add(choice);
            for(String activeChoice: parent.categoricalInnards)
            {
                //We need to make a group for this option
                HyperoptGroup childGroup = new HyperoptGroup(false, null);
                childGroup.children.add(new HyperoptString(true, parent.name, activeChoice));
                childGroup.containsNamed = true;
                choice.choices.add(childGroup);

                //Create the filtered list of contionals and parameters that are going to be fed to the recursive call
                ArrayList<Parameter> filtParameters = new ArrayList<Parameter>();
                ArrayList<Conditional> filtConditionals = new ArrayList<Conditional>();


                for(Parameter param: children)
                {
                    //Maintains a list of the conditionals that we needed to walk up the list
                    ArrayList<Conditional> conditionalPath = new ArrayList<Conditional>();
                    Parameter traverseParam = param;
                    while(traverseParam != null)
                    {
                        boolean traversal = false;
                        for(Conditional cond: conditionals)
                        {
                            if(cond.parameter == traverseParam)
                            {
                                traversal = true;
                                //Is this conditional's parent our current parent?
                                if(cond.parent == parent)
                                {
                                    //Check to see if this conditional is active
                                    if(cond.domain.contains(activeChoice))
                                    {
                                        filtParameters.add(param);
                                        for(Conditional neededCond: conditionalPath)
                                        {
                                            filtConditionals.add(neededCond);
                                        }
                                    }
                                    traverseParam = null;
                                    break;
                                }
                                else
                                {
                                    //We might need this conditional
                                    conditionalPath.add(cond);
                                    traverseParam = cond.parent;
                                }
                            }
                        }
                        //We didn't find a transition here
                        if(!traversal)
                        {
                            traverseParam = null;
                        }
                    }
                }

                createParameterTree(childGroup, filtParameters, filtConditionals);
            }
        }

    }

    public void printParamFile(PrintStream out)
    {
        out.print(mRootParam.toCode());
    }

    private abstract class HyperoptNode
    {
        public boolean isNamed;
        public boolean ignorePrefix = false;
        public String name;
        public String prefix = "";

        HyperoptNode(boolean isNamed, String name)
        {
            this.isNamed = isNamed;
            this.name = name;

            if(this.name == null)
                this.name = getUniqueName();
        }

        public String toCode() {
            StringBuilder sb = new StringBuilder();
            toCode(sb, "", 0, 0, false);
            return sb.toString();
        }
        private void toCode(StringBuilder sb, String prefix, int depth, int count, boolean inDict)
        {
            String prefixStr = prefix;
            if(this.ignorePrefix)
                prefixStr = "";
            else if(!this.prefix.equals("") || !prefix.equals(""))
                prefixStr = prefix + this.prefix + "_" + String.format("%02d", count) + "_";

            //Preamble
            indent(sb, depth);
            if(this.isNamed)
            {
                if (!inDict)
                    sb.append("{");
                sb.append("'" + prefixStr + this.name + "':");
            }
            else if (inDict)
                sb.append("'" + prefixStr + this.name + "':");

            //Call the implementation
            _toCode(sb, prefixStr, depth, count, inDict);

            //Postamble
            if(this.isNamed && !inDict)
            {
                sb.append("}");
            }
        }

        protected abstract void _toCode(StringBuilder sb, String prefix, int depth, int count, boolean inDict);

        protected void indent (StringBuilder sb, int n) {
            for(int i = 0; i < n; i++) {
                sb.append("    ");
            }
        }
    }

    private class HyperoptGroup extends HyperoptNode
    {
        public ArrayList<HyperoptNode> children;
        public boolean containsNamed = false;

        public HyperoptGroup(boolean isNamed, String name)
        {
            super(isNamed, name);
            this.children = new ArrayList<HyperoptNode>();
        }
        public HyperoptGroup(boolean isNamed, String name, ArrayList<HyperoptNode> children)
        {
            super(isNamed, name);
            this.children = children;
        }

        public void _toCode(StringBuilder sb, String prefix, int depth, int count, boolean inDict)
        {
            if(containsNamed)
                sb.append("{\n");
            else
                sb.append("(\n");
            int index=0;
            for(HyperoptNode param: children)
            {
                param.toCode(sb, prefix, depth+1, index++, containsNamed);
                sb.append(",\n");
            }
            indent(sb, depth);
            if(containsNamed)
                sb.append("}");
            else
                sb.append(")");
        }
    }

    private class HyperoptChoice extends HyperoptNode
    {
        public ArrayList<HyperoptNode> choices;

        public HyperoptChoice(boolean isNamed, String name)
        {
            super(isNamed, name);
            this.choices = new ArrayList<HyperoptNode>();
        }

        public HyperoptChoice(boolean isNamed, String name, ArrayList<HyperoptNode> choices)
        {
            super(isNamed, name);
            this.choices = choices;
        }

        public void _toCode(StringBuilder sb, String prefix, int depth, int count, boolean inDict)
        {
            sb.append("hp.choice('" + prefix + this.name + "', [\n");

            int index = 0;
            for(HyperoptNode param: choices)
            {
                param.toCode(sb, prefix, depth+1, index++, false);
                sb.append(",\n");
            }
            indent(sb, depth);
            sb.append("])");
        }
    }

    private class HyperoptString extends HyperoptNode
    {
        private String value;
        public HyperoptString(boolean named, String name, String value)
        {
            super(named, name);
            this.value = value;
        }

        public void _toCode(StringBuilder sb, String prefix, int depth, int count, boolean inDict)
        {
            sb.append("'" + value + "'");
        }
    }

    private class HyperoptParam extends HyperoptNode
    {
        private Parameter param;

        public HyperoptParam(Parameter param)
        {
            super(true, param.name);
            this.param = param;
        }

        public void _toCode(StringBuilder sb, String prefix, int depth, int count, boolean inDict)
        {
            switch(param.type)
            {
            case CATEGORICAL:
                //Make sure that the first element in the array is the default
                List<String> catparams = new ArrayList<String>(param.categoricalInnards);
                String tmp = catparams.get(0); catparams.set(0, catparams.get(param.defaultCategoricalIndex)); catparams.set(param.defaultCategoricalIndex, tmp);

                sb.append("hp.choice('" + prefix + this.name + "', [");
                for(String s: catparams)
                {
                    sb.append("'");
                    sb.append(s);
                    sb.append("', ");
                }
                sb.append("])");
                break;
            case NUMERIC:
                sb.append("hp.uniform('" + prefix + this.name + "', " + param.minNumeric + ", " + param.maxNumeric + ")");
                break;
            case LOG_NUMERIC:
                sb.append("hp.loguniform('" + prefix + this.name + "', math.log(" + param.minNumeric + "), math.log(" + param.maxNumeric + "))");
                break;
            case INTEGER:
                sb.append("hp.quniform('" + prefix + this.name + "', " + param.minNumeric + ", " + param.maxNumeric + ",1.0)");
                break;
            case LOG_INTEGER:
                sb.append("hp.qloguniform('" + prefix + this.name + "', math.log(" + param.minNumeric + "), math.log(" + param.maxNumeric + "), 1.0)");
                break;
            default:
                throw new RuntimeException("Crazy Parameter that isn't dealt with yet");
            }
        }
    }

}

