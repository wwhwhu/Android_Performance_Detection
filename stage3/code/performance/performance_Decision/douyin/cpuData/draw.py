# 用图片画出
import joblib
from matplotlib import pyplot as plt
from sklearn import tree

from options import options

feature_names = [options['target9'], options['target15'], options['target14'], options['target16'],
                     options['target17'], options['target18'], options['target19'], options['target20'],
                     options['target21'], options['target28'], options['target29'], options['target30'],
                     options['target31'], options['target32'], options['target33'], options['target34'],
                     options['target35'], options['target1'], options['target44'], options['target5'],
                     options['target23'], options['target36'], options['target37'], options['target38'],
                     options['target39'], options['target40'], options['target41'], options['target42'],
                     options['target43']
                     ]
clf = joblib.load('res/cpuDecisionTreeClassifier.pkl')
plt.figure(figsize=(40,30))
a = tree.plot_tree(clf,
                   feature_names=feature_names,
                   class_names=['Normal', 'CpuAbnormal', 'MemAbnormal'],
                   rounded=True,
                   filled=True,
                   fontsize=14)
plt.savefig('tree.png')