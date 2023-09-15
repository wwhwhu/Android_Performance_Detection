import os

if __name__ == '__main__':
    s = input("输入performance_Decision项目根路径，例如：D:\\OPPO\\stage3\\code\\performance\\performance_Decision：\n")
    y = int(input("PMML模型属于哪一类，请输入数字：1：固定前台应用淘宝，2.固定前台应用抖音，3.固定前台应用原神，4.无固定前台应用\n"))
    z = int(input("PMML模型目标，请输入数字：1：CPU异常检测，2.GPU异常应用，3.内存异常应用\n"))
    x1 = ['', 'taobao_data', 'douyin', 'yuanshen', 'entire']
    x2 = ['', 'cpuData\\res\\', 'gpuData\\res\\', 'memData\\res\\']
    x3 = ['', 'cpuDecisionTreeClassifier.pmml', 'gpuDecisionTreeClassifier.pmml', 'memDecisionTreeClassifier.pmml']
    a = input("输入pmml-android-master项目根路径，例如：D:\\OPPO\\stage3\\code\\performance\\pmml-android-master：\n")
    # 复制模型文件到pmml-android-master
    os.system("copy " + s + "\\" + x1[int(y)] + "\\" + x2[int(z)] + x3[int(z)] + " " + a + "\\pmml-android-example\\src\\main\\pmml")
    # 删除原来的文件
    if os.path.exists(a + "\\pmml-android-example\\src\\main\\pmml\\model.pmml"):
        os.system(f"del \F {a}\\pmml-android-example\\src\\main\\pmml\\model.pmml")
    os.system("cd " + a + "\\pmml-android-example\\src\\main\\pmml && rename " + x3[int(z)] + " model.pmml")
    # 替换内容
    file_path = a + "\\pmml-android-example\\src\\main\\pmml\\model.pmml"
    with open(file_path, "r+") as file:
        lines = file.readlines()
        file.seek(0)
        new_line2 = "新的第二行内容"
        lines[1] = "<PMML xmlns=\"http://www.dmg.org/PMML-4_3\" xmlns:data=\"http://jpmml.org/jpmml-model/InlineTable\" version=\"4.3\">\n"
        file.writelines(lines)
        file.close()
