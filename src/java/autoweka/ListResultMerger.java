package autoweka;

import java.io.File;
import java.util.ArrayList;
import java.io.FileInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ListResultMerger
{
    final static Logger log = LoggerFactory.getLogger(ListResultMerger.class);

    public static void main(String[] args) throws Exception
    {
        ArrayList<String> experimentFolders = new ArrayList<String>();
        //TODO: Parse options
        for(String arg: args)
            experimentFolders.add(arg);

        for(String experimentPath: experimentFolders)
        {
            //Get me the experiment
            File folder = new File(experimentPath);
            ListExperiment experiment = ListExperiment.fromXML(experimentPath + File.separator + folder.getName() + ".listexperiment");

            ListResultGroup resGroup = new ListResultGroup(experiment);

            log.info("ListExperiment {}", experimentPath);
            //Now, figure out what trajectories are there
            File[] experimentDirFiles = new File(experimentPath + File.separator).listFiles();
            for(File f: experimentDirFiles)
            {
                if(!f.getName().startsWith(folder.getName() + ".listresults."))
                    continue;
                ListResultGroup childGroup = ListResultGroup.fromXML(new FileInputStream(f));
                //TODO: Check to make sure the experiments match
                for(ListResultGroup.ListResult res: childGroup.results)
                {
                    resGroup.results.add(res);
                }
            }
            resGroup.toXML(experimentPath + File.separator + folder.getName() + ".listresults");
        }
    }
}

