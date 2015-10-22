#!/usr/bin/python
import sys, math, random

# For black box function optimization, we can ignore the first 5 arguments. 
# The remaining arguments specify parameters using this format: -name value 

x1 = 0 
x2 = 0

for i in range(len(sys.argv)-1):  
    if (sys.argv[i] == '-x1'):
        x1 = float(sys.argv[i+1])
    elif(sys.argv[i] == '-x2'):
        x2 = float(sys.argv[i+1])   
 
# Compute the branin function:
yValue = (x2 - (5.1 / (4 * math.pi * math.pi)) *x1*x1 + (5 / (math.pi)) *x1 -6) ** 2 + 10*(1- (1 / (8 * math.pi))) * math.cos(x1) + 10




  
# SMAC has a few different output fields; here, we only need the 4th output:
print "Result of algorithm run: SUCCESS, 0, 0, %f, 0" % yValue
 
