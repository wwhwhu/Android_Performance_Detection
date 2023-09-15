import subprocess
import os
import time


def run():
    os.system("cd C:/Users/DELL/Desktop/性能异常模拟/MEM/jni && ndk-build")

def push():
    os.system("adb push C:/Users/DELL/Desktop/性能异常模拟/MEM/libs/arm64-v8a/AnomalySim ./data/local/tmp/mem/")
    time.sleep(5)

def cmd(command):
    subp = subprocess.Popen(command,shell=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,encoding="utf-8")

if __name__=='__main__':
    run()
    push()
    print("执行性能异常程序中。。。")
    cmd("adb shell ./data/local/tmp/mem/AnomalySimMEM")