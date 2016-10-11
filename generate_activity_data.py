# -*- coding: utf-8 -*-
"""
Created on Wed Sep 14 10:40:38 2016

@author: cs390mb

Assignment A2 : Activity Recognition
Part 1 : Supervised Classification

This script generates noisy average and maximum speed data for 
walking, running and biking and plots the datapoints along with 
the decision boundary of a trained Suport Vector Machine (SVM) 
classifier.

Note that this data is generated using means and standard deviations that 
are not entirely realistic. When you collect actual data, you will likely 
find that it is not as clean, i.e. there is more class overlap and therefore 
more ambiguity.

Run this script to visualize the generated data, each data point colored 
according to the corresponding activity. Change the variable n_samples 
to be much smaller (~10 samples) and much larger (> 1000 samples) and 
see how the decision boundaries change.

Refer to the assignment details on what you need to do here.

"""

# %%---------------------------------------------------------------------------
#
#		                          Imports
#
# -----------------------------------------------------------------------------

import numpy as np
import matplotlib.pyplot as plt
from sklearn import svm
from sklearn.metrics import confusion_matrix
from sklearn import cross_validation


# %%---------------------------------------------------------------------------
#
#		                      Initialization
#
# -----------------------------------------------------------------------------

n_samples = 100 # number of data points per activity
n_dim = 2 # number of feature dimensions; 2 for this example so that we can visualize

# for each activity, we specify the expected max, expected average and the standard deviation  
# for simplicity the stddev is the same for both the max and average features.
stats = {'walking' : {'expected_max' : 7.0, 'expected_average' : 3.1, 'stddev' : 1.25, 'samples' : [], 'format' : 'ro'}, 
         'running' : {'expected_max' : 10.5, 'expected_average' : 5.25, 'stddev' : 2.5, 'samples' : [], 'format' : 'go'},
         'biking' : {'expected_max' : 17.0, 'expected_average' : 9.25, 'stddev' : 16.0, 'samples' : [], 'format' : 'bo'}}

plt.figure() # always call plt.figure() unless you want to plot points on an existing plot
plt_handles = [] # handles to the plots, used to configure the legend

X = np.zeros((n_samples * len(stats), n_dim))
y = np.zeros((n_samples * len(stats),), dtype=int)

# %%---------------------------------------------------------------------------
#
#		      Generate data by sampling a normal distribution
#
# -----------------------------------------------------------------------------

for index, (activity, values) in enumerate(stats.items()):
    print("The average speed for {} is {} mph.".format(activity, values['expected_average']))
    # the samples are taken from a normal distribution centered around [expected max, expected average]
    values['samples'] = np.asarray([np.random.multivariate_normal([values['expected_max'], values['expected_average']], values['stddev'] * np.eye(2)) for _ in range(n_samples)])
    # plot the data samples
    h, = plt.plot(values['samples'][:,0], values['samples'][:,1], values['format'], label=activity)
    plt_handles.append(h)
    # append features:
    X[(index*n_samples):((index+1)*n_samples),:] = values['samples']
    # append labels:
    y[(index*n_samples):((index+1)*n_samples)] = [index] * n_samples

# %%---------------------------------------------------------------------------
#
#		          Train Support Vector Machine Classifier
#
# -----------------------------------------------------------------------------
#row is num of rows, col is specific collumn
def sumCol(conf,row,col):
    res = 0
    while row >-1:        
        res = res + conf.item((row, col))
        row -= 1
    return res

#row specific row, col is number of collumns
def sumRow(conf,row,col):
    res = 0
    while col >-1:        
        res = res + conf.item((row, col))
        col -= 1
    return res

n = len(y)

# SVM regularization parameter : For the purpose of this assignment, we will 
# just set it to 1.0; in real applications, you should choose the C that 
# gives the best performance metrics on a validation dataset, a separate 
# dataset in addition to training/test. You don't need to do this.
C = 1.0  

clf = svm.SVC(kernel = 'linear', C=C )

cv = cross_validation.KFold(n, n_folds=10, shuffle=False, random_state=None)

