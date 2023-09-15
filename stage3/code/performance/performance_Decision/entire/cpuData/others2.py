import joblib
import pandas as pd
from IPython.core.pylabtools import figsize
from matplotlib import pyplot as plt
from sklearn.model_selection import train_test_split
from sklearn.svm import SVC
from sklearn.tree import DecisionTreeClassifier
from sklearn import metrics, tree, naive_bayes
from sklearn2pmml import sklearn2pmml, PMMLPipeline
from foreDeal import prepare_log
from options import options


def getdata(dir, label):
    df_s = pd.read_csv(dir)
    df_s['label'] = label
    df = df_s['label']
    df_s = df_s.drop(['label'], axis=1)
    return df_s, df

# 决策树
def decsionTreeSolution(feature):
    df_s1, df_st1 = getdata('normal/train.csv', 0)
    df_s2, df_st2 = getdata('abnormal/train.csv', 1)
    df_train = pd.concat([df_s1, df_s2], ignore_index=True)
    df_test = pd.concat([df_st1, df_st2], ignore_index=True)
    # 划分训练集和测试集
    x_train, x_test, y_train, y_test = train_test_split(df_train, df_test, test_size=0.3)
    # 训练，预测
    # 定义模型
    # 定义模型
    nb = naive_bayes.GaussianNB()
    nb.fit(x_train, y_train)
    nb.get_params()

    # 预测模型得分
    sorce = nb.score(x_test, y_test)
    print("此模型得分为%s" % sorce)

    # 预测数据，预测特征值
    predict_target = nb.predict(x_test)

    # 预测结果与真实结果的对比
    print('预测数据量：', len(y_test), '正确数据量：', len(predict_target == y_test))
    # 输出准确率，召回率，F值
    print(metrics.classification_report(y_test, predict_target))
    print(metrics.confusion_matrix(y_test, predict_target))

    # 获取测试数据集两列数据集
    X = x_test
    print(X)
    L1 = [n[0] for n in X]
    print(L1)
    L2 = [n[1] for n in X]
    print(L2)


if __name__ == '__main__':
    feature_names = [options['target28'], options['target29'], options['target30'], options['target31'],
                     options['target32'], options['target33'], options['target34'], options['target35'],
                     options['target1'], options['target44'], options['target5'], options['target23'],
                     options['target36'], options['target37'], options['target38'], options['target39'],
                     options['target40'], options['target41'], options['target42'], options['target43'],
                     options['target24'], options['target25'], options['target8']
                     ]
    decsionTreeSolution(feature_names)


