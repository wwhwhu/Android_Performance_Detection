import subprocess
import os
import time


def run():
    s = "cd " + os.getcwd().replace("\\", "/") + "/jni && ndk-build"
    print(s)
    os.system(s)


def push():
    os.system("adb shell mkdir ./data/local/tmp/cpu")
    s = "adb push " + os.getcwd().replace("\\", "/") + "/libs/arm64-v8a/AnomalySimCPU ./data/local/tmp/cpu"
    print(s)
    os.system(s)
    os.system("adb shell chmod 777 /data/local/tmp/cpu/AnomalySimCPU")


def cmd(command):
    subp = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, encoding="utf-8")


if __name__ == '__main__':
    run()
    push()
    msg = input("输入CPU死循环线程数（0-8）\n")
    msg = int(msg)
    for i in range(msg):
        cmd("adb shell ./data/local/tmp/cpu/AnomalySimCPU")
