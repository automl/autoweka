from hyperopt import fmin, tpe, hp
from optparse import OptionParser
import logging
import math
import re
import subprocess
import sys
import os
logging.basicConfig( stream=sys.stdout, level=logging.INFO)

defaultMaxEvals = 0xFFFFFFFF

#Load up the param file
parser = OptionParser()
parser.add_option("-m", "--maxevals", dest="evals", help="Maximum number of function evals", default=defaultMaxEvals)
parser.add_option("-p", "--parameterspace", dest="parameterSpace", help="Parameter space file")
parser.add_option("-s", "--seed", dest="seed", help="Random number seed", default=0, type="int")
parser.add_option("-e", "--executable", dest="executable", help="Executable")
parser.add_option("-i", "--instancefile", dest="instanceFile", help="instance file")
parser.add_option("-t", "--tunertimeout", dest="tunerTimeout", help="Tuner timeout (seconds)", type="float")
parser.add_option("-f", "--maxfailcount", dest="maxFailCount", help="Max consecutive fails", type="int", default=3)
(options, args) = parser.parse_args()

#The regexes we need to get the result
resultRegex = re.compile("Result for TPE: (?P<status>.+) time=(?P<time>.+), score=(?P<score>.+), penalty=(?P<penalty>.+), rawScore=(?P<rawScore>.+).*")

#TODO Sanity check on the args

#We need to open up the params file
paramSpace = eval(open(options.parameterSpace, 'r').read())

instances = [line.strip() for line in open(options.instanceFile, 'r').readlines()]

trainingTime = 0
tpeTime = 0
lastPythonTime = os.times()[0]

def executeableWrapper(args):
    global trainingTime, tpeTime, lastPythonTime
    def flatten(args):
        if isinstance(args, tuple) or isinstance(args, list):
            flattened = []
            for i in args:
                flattened.extend(flatten(i))
            return flattened
        elif isinstance(args, dict):
            flattened = []
            for k,v in args.iteritems():
                if isinstance(v, dict) or isinstance(v, list) or isinstance(v, tuple):
                    flattened.extend(flatten(v))
                else:
                    flattened.append(k)
                    flattened.append(v)
            return flattened
        else:
            #No idea what this is
            print "TYPE", type(args)
            return args

    def prepareArgs(args):
        if len(args) % 2 != 0:
            raise Exception("Mismatched arg length")
        argPairs = [[args[i], args[i+1]] for i in xrange(0,len(args),2)]
        argPairs.sort()
        return " ".join(["-%s %s" % (pair[0], pair[1]) for pair in argPairs])

    #Get the argument string
    argString = prepareArgs(flatten(args))
    print "ArgString", argString
    sys.stdout.flush()

    #Actually run the executable
    failCount = 0
    computedScore = 100
    losses = []
    failed = False
    for instance in instances:
        execcmd = "%s %s %s" % (options.executable, instance.replace("|", "\\|"), argString)
        #print execcmd
        res = subprocess.check_output(execcmd, stderr=subprocess.STDOUT, shell=True).strip().split("\n")[-1].strip()


        match = resultRegex.match(res)
        if match:
            trainingTime += float(match.group("time"))
            score = float(match.group("score"))
            losses.append(score)
            print "Run Status", instance, res
            if score >= 99.999:
                failCount += 1
            else:
                failCount = 0
        else:
            print "Run Status", instance, "Did not execute correctly"
            failCount += 1
        if failCount >= options.maxFailCount:
            print "Fail count Reached!"
            failed = True
            break
        sys.stdout.flush()
        sys.stderr.flush()

        if options.tunerTimeout > 0 and trainingTime > options.tunerTimeout:
            print "Tuner timeout hit inside loop"
            sys.exit(0)

    #If we didn't fail, update the computed score
    if not failed:
        computedScore = sum(losses)/len(losses)

    pythonTime = os.times()[0] - lastPythonTime
    trainingTime += pythonTime
    tpeTime += pythonTime
    lastPythonTime = os.times()[0]

    #Let the world know how long stuff has been working for
    print "Training time:", trainingTime, ", TPE time: ", tpeTime
    print "Computed score", computedScore

    if options.tunerTimeout > 0 and trainingTime > options.tunerTimeout:
        print "Tuner timeout hit"
        sys.exit(0)

    return computedScore


#We need a small wrapper to set the seed of the TPE
def tpeSuggestWrapper(*args, **kwargs):
    modifiedKwargs = dict(kwargs)
    modifiedKwargs["seed"] = options.seed
    return tpe.suggest(*args, **modifiedKwargs)

#Run the optimization
res = fmin(executeableWrapper,
           space = paramSpace,
           algo = tpeSuggestWrapper,
           max_evals = options.evals,
           rseed = options.seed)

#print "result", res
