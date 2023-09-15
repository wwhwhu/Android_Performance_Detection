import os

if __name__ == '__main__':
    # 虚拟环境名称
    env_name = "AnomalyDetection"
    # 检查是否存在虚拟环境
    command = f"conda env list | findstr {env_name}"
    output = os.system(command)
    print(output)
    # 如果虚拟环境不存在，则创建新的虚拟环境
    if output != 0:
        create_command = f"conda create --name {env_name} python=3.7 && conda activate {env_name} && pip install scikit-learn==0.23.1 && pip install sklearn2pmml==0.63.1 && pip install joblib==0.16.0 && pip install matplotlib && pip install ipython"
        os.system(create_command)
    s = input("输入performance_Decision项目根路径，例如：D:\\OPPO\\stage3\\code\\performance\\performance_Decision：\n")
    y = int(input("当前采集异常的环境属于哪一类，请输入数字：1：固定前台应用淘宝，2.固定前台应用抖音，3.固定前台应用原神，4.无固定前台应用\n"))
    z = int(input("当前采集的异常数据适用模型，请输入数字：1：CPU异常检测，2.GPU异常应用，3.内存异常应用\n"))
    x1 = ['', 'taobao_data', 'douyin', 'yuanshen', 'entire']
    x2 = ['', 'cpuData', 'gpuData', 'memData']
    os.system(f"conda activate {env_name} && cd " + s + " && python " + x1[int(y)] + "\\" + x2[int(z)] + "\\main.py")
