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

magnitude = []

def _compute_magnitude(window):
    global magnitude    
    magnitudex = 0
    magnitudey = 0
    magnitudez = 0
    for [x,y,z] in window:
        magnitudex += x**2
        magnitudey += y**2
        magnitudez += z**2
        magnitude.append((magnitudex + magnitudey + magnitudez)**.5)
    return
    
def _compute_std_x(window):
    std_x = []
    for [x,y,z] in window:
        std_x.append(x)
    return np.std(std_x)

def _compute_std_y(window):
    std_y = []
    for [x,y,z] in window:
        std_y.append(y)
    return np.std(std_y)
    
def _compute_std_z(window):
    std_z = []
    for [x,y,z] in window:
        std_z.append(z)
    return np.std(std_z)

def _compute_median_x(window):
    median_x = []
    for [x,y,z] in window:
        median_x.append(x)
    return np.median(median_x)

def _compute_median_y(window):
    median_y = []
    for [x,y,z] in window:
        median_y.append(y)
    return np.median(median_y)

def _compute_median_z(window):
    median_z = []
    for [x,y,z] in window:
        median_z.append(z)
    return np.median(median_z)
    
def _compute_rms(window):
    rms = 0
    for [x,y,z] in window:
        rms += (x**2 + y**2 + z**2)/3
    return [((rms)**.5)/len(window)]
    
def _compute_mean_features(window):
    """
    Computes the mean x, y and z acceleration over the given window. 
    """
    mean = np.mean(window, axis=0)
    return mean
    
def _compute_mean_magnitude(window):    
    return sum(magnitude)/len(window)
    
def _compute_median_magnitude():
    return np.sort(magnitude)[len(magnitude)/2 - 1]

def _compute_std_magnitude():
    return np.std(magnitude)

#def _compute_rfft(window):
#    return np.fft.rfft(window).astype(float)[:2]

def _compute_min():
    return np.amin(magnitude)
    
def _compute_max():
    return np.amax(magnitude)

def extract_features(window):
    """
    Here is where you will extract your features from the data over 
    the given window. We have given you an example of computing 
    the mean and appending it to the feature matrix X.
    
    Make sure that X is an N x d matrix, where N is the number 
    of data points and d is the number of features.
    
    """
    
    x = []
    _compute_magnitude(window)
    x = np.append(x, _compute_mean_features(window))
    #print "x " + str(x)
    #print '1'
    x = np.append(x, _compute_std_x(window))
    #print '2'    
    x = np.append(x, _compute_std_y(window))
    #print '3'
    x = np.append(x, _compute_std_z(window))
    #print '4'
    x = np.append(x, _compute_rms(window))
    #print '5'
    x = np.append(x, _compute_median_x(window))
    #print '6'
    x = np.append(x, _compute_median_y(window))
    #print '7'
    x = np.append(x, _compute_median_z(window))
    #print '8'
    x = np.append(x, _compute_mean_magnitude(window))
    #print '9'
    x = np.append(x, _compute_median_magnitude())
   # print '10'
    x = np.append(x, _compute_std_magnitude())
  #  print '11'
    x = np.append(x, _compute_min())
 #   print '12'
    x = np.append(x, _compute_max())    
#    print 'done'
    #print "x " + str(x)   
    #x = np.append(x, _compute_rfft(window))
    #print "x " + str(x)
        
    
    return x