import os
if __name__ == '__main__':
    s = input("输入performance_Decision项目根路径，例如：D:\\OPPO\\stage3\\code\\performance\\performance_Decision：\n")
    y = int(input("当前采集异常的环境属于哪一类，请输入数字：1：固定前台应用淘宝，2.固定前台应用抖音，3.固定前台应用原神，4.无固定前台应用\n"))
    z = int(input("当前采集的异常数据适用模型，请输入数字：1：CPU异常检测，2.GPU异常应用，3.内存异常应用\n"))
    x1 = ['', 'taobao_data', 'douyin', 'yuanshen', 'entire']
    x2 = ['', 'cpuData', 'gpuData', 'memData']
    os.system("adb pull data/data/com.wwh.updatemonitor/files/result.csv " + s + "\\" + x1[int(y)] + "\\" + x2[int(z)] + "\\abnormal")
    os.system("cd " + s + "\\" + x1[int(y)] + "\\" + x2[int(z)] + "\\normal && rename result.csv Abnormal.csv")