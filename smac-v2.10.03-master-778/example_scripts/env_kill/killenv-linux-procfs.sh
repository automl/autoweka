#!/bin/bash
#Uses procfs to match environment variable settings
#This may be somewhat brittle, I'm not sure
#On other systems without procfs "ps ewww" may be able to help you
#Author Steve Ramage <seramage@cs.ubc.ca>

#Without this we may kill every process
if [ "$#" -lt 2 ]; then
    echo "Must supply 2 or more arguments the first should be the environment variable key of the process we will terminate, the second should be the value of the key"
    exit 2
fi

#Send a SIGTERM to all processes
fgrep -l "$1=$2" /proc/*/environ -s | sed 's|/proc/||' | sed 's|/environ||' | grep -v self | grep -v "$$" | xargs -L 1 kill -TERM 2> /dev/null 

KILLNEEDED=1
for i in {1..20}
do
PROCS=$(fgrep -l "$1=$2" /proc/*/environ -s | grep -v self | grep -v $$ | wc -l)  #Outputs the number matches

  
  if [ "$PROCS" -eq "0" ]
  then
    #echo "No processes found"
    KILLNEEDED=0
    break
  fi
  
  #After about 1.5 seconds, we will try an INT.
  if [ "$i" -eq "7" ]
  then
    #Try a SIGINT since some things, like shells ignore SIGTERM
    fgrep -l "$1=$2" /proc/*/environ -s | sed 's|/proc/||' | sed 's|/environ||' | grep -v self | grep -v "$$" | xargs -L 1 kill -INT 2> /dev/null 
  fi
  
  sleep 0.25

done


if [ "$KILLNEEDED" -eq 1 ]
then
  fgrep -l "$1=$2" /proc/*/environ -s | sed 's|/proc/||' | sed 's|/environ||' | grep -v self | grep -v "$$" | xargs -L 1 kill -KILL 2> /dev/null 
fi



