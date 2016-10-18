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
x_axis = []
y_axis = []
z_axis = []
rms = 0
t = np.arange(256)
s = np.sin(t)
def _compute_magnitude(window):
    global magnitude
    global x_axis
    global y_axis
    global z_axis
    global rms    
    magnitudex = 0
    magnitudey = 0
    magnitudez = 0
    for [x,y,z] in window:
        x_axis.append(x)
        y_axis.append(y)
        z_axis.append(z)
        rms += (x**2 + y**2 + z**2)/3
        magnitudex += x**2
        magnitudey += y**2
        magnitudez += z**2
        magnitude.append((magnitudex + magnitudey + magnitudez)**.5)
    return
    
def _compute_std_x():
    return np.std(x_axis)

def _compute_std_y():
    return np.std(y_axis)
    
def _compute_std_z():
    return np.std(z_axis)

def _compute_median_x():
    return np.median(x_axis)

def _compute_median_y():
    return np.median(y_axis)

def _compute_median_z():
    return np.median(z_axis)
    
def _compute_rms(window):
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
    return np.median(magnitude)

def _compute_std_magnitude():
    return np.std(magnitude)

def _compute_rfft_x():
    global x_axis
    n_freq = 8
    sp =np.fft.fft(x_axis,n=n_freq)
    freq = np.fft.fftfreq(n_freq)
    print sp.real.argmax()
    return freq[sp.real.argmax()]
    
def _compute_rfft_y():
    global x_axis
    n_freq = 8
    sp =np.fft.fft(y_axis,n=n_freq)
    freq = np.fft.fftfreq(n_freq)
    print sp.real.argmax()
    return freq[sp.real.argmax()]
def _compute_rfft_z():
    global x_axis
    n_freq = 8
    sp =np.fft.fft(z_axis,n=n_freq)
    freq = np.fft.fftfreq(n_freq)
    print sp.real.argmax()
    return freq[sp.real.argmax()]
    
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
    x = np.append(x, _compute_std_x())
    #print '2'    
    x = np.append(x, _compute_std_y())
    #print '3'
    x = np.append(x, _compute_std_z())
    #print '4'
    x = np.append(x, _compute_rms(window))
    #print '5'
    x = np.append(x, _compute_median_x())
    #print '6'
    x = np.append(x, _compute_median_y())
    #print '7'
    x = np.append(x, _compute_median_z())
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
    x = np.append(x, _compute_rfft_x())
     #print "x " + str(x)
        
    
    return x