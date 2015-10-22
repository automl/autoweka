#!/usr/bin/env python2.7
# encoding: utf-8

'''
spearWrapper -- AClib target algorithm warpper for SAT solver spear

@author:     Marius Lindauer, Chris Fawcett, Alex Fréchette, Frank Hutter
@copyright:  2014 AClib. All rights reserved.
@license:    GPL
@contact:    lindauer@informatik.uni-freiburg.de, fawcettc@cs.ubc.ca, afrechet@cs.ubc.ca, fh@informatik.uni-freiburg.de

example call (in aclib folder structure):
python example_scenarios/spear/spearWrapper.py --runsolver-path example_scenarios/spear/runsolver/runsolver -- example_scenarios/spear/instances/train/qcplin2006.10085.cnf "" 30.0 2147483647 1234 -sp-var-dec-heur 16 -sp-learned-clause-sort-heur 5 -sp-orig-clause-sort-heur 8 -sp-res-order-heur 5 -sp-clause-del-heur 2 -sp-phase-dec-heur 1 -sp-resolution 1 -sp-variable-decay 2 -sp-clause-decay 1.3 -sp-restart-inc 1.9 -sp-learned-size-factor 0.136079 -sp-learned-clauses-inc 1.1 -sp-clause-activity-inc 1.0555555555555556 -sp-var-activity-inc 1.2777777777777777 -sp-rand-phase-dec-freq 0.0010 -sp-rand-var-dec-freq 0.0010 -sp-rand-var-dec-scaling 1.1 -sp-rand-phase-scaling 1 -sp-max-res-lit-inc 2.3333333333333335 -sp-first-restart 43 -sp-res-cutoff-cls 4 -sp-res-cutoff-lits 1176 -sp-max-res-runs 3 -sp-update-dec-queue 1 -sp-use-pure-literal-rule 0
'''

from genericWrapper import AbstractWrapper

class SatWrapper(AbstractWrapper):
    '''
        Simple wrapper for a SAT solver (Spear)
    '''
    
    def get_command_line_args(self, runargs, config):
        '''
        Returns the command line call string to execute the target algorithm (here: Spear).
        Args:
            runargs: a map of several optional arguments for the execution of the target algorithm.
                    {
                      "instance": <instance>,
                      "specifics" : <extra data associated with the instance>,
                      "cutoff" : <runtime cutoff>,
                      "runlength" : <runlength cutoff>,
                      "seed" : <seed>
                    }
            config: a mapping from parameter name to parameter value
        Returns:
            A command call list to execute the target algorithm.
        '''
        binary_path = "example_scenarios/spear-generic-wrapper/Spear-32_1.2.1"
        cmd = "%s --seed %d --model-stdout --dimacs %s" %(binary_path, runargs["seed"], runargs["instance"])       
        for name, value in config.items():
            cmd += " -%s %s" %(name,  value)
        return cmd
    
    def process_results(self, filepointer, out_args):
        '''
        Parse a results file to extract the run's status (SUCCESS/CRASHED/etc) and other optional results.
    
        Args:
            filepointer: a pointer to the file containing the solver execution standard out.
            out_args : a map with {"exit_code" : exit code of target algorithm} 
        Returns:
            A map containing the standard AClib run results. The current standard result map as of AClib 2.06 is:
            {
                "status" : <"SUCCESS"/"SAT"/"UNSAT"/"TIMEOUT"/"CRASHED"/"ABORT">,
                "runtime" : <runtime of target algrithm>,
                "quality" : <a domain specific measure of the quality of the solution [optional]>,
                "misc" : <a (comma-less) string that will be associated with the run [optional]>
            }
            ATTENTION: The return values will overwrite the measured results of the runsolver (if runsolver was used). 
        '''
        import re
        data = filepointer.read()
        resultMap = {}

        if (re.search('s SATISFIABLE', data)) or (re.search('s UNSATISFIABLE', data)):
            resultMap['status'] = 'SUCCESS'

        return resultMap

if __name__ == "__main__":
    wrapper = SatWrapper()
    wrapper.main()    