for i, (train_indexes, test_indexes) in enumerate(cv):
    X_train = X[train_indexes, :]
    y_train = y[train_indexes]
    X_test = X[test_indexes, :]
    y_test = y[test_indexes]
    clf.fit(X_train, y_train)

    # predict the labels on the test data
    y_pred = clf.predict(X_test)

    # show the comparison between the predicted and ground-truth labels
    conf = confusion_matrix(y_test, y_pred, labels=[0,1,2])
    
    print("Fold {} : The confusion matrix is :".format(i))
    print conf
    
    
    
    # TODO: Compute the accuracy, precision and recall from the confusion matrix
    col0 = sumCol(conf, 2, 0)
    col1 = sumCol(conf, 2, 1)
    col2 = sumCol(conf, 2, 2)
    row0 = sumRow(conf, 0, 2)
    row1 = sumRow(conf, 1, 2)
    row2 = sumRow(conf, 2, 2)
    precise0 = 0.0
    precise1 = 0.0
    precise2 = 0.0
    recall0 = 0.0
    recall1 = 0.0
    recall2 = 0.0
    accuracy = (conf.item((0,0)) + conf.item((1,1)) + conf.item((2,2))) / 30.0
    print "Accuracy is: " + str(accuracy)
    print("\n")
    #recall = conf.item((0,0)) / (conf.item((0,0)) + conf.item((0,0)))
    if conf.item((0,0)) == 0 and col0 == 0:
      print "Precision walking is: NaN"
      print("\n")
    elif conf.item((0,0)) == 0 and col0 != 0:
        print "Precision walking is:" + str(precise0) 
        print("\n")
    else:
        precise0 = conf.item((0,0)) / float(col0)
        print "Precision walking is: " + str(precise0)
        print("\n")
    if conf.item((1,1)) == 0 and col1 != 0:
        print "Precision running is:" + str(precise1)
        print("\n")
    elif conf.item((1,1)) == 0 and col1 == 0:
      print "Precision running is: NaN"
      print("\n")    
    else:
         precise1 = conf.item((1,1)) / float(col1)
         print "Precision running is: " + str(precise1)
         print("\n")
    if conf.item((2,2)) == 0 and col2 == 0:
      print "Precision biking is: NaN"
      print("\n")
    elif conf.item((2,2)) == 0 and col2 != 0:
        print "Precision biking is:" + str(precise2)
        print("\n")
    else:
         precise2 = conf.item((2,2)) / float(col2)
         print "Precision biking is: " + str(precise2)
         print("\n")
    
    #precise1 = conf.item((1,0)) / (conf.item((1,0)) + conf.item((1,1)) + conf.item((1,2)) + 0.0)
    #precise2 = conf.item((2,0)) / (conf.item((2,0)) + conf.item((2,1)) + conf.item((2,2))+ 0.0)
   
    
   
    

# TODO: Output the average accuracy, precision and recall over the 10 folds 

# TOO: Then change the CV parameter shuffle to True and describe how the results change.


# Train on entire dataset; that will give us the decision boundary we'll plot.
clf.fit(X, y)

# %%---------------------------------------------------------------------------
#
#		                 Plot Decision Boundaries
#
# -----------------------------------------------------------------------------

xx = np.linspace(0, 30)

# Decision boundary for walking and biking (we won't plot this one)

w = clf.coef_[0] #+ clf.coef_[1]
a = -w[0] / w[1]
yy = a * xx - (clf.intercept_[0]) / w[1]

# Plot decision boundary for walking and running

w = clf.coef_[1] #+ clf.coef_[1]
a = -w[0] / w[1]
yy = a * xx - (clf.intercept_[1]) / w[1]

plt.plot(xx, yy, 'k-')

# Plot decision boundary for running and biking

w = clf.coef_[2]
a = -w[0] / w[1]
yy = a * xx - (clf.intercept_[2]) / w[1]

plt.plot(xx, yy, 'k-')

# %%---------------------------------------------------------------------------
#
#		                   Format and Show Plot
#
# -----------------------------------------------------------------------------

plt.xlabel('max speed (mph)')
plt.ylabel('average speed (mph)')
plt.title('Average and max speed data samples for various activities')
plt.legend(handles = plt_handles)
plt.show()
# call plt.savefig(filename) to save to disk