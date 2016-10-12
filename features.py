# -*- coding: utf-8 -*-
"""
Created on Tue Sep 27 13:08:49 2016

@author: cs390mb

This file is used for extracting features over windows of tri-axial accelerometer 
data. We recommend using helper functions like _compute_mean_features(window) to 
extract individual features.

As a side note, the underscore at the beginning of a function is a Python 
convention indicating that the function has private access (although in reality 
it is still publicly accessible).

"""

import numpy as np

def _compute_magnitude(window):
    magnitudex = 0
    magnitudey = 0
    magnitudez = 0
    for [x,y,z] in window:
        magnitudex += x**2
        magnitudey += y**2
        magnitudez += z**2
    magnitude = [((magnitudex + magnitudey + magnitudez)/20)**.5]

    return magnitude
    
def _compute_rms(window):
    rms = 0
    for [x,y,z] in window:
        rms += (x**2 + y**2 + z**2)/3
    return [(rms/20)**.5]
    
def _compute_mean_features(window):
    """
    Computes the mean x, y and z acceleration over the given window. 
    """
    mean = np.mean(window, axis=0)
    return mean

#def _compute_rfft(window):
#    return np.fft.rfft(window).astype(float)[:2]

def extract_features(window):
    """
    Here is where you will extract your features from the data over 
    the given window. We have given you an example of computing 
    the mean and appending it to the feature matrix X.
    
    Make sure that X is an N x d matrix, where N is the number 
    of data points and d is the number of features.
    
    """
    
    x = []

    x = np.append(x, _compute_mean_features(window)) 
    #print "x " + str(x)
    x = np.append(x, _compute_magnitude(window)) 
    x = np.append(x, _compute_rms(window))
    print "x " + str(x)   
    #x = np.append(x, _compute_rfft(window))
    print "x " + str(x)
        
    
    return x