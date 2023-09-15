import os
from threading import Timer
from monitor import Monitor


if __name__ == '__main__':
    # adb连接设备
    with os.popen('adb devices', "r") as p:
        device = p.read().splitlines()[1]
    if device == '':
        print('error! No device')
    print("connected device：", device)
    os.popen('adb root', "r")

    # 初始化监控
    mo = Monitor(dflag=False,wflag=False,pflag=True)

    # 等待2s后 执行监控
    t = Timer(2, mo.runMonitor())
    while 1:
        mo.runMonitor()
