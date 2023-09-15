import os

if __name__ == '__main__':
    x = int(input('请输入：云端部署——0，本地部署——1\n'))
    if x == 1:
        a = input("输入pmml-android-master项目根路径，例如：D:\\OPPO\\stage3\\code\\performance\\pmml-android-master：\n")
        b = input("输入UpdateMonitor项目根路径，例如：D:\\OPPO\\stage3\\code\\performance\\UpdateMonitor：\n")
        y = int(input("ser模型属于哪一类，请输入数字：1：固定前台应用淘宝，2.固定前台应用抖音，3.固定前台应用原神，4.无固定前台应用\n"))
        z = int(input("ser模型目标，请输入数字：1：CPU异常检测，2.GPU异常应用，3.内存异常应用\n"))
        x1 = ['', 'taobao', 'douyin', 'yuanshen', 'entire']
        x2 = ['', 'cpu', 'gpu', 'mem']
        name = x1[int(y)] + x2[int(z)] + ".pmml.ser"
        # 复制模型文件到pmml-android-master
        os.system("copy " + a + "\\pmml-android-example\\target\\generated-sources\\combined-assets\\model.pmml.ser " + b + "\\app\\src\\main\\assets")
        # 删除原来的文件
        if os.path.exists(b + "\\app\\src\\main\\assets\\" + name):
            os.system(f"del \F {b}\\app\\src\\main\\assets\\{name}")
        os.system("cd " + b + "\\app\\src\\main\\assets && rename model.pmml.ser " + name)
    if x == 0:
        a = input("输入performance_Decision项目根路径，例如：D:\\OPPO\\stage3\\code\\performance\\performance_Decision：\n")
        b = input("输入Server项目根路径，例如：D:\\OPPO\\stage3\\code\\performance\\Server：\n")
        y = int(input("pkl模型属于哪一类，请输入数字：1：固定前台应用淘宝，2.固定前台应用抖音，3.固定前台应用原神，4.无固定前台应用\n"))
        z = int(input("pkl模型目标，请输入数字：1：CPU异常检测，2.GPU异常应用，3.内存异常应用\n"))
        x1 = ['', 'taobao_data', 'douyin', 'yuanshen', 'entire']
        x2 = ['', 'cpuData\\res\\cpuDecisionTreeClassifier.pkl', 'gpuData\\res\\gpuDecisionTreeClassifier.pkl', 'memData\\res\\memDecisionTreeClassifier.pkl']
        x3 = ['', 'taobao', 'douyin', 'yuanshen', 'entire']
        # 复制模型文件到Server
        print("copy " + a + "\\" + x1[int(y)] + "\\" + x2[int(z)] + " " + b + "\\" + x3[int(z)])
        os.system("copy " + a + "\\" + x1[int(y)] + "\\" + x2[int(z)] + " " + b + "\\" + x3[int(z)])
