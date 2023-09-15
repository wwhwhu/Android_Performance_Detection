import os

if __name__ == '__main__':
    s = input("输入UpdateMonitor项目根路径，例如：D:\\OPPO\\stage3\\code\\performance\\UpdateMonitor：")
    os.system("cd " + s + "&& gradlew assembleDebug && adb install " + s + "\\app\\build\\outputs\\apk\\debug\\AnomalyDetection.apk")
