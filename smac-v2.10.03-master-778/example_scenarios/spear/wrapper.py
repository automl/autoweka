#!/usr/bin/env python2.7
# encoding: utf-8

# Example call from SMAC root:
# ./examples/spear/wrapper.py examples/spear/instances/train/qcplin2006.10085.cnf "" 30.0 2147483647 1234 -sp-var-dec-heur 16 -sp-learned-clause-sort-heur 5 -sp-orig-clause-sort-heur 8 -sp-res-order-heur 5 -sp-clause-del-heur 2 -sp-phase-dec-heur 1 -sp-resolution 1 -sp-variable-decay 2 -sp-clause-decay 1.3 -sp-restart-inc 1.9 -sp-learned-size-factor 0.136079 -sp-learned-clauses-inc 1.1 -sp-clause-activity-inc 1.05 -sp-var-activity-inc 1.27 -sp-rand-phase-dec-freq 0.0010 -sp-rand-var-dec-freq 0.0010 -sp-rand-var-dec-scaling 1.1 -sp-rand-phase-scaling 1 -sp-max-res-lit-inc 2.33 -sp-first-restart 43 -sp-res-cutoff-cls 4 -sp-res-cutoff-lits 1176 -sp-max-res-runs 3 -sp-update-dec-queue 1 -sp-use-pure-literal-rule 0

import sys, os, time, re
from subprocess import Popen, PIPE

# Read in first 5 arguments.
instance = sys.argv[1]
specifics = sys.argv[2]
cutoff = int(float(sys.argv[3]) + 1)
runlength = int(sys.argv[4])
seed = int(sys.argv[5])

# Read in parameter setting and build a param_name->param_value map.
params = sys.argv[6:]
configMap = dict((name, value) for name, value in zip(params[::2], params[1::2]))

# Construct the call string to Spear.
spear_binary = os.path.dirname(os.path.realpath(__file__))+"/Spear-32_1.2.1"
cmd = "%s --seed %d --model-stdout --dimacs %s --tmout %d" %(spear_binary, seed, instance, cutoff)       
for name, value in configMap.items():
    cmd += " -%s %s" %(name,  value)
    
# Execute the call and track its runtime.
print(cmd)
start_time = time.time()
io = Popen(cmd.split(" "), stdout=PIPE, stderr=PIPE)
(stdout_, stderr_) = io.communicate()
runtime = time.time() - start_time

# Very simply parsing of Spear's output. Note that in practice we would check the found solution to guard against bugs.
status = "CRASHED"
if (re.search('s UNKNOWN', stdout_)): 
    status = 'TIMEOUT'
if (re.search('s SATISFIABLE', stdout_)) or (re.search('s UNSATISFIABLE', stdout_)):
    status = 'SUCCESS'
    
# Output result for SMAC.
print("Result for SMAC: %s, %s, 0, 0, %s" % (status, str(runtime), str(seed)))  
